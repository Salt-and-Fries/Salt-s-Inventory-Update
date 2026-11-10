"""The Aether: five native menu comparisons plus the Quark Oddities variant.

Design artifact only. Left references are immutable native gameplay crops;
right proposals use complete native artwork with SIU chrome.
"""
from pathlib import Path
from io import BytesIO
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
import pdfplumber

ROOT = Path(__file__).resolve().parents[1]
TMP = ROOT / 'tmp/pdfs/aether'
OUT = ROOT / 'output/aether-window-design'
PDF = ROOT / 'output/pdf/siu-aether-window-mockups.pdf'
JAR = ROOT / 'build/ui-modpack-audit-20261025/jars/e2fdd161f35ca1b6db774906e88e266d7f12eab6.jar'
spec = importlib.util.spec_from_file_location('native_siu_art', ROOT/'tools/build-enchanted-window-mockups.py')
art = importlib.util.module_from_spec(spec); spec.loader.exec_module(art)
N = Image.Resampling.NEAREST
BG = (198, 198, 198, 255)
CHECKS = []
RELEASE = 'https://modrinth.com/mod/aether/version/2411kUqF'
FABRIC_RELEASE = 'https://modrinth.com/mod/aether/version/d0gCQ3in'
SOURCE = 'https://github.com/The-Aether-Team/The-Aether/tree/1.20.1-develop'


def digest(p):
    return hashlib.sha256(Path(p).read_bytes()).hexdigest()


def asset(name):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(BytesIO(z.read('assets/aether/textures/gui/'+name+'.png'))).convert('RGBA')


def crop_native(row, rect=None):
    n = row['native']; path = ROOT/n['capture']
    assert digest(path) == n['sha256']
    b = n['bounds']; r = rect or (0, 0, b['width'], b['height'])
    im = Image.open(path).convert('RGBA')
    sx = im.width / 640; sy = im.height / 450
    assert sx == sy == 2, im.size
    return im.crop(tuple(int(2*v) for v in (b['x']+r[0], b['y']+r[1], b['x']+r[2], b['y']+r[3]))).resize((r[2]-r[0], r[3]-r[1]), N)


def paste(im, body, xy, reason):
    x, y = xy
    assert x >= 0 and y >= 0 and x+body.width <= im.width and y+body.height <= im.height, reason
    im.alpha_composite(body, xy)
    if body.getextrema()[-1] == (255, 255):
        assert im.crop((x,y,x+body.width,y+body.height)).tobytes() == body.tobytes(), reason
    CHECKS.append({'check':'complete native region retained', 'reason':reason, 'size':body.size, 'destination':xy})


def verify_slots(row, rect, predicate):
    slots = [s for s in row['native']['slots'] if predicate(s)]
    for s in slots:
        assert rect[0] <= s['x']-1 and rect[1] <= s['y']-1, s
        assert s['x']+17 <= rect[2] and s['y']+17 <= rect[3], s
    CHECKS.append({'check':'all 18 x 18 slot envelopes fit', 'case':row['id'], 'slots':len(slots)})
    return len(slots)


def header(im, title, cols=None):
    if cols is None:
        return art.header(im, title)
    start = im.width-5-len(cols)*14
    assert 7+art.tw(title)+7 <= start
    art.text(im,title,7,7)
    for i,c in enumerate(cols):
        sy=22 if c==0 else 0
        im.alpha_composite(art.CONTROLS.crop((11*c,sy,11*c+11,sy+11)),(start+i*14,5))
    return cols


