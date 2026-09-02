package lk.icbt.cis6003.dentalclinic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * What the "produce the bill" button sends (FR4).
 *
 * The amounts are not sent from the screen. Only the appointment number and an
 * optional agreed discount are, and the business tier works the rest out from
 * the price list. If the screen could send the price, anyone who can reach the
 * web service could decide what a filling costs.
 */
public class BillRequest {

    @NotBlank(message = "Please give the appointment number to bill.")
    private String appointmentNo;

    /**
     * An extra reduction the clinic has agreed, on top of any the pricing rule
     * already applies. Left empty when there is none.
     *
     * A negative figure is refused here, because it would be a price rise
     * disguised as a discount and would show nowhere on the receipt.
     */
    @DecimalMin(value = "0.00", message = "A discount cannot be a negative amount.")
    @Digits(integer = 8, fraction = 2, message = "A discount can have at most two decimal places.")
    private BigDecimal discount;

    public BillRequest() {
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }
}
