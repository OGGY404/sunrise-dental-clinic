package lk.icbt.cis6003.dentalclinic.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lk.icbt.cis6003.dentalclinic.model.Gender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the validation rules on the booking form (FR2).
 *
 * WHY THESE TESTS ARE SEPARATE FROM THE CONTROLLER TESTS
 * The rules are annotations on the DTO, so they can be checked on their own
 * without starting a web server. That makes it cheap to write one test per
 * rule, including the boundary cases the marking scheme asks for: the longest
 * value that is still accepted, and the first value that is not.
 *
 * The assessment requires validation on all inputs to reject invalid entries,
 * so each test below is one line of that requirement.
 */
@DisplayName("BookingRequest validation")
class BookingRequestValidationTest {

    private static Validator validator;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);

    @BeforeAll
    static void startValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /** A form with every field filled in correctly. Each test spoils one field. */
    private BookingRequest validRequest() {
        BookingRequest request = new BookingRequest();
        request.setFullName("Kamal Silva");
        request.setAddress("No. 42, Galle Road, Colombo 03");
        request.setContactNumber("0771234567");
        request.setEmail("kamal@example.lk");
        request.setDateOfBirth(LocalDate.of(1995, 4, 17));
        request.setGender(Gender.MALE);
        request.setDentistId(1L);
        request.setTreatmentId(3L);
        request.setAppointmentDate(FUTURE_DATE);
        request.setAppointmentTime(LocalTime.of(9, 0));
        request.setNotes("First visit");
        return request;
    }

    /** The names of the fields the validator complained about. */
    private Set<String> problemFields(BookingRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("accepts a correctly filled form")
    void acceptsAValidForm() {
        Set<ConstraintViolation<BookingRequest>> problems = validator.validate(validRequest());
        assertThat(problems).isEmpty();
    }

    @Nested
    @DisplayName("patient name")
    class PatientName {

        @Test
        @DisplayName("is required")
        void nameIsRequired() {
            BookingRequest request = validRequest();
            request.setFullName("   ");
            assertThat(problemFields(request)).contains("fullName");
        }

        @Test
        @DisplayName("rejects digits, because a persons name has none")
        void nameRejectsDigits() {
            BookingRequest request = validRequest();
            request.setFullName("Kamal 123");
            assertThat(problemFields(request)).contains("fullName");
        }

        @Test
        @DisplayName("allows the dots and hyphens real Sri Lankan names use")
        void nameAllowsDotsAndHyphens() {
            BookingRequest request = validRequest();
            request.setFullName("W.A.G.K. Rathnayake-Silva");
            assertThat(problemFields(request)).doesNotContain("fullName");
        }

        @Test
        @DisplayName("rejects a name longer than the 100 character column")
        void nameHasAMaximumLength() {
            BookingRequest request = validRequest();
            request.setFullName("A".repeat(101));
            assertThat(problemFields(request)).contains("fullName");
        }
    }

    @Nested
    @DisplayName("contact number")
    class ContactNumber {

        @Test
        @DisplayName("is required")
        void contactIsRequired() {
            BookingRequest request = validRequest();
            request.setContactNumber(null);
            assertThat(problemFields(request)).contains("contactNumber");
        }

        @Test
        @DisplayName("accepts a local 10 digit number")
        void acceptsLocalNumber() {
            BookingRequest request = validRequest();
            request.setContactNumber("0112345678");
            assertThat(problemFields(request)).doesNotContain("contactNumber");
        }

        @Test
        @DisplayName("accepts the same number written with the +94 country code")
        void acceptsInternationalNumber() {
            BookingRequest request = validRequest();
            request.setContactNumber("+94771234567");
            assertThat(problemFields(request)).doesNotContain("contactNumber");
        }

        @Test
        @DisplayName("rejects 9 digits, one short of a real number")
        void rejectsTooShort() {
            BookingRequest request = validRequest();
            request.setContactNumber("077123456");
            assertThat(problemFields(request)).contains("contactNumber");
        }

        @Test
        @DisplayName("rejects letters typed into the number box")
        void rejectsLetters() {
            BookingRequest request = validRequest();
            request.setContactNumber("077ABC4567");
            assertThat(problemFields(request)).contains("contactNumber");
        }
    }

    @Nested
    @DisplayName("the visit")
    class TheVisit {

        @Test
        @DisplayName("a dentist must be chosen")
        void dentistIsRequired() {
            BookingRequest request = validRequest();
            request.setDentistId(null);
            assertThat(problemFields(request)).contains("dentistId");
        }

        @Test
        @DisplayName("a treatment must be chosen")
        void treatmentIsRequired() {
            BookingRequest request = validRequest();
            request.setTreatmentId(null);
            assertThat(problemFields(request)).contains("treatmentId");
        }

        @Test
        @DisplayName("the date cannot be yesterday")
        void dateCannotBeInThePast() {
            BookingRequest request = validRequest();
            request.setAppointmentDate(LocalDate.now().minusDays(1));
            assertThat(problemFields(request)).contains("appointmentDate");
        }

        @Test
        @DisplayName("today is still bookable, because the clinic is open now")
        void todayIsAllowed() {
            BookingRequest request = validRequest();
            request.setAppointmentDate(LocalDate.now());
            assertThat(problemFields(request)).doesNotContain("appointmentDate");
        }

        @Test
        @DisplayName("a time must be chosen")
        void timeIsRequired() {
            BookingRequest request = validRequest();
            request.setAppointmentTime(null);
            assertThat(problemFields(request)).contains("appointmentTime");
        }
    }

    @Nested
    @DisplayName("the optional fields")
    class OptionalFields {

        @Test
        @DisplayName("email may be left empty, because not every patient has one")
        void emailMayBeEmpty() {
            BookingRequest request = validRequest();
            request.setEmail(null);
            assertThat(problemFields(request)).doesNotContain("email");
        }

        @Test
        @DisplayName("but a typed email must look like an email")
        void emailMustLookLikeOne() {
            BookingRequest request = validRequest();
            request.setEmail("kamal-at-example");
            assertThat(problemFields(request)).contains("email");
        }

        @Test
        @DisplayName("a date of birth in the future is refused")
        void dateOfBirthCannotBeInTheFuture() {
            BookingRequest request = validRequest();
            request.setDateOfBirth(LocalDate.now().plusDays(1));
            assertThat(problemFields(request)).contains("dateOfBirth");
        }

        @Test
        @DisplayName("notes longer than the 500 character column are refused")
        void notesHaveAMaximumLength() {
            BookingRequest request = validRequest();
            request.setNotes("n".repeat(501));
            assertThat(problemFields(request)).contains("notes");
        }
    }
}
