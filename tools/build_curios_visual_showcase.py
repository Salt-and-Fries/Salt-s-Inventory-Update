"""Build a screenshot-led tour from the real 26.3 NeoForge Curios capture session."""
from pathlib import Path
import hashlib
import json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, white
from reportlab.lib.utils import ImageReader

ROOT = Path(__file__).resolve().parents[1]
SHOTS = ROOT / 'output/screenshots/curios-26.3-neoforge-showcase'
OUT = ROOT / 'output/pdf/curios-26.3-neoforge-visual-showcase.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)
W, H = 792, 612
INK = HexColor('#18333C')
MUTED = HexColor('#5D7077')
TEAL = HexColor('#087E84')
LINE = HexColor('#D9E4E7')
PALE = HexColor('#F0F5F6')
AMBER = HexColor('#8B5614')
frames = []
c = canvas.Canvas(str(OUT), pagesize=(W, H), pageCompression=1)
c.setTitle("Curios + Salt's Inventory - Visual Showcase")
c.setAuthor("Salt's Inventory Update")
c.setSubject('Fresh 26.3 NeoForge gameplay captures, including the four-GUI-pixel gap')


def text(x, y, value, size=10, font='Helvetica', color=INK):
    c.setFillColor(color)
    c.setFont(font, size)
    c.drawString(x, y, value)


def lines(x, y, values, size=10, leading=15, color=MUTED):
    for i, value in enumerate(values):
        text(x, y-i*leading, value, size, color=color)


def page(title, subtitle, number):
    c.setFillColor(white)
    c.rect(0, 0, W, H, fill=1, stroke=0)
    text(36, 580, 'CURIOS + SALT\'S INVENTORY / VISUAL TOUR', 8.5, 'Helvetica-Bold', TEAL)
    text(36, 548, title, 25, 'Helvetica-Bold')
    text(36, 527, subtitle, 9, color=MUTED)
    c.setStrokeColor(LINE)
    c.line(36, 513, 756, 513)
    c.line(36, 42, 756, 42)
    text(36, 26, '26.3 NeoForge | Core 0.1.5 | Compatibility 0.1.2 | Curios 17.0.0-beta.3+26.3', 7.6, color=MUTED)
    c.setFont('Helvetica-Bold', 8)
    c.setFillColor(TEAL)
    c.drawRightString(756, 26, f'{number} / 10')
    c.bookmarkPage(f'page-{number}')
    c.addOutlineEntry(title, f'page-{number}', level=0)


def picture(name, box, crop=None, frame=True):
    """Crop only a real F2 image; retain native pixels and record the provenance."""
    source = SHOTS / (name + '.png')
    image = Image.open(source).convert('RGB')
    if crop:
        assert 0 <= crop[0] < crop[2] <= image.width and 0 <= crop[1] < crop[3] <= image.height
        image = image.crop(crop)
    x, y, width, height = box
    if frame:
        c.setFillColor(PALE)
        c.setStrokeColor(LINE)
        c.roundRect(x, y, width, height, 7, stroke=1, fill=1)
    scale = min(width/image.width, height/image.height)
    iw, ih = image.width*scale, image.height*scale
    px, py = x+(width-iw)/2, y+(height-ih)/2
    c.drawImage(ImageReader(image), px, py, width=iw, height=ih, mask='auto')
    frames.append({'page': c.getPageNumber(), 'capture': source.name, 'crop': crop, 'box': box})
    return px, py, iw, ih


def label(x, y, title, detail=None):
    text(x, y, title, 11, 'Helvetica-Bold')
    if detail:
        text(x, y-16, detail, 9, color=MUTED)


def number(x, y, value):
    c.setFillColor(TEAL)
    c.circle(x, y, 11, stroke=0, fill=1)
    c.setFillColor(white)
    c.setFont('Helvetica-Bold', 11)
    c.drawCentredString(x, y-3.8, str(value))


# 1: complete integrated view. These crops exclude the OS chrome and F2 message.
page('Curios + Salt\'s Inventory', 'Equipment, cosmetics and ordinary inventory in one desktop - including the new gap.', 1)
picture('01-equipment-overview', (36, 100, 720, 402), (420, 490, 1570, 1170))
text(36, 76, '35 accessory slots visible together', 10, 'Helvetica-Bold')
text(290, 76, 'Same-width Character panel', 10, 'Helvetica-Bold')
text(548, 76, '4-GUI-pixel separation', 10, 'Helvetica-Bold')
c.showPage()

