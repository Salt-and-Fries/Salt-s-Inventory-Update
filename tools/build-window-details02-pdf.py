"""Five real original/fixed comparisons for the second window-detail review."""
from pathlib import Path
import hashlib
import importlib.util
import json
import sys
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.utils import ImageReader

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from rollout_capture_provenance import same_ui_binary
spec = importlib.util.spec_from_file_location('previous_proof_helpers', ROOT / 'tools/build-window-artifact-fixes-pdf.py')
h = importlib.util.module_from_spec(spec)
spec.loader.exec_module(h)
WORK = ROOT / 'build/window-detail-fixes-02/pdf'
OUT = ROOT / 'output/pdf/siu-window-detail-fixes-02.pdf'
PDF_TITLE = 'SIU - Camera, storage, spells and Sculptor fixes'
STORAGE_PORTS = '1.20.1 Fabric / Forge; 1.21.1 Fabric / NeoForge; 1.21.11, 26.1.2, 26.2 and 26.3 NeoForge.'
CASES = [
    ('exposure-camera', 'Camera', 'Exposure', 'fabric',
     'The camera uses a regular SIU frame instead of the fallback bars. Its attachment guides, connector lines and camera illustration remain intact.',
     'Native slot guides are preserved for the camera attachments.',
     '1.20.1 Fabric / Forge; 1.21.1 Fabric / NeoForge.'),
    ('sophisticated-barrel', 'Barrel', 'Sophisticated Storage', 'fabric',
     'All 27 slots fit with complete borders. Older saved heights that allowed space for rows but omitted the toolbar now gain that missing space.',
     'The full bottom row and its lower borders remain inside the window.', STORAGE_PORTS),
    ('sophisticated-decoration', 'Decoration Table', 'Sophisticated Storage', 'fabric',
     'A dedicated integrated workspace replaces the fallback bars. It retains the native preview, material and color controls, slot guides and large output rim.',
     'The result rim and preview remain complete; the player inventory is excluded.', STORAGE_PORTS),
    ('spell-binding', 'Bind Spells', 'Spell Engine', 'fabric',
     'The crop now follows the native screen draw offset. All three full-height rows and the complete panel border fit, without bringing in player-inventory pixels.',
     'Three complete rows, including the top rim and bottom border.',
     '1.20.1 Fabric / Forge; 1.21.1 Fabric / NeoForge; 26.1.2, 26.2 and 26.3 Fabric / NeoForge.'),
    ('bf-sculptor-clean', 'Begin Test', "Mowzie's Mobs - Sculptor", 'forge',
     'This was a SIU initialization bug. The button now refreshes the original payment and obstruction checks after it is created. Empty or wrong payment leaves it disabled.',
     'Correct payment enables Begin Test. Removing payment disables it again. Starting the test consumes the payment.',
     '1.20.1 Forge; 1.21.1 NeoForge.'),
]


def crop_gui(row, box):
    image = Image.open(row['screenshot']).convert('RGB')
    sx, sy = image.width / row['state']['guiWidth'], image.height / row['state']['guiHeight']
    pixels = tuple(round(v * (sx if i % 2 == 0 else sy)) for i, v in enumerate(box))
    return image.crop(pixels), pixels


def native_crop(row):
    if row['ui'] == 'spell-binding':
        b = row['state']['bounds']
        # The native background starts 18px above imageBounds, as do its title
        # and first row. Include the whole original screen in this comparison.
        return crop_gui(row, (b['x']-8, b['y']-26, b['x']+b['width']+8, b['y']+b['height']+8))
    image, _, box = h.atlas.crop(row, 'native')
    return image, box


def detail(row):
    b = row['state'].get('allWindows', row['state'].get('windows'))[0]['bounds']
    x, y, w, height = b['x'], b['y'], b['width'], b['height']
    box = {
        'exposure-camera': (x+136, y+19, x+159, y+95),
        'sophisticated-barrel': (x+6, y+67, x+w-6, y+height-3),
        'sophisticated-decoration': (x+98, y+111, x+w-6, y+height-6),
        'spell-binding': (x+50, y+20, x+w-7, y+height-6),
        'bf-sculptor-clean': (x+116, y+59, x+174, y+80),
    }[row['ui']]
    return crop_gui(row, box)


