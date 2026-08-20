-- ===========================================================================
--  Sunrise Dental Clinic - TRIGGERS
--  CIS6003 Advanced Programming (WRIT1)
--
--  A trigger is a piece of code the database runs by itself, every time a row
--  is inserted, updated or deleted. The brief asks for triggers that enforce
--  business rules, so these do two jobs:
--
--    1. VALIDATION - reject data that breaks a clinic rule, using
--       SIGNAL SQLSTATE '45000', which raises an error with our own message.
--       The Java layer catches it and shows the message on screen.
--
--    2. AUDITING - record every change to an appointment automatically, so
--       there is a history no member of staff can quietly edit.
--
--  WHY VALIDATE HERE AS WELL AS IN JAVA
--  The Java validation gives the receptionist an instant, friendly message.
--  These triggers are the last line of defence: they still apply if someone
--  reaches the database through the REST API, a script, or MySQL Workbench.
--  The report discusses this trade-off (duplicated rules vs. guaranteed rules).
--
--  Statements are separated by ";;" - see the note at the top of schema.sql.
--
--  Note on "changed_by": the Java code runs  SET @app_user = 'username'  at the
--  start of each transaction. If that has not happened, the audit falls back to
--  the MySQL login name, so the column is never empty.
-- ===========================================================================


-- ===========================================================================
--  PATIENTS
-- ===========================================================================

-- ---------------------------------------------------------------------------
--  Contact numbers must be usable, because the clinic phones patients to
--  confirm appointments. Accepted formats are a local number (0771234567) or
--  an international one (+94771234567). Spaces and dashes are stripped first,
--  so staff can type the number however they like.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_patients_before_insert;;

