package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.MapServerConfiguration;
import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import at.rtr.rmbt.map.model.Settings;
import at.rtr.rmbt.map.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(SpringExtension.class)
@RequiredArgsConstructor
@SpringBootTest
@ContextConfiguration(classes = MapServerConfiguration.class)
@ActiveProfiles("test")
class ApplicationVersionServiceTest {

    private ApplicationVersionService applicationVersionService;

    @Mock
    private SettingsRepository settingsRepository;


    @BeforeEach
    public void setUp() {
        Mockito.when(settingsRepository.findFirstByKey(ApplicationVersionService.SYSTEM_UUID_KEY)).thenReturn(Optional.of(new Settings(1L,ApplicationVersionService.SYSTEM_UUID_KEY,null,"abcde")));
        applicationVersionService = new ApplicationVersionService(settingsRepository);

    }



    @Test
    void testGetApplicationVersion() {
        ApplicationVersionResponse applicationVersion = applicationVersionService.getApplicationVersion();
        assertEquals("abcde",applicationVersion.getSystemUUID());

    }
}