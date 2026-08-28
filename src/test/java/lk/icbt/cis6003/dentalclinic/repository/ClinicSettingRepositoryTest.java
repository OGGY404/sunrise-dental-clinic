package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.ClinicSetting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for clinic-wide settings.
 *
 * The consultation fee lives here rather than in the Java code, so management
 * can change it without a developer rebuilding the program. The Singleton
 * configuration holder added in step 4 reads these rows.
 */
@DisplayName("ClinicSettingRepository")
class ClinicSettingRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ClinicSettingRepository clinicSettingRepository;

    private ClinicSetting setting(String key, String value) {
        ClinicSetting clinicSetting = new ClinicSetting();
        clinicSetting.setSettingKey(key);
        clinicSetting.setSettingValue(value);
        clinicSetting.setDescription("Set by a repository test");
        return clinicSetting;
    }

    @Test
    @DisplayName("reads a setting back by its key")
    void readsSettingByKey() {
        clinicSettingRepository.save(setting("consultation_fee", "1500.00"));

        assertThat(clinicSettingRepository.findBySettingKey("consultation_fee"))
                .isPresent()
                .get()
                .extracting(ClinicSetting::getSettingValue)
                .isEqualTo("1500.00");
    }

    @Test
    @DisplayName("returns empty for a key that was never set")
    void returnsEmptyForUnknownKey() {
        assertThat(clinicSettingRepository.findBySettingKey("no_such_key")).isEmpty();
    }

    @Test
    @DisplayName("saving the same key again updates it instead of adding a second row")
    void savingSameKeyUpdatesTheRow() {
        clinicSettingRepository.saveAndFlush(setting("consultation_fee", "1500.00"));
        clinicSettingRepository.saveAndFlush(setting("consultation_fee", "2000.00"));

        assertThat(clinicSettingRepository.count()).isEqualTo(1);
        assertThat(clinicSettingRepository.findBySettingKey("consultation_fee").orElseThrow()
                .getSettingValue()).isEqualTo("2000.00");
    }
}
