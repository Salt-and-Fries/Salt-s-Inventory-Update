"""Simple native-UI reference for the third Project Everything batch.

Images are original upstream screenshots or untouched crops of audited assets.
No reconstructed item contents or claimed SIU/gameplay results are generated.
"""
from pathlib import Path
from io import BytesIO
import hashlib
import json
import urllib.request
import zipfile

from PIL import Image
from reportlab.lib.colors import HexColor, white
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parents[1]
AUDIT = ROOT / 'build/ui-modpack-audit-20261025'
TMP = ROOT / 'tmp/pdfs/next-ten-ui-reference'
OUT = ROOT / 'output/pdf/siu-project-everything-next-ten-native-ui-reference.pdf'
GAME = Path('C:/Users/texca/.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar')
NAMES = ['ExtendedDrawersAddon', 'The Graveyard (Fabric/Neoforge)', 'BetterEnd', 'Runes',
         'Deep Aether', 'Chipped', 'Simply Swords', 'TieredZ', 'SmitherZ', 'AdditionZ']
ENTRIES = json.loads((AUDIT / 'ui-audit-evidence.json').read_text(encoding='utf-8-sig'))['entries']
DATA = {name: next(e for e in ENTRIES if e['title'] == name) for name in NAMES}
W, H = 960, 720
INK, TEAL, MUTED = map(HexColor, ['#173b45', '#087f88', '#526b73'])
PALE, LINE = map(HexColor, ['#edf4f4', '#d5e2e4'])
pdfmetrics.registerFont(TTFont('Ref', 'C:/Windows/Fonts/segoeui.ttf'))
pdfmetrics.registerFont(TTFont('RefBold', 'C:/Windows/Fonts/segoeuib.ttf'))

SCREENSHOTS = {
    'drawer-ui': ('https://i.imgur.com/6y9B2GN.png', None),
    'reforge-ui': ('https://i.imgur.com/XPMvlPJ.png', None),
    'smither': ('https://i.imgur.com/9cmDvbX.png', (800, 100, 1780, 1130)),
    'grinder-ui': ('https://i.imgur.com/oO6QUZV.png', (800, 100, 1780, 1130)),
    'fletching': ('https://i.imgur.com/XOaYO8w.png', None),
}
FIGURES = []


def asset(name, resource, box=None, vanilla=False):
    entry = DATA[name]
    path = GAME if vanilla else AUDIT / 'jars' / (entry['files'][0]['sha1'] + '.jar')
    with zipfile.ZipFile(path) as jar:
        raw = jar.read(resource)
    im = Image.open(BytesIO(raw)).convert('RGBA')
    if box:
        assert box[0] >= 0 and box[1] >= 0 and box[2] <= im.width and box[3] <= im.height
        im = im.crop(box)
    source = dict(kind='native texture crop', mod=name, artifact=str(path), resource=resource,
                  crop=box, asset_sha256=hashlib.sha256(raw).hexdigest())
    return im, source


def screenshot(key):
    url, box = SCREENSHOTS[key]
    p = TMP / 'upstream' / (key + '.png')
    p.parent.mkdir(parents=True, exist_ok=True)
    if not p.exists():
        request = urllib.request.Request(url, headers={'User-Agent': 'SIU-UI-Reference/0.1'})
        with urllib.request.urlopen(request, timeout=25) as response:
            p.write_bytes(response.read())
    im = Image.open(p).convert('RGBA')
    if box:
        im = im.crop(box)
    return im, dict(kind='upstream screenshot', url=url, crop=box,
                    version='Not stated by screenshot author', sha256=hashlib.sha256(p.read_bytes()).hexdigest())


