package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for appointments (FR2 register, FR3 display, FR7 cancel and reschedule).
 *
 * These are the queries that the daily schedule screen and the search screen
 * depend on, so they are the ones most worth proving.
 */
@DisplayName("AppointmentRepository")
class AppointmentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DentistRepository dentistRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;

    private Patient kamal;
    private Patient nadeeka;
    private Dentist drPerera;
    private Dentist drFernando;
    private Treatment checkUp;

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 8);

    @BeforeEach
    void setUpReferenceData() {
        kamal = patientRepository.save(newPatient("PAT-000001", "Kamal Silva", "0771234567"));
        nadeeka = patientRepository.save(newPatient("PAT-000002", "Nadeeka Silva", "0771111111"));
        drPerera = dentistRepository.save(newDentist("DEN-001", "Dr. Nimal Perera"));
        drFernando = dentistRepository.save(newDentist("DEN-002", "Dr. Shanika Fernando"));
        checkUp = treatmentRepository.save(newTreatment("TRT-001", "Dental Check-up", "2000.00"));
    }

    @Test
    @DisplayName("finds one appointment by its appointment number (FR3)")
    void findsAppointmentByNumber() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));

        Appointment found = appointmentRepository.findByAppointmentNo("APT-20260907-0001").orElseThrow();

        assertThat(found.getPatient().getFullName()).isEqualTo("Kamal Silva");
        assertThat(found.getDentist().getFullName()).isEqualTo("Dr. Nimal Perera");
        assertThat(found.getTreatment().getName()).isEqualTo("Dental Check-up");
        assertThat(found.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    @DisplayName("returns empty for an appointment number that was never issued")
    void returnsEmptyForUnknownAppointmentNumber() {
        assertThat(appointmentRepository.findByAppointmentNo("APT-19990101-0001")).isEmpty();
    }

    @Test
    @DisplayName("lists one day of appointments in time order, for the daily schedule")
    void listsOneDayInTimeOrder() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0002", kamal, drPerera, checkUp, MONDAY, LocalTime.of(14, 30)));
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", nadeeka, drFernando, checkUp, MONDAY, LocalTime.of(9, 0)));
        appointmentRepository.save(newAppointment(
                "APT-20260908-0001", kamal, drPerera, checkUp, TUESDAY, LocalTime.of(10, 0)));

        List<Appointment> mondayList =
                appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(MONDAY);

        assertThat(mondayList)
                .extracting(Appointment::getAppointmentNo)
                .containsExactly("APT-20260907-0001", "APT-20260907-0002");
    }

    @Test
    @DisplayName("lists the day of one dentist, for the per-dentist schedule")
    void listsOneDentistDay() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));
        appointmentRepository.save(newAppointment(
                "APT-20260907-0002", nadeeka, drFernando, checkUp, MONDAY, LocalTime.of(10, 0)));

        List<Appointment> pereraMonday = appointmentRepository
                .findByDentistDentistIdAndAppointmentDateOrderByAppointmentTimeAsc(
                        drPerera.getDentistId(), MONDAY);

        assertThat(pereraMonday)
                .extracting(Appointment::getAppointmentNo)
                .containsExactly("APT-20260907-0001");
    }

    @Test
    @DisplayName("lists the visits of a patient, newest first, for treatment history")
    void listsPatientHistoryNewestFirst() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));
        appointmentRepository.save(newAppointment(
                "APT-20260908-0001", kamal, drPerera, checkUp, TUESDAY, LocalTime.of(11, 0)));
        appointmentRepository.save(newAppointment(
                "APT-20260908-0002", nadeeka, drPerera, checkUp, TUESDAY, LocalTime.of(12, 0)));

        List<Appointment> history = appointmentRepository
                .findByPatientPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(kamal.getPatientId());

        assertThat(history)
                .extracting(Appointment::getAppointmentNo)
                .containsExactly("APT-20260908-0001", "APT-20260907-0001");
    }

    @Test
    @DisplayName("reports that a slot is taken when a live appointment already holds it")
    void reportsSlotTaken() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));

        boolean taken = appointmentRepository
                .existsByDentistDentistIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        drPerera.getDentistId(), MONDAY, LocalTime.of(9, 0), AppointmentStatus.CANCELLED);

        assertThat(taken).isTrue();
    }

    @Test
    @DisplayName("frees the slot again once the appointment is cancelled")
    void cancelledSlotBecomesFree() {
        Appointment booking = appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));

        booking.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.saveAndFlush(booking);

        boolean taken = appointmentRepository
                .existsByDentistDentistIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        drPerera.getDentistId(), MONDAY, LocalTime.of(9, 0), AppointmentStatus.CANCELLED);

        assertThat(taken).isFalse();
    }

    @Test
    @DisplayName("the same time is still free for a different dentist")
    void sameTimeIsFreeForAnotherDentist() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));

        boolean takenForFernando = appointmentRepository
                .existsByDentistDentistIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        drFernando.getDentistId(), MONDAY, LocalTime.of(9, 0), AppointmentStatus.CANCELLED);

        assertThat(takenForFernando).isFalse();
    }

    @Test
    @DisplayName("counts the appointments of a dentist on a day, for the workload report")
    void countsDentistWorkloadForADay() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));
        appointmentRepository.save(newAppointment(
                "APT-20260907-0003", nadeeka, drPerera, checkUp, MONDAY, LocalTime.of(11, 0)));

        long count = appointmentRepository.countByDentistDentistIdAndAppointmentDateAndStatusNot(
                drPerera.getDentistId(), MONDAY, AppointmentStatus.CANCELLED);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("finds appointments between two dates, for the revenue report")
    void findsAppointmentsInDateRange() {
        appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, checkUp, MONDAY, LocalTime.of(9, 0)));
        appointmentRepository.save(newAppointment(
                "APT-20260908-0001", nadeeka, drPerera, checkUp, TUESDAY, LocalTime.of(9, 0)));

        List<Appointment> week = appointmentRepository
                .findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(MONDAY, TUESDAY);

        assertThat(week).hasSize(2);
    }
}
