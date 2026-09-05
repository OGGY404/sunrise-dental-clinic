"""
Builds the Word (.docx) version of the report.

What this script does, in order:
  1. reads _final.md, drops the old title block, turns FIGURE markers into images
  2. defines the styles the brief asks for: A4, 1.5 inch left margin,
     Times New Roman 12pt, 1.5 line spacing, 14pt bold headings, justified
     body text, bordered tables
  3. runs pandoc to make the document
  4. patches the finished file: applies those styles, adds a cover page and a
     page-number footer, shrinks any figure that is wider or taller than the
     page, and starts each main section on a new page

Word's own COM automation was tried first and hung for twenty minutes, so the
file is built and patched directly instead.

    python build-docx.py
"""
import io, os, re, shutil, subprocess, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SRC  = os.path.join(HERE, "_final.md")
MD   = os.path.join(HERE, "_final_pandoc.md")
DOCX = os.path.join(HERE, "st20360306 CIS6003 WRIT1.docx")

PANDOC = shutil.which("pandoc") or os.path.expandvars(
    r"%LOCALAPPDATA%\Pandoc\pandoc.exe")
if not os.path.exists(PANDOC):
    sys.exit("pandoc not found")

# --- cover page details ------------------------------------------------------
UNIVERSITY = "CARDIFF METROPOLITAN UNIVERSITY"
CAMPUS     = "ICBT Campus, Sri Lanka"
PROGRAMME  = "BSc (Hons) Software Engineering"
MODULE     = "CIS6003 Advanced Programming"
ASSESSMENT = "WRIT1: Individual Written Report"
TITLE      = "Sunrise Dental Clinic"
SUBTITLE   = "Appointment and Patient Management System"
STUDENT    = "W.A. Gangul Kalhara Rathnayake"
STUDENT_ID = "st20360306"
REPO       = "https://github.com/OGGY404/sunrise-dental-clinic"
DATE       = "September 2026"

# A4 page with a 1.5 inch left margin. Sizes are twips (1 inch = 1440).
PAGE = ('<w:pgSz w:w="11906" w:h="16838"/>'
        '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" '
        'w:left="2160" w:header="720" w:footer="720" w:gutter="0"/>')

# The text column is the A4 width minus both margins. Figures must fit inside.
EMU_CM = 360000
MAX_W  = int(14.3 * EMU_CM)   # a little narrower than the column, to be safe
MAX_H  = int(20.0 * EMU_CM)   # leaves room for the caption on the same page

TIMES = ('<w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" '
         'w:cs="Times New Roman"/>')

W_NS = ('xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"')


# ============================================================================
# 1. the markdown pandoc will read
# ============================================================================
s = io.open(SRC, encoding="utf-8").read()

# The old title block and the empty "Table of contents" heading are replaced by
# a real cover page and a real contents list, so cut everything before part 1.
cut = s.find("\n# 1. Introduction")
if cut > 0:
    s = s[cut + 1:]

# Horizontal rules turned into stray blank paragraphs in Word. Drop them.
s = "\n".join(ln for ln in s.split("\n") if ln.strip() not in ("---", "***"))


def to_image(m):
    num, title, path = m.group(1), m.group(2).strip(), m.group(3).strip()
    return "![Figure %s: %s](doc-images/%s)" % (num, title, os.path.basename(path))


s = re.sub(r'`\[FIGURE (\d+): (.+?) — (.+?)\]`', to_image, s)

# Each figure must sit alone with a blank line around it. Three markers written
# on three touching lines became one paragraph holding three pictures, and
# pandoc gives a caption only to a picture that stands alone, so nine figures
# came out with no caption at all.
apart, figures = [], 0
for ln in s.split("\n"):
    if re.fullmatch(r'!\[Figure .*\]\(doc-images/[^)]+\)', ln.strip()):
        figures += 1
        if apart and apart[-1].strip():
            apart.append("")
        apart.append(ln.strip())
        apart.append("")
    else:
        apart.append(ln)
