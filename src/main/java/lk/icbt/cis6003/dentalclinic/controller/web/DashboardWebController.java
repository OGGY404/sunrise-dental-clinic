package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.config.ClinicConfiguration;
import lk.icbt.cis6003.dentalclinic.dto.AppointmentResponse;
import lk.icbt.cis6003.dentalclinic.dto.BillResponse;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.BillingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The main menu, and the help screen (FR5).
 *
 * PRESENTATION TIER.
 *
 * The brief asks for a menu-driven, user-friendly system. This is that menu.
 * It does not only list the things a receptionist can do; it also shows the two
 * numbers they need first thing in the morning, which are how many patients are
 * coming today and how much money is still owed. A menu that answers a question
 * before it is asked saves a click, and this is the screen everyone lands on.
 */
@Controller
public class DashboardWebController {

    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final ClinicConfiguration clinicConfiguration;

    public DashboardWebController(AppointmentService appointmentService,
                                  BillingService billingService,
                                  ClinicConfiguration clinicConfiguration) {
        this.appointmentService = appointmentService;
        this.billingService = billingService;
        this.clinicConfiguration = clinicConfiguration;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();

        List<AppointmentResponse> todayAppointments = appointmentService.findDaySchedule(today)
                .stream().map(AppointmentResponse::from).toList();

        List<BillResponse> unpaidBills = billingService.findUnpaidBills()
                .stream().map(BillResponse::from).toList();

        model.addAttribute("today", today);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("unpaidBills", unpaidBills);
        model.addAttribute("unpaidTotal", totalOwed(unpaidBills));
        model.addAttribute("clinicName", clinicConfiguration.getClinicName());

        return "dashboard";
    }

    /**
     * FR5 - the help section.
     *
     * Written for somebody on their first morning at the desk, so it is a list
     * of steps in the order they will need them, not a list of features.
     */
    @GetMapping("/help")
    public String help() {
        return "help";
    }

    /** What the clinic is still owed altogether. */
    private BigDecimal totalOwed(List<BillResponse> unpaidBills) {
        return unpaidBills.stream()
                .map(BillResponse::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
