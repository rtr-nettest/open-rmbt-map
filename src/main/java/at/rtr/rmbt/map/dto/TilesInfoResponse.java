package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TilesInfoResponse {

    @JsonProperty("mapfilter")
    private MapFilter mapFilter;

    @Getter
    @Setter
    public static class MapFilter {

        @JsonProperty("mapTypes")
        private List<MapType> mapTypes;

        @JsonProperty("mapFilters")
        private Map<String, List<MapFilterOptions>> mapFilters;
    }

    @Getter
    @Setter
    public static class MapType {
        @JsonProperty("options")
        private List<MapOption> options;

        @JsonProperty("title")
        private String title;
    }

    @Getter
    @Setter
    public static class MapOption {

        @JsonProperty("map_options")
        private String mapOptions;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("title")
        private String title;

        @JsonProperty("unit")
        private String unit;

        @JsonProperty("heatmap_colors")
        private List<String> heatmapColors;

        @JsonProperty("heatmap_captions")
        private List<String> heatmapCaptions;

        @JsonProperty("classification")
        private List<String> classification;

        @JsonProperty("overlay_type")
        private String overlayType;
    }

    @Getter
    @Setter
    public static class MapFilterOptions {
        @JsonProperty("title")
        private String title;

        @JsonProperty("options")
        private List<MapFilterOption> filterOptions;
    }

    @Getter
    @Setter
    public static class MapFilterOption {
        @JsonProperty("default")
        private Boolean defaultOption;

        @JsonProperty("title")
        private String title;

        // Additional fields here as needed from JSON structure
        @JsonProperty("summary")
        private String summary;

        @JsonProperty("period")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Integer period;

        @JsonProperty("technology")
        private String technology;

        @JsonProperty("operator")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long operator;

        @JsonProperty("provider")
        private Long provider;

        @JsonProperty("device")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String device;

        @JsonProperty("statistical_method")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Double statisticalMethod;
    }
}
