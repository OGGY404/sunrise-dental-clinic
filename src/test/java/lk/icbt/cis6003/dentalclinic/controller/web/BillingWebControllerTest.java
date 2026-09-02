package lk.icbt.cis6003.dentalclinic.controller.web;

import lk.icbt.cis6003.dentalclinic.controller.CurrentUserResolver;
import lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.BillingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the billing screens (FR4 - calculate and print the bill).
 *
 * The receipt is its own page, separate from the page that produced it, so it
 * can be printed on its own and so refreshing it cannot make a second bill.
 */
@DisplayName("Billing screens")
@WebMvcTest(BillingWebController.class)
@WithMockUser(username = "reception", roles = "RECEPTIONIST")
class BillingWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingService billingService;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    private Appointment completedVisit() {
        Dentist dentist = new Dentist("DEN-003", "Dr. Ruwan Jayasinghe", "Oral Surgery");
        dentist.setDentistId(3L);
        Treatment treatment = new Treatment("TRT-005", "Root Canal Treatment", new BigDecimal("25000.00"), 60);
        treatment.setTreatmentId(5L);
        Patient patient = new Patient("PAT-000001", "Kamal Silva", "No. 42, Galle Road", "0771234567");
        patient.setPatientId(10L);

        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260908-0001")
                .patient(patient).dentist(dentist).treatment(treatment)
                .on(LocalDate.now().plusDays(7)).at(LocalTime.of(9, 0))
                .build();
        appointment.setAppointmentId(100L);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointment;
    }

    private Bill unpaidBill() {
        Bill bill = new Bill();
        bill.setBillId(500L);
        bill.setBillNo("BIL-20260902-0001");
        bill.setAppointment(completedVisit());
        bill.setTreatmentCost(new BigDecimal("28750.00"));
        bill.setConsultationFee(new BigDecimal("1500.00"));
        bill.setDiscount(new BigDecimal("500.00"));
        return bill;
    }

    @Test
    @DisplayName("the billing form shows which visit is about to be billed")
    void billingFormShowsTheVisit() throws Exception {
        when(appointmentService.findByNumber("APT-20260908-0001")).thenReturn(completedVisit());

        mockMvc.perform(get("/bills/new").param("appointmentNo", "APT-20260908-0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/generate"))
                .andExpect(model().attributeExists("appointment"));
    }

    @Test
    @DisplayName("producing a bill redirects to the receipt, so refresh cannot bill twice")
    void producingABillRedirectsToTheReceipt() throws Exception {
        when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
        when(billingService.generateBill(eq("APT-20260908-0001"), isNull(), any()))
                .thenReturn(unpaidBill());

        mockMvc.perform(post("/bills/new")
                        .with(csrf())
                        .param("appointmentNo", "APT-20260908-0001"))
                .andExpect(redirectedUrl("/bills/BIL-20260902-0001"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("an agreed discount is passed down to the business tier")
    void discountIsPassedDown() throws Exception {
        when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
        when(billingService.generateBill(any(), any(BigDecimal.class), any())).thenReturn(unpaidBill());

        mockMvc.perform(post("/bills/new")
                        .with(csrf())
                        .param("appointmentNo", "APT-20260908-0001")
                        .param("discount", "500.00"))
                .andExpect(status().is3xxRedirection());

        verify(billingService).generateBill(
                eq("APT-20260908-0001"), eq(new BigDecimal("500.00")), any());
    }

    @Test
    @DisplayName("billing a visit that has not happened sends the message back to the form")
    void refusedBillingComesBackToTheForm() throws Exception {
        when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
        when(appointmentService.findByNumber("APT-20260908-0001")).thenReturn(completedVisit());
        when(billingService.generateBill(any(), any(), any()))
                .thenThrow(new BusinessRuleException(
                        "Appointment APT-20260908-0001 has not taken place yet."));

        mockMvc.perform(post("/bills/new")
                        .with(csrf())
                        .param("appointmentNo", "APT-20260908-0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/generate"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("FR4 the receipt is its own printable page")
    void receiptIsItsOwnPage() throws Exception {
        when(billingService.findByBillNo("BIL-20260902-0001")).thenReturn(unpaidBill());

        mockMvc.perform(get("/bills/BIL-20260902-0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/view"))
                .andExpect(model().attributeExists("bill"));
    }

    @Test
    @DisplayName("the unpaid list is the chase list for the front desk")
    void unpaidListIsShown() throws Exception {
        when(billingService.findUnpaidBills()).thenReturn(List.of(unpaidBill()));

        mockMvc.perform(get("/bills/unpaid"))
                .andExpect(status().isOk())
                .andExpect(view().name("bills/unpaid"))
                .andExpect(model().attributeExists("bills"));
    }

    @Test
    @DisplayName("recording a payment returns to the receipt with a message")
    void paymentReturnsToTheReceipt() throws Exception {
        Bill paid = unpaidBill();
        paid.markPaid(PaymentMethod.CASH);
        when(billingService.markPaid("BIL-20260902-0001", PaymentMethod.CASH)).thenReturn(paid);

        mockMvc.perform(post("/bills/BIL-20260902-0001/pay")
                        .with(csrf())
                        .param("method", "CASH"))
                .andExpect(redirectedUrl("/bills/BIL-20260902-0001"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @DisplayName("paying a bill twice is reported as a message, not an error page")
    void payingTwiceIsAMessage() throws Exception {
        when(billingService.markPaid("BIL-20260902-0001", PaymentMethod.CASH))
                .thenThrow(new BusinessRuleException("Bill BIL-20260902-0001 has already been paid."));

        mockMvc.perform(post("/bills/BIL-20260902-0001/pay")
                        .with(csrf())
                        .param("method", "CASH"))
                .andExpect(redirectedUrl("/bills/BIL-20260902-0001"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
