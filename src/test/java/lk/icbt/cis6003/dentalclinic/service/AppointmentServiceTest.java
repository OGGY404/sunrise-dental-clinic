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
import lk.icbt.cis6003.dentalclinic.repository.AppointmentRepository;
import lk.icbt.cis6003.dentalclinic.repository.DentistRepository;
import lk.icbt.cis6003.dentalclinic.repository.TreatmentRepository;
import lk.icbt.cis6003.dentalclinic.service.notification.AppointmentObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the booking rules (FR2, FR3, FR7).
 *
 * This is the busiest class in the business tier, so it gets the most tests.
 * Every rule a receptionist could break is written down here as a test with the
 * message they should see.
 *
 * Mockito replaces the repositories, so these tests run in milliseconds and
 * need no database at all. That is the point of the Repository pattern: the
 * service can be tested without the thing it talks to.
 */
@DisplayName("AppointmentService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DentistRepository dentistRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private PatientService patientService;
    @Mock
    private ReferenceNumberGenerator numberGenerator;
    @Mock
    private ClinicConfiguration clinicConfiguration;
    @Mock
    private AppointmentObserver observer;

    private AppointmentService service;

    private Dentist drPerera;
    private Treatment filling;
    private Patient kamal;

    /** Far enough ahead that these tests never go stale. */
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);
    private static final LocalTime NINE_AM = LocalTime.of(9, 0);

    @BeforeEach
    void setUp() {
        drPerera = new Dentist("DEN-001", "Dr. Nimal Perera", "General Dentistry");
        drPerera.setDentistId(1L);
        drPerera.setActive(true);

        filling = new Treatment("TRT-003", "Tooth Filling", new BigDecimal("6000.00"), 45);
        filling.setTreatmentId(3L);
        filling.setActive(true);

        kamal = new Patient("PAT-000001", "Kamal Silva", "No. 42, Galle Road", "0771234567");
        kamal.setPatientId(10L);

        when(clinicConfiguration.isWithinOpeningHours(any())).thenReturn(true);
        when(clinicConfiguration.isOnSlotBoundary(any())).thenReturn(true);
        when(clinicConfiguration.getOpeningTime()).thenReturn(LocalTime.of(8, 0));
        when(clinicConfiguration.getClosingTime()).thenReturn(LocalTime.of(18, 0));

        when(dentistRepository.findById(1L)).thenReturn(Optional.of(drPerera));
        when(treatmentRepository.findById(3L)).thenReturn(Optional.of(filling));
        when(patientService.findOrCreate(any())).thenReturn(kamal);
        when(numberGenerator.nextAppointmentNo(any())).thenReturn("APT-20260907-0001");
        when(appointmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service = new AppointmentService(
                appointmentRepository,
                dentistRepository,
                treatmentRepository,
                patientService,
                numberGenerator,
                clinicConfiguration,
                List.of(observer));
    }

    private BookingRequest validRequest() {
        BookingRequest request = new BookingRequest();
        request.setFullName("Kamal Silva");
        request.setAddress("No. 42, Galle Road, Colombo 03");
        request.setContactNumber("0771234567");
        request.setDentistId(1L);
        request.setTreatmentId(3L);
        request.setAppointmentDate(FUTURE_DATE);
        request.setAppointmentTime(NINE_AM);
        request.setNotes("Pain on the left side");
        return request;
    }

    // --- the happy path ------------------------------------------------------

    @Test
    @DisplayName("registers an appointment and gives it a number (FR2)")
    void registersAnAppointment() {
        Appointment booked = service.register(validRequest(), null);

        assertThat(booked.getAppointmentNo()).isEqualTo("APT-20260907-0001");
        assertThat(booked.getPatient()).isSameAs(kamal);
        assertThat(booked.getDentist()).isSameAs(drPerera);
        assertThat(booked.getTreatment()).isSameAs(filling);
        assertThat(booked.getAppointmentDate()).isEqualTo(FUTURE_DATE);
        assertThat(booked.getAppointmentTime()).isEqualTo(NINE_AM);
        assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("asks the database for the appointment number, never invents one")
    void takesTheNumberFromTheGenerator() {
        service.register(validRequest(), null);

        // Only the database can hand out a unique number safely when two
        // receptionists save at the same moment.
        verify(numberGenerator).nextAppointmentNo(FUTURE_DATE);
    }

    @Test
    @DisplayName("tells the observers once the booking is saved (Observer pattern)")
    void notifiesObserversAfterSaving() {
        Appointment booked = service.register(validRequest(), null);

        ArgumentCaptor<Appointment> captured = ArgumentCaptor.forClass(Appointment.class);
        verify(observer).onAppointmentBooked(captured.capture());
        assertThat(captured.getValue().getAppointmentNo()).isEqualTo(booked.getAppointmentNo());
    }

    @Test
    @DisplayName("still completes the booking when a notification fails")
    void aFailingObserverDoesNotLoseTheBooking() {
        // Sending a confirmation email must never be able to cancel a real
        // appointment that the patient is expecting.
        org.mockito.Mockito.doThrow(new RuntimeException("mail server down"))
                .when(observer).onAppointmentBooked(any());

        Appointment booked = service.register(validRequest(), null);

        assertThat(booked.getAppointmentNo()).isEqualTo("APT-20260907-0001");
        verify(appointmentRepository).save(any(Appointment.class));
    }

    // --- the rules -----------------------------------------------------------

    @Test
    @DisplayName("refuses a booking in the past")
    void refusesADateInThePast() {
        BookingRequest request = validRequest();
        request.setAppointmentDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> service.register(request, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("past");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses a time outside opening hours")
    void refusesATimeOutsideOpeningHours() {
        when(clinicConfiguration.isWithinOpeningHours(any())).thenReturn(false);

        assertThatThrownBy(() -> service.register(validRequest(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("open");
    }

    @Test
    @DisplayName("refuses a time that does not start a proper slot")
    void refusesATimeOffTheSlotGrid() {
        when(clinicConfiguration.isOnSlotBoundary(any())).thenReturn(false);

        assertThatThrownBy(() -> service.register(validRequest(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("refuses a dentist who does not exist")
    void refusesAnUnknownDentist() {
        when(dentistRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(validRequest(), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("refuses a dentist who has left the clinic")
    void refusesARetiredDentist() {
        drPerera.setActive(false);

        assertThatThrownBy(() -> service.register(validRequest(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("practising");
    }

    @Test
    @DisplayName("refuses a treatment that is off the price list")
    void refusesAnInactiveTreatment() {
        filling.setActive(false);

        assertThatThrownBy(() -> service.register(validRequest(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("price list");
    }

    @Test
    @DisplayName("refuses a slot that dentist already has, with a message staff can act on")
    void refusesADoubleBooking() {
        when(appointmentRepository
                .existsByDentistDentistIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        eq(1L), eq(FUTURE_DATE), eq(NINE_AM), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest(), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("already booked");

        verify(appointmentRepository, never()).save(any());
    }

    // --- finding an appointment (FR3) ----------------------------------------

    @Test
    @DisplayName("finds an appointment by its number (FR3)")
    void findsByAppointmentNumber() {
        Appointment existing = existingAppointment(AppointmentStatus.BOOKED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));

        Appointment found = service.findByNumber("APT-20260907-0001");

        assertThat(found).isSameAs(existing);
    }

    @Test
    @DisplayName("says clearly when an appointment number does not exist")
    void reportsAnUnknownAppointmentNumber() {
        when(appointmentRepository.findByAppointmentNo(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByNumber("APT-19990101-0001"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("APT-19990101-0001");
    }

    // --- cancelling and rescheduling (FR7) -----------------------------------

    @Test
    @DisplayName("cancels a booked appointment and tells the observers")
    void cancelsABookedAppointment() {
        Appointment existing = existingAppointment(AppointmentStatus.BOOKED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));

        Appointment cancelled = service.cancel("APT-20260907-0001", "Patient called to cancel");

        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getNotes()).contains("Patient called to cancel");
        verify(observer).onAppointmentCancelled(existing);
    }

    @Test
    @DisplayName("refuses to cancel a visit that has already happened")
    void refusesToCancelACompletedVisit() {
        Appointment existing = existingAppointment(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.cancel("APT-20260907-0001", "changed my mind"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("refuses to cancel the same appointment twice")
    void refusesToCancelTwice() {
        Appointment existing = existingAppointment(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.cancel("APT-20260907-0001", "again"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("moves an appointment to a free slot")
    void reschedulesToAFreeSlot() {
        Appointment existing = existingAppointment(AppointmentStatus.BOOKED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));

        LocalDate newDate = FUTURE_DATE.plusDays(1);
        LocalTime newTime = LocalTime.of(11, 0);

        Appointment moved = service.reschedule("APT-20260907-0001", newDate, newTime);

        assertThat(moved.getAppointmentDate()).isEqualTo(newDate);
        assertThat(moved.getAppointmentTime()).isEqualTo(newTime);
        assertThat(moved.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    @DisplayName("refuses to move an appointment onto a slot that is taken")
    void refusesToRescheduleOntoATakenSlot() {
        Appointment existing = existingAppointment(AppointmentStatus.BOOKED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));
        when(appointmentRepository
                .existsByDentistDentistIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        anyLong(), any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.reschedule(
                "APT-20260907-0001", FUTURE_DATE.plusDays(1), LocalTime.of(11, 0)))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    @DisplayName("marks a visit completed, so it can then be billed")
    void marksAVisitCompleted() {
        Appointment existing = existingAppointment(AppointmentStatus.BOOKED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));

        Appointment done = service.markCompleted("APT-20260907-0001");

        assertThat(done.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(done.isBillable()).isTrue();
    }

    @Test
    @DisplayName("refuses to complete a cancelled appointment")
    void refusesToCompleteACancelledAppointment() {
        Appointment existing = existingAppointment(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.markCompleted("APT-20260907-0001"))
                .isInstanceOf(BusinessRuleException.class);
    }

    private Appointment existingAppointment(AppointmentStatus status) {
        return Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(kamal)
                .dentist(drPerera)
                .treatment(filling)
                .on(FUTURE_DATE)
                .at(NINE_AM)
                .status(status)
                .build();
    }
}
