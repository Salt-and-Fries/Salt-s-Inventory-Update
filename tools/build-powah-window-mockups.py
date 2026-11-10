"""Powah 5.0.11 native references and proposed SIU windows; design only.

Immutable, hash-verified native game captures supply all working graphics.
Publisher texture alpha removes world pixels between the native modules.
No shipping core or compatibility implementation is modified by this script.
"""
from pathlib import Path
from io import BytesIO
import hashlib
import importlib.util
import json
import math
import zipfile
from collections import Counter

from PIL import Image, ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/powah-window-design'
PDF = ROOT / 'output/pdf/siu-powah-window-mockups.pdf'
JAR = ROOT / 'build/ui-modpack-audit-20261025/jars/8885884da9e4470520faed11347507e3195e642d.jar'
SOURCE_COMMIT = 'b7343d065eec0c57c4bd99dcde7bbdbccccf1307'
SOURCE = 'https://github.com/Technici4n/Powah/tree/' + SOURCE_COMMIT
RELEASE = 'https://modrinth.com/mod/powah/version/6beFT60p'
N = Image.Resampling.NEAREST

# Reuse the project's own SIU pixel frame, controls and Minecraft glyph helpers.
_spec = importlib.util.spec_from_file_location('siu_mockup_art', ROOT / 'tools/build-enchanted-window-mockups.py')
ART = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ART)

