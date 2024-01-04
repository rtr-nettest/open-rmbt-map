package at.rtr.rmbt.map.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SqlResultSetMapping(name = "MarkerResultMapping",  classes = {
        @ConstructorResult(targetClass = MarkerQueryResult.class,
                columns = {
                @ColumnResult(name = "lat", type = Double.class),
                @ColumnResult(name = "lon", type = Double.class),
                        @ColumnResult(name = "x", type = Double.class),
                        @ColumnResult(name = "y", type = Double.class),
                        @ColumnResult(name = "time", type = Timestamp.class),
                        @ColumnResult(name = "timezone"),
                        @ColumnResult(name = "uuid"),
                        @ColumnResult(name = "openTestUuid"),
                        @ColumnResult(name = "speedDownload", type = Integer.class),
                        @ColumnResult(name = "speedUpload", type = Integer.class),
                        @ColumnResult(name = "pingMedian"),
                        @ColumnResult(name = "networkType"),
                        @ColumnResult(name = "signalStrength"),
                        @ColumnResult(name = "lteRsrp"),
                        @ColumnResult(name = "providerName"),
                        @ColumnResult(name = "wifiSSID"),
                        @ColumnResult(name = "networkOperator"),
                        @ColumnResult(name = "mobileNetworkName"),
                        @ColumnResult(name = "networkSimOperator"),
                        @ColumnResult(name = "mobileSimName"),
                        @ColumnResult(name = "roamingType")
                }
        )})
@Entity
public class MarkerQueryResult {
    Double lat;
    Double lon;
    Double x;
    Double y;
    Timestamp time;
    String timezone;
    UUID uuid;
    UUID openTestUuid;

    Integer speedDownload;
    Integer speedUpload;
    Long pingMedian;

    Integer networkType;
    Integer signalStrength;

    Integer lteRsrp;

    String providerName;
    String wifiSSID;

    String networkOperator;
    String mobileNetworkName;
    String networkSimOperator;
    String mobileSimName;
    Integer roamingType;

    @Id //needed for @Entity annotation so that sql mapping is processed
    private Long id;
}
