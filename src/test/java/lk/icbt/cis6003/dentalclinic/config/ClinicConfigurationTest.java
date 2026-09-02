package lk.icbt.cis6003.dentalclinic.config;

import lk.icbt.cis6003.dentalclinic.model.ClinicSetting;
import lk.icbt.cis6003.dentalclinic.repository.ClinicSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the clinic settings holder.
 *
 * DESIGN PATTERN UNDER TEST: Singleton.
 *
 * The consultation fee is needed on every bill. Reading it from the database
 * each time would be a wasted query on every single receipt, so it is read once
 * and kept. These tests prove two things: the values are read only once, and a
 * missing or damaged setting falls back to a safe default instead of crashing
 * the billing screen.
 *
 * Mockito is used so the test never needs a real database.
 */
@DisplayName("ClinicConfiguration")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClinicConfigurationTest {

    @Mock
    private ClinicSettingRepository settingRepository;

    private ClinicConfiguration configuration;

    private ClinicSetting setting(String key, String value) {
        return new ClinicSetting(key, value, "test");
    }

    @BeforeEach
    void setUp() {
        when(settingRepository.findAll()).thenReturn(List.of(
                setting("clinic_name", "Sunrise Dental Clinic"),
                setting("consultation_fee", "1500.00"),
                setting("opening_time", "08:00:00"),
                setting("closing_time", "18:00:00"),
                setting("appointment_slot_minutes", "30"),
                setting("currency", "LKR")
        ));
        configuration = new ClinicConfiguration(settingRepository);
        configuration.load();
    }

    @Test
    @DisplayName("reads the consultation fee from the database")
    void readsConsultationFee() {
        assertThat(configuration.getConsultationFee()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("reads the opening and closing times")
    void readsOpeningHours() {
        assertThat(configuration.getOpeningTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(configuration.getClosingTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("reads the slot length and the clinic name")
    void readsSlotLengthAndName() {
        assertThat(configuration.getAppointmentSlotMinutes()).isEqualTo(30);
        assertThat(configuration.getClinicName()).isEqualTo("Sunrise Dental Clinic");
        assertThat(configuration.getCurrency()).isEqualTo("LKR");
    }

    @Test
    @DisplayName("goes to the database only once, however many times it is asked")
    void readsTheDatabaseOnlyOnce() {
        configuration.getConsultationFee();
        configuration.getConsultationFee();
        configuration.getOpeningTime();
        configuration.getClinicName();

        // load() in setUp is the only read.
        verify(settingRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("picks up a changed fee when it is explicitly reloaded")
    void reloadPicksUpChanges() {
        when(settingRepository.findAll()).thenReturn(List.of(
                setting("consultation_fee", "2000.00")
        ));

        configuration.reload();

        assertThat(configuration.getConsultationFee()).isEqualByComparingTo("2000.00");
        verify(settingRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("falls back to a safe default when a setting is missing")
    void fallsBackWhenSettingIsMissing() {
        when(settingRepository.findAll()).thenReturn(List.of());
        ClinicConfiguration empty = new ClinicConfiguration(settingRepository);
        empty.load();

        // The billing screen must still work rather than showing an error page.
        assertThat(empty.getConsultationFee()).isEqualByComparingTo("1500.00");
        assertThat(empty.getOpeningTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(empty.getClosingTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(empty.getAppointmentSlotMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("falls back when a setting holds text that is not a number")
    void fallsBackWhenSettingIsNotANumber() {
        when(settingRepository.findAll()).thenReturn(List.of(
                setting("consultation_fee", "not a number"),
                setting("appointment_slot_minutes", "half an hour")
        ));
        ClinicConfiguration broken = new ClinicConfiguration(settingRepository);
        broken.load();

        assertThat(broken.getConsultationFee()).isEqualByComparingTo("1500.00");
        assertThat(broken.getAppointmentSlotMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("keeps working when the database cannot be reached at start-up")
    void survivesADatabaseFailureAtStartUp() {
        when(settingRepository.findAll()).thenThrow(new RuntimeException("database is down"));
        ClinicConfiguration offline = new ClinicConfiguration(settingRepository);

        offline.load();

        assertThat(offline.getConsultationFee()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("says whether a time is inside the working day")
    void checksOpeningHours() {
        assertThat(configuration.isWithinOpeningHours(LocalTime.of(9, 0))).isTrue();
        assertThat(configuration.isWithinOpeningHours(LocalTime.of(8, 0))).isTrue();
        assertThat(configuration.isWithinOpeningHours(LocalTime.of(18, 0))).isTrue();
        assertThat(configuration.isWithinOpeningHours(LocalTime.of(7, 59))).isFalse();
        assertThat(configuration.isWithinOpeningHours(LocalTime.of(18, 1))).isFalse();
    }

    @Test
    @DisplayName("says whether a time starts on a proper half-hour slot")
    void checksSlotBoundary() {
        assertThat(configuration.isOnSlotBoundary(LocalTime.of(9, 0))).isTrue();
        assertThat(configuration.isOnSlotBoundary(LocalTime.of(9, 30))).isTrue();
        assertThat(configuration.isOnSlotBoundary(LocalTime.of(9, 15))).isFalse();
        assertThat(configuration.isOnSlotBoundary(LocalTime.of(9, 1))).isFalse();
    }
}
