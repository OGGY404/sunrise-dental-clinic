package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Tests for the patient web service (FR2 patient details, FR7 treatment
 * history).
 *
 * PRESENTATION TIER only. Finding a returning patient is a clinic rule and is
 * tested in PatientServiceTest, not here.
 */
@DisplayName("Patient web service")
@WebMvcTest(PatientRestController.class)
@WithMockUser(username = "reception1", roles = "RECEPTIONIST")
class PatientRestControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @MockBean
    private AppointmentService appointmentService;

    @Nested
    @DisplayName("GET /api/patients/{code}")
    class FindOne {

        @Test
        @DisplayName("shows the patient record")
        void showsThePatient() throws Exception {
            when(patientService.findByCode("PAT-000001")).thenReturn(kamal());

            mockMvc.perform(get("/api/patients/PAT-000001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patientCode").value("PAT-000001"))
                    .andExpect(jsonPath("$.fullName").value("Kamal Silva"))
                    .andExpect(jsonPath("$.contactNumber").value("0771234567"));
        }

        @Test
        @DisplayName("answers 404 for a code that does not exist")
        void unknownCodeIs404() throws Exception {
            when(patientService.findByCode("PAT-999999"))
                    .thenThrow(NotFoundException.of("patient", "PAT-999999"));

            mockMvc.perform(get("/api/patients/PAT-999999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/patients - searching")
    class Search {

        @Test
        @DisplayName("searches by part of the name")
        void searchesByName() throws Exception {
            when(patientService.searchByName("Kamal")).thenReturn(List.of(kamal()));

            mockMvc.perform(get("/api/patients").param("name", "Kamal"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].fullName").value("Kamal Silva"));
        }

        @Test
        @DisplayName("searches by telephone number")
        void searchesByContactNumber() throws Exception {
            when(patientService.searchByContactNumber("0771234567")).thenReturn(List.of(kamal()));

            mockMvc.perform(get("/api/patients").param("contact", "0771234567"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].patientCode").value("PAT-000001"));
        }

        @Test
        @DisplayName("refuses a search with nothing to search for")
        void refusesAnEmptySearch() throws Exception {
            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("name")));
        }

        @Test
        @DisplayName("an empty result is an empty list, not an error")
        void emptyResultIsAnEmptyList() throws Exception {
            when(patientService.searchByName("Nobody")).thenReturn(List.of());

            mockMvc.perform(get("/api/patients").param("name", "Nobody"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/patients/{code}/history - FR7 treatment history")
    class History {

        @Test
        @DisplayName("lists every visit this patient has had, newest first")
        void listsTheHistory() throws Exception {
            when(patientService.findByCode("PAT-000001")).thenReturn(kamal());
            when(appointmentService.findPatientHistory(10L)).thenReturn(List.of(bookedAppointment()));

            mockMvc.perform(get("/api/patients/PAT-000001/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].appointmentNo").value("APT-20260907-0001"))
                    .andExpect(jsonPath("$[0].treatmentName").value("Tooth Filling"));
        }

        @Test
        @DisplayName("answers 404 when the patient does not exist")
        void unknownPatientIs404() throws Exception {
            when(patientService.findByCode("PAT-999999"))
                    .thenThrow(NotFoundException.of("patient", "PAT-999999"));

            mockMvc.perform(get("/api/patients/PAT-999999/history"))
                    .andExpect(status().isNotFound());
        }
    }
}