s = "\n".join(apart)

io.open(MD, "w", encoding="utf-8").write(s)
print("markdown prepared: title block removed, %d figures set apart" % figures)


# ============================================================================
# 2. the styles that carry the formatting the brief asks for
# ============================================================================

def one(xml, block_pat, remove_pats, insert, where):
    """Inside the block matched by block_pat, delete some tags then add ours.

    The old script only added tags. Word reads the last tag of a kind, so the
    theme font already there kept winning and the body was not Times New Roman.
    Deleting first is what makes the change stick.
    """
    m = re.search(block_pat, xml, re.S)
    if not m:
        return xml
    block = m.group(0)
    for p in remove_pats:
        block = re.sub(p, "", block, flags=re.S)
    block = block.replace(where, where + insert, 1)
    return xml[:m.start()] + block + xml[m.end():]


def restyle(xml):
    # ---- default font: Times New Roman 12pt --------------------------------
    xml = one(xml, r'<w:rPrDefault>.*?</w:rPrDefault>',
              [r'<w:rFonts[^>]*/>', r'<w:sz[^>]*/>', r'<w:szCs[^>]*/>'],
              TIMES + '<w:sz w:val="24"/><w:szCs w:val="24"/>', '<w:rPr>')

    # ---- default paragraph: 1.5 line spacing -------------------------------
    xml = one(xml, r'<w:pPrDefault>.*?</w:pPrDefault>',
              [r'<w:spacing[^>]*/>'],
              '<w:spacing w:line="360" w:lineRule="auto" w:after="200"/>',
              '<w:pPr>')

    # ---- Normal: say it again here so nothing can undo it ------------------
    xml = re.sub(r'<w:style [^>]*w:styleId="Normal"[^>]*>.*?</w:style>',
                 '<w:style w:default="1" w:styleId="Normal" w:type="paragraph">'
                 '<w:name w:val="Normal"/><w:qFormat/>'
                 '<w:pPr><w:spacing w:line="360" w:lineRule="auto" w:after="200"/>'
                 '</w:pPr><w:rPr>' + TIMES +
                 '<w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr></w:style>',
                 xml, count=1, flags=re.S)

    # ---- headings: Times New Roman 14pt bold, black, left aligned ----------
    for n in (1, 2, 3):
        pat = r'(<w:style [^>]*w:styleId="Heading%d"[^>]*>.*?)(</w:style>)' % n
        rpr = ('<w:rPr>' + TIMES + '<w:b/><w:color w:val="000000"/>'
               '<w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr>')

        def fix(m, rpr=rpr):
            head = re.sub(r'<w:rPr>.*?</w:rPr>', '', m.group(1), flags=re.S)
            head = head.replace('<w:outlineLvl',
                                '<w:jc w:val="left"/><w:outlineLvl', 1)
            return head + rpr + m.group(2)

        xml = re.sub(pat, fix, xml, count=1, flags=re.S)

    # ---- body text justified, the way a report reads -----------------------
    for sid in ("BodyText", "FirstParagraph", "Compact", "BlockText"):
        pat = r'(<w:style [^>]*w:styleId="%s"[^>]*>)(.*?)(</w:style>)' % sid

        def just(m):
            mid = m.group(2)
            if "<w:pPr>" in mid:
                mid = mid.replace("<w:pPr>", '<w:pPr><w:jc w:val="both"/>', 1)
            else:
                mid += '<w:pPr><w:jc w:val="both"/></w:pPr>'
            return m.group(1) + mid + m.group(3)

        xml = re.sub(pat, just, xml, count=1, flags=re.S)

    # ---- figures and captions sit in the middle of the page ----------------
    xml = re.sub(r'(<w:style [^>]*w:styleId="Figure"[^>]*>.*?)(</w:style>)',
                 r'\1<w:pPr><w:jc w:val="center"/><w:keepNext/>'
                 r'<w:spacing w:before="200" w:after="60" w:line="240" '
                 r'w:lineRule="auto"/></w:pPr>\2',
                 xml, count=1, flags=re.S)
    xml = re.sub(r'(<w:style [^>]*w:styleId="Caption"[^>]*>)(.*?)(</w:style>)',
                 lambda m: m.group(1) +
                 '<w:name w:val="Caption"/><w:basedOn w:val="Normal"/>'
                 '<w:pPr><w:jc w:val="center"/><w:keepLines/>'
                 '<w:spacing w:before="0" w:after="240" w:line="240" '
                 'w:lineRule="auto"/></w:pPr>'
                 '<w:rPr>' + TIMES + '<w:i/><w:sz w:val="21"/>'
                 '<w:szCs w:val="21"/></w:rPr>' + m.group(3),
                 xml, count=1, flags=re.S)

    # ---- code blocks: wrap them, or long lines run off the page ------------
    xml = re.sub(r'<w:style [^>]*w:styleId="SourceCode"[^>]*>.*?</w:style>',
                 '<w:style w:type="paragraph" w:customStyle="1" '
                 'w:styleId="SourceCode"><w:name w:val="Source Code"/>'
                 '<w:basedOn w:val="Normal"/><w:link w:val="VerbatimChar"/>'
                 '<w:pPr><w:wordWrap w:val="on"/><w:jc w:val="left"/>'
                 '<w:spacing w:before="120" w:after="120" w:line="240" '
                 'w:lineRule="auto"/><w:ind w:left="180" w:right="180"/>'
                 '<w:pBdr><w:top w:val="single" w:sz="4" w:color="999999"/>'
                 '<w:left w:val="single" w:sz="4" w:color="999999"/>'
                 '<w:bottom w:val="single" w:sz="4" w:color="999999"/>'
                 '<w:right w:val="single" w:sz="4" w:color="999999"/></w:pBdr>'
                 '<w:shd w:val="clear" w:fill="F5F5F5"/></w:pPr>'
                 '<w:rPr><w:rFonts w:ascii="Courier New" w:hAnsi="Courier New"/>'
                 '<w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr></w:style>',
                 xml, count=1, flags=re.S)

    # ---- tables: real grid lines and a shaded header row -------------------
    xml = re.sub(r'<w:style [^>]*w:styleId="Table"[^>]*w:type="table"[^>]*>.*?</w:style>',
                 '<w:style w:default="1" w:styleId="Table" w:type="table">'
                 '<w:name w:val="Table"/><w:basedOn w:val="TableNormal"/><w:qFormat/>'
                 '<w:pPr><w:spacing w:before="40" w:after="40" w:line="240" '
                 'w:lineRule="auto"/><w:jc w:val="left"/></w:pPr>'
                 '<w:rPr><w:sz w:val="21"/><w:szCs w:val="21"/></w:rPr>'
                 '<w:tblPr><w:tblInd w:type="dxa" w:w="0"/>'
                 '<w:tblBorders>'
                 '<w:top w:val="single" w:sz="4" w:color="000000"/>'
                 '<w:left w:val="single" w:sz="4" w:color="000000"/>'
                 '<w:bottom w:val="single" w:sz="4" w:color="000000"/>'
                 '<w:right w:val="single" w:sz="4" w:color="000000"/>'
                 '<w:insideH w:val="single" w:sz="4" w:color="000000"/>'
                 '<w:insideV w:val="single" w:sz="4" w:color="000000"/>'
                 '</w:tblBorders>'
                 '<w:tblCellMar><w:top w:type="dxa" w:w="40"/>'
                 '<w:left w:type="dxa" w:w="108"/>'
                 '<w:bottom w:type="dxa" w:w="40"/>'
                 '<w:right w:type="dxa" w:w="108"/></w:tblCellMar></w:tblPr>'
                 '<w:tblStylePr w:type="firstRow"><w:rPr><w:b/></w:rPr>'
                 '<w:tcPr><w:shd w:val="clear" w:fill="E8E8E8"/></w:tcPr>'
                 '</w:tblStylePr></w:style>',
                 xml, count=1, flags=re.S)

    # ---- the contents heading must not list itself, so give it level 9 -----
    xml = re.sub(r'(<w:style [^>]*w:styleId="TOCHeading"[^>]*>)(.*?)(</w:style>)',
                 lambda m: m.group(1) +
                 '<w:name w:val="TOC Heading"/><w:basedOn w:val="Normal"/>'
                 '<w:next w:val="BodyText"/><w:uiPriority w:val="39"/><w:qFormat/>'
                 '<w:pPr><w:keepNext/><w:jc w:val="center"/>'
                 '<w:spacing w:before="0" w:after="240" w:line="240" '
                 'w:lineRule="auto"/><w:outlineLvl w:val="9"/></w:pPr>'
                 '<w:rPr>' + TIMES + '<w:b/><w:color w:val="000000"/>'
                 '<w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr>' + m.group(3),
                 xml, count=1, flags=re.S)

    # ---- styles Word needs that pandoc's reference file does not have ------
    extra = (
        # contents list: single spaced, page number on the right with dots
        '<w:style w:type="paragraph" w:styleId="TOC1"><w:name w:val="toc 1"/>'
        '<w:basedOn w:val="Normal"/><w:uiPriority w:val="39"/>'
        '<w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="8306"/></w:tabs>'
        '<w:spacing w:before="120" w:after="0" w:line="240" w:lineRule="auto"/>'
        '</w:pPr><w:rPr><w:b/></w:rPr></w:style>'
        '<w:style w:type="paragraph" w:styleId="TOC2"><w:name w:val="toc 2"/>'
        '<w:basedOn w:val="Normal"/><w:uiPriority w:val="39"/>'
        '<w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="8306"/></w:tabs>'
        '<w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>'
        '<w:ind w:left="284"/></w:pPr></w:style>'
        # page number at the foot of the page
        '<w:style w:type="paragraph" w:styleId="Footer"><w:name w:val="footer"/>'
        '<w:basedOn w:val="Normal"/><w:pPr>'
        '<w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>'
        '<w:jc w:val="right"/></w:pPr>'
        '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr></w:style>'
        # the cover page lines
        '<w:style w:type="paragraph" w:customStyle="1" w:styleId="CoverText">'
        '<w:name w:val="Cover Text"/><w:basedOn w:val="Normal"/><w:pPr>'
        '<w:spacing w:before="0" w:after="120" w:line="240" w:lineRule="auto"/>'
        '<w:jc w:val="center"/></w:pPr></w:style>'
    )
    return xml.replace("</w:styles>", extra + "</w:styles>")


