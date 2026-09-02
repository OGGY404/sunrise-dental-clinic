package lk.icbt.cis6003.dentalclinic.service;

import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Map;

/**
 * The real number generator. Calls the stored procedures in MySQL.
 *
 * WHY THE DATABASE HANDS OUT THE NUMBERS, NOT JAVA
 * The obvious way to number appointments is to count the rows already there and
 * add one. That is wrong the moment two receptionists save at the same instant:
 * both count the same total, and both produce APT-20260907-0007.
 *
 * The stored procedures use a counter table with INSERT ... ON DUPLICATE KEY
 * UPDATE. MySQL locks that single counter row for the fraction of a second it
 * takes, so the two receptionists get 0007 and 0008. No amount of Java can give
 * that guarantee from outside the database.
 *
 * This class is where the assessed stored procedures sp_next_appointment_no and
 * sp_next_bill_no are actually used by the running system.
 *
 * WHY THE PARAMETERS ARE DECLARED AND THE METADATA LOOKUP IS SWITCHED OFF
 * Left to itself, SimpleJdbcCall asks the driver to describe the procedure
 * before calling it. On MySQL that search is not limited to the database this
 * application is connected to, so if the same server holds a second copy of
 * the schema - a test database beside the live one, which is completely normal
 * - it finds the procedure twice and refuses to guess:
 *
 *     Unable to determine the correct call signature - multiple signatures
 *     for 'sp_next_appointment_no'
 *
 * Every parameter is already written out below, so that lookup was never
 * needed. Switching it off removes the ambiguity, and saves a round trip to
 * the database on every booking as well.
 *
 * This was found by MySqlDatabaseIT, which runs against its own database on
 * the same server, which is exactly the situation that triggers it.
 */
@Component
public class StoredProcedureReferenceNumberGenerator implements ReferenceNumberGenerator {

    private final SimpleJdbcCall nextAppointmentNoCall;
    private final SimpleJdbcCall nextBillNoCall;
    private final JdbcTemplate jdbcTemplate;

    public StoredProcedureReferenceNumberGenerator(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        this.nextAppointmentNoCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("sp_next_appointment_no")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new org.springframework.jdbc.core.SqlParameter("p_date", Types.DATE),
                        new org.springframework.jdbc.core.SqlOutParameter("p_appointment_no", Types.VARCHAR));

        this.nextBillNoCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("sp_next_bill_no")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new org.springframework.jdbc.core.SqlOutParameter("p_bill_no", Types.VARCHAR));
    }

    @Override
    public String nextAppointmentNo(LocalDate date) {
        Map<String, Object> result = nextAppointmentNoCall.execute(Map.of("p_date", java.sql.Date.valueOf(date)));
        return (String) result.get("p_appointment_no");
    }

    @Override
    public String nextBillNo() {
        Map<String, Object> result = nextBillNoCall.execute(Map.of());
        return (String) result.get("p_bill_no");
    }

    /**
     * The next patient code.
     *
     * There is no separate stored procedure for this one, because the schema
     * generates patient codes inside sp_find_or_create_patient. The same
     * counter table is used here, with the same locking behaviour, so the two
     * routes can never hand out the same code.
     *
     * REQUIRES_NEW puts this in its own short transaction. The counter must
     * move on even if the booking that asked for the code is rolled back a
     * moment later; a gap in the numbering is harmless, a repeated code is not.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextPatientCode() {
        jdbcTemplate.update(
                "INSERT INTO code_counter (counter_name, last_number) VALUES ('PATIENT', 1) "
                        + "ON DUPLICATE KEY UPDATE last_number = last_number + 1");

        Integer next = jdbcTemplate.queryForObject(
                "SELECT last_number FROM code_counter WHERE counter_name = 'PATIENT'",
                Integer.class);

        return String.format("PAT-%06d", next == null ? 1 : next);
    }
}