SPECS = [
    dict(key='reactor', screen='ReactorScreen', asset='reactor', body_h=75,
         title='Reactor', representative='own-powah-placed-reactor', slots=5,
         does='Generates FE from uraninite. Carbon, redstone and coolant support the reactor; the menu exposes its fuel, temperature and energy state.',
         keep='Keep all five slots, the reactor diagram, every narrow resource gauge, the full-height coolant tank and the generation-mode toggle on the left.',
         detail='The mode tab and right-side configuration controls remain attached to the parent. The empty-looking diagram is retained because it carries the native reactor instrumentation.',
         opener='Use a formed reactor multiblock.',
         reserved='Five item slots; fuel, carbon, redstone, solid coolant, temperature, energy and fluid overlays.'),
    dict(key='furnator', screen='FurnatorScreen', asset='furnator', body_h=75,
         title='Furnator', representative='powah-furnator-basic', slots=2,
         does='Burns solid fuel to generate FE. The energy-item slot, fuel input, carbon indicator and animated heat symbol form its working area.',
         keep='Preserve the native fuel slot, energy slot, tall energy gauge and heat artwork at their original relative positions.',
         detail='Keep the subtle background marks and the complete animation envelope. Compact height comes from removing the inventory slab, rather than shaving the working panel.',
         opener='Use a placed Furnator.',
         reserved='Fuel and energy slots; carbon gauge and heat animation.'),
    dict(key='magmator', screen='MagmatorScreen', asset='magmator', body_h=75,
         title='Magmator', representative='powah-magmator-basic', slots=1,
         does='Converts supported hot fluids into FE. Its working area includes an energy-item slot, an energy gauge, a heat symbol and a tall fluid tank.',
         keep='Retain the complete tank, both end borders, the energy slot and the centered heat animation. Keep the native left-to-right spacing.',
         detail='The central panel remains full size so the heat animation and tank tooltips retain their native relationship. Fluid contents must fill the existing tank, without changing its dimensions.',
         opener='Use a placed Magmator.',
         reserved='One energy slot; heat animation and the 14 x 65 fluid-fill area.'),
    dict(key='thermo', screen='ThermoScreen', asset='thermo', body_h=75,
         title='Thermo Generator', representative='powah-thermo-generator-basic', slots=1,
         does='Generates FE using heat below the block and coolant. The native display reports generation percentage and FE/t.',
         keep='Keep the generation readout, energy-item slot, left energy gauge and the entire right-hand coolant tank together.',
         detail='The coolant strip is preserved from top to bottom, including its rim. Space for changing numeric values is retained instead of fitting the window only to the idle text.',
         opener='Use a placed Thermo Generator.',
         reserved='One energy slot; live efficiency text and the 14 x 65 coolant fill.'),
    dict(key='solar', screen='SolarScreen', asset='wide_energy', body_h=50,
         title='Solar Panel', representative='powah-solar-panel-basic', slots=0,
         does='Generates solar power and exposes its stored FE and transfer-rate readout. The native menu has no machine item slots.',
         keep='Keep the wide shaded energy display, its live fill and both numeric lines. Preserve the native side configuration and redstone controls.',
         detail='Use a shallow status window. Add no storage grid or sorting buttons to a menu that has no machine inventory.',
         opener='Use a placed Solar Panel.',
         reserved='Live energy fill and both numeric lines; zero machine slots.'),
    dict(key='energy-cell', screen='EnergyCellScreen', asset='energy_cell', body_h=50,
         title='Energy Cell', representative='powah-energy-cell-basic', slots=2,
         does='Stores FE and exposes two energy-item slots alongside its stored-energy and transfer-rate display.',
         keep='Keep the two vertically stacked slots on the left, their lightning silhouettes and the complete energy display on the right.',
         detail='Preserve the module proportions and transfer controls. The Creative variant uses the same layout; its native numeric display is retained in the variant gallery.',
         opener='Use a placed Energy Cell.',
         reserved='Two energy slots, original ghost icons, live fill and FE readouts.'),
    dict(key='ender-cell', screen='EnderCellScreen', asset='ender_cell', body_h=73,
         title='Ender Cell', representative='powah-ender-cell-basic', slots=2,
         does='Provides the channel-based Ender energy interface. Its working area combines two energy-item slots, a shared-energy display and a numbered channel strip.',
         keep='Use a teal SIU frame sampled from Powah artwork. Keep both lightning slots, the black-teal energy display and all twelve numbered channel positions.',
         detail='Enabled channel count and the selected channel continue to come from the native tier and state. Preserve the off-screen extender helper behavior without inventing an extra visible item slot.',
         opener='Use a placed Ender Cell.',
         reserved='Two visible energy slots; twelve channel positions and native selection states.'),
    dict(key='ender-gate', screen='EnderCellScreen', asset='ender_cell', body_h=73,
         title='Ender Gate', representative='powah-ender-gate-basic', slots=2,
         does='Connects machinery to the Ender energy network. It opens the same screen class as the Ender Cell, with the gate title and tier-specific state.',
         keep='Share the Ender Cell window design, including its teal shell, both item slots, complete gauge and channel strip.',
         detail='Treat this as a distinct opening route that reuses one screen layout. Do not create a different gate-specific dashboard or omit the channel controls.',
         opener='Use a placed Ender Gate.',
         reserved='The complete shared Ender layout, with gate-specific title and native state.'),
    dict(key='hopper', screen='EnergyHopperScreen', asset='wide_energy', body_h=50,
         title='Energy Hopper', representative='powah-energy-hopper-basic', slots=0,
         does='Charges supported energy items in an adjacent container. Its own screen is a status/configuration interface with no machine item slots.',
         keep='Keep the full-width FE display and side controls. Do not copy the player inventory into the hopper window or invent new charging slots.',
         detail='The separate SIU player inventory remains available when needed. Adjacent-container charging behavior stays with Powah rather than becoming a new window action.',
         opener='Use a placed Energy Hopper.',
         reserved='Energy display and native configuration controls; zero machine slots.'),
    dict(key='discharger', screen='DischargerScreen', asset='discharger', body_h=75,
         title='Energy Discharger', representative='powah-energy-discharger-basic', slots=7,
         does='Extracts FE from energy items. Seven item slots sit in one row below the stored-energy and extraction-rate display.',
         keep='Keep all seven separately framed slots, including their battery silhouettes. Preserve the original gaps and the complete gauge above them.',
         detail='The slot row belongs to the machine and must remain after the player inventory is removed. Use the compact More and Close header where the long tier name needs room.',
         opener='Use a placed Energy Discharger.',
         reserved='Seven item slots with native overlays; live energy display.'),
    dict(key='transmitter', screen='PlayerTransmitterScreen', asset='player_transmitter', body_h=50,
         title='Player Transmitter', representative='powah-player-transmitter-basic', slots=1,
         does='Uses a player card to identify the player receiving wireless charging. The native panel contains a card slot, antenna/status artwork and an FE display.',
         keep='Keep the card slot and its card silhouette on the left, the complete antenna above it and the wide energy display on the right.',
         detail='Reserve the full native on/off indicator area. Keep the card slot distinct from ordinary inventory slots and preserve its tooltip and accepted-item behavior.',
         opener='Use a placed Player Transmitter.',
         reserved='One card slot, full antenna and on/off indicator, live energy display.'),
    dict(key='cable', screen='CableScreen', asset='cable', body_h=29,
         title='Energy Cable', representative='own-powah-connected-cable', slots=0,
         does='Configures the clicked cable face. The left button cycles that face\'s transfer mode; the centered word identifies the face, and the right button controls redstone behavior.',
         keep='Keep the two native buttons and the centered face label in the same horizontal bar. Put the cable title in an SIU header above it.',
         detail='North is the face used by this captured example, not a new direction selector. No player inventory or machine slots are added to this settings-only window.',
         opener='Use a connected cable face to open its native configuration panel.',
         reserved='Transfer-mode button, clicked-face label and native redstone button.'),
]


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def jar_texture(name):
    with zipfile.ZipFile(JAR) as z:
        path = next(p for p in z.namelist() if p.endswith('/textures/gui/container/' + name + '.png'))
        return Image.open(BytesIO(z.read(path))).convert('RGBA')


