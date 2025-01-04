package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilitiesRequest {

    @JsonProperty(value = "classification")
    private final ClassificationRequest classification;


    @JsonProperty(value = "RMBThttp")
    @Schema(description = "True, if the client can handle the RMBThttp protocol", example = "true")
    private final Boolean rmbtHttp;

    @Getter
    @Builder
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class ClassificationRequest {

        @Schema(description = "Amount of classification items supported by client", example = "5")
        @JsonProperty(value = "count")
        private final Integer count;

    }
}
