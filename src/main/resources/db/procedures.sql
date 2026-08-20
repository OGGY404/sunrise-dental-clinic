-- ===========================================================================
--  Sunrise Dental Clinic - STORED PROCEDURES AND FUNCTIONS
--  CIS6003 Advanced Programming (WRIT1)
--
--  The marking brief asks for advanced database features. This file holds the
--  stored functions and procedures. Putting these rules inside the database
--  means they hold true for every client - the web pages, the REST API, or a
--  clerk typing SQL by hand - not only for the Java code.
--
--  Statements are separated by ";;" - see the note at the top of schema.sql.
--
--  NOTE ON FUNCTIONS
--  A MySQL server with binary logging enabled refuses to create a stored
--  FUNCTION that changes data (error 1418). Every function below therefore
--  only reads, and is declared READS SQL DATA. Anything that writes is a
--  stored PROCEDURE instead, which has no such restriction. This keeps the
--  scripts working on a default MySQL install with no extra privileges.
-- ===========================================================================


-- ===========================================================================
--  FUNCTIONS (read-only)
-- ===========================================================================

-- ---------------------------------------------------------------------------
--  fn_get_setting - reads one clinic-wide setting, e.g. the consultation fee.
-- ---------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_get_setting;;

CREATE FUNCTION fn_get_setting(p_key VARCHAR(60))
RETURNS VARCHAR(255)
READS SQL DATA
BEGIN
    DECLARE v_value VARCHAR(255);

    SELECT setting_value INTO v_value
      FROM clinic_settings
     WHERE setting_key = p_key;

    RETURN v_value;
END;;


-- ---------------------------------------------------------------------------
--  fn_consultation_fee - the consultation fee as a number, ready for maths.
--  Falls back to 0 if the setting has been deleted, so a bill never breaks.
-- ---------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_consultation_fee;;

CREATE FUNCTION fn_consultation_fee()
RETURNS DECIMAL(10,2)
READS SQL DATA
BEGIN
    RETURN CAST(COALESCE(fn_get_setting('consultation_fee'), '0') AS DECIMAL(10,2));
END;;


-- ---------------------------------------------------------------------------
--  fn_is_dentist_available - TRUE when that dentist has no live appointment
--  at that date and time. Cancelled appointments do not block the slot.
--
--  Used by sp_register_appointment to give a clear error message. The UNIQUE
--  index on appointments.slot_key is the real guarantee; this is the polite
--  check that happens first.
-- ---------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_is_dentist_available;;

CREATE FUNCTION fn_is_dentist_available(
    p_dentist_id BIGINT,
    p_date       DATE,
    p_time       TIME
)
RETURNS BOOLEAN
READS SQL DATA
BEGIN
    DECLARE v_clashes INT;

    SELECT COUNT(*) INTO v_clashes
      FROM appointments
     WHERE dentist_id       = p_dentist_id
       AND appointment_date = p_date
       AND appointment_time = p_time
       AND status <> 'CANCELLED';

    RETURN (v_clashes = 0);
END;;


-- ---------------------------------------------------------------------------
--  fn_is_within_opening_hours - TRUE when the time falls inside the clinic's
--  working day, read from clinic_settings so management can change it.
-- ---------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_is_within_opening_hours;;

CREATE FUNCTION fn_is_within_opening_hours(p_time TIME)
RETURNS BOOLEAN
READS SQL DATA
BEGIN
    DECLARE v_open  TIME;
    DECLARE v_close TIME;

    SET v_open  = CAST(COALESCE(fn_get_setting('opening_time'), '08:00:00') AS TIME);
    SET v_close = CAST(COALESCE(fn_get_setting('closing_time'), '18:00:00') AS TIME);

    RETURN (p_time >= v_open AND p_time < v_close);
END;;


-- ---------------------------------------------------------------------------
--  fn_calculate_bill_total - the FR4 calculation in one place:
--      treatment cost + consultation fee - discount
-- ---------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_calculate_bill_total;;

