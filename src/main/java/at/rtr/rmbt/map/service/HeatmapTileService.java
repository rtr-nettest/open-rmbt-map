package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.model.TilesQueryResult;
import at.rtr.rmbt.map.util.MapServerOptions;
import at.rtr.rmbt.map.util.TileParameters;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static at.rtr.rmbt.map.util.HelperFunctions.valueToColor;

@Service
public class HeatmapTileService extends TileGenerationService {

    @PersistenceContext
    private EntityManager entityManager;

    private final static int[] ZOOM_TO_PART_FACTOR = new int[]{
            // factor | zoomlevel
            0, // 0
            0, // 1
            0, // 2
            0, // 3
            0, // 4
            0, // 5
            0, // 6
            1, // 7
            1, // 8
            2, // 9
            2, // 10
            3, // 11
            3, // 12
            4, // 13
            4, // 14
            5, // 15
            5, // 16
            6, // 17
            6, // 18
            7, // 19
            7, // 20
    };

    private final static double ALPHA_TOP = 0.5;
    private final static int ALPHA_MAX = 1;

    private final static boolean DEBUG_LINES = false;

    private final static int HORIZON_OFFSET = 1;
    private final static int HORIZON = HORIZON_OFFSET * 2 + 2;
    private final static int HORIZON_SIZE = HORIZON * HORIZON;

    private final static double[][] FACTORS = new double[8][]; // lookup table

    private final int[][] pixelBuffers = new int[TILE_SIZES.length][];

    // for speedup
    @PostConstruct
    private void initialize() {
        initializeFactors();
        initializePixelBuffers();
    }

    private void initializePixelBuffers() {
        for (int i = 0; i < TILE_SIZES.length; i++)
        {
            final int tileSize = TILE_SIZES[i];
            pixelBuffers[i] = new int[tileSize * tileSize];

        }
    }

    private void initializeFactors() {
        for (int f = 0; f < 8; f++) {
            final int partSize = 1 << f;
            FACTORS[f] = new double[HORIZON_SIZE * partSize * partSize];

            for (int i = 0; i < FACTORS[f].length; i += HORIZON_SIZE) {
                final double qPi = Math.PI / 4;

                final double x = qPi * (i / HORIZON_SIZE % partSize) / partSize;
                final double y = qPi * (i / HORIZON_SIZE / partSize) / partSize;

                // double sum = 0;
                for (int j = 0; j < HORIZON; j++)
                    for (int k = 0; k < HORIZON; k++) {
                        final double value = Math.pow(Math.cos(x + (1 - j) * qPi), 2.0)
                                * Math.pow(Math.cos(y + (1 - k) * qPi), 2.0) / 4;
                        FACTORS[f][i + j + k * HORIZON] = value;
                        // sum += value;
                    }
            }
        }
    }


    @Override
    protected TileParameters getTileParameters(TileParameters.Path path, TilesRequest params) {
        return new TileParameters(path, params, 0.75);
    }

