"""Zenith Renewed: native reference captures and pixel-preserving SIU proposals.

Design artifact only. This script does not change the mod or compatibility code.
"""
from pathlib import Path
from io import BytesIO
from collections import Counter
import hashlib
import importlib.util
import json
import zipfile

from PIL import Image, ImageChops
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader
import pdfplumber

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/zenith-window-design'
TMP = ROOT / 'tmp/pdfs/zenith'
PDF = ROOT / 'output/pdf/siu-zenith-renewed-window-mockups.pdf'
JAR = ROOT / 'build/ui-modpack-audit-20261025/jars/6d6524da092a9bf81f08b921b21cca21a83ebfd7.jar'
RELEASE = 'https://modrinth.com/mod/zenith-renewed/version/T6bH9nd3'
SOURCE = 'https://github.com/Stalemated/ZenithRenewed/tree/1.20'
SRC = 'https://github.com/Stalemated/ZenithRenewed/blob/1.20/src/main/java/dev/shadowsoffire/apotheosis/'
N = Image.Resampling.NEAREST
TEAL, INK, MUTED = '#247C80', '#19373D', '#5E7076'
checks = []
module = importlib.util.spec_from_file_location('siu_pixel_art', ROOT/'tools/build-enchanted-window-mockups.py')
art = importlib.util.module_from_spec(module)
module.loader.exec_module(art)

