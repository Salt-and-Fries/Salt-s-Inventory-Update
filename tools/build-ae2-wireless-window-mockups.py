"""Five design-only AE2WTLib proposals, built from verified native captures.

The left images are actual SIU-absent captures. The right images are authored
mockups using those native pixels and SIU's already implemented AE2 controls.
No compatibility, Core, API or distribution artifacts are modified.
"""
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
WORK=ROOT/'build/ae2-wireless-window-design'
OUT=ROOT/'output/ae2-wireless-window-design'
PDF=ROOT/'output/pdf/siu-ae2-wireless-terminal-window-mockups.pdf'
NEAR=Image.Resampling.NEAREST
BG=(198,198,198,255)
AE2=ROOT/'build/ui-modpack-audit-20261025/jars/94ed8b253201ef420c06c941af2d450ada1a8c1a.jar'
WT=ROOT/'build/ui-modpack-audit-20261025/jars/180a4e0b62e65930e0aef9dc0380c425e7d5d7b4.jar'
ATLAS=ROOT/'build/gameplay-qa/ae2-atlas/1.20.1-fabric/atlas'
CHROME=Image.open(ATLAS/'37-crafting-terminal.png').convert('RGBA')
CHROME_CONTROLS=json.loads((ATLAS/'37-crafting-terminal.controls.json').read_text())
with zipfile.ZipFile(WT) as z:
    SKIN=Image.open(BytesIO(z.read('assets/ae2/textures/wtlib/guis/trash.png'))).convert('RGBA')
    EXTRAS=Image.open(BytesIO(z.read('assets/ae2/textures/wtlib/guis/extras.png'))).convert('RGBA')
    MAGNET=Image.open(BytesIO(z.read('assets/ae2wtlib/textures/item/magnet_card.png'))).convert('RGBA')
with zipfile.ZipFile(AE2) as z:
    TERMINAL=Image.open(BytesIO(z.read('assets/ae2/textures/item/wireless_crafting_terminal.png'))).convert('RGBA')
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
PROTECTED=[]
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()

SPECS=[
 dict(id='own-wct-main',title='Wireless Crafting Terminal',short='Crafting',
      opener='Use a linked, charged Wireless Crafting Terminal, or select its mode on a Universal Terminal.',
      function='Browse network storage and craft with a persistent 3 x 3 grid. The terminal also exposes view cells, upgrades, Quantum Bridge support and optional Magnet Card controls.',
      proposal='Use the existing AE2 terminal layout: search below the SIU controls, the full-width catalog above crafting, and complete native side panels. Remove the embedded player/equipment area; SIU Character and Inventory handle those slots. The two Magnet Card shortcuts are shown in the spare lower-right area when available.',
      source='wct/WCTScreen.java',notes='Catalog ordering, crafting-grid return buttons, crafting status, Trash and terminal cycling remain available.'),
 dict(id='own-wct-trash',title='Trash',short='Trash',
      opener='Choose the trash-can button in the Wireless Crafting Terminal toolbar.',
      function='A separate 27-slot trash inventory, reached from the crafting terminal. Item handling must retain the original mod\'s behavior.',
      proposal='Keep all three complete rows in a compact AE2 frame. Move the parent-terminal return button to the left toolbar so it cannot collide with SIU\'s title controls. Use the shared SIU Inventory for item transfers.',
      source='wct/TrashScreen.java',notes='Keep the original 27 slots and return action; do not add a new delete or confirmation workflow.'),
 dict(id='own-wct-magnet',title='Magnet Filter',short='Magnet',
      opener='Install a Magnet Card, then use its filter/settings shortcut in the crafting terminal.',
      function='Two independent 27-entry ghost filters control pickup and insertion. Each has a whitelist/blacklist mode; the arrow and swap buttons copy or exchange their contents.',
      proposal='Retain both native grids and the entire central control group as one continuous panel. Remove only the player inventory below them. The return shortcut moves to the left toolbar, leaving SIU\'s normal AE2 controls clear.',
      source='wct/magnet_card/config/MagnetScreen.java',notes='Top grid: pickup. Bottom grid: insertion. Preserve both mode buttons, Copy Down, Copy Up and Swap.'),
 dict(id='pr-wireless-pattern-encoding',title='Wireless Pattern Encoding Terminal',short='Pattern Encoding',
      opener='Use a linked, charged Wireless Pattern Encoding Terminal, or its Universal Terminal mode.',
      function='Browse network contents and encode patterns. Crafting, processing, smithing and stonecutting use the same screen, with native mode tabs and pattern input/output slots.',
      proposal='Reuse AE2\'s existing pattern-terminal window. Keep the full native encoding panel, all four mode tabs, the encode button, blank/encoded pattern cells and every right-side panel. Search gets a separate row below the window controls.',
      source='wet/WETScreen.java',notes='One window supports all four native modes; hidden slots for inactive modes must remain hidden.'),
 dict(id='pr-wireless-pattern-access',title='Wireless Pattern Access Terminal',short='Pattern Access',
      opener='Use a linked, charged Wireless Pattern Access Terminal, or its Universal Terminal mode.',
      function='Search and browse connected pattern providers and manage their patterns remotely. The captured reference has no connected provider rows to display.',
      proposal='Keep AE2\'s native provider-list renderer and scrollbar. Move search to its own complete row under the title, preserve terminal cycling and list options, and retain the Quantum Bridge and upgrade panels. The list remains a list, with its native populated rows when connected.',
      source='wat/WATScreen.java',notes='Five inventory screens total. The Universal Terminal radial selector stays native; shared AE2 settings and crafting dialogs reuse the existing integration.'),
]

