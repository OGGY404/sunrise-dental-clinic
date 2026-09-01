-- ===========================================================================
--  Sunrise Dental Clinic - DATABASE SCHEMA
--  CIS6003 Advanced Programming (WRIT1)
--
--  This file creates the tables. Stored procedures are in procedures.sql and
--  triggers are in triggers.sql. All three run automatically when the
--  application starts (see spring.sql.init.* in application.properties).
--
--  IMPORTANT - why every statement ends with a double semicolon (;;)
--  A stored procedure body contains single semicolons inside it. If we used a
--  single semicolon as the statement separator, Spring would cut the procedure
--  in half. So the whole project uses ";;" as the separator instead, set by
--  spring.sql.init.separator=;; in application.properties.
--
--  Every statement is written so it can run more than once without failing,
--  because the scripts execute on every application start.
-- ===========================================================================


-- ---------------------------------------------------------------------------
--  DATABASE COLLATION - this must come first
--
--  Every table below is created as utf8mb4_unicode_ci. The database itself,
--  however, is created by MySQL with its own default, which on MySQL 8 is
--  utf8mb4_0900_ai_ci. Those two are not the same.
--
--  That difference breaks the stored routines. A parameter such as
--  fn_get_setting(p_key VARCHAR(60)) takes its collation from the database
--  default, so comparing it with clinic_settings.setting_key, which is
--  utf8mb4_unicode_ci, fails with:
--
--      Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT)
--      and (utf8mb4_0900_ai_ci,IMPLICIT) for operation '='
--
--  Registering an appointment fires a trigger that calls that function, so on
--  a normal MySQL 8 installation every booking failed. It was found the first
--  time a booking was made against the real database.
--
--  Setting the database collation to match the tables fixes it for every
--  routine at once. The database name is deliberately left out, so this line
--  applies to whichever database the application is configured to use.
--  procedures.sql and triggers.sql drop and recreate everything on each start,
--  so they pick this up straight away.
-- ---------------------------------------------------------------------------
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  clinic_settings
--  Clinic-wide values that staff may need to change without a code rebuild,
--  for example the consultation fee. Read by the Singleton configuration
--  holder in the Java code and by the fn_get_setting database function.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clinic_settings (
    setting_key     VARCHAR(60)   NOT NULL,
    setting_value   VARCHAR(255)  NOT NULL,
    description     VARCHAR(255)  NULL,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  users  (FR1 - authentication, and FR7 - role based access)
--  Staff accounts. The password column stores a BCrypt hash, never the real
--  password. A BCrypt hash is always 60 characters long.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id        BIGINT        NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)   NOT NULL,
    password_hash  CHAR(60)      NOT NULL,
    full_name      VARCHAR(100)  NOT NULL,
    email          VARCHAR(120)  NULL,
    role           ENUM('ADMIN','RECEPTIONIST') NOT NULL DEFAULT 'RECEPTIONIST',
    enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    last_login_at  DATETIME      NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  dentists
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dentists (
    dentist_id      BIGINT        NOT NULL AUTO_INCREMENT,
    dentist_code    VARCHAR(20)   NOT NULL,
    full_name       VARCHAR(100)  NOT NULL,
    specialisation  VARCHAR(80)   NULL,
    contact_number  VARCHAR(15)   NULL,
    email           VARCHAR(120)  NULL,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dentist_id),
    UNIQUE KEY uq_dentists_code (dentist_code),
    KEY ix_dentists_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  treatments  (FR4 - the treatment cost half of the bill)
