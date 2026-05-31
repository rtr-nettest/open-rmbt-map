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
class ShapeTileServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private TaskExecutor executor;
    @InjectMocks
    private ShapeTileService shapeTileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.invokeMethod(shapeTileService, "initializeStructures");
    }

    @Test
    void generateTile() {
        when(entityManager.createNativeQuery(anyString(), eq("ShapeTilesQueryResultMapping"))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        TilesRequest tr = new TilesRequest(7, 70, 44, 0.5f, null, "mobile/download",
                null, null, null, null, null, null, null, null, null, null, null, null);

        byte[] bytes = shapeTileService.generateSingleTile(tr, Constants.TILE_TYPE.SHAPE);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}
