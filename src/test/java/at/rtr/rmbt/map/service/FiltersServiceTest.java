package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.MapServerConfiguration;
import at.rtr.rmbt.map.dto.MapFiltersResponse;
import at.rtr.rmbt.map.dto.TilesInfoRequest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@RequiredArgsConstructor
@SpringBootTest
@ContextConfiguration(classes = MapServerConfiguration.class)
@ActiveProfiles("test")
class FiltersServiceTest {

    @Autowired
    private FiltersService filtersService;

    @Test
    void getMapFilters() {
        TilesInfoRequest request = new TilesInfoRequest();
        request.setLanguage("de");

        MapFiltersResponse response = filtersService.getMapFilters(request);

        assertNotNull(response);
    }
}