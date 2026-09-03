"""
Builds an editable .docx of the report with pandoc.

The PDF is the submission format, and build-pdf.py produces it. This gives an
editable Word document as well, so sections can be reworded without going back
to the Markdown.

Word's own COM automation was tried first and hung for twenty minutes importing
the HTML, so pandoc converts the Markdown directly instead. The formatting the
brief asks for is carried by a reference document built here: A4, a 1.5 inch
left margin, Times New Roman 12pt at 1.5 line spacing, and 14pt bold headings.

    python build-docx.py
"""
import io, os, re, shutil, subprocess, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "_final.md")
MD = os.path.join(HERE, "_final_pandoc.md")
REF = os.path.join(HERE, "_reference.docx")
DOCX = os.path.join(HERE, "st20360306 CIS6003 WRIT1.docx")

PANDOC = shutil.which("pandoc") or os.path.expandvars(
    r"%LOCALAPPDATA%\Pandoc\pandoc.exe")
if not os.path.exists(PANDOC):
    sys.exit("pandoc not found")

# --- 1. turn the FIGURE markers into real images ---------------------------
s = io.open(SRC, encoding="utf-8").read()

def to_image(m):
    num, title, path = m.group(1), m.group(2).strip(), m.group(3).strip()
    name = os.path.basename(path)
    return "![Figure %s: %s](doc-images/%s)" % (num, title, name)

s = re.sub(r'`\[FIGURE (\d+): (.+?) — (.+?)\]`', to_image, s)
io.open(MD, "w", encoding="utf-8").write(s)
print("figure markers converted to images")

# --- 2. a reference document carrying the brief's formatting ---------------
subprocess.run([PANDOC, "--print-default-data-file", "reference.docx"],
               stdout=open(REF, "wb"), check=True)

TIMES = '<w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>'

def restyle(xml):
    # body text: Times New Roman 12pt (24 half-points), 1.5 spacing (line=360)
    xml = re.sub(r'(<w:docDefaults>.*?<w:rPrDefault>\s*<w:rPr>)',
                 r'\1' + TIMES + '<w:sz w:val="24"/><w:szCs w:val="24"/>',
                 xml, count=1, flags=re.S)
    xml = re.sub(r'(<w:docDefaults>.*?<w:pPrDefault>\s*<w:pPr>)',
                 r'\1<w:spacing w:line="360" w:lineRule="auto" w:after="200"/>',
                 xml, count=1, flags=re.S)
    # headings: Times New Roman 14pt bold black
    for n in (1, 2, 3):
        pat = r'(<w:style [^>]*w:styleId="Heading%d"[^>]*>.*?)(</w:style>)' % n
        ins = ('<w:rPr>' + TIMES +
               '<w:b/><w:color w:val="000000"/><w:sz w:val="28"/>'
               '<w:szCs w:val="28"/></w:rPr>')
        xml = re.sub(pat, lambda m: re.sub(r'<w:rPr>.*?</w:rPr>', '', m.group(1), flags=re.S)
                     + ins + m.group(2), xml, count=1, flags=re.S)
    return xml

def repage(xml):
    # A4 (11906 x 16838 twips) with a 1.5in left margin (2160 twips)
    sect = ('<w:pgSz w:w="11906" w:h="16838"/>'
            '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" '
            'w:left="2160" w:header="720" w:footer="720" w:gutter="0"/>')
    if "<w:sectPr" in xml:
        xml = re.sub(r'<w:pgSz[^>]*/>\s*(<w:pgMar[^>]*/>)?', sect, xml, count=1)
    return xml

tmp = REF + ".tmp"
with zipfile.ZipFile(REF) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "word/styles.xml":
            data = restyle(data.decode("utf-8")).encode("utf-8")
        elif item.filename == "word/document.xml":
            data = repage(data.decode("utf-8")).encode("utf-8")
        zout.writestr(item, data)
os.replace(tmp, REF)
print("reference document styled (A4, 1.5in left margin, Times New Roman)")

# --- 3. convert --------------------------------------------------------------
subprocess.run([PANDOC, MD, "-o", DOCX,
                "--reference-doc", REF,
                "--toc", "--toc-depth=2",
                "--resource-path", HERE],
               check=True, cwd=HERE)

# --- 4. force A4 and the margins onto the finished document -----------------
# Pandoc copies the section properties from the reference document, and its
# default reference has no page size or margins at all, so Word would fall back
# to its own. Patching the produced file is more reliable than templating it.
SECT = ('<w:pgSz w:w="11906" w:h="16838"/>'
        '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" '
        'w:left="2160" w:header="720" w:footer="720" w:gutter="0"/>')

tmp = DOCX + ".tmp"
with zipfile.ZipFile(DOCX) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "word/document.xml":
            xml = data.decode("utf-8")
            xml = re.sub(r'<w:pgSz[^>]*/>|<w:pgMar[^>]*/>', '', xml)
            xml = xml.replace("</w:sectPr>", SECT + "</w:sectPr>", 1)
            data = xml.encode("utf-8")
        zout.writestr(item, data)
os.replace(tmp, DOCX)
print("A4 and 1.5in left margin applied to the document")

os.remove(MD)
os.remove(REF)
print("\n%s" % os.path.basename(DOCX))
print("%.1f MB" % (os.path.getsize(DOCX) / 1048576))
print("\nOpen it in Word, then right-click the contents list and choose")
print("Update Field so the page numbers fill in.")
