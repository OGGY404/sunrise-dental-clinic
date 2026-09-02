package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.dto.ReminderSummary;
import lk.icbt.cis6003.dentalclinic.dto.UpcomingReminderRow;
import lk.icbt.cis6003.dentalclinic.service.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
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
 * Tests for the reminder screen (FR7 - appointment reminders).
 *
 * This is the "who do I need to telephone tomorrow?" screen. It is open to the
 * whole front desk rather than to administrators only, because ringing round
 * tomorrow's patients is reception work, not management reporting.
 */
@DisplayName("Reminder screen")
@WebMvcTest(ReminderWebController.class)
@WithMockUser(username = "reception", roles = "RECEPTIONIST")
class ReminderWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReminderService reminderService;

    private UpcomingReminderRow reminder(String patient, String email) {
        return new UpcomingReminderRow(
                "APT-20260903-0001",
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                patient,
                "0771234567",
                email,
                "Dr. Nimal Perera",
                "Tooth Filling");
    }

    @Test
    @DisplayName("shows tomorrow's list, because that is what the front desk wants")
    void showsTomorrowByDefault() throws Exception {
        when(reminderService.findUpcoming(1))
                .thenReturn(List.of(reminder("Kamal Silva", "kamal@example.lk")));

        mockMvc.perform(get("/appointments/reminders"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointments/reminders"))
                .andExpect(model().attribute("daysAhead", 1))
                .andExpect(model().attribute("date", LocalDate.now().plusDays(1)))
                .andExpect(model().attributeExists("reminders"));

        verify(reminderService).findUpcoming(1);
    }

    @Test
    @DisplayName("another day can be chosen")
    void anotherDayCanBeChosen() throws Exception {
        when(reminderService.findUpcoming(3)).thenReturn(List.of());

        mockMvc.perform(get("/appointments/reminders").param("daysAhead", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("daysAhead", 3))
                .andExpect(model().attribute("date", LocalDate.now().plusDays(3)));

        verify(reminderService).findUpcoming(3);
    }

    @Test
    @DisplayName("a quiet day is shown as a quiet day, not as an error")
    void aQuietDayIsFine() throws Exception {
        when(reminderService.findUpcoming(1)).thenReturn(List.of());

        mockMvc.perform(get("/appointments/reminders"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("reminders"));
    }

    @Test
    @DisplayName("sending the reminders reports back how many could be reached")
    void sendingReportsBack() throws Exception {
        when(reminderService.sendReminders(1)).thenReturn(new ReminderSummary(1, 5, 3, 2));

        mockMvc.perform(post("/appointments/reminders/send").with(csrf()).param("daysAhead", "1"))
                .andExpect(redirectedUrl("/appointments/reminders?daysAhead=1"))
                .andExpect(flash().attributeExists("message"));

        verify(reminderService).sendReminders(1);
    }

    @Test
    @DisplayName("a refusal from the business tier becomes a message, not an error page")
    void aRefusalIsAMessage() throws Exception {
        when(reminderService.sendReminders(anyInt()))
                .thenThrow(new lk.icbt.cis6003.dentalclinic.exception.BadRequestException(
                        "Reminders can only be sent for the next 30 days."));

        mockMvc.perform(post("/appointments/reminders/send").with(csrf()).param("daysAhead", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
