"""Five pinned Enchanted 1.20.1 native references and SIU design proposals.

Design only. Native gameplay captures are immutable; working artwork is copied
from the same publisher JAR, without its player-inventory or outer frame.
"""
from pathlib import Path
from io import BytesIO
import hashlib
import json
import zipfile

from PIL import Image, ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/enchanted-window-design'
PDF = ROOT / 'output/pdf/siu-enchanted-witchcraft-window-mockups.pdf'
JAR = ROOT / 'build/ui-modpack-audit-20261025/jars/206b980524abd5bf4fd0603cd03664f8fcc96508.jar'
GUI = ROOT / 'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
N = Image.Resampling.NEAREST
BG = (198, 198, 198, 255)
FRAME = Image.open(GUI / 'window.png').convert('RGBA')
CONTROLS = Image.open(GUI / 'window_controls.png').convert('RGBA')
with zipfile.ZipFile(Path.home() / '.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII = Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')

RELEASE = 'https://modrinth.com/mod/enchanted-witchcraft/version/JQc2jrQz'
SOURCE = 'https://github.com/Favouriteless/Enchanted'
SPECS = [
    dict(id='enchanted-witch-oven', title="Witch's Oven", asset='witch_oven', size=(162, 90),
         rect=(50, 15, 125, 71), slots=5,
         function='Processes ingredients with fuel and collects additional byproducts in clay jars. The native menu has an ingredient input, jar input, fuel input and two outputs.',
         design='Keep the five slots in exactly the original arrangement, including both jar silhouettes, the progress arrow and the heat indicator. Center this complete working area below a single SIU title row.',
         preserve='5 slots / jar ghost icons / heat and progress',
         opener="Use a placed Witch's Oven.",
         source_book='extraction/en_us/entries/witch_oven.json'),
    dict(id='enchanted-spinning-wheel', title='Spinning Wheel', asset='spinning_wheel', size=(176, 80),
         rect=(27, 20, 149, 65), slots=4,
         function='Combines three ingredients into one result using power from a nearby altar. It is used for weaving materials, including poppet recipes.',
         design='Preserve the triangular input arrangement, curved arrows, complete wheel and output slot as one unit. Reserve the native animation area and leave clear padding above and below it.',
         preserve='3 inputs + 1 output / arrows / animated wheel',
         opener='Use a Spinning Wheel; recipes require sufficient altar power.',
         source_book='poppetry/en_us/entries/spinning_wheel.json'),
    dict(id='enchanted-distillery', title='Distillery', asset='distillery', size=(176, 111),
         rect=(29, 5, 146, 82), slots=7,
         function='Separates ingredients into up to four outputs with altar power. The left side has a clay-jar input and two ingredient slots; all four results stay in the vertical column on the right.',
         design='Move the entire seven-slot apparatus below the header without rearranging it. Preserve every branch, bubble and output border, especially the top and bottom output slots. Remove the old centered title from the working area.',
         preserve='7 slots / all four output borders / branches and bubbles',
         opener='Use a Distillery near an altar with sufficient power.',
         source_book='extraction/en_us/entries/distillery.json'),
    dict(id='enchanted-poppet-shelf', title='Poppet Shelf', asset='poppet_shelf', size=(156, 58),
         rect=(45, 16, 131, 37), slots=4,
         function="Holds four poppets so their effects can work without the player carrying them. The native guide describes the shelf's effects as working across dimensions.",
         design='Keep the original four individually spaced slots in a compact storage window. The standard five window controls fit beside the title with clear spacing. Keep the separate player inventory available for transfers.',
         preserve='4 slots / original gaps / native poppet behavior',
         opener='Use a placed Poppet Shelf.',
         source_book='poppetry/en_us/entries/poppet_shelf.json'),
    dict(id='own-enchanted-formed-altar', title='Altar', asset='altar', size=(176, 64),
         rect=(20, 32, 156, 56), slots=0,
         function='Displays current power, maximum power and recharge multiplier. This is a status menu with no item slots; its reference capture reads 0/0 (1x).',
         design='Keep the native numeric readout, add a left-aligned title and reduce the empty height. Show movement, focus, link and close controls; add no item slots, sorting actions or invented power widgets.',
         preserve='Native power readout / 0 slots / status-only',
         opener='Use a formed altar made from six altar blocks in a 3 x 2 arrangement.',
         source_book='getting_started/en_us/entries/altar.json'),
]


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def glyph(ch):
    n = ord(ch)
    g = ASCII.crop((n % 16 * 8, n // 16 * 8, n % 16 * 8 + 8, n // 16 * 8 + 8))
    b = g.getbbox()
    return g, 3 if ch == ' ' else b[2] if b else 5


def tw(s):
    return sum(glyph(c)[1] + 1 for c in s)


def text(im, s, x, y, color=(64, 64, 64, 255)):
    assert x >= 0 and y >= 0 and x + tw(s) <= im.width and y + 8 <= im.height
    for ch in s:
        g, w = glyph(ch)
        ink = Image.new('RGBA', (8, 8), color)
        ink.putalpha(g.getchannel('A'))
        im.alpha_composite(ink, (x, y))
        x += w + 1


def frame(w, h):
    im = Image.new('RGBA', (w, h))
    for s, d in [
        ((0, 0, 5, 5), (0, 0, 5, 5)), ((6, 0, 11, 5), (w-5, 0, w, 5)),
        ((0, 6, 5, 11), (0, h-5, 5, h)), ((6, 6, 11, 11), (w-5, h-5, w, h)),
        ((5, 0, 6, 5), (5, 0, w-5, 5)), ((5, 6, 6, 11), (5, h-5, w-5, h)),
        ((0, 5, 5, 6), (0, 5, 5, h-5)), ((6, 5, 11, 6), (w-5, 5, w, h-5)),
        ((5, 5, 6, 6), (5, 5, w-5, h-5)),
    ]:
        im.alpha_composite(FRAME.crop(s).resize((d[2]-d[0], d[3]-d[1]), N), d[:2])
    return im


def header(im, title):
    full = [0, 6, 4, 9, 2]  # Focus, Pin, unlocked Lock, Link, Close.
    cols = full if 7 + tw(title) + 7 <= im.width - 5 - len(full)*14 else [3, 2]
    start = im.width - 5 - len(cols)*14
    assert 7 + tw(title) + 7 <= start, (title, im.width)
    text(im, title, 7, 7)
    for i, col in enumerate(cols):
        sy = 22 if col == 0 else 0
        im.alpha_composite(CONTROLS.crop((col*11, sy, col*11+11, sy+11)), (start+i*14, 5))
    return cols


def native(row):
    n = row['native']
    p = ROOT / n['capture']
    assert sha(p) == n['sha256'], row['id']
    im = Image.open(p).convert('RGBA')
    assert im.size == (1280, 900)
    b = n['bounds']
    return im.crop((2*b['x'], 2*b['y'], 2*(b['x']+b['width']), 2*(b['y']+b['height']))).resize((b['width'], b['height']), N)


def make_design(spec, row, original):
    rect = spec['rect']
    with zipfile.ZipFile(JAR) as z:
        art = Image.open(BytesIO(z.read('assets/enchanted/textures/gui/' + spec['asset'] + '.png'))).convert('RGBA')
    if spec['slots']:
        body = art.crop(rect)
        slots = [s for s in row['native']['slots'] if not s['player']]
        assert len(slots) == spec['slots']
        # Each complete 18 x 18 native slot must fit, including every border.
        for s in slots:
            assert rect[0] <= s['x']-1 and rect[1] <= s['y']-1
            assert rect[2] >= s['x']+17 and rect[3] >= s['y']+17
            slot_art = art.crop((s['x']-1, s['y']-1, s['x']+17, s['y']+17))
            assert body.crop((s['x']-1-rect[0], s['y']-1-rect[1], s['x']+17-rect[0], s['y']+17-rect[1])).tobytes() == slot_art.tobytes()
    else:
        body = original.crop(rect)  # Preserve the real numeric readout.
    im = frame(*spec['size'])
    cols = header(im, spec['title'])
    xy = ((im.width-body.width)//2, 26 if spec['slots'] else 30)
    assert xy[0] >= 7 and xy[1] >= 24 and xy[0]+body.width <= im.width-7 and xy[1]+body.height <= im.height-7
    # Both surfaces use the exact native #C6C6C6; extraction has no panel seam.
    assert BG in body.get_flattened_data(), spec['title']
    assert im.getpixel((5, 22)) == BG
    im.alpha_composite(body, xy)
    assert im.crop((xy[0], xy[1], xy[0]+body.width, xy[1]+body.height)).tobytes() == body.tobytes()
    return im, dict(body_source_rect=list(rect), body_destination=list(xy), chrome_columns=cols,
                    preserved_slots=spec['slots'], artwork_sha256=hashlib.sha256(body.tobytes()).hexdigest())


def build_pdf(pages):
    pdfmetrics.registerFont(TTFont('Segoe', 'C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB', 'C:/Windows/Fonts/segoeuib.ttf'))
    W, H = 1191, 842
    c = canvas.Canvas(str(PDF), pagesize=(W, H), pageCompression=1)
    c.setTitle('Enchanted: Witchcraft / SIU Window Mockups')
    c.setAuthor("Salt's Inventory Update / Project Everything")

    def txt(s, x, y, size=12, bold=False, color='#19373D'):
        c.setFillColor(HexColor(color)); c.setFont('SegoeB' if bold else 'Segoe', size)
        c.drawString(x, y, s)

    def para(s, x, y, width, size=12, leading=18):
        line = ''
        for word in s.split():
            candidate = (line+' '+word).strip()
            if pdfmetrics.stringWidth(candidate, 'Segoe', size) > width:
                txt(line, x, y, size); y -= leading; line = word
            else:
                line = candidate
        if line:
            txt(line, x, y, size); y -= leading
        return y

    def base(title, sub, i):
        c.setFillColor(HexColor('#FFFFFF')); c.rect(0, 0, W, H, fill=1, stroke=0)
        txt('PROJECT EVERYTHING / ENCHANTED: WITCHCRAFT', 36, H-34, 10, True, '#247C80')
        txt(title, 36, H-76, 28, True)
        txt(sub, 36, H-102, 11, color='#5E7076')
        c.setStrokeColor(HexColor('#D7E2E3')); c.line(36, 56, W-36, 56)
        txt('Design proposal only / Native references: Minecraft 1.20.1 Fabric, Enchanted 3.1.14 / No compatibility code changed', 36, 35, 9, color='#5E7076')
        txt(f'{i} / 6', W-68, 35, 9, color='#5E7076')

    for i, p in enumerate(pages, 1):
        sub = ('Status-only menu / no item slots' if p['slots'] == 0 else f"{p['slots']} container slots / Player inventory moves to its separate SIU window")
        base(p['title'], sub, i)
        c.bookmarkPage(p['id']); c.addOutlineEntry(p['title'], p['id'], level=0)
        for x, label, key in [(36, 'ORIGINAL IN GAME / SIU ABSENT', 'original'), (613, 'PROPOSED SIU WINDOW / MOCKUP', 'mockup')]:
            txt(label, x, 698, 10, True, '#247C80')
            c.setFillColor(HexColor('#EEF4F4')); c.roundRect(x, 257, 542, 425, 8, fill=1, stroke=0)
            im = Image.open(ROOT / p[key]).convert('RGBA')
            # A logical GUI pixel has the same size on both sides of the page.
            scale = 2.35
            # Pre-enlarge with nearest neighbor so PDF viewers cannot blur slots.
            large = im.resize((im.width*6, im.height*6), N)
            dw, dh = im.width*scale, im.height*scale
            assert dw <= 510 and dh <= 399
            c.drawImage(ImageReader(large), x+(542-dw)/2, 257+(425-dh)/2, dw, dh, mask='auto')
        txt('WHAT IT DOES', 36, 231, 9, True, '#247C80')
        y = para(p['function'], 36, 210, 542)
        txt('WINDOW DESIGN', 613, 231, 9, True, '#247C80')
        z = para(p['design'], 613, 210, 542)
        assert min(y, z) > 105, (p['title'], y, z)
        txt('Retained: '+p['preserve'], 36, 105, 10, color='#5E7076')
        txt('Open: '+p['opener'], 36, 85, 10, color='#5E7076')
        c.linkURL(RELEASE, (36, 60, 347, 75), relative=0, thickness=0)
        txt('Reference release and bundled native UI artwork', 36, 64, 8, color='#247C80')
        c.showPage()

    base('Scope and design notes', 'Five native screen types covered / Four inventory screens plus the Altar status menu', 6)
    c.bookmarkPage('scope'); c.addOutlineEntry('Scope and design notes', 'scope', level=0)
    sections = [
        ('ONE CONSISTENT WINDOW STYLE',
         'Use the actual SIU border, controls and Minecraft bitmap font. The window body matches the original #C6C6C6 background exactly. Copy complete native working artwork, retaining slot borders and empty-slot icons. No clipped pieces of the original outer frame remain.'),
        ('LAYOUT AND BEHAVIOR TO RETAIN',
         'The Witch\'s Oven keeps all five slot roles. The Spinning Wheel retains its three-input arrangement and animation area. The Distillery keeps its jar input, two ingredients, four stacked results, progress branches and bubbles. The Poppet Shelf keeps all four positions and their spacing.'),
        ('CONTROLS AND PLAYER INVENTORY',
         'Use the normal Focus, Pin, Lock, Link and Close controls, which fit beside all five titles here. Fall back to More and Close if a smaller layout or translated title needs extra room. Recipe slots are not treated as interchangeable storage. Show the player inventory separately for the four inventory screens.'),
        ('OTHER INTERFACES / NATIVE APPENDIX',
         'The Altar has no inventory; its compact status proposal is shown for completeness. Patchouli guidebooks stay in their native book interface. The Witch\'s Cauldron and Kettle are used through interactions in the world, rather than an inventory screen, in this release. JEI/EMI recipe displays remain in their recipe viewer.'),
        ('REFERENCE AND REVIEW LIMITS',
         'Originals are unmodified crops of saved in-game screenshots with SIU absent. Mockups use matching publisher artwork and show the same empty or idle state. All 20 container slots were checked for complete 18 x 18 borders; working areas and titles were checked for clearance. This is a design review, not a new gameplay or implementation test.'),
    ]
    y = 676
    for label, body in sections:
        txt(label, 36, y, 10, True, '#247C80')
        y = para(body, 36, y-23, 1089, 13, 20)-22
    assert y > 145, y
    txt('SOURCES', 36, y, 10, True, '#247C80')
    txt('Publisher release 3.1.14 - Minecraft 1.20.1 Fabric', 36, y-23, 11)
    c.linkURL(RELEASE, (36, y-28, 440, y-11), relative=0, thickness=0)
    txt('Favouriteless / Enchanted source repository', 36, y-45, 11)
    c.linkURL(SOURCE, (36, y-50, 420, y-33), relative=0, thickness=0)
    txt('Bundled references: textures/gui/*.png, client/screens/*, common/menus/* and Patchouli device entries.', 36, y-68, 10, color='#5E7076')
    c.showPage(); c.save()


def main():
    for d in [OUT / 'original', OUT / 'mockups', PDF.parent]:
        d.mkdir(parents=True, exist_ok=True)
    assert hashlib.sha1(JAR.read_bytes()).hexdigest() == JAR.stem
    cases = json.loads((ROOT / 'output/remaining-ui-audit/coverage.json').read_text())['cases']
    refs = {r['id']: r for r in cases if r.get('entry') == 205}
    assert len(refs) == len(SPECS) == 5
    pages = []
    for s in SPECS:
        r = refs[s['id']]
        assert r['minecraft'] == '1.20.1' and r['loader'] == 'fabric'
        original = native(r)
        mock, meta = make_design(s, r, original)
        a = OUT / 'original' / (s['id'] + '.png')
        b = OUT / 'mockups' / (s['id'] + '.png')
        original.save(a); mock.save(b)
        pages.append({**s, **meta, 'original': str(a.relative_to(ROOT)), 'mockup': str(b.relative_to(ROOT)),
                      'capture': r['native']['capture'], 'capture_sha256': r['native']['sha256'],
                      'original_sha256': sha(a), 'mockup_sha256': sha(b)})
    build_pdf(pages)
    reader = PdfReader(PDF)
    assert len(reader.pages) == 6
    for i, s in enumerate(SPECS):
        assert s['title'] in reader.pages[i].extract_text()
    manifest = dict(pdf=str(PDF.relative_to(ROOT)), pdf_sha256=sha(PDF), scope='1.20.1 Fabric / Enchanted 3.1.14',
                    pages=6, container_screens=4, status_screens=1, complete_slot_borders=20,
                    status='Design mockups; not implemented or gameplay-tested', cases=pages)
    (OUT / 'design-manifest.json').write_text(json.dumps(manifest, indent=2))
    print(json.dumps({k: v for k, v in manifest.items() if k != 'cases'}, indent=2))


if __name__ == '__main__':
    main()
