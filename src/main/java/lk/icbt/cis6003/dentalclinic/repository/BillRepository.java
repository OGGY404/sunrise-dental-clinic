package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Data access for bills (FR4). DESIGN PATTERN: Repository. */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNo(String billNo);

    /**
     * The bill for one visit. Optional, because a visit that has not been
     * billed yet simply has no bill.
     */
    Optional<Bill> findByAppointmentAppointmentId(Long appointmentId);

    /** The chase list for the front desk, oldest debt first. */
    List<Bill> findByPaymentStatusOrderByIssuedAtAsc(PaymentStatus paymentStatus);

    /** Revenue report: every bill issued between two moments in time. */
    List<Bill> findByIssuedAtBetweenOrderByIssuedAtAsc(LocalDateTime from, LocalDateTime to);

    boolean existsByAppointmentAppointmentId(Long appointmentId);
}
