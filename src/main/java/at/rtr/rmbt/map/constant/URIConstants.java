package at.rtr.rmbt.map.constant;

public interface URIConstants {
    String VERSION = "/version";

    //https://m-cloud.netztest.at/RMBTMapServer/tiles/points/12/2232/1428.png?null&statistical_method=0.5&period=180&map_options=mobile/download
    String TILES_ENDPOINT_VARS = "/tiles/{type}/{zoom}/{x}/{y}.png";
    String TILES_ENDPOINT = "/tiles/{type}";
    public static enum TILE_TYPE {points, shapes, heatmap}

    String MARKERS = "/tiles/markers";
    String TILES_INFORMATION = "/tiles/info";
    String TILES_INFORMATION_V2 = "/v2/tiles/info";
}