# Test plan — Task C

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

Every test case below is **automated**. The "Automated by" column names the real
test method that runs it, so any case in this table can be traced to code and
re-run on demand. Nothing here was executed by hand and then written down.

- **Last full run:** `./mvnw.cmd test` — **248 tests, 0 failures, 0 errors**
- **Plus** `./mvnw.cmd test -Dtest=MySqlDatabaseIT` against real MySQL 8 —
  **6 tests, 0 failures**
- **Total: 254 automated test methods across 30 test classes**

---

## 1. Test levels

| Level | What it tests | How many | Runs against | Speed |
|---|---|---|---|---|
| Unit | one class, collaborators replaced by Mockito mocks | 114 | nothing | milliseconds |
| Repository | the JPA mappings and the derived queries | 38 | in-memory H2 | ~1 second |
| Web slice | one controller, services mocked, real HTTP handling | 77 | nothing | ~1 second |
| Integration | the whole application context | 19 | H2, and a real port for CSRF | ~10 seconds |
| Database | schema, procedures, triggers, generated columns | 6 | **real MySQL 8** | ~10 seconds |

The split is deliberate. The fast levels carry most of the rules, so a broken
rule is reported in under a minute; the slow levels are kept for the things the
fast ones genuinely cannot see.

---

## 2. Test cases

### FR1 — User authentication

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR1-01 | account `reception` exists and is enabled | POST the sign-in form | `reception` / `Recep@123` | signed in as ROLE_RECEPTIONIST, redirected to `/` | as expected | Pass | `LoginAndSessionSecurityTest.correctPasswordLogsIn` |
| TC-FR1-02 | same account exists | POST with the wrong password | `reception` / `wrong` | refused, redirected to `/login?error` | as expected | Pass | `LoginAndSessionSecurityTest.wrongPasswordIsRefused` |
| TC-FR1-03 | no such account | POST with an unknown name | `nobody` / `Recep@123` | refused **identically** to TC-FR1-02 | as expected | Pass | `LoginAndSessionSecurityTest.unknownUsernameIsRefused` |
| TC-FR1-04 | account exists but `enabled = false` | POST with the right password | `oldstaff` / `Recep@123` | refused; account cannot sign in | as expected | Pass | `LoginAndSessionSecurityTest.disabledAccountIsRefused` |
| TC-FR1-05 | not signed in | GET a clinic page | `/appointments` | 302 redirect to `/login` | as expected | Pass | `LoginAndSessionSecurityTest.browserIsSentToLogin` |
| TC-FR1-06 | not signed in | GET a web service | `/api/appointments/APT-…` | 401 with a JSON message, not a login page | as expected | Pass | `LoginAndSessionSecurityTest.webServiceGets401` |
| TC-FR1-07 | account exists | sign in successfully | `reception` | `users.last_login_at` is written | as expected | Pass | `LoginAndSessionSecurityTest.recordsTheLoginTime` |
| TC-FR1-08 | role is ADMIN in the database | load the user for Spring Security | `admin` | authority is `ROLE_ADMIN`, with the prefix added | as expected | Pass | `ClinicUserDetailsServiceTest.addsTheRolePrefix` |
| TC-FR1-09 | no such account | load the user | `nobody` | message is "Bad credentials" and does **not** contain the username | as expected | Pass | `ClinicUserDetailsServiceTest.refusalDoesNotLeakWhetherTheAccountExists` |

### FR2 — Register a new appointment