def tinted_frame(w, h, ender=False):
    im = ART.frame(w, h)
    # Keep SIU's exact border geometry; use the native surface's actual palette.
    gray = jar_texture('blank').getpixel((20, 20))[:3]
    end = jar_texture('ender_blank')
    palette = {}
    # Sample opaque native artwork, not transparent atlas padding.
    if ender:
        widget = jar_texture('ender_widget')
        palette = {0: widget.getpixel((0, 0))[:3],
                   51: end.getpixel((19, 19))[:3],
                   85: end.getpixel((20, 20))[:3],
                   139: widget.getpixel((2, 2))[:3],
                   198: end.getpixel((20, 20))[:3],
                   255: widget.getpixel((1, 1))[:3]}
    pix = im.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = pix[x, y]
            if not a:
                continue
            if ender:
                col = palette[min(palette, key=lambda n: abs(n-r))]
            elif r == 198 and g == 198 and b == 198:
                col = gray
            else:
                col = (r, g, b)
            pix[x, y] = (*col, a)
    return im


def header(im, title, ender):
    full = [0, 6, 4, 9, 2]
    cols = full if 7 + ART.tw(title) + 7 <= im.width-5-len(full)*14 else [3, 2]
    start = im.width-5-len(cols)*14
    assert 7+ART.tw(title)+7 <= start, title
    ART.text(im, title, 7, 7, (194, 236, 232, 255) if ender else (48, 48, 48, 255))
    for i, col in enumerate(cols):
        sy = 22 if col == 0 else 0
        im.alpha_composite(ART.CONTROLS.crop((11*col, sy, 11*col+11, sy+11)), (start+14*i, 5))
    return cols


