"""SSN native game references and pixel-preserving SIU window proposals."""
from pathlib import Path
from collections import Counter
import hashlib,importlib.util,json,zipfile
from PIL import Image,ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader
import pdfplumber

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/ssn-window-design';TMP=ROOT/'tmp/pdfs/ssn'
PDF=ROOT/'output/pdf/siu-simple-storage-network-window-mockups.pdf'
N=Image.Resampling.NEAREST
spec=importlib.util.spec_from_file_location('siu_art',ROOT/'tools/build-enchanted-window-mockups.py')
art=importlib.util.module_from_spec(spec);spec.loader.exec_module(art)
TEAL,INK,MUTED='#247C80','#19373D','#5E7076'
BG=(198,198,198,255)
SOURCE='https://github.com/Lothrazar/Storage-Network'
RELEASE='https://modrinth.com/mod/simple-storage-network/versions'
SRC='src/main/java/com/lothrazar/storagenetwork/'

SPECS=[
 dict(id='inventory',title='Storage Inventory',rect=(4,4,174,172),slots=0,group='Block terminal',
      source='block/inventory/ScreenNetworkInventory.java',rows=8,cols=9,
      access='Use a Storage Inventory connected to a Storage Network Root and linked storage.',
      function='Browse, search, deposit and withdraw network items. The dark area is a virtual item list, not a grid of ordinary container slots. This native capture contains 27 item types.',
      design='Keep all eight visible rows and nine columns. Preserve the recessed dark list, the direction and display-order buttons, the search field and its small autofocus toggle in their native positions below the list.',
      retain='8 x 9 virtual entries / native search / display order + direction / autofocus',
      note='Remove only the player inventory below. Display-order controls are still needed for the virtual network list; they do not sort a physical chest.'),
 dict(id='request',title='Storage Request Table',rect=(4,4,174,166),slots=10,group='Block crafting terminal',
      source='block/request/ScreenNetworkTable.java',rows=4,cols=9,
      access='Use a Storage Request Table connected to the network.',
      function='Combines a searchable network list with a 3 x 3 crafting grid and one result slot. The native example crafts oak planks from an oak log.',
      design='Keep the four-row list above the toolbar and crafting section. Translate the entire working area together, retaining the clear-grid button, progress arrow and complete enlarged result border.',
      retain='4 x 9 virtual entries / 9 crafting inputs + result / native toolbar / clear grid',
      note='Keep search, recipe population, crafting pickup and inventory transfer behavior. No invented craft button or second output is added.'),
 dict(id='expanded-inventory',title='Expanded Request Table',rect=(4,4,466,449),slots=10,group='Expanded block terminal',expanded=True,
      source='block/expand/ScreenNetworkInventoryExpanded.java',rows=21,cols=25,
      access='Use an Expanded Request Table connected to the network. Lower GUI scale if needed.',
      function='The expanded block terminal exposes 21 rows by 25 columns of network entries, with the original crafting grid and controls below. It is intentionally much larger than the regular terminal.',
      design='Preserve the complete 525-entry viewport and native lower-left crafting layout. Add a single SIU header and a continuous outer frame. Keep the search and order controls where the expanded native UI puts them.',
      retain='21 x 25 virtual entries / original crafting grid + result / native lower toolbar',
      note='The reference was recaptured at GUI scale 1 to show its entire native outline. This proposal keeps the native capacity; it needs a sufficiently large logical viewport.'),
 dict(id='storage-remote',title='Storage Remote',rect=(4,4,174,172),slots=0,group='Handheld terminal',
      source='item/remote/ScreenNetworkRemote.java',rows=8,cols=9,
      access='Use the Storage Remote on the Network Root to bind it, then use it in hand.',
      function='Provides remote access to the same network inventory as the block terminal. This is a separate registered screen with item-bound network access.',
      design='Use the same eight-row window as Storage Inventory, with the correct remote title. Keep the native dark item list and all of its browsing controls; remove the lower player-inventory section.',
      retain='8 x 9 virtual entries / remote binding / native search and order controls',
      note='Binding, range and dimension rules remain the mod\'s responsibility. The mockup introduces no new remote mode or connection button.'),
 dict(id='crafting-remote',title='Crafting Remote',rect=(4,4,174,166),slots=10,group='Handheld crafting terminal',
      source='item/remote/ScreenNetworkCraftingRemote.java',rows=4,cols=9,
      access='Bind a Crafting Remote to the Network Root, then use it in hand.',
      function='Adds the same 3 x 3 crafting interface as the request table to remote network access. The capture contains a real oak-log recipe and its native result.',
      design='Match the request-table window. Keep the native four-row item list, search strip, crafting grid, small clear button, arrow and enlarged result in their original relative arrangement.',
      retain='4 x 9 virtual entries / 9 inputs + result / search / order / clear grid',
      note='The only additional chrome is the SIU title and window controls. Native crafting and connection state stay inside the same container.'),
 dict(id='expanded-remote',title='Expanded Remote',rect=(4,4,466,449),slots=10,group='Expanded handheld terminal',expanded=True,
      source='item/remote/ScreenNetworkExpandedRemote.java',rows=21,cols=25,
      access='Bind an Expanded Remote to the Network Root, then use it at a suitable GUI scale.',
      function='Provides the expanded 21 x 25 network list and the original crafting area from a handheld remote. It is a distinct screen even though its layout matches the expanded block terminal.',
      design='Share the expanded request-table design and preserve every visible entry position. Keep the native compact search and crafting controls below the list, with no extra toolbar moved into the title.',
      retain='525 visible network positions / native crafting area / complete search strip',
      note='Shown at the same logical pixel scale on both sides of this page. The entire native UI is visible; neither reference nor proposal is cropped by a game viewport.'),
 dict(id='import-cable',title='Filtered Import Cable',rect=(4,4,172,82),slots=4,group='Automation cable',
      source='block/cable/inputfilter/ScreenCableImportFilter.java',ghost=(8,45,9,2),
      access='Use a Filtered Import Cable. The example has an Operation Upgrade installed.',
      function='Moves matching items from an attached inventory into the network. Its interface combines four upgrade slots, eighteen ghost filters, priority, redstone and allow/ignore settings. The upgrade enables the comparison row.',
      design='Keep the upgrade slots on the top right, native settings on the left and both filter rows below. Reserve the operation item, comparison selector and number field without adding a second panel or changing their spacing.',
      retain='4 upgrade slots / 18 ghost filters / priority / redstone / operation controls',
      note='Ghost filters copy an item identity rather than storing the carried stack. The native import-filter action and upgrade-dependent visibility must remain intact.'),
 dict(id='export-cable',title='Export Cable',rect=(4,4,172,82),slots=4,group='Automation cable',
      source='block/cable/export/ScreenCableExportFilter.java',ghost=(8,45,9,2),
      access='Use an Export Cable. This reference also has an Operation Upgrade installed.',
      function='Sends filtered network items into an attached inventory. Its four upgrade slots and eighteen ghost entries are distinct from the player inventory below.',
      design='Keep the same dense native control layout as the import cable. Preserve its own export-specific controls and comparison row, rather than copying an import-only toggle onto it.',
      retain='4 upgrade slots / 18 ghost filters / native comparison row / priority + redstone',
      note='Keep the complete bottom and right slot edges. Filtering, operation limits and upgrades retain their native meaning and enabled states.'),
 dict(id='link-cable',title='Filtered Link Cable',rect=(4,4,172,72),slots=0,group='Storage-link filter',
      source='block/cable/linkfilter/ScreenCableFilter.java',ghost=(8,35,9,2),
      access='Use a Filtered Link Cable attached to storage.',
      function='Controls which items an attached inventory exposes to the network and its priority. The eighteen boxes are ghost filters; this screen has no real non-player item slots.',
      design='Keep the priority decrement/value/increment group on the left and the native import-filter and allow/ignore controls on the right. Retain both complete nine-entry filter rows underneath.',
      retain='18 ghost entries / priority group / native filter actions',
      note='Do not infer missing UI from the absence of real Slot objects. The filter grid is drawn and handled separately by the original mod.'),
 dict(id='collection',title='Network Collection Interface',rect=(7,31,170,71),slots=0,group='Collection filter',
      source='block/collection/ScreenCollectionFilter.java',ghost=(8,35,9,2),
      access='Use the Network Collection Interface.',
      function='Exposes the collection interface\'s eighteen ghost filter entries. The lower 36 slots belong to the player and are not part of this filter.',
      design='Place the original two-row filter grid under one full, readable SIU title. Remove the repeated native title and Inventory label, and trim the unused upper and lower margins.',
      retain='2 x 9 ghost filters / native click and count behavior / complete slot borders',
      note='Use a slightly wider header for the long name, rather than clipping it or stretching the filter slots. The grid remains centered and at native scale.'),
 dict(id='processing-cable',title='Processing Cable',rect=(4,4,172,112),slots=0,group='Newer release / automation',
      source='block/cable/processing/ScreenCableProcess.java',ghost_grids=[(8,36,3,3),(114,36,3,3)],
      access='Use a Processing Cable in the newer release (captured on 1.21.1 NeoForge).',
      function='Defines an input/output processing cycle through two 3 x 3 ghost grids. Native controls cover redstone, filter import, cycle reset, requested count, always-active mode and separate input/output faces.',
      design='Keep both grids in their original columns, including the status line above and face selectors below. Add the SIU header above the entire six-button native control strip; do not overlap or replace any of those buttons.',
      retain='9 input + 9 output ghost entries / status / 6 top controls / 2 face selectors',
      note='This screen is absent from the 1.20.1 release. The mockup preserves its current native status and all cycle-control space, including later active states.'),
 dict(id='remote-cradle',title='Network Storage Cradle',rect=(60,16,114,70),slots=9,group='Newer release / container bridge',
      source='block/cradle/ScreenCradle.java',
      access='Use a Network Storage Cradle in the newer release. The example holds a blue shulker box.',
      function='Holds nine compatible container or remote items in a 3 x 3 grid, one per slot. Native adapters decide which item capabilities can be exposed to the network.',
      design='Center the same nine-slot grid in a compact SIU window. Remove the large empty inventory margins while retaining all nine slot outlines and the item\'s native rendering.',
      retain='9 real slots / 1 item per holder / native adapter restrictions',
      note='Use ellipsis plus close when the full window-control row would crowd the title. Do not replace the holders with a view of the stored items\' contents.'),
 dict(id='network-receiver',title='Sub-Network Receiver',rect=(7,17,169,69),slots=1,group='Newer release / remote connection',
      source='block/receiver/ScreenNetworkReceiver.java',
      access='Use a Sub-Network Receiver in the newer release.',
      function='Shows receiver connection status and a single Chunkload Upgrade slot. The faint green icon is an empty-slot hint, not an installed upgrade.',
      design='Keep the native status above the centered upgrade slot, including its translucent hint. Reserve the lower line for the bound master\'s coordinates, even though this unbound example does not display them.',
      retain='1 upgrade slot / ghost hint / connection status / reserved coordinate line',
      note='Keep native binding and active/dormant feedback. The proposal leaves room for the bound state instead of cropping only around this empty capture.'),
]

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def native(row):
    p=ROOT/row['capture'];assert sha(p)==row['sha256'];im=Image.open(p).convert('RGBA');s=row['state'];b=s['bounds'];scale=im.width/s['guiWidth']
    assert scale==im.height/s['guiHeight'] and scale in (1,2)
    box=tuple(round(v*scale)for v in (b['x'],b['y'],b['x']+b['width'],b['y']+b['height']))
    assert box[0]>=0 and box[1]>=0 and box[2]<=im.width and box[3]<=im.height
    return im.crop(box),int(scale)

