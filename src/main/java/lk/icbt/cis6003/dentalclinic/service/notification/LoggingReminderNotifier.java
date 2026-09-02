package lk.icbt.cis6003.dentalclinic.service.notification;

import lk.icbt.cis6003.dentalclinic.dto.UpcomingReminderRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes every reminder to the log (FR7).
 *
 * WHY THIS EXISTS AT ALL
 * Email is switched off until the clinic supplies real credentials, so without
 * this the nightly round would leave no trace whatsoever. If a patient later
 * says "nobody told me", the clinic needs to be able to answer.
 *
 * It also logs the patients who have no email address, because those are the
 * ones somebody has to telephone, and that list is the useful half of the job.
 */
@Component
public class LoggingReminderNotifier implements ReminderNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingReminderNotifier.class);

    @Override
    public void remind(UpcomingReminderRow reminder) {
        if (reminder.canBeEmailed()) {
            log.info("Reminder due: {} at {} for {} with {} ({}) - emailing {}",
                    reminder.appointmentNo(),
                    reminder.appointmentTime(),
                    reminder.patientName(),
                    reminder.dentistName(),
                    reminder.treatmentName(),
                    reminder.patientEmail());
        } else {
            log.info("Reminder due: {} at {} for {} with {} ({}) - NO EMAIL, telephone {}",
                    reminder.appointmentNo(),
                    reminder.appointmentTime(),
                    reminder.patientName(),
                    reminder.dentistName(),
                    reminder.treatmentName(),
                    reminder.patientContact());
        }
    }
}
