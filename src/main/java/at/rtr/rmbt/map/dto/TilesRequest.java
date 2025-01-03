package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Getter
@Setter
@ToString
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TilesRequest {
    @JsonProperty("zoom")
    private Integer zoom;

    @JsonProperty("x")
    private Integer x;

    @JsonProperty("y")
    private Integer y;

    @JsonProperty("statistical_method")
    Float statisticalMethod;

    @JsonProperty("size")
    Integer size;

    @JsonProperty("map_options")
    String mapOptions = "mobile/download";

    @JsonProperty("developerCode")
    String developerCode;

    @JsonProperty("transparency")
    Double transparency;

    @JsonProperty("point_diameter")
    Double pointDiameter;

    @JsonProperty("no_fill")
    Boolean noFill;

    @JsonProperty("no_color")
    Boolean noColor;

    @JsonProperty("highlight")
    UUID highlight;

    @JsonProperty("period")
    Integer period;
}
