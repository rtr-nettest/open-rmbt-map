package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class TilesRequest {
    @JsonProperty("zoom")
    private String zoom;

    @JsonProperty("x")
    private String x;

    @JsonProperty("y")
    private String y;

    @JsonProperty("statistical_method")
    Float statisticalMethod;

    @JsonProperty("size")
    Integer size;

    @JsonProperty("map_options")
    String mapOptions = "mobile/download";

    @JsonProperty("developerCode")
    String DeveloperCode;

    @JsonProperty("transparency")
    Double transparency;
}
