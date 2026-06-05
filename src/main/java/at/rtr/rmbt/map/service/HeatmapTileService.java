package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.model.TilesQueryResult;
import at.rtr.rmbt.map.util.HelperFunctions;
import at.rtr.rmbt.map.util.MapServerOptions;
import at.rtr.rmbt.map.util.TileParameters;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    // offline fences color (gray), matches PointTileService's offline color
    private final static int COLOR_OFFLINE_RGB = 0x808080;

    private final static boolean DEBUG_LINES = false;

    private final static int HORIZON_OFFSET = 1;
    private final static int HORIZON = HORIZON_OFFSET * 2 + 2;
    private final static int HORIZON_SIZE = HORIZON * HORIZON;

    private final static double[][] FACTORS = new double[8][]; // lookup table

    // for speedup
    @PostConstruct
    private void initialize() {
        initializeFactors();
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
        if (!mo.isFences) {
            filters.add(MapServerOptions.getAccuracyMapFilter());
        }

        final int tileSize = TILE_SIZES[tileSizeIdx];

        final double transparency = params.getTransparency();

        final StringBuilder whereSQL = new StringBuilder(mo.sqlFilter);
        for (final MapServerOptions.SQLFilter sf : filters)
            whereSQL.append(" AND ").append(sf.getWhere());

        final String sql;
        if (mo.isFences) {
            // most-common technology per grid cell (mode); aggregated alongside the value/count
            // count over technology_id (not signal) so offline cells (signal == null) are still counted
            sql = String.format("SELECT count(f.technology_id) count,"
                    + " percentile_disc(?) WITHIN GROUP (ORDER BY %1$s) AS val,"
                    + " ST_X(ST_SnapToGrid(ST_Transform(f.geom4326, 3857), ?,?,?,?)) gx,"
                    + " ST_Y(ST_SnapToGrid(ST_Transform(f.geom4326, 3857), ?,?,?,?)) gy,"
                    + " MODE() WITHIN GROUP (ORDER BY f.technology_id) technology"
                    + " FROM fences f"
                    + " JOIN test t ON f.open_test_uuid = t.open_test_uuid"
                    + " WHERE "
                    + " %2$s"
                    + " AND f.geom4326 && ST_Transform(ST_SetSRID(ST_MakeBox2D(ST_Point(?,?), ST_Point(?,?)), 3857), 4326)"
                    + " GROUP BY gx,gy", mo.valueColumnLog, whereSQL);
        } else {
            sql = String.format("SELECT count(%1$s) count,"
                    + " percentile_disc(?) WITHIN GROUP (ORDER BY %1$s) AS val,"
                    + " ST_X(ST_SnapToGrid(location, ?,?,?,?)) gx,"
                    + " ST_Y(ST_SnapToGrid(location, ?,?,?,?)) gy"
                    + " FROM test t"
                    + " WHERE "
                    + " %2$s"
                    + " AND location && ST_SetSRID(ST_MakeBox2D(ST_Point(?,?), ST_Point(?,?)), 900913)"
                    + " GROUP BY gx,gy", mo.valueColumnLog, whereSQL);
        }

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
        final Integer[] technologies = new Integer[fetchPartsX * fetchPartsY];

        Arrays.fill(values, Double.NaN);

        boolean _emptyTile = true;

        if (entityManager != null)
        {
            try
            {
                // fences need the technology column -> use the technology-aware result mapping
                final String resultMapping = mo.isFences
                        ? "TilesQueryResultMappingWithTechnology"
                        : "TilesQueryResultMapping";
                Query ps = entityManager.createNativeQuery(sql, resultMapping);
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
                    final Integer technology = rs.getTechnology();

                    // offline fences cells have no signal value but must still be rendered
                    final boolean offline = mo.isFences
                            && Objects.equals(technology, Constants.TECHNOLOGY_OFFLINE);

                    if (gx == null || gy == null) {
                        continue;
                    }
                    if (!offline && (val == null || count == null || count.equals(0))) {
                        continue;
                    }

                    _emptyTile = false;

                    final int mx = (int) Math.round((gx - origX) / partSize);
                    final int my = (int) Math.round((gy - origY) / partSize);

                    // System.out.println(String.format("%f|%f %d|%d %d %f",gx, gy,
                    // mx, my, count, val));

                    if (mx >= 0 && mx < fetchPartsX && my >= 0 && my < fetchPartsY) {
                        final int idx = mx + fetchPartsX * (fetchPartsY - 1 - my);
                        if (val != null) {
                            values[idx] = val;
                        }
                        technologies[idx] = technology;
                        // countsReal[idx] = count;
                        int c = (count == null) ? 1 : count;
                        if (c > ALPHA_MAX)
                            c = ALPHA_MAX;
                        countsRel[idx] = c;
                    }
                }

            } catch(SQLException e) {
                System.out.println(e);
            }

            if (_emptyTile)
                return null;

            return drawImage(tileSizeIdx, tileSize, partSizeFactor, partSizePixels,
                    fetchPartsX, transparency, values, countsRel, technologies, mo);

        }
        return null;
    }

    private byte[] drawImage(int tileSizeIdx, int tileSize, int partSizeFactor,
                             int partSizePixels, int fetchPartsX, double transparency,
                             double[] values, int[] countsRel, Integer[] technologies,
                             MapServerOptions.MapOption mo) {
        final Image img = generateImage(tileSizeIdx);

        final int[] pixels = new int[tileSize * tileSize];
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
                // fences-only: figure out the dominant (most weighted) technology and offline weight
                double dominantTechWeight = 0;
                Integer dominantTech = null;
                double offlineWeight = 0;
                final int startIdx = mx - HORIZON_OFFSET + fetchPartsX * (my - HORIZON_OFFSET);

                for (int i = 0; i < HORIZON_SIZE; i++)
                {
                    final int idx = startIdx + i % HORIZON + fetchPartsX * (i / HORIZON);
                    final double factor = FACTORS[partSizeFactor][i + relOffset];
                    if (Double.isNaN(values[idx]))
                        valueMissing += factor;
                    else
                        valueWeight += factor * values[idx];
                    alphaWeigth += factor * countsRel[idx];

                    if (mo.isFences) {
                        final Integer tech = technologies[idx];
                        if (tech != null) {
                            if (Objects.equals(tech, Constants.TECHNOLOGY_OFFLINE)) {
                                offlineWeight += factor;
                            } else if (factor > dominantTechWeight) {
                                dominantTechWeight = factor;
                                dominantTech = tech;
                            }
                        }
                    }
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
                else if (mo.isFences) {
                    final int rgb;
                    if (dominantTech == null && offlineWeight > 0) {
                        // only offline data influences this pixel -> offline gray
                        rgb = COLOR_OFFLINE_RGB;
                    } else {
                        final Integer signal = Double.isNaN(valueWeight)
                                ? null
                                : (int) Math.round(valueWeight);
                        final Color c = HelperFunctions.technologyAndSignalStrengthToColor(
                                dominantTech, signal, null, null);
                        rgb = c.getRGB() & 0xffffff;
                    }
                    pixels[x + y * tileSize] = rgb | alpha;
                }
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
}