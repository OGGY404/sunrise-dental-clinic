package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.dto.ReminderSummary;
import lk.icbt.cis6003.dentalclinic.dto.UpcomingReminderRow;
import lk.icbt.cis6003.dentalclinic.exception.BadRequestException;
import lk.icbt.cis6003.dentalclinic.service.notification.ReminderNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Appointment reminders (FR7 - "email or SMS appointment reminders").
 *
 * BUSINESS LOGIC TIER.
 *
 * WHY THE LIST COMES FROM A STORED PROCEDURE
 * sp_report_upcoming_reminders has been in procedures.sql since step 2 and
 * nothing ever called it, which is the same as it not being there. It answers
 * one question - who is coming in N days, and how do we reach them - by joining
 * appointments to patients, dentists and treatments. That join belongs where
 * the data is; only the finished list needs to travel back.
 *
 * WHY NOTHING HERE MAY THROW
 * A reminder is a courtesy, not part of the booking. If the mail server is
 * unreachable, the remaining patients must still be reminded and the nightly
 * job must still finish. Each notifier is therefore called inside its own try.
 * The notifiers guard themselves as well, which makes two, on purpose.
 *
 * THE COST, STATED HONESTLY
 * The same "who is due in" question could have been written as a Spring Data
 * query, and then it would be visible in Java and testable against H2. Putting
 * it in SQL means it can only be tested against real MySQL, and changing it
 * means editing procedures.sql and restarting.
 */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    /** Beyond this it is not a reminder, it is a diary. */
    private static final int MAXIMUM_DAYS_AHEAD = 30;

    private final JdbcTemplate jdbcTemplate;
    private final List<ReminderNotifier> notifiers;

    public ReminderService(JdbcTemplate jdbcTemplate, List<ReminderNotifier> notifiers) {
        this.jdbcTemplate = jdbcTemplate;
        this.notifiers = notifiers;
    }

    /**
     * Who is due in, that many days from today.
     *
     * @param daysAhead 0 is today, 1 is tomorrow
     */
    @Transactional(readOnly = true)
    public List<UpcomingReminderRow> findUpcoming(int daysAhead) {
        checkDaysAhead(daysAhead);

        return jdbcTemplate.query(
                "CALL sp_report_upcoming_reminders(?)",
                reminderRowMapper(),
                daysAhead);
    }

    /**
     * Reminds everybody who is due in, and reports what could be done.
     *
     * The patients with no email address are counted separately rather than
     * quietly ignored. They are the ones somebody has to telephone, and that is
     * the only part of this job a computer cannot finish.
     */
    @Transactional(readOnly = true)
    public ReminderSummary sendReminders(int daysAhead) {
        List<UpcomingReminderRow> due = findUpcoming(daysAhead);

        for (UpcomingReminderRow reminder : due) {
            announce(reminder);
        }

        int emailed = (int) due.stream().filter(UpcomingReminderRow::canBeEmailed).count();
        ReminderSummary summary =
                new ReminderSummary(daysAhead, due.size(), emailed, due.size() - emailed);

        log.info("Reminder round for {} day(s) ahead: {}", daysAhead, summary.describe());
        return summary;
    }

    /**
     * Tells every notifier about one patient.
     *
     * Each one is called inside its own try, so a notifier that fails cannot
     * stop the next one, nor the rest of the round.
     */
    private void announce(UpcomingReminderRow reminder) {
        for (ReminderNotifier notifier : notifiers) {
            try {
                notifier.remind(reminder);
            } catch (RuntimeException problem) {
                log.warn("{} could not remind {} about {}. Carrying on. Reason: {}",
                        notifier.getClass().getSimpleName(),
                        reminder.patientName(),
                        reminder.appointmentNo(),
                        problem.getMessage());
            }
        }
    }

    private RowMapper<UpcomingReminderRow> reminderRowMapper() {
        return (row, rowNumber) -> new UpcomingReminderRow(
                row.getString("appointment_no"),
                row.getDate("appointment_date").toLocalDate(),
                row.getTime("appointment_time").toLocalTime(),
                row.getString("patient_name"),
                row.getString("patient_contact"),
                row.getString("patient_email"),
                row.getString("dentist_name"),
                row.getString("treatment_name"));
    }

    /**
     * Refuses a number of days the clinic cannot mean.
     *
     * Today is allowed, because a same-day telephone round is a real thing a
     * receptionist does when there are gaps in the diary. Yesterday is not: a
     * reminder about an appointment that has already happened is noise.
     */
    private void checkDaysAhead(int daysAhead) {
        if (daysAhead < 0) {
            throw new BadRequestException(
                    "Reminders cannot be sent for a day that has already passed.");
        }
        if (daysAhead > MAXIMUM_DAYS_AHEAD) {
            throw new BadRequestException(
                    "Reminders can only be sent for the next " + MAXIMUM_DAYS_AHEAD + " days.");
        }
    }
}
