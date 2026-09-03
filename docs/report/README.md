# The report — Task A to D

CIS6003 Advanced Programming (WRIT1) · Sunrise Dental Clinic

| File | What it is |
|---|---|
| [`CIS6003-WRIT1-report-draft.html`](CIS6003-WRIT1-report-draft.html) | **Start here.** The draft, already formatted to the brief, with all 19 figures placed and captioned. Open it in Word. |
| [`CIS6003-WRIT1-report-draft.md`](CIS6003-WRIT1-report-draft.md) | the same draft as text, if you would rather edit Markdown |
| [`report-skeleton.md`](report-skeleton.md) | the structure, word budget and full submission checklist |
| [`screenshots/`](screenshots/) | 27 screenshots, all captured automatically |
| [`make-word-version.py`](make-word-version.py) | regenerates the HTML from the Markdown |
| [`capture-screenshots.mjs`](capture-screenshots.mjs), [`capture-terminals.ps1`](capture-terminals.ps1), [`seed-demo-data.sh`](seed-demo-data.sh) | the tools that produced the screenshots |

---

## Getting to a submittable PDF

**1. Open the HTML in Word.**

```powershell
start winword "D:\ICBT\Advance programming\02_SunriseDentalClinic_Resit\SunriseDentalClinic\docs\report\CIS6003-WRIT1-report-draft.html"
```

Word opens HTML directly and keeps the formatting. The figures come in with it.

**2. Save As → Word Document (.docx)** straight away, so you are editing a real
document rather than an HTML file.

**3. Write the four sections marked ✍️ YOUR WORDS.** They are §2.5, §4.1, §4.6
and §6. Each has the material and the questions sitting underneath it. Delete
the prompt box once you have written the section.

**4. Delete the red box at the top.**

**5. Check the formatting.** The HTML already sets A4, a 1.5-inch left margin,
Times New Roman 12 pt, 1.5 line spacing and 14 pt bold headings. Word usually
carries all of that across, but confirm it, and add **page numbers bottom
right** yourself — that is a Word setting, not an HTML one.

**6. Insert the table of contents.** References → Table of Contents. It builds
itself from the headings.

**7. Check the word count.** Select the body only, excluding references and
appendices. See the red box for what to cut if you are over.

**8. Export to PDF** and name it `st20360306 CIS6003 WRIT1`.

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
