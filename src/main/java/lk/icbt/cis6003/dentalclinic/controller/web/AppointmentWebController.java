package lk.icbt.cis6003.dentalclinic.controller.web;

import jakarta.validation.Valid;
import lk.icbt.cis6003.dentalclinic.controller.CurrentUserResolver;
import lk.icbt.cis6003.dentalclinic.dto.AppointmentResponse;
import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.dto.DentistResponse;
import lk.icbt.cis6003.dentalclinic.dto.TreatmentResponse;
import lk.icbt.cis6003.dentalclinic.exception.ClinicException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.ReferenceDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The appointment screens (FR2 register, FR3 display, FR7 cancel and
 * reschedule), plus the daily schedule report.
 *
 * PRESENTATION TIER. There is no clinic rule in this file. Every decision about
 * dates, slots and statuses belongs to AppointmentService, which is what lets
 * the same rules hold for the web service in the other controller package.
 *
 * TWO THINGS THIS CLASS IS CAREFUL ABOUT
 *
 * 1. Data entry and viewing results are separate screens, which the assessment
 *    asks for. Registering uses appointments/register; the result is shown by
 *    appointments/view after a redirect.
 *
 * 2. Every save redirects instead of returning a page. Pressing refresh on a
 *    page that was returned straight from a POST would send the booking again,
 *    and the patient would end up with two appointments. Redirecting means
 *    refresh only re-reads the details, which is harmless.
 */
@Controller
@RequestMapping("/appointments")
public class AppointmentWebController {

    private final AppointmentService appointmentService;
    private final ReferenceDataService referenceDataService;
    private final CurrentUserResolver currentUserResolver;

    public AppointmentWebController(AppointmentService appointmentService,
                                    ReferenceDataService referenceDataService,
                                    CurrentUserResolver currentUserResolver) {
        this.appointmentService = appointmentService;
        this.referenceDataService = referenceDataService;
        this.currentUserResolver = currentUserResolver;
    }

    // --- FR2: register a new appointment -------------------------------------

    @GetMapping("/new")
    public String registerForm(Model model) {
        model.addAttribute("bookingRequest", new BookingRequest());
        addDropdownLists(model);
        return "appointments/register";
    }

    /**
     * Saves the booking, or sends the form back with the problem shown.
     *
     * The two ways this can fail are handled differently on purpose:
     *
     *  - the form itself is wrong (a name with digits in it). Spring collects
     *    those in the BindingResult and the page puts each message under its
     *    own box, with everything the receptionist typed still there.
     *
     *  - the form is fine but the clinic refuses it (that slot has just gone).
     *    Nothing is wrong with any single box, so the message goes at the top
     *    of the form instead.
     *
     * In both cases the dropdown lists have to be loaded again. They are not
     * part of what the browser sent, so without this the form would come back
     * with two empty dropdowns and the receptionist could not correct it.
     */
    @PostMapping("/new")
    public String register(@Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                           BindingResult binding,
                           Principal principal,
                           Model model,
                           RedirectAttributes flash) {

        if (binding.hasErrors()) {
            addDropdownLists(model);
            return "appointments/register";
        }

        try {
            User staff = currentUserResolver.resolve(principal).orElse(null);
            Appointment saved = appointmentService.register(bookingRequest, staff);

            flash.addFlashAttribute("message",
                    "Appointment " + saved.getAppointmentNo() + " has been registered for "
                            + saved.getPatient().getFullName() + ".");

            return "redirect:/appointments/" + saved.getAppointmentNo();

        } catch (ClinicException problem) {
            model.addAttribute("errorMessage", problem.getMessage());
            addDropdownLists(model);
            return "appointments/register";
        }
    }

    // --- FR3: find and display a visit ---------------------------------------

