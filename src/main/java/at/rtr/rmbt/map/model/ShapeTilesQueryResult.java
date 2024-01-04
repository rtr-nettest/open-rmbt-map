package at.rtr.rmbt.map.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.postgis.jdbc.geometry.Geometry;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SqlResultSetMapping(name = "ShapeTilesQueryResultMapping", classes = {
        @ConstructorResult(targetClass = ShapeTilesQueryResult.class,
                columns = {
                        @ColumnResult(name = "geom", type = String.class),
                        @ColumnResult(name = "count", type = Integer.class),
                        @ColumnResult(name = "val", type = Double.class),
                }
        )})
@Entity
public class ShapeTilesQueryResult {
    String geom;
    Integer count;
    Double val;

    @Id
    private Long id;
}
