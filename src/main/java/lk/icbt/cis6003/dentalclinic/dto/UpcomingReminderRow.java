package lk.icbt.cis6003.dentalclinic.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One patient who is due in, and how the clinic can reach them (FR7 -
 * appointment reminders).
 *
 * This is one row of the answer from the stored procedure
 * sp_report_upcoming_reminders. It is not a table and it is never saved, so it
 * is a record: the fields are final and there is no setter for anyone to call
 * by mistake.
 *
 * Both the email address and the telephone number are carried, because a
 * reminder that cannot be emailed still has to be made by somebody, and the
 * receptionist needs the number in front of them to do it.
 */
public record UpcomingReminderRow(

        String appointmentNo,
        LocalDate appointmentDate,
        LocalTime appointmentTime,

        String patientName,
        String patientContact,

        /** Optional. Not every patient gives one. */
        String patientEmail,

        String dentistName,
        String treatmentName) {

    /**
     * Whether this patient can be reminded without anyone picking up a
     * telephone.
     *
     * A blank string counts as no address, not as an address. An empty box on
     * the booking form arrives here as "" rather than null, and treating those
     * two differently would mean half the patients with no email were counted
     * as reachable.
     */
    public boolean canBeEmailed() {
        return patientEmail != null && !patientEmail.isBlank();
    }
}
