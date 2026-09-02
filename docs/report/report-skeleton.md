# Report skeleton — Task A to D

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic
Student **st20360306** · Deadline **5 September 2026, 2:00 pm**

This is the **structure** for the report, not the report. Every heading below
says what has to be in it, which evidence to put there, and the questions to
answer — but the analysis, the justifications and the reflection have to be
written by you, in your own words. That is what carries the marks, and Cardiff
Met's unfair practice rules cover generated text.

---

## The single most important rule to understand first

> Word count: **4000**, *including source code, text, tables, figures, captions
> and citations*, and **excluding references and appendices**.

Read that twice. A screenshot with a caption costs you words. A page of pasted
Java costs you a lot of words. **The appendices do not.**

So the strategy is:

| Put in the main body | Move to an appendix |
|---|---|
| your analysis and justification | full code listings |
| the diagrams you actually discuss | the full 111-row test plan |
| small extracted tables (5–8 rows) | the full traceability matrix |
| one screenshot per point being made | the extra screenshots |
| short code fragments (3–6 lines) that you then discuss | anything you are not going to discuss |

**Never paste anything you do not then write about.** Unexplained evidence
earns nothing and costs you words you need elsewhere.

---

## Word budget

The marks are 20 / 40 / 20 / 20, so the words should be roughly the same shape.

| Section | Marks | Target words | Running total |
|---|---|---|---|
| Title page, contents, introduction | — | 250 | 250 |
| **Task A — System design with UML** | 20 | 800 | 1050 |
| **Task B — Development** | 40 | 1500 | 2550 |
| **Task C — Testing** | 20 | 800 | 3350 |
| **Task D — Git / GitHub** | 20 | 550 | 3900 |
| Conclusion | — | 100 | 4000 |

Keep a running count as you write. If you are over, the first thing to cut is
any figure you are not discussing — not your analysis.

---

## Formatting (from the brief — check every one before exporting)

- [ ] A4 paper
- [ ] Margins: **1.5 inch left**, 1 inch right, top and bottom
- [ ] Page numbers **bottom right**
- [ ] Line spacing **1.5**
- [ ] Font **Times New Roman**, body **12 pt**
- [ ] Headings **14 pt bold**
- [ ] **Harvard** referencing throughout
- [ ] Export to **PDF**
- [ ] File named **`st20360306 CIS6003 WRIT1`**
- [ ] Submitted via Turnitin on Moodle before **2:00 pm on 5 September 2026**

---

# Section 0 — Front matter (~250 words)

**0.1 Title page** — title, module code, your name, student ID, date.
Not counted heavily, keep it plain.

**0.2 Table of contents** — Word will generate this from your headings. Use real
Heading 1 / Heading 2 styles so it works.

**0.3 Introduction (~200 words)**

Say in a short paragraph each:
- what Sunrise Dental Clinic asked for, and what was wrong with the paper system
  (double bookings, lost records, billing errors — these are in the scenario)
- what you built, in two sentences
- the repository link: `https://github.com/OGGY404/sunrise-dental-clinic`
- how the report is organised

---

# Section 1 — Task A: System design with UML (~800 words, 20 marks)

## 1.1 Use case model (~150 words + Figure 1)

**Insert:** Figure 1 — `docs/uml/png/01-use-case.png`

**Write about:**
- who the actors are and why. Say why **dentists are not actors** (they read the
  printed schedule; they do not sign in) and why the **email service is a
  supporting actor** (the system talks to it; it never starts anything).
- why Administrator is drawn as a **generalisation** of Receptionist.
- pick **one** `<<include>>` and **one** `<<extend>>` and explain the difference
  in your own words. The clearest pair:
  - *Register appointment* **includes** *Find or create patient record* — it
    happens every single time.
  - *Suggest another time* **extends** *Register appointment* — only when the
    chosen slot has just gone.

> The marker is specifically checking that include and extend are used
> **accurately**. One sentence proving you know the difference is worth more
> than listing every use case.

## 1.2 Class model (~250 words + Figures 2 and 3)

**Insert:** Figure 2 — `docs/uml/png/02-class-domain.png`
**Insert:** Figure 3 — `docs/uml/png/03-class-layers.png`

**Write about:**
- **Composition:** Patient owns its Appointments. Argue *why* — a visit is not a
  thing in its own right, it is *this patient's* visit. Say what would be lost
  if it were only an association.
