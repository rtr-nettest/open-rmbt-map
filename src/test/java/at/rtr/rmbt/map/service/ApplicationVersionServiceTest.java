package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.MapServerConfiguration;
import at.rtr.rmbt.map.response.ApplicationVersionResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@RequiredArgsConstructor
@SpringBootTest
@ContextConfiguration(classes = MapServerConfiguration.class)
@ActiveProfiles("test")
class ApplicationVersionServiceTest {

    @Autowired
    private ApplicationVersionService applicationVersionService;


    @BeforeEach
    public void setUp() {
        //applicationVersionService = new ApplicationVersionService(settingsRepository);

    }



    @Test
    void testGetApplicationVersion() {
        ApplicationVersionResponse applicationVersion = applicationVersionService.getApplicationVersion();
        System.out.println(applicationVersion);
    }
}