"""Kibe 1.10.1-BETA+1.20: verified native references and SIU proposals.

Design only. No core/compatibility sources or publisher captures are changed.
Every operative patch is copied at native GUI-pixel geometry. The inherited
crafting reference is explicitly identified as a vanilla capture, not a Kibe
gameplay capture. Chunk Loader remains a native non-inventory screen.
"""
from pathlib import Path
from io import BytesIO
from collections import Counter
import hashlib
import importlib.util
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
OUT = ROOT / 'output/kibe-window-design'
PDF = ROOT / 'output/pdf/siu-kibe-window-mockups.pdf'
TMP = ROOT / 'tmp/pdfs/kibe'
JAR = ROOT / 'build/ui-modpack-audit-20261025/jars/cf5035be0524cd5e7447af39c752aae174c28b15.jar'
COMMIT = 'a4ba6fab55119d9fcf972ee402f11b74fdc2cd9a'
SOURCE = 'https://github.com/lucaargolo/kibe/tree/' + COMMIT
RELEASE = 'https://modrinth.com/mod/kibe/version/fhSgEP7Z'
N = Image.Resampling.NEAREST
W, H = 1191, 842
TEAL = '#176f70'
INK = '#203b3e'
MUTED = '#586b6c'

_spec = importlib.util.spec_from_file_location('siu_art', ROOT / 'tools/build-enchanted-window-mockups.py')
ART = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ART)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def texture(asset):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(BytesIO(z.read('assets/kibe/textures/gui/' + asset + '.png'))).convert('RGBA')


