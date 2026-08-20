-- ===========================================================================
--  Sunrise Dental Clinic - REFERENCE DATA
--  CIS6003 Advanced Programming (WRIT1)
--
--  This file holds the data the system cannot work without: clinic settings,
--  the staff logins, the dentists, and the price list. It runs every time the
--  application starts, so every statement uses INSERT IGNORE - if the row is
--  already there, MySQL skips it instead of failing.
--
--  Sample patients, appointments and bills are NOT here. They live in
--  demo-data.sql, which is run by hand when demonstration data is needed.
--  Keeping them apart means restarting the application never invents
--  appointments that nobody booked.
--
--  Statements are separated by ";;" - see the note at the top of schema.sql.
-- ===========================================================================


-- ---------------------------------------------------------------------------
--  Clinic settings
--  Management can change these without a developer rebuilding the program.
--  They are read by the Singleton configuration holder in Java and by the
--  fn_get_setting function in the database.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO clinic_settings (setting_key, setting_value, description) VALUES
    ('clinic_name',              'Sunrise Dental Clinic', 'Name printed on bills and reports'),
    ('clinic_city',              'Colombo',               'City the clinic operates in'),
    ('clinic_address',           'No. 42, Galle Road, Colombo 03', 'Address printed on the bill'),
    ('clinic_phone',             '0112345678',            'Telephone number printed on the bill'),
    ('consultation_fee',         '1500.00',               'Added to every bill on top of the treatment cost'),
    ('opening_time',             '08:00:00',              'Earliest appointment time'),
    ('closing_time',             '18:00:00',              'Latest appointment time'),
    ('appointment_slot_minutes', '30',                    'Appointments must start on a boundary of this many minutes'),
    ('currency',                 'LKR',                   'Currency shown on bills'),
    ('reminder_days_ahead',      '1',                     'How many days before the visit a reminder is sent');;


-- ---------------------------------------------------------------------------
--  Staff logins  (FR1, and FR7 role-based access)
--
--  The password column holds a BCrypt hash, never the real password - a rule
--  the trg_users_before_insert trigger enforces. These hashes were produced at
--  strength 10, which is what Spring Security's BCryptPasswordEncoder uses.
--
--  Starting passwords, for first login only:
--      admin     / Admin@123     (ADMIN        - full access, including reports)
--      reception / Recep@123     (RECEPTIONIST - bookings and billing)
--
--  SECURITY NOTE: default accounts like these are a well-known weakness, and a
--  real clinic must change both passwords immediately after installation.
--  A "force password change on first login" screen is planned for step 6; until
--  then these accounts exist purely so the system can be demonstrated. The
--  report discusses why shipping default credentials is a risk.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO users (username, password_hash, full_name, email, role, enabled) VALUES
    ('admin',
     '$2b$10$3CG1Xb9LPd/ERcwjWy5gSe1CIFtttVsTc4FYD2lcjzsHClY/WLjj2',
     'System Administrator', 'admin@sunrisedental.lk', 'ADMIN', TRUE),
    ('reception',
     '$2b$10$ouUQlnrZizKrAaFNxqFqL.miY/G1Bam/BLKi8STx9izsAZtUUVRGS',
     'Front Desk Receptionist', 'reception@sunrisedental.lk', 'RECEPTIONIST', TRUE);;


-- ---------------------------------------------------------------------------
--  Dentists
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO dentists (dentist_code, full_name, specialisation, contact_number, email, active) VALUES
    ('DEN-001', 'Dr. Nimal Perera',      'General Dentistry',  '0771234501', 'n.perera@sunrisedental.lk',   TRUE),
    ('DEN-002', 'Dr. Shanika Fernando',  'Orthodontics',       '0771234502', 's.fernando@sunrisedental.lk', TRUE),
    ('DEN-003', 'Dr. Ruwan Jayasinghe',  'Oral Surgery',       '0771234503', 'r.jaya@sunrisedental.lk',     TRUE),
    ('DEN-004', 'Dr. Anusha Wickrama',   'Paediatric Dentistry','0771234504','a.wickrama@sunrisedental.lk', TRUE);;


-- ---------------------------------------------------------------------------
--  Price list  (FR4 - the treatment cost half of every bill)
--  Costs are in Sri Lankan Rupees. The consultation fee is added separately.
--  duration_minutes is used by the dentist workload report.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO treatments (treatment_code, name, description, cost, duration_minutes, active) VALUES
    ('TRT-001', 'Dental Check-up',        'Routine examination and advice',              2000.00,  30, TRUE),
    ('TRT-002', 'Scaling and Polishing',  'Cleaning to remove plaque and stains',        4500.00,  45, TRUE),
    ('TRT-003', 'Tooth Filling',          'Composite filling for one cavity',            6000.00,  45, TRUE),
    ('TRT-004', 'Tooth Extraction',       'Simple removal of one tooth',                 5500.00,  30, TRUE),
    ('TRT-005', 'Root Canal Treatment',   'Full endodontic treatment of one tooth',     25000.00, 120, TRUE),
    ('TRT-006', 'Dental Crown',           'Porcelain crown, fitting appointment',       32000.00,  90, TRUE),
    ('TRT-007', 'Teeth Whitening',        'In-clinic professional whitening',           18000.00,  60, TRUE),
    ('TRT-008', 'Braces Consultation',    'Orthodontic assessment and treatment plan',   3500.00,  45, TRUE),
    ('TRT-009', 'Braces Fitting',         'Fitting of fixed metal braces',              85000.00, 120, TRUE),
    ('TRT-010', 'Denture Fitting',        'Partial or complete denture fitting',        45000.00,  90, TRUE),
    ('TRT-011', 'Wisdom Tooth Surgery',   'Surgical removal of an impacted wisdom tooth',35000.00, 120, TRUE),
    ('TRT-012', 'Child Dental Care',      'Examination and fluoride treatment for children', 2500.00, 30, TRUE);;
