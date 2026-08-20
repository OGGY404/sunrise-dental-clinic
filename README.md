# Sunrise Dental Clinic — Appointment & Patient Management System

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

Install these first:

1. **JDK 17** — [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17)
2. **Apache Maven 3.9+** — [maven.apache.org/download](https://maven.apache.org/download.cgi)
3. **MySQL 8** — [dev.mysql.com/downloads/installer](https://dev.mysql.com/downloads/installer/)
4. **Git** — [git-scm.com/download/win](https://git-scm.com/download/win)

Check they work by opening a new terminal and running:

```powershell
java -version
mvn -v
mysql --version
git --version
```

### Set your database password

Copy the template and put your own MySQL password in it:

```powershell
Copy-Item .env.example .env
```

Then open `.env` and change `DB_PASSWORD=changeme` to your real MySQL password.
The `.env` file is ignored by Git, so the password is never uploaded to GitHub.

### Start the application

```powershell
mvn spring-boot:run
```

Then open <http://localhost:8080> in your browser.

### Run the tests

```powershell
mvn test
```

The coverage report is written to `target/site/jacoco/index.html`.

---

## 6. Build progress

- [x] **Step 1** — Repository, `.gitignore`, README, Spring Boot skeleton
- [x] **Step 2** — Database schema, stored procedures, triggers
- [ ] **Step 3** — Domain entities and repositories (tests first)
- [ ] **Step 4** — Service layer with design patterns (tests first)
- [ ] **Step 5** — REST controllers and validation (tests first)
- [ ] **Step 6** — Login and sessions with Spring Security
- [ ] **Step 7** — Thymeleaf pages: login, register, search, billing, reports, help
- [ ] **Step 8** — Notifications and extra features
- [ ] **Step 9** — GitHub Actions workflow and deployment
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
