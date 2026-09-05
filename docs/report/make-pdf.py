"""
Exports the finished .docx to the PDF that gets submitted.

The .docx is the master. Word opens it, fills in the contents list page
numbers, and prints to PDF, so the two files can never disagree.

Word is only asked to open and print here, which takes seconds. It was the
importing of HTML that used to hang, and that no longer happens.

    python make-pdf.py

Needs pywin32:  python -m pip install pywin32
"""
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
DOCX = os.path.join(HERE, "st20360306 CIS6003 WRIT1.docx")
PDF  = os.path.join(HERE, "st20360306 CIS6003 WRIT1.pdf")

WD_FORMAT_PDF = 17

if not os.path.exists(DOCX):
    sys.exit("%s not found — run build-docx.py first" % os.path.basename(DOCX))

try:
    import win32com.client as com
except ImportError:
    sys.exit("pywin32 not installed — run: python -m pip install pywin32")

word = com.DispatchEx("Word.Application")
word.Visible = False
word.DisplayAlerts = 0
try:
    doc = word.Documents.Open(DOCX, ConfirmConversions=False, ReadOnly=True,
                              AddToRecentFiles=False)
    try:
        # fill in the contents list, then let the page numbers settle
        if doc.TablesOfContents.Count:
            doc.TablesOfContents(1).Update()
        doc.Fields.Update()
        doc.Repaginate()
        pages = doc.ComputeStatistics(2)   # wdStatisticPages
        words = doc.ComputeStatistics(0)   # wdStatisticWords
        doc.SaveAs2(PDF, FileFormat=WD_FORMAT_PDF)
    finally:
        doc.Close(False)
finally:
    word.Quit()

print("%s" % os.path.basename(PDF))
print("%d pages, %.1f MB" % (pages, os.path.getsize(PDF) / 1048576))
print("Word counts %d words for the whole file, including the references and" % words)
print("appendices. The 4,000 limit excludes those; make-final.py reports the")
print("number that the limit actually applies to.")
