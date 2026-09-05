"""
Produces the submission-ready version of the report.

Takes the working draft, removes the guidance box and the four
"REWRITE THIS IN YOUR OWN VOICE" markers, and writes a clean HTML file that
build-docx.ps1 then turns into a .docx and a PDF.

    python make-final.py

The working draft is left untouched.
"""
import io, re, subprocess, sys

DRAFT = "CIS6003-WRIT1-report-draft.md"
CLEAN_MD = "_final.md"
CLEAN_HTML = "CIS6003-WRIT1-final.html"

s = io.open(DRAFT, encoding="utf-8").read()

# 1. Remove the guidance box, from "READ THIS FIRST" to the rule before the
#    table of contents. The box has since been deleted from the draft itself,
#    so it is fine for this to find nothing.
start = s.find("> **READ THIS FIRST")
end = s.find("## Table of contents")
if start != -1 and end != -1:
    s = s[:start] + s[end:]

# 2. Remove any remaining note to me rather than to the marker. These were
#    written as blockquotes, and the report itself quotes nothing, so removing
#    every blockquote is safe.
before = len(re.findall(r'^>', s, re.M))
s = re.sub(r'(?m)^> .*\n(?:^>.*\n)*\n?', '', s)
after = len(re.findall(r'^>', s, re.M))
if after:
    print("WARNING: %d blockquote lines remain" % after)

# 2b. Drop the notes written to me rather than to the marker. Each one is a
#     whole paragraph in italic brackets, like "(Excluded from the word count
#     — use them generously.)". Three of them: the table of contents note, and
#     one each above the references and the appendices.
notes = len(re.findall(r'(?m)^\*\((?:[^\n]|\n(?!\n))*?\)\*[ \t]*\n', s))
s = re.sub(r'(?m)^\*\((?:[^\n]|\n(?!\n))*?\)\*[ \t]*\n', '', s)

# 2c. The reference list needs no line introducing it.
s = re.sub(r'(?m)^Candidates, if you read them:[ \t]*\n', '', s)

# 3. Tidy: collapse any run of blank lines left behind.
s = re.sub(r'\n{3,}', '\n\n', s)

io.open(CLEAN_MD, "w", encoding="utf-8").write(s)

# 4. Convert to HTML with the same converter the draft uses.
subprocess.run([sys.executable, "make-word-version.py", CLEAN_MD, CLEAN_HTML], check=True)

def words(t):
    t = re.sub(r'`[^`]*`', ' ', t)
    t = re.sub(r'[|#*_>\[\]()–—•]', ' ', t)
    return len([w for w in t.split() if any(c.isalnum() for c in w)])

body, in_refs = 0, False
for ln in s.split("\n"):
    if ln.startswith("# References"):
        in_refs = True
    if not in_refs:
        body += words(ln)

h = io.open(CLEAN_HTML, encoding="utf-8").read()
caps = sum(len(c.split()) for c in re.findall(r'<p class="caption">(.*?)</p>', h))

print("removed %d blockquote lines" % (before - after))
print("removed %d notes written to me rather than to the marker" % notes)
print("body %d + captions %d = %d words (limit 4000)" % (body, caps, body + caps))
