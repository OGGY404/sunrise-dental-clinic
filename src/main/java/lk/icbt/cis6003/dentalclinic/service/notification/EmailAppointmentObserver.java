package lk.icbt.cis6003.dentalclinic.service.notification;

import lk.icbt.cis6003.dentalclinic.config.ClinicConfiguration;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Emails the patient when their appointment is booked, moved or cancelled (FR7).
 *
 * DESIGN PATTERN: Observer (a concrete observer).
 *
 * SWITCHED OFF BY DEFAULT
 * clinic.notifications.email.enabled is false unless real mail credentials are
 * supplied. With it off, the message that would have been sent is written to
 * the log instead. That means the system can be demonstrated and marked without
 * a mail account, and without accidentally emailing the sample patients.
 *
 * NOTHING HERE MAY THROW
 * A failure to send an email must never undo a booking the patient is
 * expecting. Every send is wrapped, and a failure is logged and swallowed. The
 * AppointmentService also protects itself, so this is the second of two guards.
 */
@Component
public class EmailAppointmentObserver implements AppointmentObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailAppointmentObserver.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private final JavaMailSender mailSender;
    private final ClinicConfiguration clinicConfiguration;
    private final boolean enabled;

    public EmailAppointmentObserver(JavaMailSender mailSender,
                                    ClinicConfiguration clinicConfiguration,
                                    @Value("${clinic.notifications.email.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.clinicConfiguration = clinicConfiguration;
        this.enabled = enabled;
    }

    @Override
    public void onAppointmentBooked(Appointment appointment) {
        send(appointment,
                "Your appointment at " + clinicConfiguration.getClinicName() + " is confirmed",
                "Dear " + appointment.getPatient().getFullName() + ",\n\n"
                        + "Your appointment is confirmed.\n\n"
                        + details(appointment)
                        + "\nPlease arrive ten minutes early. To change or cancel, telephone "
                        + clinicConfiguration.getClinicPhone() + ".\n\n"
                        + clinicConfiguration.getClinicName());
    }

    @Override
    public void onAppointmentRescheduled(Appointment appointment) {
        send(appointment,
                "Your appointment has been moved",
                "Dear " + appointment.getPatient().getFullName() + ",\n\n"
                        + "Your appointment has been moved to a new date and time.\n\n"
                        + details(appointment)
                        + "\n" + clinicConfiguration.getClinicName());
    }

    @Override
    public void onAppointmentCancelled(Appointment appointment) {
        send(appointment,
                "Your appointment has been cancelled",
                "Dear " + appointment.getPatient().getFullName() + ",\n\n"
                        + "Your appointment " + appointment.getAppointmentNo() + " on "
                        + appointment.getAppointmentDate().format(DATE_FORMAT)
                        + " has been cancelled.\n\n"
                        + "To book again, telephone " + clinicConfiguration.getClinicPhone() + ".\n\n"
                        + clinicConfiguration.getClinicName());
    }

    /** The block of appointment details that appears in every message. */
    private String details(Appointment appointment) {
        return "Reference: " + appointment.getAppointmentNo() + "\n"
                + "Date:      " + appointment.getAppointmentDate().format(DATE_FORMAT) + "\n"
                + "Time:      " + appointment.getAppointmentTime().format(TIME_FORMAT) + "\n"
                + "Dentist:   " + appointment.getDentist().getFullName() + "\n"
                + "Treatment: " + appointment.getTreatment().getName() + "\n";
    }

    private void send(Appointment appointment, String subject, String body) {
        String address = appointment.getPatient().getEmail();

        if (address == null || address.isBlank()) {
            // Not every patient gives an email address, and that is fine.
            return;
        }

        if (!enabled) {
            log.info("Email is switched off. Would have sent to {}: {}", address, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(address);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent \"{}\" to {}", subject, address);
        } catch (RuntimeException problem) {
            // Swallowed on purpose. See the note at the top of this class.
            log.warn("Could not email {} about {}. The appointment itself is unaffected. Reason: {}",
                    address, appointment.getAppointmentNo(), problem.getMessage());
        }
    }
}
