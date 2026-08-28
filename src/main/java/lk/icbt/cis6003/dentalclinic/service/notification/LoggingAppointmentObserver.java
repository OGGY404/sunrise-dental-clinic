package lk.icbt.cis6003.dentalclinic.service.notification;

import lk.icbt.cis6003.dentalclinic.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes a line to the application log whenever an appointment changes.
 *
 * DESIGN PATTERN: Observer (a concrete observer).
 *
 * This is not the audit trail. The audit trail is written by the database
 * triggers into appointment_audit and is the record the clinic can rely on.
 * This log is for the developer and for the demonstration: it shows, in the
 * console, that the Observer pattern really is firing.
 */
@Component
public class LoggingAppointmentObserver implements AppointmentObserver {

    private static final Logger log = LoggerFactory.getLogger(LoggingAppointmentObserver.class);

    @Override
    public void onAppointmentBooked(Appointment appointment) {
        log.info("BOOKED  {} for {} with {} on {} at {}",
                appointment.getAppointmentNo(),
                appointment.getPatient().getFullName(),
                appointment.getDentist().getFullName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime());
    }

    @Override
    public void onAppointmentCancelled(Appointment appointment) {
        log.info("CANCELLED {} - the slot on {} at {} is free again",
                appointment.getAppointmentNo(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime());
    }

    @Override
    public void onAppointmentRescheduled(Appointment appointment) {
        log.info("MOVED   {} to {} at {}",
                appointment.getAppointmentNo(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime());
    }
}
