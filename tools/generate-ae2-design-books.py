"""Create visual review books from real, completed native AE2 atlas captures."""
from collections import defaultdict
import argparse
from pathlib import Path
from io import BytesIO
import csv
import hashlib
import json
import zipfile

from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase.pdfmetrics import stringWidth

ROOT = Path(__file__).resolve().parents[1]
ATLAS = ROOT / "build/gameplay-qa/ae2-atlas"
OUT = ROOT / "output/pdf"
TMP = ROOT / "tmp/pdfs/ae2-design-books"
WIDTH, HEIGHT = landscape(A4)
INK = colors.HexColor("#172A3A")
MUTED = colors.HexColor("#546575")
PAPER = colors.HexColor("#F0F3F6")
ACCENT = colors.HexColor("#087C86")


def load(target):
    folder = ATLAS / target / "atlas"
    report = json.loads((folder.parent / "results.json").read_text())
    assert all(r["status"] == "PASS" for r in report["results"]), target
    rows = list(csv.DictReader((folder / "manifest.csv").open()))
    assert len(rows) == len({r["label"] for r in rows})
    return folder, rows


def geometry(folder, row):
    value = json.loads((folder / row["file"].replace(".png", ".geometry.json")).read_text())
    return {k.casefold(): v for k, v in value.items()}


def controls(folder, row):
    values = json.loads((folder / row["file"].replace(".png", ".controls.json")).read_text())
    return [{**v, "x": v["x"] - int(row["x"]), "y": v["y"] - int(row["y"])} for v in values]


def verify_groups():
    groups = [["1.20.1-fabric", "1.20.1-forge"],
              ["1.21.1-neoforge", "26.1.2-neoforge", "26.2-neoforge", "26.3-neoforge"]]
    evidence = []
    for group in groups:
        ref_folder, ref_rows = load(group[-1])
        for target in group[:-1]:
            folder, rows = load(target)
            received = {r["label"]: r for r in rows}
            assert set(received) == {r["label"] for r in ref_rows}
            assert all(geometry(ref_folder, r) == geometry(folder, received[r["label"]]) for r in ref_rows)
            assert all(controls(ref_folder, r) == controls(folder, received[r["label"]]) for r in ref_rows)
            evidence.append({"target": target, "representative": group[-1],
                             "sameLayoutPresentations": len(rows), "tooltipCaseNormalized": True})
    return evidence


def title(label):
    words = {"me": "ME", "cpu": "CPU", "io": "I/O", "qnb": "QNB"}
    return " ".join(words.get(w, w.capitalize()) for w in label.split("-"))


def ui_crop(folder, row):
    with Image.open(folder / row["file"]) as source:
        source = source.convert("RGB")
        scale = source.width / int(row["guiWidth"])
        # Include the complete visual envelope, native toolbar, and companion panels.
        x, y, w, h = [int(row["visual" + k]) for k in ["X", "Y", "Width", "Height"]]
        bbox = (max(0, round((x - 3) * scale)), max(0, round((y - 3) * scale)),
                min(source.width, round((x + w + 3) * scale)),
                min(source.height, round((y + h + 3) * scale)))
        return source.crop(bbox)


def line(c, text, x, y, size=10, color=INK, bold=False):
    c.setFont("Helvetica-Bold" if bold else "Helvetica", size)
    c.setFillColor(color)
    c.drawString(x, y, text)


def wrapped(c, text, x, y, max_width, size=10, leading=15, color=MUTED):
    words = text.split()
    lines, current = [], ""
    for word in words:
        candidate = (current + " " + word).strip()
        if stringWidth(candidate, "Helvetica", size) > max_width and current:
            lines.append(current)
            current = word
        else:
            current = candidate
    lines.append(current)
    for value in lines:
        line(c, value, x, y, size, color)
        y -= leading
    return y


def image_fit(c, im, x, y, w, h, max_scale=2.5):
    factor = min(w / im.width, h / im.height, max_scale)
    iw, ih = im.width * factor, im.height * factor
    # Embed integer nearest-neighbor enlargement to keep Minecraft pixels sharp.
    sharp = im.resize((im.width * 3, im.height * 3), Image.Resampling.NEAREST)
    c.drawImage(ImageReader(sharp), x + (w - iw) / 2, y + (h - ih) / 2,
                width=iw, height=ih)


