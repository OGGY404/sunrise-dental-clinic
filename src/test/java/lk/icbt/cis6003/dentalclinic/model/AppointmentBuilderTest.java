package lk.icbt.cis6003.dentalclinic.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for building an Appointment.
 *
 * DESIGN PATTERN UNDER TEST: Builder.
 *
 * An appointment needs eight things before it makes sense. A constructor with
 * eight parameters is easy to get wrong, because nothing stops you swapping the
 * dentist and the treatment, or the date and the time - they would still
 * compile. The builder names every value at the point where it is supplied, and
 * refuses to hand back an object that is missing something required.
 */
@DisplayName("Appointment.Builder")
class AppointmentBuilderTest {

    private Patient patient() {
        return new Patient("PAT-000001", "Kamal Silva", "No. 42, Galle Road", "0771234567");
    }

    private Dentist dentist() {
        return new Dentist("DEN-001", "Dr. Nimal Perera", "General Dentistry");
    }

    private Treatment treatment() {
        return new Treatment("TRT-003", "Tooth Filling", new BigDecimal("6000.00"), 45);
    }

    @Test
    @DisplayName("builds a complete appointment")
    void buildsACompleteAppointment() {
        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(patient())
                .dentist(dentist())
                .treatment(treatment())
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 30))
                .notes("Patient reports pain on the left side")
                .build();

        assertThat(appointment.getAppointmentNo()).isEqualTo("APT-20260907-0001");
        assertThat(appointment.getPatient().getFullName()).isEqualTo("Kamal Silva");
        assertThat(appointment.getDentist().getDentistCode()).isEqualTo("DEN-001");
        assertThat(appointment.getTreatment().getTreatmentCode()).isEqualTo("TRT-003");
        assertThat(appointment.getAppointmentDate()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(appointment.getAppointmentTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(appointment.getNotes()).isEqualTo("Patient reports pain on the left side");
    }

    @Test
    @DisplayName("starts a new appointment as BOOKED without being told to")
    void defaultsToBooked() {
        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(patient())
                .dentist(dentist())
                .treatment(treatment())
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 30))
                .build();

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    @DisplayName("allows the notes to be left out, because they are optional")
    void notesAreOptional() {
        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(patient())
                .dentist(dentist())
                .treatment(treatment())
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 30))
                .build();

        assertThat(appointment.getNotes()).isNull();
    }

    @Test
    @DisplayName("links the appointment to the patient on both sides")
    void linksBothSidesOfTheRelationship() {
        Patient kamal = patient();

        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(kamal)
                .dentist(dentist())
                .treatment(treatment())
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 30))
                .build();

        assertThat(appointment.getPatient()).isSameAs(kamal);
        assertThat(kamal.getAppointments()).contains(appointment);
    }

    @Test
    @DisplayName("refuses to build without a patient")
    void refusesToBuildWithoutAPatient() {
        assertThatThrownBy(() -> Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .dentist(dentist())
                .treatment(treatment())
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 30))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("patient");
    }

    @Test
    @DisplayName("refuses to build without a date")
    void refusesToBuildWithoutADate() {
        assertThatThrownBy(() -> Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(patient())
                .dentist(dentist())
                .treatment(treatment())
                .at(LocalTime.of(9, 30))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("date");
    }

    @Test
    @DisplayName("refuses to build without a treatment")
    void refusesToBuildWithoutATreatment() {
        assertThatThrownBy(() -> Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(patient())
                .dentist(dentist())
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 30))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("treatment");
    }

    @Test
    @DisplayName("can be told a different status, for loading old records")
    void acceptsAnExplicitStatus() {
        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(patient())
                .dentist(dentist())
                .treatment(treatment())
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 30))
                .status(AppointmentStatus.COMPLETED)
                .build();

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }
}
