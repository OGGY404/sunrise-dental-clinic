package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.dto.AppointmentResponse;
import lk.icbt.cis6003.dentalclinic.dto.PatientResponse;
import lk.icbt.cis6003.dentalclinic.exception.BadRequestException;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The patient web service (FR2 patient details, FR7 treatment history).
 *
 * PRESENTATION TIER.
 *
 * Patients are addressed by their patient code, such as PAT-000042, and never
 * by the database id. The code is what staff read out and write on a card; the
 * id is an internal detail that nothing outside the system should depend on.
 */
@RestController
@RequestMapping("/api/patients")
public class PatientRestController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientRestController(PatientService patientService,
                                 AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    /** One patient record, found by the code on their card. */
    @GetMapping("/{patientCode}")
    public PatientResponse findOne(@PathVariable String patientCode) {
        return PatientResponse.from(patientService.findByCode(patientCode));
    }

    /**
     * Search, either by part of the name or by telephone number.
     *
     * Telephone is checked first, because it is the reliable one. A name can be
     * spelled several ways and shared by two people; a number is typed off the
     * patient's phone and matches exactly.
     *
     * An empty search is refused rather than answered with the whole patient
     * list. Returning every patient in the clinic to a mistyped search would be
     * both slow and a needless spread of personal data.
     */
    @GetMapping
    public List<PatientResponse> search(@RequestParam(required = false) String name,
                                        @RequestParam(required = false) String contact) {

        if (contact != null && !contact.isBlank()) {
            return toResponses(patientService.searchByContactNumber(contact.trim()));
        }
        if (name != null && !name.isBlank()) {
            return toResponses(patientService.searchByName(name.trim()));
        }
        throw new BadRequestException(
                "Please search by a patient name or by a telephone number.");
    }

    /**
     * FR7 - everything this patient has ever had done, newest visit first.
     *
     * Two business calls are made here, one to find the patient and one to read
     * their visits. That orchestration is a presentation-tier job: it is the
     * screen that wants both, and neither service needs to know about the
     * other.
     */
    @GetMapping("/{patientCode}/history")
    public List<AppointmentResponse> history(@PathVariable String patientCode) {
        Patient patient = patientService.findByCode(patientCode);

        return appointmentService.findPatientHistory(patient.getPatientId())
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    private List<PatientResponse> toResponses(List<Patient> patients) {
        return patients.stream().map(PatientResponse::from).toList();
    }
}