- **Aggregation:** Dentist and Treatment. Argue *why they differ* from the above
  — a dentist who leaves is marked inactive, not deleted, and their past
  appointments stay in the record, so the parts outlive the whole.
- **Multiplicity:** point at one, e.g. Appointment 1 ↔ 0..1 Bill, and say what
  the `0..1` is protecting (a visit that has not been billed yet).
- **Navigability:** Appointment knows its User; User has no list of
  appointments. Say why that is a decision and not an omission.
- **Access modifiers:** every field is private with public accessors. One
  sentence on encapsulation is enough.

## 1.3 Sequence models (~200 words + Figures 4–6)

**Insert:** Figure 4 — `05-sequence-login.png`
**Insert:** Figure 5 — `06-sequence-register-appointment.png`
**Insert:** Figure 6 — `07-sequence-generate-bill.png`

(Figure 7, `08-sequence-cancel-appointment.png`, can go in the appendix.)

**Write about:** do **not** narrate the arrows — the marker can read them. Pick
two or three things the diagrams reveal:
- the three tiers are visible as vertical bands, and messages only ever go
  downwards and come back
- the `alt` fragments show that the failure paths were designed, not
  discovered later
- in Figure 5, the note "Nothing has been written until this line" — explain why
  the order of the checks matters (a rejected booking must not leave a
  half-created patient behind)

## 1.4 Assumptions (~100 words)

**Insert:** a small table of your **6–8 most important** assumptions, extracted
from `docs/uml/README.md`. Put the full list of 17 in an appendix.

Choose ones you can defend. Suggested strongest six: patients never sign in;
one dentist sees one patient at a time; a returning patient is recognised by
name **and** telephone; one visit, one bill; prices are copied onto the bill
when it is issued; cancelling frees the slot but a no-show does not.

## 1.5 Critical evaluation of the design (~100 words) — **YOUR OWN WORDS**

This is the part the 20 marks actually turn on. Answer:

1. **Do the three diagram types support each other?** Follow *Register
   appointment* through Figure 1 → Figures 2 and 3 → Figure 5. Where do they
   agree, and does anything disagree?
2. **Was one decision genuinely difficult?** The honest one: the same rules are
   enforced **twice**, in Java and again in SQL. State the benefit (the database
   cannot be beaten by two people saving in the same instant) and the cost (the
   rule is written twice and could drift apart). Then say whether you would do
   it again.
3. **What is weak?** A long treatment does not yet block the following slot.
   That was found while modelling. Say so — admitting a real limitation reads
   far better than claiming there are none.

---

# Section 2 — Task B: Development (~1500 words, 40 marks)

This is the biggest section. Work through the brief's own list.

## 2.1 Architecture — three tiers (~200 words + Figure 3 referenced again)

**Write about:** the packages `controller` / `service` / `repository`, and the
one rule that makes it real: **dependencies only ever point downwards**.

> The strongest single sentence you can write here: *262 tests run with no web
> server and no database, and that is only possible because the tiers are
> genuinely separate.* The test suite is your evidence for the architecture.

## 2.2 Distributed application with web services (~150 words + small table)

**Insert:** a trimmed table of 6–8 endpoints (not all 16 — full list to an
appendix). Take it from the README.

**Write about:** why there are **two** controller families (screens for staff,
web services for other programs) and why both call the same services, so a rule
cannot be true on one and false on the other.

## 2.3 Design patterns (~400 words + Figure 8) — **the biggest single block**

**Insert:** Figure 8 — `docs/uml/png/04-class-design-patterns.png`

**Insert:** one short code fragment only — the `final calculate()` in
`AbstractBillingStrategy` is the best choice, about 8 lines.

**Write about each of the six**, in this shape: *what it is · why it fits here ·
what it improved · **what it cost***.

| Pattern | Where | The cost to state honestly |
|---|---|---|
| Singleton | `ClinicConfiguration` | cached settings go stale if a manager edits MySQL directly |
| Strategy | the three billing rules | the arithmetic is no longer visible in `BillingService` |
| Template Method | `AbstractBillingStrategy` | one more layer of inheritance to follow |
| Factory | `BillingStrategyFactory` | treatment categories are hardcoded in a Set |
| Builder | `Appointment.Builder` | ~40 extra lines, a second place to update |
| Observer | `AppointmentObserver` | reading `register()` does not reveal an email is sent |

