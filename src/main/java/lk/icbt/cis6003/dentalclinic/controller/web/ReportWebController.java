package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * The management report screens.
 *
 * PRESENTATION TIER. These pages choose a date range and display what the
 * database sends back. The grouping and the totals are done by stored
 * procedures, so there is no arithmetic in this file at all.
 *
 * WHY THESE PAGES ARE ADMIN ONLY
 * They show clinic-wide takings and how each dentist is performing. A
 * receptionist needs neither in order to book a patient in, and staff
 * performance figures are not something to leave open on the front desk screen.
 *
 * The rule itself is in SecurityConfig, not here. Written there, it also covers
 * any report page added later, and it cannot be forgotten by whoever writes
 * that page.
 */
@Controller
@RequestMapping("/reports")
public class ReportWebController {

    private final ReportService reportService;

    public ReportWebController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** The menu of reports that are available. */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("from", startOfThisMonth());
        model.addAttribute("to", LocalDate.now());
        return "reports/index";
    }

    /**
     * What the clinic earned from each kind of treatment.
     *
     * With no dates chosen it covers this month so far, which is the range a
     * manager asks for most often.
     */
    @GetMapping("/revenue")
    public String revenue(@RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {

        LocalDate start = (from == null) ? startOfThisMonth() : from;
        LocalDate end = (to == null) ? LocalDate.now() : to;

        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("rows", reportService.revenueByTreatment(start, end));

        return "reports/revenue";
    }

    /** How busy each dentist was, and what that work brought in. */
    @GetMapping("/workload")
    public String workload(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           Model model) {

        LocalDate start = (from == null) ? startOfThisMonth() : from;
        LocalDate end = (to == null) ? LocalDate.now() : to;

        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("rows", reportService.dentistWorkload(start, end));

        return "reports/workload";
    }

    private LocalDate startOfThisMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }
}
