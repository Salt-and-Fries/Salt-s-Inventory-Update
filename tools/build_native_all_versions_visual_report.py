"""Screenshot-led report. Keeps raw pixels, version labels and capture lineage."""
from pathlib import Path
import hashlib,json,math,shutil
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor,white
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'build/native-all-targets/gameplay'
OUT=ROOT/'output/pdf/siu-all-versions-window-visual-report.pdf'
TMP=ROOT/'tmp/pdfs/native-all-versions'
TMP.mkdir(parents=True,exist_ok=True);OUT.parent.mkdir(parents=True,exist_ok=True)
pdfmetrics.registerFont(TTFont('UI','C:/Windows/Fonts/segoeui.ttf'))
pdfmetrics.registerFont(TTFont('UIB','C:/Windows/Fonts/segoeuib.ttf'))
W,H=960,720
INK=HexColor('#16353e');MUTED=HexColor('#526a72');TEAL=HexColor('#087f88')
PALE=HexColor('#f0f6f6');LINE=HexColor('#d2e1e3');AMBER=HexColor('#966326')
c=canvas.Canvas(str(OUT),pagesize=(W,H),pageCompression=1)
c.setTitle("Salt's Inventory Update - All-version window visual report")
c.setAuthor("Salt's Inventory Update")
figures=[];pages=[];page_no=0
cache={}

def text(x,y,value,size=11,color=INK,bold=False):
    c.setFillColor(color);c.setFont('UIB' if bold else 'UI',size);c.drawString(x,y,str(value))

def lines(x,y,value,width,size=10,color=MUTED,leading=None):
    leading=leading or size*1.4
    for paragraph in value.split('\n'):
        row=''
        for word in paragraph.split():
            trial=(row+' '+word).strip()
            if pdfmetrics.stringWidth(trial,'UI',size)>width and row:
                text(x,y,row,size,color);y-=leading;row=word
            else:row=trial
        text(x,y,row,size,color);y-=leading
    return y

def page(title,sub='',section='VISUAL VALIDATION'):
    global page_no
    if page_no:c.showPage()
    page_no+=1;pages.append({'page':page_no,'title':title})
    c.setFillColor(white);c.rect(0,0,W,H,fill=1,stroke=0)
    text(34,H-29,section,9,TEAL,True)
    text(34,H-65,title,27,bold=True)
    text(35,H-87,sub,10,MUTED)
    c.setStrokeColor(LINE);c.line(34,39,W-34,39)
    text(34,23,'SIU native windows | Core 0.1.6 / Compatibility 0.1.3 | 26 October 2026',8,MUTED)
    c.setFont('UI',8);c.drawRightString(W-34,23,f'{page_no:02d}')

def observation(folder,capture):
    if folder not in cache:cache[folder]=json.loads((folder/'observations.json').read_text(encoding='utf-8-sig'))
    return next(row for row in cache[folder] if row.get('command',{}).get('capture')==capture)

def shot(folder,capture,mode='window',top=0,bottom=0,left=0,right=0,box=None,history=False):
    folder=ROOT/folder if not isinstance(folder,Path) else folder
    p=folder/'screenshots'/f'{capture}.png';assert p.exists(),p
    im=Image.open(p).convert('RGB');r=observation(folder,capture)
    if box is None and mode!='full':
        client=r['client'];windows=client.get('windows',[])
        b=windows[-1]['bounds'] if windows else client.get('window')
        assert b,(folder,capture)
        sx=im.width/client['guiWidth'];sy=im.height/client['guiHeight']
        box=(math.floor((b['x']-left)*sx)-5,math.floor((b['y']-top)*sy)-5,
             math.ceil((b['x']+b['width']+right)*sx)+5,math.ceil((b['y']+b['height']+bottom)*sy)+5)
    box=box or (0,0,im.width,im.height)
    box=(max(0,box[0]),max(0,box[1]),min(im.width,box[2]),min(im.height,box[3]))
    assert box[2]>box[0] and box[3]>box[1],box
    cropped=im.crop(box)
    return {'image':cropped,'source':str(p.relative_to(ROOT)),'crop':list(box),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),
            'history':history,'run':folder.name,'capture':capture,'sequence':r.get('sequence')}

def legacy(name):
    manifest=json.loads((ROOT/'output/pdf/siu-six-mods-approved-ui-1.20.1.manifest.json').read_text())
    f=next(f for f in manifest['figures'] if Path(f['file']).stem==name)
    p=Path(f['file']);im=Image.open(p).convert('RGB').crop(f['crop'])
    return {'image':im,'source':str(p.relative_to(ROOT)),'crop':f['crop'],'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),
            'history':True,'run':p.parent.parent.name,'capture':name,'sequence':f['capture']}

