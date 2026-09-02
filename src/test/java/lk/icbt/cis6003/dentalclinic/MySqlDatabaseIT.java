package lk.icbt.cis6003.dentalclinic;

import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.Gender;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import lk.icbt.cis6003.dentalclinic.service.BillingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the parts of this system that only exist inside MySQL.
 *
 * WHY THIS TEST EXISTS
 * Every other test runs on H2, which is fast and needs no database installed.
 * That is the right trade for the 248 tests that check clinic rules. But H2
 * builds its tables from the JPA entities, so it can only ever agree with them,
 * and it has none of the stored procedures, functions, triggers or generated
 * columns that the assessment asks for. A green test suite therefore proved
 * nothing about the half of this system written in SQL.
 *
 * Two real bugs got through exactly that gap and were only found by running the
 * application by hand:
 *
 *   1. password_hash was CHAR(60) in schema.sql but VARCHAR on the entity, so
 *      Hibernate refused to start against real MySQL. H2 never noticed, because
 *      it had built the column from the entity in the first place.
 *
 *   2. The tables were utf8mb4_unicode_ci while MySQL 8 creates the database
 *      itself as utf8mb4_0900_ai_ci, so a stored function could not compare its
 *      own parameter against a column, and every booking failed.
 *
 * Both would have been caught in seconds by the tests below. This class is the
 * lesson from that written down as code.
 *
 * HOW IT IS RUN
 * The name ends in IT, not Test, so Maven Surefire does not pick it up during
 * an ordinary "mvnw test" and nobody needs MySQL installed to build the
 * project. GitHub Actions runs it deliberately, against a real MySQL 8 service
 * container, with:
 *
 *     ./mvnw test -Dtest=MySqlDatabaseIT
 *
 * It uses application.properties, not the test profile, so it connects to
 * MySQL exactly as the running application does. DB_NAME points it at its own
 * database so it never touches the clinic's real data.
 */
