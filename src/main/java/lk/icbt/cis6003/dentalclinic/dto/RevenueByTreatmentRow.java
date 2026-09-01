package lk.icbt.cis6003.dentalclinic.dto;

import java.math.BigDecimal;

/**
 * One line of the revenue report: what the clinic earned from one kind of
 * treatment over a date range.
 *
 * WHY THIS IS A RECORD AND NOT AN ENTITY
 * It is not a table and it is never saved. It is one row of an answer the
 * database worked out, and nothing may change it after it arrives. A record
 * says exactly that in one line: the fields are final and the accessors are
 * generated, so there is no setter for anyone to call by mistake.
 *
 * The figures come from bills that have actually been PAID, not from bills
 * issued, because money owed is not money earned.
 */
public record RevenueByTreatmentRow(

        String treatmentCode,
        String treatmentName,

        /** How many paid bills included this treatment. */
        int timesBilled,

        BigDecimal treatmentRevenue,
        BigDecimal consultationRevenue,
        BigDecimal totalDiscount,

        /** treatment + consultation - discount, added up by MySQL. */
        BigDecimal totalRevenue,

        BigDecimal averageBill) {
}
