package lk.icbt.cis6003.dentalclinic.service.notification;

import lk.icbt.cis6003.dentalclinic.config.ClinicConfiguration;
import lk.icbt.cis6003.dentalclinic.dto.UpcomingReminderRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Reminds a patient by email that they are due in (FR7).
 *
 * Email is switched off until real credentials are supplied, and while it is
 * off this class logs what it would have sent. That is deliberate: a system
 * that silently does nothing looks identical to one that is broken, and the
 * clinic would find out only when patients stopped arriving.
 *
 * NOTHING HERE MAY THROW
 * A reminder is a courtesy. A mail server being unreachable must not stop the
 * rest of the round, so every send is wrapped and a failure is logged and
 * swallowed. ReminderService guards this as well, which makes two.
 */
@Component
public class EmailReminderNotifier implements ReminderNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailReminderNotifier.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private final JavaMailSender mailSender;
    private final ClinicConfiguration clinicConfiguration;
    private final boolean enabled;

    public EmailReminderNotifier(JavaMailSender mailSender,
                                 ClinicConfiguration clinicConfiguration,
                                 @Value("${clinic.notifications.email.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.clinicConfiguration = clinicConfiguration;
        this.enabled = enabled;
    }

    @Override
    public void remind(UpcomingReminderRow reminder) {
        if (!reminder.canBeEmailed()) {
            // Not every patient gives an email address. They are counted
            // separately in the summary so somebody telephones them instead.
            return;
        }

        String subject = "Reminder: your appointment at " + clinicConfiguration.getClinicName();

        String body = "Dear " + reminder.patientName() + ",\n\n"
                + "This is a reminder about your appointment.\n\n"
                + "Reference: " + reminder.appointmentNo() + "\n"
                + "Date:      " + reminder.appointmentDate().format(DATE_FORMAT) + "\n"
                + "Time:      " + reminder.appointmentTime().format(TIME_FORMAT) + "\n"
                + "Dentist:   " + reminder.dentistName() + "\n"
                + "Treatment: " + reminder.treatmentName() + "\n\n"
                + "Please arrive ten minutes early. If you cannot come, telephone "
                + clinicConfiguration.getClinicPhone() + " so the time can be offered "
                + "to another patient.\n\n"
                + clinicConfiguration.getClinicName();

        if (!enabled) {
            log.info("Email is switched off. Would have reminded {} at {} about {}",
                    reminder.patientName(), reminder.patientEmail(), reminder.appointmentNo());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(reminder.patientEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            log.info("Reminded {} about {}", reminder.patientEmail(), reminder.appointmentNo());

        } catch (RuntimeException problem) {
            // Swallowed on purpose. See the note at the top of this class.
            log.warn("Could not remind {} about {}. The appointment is unaffected. Reason: {}",
                    reminder.patientEmail(), reminder.appointmentNo(), problem.getMessage());
        }
    }
}
