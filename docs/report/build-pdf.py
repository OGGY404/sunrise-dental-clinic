"""
Renders the submission PDF, with a real table of contents.

Chrome renders the pages but cannot build a table of contents, because nothing
knows a page number until the document has been laid out. So this renders the
PDF, reads back which page each heading landed on, writes the contents list into
the HTML, and renders again. Adding the list shifts the pages, so it repeats
until the numbers stop moving - usually twice.

    python build-pdf.py

Produces "st20360306 CIS6003 WRIT1.pdf".
"""
import io, os, re, subprocess, sys, html as htmlmod

HERE = os.path.dirname(os.path.abspath(__file__))
HTML = os.path.join(HERE, "CIS6003-WRIT1-final.html")
PDF = os.path.join(HERE, "st20360306 CIS6003 WRIT1.pdf")
RENDER = "D:/DevTools/shots/topdf.mjs"

import pypdf


def render():
    subprocess.run(["node", RENDER], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)


def headings():
    """Every h1 and h2 in document order, with its level."""
    s = io.open(HTML, encoding="utf-8").read()
    out = []
    for m in re.finditer(r'<h([12])>(.*?)</h\1>', s):
        text = re.sub(r'<[^>]+>', '', m.group(2))
        text = htmlmod.unescape(text).strip()
        if text.lower() in ("table of contents",):
            continue
        if not out and text.lower().startswith("sunrise dental clinic"):
            continue      # the document title, not a section
        out.append((int(m.group(1)), text))
    return out


def normalise(t):
    return re.sub(r'[^a-z0-9]', '', t.lower())


def page_of(reader, wanted, start):
    """First page at or after `start` whose text contains this heading."""
    key = normalise(wanted)
    for i in range(start, len(reader.pages)):
        text = normalise(reader.pages[i].extract_text() or "")
        if key and key in text:
            return i + 1
    return None


def build_toc(entries):
    rows = []
    for level, text, page in entries:
        cls = "toc1" if level == 1 else "toc2"
        rows.append('<p class="%s"><span class="t">%s</span>'
                    '<span class="p">%s</span></p>'
                    % (cls, htmlmod.escape(text), page if page else ""))
    return "\n".join(rows)


CSS = """
.toc1, .toc2 { margin: 0 0 4pt; display: flex; align-items: baseline; }
.toc1 { font-weight: bold; margin-top: 8pt; }
.toc2 { padding-left: 18pt; }
.toc1 .t, .toc2 .t { flex: 0 1 auto; }
.toc1 .p, .toc2 .p { margin-left: auto; padding-left: 8pt; }
.toc1::after, .toc2::after { content: ""; flex: 1 1 auto;
    border-bottom: 1px dotted #999; margin: 0 4pt; order: 1; }
.toc1 .p, .toc2 .p { order: 2; }
"""

previous = None
for attempt in range(1, 5):
    render()
    reader = pypdf.PdfReader(PDF)

    # The contents list itself sits on the page holding that heading; start
    # looking for real headings after it so the list does not match itself.
    toc_page = 0
    for i in range(len(reader.pages)):
        if "tableofcontents" in normalise(reader.pages[i].extract_text() or ""):
            toc_page = i
            break

    # Start after the contents page. The list repeats every heading, so
    # searching from the page it sits on matches the list rather than the
    # section, and every entry comes back as page 1.
    entries, cursor = [], toc_page + 1
    for level, text in headings():
        p = page_of(reader, text, cursor)
        if p:
            cursor = p - 1
        else:
            cursor = toc_page + 1
        entries.append((level, text, p))

    numbers = [e[2] for e in entries]
    if numbers == previous:
        print("page numbers stable after %d render(s)" % attempt)
        break
    previous = numbers

    s = io.open(HTML, encoding="utf-8").read()
    if ".toc1" not in s:
        s = s.replace("</style>", CSS + "</style>", 1)
    block = build_toc(entries)
    s = re.sub(r'(<h2>Table of contents</h2>)(.*?)(?=<p class="rule">)',
               lambda m: m.group(1) + "\n" + block + "\n", s, count=1, flags=re.S)
    io.open(HTML, "w", encoding="utf-8").write(s)
    print("pass %d: contents list written (%d entries)" % (attempt, len(entries)))

reader = pypdf.PdfReader(PDF)
print("\n%s" % os.path.basename(PDF))
print("%d pages, %.1f MB" % (len(reader.pages), os.path.getsize(PDF) / 1048576))