SPECS = [
    dict(key='placed-cooler', case='kibe-cooler', title='Cooler - placed block', asset='cooler',
         size=(100, 56), rect=(79, 17, 97, 35), slots=1, theme='cooler',
         opener='Use a Cooler placed in the world.',
         does='A one-slot food container. This placed-block menu and the held Cooler have separate screen handlers, even though their visible layouts are identical.',
         design='Keep the original cyan slot and exact cyan surface. Center the single slot below the title; remove the large player-inventory section. More and Close keep the small header clear.',
         keep='Food-only slot | cyan slot bevel | block-backed contents',
         path='blocks/cooler/CoolerScreen.kt'),
    dict(key='held-cooler', case='pr-kibe-cooler', title='Cooler - held item', asset='cooler',
         size=(100, 56), rect=(79, 17, 97, 35), slots=1, theme='cooler',
         opener='Use the Cooler item in your hand; sneak-use it on a block to open it rather than place it.',
         does='The held Cooler stores food in its item data and can feed a hungry survival player while carried. Opening it uses a distinct item-backed menu.',
         design='Use the same compact cyan window as the placed Cooler. Preserve the original food slot and native held-item protections; changing the presentation must not change automatic feeding behavior.',
         keep='Food-only slot | carried-item contents | native feeding rules',
         path='items/cooler/CoolerBlockItemScreen.kt'),
    dict(key='big-torch', case='kibe-big-torch', title='Big Torch', asset='big_torch',
         size=(176, 72), rect=(7, 18, 169, 54), slots=9, theme='gray',
         opener='Use a placed Big Torch.',
         does='Accepts regular torches in nine slots to support its mob-spawn suppression area. Hovering the upper bar reports torch percentage and chunk radius.',
         design='Keep the full 160-pixel percentage bar directly above the nine-slot row. Preserve its complete fill envelope and hover target. The title fits the normal five SIU window controls.',
         keep='9 torch-only slots | full-width bar | percentage/radius tooltip',
         path='blocks/bigtorch/BigTorchScreen.kt'),
    dict(key='cobblestone-generator', case='kibe-cobblestone-generator-mk1', title='Cobblestone Generator', asset='block_generator',
         size=(190, 108), rect=(7, 17, 169, 89), slots=27, theme='gray',
         opener='Use a placed Cobblestone Generator, MK1 through MK5.',
         does='Generates cobblestone into a 27-slot output inventory. Water and lava artwork animate toward the product icon above the grid; the tier changes generation speed.',
         design='Preserve the two bars, centered product and complete 9 x 3 grid as one group. Keep the full tier name in the header, with More and Close. All five tiers are compared in this report.',
         keep='27 output slots | both animated bars | product icon | tier title',
         path='blocks/miscellaneous/BlockGeneratorScreen.kt'),
    dict(key='basalt-generator', case='kibe-basalt-generator-mk1', title='Basalt Generator', asset='block_generator',
         size=(176, 108), rect=(7, 17, 169, 89), slots=27, theme='gray',
         opener='Use a placed Basalt Generator, MK1 through MK5.',
         does='Uses the same 27-slot generator screen for basalt. Its left animation uses blue-ice artwork rather than water; the right bar uses lava.',
         design='Reuse the generator composition while preserving the basalt icon, blue-ice animation and tier title. Keep the entire native grid and bar geometry. All five tiers receive the same treatment.',
         keep='27 output slots | blue ice and lava bars | basalt icon | tier title',
         path='blocks/miscellaneous/BlockGeneratorScreen.kt'),
    dict(key='breaker', case='kibe-breaker', title='Breaker', asset='breaker',
         size=(112, 90), rect=(61, 17, 115, 71), slots=9, theme='gray',
         opener='Use a placed Breaker.',
         does='Breaks the block in front of it when activated by redstone and stores the resulting drops in a 3 x 3 inventory.',
         design='Center the original 3 x 3 grid in a compact gray window. Keep every slot border and its original item spacing. Only the unused shoulders and embedded player inventory disappear.',
         keep='9 storage slots | original grid geometry | native redstone operation',
         path='blocks/breaker/BreakerScreen.kt'),
    dict(key='placer', case='kibe-placer', title='Placer', asset='placer',
         size=(112, 90), rect=(61, 17, 115, 71), slots=9, theme='gray',
         opener='Use a placed Placer.',
         does='Places stored blocks in front of the machine when activated by redstone. Its 3 x 3 inventory accepts block items.',
         design='Match the Breaker window proportions and retain the original nine-slot arrangement. Preserve block-item restrictions and existing redstone behavior; add no new machine-action button.',
         keep='9 block-item slots | complete native rims | original activation rules',
         path='blocks/placer/PlacerScreen.kt'),
    dict(key='drawbridge', case='kibe-drawbridge', title='Drawbridge', asset='drawbridge',
         size=(142, 58), rect=(79, 17, 151, 35), slots=2, theme='drawbridge',
         opener='Use a placed Drawbridge.',
         does='Extends and retracts a line of blocks using redstone. The main slot supplies building blocks; the separate one-item slot sets the machine cover appearance.',
         design='Keep the gold input slot and the cover icon to its right, separated by the native 54-pixel offset. Use the exact charcoal surface with a gold-highlighted SIU rim and yellow title.',
         keep='Building-block slot + cover slot | original ghost icon | gold/charcoal palette',
         path='blocks/drawbridge/DrawbridgeScreen.kt'),
    dict(key='wither-builder', case='kibe-wither-builder', title='Wither Builder', asset='wither_builder',
         size=(132, 90), rect=(61, 17, 115, 71), slots=7, theme='gray',
         opener='Use a placed Wither Builder; actual summoning also needs its materials and clear space.',
         does='Holds three wither-skeleton skull stacks and four stacks of valid wither-base blocks in a seven-slot ingredient silhouette.',
         design='Preserve the complete skull-and-T arrangement, including the differently colored slot interiors. Do not replace it with a generic rectangle of seven slots. Keep all native ingredient restrictions.',
         keep='3 skull slots + 4 base-block slots | colored ingredient silhouette',
         path='blocks/witherbuilder/WitherBuilderScreen.kt'),
    dict(key='vacuum-hopper', case='kibe-vacuum-hopper', title='Vacuum Hopper', asset='vacuum_hopper',
         size=(148, 90), rect=(43, 17, 133, 71), slots=11, theme='teal',
         opener='Use a placed Vacuum Hopper.',
         does='Collects nearby item drops and XP. Nine slots hold collected items; the two slots to the right form the XP-conversion input and output beside a liquid-XP gauge.',
         design='Move the complete mechanism together: 3 x 3 storage, full-height XP tank, conversion input, progress arrow and output. Keep the native teal slots, gauge markings, tooltip and all 11 slot positions relative to one another.',
         keep='11 slots | full XP gauge | conversion progress | tank tooltip',
         path='blocks/vacuum/VacuumHopperScreen.kt'),
    dict(key='entangled-chest', case='kibe-entangled-chest', title='Entangled Chest', asset='entangled_chest',
         size=(176, 108), rect=(7, 17, 169, 71), slots=27, theme='teal', runes=True,
         opener='Use a placed Entangled Chest. Rune and ownership changes remain native world interactions.',
         does='Provides shared storage identified by an eight-rune combination and its native ownership key. The menu shows 27 shared slots and all eight rune icons.',
         design='Use the original dark-teal slot grid in a matching teal SIU shell. Give the eight runes a centered row below the header so the title and window controls never cover them. Keep the grid directly below the rune row.',
         keep='27 shared slots | all 8 rune icons in order | channel/ownership identity',
         path='blocks/entangledchest/EntangledChestScreen.kt'),
    dict(key='entangled-bag', case='item-kibe-entangled-bag', title='Entangled Bag', asset='entangled_chest',
         size=(176, 108), rect=(7, 17, 169, 71), slots=27, theme='teal', runes=True,
         opener='Use the bag in hand. Sneak-use it on an Entangled Chest to copy that chest\'s link.',
         does='Opens the linked shared inventory from a carried item. Its 27-slot layout and eight rune indicators match the chest. The bag stores the link; the contents belong to the shared entangled inventory.',
         design='Share the Entangled Chest window design, preserving all eight rune icons and the complete shared grid. Keep the native link lookup and item-transfer behavior; the rune display remains an indicator row.',
         keep='27 shared slots | all 8 rune icons | native bag linking and transfers',
         path='items/entangledbag/EntangledBagScreen.kt'),
    dict(key='trash-can', case='kibe-trash-can', title='Trash Can', asset='trash_can',
         size=(128, 58), rect=(79, 35, 97, 53), slots=1, theme='trash',
         opener='Use a placed Trash Can.',
         does='Discards items inserted into its single slot. This is a disposal menu rather than a storage chest.',
         design='Keep the original dark slot on a charcoal SIU surface with a white title. Center it under the header. Preserve native deletion and shift-click behavior; add no invented storage, recovery or machine-action controls.',
         keep='1 disposal slot | charcoal surface | native deletion semantics',
         path='blocks/trashcan/TrashCanScreen.kt'),
    dict(key='pocket-trash-can', case='item-kibe-pocket-trash-can', title='Pocket Trash Can', asset='trash_can',
         size=(144, 58), rect=(79, 35, 97, 53), slots=1, theme='trash',
         opener='Use the Pocket Trash Can item in your hand.',
         does='Provides the same single-slot disposal action from a carried item. The native slot immediately discards inserted items rather than storing a recoverable stack.',
         design='Use the same charcoal disposal-window language. Put its full title on one clean line and retain the original dark slot and native disposal/transfer rules. More and Close fit without squeezing the title.',
         keep='1 disposal slot | complete title | native carried-item opening route',
         path='items/trashcan/PocketTrashCanScreen.kt'),
]


