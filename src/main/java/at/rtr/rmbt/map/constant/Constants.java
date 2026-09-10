package at.rtr.rmbt.map.constant;

import java.util.List;

public interface Constants {
    String VERSION_TEMPLATE = "%s_%s";

    String VALUE_AND_UNIT_TEMPLATE = "%s %s";
    Double BYTES_UNIT_CONVERSION_MULTIPLICATOR = 1000d;
    Double PING_CONVERSION_MULTIPLICATOR = 1000000d;
    Integer SIGNIFICANT_PLACES = 2;
    public enum TILE_TYPE {POINT, HEATMAP, SHAPE}
    Integer TECHNOLOGY_OFFLINE = 1000;
    // signal value of the technology layer: clamped to max signal, i.e. always full color
    Integer TECHNOLOGY_ONLY_SIGNAL = -85;
    int COLOR_OFFLINE_RGB = 0x808080;
    int COLOR_2G_RGB = 0xFFDE00;
    int COLOR_3G_RGB = 0xEFFF00;
    int COLOR_4G_RGB = 0x00DEFF;
    int COLOR_5G_RGB = 0x0091FF;
    int COLOR_5G_SA_RGB = 0x5E00FF;

    Float SHAPE_DEFAULT_TRANSPARENCY = 0.4f;
    Float POINT_DEFAULT_TRANSPARENCY = 0.6f;
    Float HEATMAP_DEFAULT_TRANSPARENCY = 0.75f;

    String TILE_CACHE = "tile_cache";
    int TILE_CACHE_STALE = 60*60;
    int TILE_CACHE_EXPIRE = 24*60*60;

    int TILE_SHORT_CACHE_EXPIRE = 5*60;


    final static String[] OPTION_TECHNOLOGY_TITLE = new String[] {
            "ANY", "3G_4G_5G", "4G_5G", "3G_4G", "2G", "3G", "4G", "5G"};

    final static String[] OPTION_TECHNOLOGY_VALUE = new String[] {
            "", "345", "45", "34", "2", "3", "4", "5"};
}