def glyph(ch):
    n=ord(ch);g=ASCII.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8));b=g.getbbox()
    return g,3 if ch==' ' else b[2] if b else 5

def tw(s):return sum(glyph(ch)[1]+1 for ch in s)

def text(im,s,x,y,color=(64,64,64,255)):
    assert 0<=x and 0<=y and x+tw(s)<=im.width and y+8<=im.height,(s,im.size,x,y)
    for ch in s:
        g,w=glyph(ch);ink=Image.new('RGBA',(8,8),color);ink.putalpha(g.getchannel('A'))
        im.alpha_composite(ink,(x,y));x+=w+1

def frame(w,h):
    # Exact legacy AE2WTLib corner/edge pixels, without the native inventory band.
    im=Image.new('RGBA',(w,h),BG)
    for src,dst in [((0,0,5,5),(0,0,5,5)),((171,0,176,5),(w-5,0,w,5)),
        ((0,160,5,165),(0,h-5,5,h)),((171,160,176,165),(w-5,h-5,w,h)),
        ((5,0,6,5),(5,0,w-5,5)),((5,160,6,165),(5,h-5,w-5,h)),
        ((0,5,5,6),(0,5,5,h-5)),((171,5,176,6),(w-5,5,w,h-5))]:
        # Paste rather than composite so transparent chamfer pixels stay clear.
        im.paste(SKIN.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]),NEAR),dst[:2])
    return im

def header(im,title,reserve_status=False):
    start=im.width-5-(26 if reserve_status else 0)-67
    assert 8+tw(title)+5<=start,(title,im.width,start)
    text(im,title,8,6)
    for i,c in enumerate(CHROME_CONTROLS):
        icon=CHROME.crop((c['x']*2,c['y']*2,(c['x']+11)*2,(c['y']+11)*2)).resize((11,11),NEAR)
        im.alpha_composite(icon,(start+i*14,4))
    return ['focus','pin','lock','link','close']

def search(im,y=23):
    d=ImageDraw.Draw(im);x=8;w=162;h=13
    d.rectangle((x,y,x+w-1,y+h-1),fill=(139,139,139,255))
    d.line((x,y,x+w-2,y),fill=(85,85,85,255));d.line((x,y,x,y+h-2),fill=(85,85,85,255))
    d.line((x,y+h-1,x+w-1,y+h-1),fill=(255,255,255,255));d.line((x+w-1,y,x+w-1,y+h-1),fill=(255,255,255,255))
    text(im,'Search...',x+3,y+2,(213,213,213,255))

def sources():
    rows={}
    for filename in ['own-wct-three-pairs.json','pr-wireless-additional-pairs.json','pr-wireless-wat-pairs.json']:
        for r in json.loads((ROOT/'output/remaining-ui-audit'/filename).read_text())['cases']:
            if r['entry']==48:rows[r['id']]={**r,'reference_manifest':filename}
    assert set(rows)=={s['id'] for s in SPECS}
    for row in rows.values():
        p=ROOT/row['native']['capture'];assert sha(p)==row['native']['sha256']
        lineage=json.loads((p.parent.parent/'staged-artifacts.json').read_text())
        assert lineage['siu_loaded'] is False and not any(a['filename'].startswith('salts_inventory') for a in lineage['artifacts'])
        assert any(a['filename']=='ae2wtlib-15.2.1-fabric.jar' and a['sha256']==sha(WT) for a in lineage['artifacts'])
        assert any(a['filename']=='appliedenergistics2-fabric-15.4.11.jar' and a['sha256']==sha(AE2) for a in lineage['artifacts'])
        assert Image.open(p).size==(1280,900)
    return rows

