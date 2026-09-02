package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Data access for the price list (FR4). DESIGN PATTERN: Repository. */
@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    Optional<Treatment> findByTreatmentCode(String treatmentCode);

    /** The treatments the clinic still offers, for the booking form. */
    List<Treatment> findByActiveTrueOrderByNameAsc();

    Optional<Treatment> findByNameIgnoreCase(String name);
}