--  duration_minutes lets the system block the right number of slots later.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS treatments (
    treatment_id     BIGINT         NOT NULL AUTO_INCREMENT,
    treatment_code   VARCHAR(20)    NOT NULL,
    name             VARCHAR(100)   NOT NULL,
    description      VARCHAR(255)   NULL,
    cost             DECIMAL(10,2)  NOT NULL,
    duration_minutes INT            NOT NULL DEFAULT 30,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (treatment_id),
    UNIQUE KEY uq_treatments_code (treatment_code),
    CONSTRAINT ck_treatments_cost CHECK (cost >= 0),
    CONSTRAINT ck_treatments_duration CHECK (duration_minutes BETWEEN 5 AND 480)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  patients  (FR2 - patient details, and FR7 - treatment history)
--  Patients are stored in their own table rather than repeated on every
--  appointment row. That is what lets one patient have many appointments,
--  which is the treatment-history feature.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patients (
    patient_id      BIGINT        NOT NULL AUTO_INCREMENT,
    patient_code    VARCHAR(20)   NOT NULL,
    full_name       VARCHAR(100)  NOT NULL,
    address         VARCHAR(255)  NOT NULL,
    contact_number  VARCHAR(15)   NOT NULL,
    email           VARCHAR(120)  NULL,
    date_of_birth   DATE          NULL,
    gender          ENUM('MALE','FEMALE','OTHER') NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (patient_id),
    UNIQUE KEY uq_patients_code (patient_code),
    KEY ix_patients_contact (contact_number),
    KEY ix_patients_name (full_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  appointments  (FR2, FR3, and FR7 - cancel / reschedule)
--
--  HOW DOUBLE BOOKING IS PREVENTED
--  slot_key is a generated column. For a live appointment it holds
--  "dentistId|date|time"; for a cancelled one it is NULL. A UNIQUE index on it
--  therefore blocks two live appointments in the same chair at the same time,
--  while still allowing a cancelled slot to be rebooked - because SQL lets a
--  UNIQUE index hold many NULLs.
--
--  This is enforced by the database itself, so the rule holds even if two
--  receptionists click Save at the same instant. The Java service layer checks
--  first and shows a friendly message; this index is the final safety net.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id    BIGINT        NOT NULL AUTO_INCREMENT,
    appointment_no    VARCHAR(20)   NOT NULL,
    patient_id        BIGINT        NOT NULL,
    dentist_id        BIGINT        NOT NULL,
    treatment_id      BIGINT        NOT NULL,
    appointment_date  DATE          NOT NULL,
    appointment_time  TIME          NOT NULL,
    status            ENUM('BOOKED','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'BOOKED',
    notes             VARCHAR(500)  NULL,
    created_by        BIGINT        NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    slot_key VARCHAR(64) GENERATED ALWAYS AS (
        CASE WHEN status = 'CANCELLED' THEN NULL
             ELSE CONCAT(dentist_id, '|', appointment_date, '|', appointment_time)
        END
    ) STORED,

    PRIMARY KEY (appointment_id),
    UNIQUE KEY uq_appointments_no (appointment_no),
    UNIQUE KEY uq_appointments_slot (slot_key),
    KEY ix_appointments_date (appointment_date),
    KEY ix_appointments_dentist_date (dentist_id, appointment_date),
    KEY ix_appointments_patient (patient_id),
    KEY ix_appointments_status (status),

    CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_id)   REFERENCES patients (patient_id),
    CONSTRAINT fk_appointments_dentist
        FOREIGN KEY (dentist_id)   REFERENCES dentists (dentist_id),
    CONSTRAINT fk_appointments_treatment
        FOREIGN KEY (treatment_id) REFERENCES treatments (treatment_id),
    CONSTRAINT fk_appointments_user
        FOREIGN KEY (created_by)   REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  bills  (FR4 - calculate and print the bill)
--
--  The amounts are copied onto the bill rather than looked up live. If the
--  clinic raises the price of a filling next month, an old receipt must still
--  show what the patient actually paid. This is a normal accounting rule.
--
--  total_amount is a generated column, so the arithmetic cannot drift out of
--  step with its parts.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bills (
    bill_id          BIGINT         NOT NULL AUTO_INCREMENT,
    bill_no          VARCHAR(20)    NOT NULL,
    appointment_id   BIGINT         NOT NULL,
    treatment_cost   DECIMAL(10,2)  NOT NULL,
    consultation_fee DECIMAL(10,2)  NOT NULL,
    discount         DECIMAL(10,2)  NOT NULL DEFAULT 0.00,

    total_amount DECIMAL(10,2) GENERATED ALWAYS AS
        (treatment_cost + consultation_fee - discount) STORED,

    payment_status   ENUM('UNPAID','PAID') NOT NULL DEFAULT 'UNPAID',
    payment_method   ENUM('CASH','CARD','INSURANCE') NULL,
    issued_by        BIGINT         NULL,
    issued_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at          DATETIME       NULL,

    PRIMARY KEY (bill_id),
    UNIQUE KEY uq_bills_no (bill_no),
    -- One appointment can only ever be billed once
    UNIQUE KEY uq_bills_appointment (appointment_id),
    KEY ix_bills_issued_at (issued_at),
    KEY ix_bills_status (payment_status),

    CONSTRAINT fk_bills_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (appointment_id),
    CONSTRAINT fk_bills_user
        FOREIGN KEY (issued_by)      REFERENCES users (user_id),

    CONSTRAINT ck_bills_treatment_cost   CHECK (treatment_cost >= 0),
    CONSTRAINT ck_bills_consultation_fee CHECK (consultation_fee >= 0),
    CONSTRAINT ck_bills_discount         CHECK (discount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  appointment_audit
--  Written automatically by triggers, never by the Java code. It gives the
--  clinic a tamper-evident record of who changed an appointment and when,
--  which is what the paper system could not do.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointment_audit (
    audit_id       BIGINT       NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT       NOT NULL,
    appointment_no VARCHAR(20)  NOT NULL,
    action         VARCHAR(30)  NOT NULL,
    old_status     VARCHAR(20)  NULL,
    new_status     VARCHAR(20)  NULL,
    old_slot       VARCHAR(64)  NULL,
    new_slot       VARCHAR(64)  NULL,
    changed_by     VARCHAR(80)  NOT NULL,
    changed_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_id),
    KEY ix_audit_appointment (appointment_id),
    KEY ix_audit_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;


-- ---------------------------------------------------------------------------
--  COUNTER TABLES - how unique reference numbers are produced
--
--  Appointment numbers look like APT-20260820-0007 and bill numbers like
--  BIL-20260820-0003, so their sequence restarts each day. Patient codes look
--  like PAT-000042 and never restart.
--
--  A counter table is used rather than counting the rows already in the table.
--  Counting would hand the same number to two receptionists who click Save at
--  the same moment; a counter row can be locked by the database, so it cannot.
-- ---------------------------------------------------------------------------

-- Counters that restart every day: 'APPOINTMENT', 'BILL'
CREATE TABLE IF NOT EXISTS daily_counter (
    counter_name VARCHAR(30) NOT NULL,
    counter_date DATE        NOT NULL,
    last_number  INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (counter_name, counter_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;

-- Counters that run continuously: 'PATIENT'
CREATE TABLE IF NOT EXISTS code_counter (
    counter_name VARCHAR(30) NOT NULL,
    last_number  INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (counter_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;
