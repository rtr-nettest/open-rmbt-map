package at.rtr.rmbt.map.constant;

import java.util.List;

public interface Constants {
    String VERSION_TEMPLATE = "%s_%s";

    String VALUE_AND_UNIT_TEMPLATE = "%s %s";
    Double BYTES_UNIT_CONVERSION_MULTIPLICATOR = 1000d;
    Double PING_CONVERSION_MULTIPLICATOR = 1000000d;
    Integer SIGNIFICANT_PLACES = 2;
    public enum TILE_TYPE {POINT, HEATMAP, SHAPE}

    Float SHAPE_DEFAULT_TRANSPARENCY = 0.4f;
    Float POINT_DEFAULT_TRANSPARENCY = 0.6f;
    Float HEATMAP_DEFAULT_TRANSPARENCY = 0.75f;
}
