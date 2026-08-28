package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.config.ClinicConfiguration;
import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.exception.SlotUnavailableException;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.AppointmentRepository;
import lk.icbt.cis6003.dentalclinic.repository.DentistRepository;
import lk.icbt.cis6003.dentalclinic.repository.TreatmentRepository;
import lk.icbt.cis6003.dentalclinic.service.notification.AppointmentObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * The booking rules (FR2 register, FR3 display, FR7 cancel and reschedule).
 *
 * BUSINESS LOGIC TIER, and the busiest class in it.
 *
 * DESIGN PATTERN: Observer. This class is the subject. It announces bookings,
 * cancellations and changes, without knowing who is listening. Spring hands it
 * the list of observers.
 *
 * WHERE THE RULES REALLY LIVE
 * Every rule checked here is also enforced by the database, either by a trigger
 * or by the UNIQUE index on appointments.slot_key. That is not an accident and
 * it is not wasted work:
 *
 *   - the check here happens first, and produces a sentence a receptionist can
 *     read and act on ("that dentist is already booked at 9:00");
 *   - the database check happens last, and cannot be beaten by two people
 *     pressing Save in the same fraction of a second, or by anyone reaching the
 *     database through a script or MySQL Workbench.
 *
 * The cost is that the rule is written twice and could drift apart. The report
 * discusses this trade-off honestly.
 */
