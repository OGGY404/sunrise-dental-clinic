package lk.icbt.cis6003.dentalclinic.controller;

import jakarta.validation.Valid;
import lk.icbt.cis6003.dentalclinic.dto.BillRequest;
import lk.icbt.cis6003.dentalclinic.dto.BillResponse;
import lk.icbt.cis6003.dentalclinic.dto.PaymentRequest;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * The billing web service (FR4 - calculate and print the bill).
 *
 * PRESENTATION TIER. No arithmetic happens in this file. The screen sends an
 * appointment number, the business tier picks the pricing rule and works out
 * the amounts, and this class only carries the answer back.
 *
 * That separation is what stops a bill being decided by the browser. If the
 * screen could send the price, anyone able to reach this web service could
 * choose what a filling costs.
 */
@RestController
@RequestMapping("/api/bills")
public class BillingRestController {

    private final BillingService billingService;
    private final CurrentUserResolver currentUserResolver;

    public BillingRestController(BillingService billingService,
                                 CurrentUserResolver currentUserResolver) {
        this.billingService = billingService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Produces the bill for a completed visit and records who produced it.
     *
     * 201 Created, because a bill is a new record with its own number, not just
     * a calculation. Asking twice for the same visit is refused by the business
     * tier, so this can never quietly make a second receipt.
     */
    @PostMapping
    public ResponseEntity<BillResponse> generateBill(@Valid @RequestBody BillRequest request,
                                                     Principal principal) {
        User staff = currentUserResolver.resolve(principal).orElse(null);
        Bill bill = billingService.generateBill(
                request.getAppointmentNo(), request.getDiscount(), staff);

        return ResponseEntity
                .created(URI.create("/api/bills/" + bill.getBillNo()))
                .body(BillResponse.from(bill));
    }

    /** The chase list for the front desk: what is still owed, oldest first. */
    @GetMapping("/unpaid")
    public List<BillResponse> unpaidBills() {
        return billingService.findUnpaidBills().stream().map(BillResponse::from).toList();
    }

    /** The receipt for a visit, for the "print bill" button on the visit screen. */
    @GetMapping("/for-appointment/{appointmentNo}")
    public BillResponse findForAppointment(@PathVariable String appointmentNo) {
        return BillResponse.from(billingService.findByAppointmentNo(appointmentNo));
    }

    /** One receipt, found by its bill number. */
    @GetMapping("/{billNo}")
    public BillResponse findOne(@PathVariable String billNo) {
        return BillResponse.from(billingService.findByBillNo(billNo));
    }

    /** Records that the patient has paid, and how. */
    @PostMapping("/{billNo}/pay")
    public BillResponse pay(@PathVariable String billNo,
                            @Valid @RequestBody PaymentRequest request) {
        return BillResponse.from(billingService.markPaid(billNo, request.getMethod()));
    }
}
