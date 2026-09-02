# UML diagrams — Task A

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

These are the design diagrams for Task A. They are written in **PlantUML**,
which is text, so they live in Git beside the code they describe and change
with it. The rendered PNGs in [`png/`](png/) are the ones to paste into the
report.

---

## The diagrams

| # | File | What it shows | Task A requirement |
|---|---|---|---|
| 1 | [`01-use-case.puml`](01-use-case.puml) | actors, use cases, `<<include>>` and `<<extend>>` | Use Case diagram |
| 2 | [`02-class-domain.puml`](02-class-domain.puml) | the domain model: attributes with types, method signatures, visibility, multiplicity, navigability, aggregation and composition | Class diagram |
| 3 | [`03-class-layers.puml`](03-class-layers.puml) | the three tiers and which way the dependencies point | Class diagram (architecture) |
| 4 | [`04-class-design-patterns.puml`](04-class-design-patterns.puml) | Factory, Strategy, Template Method, Observer, Singleton | Class diagram (patterns) |
| 5 | [`05-sequence-login.puml`](05-sequence-login.puml) | signing in | Sequence diagram — FR1 |
| 6 | [`06-sequence-register-appointment.puml`](06-sequence-register-appointment.puml) | registering a visit, including both failure paths | Sequence diagram — FR2 |
| 7 | [`07-sequence-generate-bill.puml`](07-sequence-generate-bill.puml) | calculating and printing the bill | Sequence diagram — FR4 |
| 8 | [`08-sequence-cancel-appointment.puml`](08-sequence-cancel-appointment.puml) | cancelling, and how the slot is freed | Sequence diagram — FR7 |

The brief asks for at least three sequence diagrams. There are four, because
cancelling shows two things the other three do not: how a slot is given back
without deleting anything, and how an `UPDATE` is audited by a trigger.

---

## Re-drawing them after a change

The `.puml` files are the originals. The PNGs are generated, so never edit a
PNG.

```powershell
# from this folder
java -jar D:\DevTools\plantuml.jar -tpng -o png *.puml
```

To work on them with a live preview, install the **PlantUML** extension in
VS Code and press `Alt+D` with a `.puml` file open. Nothing else needs to be
installed: PlantUML brings its own layout engine.

---

## Documented assumptions

Task A asks for assumptions to be written down. Each diagram carries the ones
that belong to it in its own legend; they are gathered here so they can be
read in one place.

### About who uses the system

1. **Only clinic staff sign in.** Patients never touch the system. They
   telephone or walk in, and a receptionist works on their behalf. Adding a
   patient-facing booking site would change the security model completely and
   is recorded as future work.
2. **Dentists are not actors.** They read the printed day schedule; they do not
   have logins. Giving them their own screen is a sensible next feature but it
   is not in this scope.
3. **An administrator is also a receptionist.** This is drawn as a
   generalisation, so every association to Receptionist applies to
   Administrator too.
4. **Signing in is assumed throughout.** Every use case except "Sign in"
   requires a signed-in member of staff. Drawing that as an `<<include>>` from
   all seventeen would bury the diagram in arrows.
5. **The email service is a supporting actor.** The system talks to it; it
   never starts anything. Notifications stay switched off until real mail
   credentials are supplied.

### About the clinic's rules

6. **One dentist can see one patient at a time.** There is no concept of two
   chairs for one dentist, so a dentist plus a date plus a time is unique.
7. **Appointments start on a half hour**, between 08:00 and 18:00. A treatment
   records how long it takes, but the system does not yet block the following
   slot for a long treatment. This is a known limitation and is stated in the
   report rather than hidden.
8. **A returning patient is recognised by name *and* telephone number.** Name
   alone is not enough, because two people really can both be called Kamal
   Silva. Telephone alone is not enough either, because a family shares one
   number.
9. **One visit, one bill.** Enforced in the service layer and again by a
   `UNIQUE` key on `bills.appointment_id`.
10. **Only a completed visit can be billed.** This stops the clinic charging
    for work it has not done.
11. **Prices are copied onto the bill when it is issued.** If the clinic raises
    the price of a filling next month, a receipt printed today must still show
    what the patient actually paid.
12. **Cancelling frees the slot; a no-show does not.** The dentist's time was
    already spent waiting for a patient who never came.

### About the design

13. **Patient owns its appointments (composition).** A visit is not a thing in
    its own right; it is *this patient's* visit.
14. **Dentist and Treatment only aggregate.** A dentist who leaves is marked
    inactive, never deleted, and their past appointments stay in the record, so
    the parts outlive the whole.
15. **Navigability is not decoration.** `Appointment` knows its `User`; `User`
    has no list of appointments, because nothing in the clinic asks "what did
    this receptionist book?".
16. **Getters and setters are omitted from diagram 02.** Every field has an
    ordinary pair. Drawing eighty of them would hide the handful of methods
    that decide anything.
17. **Some rules are deliberately enforced twice**, once in Java and once in
    the database. The Java check produces a sentence a receptionist can act on;
    the database check cannot be beaten by two people saving in the same
    instant, or by anyone reaching the database another way. The cost is that
    the rule is written in two places and could drift apart.

---

## What still has to be written by hand

The marking scheme for Task A asks for a **critical evaluation** of the design,
and Cardiff Met's unfair practice rules mean that analysis must be in your own
words. These diagrams and the assumptions above are the *evidence*; the
evaluation is the writing about that evidence, and it carries the marks.

Questions worth answering in that section, each of which the diagrams give you
something concrete to point at:

- **Do the three diagram types agree with each other?** Take one use case,
  "Register appointment", and follow it: it appears on diagram 1, its classes
  are on diagrams 2 and 3, and its messages are on diagram 6. Where they
  disagree, say so.
- **Was composition the right call for Patient → Appointment?** Argue it, and
  say what would break if it were aggregation instead.
- **Was it right to enforce the same rule in Java and in SQL?** Assumption 17
  is a genuine trade-off, not an obvious win. Take a position.
- **What does the design not do well?** Assumption 7 (long treatments do not
  block the next slot) is a real weakness found while modelling, not a
  hypothetical one.
- **What would you change if you started again?** The two controller families
  on diagram 3 are the most obvious candidate to defend or criticise.
