package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.response.ApplicationVersionResponse;
import at.rtr.rmbt.map.constant.Constants;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationVersionService {

    @Value("${git.branch}")
    private String branch;

    @Value("${git.commit.id.describe}")
    private String describe;

    @Value("${application-version.host}")
    private String applicationHost;


    public ApplicationVersionResponse getApplicationVersion() {
        return ApplicationVersionResponse.builder()
                .version(String.format(Constants.VERSION_TEMPLATE, branch, describe))
                .systemUUID(getSystemUUID())
                .host(applicationHost)
                .build();
    }

    private String getSystemUUID() {
        /*return settingsRepository.findFirstByKeyAndLangIsNullOrKeyAndLangOrderByLang(AdminSettingConfig.SYSTEM_UUID_KEY, AdminSettingConfig.SYSTEM_UUID_KEY, StringUtils.EMPTY)
                .map(Settings::getValue)
                .orElse(null);*/
        return "test";
    }
}
