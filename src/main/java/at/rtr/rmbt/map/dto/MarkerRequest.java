package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkerRequest {
    String language;

    @JsonProperty("open_test_uuid")
    String openTestUuid;

    @JsonProperty("coords")
    MarkerRequestCoordinates coordinates;

    @JsonProperty("options")
    MarkerRequestMapOptions options;

    @JsonProperty("filter")
    MarkerRequestFilter filter;

    @JsonProperty("capabilities")
    private final CapabilitiesRequest capabilities;

    @Setter
    @Getter
    @ToString
    @AllArgsConstructor
    public static class MarkerRequestCoordinates {
        @JsonProperty
        Double x;
        @JsonProperty
        Double y;

        @JsonProperty("z")
        Integer zoom;

        @JsonProperty("lat")
        Double latitude;

        @JsonProperty("long")
        Double longitude;

        @JsonProperty
        Integer size;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MarkerRequestMapOptions {
        @JsonProperty("map_options")
        String options;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MarkerRequestFilter {
        @JsonProperty("highlight")
        String highlightUuid;

        @JsonProperty("four_color")
        Boolean fourColorSupported;

        //@TODO: operator, provider, technology, period, age, user_server_selection
    }
}
