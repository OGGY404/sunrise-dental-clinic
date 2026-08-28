package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access for appointments (FR2, FR3, FR7).
 * DESIGN PATTERN: Repository.
 *
 * The method names are long because Spring Data builds the SQL from them. Read
 * them in pieces: findBy + Dentist.dentistId + And + appointmentDate +
 * OrderBy + appointmentTime + Asc.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** FR3 - the receptionist types the appointment number and sees the visit. */
    Optional<Appointment> findByAppointmentNo(String appointmentNo);

    /** Report: the whole clinic diary for one day. */
    List<Appointment> findByAppointmentDateOrderByAppointmentTimeAsc(LocalDate appointmentDate);

    /** Report: the diary of one dentist for one day. */
    List<Appointment> findByDentistDentistIdAndAppointmentDateOrderByAppointmentTimeAsc(
            Long dentistId, LocalDate appointmentDate);

    /** FR7 - the treatment history of one patient, newest visit first. */
    List<Appointment> findByPatientPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    /** Report: everything between two dates, used by the revenue report. */
    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
            LocalDate from, LocalDate to);

    /**
     * Is this dentist already busy at this exact date and time?
     *
     * StatusNot(CANCELLED) is the important part: a cancelled appointment must
     * not keep blocking the slot, otherwise the time could never be resold.
     *
     * This is the polite check that produces a friendly message. The real
     * guarantee is the UNIQUE index on appointments.slot_key in the database,
     * which cannot be beaten by two people saving at the same instant.
     */
    boolean existsByDentistDentistIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long dentistId, LocalDate appointmentDate, LocalTime appointmentTime, AppointmentStatus status);

    /** Report: how many real appointments a dentist has on a day. */
    long countByDentistDentistIdAndAppointmentDateAndStatusNot(
            Long dentistId, LocalDate appointmentDate, AppointmentStatus status);

    /** Everything at one status, for example every visit still BOOKED. */
    List<Appointment> findByStatusOrderByAppointmentDateAscAppointmentTimeAsc(AppointmentStatus status);

    /** Used by the reminder feature: tomorrow list of visits still booked. */
    List<Appointment> findByAppointmentDateAndStatusOrderByAppointmentTimeAsc(
            LocalDate appointmentDate, AppointmentStatus status);

    boolean existsByAppointmentNo(String appointmentNo);
}
