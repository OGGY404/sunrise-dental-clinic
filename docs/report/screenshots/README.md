# Screenshots for the report

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

**27 screenshots, all captured automatically** on 2 September 2026. Nothing is
left to do by hand.

- **1–23 and 27** are browser screens, taken by
  [`../capture-screenshots.mjs`](../capture-screenshots.mjs) driving the copy of
  Chrome already installed on this machine, at 2880px so they stay sharp when
  scaled into Word.
- **24–26** are real console windows, captured by
  [`../capture-terminals.ps1`](../capture-terminals.ps1). Each command is run in
  a maximised console and the screen is captured — these are genuine screen
  captures of genuine output, not renderings.

Nothing is staged: the clinic data behind the screens was created through the
real web service by [`../seed-demo-data.sh`](../seed-demo-data.sh).

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
| `20-jacoco-coverage.png` | the coverage report, 78.8% | Task C |
| `21-github-actions.png` | CI workflows, all green | Task D |
| `22-github-release.png` | release v0.9.0 with the runnable jar | Task D, deployment |
| `23-github-commit-history.png` | the commit history | Task D |
| `24-tests-passing.png` | **`Tests run: 262, Failures: 0, Errors: 0`** and `BUILD SUCCESS` | Task C |
| `25-tdd-red-commit.png` | the red commit: **7 files, all tests, nothing in `src/main`** | Task C, TDD |
| `26-database-triggers-audit.png` | all 10 triggers, the audit rows they wrote, and the generated column | Task B, advanced DB |
| `27-appointment-reminders.png` | tomorrow's patients from a **stored procedure**, split into who can be emailed and who must be telephoned | FR7 reminders |

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
| 9 | `26-database-triggers-audit.png` |
| 10 | `04-booking-form-rejected.png` |
| 11 | `02-dashboard-menu.png` |
| 12 | `05-appointment-details-booked.png` |
| 13 | `12-bill-print-preview.png` |
| 14 | `25-tdd-red-commit.png` |
| 15 | `24-tests-passing.png` |
| 16 | `20-jacoco-coverage.png` |
| 17 | `21-github-actions.png` |
| 18 | `23-github-commit-history.png` |
| 19 | `21-github-actions.png` *(same image, or open one run for detail)* |
| 20 | `22-github-release.png` |

Everything else — `01`, `03`, `06`–`11`, `13`–`19` and `27` — is spare, for the
appendix. `27-appointment-reminders.png` is worth using if you write about FR7.

---

## Nothing is left to take by hand

The three terminal figures used to need a manual **Win + Shift + S**. They are
now captured by [`../capture-terminals.ps1`](../capture-terminals.ps1), which
runs each command in a maximised console and captures the screen.

Two things were worth solving along the way, and are worth knowing if you ever
script this again:

1. **The window handle is useless on Windows 11.** The console is hosted by
   Windows Terminal, so the `powershell.exe` that was started owns no window
   and `MainWindowHandle` is `0`. Opening the console maximised and capturing
   the whole screen sidesteps it entirely.
2. **`git show` opens a pager and waits forever.** It needs `--no-pager`, and a
   short `--format` as well, or the long commit message pushes the file list —
   the whole point of the picture — off the bottom of the screen.

## Taking them all again

If you change a screen and need fresh images:

```powershell
D:\DevTools\start-mysql.cmd
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic"
.\mvnw.cmd spring-boot:run                        # leave running in one terminal

bash docs\report\seed-demo-data.sh                # optional, if the data is gone

cd D:\DevTools\shots
node capture.mjs                                  # rewrites 1-23 and 27, the browser screens
powershell -ExecutionPolicy Bypass -File capture-terminals.ps1    # rewrites 24-26
```

The browser script lives in `D:\DevTools\shots` because that is where its
`puppeteer-core` dependency is installed, which keeps `node_modules` out of this
repository. Copies of both scripts are kept here, as
[`../capture-screenshots.mjs`](../capture-screenshots.mjs) and
[`../capture-terminals.ps1`](../capture-terminals.ps1).

While `capture-terminals.ps1` is running it opens maximised console windows and
captures the whole screen, so leave the machine alone for the minute or so it
takes.