    @Override
    protected byte[] generateTile(TileParameters params, int tileSizeIdx, int zoom, DBox box, MapServerOptions.MapOption mo, List<MapServerOptions.SQLFilter> filters, float quantile) {
        filters.add(MapServerOptions.getAccuracyMapFilter());

        final int tileSize = TILE_SIZES[tileSizeIdx];

        final double transparency = params.getTransparency();

        final StringBuilder whereSQL = new StringBuilder(mo.sqlFilter);
        for (final MapServerOptions.SQLFilter sf : filters)
            whereSQL.append(" AND ").append(sf.getWhere());

        final String sql = String.format("SELECT count(\"%1$s\") count,"
                + " percentile_disc(?) WITHIN GROUP (ORDER BY \"%1$s\") AS val,"
                + " ST_X(ST_SnapToGrid(location, ?,?,?,?)) gx,"
                + " ST_Y(ST_SnapToGrid(location, ?,?,?,?)) gy"
                + " FROM v_test2 t"
                + " WHERE "
                + " %2$s"
                + " AND location && ST_SetSRID(ST_MakeBox2D(ST_Point(?,?), ST_Point(?,?)), 900913)"
                + " GROUP BY gx,gy", mo.valueColumnLog, whereSQL);

        final int partSizeFactor;
        if (zoom >= ZOOM_TO_PART_FACTOR.length)
            partSizeFactor = ZOOM_TO_PART_FACTOR[ZOOM_TO_PART_FACTOR.length - 1];
        else
            partSizeFactor = ZOOM_TO_PART_FACTOR[zoom];
        final int partSizePixels = 1 << partSizeFactor;

        final int fetchPartsX = tileSize / partSizePixels + (HORIZON_OFFSET + 2) * 2;
        final int fetchPartsY = tileSize / partSizePixels + (HORIZON_OFFSET + 2) * 2;

        final double[] values = new double[fetchPartsX * fetchPartsY];
        // final int[] countsReal = new int[fetchPartsX * fetchPartsY];
        final int[] countsRel = new int[fetchPartsX * fetchPartsY];

        Arrays.fill(values, Double.NaN);

        boolean _emptyTile = true;

        if (entityManager != null)
        {
            try
            {
                Query ps = entityManager.createNativeQuery(sql, "TilesQueryResultMapping");
                int p = 1;
                ps.setParameter(p++, quantile);


                final double partSize = box.res * partSizePixels;
                final double origX = box.x1 - box.res * (partSizePixels / 2) - partSize * (HORIZON_OFFSET + 1);
                final double origY = box.y1 - box.res * (partSizePixels / 2) - partSize * (HORIZON_OFFSET + 1);
                for (int j = 0; j < 2; j++)
                {
                    ps.setParameter(p++, origX);
                    ps.setParameter(p++, origY);
                    ps.setParameter(p++, partSize);
                    ps.setParameter(p++, partSize);
                }

                for (final MapServerOptions.SQLFilter sf : filters) {
                    p = sf.fillParams(p, ps);
                }


                final double margin = partSize * (HORIZON_OFFSET + 1);
                ps.setParameter(p++, box.x1 - margin);
                ps.setParameter(p++, box.y1 - margin);
                ps.setParameter(p++, box.x2 + margin);
                ps.setParameter(p++, box.y2 + margin);

                //            System.out.println(ps);

                List<TilesQueryResult> resultList = ps.getResultList();
                for (TilesQueryResult rs : resultList) {
                    Integer count = rs.getCount();
                    final Double val = rs.getVal();
                    final Double gx = rs.getGx();
                    final Double gy = rs.getGy();

                    if (val == null || count == null || count.equals(0)) {
                        continue;
                    }

                    _emptyTile = false;

                    final int mx = (int) Math.round((gx - origX) / partSize);
                    final int my = (int) Math.round((gy - origY) / partSize);

                    // System.out.println(String.format("%f|%f %d|%d %d %f",gx, gy,
                    // mx, my, count, val));

                    if (mx >= 0 && mx < fetchPartsX && my >= 0 && my < fetchPartsY) {
                        final int idx = mx + fetchPartsX * (fetchPartsY - 1 - my);
                        values[idx] = val;
                        // countsReal[idx] = count;
                        if (count > ALPHA_MAX)
                            count = ALPHA_MAX;
                        countsRel[idx] = count;
                    }
                }

            } catch(SQLException e) {
                System.out.println(e);
            }

            if (_emptyTile)
                return null;

            final Image img = images[tileSizeIdx];

            final int[] pixels = pixelBuffers[tileSizeIdx];
            for (int y = 0; y < tileSize; y++)
                for (int x = 0; x < tileSize; x++)
                {
                    final int mx = HORIZON_OFFSET + 1 + (x + partSizePixels / 2) / partSizePixels;
                    final int my = HORIZON_OFFSET + 1 + (y + partSizePixels / 2) / partSizePixels;
                    final int relX = (x + partSizePixels / 2) % partSizePixels;
                    final int relY = (y + partSizePixels / 2) % partSizePixels;
                    final int relOffset = (relY * partSizePixels + relX) * HORIZON_SIZE;

                    double alphaWeigth = 0;
                    double valueWeight = 0;
                    double valueMissing = 0;
                    final int startIdx = mx - HORIZON_OFFSET + fetchPartsX * (my - HORIZON_OFFSET);

                    for (int i = 0; i < HORIZON_SIZE; i++)
                    {
                        final int idx = startIdx + i % HORIZON + fetchPartsX * (i / HORIZON);
                        if (Double.isNaN(values[idx]))
                            valueMissing += FACTORS[partSizeFactor][i + relOffset];
                        else
                            valueWeight += FACTORS[partSizeFactor][i + relOffset] * values[idx];
                        alphaWeigth += FACTORS[partSizeFactor][i + relOffset] * countsRel[idx];
                    }

                    if (valueMissing > 0)
                        valueWeight += valueWeight / (1 - valueMissing) * valueMissing;

                    alphaWeigth /= ALPHA_TOP;
                    if (alphaWeigth < 0)
                        alphaWeigth = 0;
                    if (alphaWeigth > 1)
                        alphaWeigth = 1;

                    alphaWeigth *= transparency;

                    final int alpha = (int) (alphaWeigth * 255) << 24;
                    assert alpha >= 0 || alpha <= 255 : alpha;
                    if (alpha == 0)
                        pixels[x + y * tileSize] = 0;
                    else
                        pixels[x + y * tileSize] = valueToColor(mo.colorsSorted, mo.intervalsSorted, valueWeight)
                                | alpha;
                    // pixels[x + y * WIDTH] = 255 << 24 | alpha >>> 8 |
                    // alpha >>> 16 | alpha >>> 24;

                    if (DEBUG_LINES)
                        if (relX == partSizePixels / 2 || relY == partSizePixels / 2)
                            pixels[x + y * tileSize] = 0xff000000;
                }
            img.bi.setRGB(0, 0, tileSize, tileSize, pixels, 0, tileSize);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(img.bi, "png", baos);
                //Files.write(new File("/tmp/test").toPath(),baos.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return baos.toByteArray();
        }
        return null;
    }
}
