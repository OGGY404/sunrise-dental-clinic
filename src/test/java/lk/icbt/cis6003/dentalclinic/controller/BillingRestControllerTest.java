package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.exception.BusinessRuleException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;
import lk.icbt.cis6003.dentalclinic.service.BillingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the billing web service (FR4 - calculate and print the bill).
 *
 * PRESENTATION TIER. The arithmetic belongs to the billing strategies and is
 * already tested in BillingStrategyTest. What is checked here is only that the
 * web layer passes the right values down, and turns each kind of refusal into
 * the right HTTP status.
 */
@DisplayName("Billing web service")
@WebMvcTest(BillingRestController.class)
@WithMockUser(username = "reception1", roles = "RECEPTIONIST")
class BillingRestControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingService billingService;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @Nested
    @DisplayName("POST /api/bills - produce the bill")
    class GenerateBill {

        @Test
        @DisplayName("returns 201 with the three amounts and the total")
        void producesABill() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(billingService.generateBill(eq("APT-20260907-0001"), isNull(), any()))
                    .thenReturn(unpaidBill());

            mockMvc.perform(post("/api/bills")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentNo\": \"APT-20260907-0001\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.billNo").value("BIL-20260907-0001"))
                    .andExpect(jsonPath("$.appointmentNo").value("APT-20260907-0001"))
                    .andExpect(jsonPath("$.patientName").value("Kamal Silva"))
                    .andExpect(jsonPath("$.treatmentCost").value(6000.00))
                    .andExpect(jsonPath("$.consultationFee").value(1500.00))
                    .andExpect(jsonPath("$.totalAmount").value(7500.00))
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
        }

        @Test
        @DisplayName("passes an agreed discount down to the business tier")
        void passesTheDiscountDown() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(billingService.generateBill(anyString(), any(BigDecimal.class), any()))
                    .thenReturn(unpaidBill());

            mockMvc.perform(post("/api/bills")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentNo\": \"APT-20260907-0001\", \"discount\": 500.00}"))
                    .andExpect(status().isCreated());

            verify(billingService).generateBill(
                    eq("APT-20260907-0001"), eq(new BigDecimal("500.00")), any());
        }

        @Test
        @DisplayName("refuses a negative discount with 400 before the service is called")
        void refusesANegativeDiscount() throws Exception {
            mockMvc.perform(post("/api/bills")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentNo\": \"APT-20260907-0001\", \"discount\": -1.00}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.discount").exists());

            verify(billingService, never()).generateBill(anyString(), any(), any());
        }

        @Test
        @DisplayName("refuses a missing appointment number with 400")
        void refusesAMissingAppointmentNumber() throws Exception {
            mockMvc.perform(post("/api/bills")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentNo\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.appointmentNo").exists());
        }

        @Test
        @DisplayName("answers 422 when the visit has not taken place yet")
        void refusesToBillAVisitThatHasNotHappened() throws Exception {
            when(currentUserResolver.resolve(any())).thenReturn(Optional.empty());
            when(billingService.generateBill(anyString(), any(), any()))
                    .thenThrow(new BusinessRuleException(
                            "Appointment APT-20260907-0001 has not taken place yet."));

            mockMvc.perform(post("/api/bills")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appointmentNo\": \"APT-20260907-0001\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("not taken place")));
        }
    }

    @Nested
    @DisplayName("reading bills")
    class ReadBills {

        @Test
        @DisplayName("GET /api/bills/{billNo} shows the receipt")
        void showsOneBill() throws Exception {
            when(billingService.findByBillNo("BIL-20260907-0001")).thenReturn(unpaidBill());

            mockMvc.perform(get("/api/bills/BIL-20260907-0001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.billNo").value("BIL-20260907-0001"))
                    .andExpect(jsonPath("$.treatmentName").value("Tooth Filling"));
        }

        @Test
        @DisplayName("GET an unknown bill number answers 404")
        void unknownBillIs404() throws Exception {
            when(billingService.findByBillNo("BIL-NOPE"))
                    .thenThrow(NotFoundException.of("bill", "BIL-NOPE"));

            mockMvc.perform(get("/api/bills/BIL-NOPE"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/bills/for-appointment/{no} finds the receipt for a visit")
        void findsTheBillForAVisit() throws Exception {
            when(billingService.findByAppointmentNo("APT-20260907-0001")).thenReturn(unpaidBill());

            mockMvc.perform(get("/api/bills/for-appointment/APT-20260907-0001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentNo").value("APT-20260907-0001"));
        }

        @Test
        @DisplayName("GET /api/bills/unpaid lists the money still owed")
        void listsUnpaidBills() throws Exception {
            when(billingService.findUnpaidBills()).thenReturn(List.of(unpaidBill()));

            mockMvc.perform(get("/api/bills/unpaid"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].paymentStatus").value("UNPAID"));
        }
    }

    @Nested
    @DisplayName("POST /api/bills/{billNo}/pay - settle the bill")
    class PayBill {

        @Test
        @DisplayName("records the payment method and returns the settled bill")
        void recordsThePayment() throws Exception {
            Bill paid = unpaidBill();
            paid.markPaid(PaymentMethod.CASH);
            when(billingService.markPaid("BIL-20260907-0001", PaymentMethod.CASH)).thenReturn(paid);

            mockMvc.perform(post("/api/bills/BIL-20260907-0001/pay")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\": \"CASH\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                    .andExpect(jsonPath("$.paymentMethod").value("CASH"));
        }

        @Test
        @DisplayName("refuses a missing payment method with 400")
        void refusesAMissingMethod() throws Exception {
            mockMvc.perform(post("/api/bills/BIL-20260907-0001/pay")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.method").exists());

            verify(billingService, never()).markPaid(anyString(), any());
        }

        @Test
        @DisplayName("refuses a payment method the clinic does not take, with 400")
        void refusesAnUnknownMethod() throws Exception {
            mockMvc.perform(post("/api/bills/BIL-20260907-0001/pay")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\": \"BITCOIN\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("answers 422 when the bill was already paid")
        void refusesToPayTwice() throws Exception {
            when(billingService.markPaid("BIL-20260907-0001", PaymentMethod.CASH))
                    .thenThrow(new BusinessRuleException(
                            "Bill BIL-20260907-0001 has already been paid, so it cannot be paid again."));

            mockMvc.perform(post("/api/bills/BIL-20260907-0001/pay")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\": \"CASH\"}"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
