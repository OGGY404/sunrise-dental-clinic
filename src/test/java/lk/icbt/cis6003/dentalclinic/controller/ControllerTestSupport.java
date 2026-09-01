package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;
import lk.icbt.cis6003.dentalclinic.model.Bill;
import lk.icbt.cis6003.dentalclinic.model.Dentist;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.model.Treatment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Shared sample data for the web tests.
 *
 * Every controller test needs the same patient, dentist and treatment. Building
 * them here keeps each test file short, so the only thing you read in a test is
 * the behaviour it is checking.
 *
 * The dates are worked out from today, never written down as fixed days, so
 * these tests still pass next year.
 */
abstract class ControllerTestSupport {

    /** Far enough ahead that a booking for this day is always allowed. */
    protected static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);
    protected static final LocalTime NINE_AM = LocalTime.of(9, 0);

    protected Dentist drPerera() {
        Dentist dentist = new Dentist("DEN-001", "Dr. Nimal Perera", "General Dentistry");
        dentist.setDentistId(1L);
        dentist.setActive(true);
        return dentist;
    }

    protected Treatment filling() {
        Treatment treatment = new Treatment("TRT-003", "Tooth Filling", new BigDecimal("6000.00"), 45);
        treatment.setTreatmentId(3L);
        treatment.setActive(true);
        return treatment;
    }

    protected Patient kamal() {
        Patient patient = new Patient("PAT-000001", "Kamal Silva", "No. 42, Galle Road, Colombo 03", "0771234567");
        patient.setPatientId(10L);
        patient.setEmail("kamal@example.lk");
        patient.setDateOfBirth(LocalDate.of(1995, 4, 17));
        return patient;
    }

    /** A booked appointment with everything filled in, as the service would return it. */
    protected Appointment bookedAppointment() {
        Appointment appointment = Appointment.builder()
                .appointmentNo("APT-20260907-0001")
                .patient(kamal())
                .dentist(drPerera())
                .treatment(filling())
                .on(FUTURE_DATE)
                .at(NINE_AM)
                .notes("First visit")
                .build();
        appointment.setAppointmentId(100L);
        return appointment;
    }

    protected Appointment appointmentWithStatus(AppointmentStatus status) {
        Appointment appointment = bookedAppointment();
        appointment.setStatus(status);
        return appointment;
    }

    /** An unpaid bill for a completed visit. */
    protected Bill unpaidBill() {
        Bill bill = new Bill();
        bill.setBillId(500L);
        bill.setBillNo("BIL-20260907-0001");
        bill.setAppointment(appointmentWithStatus(AppointmentStatus.COMPLETED));
        bill.setTreatmentCost(new BigDecimal("6000.00"));
        bill.setConsultationFee(new BigDecimal("1500.00"));
        bill.setDiscount(BigDecimal.ZERO);
        return bill;
    }

    /** The JSON a correctly filled booking form sends. */
    protected String validBookingJson() {
        return """
                {
                  "fullName": "Kamal Silva",
                  "address": "No. 42, Galle Road, Colombo 03",
                  "contactNumber": "0771234567",
                  "email": "kamal@example.lk",
                  "dateOfBirth": "1995-04-17",
                  "gender": "MALE",
                  "dentistId": 1,
                  "treatmentId": 3,
                  "appointmentDate": "%s",
                  "appointmentTime": "09:00:00",
                  "notes": "First visit"
                }
                """.formatted(FUTURE_DATE);
    }
}