def native_view(row):
    n = row['native']
    p = ROOT / n['capture']
    assert sha(p) == n['sha256'], row['id']
    full = Image.open(p).convert('RGBA')
    assert full.size == (1280, 900)
    b = n['bounds']
    # Logical pixels are copied 1:1. Crop the original with enough world margin
    # to include the floating title and all protruding native configuration tabs.
    rect = (b['x']-24, b['y']-23, b['x']+b['width']+31, b['y']+b['height']+7)
    original = full.crop(tuple(2*v for v in rect))
    logical = full.resize((640, 450), N)
    def part(x, y, w, h):
        return logical.crop((b['x']+x, b['y']+y, b['x']+x+w, b['y']+y+h))
    return original, part


def choose_spec(row):
    s = row['native']['screen'].split('.')[-1]
    if s == 'EnderCellScreen':
        return next(t for t in SPECS if t['key'] == ('ender-gate' if 'ender-gate-' in row['id'] else 'ender-cell'))
    return next(t for t in SPECS if t['screen'] == s)


def design(row, spec, width):
    original, part = native_view(row)
    cable = spec['key'] == 'cable'
    reactor = spec['key'] == 'reactor'
    ender = spec['key'].startswith('ender-')
    body_w, body_h = (153 if cable else 176), spec['body_h']
    bx, by = (width-body_w)//2, 25
    height = by+body_h+7
    main = tinted_frame(width, height, ender)
    title = row['native']['title']
    controls = header(main, title, ender)
    art = jar_texture(spec['asset']).crop((0, 0, body_w, body_h))
    mask = art.getchannel('A')
    # Native slot overlays are rendered by Powah and lie inside this mask.
    body = part(0, 0, body_w, body_h)
    body.putalpha(mask)
    main.alpha_composite(body, (bx, by))
    left = 14 if reactor else 0
    right = 0 if cable else 22
    result = Image.new('RGBA', (width+left+right, height))
    result.alpha_composite(main, (left, 0))
    ov = jar_texture('button_ov')
    attachments = []
    if not cable:
        for source, dest, texrect, label in [
            ((176, 4, 23, 25), (left+width-1, by+4), (0, 0, 23, 25), 'six faces + all sides'),
            ((176, 30, 15, 16), (left+width-1, by+30), (23, 0, 38, 16), 'redstone mode'),
        ]:
            patch = part(*source)
            patch.putalpha(ov.crop(texrect).getchannel('A'))
            result.alpha_composite(patch, dest)
            attachments.append(dict(label=label, source=list(source), destination=list(dest), size=list(patch.size)))
    if reactor:
        patch = part(-15, 6, 15, 16)
        # Generation-mode background is a separate sprite in the reactor atlas.
        m = jar_texture('reactor').crop((209, 0, 224, 16)).getchannel('A')
        patch.putalpha(m)
        result.alpha_composite(patch, (0, by+6))
        attachments.append(dict(label='generation mode', source=[-15, 6, 15, 16], destination=[0, by+6], size=[15, 16]))
    slots = [s for s in row['native'].get('slots', []) if not s['player'] and s['y'] < 500]
    assert len(slots) == spec['slots'], (row['id'], len(slots))
    slot_rects = []
    for s in slots:
        # All 16px slots retain at least the native one-pixel bevel on all sides.
        rect = (s['x']-1, s['y']-1, s['x']+17, s['y']+17)
        assert 0 <= rect[0] < rect[2] <= body_w and 0 <= rect[1] < rect[3] <= body_h
        slot_rects.append(dict(index=s['index'], source=list(rect), destination=[left+bx+rect[0], by+rect[1], left+bx+rect[2], by+rect[3]]))
    # Every opaque native pixel must survive the alpha composition unchanged.
    final_body = result.crop((left+bx, by, left+bx+body_w, by+body_h))
    a = mask.load(); p = body.load(); q = final_body.load()
    opaque = 0
    for y in range(body_h):
        for x in range(body_w):
            if a[x, y] == 255:
                assert p[x, y] == q[x, y], (row['id'], x, y)
                opaque += 1
    for dock in attachments:
        x, y = dock['destination']; w, h = dock['size']
        assert 0 <= x and 0 <= y and x+w <= result.width and y+h <= result.height
    assert bx >= 7 and bx+body_w <= width-7
    assert by >= 24 and height-(by+body_h) >= 7
    return original, result, dict(id=row['id'], family=spec['key'], title=title,
        screen=row['native']['screen'], source_capture=row['native']['capture'],
        source_sha256=row['native']['sha256'], body_source=[0, 0, body_w, body_h],
        body_destination=[left+bx, by], main_size=[width, height], size=list(result.size),
        header_controls=controls, preserved_visible_slots=len(slots), slot_rects=slot_rects,
        offscreen_helper_slots=[s['index'] for s in row['native'].get('slots', []) if not s['player'] and s['y'] >= 500],
        preserved_opaque_body_pixels=opaque, attachments=attachments)


