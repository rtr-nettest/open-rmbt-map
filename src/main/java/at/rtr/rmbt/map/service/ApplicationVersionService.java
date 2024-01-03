package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.model.Settings;
import at.rtr.rmbt.map.repository.SettingsRepository;
import at.rtr.rmbt.map.response.ApplicationVersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationVersionService {
    final String SYSTEM_UUID_KEY = "system_UUID";

    @Value("${git.branch}")
    private String branch;

    @Value("${git.commit.id.describe}")
    private String describe;

    @Value("${application-version.host}")
    private String applicationHost;


    private final SettingsRepository settingsRepository;

    public ApplicationVersionResponse getApplicationVersion() {
        return ApplicationVersionResponse.builder()
                .version(String.format(Constants.VERSION_TEMPLATE, branch, describe))
                .systemUUID(getSystemUUID())
                .host(applicationHost)
                .build();
    }

    private String getSystemUUID() {
        return settingsRepository.findFirstByKey(SYSTEM_UUID_KEY)
                .map(Settings::getValue)
                .orElse(null);
    }
}
