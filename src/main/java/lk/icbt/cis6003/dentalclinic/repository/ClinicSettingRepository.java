package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.ClinicSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for clinic-wide settings.
 * DESIGN PATTERN: Repository.
 *
 * Read by the Singleton configuration holder added in step 4, so the
 * consultation fee is loaded once instead of on every bill.
 */
@Repository
public interface ClinicSettingRepository extends JpaRepository<ClinicSetting, String> {

    Optional<ClinicSetting> findBySettingKey(String settingKey);
}
