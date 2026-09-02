# Screenshots for the report

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

**23 screenshots, captured automatically** from the running application on
2 September 2026, at 2880 × high resolution (Retina scale), so they stay sharp
when scaled down into a Word document.

They were taken by [`../capture-screenshots.mjs`](../capture-screenshots.mjs)
driving the copy of Chrome already installed on this machine. Nothing was
staged: the clinic data behind them was created through the real web service by
[`../seed-demo-data.sh`](../seed-demo-data.sh).

---

## What each one shows

| File | Shows | Use for |
|---|---|---|
| `01-login.png` | the sign-in screen | FR1 |
| `02-dashboard-menu.png` | main menu, today's diary, money owed | menu-driven system |
| `03-booking-form.png` | the empty booking form | FR2, data entry screen |
| `04-booking-form-rejected.png` | **five fields rejected**, values kept, messages under each box | FR2, validation |
| `05-appointment-details-booked.png` | a BOOKED visit with complete / cancel / move | FR3, FR7 |
| `06-appointment-details-completed.png` | a COMPLETED visit offering "Produce the bill" | FR3, state machine |
| `07-find-appointment.png` | search by appointment number | FR3, data entry screen |
| `08-day-schedule.png` | the whole clinic diary for a day | reports |
| `09-patient-search.png` | patient search results | FR7 |
| `10-patient-treatment-history.png` | one patient, every visit, newest first | FR7 |
| `11-bill-receipt.png` | the receipt on screen, with the payment box | FR4 |
| `12-bill-print-preview.png` | **exactly what prints** — no menu, no buttons, no background | FR4, print |
| `13-unpaid-bills.png` | the chase list, oldest first | reports |
| `14-help-section.png` | step-by-step instructions for new staff | FR5 |
| `15-error-page.png` | a friendly error page, no stack trace | error handling |
| `16-receptionist-refused-reports.png` | a receptionist refused the reports | role-based access |
| `17-reports-menu.png` | the reports menu, as an administrator | reports |
| `18-revenue-by-treatment.png` | revenue report, from a **stored procedure** | advanced DB features |
| `19-dentist-workload.png` | workload report, from a **stored procedure** | advanced DB features |
| `20-jacoco-coverage.png` | the coverage report, 80.9% | Task C |
| `21-github-actions.png` | CI workflows, all green | Task D |
| `22-github-release.png` | release v0.9.0 with the runnable jar | Task D, deployment |
| `23-github-commit-history.png` | the commit history | Task D |

### Two worth pointing at in the writing

**`04-booking-form-rejected.png`** — five red boxes, each with its own message
underneath, and everything the receptionist typed still in place. That single
image evidences the validation requirement, the "friendly messages"
requirement, and the design decision that a refused form comes back rather than
redirecting.

**`12-bill-print-preview.png`** — this is a real print-media render, not the
screen with the menu cropped off. It shows the print stylesheet doing its job:
`6,000 + 1,500 − 500 = Rs. 7,000.00`, and nothing else on the paper.

---

## Report figure numbers

Following the figure list in [`../report-skeleton.md`](../report-skeleton.md):

| Fig | File |
|---|---|
| 1 | `../../uml/png/01-use-case.png` |
| 2 | `../../uml/png/02-class-domain.png` |
| 3 | `../../uml/png/03-class-layers.png` |
| 4 | `../../uml/png/05-sequence-login.png` |
| 5 | `../../uml/png/06-sequence-register-appointment.png` |
| 6 | `../../uml/png/07-sequence-generate-bill.png` |
| 7 | `../../uml/png/08-sequence-cancel-appointment.png` *(appendix)* |
| 8 | `../../uml/png/04-class-design-patterns.png` |
| 9 | **take by hand** — the database (see below) |
| 10 | `04-booking-form-rejected.png` |
| 11 | `02-dashboard-menu.png` |
| 12 | `05-appointment-details-booked.png` |
| 13 | `12-bill-print-preview.png` |
| 14 | **take by hand** — the red commit (see below) |
| 15 | **take by hand** — tests passing (see below) |
| 16 | `20-jacoco-coverage.png` |
| 17 | `21-github-actions.png` |
| 18 | `23-github-commit-history.png` |
| 19 | `21-github-actions.png` *(same image, or open one run for detail)* |
| 20 | `22-github-release.png` |

Everything else — `01`, `03`, `06`–`11`, `13`–`19` — is spare, for the
appendix.

---

## The three left to take by hand

These are terminal windows, so they cannot be captured by a browser. Each takes
about ten seconds: run the command, then press **Win + Shift + S**, drag over
the window, and paste into Word.

**Figure 15 — the tests passing**

```powershell
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic"
.\mvnw.cmd test
```

Capture the end: `Tests run: 248, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

**Figure 14 — the TDD red commit**

```powershell
git show 2d3904e --stat
```

Capture the file list. Every file is a test file and there is nothing in
`src/main` — that is the whole point.

**Figure 9 — the database doing the work**

```powershell
D:\DevTools\mysql-8.4.9-winx64\bin\mysql.exe -u root --protocol=TCP -h 127.0.0.1 -P 3306 -D sunrise_dental -e "SHOW TRIGGERS\G" | more
```

Or, better for the report, the audit trail the triggers wrote by themselves:

```powershell
D:\DevTools\mysql-8.4.9-winx64\bin\mysql.exe -u root --protocol=TCP -h 127.0.0.1 -P 3306 -D sunrise_dental -e "SELECT appointment_no, action, old_status, new_status, changed_at FROM appointment_audit ORDER BY audit_id;"
```

---

## Taking them all again

If you change a screen and need fresh images:

```powershell
D:\DevTools\start-mysql.cmd
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic"
.\mvnw.cmd spring-boot:run                        # leave running in one terminal

bash docs\report\seed-demo-data.sh                # optional, if the data is gone
cd D:\DevTools\shots ; node capture.mjs           # rewrites all 23
```

The script lives in `D:\DevTools\shots` because that is where its
`puppeteer-core` dependency is installed, which keeps `node_modules` out of
this repository. A copy of the script itself is kept here as
[`../capture-screenshots.mjs`](../capture-screenshots.mjs).
