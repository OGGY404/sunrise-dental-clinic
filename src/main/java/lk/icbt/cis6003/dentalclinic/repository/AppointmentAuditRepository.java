package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.AppointmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only data access for the appointment audit trail.
 * DESIGN PATTERN: Repository.
 *
 * Nothing here writes. The rows are created by the database triggers in
 * db/triggers.sql. This repository only lets an ADMIN read the trail back on
 * screen, so the clinic can see who moved or cancelled an appointment.
 */
@Repository
public interface AppointmentAuditRepository extends JpaRepository<AppointmentAudit, Long> {

    List<AppointmentAudit> findByAppointmentIdOrderByChangedAtDesc(Long appointmentId);

    List<AppointmentAudit> findByAppointmentNoOrderByChangedAtDesc(String appointmentNo);

    /** The most recent changes across the whole clinic, for the admin screen. */
    List<AppointmentAudit> findTop50ByOrderByChangedAtDesc();
}
