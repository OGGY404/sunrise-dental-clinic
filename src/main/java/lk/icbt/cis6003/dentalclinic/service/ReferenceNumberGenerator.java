package lk.icbt.cis6003.dentalclinic.service;

import java.time.LocalDate;

/**
 * Hands out the reference numbers the clinic prints on paper: appointment
 * numbers, bill numbers and patient codes.
 *
 * WHY THIS IS AN INTERFACE AND NOT JUST A CLASS
 * The real implementation calls stored procedures in MySQL, because only the
 * database can guarantee that two receptionists pressing Save at the same
 * instant get different numbers. That makes it impossible to use in a fast unit
 * test, which must not need a database.
 *
 * Putting an interface here means the services depend on the idea of "give me
 * the next number", not on MySQL. The tests supply a simple stand-in, and the
 * production code supplies the real thing. This is the Dependency Inversion
 * principle, and it is the reason AppointmentServiceTest runs in milliseconds.
 */
public interface ReferenceNumberGenerator {

    /**
     * The next appointment number for that date, such as APT-20260907-0007.
     * The sequence restarts each day.
     */
    String nextAppointmentNo(LocalDate date);

    /** The next bill number for today, such as BIL-20260907-0003. */
    String nextBillNo();

    /** The next patient code, such as PAT-000042. This one never restarts. */
    String nextPatientCode();
}
