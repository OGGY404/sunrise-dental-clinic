package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.service.ReferenceDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the two lists the booking form needs: which dentists can be booked,
 * and what treatments are on the current price list.
 *
 * WHY THESE ENDPOINTS EXIST
 * The booking screen has two dropdowns. Without these, the receptionist would
 * have to remember dentist and treatment id numbers.
 *
 * WHY ONLY THE ACTIVE ONES ARE RETURNED
 * A dentist who has left the clinic, or a treatment taken off the price list,
 * must not appear in a dropdown, or staff would book something that the
 * business tier will then refuse.
 */
@DisplayName("Reference data web service")
@WebMvcTest(ReferenceDataRestController.class)
@WithMockUser(username = "reception1", roles = "RECEPTIONIST")
class ReferenceDataRestControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReferenceDataService referenceDataService;

    @Test
    @DisplayName("GET /api/dentists lists the dentists who can be booked")
    void listsBookableDentists() throws Exception {
        when(referenceDataService.bookableDentists()).thenReturn(List.of(drPerera()));

        mockMvc.perform(get("/api/dentists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].dentistId").value(1))
                .andExpect(jsonPath("$[0].dentistCode").value("DEN-001"))
                .andExpect(jsonPath("$[0].fullName").value("Dr. Nimal Perera"))
                .andExpect(jsonPath("$[0].specialisation").value("General Dentistry"));
    }

    @Test
    @DisplayName("GET /api/treatments lists the current price list with the costs")
    void listsBookableTreatments() throws Exception {
        when(referenceDataService.bookableTreatments()).thenReturn(List.of(filling()));

        mockMvc.perform(get("/api/treatments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].treatmentId").value(3))
                .andExpect(jsonPath("$[0].name").value("Tooth Filling"))
                .andExpect(jsonPath("$[0].cost").value(6000.00))
                .andExpect(jsonPath("$[0].durationMinutes").value(45));
    }

    @Test
    @DisplayName("an empty price list is an empty array, not an error")
    void emptyListIsNotAnError() throws Exception {
        when(referenceDataService.bookableTreatments()).thenReturn(List.of());

        mockMvc.perform(get("/api/treatments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }
}
