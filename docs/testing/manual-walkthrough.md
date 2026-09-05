# End-to-end walkthrough — Task C

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

The 268 automated tests prove the parts. This walkthrough proves the **whole
thing**, against a real MySQL 8 and a real running server, in the order a
receptionist would actually do it.

It matters because three real bugs got past a fully green test suite and were
only found this way. A suite that only talks to itself will confirm its own
assumptions.

- **Script:** [`walkthrough.sh`](walkthrough.sh) — runs the whole thing with
  `curl`, exactly as a browser would, carrying cookies and the CSRF token
- **Captured output:** [`manual-walkthrough-output.txt`](manual-walkthrough-output.txt)
- **Run on:** 2 September 2026, against MySQL 8.4.9 and the application on
  `localhost:8080`

---

## How to run it yourself

```powershell
D:\DevTools\start-mysql.cmd                     # MySQL is not a service on this machine
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic"
.\mvnw.cmd spring-boot:run                      # in one terminal
bash docs\testing\walkthrough.sh                # in another
```

The script clears nothing, so run it against a fresh database if you want the
reference numbers to come out as `…-0001`.

---

## What it checks, and what came back

| Step | Checking | Result |
|---|---|---|
| 1 | web service refuses an unauthenticated caller | `401` + JSON message |
| 2 | a browser page redirects instead | `302 → /login` |
| 3 | sign in as `reception` | `302 → /`, fresh CSRF token issued |
| 4 | the two dropdown lists load | 4 dentists, 12 treatments |
| 5 | **FR2** register an appointment | `201`, `APT-20260915-0001`, patient `PAT-000001` created |
| 6 | validation refuses a bad form | `400`, both fields named |
| 7 | **double booking refused** | `409` "That dentist is already booked…" |
| 8 | **FR3** look the visit up by number | `200`, full patient and visit details |
| 9 | billing refused before the visit happened | `422` "has not taken place yet" |
| 10 | mark the visit completed | `200`, status `COMPLETED` |
| 11 | **FR4** produce the bill | `201`, **Rs. 29,750.00** |
| 12 | one visit, one bill | `422` "has already been billed" |
| 13 | record the payment | `200`, `PAID` by `CASH` |
| 14 | **FR7** treatment history | the visit, newest first |
| 15 | day schedule report | the diary for 15 Sep |
| 16 | receptionist refused the admin area | `403` |
| 17 | write with no CSRF token | `403` |
| 18 | **FR6** sign out | `302 → /login?logout`, then `401` |

### The bill proves the Strategy pattern is real

A root canal is **Rs. 25,000.00** on the price list, and the surgical rule adds
a 15% sterilisation supplement:

```
treatment    : Rs. 28,750.00      25,000 + 15%
consultation : Rs.  1,500.00
discount     : Rs.    500.00      agreed with the patient
TOTAL        : Rs. 29,750.00
```

Nothing in that calculation was typed in by the screen. The screen sent an
appointment number and a discount; every other figure came from the price list
through the pricing rule the factory chose.

---

## What the database did by itself

The second half of the captured output reads the database directly, to show the
work that no Java code performed.

**Audit rows, written by triggers**

```
appointment_no       action     old_status  new_status  changed_by
APT-20260915-0001    CREATED    NULL        BOOKED      root@localhost
APT-20260915-0001    COMPLETED  BOOKED      COMPLETED   root@localhost
```

No Java code writes to `appointment_audit`. `trg_appointments_after_insert` and
`trg_appointments_after_update` did this, which is why the record cannot be
quietly edited by the application.

**Reference numbers, from stored procedures and counter tables**

```
APPOINTMENT  2026-09-15  1        ->  APT-20260915-0001
BILL         2026-09-02  1        ->  BIL-20260902-0001
PATIENT                  1        ->  PAT-000001
```

Not from counting rows. `sp_next_appointment_no` locks a counter row, so two
receptionists saving in the same instant get different numbers.

**The bill total, from a generated column**

```
bill_no             treatment_cost  consultation_fee  discount  total_amount
BIL-20260902-0001   28750.00        1500.00           500.00    29750.00
```

Java writes the first three. MySQL computes `total_amount` itself, and the JPA
mapping is `insertable = false, updatable = false`, so the total cannot drift
out of step with the numbers it is made from.

**Sign-in times, from the event listener**

```
username    role          last_login_at
admin       ADMIN         2026-09-02 01:04:52
reception   RECEPTIONIST  2026-09-02 10:15:50
```

---

## Checks that only a person can make

These are in this document rather than in the automated suite, and it is worth
being open about why.

| Check | How it was verified | Result |
|---|---|---|
| The printed bill has no menu or buttons on it | opened `/bills/{billNo}`, pressed **Print this bill**, looked at the preview | correct — the print stylesheet removes them |
| The day schedule prints as a clean list | opened `/appointments/schedule`, print preview | correct |
| Bad fields turn red with the message under the box | submitted a bad booking form in Chrome | correct |
| The screens are readable and the navigation is the same everywhere | opened all 13 screens as both roles | correct |

Automated visual testing was judged out of scope for a project this size. That
is a decision, not an oversight, and the report says so.

---

## Screenshots to take for the report

The report needs screenshots. These are the ones worth taking, and where.

| # | Screenshot | Where to get it |
|---|---|---|
| 1 | **All tests passing** | run `.\mvnw.cmd test`, capture the `Tests run: 248, Failures: 0, Errors: 0` summary and `BUILD SUCCESS` |
| 2 | **MySQL integration tests passing** | run `.\mvnw.cmd test -Dtest=MySqlDatabaseIT`, capture `Tests run: 6, Failures: 0` |
| 3 | **Coverage report** | open `target\site\jacoco\index.html` in a browser |
| 4 | **Tests in the IDE** | in VS Code open the Testing panel and run all — the green ticks per class read well in a report |
| 5 | **CI passing on GitHub** | the Actions tab, showing the three green jobs |
| 6 | **The published release** | the Releases page, showing `v0.9.0` and the attached jar |
| 7 | **Red commit** | `git show 2d3904e --stat` — only test files changed |
| 8–13 | **The screens** | sign in, menu, booking form, a rejected form, the visit details, the printed bill |
| 14 | **A report screen** | sign in as `admin`, open Reports → Revenue by treatment |
| 15 | **The database** | in MySQL Workbench, `SHOW TRIGGERS;` and the `appointment_audit` table |

Numbers 1, 2, 3, 5 and 6 are the evidence Task C asks for directly. Number 7 is
the TDD evidence. Number 15 is the advanced-database-features evidence.