# ============================================================================
# 3. convert
# ============================================================================
# The styles are fixed on the finished file, not on a reference document.
# Pandoc writes some styles of its own into the output, so a reference document
# never reaches them: the tables kept losing their grid lines that way.
subprocess.run([PANDOC, MD, "-o", DOCX,
                "--toc", "--toc-depth=2",
                "--resource-path", HERE],
               check=True, cwd=HERE)
print("pandoc conversion done")


# ============================================================================
# 4. patch the finished document
# ============================================================================

def cover_p(text, size=24, bold=False, italic=False, before=0, after=120):
    """One centred line of the cover page. size is in half-points."""
    rpr = TIMES + '<w:sz w:val="%d"/><w:szCs w:val="%d"/>' % (size, size)
    if bold:
        rpr += '<w:b/>'
    if italic:
        rpr += '<w:i/>'
    return ('<w:p><w:pPr><w:pStyle w:val="CoverText"/>'
            '<w:spacing w:before="%d" w:after="%d" w:line="240" '
            'w:lineRule="auto"/><w:jc w:val="center"/></w:pPr>'
            '<w:r><w:rPr>%s</w:rPr><w:t xml:space="preserve">%s</w:t></w:r></w:p>'
            % (before, after, rpr, text))


def cover_rule():
    """A thin line across the page, used to frame the title."""
    return ('<w:p><w:pPr><w:pStyle w:val="CoverText"/>'
            '<w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>'
            '<w:pBdr><w:bottom w:val="single" w:sz="6" w:color="000000"/></w:pBdr>'
            '</w:pPr></w:p>')