def build_pdf(rows):
    pdfmetrics.registerFont(TTFont('Segoe', 'C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB', 'C:/Windows/Fonts/segoeuib.ttf'))
    W, H = 1191, 842
    c = canvas.Canvas(str(PDF), pagesize=(W, H), pageCompression=1)
    c.setTitle('Powah! / SIU window design comparisons')
    c.setAuthor("Salt's Inventory Update / Project Everything")
    c.setSubject('Design proposals, not implemented SIU gameplay captures. Native references: Minecraft 1.20.1 Fabric, Powah 5.0.11.')
    ink, teal, muted, light = '#20373D', '#197C7C', '#5E7278', '#EFF5F5'
    page = 0

    def txt(s, x, y, size=12, bold=False, color=ink):
        c.setFillColor(HexColor(color)); c.setFont('SegoeB' if bold else 'Segoe', size)
        c.drawString(x, y, s)

    def wrap(s, x, y, width, size=12, leading=18, bold=False, color=ink):
        words = s.split(); line = ''; start = y
        for word in words:
            trial = (line+' '+word).strip()
            if pdfmetrics.stringWidth(trial, 'SegoeB' if bold else 'Segoe', size) > width and line:
                txt(line, x, y, size, bold, color); y -= leading; line = word
            else:
                line = trial
        if line: txt(line, x, y, size, bold, color); y -= leading
        return y

    def start(title, subtitle, section='WINDOW DESIGN REVIEW'):
        nonlocal page
        page += 1
        c.setFillColor(HexColor('#FFFFFF')); c.rect(0, 0, W, H, fill=1, stroke=0)
        txt('PROJECT EVERYTHING / POWAH! / '+section, 42, H-42, 10, True, teal)
        txt(title, 42, H-79, 27, True)
        txt(subtitle, 42, H-104, 11, False, muted)
        c.setStrokeColor(HexColor('#D6E3E3')); c.line(42, 44, W-42, 44)
        txt('Native references: 1.20.1 Fabric / Powah 5.0.11  |  Right-hand images are proposed SIU windows.', 42, 26, 9, False, muted)
        txt(str(page), W-60, 26, 10, True, muted)

    def panel(path, box, maxscale=None):
        x, y, w, h = box
        c.setFillColor(HexColor(light)); c.roundRect(x, y, w, h, 9, fill=1, stroke=0)
        im = Image.open(path).convert('RGBA')
        scale = min((w-22)/im.width, (h-22)/im.height)
        if maxscale: scale = min(scale, maxscale)
        iw, ih = im.width*scale, im.height*scale
        # Enlarge raster pixels with nearest-neighbor before embedding in PDF.
        im = im.resize((im.width*4, im.height*4), N)
        c.drawImage(ImageReader(im), x+(w-iw)/2, y+(h-ih)/2, iw, ih, mask='auto')

    def link(label, url, x, y):
        txt(label, x, y, 10, False, teal)
        c.linkURL(url, (x, y-2, x+pdfmetrics.stringWidth(label, 'Segoe', 10), y+11), relative=0)

    def end(): c.showPage()

    start('Powah! as SIU windows', 'All 11 concrete container screen types, plus the Ender Gate opening variant. Design proposals only.')
    txt('Keep the machinery. Remove the embedded inventory.', 42, 695, 21, True)
    wrap('A compact SIU shell holds Powah\'s original gauges, slot silhouettes, channel controls and machine artwork. External configuration controls stay attached in their original groups.', 42, 663, 1070, 14, 21)
    heroes = [next(r for r in rows if r['id'] == k) for k in ['own-powah-placed-reactor', 'powah-ender-cell-nitro', 'powah-energy-discharger-basic']]
    for i, r in enumerate(heroes):
        panel(r['mockup_path'], (42+i*371, 383, 357, 203))
        txt(r['title'], 54+i*371, 363, 13, True)
    blocks = [
        ('Faithful working area', 'Every visible machine slot and the full animation/fill envelope remains. Transparent gaps use the window surface, rather than a rectangle of darkened world.'),
        ('One movable parent', 'The title and window controls share one header. Face configuration, redstone mode and the reactor mode tab follow that parent; they do not become independent windows.'),
        ('Complete scope', 'Twelve main comparisons explain each layout and opening route. The appendix includes all 73 captured tier/variant references beside their own mockups.'),
    ]
    for i, (heading, body) in enumerate(blocks):
        x = 42+i*371
        txt(heading, x, 306, 15, True)
        wrap(body, x, 279, 340, 12, 18)
    wrap('Player inventory: use SIU\'s existing separate inventory window for item transfers. It is omitted from the proposed machine previews so the working layout is easy to inspect. Long titles use More (...) and Close; other applicable window actions remain in More.', 42, 138, 1090, 12, 18)
    link('Publisher release', RELEASE, 42, 65); link('Pinned upstream source', SOURCE, 170, 65)
    end()

    for spec in SPECS:
        r = next(r for r in rows if r['id'] == spec['representative'])
        start(spec['title'], 'Reference: '+r['title']+'  |  Open: '+spec['opener'])
        txt('ORIGINAL IN GAME / SIU ABSENT', 42, 695, 11, True, teal)
        txt('PROPOSED SIU WINDOW / MOCKUP', 611, 695, 11, True, teal)
        panel(r['original_path'], (42, 277, 538, 400))
        panel(r['mockup_path'], (611, 277, 538, 400), maxscale=2.5)
        # Three distinct concise descriptions avoid squeezing text below images.
        txt('What it does', 42, 245, 13, True)
        wrap(spec['does'], 42, 222, 333, 12, 17)
        txt('Window design', 416, 245, 13, True)
        wrap(spec['keep'], 416, 222, 333, 12, 17)
        txt('Keep working', 790, 245, 13, True)
        wrap(spec['detail'], 790, 222, 355, 12, 17)
        txt('PRESERVED: '+spec['reserved'], 42, 91, 10, True, teal)
        txt('Native screen: '+spec['screen']+'  |  Capture: '+r['id'], 42, 65, 9, False, muted)
        end()

    start('Coverage and behavior', 'Inventory/container scope. The native screen registry defines the container inventory.', 'SCOPE APPENDIX')
    table = [
        ('Reactor', '5', 'Starter representative', 'One formed-multiblock capture'),
        ('Energy Cable', '0', 'Starter representative', 'One connected-face capture'),
        ('Energy Cell', '2', 'Seven tiers + Creative', '8 comparisons'),
        ('Ender Cell / Ender Gate', '2 each', 'Seven tiers for each opening route', '14 comparisons / one shared screen'),
        ('Energy Discharger', '7', 'Seven tiers', '7 comparisons'),
        ('Energy Hopper', '0', 'Seven tiers', '7 comparisons'),
        ('Furnator', '2', 'Seven tiers', '7 comparisons'),
        ('Magmator', '1', 'Seven tiers', '7 comparisons'),
        ('Player Transmitter', '1', 'Seven tiers', '7 comparisons'),
        ('Solar Panel', '0', 'Seven tiers', '7 comparisons'),
        ('Thermo Generator', '1', 'Seven tiers', '7 comparisons'),
    ]
    cols = [42, 314, 415, 804]
    for x, s in zip(cols, ['INTERFACE', 'VISIBLE SLOTS', 'REFERENCE VARIANTS', 'COVERAGE']): txt(s, x, 687, 10, True, teal)
    y = 654
    for i, line in enumerate(table):
        if i%2 == 0:
            c.setFillColor(HexColor(light)); c.rect(36, y-11, W-72, 30, fill=1, stroke=0)
        for x, s in zip(cols, line): txt(s, x, y, 11)
        y -= 33
    wrap('Seven tiers: Starter, Basic, Hardened, Blazing, Niotic, Spirited and Nitro. Reactor and cable tiers share their respective screen classes; this report uses one captured example of each, without inventing images of uncaptured states.', 42, 264, 1090, 12, 18)
    wrap('Native actions retained: six face buttons plus the all-sides action; redstone mode; the reactor generation toggle; tier-limited Ender channels; native item restrictions, slot overlays, tooltip targets and live gauge fills. The Ender screen\'s off-screen helper slot is not a third visible slot.', 42, 207, 1090, 12, 18)
    wrap('Design verification: all 73 source images match their audit hashes. Every opaque working-area pixel is checked against its source, every visible slot fits inside its extraction, and all attached controls fit inside the preview canvas. These are layout checks, not a gameplay test of an implemented compatibility adapter.', 42, 133, 1090, 12, 18)
    end()

    # All 73 native screenshots and derived tier proposals are explicitly shown.
    ordered = []
    tier_order = ['starter', 'basic', 'hardened', 'blazing', 'niotic', 'spirited', 'nitro', 'creative']
    for spec in SPECS:
        subset = [r for r in rows if r['family'] == spec['key']]
        subset.sort(key=lambda r: tier_order.index(r['id'].split('-')[-1]) if r['id'].split('-')[-1] in tier_order else -1)
        ordered.extend(subset)
    for offset in range(0, len(ordered), 4):
        group = ordered[offset:offset+4]
        start('Tier and variant gallery', f'Captured examples {offset+1}-{offset+len(group)} of {len(ordered)}. Originals on the left; proposed SIU windows on the right.', 'VARIANT APPENDIX')
        for j, r in enumerate(group):
            col, row = j%2, j//2
            x, top = 42+col*565, 686-row*303
            txt(r['title'], x, top, 14, True)
            txt('ORIGINAL / SIU ABSENT', x, top-21, 8, True, teal)
            txt('SIU / MOCKUP', x+271, top-21, 8, True, teal)
            panel(r['original_path'], (x, top-254, 257, 222))
            panel(r['mockup_path'], (x+271, top-254, 257, 222), maxscale=1.18)
            txt('Ref: '+r['id'], x, top-272, 8, False, muted)
        end()

    start('Interfaces that stay native', 'Non-container surfaces and reference provenance.', 'SCOPE APPENDIX')
    sections = [
        ('Powah manual / WikiScreen', 'The guidebook is a reading and navigation screen, not a container. Keep it in its native full-screen flow; no SIU window mockup is proposed.'),
        ('Energizing Orb and Energizing Rod', 'The Orb accepts and returns items through interaction with the block in the world. These blocks do not register a separate inventory screen in the 1.20.1 container registry. Preserve their world interactions and informational overlays.'),
        ('Recipe viewer pages and overlays', 'JEI/REI recipe displays and on-world energy information are not extra container windows. Leave those surfaces under the mod or recipe viewer\'s normal handling.'),
        ('Scope and evidence', 'The design baseline is Minecraft 1.20.1 Fabric with the unmodified Powah 5.0.11 publisher release. Native images come from the existing Project Everything game audit, not synthetic reconstructions. Right-hand images are theoretical designs assembled from those captures and native texture masks.'),
        ('Future implementation', 'Use the existing SIU window lifecycle and separate inventory. Map native slots, widgets, hover targets and packets through the same transform. Recheck active fluids, reactor resource states, enabled channels and tier capacities in game when implementing.'),
    ]
    y = 687
    for h, body in sections:
        txt(h, 42, y, 16, True); y = wrap(body, 42, y-27, 1080, 12, 18)-25
    txt('SOURCE REFERENCES', 42, y, 11, True, teal); y -= 25
    link('Powah 5.0.11 Fabric - publisher release', RELEASE, 42, y); y -= 23
    link('Powah upstream source - pinned 1.20.1 commit', SOURCE, 42, y); y -= 23
    link('Container registry - 11 concrete menu registrations', SOURCE.replace('/tree/', '/blob/')+'/common/src/main/java/owmii/powah/inventory/Containers.java', 42, y)
    end()
    c.save()
    assert len(PdfReader(PDF).pages) == 1+len(SPECS)+1+math.ceil(len(rows)/4)+1
    return page


