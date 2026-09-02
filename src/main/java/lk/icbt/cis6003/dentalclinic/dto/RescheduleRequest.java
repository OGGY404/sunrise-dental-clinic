package lk.icbt.cis6003.dentalclinic.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The new day and time a visit is being moved to (FR7).
 *
 * Only the date and the time can change. The patient, the dentist and the
 * treatment stay as they were, because moving a booking is not the same as
 * making a different one. If any of those must change, the visit is cancelled
 * and a new appointment is registered, which keeps both events in the history.
 */
public class RescheduleRequest {

    @NotNull(message = "Please choose the new date.")
    @FutureOrPresent(message = "An appointment cannot be moved into the past.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate newDate;

    @NotNull(message = "Please choose the new time.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime newTime;

    public RescheduleRequest() {
    }

    public LocalDate getNewDate() {
        return newDate;
    }

    public void setNewDate(LocalDate newDate) {
        this.newDate = newDate;
    }

    public LocalTime getNewTime() {
        return newTime;
    }

    public void setNewTime(LocalTime newTime) {
        this.newTime = newTime;
    }
}
