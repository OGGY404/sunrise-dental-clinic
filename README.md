# Sunrise Dental Clinic - Appointment & Patient Management System

[![Build and test](https://github.com/OGGY404/sunrise-dental-clinic/actions/workflows/ci.yml/badge.svg)](https://github.com/OGGY404/sunrise-dental-clinic/actions/workflows/ci.yml)


Coursework project for **CIS6003 Advanced Programming (WRIT1)**
ICBT Campus / Cardiff Metropolitan University — BSc (Hons) Software Engineering

---

## 1. What this system does

Sunrise Dental Clinic in Colombo handles appointments and treatment records on paper.
That has caused double bookings, lost records, long waiting times and billing errors.
This system replaces the paper process with a computerised one.

Clinic staff log in, register an appointment for a patient, look the appointment up
again by its unique appointment number, and print the bill for the visit.

---

## 2. Features

| # | Requirement | What it does |
|---|---|---|
| FR1 | User authentication | Staff log in with a username and password. Nobody else gets in. |
| FR2 | Register new appointment | Stores appointment number, patient name, address, contact number, dentist, treatment type, date and time. |
| FR3 | Display appointment details | Search by appointment number and see the full patient and appointment record. |
| FR4 | Calculate and print bill | Treatment cost + consultation fee, printed as a receipt. |
| FR5 | Help section | Step-by-step instructions for new staff. |
| FR6 | Exit system | Safe, clean shutdown and logout. |
| FR7 | Extra features | Appointment reminders, double-booking prevention, cancel/reschedule, treatment history, admin vs receptionist roles, reports. |

---

## 3. Architecture

The system is a **distributed application with web services**, built in **three tiers**
that are kept in separate Java packages:

```
Browser (Thymeleaf pages)  ─┐
REST API clients           ─┴─►  controller/   PRESENTATION TIER
                                     │           accepts input, chooses the view
                                     ▼
                                 service/      BUSINESS LOGIC TIER
                                     │           clinic rules, billing, notifications
                                     ▼
                                 repository/   DATA ACCESS TIER
                                     │           the only code that talks to the database
                                     ▼
                                  MySQL 8      tables + stored procedures + triggers
```

A tier only ever calls the tier directly below it. The controllers never touch the
database, and the repositories never contain business rules.

### Project layout

```
SunriseDentalClinic/
├── pom.xml                     Maven build file
├── .env.example                Template for local credentials (copy to .env)
├── .gitignore
└── src/
    ├── main/
    │   ├── java/lk/icbt/cis6003/dentalclinic/
    │   │   ├── DentalClinicApplication.java   Entry point
    │   │   ├── controller/                    Presentation tier
    │   │   ├── service/                       Business logic tier
    │   │   ├── repository/                    Data access tier
    │   │   ├── model/                         JPA entities (the class diagram)
    │   │   ├── dto/                           Form and API objects + validation
    │   │   ├── config/                        Security, sessions, Singleton config
    │   │   └── exception/                     Custom errors + friendly messages
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/                     Thymeleaf HTML pages
    │       ├── static/                        CSS, images
    │       └── db/                            schema.sql, procedures.sql, triggers.sql
    └── test/
        ├── java/...                           JUnit 5 + Mockito test classes
        └── resources/application-test.properties
```

---

## 4. Technology used

| Layer | Technology | Why |
|---|---|---|
| Language | Java 17 (LTS) | Required by the assessment brief |
| Framework | Spring Boot 3.3.5 | Gives REST web services and a layered structure |
| Web services | Spring Web (REST controllers) | Satisfies "distributed application with web services" |
| User interface | Thymeleaf | Server-side pages, which makes sessions and cookies natural |
| Persistence | Spring Data JPA + JdbcTemplate | JPA for normal queries, JDBC for stored procedures |
| Database | MySQL 8 | Free, and supports stored procedures, functions and triggers |
| Security | Spring Security + BCrypt | Hashed passwords, role-based access, session management |
| Testing | JUnit 5, Mockito, MockMvc, H2 | Test-driven development for Task C |
| Coverage | JaCoCo | Evidence of test coverage |
| Build | Maven | Standard, works with GitHub Actions |
| CI/CD | GitHub Actions | Required for the Task D top band |

**Note:** Lombok is deliberately *not* used. The report has to show real code with
visible access modifiers and full method signatures to match the UML class diagram,
and Lombok would hide the getters and setters.

---

## 5. Running the project locally

### Prerequisites

1. **JDK 17** - [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17)
2. **MySQL 8** - [dev.mysql.com/downloads](https://dev.mysql.com/downloads/)
3. **Git** - [git-scm.com/download/win](https://git-scm.com/download/win)

Maven does **not** need to be installed. The project includes the Maven
wrapper, so use `mvnw.cmd` everywhere instead of `mvn` and the right Maven
version downloads itself the first time.

Check what you have:

```powershell
java -version
git --version
```

### If you cannot install MySQL as a service

Installing MySQL normally needs administrator rights. If you do not have them,
download the **Windows ZIP archive** of MySQL 8.4 instead of the installer,
unzip it, and run it from that folder. That is how this machine is set up, in
`D:\DevTools`, with a small config file and two helper scripts:

- `D:\DevTools\start-mysql.cmd` - starts MySQL (run this before the app)
- `D:\DevTools\stop-mysql.cmd` - stops it cleanly

Because it is not a Windows service, **MySQL does not start by itself when the
computer is switched on**. Start it first, every time.

### Set your database password

```powershell
Copy-Item .env.example .env
```

Open `.env` and set `DB_PASSWORD` to your MySQL password. The `.env` file is
ignored by Git, so the password is never uploaded to GitHub. If your local root
account has no password, leave it empty.

The application creates the `sunrise_dental` database itself on first start,
then builds the tables, the stored procedures and functions, and the triggers,
and loads the reference data. Nothing has to be created by hand.

### Start the application

```powershell
.\mvnw.cmd spring-boot:run
```

Then open <http://localhost:8080>. You will be asked to sign in.

**Starting logins** (change these before any real use - see `db/data.sql`):

| Username    | Password    | Role         | Can do                        |
|-------------|-------------|--------------|-------------------------------|
| `admin`     | `Admin@123` | ADMIN        | everything, including reports |
| `reception` | `Recep@123` | RECEPTIONIST | bookings and billing          |

### The screens

Sign in and everything is one click away from the green bar at the top.

| Address | Screen | Requirement |
|---|---|---|
| `/login` | sign in | FR1 |
| `/` | main menu, today's diary, what is owed | menu-driven system |
| `/appointments/new` | register a visit (data entry) | FR2 |
| `/appointments/search` | find by appointment number (data entry) | FR3 |
| `/appointments/{no}` | the visit, and complete / cancel / move it | FR3, FR7 |
| `/appointments/schedule` | the day's diary, ready to print | reports |
| `/patients` | search by telephone or name | |
| `/patients/{code}` | one patient and their whole history | FR7 |
| `/bills/new` | produce a bill (data entry) | FR4 |
| `/bills/{billNo}` | the printable receipt | FR4 |
| `/bills/unpaid` | what is still owed, oldest first | reports |
| `/reports` | revenue by treatment, dentist workload | reports, **admin only** |
| `/help` | step-by-step instructions for new staff | FR5 |
| Sign out button | ends the session safely | FR6 |

Data entry and viewing results are deliberately separate screens, and every
save redirects to the result page. That is not decoration: it means pressing
refresh on a result page cannot book a second appointment or produce a second
bill.

The two management reports are produced by the stored procedures
`sp_report_revenue_by_treatment` and `sp_report_dentist_workload`, so the
grouping and totalling happen inside MySQL and only the finished summary is
sent back.

### The web services

Everything below needs a signed-in session. Anything that changes data also
needs the CSRF token, which the server sends in the `XSRF-TOKEN` cookie and
expects back in the `X-XSRF-TOKEN` header.

| Method | Address | What it does |
|---|---|---|
| POST | `/api/appointments` | FR2 register a visit |
| GET | `/api/appointments/{no}` | FR3 display a visit |
| GET | `/api/appointments?date=&dentistId=` | the day schedule report |
| POST | `/api/appointments/{no}/cancel` | FR7 cancel |
| POST | `/api/appointments/{no}/reschedule` | FR7 move to another slot |
| POST | `/api/appointments/{no}/complete` | the visit took place |
| POST | `/api/appointments/{no}/no-show` | the patient never arrived |
| GET | `/api/patients/{code}` | one patient record |
| GET | `/api/patients?name=` or `?contact=` | search |
| GET | `/api/patients/{code}/history` | FR7 treatment history |
| POST | `/api/bills` | FR4 produce the bill |
| GET | `/api/bills/{billNo}` | FR4 print the receipt |
| GET | `/api/bills/for-appointment/{no}` | the receipt for a visit |
| GET | `/api/bills/unpaid` | what is still owed |
| POST | `/api/bills/{billNo}/pay` | record the payment |
| GET | `/api/dentists`, `/api/treatments` | the two dropdown lists |

Every failure comes back in the same shape, so a screen only ever has to read
`message`:

| Status | Meaning |
|---|---|
| 400 | the form is wrong; `fieldErrors` names each box that failed |
| 401 | not signed in |
| 403 | signed in, but not allowed, or the CSRF token was missing |
| 404 | that reference number does not exist |
| 409 | that dentist is already booked at that time |
| 422 | understood, but a clinic rule refuses it |

### Run the tests

```powershell
.\mvnw.cmd test
```

248 tests. They all run on an in-memory H2 database, so **MySQL does not need
to be running** to test. The coverage report is written to
`target/site/jacoco/index.html`.

---

## 5b. Continuous integration and releases (Task D)

Every push and every pull request runs
[`.github/workflows/ci.yml`](.github/workflows/ci.yml) on GitHub Actions, in
three jobs that run in this order:

| Job | What it proves | Needs a database? |
|---|---|---|
| **Unit and web tests** | the 248 tests: clinic rules, validation, screens, security | no, in-memory H2 |
| **Schema, procedures and triggers** | the half of the system written in SQL | yes, a real MySQL 8 container |
| **Package the runnable jar** | only runs if both test jobs passed | no |

**Why the second job exists.** H2 builds its tables from the Java entities, so
it can only ever agree with them, and it has none of the stored procedures,
triggers or generated columns. Two real bugs reached the running application
through exactly that gap — a `CHAR(60)` column the entity called `VARCHAR`, and
a database collation that stopped every booking. `MySqlDatabaseIT` now checks
all of it on a clean MySQL 8 every time: that the scripts create 10 tables, 6
functions, 13 procedures and 10 triggers; that the collations match; that
booking really does go through the counter procedure and fire the audit
trigger; that MySQL calculates the bill total itself; and that the database
refuses a double booking even when the Java is bypassed entirely.

Writing that test found a third bug straight away, which is recorded in the
commit history.

### Releasing a version

Pushing a version tag runs
[`.github/workflows/release.yml`](.github/workflows/release.yml), which builds
from a clean checkout, runs the tests again, and publishes the runnable jar as
a GitHub Release:

```powershell
git tag -a v0.9.0 -m "Step 9: CI/CD pipeline"
git push origin v0.9.0
```

Releases are at
<https://github.com/OGGY404/sunrise-dental-clinic/releases>.

### Deploying that jar

Spring Boot packages the web server inside the jar, so a server needs Java 17,
MySQL 8 and one command:

```bash
export DB_HOST=localhost DB_NAME=sunrise_dental DB_USERNAME=clinic DB_PASSWORD=...
export COOKIE_SECURE=true          # once it is served over HTTPS
java -jar dental-clinic-0.0.1-SNAPSHOT.jar
```

The database, its tables, stored procedures, functions and triggers are all
created on first start, so nothing has to be set up by hand.

---

## 6. Build progress

- [x] **Step 1** — Repository, `.gitignore`, README, Spring Boot skeleton
- [x] **Step 2** — Database schema, stored procedures, triggers
- [x] **Step 3** — Domain entities and repositories (tests first)
- [x] **Step 4** — Service layer with design patterns (tests first)
- [x] **Step 5** — REST controllers and validation (tests first)
- [x] **Step 6** — Login and sessions with Spring Security
- [x] **Step 7** — Thymeleaf pages: login, register, search, billing, reports, help
- [ ] **Step 8** — Notifications and extra features
- [x] **Step 9** — GitHub Actions workflow and deployment
- [ ] **Step 10** — UML diagrams
- [ ] **Step 11** — Test plan, traceability matrix, screenshots
- [ ] **Step 12** — Final report

---

## 7. The database

The brief asks for a proper database using advanced features, so the schema does
real work rather than just storing rows. The SQL lives in
`src/main/resources/db/` and runs automatically when the application starts.

| File | What it contains |
|---|---|
| `schema.sql` | 10 tables, foreign keys, CHECK constraints, indexes |
| `procedures.sql` | 6 stored functions and 13 stored procedures |
| `triggers.sql` | 10 triggers for business rules and auditing |
| `data.sql` | Reference data: settings, staff logins, dentists, price list |
| `demo-data.sql` | Sample patients and visits — **run by hand only** |

### Tables

`clinic_settings` · `users` · `dentists` · `treatments` · `patients` ·
`appointments` · `bills` · `appointment_audit` · `daily_counter` · `code_counter`

### Advanced features and why each one is there

| Feature | Where | What problem it solves |
|---|---|---|
| Stored procedure | `sp_register_appointment` | Books a patient and an appointment as one unit of work, so the clinic can never end up with half a booking |
| Stored function | `fn_is_dentist_available` | Answers "is this chair free?" in one place, used by booking and rescheduling |
| Generated column | `appointments.slot_key` | Holds `dentist\|date\|time` for live appointments and NULL for cancelled ones |
| Unique index | `uq_appointments_slot` | **Stops double booking at database level** — two receptionists saving at the same instant cannot both win |
| Generated column | `bills.total_amount` | The total is computed by the database, so it can never disagree with its parts |
| Trigger | `trg_users_before_insert` | Rejects any password that is not a BCrypt hash, so plain text can never be stored |
| Trigger | `trg_appointments_after_update` | Writes an audit row on every change — the paper system could not do this |
| Trigger | `trg_bills_before_update` | Freezes a receipt once issued; only the payment can be recorded |
| Counter tables | `daily_counter`, `code_counter` | Hand out unique reference numbers safely instead of counting existing rows |

### Reference numbers

| Kind | Example | Resets |
|---|---|---|
| Appointment | `APT-20260820-0007` | Daily |
| Bill | `BIL-20260820-0003` | Daily |
| Patient | `PAT-000042` | Never |

### Reports (stored procedures)

`sp_report_daily_schedule` · `sp_report_revenue_by_treatment` ·
`sp_report_patient_history` · `sp_report_dentist_workload` ·
`sp_report_upcoming_reminders`

### Loading the sample data

The reference data loads by itself. Sample patients and visits are separate,
because restarting the application should not invent appointments nobody booked:

```powershell
mysql -u root -p sunrise_dental < src/main/resources/db/demo-data.sql
```

It deletes existing patient, appointment and bill data first, so it can be run
repeatedly while preparing the report. Every appointment in it is created by
calling `sp_register_appointment`, so running it also tests the procedures and
triggers.

### A note on the `;;` separator

A stored procedure body contains single semicolons inside it. If `;` were the
statement separator, Spring would cut each procedure in half. The scripts
therefore use `;;`, set by `spring.sql.init.separator` in
`application.properties`. `demo-data.sql` is the exception — it uses a normal
`;`, because it is run by the MySQL client rather than by Spring.

---

## 8. Design patterns used

Each pattern is marked in the source code with a comment naming it and explaining
why it is there, so the code itself is the evidence for the report.

| Pattern | Where it is used |
|---|---|
| Singleton | Clinic configuration holder (consultation fee, opening hours) |
| Repository (DAO) | Data access for users, patients, appointments |
| Factory | Creating the right billing rule for a treatment type |
| Strategy | Different billing calculations per treatment type |
| MVC | Overall structure of the web layer |
| Builder | Building `Appointment` objects, which have many fields |
| Observer | Sending notifications when an appointment is created |

---

## 9. Academic integrity

The source code in this repository was developed with AI assistance, which is
recorded in the accompanying report. The written analysis, UML justifications,
critical evaluation and reflection in the report are the author's own work, in
line with Cardiff Metropolitan University's unfair practice regulations.
