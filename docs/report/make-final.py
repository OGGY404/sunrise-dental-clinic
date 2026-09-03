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
#    table of contents.
start = s.index("> **READ THIS FIRST")
end = s.index("## Table of contents")
s = s[:start] + s[end:]

# 2. Remove the four "rewrite this" blockquotes. They are the only blockquotes
#    left in the document, so removing every remaining one is safe and is
#    checked below.
before = len(re.findall(r'^>', s, re.M))
s = re.sub(r'(?m)^> .*\n(?:^>.*\n)*\n?', '', s)
after = len(re.findall(r'^>', s, re.M))
if after:
    print("WARNING: %d blockquote lines remain" % after)

# 2b. Drop the table-of-contents placeholder note; Word inserts a real one.
s = re.sub(r'\*\(Generate in Word from the Heading styles.*?\)\*\n', '', s)

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
print("body %d + captions %d = %d words (limit 4000)" % (body, caps, body + caps))
