package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.dto.ReminderSummary;
import lk.icbt.cis6003.dentalclinic.dto.UpcomingReminderRow;
import lk.icbt.cis6003.dentalclinic.exception.ClinicException;
import lk.icbt.cis6003.dentalclinic.service.ReminderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * The reminder screen (FR7 - appointment reminders).
 *
 * PRESENTATION TIER.
 *
 * This is the "who do I need to telephone tomorrow?" screen. It is open to the
 * whole front desk rather than to administrators only, because ringing round
 * tomorrow's patients is reception work and not management reporting.
 *
 * The screen shows both halves of the answer: the patients the system can email
 * by itself, and the patients who have no email address and therefore need a
 * person with a telephone. The second list is the useful one.
 */
@Controller
@RequestMapping("/appointments/reminders")
public class ReminderWebController {

    private final ReminderService reminderService;

    public ReminderWebController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * Who is due in.
     *
     * Defaults to tomorrow, because that is the round the front desk actually
     * does at the end of the day.
     */
    @GetMapping
    public String reminders(@RequestParam(defaultValue = "1") int daysAhead, Model model) {
        List<UpcomingReminderRow> reminders = reminderService.findUpcoming(daysAhead);

        model.addAttribute("daysAhead", daysAhead);
        model.addAttribute("date", LocalDate.now().plusDays(daysAhead));
        model.addAttribute("reminders", reminders);
        model.addAttribute("emailable",
                reminders.stream().filter(UpcomingReminderRow::canBeEmailed).count());
        model.addAttribute("needTelephoning",
                reminders.stream().filter(row -> !row.canBeEmailed()).count());

        return "appointments/reminders";
    }

    /**
     * Sends the round now, rather than waiting for the nightly job.
     *
     * A redirect afterwards, like every other save in this system, so pressing
     * refresh cannot send every patient a second reminder.
     */
    @PostMapping("/send")
    public String send(@RequestParam(defaultValue = "1") int daysAhead, RedirectAttributes flash) {
        try {
            ReminderSummary summary = reminderService.sendReminders(daysAhead);
            flash.addFlashAttribute("message", summary.describe());
        } catch (ClinicException problem) {
            flash.addFlashAttribute("errorMessage", problem.getMessage());
        }
        return "redirect:/appointments/reminders?daysAhead=" + daysAhead;
    }
}
