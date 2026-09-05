"""
Makes document-sized copies of the figures.

The originals are captured at 2880px wide, and some are nearly 4000px tall, so
that they stay sharp. That is right for the repository and wrong for a Word
document: nineteen images that size made Word hang for twenty minutes trying to
repaginate them.

These copies are capped at 1600px wide and 2000px tall, which is still well
above what an A4 page needs at 300dpi, and they are written to doc-images/.
The originals are left untouched.

    python make-doc-images.py
"""
import io, os, re, glob
from PIL import Image

OUT = "doc-images"
MAX_W, MAX_H = 1600, 2000

os.makedirs(OUT, exist_ok=True)

sources = sorted(glob.glob("screenshots/*.png")) + sorted(glob.glob("../uml/png/*.png"))

# Only the pictures the report actually uses. Copying all 35 left 16 files in
# doc-images/ that nothing embeds — the same originals again at a smaller size.
# If _final.md is missing, fall back to copying everything.
if os.path.exists("_final.md"):
    text = io.open("_final.md", encoding="utf-8").read()
    used = {os.path.basename(p)
            for _, _, p in re.findall(r'`\[FIGURE (\d+): (.+?) — (.+?)\]`', text)}
    sources = [s for s in sources if os.path.basename(s) in used]

before = after = 0

for src in sources:
    name = os.path.basename(src)
    dst = os.path.join(OUT, name)
    with Image.open(src) as im:
        w, h = im.size
        scale = min(MAX_W / w, MAX_H / h, 1.0)
        if scale < 1.0:
            im = im.resize((int(w * scale), int(h * scale)), Image.LANCZOS)
        im.save(dst, optimize=True)
    before += os.path.getsize(src)
    after += os.path.getsize(dst)

print("%d images copied into %s/" % (len(sources), OUT))
print("total size %.1f MB -> %.1f MB" % (before / 1048576, after / 1048576))

# Point the final HTML at the smaller copies.
html = "CIS6003-WRIT1-final.html"
if os.path.exists(html):
    s = io.open(html, encoding="utf-8").read()
    s = re.sub(r'src="(?:\.\./uml/png|screenshots)/([^"]+)"', r'src="doc-images/\1"', s)
    # Give Word an explicit width so it does not have to work one out.
    s = s.replace(".figure img { max-width: 100%; border: 1px solid #999; }",
                  ".figure img { width: 13.8cm; border: 1px solid #999; }")
    io.open(html, "w", encoding="utf-8").write(s)
    print("rewrote %s to use doc-images/ at a fixed width" % html)
