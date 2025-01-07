package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.model.CachedTile;
import at.rtr.rmbt.map.util.GeoCalc;
import at.rtr.rmbt.map.util.MapServerOptions;
import at.rtr.rmbt.map.util.TileParameters;
import at.rtr.rmbt.map.util.TileParameters.Path;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.script.DigestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class TileGenerationService {
    protected static final int[] TILE_SIZES = new int[] { 256, 512, 768 };
    protected static final byte[][] EMPTY_IMAGES = new byte[TILE_SIZES.length][];
    protected final Image[] images = new Image[TILE_SIZES.length];

    protected static final byte[] EMPTY_MARKER = "EMPTY".getBytes();

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TaskExecutor executor;

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


    public byte[] generateSingleTile(TilesRequest request, Constants.TILE_TYPE tileType) {
        final Integer zoomStr = request.getZoom();
        final Integer xStr = request.getX();
        final Integer yStr = request.getY();

        final Path path = new Path(zoomStr, xStr, yStr, ""); //"Path" parameter seems obsolete with Spring Boot
        TileParameters parser = null;
        switch(tileType) {
            case POINT -> {
                 parser = new TileParameters.PointTileParameters(path, request);
            }
            case HEATMAP -> {
                 parser = new TileParameters(path, request, 0.5f);
            }
            case SHAPE -> {
                 parser = new TileParameters(path, request, 0.5f);
            }
        }

        byte[] tile = getTile(parser);
        return tile;
    }

    //get a specific tile, try getting from cache, otherwise from subclass
    public byte[] getTile(final TileParameters p)
    {
        boolean useCache = true;
        if (p.isNoCache()) {
            useCache = false;
        }

        String cacheKeyStr = p.toString();
        final String cacheKey = DigestUtils.sha1DigestAsHex(cacheKeyStr);;
        Cache cache = cacheManager.getCache(Constants.TILE_CACHE);
        if (useCache && cache != null)
        {

            CachedTile cacheObject = cache.get(cacheKey, CachedTile.class);
            if (cacheObject != null)
            {
                boolean isStale = Instant.now().minus(Constants.TILE_CACHE_STALE, ChronoUnit.SECONDS).isAfter(cacheObject.getCreationTime());
                log.debug("cache hit for: " + cacheKey + "; is stale: " + isStale);
                byte[] data = (byte[]) cacheObject.getTileContent();
                if (Arrays.equals(EMPTY_MARKER, data))
                    data = EMPTY_IMAGES[getTileSizeIdx(p)];
                if (isStale)
                {
                    final Runnable refreshCacheRunnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            log.debug("adding in background: " + cacheKey);
                            final int tileSizeIdx = getTileSizeIdx(p);
                            byte[] data = generateTile(p, tileSizeIdx);
                            CachedTile ct = new CachedTile();
                            ct.setCreationTime(Instant.now());
                            ct.setTileContent(data);
                            cache.put(cacheKey, ct);
                        }
                    };
                    executor.execute(refreshCacheRunnable);

                }
                return data;
            }
            else {
                log.info("no cache hit for: " + cacheKey);
            }
        }

        final int tileSizeIdx = getTileSizeIdx(p);
        byte[] data = generateTile(p, tileSizeIdx);

        if (useCache && cache != null)
        {
            log.info("adding to cache: " + cacheKey);
            CachedTile ct = new CachedTile();
            ct.setCreationTime(Instant.now());
            ct.setTileContent(data != null ? data : EMPTY_MARKER);
            cache.put(cacheKey, ct);
        }

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
        if (mo == null) {
            throw new IllegalArgumentException();
        }

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

    protected abstract TileParameters getTileParameters(TileParameters.Path path, TilesRequest request);

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
