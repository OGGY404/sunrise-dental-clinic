package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.dto.DentistWorkloadRow;
import lk.icbt.cis6003.dentalclinic.dto.RevenueByTreatmentRow;
import lk.icbt.cis6003.dentalclinic.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * The management reports (the reports that "add more value" in the brief).
 *
 * BUSINESS LOGIC TIER.
 *
 * WHY THESE ARE STORED PROCEDURES AND NOT JAVA
 * Every other read in this system goes through JPA, and that is right for
 * working with one patient or one appointment. A report is a different job: it
 * groups and totals every bill in a date range. Doing that in Java would mean
 * dragging all those rows across the network and adding them up here, when the
 * database can do the arithmetic where the data already is and send back only
 * the finished summary. It is also the assessed requirement to use advanced
 * database features, and a procedure that is written but never called is not
 * really used.
 *
 * The cost is honest and belongs in the report: the logic of these two reports
 * is not visible in the Java code, and changing them means editing
 * procedures.sql and restarting so the scripts run again. The report class
 * diagram will show this service with no arithmetic in it at all.
 *
 * JdbcTemplate is used rather than JPA because the answer is not an entity. No
 * table has a "total revenue" column; it exists only for as long as the report
 * is on screen.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * What the clinic earned from each kind of treatment (FR7 reports).
     *
     * Only bills that have been PAID are counted, because money still owed is
     * not money earned.
     */
    public List<RevenueByTreatmentRow> revenueByTreatment(LocalDate from, LocalDate to) {
        checkDateRange(from, to);

        return jdbcTemplate.query(
                "CALL sp_report_revenue_by_treatment(?, ?)",
                revenueRowMapper(),
                from,
                to);
    }

    /** How busy each dentist was, and what that work brought in. */
    public List<DentistWorkloadRow> dentistWorkload(LocalDate from, LocalDate to) {
        checkDateRange(from, to);

        return jdbcTemplate.query(
                "CALL sp_report_dentist_workload(?, ?)",
                workloadRowMapper(),
                from,
                to);
    }

    // --- turning a database row into one line of the report -------------------

    private RowMapper<RevenueByTreatmentRow> revenueRowMapper() {
        return (row, rowNumber) -> new RevenueByTreatmentRow(
                row.getString("treatment_code"),
                row.getString("treatment_name"),
                row.getInt("times_billed"),
                row.getBigDecimal("treatment_revenue"),
                row.getBigDecimal("consultation_revenue"),
                row.getBigDecimal("total_discount"),
                row.getBigDecimal("total_revenue"),
                row.getBigDecimal("average_bill"));
    }

    private RowMapper<DentistWorkloadRow> workloadRowMapper() {
        return (row, rowNumber) -> new DentistWorkloadRow(
                row.getString("dentist_code"),
                row.getString("dentist_name"),
                row.getString("specialisation"),
                row.getInt("total_appointments"),
                row.getInt("completed"),
                row.getInt("cancelled"),
                row.getInt("no_shows"),
                row.getInt("total_minutes_booked"),
                row.getBigDecimal("revenue_generated"));
    }

    // --- the one rule this service owns --------------------------------------

    /**
     * Refuses a range the clinic cannot mean, before the database is asked.
     *
     * A missing date is refused rather than quietly treated as "everything",
     * because a report with no dates on it cannot be checked by whoever reads
     * it later. The same day at both ends is allowed, since a single day report
     * is a normal thing to want.
     */
    private void checkDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Please choose both a start date and an end date for the report.");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("The start date cannot be after the end date.");
        }
    }
}
