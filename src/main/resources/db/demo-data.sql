-- ===========================================================================
--  Sunrise Dental Clinic - DEMONSTRATION DATA
--  CIS6003 Advanced Programming (WRIT1)
--
--  Sample patients, appointments and bills, used for the screenshots in the
--  report and for trying the reports out. It gives the system a believable
--  history instead of empty tables.
--
--  ---------------------------------------------------------------------
--  WARNING: THIS SCRIPT DELETES ALL PATIENT, APPOINTMENT AND BILL DATA
--  before it inserts the samples, so it can be run again and again while
--  preparing the report. Never run it against real clinic data.
--  ---------------------------------------------------------------------
--
--  HOW TO RUN IT
--  This file is NOT run automatically at start-up. Run it yourself, either by
--  opening it in MySQL Workbench and pressing the lightning-bolt button, or in
--  a terminal:
--
--      mysql -u root -p sunrise_dental < src/main/resources/db/demo-data.sql
--
--  Unlike the other scripts, statements here end with a single semicolon,
--  because this file is run by the normal MySQL client rather than by Spring.
--
--  NOTE: every appointment is booked through CALL sp_register_appointment,
--  not by inserting rows directly. That means the demonstration data is
--  created by the very stored procedures and triggers being assessed - if any
--  of them is broken, this script fails, which makes it a useful check.
-- ===========================================================================


-- ---------------------------------------------------------------------------
--  1. Clear any previous demonstration data
--     Deleted child-first, because foreign keys forbid removing a row that
--     something else still points at.
-- ---------------------------------------------------------------------------
DELETE FROM bills;
DELETE FROM appointment_audit;
DELETE FROM appointments;
DELETE FROM patients;
DELETE FROM daily_counter;
DELETE FROM code_counter;


-- ---------------------------------------------------------------------------
--  2. Look up the ids we need, so this script does not depend on the exact
--     auto-increment numbers in your copy of the database.
-- ---------------------------------------------------------------------------
SET @admin      = (SELECT user_id FROM users WHERE username = 'admin');
SET @reception  = (SELECT user_id FROM users WHERE username = 'reception');

SET @den_perera    = (SELECT dentist_id FROM dentists WHERE dentist_code = 'DEN-001');
SET @den_fernando  = (SELECT dentist_id FROM dentists WHERE dentist_code = 'DEN-002');
SET @den_jaya      = (SELECT dentist_id FROM dentists WHERE dentist_code = 'DEN-003');
SET @den_wickrama  = (SELECT dentist_id FROM dentists WHERE dentist_code = 'DEN-004');

SET @trt_checkup   = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-001');
SET @trt_scaling   = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-002');
SET @trt_filling   = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-003');
SET @trt_extract   = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-004');
SET @trt_rootcanal = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-005');
SET @trt_crown     = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-006');
SET @trt_whitening = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-007');
SET @trt_braces_c  = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-008');
SET @trt_child     = (SELECT treatment_id FROM treatments WHERE treatment_code = 'TRT-012');

-- Named in the audit trail as the person who made these changes
SET @app_user = 'demo-data-script';


-- ===========================================================================
--  3. PAST VISITS - completed and paid, so the revenue reports have figures
--
--     All dates are counted back from today, and stay inside the 30-day limit
--     that trg_appointments_before_insert allows for recording a past visit.
-- ===========================================================================

-- --- Visit 1: Kamal Silva, scaling, 21 days ago ---------------------------
CALL sp_register_appointment(
    'Kamal Silva', 'No. 15, Temple Road, Nugegoda', '0771112233', 'kamal.silva@gmail.com',
    '1988-04-12', 'MALE',
    @den_perera, @trt_scaling,
    DATE_SUB(CURDATE(), INTERVAL 21 DAY), '09:00:00',
    'Complained of sensitivity on the lower left side.',
    @reception, @apt, @pat);
CALL sp_generate_bill(@apt, 0.00, @reception, @bill);
UPDATE bills SET payment_status = 'PAID', payment_method = 'CASH' WHERE bill_no = @bill;

-- --- Visit 2: Kamal Silva again, filling, 7 days ago ----------------------
--     Same patient, so this proves the treatment-history feature works.
CALL sp_register_appointment(
    'Kamal Silva', 'No. 15, Temple Road, Nugegoda', '0771112233', 'kamal.silva@gmail.com',
    '1988-04-12', 'MALE',
    @den_perera, @trt_filling,
    DATE_SUB(CURDATE(), INTERVAL 7 DAY), '10:30:00',
    'Filling for the cavity found at the previous visit.',
    @reception, @apt, @pat);
