package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.dto.DentistWorkloadRow;
import lk.icbt.cis6003.dentalclinic.dto.RevenueByTreatmentRow;
import lk.icbt.cis6003.dentalclinic.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the management reports (the "reports that add value" the brief
 * asks for).
 *
 * WHY THESE REPORTS ARE NOT WRITTEN IN JAVA
 * They are stored procedures, called from here. The brief asks for advanced
 * database features to be used, not merely present, and a report that groups
 * and totals thousands of rows is exactly the work a database does better than
 * Java: the arithmetic happens where the data already is, and only the finished
 * summary travels back.
 *
 * Mockito replaces JdbcTemplate here, so these tests check the part that is
 * ours - that the right procedure is called with the right dates, and that a
 * backwards date range is refused - without needing MySQL. The SQL inside the
 * procedures is exercised against the real database by hand.
 */
@DisplayName("ReportService")
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReportService service;

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    @BeforeEach
    void setUp() {
        service = new ReportService(jdbcTemplate);
    }

    @Test
    @DisplayName("revenue report calls the stored procedure with the two dates")
    void revenueCallsTheStoredProcedure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of());

        service.revenueByTreatment(FROM, TO);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), eq(FROM), eq(TO));

        assertThat(sql.getValue()).contains("sp_report_revenue_by_treatment");
    }

    @Test
    @DisplayName("revenue report hands back the rows the database produced")
    void revenueReturnsTheRows() {
        RevenueByTreatmentRow row = new RevenueByTreatmentRow(
                "TRT-005", "Root Canal Treatment", 3,
                new BigDecimal("86250.00"), new BigDecimal("4500.00"),
                new BigDecimal("500.00"), new BigDecimal("90250.00"),
                new BigDecimal("30083.33"));

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of(row));

        assertThat(service.revenueByTreatment(FROM, TO))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.treatmentName()).isEqualTo("Root Canal Treatment");
                    assertThat(result.totalRevenue()).isEqualByComparingTo("90250.00");
                });
    }

    @Test
    @DisplayName("workload report calls its own stored procedure")
    void workloadCallsTheStoredProcedure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of());

        service.dentistWorkload(FROM, TO);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), eq(FROM), eq(TO));

        assertThat(sql.getValue()).contains("sp_report_dentist_workload");
    }

    @Test
    @DisplayName("workload report hands back the rows the database produced")
    void workloadReturnsTheRows() {
        DentistWorkloadRow row = new DentistWorkloadRow(
                "DEN-003", "Dr. Ruwan Jayasinghe", "Oral Surgery",
                12, 9, 2, 1, 540, new BigDecimal("214000.00"));

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of(row));

        assertThat(service.dentistWorkload(FROM, TO))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.dentistName()).isEqualTo("Dr. Ruwan Jayasinghe");
                    assertThat(result.completed()).isEqualTo(9);
                });
    }

    @Test
    @DisplayName("a date range that runs backwards is refused before the database is troubled")
    void backwardsDateRangeIsRefused() {
        assertThatThrownBy(() -> service.revenueByTreatment(TO, FROM))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("after");

        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(), any());
    }

    @Test
    @DisplayName("a missing date is refused, rather than quietly reporting on everything")
    void missingDateIsRefused() {
        assertThatThrownBy(() -> service.dentistWorkload(null, TO))
                .isInstanceOf(BadRequestException.class);

        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(), any());
    }

    @Test
    @DisplayName("the same day counts as a valid range, for a single day report")
    void oneDayRangeIsAllowed() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of());

        assertThat(service.revenueByTreatment(FROM, FROM)).isEmpty();
    }
}
