package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.TilesInfoRequest;
import at.rtr.rmbt.map.dto.TilesInfoResponse;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfoServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
    @InjectMocks
    private InfoService infoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(infoService, "supportedLanguagesAsArray", "en,de");
        ReflectionTestUtils.setField(infoService, "defaultLanguage", "en");
    }

    @Test
    void getTilesInfo() {
        when(entityManager.createNativeQuery(anyString(), eq("TilesInfoOperatorResults"))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        TilesInfoRequest request = new TilesInfoRequest();
        request.setLanguage("de");

        TilesInfoResponse response = infoService.getTilesInfo(request);

        assertNotNull(response);
    }
}