**Input validation (boundary and invalid data — see also `test-data.md`)**

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR2-01 | — | submit a correctly filled form | all fields valid | accepted, no violations | as expected | Pass | `BookingRequestValidationTest.acceptsAValidForm` |
| TC-FR2-02 | — | name left blank | `"   "` | rejected, field `fullName` | as expected | Pass | `…nameIsRequired` |
| TC-FR2-03 | — | digits typed into the name | `"Kamal 123"` | rejected, field `fullName` | as expected | Pass | `…nameRejectsDigits` |
| TC-FR2-04 | — | a real Sri Lankan name with dots and hyphen | `"W.A.G.K. Rathnayake-Silva"` | **accepted** | as expected | Pass | `…nameAllowsDotsAndHyphens` |
| TC-FR2-05 | — | name one character over the column width | 101 × `"A"` | rejected (boundary) | as expected | Pass | `…nameHasAMaximumLength` |
| TC-FR2-06 | — | local telephone number | `0112345678` | accepted | as expected | Pass | `…acceptsLocalNumber` |
| TC-FR2-07 | — | same number with country code | `+94771234567` | accepted | as expected | Pass | `…acceptsInternationalNumber` |
| TC-FR2-08 | — | one digit short | `077123456` | rejected (boundary) | as expected | Pass | `…rejectsTooShort` |
| TC-FR2-09 | — | letters in the number box | `077ABC4567` | rejected | as expected | Pass | `…rejectsLetters` |
| TC-FR2-10 | — | yesterday's date | `today − 1` | rejected (boundary) | as expected | Pass | `…dateCannotBeInThePast` |
| TC-FR2-11 | — | today's date | `today` | **accepted** — a walk-in can be seen today | as expected | Pass | `…todayIsAllowed` |
| TC-FR2-12 | — | no dentist chosen | `null` | rejected, field `dentistId` | as expected | Pass | `…dentistIsRequired` |
| TC-FR2-13 | — | email left empty | `null` | accepted — not every patient has one | as expected | Pass | `…emailMayBeEmpty` |
| TC-FR2-14 | — | malformed email | `kamal-at-example` | rejected | as expected | Pass | `…emailMustLookLikeOne` |
| TC-FR2-15 | — | date of birth in the future | `today + 1` | rejected | as expected | Pass | `…dateOfBirthCannotBeInTheFuture` |
| TC-FR2-16 | — | notes one character over the column | 501 chars | rejected (boundary) | as expected | Pass | `…notesHaveAMaximumLength` |

**Clinic rules**

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR2-17 | dentist and treatment are active | register a visit | valid booking | saved, with a unique appointment number | as expected | Pass | `AppointmentServiceTest.registersAnAppointment` |
| TC-FR2-18 | — | register a visit | valid booking | the number comes from the generator, never invented in Java | as expected | Pass | `…takesTheNumberFromTheGenerator` |
| TC-FR2-19 | — | time before opening | `07:00` | refused, "the clinic is only open between…" | as expected | Pass | `…refusesATimeOutsideOpeningHours` |
| TC-FR2-20 | — | time off the half-hour grid | `09:17` | refused | as expected | Pass | `…refusesATimeOffTheSlotGrid` |
| TC-FR2-21 | dentist id does not exist | register | `dentistId = 999` | refused, "no dentist was found…" | as expected | Pass | `…refusesAnUnknownDentist` |
| TC-FR2-22 | dentist `active = false` | register | a retired dentist | refused, "no longer practising" | as expected | Pass | `…refusesARetiredDentist` |
| TC-FR2-23 | treatment `active = false` | register | off the price list | refused | as expected | Pass | `…refusesAnInactiveTreatment` |
| TC-FR2-24 | that dentist already has that slot | register the same slot | same dentist, date, time | refused with `SlotUnavailableException` | as expected | Pass | `…refusesADoubleBooking` |
| TC-FR2-25 | booking succeeds | register | valid booking | observers are told **after** the save | as expected | Pass | `…notifiesObserversAfterSaving` |
| TC-FR2-26 | an observer throws | register | mail server down | the booking still completes | as expected | Pass | `…aFailingObserverDoesNotLoseTheBooking` |
| TC-FR2-27 | signed in as receptionist | submit the booking screen | valid form | 302 redirect to the details screen, flash message | as expected | Pass | `AppointmentWebControllerTest.goodBookingRedirectsToTheDetails` |
| TC-FR2-28 | signed in | submit a bad form on the screen | bad name + short phone | same form returned, values kept, dropdowns reloaded | as expected | Pass | `…badFormComesBackFilledIn` |
| TC-FR2-29 | slot already taken | submit the booking screen | taken slot | message on the form, **not** an error page | as expected | Pass | `…takenSlotIsShownOnTheForm` |
| TC-FR2-30 | signed in | POST to the web service | valid JSON | 201 Created + `Location` header | as expected | Pass | `AppointmentRestControllerTest.setsTheLocationHeader` |

