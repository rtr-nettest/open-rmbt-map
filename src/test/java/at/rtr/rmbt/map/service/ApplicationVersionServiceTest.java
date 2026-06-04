package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import at.rtr.rmbt.map.model.Settings;
import at.rtr.rmbt.map.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationVersionServiceTest {

    @Mock
    private SettingsRepository settingsRepository;

    private ApplicationVersionService applicationVersionService;

    @BeforeEach
    void setUp() {
        when(settingsRepository.findFirstByKey(ApplicationVersionService.SYSTEM_UUID_KEY))
                .thenReturn(Optional.of(new Settings(1L, ApplicationVersionService.SYSTEM_UUID_KEY, null, "abcde")));
        applicationVersionService = new ApplicationVersionService(settingsRepository);
    }

    @Test
    void testGetApplicationVersion() {
        ApplicationVersionResponse applicationVersion = applicationVersionService.getApplicationVersion();
        assertEquals("abcde", applicationVersion.getSystemUUID());
    }
}