def make(spec,row,original,scale):
    rect=spec['rect'];body=original.crop(tuple(v*scale for v in rect));w=rect[2]-rect[0];h=rect[3]-rect[1]
    # Search-autofocus extends onto the native frame. Remove only the redundant
    # outer frame column, preserving every pixel belonging to that native widget.
    cleaned=0
    if spec['id'] in ['inventory','request','storage-remote','crafting-remote']:
        b=row['state']['bounds'];focus=next(v for v in row['state']['widgets']if v['width']==6)
        fy=focus['y']-b['y'];d=ImageDraw.Draw(body)
        for yy in range(h):
            if not fy<=yy+rect[1]<fy+focus['height']:
                d.rectangle(((173-rect[0])*scale,yy*scale,body.width-1,(yy+1)*scale-1),fill=BG);cleaned+=scale*scale
    width=max(w+14,art.tw(spec['title'])+49)
    xy=((width-w)//2,26);height=xy[1]+h+7
    im=art.frame(width,height);chrome=art.header(im,spec['title']);im=im.resize((width*scale,height*scale),N)
    im.alpha_composite(body,(xy[0]*scale,xy[1]*scale))
    slots=[s for s in row['state']['slots']if not s['player']];assert len(slots)==spec['slots'],spec['id']
    bounds=[]
    for s in slots:bounds.append((s['x']-1,s['y']-1,s['x']+17,s['y']+17))
    for gx,gy,cols,rows in ([spec['ghost']]if 'ghost'in spec else spec.get('ghost_grids',[])):
        for y in range(rows):
            for x in range(cols):bounds.append((gx+x*18-1,gy+y*18-1,gx+x*18+17,gy+y*18+17))
    for box in bounds:
        assert rect[0]<=box[0] and rect[1]<=box[1] and rect[2]>=box[2] and rect[3]>=box[3],(spec['id'],rect,box)
        src=original.crop(tuple(v*scale for v in box));dst=tuple((box[i]-rect[i%2]+xy[i%2])*scale for i in range(4))
        assert im.crop(dst).tobytes()==src.tobytes(),(spec['id'],'slot pixels')
    b=row['state']['bounds']
    for v in row['state']['widgets']:
        if not v.get('visible',True):continue
        box=(v['x']-b['x'],v['y']-b['y'],v['x']-b['x']+v['width'],v['y']-b['y']+v['height'])
        assert rect[0]<=box[0] and rect[1]<=box[1] and rect[2]>=box[2] and rect[3]>=box[3],(spec['id'],'widget',box)
        src=original.crop(tuple(v*scale for v in box));dst=tuple((box[i]-rect[i%2]+xy[i%2])*scale for i in range(4))
        assert im.crop(dst).tobytes()==src.tobytes(),(spec['id'],'widget pixels')
    player=min(s['y']for s in row['state']['slots']if s['player'])
    assert rect[3]<=player-1
    assert xy[0]>=7 and width-xy[0]-w>=7 and xy[1]>=24 and height-xy[1]-h>=7
    assert im.crop((xy[0]*scale,xy[1]*scale,(xy[0]+w)*scale,(xy[1]+h)*scale)).tobytes()==body.tobytes()
    return im,dict(source_rect=rect,body_xy=xy,logical_size=(width,height),scale=scale,chrome=chrome,
        checked_slot_outlines=len(bounds),checked_widgets=len(row['state']['widgets']),native_frame_pixels_removed=cleaned,
        body_sha256=hashlib.sha256(body.tobytes()).hexdigest())

def wrap(c,text,x,y,width,size=13,leading=19,font='Segoe',color=MUTED):
    c.setFillColor(HexColor(color));c.setFont(font,size);line=''
    for word in text.split():
        cand=(line+' '+word).strip()
        if pdfmetrics.stringWidth(cand,font,size)>width and line:c.drawString(x,y,line);y-=leading;line=word
        else:line=cand
    if line:c.drawString(x,y,line);y-=leading
    return y
def label(c,t,x,y,size=10):
    c.setFont('SegoeB',size);c.setFillColor(HexColor(TEAL));c.drawString(x,y,t)
def footer(c,W,H,page,extra='Native screenshot + proposed mockup / design only'):
    c.setStrokeColor(HexColor('#D8E1E3'));c.line(36,39,W-36,39)
    c.setFont('Segoe',9);c.setFillColor(HexColor(MUTED));c.drawString(36,23,extra);c.drawRightString(W-36,23,str(page))
def image(c,im,x,y,w,h):
    c.drawImage(ImageReader(im),x,y,width=w,height=h,mask='auto')
def page(c,W,H):
    c.setPageSize((W,H));c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)