CALL sp_generate_bill(@apt, 500.00, @reception, @bill);
UPDATE bills SET payment_status = 'PAID', payment_method = 'CARD' WHERE bill_no = @bill;

-- --- Visit 3: Nadeesha Bandara, root canal, 18 days ago -------------------
CALL sp_register_appointment(
    'Nadeesha Bandara', 'No. 210/A, Kandy Road, Kadawatha', '0762223344', 'nadeesha.b@yahoo.com',
    '1995-11-30', 'FEMALE',
    @den_jaya, @trt_rootcanal,
    DATE_SUB(CURDATE(), INTERVAL 18 DAY), '14:00:00',
    'Severe pain in the upper right molar.',
    @reception, @apt, @pat);
CALL sp_generate_bill(@apt, 2000.00, @admin, @bill);
UPDATE bills SET payment_status = 'PAID', payment_method = 'INSURANCE' WHERE bill_no = @bill;

-- --- Visit 4: Suresh Rajapaksa, extraction, 14 days ago -------------------
CALL sp_register_appointment(
    'Suresh Rajapaksa', 'No. 7, Lake Drive, Rajagiriya', '0713334455', NULL,
    '1979-02-08', 'MALE',
    @den_jaya, @trt_extract,
    DATE_SUB(CURDATE(), INTERVAL 14 DAY), '11:00:00',
    NULL,
    @reception, @apt, @pat);
CALL sp_generate_bill(@apt, 0.00, @reception, @bill);
UPDATE bills SET payment_status = 'PAID', payment_method = 'CASH' WHERE bill_no = @bill;

-- --- Visit 5: Fathima Rizwan, whitening, 10 days ago ----------------------
CALL sp_register_appointment(
    'Fathima Rizwan', 'No. 88, Marine Drive, Colombo 06', '0774445566', 'f.rizwan@outlook.com',
    '1992-07-19', 'FEMALE',
    @den_fernando, @trt_whitening,
    DATE_SUB(CURDATE(), INTERVAL 10 DAY), '15:30:00',
    'Requested whitening before a wedding.',
    @reception, @apt, @pat);
CALL sp_generate_bill(@apt, 1000.00, @reception, @bill);
UPDATE bills SET payment_status = 'PAID', payment_method = 'CARD' WHERE bill_no = @bill;

-- --- Visit 6: Tharindu Alwis, crown, 5 days ago ---------------------------
CALL sp_register_appointment(
    'Tharindu Alwis', 'No. 33, Hill Street, Dehiwala', '0705556677', 'tharindu.a@gmail.com',
    '1985-09-25', 'MALE',
    @den_perera, @trt_crown,
    DATE_SUB(CURDATE(), INTERVAL 5 DAY), '09:30:00',
    NULL,
    @admin, @apt, @pat);
CALL sp_generate_bill(@apt, 0.00, @admin, @bill);
-- Left UNPAID on purpose, so the outstanding-payments screen has something in it

-- --- Visit 7: Ishara Gunawardena, child care, 3 days ago ------------------
CALL sp_register_appointment(
    'Ishara Gunawardena', 'No. 120, Station Road, Mount Lavinia', '0766667788', 'ishara.g@gmail.com',
    '2016-03-14', 'FEMALE',
    @den_wickrama, @trt_child,
    DATE_SUB(CURDATE(), INTERVAL 3 DAY), '16:00:00',
    'First visit. Nervous - handled gently.',
    @reception, @apt, @pat);
CALL sp_generate_bill(@apt, 0.00, @reception, @bill);
UPDATE bills SET payment_status = 'PAID', payment_method = 'CASH' WHERE bill_no = @bill;


-- ===========================================================================
--  4. A CANCELLED VISIT
--     Proves cancelling keeps the record but frees the slot, because
--     slot_key becomes NULL. See the note in schema.sql.
-- ===========================================================================
CALL sp_register_appointment(
    'Priyanka Mendis', 'No. 5, Flower Road, Colombo 07', '0777778899', 'p.mendis@gmail.com',
    '1990-12-01', 'FEMALE',
    @den_fernando, @trt_braces_c,
    DATE_SUB(CURDATE(), INTERVAL 2 DAY), '13:00:00',
    NULL,
    @reception, @apt, @pat);
CALL sp_cancel_appointment(@apt, 'Patient called to say she was unwell');

