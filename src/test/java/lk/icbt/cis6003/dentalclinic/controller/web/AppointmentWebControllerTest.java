package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.controller.CurrentUserResolver;
import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.exception.SlotUnavailableException;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.ReferenceDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the appointment screens (FR2 register, FR3 display, FR7 cancel and
 * reschedule).
 *
 * PRESENTATION TIER, the pages a receptionist actually looks at.
 *
 * WHY THE SCREENS ARE TESTED SEPARATELY FROM THE WEB SERVICES
 * They answer differently, and the difference is the whole point of a screen. A
 * web service refuses a bad form with 400 and a JSON body; a screen must send
 * the receptionist back to the same form, with what they already typed still in
 * the boxes and the message under the right box. A redirect or a blank form
 * would make them type everything again.
 *
 * THE SEPARATE VIEWS THE BRIEF ASKS FOR
 * The assessment requires data entry and viewing results to be different
 * screens. Registering uses appointments/register; the result is shown by
 * appointments/view, reached by a redirect. The redirect matters: it means
 * pressing refresh on the result page cannot book a second appointment.
 */
@DisplayName("Appointment screens")
@WebMvcTest(AppointmentWebController.class)
@WithMockUser(username = "reception", roles = "RECEPTIONIST")
class AppointmentWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private ReferenceDataService referenceDataService;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);

    private Appointment bookedAppointment() {
        Dentist dentist = new Dentist("DEN-001", "Dr. Nimal Perera", "General Dentistry");
        dentist.setDentistId(1L);
        Treatment treatment = new Treatment("TRT-003", "Tooth Filling", new BigDecimal("6000.00"), 45);
        treatment.setTreatmentId(3L);
        Patient patient = new Patient("PAT-000001", "Kamal Silva", "No. 42, Galle Road", "0771234567");
        patient.setPatientId(10L);

        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260908-0001")
                .patient(patient)
                .dentist(dentist)
                .treatment(treatment)
                .on(FUTURE_DATE)
                .at(LocalTime.of(9, 0))
                .build();
        appointment.setAppointmentId(100L);
        return appointment;
    }

    @Nested
    @DisplayName("the registration form")
    class RegistrationForm {

        @Test
        @DisplayName("shows the form with an empty booking and both dropdown lists")
        void showsTheEmptyForm() throws Exception {
            when(referenceDataService.bookableDentists()).thenReturn(List.of());
            when(referenceDataService.bookableTreatments()).thenReturn(List.of());

            mockMvc.perform(get("/appointments/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("appointments/register"))
                    .andExpect(model().attributeExists("bookingRequest"))
                    .andExpect(model().attributeExists("dentists"))
                    .andExpect(model().attributeExists("treatments"));
        }

        @Test
        @DisplayName("a good booking redirects to the details screen, so refresh cannot book twice")
        void goodBookingRedirectsToTheDetails() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(appointmentService.register(any(BookingRequest.class), any()))
                    .thenReturn(bookedAppointment());

            mockMvc.perform(post("/appointments/new")
                            .with(csrf())
                            .param("fullName", "Kamal Silva")
                            .param("address", "No. 42, Galle Road, Colombo 03")
                            .param("contactNumber", "0771234567")
                            .param("dentistId", "1")
                            .param("treatmentId", "3")
                            .param("appointmentDate", FUTURE_DATE.toString())
                            .param("appointmentTime", "09:00"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/appointments/APT-20260908-0001"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("a bad form comes straight back, with the boxes still filled in")
        void badFormComesBackFilledIn() throws Exception {
            when(referenceDataService.bookableDentists()).thenReturn(List.of());
            when(referenceDataService.bookableTreatments()).thenReturn(List.of());

            mockMvc.perform(post("/appointments/new")
                            .with(csrf())
                            .param("fullName", "Kamal 123")
                            .param("address", "No. 42, Galle Road, Colombo 03")
                            .param("contactNumber", "077")
                            .param("dentistId", "1")
                            .param("treatmentId", "3")
                            .param("appointmentDate", FUTURE_DATE.toString())
                            .param("appointmentTime", "09:00"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("appointments/register"))
                    .andExpect(model().attributeHasFieldErrors("bookingRequest", "fullName", "contactNumber"))
                    // the dropdowns must be reloaded, or the form would come back empty
                    .andExpect(model().attributeExists("dentists"))
                    .andExpect(model().attributeExists("treatments"));

            verify(appointmentService, never()).register(any(), any());
        }

        @Test
        @DisplayName("a taken slot is shown on the form, not on an error page")
        void takenSlotIsShownOnTheForm() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(referenceDataService.bookableDentists()).thenReturn(List.of());
            when(referenceDataService.bookableTreatments()).thenReturn(List.of());
            when(appointmentService.register(any(BookingRequest.class), any()))
                    .thenThrow(new SlotUnavailableException(
                            "That dentist is already booked at 09:00. Please choose another time."));

            mockMvc.perform(post("/appointments/new")
                            .with(csrf())
                            .param("fullName", "Kamal Silva")
                            .param("address", "No. 42, Galle Road, Colombo 03")
                            .param("contactNumber", "0771234567")
                            .param("dentistId", "1")
                            .param("treatmentId", "3")
                            .param("appointmentDate", FUTURE_DATE.toString())
                            .param("appointmentTime", "09:00"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("appointments/register"))
                    .andExpect(model().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("finding and showing a visit")
    class FindAndShow {

        @Test
        @DisplayName("the search screen is its own page")
        void searchScreenIsItsOwnPage() throws Exception {
            mockMvc.perform(get("/appointments/search"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("appointments/search"));
        }

        @Test
        @DisplayName("FR3 the details screen shows the visit")
        void detailsScreenShowsTheVisit() throws Exception {
            when(appointmentService.findByNumber("APT-20260908-0001")).thenReturn(bookedAppointment());

            mockMvc.perform(get("/appointments/APT-20260908-0001"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("appointments/view"))
                    .andExpect(model().attributeExists("appointment"));
        }

        @Test
        @DisplayName("a number that does not exist goes back to the search screen with a message")
        void unknownNumberGoesBackToSearch() throws Exception {
            when(appointmentService.findByNumber("APT-NOPE"))
                    .thenThrow(NotFoundException.of("appointment", "APT-NOPE"));

            mockMvc.perform(get("/appointments/search").param("appointmentNo", "APT-NOPE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("appointments/search"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        @DisplayName("searching with a number sends the receptionist to the details screen")
        void searchingRedirectsToTheDetails() throws Exception {
            when(appointmentService.findByNumber("APT-20260908-0001")).thenReturn(bookedAppointment());

            mockMvc.perform(get("/appointments/search").param("appointmentNo", "APT-20260908-0001"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/appointments/APT-20260908-0001"));
        }
    }

    @Nested
    @DisplayName("changing a visit")
    class ChangingAVisit {

        @Test
        @DisplayName("completing a visit returns to the same screen with a message")
        void completeReturnsWithAMessage() throws Exception {
            Appointment done = bookedAppointment();
            done.setStatus(AppointmentStatus.COMPLETED);
            when(appointmentService.markCompleted("APT-20260908-0001")).thenReturn(done);

            mockMvc.perform(post("/appointments/APT-20260908-0001/complete").with(csrf()))
                    .andExpect(redirectedUrl("/appointments/APT-20260908-0001"))
                    .andExpect(flash().attributeExists("message"));
        }

        @Test
        @DisplayName("cancelling passes the reason through")
        void cancelPassesTheReason() throws Exception {
            Appointment cancelled = bookedAppointment();
            cancelled.setStatus(AppointmentStatus.CANCELLED);
            when(appointmentService.cancel(anyString(), anyString())).thenReturn(cancelled);

            mockMvc.perform(post("/appointments/APT-20260908-0001/cancel")
                            .with(csrf())
                            .param("reason", "Patient telephoned to cancel"))
                    .andExpect(redirectedUrl("/appointments/APT-20260908-0001"));

            verify(appointmentService).cancel("APT-20260908-0001", "Patient telephoned to cancel");
        }

        @Test
        @DisplayName("a refused change is shown as a message, not an error page")
        void refusedChangeIsAMessage() throws Exception {
            when(appointmentService.markCompleted("APT-20260908-0001"))
                    .thenThrow(new lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException(
                            "Only a booked appointment can be marked as completed."));

            mockMvc.perform(post("/appointments/APT-20260908-0001/complete").with(csrf()))
                    .andExpect(redirectedUrl("/appointments/APT-20260908-0001"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("the day schedule report")
    class DaySchedule {

        @Test
        @DisplayName("shows the whole clinic diary for a chosen day")
        void showsTheDiary() throws Exception {
            when(appointmentService.findDaySchedule(FUTURE_DATE))
                    .thenReturn(List.of(bookedAppointment()));
            when(referenceDataService.bookableDentists()).thenReturn(List.of());

            mockMvc.perform(get("/appointments/schedule").param("date", FUTURE_DATE.toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("appointments/schedule"))
                    .andExpect(model().attribute("date", FUTURE_DATE))
                    .andExpect(model().attributeExists("appointments"));
        }

        @Test
        @DisplayName("with no date chosen it shows today, which is what the front desk wants")
        void defaultsToToday() throws Exception {
            when(appointmentService.findDaySchedule(LocalDate.now())).thenReturn(List.of());
            when(referenceDataService.bookableDentists()).thenReturn(List.of());

            mockMvc.perform(get("/appointments/schedule"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("date", LocalDate.now()));

            verify(appointmentService).findDaySchedule(LocalDate.now());
        }

        @Test
        @DisplayName("narrows to one dentist when one is chosen")
        void narrowsToOneDentist() throws Exception {
            when(appointmentService.findDayScheduleForDentist(1L, FUTURE_DATE))
                    .thenReturn(List.of(bookedAppointment()));
            when(referenceDataService.bookableDentists()).thenReturn(List.of());

            mockMvc.perform(get("/appointments/schedule")
                            .param("date", FUTURE_DATE.toString())
                            .param("dentistId", "1"))
                    .andExpect(status().isOk());

            verify(appointmentService).findDayScheduleForDentist(1L, FUTURE_DATE);
        }
    }
}