# The cover is its own section, so it carries no page number.
COVER_SECT = '<w:sectPr>' + PAGE + '</w:sectPr>'

COVER = (
    cover_p(UNIVERSITY, size=28, bold=True, before=1700, after=80)
    + cover_p(CAMPUS, size=24, after=80)
    + cover_p(PROGRAMME, size=24, after=1100)
    + cover_p(MODULE, size=26, bold=True, after=80)
    + cover_p(ASSESSMENT, size=24, italic=True, after=900)
    + cover_rule()
    + cover_p(TITLE, size=40, bold=True, before=320, after=120)
    + cover_p(SUBTITLE, size=28, after=320)
    + cover_rule()
    + cover_p("Submitted by", size=22, italic=True, before=1100, after=60)
    + cover_p(STUDENT, size=26, bold=True, after=60)
    + cover_p("Student ID: " + STUDENT_ID, size=24, after=60)
    + cover_p("Repository: " + REPO, size=20, after=900)
    + cover_p(DATE, size=24, after=0)
    # the last paragraph of a section carries that section's page setup
    + '<w:p><w:pPr><w:pStyle w:val="CoverText"/>'
      '<w:spacing w:before="0" w:after="0"/>' + COVER_SECT + '</w:pPr></w:p>'
)

# The footer: "Page N", bottom right.
FOOTER_RID = "rIdFooterPage"
FOOTER_XML = (
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    '<w:ftr ' + W_NS + '>'
    '<w:p><w:pPr><w:pStyle w:val="Footer"/><w:jc w:val="right"/></w:pPr>'
    '<w:r><w:rPr>' + TIMES + '<w:sz w:val="20"/></w:rPr>'
    '<w:t xml:space="preserve">Page </w:t></w:r>'
    '<w:r><w:fldChar w:fldCharType="begin"/></w:r>'
    '<w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r>'
    '<w:r><w:fldChar w:fldCharType="separate"/></w:r>'
    '<w:r><w:rPr>' + TIMES + '<w:sz w:val="20"/></w:rPr><w:t>1</w:t></w:r>'
    '<w:r><w:fldChar w:fldCharType="end"/></w:r>'
    '</w:p></w:ftr>'
)


