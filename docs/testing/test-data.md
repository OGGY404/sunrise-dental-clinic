# Test data — Task C

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

The brief asks for **devised and derived test data, including boundary and
invalid cases**. This is where that data came from and why each value was
chosen.

- **Derived** data comes from the specification: the column widths in
  `schema.sql`, the opening hours in `clinic_settings`, and the rules in the
  brief. Every partition boundary is derived, not guessed.
- **Devised** data is invented to probe a specific risk that the specification
  does not mention — a Sri Lankan name containing full stops, a discount larger
  than the bill, an insert that goes straight to the table and skips the Java.

Two techniques are used throughout: **equivalence partitioning** (one value
from each class of input that should behave the same way) and **boundary value
analysis** (the last value that is accepted and the first that is not).

---

## 1. Patient name — `BookingRequest.fullName`

Rule derived from `schema.sql`: `VARCHAR(100) NOT NULL`, and from the domain: a
person's name contains no digits.

| Partition | Value used | Expected | Type | Test |
|---|---|---|---|---|
| valid, ordinary | `Kamal Silva` | accept | derived | `acceptsAValidForm` |
| valid, real Sri Lankan form | `W.A.G.K. Rathnayake-Silva` | accept | **devised** | `nameAllowsDotsAndHyphens` |
| blank | `"   "` | reject | derived | `nameIsRequired` |
| contains digits | `Kamal 123` | reject | devised | `nameRejectsDigits` |
| **boundary: at the limit** | 100 × `A` | accept | derived | covered by the pattern in `nameHasAMaximumLength` |
| **boundary: one over** | 101 × `A` | reject | derived | `nameHasAMaximumLength` |

Why the second row was devised: an early version of the pattern allowed letters
and spaces only. It would have rejected the name of the student writing this
project. Names in Sri Lanka routinely carry initials with full stops and
hyphenated surnames, so the rule had to be widened — and then pinned down by a
test, so nobody narrows it again.

## 2. Contact number — `BookingRequest.contactNumber`

Rule derived from `schema.sql`: `VARCHAR(15) NOT NULL`, and from the domain: a
Sri Lankan number is `0` + 9 digits, or `+94` + 9 digits.

| Partition | Value used | Expected | Type | Test |
|---|---|---|---|---|
| valid, mobile | `0771234567` | accept | derived | `acceptsLocalNumber` |
| valid, landline | `0112345678` | accept | derived | `acceptsLocalNumber` |
| valid, country code | `+94771234567` | accept | derived | `acceptsInternationalNumber` |
| **boundary: one digit short** | `077123456` (9) | reject | derived | `rejectsTooShort` |
| letters | `077ABC4567` | reject | devised | `rejectsLetters` |
| missing | `null` | reject | derived | `contactIsRequired` |

## 3. Appointment date — `BookingRequest.appointmentDate`

Rule from the brief: an appointment cannot be booked in the past.

| Partition | Value used | Expected | Type | Test |
|---|---|---|---|---|
| **boundary: yesterday** | `today − 1 day` | reject | derived | `dateCannotBeInThePast` |
| **boundary: today** | `today` | **accept** | derived | `todayIsAllowed` |
| future | `today + 7 days` | accept | derived | `acceptsAValidForm` |
| missing | `null` | reject | derived | `dentistIsRequired` group |

Today is the interesting boundary. It would be easy to write `@Future` and
refuse it, and that would be wrong: a patient can walk in with toothache and be
seen this afternoon. The test exists so that mistake cannot be made later.

## 4. Appointment time — clinic opening hours

Derived from `clinic_settings`: open `08:00`, close `18:00`, slots every 30
minutes.

| Partition | Value used | Expected | Type | Test |
|---|---|---|---|---|
| inside opening hours | `09:00` | accept | derived | `registersAnAppointment` |
| before opening | `07:00` | reject | derived | `refusesATimeOutsideOpeningHours` |
| off the half-hour grid | `09:17` | reject | derived | `refusesATimeOffTheSlotGrid` |
| straight to the table, 03:00 | `03:00` | reject **by the trigger** | devised | `MySqlDatabaseIT.theTriggerRefusesBookingsOutsideOpeningHours` |

The last row is devised, and it is the one that matters most. It writes
directly to the table with `JdbcTemplate`, skipping every Java check, to prove
the rule is really in the database and not only in the application.

## 5. Loyalty discount — visit count

