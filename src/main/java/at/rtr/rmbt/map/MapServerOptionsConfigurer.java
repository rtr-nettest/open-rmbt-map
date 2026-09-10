package at.rtr.rmbt.map;

import at.rtr.rmbt.map.util.MapServerOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Applies configuration-driven adjustments to the static {@link MapServerOptions#getMapOptionMap()} at
 * startup. Currently gates the {@code mobile/fences} and {@code mobile/technology} map options behind the
 * {@code app.mobile_fences} flag.
 */
@Component
@Slf4j
public class MapServerOptionsConfigurer {

    @Value("${app.mobile_fences:false}")
    private boolean mobileFencesEnabled;

    @PostConstruct
    public void configure() {
        MapServerOptions.setMobileFencesEnabled(mobileFencesEnabled);
        log.info("Map options {} and {} are {}", MapServerOptions.MOBILE_FENCES_OPTION,
                MapServerOptions.MOBILE_TECHNOLOGY_OPTION,
                mobileFencesEnabled ? "enabled" : "disabled");
    }
}
