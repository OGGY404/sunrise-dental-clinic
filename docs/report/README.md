# The report — Task A to D

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

| File | What it is |
|---|---|
| **`st20360306 CIS6003 WRIT1.pdf`** | **the submission.** 33 pages, printed from the .docx by Word, so the two cannot disagree. |
| `st20360306 CIS6003 WRIT1.docx` | the master: cover page, contents list that fills itself in, page numbers bottom right, all 19 figures captioned, cited and inside the margins |
| `_final.md` / `CIS6003-WRIT1-final.html` | the clean source both are built from |
| `CIS6003-WRIT1-report-draft.md` | **the source. Edit this one.** |
| [`report-skeleton.md`](report-skeleton.md) | the structure, word budget and submission checklist |
| [`screenshots/`](screenshots/) | 28 screenshots, all captured automatically. Appendix F points here |
| `doc-images/` | the 19 the report embeds, shrunk to document size. Rebuilt by `make-doc-images.py`, not kept in Git |

## Rebuilding

```powershell
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic\docs\report"

python make-final.py           # draft -> _final.md, and the word count
python make-doc-images.py      # figures -> doc-images/ at document size
python build-docx.py           # _final.md -> the .docx
python make-pdf.py             # the .docx -> the PDF that gets submitted
```

Edit the **draft**, never `_final.md`: `make-final.py` overwrites `_final.md`
every run. It strips the guidance box, the four "rewrite this" markers and the
notes written to you rather than to the marker, then reports the word count the
4,000 limit actually applies to — body plus captions and tables, excluding
references and appendices.

`build-docx.py` fixes the styles on the finished Word file rather than on a
pandoc reference document, because pandoc writes some styles of its own into
the output that a reference document never reaches. Word fills the contents
list in by itself when the file opens; press F9 on it if it ever looks stale.

`make-pdf.py` has Word open the .docx, update the contents list and print to
PDF, so the PDF always matches the .docx. Only opening and printing is
automated — it was importing HTML that used to hang Word for twenty minutes,
and that no longer happens. `build-pdf.py` is the older Chrome route, kept but
no longer used.

---

## Before submitting

Done already, and checked in the finished PDF:

- [x] All 19 figures numbered, captioned, and **referred to in the text**.
- [x] Word count **3,989** — body plus captions and tables, excluding
      references and appendices, against the 4,000 limit. `make-final.py`
      prints this number on every run. Word's own count says 4,403 because it
      counts the references and appendices too; that is not the limit.
- [x] Repository link and student ID both appear in the report.
- [x] No placeholders left: the four `(Accessed: DATE)` now read
      **2 September 2026** — change that if you read them on another day.
- [x] Cover page, contents list with page numbers, page numbers bottom right.
- [x] Named `st20360306 CIS6003 WRIT1.pdf`.

Still yours to do:

- [ ] **Rewrite §2.5, §4.1, §4.6 and §6 in your own voice.** See the section
      below. This is the one thing that cannot be done for you.
- [ ] Read it through once, out loud if you can.
- [ ] Turnitin similarity check — and remember this is a **resit**: nothing may
      be reused from the Ocean View Resort submission, or it will match your
      own earlier work.
- [ ] Submit through Turnitin on Moodle before **2:00 pm on 5 September 2026**.

---

## What was written for you, and what was not

**Written:** every descriptive and technical section — what the system does, how
it is built, what each figure shows, and the numbers. All of it documents work
that exists in this repository, and every figure was measured rather than
estimated.

**Drafted, but not yours yet:** the four analysis sections — **§2.5** critical
evaluation of the design, **§4.1** rationale for the testing approach, **§4.6**
evaluation and lessons learned, and **§6** the conclusion. They are written and
every claim in them is true and defensible in a viva, but the sentences are not
yours. The "REWRITE THIS IN YOUR OWN VOICE" markers have been deleted from the
draft, so this list is now the only record of which four they are.

Those four carry the marks, and Cardiff Met's unfair practice rules cover
generated text. Read each one, close it, and write the same argument the way you
would say it out loud. The evidence is already gathered for you, including the
evidence that argues *against* the work — which reads far better than claiming
everything went well.

Then run `python make-final.py` and check the word count still fits, and
`python build-docx.py` followed by `python make-pdf.py` to rebuild both files.

---

## If you change the draft

Edit `CIS6003-WRIT1-report-draft.md`, then rebuild everything:

```powershell
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic\docs\report"
python make-final.py     # also prints the word count against the 4,000 limit
python build-docx.py
python make-pdf.py
```
