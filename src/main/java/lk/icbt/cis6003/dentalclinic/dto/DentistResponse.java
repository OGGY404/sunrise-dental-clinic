package lk.icbt.cis6003.dentalclinic.dto;

import lk.icbt.cis6003.dentalclinic.model.Dentist;

/**
 * One line of the "choose a dentist" dropdown on the booking form.
 *
 * The id is included because that is what the booking form sends back. The
 * code and the name are included because that is what the receptionist reads.
 */
public class DentistResponse {

    private final Long dentistId;
    private final String dentistCode;
    private final String fullName;
    private final String specialisation;

    private DentistResponse(Dentist dentist) {
        this.dentistId = dentist.getDentistId();
        this.dentistCode = dentist.getDentistCode();
        this.fullName = dentist.getFullName();
        this.specialisation = dentist.getSpecialisation();
    }

    public static DentistResponse from(Dentist dentist) {
        return new DentistResponse(dentist);
    }

    public Long getDentistId() {
        return dentistId;
    }

    public String getDentistCode() {
        return dentistCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getSpecialisation() {
        return specialisation;
    }
}