def footer(c, family, page, total):
    c.setStrokeColor(colors.HexColor("#D3DCE3"))
    c.line(36, 37, WIDTH - 36, 37)
    line(c, "AE2 desktop design review | " + family, 36, 23, 8, MUTED)
    c.setFont("Helvetica", 8)
    c.drawRightString(WIDTH - 36, 23, f"{page} / {total}")


def cover(c, family, rows, folder, targets, count, total):
    line(c, "SALT'S INVENTORY COMPATIBILITY", 36, HEIGHT - 43, 10, ACCENT, True)
    line(c, family + " design review", 36, HEIGHT - 88, 30, INK, True)
    line(c, targets, 36, HEIGHT - 117, 13)
    wrapped(c, f"{count} captured presentations. One enlarged view per page, with a clickable "
            "40-container index and PDF bookmarks. Screenshots come from the real Minecraft "
            "client running Core, the extension and unmodified upstream AE2.",
            36, HEIGHT - 151, WIDTH - 72, 11, 16)
    if family == "Legacy":
        note = ("AE2 15.4.11 / Minecraft 1.20.1. Fabric and Forge share all 76 captured layouts. "
                "This book uses the fresh Forge release-activation captures. It retains AE2 15's "
                "native artwork, combined chest menu and larger sky chest. Updated captures show "
                "the corrected bottom corners, pattern mode tabs and crafting CPU panel join.")
    else:
        note = ("AE2 19 and 26 share all 90 captured container layouts. AE2 26.3 is the main "
                "representative. The appendix includes actual 1.21.1 screenshots and small "
                "upstream artwork variations, so those differences remain visible without "
                "duplicating the entire atlas.")
    wrapped(c, note, 36, HEIGHT - 208, WIDTH - 72, 10, 15)
    labels = ["me-drive", "pattern-encoding-terminal", "crafting-status"]
    cards_y, cards_h, gap = 126, 190, 16
    cards_w = (WIDTH - 72 - gap * 2) / 3
    for i, label in enumerate(labels):
        row = next(r for r in rows if r["label"] == label)
        x = 36 + i * (cards_w + gap)
        c.setFillColor(PAPER)
        c.roundRect(x, cards_y, cards_w, cards_h, 6, fill=1, stroke=0)
        line(c, title(label), x + 10, cards_y + cards_h - 18, 9, INK, True)
        image_fit(c, ui_crop(folder, row), x + 8, cards_y + 10, cards_w - 16, cards_h - 38, 1)
    wrapped(c, "Review the title/control row, native slot housings, tabs, search fields, scrollbars "
            "and toolbox companions. Crops include the full visible UI; gaps can contain the "
            "world background. Populated values and status variants are disposable QA fixtures.",
            36, 101, WIDTH - 72, 9, 13)
    footer(c, family, 1, total)
    c.showPage()


def index_page(c, family, groups, first_pages, total):
    c.bookmarkPage("index")
    c.addOutlineEntry("Container index", "index", level=0)
    line(c, "Container index", 36, HEIGHT - 54, 25, INK, True)
    line(c, "Click a row to open that container. Its state variants follow immediately.",
         36, HEIGHT - 80, 10, MUTED)
    columns = (WIDTH - 92) / 2
    for i, (key, entries) in enumerate(groups.items()):
        col, row = divmod(i, 20)
        x, y = 36 + col * (columns + 20), HEIGHT - 112 - row * 20
        label = f"{int(key):02d}  {title(entries[0]['label'])}"
        while stringWidth(label, "Helvetica", 9) > columns - 68:
            label = label[:-2].rstrip() + "."
        line(c, label, x, y, 9)
        line(c, str(len(entries)) + (" view" if len(entries) == 1 else " views"), x + columns - 62, y, 8, MUTED)
        c.setFont("Helvetica-Bold", 9)
        c.drawRightString(x + columns, y, str(first_pages[key]))
        c.linkRect("", "view-" + entries[0]["label"], (x, y - 4, x + columns, y + 11), thickness=0)
    footer(c, family, 2, total)
    c.showPage()


