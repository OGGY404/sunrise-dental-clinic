"""
Turns the report draft into a Word-ready HTML file.

Word opens .html directly and keeps the styling, so opening this file and
choosing File > Save As > .docx gives a document already formatted to the
CIS6003 brief: A4, 1.5" left margin, Times New Roman 12pt, 1.5 line spacing,
14pt bold headings, with every figure already placed and captioned.
"""
import io, re, os, html

import sys
SRC = sys.argv[1] if len(sys.argv) > 1 else "CIS6003-WRIT1-report-draft.md"
OUT = sys.argv[2] if len(sys.argv) > 2 else "CIS6003-WRIT1-report-draft.html"

lines = io.open(SRC, encoding="utf-8").read().split("\n")

# ---- figure captions, taken from the screenshot documentation ---------------
CAPTIONS = {
 "01-use-case":                       "Use case diagram.",
 "02-class-domain":                   "Domain model.",
 "03-class-layers":                   "The three tiers and their dependencies.",
 "04-class-design-patterns":          "Design patterns in the business tier.",
 "05-sequence-login":                 "Sequence: signing in (FR1).",
 "06-sequence-register-appointment":  "Sequence: registering an appointment (FR2).",
 "07-sequence-generate-bill":         "Sequence: calculating the bill (FR4).",
 "26-database-triggers-audit":        "Triggers, audit rows and the generated column.",
 "04-booking-form-rejected":          "A rejected booking form.",
 "02-dashboard-menu":                 "The main menu.",
 "05-appointment-details-booked":     "Appointment details for a booked visit.",
 "12-bill-print-preview":             "The bill as printed.",
 "25-tdd-red-commit":                 "A red commit: only test files.",
 "24-tests-passing":                  "The full suite: 262 tests, 0 failures.",
 "20-jacoco-coverage":                "JaCoCo coverage report.",
 "21-github-actions":                 "GitHub Actions: the three CI jobs.",
 "23-github-commit-history":          "Commit history.",
 "22-github-release":                 "Release v0.9.0 with the runnable jar.",
 "27-appointment-reminders":          "Appointment reminders.",
}

def inline(t):
    t = html.escape(t)
    t = re.sub(r'`([^`]+)`', r'<code>\1</code>', t)
    t = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', t)
    t = re.sub(r'(?<!\*)\*([^*]+)\*(?!\*)', r'<em>\1</em>', t)
    t = re.sub(r'&lt;(https?://[^&]+)&gt;', r'<a href="\1">\1</a>', t)
    t = re.sub(r'\[([^\]]+)\]\((https?://[^)]+)\)', r'<a href="\2">\1</a>', t)
    t = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'\1', t)
    return t

def fig_path(p):
    p = p.strip()
    if p.startswith("docs/report/"):  return p[len("docs/report/"):]
    if p.startswith("docs/"):         return "../" + p[len("docs/"):]
    return p

out, i, fignum = [], 0, 0
while i < len(lines):
    ln = lines[i]

    m = re.match(r'\s*`\[FIGURE (\d+): (.+?) — (.+?)\]`\s*$', ln)
    if m:
        fignum = int(m.group(1))
        path = fig_path(m.group(3))
        key = os.path.splitext(os.path.basename(path))[0]
        cap = CAPTIONS.get(key, m.group(2))
        exists = os.path.exists(path)
        out.append('<div class="figure">')
        if exists:
            out.append('<img src="%s" alt="Figure %d">' % (html.escape(path), fignum))
        else:
            out.append('<p class="missing">[Figure %d image not found: %s]</p>' % (fignum, html.escape(path)))
        out.append('<p class="caption">Figure %d: %s</p>' % (fignum, html.escape(cap)))
        out.append('</div>')
        i += 1; continue

    if ln.startswith("> "):
        block = []
        while i < len(lines) and (lines[i].startswith(">") or lines[i].strip() == ""):
            if lines[i].strip() == "":
                if i + 1 < len(lines) and lines[i+1].startswith(">"): block.append(""); i += 1; continue
                break
            block.append(lines[i][1:].lstrip()); i += 1
        out.append('<div class="note">')
        para = []
        for b in block:
            if not b:
                if para: out.append("<p>" + inline(" ".join(para)) + "</p>"); para = []
            elif re.match(r'^[-\d]+[.)]?\s', b) or b.startswith("|"):
                if para: out.append("<p>" + inline(" ".join(para)) + "</p>"); para = []
                out.append("<p>" + inline(b) + "</p>")
            else:
                para.append(b)
        if para: out.append("<p>" + inline(" ".join(para)) + "</p>")
        out.append("</div>")
        continue

    if ln.startswith("|"):
        rows = []
        while i < len(lines) and lines[i].startswith("|"):
            rows.append(lines[i]); i += 1
        cells = [[c.strip() for c in r.strip().strip("|").split("|")] for r in rows]
        cells = [c for c in cells if not all(re.fullmatch(r':?-{2,}:?', x or "-") for x in c)]
        if cells:
            out.append("<table>")
            out.append("<tr>" + "".join("<th>%s</th>" % inline(c) for c in cells[0]) + "</tr>")
            for r in cells[1:]:
                out.append("<tr>" + "".join("<td>%s</td>" % inline(c) for c in r) + "</tr>")
            out.append("</table>")
        continue

    if ln.startswith("```"):
        i += 1; code = []
        while i < len(lines) and not lines[i].startswith("```"):
            code.append(lines[i]); i += 1
        i += 1
        out.append("<pre>" + html.escape("\n".join(code)) + "</pre>")
        continue

    if ln.startswith("- "):
        items = []
        while i < len(lines) and lines[i].startswith("- "):
            item = lines[i][2:]; i += 1
            while i < len(lines) and lines[i].startswith("  ") and lines[i].strip():
                item += " " + lines[i].strip(); i += 1
            items.append(item)
        out.append("<ul>" + "".join("<li>%s</li>" % inline(x) for x in items) + "</ul>")
        continue

    if re.match(r'^\d+\. ', ln):
        items = []
        while i < len(lines) and re.match(r'^\d+\. ', lines[i]):
            item = re.sub(r'^\d+\. ', '', lines[i]); i += 1
            while i < len(lines) and lines[i].startswith("   ") and lines[i].strip():
                item += " " + lines[i].strip(); i += 1
            items.append(item)
        out.append("<ol>" + "".join("<li>%s</li>" % inline(x) for x in items) + "</ol>")
        continue

    if ln.startswith("#"):
        lvl = len(ln) - len(ln.lstrip("#"))
        out.append("<h%d>%s</h%d>" % (min(lvl, 3), inline(ln.lstrip("# ").strip()), min(lvl, 3)))
        i += 1; continue

    if ln.strip() in ("---", "***"):
        out.append('<p class="rule"></p>'); i += 1; continue

    if ln.strip() == "":
        i += 1; continue

    para = []
    while i < len(lines) and lines[i].strip() and not lines[i].startswith(("#", "|", "- ", "> ", "```")) \
          and not re.match(r'^\d+\. ', lines[i]) and lines[i].strip() not in ("---", "***") \
          and not re.match(r'\s*`\[FIGURE', lines[i]):
        para.append(lines[i].strip()); i += 1
    if para:
        out.append("<p>" + inline(" ".join(para)) + "</p>")