def card(x,y,w,h,title,image,caption='',tag=None,max_scale=2):
    # y is the bottom of the card. Every label lies outside the original screenshot.
    c.setFillColor(PALE);c.setStrokeColor(LINE);c.roundRect(x,y,w,h,8,fill=1,stroke=1)
    text(x+13,y+h-24,title,12,bold=True)
    if tag:text(x+13,y+h-41,tag,9,AMBER if image.get('history') else TEAL)
    top=y+h-(53 if tag else 40);bottom=y+(37 if caption else 12)
    im=image['image'];scale=min((w-24)/im.width,(top-bottom)/im.height,max_scale)
    iw,ih=im.width*scale,im.height*scale
    px=x+(w-iw)/2;py=bottom+(top-bottom-ih)/2
    c.drawImage(ImageReader(im),px,py,iw,ih,mask='auto')
    if caption:lines(x+13,y+24,caption,w-26,9,MUTED,12)
    figures.append({**{k:v for k,v in image.items() if k!='image'},'page':page_no,'title':title,'pdfRect':[px,py,iw,ih]})

def grid(cards,cols=2,top=608,bottom=58,gap=14):
    rows=math.ceil(len(cards)/cols);cw=(W-68-gap*(cols-1))/cols;ch=(top-bottom-gap*(rows-1))/rows
    for i,item in enumerate(cards):
        x=34+(i%cols)*(cw+gap);y=top-(i//cols+1)*ch-(i//cols)*gap
        title,image,*extras=item;caption=extras[0] if extras else '';tag=extras[1] if len(extras)>1 else None
        card(x,y,cw,ch,title,image,caption,tag)

targets={
 '1.20.1':{'fabric':'port-final-02','forge':'port-final-02'},
 '1.21.1':{'fabric':'port-fabric-06','neoforge':'port-neo-04'},
 '1.21.11':{'fabric':'port-final-01','neoforge':'port-final-01'},
 '26.1.2':{'fabric':'port-final-01','neoforge':'port-final-01'},
 '26.2':{'fabric':'port-final-01','neoforge':'port-final-01'},
 '26.3':{'fabric':'background-final-01','neoforge':'port-final-01'}}

page('Windows across every supported version',
     'Six Minecraft versions | Twelve loader targets | Real game captures','CORE 0.1.6 - PROJECT EVERYTHING')
text(35,590,'What this report shows',17,bold=True)
lines(35,565,'The main pages pair Fabric with Forge or NeoForge. Each shows the native Nether Chest and the compact Feeding Trough window from a successful focused run.',418,12)
text(35,480,'Minecraft',12,bold=True);text(185,480,'Loaders shown',12,bold=True);text(370,480,'Page',12,bold=True)
for i,(mc,loaders) in enumerate(targets.items()):
    y=451-i*38;c.setStrokeColor(LINE);c.line(34,y-12,442,y-12)
    text(35,y,mc,13,bold=True);text(185,y,'Fabric + '+('Forge' if mc=='1.20.1' else 'NeoForge'),11);text(370,y,str(i+2),11,TEAL,True)
lines(35,188,'Extra pages show distinct machines, backpacks, window boundaries, standalone Core and public-API templates. Historical 1.20.1 references are explicitly marked.',410,11)
hero=shot(BASE/'26.3-fabric-background-final-01','nether-transferred')
card(480,261,445,348,'Compatibility window',hero,'512-item stack after transfer','26.3 Fabric | Core 0.1.6 + Compatibility 0.1.3')
lines(493,220,'Screenshot crops preserve the original pixels. No UI has been repainted or reconstructed for this report.',415,11)
lines(493,151,'Evidence is focused, not exhaustive. These images do not certify every menu, integration combination, recipe or multiplayer scenario.',415,11)

for mc,loaders in targets.items():
    page(f'Minecraft {mc}','Nether Chested and Animal Feeding Trough | Focused window and inventory checks passed')
    cw=439
    for col,(loader,run) in enumerate(loaders.items()):
        folder=BASE/f'{mc}-{loader}-{run}';x=34+col*453
        label={'fabric':'Fabric','forge':'Forge','neoforge':'NeoForge'}[loader]
        card(x,269,cw,340,f'{label} - Nether Chest',shot(folder,'nether-transferred'),
             'Native menu, standard cells, 512-item pickup and placement.',f'{mc} | Core 0.1.6 + Compatibility 0.1.3')
        card(x,104,cw,151,f'{label} - Feeding Trough',shot(folder,'feeding-trough'),
             'Compact API window; wheat transfer and source-removal cleanup.')
    text(35,73,'Also checked in both runs: dragging to all four inner walls; the entire window stayed on screen.',10,MUTED)
    text(35,56,'Capture lineage and final-renderer coverage are explained on the last page.',9,MUTED)

fab=BASE/'1.21.1-fabric-port-fabric-05'
page('1.21.1 Fabric - distinct native layouts','Core 0.1.6 + Compatibility 0.1.3 | Native artwork, controls and inventory slots')
grid([
 ('Oxidized - Copper Kiln',shot(fab,'oxidized-active'),'Processing artwork and real output retained.'),
 ('Copper Kiln - recipe book open',shot(fab,'oxidized-recipe-open',left=185,bottom=86),'The recipe companion remains separate from the machine.'),
 ('Brick & Mortar - preview expanded',shot(fab,'brick-kiln-open',top=48),'Animated preview sits outside the top border.'),
 ('Inmis - Baby Backpack',shot(BASE/'1.21.1-fabric-port-fabric-06','inmis-baby'),'Compact three-slot layout; sorting verified in the run.')])

neo=BASE/'1.21.1-neoforge-port-neo-04'
page('1.21.1 NeoForge - distinct native layouts','Core 0.1.6 + Compatibility 0.1.3 | Real native block-use and opening data')
card(34,59,439,550,'Expanded Delight - Juicer',shot(neo,'juicer-transfer'),
     'Native recipe control, press artwork and all five slots. Apple pickup and return verified.')
card(487,340,439,269,"Crabber's Delight - Crab Trap",shot(neo,'crab-trap'),
     'Compact bait/catch arrangement; correct current menu selected.')
card(487,59,439,267,'Brick & Mortar - Kiln',shot(neo,'brick-kiln',top=12),
     'Closed preview tab remains outside the frame.')

page('All four inner walls','26.3 | Full viewport captures preserve the screen edges')
bounds_cards=[]
for loader in ['fabric','neoforge']:
    folder=BASE/f'26.3-{loader}-{targets["26.3"][loader]}'
    for capture,label in [('nether-top-left','Top + left'),('nether-bottom-right','Bottom + right')]:
        b=observation(folder,capture)['client']['windows'][-1]['bounds']
        bounds_cards.append((f'{loader.title()} - {label}',shot(folder,capture,mode='full'),
                             f'GUI bounds: x {b["x"]}, y {b["y"]}, {b["width"]} x {b["height"]}.'))
grid(bounds_cards)

page('Standalone Core and a native API consumer','Native textures remain visible when standard Compatibility slot backgrounds are not used')
grid([
 ('26.3 Fabric - Core only',shot(BASE/'26.3-fabric-core-artwork-final-02','native-transferred'),
  'Original dark native cells; 512-item transfer verified.','Core 0.1.6 | Compatibility absent'),
 ('26.3 Fabric - local native profile',shot(BASE/'26.3-fabric-core-artwork-final-02','native-profile-background'),
  'Retained source region rendered through a reloaded profile.','Core 0.1.6 | Compatibility absent'),
 ('1.21.11 NeoForge - public API template',shot(BASE/'1.21.11-neoforge-template-native-final-02','native-retained-transferred'),
  'Ordinary API consumer enables original native slot backgrounds.','Core 0.1.6 + Compatibility 0.1.3'),
 ('1.21.11 NeoForge - developer inspector',shot(BASE/'1.21.11-neoforge-background-final-01','core-native-template',bottom=105,right=40),
  'Cyan marks the native slot bounds; editor controls are an overlay.','Core 0.1.6 | Compatibility absent')])

page('Safe fallback without the extension','1.21.1 Fabric | Core 0.1.6 with Compatibility absent')
fallback=shot(BASE/'1.21.1-fabric-core-only-final-01','native-fallback',box=(452,210,828,690))
card(34,59,439,550,'Original Nether Chested screen',fallback,
     'This release needs its optional synchronization bridge for desktop hosting.')
text(505,548,'What was observed',18,bold=True)
lines(505,516,'Core declined the unsupported automatic desktop session and opened the original native interface.\n\nThe client stayed connected and no empty or stale desktop window remained.',390,13,INK,19)
text(505,325,'Why this matters',18,bold=True)
lines(505,294,'Core can work independently. A container that cannot be hosted safely still has a usable original screen.',390,13,INK,19)
lines(505,165,'The later Core-only 26.3 capture and public API template on the previous page also verify the corrected separate native-background pass.',390,11)

page('1.20.1 - approved machine designs','Historical layout reference | Core 0.1.5 + Compatibility 0.1.3','DESIGN REFERENCE - NOT A NEW 0.1.6 CAPTURE')
grid([
 ('Fabric - Oxidized Copper Kiln',legacy('fabric-oxidized-active'),'Tight frame around the complete machine.','Approved 1.20.1 design reference'),
 ('Fabric - Expanded Delight Juicer',legacy('fabric-juicer-active'),'Legacy Juicer has a smaller native design.','Approved 1.20.1 design reference'),
 ('Fabric - Brick & Mortar preview',legacy('fabric-kiln-open'),'The preview is outside the frame.','Approved 1.20.1 design reference'),
 ("Forge - Crabber's Delight",legacy('forge-crab-trap'),'Bait slot plus the full catch row.','Approved 1.20.1 design reference')])

page('1.20.1 - Inmis tier shapes','Historical layout reference | Fabric | Every default backpack shape plus Ender Pouch','DESIGN REFERENCE - NOT A NEW 0.1.6 CAPTURE')
tiers=['baby','frayed','plated','gilded','bejeweled','blazing','withered','endless']
cards=[(tier.title(),legacy('fabric-inmis-'+tier),'','Core 0.1.5 + Compatibility 0.1.3') for tier in tiers]
cards.append(('Ender Pouch',legacy('fabric-ender-pouch'),'','Core 0.1.5 + Compatibility 0.1.3'))
grid(cards,cols=3,gap=10)

page('1.20.1 - Iron Chests: Restocked','Historical functional captures | Core 0.1.5 + Compatibility 0.1.3','DESIGN REFERENCE - NOT A NEW 0.1.6 CAPTURE')
iron=ROOT/'build/gameplay-qa/packaged-native'
irf=iron/'1.20.1-fabric-iron-chests-fabric-r1';irn=iron/'1.20.1-forge-iron-chests-forge-r1'
grid([
 ('Fabric - Dirt Chest',shot(irf,'fabric-dirt_chest-sorted',history=True),'Small restricted storage uses a compact body.','Earlier focused integration validation'),
 ('Forge - Dirt Chest',shot(irn,'forge-dirt_chest-sorted',history=True),'The same compact layout on the other supported loader.','Earlier focused integration validation'),
 ('Fabric - scrolled large storage',shot(irf,'fabric-netherite-scrolled-last-slot-transfer',history=True),'Final row reached; last-slot transfer verified.','Earlier focused integration validation'),
 ('Forge - scrolled large storage',shot(irn,'forge-netherite-scrolled-last-slot-transfer',history=True),'Final row reached; last-slot transfer verified.','Earlier focused integration validation')])

page('Coverage and reading this report','Visual evidence for the Project Everything container port')
text(35,578,'Main pages: observed Core 0.1.6 windows',18,bold=True)
lines(35,548,'All twelve loader targets have successful focused captures. The baseline exercised native Nether Chested opening, a 512-item pickup/placement, all four movement walls, API Feeding Trough opening, wheat pickup/return and source-removal closure.',418,12,INK,18)
text(35,419,'Final background correction',18,bold=True)
lines(35,389,'The correction was exercised separately on 26.3 Fabric (Core-only automatic window and retained profile), 1.21.11 Fabric/NeoForge (Core-only windows), and a 1.21.11 NeoForge public API template. The 26.3 Fabric Compatibility baseline was repeated afterward.',418,12,INK,18)
lines(35,259,'Some earlier matrix captures precede that last shared-renderer correction. They show their recorded tested state; they are not a claim that every final JAR was individually replayed after the fix.',418,11)
text(496,578,'Build and packaging',18,bold=True)
lines(496,548,'Root build passed. All twelve Core and Compatibility targets compile; the public native-template examples and native-symbol checks pass across the matrix.\n\nThe release contains 36 production JARs: Core 0.1.6, Compatibility 0.1.3 and the dependency-rebuilt Controlify 0.1.1.',430,12,INK,18)
text(496,359,'Limits',18,bold=True)
lines(496,329,'This is a screenshot report of focused checks, not a full functional certification. Dedicated remote multiplayer, physical controllers, every recipe/configuration and all integration combinations were not tested in this pass.\n\nThe final three reference pages use older approved 1.20.1 captures, marked with their actual Core 0.1.5 version.',430,12,INK,18)
text(35,137,'Evidence files',13,bold=True)
lines(35,115,'Capture paths, crop rectangles and SHA-256 values are saved in the companion manifest. Detailed coverage is in docs/markdown/native-containers-all-targets.md; artifact/runtime lineage is in output/native-container-port/validation.json.',888,10)
c.save()
manifest={'pdf':str(OUT.relative_to(ROOT)),'pages':pages,'figures':figures,'mockups':False,'historicalFiguresExplicitlyLabeled':True,
          'sourceReport':'docs/markdown/native-containers-all-targets.md','artifactValidation':'output/native-container-port/validation.json'}
OUT.with_suffix('.manifest.json').write_text(json.dumps(manifest,indent=2))
print(f'Created {OUT}: {page_no} pages, {len(figures)} real screenshot figures.')