def themed_frame(w, h, theme):
    im = ART.frame(w, h)
    palettes = {
        'gray': {0:(0,0,0),51:(51,51,51),85:(85,85,85),139:(139,139,139),198:(198,198,198),255:(255,255,255)},
        'cooler': {0:(0,0,0),51:(0,70,70),85:(0,126,126),139:(0,145,149),198:(0,206,210),255:(0,255,255)},
        'teal': {0:(0,0,0),51:(14,43,43),85:(13,82,83),139:(25,78,78),198:(28,122,123),255:(61,183,183)},
        'drawbridge': {0:(0,0,0),51:(75,61,13),85:(124,94,0),139:(185,148,32),198:(76,76,76),255:(255,224,0)},
        'trash': {0:(0,0,0),51:(35,35,35),85:(51,51,51),139:(70,70,70),198:(76,76,76),255:(122,122,122)},
    }
    p = palettes[theme]
    for y in range(h):
        for x in range(w):
            r,g,b,a = im.getpixel((x,y))
            if a:
                assert r == g == b
                color = p[min(p,key=lambda v:abs(v-r))]
                im.putpixel((x,y),(*color,a))
    return im


def header(im, title, theme):
    full = [0,6,4,9,2]
    cols = full if 7 + ART.tw(title) + 7 <= im.width-5-len(full)*14 else [3,2]
    start = im.width-5-len(cols)*14
    assert 7 + ART.tw(title) + 7 <= start, (title,im.size)
    color = (64,64,64,255) if theme=='gray' else (255,224,0,255) if theme=='drawbridge' else (255,255,255,255)
    ART.text(im,title,7,7,color)
    for i,col in enumerate(cols):
        sy = 22 if col==0 else 0
        im.alpha_composite(ART.CONTROLS.crop((col*11,sy,col*11+11,sy+11)),(start+i*14,5))
    return cols


