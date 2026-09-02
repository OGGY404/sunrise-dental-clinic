# Traceability matrix — Task C

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

Every requirement in the brief is followed here from the **design** that models
it, through the **code** that implements it, to the **tests** that prove it
works. A requirement with nothing in its test column would be a requirement
nobody has checked; there are none.

Read a row left to right and it answers: where was this designed, where does it
live, and how do I know it works.

---

## 1. Functional requirements

| Req | Requirement | Designed in | Implemented in | Verified by | Cases |
|---|---|---|---|---|---|
| **FR1** | Username and password login; only authorised staff may enter | UC diagram 1 (*Sign in*), sequence 5 | `SecurityConfig`, `ClinicUserDetailsService`, `LoginWebController`, `login.html` | `LoginAndSessionSecurityTest`, `ClinicUserDetailsServiceTest` | TC-FR1-01…09 |
| **FR2** | Register an appointment: unique number, patient name, address, contact, dentist, treatment, date, time | UC diagram 1 (*Register appointment* + 3 includes), class 2, sequence 6 | `BookingRequest`, `AppointmentService.register()`, `Appointment.Builder`, `AppointmentWebController`, `AppointmentRestController`, `register.html` | `BookingRequestValidationTest`, `AppointmentServiceTest`, `AppointmentWebControllerTest`, `AppointmentRestControllerTest`, `MySqlDatabaseIT` | TC-FR2-01…30, TC-DB-03 |
| **FR3** | Search by appointment number and show the full details | UC diagram 1 (*Find and display*), sequence 6 | `AppointmentService.findByNumber()`, `AppointmentWebController.search()/view()`, `search.html`, `view.html` | `AppointmentServiceTest`, `AppointmentWebControllerTest`, `AppointmentRestControllerTest` | TC-FR3-01…06 |
| **FR4** | Total = treatment cost + consultation fee; print the bill | UC diagram 1 (*Produce bill* + *Calculate charge*), class 4, sequence 7 | `BillingService`, `BillingStrategyFactory`, the three `…BillingStrategy` classes, `BillingWebController`, `bills/view.html` | `BillingServiceTest`, `BillingStrategyTest`, `BillingStrategyFactoryTest`, `BillingWebControllerTest`, `BillingRestControllerTest`, `MySqlDatabaseIT` | TC-FR4-01…20, TC-DB-04 |
| **FR5** | Help section with step-by-step instructions for new staff | UC diagram 1 (*Open help*) | `DashboardWebController.help()`, `help.html` | `DashboardWebControllerTest` | TC-FR5-01 |
| **FR6** | Safe, clean shutdown of the session | UC diagram 1 (*Sign out*) | `SecurityConfig` logout, Sign out form in `fragments.html` | `LoginAndSessionSecurityTest` | TC-FR6-01, TC-FR6-02 |

## 2. FR7 — additional functionality (the brief allows and rewards these)

| # | Feature | Designed in | Implemented in | Verified by | Cases |
|---|---|---|---|---|---|
| 7.1 | Cancel an appointment, and free the slot | UC diagram 1, sequence 8 | `AppointmentService.cancel()`, `slot_key` generated column | `AppointmentServiceTest`, `MySqlDatabaseIT` | TC-FR7-01…03, TC-FR7-08 |
| 7.2 | Reschedule an appointment | UC diagram 1 (*Reschedule* → *Check availability*) | `AppointmentService.reschedule()` | `AppointmentServiceTest` | TC-FR7-04, TC-FR7-05 |
| 7.3 | Mark completed / record a no-show | UC diagram 1 | `markCompleted()`, `markNoShow()` | `AppointmentServiceTest` | TC-FR7-06, TC-FR7-07 |
| 7.4 | Treatment history per patient | UC diagram 1 (*View treatment history*), class 2 | `AppointmentService.findPatientHistory()`, `PatientWebController.view()` | `PatientWebControllerTest`, `AppointmentRepositoryTest` | TC-FR7-09 |
| 7.5 | Patient search by name or telephone | class 2 | `PatientService.searchByName()/searchByContactNumber()` | `PatientServiceTest`, `PatientWebControllerTest` | TC-FR7-10…12 |
| 7.6 | Dentist availability / double-booking prevention | UC diagram 1 (*Check availability*), sequence 6 | `AppointmentService.checkSlotIsFree()` **and** `UNIQUE (slot_key)` | `AppointmentServiceTest`, `MySqlDatabaseIT` | TC-FR2-24, TC-DB-05 |
| 7.7 | Role-based access (admin vs receptionist) | UC diagram 1 (actor generalisation), class 3 | `SecurityConfig` rules, `Role` enum | `LoginAndSessionSecurityTest` | TC-SEC-01…04 |
| 7.8 | Daily appointment schedule report | class 3 | `AppointmentWebController.schedule()`, `schedule.html` | `AppointmentWebControllerTest` | TC-FR7-13…15 |
| 7.9 | Revenue by treatment report | class 3 | `ReportService.revenueByTreatment()` → `sp_report_revenue_by_treatment` | `ReportServiceTest`, `ReportWebControllerTest` | TC-FR7-16…20 |
| 7.10 | Dentist workload report | class 3 | `ReportService.dentistWorkload()` → `sp_report_dentist_workload` | `ReportServiceTest`, `ReportWebControllerTest` | TC-FR7-16, TC-FR7-18…20 |
| 7.11 | Unpaid bills chase list | class 3 | `BillingService.findUnpaidBills()`, `bills/unpaid.html` | `BillingWebControllerTest`, `BillRepositoryTest` | TC-FR4-19 |
| 7.12 | Email notification on booking (Observer) | class 4, sequence 6 | `AppointmentObserver`, `EmailAppointmentObserver` | `AppointmentServiceTest` | TC-FR2-25, TC-FR2-26 |