def crop(row,rect):
    b=row['native']['bounds'];a=Image.open(ROOT/row['native']['capture']).convert('RGBA')
    box=tuple(2*v for v in (b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3]))
    assert box[0]>=0 and box[1]>=0 and box[2]<=a.width and box[3]<=a.height
    return a.crop(box).resize((rect[2]-rect[0],rect[3]-rect[1]),NEAR)

def clean_world(im):
    """Remove only edge-connected world pixels outside the native panel outline."""
    im=im.copy();pending=[(x,y) for x in range(im.width) for y in [0,im.height-1]]+[(x,y) for y in range(im.height) for x in [0,im.width-1]]
    seen=set()
    while pending:
        x,y=pending.pop()
        if (x,y) in seen or not(0<=x<im.width and 0<=y<im.height):continue
        seen.add((x,y));r,g,b,a=im.getpixel((x,y))
        if max(r,g,b)-min(r,g,b)<3 and a:continue
        im.putpixel((x,y),(0,0,0,0));pending.extend([(x-1,y),(x+1,y),(x,y-1),(x,y+1)])
    return im

def protected(dst,row,rect,xy,label):
    part=crop(row,rect);x,y=xy
    assert x>=0 and y>=20 and x+part.width<=dst.width and y+part.height<=dst.height-6,(label,dst.size,rect,xy)
    dst.alpha_composite(part,xy)
    assert dst.crop((x,y,x+part.width,y+part.height)).tobytes()==part.tobytes(),label
    PROTECTED.append(dict(case=row['id'],label=label,source=rect,destination=list(xy),size=list(part.size),unchanged=True))

def toolbar(row,count):return [(crop(row,(-20,8+i*20,-4,24+i*20)),(-21,8+i*20)) for i in range(count)]

def item_button(icon):
    im=Image.new('RGBA',(18,18),BG);d=ImageDraw.Draw(im)
    d.line((0,0,17,0),fill='white');d.line((0,0,0,17),fill='white')
    d.line((0,17,17,17),fill=(85,85,85,255));d.line((17,0,17,17),fill=(85,85,85,255))
    im.alpha_composite(icon.resize((16,16),NEAR),(1,1));return im

def panels(row):
    n=row['native']['bounds']['width'];is_access=row['id'].endswith('pattern-access')
    rects=[(n+2,0,n+34,32),(n+2,33,n+34,83)] if is_access else [(n+2,0,n+34,104),(n+2,107,n+34,139),(n+2,142,n+34,192)]
    result=[]
    for rect in rects:
        original=crop(row,rect);part=clean_world(original)
        assert all(part.getpixel((x,y))==original.getpixel((x,y)) for y in range(part.height) for x in range(part.width) if part.getpixel((x,y))[3])
        PROTECTED.append(dict(case=row['id'],label='complete native auxiliary panel',source=list(rect),destination=[197,rect[1]],size=list(part.size),unchanged_native_pixels=True,world_corners_masked=True))
        result.append((part,(197,rect[1])))
    return result