def build_pdf(pages):
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    c=canvas.Canvas(str(PDF),pagesize=(1191,842),pageCompression=1);c.setTitle('Simple Storage Network - SIU window design review');c.setAuthor('Salt\'s Inventory Update')
    page(c,1191,842);label(c,'PROJECT EVERYTHING / WINDOW DESIGN REVIEW',36,803)
    c.setFont('SegoeB',36);c.setFillColor(HexColor(INK));c.drawString(36,746,'Simple Storage Network')
    wrap(c,'13 container designs, based on actual native game captures.',36,710,1100,20,28,color=INK)
    wrap(c,'Left: the complete original menu with SIU absent. Right: a proposed integrated SIU window. Each comparison uses the same logical pixel scale on both sides.',36,655,1080,16,24)
    columns=[(36,'BROWSE + CRAFT',SPECS[:6]),(415,'FILTER + AUTOMATE',SPECS[6:10]),(794,'NEWER CONTAINERS',SPECS[10:])]
    for x,title,rows in columns:
        c.setFillColor(HexColor('#EFF5F5'));c.roundRect(x,300,361,286,12,fill=1,stroke=0);label(c,title,x+18,556)
        yy=522
        for row in rows:
            c.setFont('Segoe',14);c.setFillColor(HexColor(INK));c.drawString(x+18,yy,row['title']);yy-=31
        if x==794:wrap(c,'These three are not in the 1.20.1 release. References use 1.21.1 NeoForge.',x+18,390,325,12,18)
    label(c,'DESIGN RULES',36,263)
    wrap(c,'One continuous SIU frame. Native #C6C6C6 surface. Native slot and widget artwork at unchanged scale. Keep the mod\'s working layout; separate the player inventory. Use normal window controls when they fit, otherwise ellipsis + close.',36,233,1080,15,23,color=INK)
    wrap(c,'Capture baseline: publisher release 1.13.2 on Minecraft 1.20.1 Forge and 1.21.1 NeoForge. Expanded screens were recaptured at GUI scale 1 so their original top and bottom are visible. Non-container features are accounted for in the appendix.',36,149,1080,12,18)
    c.linkURL(RELEASE,(36,64,455,87));label(c,'Publisher releases and scope notes in the appendix',36,70)
    footer(c,1191,842,1,'13 native screen types / proposals only / no compatibility code changed');c.showPage()
    for index,(s,r,original,mock,meta)in enumerate(pages,2):
        W,H=(1684,1191)if s.get('expanded')else(1191,842);page(c,W,H)
        label(c,'PROJECT EVERYTHING / SIMPLE STORAGE NETWORK',36,H-34)
        c.setFont('SegoeB',28);c.setFillColor(HexColor(INK));c.drawString(36,H-77,s['title'])
        sub=f"{r['minecraft']} / {r['loader'].title()} / SSN 1.13.2 / {s['group']}"
        wrap(c,sub,36,H-104,W-72,12,18)
        panel_y=257;panel_h=H-417;gap=35;pw=(W-72-gap)/2;xs=[36,36+pw+gap]
        label(c,'ORIGINAL IN GAME / SIU ABSENT',xs[0],H-145);label(c,'PROPOSED SIU WINDOW / MOCKUP',xs[1],H-145)
        for x in xs:c.setFillColor(HexColor('#EFF5F5'));c.roundRect(x,panel_y,pw,panel_h,10,fill=1,stroke=0)
        # Identical logical pixel size for both columns, including the native
        # expanded capture's 1x scale versus ordinary screenshots' 2x scale.
        source_scale=meta['scale'];ow,oh=original.width/source_scale,original.height/source_scale;mw,mh=meta['logical_size']
        factor=min(2.5 if not s.get('expanded') else 1.45,(pw-36)/max(ow,mw),(panel_h-38)/max(oh,mh))
        for x,im,lw,lh in [(xs[0],original,ow,oh),(xs[1],mock,mw,mh)]:image(c,im,x+(pw-lw*factor)/2,panel_y+(panel_h-lh*factor)/2,lw*factor,lh*factor)
        label(c,'WHAT IT DOES',xs[0],229);label(c,'WINDOW DESIGN',xs[1],229)
        y1=wrap(c,s['function'],xs[0],207,pw,12.5,18);y2=wrap(c,s['design'],xs[1],207,pw,12.5,18)
        assert min(y1,y2)>113,(s['id'],y1,y2)
        wrap(c,s['note'],36,111,W-72,11,16)
        url=SOURCE+'/blob/trunk/'+r['minecraft']+'/'+SRC+s['source']
        c.linkURL(url,(36,47,W-36,66));c.setFont('Segoe',9);c.setFillColor(HexColor(MUTED));c.drawString(36,54,'Native source: '+s['source']+' / original artwork and controls retained')
        footer(c,W,H,index);c.showPage()
    page(c,1191,842);label(c,'APPENDIX / SCOPE + PRESERVATION',36,803)
    c.setFont('SegoeB',28);c.setFillColor(HexColor(INK));c.drawString(36,760,'Everything else stays native')
    wrap(c,'No new window is proposed for a guidebook, world interaction, tooltip or keybind-only action.',36,727,1100,15,22,color=INK)
    entries=[
      ('Network Guide Book','Native Patchouli guidebook when Patchouli is installed. Keep its full-screen pages, navigation and links.'),
      ('Builder + Picker Remotes','World tools, not container screens. Keep native block selection, placement and pickup behavior.'),
      ('Collector Remote + keybinds','Background pickup routing and an enable/disable keybind. Keep native feedback and remote-opening bindings.'),
      ('Root, basic cables + Exchange Interface','Connection blocks and world interactions. Root connection feedback is chat; these have no separate registered container screens.'),
      ('Newer Network Drawer','In-world insert/extract interaction and item rendering. No separate container screen; do not invent a window.'),
      ('Conditional controls + overlays','Keep ghost-item tooltips, keyboard search, scroll behavior and upgrade-dependent comparison controls. JEI search sync appears only when JEI is installed; these reference clients do not contain JEI.'),
    ]
    y=677
    for title,desc in entries:
        c.setFillColor(HexColor('#EFF5F5'));c.roundRect(36,y-62,1119,65,8,fill=1,stroke=0);label(c,title.upper(),49,y-16)
        wrap(c,desc,49,y-38,1085,12,17);y-=77
    label(c,'COVERAGE + REVIEW LIMITS',36,185)
    wrap(c,'Exact publisher JARs contain 10 concrete screens on 1.20.1 and 13 on 1.21.1 and 26.1.2. This PDF shows one actual native reference and one proposal for each of the 13 types. The three newer-only screens are labeled. It does not claim a capture of every version or loader. Original cable screens retain their native incomplete lower rim; proposals have a complete SIU frame.',36,161,1110,11.5,17)
    wrap(c,'Checked: native screen identity, complete screenshot bounds, all real/ghost slot outlines and native widget rectangles inside each proposal, plus visual review of every PDF page. These are design and capture checks, not a gameplay or compatibility implementation test.',36,102,1110,11,16)
    c.linkURL(SOURCE,(36,44,470,63));c.linkURL(RELEASE,(615,44,1155,63));label(c,'Source: Lothrazar/Storage-Network',36,51,9);label(c,'Publisher: Simple Storage Network / Modrinth',615,51,9)
    footer(c,1191,842,15,'Native reference coverage / version-specific containers and exclusions');c.showPage();c.save()