@DisplayName("Real MySQL: schema, procedures, triggers and generated columns")
@SpringBootTest
@Transactional
class MySqlDatabaseIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private BillingService billingService;

    /** Far enough ahead that these bookings never clash with sample data. */
    private static final LocalDate TEST_DAY = LocalDate.now().plusDays(90);

    // --- what the scripts built ----------------------------------------------

    /**
     * The application context has already started by the time this runs, and
     * that is most of the point.
     *
     * Starting means schema.sql, procedures.sql, triggers.sql and data.sql all
     * executed against real MySQL, and then Hibernate compared every entity
     * against every table and agreed. That single fact is what the CHAR(60) bug
     * broke.
     */
    @Test
    @DisplayName("every table, routine and trigger was created by the scripts")
    void theScriptsBuiltEverything() {
        assertThat(countOf("tables", "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'"))
                .as("tables from schema.sql").isEqualTo(10);

        assertThat(countOf("functions", "SELECT COUNT(*) FROM information_schema.routines "
                + "WHERE routine_schema = DATABASE() AND routine_type = 'FUNCTION'"))
                .as("stored functions from procedures.sql").isEqualTo(6);

        assertThat(countOf("procedures", "SELECT COUNT(*) FROM information_schema.routines "
                + "WHERE routine_schema = DATABASE() AND routine_type = 'PROCEDURE'"))
                .as("stored procedures from procedures.sql").isEqualTo(13);

        assertThat(countOf("triggers", "SELECT COUNT(*) FROM information_schema.triggers "
                + "WHERE trigger_schema = DATABASE()"))
                .as("triggers from triggers.sql").isEqualTo(10);
    }

    /**
     * The database and its tables must use the same collation.
     *
     * If they drift apart, a stored routine cannot compare its own VARCHAR
     * parameter against a column, and MySQL refuses with "Illegal mix of
     * collations". That broke every booking, because inserting an appointment
     * fires a trigger that calls a function that does exactly that comparison.
     */
    @Test
    @DisplayName("the database collation matches the tables, so the routines can compare")
    void collationsMatch() {
        String databaseCollation = jdbcTemplate.queryForObject(
                "SELECT default_collation_name FROM information_schema.schemata "
                        + "WHERE schema_name = DATABASE()", String.class);

        assertThat(databaseCollation).isEqualTo("utf8mb4_unicode_ci");

        // The proof that it works: a function taking a VARCHAR parameter and
        // comparing it against a column. This is the exact call that failed.
        String fee = jdbcTemplate.queryForObject(
                "SELECT fn_get_setting('consultation_fee')", String.class);

        assertThat(fee).as("read back through fn_get_setting").isNotBlank();
    }

    // --- what the database does by itself -------------------------------------

    /**
     * Booking through the normal service must set the triggers working.
     *
     * The appointment number comes from a stored procedure and a counter table,
     * not from counting rows, and the audit row is written by an AFTER INSERT
     * trigger that no Java code calls.
     */
    @Test
    @DisplayName("booking produces a reference number and an audit row, both from the database")
    void bookingUsesTheProceduresAndTriggers() {
        Appointment saved = appointmentService.register(bookingAt(LocalTime.of(9, 0)), null);

        assertThat(saved.getAppointmentNo())
                .as("produced by sp_next_appointment_no")
                .matches("APT-\\d{8}-\\d{4}");

        assertThat(saved.getPatient().getPatientCode())
                .as("produced by the patient counter")
                .matches("PAT-\\d{6}");

        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment_audit WHERE appointment_no = ?",
                Integer.class, saved.getAppointmentNo());

        assertThat(auditRows)
                .as("written by trg_appointments_after_insert, which no Java code calls")
                .isEqualTo(1);
    }

    /**
     * The total on a bill is a generated column: MySQL works it out from the
     * three amounts, and Java is not allowed to write it.
     *
     * H2 does not copy that behaviour, so this can only be checked here.
     */
    @Test
    @DisplayName("MySQL calculates the bill total itself, in a generated column")
    void mysqlCalculatesTheBillTotal() {
        Appointment visit = appointmentService.register(bookingAt(LocalTime.of(10, 0)), null);
        appointmentService.markCompleted(visit.getAppointmentNo());

        Bill bill = billingService.generateBill(
                visit.getAppointmentNo(), new BigDecimal("100.00"), null);

        BigDecimal storedByMySql = jdbcTemplate.queryForObject(
                "SELECT total_amount FROM bills WHERE bill_no = ?",
                BigDecimal.class, bill.getBillNo());

        BigDecimal expected = bill.getTreatmentCost()
                .add(bill.getConsultationFee())
                .subtract(bill.getDiscount());

        assertThat(storedByMySql)
                .as("treatment + consultation - discount, added up by the database")
                .isEqualByComparingTo(expected);
    }

    /**
     * The last line of defence against double booking.
     *
     * The service checks first and gives a friendly message, but that check
     * could be beaten by two receptionists saving in the same instant, or
     * simply bypassed by anyone writing to the database another way. The UNIQUE
     * index on the generated slot_key column cannot be.
     *
     * This test goes around the Java entirely and writes straight to the table,
     * which is the only way to prove the database is really holding the line.
     */
    @Test
    @DisplayName("the database itself refuses a second booking in the same chair and slot")
    void theDatabaseBlocksDoubleBooking() {
        Appointment first = appointmentService.register(bookingAt(LocalTime.of(11, 0)), null);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO appointments "
                        + "(appointment_no, patient_id, dentist_id, treatment_id, appointment_date, appointment_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                "APT-DUPLICATE-01",
                first.getPatient().getPatientId(),
                first.getDentist().getDentistId(),
                first.getTreatment().getTreatmentId(),
                TEST_DAY,
                LocalTime.of(11, 0)))
                .as("blocked by the UNIQUE index on appointments.slot_key")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * A rule that lives only in the database.
     *
     * The opening hours trigger refuses a booking outside them even when the
     * insert comes from somewhere that never ran the Java checks.
     */
    @Test
    @DisplayName("a trigger refuses a booking outside opening hours, even from raw SQL")
    void theTriggerRefusesBookingsOutsideOpeningHours() {
        Appointment existing = appointmentService.register(bookingAt(LocalTime.of(12, 0)), null);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO appointments "
                        + "(appointment_no, patient_id, dentist_id, treatment_id, appointment_date, appointment_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                "APT-MIDNIGHT-1",
                existing.getPatient().getPatientId(),
                existing.getDentist().getDentistId(),
                existing.getTreatment().getTreatmentId(),
                TEST_DAY,
                LocalTime.of(3, 0)))
                .as("refused by trg_appointments_before_insert")
                .hasMessageContaining("closed");
    }

    // --- helpers --------------------------------------------------------------

    /** A booking for a dentist and treatment that data.sql always provides. */
    private BookingRequest bookingAt(LocalTime time) {
        BookingRequest request = new BookingRequest();
        request.setFullName("Integration Test Patient");
        request.setAddress("No. 1, Test Road, Colombo 01");
        request.setContactNumber("0770000001");
        request.setEmail("integration@example.lk");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setGender(Gender.OTHER);
        request.setDentistId(firstIdFrom("SELECT MIN(dentist_id) FROM dentists WHERE active = TRUE"));
        request.setTreatmentId(firstIdFrom("SELECT MIN(treatment_id) FROM treatments WHERE active = TRUE"));
        request.setAppointmentDate(TEST_DAY);
        request.setAppointmentTime(time);
        request.setNotes("Created by MySqlDatabaseIT");
        return request;
    }

    /** Reads an id from the reference data, so the test does not hardcode one. */
    private Long firstIdFrom(String sql) {
        Long id = jdbcTemplate.queryForObject(sql, Long.class);
        assertThat(id).as("reference data loaded by data.sql").isNotNull();
        return id;
    }

    private int countOf(String what, String sql) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).as(what).isNotNull();
        return count;
    }
}