def design(row):
    key=row['id'];parts=[];mapping=[]
    if key=='own-wct-main':
        im=frame(195,172);header(im,'Terminal',True);search(im)
        protected(im,row,(7,17,169,53),(7,39),'18 catalog cells')
        protected(im,row,(174,17,188,53),(174,39),'complete catalog scrollbar')
        text(im,'Crafting Terminal',8,81)
        # The native screenshot has a hovered cell. Use the original asset for
        # this unchanged empty grid, avoiding a permanent hover in the mockup.
        grid=EXTRAS.crop((79,11,133,65));im.alpha_composite(grid,(36,96))
        assert im.crop((36,96,90,150)).tobytes()==grid.tobytes()
        PROTECTED.append(dict(case=key,label='complete 3 x 3 crafting grid without transient hover',asset='assets/ae2/textures/wtlib/guis/extras.png',source=[79,11,133,65],native_source=[79,64,133,118],destination=[36,96],size=[54,54],unchanged=True))
        protected(im,row,(134,63,152,73),(91,94),'both native crafting-grid return buttons')
        protected(im,row,(138,83,162,100),(99,115),'native crafting arrow')
        protected(im,row,(169,78,195,104),(127,110),'complete result frame')
        im.alpha_composite(item_button(MAGNET),(147,146));im.alpha_composite(item_button(MAGNET),(167,146))
        parts+=toolbar(row,8)+panels(row)
        status=clean_world(crop(row,(170,-4,195,17)));parts.append((status,(170,-4)))
        mapping=[('equipment and offhand','SIU Character'),('player inventory and hotbar','SIU Inventory')]
    elif key=='own-wct-trash':
        im=frame(176,88);header(im,'Trash')
        protected(im,row,(7,17,169,71),(7,25),'all 27 native trash cells')
        parts+=toolbar(row,1)+[(item_button(TERMINAL),(-22,29))]
    elif key=='own-wct-magnet':
        im=frame(176,180);header(im,'Magnet Filter')
        protected(im,row,(7,17,169,163),(7,25),'both 27-entry filters and all five native controls')
        parts+=toolbar(row,1)+[(item_button(TERMINAL),(-22,29))]
    elif key=='pr-wireless-pattern-encoding':
        im=frame(195,179);header(im,'Terminal',True);search(im)
        protected(im,row,(7,17,169,53),(7,39),'18 catalog cells')
        protected(im,row,(174,17,188,53),(174,39),'complete catalog scrollbar')
        # Inventory's title starts on source row 138. Stop before that row,
        # after the working panel has ended, while retaining all four tabs.
        protected(im,row,(7,55,173,138),(7,83),'complete native encoding panel and both pattern cells')
        tabs=clean_world(crop(row,(173,61,195,143)));im.alpha_composite(tabs,(173,89))
        PROTECTED.append(dict(case=key,label='all four native encoding tabs, including their bottom edges',source=[173,61,195,143],destination=[173,89],size=list(tabs.size),unchanged_native_pixels=True,world_corners_masked=True))
        parts+=toolbar(row,7)+panels(row)
        parts.append((clean_world(crop(row,(170,-4,195,17))),(170,-4)))
    else:
        im=frame(195,103);header(im,'Pattern Access');search(im)
        protected(im,row,(7,17,169,71),(7,39),'full native provider list, empty in reference')
        protected(im,row,(174,17,188,71),(174,39),'full native provider-list scrollbar')
        parts+=toolbar(row,3)+panels(row)
    minx=min([0]+[xy[0] for _,xy in parts]);miny=min([0]+[xy[1] for _,xy in parts])
    maxx=max([im.width]+[xy[0]+p.width for p,xy in parts]);maxy=max([im.height]+[xy[1]+p.height for p,xy in parts])
    result=Image.new('RGBA',(maxx-minx+8,maxy-miny+8));offset=(4-minx,4-miny)
    result.alpha_composite(im,offset)
    for part,(x,y) in parts:
        result.alpha_composite(part,(offset[0]+x,offset[1]+y))
    return result,dict(window_size=list(im.size),full_envelope=list(result.size),redirected_player_slots=mapping,controls=['focus','pin','lock','link','close'])

