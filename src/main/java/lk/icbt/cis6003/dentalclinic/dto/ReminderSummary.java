package lk.icbt.cis6003.dentalclinic.dto;

/**
 * What happened when a round of reminders was sent (FR7).
 *
 * WHY THE TWO COUNTS ARE KEPT APART
 * "Five reminders sent" would be a comfortable lie if two of those patients had
 * no email address. Splitting the figure means the receptionist can see at a
 * glance how many people still have to be telephoned, which is the only part of
 * the job a computer cannot finish.
 */
public record ReminderSummary(

        /** How many days ahead this round covered. 1 is tomorrow. */
        int daysAhead,

        /** How many appointments are booked for that day. */
        int found,

        /** How many of those patients had an email address. */
        int emailed,

        /** How many have to be telephoned instead. */
        int needTelephoning) {

    /** A sentence the screen can show without assembling one itself. */
    public String describe() {
        if (found == 0) {
            return "There are no appointments booked for that day, so no reminders were needed.";
        }
        if (needTelephoning == 0) {
            return "Reminders sent to all " + emailed + " patient(s) by email.";
        }
        if (emailed == 0) {
            return "None of the " + found + " patient(s) has an email address. "
                    + "They all need to be telephoned.";
        }
        return "Reminders sent to " + emailed + " patient(s) by email. "
                + needTelephoning + " have no email address and need to be telephoned.";
    }
}
