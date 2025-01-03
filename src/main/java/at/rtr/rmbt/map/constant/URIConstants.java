package at.rtr.rmbt.map.constant;

public interface URIConstants {
    String VERSION = "/version";

    //https://m-cloud.netztest.at/RMBTMapServer/tiles/points/12/2232/1428.png?null&statistical_method=0.5&period=180&map_options=mobile/download
    String TILES_POINT = "/tiles/points/{zoom}/{x}/{y}.png";
    String TILES_SHAPE = "/tiles/shapes/{zoom}/{x}/{y}.png";
    String TILES_HEATMAP = "/tiles/heatmap/{zoom}/{x}/{y}.png";

    String MARKERS = "/tiles/markers";
}