"""Five concise, real-capture comparisons for the reported window artifacts."""
from pathlib import Path
import hashlib
import importlib.util
import json
import sys
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A3, landscape
from reportlab.lib.utils import ImageReader

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from rollout_capture_provenance import same_ui_binary

spec = importlib.util.spec_from_file_location('rollout_pdf_helpers', ROOT / 'tools/build-rollout-ui-pdf.py')
atlas = importlib.util.module_from_spec(spec)
spec.loader.exec_module(atlas)
WORK = ROOT / 'build/window-artifact-fixes/pdf'
WORK.mkdir(parents=True, exist_ok=True)
OUT = ROOT / 'output/pdf/siu-window-artifact-fixes.pdf'
W, H = landscape(A3)
M, GAP = 42, 24
PW = (W - M * 2 - GAP) / 2
INK, TEAL, GRAY, PALE = '#183D40', '#247B80', '#627276', '#EDF4F4'

CASES = [
    dict(ui='ae2-craft-confirm', title='Crafting Plan', publisher='Applied Energistics 2',
         original='native-ae2-craft-confirm-1201-fabric-457ee880',
         fix='The original three-column, five-row table is restored. Its lower edge stays above the CPU selector, storage information and action buttons.',
         detail='The table no longer continues through the controls below it.',
         ports='1.20.1 Fabric / Forge; 1.21.1, 26.1.2, 26.2 and 26.3 NeoForge.'),
    dict(ui='ae2-drive', title='ME Drive', publisher='Applied Energistics 2',
         original='native-ae2-drive-1201-fabric-eab892d0',
         fix='Slots now inherit the exact positions from AE2. The empty-cell illustrations align with their slot borders, and SIU controls clear the top row.',
         detail='Enlarged detail: empty-cell illustrations are centered in the native slots.',
         ports='1.20.1 Fabric / Forge; 1.21.1, 26.1.2, 26.2 and 26.3 NeoForge.'),
    dict(ui='ae2-portable-item-cell', title='Portable Cell / catalog footer', publisher='Applied Energistics 2',
         original='native-ae2-portable-item-cell-1201-fabric-900d7673',
         fix='The closing edge now uses the footer space supplied by each AE2 texture. It no longer paints over the bottom slot-border pixels in the shared catalog frame.',
         detail='Enlarged detail: the entire bottom border of the last slot row remains visible.',
         ports='Shared catalog frame: 1.20.1 Fabric / Forge; 1.21.1, 26.1.2, 26.2 and 26.3 NeoForge.'),
    dict(ui='alloy-forge', title='Alloy Forge', publisher='Alloy Forgery',
         original='native-alloy-forge-1201-fabric-bd687d21',
         fix='The complete reservoir and both side supports are extracted above the title bar. The crop excludes the main panel seam, and the hosted backdrop no longer darkens its transparent surround.',
         detail='Enlarged detail: clean reservoir supports with transparent sky around them.',
         ports='1.20.1, 1.21.1 and 26.1.2 Fabric.'),
    dict(ui='create-ammo', title='Autocannon Ammo Container', publisher='Create Big Cannons',
         original='native-create-ammo-1201-fabric-a750c65f',
         fix='The header now starts at the correct atlas row. The detached line above the title bar is gone; the native panel, confirmation button and model are retained.',
         detail='Enlarged detail: a single, continuous gold header with no floating line.',
         ports='1.20.1 Fabric / Forge; 1.21.1 NeoForge.'),
]


def text(c, value, x, y, size=12, color=INK, bold=False):
    c.setFillColor(colors.HexColor(color))
    c.setFont('Helvetica-Bold' if bold else 'Helvetica', size)
    c.drawString(x, y, value)


def paragraph(c, value, x, y, width, size=12, leading=18):
    line = ''
    for word in value.split():
        trial = (line + ' ' + word).strip()
        if c.stringWidth(trial, 'Helvetica', size) > width:
            text(c, line, x, y, size)
            y -= leading
            line = word
        else:
            line = trial
    if line:
        text(c, line, x, y, size)
        y -= leading
    return y


def draw_image(c, image, box, scale=None):
    x, y, w, h = box
    scale = min(w / image.width, h / image.height) if scale is None else scale
    dw, dh = image.width * scale, image.height * scale
    assert dw <= w + 0.1 and dh <= h + 0.1
    c.drawImage(ImageReader(image), x + (w - dw) / 2, y + (h - dh) / 2,
                width=dw, height=dh, mask='auto')


def window(row):
    state = row['state']
    return state.get('allWindows', state.get('windows', []))[0]['bounds']


