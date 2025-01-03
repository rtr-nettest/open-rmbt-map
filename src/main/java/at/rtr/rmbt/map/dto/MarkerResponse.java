package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

        @JsonProperty("time_string")
        String timeString;

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