SPECS = [
    dict(id='zenith-enchanting', title='Enchanting Table', kind='gray', rect=(7, 13, 170, 102),
         size=(178, 126), xy=(7, 27), slots=2, family='Enchanting',
         source='ench/table/ApothEnchScreen.java', access='Use an Enchanting Table. Insert an item and lapis; nearby enchanting blocks affect the offers.',
         purpose='Zenith replaces the vanilla enchanting screen with three offers, the animated book, and Eterna, Quanta and Arcana readouts. The capture shows a sword, lapis and three available offers.',
         design='Keep the complete working layout: book on the left, offers on the right, and all three stat bars below. Add one SIU header and retain the native information tab above its right edge.',
         preserve='2 slots / 3 offers / animated book / 3 stat bars / information tab',
         note='The information tab opens Zenith\'s native detail screen. Long offer and stat tooltips may extend outside the window.'),
    dict(id='zenith-library', title='Enchantment Library', kind='ornate', asset='library', end=143, slots=3,
         family='Libraries', source='ench/library/EnchLibraryScreen.java',
         access='Use an Enchantment Library. Deposit enchanted books to populate the list.',
         purpose='Stores enchantment points from deposited books and assembles enchanted books from the list. The upper round slot filters by an item; the blue slot receives deposits and the orange slot holds the assembled book.',
         design='Retain the wooden list, five visible entries, search field, narrow scrollbar and all three colored round slots. The SIU header uses a brown sampled from the native panel; no generic slot overlays.',
         preserve='3 round slots / search / 5 visible entries / native scrollbar',
         note='Keep list scrolling, left-click and Shift-click extraction, and right-click search clearing. The reference contains six enchantments, so scrolling remains necessary.'),
    dict(id='zenith-ender-library', title='Library of Alexandria', kind='ornate', asset='library', end=143, slots=3,
         family='Libraries', source='ench/library/EnchLibraryScreen.java',
         access='Use a Library of Alexandria, the higher-capacity library variant.',
         purpose='Uses the same three-slot library interface with higher native storage and enchantment limits. It is a separate workstation view of the same library screen, rather than a new screen type.',
         design='Use the same wooden inset and round slots as the basic library. Give this variant its proper title and use ellipsis plus close so the longer name remains readable.',
         preserve='Shared library layout / higher native limits / complete title',
         note='Do not invent additional slots or compress the list. The original library rules still decide what can be stored and extracted.'),
    dict(id='zenith-fletching', title='Fletching Table', kind='gray', rect=(5, 14, 149, 72),
         size=(176, 93), xy=(16, 27), slots=4, family='Fletching',
         source='village/fletching/FletchingScreen.java',
         access='Use a Fletching Table. The reference uses flint, sticks and feathers.',
         purpose='Zenith makes the Fletching Table usable for arrow recipes. Three vertically arranged ingredients feed one output; the native recipe shown produces six arrows.',
         design='Keep the arrow illustration, three input slots, progress arrow and complete enlarged output border together. Remove the inventory section and replace the old centered label with the SIU title.',
         preserve='3 inputs + 1 result / arrow illustration / full output border',
         note='Recipe inputs, output pickup and inventory transfers remain native. Do not add a second craft button or a second result slot.'),
    dict(id='zenith-salvaging-table', title='Salvaging Table', kind='ornate', asset='salvage', end=87, slots=18,
         family='Salvaging', source='adventure/affix/salvaging/SalvagingScreen.java',
         access='Use a Salvaging Table. Insert eligible equipment into the left grid.',
         purpose='Breaks eligible gear into salvage materials. The original menu has twelve inputs in a 4 x 3 grid, six outputs in a 2 x 3 grid, and a hammer action between them.',
         design='Preserve all eighteen dark recessed slots, their original spacing, the hammer and the full engraved frame. Keep the gray-purple window surface close to the source palette.',
         preserve='12 inputs + 6 outputs / native hammer / engraved frame',
         note='This reference is empty, so the native hammer is inactive. Eligibility checks, result previews and hover information belong to Zenith.'),
    dict(id='zenith-gem-cutting-table', title='Gem Cutting Table', kind='ornate', asset='gem_cutting', end=93, slots=4,
         family='Gem cutting', source='adventure/socket/gem/cutting/GemCuttingScreen.java',
         access='Use a Gem Cutting Table. Supply a valid gem and the materials required by its native upgrade.',
         purpose='Upgrades a gem in place using gem dust, a matching gem and rarity material. The center circle is the main gem slot; it is not a crafting input leading to a separate output.',
         design='Keep the entire rune triangle, four circular slots, scattered gem artwork and hammer. The complete lower rim closes the inset before the separated player inventory.',
         preserve='4 circular slots / upgrade in place / rune triangle / hammer',
         note='This reference is empty. Material hints and the upgrade action retain native validity rules; no extra result slot is added.'),
    dict(id='zenith-reforging-table', title='Reforging Table', kind='ornate', asset='reforge', end=179, slots=6,
         family='Reforging', source='adventure/affix/reforging/ReforgingScreen.java',
         access='Use a Reforging Table. Insert equipment, a rarity material and Sigils of Rebirth.',
         purpose='Rerolls equipment affixes through the large three-circle sigil and presents three native candidate items below it. This capture shows populated inputs and all three generated choices.',
         design='Keep the complete animated sigil, its diagonal links, all three choice circles and the cost dots beneath them. Retain the original working height rather than cropping the bottom choices.',
         preserve='3 input slots + 3 choices / animated sigil / cost dots',
         note='Candidate tooltips remain available outside the panel. Preserve native rarity, material, experience and choice-selection rules.'),
    dict(id='zenith-simple-reforging-table', title='Simple Reforging Table', kind='ornate', asset='reforge', end=179, slots=6,
         family='Reforging', source='adventure/affix/reforging/ReforgingScreen.java',
         access='Use a Simple Reforging Table. Its allowed reforging options follow the native workstation rules.',
         purpose='A second workstation variant of the same six-slot reforging screen. It uses different native reforging limits, while keeping the same sigil, inputs and three candidate positions.',
         design='Use exactly the same complete inset as the regular table. Keep the full variant name in the header; use ellipsis plus close when the full control row would crowd it.',
         preserve='Same 6-slot layout / native limits / full variant name',
         note='The empty circles and subdued runes are the native idle state. They are not missing sprites or replacement generic slots.'),
    dict(id='zenith-augmenting-table', title='Augmenting Table', kind='ornate', asset='augmenting', end=135, slots=2,
         family='Augmenting', source='adventure/affix/augmenting/AugmentingScreen.java',
         access='Use an Augmenting Table. Insert affixed equipment and Sigils of Enhancement.',
         purpose='Selects one of an item\'s affixes, shows its current value and range, and offers native upgrade or reroll actions. This capture contains a genuinely generated affixed sword and a selected affix.',
         design='Keep the item and sigil circles on the left, dropdown across the top, full explanation box and both action buttons below. The sword ornament and entire engraved lower rim stay visible.',
         preserve='2 circular slots / affix selector / explanation / upgrade + reroll',
         note='Button eligibility, sigil costs and affix alternatives stay native. Long explanations and comparison tooltips must not be clipped to the window.'),
    dict(id='zenith-augmenting-dropdown', title='Augmenting / open selector', window_title='Augmenting Table',
         kind='ornate', asset='augmenting', end=135, slots=2, family='Augmenting state',
         source='adventure/affix/augmenting/AugmentingScreen.java',
         access='Open the affix dropdown in the Augmenting Table.',
         purpose='The same Augmenting Table with its selector expanded. Graceful, Infernal, Intricate and Elusive are the actual generated affixes in this native reference.',
         design='Let the original dropdown draw over the explanation area in the same position and width. Preserve its row backgrounds, highlight and arrow, without moving it into the SIU title bar.',
         preserve='Native dropdown rows / original stacking order / original position',
         note='This is an interaction state, not an additional container type. The list must retain its native maximum visible entries and scrolling when more entries are present.'),
]


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def asset(name):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(BytesIO(z.read('assets/zenith/textures/gui/'+name+'.png'))).convert('RGBA')