@Service
@Transactional
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final DentistRepository dentistRepository;
    private final TreatmentRepository treatmentRepository;
    private final PatientService patientService;
    private final ReferenceNumberGenerator numberGenerator;
    private final ClinicConfiguration clinicConfiguration;
    private final List<AppointmentObserver> observers;

    /**
     * Spring finds every AppointmentObserver and passes them all in. Adding a
     * new kind of notification therefore means writing one class and changing
     * nothing here.
     */
    public AppointmentService(AppointmentRepository appointmentRepository,
                              DentistRepository dentistRepository,
                              TreatmentRepository treatmentRepository,
                              PatientService patientService,
                              ReferenceNumberGenerator numberGenerator,
                              ClinicConfiguration clinicConfiguration,
                              List<AppointmentObserver> observers) {
        this.appointmentRepository = appointmentRepository;
        this.dentistRepository = dentistRepository;
        this.treatmentRepository = treatmentRepository;
        this.patientService = patientService;
        this.numberGenerator = numberGenerator;
        this.clinicConfiguration = clinicConfiguration;
        this.observers = observers;
    }

    // --- FR2: register a new appointment -------------------------------------

    /**
     * Books a visit and returns the saved appointment.
     *
     * The order of the steps matters. Everything that can be refused is checked
     * before anything is written, so a rejected booking never leaves a
     * half-created patient behind.
     */
    public Appointment register(BookingRequest request, User createdBy) {
        checkDateIsNotInThePast(request.getAppointmentDate());
        checkTimeIsBookable(request.getAppointmentTime());

        Dentist dentist = loadActiveDentist(request.getDentistId());
        Treatment treatment = loadActiveTreatment(request.getTreatmentId());

        checkSlotIsFree(dentist.getDentistId(), request.getAppointmentDate(), request.getAppointmentTime());

        Patient patient = patientService.findOrCreate(request);
        String appointmentNo = numberGenerator.nextAppointmentNo(request.getAppointmentDate());

        // DESIGN PATTERN: Builder. Each value is named, so the dentist and the
        // treatment cannot be swapped by accident.
        Appointment appointment = Appointment.builder()
                .appointmentNo(appointmentNo)
                .patient(patient)
                .dentist(dentist)
                .treatment(treatment)
                .on(request.getAppointmentDate())
                .at(request.getAppointmentTime())
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Registered appointment {}", saved.getAppointmentNo());

        announce(observer -> observer.onAppointmentBooked(saved));
        return saved;
    }

    // --- FR3: display appointment details ------------------------------------

    @Transactional(readOnly = true)
    public Appointment findByNumber(String appointmentNo) {
        return appointmentRepository.findByAppointmentNo(appointmentNo)
                .orElseThrow(() -> NotFoundException.of("appointment", appointmentNo));
    }

    @Transactional(readOnly = true)
    public List<Appointment> findDaySchedule(LocalDate date) {
        return appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(date);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findDayScheduleForDentist(Long dentistId, LocalDate date) {
        return appointmentRepository
                .findByDentistDentistIdAndAppointmentDateOrderByAppointmentTimeAsc(dentistId, date);
    }

    /** FR7: everything this patient has ever had done, newest visit first. */
    @Transactional(readOnly = true)
    public List<Appointment> findPatientHistory(Long patientId) {
        return appointmentRepository
                .findByPatientPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId);
    }

    // --- FR7: cancel, reschedule, complete ------------------------------------

    /**
     * Cancels a booking. The slot becomes free again straight away, because the
     * database only treats a live appointment as holding its slot.
     */
    public Appointment cancel(String appointmentNo, String reason) {
        Appointment appointment = findByNumber(appointmentNo);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Appointment " + appointmentNo + " has already been cancelled.");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "Appointment " + appointmentNo + " cannot be cancelled, because the visit has already taken place.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setNotes(appendNote(appointment.getNotes(), "Cancelled: " + reason));

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Cancelled appointment {}", appointmentNo);

        announce(observer -> observer.onAppointmentCancelled(saved));
        return saved;
    }

    /** Moves a booking to a different date and time. */
    public Appointment reschedule(String appointmentNo, LocalDate newDate, LocalTime newTime) {
        Appointment appointment = findByNumber(appointmentNo);

        if (!appointment.isLive()) {
            throw new BusinessRuleException(
                    "Appointment " + appointmentNo + " has been cancelled, so it cannot be moved. Please book a new one.");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "Appointment " + appointmentNo + " cannot be moved, because the visit has already taken place.");
        }

        checkDateIsNotInThePast(newDate);
        checkTimeIsBookable(newTime);
        checkSlotIsFree(appointment.getDentist().getDentistId(), newDate, newTime);

        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Moved appointment {} to {} at {}", appointmentNo, newDate, newTime);

        announce(observer -> observer.onAppointmentRescheduled(saved));
        return saved;
    }

    /** Marks a visit as having taken place, which is what makes it billable. */
    public Appointment markCompleted(String appointmentNo) {
        Appointment appointment = findByNumber(appointmentNo);

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessRuleException(
                    "Only a booked appointment can be marked as completed. Appointment "
                            + appointmentNo + " is currently " + appointment.getStatus() + ".");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    /** Records that the patient never arrived. The slot is not given back. */
    public Appointment markNoShow(String appointmentNo) {
        Appointment appointment = findByNumber(appointmentNo);

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessRuleException(
                    "Only a booked appointment can be marked as a no-show. Appointment "
                            + appointmentNo + " is currently " + appointment.getStatus() + ".");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return appointmentRepository.save(appointment);
    }

    // --- the checks ----------------------------------------------------------

    private void checkDateIsNotInThePast(LocalDate date) {
        if (date == null) {
            throw new BusinessRuleException("Please choose a date for the appointment.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("An appointment cannot be booked in the past.");
        }
    }

    private void checkTimeIsBookable(LocalTime time) {
        if (time == null) {
            throw new BusinessRuleException("Please choose a time for the appointment.");
        }
        if (!clinicConfiguration.isWithinOpeningHours(time)) {
            throw new BusinessRuleException("The clinic is only open between "
                    + clinicConfiguration.getOpeningTime() + " and "
                    + clinicConfiguration.getClosingTime() + ".");
        }
        if (!clinicConfiguration.isOnSlotBoundary(time)) {
            throw new BusinessRuleException("Appointments must start on a "
                    + clinicConfiguration.getAppointmentSlotMinutes()
                    + " minute boundary, for example 09:00 or 09:30.");
        }
    }

    private Dentist loadActiveDentist(Long dentistId) {
        if (dentistId == null) {
            throw new BusinessRuleException("Please choose a dentist.");
        }
        Dentist dentist = dentistRepository.findById(dentistId)
                .orElseThrow(() -> NotFoundException.of("dentist", String.valueOf(dentistId)));

        if (!dentist.isActive()) {
            throw new BusinessRuleException(
                    dentist.getFullName() + " is no longer practising at this clinic, so cannot be booked.");
        }
        return dentist;
    }

    private Treatment loadActiveTreatment(Long treatmentId) {
        if (treatmentId == null) {
            throw new BusinessRuleException("Please choose a treatment.");
        }
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> NotFoundException.of("treatment", String.valueOf(treatmentId)));

        if (!treatment.isActive()) {
            throw new BusinessRuleException(
                    treatment.getName() + " is not on the current price list, so it cannot be booked.");
        }
        return treatment;
    }

    /**
     * The polite double-booking check.
     *
     * A cancelled appointment does not block the slot, which is what lets the
     * time be sold again. The final guarantee is the UNIQUE index on
     * appointments.slot_key; this check exists so the receptionist gets a
     * sentence instead of a database error.
     */
    private void checkSlotIsFree(Long dentistId, LocalDate date, LocalTime time) {
        boolean taken = appointmentRepository
                .existsByDentistDentistIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        dentistId, date, time, AppointmentStatus.CANCELLED);

        if (taken) {
            throw new SlotUnavailableException(
                    "That dentist is already booked on " + date + " at " + time
                            + ". Please choose another time.");
        }
    }

    // --- the Observer notification -------------------------------------------

    /**
     * Tells every observer, and never lets one of them break the booking.
     *
     * A mail server being down must not undo an appointment the patient is
     * expecting. Each observer is called inside its own try, so one failing
     * observer does not stop the others being told either.
     */
    private void announce(Consumer<AppointmentObserver> event) {
        for (AppointmentObserver observer : observers) {
            try {
                event.accept(observer);
            } catch (RuntimeException problem) {
                log.warn("Notification failed in {}. The appointment itself is unaffected. Reason: {}",
                        observer.getClass().getSimpleName(), problem.getMessage());
            }
        }
    }

    /** Adds a line to the notes without throwing away what was already there. */
    private String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) {
            return addition;
        }
        return existing + " | " + addition;
    }
}