def screenshot_page(c, family, folder, row, page, total, key, variant, appendix=False, note=None):
    bookmark = ("appendix-" if appendix else "view-") + row["label"]
    c.bookmarkPage(bookmark)
    c.addOutlineEntry(title(row["label"]), bookmark, level=1 if variant or appendix else 0)
    line(c, "AE2 19 / MINECRAFT 1.21.1 VARIATION" if appendix else f"CONTAINER {int(key):02d} / {family.upper()}",
         36, HEIGHT - 36, 9, ACCENT, True)
    heading = title(row["label"])
    size = min(22, (WIDTH - 72) / max(1, stringWidth(heading, "Helvetica-Bold", 1)))
    line(c, heading, 36, HEIGHT - 67, size, INK, True)
    line(c, row["menu"] + "  |  Native body " + row["bodyWidth"] + " x " + row["bodyHeight"]
         + " GUI units  |  " + ("1.21.1 NeoForge" if appendix else folder.parent.name),
         36, HEIGHT - 88, 9, MUTED)
    card_x, card_y, card_w, card_h = 36, 65, WIDTH - 72, HEIGHT - 169
    c.setFillColor(PAPER)
    c.roundRect(card_x, card_y, card_w, card_h, 6, fill=1, stroke=0)
    image_fit(c, ui_crop(folder, row), card_x + 12, card_y + 10, card_w - 24, card_h - 20)
    line(c, "Back to container index", WIDTH - 155, HEIGHT - 36, 8, MUTED)
    c.linkRect("", "index", (WIDTH - 158, HEIGHT - 40, WIDTH - 36, HEIGHT - 27), thickness=0)
    if note:
        line(c, note, 36, 51, 8, MUTED)
    footer(c, family, page, total)
    c.showPage()


def native_image(jar, resource, rect=None):
    with zipfile.ZipFile(ROOT / "build/ae2-port" / jar) as z:
        im = Image.open(BytesIO(z.read(resource))).convert("RGBA")
    if rect:
        im = im.crop(rect)
    bg = Image.new("RGBA", im.size, (231, 235, 240, 255))
    bg.alpha_composite(im)
    return bg.convert("RGB")


def art_page(c, page, total):
    c.bookmarkPage("native-art-variations")
    c.addOutlineEntry("Small native artwork differences", "native-art-variations", level=1)
    line(c, "MODERN FAMILY / NATIVE ARTWORK", 36, HEIGHT - 38, 9, ACCENT, True)
    line(c, "AE2 19 and 26: small artwork variations", 36, HEIGHT - 70, 23, INK, True)
    wrapped(c, "Container geometry is shared. These excerpts show the upstream art that differs: "
            "the suspended-status sprite, memory-card hash overlay and recipe-viewer additions. These "
            "are native asset samples, not fabricated in-game screenshots. Transparent areas "
            "are composited on light gray for inspection.", 36, HEIGHT - 100, WIDTH - 72, 10, 15)
    rows = [("Suspended status", "assets/ae2/textures/guis/states.png", (192, 244, 202, 254), 70),
            ("Memory-card hash overlay", "assets/ae2/textures/item/memory_card_hash.png", None, 70),
            ("Recipe-viewer art", "assets/ae2/textures/guis/jei.png", (0, 0, 90, 80), 140)]
    y = HEIGHT - 199
    for label, resource, rect, h in rows:
        line(c, label, 36, y + 6, 11, INK, True)
        for i, (version, jar) in enumerate([("AE2 19 / 1.21.1", "appliedenergistics2-19.2.18.jar"),
                                           ("AE2 26 / 26.x", "appliedenergistics2-26.3.1-alpha.jar")]):
            x = 248 + i * 285
            line(c, version, x, y + 6, 9, MUTED)
            image_fit(c, native_image(jar, resource, rect), x, y - h, 240, h - 6, 8)
        y -= h + 30
    footer(c, "Modern", page, total)
    c.showPage()