def native_region(row, rect):
    b = row['state']['bounds']
    screen = Image.open(ROOT/row['capture']).convert('RGBA')
    assert screen.size == (1280, 900)
    return screen.crop(tuple(2*v for v in (b['x']+rect[0], b['y']+rect[1], b['x']+rect[2], b['y']+rect[3])))


def tinted_frame(w, h, palette):
    im = art.frame(w, h)
    if palette == (198, 198, 198):
        return im
    def tint(v):
        g = v[0]
        if g <= 198:
            rgb = tuple(round(c*g/198) for c in palette)
        else:
            rgb = tuple(round(c+(255-c)*(g-198)/57*.30) for c in palette)
        return (*rgb, v[3])
    im.putdata([tint(v) for v in im.get_flattened_data()])
    assert im.getpixel((5, 22))[:3] == palette
    return im


def header(im, title, palette):
    full = [0, 6, 4, 9, 2]
    cols = full if 7+art.tw(title)+7 <= im.width-5-len(full)*14 else [3, 2]
    start = im.width-5-len(cols)*14
    assert 7+art.tw(title)+7 <= start, title
    color = (64, 64, 64, 255) if palette == (198, 198, 198) else (231, 225, 232, 255)
    art.text(im, title, 7, 8, color)
    for i, col in enumerate(cols):
        sy = 22 if col == 0 else 0
        im.alpha_composite(art.CONTROLS.crop((col*11, sy, col*11+11, sy+11)), (start+i*14, 6))
    return cols


