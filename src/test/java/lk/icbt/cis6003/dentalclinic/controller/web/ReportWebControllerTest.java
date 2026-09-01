package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the management report screens.
 *
 * These are the reports the brief asks for that "add more value": what the
 * clinic earned by treatment, and how busy each dentist was.
 *
 * WHY THEY ARE ADMIN ONLY
 * They show clinic-wide money and staff performance. A receptionist needs
 * neither to do their job, and the rule is enforced in SecurityConfig rather
 * than in this controller, so it holds for any future report page too.
 */
@DisplayName("Report screens")
@WebMvcTest(ReportWebController.class)
class ReportWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    @Test
    @DisplayName("the reports menu lists what is available")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reportsMenuOpens() throws Exception {
        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/index"));
    }

    @Test
    @DisplayName("the revenue report asks the database for the chosen date range")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void revenueReportUsesTheChosenDates() throws Exception {
        when(reportService.revenueByTreatment(FROM, TO)).thenReturn(List.of());

        mockMvc.perform(get("/reports/revenue")
                        .param("from", FROM.toString())
                        .param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/revenue"))
                .andExpect(model().attributeExists("rows"))
                .andExpect(model().attribute("from", FROM))
                .andExpect(model().attribute("to", TO));

        verify(reportService).revenueByTreatment(FROM, TO);
    }

    @Test
    @DisplayName("with no dates chosen the revenue report covers this month so far")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void revenueReportDefaultsToThisMonth() throws Exception {
        when(reportService.revenueByTreatment(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/reports/revenue"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("from", LocalDate.now().withDayOfMonth(1)))
                .andExpect(model().attribute("to", LocalDate.now()));
    }

    @Test
    @DisplayName("the workload report asks its own stored procedure")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void workloadReportUsesItsOwnProcedure() throws Exception {
        when(reportService.dentistWorkload(FROM, TO)).thenReturn(List.of());

        mockMvc.perform(get("/reports/workload")
                        .param("from", FROM.toString())
                        .param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/workload"))
                .andExpect(model().attributeExists("rows"));

        verify(reportService).dentistWorkload(FROM, TO);
    }
}