> The brief asks for **critical evaluation, not identification**. Naming six
> patterns is worth little; the "what it cost" column is where the marks are.
> Also make the point that the three billing rules **genuinely differ** — that
> is what makes Strategy honest here rather than decorative.

## 2.4 Database and advanced features (~300 words + small table + Figure 9)

**Insert:** Figure 9 — a screenshot of MySQL Workbench showing `SHOW TRIGGERS;`
or the `appointment_audit` table with real rows.

**Write about:**
- 10 tables, 6 stored functions, 13 stored procedures, 10 triggers
- **why the database hands out reference numbers** rather than Java counting
  rows: two receptionists saving in the same instant would both count the same
  total. A locked counter row cannot be beaten. This is your strongest single
  technical argument in the whole report.
- the **generated column** `total_amount` — MySQL computes it, Java is not
  allowed to write it, so the total cannot drift from its parts
- the **`slot_key` generated column + UNIQUE index** — NULL when cancelled, and
  SQL allows many NULLs in a unique index, which is what frees the slot without
  deleting anything
- the two **report stored procedures**, and why grouping and totalling belongs
  in the database

## 2.5 Validation, sessions and security (~250 words + Figure 10)

**Insert:** Figure 10 — screenshot of the booking form with two fields rejected
(red boxes, messages underneath).

**Write about:**
- Bean Validation on the DTOs, with limits that match the column widths
- **sessions and cookies** (assessed): `SUNRISEID`, HttpOnly, SameSite=Lax,
  30-minute timeout, and a new session id issued at login (session fixation)
- **CSRF protection left on**, and why: keeping login state in a cookie is
  exactly what makes that attack possible
- BCrypt hashing, and identical refusals for a wrong username and a wrong
  password
- **role-based access** — a receptionist is refused `/reports`

## 2.6 The user interface (~200 words + Figures 11–13)

**Insert:** Figure 11 — the main menu · Figure 12 — the appointment details
screen · Figure 13 — the printed bill.

**Write about:**
- **separate screens for data entry and viewing results** (assessed) — and the
  practical reason: every save redirects, so pressing refresh cannot book a
  second appointment
- how a refused form behaves: same form, values kept, message under the right
  box, dropdowns reloaded
- the help section (FR5) and safe sign-out (FR6)

---

# Section 3 — Task C: Testing (~800 words, 20 marks)

Everything you need is in `docs/testing/`.

## 3.1 Rationale for the approach (~150 words) — **YOUR OWN WORDS**

**Insert:** the five-level table from `docs/testing/README.md`.

**Write about:** why the shape is the way it is — most tests at the levels that
run in milliseconds, few at the slow levels. Argue why 114 unit tests and only
6 database tests is a balance and not a shortcut.

## 3.2 How TDD was used (~200 words + Figure 14) — **YOUR OWN WORDS**

**Insert:** Figure 14 — screenshot of `git show 2d3904e --stat` showing that the
red commit touched **only test files**.

**Insert:** the six-row red→green table from `docs/testing/tdd-evidence.md`.

**Write about:** red → green → **refactor**. Most students forget refactor, so
name the real one: the three pricing rules were written separately and passing,
then `calculate()` was pulled up into an abstract parent and made `final` — and
**the tests did not change**, which is what made it safe.

Then give one concrete decision the red step changed:
*"today must be bookable"* — `@Future` would have been the obvious annotation
and would have been wrong, because a patient with toothache can walk in.

## 3.3 Test plan and test data (~150 words + extracted table)

**Insert:** an extracted table of about **8 representative cases** — not all 102.
Pick ones that show boundary analysis: name at 100 vs 101 characters, telephone
at 10 vs 9 digits, today vs yesterday, 4th vs 5th visit for the loyalty
discount.

**Full 111-row plan → Appendix.**

**Write about:** equivalence partitioning and boundary value analysis by name,
and the difference between **derived** data (from the column widths and the
brief) and **devised** data (invented to probe a risk, like a name containing
full stops, or an INSERT that bypasses Java entirely).

## 3.4 Test automation and results (~150 words + Figures 15–17)

**Insert:** Figure 15 — `mvnw test` showing `Tests run: 248, Failures: 0` and
`BUILD SUCCESS` · Figure 16 — the JaCoCo report at
`target/site/jacoco/index.html` · Figure 17 — GitHub Actions showing three green
jobs.

**Write about:** 268 tests, 32 classes, 0 failures, 78.8% instruction and 62.2%
branch coverage; and that everything runs automatically on every push.

