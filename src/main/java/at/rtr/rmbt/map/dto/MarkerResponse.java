package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class MarkerResponse {

    List<SingleMarker> measurements = new ArrayList<>();


    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class SingleMarker {
        Boolean highlight;

        @JsonProperty("lat")
        Double latitude;

        @JsonProperty("lon")
        Double longitude;

        @JsonProperty("open_test_uuid")
        String openTestUuid;

        /**
         * @deprecated formatted in the time zone of the measuring client, not of the viewer;
         * clients should format {@link #time} themselves. Still sent for existing clients.
         */
        @Deprecated
        @Schema(deprecated = true, description = "Deprecated: measurement time formatted in the time zone of the measuring client "
                + "(UTC if unknown), not of the viewer. Format 'time' on the client instead.",
                example = "Sep 26, 2026, 10:49:21 AM")
        @JsonProperty("time_string")
        String timeString;

        @Schema(description = "Measurement time as Unix epoch in milliseconds (for fences: time of the fence)",
                example = "1790412561137")
        @JsonProperty
        Long time;

        @JsonProperty
        List<SingleMarkerMetricItem> measurement = new ArrayList();

        @JsonProperty("measurement_result")
        SingleMarkerMeasurementResult result;

        @JsonProperty("net")
        List<SingleMarkerMetricItem> network = new ArrayList<>();

        @JsonProperty("network_info")
        NetworkInfo networkInfo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SingleMarkerMetricItem {
        @JsonProperty
        String title;

        @JsonProperty
        String value;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty
        Integer classification;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SingleMarkerMeasurementResult {
        @JsonProperty("download_kbit")
        Integer downloadKbps;

        @JsonProperty("download_classification")
        Integer downloadClassification;

        @JsonProperty("upload_kbit")
        Integer uploadKbps;

        @JsonProperty("upload_classification")
        Integer uploadClassification;

        @JsonProperty("ping_ms")
        Double pingMs;

        @JsonProperty("ping_classification")
        Integer pingClassification;

        @JsonProperty("signal_strength")
        Integer signalStrength;

        @JsonProperty("signal_classification")
        Integer signalClassification;

        @JsonProperty("lte_rsrp")
        Integer lteRsrp;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class NetworkInfo {
        @JsonProperty("network_type_label")
        String networkTypeLabel;

        @JsonProperty("provider_name")
        String providerName;

        @JsonProperty("wifi_ssid")
        String wifiSSID;

        @JsonProperty("roaming_type_label")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String roamingTypeLabel;
    }
}