def native(row):
    n = row['native']
    p = ROOT / n['capture']
    assert sha(p)==n['sha256'],row['id']
    image = Image.open(p).convert('RGBA')
    assert image.size == (1280,900)
    b = n['bounds']
    crop = image.crop((2*b['x'],2*b['y'],2*(b['x']+b['width']),2*(b['y']+b['height'])))
    return crop,crop.resize((b['width'],b['height']),N)


def make_design(row,spec):
    original,logical = native(row)
    rect = spec['rect']
    patch = logical.crop(rect)
    # This is operative artwork inside the native UI, not a rectangle of world.
    atlas = texture(spec['asset'])
    assert min(atlas.crop(rect).getchannel('A').getextrema())==255
    im = themed_frame(*spec['size'],spec['theme'])
    cols = header(im,row['native']['title'],spec['theme'])
    dest = ((im.width-patch.width)//2,44 if spec.get('runes') else 26 if patch.height>18 else 28)
    assert dest[0]>=7 and dest[1]>=24
    assert dest[0]+patch.width<=im.width-7 and dest[1]+patch.height<=im.height-7
    im.alpha_composite(patch,dest)
    assert im.crop((dest[0],dest[1],dest[0]+patch.width,dest[1]+patch.height)).tobytes()==patch.tobytes()
    slots = [s for s in row['native']['slots'] if not s['player']]
    assert len(slots)==spec['slots']
    moved = []
    for s in slots:
        x,y = s['x']-1,s['y']-1
        assert rect[0]<=x and rect[1]<=y and x+18<=rect[2] and y+18<=rect[3], (row['id'],s)
        dx,dy = dest[0]+x-rect[0],dest[1]+y-rect[1]
        assert im.crop((dx,dy,dx+18,dy+18)).tobytes()==logical.crop((x,y,x+18,y+18)).tobytes()
        moved.append(dict(index=s['index'],source=[x,y,18,18],destination=[dx,dy,18,18]))
    runes = None
    if spec.get('runes'):
        # Eight complete native indicators at their original 10-pixel pitch.
        # Mask each rendered item with its publisher sprite alpha so neither
        # the old outer top bevel nor the grid rim moves with the rune row.
        # This also removes the native title's overlap outside the first icon.
        with zipfile.ZipFile(JAR) as z:
            rune = Image.open(BytesIO(z.read('assets/kibe/textures/item/white_rune.png'))).convert('RGBA')
        assert rune.size==(16,16) and rune.getbbox()[0]>=2
        strip=Image.new('RGBA',(86,16))
        for i in range(8):
            icon=logical.crop((87+i*10,2,103+i*10,18))
            icon.putalpha(rune.getchannel('A'))
            strip.alpha_composite(icon,(i*10,0))
        xy = ((im.width-86)//2,24)
        im.alpha_composite(strip,xy)
        runes = dict(count=8,pitch=10,source=[87,2,86,16],destination=list(xy),captured_colors='eight white runes',note='Each rendered icon uses its publisher alpha mask to exclude old frame/title/grid pixels. Runtime retains each actual rune color.')
    # Surface color comes from the publisher palette, and must equal the actual
    # captured background so the operative region has no pasted-panel seam.
    surface = im.getpixel((5,22))
    bg_sample = (60,28) if spec['theme']=='trash' else (40,14)
    assert surface == logical.getpixel(bg_sample), (row['id'],surface,logical.getpixel(bg_sample))
    return original,im,dict(size=list(im.size),source_rect=list(rect),destination=list(dest),controls=cols,
                           slots=moved,runes=runes,surface_rgba=list(surface),operative_patch_sha256=hashlib.sha256(patch.tobytes()).hexdigest())


def para(c,body,x,top,width,size=12,color=INK,leading=None,bold=False):
    assert body.isascii(),body
    font = 'SegoeBold' if bold else 'Segoe'
    c.setFont(font,size); c.setFillColor(HexColor(color))
    leading = leading or size*1.38
    words = body.split(); lines=[]; line=''
    for word in words:
        proposed = (line+' '+word).strip()
        if pdfmetrics.stringWidth(proposed,font,size)>width:
            assert line,word
            lines.append(line);line=word
        else: line=proposed
    if line:lines.append(line)
    for i,line in enumerate(lines):c.drawString(x,H-top-i*leading-size*.8,line)
    return top+len(lines)*leading


def label(c,s,x,top):
    para(c,s.upper(),x,top,1090,10,TEAL,bold=True)


def foot(c,page,note='Native game captures + proposed mockups | Design only; not an implemented integration'):
    c.setStrokeColor(HexColor('#d2dddd'));c.line(42,43,W-42,43)
    para(c,note,42,H-32,1060,8,MUTED)
    para(c,str(page),W-68,H-32,30,9,MUTED)


def picture(c,im,x,top,width,height,max_scale=3):
    # All figures use integer enlargement of the original logical pixels.
    if im.width>=300:
        logical = im.resize((im.width//2,im.height//2),N)
    else:logical=im
    factor=min(max_scale,int(width/logical.width),int(height/logical.height))
    assert factor>=1
    display=logical.resize((logical.width*factor,logical.height*factor),N)
    xx=x+(width-display.width)/2; yy=H-top-height+(height-display.height)/2
    c.drawImage(ImageReader(display),xx,yy,display.width,display.height,mask='auto')


def comparison_page(c,row,spec,original,mock,meta,page):
    label(c,'PROJECT EVERYTHING / KIBE WINDOW DESIGN',42,30)
    title = spec['title']
    is_generator = 'generator' in spec['key']
    if is_generator:title=row['native']['title']
    para(c,title,42,59,1100,27,bold=True)
    para(c,'Minecraft 1.20.1 / Fabric / Kibe 1.10.1-BETA+1.20',42,100,1100,11,MUTED)
    para(c,'Open: '+spec['opener'],42,126,1100,11)
    para(c,spec['keep'],42,154,1100,11,TEAL)
    label(c,'ORIGINAL IN GAME / SIU ABSENT',42,188)
    label(c,'PROPOSED SIU WINDOW / MOCKUP',610,188)
    for x in [42,610]:
        c.setFillColor(HexColor('#edf4f4'));c.roundRect(x,H-216-397,539,397,10,fill=1,stroke=0)
    picture(c,original,46,224,531,381)
    picture(c,mock,614,224,531,381)
    b=row['native']['bounds']
    para(c,f"Native: {b['width']} x {b['height']} GUI pixels",52,623,519,9,MUTED)
    para(c,f"Window: {mock.width} x {mock.height} GUI pixels | Enlarged independently for review",620,623,519,9,MUTED)
    label(c,'WHAT THIS MENU DOES',42,654)
    para(c,spec['does'],42,676,520,12)
    label(c,'WINDOW DESIGN',610,654)
    end=para(c,spec['design'],610,676,539,12)
    assert end<780,(title,end)
    foot(c,page)
    c.showPage()


def cover(c,rows):
    label(c,'PROJECT EVERYTHING / WINDOW DESIGN REVIEW',42,34)
    para(c,'Kibe',42,76,1100,48,bold=True)
    para(c,'Native interfaces, carefully turned into windows',42,142,1100,23)
    para(c,'Minecraft 1.20.1 / Fabric / Kibe 1.10.1-BETA+1.20',42,184,1100,12,MUTED)
    para(c,'13 custom container screen types | 22 captured views | 1 inherited vanilla crafting UI',42,226,1100,16,TEAL,bold=True)
    para(c,'Keep Kibe\'s own colors, slot textures and working arrangements. Remove the embedded player inventory, add familiar SIU window controls, and preserve clear padding around every operative sprite.',42,266,1090,15)
    for x,y,key,name in [(42,365,'kibe-entangled-chest','Teal entangled storage'),(420,365,'kibe-vacuum-hopper','Complete XP apparatus'),(798,365,'kibe-drawbridge','Gold and charcoal')]:
        c.setFillColor(HexColor('#edf4f4'));c.roundRect(x,H-y-260,351,260,10,fill=1,stroke=0)
        picture(c,rows[key]['mock'],x+8,y+12,335,200,2)
        para(c,name,x+14,y+229,323,12,TEAL,bold=True)
    para(c,'Every custom interface has a real SIU-absent game reference on the left and an artwork-based proposal on the right. Both generator families include MK1 through MK5. The Pocket Crafting Table reuses vanilla crafting; the separate appendix accounts for non-inventory interfaces.',42,660,1090,14)
    para(c,'Design proposals only. Archived native captures and matching publisher source were checked; no Kibe implementation or gameplay test is claimed by this report.',42,744,1090,11,MUTED)
    foot(c,1)
    c.showPage()


def crafting_page(c,page):
    row=json.loads((TMP/'vanilla-crafting-reference.json').read_text(encoding='utf-8'))
    original,logical=native(row)
    im=ART.frame(176,96)
    title='Pocket Crafting Table'
    header(im,title,'gray')
    # Existing vanilla integration: retain crafting grid, arrow and full output
    # frame, using this verified actual vanilla screenshot as the visual base.
    rect=(27,16,145,72)
    patch=logical.crop(rect)
    im.alpha_composite(patch,(29,26))
    # The recipe book control is outside that operative group, in its original
    # left-side relationship. The full 20 x 18 native button is copied intact.
    book=logical.crop((4,34,24,52))
    im.alpha_composite(book,(6,44))
    inherited_slots=[]
    for s in row['native']['slots']:
        if s['player']:continue
        x,y=s['x']-1,s['y']-1
        assert rect[0]<=x and rect[1]<=y and x+18<=rect[2] and y+18<=rect[3]
        dx,dy=x+2,y+10
        assert im.crop((dx,dy,dx+18,dy+18)).tobytes()==logical.crop((x,y,x+18,y+18)).tobytes()
        inherited_slots.append(dict(index=s['index'],source=[x,y,18,18],destination=[dx,dy,18,18]))
    assert len(inherited_slots)==10
    assert im.crop((6,44,26,62)).tobytes()==book.tobytes()
    im.save(OUT/'mockups/pocket-crafting-table.png')
    label(c,'PROJECT EVERYTHING / INHERITED VANILLA INTERFACE',42,30)
    para(c,'Pocket Crafting Table',42,59,1100,27,bold=True)
    para(c,'Kibe opens the vanilla CraftingScreenHandler; it does not register a new screen layout.',42,103,1100,12,MUTED)
    para(c,'Open: Use the Pocket Crafting Table item in your hand.',42,136,1100,12)
    label(c,'ACTUAL VANILLA CRAFTING REFERENCE / SIU ABSENT',42,188)
    label(c,'PROPOSED SIU WINDOW / MOCKUP / VANILLA REUSE',610,188)
    for x in [42,610]:
        c.setFillColor(HexColor('#edf4f4'));c.roundRect(x,H-216-397,539,397,10,fill=1,stroke=0)
    picture(c,original,46,224,531,381)
    picture(c,im,614,224,531,381)
    label(c,'SAME NATIVE SCREEN, DIFFERENT OPENING ROUTE',42,646)
    para(c,'The left is a genuine vanilla crafting capture, not a Pocket Crafting Table capture. Kibe source confirms the inherited handler and a custom title. Reuse the existing SIU crafting integration rather than inventing a Kibe-specific arrangement.',42,670,520,12)
    label(c,'PRESERVE',610,646)
    para(c,'Keep the 3 x 3 grid, arrow, full output-slot border and native recipe-book control. Preserve Kibe\'s result updates and return/drop behavior on close. The ordinary SIU player-inventory window supplies the separate inventory.',610,670,539,12)
    foot(c,page,'Vanilla screen reference clearly identified | Proposed integration reuse, not new gameplay evidence')
    c.showPage()
    return dict(id='pocket-crafting-table',reference_kind='actual vanilla screen, inherited by Kibe',native_capture=row['native']['capture'],native_sha256=row['native']['sha256'],size=list(im.size),source_path='items/miscellaneous/PocketCraftingTable.kt',slots=inherited_slots,recipe_book_pixel_check=True)


def scope_page(c,page):
    label(c,'APPENDIX / COVERAGE AND SCOPE',42,30)
    para(c,'What is included - and what stays native',42,61,1100,28,bold=True)
    para(c,'The 13-type audit count describes custom container screens, not every item or opening route.',42,110,1100,14,MUTED)
    rows=[
        ('Cooler','Placed block + carried item','2 screen types / 2 views'),
        ('Big Torch','Torch row and percentage/radius bar','1 type / 1 view'),
        ('Block generators','Cobblestone and Basalt, MK1-MK5','1 shared type / 10 views'),
        ('Breaker / Placer','Redstone block machines','2 types / 2 views'),
        ('Drawbridge / Wither Builder','Two specialized ingredient layouts','2 types / 2 views'),
        ('Vacuum Hopper','Items, XP tank and conversion slots','1 type / 1 view'),
        ('Entangled Chest / Bag','Shared storage and eight-rune identity','2 types / 2 views'),
        ('Trash Can / Pocket Trash Can','Block and carried disposal menus','2 types / 2 views'),
        ('Pocket Crafting Table','Inherited vanilla crafting','Vanilla reuse / extra reference'),
    ]
    y=164
    for i,(name,desc,count) in enumerate(rows):
        c.setFillColor(HexColor('#edf4f4' if i%2==0 else '#ffffff'));c.rect(42,H-y-39,1107,39,fill=1,stroke=0)
        para(c,name,52,y+11,290,11,bold=True);para(c,desc,358,y+11,445,11);para(c,count,843,y+11,296,11,TEAL)
        y+=39
    label(c,'CHUNK LOADER: KEEP ITS ORIGINAL NON-INVENTORY SCREEN',42,555)
    para(c,'An enabled Chunk Loader opens a native map/configuration screen. It contains a 5 x 5 chunk map, enabled-chunk overlays, a check-for-owner toggle and tooltips; it has no item slots. Keep this in its original screen presentation, outside the inventory-window conversion. It is source-accounted here, not presented as a newly captured screenshot.',42,581,1098,13)
    label(c,'WORLD INTERACTIONS ARE NOT EXTRA CONTAINER WINDOWS',42,668)
    para(c,'Entangled Tanks and Buckets, XP Shower/Drain, elevators, rings, lassos and other tools retain their native world interactions and tooltips. Entangled-chest rune/ownership edits also stay with their original world interactions. No new inventory windows are proposed for these actions.',42,695,1098,13)
    foot(c,page)
    c.showPage()


def sources_page(c,page,verification):
    label(c,'APPENDIX / EVIDENCE AND DESIGN CONTRACT',42,30)
    para(c,'Sources and review checks',42,61,1100,28,bold=True)
    blocks=[
        ('Pinned baseline','Kibe 1.10.1-BETA+1.20 on Minecraft 1.20.1 Fabric. Native captures come from the retained Project Everything audit. All 22 source screenshot SHA-256 values are checked before composition.'),
        ('Publisher source','The release tag resolves to commit '+COMMIT+'. The screen registrations, draw routines, slot handlers and original PNG atlases were inspected to identify slot roles, rune indicators, progress areas and inherited crafting.'),
        ('Pixel geometry','Every custom menu slot retains its complete 18 x 18 native pixel envelope, including its own original slot artwork. Operative groups are translated together; no slot, gauge or button is stretched. The rune row is deliberately moved below the header.'),
        ('Window behavior','The proposals use SIU\'s own frame geometry and control sprites. Full controls appear where the title fits; compact headers show More and Close. The player inventory remains its ordinary separate SIU window. Slot permissions, shift-click rules and server actions remain native.'),
        ('Coverage limits','This is a visual design report, not an implementation or functional test. Idle captures do not prove live operation. Future implementation checks must cover generator/Big Torch fills, XP conversion and hover areas, colored rune combinations, held-item behavior, slot transfers and close behavior.'),
    ]
    y=120
    for title,body in blocks:
        label(c,title,42,y);y=para(c,body,42,y+23,1100,13)+24
    label(c,'PRIMARY REFERENCES',42,640)
    links=[('Pinned Kibe publisher release',RELEASE),('Matching source commit',SOURCE),('Kibe publisher overview','https://modrinth.com/mod/kibe')]
    y=666
    for title,url in links:
        para(c,title,42,y,1090,12,TEAL,bold=True)
        c.linkURL(url,(42,H-y-17,550,H-y+2),relative=0,thickness=0)
        y+=29
    para(c,'Reproducible assets: output/kibe-window-design/manifest.json and tools/build-kibe-window-mockups.py',42,766,1100,9,MUTED)
    foot(c,page)
    c.showPage()


def main():
    (OUT/'mockups').mkdir(parents=True,exist_ok=True)
    (OUT/'native').mkdir(exist_ok=True)
    PDF.parent.mkdir(parents=True,exist_ok=True)
    rows=json.loads((TMP/'audit-cases.json').read_text(encoding='utf-8'))
    assert len(rows)==22 and len({r['native']['screen'] for r in rows})==13
    byid={r['id']:r for r in rows}
    designs={};manifest=[];ordered=[]
    for spec in SPECS:
        ids=[spec['case']]
        if 'generator' in spec['key']:
            ids=[spec['case'][:-1]+str(i) for i in range(1,6)]
        for id in ids:
            row=byid[id];original,mock,meta=make_design(row,spec)
            original.save(OUT/'native'/(id+'.png'));mock.save(OUT/'mockups'/(id+'.png'))
            designs[id]=dict(original=original,mock=mock,meta=meta)
            manifest.append(dict(id=id,spec=spec['key'],title=row['native']['title'],screen=row['native']['screen'],native_capture=row['native']['capture'],native_sha256=row['native']['sha256'],opener=spec['opener'],**meta))
            ordered.append((row,spec))
    assert len(designs)==22 and set(designs)==set(byid)
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeBold','C:/Windows/Fonts/segoeuib.ttf'))
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('Kibe - Native UI and proposed SIU windows')
    c.setAuthor('Salts Inventory Update / Project Everything')
    cover(c,designs)
    page=2
    for row,spec in ordered:
        d=designs[row['id']];comparison_page(c,row,spec,d['original'],d['mock'],d['meta'],page);page+=1
    inherited=crafting_page(c,page);page+=1
    scope_page(c,page);page+=1
    verification=dict(custom_screen_types=13,native_capture_views=22,custom_mockups=22,inherited_crafting_reference=1,
                      preserved_slot_envelopes=sum(len(m['slots']) for m in manifest),native_sha256_checks=22,
                      complete_slot_pixel_checks=True,operative_patch_pixel_checks=True,palette_surface_checks=True,
                      implementation_changed=False,functional_test_run=False)
    sources_page(c,page,verification)
    c.save()
    assert len(PdfReader(str(PDF)).pages)==26
    (OUT/'manifest.json').write_text(json.dumps(dict(baseline='Minecraft 1.20.1 Fabric / Kibe 1.10.1-BETA+1.20',release=RELEASE,source=SOURCE,pdf=str(PDF.relative_to(ROOT)),cases=manifest,inherited=inherited,verification=verification),indent=2),encoding='utf-8')
    # Standalone review contact for all proposed custom views.
    contact=Image.new('RGB',(1100,((len(manifest)+3)//4)*210),'#edf4f4');draw=ImageDraw.Draw(contact)
    for i,m in enumerate(manifest):
        x=(i%4)*275;y=(i//4)*210;im=designs[m['id']]['mock'];s=min(1,260/im.width)
        im=im.resize((int(im.width*s),int(im.height*s)),N)
        contact.paste(im,(x+10,y+28),im);draw.text((x+8,y+9),m['title'],fill='#203b3e')
    contact.save(OUT/'mockup-contact.png')
    print(json.dumps(dict(pdf=str(PDF),pages=26,verification=verification),indent=2))


if __name__=='__main__':
    main()
