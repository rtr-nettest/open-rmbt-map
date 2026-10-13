package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import at.rtr.rmbt.map.model.Settings;
import at.rtr.rmbt.map.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationVersionServiceTest {

    @Mock
    private SettingsRepository settingsRepository;

    @Mock
    private ObjectProvider<RedisConnectionFactory> redisConnectionFactory;

    private ApplicationVersionService applicationVersionService;

    @BeforeEach
    void setUp() {
        when(settingsRepository.findAll())
                .thenReturn(List.of(new Settings(1L, ApplicationVersionService.SYSTEM_UUID_KEY, null, "abcde")));
        SettingsCache settingsCache = new SettingsCache(settingsRepository);
        settingsCache.refresh();
        applicationVersionService = new ApplicationVersionService(settingsCache, redisConnectionFactory);
    }

    @Test
    void testGetApplicationVersion() {
        ApplicationVersionResponse applicationVersion = applicationVersionService.getApplicationVersion();
        assertEquals("abcde", applicationVersion.getSystemUUID());
        assertEquals("none", applicationVersion.getCache());
    }
}