def main():
    OUT.mkdir(parents=True,exist_ok=True);(OUT/'original').mkdir(exist_ok=True);(OUT/'mockups').mkdir(exist_ok=True);PDF.parent.mkdir(parents=True,exist_ok=True)
    refs={r['id']:r for v in ['1.20.1','1.21.1'] for r in json.loads((OUT/f'native-references-{v}.json').read_text())['cases']}
    assert len(refs)==len(SPECS)==13
    pages=[];rows=[]
    for s in SPECS:
        r=refs[s['id']];original,scale=native(r);mock,meta=make(s,r,original,scale)
        op=OUT/'original'/(s['id']+'.png');mp=OUT/'mockups'/(s['id']+'.png');original.save(op);mock.save(mp)
        pages.append((s,r,original,mock,meta));rows.append(dict(**s,native=r,geometry=meta,original=str(op.relative_to(ROOT)),mockup=str(mp.relative_to(ROOT))))
    build_pdf(pages)
    reader=PdfReader(PDF);assert len(reader.pages)==15
    text='\n'.join(p.extract_text()for p in reader.pages)
    for s in SPECS:assert s['title'] in text
    bounds=[]
    with pdfplumber.open(PDF)as pdf:
        for i,p in enumerate(pdf.pages,1):
            bad=[(x['text'],x['x0'],x['top'])for x in p.chars if x['x0']<25 or x['x1']>p.width-25 or x['top']<15 or x['bottom']>p.height-12]
            assert not bad,(i,bad[:8]);bounds.append(dict(page=i,width=p.width,height=p.height,chars=len(p.chars)))
    screen_inventory={}
    for v,name in [('1.20.1','SimpleStorageNetwork-1.20.1-1.13.2.jar'),('1.21.1','storagenetwork-1.21.1-1.13.2.jar'),('26.1.2','storagenetwork-26.1.2-1.13.2.jar')]:
        with zipfile.ZipFile(TMP/name)as jar:screen_inventory[v]=[n for n in jar.namelist()if '/Screen' in n and n.endswith('.class')]
    manifest=dict(pdf=str(PDF.relative_to(ROOT)),sha256=sha(PDF),screen_types=13,pages=15,scope='Native references plus theoretical SIU proposals; shipping source and JARs unchanged.',cases=rows,publisher_screen_inventory=screen_inventory)
    (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2));(OUT/'geometry-review.json').write_text(json.dumps(dict(status='pass',pages=bounds,slot_outlines=sum(r['geometry']['checked_slot_outlines']for r in rows),widgets=sum(r['geometry']['checked_widgets']for r in rows),visual_review='pending'),indent=2))
    print(json.dumps(dict(pdf=str(PDF),pages=len(reader.pages),types=len(rows),slot_outlines=sum(r['geometry']['checked_slot_outlines']for r in rows)),indent=2))
if __name__=='__main__':main()
