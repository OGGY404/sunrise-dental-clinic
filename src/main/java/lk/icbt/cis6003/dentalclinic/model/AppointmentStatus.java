package lk.icbt.cis6003.dentalclinic.model;

/**
 * The life of an appointment.
 *
 * BOOKED    the slot is reserved and blocks anyone else taking it
 * COMPLETED the patient came and was treated, so it can be billed
 * CANCELLED called off; the slot goes back on sale
 * NO_SHOW   the patient never arrived; the slot is gone but nothing is owed
 *
 * Only CANCELLED frees the time slot. That rule lives in the database as well,
 * in the slot_key generated column (see db/schema.sql).
 */
public enum AppointmentStatus {
    BOOKED,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