CREATE FUNCTION fn_calculate_bill_total(
    p_treatment_id BIGINT,
    p_discount     DECIMAL(10,2)
)
RETURNS DECIMAL(10,2)
READS SQL DATA
BEGIN
    DECLARE v_treatment_cost DECIMAL(10,2);

    SELECT cost INTO v_treatment_cost
      FROM treatments
     WHERE treatment_id = p_treatment_id;

    IF v_treatment_cost IS NULL THEN
        RETURN NULL;
    END IF;

    RETURN v_treatment_cost + fn_consultation_fee() - COALESCE(p_discount, 0.00);
END;;


-- ---------------------------------------------------------------------------
--  fn_patient_visit_count - how many completed visits a patient has had.
--  Used by the treatment-history screen and by the loyalty discount rule.
-- ---------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_patient_visit_count;;

CREATE FUNCTION fn_patient_visit_count(p_patient_id BIGINT)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE v_count INT;

    SELECT COUNT(*) INTO v_count
      FROM appointments
     WHERE patient_id = p_patient_id
       AND status     = 'COMPLETED';

    RETURN v_count;
END;;


-- ===========================================================================
--  PROCEDURES (may change data)
-- ===========================================================================

-- ---------------------------------------------------------------------------
--  sp_next_appointment_no - hands out the next number for today, such as
--  APT-20260820-0007.
--
--  The INSERT ... ON DUPLICATE KEY UPDATE creates today's counter row if it is
--  the first booking of the day, and otherwise adds one to it. The database
--  locks that single row for the moment it takes, so two people booking at the
--  same instant get 0007 and 0008 - never 0007 twice.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_next_appointment_no;;

