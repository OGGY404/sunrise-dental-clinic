package lk.icbt.cis6003.dentalclinic.model;

/**
 * What a member of staff is allowed to do (FR7 - role based access).
 *
 * ADMIN        sees everything, including the reports and the price list.
 * RECEPTIONIST books appointments and prints bills, but cannot change prices.
 *
 * Spring Security expects role names to start with "ROLE_", so the security
 * configuration in step 6 adds that prefix. The database column stores the
 * short name, which is easier to read in MySQL Workbench.
 */
public enum Role {
    ADMIN,
    RECEPTIONIST
}
