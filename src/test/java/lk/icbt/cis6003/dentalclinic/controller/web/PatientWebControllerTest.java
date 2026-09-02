package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the patient screens (FR7 treatment history, and looking a returning
 * patient up before booking).
 */
@DisplayName("Patient screens")
@WebMvcTest(PatientWebController.class)
@WithMockUser(username = "reception", roles = "RECEPTIONIST")
class PatientWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @MockBean
    private AppointmentService appointmentService;

    private Patient kamal() {
        Patient patient = new Patient("PAT-000001", "Kamal Silva", "No. 42, Galle Road", "0771234567");
        patient.setPatientId(10L);
        return patient;
    }

    @Test
    @DisplayName("the search screen opens empty, without listing every patient")
    void searchScreenOpensEmpty() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/search"))
                .andExpect(model().attributeDoesNotExist("results"));

        // Showing every patient in the clinic to someone who has searched for
        // nothing would be slow and would spread personal data for no reason.
        verify(patientService, never()).searchByName(anyString());
    }

    @Test
    @DisplayName("searching by name shows the matches on the same screen")
    void searchByNameShowsResults() throws Exception {
        when(patientService.searchByName("Kamal")).thenReturn(List.of(kamal()));

        mockMvc.perform(get("/patients").param("name", "Kamal"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/search"))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    @DisplayName("searching by telephone number is preferred when both are given")
    void telephoneWinsOverName() throws Exception {
        when(patientService.searchByContactNumber("0771234567")).thenReturn(List.of(kamal()));

        mockMvc.perform(get("/patients")
                        .param("name", "Kamal")
                        .param("contact", "0771234567"))
                .andExpect(status().isOk());

        verify(patientService).searchByContactNumber("0771234567");
        verify(patientService, never()).searchByName(anyString());
    }

    @Test
    @DisplayName("a search that matches nobody says so, and is not an error")
    void emptyResultIsNotAnError() throws Exception {
        when(patientService.searchByName("Nobody")).thenReturn(List.of());

        mockMvc.perform(get("/patients").param("name", "Nobody"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/search"))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    @DisplayName("FR7 the patient screen shows their whole treatment history")
    void patientScreenShowsHistory() throws Exception {
        when(patientService.findByCode("PAT-000001")).thenReturn(kamal());
        when(appointmentService.findPatientHistory(10L)).thenReturn(List.of());

        mockMvc.perform(get("/patients/PAT-000001"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/view"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attributeExists("history"));
    }

    @Test
    @DisplayName("an unknown patient code goes back to the search screen with a message")
    void unknownPatientGoesBackToSearch() throws Exception {
        when(patientService.findByCode("PAT-999999"))
                .thenThrow(NotFoundException.of("patient", "PAT-999999"));

        mockMvc.perform(get("/patients/PAT-999999"))
                .andExpect(status().isOk())
                .andExpect(view().name("patients/search"))
                .andExpect(model().attributeExists("errorMessage"));
    }
}
