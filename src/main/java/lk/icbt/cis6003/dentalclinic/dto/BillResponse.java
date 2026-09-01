package lk.icbt.cis6003.dentalclinic.dto;

import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;
import lk.icbt.cis6003.dentalclinic.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The receipt, as the screen and the printer see it (FR4).
 *
 * The three amounts are shown separately as well as the total, because a
 * patient asking "why is it 7500?" must be able to see the treatment cost, the
 * consultation fee and the discount that make it up. A single total would not
 * answer that question.
 */
public class BillResponse {

    private final String billNo;
    private final String appointmentNo;
    private final String patientName;
    private final String treatmentName;

    private final BigDecimal treatmentCost;
    private final BigDecimal consultationFee;
    private final BigDecimal discount;
    private final BigDecimal totalAmount;

    private final PaymentStatus paymentStatus;
    private final PaymentMethod paymentMethod;
    private final LocalDateTime issuedAt;
    private final LocalDateTime paidAt;

    private BillResponse(Bill bill) {
        this.billNo = bill.getBillNo();
        this.appointmentNo = bill.getAppointment().getAppointmentNo();
        this.patientName = bill.getAppointment().getPatient().getFullName();
        this.treatmentName = bill.getAppointment().getTreatment().getName();

        this.treatmentCost = bill.getTreatmentCost();
        this.consultationFee = bill.getConsultationFee();
        this.discount = bill.getDiscount();
        this.totalAmount = bill.getTotalAmount();

        this.paymentStatus = bill.getPaymentStatus();
        this.paymentMethod = bill.getPaymentMethod();
        this.issuedAt = bill.getIssuedAt();
        this.paidAt = bill.getPaidAt();
    }

    public static BillResponse from(Bill bill) {
        return new BillResponse(bill);
    }

    public String getBillNo() {
        return billNo;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
