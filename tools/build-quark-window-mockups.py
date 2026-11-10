"""Quark design review: real native references and explicitly authored proposals."""
from pathlib import Path
from io import BytesIO
import hashlib,json,zipfile
from PIL import Image,ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/quark-window-design'
OUT=ROOT/'output/quark-window-design'
PDF=ROOT/'output/pdf/siu-quark-container-window-mockups.pdf'
GUI=ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
FRAME=Image.open(GUI/'window.png').convert('RGBA')
CONTROLS=Image.open(GUI/'window_controls.png').convert('RGBA')
NEAR=Image.Resampling.NEAREST
BG=(198,198,198,255)
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
PROTECTED=[]
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()

def glyph(ch):
    n=ord(ch);g=ASCII.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8));b=g.getbbox()
    return g,3 if ch==' ' else b[2] if b else 5

def tw(s):return sum(glyph(c)[1]+1 for c in s)

def text(im,s,x,y,color=(64,64,64,255)):
    assert x>=0 and y>=0 and x+tw(s)<=im.width and y+8<=im.height,(s,im.size,x,y)
    for ch in s:
        g,w=glyph(ch);ink=Image.new('RGBA',(8,8),color);ink.putalpha(g.getchannel('A'));im.alpha_composite(ink,(x,y));x+=w+1