Rule from the clinic: the discount begins on the **fifth** completed visit.

| Partition | Value used | Expected | Type | Test |
|---|---|---|---|---|
| **boundary: fourth visit** | `completedVisits = 4` | no discount | derived | `noDiscountOnTheFourthVisit` |
| **boundary: fifth visit** | `completedVisits = 5` | 10% off treatment cost | derived | `appliesLoyaltyDiscount` |
| first visit | `completedVisits = 1` | no discount | derived | `chargesTreatmentPlusConsultation` |

## 6. Money — discounts and totals

| Partition | Value used | Expected | Type | Test |
|---|---|---|---|---|
| no discount | `null` | total unchanged | derived | `billsAnOrdinaryTreatment` |
| ordinary discount | `500.00` | subtracted | derived | `addsAManualDiscount` |
| **boundary: zero** | `0.00` | treated as none | derived | `addsAManualDiscount` group |
| negative | `−1.00` | reject — a hidden price rise | devised | `refusesANegativeDiscount` |
| larger than the bill | above the total | reject | devised | `refusesADiscountBiggerThanTheBill` |
| produces fractions of a cent | odd percentages | round to 2 dp | devised | `roundsDiscountToTwoDecimalPlaces` |
| would make the total negative | very large | floor at zero | devised | `neverGoesBelowZero` |

Real prices are used throughout, taken from `data.sql`: a filling at
Rs. 6,000.00, a root canal at Rs. 25,000.00 and a consultation fee of
Rs. 1,500.00. That way an expected figure such as **28,750.00** (25,000 + 15%
sterilisation supplement) can be checked by hand against the price list.

## 7. Appointment status — the state machine

The status decides what may happen next, so every transition was tested,
including the ones that must be refused.

| From | Action | Expected | Test |
|---|---|---|---|
| BOOKED | complete | → COMPLETED | `marksAVisitCompleted` |
| BOOKED | no-show | → NO_SHOW | `AppointmentServiceTest` |
| BOOKED | cancel | → CANCELLED, slot freed | `cancelsABookedAppointment` |
| BOOKED | bill | **refuse** — it has not happened | `refusesToBillABookedVisit` |
| COMPLETED | bill | bill produced | `billsAnOrdinaryTreatment` |
| COMPLETED | bill again | **refuse** — one visit, one bill | `refusesToBillTwice` |
| COMPLETED | cancel | **refuse** — it already happened | `refusesToCancelACompletedVisit` |
| CANCELLED | cancel | **refuse** — already cancelled | `refusesToCancelTwice` |
| CANCELLED | complete | **refuse** | `refusesToCompleteACancelledAppointment` |
| CANCELLED | bill | **refuse** | `refusesToBillACancelledVisit` |
| PAID bill | pay again | **refuse** | `refusesToPayTwice` |

## 8. Security data

| Case | Value used | Expected | Type | Test |
|---|---|---|---|---|
| correct password | `reception` / `Recep@123` | signed in | derived | `correctPasswordLogsIn` |
| wrong password | `reception` / `wrong` | refused | derived | `wrongPasswordIsRefused` |
| unknown username | `nobody` / `Recep@123` | refused **identically** | devised | `unknownUsernameIsRefused` |
| disabled account | `oldstaff`, `enabled = false` | refused | devised | `disabledAccountIsRefused` |
| write with no CSRF token | valid session, header omitted | 403 | devised | `writeWithoutTokenIsStillRefused` |
| write with the cookie's token | full browser flow | allowed through | devised | `tokenFromCookieLetsAWriteThrough` |

Rows 3 and 4 are devised from a security concern rather than from the brief. If
a wrong username and a wrong password produced different answers, somebody
guessing could first learn which usernames are real and then attack only those.

## 9. Data that bypasses the application entirely

These are the most valuable devised cases in the whole suite, because they are
the only ones that can prove a rule really lives in the database.

| Case | How | Expected | Test |
|---|---|---|---|
| duplicate slot | `INSERT` straight into `appointments` | refused by `UNIQUE (slot_key)` | `theDatabaseBlocksDoubleBooking` |
| booking at 03:00 | `INSERT` straight into `appointments` | refused by `trg_appointments_before_insert` | `theTriggerRefusesBookingsOutsideOpeningHours` |
| bill total | insert three amounts, read the fourth back | MySQL computes `total_amount` itself | `mysqlCalculatesTheBillTotal` |
