package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.dto.DentistResponse;
import lk.icbt.cis6003.dentalclinic.dto.TreatmentResponse;
import lk.icbt.cis6003.dentalclinic.service.ReferenceDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The two lists the booking form fills its dropdowns from.
 *
 * PRESENTATION TIER.
 *
 * These are separate from the appointment web service on purpose. They are read
 * often and change rarely, and nothing about them belongs to one appointment.
 */
@RestController
@RequestMapping("/api")
public class ReferenceDataRestController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataRestController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    /** The dentists who can still be booked. */
    @GetMapping("/dentists")
    public List<DentistResponse> dentists() {
        return referenceDataService.bookableDentists().stream()
                .map(DentistResponse::from)
                .toList();
    }

    /** The current price list. */
    @GetMapping("/treatments")
    public List<TreatmentResponse> treatments() {
        return referenceDataService.bookableTreatments().stream()
                .map(TreatmentResponse::from)
                .toList();
    }
}