def frame(w,h):
    im=Image.new('RGBA',(w,h))
    for s,d in [((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),
        ((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]:
        im.alpha_composite(FRAME.crop(s).resize((d[2]-d[0],d[3]-d[1]),NEAR),d[:2])
    return im

def header(im,title,storage=False):
    full=[10,1,0,6,5,9,2] if storage else [0,6,5,9,2]
    cols=full if tw(title)+len(full)*14+14<=im.width else [3,2]
    start=im.width-5-len(cols)*14
    assert 7+tw(title)+6<=start,(title,im.width,cols)
    text(im,title,7,7)
    for i,col in enumerate(cols):
        y=22 if col==0 else 0
        im.alpha_composite(CONTROLS.crop((col*11,y,col*11+11,y+11)),(start+i*14,5))
    return cols

def paste(im,part,xy,label,source_rect):
    x,y=xy;assert x>=6 and y>=22 and x+part.width<=im.width-6 and y+part.height<=im.height-6,(label,im.size,part.size,xy)
    im.alpha_composite(part,xy)
    assert im.crop((x,y,x+part.width,y+part.height)).tobytes()==part.tobytes(),label
    PROTECTED.append(dict(label=label,source_rect=source_rect,destination=[x,y],size=list(part.size),canvas=list(im.size)))

def native_crop(row,rect):
    p=ROOT/row['capture'];assert sha(p)==row['sha256'];s=row['state'];b=s['bounds'];im=Image.open(p).convert('RGBA')
    scale=round(im.width/s['guiWidth']);assert scale==2
    box=tuple(scale*v for v in [b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3]])
    assert min(box)>=0 and box[2]<=im.width and box[3]<=im.height
    return im.crop(box).resize((rect[2]-rect[0],rect[3]-rect[1]),NEAR)

def cells_rect(row,predicate):
    cells=[c for c in row['state']['slots'] if predicate(c)];assert cells
    return [min(c['x'] for c in cells)-1,min(c['y'] for c in cells)-1,max(c['x'] for c in cells)+17,max(c['y'] for c in cells)+17]

def design(row):
    key=row['id'];body_rect=None;edits=[]
    if key=='quark-crate':
        im=frame(200,164);controls=header(im,'Storage Crate',True)
        # Keep search and capacity in a separate row, clear of SIU controls.
        d=ImageDraw.Draw(im);d.rectangle((7,24,140,36),fill=(139,139,139,255));d.line((7,24,140,24),fill=(55,55,55,255));d.line((7,24,7,36),fill=(55,55,55,255));d.line((7,36,140,36),fill=(255,255,255,255));d.line((140,24,140,36),fill=(255,255,255,255))
        text(im,'Search...',11,27,(213,213,213,255))
        count=sum(c['count'] for c in row['state']['slots'] if not c['player']);label=f'{count}/640';text(im,label,193-tw(label),27)
        body_rect=[7,17,191,130];body=native_crop(row,body_rect)
        # The scrollbar is taller than the six-row grid. Remove the old
        # inventory label/buttons from only the spare area below the cells.
        ImageDraw.Draw(body).rectangle((0,108,164,112),fill=BG)
        edits.append(dict(reason='old Inventory label and native inventory buttons below grid',rect=[7,125,172,130]))
        paste(im,body,(8,43),'54 visible crate cells and complete scrollbar',body_rect)
    elif key=='quark-feeding-trough':
        im=frame(144,88);controls=header(im,'Feeding Trough',True);body_rect=cells_rect(row,lambda c:not c['player'])
        part=native_crop(row,body_rect);paste(im,part,((im.width-part.width)//2,26),'all nine feeding cells',body_rect)
    elif key=='quark-crafter':
        im=frame(156,88);controls=header(im,'Crafter');body_rect=[25,16,155,71]
        paste(im,native_crop(row,body_rect),(13,26),'native disabled cells, redstone arrow and recipe result',body_rect)
    elif key=='quark-held-shulker':
        im=frame(176,88);controls=header(im,'Shulker Box',True);body_rect=[7,17,169,71]
        paste(im,native_crop(row,body_rect),(7,26),'all 27 shulker cells',body_rect)
    elif key=='quark-backpack':
        im=frame(184,96);controls=header(im,'Backpack',True)
        assert cells_rect(row,lambda c:c['index']>=46)==[7,83,169,137]
        # Keep the complete stitched leather surround as well as the cells.
        # Native y=141 begins the ordinary player inventory and is excluded.
        body_rect=[4,80,173,141]
        paste(im,native_crop(row,body_rect),(7,26),'all 27 backpack cells and complete stitched leather surround',body_rect)
    elif key=='quark-matrix-enchanter':
        im=frame(180,98);controls=header(im,'Matrix Enchanting');body_rect=[5,9,171,76];body=native_crop(row,body_rect)
        # Remove only the old Inventory label below the left-side controls.
        # The grid, cost sprites, charge bar, docket and + button stay complete.
        d=ImageDraw.Draw(body);d.rectangle((1,63,73,66),fill=BG);d.rectangle((1,0,73,4),fill=BG)
        edits.extend([dict(reason='old Inventory label only',rect=[6,72,79,76]),dict(reason='old Enchant title only',rect=[6,9,79,14])])
        paste(im,body,(7,24),'complete matrix grid, pieces, charge, slots, costs and plus button',body_rect)
    else:raise ValueError(key)
    return im,dict(body_rect=body_rect,chrome_columns=controls,artwork_edits=edits)

SPECS=[
 ('quark-crate','Storage Crate','Audited scope / Oddities storage','Use the Storage Crate block.',
  'A large storage container with a six-row viewport, scrolling, search and a total-item capacity counter.',
  'Keep the original nine-column grid and full scrollbar. Give search and capacity their own slim row below the title. Use SIU sorting and transfer controls.',
  'addons/oddities/client/screen/CrateScreen.java'),
 ('quark-feeding-trough','Feeding Trough','Audited scope','Use a Feeding Trough block.',
  'Nine food slots supply nearby animals. The example shows wheat, carrots and potatoes.',
  'Keep the 3 x 3 food grid at its native size. Use a compact gray frame with balanced padding; More holds controls that do not fit beside the title.',
  'content/automation/client/screen/TroughScreen.java'),
 ('quark-crafter','Crafter','Audited scope / 1.20.1 backport','Use Quark\'s Crafter block.',
  'A redstone-driven crafting grid with a recipe-result preview. Empty input cells can be enabled or disabled.',
  'Preserve the original input arrangement, disabled-slot artwork, arrow and result preview together. Keep native slot toggles; sorting does not belong on this recipe grid.',
  'content/automation/client/screen/CrafterScreen.java'),
 ('quark-held-shulker','Held Shulker Box','Audited scope','In Survival inventory, right-click a carried Shulker Box with an empty cursor.',
  'Open a carried Shulker Box without placing it. The container has the familiar 27 storage cells.',
  'Use SIU\'s normal storage-window design and retain all three complete rows. The held-box restrictions stay native; player inventory is a separate SIU window.',
  'content/management/client/screen/HeldShulkerBoxScreen.java'),
 ('quark-backpack','Backpack','Additional optional Oddities layout','Equip the Backpack in the chest slot, then open Survival inventory.',
  'The worn Backpack adds 27 storage slots to the player inventory screen.',
  'Keep all 27 slots and their complete stitched leather surround in a connected storage window. SIU\'s existing Character, Crafting and Inventory windows handle the other parts of the original screen.',
  'addons/oddities/client/screen/BackpackInventoryScreen.java'),
 ('quark-matrix-enchanter','Matrix Enchanting','Additional optional Oddities layout','With Oddities enabled, use the Matrix Enchanting Table.',
  'Lapis and XP generate enchantment pieces. Place, rotate and merge them on the matrix; the native docket lists available pieces.',
  'Keep the complete working panel as one unit: item, lapis, charge meter, output, bookshelf/XP indicators, matrix, piece list and + button. Add SIU chrome above it with clear padding.',
  'addons/oddities/client/screen/MatrixEnchantingScreen.java'),
]

def main():
    for p in [OUT/'original',OUT/'mockups',PDF.parent]:p.mkdir(parents=True,exist_ok=True)
    refs=json.loads((WORK/'native-references.json').read_text());by_id={r['id']:r for r in refs['cases']};assert len(by_id)==6
    pages=[]
    for ident,title,badge,opener,function,proposal,source in SPECS:
        row=by_id[ident];s=row['state'];assert not s['siu_loaded'] and s['screen'].startswith('org.violetmoon.quark.')
        rect=[0,0,196 if ident=='quark-crate' else s['bounds']['width'],s['bounds']['height']]
        original=native_crop(row,rect);mock,meta=design(row)
        a=OUT/'original'/(ident+'.png');b=OUT/'mockups'/(ident+'.png');original.save(a);mock.save(b)
        pages.append(dict(id=ident,title=title,badge=badge,how_to_open=opener,function=function,proposal=proposal,source_url='https://github.com/VazkiiMods/Quark/blob/1.20/src/main/java/org/violetmoon/quark/'+source,
            original=str(a.relative_to(ROOT)),mockup=str(b.relative_to(ROOT)),original_sha256=sha(a),mockup_sha256=sha(b),capture=row['capture'],capture_sha256=row['sha256'],native_crop=rect,logical_size=list(mock.size),**meta))
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    W,H=1191,842;c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1);c.setTitle('Quark / SIU Container Window Mockups');c.setAuthor("Salt's Inventory Update / Project Everything")
    def txt(s,x,y,size=11,bold=False,color='#19373D'):
        c.setFillColor(HexColor(color));c.setFont('SegoeB' if bold else 'Segoe',size);c.drawString(x,y,s)
    def para(s,x,y,width,size=12,leading=17):
        line=''
        for word in s.split():
            candidate=(line+' '+word).strip()
            if pdfmetrics.stringWidth(candidate,'Segoe',size)>width:
                txt(line,x,y,size);y-=leading;line=word
            else:line=candidate
        if line:txt(line,x,y,size);y-=leading
        return y
    for i,p in enumerate(pages,1):
        c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
        txt('PROJECT EVERYTHING / QUARK WINDOW DESIGN',34,H-34,10,True,'#247C80');txt(p['title'],34,H-74,28,True)
        txt(p['badge']+' | Minecraft 1.20.1 Forge | Quark 4.0-462 + Quark Oddities 1.20.1',34,H-101,11,color='#5E7076')
        c.bookmarkPage(p['id']);c.addOutlineEntry(p['title'],p['id'],level=0)
        panels=[(34,'ORIGINAL IN GAME / SIU ABSENT','original'),(610,'PROPOSED SIU WINDOW / MOCKUP','mockup')]
        for x,label,key in panels:
            txt(label,x,H-138,10,True,'#247C80');c.setFillColor(HexColor('#EEF4F4'));c.roundRect(x,258,547,426,7,fill=1,stroke=0)
            im=Image.open(ROOT/p[key]).convert('RGBA');scale=min(2.4,515/im.width,394/im.height);dw,dh=im.width*scale,im.height*scale
            # The source crop is exact; only whole-document display scales it.
            c.drawImage(ImageReader(im),x+(547-dw)/2,258+(426-dh)/2,dw,dh,mask='auto')
        txt('WHAT IT DOES',34,228,9,True,'#247C80');y=para(p['function'],34,207,547,12,17)
        txt('WINDOW DESIGN',610,228,9,True,'#247C80');z=para(p['proposal'],610,207,547,12,17);assert min(y,z)>113,(p['id'],y,z)
        txt('Open: '+p['how_to_open'],34,106,10,color='#5E7076')
        txt('Reference: original screen source',34,81,9,color='#247C80');c.linkURL(p['source_url'],(34,78,210,94),relative=0,thickness=0)
        if i==6:
            txt('Quark feature guide: Matrix Enchanting / Backpack',265,81,9,color='#247C80');c.linkURL('https://www.quarkmod.net/',(265,78,525,94),relative=0,thickness=0)
        c.setStrokeColor(HexColor('#D7E2E3'));c.line(34,57,W-34,57)
        txt('Real native screenshot + theoretical SIU mockup. Player inventory stays separate. No compatibility code changed.',34,37,9,color='#5E7076');txt(f'{i} / {len(pages)}',W-68,37,9,color='#5E7076');c.showPage()
    c.save()
    manifest=dict(pdf=str(PDF.relative_to(ROOT)),pdf_sha256=sha(PDF),pages=6,scope='Four audited container layouts plus Backpack and Matrix Enchanting from optional Oddities; design only, not implemented or gameplay-certified.',
        reference_profile=refs['run'],source_reference=str((WORK/'native-references.json').relative_to(ROOT)),cases=pages,protected_artwork=PROTECTED)
    (OUT/'design-manifest.json').write_text(json.dumps(manifest,indent=2));print(json.dumps({k:v for k,v in manifest.items() if k not in ['cases','protected_artwork']},indent=2))

if __name__=='__main__':main()