def machine(row, title):
    rect = (35,15,113,71) if title=='Incubator' else (18,15,143,71)
    body = crop_native(row,rect)
    verify_slots(row,rect,lambda s:not s['player'])
    im=art.frame(146 if title=='Incubator' else 158,92)
    controls=header(im,title)
    paste(im,body,((im.width-body.width)//2,27),title+' apparatus, recipe button and every indicator')
    return im, {'source_rect':rect,'chrome':controls}


def accessories_window(row):
    # Native visibility toggles extend above and right of the 2 x 4 slot grid.
    rect=(76,4,114,80)
    assert verify_slots(row,rect,lambda s:not s['player'] and s['index'] >= (73 if 'backpack' in row['id'] else 46)) == 8
    body=crop_native(row,rect)
    im=art.frame(120,111);header(im,'Accessories')
    paste(im,body,((im.width-body.width)//2,26),'8 equipment ghosts, 8 complete slot borders and visibility toggles')
    return im


def character_context(row):
    im=art.frame(120,109);header(im,'Character')
    # Context only: native player preview and existing armor, not extra Aether slots.
    paste(im,crop_native(row,(7,7,76,79)),(10,28),'native player preview and armor context')
    paste(im,crop_native(row,(115,61,133,79)),(91,80),'native offhand context')
    button=asset('inventory/button/accessories_button').crop((0,0,12,8))
    paste(im,button,(94,30),'Aether equipment shortcut retained on Character')
    return im


def accessories(row, backpack=False):
    im=Image.new('RGBA',(330 if backpack else 144,229))
    # Short connectors illustrate the existing SIU connected-window behavior.
    d=ImageDraw.Draw(im);d.rectangle((79,108,82,122),fill=BG);d.line((79,108,79,122),fill='#555555')
    a=accessories_window(row);paste(im,a,(24,0),'attached Aether equipment window')
    c=character_context(row);paste(im,c,(24,120),'existing Character context, not a new equipment renderer')
    skins=crop_native(row,(-22,2,-2,22));paste(im,skins,(2,28),'native Moa Skins shortcut')
    if backpack:
        # The additional native rows are 27 backpack cells, not Aether equipment.
        rect=(4,80,173,141)
        assert verify_slots(row,rect,lambda s:not s['player'] and 45 <= s['index'] <= 71) == 27
        bp=art.frame(184,96);header(bp,'Backpack',[0,6,4,9])
        paste(bp,crop_native(row,rect),(7,26),'all 27 leather cells plus the full stitched surround')
        d=ImageDraw.Draw(im);d.rectangle((143,143,150,146),fill=BG)
        paste(im,bp,(146,120),'existing attached Backpack, no independent Close button')
    return im,{'equipment_layout':'2 columns x 4 rows; native visibility toggles',
               'context':'Character; optional existing Quark Backpack', 'backpack':backpack}


def lore(row):
    im=art.frame(256,168);header(im,'Book of Lore')
    body=asset('menu/lore_book').crop((0,0,234,105))
    # Preserve the real native lettering/inspection field inside the left page.
    # All cover pixels come from pristine publisher art: the screenshot's old
    # foreign sort button overlaps the lower cover and must not be copied.
    native=crop_native(row,(50,14,120,86))
    body.alpha_composite(native,(38,12))
    verify_slots(row,(12,2,246,107),lambda s:not s['player'])
    paste(im,body,(11,25),'both full pages, spine, blue cover and real inspection slot')
    paste(im,crop_native(row,(14,169,34,189)),(13,139),'complete native previous-page button')
    paste(im,crop_native(row,(221,169,241,189)),(223,139),'complete native next-page button')
    art.text(im,'Prev.',39,145);art.text(im,'Next',196,145)
    return im,{'book_source_rect':[0,0,234,105], 'navigation':'native buttons relocated into footer',
               'foreign_sort_overlap':'entire cover uses pristine publisher artwork; native page interior copied separately'}


SPECS=[
    dict(id='aether-altar',title='Altar',type='3 slots / Input, fuel and result',
         function='Enhances compatible ingredients and repairs equipment. The working area has one input, one fuel slot and one result, with the native processing indicators.',
         design='Keep the original furnace-like layout, recipe-book button, arrow, heat indicator and enlarged output border. Lift the complete apparatus below the SIU title, with a separate player inventory window.',
         preserve='3 slot roles / recipe book / progress and heat',opener='Use a placed Altar.'),
    dict(id='aether-freezer',title='Freezer',type='3 slots / Input, fuel and result',
         function='Processes compatible freezing recipes. The input sits above the fuel slot and feeds the result to the right.',
         design='Use the same compact layout as the Altar without changing any slot position relative to the machine artwork. Keep the recipe shortcut, full arrow and all indicator animation space.',
         preserve='3 slot roles / recipe book / full processing artwork',opener='Use a placed Freezer.'),
    dict(id='aether-incubator',title='Incubator',type='2 slots / Egg, fuel and a vertical progress meter',
         function='Incubates a Moa egg into a baby Moa. It has an egg slot and a fuel slot; the result is a creature rather than an item output.',
         design='Keep the native vertical arrangement and the complete tall progress meter beside it. Preserve the recipe button and fuel animation, with padding beneath every border. Do not invent an output slot.',
         preserve='2 slots / full-height progress meter / recipe book',opener='Use a placed Incubator.'),
    dict(id='aether-accessories-forge',title='Accessories',type='8 Aether equipment slots / Attached above Character',
         function='Adds eight equipment positions to the inventory, including native empty-slot symbols and individual render-visibility toggles. Moa Skins is a separate appearance shortcut.',
         design='Move only the two-by-four Aether equipment grid into a connected panel above Character. Keep every ghost icon and visibility toggle. Vanilla armor, offhand, crafting and inventory continue through their existing SIU windows; the small Character panel is shown as context.',
         preserve='8 ghosts and slot borders / visibility toggles / Moa Skins shortcut',opener='Use the Aether equipment button from the player inventory or its configured key binding.'),
    dict(id='shared-bb-aether-lore',title='Book of Lore',type='1 real item-inspection slot / Both pages and navigation retained',
         function='Accepts an actual item and displays its lore on the book pages. This is included as an item-inspection container, unlike a read-only guidebook.',
         design='Retain the complete book silhouette, blue cover, spine, pages and pale native item slot. Remove the integrated player inventory and move both original page buttons into a short footer. The separate SIU inventory supplies items for inspection.',
         preserve='1 inspection slot / both complete pages / previous and next controls',opener='Use a Book of Lore.'),
    dict(id='aether-accessories-backpack',title='Accessories + Quark Backpack',type='Optional integration / 8 equipment slots + 27 backpack slots',
         function='When Quark Oddities and a worn Backpack are present, Aether supplies a combined inventory layout containing both its equipment slots and the leather backpack storage.',
         design='Reuse the same Accessories panel and the existing Quark Backpack window. Keep all 27 leather slots and the complete stitched frame. The Backpack opens and closes with the player inventory, has no independent X, and retains its existing movement, pin and lock behavior.',
         preserve='8 Aether equipment slots / 27 leather storage slots / existing Backpack behavior',opener='Equip the Quark Backpack, open the player inventory and press the Aether equipment button.'),
]


def build_pdf(pages):
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    W,H=1191,842
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('The Aether / SIU Window Mockups');c.setAuthor("Salt's Inventory Update / Project Everything")
    def txt(s,x,y,size=12,bold=False,color='#19373D'):
        c.setFillColor(HexColor(color));c.setFont('SegoeB' if bold else 'Segoe',size)
        assert pdfmetrics.stringWidth(s,'SegoeB' if bold else 'Segoe',size) <= W-36-x, s
        c.drawString(x,y,s)
    def para(s,x,y,width,size=12,leading=18):
        line=''
        for word in s.split():
            candidate=(line+' '+word).strip()
            if pdfmetrics.stringWidth(candidate,'Segoe',size)>width:
                txt(line,x,y,size);y-=leading;line=word
            else:line=candidate
        if line:txt(line,x,y,size);y-=leading
        return y
    def base(title,sub,i):
        c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
        txt('PROJECT EVERYTHING / THE AETHER',36,H-34,10,True,'#247C80')
        txt(title,36,H-76,28,True);txt(sub,36,H-102,11,color='#5E7076')
        c.setStrokeColor(HexColor('#D7E2E3'));c.line(36,56,W-36,56)
        txt('Design proposals / Native game references: Minecraft 1.20.1, Aether 1.5.2 / No compatibility code changed',36,35,9,color='#5E7076')
        txt(f'{i} / 7',W-68,35,9,color='#5E7076')
    for i,p in enumerate(pages,1):
        base(p['title'],p['type'],i)
        c.bookmarkPage(p['id']);c.addOutlineEntry(p['title'],p['id'],level=0)
        ims=[Image.open(ROOT/p[k]).convert('RGBA') for k in ['original','mockup']]
        scale=min(2.35,510/max(im.width for im in ims),399/max(im.height for im in ims))
        for x,label,im in zip([36,613],['ORIGINAL IN GAME / SIU ABSENT','PROPOSED SIU WINDOW / MOCKUP'],ims):
            txt(label,x,698,10,True,'#247C80')
            c.setFillColor(HexColor('#EEF4F4'));c.roundRect(x,257,542,425,8,fill=1,stroke=0)
            enlarged=im.resize((im.width*6,im.height*6),N)
            dw,dh=im.width*scale,im.height*scale
            c.drawImage(ImageReader(enlarged),x+(542-dw)/2,257+(425-dh)/2,dw,dh,mask='auto')
        txt('WHAT IT DOES',36,231,9,True,'#247C80');a=para(p['function'],36,210,542)
        txt('WINDOW DESIGN',613,231,9,True,'#247C80');b=para(p['design'],613,210,542)
        assert min(a,b)>105,(p['id'],a,b)
        txt('Retained: '+p['preserve'],36,105,10,color='#5E7076')
        txt('Open: '+p['opener'],36,85,10,color='#5E7076')
        txt(p['reference_label'],36,65,8,color='#247C80')
        c.linkURL(RELEASE if p['loader']=='forge' else FABRIC_RELEASE,(36,60,600,76),thickness=0)
        c.showPage()
    base('Scope and native-screen appendix','Five main container screens, plus the optional Quark Oddities inventory variant',7)
    c.bookmarkPage('scope');c.addOutlineEntry('Scope and native-screen appendix','scope',level=0)
    sections=[
        ('ONE CONSISTENT DESIGN',
         'Use the real SIU border, bitmap font and normal window controls. The gray body matches the native #C6C6C6 surface exactly. Whole working regions are retained, with complete slot borders and padding. Controls use More and Close when the full row does not fit. Foreign redundant sort buttons are omitted from the proposals.'),
        ('ITEMS, INVENTORY AND ATTACHMENTS',
         'The machine windows retain their native slot roles, recipe books and progress animations. The Book of Lore is included because it accepts a real item; its native pages and navigation remain intact. Accessories keeps eight equipment slots, not the vanilla crafting cells. The optional Backpack uses the already approved attached-window behavior.'),
        ('NON-INVENTORY SCREENS / KEEP NATIVE',
         'Sun Altar time controls, Valkyrie Queen dialogue, Aether and alternate title menus, Moa Skins and supporter Customization stay in their native interfaces. Their existing entry buttons remain reachable. HUD overlays and recipe-viewer pages also stay native. No inventory window is proposed for these screens.'),
        ('REUSED SCREENS AND TEXTURE VARIANTS',
         'Treasure Chests use the vanilla chest menu, so they reuse the existing SIU chest window. Creative Accessories and Creative Accessories + Backpack are mode variants of the same two equipment screens: retain their native Creative/delete behavior without duplicating windows. The optional Classic book texture should keep its complete cover and page artwork in the same design.'),
        ('REFERENCE AND REVIEW LIMITS',
         'All six left-hand references are real native in-game crops with SIU absent. Machines and Lore use saved Fabric captures; both equipment layouts use fresh Forge captures with Quark and Oddities loaded. The Backpack is unequipped in the basic equipment reference. The right-hand images are mockups, including Character context. No compatibility implementation or gameplay suite was run for this PDF.'),
    ]
    y=677
    for label,body in sections:
        txt(label,36,y,10,True,'#247C80');y=para(body,36,y-23,1095,12,18)-20
    assert y>160,y
    txt('SOURCES',36,y,10,True,'#247C80')
    for n,(label,url) in enumerate([
        ('Publisher release: Aether 1.20.1-1.5.2, Forge/NeoForge',RELEASE),
        ('Publisher release: Aether 1.20.1-1.5.2-beta.9, Fabric',FABRIC_RELEASE),
        ('The Aether Team: source repository, 1.20.1 development branch',SOURCE),
    ]):
        yy=y-22-n*20;txt(label,36,yy,10,color='#247C80');c.linkURL(url,(36,yy-3,620,yy+12),thickness=0)
    txt('Pinned native classes and GUI assets checked against the original release JARs; screenshots and checks recorded in the design manifest.',36,y-88,9,color='#5E7076')
    c.showPage();c.save()


def main():
    for d in [OUT/'original',OUT/'mockups',PDF.parent]:d.mkdir(parents=True,exist_ok=True)
    assert hashlib.sha1(JAR.read_bytes()).hexdigest()==JAR.stem
    cases=json.loads((ROOT/'output/remaining-ui-audit/coverage.json').read_text())['cases']
    refs={r['id']:r for r in cases if r.get('entry')==576}
    for file in ['forge-accessories-reference.json','optional-reference.json']:
        r=json.loads((TMP/file).read_text());refs[r['id']]=r
    pages=[]
    for s in SPECS:
        row=refs[s['id']];is_equipment='accessories' in s['id']
        original=crop_native(row,(-24,0,176,row['native']['bounds']['height']) if is_equipment else (0,-4,256,199) if s['title']=='Book of Lore' else None)
        if is_equipment:mock,meta=accessories(row,'backpack' in s['id'])
        elif s['title']=='Book of Lore':mock,meta=lore(row)
        else:mock,meta=machine(row,s['title'])
        a=OUT/'original'/(s['id']+'.png');b=OUT/'mockups'/(s['id']+'.png')
        original.save(a);mock.save(b)
        pages.append({**s,**meta,'original':str(a.relative_to(ROOT)),'mockup':str(b.relative_to(ROOT)),
                      'loader':row['loader'],'reference_label':('Forge 1.20.1 / Aether 1.5.2 / Quark 4.0-462 + Oddities 1.20.1' if is_equipment else 'Fabric 1.20.1 / Aether 1.5.2-beta.9 / Native gameplay reference'),
                      'capture':row['native']['capture'],'capture_sha256':row['native']['sha256'],
                      'mockup_sha256':digest(b),'original_crop_sha256':digest(a)})
    build_pdf(pages)
    reader=PdfReader(PDF);assert len(reader.pages)==7
    for i,s in enumerate(SPECS):assert s['title'] in reader.pages[i].extract_text()
    with pdfplumber.open(PDF) as doc:
        for page in doc.pages:
            for ch in page.chars:
                assert 26<=ch['x0']<=ch['x1']<=1166 and 20<=ch['top']<=ch['bottom']<=820,ch
    assert sum(c.get('slots',0) for c in CHECKS) == 52
    manifest={'status':'design only; not implemented','scope':'five native screens plus optional Quark Oddities layout',
              'pdf':str(PDF.relative_to(ROOT)),'pdf_sha256':digest(PDF),'pages':7,
              'native_screenshot_comparisons':6,'slot_envelopes_checked':sum(c.get('slots',0) for c in CHECKS),
              'checks':CHECKS,'cases':pages}
    (OUT/'design-manifest.json').write_text(json.dumps(manifest,indent=2))
    print(json.dumps({k:v for k,v in manifest.items() if k not in ['checks','cases']},indent=2))


if __name__=='__main__':main()
