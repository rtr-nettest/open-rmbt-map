package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.model.ShapeTilesQueryResult;
import at.rtr.rmbt.map.util.HelperFunctions;
import at.rtr.rmbt.map.util.MapServerOptions;
import at.rtr.rmbt.map.util.TileParameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import net.postgis.jdbc.geometry.Point;
import net.postgis.jdbc.geometry.Polygon;
import net.postgis.jdbc.geometry.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static at.rtr.rmbt.map.util.HelperFunctions.valueToColor;

@Service
public class ShapeTileService extends TileGenerationService {

    // offline fences color (gray), matches PointTileService's offline color
    private final static int COLOR_OFFLINE_RGB = 0x808080;

    @PersistenceContext
    private EntityManager entityManager;

    private static class GeometryColor {
        final Geometry geometry;
        final Color color;

        GeometryColor(final Geometry geometry, final Color color) {
            this.geometry = geometry;
            this.color = color;
        }
    }

    @Override
    protected TileParameters getTileParameters(TileParameters.Path path, TilesRequest params) {
        return new TileParameters.ShapeTileParameters(path, params);
    }

    @Override
    protected byte[] generateTile(TileParameters params, int tileSizeIdx, int zoom, DBox box, MapServerOptions.MapOption mo,
                                  List<MapServerOptions.SQLFilter> filters, float quantile) {
        if (!mo.isFences) {
            filters.add(MapServerOptions.getAccuracyMapFilter());
        }

        double _transparency = params.getTransparency();

        try {
            if (entityManager != null) {

                final StringBuilder whereSQL = new StringBuilder(mo.sqlFilter);
                for (final MapServerOptions.SQLFilter sf : filters)
                    whereSQL.append(" AND ").append(sf.getWhere());

                    final String sql;
                    if (mo.isFences) {
                        // fences join to bev_vgd via test -> test_location (same as non-fences, but through fences table)
                        // most-common technology per shape (mode); aggregated alongside the value/count
                        // count over technology_id (not signal) so offline shapes (signal == null) are still counted
                        sql = String.format(
                                "WITH box AS"
                                        + " (SELECT ST_Transform(ST_SetSRID(ST_MakeBox2D(ST_Point(?,?),"
                                        + " ST_Point(?,?)), 3857), 31287) AS box)"
                                        + " SELECT"
                                        + " (CAST (ST_SnapToGrid(ST_Transform(ST_intersection(p.geom, box.box), 3857), ?,?,?,?) AS VARCHAR)) AS geom,"
                                        + " count(f.technology_id) count,"
                                        + " percentile_disc(?) WITHIN GROUP (ORDER BY %1$s) AS val,"
                                        + " MODE() WITHIN GROUP (ORDER BY f.technology_id) technology"
                                        + " FROM box, bev_vgd p"
                                        + " JOIN test_location tl ON tl.kg_nr_bev=p.kg_nr_int"
                                        + " JOIN fences f ON f.open_test_uuid = tl.open_test_uuid"
                                        + " JOIN test t ON f.open_test_uuid = t.open_test_uuid"
                                        + " WHERE" + " %2$s"
                                        + " AND p.geom && box.box"
                                        + " AND ST_intersects(p.geom, box.box)"
                                        + " GROUP BY p.geom, box.box", mo.valueColumnLog, whereSQL);
                    } else {
                        //debugging hint: St_AsText allows human-readable representation of a geometry object
                        sql = String.format(
                                "WITH box AS"
                                    //input from Browser is converted to 3857; has to be transformed to 31287 for use with bev data
                                    + " (SELECT ST_Transform(ST_SetSRID(ST_MakeBox2D(ST_Point(?,?),"
                                    + " ST_Point(?,?)), 3857), 31287) AS box)"
                                    + " SELECT"
                                    //output has to be transformed to EPSG:3857 for Browsers
                                    + " (CAST (ST_SnapToGrid(ST_Transform(ST_intersection(p.geom, box.box), 3857), ?,?,?,?) AS VARCHAR)) AS geom,"
                                    + " count(%1$s) count,"
                                    + " percentile_disc(?) WITHIN GROUP (ORDER BY %1$s) AS val"
                                    + " FROM box, bev_vgd p"
                                    + " JOIN test_location tl ON tl.kg_nr_bev=p.kg_nr_int"
                                    + " JOIN test t ON t.open_test_uuid = tl.open_test_uuid"
                                    + " WHERE" + " %2$s"
                                    + " AND p.geom && box.box"
                                    + " AND ST_intersects(p.geom, box.box)"
                                    + " GROUP BY p.geom, box.box", mo.valueColumnLog, whereSQL);
                }

                // fences need the technology column -> use the technology-aware result mapping
                final String resultMapping = mo.isFences
                        ? "ShapeTilesQueryResultMappingWithTechnology"
                        : "ShapeTilesQueryResultMapping";
                Query ps = entityManager.createNativeQuery(sql, resultMapping);

                int idx = 1;

                /* makeBox2D */
                final double margin = box.res * 1;
                ps.setParameter(idx++, box.x1 - margin);
                ps.setParameter(idx++, box.y1 - margin);
                ps.setParameter(idx++, box.x2 + margin);
                ps.setParameter(idx++, box.y2 + margin);

                /* snapToGrid */
                ps.setParameter(idx++, box.x1);
                ps.setParameter(idx++, box.y1);
                ps.setParameter(idx++, box.res);
                ps.setParameter(idx++, box.res);

                ps.setParameter(idx++, quantile);

                for (final MapServerOptions.SQLFilter sf : filters) {
                    idx = sf.fillParams(idx, ps);
                }

                List<ShapeTilesQueryResult> result = ps.getResultList();

                final List<GeometryColor> geoms = new ArrayList<>();
                for (ShapeTilesQueryResult rs : result) {
                    //final String geomStr = rs.getGeom();
                    if (rs.getGeom() != null) {
                        final Geometry geom = GeometryBuilder.geomFromString(rs.getGeom());

                        final long count = rs.getCount();
                        final Double val = rs.getVal();

                        final int colorInt;
                        if (mo.isFences) {
                            // offline fences shapes have no signal value but must still be rendered
                            if (Objects.equals(rs.getTechnology(), Constants.TECHNOLOGY_OFFLINE)) {
                                colorInt = COLOR_OFFLINE_RGB;
                            } else {
                                final Integer signal = (val == null) ? null : (int) Math.round(val);
                                colorInt = HelperFunctions.technologyAndSignalStrengthToColor(
                                        rs.getTechnology(), signal, null, null).getRGB() & 0xffffff;
                            }
                        } else {
                            if (val == null)
                                continue;
                            colorInt = valueToColor(mo.colorsSorted, mo.intervalsSorted, val);
                        }
                        double transparency = ((double) count / 20d) * _transparency;
                        if (transparency > _transparency)
                            transparency = _transparency;
                        final int alpha = (int) Math.round(transparency * 255) << 24;
                        final Color color = new Color(colorInt | alpha, true);

                        geoms.add(new GeometryColor(geom, color));
                    }
                }

                if (geoms.isEmpty()) {
                    return null;
                }

                return drawImage(tileSizeIdx, box, geoms);

            }
        } catch (final SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } finally {
            /*
            try
            {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (con != null)
                    con.close();
            }
            catch (final SQLException e)
            {
                e.printStackTrace();
            }
            */

        }
        return null;
    }

