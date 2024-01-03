package at.rtr.rmbt.map.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "settings")
@EqualsAndHashCode
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id;

    @Column(name = "key")
    private String key;

    @Column(name = "lang")
    private String lang;

    @Column(name = "value")
    private String value;
}
