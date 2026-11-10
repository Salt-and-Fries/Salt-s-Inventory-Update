"""Real native and implemented Quark captures; no mock screenshots or UI repainting."""
from pathlib import Path
import hashlib,json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/quark-window-implementation'
OUT=ROOT/'output/quark-window-implementation'
PDF=ROOT/'output/pdf/siu-quark-implemented-comparisons.pdf'
OUT.mkdir(parents=True,exist_ok=True);PDF.parent.mkdir(parents=True,exist_ok=True)
specs=[
 ('quark-crate','Storage Crate','Original six-row grid and scrollbar. Search and capacity sit below the title; SIU supplies storage controls.',
  'Wheel and thumb scrolling; search and clear; 640-item capacity, remainder return and sorting with no count loss.'),
 ('quark-feeding-trough','Feeding Trough','Nine native feeding slots stay together, with complete borders and compact padding.',
  'Native window opening and item transfers. Quark without Oddities was checked separately.'),
 ('quark-crafter','Crafter','Native 3×3 recipe grid, disabled-slot artwork, redstone indicator and complete result-preview frame.',
  'Disable and re-enable empty inputs; live recipe preview; a redstone pulse consumes one of each ingredient.'),
 ('quark-held-shulker','Held Shulker Box','All 27 original cells become a regular storage window. Open it by right-clicking the carried box in Inventory.',
  'Native entry and transfers; edited contents persist to the box; the source item stays locked while open.'),
 ('quark-backpack','Backpack + Inventory','The equipped Backpack opens above Inventory. No Backpack X; dragging either header moves both. Pin and lock stay synchronized.',
  'Inventory X/key close and reopen; content persistence; edge fit and scaling; native filled/empty equip restrictions.'),
 ('quark-matrix-enchanter','Matrix Enchanting','The complete native puzzle panel retains its item/lapis/output slots, indicators, grid, visible piece docket and + button.',
  'Paid generation; select and rotate; place and remove; take enchanted output with native input consumption.'),
]
pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
W,H=1191,842
c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
c.setTitle('Quark / Implemented SIU Windows — 1.20.1 Forge')
c.setAuthor("Salt's Inventory Update / Project Everything")
def txt(text,x,y,size=11,bold=False,color='#19373D'):
    c.setFillColor(HexColor(color));c.setFont('SegoeB' if bold else 'Segoe',size);c.drawString(x,y,text)
def para(text,x,y,width,size=12,leading=18):
    line=''
    for word in text.split():
        candidate=(line+' '+word).strip()
        if pdfmetrics.stringWidth(candidate,'Segoe',size)>width:
            txt(line,x,y,size);y-=leading;line=word
        else:line=candidate
    if line:txt(line,x,y,size);y-=leading
    return y
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
pages=[]
for number,(ident,title,behavior,checked) in enumerate(specs,1):
    case=json.loads((WORK/(ident+'.json')).read_text())
    original=ROOT/'output/quark-window-design/original'/(ident+'.png')
    implemented=WORK/(ident+'-window.png')
    if ident=='quark-backpack':
        state=case['state'];ss=Image.open(case['capture']).convert('RGB');scale=ss.width/state['guiWidth']
        bag=next(w['bounds'] for w in state['allWindows'] if w['menuId']=='salts_inventory_compat:quark_backpack')
        inv=next(w['bounds'] for w in state['workspaceWindows'] if w['kind']=='INVENTORY')
        x=min(inv['x'],bag['x'])-2;y=min(inv['y'],bag['y'])-2
        right=max(inv['x']+inv['width'],bag['x']+bag['width'])+2;bottom=max(inv['y']+inv['height'],bag['y']+bag['height'])+2
        pair=ss.crop((x*scale,y*scale,right*scale,bottom*scale)).resize((right-x,bottom-y),Image.Resampling.NEAREST)
        implemented=OUT/'quark-backpack-and-inventory.png';pair.save(implemented)
    else:
        target=OUT/(ident+'.png');target.write_bytes(implemented.read_bytes());implemented=target
    txt('PROJECT EVERYTHING / QUARK WINDOWS IMPLEMENTED',34,H-34,10,True,'#247C80')
    txt(title,34,H-74,28,True)
    txt('Minecraft 1.20.1 Forge | Quark 4.0-462 | Zeta 1.0-31 | optional Oddities 1.20.1',34,H-101,11,color='#5E7076')
    c.bookmarkPage(ident);c.addOutlineEntry(title,ident,level=0)
    images=[Image.open(original).convert('RGB'),Image.open(implemented).convert('RGB')]
    scale=min(2.4,515/max(im.width for im in images),394/max(im.height for im in images))
    for x,label,im in zip([34,610],['ORIGINAL IN GAME / SIU ABSENT','IMPLEMENTED SIU WINDOW / IN GAME'],images):
        txt(label,x,H-138,10,True,'#247C80');c.setFillColor(HexColor('#EEF4F4'));c.roundRect(x,258,547,426,7,fill=1,stroke=0)
        dw,dh=im.width*scale,im.height*scale
        # Integer nearest-neighbor enlargement keeps original pixel edges in the embedded raster.
        big=im.resize((im.width*4,im.height*4),Image.Resampling.NEAREST)
        c.drawImage(ImageReader(big),x+(547-dw)/2,258+(426-dh)/2,dw,dh)
    txt('IMPLEMENTED BEHAVIOR',34,228,9,True,'#247C80');y=para(behavior,34,207,547)
    txt('FOCUSED CHECKS',610,228,9,True,'#247C80');z=para(checked,610,207,547)
    assert min(y,z)>114
    para('Actual game captures with independently seeded contents. This is focused integration evidence, not a complete gameplay suite. All 130 cell bounds and 6,370 count-free rim pixels were checked against native references.',34,104,1120,10,14)
    c.setStrokeColor(HexColor('#D8E3E5'));c.line(34,55,W-34,55)
    txt('Core/API unchanged | Original inventory is handled by SIU’s player windows | '+('Oddities optional' if ident in ['quark-crate','quark-backpack','quark-matrix-enchanter'] else 'Quark native menu'),34,35,9,color='#60747A')
    txt(str(number)+' / 6',W-69,35,9,color='#60747A')
    c.showPage()
    pages.append(dict(id=ident,title=title,original=str(original.relative_to(ROOT)),implemented=str(implemented.relative_to(ROOT)),
                      capture=case['capture'],run=case['run'],capture_sha256=sha(case['capture']),original_sha256=sha(original),
                      implemented_sha256=sha(implemented),behavior=behavior,focused_checks=checked))
c.save()
manifest=dict(pdf=str(PDF.relative_to(ROOT)),sha256=sha(PDF),pages=pages,
              scope='Six implemented Quark windows on 1.20.1 Forge, including optional Oddities. Focused checks only.',
              coverage_limits=['Matrix duplicate-piece merging was not separately exercised.','Not every configured item blacklist was exercised.','The optional Aether backpack integration is outside this adapter.'],
              layout_checks=json.loads((WORK/'layout-checks.json').read_text()))
(OUT/'implementation-manifest.json').write_text(json.dumps(manifest,indent=2))
print(PDF)