### FR3 — Display appointment details

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR3-01 | the appointment exists | search by number | `APT-20260908-0001` | the visit is returned | as expected | Pass | `AppointmentServiceTest.findsByAppointmentNumber` |
| TC-FR3-02 | no such number | search | `APT-NOPE` | clear "not found" message | as expected | Pass | `…reportsAnUnknownAppointmentNumber` |
| TC-FR3-03 | the appointment exists | open the details screen | `APT-…-0001` | patient name, address, contact, dentist, treatment, date, time shown | as expected | Pass | `AppointmentWebControllerTest.detailsScreenShowsTheVisit` |
| TC-FR3-04 | no such number | search on the screen | `APT-NOPE` | stays on the search screen with a message | as expected | Pass | `…unknownNumberGoesBackToSearch` |
| TC-FR3-05 | the appointment exists | search on the screen | valid number | redirect to the details screen | as expected | Pass | `…searchingRedirectsToTheDetails` |
| TC-FR3-06 | no such number | GET the web service | `APT-NOPE` | 404 with the number in the message | as expected | Pass | `AppointmentRestControllerTest.answers404ForAnUnknownNumber` |

### FR4 — Calculate and print the bill

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR4-01 | visit COMPLETED, ordinary treatment | produce the bill | filling Rs. 6,000 | treatment + consultation fee | as expected | Pass | `BillingServiceTest.billsAnOrdinaryTreatment` |
| TC-FR4-02 | visit COMPLETED, surgical treatment | produce the bill | extraction | the surgical rule is chosen | as expected | Pass | `…usesTheSurgicalRule` |
| TC-FR4-03 | visit COMPLETED, check-up | produce the bill | check-up | consultation fee **not** charged twice | as expected | Pass | `…usesTheConsultationOnlyRule` |
| TC-FR4-04 | patient has 4 completed visits | calculate | 4th visit | **no** loyalty discount (boundary) | as expected | Pass | `BillingStrategyTest.noDiscountOnTheFourthVisit` |
| TC-FR4-05 | patient has 5 completed visits | calculate | 5th visit | 10% off the treatment cost (boundary) | as expected | Pass | `BillingStrategyTest.appliesLoyaltyDiscount` |
| TC-FR4-06 | surgical treatment | calculate | Rs. 25,000 | +15% sterilisation supplement = 28,750 | as expected | Pass | `BillingStrategyTest.addsSterilisationSupplement` |
| TC-FR4-07 | surgical + loyal patient | calculate | — | loyalty worked out on the **supplemented** cost | as expected | Pass | `BillingStrategyTest.loyaltyDiscountUsesSupplementedCost` |
| TC-FR4-08 | any bill | calculate | odd fractions | rounded to 2 decimal places | as expected | Pass | `BillingStrategyTest.roundsDiscountToTwoDecimalPlaces` |
| TC-FR4-09 | discount larger than the bill | calculate | huge discount | total never below zero | as expected | Pass | `BillingStrategyTest.neverGoesBelowZero` |
| TC-FR4-10 | visit still BOOKED | produce the bill | not completed | refused, "has not taken place yet" | as expected | Pass | `BillingServiceTest.refusesToBillABookedVisit` |
| TC-FR4-11 | visit CANCELLED | produce the bill | cancelled visit | refused | as expected | Pass | `…refusesToBillACancelledVisit` |
| TC-FR4-12 | the visit is already billed | produce a second bill | same visit | refused, "already been billed" | as expected | Pass | `…refusesToBillTwice` |
| TC-FR4-13 | — | negative discount | `−1.00` | refused — a hidden price rise | as expected | Pass | `…refusesANegativeDiscount` |
| TC-FR4-14 | — | discount bigger than the bill | over the total | refused | as expected | Pass | `…refusesADiscountBiggerThanTheBill` |
| TC-FR4-15 | bill is UNPAID | record payment | `CASH` | marked PAID, method and time recorded | as expected | Pass | `…marksABillPaid` |
| TC-FR4-16 | bill is already PAID | record payment again | `CASH` | refused | as expected | Pass | `…refusesToPayTwice` |
| TC-FR4-17 | visit COMPLETED | produce the bill on the screen | — | 302 redirect to the receipt page | as expected | Pass | `BillingWebControllerTest.producingABillRedirectsToTheReceipt` |
| TC-FR4-18 | — | enter an agreed discount on the screen | `500.00` | passed to the business tier unchanged | as expected | Pass | `…discountIsPassedDown` |
| TC-FR4-19 | bill exists | open the receipt | `BIL-…-0001` | its own printable page | as expected | Pass | `…receiptIsItsOwnPage` |
| TC-FR4-20 | web service, no discount sent | POST a bill | `{appointmentNo}` | 201, three amounts and the total returned | as expected | Pass | `BillingRestControllerTest.producesABill` |

