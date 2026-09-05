# Testing — Task C

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

Everything Task C asks for, with the evidence it is based on.

| Document | What it is | Task C requirement |
|---|---|---|
| [`test-plan.md`](test-plan.md) | 111 test cases: ID, precondition, steps, data, expected, actual, pass/fail, and the automated test that runs each one | test plan |
| [`test-data.md`](test-data.md) | where every test value came from — equivalence partitions, boundary values, and the devised cases | devised and derived test data |
| [`traceability-matrix.md`](traceability-matrix.md) | every requirement → design → code → tests | traceability matrix |
| [`tdd-evidence.md`](tdd-evidence.md) | the six red→green commit pairs, with hashes and timestamps | how TDD was used |
| [`manual-walkthrough.md`](manual-walkthrough.md) | the whole system driven end to end against real MySQL | screenshots and evidence |
| [`manual-walkthrough-output.txt`](manual-walkthrough-output.txt) | the captured output of that run | evidence |
| [`walkthrough.sh`](walkthrough.sh) | the script, so anyone can repeat it | test automation |

---

## The numbers

| | |
|---|---|
| Test classes | **32** |
| Automated test methods | **268** |
| Failures / errors | **0** |
| Instruction coverage | **78.8%** |
| Branch coverage | **62.2%** |
| Runs automatically | on every push and pull request |

```powershell
.\mvnw.cmd test                              # 262 tests, no database needed
.\mvnw.cmd test -Dtest=MySqlDatabaseIT       # 6 more, against real MySQL 8
```

---

## The testing approach, in short

Five levels, each there for a reason the others cannot cover.

1. **Unit tests (123).** One class at a time, collaborators replaced by Mockito
   mocks. Every clinic rule lives here. They need no database and no web
   server, which is only possible because the three tiers are genuinely
   separate — so the test suite is itself evidence for the architecture.

2. **Repository tests (38).** `@DataJpaTest` against in-memory H2, checking the
   JPA mappings and the derived query methods actually return what their long
   names promise.

3. **Web slice tests (82).** `@WebMvcTest`, one controller with its services
   mocked. These check the job of the presentation tier only: read the request,
   refuse bad input, call one service, return the right status and view.

4. **Integration tests (19).** The whole application context. Security is made
   of filters in front of the controllers, so nothing smaller could prove that
   an unauthenticated request is really stopped. Two of them drive a **real HTTP
   port** with real cookies.

5. **Database tests (6).** Against a **real MySQL 8**. H2 builds its tables from
   the entities, so it can only ever agree with them, and it has no stored
   procedures, triggers or generated columns at all.

The shape is deliberate: most of the tests are at the levels that run in
milliseconds, so a broken rule is reported in under a minute; the slow levels
are kept for what the fast ones genuinely cannot see.

### Why level 5 exists

It was added after the fact, and honestly so. Three bugs reached the running
application while the suite was passing:

- `password_hash` was `CHAR(60)` in SQL but `VARCHAR` on the entity — H2 could
  not notice, because it had built the column from the entity;
- the CSRF cookie was deleted at login and never reissued — MockMvc could not
  notice, because it supplies its own token;
- the tables and the database used different collations, so a stored function
  could not compare its parameter against a column and **every booking failed**
  — H2 could not notice, because it has no stored functions.

All three are now covered, and the fixes were checked to fail without them.
[`tdd-evidence.md`](tdd-evidence.md) §4 has the detail.

---

## What still has to be written by hand

Task C asks for a **rationale**, an **evaluation of overall success or failure**
and **lessons learned**. Those are analysis, and under Cardiff Met's unfair
practice rules they must be in your own words. The documents here are the
evidence to write *about*.

Prompts, each with something concrete to point at:

- **Why this shape of test suite?** Section above. Argue why 123 unit tests and
  only 6 database tests is the right balance, not a shortcut.
- **Was TDD worth it?** You have both sides: three decisions the red step got
  right (`tdd-evidence.md` §4) and three bugs it missed entirely. Take a
  position rather than praising the method.
- **Is 78.8% coverage good enough?** Use `service.notification` at 32.6% to
  explain why chasing a higher number is not automatically better, and say what
  you would test if the clinic switched email on.
- **What was the single biggest lesson?** The honest answer is in the three
  bugs: a test suite that supplies both sides of the exchange will confirm its
  own assumptions. Say what you would do differently — start the real database
  earlier — and what that would have cost.
- **What is not covered, and is that acceptable?** `traceability-matrix.md` §4
  lists the three gaps openly. Defend them.
