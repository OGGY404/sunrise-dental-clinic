# The report — Task A to D

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

| File | What it is |
|---|---|
| **`st20360306 CIS6003 WRIT1.pdf`** | **the submission.** 28 pages, formatted to the brief, all 19 figures, table of contents with page numbers, page numbers bottom right. |
| `st20360306 CIS6003 WRIT1.docx` | the same content, editable in Word |
| `CIS6003-WRIT1-final.html` | the clean source both are built from |
| `CIS6003-WRIT1-report-draft.md` / `.html` | the working draft, which still carries the guidance box and the four "rewrite this" markers |
| [`report-skeleton.md`](report-skeleton.md) | the structure, word budget and submission checklist |
| [`screenshots/`](screenshots/) | 27 screenshots, all captured automatically |

## Rebuilding

```powershell
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic\docs\report"
eport"

python make-word-version.py    # draft .md  -> draft .html
python make-final.py           # draft      -> clean final .html (no guidance box)
python make-doc-images.py      # figures    -> doc-images/ at document size
python build-pdf.py            # final .html -> the PDF, with a real contents list
python build-docx.py           # final .md  -> the .docx
```

`build-pdf.py` renders the PDF, reads back which page each heading landed on,
writes the contents list, and renders again until the page numbers stop moving.

**Word's own automation does not work here.** It applies the formatting and then
hangs on save — twenty minutes, twice, producing nothing. Chrome renders the PDF
in seconds and pandoc produces the .docx, which is why the scripts are built
that way.

---

## Before submitting

- [ ] Read it through once.
- [ ] Check the word count in Word: select the body only, excluding references
      and appendices. It measured 4,012 including figure captions against a
      4,000 limit; drop a figure you do not discuss if Word puts you over.
- [ ] Confirm the file is named `st20360306 CIS6003 WRIT1.pdf`.
- [ ] Submit through Turnitin on Moodle before **2:00 pm on 5 September**.

---

## What was written for you, and what was not

**Written:** every descriptive and technical section — what the system does, how
it is built, what each figure shows, and the numbers. All of it documents work
that exists in this repository, and every figure was measured rather than
estimated.

**Not written, on purpose:** the four analysis sections. Those carry the marks,
and `CLAUDE.md` §10 — your own instruction — says the analysis, justifications,
critical evaluation and reflection must be in your own words. Each is left as a
prompt with the evidence beside it, including the evidence that argues *against*
the work, which reads far better than claiming everything went well.

---

## If you change the draft

Edit the Markdown, then regenerate the HTML:

```powershell
cd "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic\docs\report"
python make-word-version.py
```
