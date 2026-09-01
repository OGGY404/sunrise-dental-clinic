package lk.icbt.cis6003.dentalclinic.dto;

import java.math.BigDecimal;

/**
 * One line of the dentist workload report: how busy one dentist was over a date
 * range, and what that work brought in.
 *
 * Cancellations and no-shows are counted separately from completed visits on
 * purpose. A dentist with many no-shows is not an idle dentist; it is a
 * reminder problem, and the clinic can only see the difference if the two are
 * not added together.
 */
public record DentistWorkloadRow(

        String dentistCode,
        String dentistName,
        String specialisation,

        int totalAppointments,
        int completed,
        int cancelled,
        int noShows,

        /** Chair time booked, from the duration of each treatment. */
        int totalMinutesBooked,

        /** From paid bills only. */
        BigDecimal revenueGenerated) {

    /**
     * The share of this dentist's booked visits that actually happened, as a
     * percentage, or null when there were no bookings at all.
     *
     * Worked out here rather than in the report page, so the page only has to
     * display it and the rule lives in one place.
     */
    public Integer attendanceRate() {
        if (totalAppointments == 0) {
            return null;
        }
        return Math.round((completed * 100f) / totalAppointments);
    }
}
