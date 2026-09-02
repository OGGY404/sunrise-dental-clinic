package lk.icbt.cis6003.dentalclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One line of the appointment audit trail.
 *
 * READ ONLY FROM JAVA. Nothing in this application writes to this table. The
 * rows are put there by the database triggers in db/triggers.sql every time an
 * appointment is inserted or changed.
 *
 * That is the point of it. Because the database writes the record rather than
 * the program, the trail still captures a change made through the REST API, a
 * script, or somebody typing SQL into MySQL Workbench. The paper system could
 * not tell the clinic who moved an appointment; this can.
 */
@Entity
@Table(name = "appointment_audit")
public class AppointmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    /**
     * Plain id numbers, not a link to the Appointment object. The audit trail
     * has to survive on its own and stay readable even if the appointment it
     * describes is later removed.
     */
    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "appointment_no", nullable = false, length = 20)
    private String appointmentNo;

    /** CREATED, CANCELLED, COMPLETED, RESCHEDULED or UPDATED. */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "old_slot", length = 64)
    private String oldSlot;

    @Column(name = "new_slot", length = 64)
    private String newSlot;

    /** The username the Java code put into the MySQL variable @app_user. */
    @Column(name = "changed_by", nullable = false, length = 80)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public AppointmentAudit() {
    }

    public Long getAuditId() {
        return auditId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public String getAction() {
        return action;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getOldSlot() {
        return oldSlot;
    }

    public String getNewSlot() {
        return newSlot;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    @Override
    public String toString() {
        return "AppointmentAudit{no=" + appointmentNo
                + ", action=" + action
                + ", by=" + changedBy
                + ", at=" + changedAt + "}";
    }
}