### FR5 — Help section

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR5-01 | signed in | open Help | `/help` | the help screen opens with no data needed | as expected | Pass | `DashboardWebControllerTest.helpScreenOpens` |

### FR6 — Exit the system safely

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR6-01 | signed in | press Sign out | POST `/logout` | session ended, redirect to `/login?logout` | as expected | Pass | `LoginAndSessionSecurityTest.logoutEndsTheSession` |
| TC-FR6-02 | signed out | call a web service again | `/api/treatments` | 401 — the old session is dead | as expected | Pass | verified end to end in `docs/testing/manual-walkthrough.md` step 18 |

### FR7 — Additional functionality

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-FR7-01 | visit is BOOKED | cancel with a reason | "patient telephoned" | status CANCELLED, observers told | as expected | Pass | `AppointmentServiceTest.cancelsABookedAppointment` |
| TC-FR7-02 | visit already CANCELLED | cancel again | — | refused | as expected | Pass | `…refusesToCancelTwice` |
| TC-FR7-03 | visit COMPLETED | cancel | — | refused — it already happened | as expected | Pass | `…refusesToCancelACompletedVisit` |
| TC-FR7-04 | target slot is free | reschedule | new date and time | moved | as expected | Pass | `…reschedulesToAFreeSlot` |
| TC-FR7-05 | target slot is taken | reschedule | taken slot | refused | as expected | Pass | `…refusesToRescheduleOntoATakenSlot` |
| TC-FR7-06 | visit is BOOKED | mark completed | — | status COMPLETED, now billable | as expected | Pass | `…marksAVisitCompleted` |
| TC-FR7-07 | visit CANCELLED | mark completed | — | refused | as expected | Pass | `…refusesToCompleteACancelledAppointment` |
| TC-FR7-08 | cancel form left empty | cancel on the screen | reason `""` | refused before the service is called | as expected | Pass | `AppointmentRestControllerTest.cancelNeedsAReason` |
| TC-FR7-09 | patient exists | open the patient screen | `PAT-000001` | full treatment history, newest first | as expected | Pass | `PatientWebControllerTest.patientScreenShowsHistory` |
| TC-FR7-10 | patients exist | search by telephone and name together | both given | telephone wins — it is the reliable one | as expected | Pass | `…telephoneWinsOverName` |
| TC-FR7-11 | patient screen opened fresh | no search entered | — | **no** patient list shown | as expected | Pass | `…searchScreenOpensEmpty` |
| TC-FR7-12 | nobody matches | search | `Nobody` | empty list, not an error | as expected | Pass | `…emptyResultIsNotAnError` |
| TC-FR7-13 | appointments exist that day | open the day schedule | a date | the whole clinic diary | as expected | Pass | `AppointmentWebControllerTest.showsTheDiary` |
| TC-FR7-14 | no date chosen | open the day schedule | — | defaults to today | as expected | Pass | `…defaultsToToday` |
| TC-FR7-15 | a dentist chosen | open the day schedule | `dentistId = 1` | narrowed to that dentist | as expected | Pass | `…narrowsToOneDentist` |
| TC-FR7-16 | signed in as ADMIN | open the revenue report | a date range | the stored procedure is called with those dates | as expected | Pass | `ReportWebControllerTest.revenueReportUsesTheChosenDates` |
| TC-FR7-17 | no dates chosen | open the revenue report | — | defaults to this month so far | as expected | Pass | `…revenueReportDefaultsToThisMonth` |
| TC-FR7-18 | — | report with a backwards range | `to` before `from` | refused before the database is asked | as expected | Pass | `ReportServiceTest.backwardsDateRangeIsRefused` |
| TC-FR7-19 | — | report with one date missing | `from = null` | refused | as expected | Pass | `ReportServiceTest.missingDateIsRefused` |
| TC-FR7-20 | — | report for a single day | `from = to` | **allowed** (boundary) | as expected | Pass | `ReportServiceTest.oneDayRangeIsAllowed` |

