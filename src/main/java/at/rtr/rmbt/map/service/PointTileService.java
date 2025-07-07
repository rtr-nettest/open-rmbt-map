package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.model.TilesQueryResult;
import at.rtr.rmbt.map.util.MapServerOptions;
import at.rtr.rmbt.map.util.TileParameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PointTileService extends TileGenerationService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected TileParameters.PointTileParameters getTileParameters(TileParameters.Path path, TilesRequest params) {
        return new TileParameters.PointTileParameters(path, params);
    }

    @Override
    protected byte[] generateTile(TileParameters gParams, int tileSizeIdx, int zoom, DBox box, MapServerOptions.MapOption mo,
                                  List<MapServerOptions.SQLFilter> filters, float quantile) {

        final UUID highlightUUID;
        final byte[] baseTile;

        //@TODO: Fix?
        TileParameters.PointTileParameters params = (TileParameters.PointTileParameters) gParams;

        if (params.getGenericParameters() != null) {
            // recursive call to get generic tile w/o highlight probably from cache

            final TileParameters.PointTileParameters genericParams = params.getGenericParameters();
            baseTile = getTile(genericParams);

            highlightUUID = params.getHighlight();
        } else {
            highlightUUID = null;
            baseTile = null;
        }


        filters.add(MapServerOptions.getAccuracyMapFilter());

        final StringBuilder whereSQL = new StringBuilder(mo.sqlFilter);
        for (final MapServerOptions.SQLFilter sf : filters)
            whereSQL.append(" AND ").append(sf.getWhere());

        final String sql = String.format("SELECT ST_X(t.location) gx, ST_Y(t.location) gy, NULL count, \"%s\" val"
                + " FROM v_test2 t"
                + (highlightUUID == null ? "" : " JOIN client c ON (t.client_id=c.uid AND c.uuid=?)")
                + " WHERE "
                + " %s"
                + " AND location && ST_SetSRID(ST_MakeBox2D(ST_Point(?,?), ST_Point(?,?)), 900913)"
                + " ORDER BY"
                + " t.uid", mo.valueColumn, whereSQL);

        final double diameter = params.getPointDiameter();
        final double radius = diameter / 2d;
        final double triangleSide = diameter * 1.75;
        final double triangleHeight = Math.sqrt(3) / 2d * triangleSide;
        final int transparency = (int) Math.round(params.getTransparency() * 255);
        final boolean noFill = params.isNoFill();
        final boolean noColor = params.isNoColor();

        final Color borderColor = new Color(0, 0, 0, transparency);
        final Color highlightBorderColor = new Color(0, 0, 0, transparency);
        final Color colorUltraGreen = new Color(0, 153, 0, transparency);
        final Color colorGreen = new Color(0, 255, 0, transparency);
        final Color colorYellow = new Color(255, 255, 0, transparency);
        final Color colorRed = new Color(255, 0, 0, transparency);
        final Color colorGray = new Color(128, 128, 128, transparency);

        final List<Dot> dots = new ArrayList<>();

        if (entityManager != null) {
            try {
                Query ps = entityManager.createNativeQuery(sql, "TilesQueryResultMapping");

                int i = 1;

                if (highlightUUID != null)
                    ps.setParameter(i++, highlightUUID);

                for (final MapServerOptions.SQLFilter sf : filters)
                    i = sf.fillParams(i, ps);

                final double margin = box.res * triangleSide;
                ps.setParameter(i++, box.x1 - margin);
                ps.setParameter(i++, box.y1 - margin);
                ps.setParameter(i++, box.x2 + margin);
                ps.setParameter(i++, box.y2 + margin);

//            System.out.println(ps);

                List<TilesQueryResult> resultList = ps.getResultList();
                boolean _emptyTile = true;

                for (TilesQueryResult rs : resultList) {
                    _emptyTile = false;

                    final double cx = rs.getGx();
                    final double cy = rs.getGy();
                    final long value = rs.getVal().longValue();

                    final boolean highlight = highlightUUID != null;

                    final int classification = noColor || noFill ? 0 : mo.getClassification(value);

                    final Color color;
                    switch (classification) {
                        case 4:
                            color = colorUltraGreen;
                            break;
                        case 3:
                            color = colorGreen;
                            break;
                        case 2:
                            color = colorYellow;
                            break;
                        case 1:
                            color = colorRed;
                            break;
                        default:
                            color = colorGray;
                            break;
                    }

                    dots.add(new Dot(cx, cy, color, highlight));
                }

                if (_emptyTile)
                    return baseTile;

                final byte[] data = drawImage(baseTile, diameter, dots, tileSizeIdx, noFill, triangleHeight, triangleSide, box, highlightBorderColor, radius, borderColor);
                return data;
            } catch (SQLException | IOException ex) {
                throw new RuntimeException(ex);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    private byte[] drawImage(byte[] baseTile, double diameter, List<Dot> dots, int tileSizeIdx, boolean noFill, double triangleHeight, double triangleSide, DBox box,
                                          Color highlightBorderColor, double radius, Color borderColor) throws IOException {
        final Image img = generateImage(tileSizeIdx);
        final Graphics2D g = img.g;

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, img.width, img.height);

        if (baseTile != null) {
            final ByteArrayInputStream bais = new ByteArrayInputStream(baseTile);
            final BufferedImage image = ImageIO.read(bais);
            g.drawImage(image, 0, 0, null);
        }

        g.setComposite(AlphaComposite.Src);
        g.setStroke((new BasicStroke(((float) diameter / 8f))));

        final Path2D.Double triangle = new Path2D.Double();
        final Ellipse2D.Double shape = new Ellipse2D.Double(0, 0, diameter, diameter);

        for (final Dot dot : dots) {
            final double relX = (dot.x - box.x1) / box.res;
            final double relY = TILE_SIZES[tileSizeIdx] - (dot.y - box.y1) / box.res;

            if (dot.highlight) // triangle
            {
                triangle.reset();
                triangle.moveTo(relX, relY - triangleHeight / 3 * 2);
                triangle.lineTo(relX - triangleSide / 2, relY + triangleHeight / 3);
                triangle.lineTo(relX + triangleSide / 2, relY + triangleHeight / 3);
                triangle.closePath();
                if (!noFill) {
                    g.setPaint(dot.color);
                    g.fill(triangle);
                }
                g.setPaint(highlightBorderColor);
                g.draw(triangle);
            } else // circle
            {
                shape.x = relX - radius;
                shape.y = relY - radius;
                if (!noFill) {
                    g.setPaint(dot.color);
                    g.fill(shape);
                }
                g.setPaint(borderColor);
                g.draw(shape);
            }
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img.bi, "png", baos);
        final byte[] data = baos.toByteArray();
        return data;
    }

    @Getter
    @Setter
    public static class Dot {
        public Dot(final double x, final double y, final Color color, final boolean highlight) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.highlight = highlight;
        }

        final double x;
        final double y;
        final Color color;
        final boolean highlight;
    }
}
