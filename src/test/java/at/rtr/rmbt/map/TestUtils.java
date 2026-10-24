package at.rtr.rmbt.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.experimental.UtilityClass;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;


@UtilityClass
public class TestUtils {
    public static final JsonMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    public static String asJsonString(final Object obj) {
        return mapper.writeValueAsString(obj);
    }

}