## 3.5 Traceability matrix (~50 words + extracted table)

**Insert:** an extracted table of **FR1–FR6 only** (six rows). Full matrix →
Appendix.

**Write about:** one sentence — every requirement is traced from design to code
to tests, and the three items that are deliberately **not** automated are listed
with reasons.

## 3.6 Evaluation and lessons learned (~100 words) — **YOUR OWN WORDS**

This is the highest-value paragraph in Task C. The honest material:

**Three bugs reached the running application while the whole suite was passing.**
- `CHAR(60)` vs `VARCHAR` — H2 could not notice, because it builds its tables
  *from the entities*
- the CSRF cookie vanished at login — MockMvc could not notice, because it
  supplies its own token
- a collation mismatch stopped **every booking** — H2 has no stored functions

The lesson, in your own words: *a test suite that supplies both sides of the
exchange will confirm its own assumptions.* Then say what you changed (added
real-MySQL and real-HTTP levels) and what you would do differently next time
(start the real database earlier), and what that would have cost.

> Writing about tests that **failed to catch** something reads as far more
> mature than claiming everything passed first time. Do not hide this.

---

# Section 4 — Task D: Git / GitHub (~550 words, 20 marks)

## 4.1 Repository and branching strategy (~150 words + Figure 18)

**Repository:** `https://github.com/OGGY404/sunrise-dental-clinic`

**Insert:** Figure 18 — the GitHub network/branch graph, or the output of
`git log --graph --oneline --all | head -40`.

**Write about:** `main` holds tagged releases, `develop` is the integration
branch, and each step was a `feature/*` branch merged with `--no-ff` so the
history shows the shape of the work rather than a flat line.

## 4.2 Commits across multiple days (~100 words + small table)

**Insert:** this table — the figures are real, from `git log`:

| Date | Commits | What was done |
|---|---|---|
| 21 Aug 2026 | 6 | repository set up, Spring Boot skeleton, database schema, procedures and triggers |
| 28 Aug 2026 | 9 | entities and repositories, then the design patterns and the service layer |
| 1 Sep 2026 | 7 | REST web services and validation, then login, sessions and roles |
| 2 Sep 2026 | 12 | the screens and reports, CI/CD, UML, testing documentation |
| | **34 total** | across **four separate days** |

Regenerate it any time with:

```powershell
git log --format="%ad" --date=short | sort | uniq -c
```

The brief specifically asks for **several versions across multiple days**, so
say this explicitly rather than leaving the marker to count.

**Write about:** conventional commit messages (`feat:`, `fix:`, `test:`, `ci:`,
`docs:`) and why the `test:` commits come **before** their `feat:` commits.

## 4.3 CI/CD (~150 words + Figures 17 and 19)

**Insert:** Figure 19 — the Actions run detail, showing the three jobs.

**Write about:** the pipeline in `ci.yml` — fast tests, then the real MySQL job,
then package, and that the jar is only built if both test jobs passed. Say why
the MySQL job exists (it is the same argument as §3.6).

## 4.4 Release and deployment (~100 words + Figure 20)

**Insert:** Figure 20 — the Releases page showing `v0.9.0` with the attached
`dental-clinic-0.0.1-SNAPSHOT.jar`.

**Write about:** pushing a tag triggers `release.yml`, which builds from a clean
checkout, runs the tests **again** (a tag can be pushed from any machine), and
publishes the jar. Deployment is then `java -jar` plus the database environment
variables, because Spring Boot packages the server inside the jar.

## 4.5 `.gitignore` and what is not committed (~50 words)

**Write about:** `target/`, IDE folders and `.env` are ignored, so no credential
ever reaches GitHub. `.env.example` is committed as a template instead.

---

# Section 5 — Conclusion (~100 words)

Three or four sentences: what was delivered against FR1–FR7, the one thing you
are most pleased with, the one thing you would change, and what you learned that
you will carry into the next project.

---

# References

**Excluded from the word count**, so a proper list costs you nothing.

Cite **only what you actually read.** A reference list containing books you have
not opened is itself an academic offence, and markers do ask.

Genuine candidates, if you read them:
- Gamma, E., Helm, R., Johnson, R. and Vlissides, J. (1994) *Design Patterns*.
  — for Singleton, Strategy, Factory, Observer, Template Method
- Fowler, M. (2002) *Patterns of Enterprise Application Architecture*.
  — for Repository, and for the three-tier / service-layer argument
