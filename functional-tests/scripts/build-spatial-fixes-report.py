"""Build the before/after report from the recorded, focused Spatial GUI fix runs."""
from pathlib import Path
import hashlib
import json
from xml.sax.saxutils import escape

from PIL import Image
from pypdf import PdfReader
from reportlab.lib.colors import HexColor
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph

ROOT = Path(__file__).resolve().parents[2]
QA = ROOT / 'build/gameplay-qa/packaged-core'
BEFORE = QA / '26.3-fabric-spatial-findings-r1'
AFTER = QA / '26.3-fabric-spatial-fixes-r3'
HUD = QA / '26.3-fabric-spatial-fixes-hotbar'
REGRESSION = QA / '26.3-fabric-spatial-fixes-regression'
ABSENT = QA / '26.3-fabric-spatial-fixes-absent'
OUT = ROOT / 'output/pdf/Salts-Inventory-Spatial-GUI-Fixes-Report.pdf'
TEMP = ROOT / 'tmp/pdfs/spatial-fixes'
W, H = 960, 820
INK, MUTED, TEAL = map(HexColor, ['#172A36', '#526875', '#087F83'])
BG, LINE = map(HexColor, ['#F5F8FA', '#D6E1E7'])


def result(run):
    data = json.loads((run / 'results.json').read_text())
    assert data['failures'] == 0, run
    return data


def shot(run, name):
    return run / 'screenshots' / (name + '.png')


