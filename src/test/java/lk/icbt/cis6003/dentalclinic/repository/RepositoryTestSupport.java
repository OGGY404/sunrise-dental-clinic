package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Gender;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.Role;
import lk.icbt.cis6003.dentalclinic.model.Treatment;
import lk.icbt.cis6003.dentalclinic.model.User;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Shared setup for every repository test.
 *
 * Why a base class:
 * every repository test needs the same three annotations and the same kind of
 * sample rows. Putting them here keeps each test file short, so the test itself
 * is the only thing you read in it.
 *
 * @DataJpaTest loads only the JPA parts of the application, not the web pages or
 * security. That makes these tests start in about a second instead of ten.
 *
 * AutoConfigureTestDatabase(replace = NONE) tells Spring to use the H2 settings
 * we wrote in application-test.properties, instead of guessing its own.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
abstract class RepositoryTestSupport {

    /** A staff login. The hash is a real BCrypt hash of "Admin@123". */
    protected User newUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("$2b$10$3CG1Xb9LPd/ERcwjWy5gSe1CIFtttVsTc4FYD2lcjzsHClY/WLjj2");
        user.setFullName("Test " + username);
        user.setEmail(username + "@sunrisedental.lk");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    protected Dentist newDentist(String code, String name) {
        Dentist dentist = new Dentist();
        dentist.setDentistCode(code);
        dentist.setFullName(name);
        dentist.setSpecialisation("General Dentistry");
        dentist.setContactNumber("0771234501");
        dentist.setEmail(code.toLowerCase() + "@sunrisedental.lk");
        dentist.setActive(true);
        return dentist;
    }

    protected Treatment newTreatment(String code, String name, String cost) {
        Treatment treatment = new Treatment();
        treatment.setTreatmentCode(code);
        treatment.setName(name);
        treatment.setDescription(name + " description");
        treatment.setCost(new BigDecimal(cost));
        treatment.setDurationMinutes(30);
        treatment.setActive(true);
        return treatment;
    }

    protected Patient newPatient(String code, String name, String contactNumber) {
        Patient patient = new Patient();
        patient.setPatientCode(code);
        patient.setFullName(name);
        patient.setAddress("No. 42, Galle Road, Colombo 03");
        patient.setContactNumber(contactNumber);
        patient.setEmail("patient@example.lk");
        patient.setDateOfBirth(LocalDate.of(1995, 4, 17));
        patient.setGender(Gender.MALE);
        return patient;
    }

    protected Appointment newAppointment(String appointmentNo,
                                         Patient patient,
                                         Dentist dentist,
                                         Treatment treatment,
                                         LocalDate date,
                                         LocalTime time) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(appointmentNo);
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setTreatment(treatment);
        appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(time);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setNotes("Created by a repository test");
        return appointment;
    }

    protected Bill newBill(String billNo, Appointment appointment, String treatmentCost) {
        Bill bill = new Bill();
        bill.setBillNo(billNo);
        bill.setAppointment(appointment);
        bill.setTreatmentCost(new BigDecimal(treatmentCost));
        bill.setConsultationFee(new BigDecimal("1500.00"));
        bill.setDiscount(BigDecimal.ZERO);
        return bill;
    }
}
