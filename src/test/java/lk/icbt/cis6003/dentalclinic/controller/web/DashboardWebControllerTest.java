package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.config.ClinicConfiguration;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.BillingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the main menu screen and the help screen (FR5).
 *
 * The brief asks for a menu-driven, user-friendly system, and for a help
 * section with step-by-step instructions for new staff. Those are these two
 * pages.
 */
@DisplayName("Dashboard and help screens")
@WebMvcTest(DashboardWebController.class)
@WithMockUser(username = "reception", roles = "RECEPTIONIST")
class DashboardWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private BillingService billingService;

    @MockBean
    private ClinicConfiguration clinicConfiguration;

    @Test
    @DisplayName("the main menu shows todays diary and what is still owed")
    void mainMenuShowsTodaysNumbers() throws Exception {
        when(appointmentService.findDaySchedule(LocalDate.now())).thenReturn(List.of());
        when(billingService.findUnpaidBills()).thenReturn(List.of());
        when(clinicConfiguration.getClinicName()).thenReturn("Sunrise Dental Clinic");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("today"))
                .andExpect(model().attributeExists("todayAppointments"))
                .andExpect(model().attributeExists("unpaidBills"));
    }

    @Test
    @DisplayName("FR5 the help screen opens without needing anything loaded")
    void helpScreenOpens() throws Exception {
        mockMvc.perform(get("/help"))
                .andExpect(status().isOk())
                .andExpect(view().name("help"));
    }
}