- Beck, K. (2002) *Test-Driven Development: By Example*.
  — for red–green–refactor
- The Spring Framework and Spring Security reference documentation (online, with
  access dates)
- The MySQL 8.4 reference manual (online) — for triggers, stored procedures and
  generated columns
- OWASP guidance on CSRF and on password storage

Harvard format, alphabetical by author, with access dates for anything online.

---

# Appendices

**Excluded from the word count.** Use them generously.

| Appendix | Contents | Source |
|---|---|---|
| A | Full source code listings of the key classes | `src/main/java/...` |
| B | Database scripts | `schema.sql`, `procedures.sql`, `triggers.sql` |
| C | Full test plan, all 102 cases | `docs/testing/test-plan.md` |
| D | Full traceability matrix | `docs/testing/traceability-matrix.md` |
| E | Full list of 17 assumptions | `docs/uml/README.md` |
| F | Remaining screenshots | your own |
| G | End-to-end walkthrough output | `docs/testing/manual-walkthrough-output.txt` |

---

# Figure list — every figure is already captured

**All done.** `docs/report/screenshots/` holds 26 screenshots, taken
automatically from the running application and from real console windows.
`docs/uml/png/` holds the 8 diagrams. Nothing is left to capture by hand. See
[`screenshots/README.md`](screenshots/README.md) for what each one shows.

| Fig | What | File | Status |
|---|---|---|---|
| 1 | Use case diagram | `docs/uml/png/01-use-case.png` | ready |
| 2 | Class diagram — domain | `docs/uml/png/02-class-domain.png` | ready |
| 3 | Class diagram — three tiers | `docs/uml/png/03-class-layers.png` | ready |
| 4 | Sequence — sign in | `docs/uml/png/05-sequence-login.png` | ready |
| 5 | Sequence — register | `docs/uml/png/06-sequence-register-appointment.png` | ready |
| 6 | Sequence — bill | `docs/uml/png/07-sequence-generate-bill.png` | ready |
| 7 | Sequence — cancel *(appendix)* | `docs/uml/png/08-sequence-cancel-appointment.png` | ready |
| 8 | Class diagram — design patterns | `docs/uml/png/04-class-design-patterns.png` | ready |
| 9 | The database: 10 triggers, audit trail, generated column | `screenshots/26-database-triggers-audit.png` | ready |
| 10 | Booking form with 5 fields rejected | `screenshots/04-booking-form-rejected.png` | ready |
| 11 | Main menu and today's diary | `screenshots/02-dashboard-menu.png` | ready |
| 12 | Appointment details, BOOKED | `screenshots/05-appointment-details-booked.png` | ready |
| 13 | The printed bill | `screenshots/12-bill-print-preview.png` | ready |
| 14 | The TDD red commit: 7 files, all tests | `screenshots/25-tdd-red-commit.png` | ready |
| 15 | 262 tests passing, BUILD SUCCESS | `screenshots/24-tests-passing.png` | ready |
| 16 | JaCoCo coverage | `screenshots/20-jacoco-coverage.png` | ready |
| 17 | GitHub Actions, all green | `screenshots/21-github-actions.png` | ready |
| 18 | Commit history | `screenshots/23-github-commit-history.png` | ready |
| 19 | Actions run detail | `screenshots/21-github-actions.png` | ready |
| 20 | Release v0.9.0 with the jar | `screenshots/22-github-release.png` | ready |

**Spares for the appendix:** the sign-in screen, the empty booking form, the
completed visit, appointment search, the day schedule, patient search, the
treatment history, the receipt on screen, the unpaid list, the help section,
the error page, a receptionist refused the reports, the reports menu, and both
management reports.

Every figure in the list is a file on disk. Drop them straight into Word, add
the caption, and refer to each one in the text.

# Before you submit — final checklist

- [ ] Every figure has a **numbered caption** and is **referred to in the text**
      ("as Figure 5 shows…"). An uncited figure earns nothing.
- [ ] Word count is under 4000 **excluding references and appendices**
- [ ] No pasted code that you do not then discuss
- [ ] The repository link appears in the report
- [ ] Every formatting rule in the checklist above is met
- [ ] Exported as **PDF**
- [ ] Named `st20360306 CIS6003 WRIT1`
- [ ] Turnitin similarity checked — and remember this is a **resit**: nothing
      may be reused from your Ocean View Resort submission, or it will match
      your own earlier work
- [ ] Submitted before **2:00 pm on 5 September 2026**