def main():
    OUT.mkdir(parents=True, exist_ok=True); PDF.parent.mkdir(parents=True, exist_ok=True)
    cases = json.loads((ROOT/'tmp/pdfs/powah/audit-cases.json').read_text())
    assert len(cases) == 73
    widths = {}
    for s in SPECS:
        titles = [c['native']['title'] for c in cases if choose_spec(c)['key'] == s['key']]
        widths[s['key']] = max(190 if s['key'] != 'cable' else 174, max(ART.tw(t)+48 for t in titles))
    rows = []
    for case in cases:
        s = choose_spec(case)
        original, mockup, manifest = design(case, s, widths[s['key']])
        stem = case['id']
        original_path = OUT/(stem+'-original.png'); mockup_path = OUT/(stem+'-mockup.png')
        original.save(original_path); mockup.save(mockup_path)
        manifest.update(original_path=str(original_path), mockup_path=str(mockup_path), original_sha256=sha(original_path), mockup_sha256=sha(mockup_path))
        rows.append(manifest)
    pages = build_pdf(rows)
    report = dict(design_only=True, minecraft='1.20.1', loader='fabric', powah='5.0.11',
        source_commit=SOURCE_COMMIT, publisher_release=RELEASE, publisher_jar_sha256=sha(JAR),
        concrete_screen_types=11, explained_opening_variants=12, captured_views=len(rows),
        pages=pages, source_hashes_verified=True, opaque_body_pixels_verified=True,
        visible_slot_envelopes_verified=True, attached_control_envelopes_verified=True,
        exclusions=['WikiScreen / guidebook', 'world interactions and overlays', 'recipe viewer pages'], rows=rows)
    (OUT/'design-manifest.json').write_text(json.dumps(report, indent=2)+'\n')
    # Contact sheet for the detailed 12 opening variants, not the tier appendix.
    contact = Image.new('RGB', (1536, 1120), '#EAF1F1'); draw = ImageDraw.Draw(contact)
    for i, spec in enumerate(SPECS):
        r = next(r for r in rows if r['id'] == spec['representative'])
        im = Image.open(r['mockup_path']).convert('RGBA'); im = im.resize((im.width*2, im.height*2), N)
        x = i%3*512; y = i//3*280
        draw.text((x+8, y+8), r['title'], fill='#20373D')
        assert im.width <= 512 and im.height <= 248
        contact.paste(im, (x+(512-im.width)//2, y+32), im)
    contact.save(OUT/'mockup-contact.png')
    print(json.dumps(dict(pdf=str(PDF), pages=pages, captured_views=len(rows), families=len(SPECS), visible_slots=Counter(r['preserved_visible_slots'] for r in rows)), indent=2))


if __name__ == '__main__': main()
