package lk.icbt.cis6003.dentalclinic.service.notification;

import lk.icbt.cis6003.dentalclinic.dto.UpcomingReminderRow;

/**
 * Something that can remind a patient about an appointment that is coming up.
 *
 * The same shape as AppointmentObserver, and for the same reason: ReminderService
 * announces that a patient is due in, without knowing or caring who is
 * listening. Spring finds every implementation and hands them over, so adding
 * SMS reminders later means writing one class and changing nothing else.
 *
 * IMPLEMENTATIONS MUST NOT THROW
 * ReminderService guards each call anyway, but an implementation that swallows
 * its own failures gives a better log message, because it knows what it was
 * trying to do. This is the same two-guard arrangement the booking observers
 * use.
 */
public interface ReminderNotifier {

    /**
     * Remind one patient.
     *
     * @param reminder who is due in, when, and how to reach them
     */
    void remind(UpcomingReminderRow reminder);
}
