package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.config.ClinicConfiguration;
import lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;
import lk.icbt.cis6003.dentalclinic.model.PaymentStatus;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import lk.icbt.cis6003.dentalclinic.repository.AppointmentRepository;
import lk.icbt.cis6003.dentalclinic.repository.BillRepository;
import lk.icbt.cis6003.dentalclinic.service.billing.BillingStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for producing a bill (FR4).
 *
 * The real BillingStrategyFactory is used here rather than a mock, because the
 * point of these tests is that the service and the pricing rules agree with
 * each other. Only the repositories are replaced.
 */
@DisplayName("BillingService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingServiceTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ReferenceNumberGenerator numberGenerator;
    @Mock
    private ClinicConfiguration clinicConfiguration;

    private BillingService service;

    private Patient kamal;
    private Dentist drPerera;

    @BeforeEach
    void setUp() {
        kamal = new Patient("PAT-000001", "Kamal Silva", "No. 42, Galle Road", "0771234567");
        kamal.setPatientId(10L);
        drPerera = new Dentist("DEN-001", "Dr. Nimal Perera", "General Dentistry");
        drPerera.setDentistId(1L);

        when(clinicConfiguration.getConsultationFee()).thenReturn(new BigDecimal("1500.00"));
        when(numberGenerator.nextBillNo()).thenReturn("BIL-20260907-0001");
        when(billRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(appointmentRepository.countByDentistDentistIdAndAppointmentDateAndStatusNot(
                anyLong(), any(), any())).thenReturn(0L);

        service = new BillingService(
                billRepository,
                appointmentRepository,
                new BillingStrategyFactory(),
                numberGenerator,
                clinicConfiguration);
    }

    private Treatment treatment(String code, String name, String cost) {
        Treatment t = new Treatment(code, name, new BigDecimal(cost), 45);
        t.setTreatmentId(3L);
        t.setActive(true);
        return t;
    }

    private Appointment completedVisit(Treatment treatment) {
        return Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(kamal)
                .dentist(drPerera)
                .treatment(treatment)
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();
    }

    private void visitExists(Appointment appointment) {
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(appointment));
        when(billRepository.findByAppointmentAppointmentId(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("bills an ordinary treatment as cost plus consultation fee (FR4)")
    void billsAnOrdinaryTreatment() {
        visitExists(completedVisit(treatment("TRT-003", "Tooth Filling", "6000.00")));

        Bill bill = service.generateBill("APT-20260907-0001", null);

        assertThat(bill.getBillNo()).isEqualTo("BIL-20260907-0001");
        assertThat(bill.getTreatmentCost()).isEqualByComparingTo("6000.00");
        assertThat(bill.getConsultationFee()).isEqualByComparingTo("1500.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("7500.00");
        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    @DisplayName("uses the surgical rule for an extraction, so the supplement is charged")
    void usesTheSurgicalRule() {
        visitExists(completedVisit(treatment("TRT-004", "Tooth Extraction", "5500.00")));

        Bill bill = service.generateBill("APT-20260907-0001", null);

        // 5500 + 15% = 6325
        assertThat(bill.getTreatmentCost()).isEqualByComparingTo("6325.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("7825.00");
    }

    @Test
    @DisplayName("uses the consultation-only rule for a check-up, so the fee is not doubled")
    void usesTheConsultationOnlyRule() {
        visitExists(completedVisit(treatment("TRT-001", "Dental Check-up", "2000.00")));

        Bill bill = service.generateBill("APT-20260907-0001", null);

        assertThat(bill.getConsultationFee()).isEqualByComparingTo("0.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("gives a loyal patient their discount")
    void appliesTheLoyaltyDiscount() {
        visitExists(completedVisit(treatment("TRT-003", "Tooth Filling", "6000.00")));
        when(appointmentRepository.countByDentistDentistIdAndAppointmentDateAndStatusNot(
                anyLong(), any(), any())).thenReturn(0L);
        when(billRepository.count()).thenReturn(0L);
        // Six previous completed visits makes this patient loyal.
        when(appointmentRepository.findByPatientPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(10L))
                .thenReturn(java.util.Collections.nCopies(6, completedVisit(
                        treatment("TRT-003", "Tooth Filling", "6000.00"))));

        Bill bill = service.generateBill("APT-20260907-0001", null);

        assertThat(bill.getDiscount()).isEqualByComparingTo("600.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("6900.00");
    }

    @Test
    @DisplayName("adds a manual discount on top of the calculated one")
    void addsAManualDiscount() {
        visitExists(completedVisit(treatment("TRT-003", "Tooth Filling", "6000.00")));

        Bill bill = service.generateBill("APT-20260907-0001", new BigDecimal("500.00"));

        assertThat(bill.getDiscount()).isEqualByComparingTo("500.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("7000.00");
    }

    @Test
    @DisplayName("refuses a manual discount larger than the bill")
    void refusesADiscountBiggerThanTheBill() {
        visitExists(completedVisit(treatment("TRT-003", "Tooth Filling", "6000.00")));

        assertThatThrownBy(() -> service.generateBill("APT-20260907-0001", new BigDecimal("99999.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("refuses a negative discount, which would be a hidden price rise")
    void refusesANegativeDiscount() {
        visitExists(completedVisit(treatment("TRT-003", "Tooth Filling", "6000.00")));

        assertThatThrownBy(() -> service.generateBill("APT-20260907-0001", new BigDecimal("-100.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("refuses to bill a visit that has not happened yet")
    void refusesToBillABookedVisit() {
        Appointment stillBooked = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(kamal)
                .dentist(drPerera)
                .treatment(treatment("TRT-003", "Tooth Filling", "6000.00"))
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 0))
                .status(AppointmentStatus.BOOKED)
                .build();
        visitExists(stillBooked);

        assertThatThrownBy(() -> service.generateBill("APT-20260907-0001", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completed");

        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to bill a cancelled visit")
    void refusesToBillACancelledVisit() {
        Appointment cancelled = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(kamal)
                .dentist(drPerera)
                .treatment(treatment("TRT-003", "Tooth Filling", "6000.00"))
                .on(LocalDate.of(2026, 9, 7))
                .at(LocalTime.of(9, 0))
                .status(AppointmentStatus.CANCELLED)
                .build();
        visitExists(cancelled);

        assertThatThrownBy(() -> service.generateBill("APT-20260907-0001", null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("refuses to bill the same visit twice")
    void refusesToBillTwice() {
        Appointment visit = completedVisit(treatment("TRT-003", "Tooth Filling", "6000.00"));
        when(appointmentRepository.findByAppointmentNo("APT-20260907-0001"))
                .thenReturn(Optional.of(visit));
        when(billRepository.findByAppointmentAppointmentId(any()))
                .thenReturn(Optional.of(new Bill()));

        assertThatThrownBy(() -> service.generateBill("APT-20260907-0001", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already");
    }

    @Test
    @DisplayName("says clearly when the appointment number does not exist")
    void reportsAnUnknownAppointment() {
        when(appointmentRepository.findByAppointmentNo(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateBill("APT-19990101-0001", null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("records how and when a bill was paid")
    void marksABillPaid() {
        Bill unpaid = new Bill();
        unpaid.setBillNo("BIL-20260907-0001");
        unpaid.setTreatmentCost(new BigDecimal("6000.00"));
        unpaid.setConsultationFee(new BigDecimal("1500.00"));
        when(billRepository.findByBillNo("BIL-20260907-0001")).thenReturn(Optional.of(unpaid));

        Bill paid = service.markPaid("BIL-20260907-0001", PaymentMethod.CASH);

        assertThat(paid.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paid.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(paid.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("refuses to take payment for a bill that is already paid")
    void refusesToPayTwice() {
        Bill alreadyPaid = new Bill();
        alreadyPaid.setBillNo("BIL-20260907-0001");
        alreadyPaid.setTreatmentCost(new BigDecimal("6000.00"));
        alreadyPaid.setConsultationFee(new BigDecimal("1500.00"));
        alreadyPaid.markPaid(PaymentMethod.CARD);
        when(billRepository.findByBillNo("BIL-20260907-0001")).thenReturn(Optional.of(alreadyPaid));

        assertThatThrownBy(() -> service.markPaid("BIL-20260907-0001", PaymentMethod.CASH))
                .isInstanceOf(BusinessRuleException.class);
    }
}