def main():
    fixed = {r['ui']: r for r in json.loads((WORK / 'fixed-evidence.json').read_text())}
    rows = json.loads((ROOT / 'build/all-target-rollout/ui-capture-ledger.json').read_text())
    original = {(r['ui'], r['minecraft'], r['loader']): r for r in rows if r['side'] == 'native'}
    c = canvas.Canvas(str(OUT), pagesize=(h.W, h.H), pageCompression=1)
    c.setTitle(PDF_TITLE)
    c.setAuthor("Salt's Inventory Update")
    evidence = []
    for page, (ui, title, mod, loader, note, caption, ports) in enumerate(CASES, 1):
        pair = dict(native=original[ui, fixed[ui]['minecraft'], loader], siu=fixed[ui])
        same, provenance = same_ui_binary(pair)
        assert same and provenance['status'] == 'same original UI publisher binary on both sides', provenance
        panels = {}
        record = dict(ui=ui, provenance=provenance, sources={})
        for side, row in pair.items():
            path = Path(row['screenshot'])
            assert hashlib.sha256(path.read_bytes()).hexdigest() == row['sha256']
            if side == 'native':
                panels[side], box = native_crop(row)
            else:
                panels[side], _, box = h.atlas.crop(row, side)
            panels[side].save(WORK / f'{ui}-{side}.png')
            record['sources'][side] = dict(path=str(path), sha256=row['sha256'], crop=box, run=row['run'])

        h.text(c, 'PROJECT EVERYTHING / WINDOW DETAIL FIXES', h.M, h.H-39, 11, h.TEAL, True)
        h.text(c, title, h.M, h.H-76, 27, h.INK, True)
        h.text(c, f'{mod}  |  Example: Minecraft {fixed[ui]["minecraft"]} / {loader.title()}', h.M, h.H-100, 12, h.GRAY)
        h.text(c, 'ORIGINAL UI / SIU ABSENT', h.M, h.H-133, 11, h.TEAL, True)
        h.text(c, 'FIXED SIU WINDOW / REAL IN-GAME CAPTURE', h.M+h.PW+h.GAP, h.H-133, 11, h.TEAL, True)
        for x in (h.M, h.M+h.PW+h.GAP):
            c.setFillColor(colors.HexColor(h.PALE))
            c.roundRect(x, 277, h.PW, 420, 8, stroke=0, fill=1)
        scale = min((h.PW-20)/max(i.width for i in panels.values()), 400/max(i.height for i in panels.values()))
        for index, side in enumerate(('native', 'siu')):
            h.draw_image(c, panels[side], (h.M+index*(h.PW+h.GAP)+10, 287, h.PW-20, 400), scale)
        h.paragraph(c, note, h.M, 253, h.W-2*h.M, 13, 19)
        h.text(c, 'FIXED DETAIL' if ui != 'bf-sculptor-clean' else 'EMPTY / WRONG PAYMENT', h.M, 184, 10, h.TEAL, True)
        picture, detail_box = detail(pair['siu'])
        record['detail_crop'] = detail_box
        if ui == 'bf-sculptor-clean':
            h.draw_image(c, picture.resize((picture.width*4, picture.height*4), Image.Resampling.NEAREST), (h.M, 76, 235, 94))
            paid_path = ROOT / 'build/remaining-ui-audit/gameplay/1.20.1-forge-siu-window-details02-fixed01/screenshots/sculptor-correct-payment-enabled.png'
            paid = {**pair['siu'], 'screenshot': str(paid_path)}
            paid_detail, paid_box = detail(paid)
            h.text(c, 'CORRECT PAYMENT', h.M+265, 184, 10, h.TEAL, True)
            h.draw_image(c, paid_detail.resize((paid_detail.width*4, paid_detail.height*4), Image.Resampling.NEAREST), (h.M+265, 76, 235, 94))
            record['paid_detail'] = dict(path=str(paid_path), sha256=hashlib.sha256(paid_path.read_bytes()).hexdigest(), crop=paid_box)
        else:
            h.draw_image(c, picture.resize((picture.width*4, picture.height*4), Image.Resampling.NEAREST), (h.M, 76, h.PW, 94))
        h.paragraph(c, caption, h.M+h.PW+h.GAP, 162, h.PW, 13, 18)
        h.text(c, 'APPLIED TO', h.M+h.PW+h.GAP, 112, 10, h.TEAL, True)
        h.paragraph(c, ports, h.M+h.PW+h.GAP, 93, h.PW, 11, 16)
        c.setStrokeColor(colors.HexColor('#D3E0E0'))
        c.line(h.M, 51, h.W-h.M, 51)
        h.text(c, 'Real screenshots; identical publisher binaries within each pair. Focused checks only; no full gameplay suite.', h.M, 33, 9, h.GRAY)
        c.setFont('Helvetica', 9)
        c.drawRightString(h.W-h.M, 33, f'{page} / {len(CASES)}')
        c.showPage()
        evidence.append(record)
    c.save()
    (WORK / 'evidence.json').write_text(json.dumps(evidence, indent=2))
    print('Created', OUT, 'with', len(CASES), 'verified original/fixed pairs.')


if __name__ == '__main__':
    main()