    /**
     * The search screen, and where it sends you.
     *
     * With no number typed it is just the empty search box. With a number that
     * exists it redirects to the details screen, so the address bar then holds
     * that appointment and the page can be bookmarked or refreshed. With a
     * number that does not exist the receptionist stays on the search screen,
     * where the box is, rather than being sent to an error page they would have
     * to navigate back from.
     */
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String appointmentNo, Model model) {
        if (appointmentNo == null || appointmentNo.isBlank()) {
            return "appointments/search";
        }

        String typed = appointmentNo.trim();
        try {
            appointmentService.findByNumber(typed);
            return "redirect:/appointments/" + typed;
        } catch (NotFoundException notFound) {
            model.addAttribute("errorMessage", notFound.getMessage());
            model.addAttribute("appointmentNo", typed);
            return "appointments/search";
        }
    }

    @GetMapping("/{appointmentNo}")
    public String view(@PathVariable String appointmentNo, Model model) {
        model.addAttribute("appointment",
                AppointmentResponse.from(appointmentService.findByNumber(appointmentNo)));
        return "appointments/view";
    }

    // --- the daily schedule report -------------------------------------------

    /**
     * The clinic diary for one day, printed and put on the door each morning.
     *
     * With no date chosen it shows today, because that is what the front desk
     * wants nine times out of ten.
     */
    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) Long dentistId,
                           Model model) {

        LocalDate day = (date == null) ? LocalDate.now() : date;

        List<Appointment> found = (dentistId == null)
                ? appointmentService.findDaySchedule(day)
                : appointmentService.findDayScheduleForDentist(dentistId, day);

        model.addAttribute("date", day);
        model.addAttribute("dentistId", dentistId);
        model.addAttribute("appointments", found.stream().map(AppointmentResponse::from).toList());
        model.addAttribute("dentists", referenceDataService.bookableDentists()
                .stream().map(DentistResponse::from).toList());

        return "appointments/schedule";
    }

    // --- FR7: changing a visit -----------------------------------------------

    @PostMapping("/{appointmentNo}/complete")
    public String complete(@PathVariable String appointmentNo, RedirectAttributes flash) {
        return afterChanging(appointmentNo, flash,
                () -> appointmentService.markCompleted(appointmentNo),
                "The visit has been marked as completed. It can now be billed.");
    }

    @PostMapping("/{appointmentNo}/no-show")
    public String noShow(@PathVariable String appointmentNo, RedirectAttributes flash) {
        return afterChanging(appointmentNo, flash,
                () -> appointmentService.markNoShow(appointmentNo),
                "Recorded that the patient did not arrive.");
    }

    @PostMapping("/{appointmentNo}/cancel")
    public String cancel(@PathVariable String appointmentNo,
                         @RequestParam String reason,
                         RedirectAttributes flash) {
        return afterChanging(appointmentNo, flash,
                () -> appointmentService.cancel(appointmentNo, reason),
                "The appointment has been cancelled and the slot is free again.");
    }

    @PostMapping("/{appointmentNo}/reschedule")
    public String reschedule(@PathVariable String appointmentNo,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newTime,
                             RedirectAttributes flash) {
        return afterChanging(appointmentNo, flash,
                () -> appointmentService.reschedule(appointmentNo, newDate, newTime),
                "The appointment has been moved to " + newDate + " at " + newTime + ".");
    }

    // --- shared plumbing ------------------------------------------------------

    /**
     * Runs one change and always returns to the same details screen.
     *
     * A refused change is not an error page. The receptionist is told what
     * happened at the top of the page they were already on, so they can see the
     * current state of the appointment and decide what to do instead.
     */
    private String afterChanging(String appointmentNo,
                                 RedirectAttributes flash,
                                 Runnable change,
                                 String successMessage) {
        try {
            change.run();
            flash.addFlashAttribute("message", successMessage);
        } catch (ClinicException problem) {
            flash.addFlashAttribute("errorMessage", problem.getMessage());
        }
        return "redirect:/appointments/" + appointmentNo;
    }

    /** The two dropdowns the booking form cannot be filled in without. */
    private void addDropdownLists(Model model) {
        model.addAttribute("dentists", referenceDataService.bookableDentists()
                .stream().map(DentistResponse::from).toList());
        model.addAttribute("treatments", referenceDataService.bookableTreatments()
                .stream().map(TreatmentResponse::from).toList());
    }
}