class Reference:
    def __init__(self):
        TMP.mkdir(parents=True, exist_ok=True)
        OUT.parent.mkdir(parents=True, exist_ok=True)
        self.c = canvas.Canvas(str(OUT), pagesize=(W, H), pageCompression=1)
        self.c.setTitle('Project Everything - next ten native UI references')
        self.c.setAuthor("Salt's Inventory Update")
        self.page = 0

    def text(self, x, y, value, size=12, bold=False, color=INK):
        self.c.setFillColor(color)
        self.c.setFont('RefBold' if bold else 'Ref', size)
        self.c.drawString(x, y, value)

    def page_start(self, title, subtitle, name=None):
        if self.page:
            self.c.showPage()
        self.page += 1
        self.c.setFillColor(white)
        self.c.rect(0, 0, W, H, fill=1, stroke=0)
        self.text(36, 691, 'PROJECT EVERYTHING / NEXT TEN / ORIGINAL UI', 9, True, TEAL)
        self.text(36, 650, title, 27, True)
        self.text(36, 625, subtitle, 12, color=MUTED)
        self.c.setStrokeColor(LINE)
        self.c.line(36, 40, 924, 40)
        self.text(36, 23, 'Native references, not SIU results | Audited Minecraft 1.20.1 releases | 27 October 2026', 9, color=MUTED)
        self.c.setFont('Ref', 9)
        self.c.drawRightString(924, 23, str(self.page))
        if name:
            entry = DATA[name]
            key = 'mod-' + entry['slug']
            self.c.bookmarkPage(key)
            self.c.addOutlineEntry(name, key, 0)
            label = entry['url'] + ' | audited release ' + entry['files'][0]['version']
            self.text(36, 58, label, 9, color=MUTED)
            self.c.linkURL(entry['url'], (36, 55, 924, 71), relative=0)

    def picture(self, source, x, y, w, h, title, caption):
        im, provenance = source
        self.c.setFillColor(PALE)
        self.c.roundRect(x, y, w, h, 8, stroke=0, fill=1)
        self.text(x + 16, y + h - 27, title, 10 if w < 200 else 14, True)
        self.text(x + 16, y + 14, caption, 9, color=MUTED)
        factor = min((w - 32) / im.width, (h - 79) / im.height)
        iw, ih = im.width * factor, im.height * factor
        self.c.drawImage(ImageReader(im), x + (w - iw) / 2, y + 35 + (h - 79 - ih) / 2,
                         width=iw, height=ih, mask='auto')
        FIGURES.append(dict(page=self.page, title=title, caption=caption, source=provenance))

    def note(self, *lines):
        for i, line in enumerate(lines):
            self.text(36, 116 - i * 20, line, 11, color=MUTED)

    def build(self):
        self.page_start('The next ten: what needs adapting', 'A simple visual inventory of the original workstation and container interfaces.')
        for i, (name, scope) in enumerate(zip(NAMES, [
            'Drawer upgrades and variant controls', 'Ossuary and ordinary storage',
            'End Stone Smelter and recipe control', 'Rune Crafting Altar', 'Combiner and recipe control',
            'Seven tables sharing one Workbench UI', 'Runic Forge and its native animation',
            'Reforge Gear and anvil-entry tabs', 'Smither, Grinder and entry tabs',
            'Fletching Table and Chunk Loader',
        ])):
            y = 565 - i * 38
            self.text(42, y, f'{i+1:02}', 13, True, TEAL)
            self.text(82, y, name.replace(' (Fabric/Neoforge)', ''), 13, True)
            self.text(360, y, scope, 12, color=MUTED)
            self.c.linkRect('', 'mod-' + DATA[name]['slug'], (36, y-8, 924, y+18), thickness=0)
        self.text(36, 148, '12 owned container/menu families, plus their shared controls and visual variants.', 13, True, TEAL)
        self.text(36, 111, 'Images are original GUI textures or upstream screenshots. Texture views omit live items and text.', 11, color=MUTED)
        self.text(36, 89, 'Upstream screenshot versions are unspecified. This is a conversion-scope reference, not a gameplay test.', 11, color=MUTED)
        self.text(36, 67, 'Standalone editors, world rendering and HUDs stay native unless the integration review finds a conflict.', 11, color=MUTED)

        name = NAMES[0]
        self.page_start('01 / ExtendedDrawersAddon', 'Drawer upgrade screen, compartment variants and native upgrade controls.', name)
        self.picture(screenshot('drawer-ui'), 36, 153, 558, 445, 'Drawer upgrades - upstream view', 'Original project screenshot; not a SIU capture.')
        self.picture(asset(name, 'assets/extendeddrawersaddon/textures/gui/drawer.png', (176,0,248,54)),
                     612, 153, 312, 445, 'Native control sprites', 'Atlas fragment; not their on-screen arrangement.')
        self.note('Cover 1-, 2- and 4-compartment layouts, including normal and creative upgrade rows.')

        name = NAMES[1]
        self.page_start('02 / The Graveyard', 'Ossuary recipe selector and standard storage containers.', name)
        self.picture(asset(name, 'assets/minecraft/textures/gui/container/stonecutter.png', (0,0,176,166), True),
                     36, 153, 558, 445, 'Ossuary - native base frame', 'Uses the vanilla stonecutter texture; recipe entries are drawn at runtime.')
        self.picture(asset(name, 'assets/minecraft/textures/gui/container/stonecutter.png', (0,166,16,220), True),
                     612, 153, 312, 445, 'Recipe-button states', 'Original selector sprites: normal / selected / hover.')
        self.note('Also check urn, coffin and sarcophagus storage through the ordinary SIU storage host.',
                  'The separate gravestone text editor stays native; no inventory-window conversion is planned for it.')

        name = NAMES[2]
        self.page_start('03 / BetterEnd', 'End Stone Smelter: two ingredients, fuel, output and native processing feedback.', name)
        self.picture(asset(name, 'assets/betterend/textures/gui/smelter_gui.png', (0,0,176,166)),
                     36, 153, 558, 445, 'Smelter - native GUI artwork', 'Original 1.20.1 texture; live text, stacks and progress are not included.')
        self.picture(asset(name, 'assets/minecraft/textures/gui/recipe_button.png', (0,0,20,36), True),
                     612, 153, 312, 445, 'Native recipe toggle', 'The recipe component opens separately from the machine body.')
        self.note('Check every furnace variant using this family and preserve its recipe-book behavior.')

        name = NAMES[3]
        self.page_start('04 / Runes', 'Rune Crafting Altar: ingredient inputs, rune result and native invalid-recipe feedback.', name)
        self.picture(asset(name, 'assets/runes/textures/gui/crafting_altar.png', (0,0,176,166)),
                     36, 153, 888, 445, 'Rune Crafting Altar - native GUI artwork', 'Original texture reference; item contents and text are drawn in game.')
        self.note('One dedicated crafting interface. Keep its native recipes, input rules and output amounts.')

        name = NAMES[4]
        self.page_start('05 / Deep Aether', 'Combiner: three ingredient slots, native result and recipe support.', name)
        self.picture(asset(name, 'assets/deep_aether/textures/gui/combiner_gui.png', (0,0,176,166)),
                     36, 153, 558, 445, 'Combiner - native GUI artwork', 'Original texture reference; runtime stacks and recipe component omitted.')
        self.picture(asset(name, 'assets/deep_aether/textures/gui/sprites/combiner_arrow.png'),
                     612, 153, 312, 445, 'Native arrow artwork', 'Bundled animation/state strip, shown unassembled.')
        self.note('Retain the native recipe-book component. Adapting this add-on does not cover The Aether itself.')

        name = NAMES[5]
        self.page_start('06 / Chipped', 'Seven workbench variants share the same recipe browser, preview and crafting surface.', name)
        self.picture(asset(name, 'assets/chipped/textures/gui/container/workbench.png'),
                     36, 153, 558, 445, 'Workbench - full native artwork', 'Original full-size background; recipe results and 3D preview render at runtime.')
        for i, (file, label) in enumerate([('single_block_button', 'Single-block preview'),
                                         ('horizontal_blocks_button', 'Horizontal preview'),
                                         ('vertical_blocks_button', 'Vertical preview'),
                                         ('two_by_two_button', '2 x 2 preview')]):
            self.picture(asset(name, f'assets/chipped/textures/gui/sprites/{file}.png'),
                         612 + i%2*162, 383 - i//2*230, 150, 215, label, 'Normal / hover states')
        self.note('Include search, result scrolling, preview modes and craft/craft-all controls for every table type.')

        name = NAMES[6]
        self.page_start('07 / Simply Swords', 'Runic Forge: weapon and material slots, awakening feedback and channel/glow animation.', name)
        self.picture(asset(name, 'assets/simplyswords/textures/gui/container/runic_forge.png', (0,0,200,182)),
                     36, 153, 436, 445, 'Runic Forge - ornate artwork', 'Texture used by the audited screen; live slot effects are not pictured.')
        self.picture(asset(name, 'assets/simplyswords/textures/gui/container/runic_forge_vanilla.png', (12,0,188,166)),
                     488, 153, 436, 445, 'Bundled alternate artwork', 'Not a second menu; active use of this alternate asset is unverified.')
        self.note('The audited screen has animated slot feedback. A still texture cannot show its motion.')

        name = NAMES[7]
        self.page_start('08 / TieredZ', 'Reforge Gear, its action button and the tabs connecting it to the anvil.', name)
        self.picture(screenshot('reforge-ui'), 36, 153, 558, 445, 'Reforge Gear - upstream view', 'Original project screenshot; includes the native anvil/reforge tabs.')
        self.picture(asset(name, 'assets/tiered/textures/gui/reforging_screen.png', (176,0,230,18)),
                     612, 153, 312, 445, 'Native action sprites', 'Original action states; not their on-screen arrangement.')
        self.note('The reforging tab must remain reachable from the SIU anvil. Tierify is a separate candidate.')

        name = NAMES[8]
        self.page_start('09 / SmitherZ', 'Two workstations: link gems in the Smither and remove them in the Grinder.', name)
        self.picture(screenshot('smither'), 36, 153, 436, 445, 'Smither / Link Gear', 'Upstream screenshot crop; native entry tabs retained in the image.')
        self.picture(screenshot('grinder-ui'), 488, 153, 436, 445, 'Grinder / Unlink Gear', 'Upstream screenshot crop; native entry tabs retained in the image.')
        self.note('Include smithing/grindstone entry tabs, the linking action, gem slots and extraction results.')

        name = NAMES[9]
        self.page_start('10 / AdditionZ', 'Two dedicated interfaces: Fletching Table and Chunk Loader.', name)
        self.picture(screenshot('fletching'), 36, 153, 436, 445, 'Fletching Table - upstream view', 'Original project screenshot with native inputs and arrow result.')
        self.picture(asset(name, 'assets/additionz/textures/gui/chunk_loader.png', (0,0,176,166)),
                     488, 153, 436, 445, 'Chunk Loader - native base frame', 'Its 3 x 3 chunk buttons and status colors are drawn at runtime.')
        self.note('Also review the mod\'s inventory/Trinkets hooks. World/HUD features are not extra container windows.')

        self.page_start('Source key', 'Original references only. These ten integrations have not been implemented or gameplay-tested in this batch.')
        for i, name in enumerate(NAMES):
            e = DATA[name]
            y = 567 - i*42
            self.text(36, y, f'{i+1:02}  '+name.replace(' (Fabric/Neoforge)', ''), 12, True)
            self.text(350, y, e['files'][0]['version'], 11, color=MUTED)
            self.text(36, y-17, e['url'], 9, color=TEAL)
            self.c.linkURL(e['url'], (36,y-20,900,y+13), relative=0)
        self.text(36, 111, 'Textures: exact pinned audit JARs. Ossuary frame and recipe toggle: vanilla Minecraft 1.20.1 assets.', 11, color=MUTED)
        self.text(36, 89, 'Screenshots: the authors\' Modrinth descriptions (Imgur links); exact screenshot versions are not stated.', 11, color=MUTED)
        self.text(36, 67, 'Scope can expand after checking each release\'s actual menus, native controls and opening behavior.', 11, color=MUTED)
        self.c.save()
        manifest = dict(pdf=str(OUT), pages=self.page, mods=10, owned_menu_families=12,
                        gameplay_tests_run=False, figures=FIGURES)
        (ROOT/'output/project-everything-ten-port/next-ten-ui-reference-sources.json').write_text(
            json.dumps(manifest, indent=2), encoding='utf-8')
        print(json.dumps(dict(pdf=str(OUT), pages=self.page, figures=len(FIGURES))))


if __name__ == '__main__':
    Reference().build()
