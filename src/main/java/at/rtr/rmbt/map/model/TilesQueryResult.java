package at.rtr.rmbt.map.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SqlResultSetMapping(name = "TilesQueryResultMapping",  classes = {
        @ConstructorResult(targetClass = TilesQueryResult.class,
                columns = {
                        @ColumnResult(name = "count", type = Integer.class),
                        @ColumnResult(name = "val", type = Double.class),
                        @ColumnResult(name = "gx", type = Double.class),
                        @ColumnResult(name = "gy", type = Double.class)
                }
        )})
@Entity
public class TilesQueryResult {
    Integer count;
    Double val;
    Double gx;
    Double gy;


    @Id
    private Long id;
}