def detail_image(row):
    b = window(row)
    x, y, w, h = b['x'], b['y'], b['width'], b['height']
    ui = row['ui']
    box = {
        'ae2-craft-confirm': (x + 3, y + h - 65, x + w - 15, y + h),
        'ae2-drive': (x + 69, y + 12, x + 109, y + 49),
        'ae2-portable-item-cell': (x + 4, y + h - 23, x + 170, y + h + 1),
        'alloy-forge': (x + 49, y - 19, x + 127, y + 2),
        'create-ammo': (x - 2, y - 7, x + 171, y + 17),
    }[ui]
    image = Image.open(row['screenshot']).convert('RGB')
    sx, sy = image.width / row['state']['guiWidth'], image.height / row['state']['guiHeight']
    box = tuple(round(v * (sx if i % 2 == 0 else sy)) for i, v in enumerate(box))
    return image.crop(box), box


def main():
    fixed = {r['ui']: r for r in json.loads((WORK / 'selected-fixed-evidence.json').read_text())}
    ledger = json.loads((ROOT / 'build/all-target-rollout/ui-capture-ledger.json').read_text())
    originals = {Path(r['screenshot']).stem: r for r in ledger if r['side'] == 'native'}
    c = canvas.Canvas(str(OUT), pagesize=(W, H), pageCompression=1)
    c.setTitle('SIU - Five window artifact fixes')
    c.setAuthor("Salt's Inventory Update")
    evidence = []
    for page, case in enumerate(CASES, 1):
        sides = dict(native=originals[case['original']], siu=fixed[case['ui']])
        same, provenance = same_ui_binary(sides)
        assert same and provenance['status'] == 'same original UI publisher binary on both sides', provenance
        panels = {}
        row = dict(ui=case['ui'], publisher=case['publisher'], minecraft='1.20.1', loader='fabric',
                   provenance=provenance, sources={})
        for side, capture in sides.items():
            path = Path(capture['screenshot'])
            assert hashlib.sha256(path.read_bytes()).hexdigest() == capture['sha256']
            panels[side], _, crop = atlas.crop(capture, side)
            panels[side].save(WORK / (case['ui'] + '-' + side + '.png'))
            row['sources'][side] = dict(path=str(path), sha256=capture['sha256'], crop=crop, run=capture['run'])
        detail, detail_box = detail_image(sides['siu'])
        detail.save(WORK / (case['ui'] + '-detail.png'))
        row['detail_crop'] = detail_box
        evidence.append(row)

        text(c, 'PROJECT EVERYTHING / WINDOW FIX CONFIRMATION', M, H - 39, 11, TEAL, True)
        text(c, case['title'], M, H - 76, 27, INK, True)
        text(c, case['publisher'] + '  |  Example: Minecraft 1.20.1 / Fabric', M, H - 100, 12, GRAY)
        text(c, 'ORIGINAL UI / SIU ABSENT', M, H - 133, 11, TEAL, True)
        text(c, 'FIXED SIU WINDOW / REAL IN-GAME CAPTURE', M + PW + GAP, H - 133, 11, TEAL, True)
        panel_y, panel_h = 277, 420
        for x in (M, M + PW + GAP):
            c.setFillColor(colors.HexColor(PALE))
            c.roundRect(x, panel_y, PW, panel_h, 8, stroke=0, fill=1)
        # A common scale keeps native and hosted UI proportions comparable.
        scale = min((PW - 20) / max(im.width for im in panels.values()),
                    (panel_h - 20) / max(im.height for im in panels.values()))
        for i, side in enumerate(('native', 'siu')):
            draw_image(c, panels[side], (M + i * (PW + GAP) + 10, panel_y + 10, PW - 20, panel_h - 20), scale)

        paragraph(c, case['fix'], M, 253, W - M * 2, 13, 19)
        text(c, 'FIXED DETAIL', M, 184, 10, TEAL, True)
        # Nearest-neighbor enlargement keeps individual source pixels visible.
        enlarged = detail.resize((detail.width * 4, detail.height * 4), Image.Resampling.NEAREST)
        draw_image(c, enlarged, (M, 76, PW, 94))
        paragraph(c, case['detail'], M + PW + GAP, 162, PW, 13, 19)
        text(c, 'APPLIED TO', M + PW + GAP, 112, 10, TEAL, True)
        paragraph(c, case['ports'], M + PW + GAP, 93, PW, 11, 16)
        c.setStrokeColor(colors.HexColor('#D3E0E0'))
        c.line(M, 51, W - M, 51)
        text(c, 'Real screenshots; same publisher JAR on both sides. Focused menu checks; no full gameplay suite.', M, 33, 9, GRAY)
        c.setFont('Helvetica', 9)
        c.drawRightString(W - M, 33, f'{page} / 5')
        c.showPage()
    c.save()
    (WORK / 'evidence.json').write_text(json.dumps(evidence, indent=2))
    print('Created', OUT, 'with', len(evidence), 'verified real-capture pairs')


if __name__ == '__main__':
    main()