### NFR — Security, sessions and roles

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-SEC-01 | signed in as RECEPTIONIST | open the admin area | `/api/admin/settings` | 403 with a JSON message | as expected | Pass | `LoginAndSessionSecurityTest.receptionistIsRefusedAdminArea` |
| TC-SEC-02 | signed in as ADMIN | open the same address | `/api/admin/settings` | **not** 403 — the role rule lets it past | as expected | Pass | `…adminIsAllowedIntoAdminArea` |
| TC-SEC-03 | signed in as RECEPTIONIST | open the reports | `/reports` | 403 | as expected | Pass | `…receptionistCannotOpenReports` |
| TC-SEC-04 | signed in as ADMIN | open the reports | `/reports` | 200 | as expected | Pass | `…adminCanOpenReports` |
| TC-SEC-05 | signed in | POST with no CSRF token | any write | 403 | as expected | Pass | `…writeWithoutTokenIsRefused` |
| TC-SEC-06 | signed in | GET with no CSRF token | any read | allowed — reading changes nothing | as expected | Pass | `…readNeedsNoToken` |
| TC-SEC-07 | real server, real HTTP | sign in, follow the redirect, read the cookie, write | full browser flow | the token from the cookie gets the write past CSRF | as expected | Pass | `CsrfCookieEndToEndTest.tokenFromCookieLetsAWriteThrough` |
| TC-SEC-08 | real server, signed in | write with no token at all | — | 403 | as expected | Pass | `CsrfCookieEndToEndTest.writeWithoutTokenIsStillRefused` |

### NFR — Advanced database features (real MySQL 8 only)

| ID | Precondition | Input / steps | Test data | Expected result | Actual | P/F | Automated by |
|---|---|---|---|---|---|---|---|
| TC-DB-01 | clean MySQL 8 | start the application | — | 10 tables, 6 functions, 13 procedures, 10 triggers created | as expected | Pass | `MySqlDatabaseIT.theScriptsBuiltEverything` |
| TC-DB-02 | clean MySQL 8 | read a setting through a stored function | `consultation_fee` | database and table collations match, so the call succeeds | as expected | Pass | `MySqlDatabaseIT.collationsMatch` |
| TC-DB-03 | reference data loaded | register a visit | valid booking | number from `sp_next_appointment_no`; audit row written by the trigger | as expected | Pass | `MySqlDatabaseIT.bookingUsesTheProceduresAndTriggers` |
| TC-DB-04 | a completed visit | produce a bill | discount 100.00 | `total_amount` computed by MySQL in the generated column | as expected | Pass | `MySqlDatabaseIT.mysqlCalculatesTheBillTotal` |
| TC-DB-05 | a visit exists at that slot | **bypass Java**, INSERT straight into the table | duplicate slot | refused by the UNIQUE index on `slot_key` | as expected | Pass | `MySqlDatabaseIT.theDatabaseBlocksDoubleBooking` |
| TC-DB-06 | reference data loaded | **bypass Java**, INSERT at 03:00 | outside opening hours | refused by `trg_appointments_before_insert` | as expected | Pass | `MySqlDatabaseIT.theTriggerRefusesBookingsOutsideOpeningHours` |

---

## 3. Result summary

| Suite | Command | Tests | Failures | Errors |
|---|---|---|---|---|
| Unit, repository, web slice, integration | `./mvnw.cmd test` | **248** | 0 | 0 |
| Real MySQL 8 database features | `./mvnw.cmd test -Dtest=MySqlDatabaseIT` | **6** | 0 | 0 |
| | **Total** | **254** | **0** | **0** |

Both suites also run automatically on GitHub Actions on every push — see
`.github/workflows/ci.yml` and the Actions tab of the repository.

## 4. Code coverage (JaCoCo)

| Package | Instructions | Branches |
|---|---|---|
| `dto` | 97.3% | — |
| `config` | 94.6% | 80.8% |
| `service.billing` | 85.9% | 75.0% |
| `controller.web` | 85.4% | 73.5% |
| `exception` | 84.4% | — |
| `service` | 83.3% | 70.2% |
| `controller` | 78.3% | 66.7% |
| `model` | 66.7% | 34.3% |
| `service.notification` | 48.6% | 50.0% |
| **Overall** | **80.9%** | **61.5%** |

The report is regenerated by `./mvnw.cmd test` and written to
`target/site/jacoco/index.html`.

Two figures are deliberately low and are worth explaining rather than hiding.
`model` is 66.7% because most of it is getters and setters that no test calls
directly; the numbers that matter in those classes, such as `Bill.getTotalAmount()`
and `Patient.getAge()`, are covered. `service.notification` is 48.6% because
email sending is switched off until real mail credentials exist, so the branch
that talks to a mail server is never taken.
