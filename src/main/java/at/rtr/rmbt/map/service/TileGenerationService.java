package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.util.GeoCalc;
import at.rtr.rmbt.map.util.MapServerOptions;
import at.rtr.rmbt.map.util.TileParameters;
import at.rtr.rmbt.map.util.TileParameters.Path;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class TileGenerationService {
    protected static final int[] TILE_SIZES = new int[] { 256, 512, 768 };
    protected static final byte[][] EMPTY_IMAGES = new byte[TILE_SIZES.length][];
    protected final Image[] images = new Image[TILE_SIZES.length];

    protected static final byte[] EMPTY_MARKER = "EMPTY".getBytes();

    private static final int CACHE_STALE = 60*60;
    private static final int CACHE_EXPIRE = 24*60*60;

    @PostConstruct
    private void initializeStructures() {
        generateEmpty();
        generateImages();
    }

    private void generateEmpty() {
        for (int i = 0; i < TILE_SIZES.length; i++) {
            final BufferedImage img = new BufferedImage(TILE_SIZES[i], TILE_SIZES[i], BufferedImage.TYPE_INT_ARGB);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(img, "png", baos);
            } catch (final IOException e) {
                e.printStackTrace();
            }
            EMPTY_IMAGES[i] = baos.toByteArray();
        }
    }

    private void generateImages() {
        for (int i = 0; i < TILE_SIZES.length; i++)
        {
            final int tileSize = TILE_SIZES[i];
            final Image image = new Image();
            image.bi = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            image.g = image.bi.createGraphics();
            image.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            image.g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            image.g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            image.g.setStroke(new BasicStroke(1f));
            image.width = image.bi.getWidth();
            image.height = image.bi.getHeight();

            images[i] = image;
        }
    }


    public byte[] generateSingleTile(TilesRequest request) {
        final String zoomStr = request.getZoom();
        final String xStr = request.getX();
        final String yStr = request.getY();

        final Path path = new Path(zoomStr, xStr, yStr, ""); //"Path" parameter seems obsolete with Spring Boot
        final TileParameters parser = new TileParameters(path, request, 0.5f);

        byte[] tile = getTile(parser);
        return tile;
    }

    //get a specific tile, try getting from cache, otherwise from subclass
    private byte[] getTile(final TileParameters p)
    {
        boolean useCache = false;
        //if (p.isNoCache())
        //    useCache = false;

        final String cacheKey;

        /*if (useCache)
        {
            cacheKey = CacheHelper.getHash((TileParameters)p);
            final ObjectWithTimestamp cacheObject = cache.getWithTimestamp(cacheKey, CACHE_STALE);
            if (cacheObject != null)
            {
                System.out.println("cache hit for: " + cacheKey + "; is stale: " + cacheObject.stale);
                byte[] data = (byte[]) cacheObject.o;
                if (Arrays.equals(EMPTY_MARKER, data))
                    data = EMPTY_IMAGES[getTileSizeIdx(p)];
                if (cacheObject.stale)
                {
                    final Runnable refreshCacheRunnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            System.out.println("adding in background: " + cacheKey);
                            final byte[] newData = generateTile(p, getTileSizeIdx(p));
                            cache.set(cacheKey, CACHE_EXPIRE, newData != null ? newData : EMPTY_MARKER, true);
                        }
                    };
                    cache.getExecutor().execute(refreshCacheRunnable);
                }
                return data;
            }
            else {
                System.out.println("no cache hit for: " + cacheKey);
            }
        }
        else
            cacheKey = null;

         */

        final int tileSizeIdx = getTileSizeIdx(p);
        byte[] data = generateTile(p, tileSizeIdx);
//        if (data == null)

        /*if (useCache)
        {
            System.out.println("adding to cache: " + cacheKey);
            cache.set(cacheKey, CACHE_EXPIRE, data != null ? data : EMPTY_MARKER, true);
        }*/

        if (data == null)
            data = EMPTY_IMAGES[tileSizeIdx];
        return data;
    }

    private int getTileSizeIdx(final TileParameters p)
    {
        int tileSizeIdx = 0;
        final int size = p.getSize();
        for (int i = 0; i < TILE_SIZES.length; i++)
        {
            if (size == TILE_SIZES[i])
            {
                tileSizeIdx = i;
                break;
            }
        }
        return tileSizeIdx;
    }

    private byte[] generateTile(final TileParameters p, int tileSizeIdx)
    {
        final MapServerOptions.MapOption mo = MapServerOptions.getMapOptionMap().get(p.getMapOption());
        if (mo == null)
            throw new IllegalArgumentException();

        final List<MapServerOptions.SQLFilter> filters = new ArrayList<>(MapServerOptions.getDefaultMapFilters());
        for (final Map.Entry<String, String> entry : p.getFilterMap().entrySet())
        {
            final MapServerOptions.MapFilter mapFilter = MapServerOptions.getMapFilterMap().get(entry.getKey());
            if (mapFilter != null)
            {
                final MapServerOptions.SQLFilter filter = mapFilter.getFilter(entry.getValue());
                if (filter != null)
                    filters.add(filter);
            }
        }

        final Path path = p.getPath();
        final DBox box = GeoCalc.xyToMeters(TILE_SIZES[tileSizeIdx], path.getX(), path.getY(), path.getZoom());

        float quantile = p.getQuantile();
        if (mo.reverseScale)
            quantile = 1 - quantile;

        final byte[] data = generateTile(p, tileSizeIdx, path.getZoom(), box, mo, filters, quantile);
        return data;
    }

    protected abstract byte[] generateTile(TileParameters params, int tileSizeIdx, int zoom, DBox box, MapServerOptions.MapOption mo,
                                           List<MapServerOptions.SQLFilter> filters, float quantile);

    protected static class Image {
        protected BufferedImage bi;
        protected Graphics2D g;
        protected int width;
        protected int height;
    }

    static class DPoint {
        double x;
        double y;
    }

    @Getter
    @Setter
    public static class DBox {
        double x1;
        double y1;
        double x2;
        double y2;
        double res;
    }


}
