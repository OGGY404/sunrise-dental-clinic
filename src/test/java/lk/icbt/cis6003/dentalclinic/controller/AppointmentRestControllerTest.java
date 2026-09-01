package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.exception.SlotUnavailableException;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the appointment web service (FR2 register, FR3 display, FR7 cancel
 * and reschedule).
 *
 * PRESENTATION TIER. These tests check only what the web layer is responsible
 * for: reading the request, refusing bad input, calling the right service
 * method, and turning the answer into the right HTTP status and JSON.
 *
 * The booking rules themselves are not retested here. They already have their
 * own tests in AppointmentServiceTest, and repeating them would mean two places
 * to change when a rule changes.
 *
 * WebMvcTest starts the web layer only. The service is replaced by a Mockito
 * mock, so no database and no Spring Boot start-up is needed and each test runs
 * in milliseconds.
 */
@DisplayName("Appointment web service")
@WebMvcTest(AppointmentRestController.class)
@WithMockUser(username = "reception1", roles = "RECEPTIONIST")
class AppointmentRestControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @Nested
    @DisplayName("POST /api/appointments - register a visit")
    class Register {

        @Test
        @DisplayName("saves the booking and answers 201 Created with the new number")
        void savesAValidBooking() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(appointmentService.register(any(BookingRequest.class), any()))
                    .thenReturn(bookedAppointment());

            mockMvc.perform(post("/api/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBookingJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.appointmentNo").value("APT-20260907-0001"))
                    .andExpect(jsonPath("$.status").value("BOOKED"))
                    .andExpect(jsonPath("$.patient.fullName").value("Kamal Silva"))
                    .andExpect(jsonPath("$.dentistName").value("Dr. Nimal Perera"))
                    .andExpect(jsonPath("$.treatmentName").value("Tooth Filling"));
        }

        @Test
        @DisplayName("puts the new appointment address in the Location header")
        void setsTheLocationHeader() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(appointmentService.register(any(BookingRequest.class), any()))
                    .thenReturn(bookedAppointment());

            mockMvc.perform(post("/api/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBookingJson()))
                    .andExpect(status().isCreated())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().string("Location", "/api/appointments/APT-20260907-0001"));
        }

        @Test
        @DisplayName("refuses a blank patient name with 400 and names the field")
        void refusesABlankName() throws Exception {
            String badJson = validBookingJson().replace("\"Kamal Silva\"", "\"   \"");

            mockMvc.perform(post("/api/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());

            // Nothing invalid should ever reach the business tier.
            verify(appointmentService, never()).register(any(), any());
        }

        @Test
        @DisplayName("refuses a badly typed telephone number with 400")
        void refusesABadTelephoneNumber() throws Exception {
            String badJson = validBookingJson().replace("\"0771234567\"", "\"077\"");

            mockMvc.perform(post("/api/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.contactNumber").exists());
        }

        @Test
        @DisplayName("answers 409 Conflict when the dentist is already booked")
        void reportsADoubleBooking() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(appointmentService.register(any(BookingRequest.class), any()))
                    .thenThrow(new SlotUnavailableException(
                            "That dentist is already booked on that day at 09:00. Please choose another time."));

            mockMvc.perform(post("/api/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBookingJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("already booked")));
        }

        @Test
        @DisplayName("answers 422 when a clinic rule refuses the booking")
        void reportsABrokenClinicRule() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(appointmentService.register(any(BookingRequest.class), any()))
                    .thenThrow(new BusinessRuleException("The clinic is only open between 08:00 and 18:00."));

            mockMvc.perform(post("/api/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBookingJson()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("only open between")));
        }

        @Test
        @DisplayName("answers 400 when the body is not readable JSON")
        void refusesBrokenJson() throws Exception {
            mockMvc.perform(post("/api/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ this is not json }"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/appointments/{no} - display a visit")
    class FindOne {

        @Test
        @DisplayName("shows the full patient and appointment details")
        void showsTheDetails() throws Exception {
            when(appointmentService.findByNumber("APT-20260907-0001")).thenReturn(bookedAppointment());

            mockMvc.perform(get("/api/appointments/APT-20260907-0001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentNo").value("APT-20260907-0001"))
                    .andExpect(jsonPath("$.patient.patientCode").value("PAT-000001"))
                    .andExpect(jsonPath("$.patient.contactNumber").value("0771234567"))
                    .andExpect(jsonPath("$.patient.address").value("No. 42, Galle Road, Colombo 03"))
                    .andExpect(jsonPath("$.dentistCode").value("DEN-001"))
                    .andExpect(jsonPath("$.treatmentCost").value(6000.00));
        }

        @Test
        @DisplayName("answers 404 when the number was typed wrongly")
        void answers404ForAnUnknownNumber() throws Exception {
            when(appointmentService.findByNumber("APT-NOPE"))
                    .thenThrow(NotFoundException.of("appointment", "APT-NOPE"));

            mockMvc.perform(get("/api/appointments/APT-NOPE"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("APT-NOPE")));
        }
    }

    @Nested
    @DisplayName("GET /api/appointments - the day schedule report")
    class DaySchedule {

        @Test
        @DisplayName("lists every visit on a day when no dentist is named")
        void listsTheWholeClinicDiary() throws Exception {
            when(appointmentService.findDaySchedule(FUTURE_DATE))
                    .thenReturn(List.of(bookedAppointment()));

            mockMvc.perform(get("/api/appointments").param("date", FUTURE_DATE.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].appointmentNo").value("APT-20260907-0001"));

            verify(appointmentService).findDaySchedule(FUTURE_DATE);
        }

        @Test
        @DisplayName("narrows the list to one dentist when a dentist is named")
        void listsOneDentistDiary() throws Exception {
            when(appointmentService.findDayScheduleForDentist(1L, FUTURE_DATE))
                    .thenReturn(List.of(bookedAppointment()));

            mockMvc.perform(get("/api/appointments")
                            .param("date", FUTURE_DATE.toString())
                            .param("dentistId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

            verify(appointmentService).findDayScheduleForDentist(1L, FUTURE_DATE);
            verify(appointmentService, never()).findDaySchedule(any());
        }

        @Test
        @DisplayName("answers 400 when the date is not written as yyyy-MM-dd")
        void refusesAMisspelledDate() throws Exception {
            mockMvc.perform(get("/api/appointments").param("date", "08-09-2026"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("date")));
        }
    }

    @Nested
    @DisplayName("changing a visit")
    class ChangeAVisit {

        @Test
        @DisplayName("cancel passes the reason through and returns the cancelled visit")
        void cancels() throws Exception {
            when(appointmentService.cancel(eq("APT-20260907-0001"), anyString()))
                    .thenReturn(appointmentWithStatus(AppointmentStatus.CANCELLED));

            mockMvc.perform(post("/api/appointments/APT-20260907-0001/cancel")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"Patient called to cancel\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            verify(appointmentService).cancel("APT-20260907-0001", "Patient called to cancel");
        }

        @Test
        @DisplayName("cancel without a reason is refused, so the record says why")
        void cancelNeedsAReason() throws Exception {
            mockMvc.perform(post("/api/appointments/APT-20260907-0001/cancel")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.reason").exists());

            verify(appointmentService, never()).cancel(anyString(), anyString());
        }

        @Test
        @DisplayName("reschedule moves the visit to the new date and time")
        void reschedules() throws Exception {
            LocalDate newDate = FUTURE_DATE.plusDays(1);
            Appointment moved = bookedAppointment();
            moved.setAppointmentDate(newDate);
            moved.setAppointmentTime(LocalTime.of(11, 0));

            when(appointmentService.reschedule("APT-20260907-0001", newDate, LocalTime.of(11, 0)))
                    .thenReturn(moved);

            mockMvc.perform(post("/api/appointments/APT-20260907-0001/reschedule")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newDate\": \"" + newDate + "\", \"newTime\": \"11:00:00\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentDate").value(newDate.toString()))
                    .andExpect(jsonPath("$.appointmentTime").value("11:00:00"));
        }

        @Test
        @DisplayName("reschedule into the past is refused before the service is called")
        void rescheduleRefusesAPastDate() throws Exception {
            String yesterday = LocalDate.now().minusDays(1).toString();

            mockMvc.perform(post("/api/appointments/APT-20260907-0001/reschedule")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newDate\": \"" + yesterday + "\", \"newTime\": \"11:00:00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.newDate").exists());

            verify(appointmentService, never()).reschedule(anyString(), any(), any());
        }

        @Test
        @DisplayName("complete marks the visit as having taken place")
        void marksCompleted() throws Exception {
            when(appointmentService.markCompleted("APT-20260907-0001"))
                    .thenReturn(appointmentWithStatus(AppointmentStatus.COMPLETED));

            mockMvc.perform(post("/api/appointments/APT-20260907-0001/complete").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("no-show records that the patient never arrived")
        void marksNoShow() throws Exception {
            when(appointmentService.markNoShow("APT-20260907-0001"))
                    .thenReturn(appointmentWithStatus(AppointmentStatus.NO_SHOW));

            mockMvc.perform(post("/api/appointments/APT-20260907-0001/no-show").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("NO_SHOW"));
        }

        @Test
        @DisplayName("answers 422 when the visit is in the wrong state to be changed")
        void reportsAWrongStateChange() throws Exception {
            when(appointmentService.markCompleted("APT-20260907-0001"))
                    .thenThrow(new BusinessRuleException(
                            "Only a booked appointment can be marked as completed."));

            mockMvc.perform(post("/api/appointments/APT-20260907-0001/complete").with(csrf()))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