def crop(source, name, box):
    path = TEMP / (name + '.png')
    with Image.open(source) as im:
        im.crop(box).save(path)
    return path


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    TEMP.mkdir(parents=True, exist_ok=True)
    for name, file in [('Body', 'segoeui.ttf'), ('Bold', 'segoeuib.ttf'), ('Mono', 'consola.ttf')]:
        pdfmetrics.registerFont(TTFont(name, 'C:/Windows/Fonts/' + file))
    body = ParagraphStyle('body', fontName='Body', fontSize=12, leading=17, textColor=INK)
    small = ParagraphStyle('small', parent=body, fontSize=10, leading=14, textColor=MUTED)
    label = ParagraphStyle('label', parent=small, fontName='Bold', textColor=TEAL)
    title = ParagraphStyle('title', parent=body, fontName='Bold', fontSize=25, leading=30)
    runs = {name: result(run) for name, run in [('Visual matrix', AFTER), ('Interaction regression', REGRESSION), ('Hotbar placement', HUD), ('Core without Spatial GUI', ABSENT)]}
    def geometry(scene):
        data = json.loads((AFTER / ('finding-' + scene + '.json')).read_text())
        return {w['instanceId']: tuple(w[k] for k in ('x', 'y', 'width', 'height')) for w in data['windows']}

    preservation = {
        'alt_roundtrip': geometry('restored-frame') == geometry('gameplay-camera') == geometry('desktop-return'),
        'gui_scale_roundtrip': geometry('carry-outside') == geometry('hotbar-scale-three') == geometry('hotbar-scale-two'),
    }
    assert all(preservation.values())
    (AFTER / 'layout-preservation.json').write_text(json.dumps(preservation, indent=2) + '\n')
    assert any(r['name'] == 'finding-hud-server-stack32' and r['status'] == 'PASS' for r in runs['Hotbar placement']['results'])
    sha = hashlib.sha256((ROOT / 'dist/core/salts_inventory_update-26.3-Fabric-0.1.5.jar').read_bytes()).hexdigest()
    audit = json.loads((ROOT / 'build/spatial-gui-analysis/fixes-artifact-verification.json').read_text())
    assert audit['core_sha256'] == sha and len(audit['changed_dist_jars']) == 1

    pages = [dict(kind='cover', tag='IMPLEMENTED FIXES', title='Salt\'s Inventory + Spatial GUI')]

    def pair(tag, heading, left, right, ll, rr, explanation, proof):
        pages.append(dict(kind='pair', tag=tag, title=heading, left=left, right=right, ll=ll, rr=rr, explanation=explanation, proof=proof))

    def comparison(tag, heading, scene, explanation, proof):
        pair(tag, heading, shot(BEFORE, 'finding-' + scene), shot(AFTER, 'finding-' + scene), 'BEFORE / original full framebuffer', 'AFTER / fitted full framebuffer', explanation, proof)

    comparison('F01 / WINDOW BOUNDS', 'Right-edge controls stay visible', 'right-mid',
               'The complete desktop surface now fits within a camera-visible rectangle. Perspective and pointer movement are included in the fit, so the title controls no longer disappear at the right edge in this case.',
               'The runtime check projects the actual panel vertices and verifies all bounds are within the available rectangle. Window geometry is preserved; the whole surface changes placement and uniform scale.')
    pair('F01 / PHYSICAL MOUSE', 'A real rightward drag stays usable', shot(BEFORE, 'manual-offscreen-right'), shot(AFTER, 'manual-right-edge-fixed'),
         'BEFORE / rightward title-bar drag', 'AFTER / rightward title-bar drag',
         'Computer use dragged the inventory toward the right edge in the running game. The fixed view retains the complete inventory and its title controls beside the avatar.',
         'These are separate disposable worlds, so the avatar skin and world contents differ. The comparison demonstrates the observed drag result, not identical framebuffer pixels.')
    comparison('F01 / BOTTOM-RIGHT', 'The lower corner remains inside the frame', 'bottom-right',
               'The fit includes the complete surface footprint rather than only the logical window rectangles. The lower/right placement remains visible with a margin around the projected panel.',
               'No per-window recovery or transition rearrangement is used. The panel retains its aspect ratio and orientation.')
    comparison('F03 / AVATAR SPACE', 'The desktop leaves room for the player', 'left-bottom',
               'In third person, an expanded player volume is projected into the camera. The desktop fits on the larger available side, leaving a reserved column around the avatar.',
               'The left/bottom fixture previously covered the player arm. The fixed view places the desktop to the avatar\'s right. Extreme cameras with almost no free area remain a coverage limit.')
    comparison('F02 / DETACHED HOTBAR', 'The hotbar follows the workspace', 'baseline',
               'The native hotbar, hearts, hunger and related decorations share a compact dock below the window cluster. The HUD and its interactive slot coordinates use the same offset.',
               'The old screen-bottom anchor left roughly 350 framebuffer pixels below the windows in this baseline. The updated dock closes that gap. A separate real-stack placement probe is shown later.')
    comparison('F01 / FRAME SIZE', 'A narrower frame preserves the full desktop', 'tall-frame',
               'The 1000 x 900 frame previously lost the right side of the inventory. Whole-panel fitting now keeps the desktop visible without moving the individual windows.',
               'The focused matrix also exercised 1280 x 720 and 1280 x 900. Full images are letterboxed here; they are not cropped to hide the frame edges.')
    comparison('F01 / GUI SCALE', 'GUI scale changes keep retained windows visible', 'hotbar-scale-three',
               'When the physical GUI becomes smaller than the retained layout, the capture uses a uniformly scaled logical canvas with the same aspect ratio. Mouse mapping applies the inverse scale.',
               'Recorded window IDs, positions and sizes match across GUI scales 2 and 3. This avoids clamping or relocating each window to recover visibility.')
    comparison('F04 / FIRST-PERSON PARALLAX', 'Corner-pointer framing stays readable', 'first-mouse-corner',
               'The fitter selects the largest uniform scale that fits after centering the whole surface. This avoids excessive shrinking when the original plane center moves off-screen.',
               'Parallax remains enabled in this comparison. The tested panel boundary stays inside the visible rectangle; normal physical text size still depends on the available viewing area.')
    pair('F04 / ADDITIONAL MODES', 'Curved and crosshair modes remain supported', shot(AFTER, 'finding-first-curved'), shot(AFTER, 'finding-first-crosshair'),
         'AFTER / first-person curved surface', 'AFTER / first-person crosshair mode',
         'Curved fitting checks all vertices of the rendered 32-segment surface. Its interaction basis receives the same fit scale, so the visible surface and hit geometry agree.',
         'The affected regression also exercises a curved-mode item interaction. These checks do not cover every combination of upstream settings.')
    comparison('F05 / CARRIED STACK', 'The item stays complete at the panel edge', 'carry-panel-left',
               'Carried feedback now uses the existing Core stack renderer at the physical cursor in the final HUD pass. The duplicate captured draw is suppressed, keeping the sprite and count complete outside the 3D texture.',
               'The cursor is clamped slightly inside the physical screen for visibility. Pickup, placement and outside-drop transactions still use the existing server-backed inventory flow.')
    pair('F05 / PIXEL DETAIL', 'Edge fragment versus complete stack feedback',
         crop(shot(BEFORE, 'finding-carry-panel-left'), 'before-carried-edge', (380, 395, 480, 495)),
         crop(shot(AFTER, 'finding-carry-outside'), 'after-carried-outside', (0, 0, 100, 100)),
         'BEFORE / panel-edge crop', 'AFTER / physical screen-corner crop',
         'These enlarged, unretouched framebuffer crops show the old captured-edge fragment and the updated complete sprite/count. The sample positions differ deliberately: the updated feedback remains visible even outside the panel.',
         'The scene records retained 32 carried items; the authoritative integrated-server check confirmed all 32 were returned to the chest afterward.')
    pair('ALT / ONE LOGICAL LAYOUT', 'Movement control preserves the workspace', shot(AFTER, 'finding-gameplay-camera'), shot(AFTER, 'finding-desktop-return'),
         'Movement control / gameplay camera', 'Return / spatial desktop camera',
         'Alt returns camera and movement control to gameplay, then restores the spatial desktop. The presentation changes as a whole; window IDs, positions and sizes match exactly before, during and after the round trip.',
         'A separate focused interaction run covers the default Alt Toggle and remapped P binding, held-slot controls, world targeting, outside drops and clean close.')
    pair('HOTBAR / SERVER-BACKED INPUT', 'The compact dock accepts a real stack', shot(HUD, 'finding-hud-probe-hover'), shot(HUD, 'finding-hud-probe-result'),
         'Projected HUD target / on-screen check', 'After placement / 32 in hotbar slot 2',
         'The harness first requires the projected target to be inside the physical monitor. It then clicks the visible hotbar slot and checks the canonical integrated-server inventory.',
         'The carried stack emptied and hotbar slot 2 contained 32 dirt. This tests rendering and slot hit coordinates together, beyond a screenshot-only layout check.')
    pages.append(dict(kind='manual', tag='PHYSICAL MOUSE / TRANSFER', title='Window-to-window transfer still works'))
    pages.append(dict(kind='evidence', tag='VALIDATION AND LIMITS', title='What was checked on the rebuilt JAR'))

    c = canvas.Canvas(str(OUT), pagesize=(W, H), pageCompression=1)
    c.setTitle("Salt's Inventory + Spatial GUI - Fixes Report")
    c.setAuthor("Salt's Inventory Update")
    manifest = []

    def text(value, x, y, width, style=body, limit=110):
        p = Paragraph(escape(value), style)
        _, height = p.wrap(width, limit)
        if height > limit:
            raise ValueError('Text overflow: ' + value[:100])
        p.drawOn(c, x, y - height)
        return y - height

    def pic(path, x, y, width, height):
        with Image.open(path) as im:
            iw, ih = im.size
        scale = min(width / iw, height / ih)
        dw, dh = iw * scale, ih * scale
        c.setFillColor(HexColor('#E9EEF1'))
        c.rect(x, y, width, height, fill=1, stroke=0)
        c.drawImage(ImageReader(str(path)), x + (width - dw) / 2, y + (height - dh) / 2, dw, dh)
        c.setStrokeColor(LINE)
        c.rect(x, y, width, height, fill=0, stroke=1)

    for i, p in enumerate(pages, 1):
        c.setFillColor(BG)
        c.rect(0, 0, W, H, fill=1, stroke=0)
        c.bookmarkPage(f'p{i}')
        c.addOutlineEntry(p['title'], f'p{i}', 0, False)
        c.setFillColor(TEAL)
        c.setFont('Bold', 10)
        c.drawString(40, 790, p['tag'])
        c.setFont('Body', 9)
        c.setFillColor(MUTED)
        c.drawRightString(920, 790, '26.3 FABRIC / CORE 0.1.5 / SPATIAL GUI 1.8')
        text(p['title'], 40, 757, 880, title, 65)
        images = []
        if p['kind'] == 'cover':
            text('Implemented fixes and focused in-game evidence / October 21, 2026', 40, 713, 880, small, 25)
            images = [shot(AFTER, 'manual-transfer-fixed')]
            pic(images[0], 40, 233, 880, 460)
            text('ONE DESKTOP, FITTED AS A WHOLE', 40, 211, 880, label, 25)
            text('The complete panel fits beside the avatar. The hotbar sits near the windows, and carried items stay visible at the cursor. Window geometry is retained across movement-control transitions.', 40, 185, 880, body, 65)
            text('All five finding groups from the investigation are addressed. This report shows raw before/after frames and focused checks; it does not claim exhaustive modpack or multiplayer coverage.', 40, 103, 880, small, 45)
        elif p['kind'] == 'pair':
            images = [p['left'], p['right']]
            pic(p['left'], 40, 338, 430, 353)
            pic(p['right'], 490, 338, 430, 353)
            text(p['ll'], 40, 320, 430, label, 35)
            text(p['rr'], 490, 320, 430, label, 35)
            c.setFillColor(HexColor('#E7F0F2'))
            c.roundRect(40, 146, 880, 126, 6, fill=1, stroke=0)
            text(p['explanation'], 58, 253, 844, body, 90)
            text(p['proof'], 40, 121, 880, small, 65)
        elif p['kind'] == 'manual':
            images = [shot(AFTER, 'manual-transfer-fixed')]
            pic(images[0], 40, 224, 880, 468)
            text('Computer use clicked the chest source slot, then the inventory destination slot in the fitted desktop. The F2 capture shows the 32 dirt in the player window and an empty source chest.', 40, 200, 880, body, 60)
            text('The automatic native-slot regression and server-state assertions provide separate transaction checks. This manual pass checks that visible screen coordinates remain usable after the rightward drag.', 40, 118, 880, small, 55)
        else:
            y = 704
            counts = '; '.join(f"{name}: {len(data['results'])} records" for name, data in runs.items()) + '. All four final runs finished with zero failures.'
            sections = [
                ('Focused final runs', counts + ' Records include assertions, stage completion and screenshots; they are not a count of independent tests.'),
                ('Visual acceptance and preserved layout', 'The visual matrix includes 1280 x 900, 1280 x 720, 1000 x 900, GUI scales 2/3, window edges/larger size, first/third person, curve/crosshair and parallax on/off. Each of 24 captured states includes a projected-bounds assertion. Scene JSON confirms unchanged window geometry across Alt and GUI-scale round trips.'),
                ('Affected interaction checks', 'Native pickup/place, curved input, Alt Toggle and rebinding, hotbar selection lock and movement restore, world-use obstruction/reach, outside right/left drops, feature disable/re-enable and close. The Core-only run starts without Spatial GUI and checks native inventory interaction.'),
                ('Build and packaging', 'The target module build, source/JAR boundary audit and whitespace check passed. Only the 26.3 Fabric Core distribution JAR changed; the other 35 JARs are byte-identical. No upstream Spatial GUI classes or QA classes are bundled. The optional integration adds no required runtime dependency or protocol change.'),
                ('Limits', 'Only 26.3 Fabric with Spatial GUI 1.8 was changed and checked. No complete functional suite or fresh dedicated-server run after these visual fixes. Prior dedicated-server results apply to the earlier integration artifact. Controlify, shaders, unrelated camera/HUD mods, extreme FOV/near-camera avatar configurations and exhaustive upstream settings were not tested. Fitting a large layout into a small viewing area can reduce physical text size. Animation continuity was not measured frame by frame.'),
                ('Evidence', 'Raw screenshots and results: build/gameplay-qa/packaged-core/26.3-fabric-spatial-fixes-*/. Detailed commands, historical findings and coverage are recorded in functional-tests/SPATIAL-GUI-VALIDATION.md. The report builder checks the successful run files, compares recorded window geometry and verifies the staged artifact hash before rendering.'),
            ]
            for heading, value in sections:
                text(heading.upper(), 40, y, 880, label, 20)
                y = text(value, 40, y - 22, 880, small, 100) - 23
            text('REBUILT CORE SHA-256', 40, 92, 880, label, 20)
            text(sha, 40, 69, 880, ParagraphStyle('mono', parent=small, fontName='Mono', fontSize=9), 20)
        manifest.append({'page': i, 'images': [str(path.relative_to(ROOT)) for path in images]})
        c.setStrokeColor(LINE)
        c.line(40, 38, 920, 38)
        c.setFont('Body', 8)
        c.setFillColor(MUTED)
        c.drawString(40, 23, 'Implemented fixes / Real game frames / Focused validation')
        c.drawRightString(920, 23, f'{i:02d} / {len(pages):02d}')
        c.showPage()
    c.save()
    reader = PdfReader(OUT)
    assert len(reader.pages) == len(pages)
    OUT.with_suffix('.manifest.json').write_text(json.dumps({'pages': len(pages), 'core_sha256': sha, 'runs': {k: len(v['results']) for k, v in runs.items()}, 'images': manifest}, indent=2) + '\n')
    print(json.dumps({'pdf': str(OUT), 'pages': len(pages), 'bytes': OUT.stat().st_size}, indent=2))


if __name__ == '__main__':
    main()