-- The very same slot can now be booked again, which would have been blocked
-- if the appointment were still live.
CALL sp_register_appointment(
    'Dilan Weerasinghe', 'No. 61, Church Street, Moratuwa', '0728889900', NULL,
    '1983-06-05', 'MALE',
    @den_fernando, @trt_checkup,
    DATE_SUB(CURDATE(), INTERVAL 2 DAY), '13:00:00',
    'Took the slot freed by a cancellation.',
    @reception, @apt, @pat);
CALL sp_generate_bill(@apt, 0.00, @reception, @bill);
UPDATE bills SET payment_status = 'PAID', payment_method = 'CASH' WHERE bill_no = @bill;


-- ===========================================================================
--  5. TODAY'S SCHEDULE
--     Gives the daily schedule report something to show in the screenshots.
-- ===========================================================================
CALL sp_register_appointment(
    'Chamari Dissanayake', 'No. 19, Park Avenue, Battaramulla', '0771119988', 'chamari.d@gmail.com',
    '1998-01-22', 'FEMALE', @den_perera, @trt_checkup,
    CURDATE(), '09:00:00', NULL, @reception, @apt, @pat);

CALL sp_register_appointment(
    'Roshan Peiris', 'No. 8, Sea Street, Wattala', '0752223311', NULL,
    '1975-05-17', 'MALE', @den_perera, @trt_scaling,
    CURDATE(), '10:00:00', NULL, @reception, @apt, @pat);

CALL sp_register_appointment(
    'Sanduni Herath', 'No. 44, Lily Lane, Maharagama', '0763334422', 's.herath@gmail.com',
    '2001-08-09', 'FEMALE', @den_fernando, @trt_braces_c,
    CURDATE(), '11:00:00', 'Interested in clear aligners.', @reception, @apt, @pat);

CALL sp_register_appointment(
    'Mohamed Nazeer', 'No. 71, Main Street, Kotahena', '0774445511', NULL,
    '1968-10-03', 'MALE', @den_jaya, @trt_extract,
    CURDATE(), '14:30:00', NULL, @admin, @apt, @pat);


-- ===========================================================================
--  6. FUTURE BOOKINGS
--     Tomorrow's appointments are what the reminder feature (FR7) picks up
--     through sp_report_upcoming_reminders.
-- ===========================================================================
CALL sp_register_appointment(
    'Kamal Silva', 'No. 15, Temple Road, Nugegoda', '0771112233', 'kamal.silva@gmail.com',
    '1988-04-12', 'MALE', @den_perera, @trt_checkup,
    DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00',
    'Follow-up on the filling.', @reception, @apt, @pat);

CALL sp_register_appointment(
    'Nadeesha Bandara', 'No. 210/A, Kandy Road, Kadawatha', '0762223344', 'nadeesha.b@yahoo.com',
    '1995-11-30', 'FEMALE', @den_jaya, @trt_crown,
    DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00',
    'Crown to finish the root canal treatment.', @reception, @apt, @pat);

CALL sp_register_appointment(
    'Ishara Gunawardena', 'No. 120, Station Road, Mount Lavinia', '0766667788', 'ishara.g@gmail.com',
    '2016-03-14', 'FEMALE', @den_wickrama, @trt_child,
    DATE_ADD(CURDATE(), INTERVAL 3 DAY), '16:30:00',
    'Six-month check-up.', @reception, @apt, @pat);

CALL sp_register_appointment(
    'Fathima Rizwan', 'No. 88, Marine Drive, Colombo 06', '0774445566', 'f.rizwan@outlook.com',
    '1992-07-19', 'FEMALE', @den_fernando, @trt_scaling,
    DATE_ADD(CURDATE(), INTERVAL 7 DAY), '15:00:00',
    NULL, @reception, @apt, @pat);


-- ===========================================================================
--  7. Check what was created
-- ===========================================================================
SELECT 'Patients'     AS record_type, COUNT(*) AS total FROM patients
UNION ALL SELECT 'Appointments',      COUNT(*) FROM appointments
UNION ALL SELECT '  ...booked',       COUNT(*) FROM appointments WHERE status = 'BOOKED'
UNION ALL SELECT '  ...completed',    COUNT(*) FROM appointments WHERE status = 'COMPLETED'
UNION ALL SELECT '  ...cancelled',    COUNT(*) FROM appointments WHERE status = 'CANCELLED'
UNION ALL SELECT 'Bills',             COUNT(*) FROM bills
UNION ALL SELECT '  ...paid',         COUNT(*) FROM bills WHERE payment_status = 'PAID'
UNION ALL SELECT '  ...unpaid',       COUNT(*) FROM bills WHERE payment_status = 'UNPAID'
UNION ALL SELECT 'Audit entries',     COUNT(*) FROM appointment_audit;
