package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Data access for dentists. DESIGN PATTERN: Repository. */
@Repository
public interface DentistRepository extends JpaRepository<Dentist, Long> {

    Optional<Dentist> findByDentistCode(String dentistCode);

    /**
     * The dentists the booking form may offer, in name order.
     * A dentist who has left is switched off rather than deleted, so this
     * filter is what keeps them out of the drop-down.
     */
    List<Dentist> findByActiveTrueOrderByFullNameAsc();

    List<Dentist> findBySpecialisationIgnoreCaseAndActiveTrue(String specialisation);
}