# 2: control location and the gap in detail.
page('Open accessories from Character', 'The native Curios button opens and closes its attached panel.', 2)
label(36, 486, 'Panel closed', 'Character remains available.')
picture('02-character-toggle', (36, 265, 346, 195), (430, 840, 1040, 1180))
label(36, 233, 'Button beside the player preview')
picture('02-character-toggle', (36, 100, 150, 110), (518, 917, 558, 961))
lines(205, 183, ['The accessory panel stays above', 'Character and follows its width.', '', 'A narrow gap separates the frames.'], 10)
label(410, 486, 'Panel open')
picture('01-equipment-overview', (410, 81, 346, 380), (435, 500, 985, 1170))
c.showPage()

# 3: genuine side-by-side mode views with their native iconography.
page('Equipment and cosmetic views', 'Separate slot contents, native artwork and a cosmetic tab above the panel.', 3)
label(36, 486, 'Equipment', 'Standard accessory slots and their type icons.')
label(410, 486, 'Cosmetics', 'Purple slots with independent contents.')
picture('01-equipment-overview', (36, 235, 346, 220), (435, 502, 985, 845))
picture('06-transfer-result-clean', (410, 235, 346, 220), (435, 502, 985, 845))
text(36, 205, 'Native slot types in this sample layout', 11, 'Helvetica-Bold')
lines(36, 184, ['Head, Necklace, Back, Body, Hands, Bracelet, Ring, Belt, Charm, Curio and Feet.',
                'The layout follows the slot allocation supplied by Curios and installed content.'], 10)
picture('01-equipment-overview', (36, 76, 105, 78), (855, 505, 957, 582))
picture('06-transfer-result-clean', (160, 76, 105, 78), (855, 505, 957, 582))
lines(288, 123, ['The protruding tab switches the active view.',
                 'The gold nugget is a disposable test item, not content added by Curios.'], 9)
c.showPage()

# 4: three actual states of a cross-window gesture, not staged UI mockups.
page('Move items across the windows', 'One gold nugget: ordinary inventory -> shared cursor -> cosmetic slot.', 4)
steps = [('03-cosmetics-overview', 'In inventory', 'Source slot contains the sample item.'),
         ('04-item-carried', 'On the cursor', 'Picked up; cursor is over the source slot.'),
         ('06-transfer-result-clean', 'In Curios', 'Item is placed in the cosmetic slot.')]
for i, (shot, title, detail) in enumerate(steps):
    x = 36+i*247
    number(x+11, 479, i+1)
    text(x+29, 475, title, 12, 'Helvetica-Bold')
    text(x, 446, 'Inventory window', 9, color=MUTED)
    picture(shot, (x, 307, 226, 126), (1010, 555, 1555, 815))
    text(x, 277, 'Cosmetic panel', 9, color=MUTED)
    picture(shot, (x, 159, 226, 105), (440, 575, 985, 845))
    text(x, 145, detail, 8.7, color=MUTED)
lines(36, 94, ['The screenshots show real clicks between separate windows in the same running session.',
               'The disposable slot datapack allows the sample vanilla item for this interaction check.'], 9)
c.showPage()

# 5: native labels, item tooltips, restrictions and visibility buttons.
page('Native labels and controls', 'Curios keeps its slot icons, item tooltips, removal rules and visibility controls.', 5)
label(36, 486, 'Item tooltip and removal restriction')
label(410, 486, 'Empty-slot label')
picture('07-native-item-tooltip', (36, 275, 346, 190), (440, 580, 985, 845))
picture('08-empty-slot-label', (410, 275, 346, 190), (440, 580, 985, 845))
lines(36, 250, ['Clicking the Curse of Binding helmet in Survival', 'left it in the slot and kept the cursor empty.'], 9)
lines(410, 250, ['An empty necklace slot displays its native label.', 'The type icon remains visible in the grid.'], 9)
label(36, 186, 'Per-slot visibility')
picture('01-equipment-overview', (36, 72, 98, 98), (462, 597, 522, 657))
picture('09-visibility-toggle', (163, 72, 98, 98), (462, 597, 522, 657))
text(64, 57, 'Before', 8, color=MUTED)
text(193, 57, 'After', 8, color=MUTED)
picture('09-visibility-toggle', (294, 73, 275, 116), (450, 555, 800, 703))
lines(589, 148, ['Native visibility', 'toggle and tooltip.', '', 'Wearable rendering', 'depends on the item.'], 9)
c.showPage()

# 6: coexistence with Character's ordinary tools, without a replacement native screen.
page('Character tools remain available', 'Accessories, armor, crafting, the recipe guide and inventory share the desktop.', 6)
picture('10-recipe-guide-coexistence', (36, 75, 720, 428), (420, 490, 1570, 1369))
text(36, 56, 'Player preview + armor', 9, 'Helvetica-Bold')
text(247, 56, '2 x 2 crafting', 9, 'Helvetica-Bold')
text(443, 56, 'Recipe guide', 9, 'Helvetica-Bold')
text(624, 56, 'Inventory window', 9, 'Helvetica-Bold')
c.showPage()

