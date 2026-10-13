package at.rtr.rmbt.map.repository;

import at.rtr.rmbt.map.model.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read only through SettingsCache. */
public interface SettingsRepository extends JpaRepository<Settings, Long> {
}
