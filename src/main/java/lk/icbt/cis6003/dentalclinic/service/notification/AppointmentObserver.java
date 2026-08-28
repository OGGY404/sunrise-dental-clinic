package lk.icbt.cis6003.dentalclinic.service.notification;

import lk.icbt.cis6003.dentalclinic.model.Appointment;

/**
 * Something that wants to be told when an appointment changes.
 *
 * DESIGN PATTERN: Observer.
 *
 * AppointmentService is the subject. It does not know or care who is listening.
 * When a booking is made it simply announces the fact, and whoever has
 * registered an interest reacts: one observer sends the patient a confirmation
 * email, another writes a line to the application log.
 *
 * WHY THIS RATHER THAN CALLING THE EMAIL SENDER DIRECTLY
 * Without the pattern, AppointmentService would have to import the mail sender,
 * the logger, and later the SMS sender, and would need editing every time the
 * clinic wants another kind of notification. It would also become impossible to
 * test the booking rules without a mail server. With it, the service depends on
 * this one small interface and nothing else.
 *
 * Spring finds every class that implements this interface and hands the whole
 * list to the service, so adding a third notification means writing one class
 * and changing nothing else.
 *
 * CRITICAL EVALUATION (for the report): the weakness of Observer is that the
 * flow of control becomes hard to follow. Reading AppointmentService.register()
 * does not tell you that an email is sent; you have to know to look for the
 * implementations. The gain is that the booking rules and the notifications can
 * be changed, and tested, entirely independently of one another.
 *
 * Default methods are used so an observer only has to write the events it
 * actually cares about.
 */
public interface AppointmentObserver {

    /** A new appointment has been booked and saved. */
    default void onAppointmentBooked(Appointment appointment) {
        // Nothing by default.
    }

    /** An appointment has been cancelled, and the slot is free again. */
    default void onAppointmentCancelled(Appointment appointment) {
        // Nothing by default.
    }

    /** An appointment has been moved to a new date or time. */
    default void onAppointmentRescheduled(Appointment appointment) {
        // Nothing by default.
    }
}
