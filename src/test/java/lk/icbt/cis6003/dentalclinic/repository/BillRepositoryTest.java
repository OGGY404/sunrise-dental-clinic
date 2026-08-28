package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;
import lk.icbt.cis6003.dentalclinic.model.PaymentStatus;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for bills (FR4 - calculate and print the bill).
 *
 * A bill copies the prices onto itself instead of looking them up later. If the
 * clinic raises the price of a filling next month, an old receipt must still
 * show what the patient actually paid.
 */
@DisplayName("BillRepository")
class BillRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private BillRepository billRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DentistRepository dentistRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;

    private Appointment visit;
    private Appointment secondVisit;

    @BeforeEach
    void setUpAVisit() {
        Patient kamal = patientRepository.save(newPatient("PAT-000001", "Kamal Silva", "0771234567"));
        Dentist drPerera = dentistRepository.save(newDentist("DEN-001", "Dr. Nimal Perera"));
        Treatment filling = treatmentRepository.save(newTreatment("TRT-003", "Tooth Filling", "6000.00"));

        visit = appointmentRepository.save(newAppointment(
                "APT-20260907-0001", kamal, drPerera, filling,
                LocalDate.of(2026, 9, 7), LocalTime.of(9, 0)));
        secondVisit = appointmentRepository.save(newAppointment(
                "APT-20260907-0002", kamal, drPerera, filling,
                LocalDate.of(2026, 9, 7), LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("finds a bill by its bill number")
    void findsBillByNumber() {
        billRepository.save(newBill("BIL-20260907-0001", visit, "6000.00"));

        Bill found = billRepository.findByBillNo("BIL-20260907-0001").orElseThrow();

        assertThat(found.getAppointment().getAppointmentNo()).isEqualTo("APT-20260907-0001");
        assertThat(found.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    @DisplayName("adds the treatment cost and the consultation fee together (FR4)")
    void totalIsTreatmentCostPlusConsultationFee() {
        Bill saved = billRepository.saveAndFlush(newBill("BIL-20260907-0001", visit, "6000.00"));

        // 6000 for the filling + 1500 consultation - 0 discount = 7500
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("7500.00"));
    }

    @Test
    @DisplayName("takes a discount off the total")
    void subtractsDiscountFromTotal() {
        Bill discounted = newBill("BIL-20260907-0002", visit, "6000.00");
        discounted.setDiscount(new BigDecimal("500.00"));

        Bill saved = billRepository.saveAndFlush(discounted);

        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("finds the bill that belongs to one appointment")
    void findsBillForAnAppointment() {
        billRepository.save(newBill("BIL-20260907-0001", visit, "6000.00"));

        assertThat(billRepository.findByAppointmentAppointmentId(visit.getAppointmentId()))
                .isPresent()
                .get()
                .extracting(Bill::getBillNo)
                .isEqualTo("BIL-20260907-0001");
    }

    @Test
    @DisplayName("refuses to bill the same appointment twice")
    void rejectsSecondBillForSameAppointment() {
        billRepository.saveAndFlush(newBill("BIL-20260907-0001", visit, "6000.00"));

        assertThatThrownBy(() ->
                billRepository.saveAndFlush(newBill("BIL-20260907-0002", visit, "6000.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("lists the unpaid bills, so the front desk can chase them")
    void listsUnpaidBills() {
        billRepository.save(newBill("BIL-20260907-0001", visit, "6000.00"));

        Bill paid = newBill("BIL-20260907-0002", secondVisit, "2000.00");
        paid.setPaymentStatus(PaymentStatus.PAID);
        paid.setPaymentMethod(PaymentMethod.CASH);
        billRepository.save(paid);

        List<Bill> unpaid = billRepository.findByPaymentStatusOrderByIssuedAtAsc(PaymentStatus.UNPAID);

        assertThat(unpaid)
                .extracting(Bill::getBillNo)
                .containsExactly("BIL-20260907-0001");
    }

    @Test
    @DisplayName("records when a bill was issued, so revenue can be reported by date")
    void recordsIssuedAt() {
        Bill saved = billRepository.saveAndFlush(newBill("BIL-20260907-0001", visit, "6000.00"));

        assertThat(saved.getIssuedAt()).isNotNull();
    }
}
