package at.rtr.rmbt.map.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SqlResultSetMapping(name = "TilesInfoOperatorResults", classes = {
        @ConstructorResult(targetClass = TilesInfoOperatorResults.class,
                columns = {
                        @ColumnResult(name = "name"),
                        @ColumnResult(name = "shortname"),
                        @ColumnResult(name = "mccMnc", type = String.class),
                        @ColumnResult(name = "uid", type = Long.class)
                }
        )})
@Entity
public class TilesInfoOperatorResults {

    //uid,name,mcc_mnc,shortname
    @Id //needed for @Entity annotation so that sql mapping is processed
    private Long id;

    Long uid;
    String name;
    String shortname;
    String mccMnc;

}

