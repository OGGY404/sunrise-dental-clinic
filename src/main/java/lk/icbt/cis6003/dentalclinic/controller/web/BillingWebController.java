package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.controller.CurrentUserResolver;
import lk.icbt.cis6003.dentalclinic.dto.AppointmentResponse;
import lk.icbt.cis6003.dentalclinic.dto.BillResponse;
import lk.icbt.cis6003.dentalclinic.exception.BadRequestException;
import lk.icbt.cis6003.dentalclinic.exception.ClinicException;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.BillingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * The billing screens (FR4 - calculate and print the bill).
 *
 * PRESENTATION TIER. No arithmetic happens here. The screen sends an
 * appointment number and, if the clinic has agreed one, a discount; the
 * business tier picks the pricing rule and works out every amount.
 *
 * That matters for more than tidiness. If the screen could send the price, then
 * anyone who can open this page could decide what a root canal costs.
 *
 * The receipt is its own page so that it can be printed on its own, and it is
 * reached by a redirect so that pressing refresh cannot produce a second bill.
 */
@Controller
@RequestMapping("/bills")
public class BillingWebController {

    private final BillingService billingService;
    private final AppointmentService appointmentService;
    private final CurrentUserResolver currentUserResolver;

    public BillingWebController(BillingService billingService,
                                AppointmentService appointmentService,
                                CurrentUserResolver currentUserResolver) {
        this.billingService = billingService;
        this.appointmentService = appointmentService;
        this.currentUserResolver = currentUserResolver;
    }

    /** Shows which visit is about to be billed, and asks for any discount. */
    @GetMapping("/new")
    public String billingForm(@RequestParam String appointmentNo, Model model) {
        model.addAttribute("appointment",
                AppointmentResponse.from(appointmentService.findByNumber(appointmentNo)));
        return "bills/generate";
    }

    @PostMapping("/new")
    public String generateBill(@RequestParam String appointmentNo,
                               @RequestParam(required = false) String discount,
                               Principal principal,
                               Model model,
                               RedirectAttributes flash) {
        try {
            User staff = currentUserResolver.resolve(principal).orElse(null);
            Bill bill = billingService.generateBill(appointmentNo, readDiscount(discount), staff);

            flash.addFlashAttribute("message",
                    "Bill " + bill.getBillNo() + " has been produced.");

            return "redirect:/bills/" + bill.getBillNo();

        } catch (ClinicException problem) {
            // Stay on the billing form, where the receptionist can see the
            // visit and change the discount, rather than sending them to an
            // error page they would have to navigate back from.
            model.addAttribute("errorMessage", problem.getMessage());
            model.addAttribute("appointment",
                    AppointmentResponse.from(appointmentService.findByNumber(appointmentNo)));
            return "bills/generate";
        }
    }

    /** FR4 - the receipt, laid out to be printed and handed to the patient. */
    @GetMapping("/{billNo}")
    public String view(@PathVariable String billNo, Model model) {
        model.addAttribute("bill", BillResponse.from(billingService.findByBillNo(billNo)));
        return "bills/view";
    }

    /** The chase list for the front desk: what is still owed, oldest first. */
    @GetMapping("/unpaid")
    public String unpaid(Model model) {
        List<BillResponse> bills = billingService.findUnpaidBills()
                .stream().map(BillResponse::from).toList();

        model.addAttribute("bills", bills);
        model.addAttribute("total", bills.stream()
                .map(BillResponse::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return "bills/unpaid";
    }

    @PostMapping("/{billNo}/pay")
    public String pay(@PathVariable String billNo,
                      @RequestParam PaymentMethod method,
                      RedirectAttributes flash) {
        try {
            billingService.markPaid(billNo, method);
            flash.addFlashAttribute("message", "Payment recorded. The bill is now settled.");
        } catch (ClinicException problem) {
            flash.addFlashAttribute("errorMessage", problem.getMessage());
        }
        return "redirect:/bills/" + billNo;
    }

    /**
     * Reads the discount box.
     *
     * An empty box means no discount, which is the normal case, so it must not
     * be an error. Anything that is not a number is refused here with a
     * sentence the receptionist can act on, rather than being allowed to
     * surface later as a type conversion failure.
     */
    private BigDecimal readDiscount(String typed) {
        if (typed == null || typed.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(typed.trim());
        } catch (NumberFormatException notANumber) {
            throw new BadRequestException(
                    "The discount must be an amount of money, for example 500 or 500.00.");
        }
    }
}
