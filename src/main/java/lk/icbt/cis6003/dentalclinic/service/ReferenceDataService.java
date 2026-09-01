package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import lk.icbt.cis6003.dentalclinic.repository.DentistRepository;
import lk.icbt.cis6003.dentalclinic.repository.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The two lists the booking form needs: who can be booked, and what can be
 * booked.
 *
 * BUSINESS LOGIC TIER.
 *
 * WHY THIS CLASS EXISTS AT ALL, WHEN IT ONLY CALLS THE REPOSITORY
 * The three-tier rule says the presentation tier talks to the business tier and
 * the business tier talks to the data tier. A controller reaching straight into
 * a repository would break that, and would also be the place where somebody
 * later adds "and hide the dentists who are on leave" to a controller.
 *
 * The word "bookable" is the point. It is not "all dentists": a dentist who has
 * left the clinic and a treatment taken off the price list must never appear in
 * a dropdown, because the booking service would then refuse the booking that
 * the screen had just offered.
 */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final DentistRepository dentistRepository;
    private final TreatmentRepository treatmentRepository;

    public ReferenceDataService(DentistRepository dentistRepository,
                                TreatmentRepository treatmentRepository) {
        this.dentistRepository = dentistRepository;
        this.treatmentRepository = treatmentRepository;
    }

    /** The dentists still practising here, in name order. */
    public List<Dentist> bookableDentists() {
        return dentistRepository.findByActiveTrueOrderByFullNameAsc();
    }

    /** The current price list, in name order. */
    public List<Treatment> bookableTreatments() {
        return treatmentRepository.findByActiveTrueOrderByNameAsc();
    }
}
