package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.config.ClinicConfiguration;
import lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;
import lk.icbt.cis6003.dentalclinic.model.PaymentStatus;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.AppointmentRepository;
import lk.icbt.cis6003.dentalclinic.repository.BillRepository;
import lk.icbt.cis6003.dentalclinic.service.billing.BillCharge;
import lk.icbt.cis6003.dentalclinic.service.billing.BillingContext;
import lk.icbt.cis6003.dentalclinic.service.billing.BillingStrategy;
import lk.icbt.cis6003.dentalclinic.service.billing.BillingStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Producing and settling bills (FR4 - calculate and print the bill).
 *
 * BUSINESS LOGIC TIER.
 *
 * This class does not contain any arithmetic about prices. It asks the factory
 * which rule applies, lets the rule work out the amounts, and then records the
 * result. That is what keeps it short and what lets the clinic change its
 * pricing without this class being touched.
 */
@Service
@Transactional
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillingStrategyFactory billingStrategyFactory;
    private final ReferenceNumberGenerator numberGenerator;
    private final ClinicConfiguration clinicConfiguration;

    public BillingService(BillRepository billRepository,
                          AppointmentRepository appointmentRepository,
                          BillingStrategyFactory billingStrategyFactory,
                          ReferenceNumberGenerator numberGenerator,
                          ClinicConfiguration clinicConfiguration) {
        this.billRepository = billRepository;
        this.appointmentRepository = appointmentRepository;
        this.billingStrategyFactory = billingStrategyFactory;
        this.numberGenerator = numberGenerator;
        this.clinicConfiguration = clinicConfiguration;
    }

    /**
     * Works out and saves the bill for a completed visit.
     *
     * @param appointmentNo  the visit being billed
     * @param manualDiscount an extra reduction the clinic has agreed, or null
     */
    public Bill generateBill(String appointmentNo, BigDecimal manualDiscount) {
        return generateBill(appointmentNo, manualDiscount, null);
    }

    /** As above, and also records which member of staff produced the bill. */
    public Bill generateBill(String appointmentNo, BigDecimal manualDiscount, User issuedBy) {
        Appointment appointment = appointmentRepository.findByAppointmentNo(appointmentNo)
                .orElseThrow(() -> NotFoundException.of("appointment", appointmentNo));

        checkTheVisitCanBeBilled(appointment);
        checkItHasNotBeenBilledAlready(appointment);

        // DESIGN PATTERN: Factory chooses the rule, Strategy applies it.
        BillingStrategy strategy = billingStrategyFactory.strategyFor(appointment.getTreatment());
        BillingContext context = new BillingContext(
                appointment.getTreatment(),
                clinicConfiguration.getConsultationFee(),
                countCompletedVisits(appointment));

        BillCharge charge = strategy.calculate(context);
        BigDecimal totalDiscount = addManualDiscount(charge, manualDiscount);

        Bill bill = new Bill();
        bill.setBillNo(numberGenerator.nextBillNo());
        bill.setAppointment(appointment);
        bill.setTreatmentCost(charge.getTreatmentCost());
        bill.setConsultationFee(charge.getConsultationFee());
        bill.setDiscount(totalDiscount);
        bill.setPaymentStatus(PaymentStatus.UNPAID);
        bill.setIssuedBy(issuedBy);

        Bill saved = billRepository.save(bill);
        log.info("Issued bill {} for appointment {} using the {} rule",
                saved.getBillNo(), appointmentNo, charge.getStrategyName());

        return saved;
    }

    /** Records that the patient has paid, and how. */
    public Bill markPaid(String billNo, PaymentMethod method) {
        Bill bill = findByBillNo(billNo);

        if (bill.isPaid()) {
            throw new BusinessRuleException(
                    "Bill " + billNo + " has already been paid, so it cannot be paid again.");
        }
        if (method == null) {
            throw new BusinessRuleException("Please record how the bill was paid.");
        }

        bill.markPaid(method);
        log.info("Bill {} paid by {}", billNo, method);
        return billRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public Bill findByBillNo(String billNo) {
        return billRepository.findByBillNo(billNo)
                .orElseThrow(() -> NotFoundException.of("bill", billNo));
    }

    /** The bill for a visit, for the "print receipt" button on the details screen. */
    @Transactional(readOnly = true)
    public Bill findByAppointmentNo(String appointmentNo) {
        Appointment appointment = appointmentRepository.findByAppointmentNo(appointmentNo)
                .orElseThrow(() -> NotFoundException.of("appointment", appointmentNo));

        return billRepository.findByAppointmentAppointmentId(appointment.getAppointmentId())
                .orElseThrow(() -> new NotFoundException(
                        "Appointment " + appointmentNo + " has not been billed yet."));
    }

    /** The chase list for the front desk. */
    @Transactional(readOnly = true)
    public List<Bill> findUnpaidBills() {
        return billRepository.findByPaymentStatusOrderByIssuedAtAsc(PaymentStatus.UNPAID);
    }

    // --- the rules -----------------------------------------------------------

    /**
     * Only a visit that actually happened may be billed.
     *
     * Billing a booking that has not taken place yet would let the clinic take
     * money for work it has not done. Billing a cancelled or missed visit would
     * charge a patient who was never treated.
     */
    private void checkTheVisitCanBeBilled(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            return;
        }
        if (appointment.getStatus() == AppointmentStatus.BOOKED) {
            throw new BusinessRuleException("Appointment " + appointment.getAppointmentNo()
                    + " has not taken place yet. Mark it as completed before producing a bill.");
        }
        throw new BusinessRuleException("Appointment " + appointment.getAppointmentNo()
                + " is " + appointment.getStatus() + ", so it cannot be billed.");
    }

    /**
     * One visit, one bill.
     *
     * The database enforces this too, with a UNIQUE key on
     * bills.appointment_id. Checking here first turns a database error into a
     * sentence the receptionist can understand.
     */
    private void checkItHasNotBeenBilledAlready(Appointment appointment) {
        boolean billed = billRepository
                .findByAppointmentAppointmentId(appointment.getAppointmentId())
                .isPresent();

        if (billed) {
            throw new BusinessRuleException("Appointment " + appointment.getAppointmentNo()
                    + " has already been billed. Open the existing bill instead of making a second one.");
        }
    }

    /**
     * Adds any discount the clinic has agreed by hand to the one the pricing
     * rule already worked out.
     *
     * A negative figure is refused, because it would be a price rise disguised
     * as a discount and would not appear anywhere on the receipt. A discount
     * larger than the bill is refused too, since the clinic does not pay
     * patients to be treated.
     */
    private BigDecimal addManualDiscount(BillCharge charge, BigDecimal manualDiscount) {
        if (manualDiscount == null || manualDiscount.compareTo(BigDecimal.ZERO) == 0) {
            return charge.getDiscount();
        }
        if (manualDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("A discount cannot be a negative amount.");
        }

        BigDecimal beforeDiscount = charge.getTreatmentCost().add(charge.getConsultationFee());
        BigDecimal total = charge.getDiscount().add(manualDiscount);

        if (total.compareTo(beforeDiscount) > 0) {
            throw new BusinessRuleException("The discount cannot be more than the bill itself, which is "
                    + clinicConfiguration.getCurrency() + " " + beforeDiscount + ".");
        }
        return total;
    }

    /**
     * How many visits this patient has completed, which decides the loyalty
     * discount.
     *
     * The visit being billed counts towards the total, so the discount begins
     * on the fifth completed visit rather than the sixth. That is the clinic
     * rule, and it is the friendlier reading of "your fifth visit is
     * discounted".
     */
    private int countCompletedVisits(Appointment appointment) {
        Long patientId = appointment.getPatient().getPatientId();
        if (patientId == null) {
            return 0;
        }

        List<Appointment> history = appointmentRepository
                .findByPatientPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId);

        return (int) history.stream()
                .filter(visit -> visit.getStatus() == AppointmentStatus.COMPLETED)
                .count();
    }
}