CSS = """
@page { size: A4; margin: 1in 1in 1in 1.5in; }
body { font-family: "Times New Roman", Times, serif; font-size: 12pt;
       line-height: 1.5; color: #000; max-width: 17cm; margin: 0 auto; }
h1 { font-size: 14pt; font-weight: bold; margin: 22pt 0 10pt; page-break-after: avoid; }
h2 { font-size: 14pt; font-weight: bold; margin: 18pt 0 8pt; page-break-after: avoid; }
h3 { font-size: 12pt; font-weight: bold; margin: 14pt 0 6pt; page-break-after: avoid; }
p { margin: 0 0 10pt; text-align: justify; }
ul, ol { margin: 0 0 10pt 0; padding-left: 22pt; }
li { margin-bottom: 4pt; }
table { border-collapse: collapse; width: 100%; margin: 10pt 0 14pt; font-size: 10.5pt; }
th, td { border: 1px solid #000; padding: 4pt 6pt; text-align: left; vertical-align: top; }
th { background: #e8e8e8; font-weight: bold; }
code { font-family: "Courier New", monospace; font-size: 10.5pt; }
pre { font-family: "Courier New", monospace; font-size: 10pt; border: 1px solid #999;
      padding: 8pt; background: #f5f5f5; white-space: pre-wrap; }
.figure { margin: 14pt 0 16pt; text-align: center; page-break-inside: avoid; }
.figure img { max-width: 100%; border: 1px solid #999; }
.caption { font-size: 10.5pt; font-style: italic; text-align: center; margin-top: 6pt; }
.missing { color: #b00; font-style: italic; }
.note { border: 2px solid #b00; background: #fff5f5; padding: 10pt 14pt; margin: 14pt 0; }
.note p { font-size: 11pt; }
.rule { border-top: 1px solid #bbb; margin: 16pt 0; }
.cover { text-align: center; page-break-after: always; padding-top: 5cm; }
.cover h1 { font-size: 20pt; margin-bottom: 6pt; page-break-after: avoid; }
.cover p { text-align: center; margin: 0 0 8pt; }
.cover .sub { font-size: 14pt; margin-bottom: 40pt; }
.cover .meta { margin-top: 60pt; line-height: 2; }
"""

# The leading title block becomes a cover page of its own: the first heading
# and everything before the first horizontal rule, centred, with a page break
# after it. The brief does not demand a cover page, but a report without one
# reads as unfinished.
for i, line in enumerate(out):
    if line.startswith("<p class=\"rule\">"):
        block = out[:i]
        if block and block[0].startswith("<h1>"):
            title = block[0]
            rest = block[1:]
            if rest:
                rest[0] = rest[0].replace("<p>", '<p class="sub">', 1)
            if len(rest) > 1:
                rest[1] = '<div class="meta">' + rest[1]
                rest[-1] = rest[-1] + "</div>"
            out[:i] = ['<div class="cover">', title] + rest + ["</div>"]
        break

io.open(OUT, "w", encoding="utf-8").write(
    '<html><head><meta charset="utf-8"><title>CIS6003 WRIT1 - Sunrise Dental Clinic</title>'
    "<style>%s</style></head><body>\n%s\n</body></html>" % (CSS, "\n".join(out)))

imgs = len(re.findall(r'<img ', "\n".join(out)))
missing = len(re.findall(r'class="missing"', "\n".join(out)))
print("wrote %s" % OUT)
print("figures embedded: %d   missing: %d" % (imgs, missing))
