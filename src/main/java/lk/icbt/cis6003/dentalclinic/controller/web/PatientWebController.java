package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.dto.AppointmentResponse;
import lk.icbt.cis6003.dentalclinic.dto.PatientResponse;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.PatientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * The patient screens (FR7 treatment history, and finding a returning patient).
 *
 * PRESENTATION TIER.
 *
 * WHY AN EMPTY SEARCH SHOWS NOTHING
 * Opening this screen does not list every patient in the clinic. That would be
 * slow once the clinic has been running a year, and it would put everybody's
 * name, address and telephone number on screen for no reason. The receptionist
 * has to search for somebody in particular.
 */
@Controller
@RequestMapping("/patients")
public class PatientWebController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientWebController(PatientService patientService,
                                AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    /**
     * Search, by telephone number or by part of a name.
     *
     * Telephone is tried first when both are given, because it is the reliable
     * one: a name can be spelled several ways and shared by two people, while a
     * number is read off the patient's own phone.
     */
    @GetMapping
    public String search(@RequestParam(required = false) String name,
                         @RequestParam(required = false) String contact,
                         Model model) {

        model.addAttribute("name", name);
        model.addAttribute("contact", contact);

        if (contact != null && !contact.isBlank()) {
            addResults(model, patientService.searchByContactNumber(contact.trim()));
        } else if (name != null && !name.isBlank()) {
            addResults(model, patientService.searchByName(name.trim()));
        }

        return "patients/search";
    }

    /**
     * One patient, with everything that has ever been done for them (FR7).
     *
     * An unknown code sends the receptionist back to the search screen rather
     * than to an error page, because the search box is what they need next.
     */
    @GetMapping("/{patientCode}")
    public String view(@PathVariable String patientCode, Model model) {
        try {
            Patient patient = patientService.findByCode(patientCode);

            model.addAttribute("patient", PatientResponse.from(patient));
            model.addAttribute("history", appointmentService.findPatientHistory(patient.getPatientId())
                    .stream().map(AppointmentResponse::from).toList());

            return "patients/view";

        } catch (NotFoundException notFound) {
            model.addAttribute("errorMessage", notFound.getMessage());
            return "patients/search";
        }
    }

    private void addResults(Model model, List<Patient> found) {
        model.addAttribute("results", found.stream().map(PatientResponse::from).toList());
    }
}
