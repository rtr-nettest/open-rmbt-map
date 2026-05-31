package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.CapabilitiesRequest;
import at.rtr.rmbt.map.dto.MarkerRequest;
import at.rtr.rmbt.map.dto.MarkerResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkerServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
    @InjectMocks
    private MarkerService markerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(markerService, "supportedLanguagesAsArray", "en,de");
        ReflectionTestUtils.setField(markerService, "defaultLanguage", "en");
        when(entityManager.createNativeQuery(anyString(), eq("MarkerResultMapping"))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
    }

    @Test
    void testGetMarkersForPoint() {
        MarkerRequest mr = new MarkerRequest("de",
                null,
                new MarkerRequest.MarkerRequestCoordinates(1818747.1904024533, 6137926.386049775, 13, null, null, null),
                new MarkerRequest.MarkerRequestMapOptions("mobile/download"),
                new MarkerRequest.MarkerRequestFilter("160d5145-5ad2-4e0e-9c5a-b377e63d4eda", null, null, null, null, null),
                new CapabilitiesRequest(new CapabilitiesRequest.ClassificationRequest(4), null));

        MarkerResponse response = markerService.getMarkersForPoint(mr);

        assertNotNull(response);
        assertTrue(response.getMeasurements().isEmpty());
    }

    @Test
    void testGetSpecificMarker() {
        MarkerRequest mr = new MarkerRequest("de",
                "93f282f8-7047-49ef-985a-ef9898d5c474",
                new MarkerRequest.MarkerRequestCoordinates(null, null, null, null, null, null),
                new MarkerRequest.MarkerRequestMapOptions("mobile/download"),
                null,
                new CapabilitiesRequest(new CapabilitiesRequest.ClassificationRequest(4), null));

        MarkerResponse response = markerService.getMarkersForPoint(mr);

        assertNotNull(response);
        assertTrue(response.getMeasurements().isEmpty());
    }
}