# 7: actual screen-edge placements, preserving every pixel up to the viewport edge.
page('Move both panels as one unit', 'The full joined group stays inside the screen, including its cosmetic tab and gap.', 7)
label(36, 486, 'Top and left limits')
label(410, 486, 'Right and bottom limits')
picture('11-top-left-boundary', (36, 77, 346, 390), (0, 0, 560, 660))
picture('16-right-bottom-and-shrink', (410, 77, 346, 390), (490, 45, 854, 480))
text(36, 56, 'The tab reaches the upper edge; the frames retain their gap.', 9, color=MUTED)
c.showPage()

# 8: native capacity grows, then the viewport changes; no manufactured composite UI.
page('Expand first; scroll when necessary', 'The same 127-slot allocation shown on a large desktop and a compact one.', 8)
label(36, 486, 'Space available')
picture('12-large-capacity-fits', (36, 72, 196, 396), (0, 0, 535, 1250))
label(267, 486, 'Space limited')
picture('13-compact-scroll-top', (267, 201, 489, 275))
lines(267, 172, ['Character is pushed to the bottom.',
                 'The panel keeps its width and top tab.',
                 'A scrollbar appears once all rows cannot fit.'], 11, 21)
text(267, 82, '100 ring slots + 27 other slots, allocated by the disposable native Curios fixture.', 8.5, color=MUTED)
c.showPage()

# 9: show start, end and cosmetic scrolling without hiding the owning Character.
page('Reach every row', 'Scrollbar movement reaches the final types; cosmetic mode works in the same constrained layout.', 9)
for i, (shot, title, detail) in enumerate([
    ('13-compact-scroll-top', 'First rows', 'Equipment at the start of the list.'),
    ('14-compact-scroll-bottom', 'Final rows', 'Charm, Curio and Feet are reachable.'),
    ('15-compact-cosmetics', 'Cosmetic mode', 'The sample item remains in its slot.')]):
    x = 36+i*247
    label(x, 486, title)
    picture(shot, (x, 127, 226, 341), (0, 50, 352, 480))
    text(x, 102, detail, 8.5, color=MUTED)
text(36, 69, 'The scrollbar thumb is at the top in the first view and at the bottom in the second.', 9, color=MUTED)
c.showPage()

# 10: reduced capacity and closure are real observed results; record the discovered limit.
page('Capacity changes and closing', 'Reducing the allocation removes scrolling; closing Character also closes accessories.', 10)
label(36, 486, 'Back to 35 slots')
label(410, 486, 'Character closed')
picture('16-right-bottom-and-shrink', (36, 261, 346, 207))
picture('17-owner-closed', (410, 261, 346, 207))
text(36, 239, 'No scrollbar; the transferred gold nugget is retained.', 9, color=MUTED)
text(410, 239, 'Both joined panels close; Inventory remains open.', 9, color=MUTED)
c.setFillColor(HexColor('#FFF4E4'))
c.roundRect(36, 137, 720, 78, 7, stroke=0, fill=1)
text(50, 195, 'One follow-up observed during capture', 11, 'Helvetica-Bold', AMBER)
lines(50, 177, ['G opened the workspace from gameplay, but did not reopen Curios while the desktop was already open.',
                'The Character button worked. The in-desktop keyboard shortcut needs a follow-up check.'], 9, 15, AMBER)
lines(36, 110, ['Real gameplay screenshots captured 2026-10-22. UI crops and labels only; no UI mockups.',
                'Focused singleplayer capture with a slot datapack and vanilla sample items. No complete suite,',
                'dedicated-server run, accessory-content-mod effects, Controlify or Spatial GUI check in this session.'], 8.8, 14)
c.showPage()
c.save()

capture_names = json.loads((SHOTS/'captures.json').read_text())
manifest = {
    'pdf': OUT.name, 'pages': 10, 'minecraft': '26.3', 'loader': 'NeoForge 26.3.0.76',
    'core': '0.1.5', 'compatibility': '0.1.2', 'curios': '17.0.0-beta.3+26.3', 'gapGuiPixels': 4,
    'captures': [{'file': n+'.png', 'nativeF2File': f,
                  'sha256': hashlib.sha256((SHOTS/(n+'.png')).read_bytes()).hexdigest()}
                 for n, f in capture_names.items()],
    'placements': frames,
    'followUp': 'G did not reopen Curios with the desktop already active; Character button worked.',
    'scope': 'Focused integrated-server screenshot session; disposable slot datapack and vanilla sample items.',
}
OUT.with_suffix('.manifest.json').write_text(json.dumps(manifest, indent=2))
print(OUT)
print(f'10 pages, {len(capture_names)} original captures, {len(frames)} image placements')