def create(family, target, targets, output_name, evidence):
    folder, rows = load(target)
    groups = defaultdict(list)
    for row in rows:
        groups[row["index"]].append(row)
    assert len(groups) == 40
    appendix = []
    if family == "Modern":
        old_folder, old_rows = load("1.21.1-neoforge")
        appendix_labels = ["network-tool", "crafting-status", "crafting-cpu"]
        for label in appendix_labels:
            if label == "crafting-cpu":
                focused_folder, focused_rows = load("1.21.1-neoforge-focus-craftingcpu-lifecycle")
                appendix.append((focused_folder, next(r for r in focused_rows if r["label"] == label)))
            else:
                appendix.append((old_folder, next(r for r in old_rows if r["label"] == label)))
    total = 2 + len(rows) + len(appendix) + (1 if appendix else 0)
    destination = OUT / output_name
    c = canvas.Canvas(str(destination), pagesize=(WIDTH, HEIGHT), pageCompression=1)
    c.setTitle(f"AE2 {family} Desktop Design Review")
    c.setAuthor("Salt's Inventory Update")
    c.setSubject("Native AE2 UI screenshots, grouped by design rather than Minecraft release")
    c.bookmarkPage("cover")
    c.addOutlineEntry("Design family and review guide", "cover", level=0)
    cover(c, family, rows, folder, targets, len(rows), total)
    first_pages, page = {}, 3
    for key, entries in groups.items():
        first_pages[key] = page
        page += len(entries)
    index_page(c, family, groups, first_pages, total)
    records = []
    for page, row in enumerate(rows, 3):
        variant = row is not groups[row["index"]][0]
        source_row = row
        note = None
        if row["label"] == "wireless-terminal":
            source_row = next(r for r in rows if r["label"] == "wireless-terminal-settings-return")
            assert geometry(folder, row) == geometry(folder, source_row)
            assert controls(folder, row) == controls(folder, source_row)
            note = "Matching return capture used to avoid a temporary Minecraft recipe notification."
        displayed_row = {**source_row, "label": row["label"], "index": row["index"]}
        screenshot_page(c, family, folder, displayed_row, page, total, row["index"], variant, note=note)
        records.append({"page": page, "label": row["label"],
                        "sourcePresentation": source_row["label"],
                        "source": str(folder / source_row["file"]),
                        "sha256": hashlib.sha256((folder / source_row["file"]).read_bytes()).hexdigest()})
    if appendix:
        c.addOutlineEntry("AE2 19 native variations", "appendix-" + appendix[0][1]["label"], level=0)
        for page, (old_folder, row) in enumerate(appendix, 3 + len(rows)):
            screenshot_page(c, family, old_folder, row, page, total, row["index"], True, True)
            records.append({"page": page, "label": row["label"], "source": str(old_folder / row["file"]),
                            "sha256": hashlib.sha256((old_folder / row["file"]).read_bytes()).hexdigest()})
        art_page(c, total, total)
    c.save()
    evidence[family] = {"pdf": str(destination), "pages": total, "atlasViews": len(rows),
                        "screenshots": records, "coveredTargets": targets}
    print(f"Created {destination.name}: {total} pages / {len(rows)} atlas presentations")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--family', choices=['all', 'legacy', 'modern'], default='all')
    parser.add_argument('--output-name', help='Override the PDF filename when creating a single family')
    args = parser.parse_args()
    if args.output_name and (args.family == 'all' or Path(args.output_name).name != args.output_name
                            or not args.output_name.endswith('.pdf')):
        parser.error('--output-name requires one family and a plain PDF filename')
    OUT.mkdir(parents=True, exist_ok=True)
    TMP.mkdir(parents=True, exist_ok=True)
    evidence = {"grouping": verify_groups()}
    if args.family in ['all', 'legacy']:
        create("Legacy", "1.20.1-forge", "Minecraft 1.20.1 / Fabric and Forge / AE2 15.4.11",
               args.output_name or "ae2-legacy-design-review.pdf", evidence)
    if args.family in ['all', 'modern']:
        create("Modern", "26.3-neoforge", "Minecraft 1.21.1, 26.1.2, 26.2 and 26.3 / NeoForge",
               args.output_name or "ae2-modern-design-review.pdf", evidence)
    (TMP / "source-manifest.json").write_text(json.dumps(evidence, indent=2) + "\n")


if __name__ == "__main__":
    main()