def build_mockup(spec, row):
    title = spec.get('window_title', spec['title'])
    if spec['kind'] == 'ornate':
        end = spec['end']
        rect = (0, 0, 176, end)
        a = asset(spec['asset'])
        palette = Counter(a.crop((9, 9, 167, end-9)).get_flattened_data()).most_common(1)[0][0][:3]
        # Enough width for the full title and compact window controls.
        w = max(190, 7+art.tw(title)+7+28+5)
        size, xy = (w, 26+end+7), ((w-176)//2, 26)
    else:
        rect, size, xy = spec['rect'], spec['size'], spec['xy']
        palette = (198, 198, 198)
    body = native_region(row, rect)
    if spec['id'] == 'zenith-fletching':
        # The arrow illustration extends left of the ordinary slot envelope.
        # The result is also a full 26 x 26 texture, not an 18 x 18 slot.
        working = asset('fletching_table').crop((5, 14, 170, 75)).convert('RGB')
        ink = ImageChops.difference(working, Image.new('RGB', working.size, (198,198,198))).getbbox()
        bounds = (ink[0]+5, ink[1]+14, ink[2]+5, ink[3]+14)
        assert rect[0] <= bounds[0] and rect[1] <= bounds[1] and rect[2] >= bounds[2] and rect[3] >= bounds[3], bounds
        assert rect[0] <= 119 and rect[1] <= 30 and rect[2] >= 145 and rect[3] >= 56
    # Verify full slot borders before any composition. Native circle silhouettes
    # and artwork stay in the complete original inset, never repainted.
    slots = [s for s in row['state']['slots'] if not s['player']]
    assert len(slots) == spec['slots'], spec['id']
    for s in slots:
        assert rect[0] <= s['x']-1 and rect[1] <= s['y']-1, (spec['id'], s)
        assert rect[2] >= s['x']+17 and rect[3] >= s['y']+17, (spec['id'], s)
    assert xy[0] >= 7 and xy[1] >= 26
    assert xy[0]+body.width//2 <= size[0]-7
    assert xy[1]+body.height//2 <= size[1]-7
    logical = tinted_frame(*size, palette)
    cols = header(logical, title, palette)
    im = logical.resize((size[0]*2, size[1]*2), N)
    im.alpha_composite(body, (xy[0]*2, xy[1]*2))
    assert im.crop((xy[0]*2, xy[1]*2, xy[0]*2+body.width, xy[1]*2+body.height)).tobytes() == body.tobytes()
    if spec['id'] == 'zenith-enchanting':
        # Preserve the native silhouette, including transparent stepped corners.
        tab = asset('enchanting_table').crop((176, 0, 203, 15)).resize((54, 30), N)
        with_tab = Image.new('RGBA', (im.width, im.height+30))
        with_tab.alpha_composite(im, (0, 30))
        with_tab.alpha_composite(tab, (145*2, 0))
        im = with_tab
    checks.append(dict(id=spec['id'], slots=len(slots), rect=rect, destination=xy,
                       outer_size=size, palette=palette, chrome=cols,
                       native_body_preserved_byte_for_byte=True,
                       native_body_sha256=hashlib.sha256(body.tobytes()).hexdigest()))
    return im


def setup():
    refs = json.loads((OUT/'native-references.json').read_text())
    assert refs['siu_loaded'] is False
    cases = {r['id']:r for r in refs['cases']}
    for r in refs['cases']:
        assert sha(ROOT/r['capture']) == r['sha256'], r['id']
    pages = []
    for spec in SPECS:
        row = cases[spec['id']]
        b = row['state']['bounds']
        rect = (0, -15 if spec['id']=='zenith-enchanting' else 0, b['width'], b['height'])
        original = native_region(row, rect)
        mockup = build_mockup(spec, row)
        paths = {}
        for name, im in [('original', original), ('mockup', mockup)]:
            p = OUT/name/(spec['id']+'.png'); p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
            paths[name] = str(p.relative_to(ROOT)).replace('\\','/')
        pages.append({**spec, **paths, 'reference_sha256':row['sha256'], 'screen':row['state']['screen']})
    # Inventory-free appendix references retain their native UI, without SIU chrome.
    for id, box in [('zenith-enchanting-info',(197,137,443,313)), ('zenith-chronicle',(169,138,471,325))]:
        screen = Image.open(ROOT/cases[id]['capture']).convert('RGBA')
        im = screen.crop(tuple(2*v for v in box))
        p = OUT/'original'/(id+'.png');im.save(p)
    return refs, pages


def build_pdf(pages):
    pdfmetrics.registerFont(TTFont('Segoe', 'C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB', 'C:/Windows/Fonts/segoeuib.ttf'))
    W, H, total = 1191, 842, len(pages)+2
    PDF.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(PDF), pagesize=(W,H), pageCompression=1)
    c.setTitle('Zenith Renewed / SIU Window Mockups')
    c.setAuthor("Salt's Inventory Update / Project Everything")

    def txt(s,x,y,size=12,bold=False,color=INK):
        c.setFillColor(HexColor(color));c.setFont('SegoeB' if bold else 'Segoe',size);c.drawString(x,y,s)

    def para(s,x,y,w,size=12,leading=18):
        line=''
        for word in s.split():
            candidate=(line+' '+word).strip()
            if pdfmetrics.stringWidth(candidate,'Segoe',size)>w:
                txt(line,x,y,size);y-=leading;line=word
            else:line=candidate
        if line:txt(line,x,y,size);y-=leading
        return y

    def link(label,url,x,y,size=10):
        txt(label,x,y,size,color=TEAL)
        c.linkURL(url,(x,y-2,x+pdfmetrics.stringWidth(label,'Segoe',size),y+size+2),relative=0)

    def base(title,sub,i):
        c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
        txt('PROJECT EVERYTHING / ZENITH RENEWED',36,H-34,10,True,TEAL)
        txt(title,36,H-77,28,True)
        txt(sub,36,H-103,11,color=MUTED)
        c.setStrokeColor(HexColor('#D7E2E3'));c.line(36,56,W-36,56)
        txt('Design proposals only / Native references: Minecraft 1.20.1 Fabric, Zenith Renewed 2.0.2 / No compatibility code changed',36,35,9,color=MUTED)
        txt(f'{i} / {total}',W-76,35,9,color=MUTED)

    def image(p,x,y,w,h,scale=None):
        im=Image.open(ROOT/p).convert('RGBA')
        if scale is None:scale=min(w/(im.width/2),h/(im.height/2))
        dw,dh=im.width/2*scale,im.height/2*scale
        assert dw<=w+.1 and dh<=h+.1,(p,dw,dh,w,h)
        enlarged=im.resize((im.width*3,im.height*3),N)
        c.drawImage(ImageReader(enlarged),x+(w-dw)/2,y+(h-dh)/2,dw,dh,mask='auto')

    base('Zenith Renewed: window design', '7 container screen types / 9 workstation views / 1 additional interaction-state comparison',1)
    c.bookmarkPage('overview');c.addOutlineEntry('Overview','overview',level=0)
    txt('KEEP ZENITH\'S IDENTITY',36,695,10,True,TEAL)
    y=para('Use SIU for window movement and controls, while preserving Zenith\'s complete working panels: engraved metal, rune circles, wooden library lists and native slot shapes. The player inventory opens as its own SIU window.',36,670,527,15,22)
    y=para('The left side of every comparison is an unchanged crop from a real game capture with SIU absent. The right side is a design mockup using that same working artwork and the existing SIU frame and controls.',36,y-12,527,12,18)
    txt('WINDOW COVERAGE',36,477,10,True,TEAL)
    coverage=[('Enchanting Table','2 slots + 3 offers'),('Enchantment Library','3 round slots'),('Library of Alexandria','Library variant'),('Fletching Table','3 inputs + 1 result'),('Salvaging Table','12 inputs + 6 outputs'),('Gem Cutting Table','4 circles; upgrade in place'),('Reforging Table','3 inputs + 3 choices'),('Simple Reforging Table','Reforging variant'),('Augmenting Table','2 slots + affix controls')]
    for i,(name,detail) in enumerate(coverage):
        yy=450-i*29
        if i%2==0:c.setFillColor(HexColor('#F1F5F5'));c.roundRect(36,yy-7,527,26,3,fill=1,stroke=0)
        txt(name,47,yy,12,True);txt(detail,324,yy,11,color=MUTED)
    txt('NOT NEW WINDOWS',36,164,10,True,TEAL)
    para('Enchanting details and the Chronicle stay native. Shared anvil, brewing, smithing and trading screens reuse their existing SIU integrations. See the appendix.',36,143,527,11,16)
    c.setFillColor(HexColor('#EEF4F4'));c.roundRect(607,119,548,586,9,fill=1,stroke=0)
    txt('PROPOSED WINDOW LANGUAGE',625,681,10,True,TEAL)
    for id,box in [('zenith-gem-cutting-table',(622,490,245,169)),('zenith-salvaging-table',(890,490,245,169)),('zenith-library',(702,209,360,262))]:
        p=next(v for v in pages if v['id']==id);image(p['mockup'],*box)
    link('Publisher release',RELEASE,625,151);link('Primary source',SOURCE,752,151)
    txt('Native layout retained / no new crafting behavior',625,133,10,color=MUTED)
    c.showPage()

    for i,p in enumerate(pages,2):
        subtitle=('Additional interaction state / Same Augmenting Table' if p['family']=='Augmenting state' else f"{p['slots']} container slots / Separate SIU player inventory")
        base(p['title'],subtitle,i)
        c.bookmarkPage(p['id']);c.addOutlineEntry(p['title'],p['id'],level=0)
        imgs=[Image.open(ROOT/p[k])for k in ['original','mockup']]
        scale=min(2.45,504/max(im.width/2 for im in imgs),392/max(im.height/2 for im in imgs))
        for x,label,key in [(36,'ORIGINAL IN GAME / SIU ABSENT','original'),(613,'PROPOSED SIU WINDOW / MOCKUP','mockup')]:
            txt(label,x,698,10,True,TEAL)
            c.setFillColor(HexColor('#EEF4F4'));c.roundRect(x,257,542,425,8,fill=1,stroke=0)
            image(p[key],x+17,270,508,399,scale)
        txt('WHAT IT DOES',36,231,9,True,TEAL)
        end1=para(p['purpose'],36,210,527,12,18)
        txt('WINDOW DESIGN',613,231,9,True,TEAL)
        end2=para(p['design'],613,210,542,12,18)
        assert end1>=133 and end2>=133,(p['id'],end1,end2)
        txt('KEEP',36,118,9,True,TEAL);txt(p['preserve'],72,118,10,color=MUTED)
        end3=para(p['note'],36,97,1118,10,14);assert end3>=62
        link('Native screen source',SRC+p['source'],613,118,10)
        c.showPage()

    base('Appendix: interfaces that stay native','Inventory-only scope / No SIU window mockups for guidebooks or information screens',total)
    c.bookmarkPage('appendix');c.addOutlineEntry('Native-only interfaces and shared workflows','appendix',level=0)
    for x,label,id in [(36,'AVAILABLE ENCHANTMENTS','zenith-enchanting-info'),(613,'CHRONICLE OF SHADOWS','zenith-chronicle')]:
        txt(label,x,698,10,True,TEAL)
        c.setFillColor(HexColor('#EEF4F4'));c.roundRect(x,377,542,305,8,fill=1,stroke=0)
        image('output/zenith-window-design/original/'+id+'.png',x+16,391,510,277)
    y=para('Open from the enchanting information tab. Keep the native power slider, enchantment list, scrolling and stats; return to the same enchanting session. This is an information screen, with no inventory slots.',36,354,527,12,18)
    y2=para('Open the Chronicle of Shadows guidebook. Keep its native book pages, categories, navigation, search and resizing. Guidebook reading does not become a floating inventory window.',613,354,542,12,18)
    assert min(y,y2)>264
    txt('SHARED UI AND COVERAGE',36,251,10,True,TEAL)
    end=para('Zenith also changes recipes and interactions used through vanilla anvil, brewing, smithing and villager-trading screens. Reuse the current SIU implementations there. Spawner upgrades and equipment tooltips do not add owned container screens; recipe-viewer pages stay in their recipe browser. Dependency-provided attribute panels are outside Zenith\'s seven container screen types.',36,228,1119,12,18)
    txt('DESIGN CHECKS',36,137,10,True,TEAL)
    txt('All 7 concrete container screen classes accounted for; both library and reforging variants included.',36,116,11,color=MUTED)
    txt('All native slot envelopes and complete working panels preserved. Layout review only; no implementation or gameplay-suite claim.',36,97,11,color=MUTED)
    link('Publisher release (2.0.2)',RELEASE,36,75,10)
    link('Primary source / 1.20 branch',SOURCE,217,75,10)
    link('Enchanting information source',SRC+'ench/table/EnchantingInfoScreen.java',423,75,10)
    c.showPage();c.save()


