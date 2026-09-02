package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.dto.ReminderSummary;
import lk.icbt.cis6003.dentalclinic.dto.UpcomingReminderRow;
import lk.icbt.cis6003.dentalclinic.exception.BadRequestException;
import lk.icbt.cis6003.dentalclinic.service.notification.ReminderNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for appointment reminders (FR7 - "email or SMS appointment reminders").
 *
 * WHY THE LIST COMES FROM A STORED PROCEDURE
 * sp_report_upcoming_reminders already existed in procedures.sql but nothing
 * ever called it, which is the same as it not being there. It answers one
 * question - who is coming in N days and how do we reach them - by joining four
 * tables, and that is work the database does better than Java.
 *
 * WHY NOTHING HERE MAY THROW
 * A reminder is a courtesy. A mail server being unreachable must not stop the
 * other patients being reminded, and must certainly not fail the nightly job.
 * That is the same lesson the Observer tests already record for booking.
 */
@DisplayName("ReminderService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ReminderNotifier emailNotifier;
    @Mock
    private ReminderNotifier loggingNotifier;

    private ReminderService service;

    @BeforeEach
    void setUp() {
        service = new ReminderService(jdbcTemplate, List.of(emailNotifier, loggingNotifier));
    }

    private UpcomingReminderRow reminderFor(String patient, String email) {
        return new UpcomingReminderRow(
                "APT-20260903-0001",
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                patient,
                "0771234567",
                email,
                "Dr. Nimal Perera",
                "Tooth Filling");
    }

    @Nested
    @DisplayName("reading the list")
    class ReadingTheList {

        @Test
        @DisplayName("asks the stored procedure, with the number of days ahead")
        void callsTheStoredProcedure() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

            service.findUpcoming(1);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), eq(1));

            assertThat(sql.getValue()).contains("sp_report_upcoming_reminders");
        }

        @Test
        @DisplayName("hands back the rows the database produced")
        void returnsTheRows() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                    .thenReturn(List.of(reminderFor("Kamal Silva", "kamal@example.lk")));

            assertThat(service.findUpcoming(1))
                    .singleElement()
                    .satisfies(row -> {
                        assertThat(row.patientName()).isEqualTo("Kamal Silva");
                        assertThat(row.canBeEmailed()).isTrue();
                    });
        }

        @Test
        @DisplayName("today is allowed, for a same-day telephone round")
        void todayIsAllowed() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

            assertThat(service.findUpcoming(0)).isEmpty();
        }

        @Test
        @DisplayName("a negative number of days is refused")
        void negativeDaysIsRefused() {
            assertThatThrownBy(() -> service.findUpcoming(-1))
                    .isInstanceOf(BadRequestException.class);

            verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any());
        }

        @Test
        @DisplayName("more than thirty days ahead is refused, because that is not a reminder")
        void tooFarAheadIsRefused() {
            assertThatThrownBy(() -> service.findUpcoming(31))
                    .isInstanceOf(BadRequestException.class);

            verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any());
        }
    }

    @Nested
    @DisplayName("sending the reminders")
    class SendingReminders {

        @Test
        @DisplayName("gives every reminder to every notifier")
        void everyNotifierGetsEveryReminder() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                    .thenReturn(List.of(
                            reminderFor("Kamal Silva", "kamal@example.lk"),
                            reminderFor("Nadeeka Fernando", "nadeeka@example.lk")));

            service.sendReminders(1);

            verify(emailNotifier, times(2)).remind(any(UpcomingReminderRow.class));
            verify(loggingNotifier, times(2)).remind(any(UpcomingReminderRow.class));
        }

        @Test
        @DisplayName("counts how many of tomorrow's patients can actually be reached by email")
        void countsWhoCanBeReached() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                    .thenReturn(List.of(
                            reminderFor("Kamal Silva", "kamal@example.lk"),
                            reminderFor("Saman Kumara", null),
                            reminderFor("Anusha Perera", "   ")));

            ReminderSummary summary = service.sendReminders(1);

            assertThat(summary.found()).isEqualTo(3);
            assertThat(summary.emailed()).isEqualTo(1);
            assertThat(summary.needTelephoning()).isEqualTo(2);
        }

        @Test
        @DisplayName("one notifier failing does not stop the others, or the rest of the list")
        void aFailingNotifierDoesNotStopTheRound() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                    .thenReturn(List.of(
                            reminderFor("Kamal Silva", "kamal@example.lk"),
                            reminderFor("Nadeeka Fernando", "nadeeka@example.lk")));

            doThrow(new RuntimeException("mail server unreachable"))
                    .when(emailNotifier).remind(any(UpcomingReminderRow.class));

            ReminderSummary summary = service.sendReminders(1);

            // The round completed and the other notifier still saw both rows.
            assertThat(summary.found()).isEqualTo(2);
            verify(loggingNotifier, times(2)).remind(any(UpcomingReminderRow.class));
        }

        @Test
        @DisplayName("an empty day is not an error, it is a quiet day")
        void anEmptyDayIsFine() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

            ReminderSummary summary = service.sendReminders(1);

            assertThat(summary.found()).isZero();
            verify(emailNotifier, never()).remind(any(UpcomingReminderRow.class));
        }
    }
}
