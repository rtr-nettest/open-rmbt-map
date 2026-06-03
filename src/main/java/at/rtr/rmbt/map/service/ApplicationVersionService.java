package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.model.Settings;
import at.rtr.rmbt.map.repository.SettingsRepository;
import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationVersionService {
    public final static String SYSTEM_UUID_KEY = "system_UUID";

    @Value("${git.branch}")
    private String branch;

    @Value("${git.commit.id.describe}")
    private String describe;

    @Value("${git.build.time}")
    private String buildTime;

    @Value("${application-version.host}")
    private String applicationHost;


    private final SettingsRepository settingsRepository;

    public ApplicationVersionResponse getApplicationVersion() {
        return ApplicationVersionResponse.builder()
                .version(formatVersion(describe, branch, buildTime))
                .systemUUID(getSystemUUID())
                .host(applicationHost)
                .build();
    }

    /**
     * Builds the harmonised version string {@code <describe>(<branch>) <buildTime>}, e.g.
     * {@code v1.0.0-25-ga8ae45c(master) 2026-06-02T18:35:58Z}. With the git plugin configured for a
     * "long" describe, this always carries the latest tag, the commit count since that tag, and the
     * short commit hash - even on a clean tag build.
     *
     * @param describe  the git describe (tag-N-gHASH)
     * @param branch    the git branch
     * @param buildTime the build timestamp
     * @return the formatted version string
     */
    public static String formatVersion(String describe, String branch, String buildTime) {
        return describe + "(" + formatBranch(branch) + ") " + buildTime;
    }

    private static String formatBranch(String branch) {
        // In a detached HEAD checkout (e.g. CI building a tag) git.branch is the full commit hash;
        // show "HEAD" instead of a 40-char hash so the branch field stays meaningful.
        if (branch == null || branch.isBlank() || branch.matches("[0-9a-f]{7,40}")) {
            return "HEAD";
        }
        return branch;
    }

    private String getSystemUUID() {
        return settingsRepository.findFirstByKey(SYSTEM_UUID_KEY)
                .map(Settings::getValue)
                .orElse(null);
    }
}