## 3. Non-functional and technical requirements

| Requirement (from the brief) | Designed in | Implemented in | Verified by | Cases |
|---|---|---|---|---|
| Distributed application with **web services** | class 3 (two controller families) | `controller` package — 16 REST endpoints | `AppointmentRestControllerTest`, `BillingRestControllerTest`, `PatientRestControllerTest`, `ReferenceDataRestControllerTest` | 43 tests |
| **Design patterns** implemented and named | class 4, class 2 (Builder) | Singleton, Strategy, Template Method, Factory, Builder, Observer, Repository, MVC | `BillingStrategyTest`, `BillingStrategyFactoryTest`, `AppointmentBuilderTest`, `ClinicConfigurationTest`, `AppointmentServiceTest` | 46 tests |
| **Proper database**, not text files | class 3 | MySQL 8, 10 tables | `MySqlDatabaseIT` | TC-DB-01 |
| Advanced DB features: **stored procedures, functions, triggers** | class 3, sequences 6–8 | `procedures.sql` (6 functions, 13 procedures), `triggers.sql` (10 triggers) | `MySqlDatabaseIT` | TC-DB-01…06 |
| **Three-tier architecture** | class diagram 3 | `controller` / `service` / `repository` packages | the layering is what allows 248 tests to run with no web server or database | whole suite |
| **Validation** on all inputs | class 2 | Bean Validation on `BookingRequest`, `CancelRequest`, `RescheduleRequest`, `BillRequest`, `PaymentRequest` | `BookingRequestValidationTest` | TC-FR2-01…16 |
| **Sessions and cookies** used effectively | sequence 5 | `SUNRISEID` cookie, HttpOnly, SameSite=Lax, 30-min timeout, session fixation protection | `LoginAndSessionSecurityTest`, `CsrfCookieEndToEndTest` | TC-FR1-05…07, TC-SEC-05…08 |
| **Separate views** for data entry and viewing results | sequences 6 and 7 | `register.html` → `view.html`; `generate.html` → `bills/view.html` | `AppointmentWebControllerTest`, `BillingWebControllerTest` | TC-FR2-27, TC-FR4-17 |
| **Multiple classes** at the business level | class 3 | 6 services + 3 strategies + factory + 2 observers | — | — |
| **Error-free, menu-driven, friendly messages** | class 3 | `fragments.html` navigation, `GlobalExceptionHandler`, `WebExceptionHandler` | `AppointmentWebControllerTest`, `BillingWebControllerTest` | TC-FR2-28, TC-FR2-29, TC-FR4-10 |
| Secure coding (ETHICAL criterion) | sequence 5 | BCrypt hashing, CSRF protection, no credentials in Git, identical refusal messages | `ClinicUserDetailsServiceTest`, `CsrfCookieEndToEndTest` | TC-FR1-09, TC-SEC-05…08 |

## 4. Requirements without a test — and why

| Item | Why it is not automated |
|---|---|
| Printing the bill on paper | `window.print()` hands over to the browser's own print dialogue. The print **stylesheet** is testable by eye only; this was checked by hand and recorded in `manual-walkthrough.md`. |
| Sending a real email | Notifications are switched off until real SMTP credentials exist. That the observer is *called*, and that a failing observer cannot lose a booking, are both tested (TC-FR2-25, TC-FR2-26). |
| Look and feel of the screens | Checked by hand; screenshots go in the report. Automated visual testing was judged out of scope for a project this size. |

Stating these three openly is deliberate. A traceability matrix that claimed
complete automated coverage would not be honest, and the gaps are small and
understood.

---

## 5. Coverage summary

| | Count |
|---|---|
| Functional requirements in the brief (FR1–FR6) | 6 — **all traced, all tested** |
| FR7 additional features delivered | 12 — all traced, all tested |
| Non-functional / technical requirements | 11 — 11 traced, 10 automated |
| Total automated test methods | **254** |
| Test classes | **30** |