def scale_pictures(xml):
    """Shrink any picture wider or taller than the printable page.

    Every figure came out 14.82 cm wide against a 14.65 cm column, and one was
    32 cm tall, so all of them spilled past the margin. The size is stored twice
    per picture (wp:extent and a:ext) and both copies must match.
    """
    fixed = [0]

    def per_drawing(m):
        d = m.group(0)
        e = re.search(r'<wp:extent cx="(\d+)" cy="(\d+)"', d)
        if not e:
            return d
        cx, cy = int(e.group(1)), int(e.group(2))
        f = min(1.0, MAX_W / cx, MAX_H / cy)
        if f >= 0.999:
            return d
        fixed[0] += 1
        nx, ny = int(cx * f), int(cy * f)
        d = re.sub(r'(<wp:extent )cx="\d+" cy="\d+"',
                   r'\g<1>cx="%d" cy="%d"' % (nx, ny), d, count=1)
        d = re.sub(r'(<a:ext )cx="\d+" cy="\d+"',
                   r'\g<1>cx="%d" cy="%d"' % (nx, ny), d)
        return d

    xml = re.sub(r'<w:drawing>.*?</w:drawing>', per_drawing, xml, flags=re.S)
    print("figures resized to fit the page: %d" % fixed[0])
    return xml