CREATE PROCEDURE sp_next_appointment_no(
    IN  p_date DATE,
    OUT p_appointment_no VARCHAR(20)
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_next INT;

    INSERT INTO daily_counter (counter_name, counter_date, last_number)
    VALUES ('APPOINTMENT', p_date, 1)
    ON DUPLICATE KEY UPDATE last_number = last_number + 1;

    SELECT last_number INTO v_next
      FROM daily_counter
     WHERE counter_name = 'APPOINTMENT'
       AND counter_date = p_date;

    SET p_appointment_no = CONCAT('APT-', DATE_FORMAT(p_date, '%Y%m%d'), '-', LPAD(v_next, 4, '0'));
END;;


-- ---------------------------------------------------------------------------
--  sp_next_bill_no - the same idea for bills: BIL-20260820-0003.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_next_bill_no;;

CREATE PROCEDURE sp_next_bill_no(
    OUT p_bill_no VARCHAR(20)
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_next INT;
    DECLARE v_today DATE;

    SET v_today = CURDATE();

    INSERT INTO daily_counter (counter_name, counter_date, last_number)
    VALUES ('BILL', v_today, 1)
    ON DUPLICATE KEY UPDATE last_number = last_number + 1;

    SELECT last_number INTO v_next
      FROM daily_counter
     WHERE counter_name = 'BILL'
       AND counter_date = v_today;

    SET p_bill_no = CONCAT('BIL-', DATE_FORMAT(v_today, '%Y%m%d'), '-', LPAD(v_next, 4, '0'));
END;;


-- ---------------------------------------------------------------------------
--  sp_find_or_create_patient  (FR2)
--
--  A returning patient must not be duplicated, otherwise their treatment
--  history splits in two. A patient is treated as the same person when the
--  contact number and the name both match.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_find_or_create_patient;;

CREATE PROCEDURE sp_find_or_create_patient(
    IN  p_full_name      VARCHAR(100),
    IN  p_address        VARCHAR(255),
    IN  p_contact_number VARCHAR(15),
    IN  p_email          VARCHAR(120),
    IN  p_date_of_birth  DATE,
    IN  p_gender         VARCHAR(10),
    OUT p_patient_id     BIGINT,
    OUT p_was_created    BOOLEAN
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_next INT;
    DECLARE v_code VARCHAR(20);

    SET p_patient_id = NULL;

    SELECT patient_id INTO p_patient_id
      FROM patients
     WHERE contact_number = p_contact_number
       AND full_name      = p_full_name
     LIMIT 1;

    IF p_patient_id IS NOT NULL THEN
        -- Known patient: refresh the details in case they have moved house
        UPDATE patients
           SET address       = p_address,
               email         = COALESCE(p_email, email),
               date_of_birth = COALESCE(p_date_of_birth, date_of_birth),
               gender        = COALESCE(p_gender, gender)
         WHERE patient_id = p_patient_id;

        SET p_was_created = FALSE;
    ELSE
        -- New patient: take the next code, then insert
        INSERT INTO code_counter (counter_name, last_number)
        VALUES ('PATIENT', 1)
        ON DUPLICATE KEY UPDATE last_number = last_number + 1;

        SELECT last_number INTO v_next
          FROM code_counter
         WHERE counter_name = 'PATIENT';

        SET v_code = CONCAT('PAT-', LPAD(v_next, 6, '0'));

        INSERT INTO patients (patient_code, full_name, address, contact_number,
                              email, date_of_birth, gender)
        VALUES (v_code, p_full_name, p_address, p_contact_number,
                p_email, p_date_of_birth, p_gender);

        SET p_patient_id  = LAST_INSERT_ID();
        SET p_was_created = TRUE;
    END IF;
END;;


-- ---------------------------------------------------------------------------
--  sp_register_appointment  (FR2 - the main booking use case)
--
--  Does the whole booking as one unit of work: find or create the patient,
--  check the slot is free, take the next appointment number, and insert the
--  row. If any step fails the whole thing is rolled back, so the clinic can
--  never end up with a patient record and no appointment.
--
--  The two DECLARE ... HANDLER blocks turn database errors into messages a
--  receptionist can understand. SQLSTATE '23000' is the duplicate-key error
--  raised by the UNIQUE index on slot_key when another user takes the same
--  slot a fraction of a second earlier.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_register_appointment;;

CREATE PROCEDURE sp_register_appointment(
    IN  p_full_name       VARCHAR(100),
    IN  p_address         VARCHAR(255),
    IN  p_contact_number  VARCHAR(15),
    IN  p_email           VARCHAR(120),
    IN  p_date_of_birth   DATE,
    IN  p_gender          VARCHAR(10),
    IN  p_dentist_id      BIGINT,
    IN  p_treatment_id    BIGINT,
    IN  p_appointment_date DATE,
    IN  p_appointment_time TIME,
    IN  p_notes           VARCHAR(500),
    IN  p_created_by      BIGINT,
    OUT p_appointment_no  VARCHAR(20),
    OUT p_patient_id      BIGINT
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_was_created BOOLEAN;
    DECLARE v_dentist_ok  INT;
    DECLARE v_treatment_ok INT;

    -- A slot taken by another user in the same instant
    DECLARE EXIT HANDLER FOR SQLSTATE '23000'
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'That dentist is already booked for this date and time. Please choose another slot.';
    END;

    -- Anything else unexpected
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- --- validation ------------------------------------------------------
    SELECT COUNT(*) INTO v_dentist_ok
      FROM dentists WHERE dentist_id = p_dentist_id AND active = TRUE;

    IF v_dentist_ok = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'The selected dentist does not exist or is not currently practising.';
    END IF;

    SELECT COUNT(*) INTO v_treatment_ok
      FROM treatments WHERE treatment_id = p_treatment_id AND active = TRUE;

    IF v_treatment_ok = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'The selected treatment is not on the current price list.';
    END IF;

    IF NOT fn_is_dentist_available(p_dentist_id, p_appointment_date, p_appointment_time) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'That dentist is already booked for this date and time. Please choose another slot.';
    END IF;

    -- --- do the work -----------------------------------------------------
    CALL sp_find_or_create_patient(
        p_full_name, p_address, p_contact_number, p_email,
        p_date_of_birth, p_gender, p_patient_id, v_was_created);

    CALL sp_next_appointment_no(p_appointment_date, p_appointment_no);

    INSERT INTO appointments (appointment_no, patient_id, dentist_id, treatment_id,
                              appointment_date, appointment_time, status, notes, created_by)
    VALUES (p_appointment_no, p_patient_id, p_dentist_id, p_treatment_id,
            p_appointment_date, p_appointment_time, 'BOOKED', p_notes, p_created_by);

    COMMIT;
END;;


-- ---------------------------------------------------------------------------
--  sp_find_appointment  (FR3 - display appointment details)
--
--  One call returns everything the details screen shows, by joining the four
--  tables together. Doing the join here rather than in Java means the database
--  sends one row instead of four separate result sets over the network.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_find_appointment;;

CREATE PROCEDURE sp_find_appointment(
    IN p_appointment_no VARCHAR(20)
)
READS SQL DATA
BEGIN
    SELECT a.appointment_id,
           a.appointment_no,
           a.appointment_date,
           a.appointment_time,
           a.status,
           a.notes,
           a.created_at,
           p.patient_id,
           p.patient_code,
           p.full_name        AS patient_name,
           p.address          AS patient_address,
           p.contact_number   AS patient_contact,
           p.email            AS patient_email,
           d.dentist_id,
           d.full_name        AS dentist_name,
           d.specialisation   AS dentist_specialisation,
           t.treatment_id,
           t.name             AS treatment_name,
           t.cost             AS treatment_cost,
           fn_consultation_fee() AS consultation_fee,
           b.bill_no,
           b.total_amount,
           b.payment_status
      FROM appointments a
      JOIN patients   p ON p.patient_id   = a.patient_id
      JOIN dentists   d ON d.dentist_id   = a.dentist_id
      JOIN treatments t ON t.treatment_id = a.treatment_id
      LEFT JOIN bills b ON b.appointment_id = a.appointment_id
     WHERE a.appointment_no = p_appointment_no;
END;;


-- ---------------------------------------------------------------------------
--  sp_generate_bill  (FR4 - calculate and print the bill)
--
--  Copies today's prices onto the bill so an old receipt still shows what the
--  patient actually paid, even after the price list changes. Marks the visit
--  as COMPLETED at the same time, because billing is the end of the visit.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_generate_bill;;

CREATE PROCEDURE sp_generate_bill(
    IN  p_appointment_no VARCHAR(20),
    IN  p_discount       DECIMAL(10,2),
    IN  p_issued_by      BIGINT,
    OUT p_bill_no        VARCHAR(20)
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_appointment_id BIGINT DEFAULT NULL;
    DECLARE v_status         VARCHAR(20);
    DECLARE v_treatment_cost DECIMAL(10,2);
    DECLARE v_fee            DECIMAL(10,2);
    DECLARE v_existing       VARCHAR(20) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT a.appointment_id, a.status, t.cost
      INTO v_appointment_id, v_status, v_treatment_cost
      FROM appointments a
      JOIN treatments  t ON t.treatment_id = a.treatment_id
     WHERE a.appointment_no = p_appointment_no;

    IF v_appointment_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No appointment was found with that number.';
    END IF;

    IF v_status = 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'This appointment was cancelled, so it cannot be billed.';
    END IF;

    -- A bill already exists: return it rather than charging the patient twice
    SELECT bill_no INTO v_existing
      FROM bills WHERE appointment_id = v_appointment_id;

    IF v_existing IS NOT NULL THEN
        SET p_bill_no = v_existing;
        COMMIT;
    ELSE
        SET v_fee = fn_consultation_fee();
        SET p_discount = COALESCE(p_discount, 0.00);

        IF p_discount > (v_treatment_cost + v_fee) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'The discount cannot be larger than the bill itself.';
        END IF;

        CALL sp_next_bill_no(p_bill_no);

        INSERT INTO bills (bill_no, appointment_id, treatment_cost,
                           consultation_fee, discount, issued_by)
        VALUES (p_bill_no, v_appointment_id, v_treatment_cost,
                v_fee, p_discount, p_issued_by);

        UPDATE appointments
           SET status = 'COMPLETED'
         WHERE appointment_id = v_appointment_id
           AND status = 'BOOKED';

        COMMIT;
    END IF;
END;;


-- ---------------------------------------------------------------------------
--  sp_cancel_appointment  (FR7)
--  Cancelling sets the status rather than deleting the row, so the clinic
--  keeps its history. It also frees the slot, because slot_key becomes NULL.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_cancel_appointment;;

CREATE PROCEDURE sp_cancel_appointment(
    IN p_appointment_no VARCHAR(20),
    IN p_reason         VARCHAR(200)
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_status VARCHAR(20) DEFAULT NULL;

    SELECT status INTO v_status
      FROM appointments WHERE appointment_no = p_appointment_no;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No appointment was found with that number.';
    END IF;

    IF v_status = 'COMPLETED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'This visit has already been completed and billed, so it cannot be cancelled.';
    END IF;

    UPDATE appointments
       SET status = 'CANCELLED',
           notes  = CONCAT(COALESCE(notes, ''), ' [Cancelled: ', COALESCE(p_reason, 'no reason given'), ']')
     WHERE appointment_no = p_appointment_no;
END;;


-- ---------------------------------------------------------------------------
--  sp_reschedule_appointment  (FR7)
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_reschedule_appointment;;

CREATE PROCEDURE sp_reschedule_appointment(
    IN p_appointment_no VARCHAR(20),
    IN p_new_date       DATE,
    IN p_new_time       TIME
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_dentist_id BIGINT DEFAULT NULL;
    DECLARE v_status     VARCHAR(20);

    DECLARE EXIT HANDLER FOR SQLSTATE '23000'
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'That dentist is already booked for the new date and time.';

    SELECT dentist_id, status INTO v_dentist_id, v_status
      FROM appointments WHERE appointment_no = p_appointment_no;

    IF v_dentist_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No appointment was found with that number.';
    END IF;

    IF v_status <> 'BOOKED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Only an appointment that is still booked can be rescheduled.';
    END IF;

    IF NOT fn_is_dentist_available(v_dentist_id, p_new_date, p_new_time) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'That dentist is already booked for the new date and time.';
    END IF;

    UPDATE appointments
       SET appointment_date = p_new_date,
           appointment_time = p_new_time
     WHERE appointment_no = p_appointment_no;
END;;


-- ===========================================================================
--  REPORTS  (section 5 of the brief - "reports which add more value")
-- ===========================================================================

-- ---------------------------------------------------------------------------
--  Report 1: the daily appointment schedule.
--  Pass NULL for p_dentist_id to see every dentist.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_daily_schedule;;

CREATE PROCEDURE sp_report_daily_schedule(
    IN p_date       DATE,
    IN p_dentist_id BIGINT
)
READS SQL DATA
BEGIN
    SELECT a.appointment_time,
           a.appointment_no,
           d.full_name      AS dentist_name,
           p.full_name      AS patient_name,
           p.contact_number AS patient_contact,
           t.name           AS treatment_name,
           t.duration_minutes,
           a.status
      FROM appointments a
      JOIN patients   p ON p.patient_id   = a.patient_id
      JOIN dentists   d ON d.dentist_id   = a.dentist_id
      JOIN treatments t ON t.treatment_id = a.treatment_id
     WHERE a.appointment_date = p_date
       AND (p_dentist_id IS NULL OR a.dentist_id = p_dentist_id)
     ORDER BY d.full_name, a.appointment_time;
END;;


-- ---------------------------------------------------------------------------
--  Report 2: revenue by treatment type over a date range.
--  Only bills that have actually been paid count as revenue.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_revenue_by_treatment;;

CREATE PROCEDURE sp_report_revenue_by_treatment(
    IN p_from DATE,
    IN p_to   DATE
)
READS SQL DATA
BEGIN
    SELECT t.treatment_code,
           t.name                       AS treatment_name,
           COUNT(b.bill_id)             AS times_billed,
           SUM(b.treatment_cost)        AS treatment_revenue,
           SUM(b.consultation_fee)      AS consultation_revenue,
           SUM(b.discount)              AS total_discount,
           SUM(b.total_amount)          AS total_revenue,
           ROUND(AVG(b.total_amount), 2) AS average_bill
      FROM bills b
      JOIN appointments a ON a.appointment_id = b.appointment_id
      JOIN treatments   t ON t.treatment_id   = a.treatment_id
     WHERE b.payment_status = 'PAID'
       AND DATE(b.issued_at) BETWEEN p_from AND p_to
     GROUP BY t.treatment_id, t.treatment_code, t.name
     ORDER BY total_revenue DESC;
END;;


-- ---------------------------------------------------------------------------
--  Report 3: a patient's full treatment history.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_patient_history;;

CREATE PROCEDURE sp_report_patient_history(
    IN p_patient_code VARCHAR(20)
)
READS SQL DATA
BEGIN
    SELECT a.appointment_no,
           a.appointment_date,
           a.appointment_time,
           d.full_name  AS dentist_name,
           t.name       AS treatment_name,
           a.status,
           a.notes,
           b.bill_no,
           b.total_amount,
           b.payment_status
      FROM appointments a
      JOIN patients   p ON p.patient_id   = a.patient_id
      JOIN dentists   d ON d.dentist_id   = a.dentist_id
      JOIN treatments t ON t.treatment_id = a.treatment_id
      LEFT JOIN bills b ON b.appointment_id = a.appointment_id
     WHERE p.patient_code = p_patient_code
     ORDER BY a.appointment_date DESC, a.appointment_time DESC;
END;;


-- ---------------------------------------------------------------------------
--  Report 4: dentist workload summary, for staffing decisions.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_dentist_workload;;

CREATE PROCEDURE sp_report_dentist_workload(
    IN p_from DATE,
    IN p_to   DATE
)
READS SQL DATA
BEGIN
    SELECT d.dentist_code,
           d.full_name AS dentist_name,
           d.specialisation,
           COUNT(a.appointment_id) AS total_appointments,
           SUM(a.status = 'COMPLETED') AS completed,
           SUM(a.status = 'CANCELLED') AS cancelled,
           SUM(a.status = 'NO_SHOW')   AS no_shows,
           COALESCE(SUM(t.duration_minutes), 0) AS total_minutes_booked,
           COALESCE(SUM(b.total_amount), 0)     AS revenue_generated
      FROM dentists d
      LEFT JOIN appointments a
             ON a.dentist_id = d.dentist_id
            AND a.appointment_date BETWEEN p_from AND p_to
      LEFT JOIN treatments t ON t.treatment_id   = a.treatment_id
      LEFT JOIN bills      b ON b.appointment_id = a.appointment_id
                            AND b.payment_status = 'PAID'
     WHERE d.active = TRUE
     GROUP BY d.dentist_id, d.dentist_code, d.full_name, d.specialisation
     ORDER BY total_appointments DESC;
END;;


-- ---------------------------------------------------------------------------
--  Report 5: tomorrow's appointments, for the reminder feature (FR7).
--  The Observer notification service calls this once a day.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_upcoming_reminders;;

CREATE PROCEDURE sp_report_upcoming_reminders(
    IN p_days_ahead INT
)
READS SQL DATA
BEGIN
    SELECT a.appointment_no,
           a.appointment_date,
           a.appointment_time,
           p.full_name      AS patient_name,
           p.contact_number AS patient_contact,
           p.email          AS patient_email,
           d.full_name      AS dentist_name,
           t.name           AS treatment_name
      FROM appointments a
      JOIN patients   p ON p.patient_id   = a.patient_id
      JOIN dentists   d ON d.dentist_id   = a.dentist_id
      JOIN treatments t ON t.treatment_id = a.treatment_id
     WHERE a.status = 'BOOKED'
       AND a.appointment_date = DATE_ADD(CURDATE(), INTERVAL COALESCE(p_days_ahead, 1) DAY)
     ORDER BY a.appointment_time;
END;;
