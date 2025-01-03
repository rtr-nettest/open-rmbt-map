package at.rtr.rmbt.map.util;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.dto.TilesRequest;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TileParameters {
    protected static final Pattern PATH_PATTERN = Pattern.compile("(\\d+)/(\\d+)/(\\d+)");
    protected static final int MAX_ZOOM = 21;

    protected final Path path;
    protected final int size;
    protected final String mapOption;
    protected final float quantile;
    protected final Map<String,String> filterMap;
    protected final double transparency;
    protected final String developerCode;

    public final static class Path {
        protected final int zoom, x, y;

        public Path(Integer zoomStr, Integer xStr, Integer yStr, String path) {
            if (zoomStr != null && xStr != null && yStr != null) {
                zoom = zoomStr;
                x = xStr;
                y = yStr;
            } else {
                if (path == null)
                    throw new IllegalArgumentException();
                final Matcher m = PATH_PATTERN.matcher(path);
                if (!m.matches())
                    throw new IllegalArgumentException();
                zoom = Integer.valueOf(m.group(1));
                x = Integer.valueOf(m.group(2));
                y = Integer.valueOf(m.group(3));
            }
            if (zoom < 0 || zoom > MAX_ZOOM)
                throw new IllegalArgumentException();
            if (x < 0 || y < 0)
                throw new IllegalArgumentException();
            int pow = 1 << zoom;
            if (x >= pow || y >= pow)
                throw new IllegalArgumentException();
        }

        public Path(Integer zoom, Integer x, Integer y) {
            this.zoom = zoom;
            this.x = x;
            this.y = y;
        }

        public int getZoom() {
            return zoom;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        /*@Override
        public void funnel(Path o, PrimitiveSink into) {
            into
                    .putInt(o.zoom)
                    .putInt(o.x)
                    .putInt(o.y);
        }}*/
    }

    public TileParameters(Path path, TilesRequest params, double defaultTransparency) {
        this.path = path;

        int _size = 0;
        if (params.getSize() != null) {
            try {
                _size = params.getSize();
            } catch (NumberFormatException e) {
            }
        }
        size = _size;

        mapOption = params.getMapOptions();

        float _quantile = 0.5f; //median is default quantile
        if (params.getStatisticalMethod() != null) {
            try {
                float __quantile = params.getStatisticalMethod();
                if (__quantile >= 0f && __quantile <= 1f)
                    _quantile = __quantile;
            } catch (NumberFormatException e) {
            }
        }
        quantile = _quantile;

        //developer code parameter
        developerCode = params.getDeveloperCode();

        double _transparency = defaultTransparency;
        if (params.getTransparency() != null) {
            try {
                _transparency = params.getTransparency();
            } catch (final NumberFormatException e) {
            }
        }
        if (_transparency < 0) {
            _transparency = 0;
        }
        if (_transparency > 1) {
            _transparency = 1;
        }
        transparency = _transparency;

        final TreeMap<String, String> _filterMap = new TreeMap<>();

        if (params.getOperator() != null) {
            _filterMap.put("operator", params.getOperator());
        }
        if (params.getProvider() != null) {
            _filterMap.put("provider", params.getProvider());
        }
        if (params.getTechnology() != null) {
            _filterMap.put("technology", params.getProvider());
        }
        if (params.getPeriod() != null) {
            _filterMap.put("period", params.getPeriod().toString());
        }
        if (params.getAge() != null) {
            _filterMap.put("age", params.getAge().toString());
        }
        if (params.getUserServerSelection() != null) {
            _filterMap.put("user_server_selection", params.getUserServerSelection());
        }

        filterMap = Collections.unmodifiableMap(_filterMap);
    }

    public Path getPath() {
        return path;
    }

    public int getSize() {
        return size;
    }

    public String getMapOption() {
        return mapOption;
    }

    public float getQuantile() {
        return quantile;
    }

    public Map<String, String> getFilterMap() {
        return filterMap;
    }

    public double getTransparency() {
        return transparency;
    }

    public boolean isNoCache()  {
        return false;
    };



    public static class PointTileParameters extends TileParameters
    {
        protected final double pointDiameter;
        protected final boolean noFill;
        protected final boolean noColor;
        protected final UUID highlight;
        protected final PointTileParameters genericParameters; // same without highlight for caching

        public PointTileParameters(Path path, TilesRequest params)
        {
            this(path, params, false);
        }

        protected PointTileParameters(Path path, TilesRequest params, boolean generic)
        {
            super(path, params, Constants.POINT_DEFAULT_TRANSPARENCY);

            double _diameter = 8.0;
            if (params.getPointDiameter() != null) {
                _diameter = params.getPointDiameter();
            }
            pointDiameter = _diameter;

            boolean _noFill = false;
            if (params.getNoFill() != null) {
                _noFill = params.getNoFill();
            }
            noFill = _noFill;

            boolean _noColor = false;
            if (params.getNoColor() != null) {
                _noColor = params.getNoColor();
            }
            noColor = _noColor;

            if (generic)
                highlight = null;
            else {

                UUID hightlightUUID = null;
                if (params.getHighlight() != null) {
                    hightlightUUID = params.getHighlight();
                }
                highlight = hightlightUUID;
            }

            if (highlight == null)
                genericParameters = null;
            else
                genericParameters = new PointTileParameters(path, params, true);
        }

        public double getPointDiameter()
        {
            return pointDiameter;
        }

        public boolean isNoFill()
        {
            return noFill;
        }

        public boolean isNoColor()
        {
            return noColor;
        }

        public UUID getHighlight()
        {
            return highlight;
        }

        @Override
        public boolean isNoCache()
        {
            return highlight != null;
        }

        public PointTileParameters getGenericParameters()
        {
            return genericParameters;
        }


    }

    public static class ShapeTileParameters extends TileParameters
    {
        public ShapeTileParameters(Path path, TilesRequest params)
        {
            super(path, params, Constants.SHAPE_DEFAULT_TRANSPARENCY);
        }

        @Override
        public boolean isNoCache()
        {
            return false;
        }
    }


}
