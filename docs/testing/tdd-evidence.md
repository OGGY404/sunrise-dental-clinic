# Test-driven development — the evidence

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

Task C asks how test-driven development was used **in this scenario**, with
real examples. The evidence is not a claim in a report: it is in the Git
history, where the failing tests were committed **before** the code that makes
them pass, every time, on five separate occasions.

Repository: <https://github.com/OGGY404/sunrise-dental-clinic>

---

## 1. The red–green pairs

Each pair below is two consecutive commits on a feature branch. The first adds
tests that **do not compile**, because the classes they name do not exist yet.
That is the red state, committed deliberately so it is visible in the history.
The second adds the code and turns the suite green.

| # | Step | RED commit | GREEN commit | When | Tests after |
|---|---|---|---|---|---|
| 1 | Domain model | `5c5c357` *add failing repository tests for the domain model* | `a05f7c4` *add JPA entities and Spring Data repositories* | 28 Aug 2026, 19:47 | **39** |
| 2 | Design patterns | `2e89427` *add failing tests for the Singleton, Strategy, Factory and Builder* | `dab3032` *implement the Singleton, Strategy, Template Method, Factory and Builder* | 28 Aug 2026, 20:35 → 20:39 | **77** |
| 3 | Service layer | `4fa3ebc` *add failing tests for the booking, patient and billing services* | `49c5559` *add the booking, patient and billing services with the Observer pattern* | 28 Aug 2026, 20:42 → 20:47 | **121** |
| 4 | Web services | `ca70acc` *add failing tests for the REST web services and input validation* | `1e07179` *add the REST web services, input validation and one error shape* | 1 Sep 2026, 23:45 → 23:51 | **183** |
| 5 | Security | `a41b34b` *add failing tests for login, sessions and role based access* | `2e068c6` *add login, session handling and role based access* | 1 Sep 2026, 23:54 → 23:59 | **203 → 205** |
| 6 | The screens | `2d3904e` *add failing tests for the clinic screens and the management reports* | `ee29194` *add the clinic screens and the two management reports* | 2 Sep 2026, 00:54 → 01:05 | **248** |

To see any red commit for yourself:

```powershell
git show 2d3904e --stat        # only test files, nothing in src/main
git checkout 2d3904e
.\mvnw.cmd test-compile        # fails: cannot find symbol AppointmentWebController
git checkout develop
```

## 2. How the suite grew

| After step | Tests | Added |
|---|---|---|
| 3 — entities and repositories | 39 | +39 |
| 4 — design patterns | 77 | +38 |
| 4 — services | 121 | +44 |
| 5 — web services and validation | 183 | +62 |
| 6 — login, sessions, roles | 205 | +22 |
| 7 — screens and reports | 248 | +43 |
| 9 — real MySQL integration tests | 254 | +6 |

Not one of those figures is a test written after the fact to raise a number.
Each block was written first, watched to fail, and then made to pass.

## 3. Refactor — the third step people forget

Red and green are easy to show. The refactor step is what the history has to
prove separately, and there are three clear examples.

**`AbstractBillingStrategy`.** The three pricing rules were written first as
three independent classes, and all their tests passed. Reading them afterwards
showed the same three things repeated in each: apply the loyalty discount, then
round the money, then build the explanation. `calculate()` was pulled up into an
abstract parent and made `final`, leaving each subclass three small methods to
fill in. **The tests did not change.** That is what made the refactor safe, and
it is exactly what the tests are for.

**`@EntityGraph` on the repositories.** Added in `1e07179` while building the
web layer. The associations are lazy and `open-in-view` is off, so mapping an
appointment to JSON after the transaction closed would have failed. The change
also removed an N+1 query problem: a day schedule of twenty visits had been one
query for the list and sixty more for the names. Again, no test changed — the
behaviour was already pinned down.

**`withoutProcedureColumnMetaDataAccess()`.** Added in step 9. The parameters
were already declared in the code, so the metadata lookup was never needed;
removing it fixed a real bug *and* saved a round trip to the database on every
booking.

## 4. What TDD actually caught, and what it did not

This is the honest part, and it is worth more in the report than a claim that
TDD prevented everything.

### Caught before the code existed

- **Today must be bookable.** Writing `todayIsAllowed` before the validation
  annotation forced the question "is today past or future?" to be answered on
  purpose. `@Future` would have been the obvious annotation and would have been
  wrong: a patient with toothache can walk in and be seen this afternoon.
- **A failing observer must not lose a booking.** Writing
  `aFailingObserverDoesNotLoseTheBooking` first is why `announce()` calls each
  observer inside its own `try`. Written code-first, the natural version would
  have let a mail server outage undo an appointment.
- **The loyalty boundary.** Writing the fourth-visit and fifth-visit tests
  together made "your fifth visit is discounted" mean one specific thing before
  any arithmetic was written.

### Missed by the whole suite, and found only by running the system

Three bugs reached the running application. All three are recorded in the
history with `fix:` commits, and all three are now covered.

| Bug | Why 248 tests all passed | Now covered by |
|---|---|---|
| `password_hash` was `CHAR(60)` in SQL but `VARCHAR` on the entity, so the app would not start against MySQL | H2 builds its tables **from the entities**, so the two could never disagree | `MySqlDatabaseIT.theScriptsBuiltEverything` |
| The CSRF cookie was deleted at login and never reissued, so every save after signing in returned 403 | MockMvc supplies its own token instead of reading the cookie, so no test depended on the cookie existing | `CsrfCookieEndToEndTest` (real port, real HTTP) |
| Tables were `utf8mb4_unicode_ci` but MySQL 8 creates the database as `utf8mb4_0900_ai_ci`, so a stored function could not compare its parameter against a column and **every booking failed** | H2 has no stored functions at all | `MySqlDatabaseIT.collationsMatch` |

The pattern is the same in all three: the tests were only ever asking the parts
of the system they could reach. H2 agreed with the entities because it was
built from them, and MockMvc agreed with itself because it supplied both sides
of the exchange. A test suite that only talks to itself will confirm its own
assumptions.

That is why the suite now has two levels it did not start with: six tests
against a **real MySQL 8**, and two driving a **real HTTP port** with real
cookies. Both were added after the bug, not before, and both were checked to
fail when the fix is removed — `CsrfCookieEndToEndTest` fails on *"a fresh token
must be issued on the first request after signing in"*, which is the exact
symptom the receptionist saw.

## 5. Test automation

- Every one of the 254 tests runs from one command and needs no human.
- `.github/workflows/ci.yml` runs the whole suite on **every push and pull
  request**, then repeats the database half against a real MySQL 8 service
  container, and only packages the jar if both passed.
- `.github/workflows/release.yml` runs the tests **again** before publishing a
  release, because a tag can be pushed from any machine and a released version
  must never be one nobody proved.
- JaCoCo writes a coverage report on every run, and both workflows keep the
  reports as downloadable artefacts for 30 days — including when the build
  fails, which is when they are most useful.

## 6. For the report

The rationale, the evaluation and the lessons learned have to be in your own
words. The material above is the evidence to write *about*. Questions worth
answering:

- **Was TDD worth it here?** You have the counter-evidence as well as the
  evidence: three bugs reached the running system anyway. Argue the balance.
- **What did the red step actually buy?** Section 4 has three concrete answers,
  each about a decision that would probably have gone the other way.
- **What would you test differently next time?** The honest answer is in the
  table in section 4: start the real database earlier. Say so, and say what it
  would have cost.
- **Is 80.9% coverage good?** Take a position, and use `service.notification`
  at 48.6% to explain why chasing a higher number is not automatically better.
