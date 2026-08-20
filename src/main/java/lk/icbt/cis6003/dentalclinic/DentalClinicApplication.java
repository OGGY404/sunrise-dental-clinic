package lk.icbt.cis6003.dentalclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Sunrise Dental Clinic Appointment and Patient Management System.
 *
 * <p>The application is organised into three tiers, each in its own package:</p>
 * <ul>
 *   <li>{@code controller} - presentation tier (web pages and REST endpoints)</li>
 *   <li>{@code service}    - business logic tier (rules, calculations, patterns)</li>
 *   <li>{@code repository} - data access tier (database reads and writes)</li>
 * </ul>
 *
 * CIS6003 Advanced Programming - WRIT1
 */
@SpringBootApplication
public class DentalClinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentalClinicApplication.class, args);
    }
}