def main():
    for p in [OUT/'original',OUT/'mockups',PDF.parent]:p.mkdir(parents=True,exist_ok=True)
    rows=sources();pages=[]
    for spec in SPECS:
        row=rows[spec['id']];b=row['native']['bounds']
        height=169 if spec['id']=='own-wct-trash' else b['height']+4
        rect=(-24,-8,b['width']+(36 if spec['id'] not in ['own-wct-trash','own-wct-magnet'] else 2),height)
        original=crop(row,rect);mock,meta=design(row)
        orig=OUT/'original'/(spec['id']+'.png');proposal=OUT/'mockups'/(spec['id']+'.png');original.save(orig);mock.save(proposal)
        pages.append({**spec,**meta,'original':str(orig.relative_to(ROOT)),'mockup':str(proposal.relative_to(ROOT)),'capture':row['native']['capture'],'capture_sha256':row['native']['sha256'],'original_crop':list(rect),'source_url':'https://github.com/Mari023/AE2WirelessTerminalLibrary/blob/1.20.1/src/main/java/de/mari_023/ae2wtlib/'+spec['source']})
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    W,H=1191,842;c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('AE2 Wireless Terminals - SIU Window Mockups');c.setAuthor("Salt's Inventory Update / Project Everything")
    def txt(s,x,y,size=11,bold=False,color='#19373D'):
        c.setFont('SegoeB' if bold else 'Segoe',size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
    def para(s,x,y,width,size=12,leading=17):
        line=''
        for word in s.split():
            trial=(line+' '+word).strip()
            if pdfmetrics.stringWidth(trial,'Segoe',size)>width:
                txt(line,x,y,size);y-=leading;line=word
            else:line=trial
        if line:txt(line,x,y,size);y-=leading
        return y
    for i,page in enumerate(pages,1):
        txt('PROJECT EVERYTHING / AE2 WIRELESS TERMINALS',34,H-32,10,True,'#247C80')
        txt(page['title'],34,H-73,28,True)
        txt('Minecraft 1.20.1 Fabric | AE2WTLib 15.2.1 | Legacy AE2 palette and existing SIU / AE2 controls',34,H-100,11,color='#5E7076')
        c.bookmarkPage(page['id']);c.addOutlineEntry(page['title'],page['id'],level=0)
        ims=[Image.open(ROOT/page[k]).convert('RGBA') for k in ['original','mockup']]
        scale=min(2.35,511/max(im.width for im in ims),412/max(im.height for im in ims))
        for x,label,im in [(34,'ORIGINAL IN GAME / SIU ABSENT',ims[0]),(610,'PROPOSED SIU WINDOW / MOCKUP',ims[1])]:
            txt(label,x,709,10,True,'#247C80');c.setFillColor(HexColor('#EEF4F4'));c.roundRect(x,251,547,442,7,fill=1,stroke=0)
            dw,dh=im.width*scale,im.height*scale
            # Native GUI pixels are rasterized with nearest-neighbor sampling.
            preview=im.resize((round(dw*2),round(dh*2)),NEAR)
            c.drawImage(ImageReader(preview),x+(547-dw)/2,251+(442-dh)/2,dw,dh,mask='auto')
        txt('WHAT IT DOES',34,225,9,True,'#247C80');left=para(page['function'],34,204,540,12,17)
        txt('WINDOW DESIGN',610,225,9,True,'#247C80');right=para(page['proposal'],610,204,540,12,17)
        assert min(left,right)>=102,(page['id'],left,right)
        para('Open: '+page['opener'],34,104,1120,10,14)
        para(page['notes'],34,77,1120,10,14)
        c.setStrokeColor(HexColor('#D7E2E3'));c.line(34,51,W-34,51)
        txt('Native screenshot + theoretical mockup | Five inventory interfaces | No implementation or gameplay claim',34,31,9,color='#5E7076')
        txt('Original screen source',830,31,9,color='#247C80');c.linkURL(page['source_url'],(830,27,933,42),relative=0,thickness=0)
        txt('Publisher release',953,31,9,color='#247C80');c.linkURL('https://modrinth.com/mod/applied-energistics-2-wireless-terminals/version/QewbGM3G',(953,27,1045,42),relative=0,thickness=0)
        txt(f'{i} / 5',W-62,31,9,color='#5E7076');c.showPage()
    c.save()
    manifest={'pdf':str(PDF.relative_to(ROOT)),'pdf_sha256':sha(PDF),'pages':5,'scope':'Five inventoried AE2WTLib 1.20.1 container screens; all right-side images are theoretical proposals. No runtime implementation changed.','native_assets':{'ae2wtlib':{'release':'15.2.1-fabric','sha256':sha(WT)},'ae2':{'release':'15.4.11-fabric','sha256':sha(AE2)}},'existing_ae2_controls_reference':str((ATLAS/'37-crafting-terminal.png').relative_to(ROOT)),'source_checked':'2026-11-01','cases':pages,'protected_artwork':PROTECTED,'native_only':['Universal Terminal radial selector','mod configuration screen'],'shared_existing_ae2':['terminal settings','crafting status/amount/confirmation and errors'],'version_policy':'Use the installed AE2 release palette and native screen documents when porting; do not reuse legacy gray textures on modern AE2 releases.'}
    (OUT/'design-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps({'pdf':str(PDF),'pages':5,'native_regions_preserved':len(PROTECTED),'cases':[p['title'] for p in pages]},indent=2))

if __name__=='__main__':main()
