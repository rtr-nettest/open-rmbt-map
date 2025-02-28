package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.MapServerConfiguration;
import at.rtr.rmbt.map.dto.CapabilitiesRequest;
import at.rtr.rmbt.map.dto.MarkerRequest;
import at.rtr.rmbt.map.dto.MarkerResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;

@ExtendWith(SpringExtension.class)
@RequiredArgsConstructor
@SpringBootTest
@ContextConfiguration(classes = MapServerConfiguration.class)
@ActiveProfiles("test")
class MarkerServiceTest {

    @Autowired
    private MarkerService markerService;

    @Test
    void testGetMarkersForPoint() throws SQLException {
        MarkerRequest mr = new MarkerRequest("de",
                null,
                new MarkerRequest.MarkerRequestCoordinates(1818747.1904024533, 6137926.386049775, 13, null, null, null),
                new MarkerRequest.MarkerRequestMapOptions("mobile/download"),
                new MarkerRequest.MarkerRequestFilter("160d5145-5ad2-4e0e-9c5a-b377e63d4eda",null, null, null, null, null),
                new CapabilitiesRequest(new CapabilitiesRequest.ClassificationRequest(4),null));

        MarkerResponse markersForPoint = markerService.getMarkersForPoint(mr);

        System.out.println(markersForPoint);
    }

    @Test
    void testGetSpecificMarker() throws SQLException {
        MarkerRequest mr = new MarkerRequest("de",
                "93f282f8-7047-49ef-985a-ef9898d5c474",
                new MarkerRequest.MarkerRequestCoordinates(null, null, null, null, null, null),
                new MarkerRequest.MarkerRequestMapOptions("mobile/download"),
                null,
                new CapabilitiesRequest(new CapabilitiesRequest.ClassificationRequest(4),null));

        MarkerResponse markersForPoint = markerService.getMarkersForPoint(mr);

        System.out.println(markersForPoint);
    }
}