CREATE TRIGGER trg_patients_before_insert
BEFORE INSERT ON patients
FOR EACH ROW
BEGIN
    -- Tidy the data before it is stored
    SET NEW.full_name      = TRIM(NEW.full_name);
    SET NEW.address        = TRIM(NEW.address);
    SET NEW.contact_number = REPLACE(REPLACE(REPLACE(TRIM(NEW.contact_number), ' ', ''), '-', ''), '(', '');
    SET NEW.email          = NULLIF(TRIM(LOWER(COALESCE(NEW.email, ''))), '');

    IF CHAR_LENGTH(NEW.full_name) < 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Patient name must be at least 3 characters long.';
    END IF;

    IF NOT (NEW.contact_number REGEXP '^(0[0-9]{9}|\\+94[0-9]{9})$') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Contact number must be 10 digits starting with 0, or +94 followed by 9 digits.';
    END IF;

    IF NEW.email IS NOT NULL AND NOT (NEW.email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'That email address does not look valid.';
    END IF;

    -- A date of birth in the future, or an impossible age, is a typing mistake
    IF NEW.date_of_birth IS NOT NULL THEN
        IF NEW.date_of_birth > CURDATE() THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Date of birth cannot be in the future.';
        END IF;
        IF TIMESTAMPDIFF(YEAR, NEW.date_of_birth, CURDATE()) > 120 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Please check the date of birth - the age is over 120 years.';
        END IF;
    END IF;
END;;


DROP TRIGGER IF EXISTS trg_patients_before_update;;

CREATE TRIGGER trg_patients_before_update
BEFORE UPDATE ON patients
FOR EACH ROW
BEGIN
    SET NEW.full_name      = TRIM(NEW.full_name);
    SET NEW.address        = TRIM(NEW.address);
    SET NEW.contact_number = REPLACE(REPLACE(REPLACE(TRIM(NEW.contact_number), ' ', ''), '-', ''), '(', '');
    SET NEW.email          = NULLIF(TRIM(LOWER(COALESCE(NEW.email, ''))), '');

    IF NOT (NEW.contact_number REGEXP '^(0[0-9]{9}|\\+94[0-9]{9})$') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Contact number must be 10 digits starting with 0, or +94 followed by 9 digits.';
    END IF;

    -- The patient code identifies the person for life and must never change
    IF NEW.patient_code <> OLD.patient_code THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A patient code cannot be changed once it has been issued.';
    END IF;
END;;


-- ===========================================================================
--  APPOINTMENTS - validation
-- ===========================================================================

-- ---------------------------------------------------------------------------
--  Rules enforced when an appointment is booked:
--    * the time must be inside the clinic's opening hours
--    * it must sit on a proper slot boundary (08:00, 08:30, 09:00 ...)
--    * it cannot be booked far in the past, although recording a walk-in seen
--      in the last 30 days is allowed
--    * it cannot be booked more than a year ahead
--
--  Double booking is not checked here. It is stopped by the UNIQUE index on
--  appointments.slot_key, which is safer, because an index cannot be beaten by
--  two users saving at exactly the same moment. See schema.sql.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_appointments_before_insert;;

CREATE TRIGGER trg_appointments_before_insert
BEFORE INSERT ON appointments
FOR EACH ROW
BEGIN
    DECLARE v_slot INT;

    SET NEW.notes = NULLIF(TRIM(COALESCE(NEW.notes, '')), '');

    IF NOT fn_is_within_opening_hours(NEW.appointment_time) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'The clinic is closed at that time. Please choose a time within opening hours.';
    END IF;

    SET v_slot = CAST(COALESCE(fn_get_setting('appointment_slot_minutes'), '30') AS UNSIGNED);

    IF SECOND(NEW.appointment_time) <> 0
       OR (MINUTE(NEW.appointment_time) MOD v_slot) <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Appointments must start on a slot boundary, for example 09:00 or 09:30.';
    END IF;

    IF NEW.appointment_date < DATE_SUB(CURDATE(), INTERVAL 30 DAY) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'An appointment cannot be recorded more than 30 days in the past.';
    END IF;

    IF NEW.appointment_date > DATE_ADD(CURDATE(), INTERVAL 1 YEAR) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'An appointment cannot be booked more than one year ahead.';
    END IF;
END;;


-- ---------------------------------------------------------------------------
--  Rules enforced when an appointment is changed. The main one protects the
--  accounts: once a visit has been billed, its record is frozen.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_appointments_before_update;;

CREATE TRIGGER trg_appointments_before_update
BEFORE UPDATE ON appointments
FOR EACH ROW
BEGIN
    DECLARE v_slot     INT;
    DECLARE v_billed   INT;

    SELECT COUNT(*) INTO v_billed
      FROM bills WHERE appointment_id = OLD.appointment_id;

    -- A billed visit may still be marked paid, but its details are fixed
    IF v_billed > 0 AND (NEW.appointment_date <> OLD.appointment_date
                      OR NEW.appointment_time <> OLD.appointment_time
                      OR NEW.treatment_id     <> OLD.treatment_id
                      OR NEW.dentist_id       <> OLD.dentist_id) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'This visit has already been billed, so its details can no longer be changed.';
    END IF;

    -- A cancelled appointment is final: book a new one instead of reviving it
    IF OLD.status = 'CANCELLED' AND NEW.status <> 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A cancelled appointment cannot be reopened. Please create a new appointment.';
    END IF;

    -- The appointment number is the patient's reference and must not change
    IF NEW.appointment_no <> OLD.appointment_no THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'An appointment number cannot be changed once it has been issued.';
    END IF;

    -- If the visit is being moved, the new time must obey the same rules
    IF NEW.appointment_time <> OLD.appointment_time OR NEW.appointment_date <> OLD.appointment_date THEN
        IF NOT fn_is_within_opening_hours(NEW.appointment_time) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'The clinic is closed at that time. Please choose a time within opening hours.';
        END IF;

        SET v_slot = CAST(COALESCE(fn_get_setting('appointment_slot_minutes'), '30') AS UNSIGNED);

        IF SECOND(NEW.appointment_time) <> 0
           OR (MINUTE(NEW.appointment_time) MOD v_slot) <> 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Appointments must start on a slot boundary, for example 09:00 or 09:30.';
        END IF;

        IF NEW.appointment_date < CURDATE() THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'An appointment cannot be moved to a date in the past.';
        END IF;
    END IF;
END;;


-- ===========================================================================
--  APPOINTMENTS - auditing
-- ===========================================================================

DROP TRIGGER IF EXISTS trg_appointments_after_insert;;

CREATE TRIGGER trg_appointments_after_insert
AFTER INSERT ON appointments
FOR EACH ROW
BEGIN
    INSERT INTO appointment_audit (appointment_id, appointment_no, action,
                                   old_status, new_status, old_slot, new_slot, changed_by)
    VALUES (NEW.appointment_id, NEW.appointment_no, 'CREATED',
            NULL, NEW.status,
            NULL, CONCAT(NEW.dentist_id, '|', NEW.appointment_date, '|', NEW.appointment_time),
            COALESCE(@app_user, CURRENT_USER()));
END;;


-- ---------------------------------------------------------------------------
--  Records what actually changed, so the audit trail reads as plain English:
--  CANCELLED, COMPLETED, RESCHEDULED or UPDATED.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_appointments_after_update;;

CREATE TRIGGER trg_appointments_after_update
AFTER UPDATE ON appointments
FOR EACH ROW
BEGIN
    DECLARE v_action VARCHAR(30);

    IF NEW.status <> OLD.status THEN
        SET v_action = CASE NEW.status
                           WHEN 'CANCELLED' THEN 'CANCELLED'
                           WHEN 'COMPLETED' THEN 'COMPLETED'
                           WHEN 'NO_SHOW'   THEN 'MARKED_NO_SHOW'
                           ELSE 'STATUS_CHANGED'
                       END;
    ELSEIF NEW.appointment_date <> OLD.appointment_date
        OR NEW.appointment_time <> OLD.appointment_time THEN
        SET v_action = 'RESCHEDULED';
    ELSE
        SET v_action = 'UPDATED';
    END IF;

    INSERT INTO appointment_audit (appointment_id, appointment_no, action,
                                   old_status, new_status, old_slot, new_slot, changed_by)
    VALUES (NEW.appointment_id, NEW.appointment_no, v_action,
            OLD.status, NEW.status,
            CONCAT(OLD.dentist_id, '|', OLD.appointment_date, '|', OLD.appointment_time),
            CONCAT(NEW.dentist_id, '|', NEW.appointment_date, '|', NEW.appointment_time),
            COALESCE(@app_user, CURRENT_USER()));
END;;


-- ===========================================================================
--  BILLS
-- ===========================================================================

-- ---------------------------------------------------------------------------
--  Protects the money side of the system. total_amount itself is calculated by
--  the database (a generated column in schema.sql), so it can never disagree
--  with the parts it is made of.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_bills_before_insert;;

CREATE TRIGGER trg_bills_before_insert
BEFORE INSERT ON bills
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(20);

    SELECT status INTO v_status
      FROM appointments WHERE appointment_id = NEW.appointment_id;

    IF v_status = 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A cancelled appointment cannot be billed.';
    END IF;

    IF NEW.discount > (NEW.treatment_cost + NEW.consultation_fee) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'The discount cannot be larger than the bill itself.';
    END IF;

    -- Keep the two payment columns telling the same story
    IF NEW.payment_status = 'PAID' AND NEW.paid_at IS NULL THEN
        SET NEW.paid_at = NOW();
    END IF;
END;;


-- ---------------------------------------------------------------------------
--  A receipt already handed to a patient must not be quietly rewritten.
--  The only change allowed is recording the payment.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_bills_before_update;;

CREATE TRIGGER trg_bills_before_update
BEFORE UPDATE ON bills
FOR EACH ROW
BEGIN
    IF NEW.treatment_cost   <> OLD.treatment_cost
    OR NEW.consultation_fee <> OLD.consultation_fee
    OR NEW.discount         <> OLD.discount
    OR NEW.appointment_id   <> OLD.appointment_id
    OR NEW.bill_no          <> OLD.bill_no THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A bill cannot be edited once issued. Cancel it and issue a new one instead.';
    END IF;

    IF OLD.payment_status = 'PAID' AND NEW.payment_status = 'UNPAID' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A bill that has been paid cannot be marked unpaid again.';
    END IF;

    IF NEW.payment_status = 'PAID' AND OLD.payment_status = 'UNPAID' THEN
        SET NEW.paid_at = NOW();

        IF NEW.payment_method IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Please record how the patient paid: cash, card or insurance.';
        END IF;
    END IF;
END;;


-- ===========================================================================
--  USERS
-- ===========================================================================

-- ---------------------------------------------------------------------------
--  Guarantees that no plain-text password can ever reach the database. A
--  BCrypt hash is always exactly 60 characters and begins with $2a$, $2b$ or
--  $2y$, so anything else means the hashing step was skipped by mistake.
-- ---------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_users_before_insert;;

CREATE TRIGGER trg_users_before_insert
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    SET NEW.username = LOWER(TRIM(NEW.username));

    IF CHAR_LENGTH(NEW.username) < 4 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Username must be at least 4 characters long.';
    END IF;

    IF NOT (NEW.password_hash REGEXP '^\\$2[aby]\\$[0-9]{2}\\$.{53}$') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Passwords must be stored as a BCrypt hash, never as plain text.';
    END IF;
END;;


DROP TRIGGER IF EXISTS trg_users_before_update;;

CREATE TRIGGER trg_users_before_update
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    IF NEW.password_hash <> OLD.password_hash
       AND NOT (NEW.password_hash REGEXP '^\\$2[aby]\\$[0-9]{2}\\$.{53}$') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Passwords must be stored as a BCrypt hash, never as plain text.';
    END IF;
END;;