def validate(pages):
    reader=PdfReader(PDF)
    assert len(reader.pages)==len(pages)+2
    with pdfplumber.open(PDF) as pdf:
        for i,page in enumerate(pdf.pages,1):
            assert all(0<=ch['x0']<1191 and ch['x1']<=1192 and 0<=ch['top']<842 and ch['bottom']<=843 for ch in page.chars),i
            text=page.extract_text() or ''
            assert 'Design proposals only' in text
            assert '\ufffd' not in text and '\u25a0' not in text
    assert len(checks)==10 and len({p['family']for p in pages if p['family']!='Augmenting state'})==7
    manifest=dict(minecraft='1.20.1',loader='Fabric',zenith_version='2.0.2',siu_installed_in_references=False,
                  container_screen_types=7,workstation_views=9,additional_interaction_states=1,pages=len(reader.pages),
                  pdf=str(PDF.relative_to(ROOT)).replace('\\','/'),pdf_sha256=sha(PDF),publisher=RELEASE,
                  source=SOURCE,native_jar_sha256=sha(JAR),comparisons=pages,checks=checks,
                  validation='Native reference hashes, complete slot envelopes, byte-identical working regions and PDF text bounds passed. Visual review is recorded separately.')
    (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2))
    print(json.dumps(dict(pdf=str(PDF),pages=len(reader.pages),comparisons=len(pages),checks=len(checks)),indent=2))


if __name__=='__main__':
    refs,pages=setup();build_pdf(pages);validate(pages)
