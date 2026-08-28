package lk.icbt.cis6003.dentalclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The bill for one visit (FR4 - calculate and print the bill).
 *
 * WHY THE AMOUNTS ARE COPIED ONTO THE BILL
 * The prices are written onto the bill rather than looked up from the price
 * list every time it is opened. If the clinic raises the price of a filling
 * next month, a receipt printed today must still show what the patient really
 * paid. This is an ordinary accounting rule, not a shortcut.
 */
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id")
    private Long billId;

    /** Receipt number, for example BIL-20260907-0003. */
    @Column(name = "bill_no", nullable = false, unique = true, length = 20)
    private String billNo;

    /** One appointment can only ever be billed once. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "treatment_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal treatmentCost;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * The total, worked out by MySQL itself as a generated column:
     * treatment_cost + consultation_fee - discount.
     *
     * insertable and updatable are false because Java must never write to it.
     * Letting the database own the sum means the total can never drift out of
     * step with the three numbers it is made from, no matter which program
     * wrote the row.
     */
    @Column(name = "total_amount", insertable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 10)
    private PaymentMethod paymentMethod;

    /** Which member of staff printed the bill. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Bill() {
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public void setTreatmentCost(BigDecimal treatmentCost) {
        this.treatmentCost = treatmentCost;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    /**
     * The amount the patient owes.
     *
     * Normally this is the value MySQL calculated in the generated column. The
     * fallback below works out the same sum in Java, and is needed for two
     * cases: a bill that has been built in memory but not saved yet, and the
     * H2 test database, which does not copy the MySQL generated column.
     */
    public BigDecimal getTotalAmount() {
        if (totalAmount != null) {
            return totalAmount;
        }
        if (treatmentCost == null || consultationFee == null) {
            return null;
        }
        BigDecimal off = (discount == null) ? BigDecimal.ZERO : discount;
        return treatmentCost.add(consultationFee).subtract(off);
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public User getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(User issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    /** Marks the bill settled and records when and how it was paid. */
    public void markPaid(PaymentMethod method) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentMethod = method;
        this.paidAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Bill)) {
            return false;
        }
        return Objects.equals(billNo, ((Bill) other).billNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billNo);
    }

    @Override
    public String toString() {
        return "Bill{no=" + billNo + ", total=" + getTotalAmount() + ", status=" + paymentStatus + "}";
    }
}
