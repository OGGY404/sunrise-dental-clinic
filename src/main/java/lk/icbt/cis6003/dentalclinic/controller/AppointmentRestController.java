package lk.icbt.cis6003.dentalclinic.controller;

import jakarta.validation.Valid;
import lk.icbt.cis6003.dentalclinic.dto.AppointmentResponse;
import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.dto.CancelRequest;
import lk.icbt.cis6003.dentalclinic.dto.RescheduleRequest;
import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.service.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * The appointment web service (FR2 register, FR3 display, FR7 cancel,
 * reschedule and treatment history).
 *
 * PRESENTATION TIER. This is a distributed application with web services, which
 * the assessment asks for: the clinic screens talk to this over HTTP, so the
 * front desk and the dentists could later run on different machines from the
 * server.
 *
 * WHAT A CONTROLLER IS AND IS NOT ALLOWED TO DO
 * It reads the request, lets Spring refuse anything invalid, calls one business
 * method, and turns the answer into JSON. There is no clinic rule anywhere in
 * this file. Every "if" about dates, slots or statuses lives in
 * AppointmentService, so the rules stay true no matter which screen calls them.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentRestController {

    private final AppointmentService appointmentService;
    private final CurrentUserResolver currentUserResolver;

    public AppointmentRestController(AppointmentService appointmentService,
                                     CurrentUserResolver currentUserResolver) {
        this.appointmentService = appointmentService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * FR2 - register a new appointment.
     *
     * Answers 201 Created with a Location header, which is the correct HTTP way
     * to say "the thing you asked for now exists, and here is where to read
     * it". The screen uses that address for the "view this booking" link.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> register(@Valid @RequestBody BookingRequest request,
                                                        Principal principal) {
        User staff = currentUserResolver.resolve(principal).orElse(null);
        Appointment saved = appointmentService.register(request, staff);

        return ResponseEntity
                .created(URI.create("/api/appointments/" + saved.getAppointmentNo()))
                .body(AppointmentResponse.from(saved));
    }

    /** FR3 - the receptionist types the appointment number and sees the visit. */
    @GetMapping("/{appointmentNo}")
    public AppointmentResponse findOne(@PathVariable String appointmentNo) {
        return AppointmentResponse.from(appointmentService.findByNumber(appointmentNo));
    }

    /**
     * The daily appointment schedule report.
     *
     * With a date alone it is the whole clinic diary for that day. Add a
     * dentist and it becomes that one dentist's list, which is what gets
     * printed and put on their door in the morning.
     */
    @GetMapping
    public List<AppointmentResponse> daySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long dentistId) {

        List<Appointment> found = (dentistId == null)
                ? appointmentService.findDaySchedule(date)
                : appointmentService.findDayScheduleForDentist(dentistId, date);

        return found.stream().map(AppointmentResponse::from).toList();
    }

    /** FR7 - cancel a booking and free the slot for someone else. */
    @PostMapping("/{appointmentNo}/cancel")
    public AppointmentResponse cancel(@PathVariable String appointmentNo,
                                      @Valid @RequestBody CancelRequest request) {
        return AppointmentResponse.from(
                appointmentService.cancel(appointmentNo, request.getReason()));
    }

    /** FR7 - move a booking to a different day or time. */
    @PostMapping("/{appointmentNo}/reschedule")
    public AppointmentResponse reschedule(@PathVariable String appointmentNo,
                                          @Valid @RequestBody RescheduleRequest request) {
        return AppointmentResponse.from(
                appointmentService.reschedule(appointmentNo, request.getNewDate(), request.getNewTime()));
    }

    /**
     * The visit took place. This is what makes it billable, so the front desk
     * presses it as the patient leaves the chair.
     */
    @PostMapping("/{appointmentNo}/complete")
    public AppointmentResponse complete(@PathVariable String appointmentNo) {
        return AppointmentResponse.from(appointmentService.markCompleted(appointmentNo));
    }

    /** The patient never arrived. The slot is not given back. */
    @PostMapping("/{appointmentNo}/no-show")
    public AppointmentResponse noShow(@PathVariable String appointmentNo) {
        return AppointmentResponse.from(appointmentService.markNoShow(appointmentNo));
    }
}