def tidy_tables(xml):
    """Single spacing and left alignment inside table cells.

    Cell text uses the Compact paragraph style, which the body needs justified
    at 1.5 spacing. In a table that reads far too loose, and a paragraph style
    beats a table style in Word, so the cells are set directly instead.
    """
    cell_pr = ('<w:spacing w:before="20" w:after="20" w:line="240" '
               'w:lineRule="auto"/><w:jc w:val="left"/>')

    def per_table(m):
        t = m.group(0)
        # Word wants spacing and alignment at the end of the properties block.
        t = t.replace('</w:pPr>', cell_pr + '</w:pPr>')
        t = re.sub(r'<w:pPr\s*/>', '<w:pPr>' + cell_pr + '</w:pPr>', t)
        # a paragraph that carries no properties at all
        t = t.replace('<w:p><w:r>', '<w:p><w:pPr>' + cell_pr + '</w:pPr><w:r>')
        return t

    return re.sub(r'<w:tbl>.*?</w:tbl>', per_table, xml, flags=re.S)


def patch_document(xml):
    # -- the cover goes above everything; the contents list follows it -------
    xml = xml.replace("<w:body>", "<w:body>" + COVER, 1)

    # -- table cells read better tight ---------------------------------------
    xml = tidy_tables(xml)

    # -- each main section starts on a fresh page ----------------------------
    breaks = xml.count('<w:pStyle w:val="Heading1" />')
    xml = xml.replace('<w:pPr><w:pStyle w:val="Heading1" />',
                      '<w:pPr><w:pStyle w:val="Heading1" /><w:pageBreakBefore/>')
    print("main sections starting on a new page: %d" % breaks)

    # -- page size, margins and page numbers ---------------------------------
    # Clear what pandoc left, then set the body section, which is the last one.
    xml = re.sub(r'<w:pgSz[^>]*/>|<w:pgMar[^>]*/>', '', xml)
    main_sect = ('<w:footerReference w:type="default" r:id="%s"/>' % FOOTER_RID
                 + PAGE + '<w:pgNumType w:start="1"/>')
    i = xml.rfind("</w:sectPr>")
    xml = xml[:i] + main_sect + xml[i:]
    # the cover section lost its page setup in the clear above, so put it back
    xml = xml.replace('<w:sectPr></w:sectPr>', COVER_SECT, 1)
    return scale_pictures(xml)


tmp = DOCX + ".tmp"
with zipfile.ZipFile(DOCX) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "word/document.xml":
            data = patch_document(data.decode("utf-8")).encode("utf-8")
        elif item.filename == "word/styles.xml":
            data = restyle(data.decode("utf-8")).encode("utf-8")
        elif item.filename == "word/_rels/document.xml.rels":
            x = data.decode("utf-8")
            rel = ('<Relationship Type="http://schemas.openxmlformats.org/'
                   'officeDocument/2006/relationships/footer" Id="%s" '
                   'Target="footer1.xml" />' % FOOTER_RID)
            data = x.replace("</Relationships>",
                             rel + "</Relationships>").encode("utf-8")
        elif item.filename == "[Content_Types].xml":
            x = data.decode("utf-8")
            ov = ('<Override PartName="/word/footer1.xml" ContentType="application/'
                  'vnd.openxmlformats-officedocument.wordprocessingml.footer+xml" />')
            data = x.replace("</Types>", ov + "</Types>").encode("utf-8")
        elif item.filename == "word/settings.xml":
            # tells Word to fill the contents list page numbers when it opens
            x = data.decode("utf-8")
            if "updateFields" not in x:
                x = x.replace("<w:rsids>",
                              '<w:updateFields w:val="true"/><w:rsids>', 1)
            data = x.encode("utf-8")
        zout.writestr(item, data)
    zout.writestr("word/footer1.xml", FOOTER_XML.encode("utf-8"))
os.replace(tmp, DOCX)
print("cover page, page numbers and section breaks applied")

os.remove(MD)

print("\n%s" % os.path.basename(DOCX))
print("%.1f MB" % (os.path.getsize(DOCX) / 1048576))
print("\nWord fills the contents list in when the file opens.")
print("If it ever looks stale, click it and press F9.")
