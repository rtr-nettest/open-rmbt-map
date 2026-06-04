package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.dto.TilesRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatmapTileServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private TaskExecutor executor;
    @InjectMocks
    private HeatmapTileService heatmapTileService;

    @BeforeEach
    void setUp() {
        // @PostConstruct hooks are not run without a Spring context; invoke them manually.
        ReflectionTestUtils.invokeMethod(heatmapTileService, "initializeStructures");
        ReflectionTestUtils.invokeMethod(heatmapTileService, "initialize");
    }

    @Test
    void testGenerateHeatmapTile() {
        when(entityManager.createNativeQuery(anyString(), eq("TilesQueryResultMapping"))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        TilesRequest tr = new TilesRequest(7, 70, 44, 0.5f, null, "mobile/download",
                null, null, null, null, null, null, null, null, null, null, null, null);

        byte[] bytes = heatmapTileService.generateSingleTile(tr, Constants.TILE_TYPE.HEATMAP);

        // No data -> empty tile: the service returns a blank PNG rather than null.
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}
