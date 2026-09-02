package lk.icbt.cis6003.dentalclinic.config;

import lk.icbt.cis6003.dentalclinic.dto.ReminderSummary;
import lk.icbt.cis6003.dentalclinic.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Sends tomorrow's reminders once a day, without anybody remembering to
 * (FR7).
 *
 * WHY THIS IS SWITCHED ON BY A PROPERTY
 * A timed job that runs during the test suite is a nuisance: it fires while
 * tests are asserting, writes to the log, and makes failures look random.
 * clinic.reminders.scheduled is true in application.properties and false in
 * application-test.properties, so the running clinic gets the job and the tests
 * never do.
 *
 * WHY THE TIME IS CONFIGURABLE
 * Six in the evening suits a clinic that closes at six, but that is a business
 * decision and not a programming one, so it is a setting rather than a constant.
 *
 * WHY THIS CLASS CANNOT FAIL
 * ReminderService already guards each notifier, and this catches anything left,
 * because an exception escaping a scheduled method silently stops the job from
 * ever running again. The clinic would not find out until patients stopped
 * arriving.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "clinic.reminders.scheduled", havingValue = "true")
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminderService;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /** Default: every evening at six o'clock, for tomorrow's patients. */
    @Scheduled(cron = "${clinic.reminders.cron:0 0 18 * * *}", zone = "Asia/Colombo")
    public void remindTomorrowsPatients() {
        try {
            ReminderSummary summary = reminderService.sendReminders(1);
            log.info("Nightly reminder round finished. {}", summary.describe());
        } catch (RuntimeException problem) {
            log.error("The nightly reminder round failed. The clinic is unaffected, "
                    + "but tomorrow's patients were not reminded.", problem);
        }
    }
}
