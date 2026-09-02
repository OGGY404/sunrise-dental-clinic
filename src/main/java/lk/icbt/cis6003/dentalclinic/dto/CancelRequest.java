package lk.icbt.cis6003.dentalclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the cancel button sends (FR7).
 *
 * WHY A REASON IS COMPULSORY
 * The reason is written into the appointment notes and copied into the audit
 * table by a trigger. A cancellation with no reason tells the clinic nothing
 * later, and the paper system this replaces failed for exactly that reason:
 * nobody could say why a slot had been given away.
 */
public class CancelRequest {

    @NotBlank(message = "Please say why the appointment is being cancelled.")
    @Size(max = 200, message = "The reason cannot be longer than 200 characters.")
    private String reason;

    public CancelRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