    private byte[] drawImage(int tileSizeIdx, DBox box, List<GeometryColor> geoms) throws IOException {

        final Image img = generateImage(tileSizeIdx);
        final Graphics2D g = img.g;

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, img.width, img.height);
//                    g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Path2D.Double path = new Path2D.Double();

        for (final GeometryColor geomColor : geoms) {
            final Geometry geom = geomColor.geometry;

            final Polygon[] polys;
            if (geom instanceof MultiPolygon)
                polys = ((MultiPolygon) geom).getPolygons();
            else if (geom instanceof Polygon)
                polys = new Polygon[]{(Polygon) geom};
            else
                polys = new Polygon[]{};

            for (final Polygon poly : polys)
                for (int i = 0; i < poly.numRings(); i++) {
                    final net.postgis.jdbc.geometry.Point[] points = poly.getRing(i).getPoints();

                    path.reset();
                    boolean initial = true;
                    for (final Point point : points) {
                        final double relX = (point.x - box.x1) / box.res;
                        final double relY = TILE_SIZES[tileSizeIdx] - (point.y - box.y1) / box.res;
                        if (initial) {
                            initial = false;
                            path.moveTo(relX, relY);
                        }
                        path.lineTo(relX, relY);
                    }
                    g.setPaint(geomColor.color);
                    g.fill(path);
                }
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img.bi, "png", baos);
        //Files.write(new File("/tmp/test1.png").toPath(), baos.toByteArray());
        return baos.toByteArray();
    }
}
