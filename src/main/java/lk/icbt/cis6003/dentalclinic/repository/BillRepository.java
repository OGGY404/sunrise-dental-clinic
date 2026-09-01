package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access for bills (FR4). DESIGN PATTERN: Repository.
 *
 * A receipt has to print the patient name and the treatment name, and both of
 * those live two steps away, through the appointment. The @EntityGraph below
 * fetches that whole chain in one query, so the receipt can still be built
 * after the transaction has closed.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    @EntityGraph(attributePaths = {"appointment", "appointment.patient", "appointment.treatment", "appointment.dentist"})
    Optional<Bill> findByBillNo(String billNo);

    /**
     * The bill for one visit. Optional, because a visit that has not been
     * billed yet simply has no bill.
     */
    @EntityGraph(attributePaths = {"appointment", "appointment.patient", "appointment.treatment", "appointment.dentist"})
    Optional<Bill> findByAppointmentAppointmentId(Long appointmentId);

    /** The chase list for the front desk, oldest debt first. */
    @EntityGraph(attributePaths = {"appointment", "appointment.patient", "appointment.treatment", "appointment.dentist"})
    List<Bill> findByPaymentStatusOrderByIssuedAtAsc(PaymentStatus paymentStatus);

    /** Revenue report: every bill issued between two moments in time. */
    @EntityGraph(attributePaths = {"appointment", "appointment.patient", "appointment.treatment", "appointment.dentist"})
    List<Bill> findByIssuedAtBetweenOrderByIssuedAtAsc(LocalDateTime from, LocalDateTime to);

    boolean existsByAppointmentAppointmentId(Long appointmentId);
}
