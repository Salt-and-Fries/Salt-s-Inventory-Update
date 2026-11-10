"""Tech Reborn 5.8.14: immutable native captures and code-authored SIU proposals."""
from pathlib import Path
from io import BytesIO
import hashlib, importlib.util, json, re, zipfile
from collections import Counter
from PIL import Image, ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'output/tech-reborn-window-design'
PDF = ROOT/'output/pdf/siu-tech-reborn-window-mockups.pdf'
SOURCE = ROOT/'build/tech-reborn-window-design/source'
COMMIT = '380a06253bea70924a2470454875cbea99682a0a'
SOURCE_URL = 'https://github.com/TechReborn/TechReborn/tree/'+COMMIT
CORE = ROOT/'build/ui-modpack-audit-20261025/jars/37ecbb813d15e5bd347da2c39227c194673e1948.jar'
JAR = ROOT/'build/ui-modpack-audit-20261025/jars/3a7f907b5da70e3fc114493c17ee7518042cf14b.jar'
BG = (198,198,198,255)
N = Image.Resampling.NEAREST
spec = importlib.util.spec_from_file_location('art',ROOT/'tools/build-enchanted-window-mockups.py')
ART = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ART)

# Every description is tied to the pinned native screen, not a proposed new action.
DESCRIPTIONS = {
 'GuiAlloyFurnace': 'Combines ingredients with solid fuel. Keep both upper inputs, the fuel well, heat animation, progress arrow and large output.',
 'GuiAlloySmelter': 'Combines two ingredients into a central result using machine energy. Keep the opposing arrows, energy gauge and charging well.',
 'GuiAssemblingMachine': 'Processes a pair of ingredients into a result. Preserve the vertical input pair, horizontal progress arrow and larger output.',
 'GuiAutoCrafting': 'Automates a stored crafting recipe. Preserve the 3 x 3 grid, result preview, output wells, progress, energy gauge and native recipe lock.',
 'GuiBlastFurnace': 'Processes a multiblock furnace recipe with heat. Keep both inputs, the two-result bar, full heat readout and hologram toggle.',
 'GuiBlockBreaker': 'Breaks blocks in front of the machine. Preserve the output, energy display and the full blue target/status text area.',
 'GuiBlockPlacer': 'Places blocks from its input. Preserve the input, energy display and blue target/status text, including stopped and empty states.',
 'GuiCentrifuge': 'Separates ingredients into several products. Keep the vertical inputs, four-result arrangement, progress arrow and energy display.',
 'GuiChemicalReactor': 'Combines materials in a chemical recipe. Keep both inputs around the central output, both arrows and charging well.',
 'GuiChunkLoader': 'Controls loaded chunk radius. Retain the radius readout, all four adjustment arrows and Show Loaded Chunks, with the upgrade tray.',
 'GuiCompressor': 'Compresses a single input into a result. Keep the input, full progress arrow, large output, energy gauge and charging well.',
 'GuiDieselGenerator': 'Generates energy from accepted fuel fluid. Retain both container slots, the complete fluid tank, conversion arrow and energy gauge.',
 'GuiDistillationTower': 'Runs multiblock distillation recipes. Keep every ingredient/result well, progress display, charging well and hologram control.',
 'GuiElectricFurnace': 'Smelts with machine energy. Keep the original input/output spacing, progress arrow, full gauge and charging well.',
 'GuiElevator': 'Displays the elevator energy buffer. This screen has no machine item slots; keep the complete gauge and redstone settings without inventing a grid.',
 'GuiExtractor': 'Extracts a product from an ingredient. Keep the input, arrow, large output, complete energy gauge and charging well.',
 'GuiFishingStation': 'Collects fishing results in its six-slot group. Keep every result well, charging slot and energy gauge.',
 'GuiFluidReplicator': 'Runs a fluid-producing multiblock process. Preserve the item wells, full tank, energy gauge, progress and hologram control.',
 'GuiGasTurbine': 'Converts accepted gas into energy. Keep the two container wells, full fluid display, arrow and stored-energy gauge.',
 'GuiGenerator': 'Burns solid fuel for energy. Keep the fuel well, heat animation and the full-height energy buffer display.',
 'GuiGreenhouseController': 'Controls greenhouse processing. Keep all six work slots, charging well, energy gauge, hologram and upgrade/status icon area.',
 'GuiGrinder': 'Grinds an ingredient into a result. Preserve the input, arrow, larger output, charging well and energy gauge.',
 'GuiMFSU': 'Stores high-voltage energy and charges equipment. Keep both transfer wells, the energy readout and all four actual player armor slots.',
 'GuiImplosionCompressor': 'Runs multiblock compression recipes. Keep all process slots, progress, energy display, charging well and hologram control.',
 'GuiIndustrialElectrolyzer': 'Separates ingredients into a four-result bar. Keep the entire top result frame, upward arrow, both lower inputs and charging well.',
 'GuiIndustrialGrinder': 'Runs multiblock grinding with fluid. Keep both fluid-container wells, material input, four outputs, tank, energy and hologram.',
 'GuiIronFurnace': 'Smelts with solid fuel. Preserve the input, fuel well, heat, progress, large output and native experience-collection button.',
 'GuiLaunchpad': 'Controls launch speed and exposes stored energy. Keep the speed label, all four adjustment arrows and the energy gauge.',
 'GuiBatbox': 'Stores low-voltage energy. Preserve the two transfer wells around the energy gauge, plus the live stored/capacity readout.',
 'GuiMatterFabricator': 'Fabricates matter using energy and its ingredient buffer. Preserve all six upper wells, the down arrow and full lower output bar.',
 'GuiMFE': 'Stores medium-voltage energy and charges equipment. Preserve the transfer wells, energy readout and all four player armor slots.',
 'GuiPlasmaGenerator': 'Converts accepted plasma fluid into energy. Keep both container slots, full tank, conversion arrow and the energy gauge.',
 'GuiPlayerDetector': 'Adjusts the player-detection radius. Preserve the radius text, four adjustment arrows, energy gauge and redstone options.',
 'GuiPump': 'Controls pumping depth and range. Keep both four-arrow rows, the exhausted/status line, full tank, energy gauge and charging well.',
 'GuiRecycler': 'Processes material into recycling products. Preserve input/output relationships, progress, charging well and the energy gauge.',
 'GuiRollingMachine': 'Processes a 3 x 3 recipe. Preserve all nine inputs, arrow, large output, charging well, energy gauge and native recipe lock.',
 'GuiIndustrialSawmill': 'Runs multiblock cutting recipes with fluid. Preserve container wells, tank, ingredient and result slots, energy and hologram.',
 'GuiScrapboxinator': 'Processes scrap boxes into results. Keep its single input, progress arrow, large output and energy buffer.',
 'GuiSemifluidGenerator': 'Converts accepted semi-fluid fuel into energy. Preserve the complete tank, both container wells, arrow and energy gauge.',
 'GuiSolar': 'Reports solar generation and stored energy. Keep the complete generation/blocked-text area and the full right energy gauge; no item grid is added.',
 'GuiSolidCanningMachine': 'Combines materials with containers. Preserve both inputs, the opposing arrows, central output and charging well.',
 'GuiStorageUnit': 'Stores a large quantity of one item. Preserve IN/OUT wells, native locking, stored item, amount, capacity use and wrench-tip space.',
 'GuiTankUnit': 'Stores a fluid and handles containers. Preserve IN/OUT wells and reserve the full area used for fluid name, amount and capacity text.',
 'GuiThermalGenerator': 'Converts hot fluid into energy. Keep both container wells, the complete tank, conversion arrow and stored-energy gauge.',
 'GuiVacuumFreezer': 'Runs multiblock cooling recipes. Preserve the process wells, progress arrow, charging well, energy gauge and hologram.',
 'GuiWireMill': 'Processes an ingredient into wire. Keep the original input, progress arrow, large output, charging well and energy gauge.',
 'GuiAESU': 'Stores energy with adjustable output. Keep both transfer wells, four armor slots, output-rate icon/readout and all four adjustment arrows.',
 'GuiIDSU': 'Provides interdimensional energy storage. Preserve the two transfer wells, shared energy readout, full gauge and four player armor slots.',
 'GuiLESU': 'Controls expandable lapotronic storage. Preserve both transfer wells, live capacity readout, full gauge and four player armor slots.',
 'GuiChargeBench': 'Charges multiple energy items. Keep all six charging wells around the gauge, including their individual slot frames.',
 'GuiFusionReactor': 'Controls a fusion multiblock. Preserve both inputs, central result, energy gauge, coil requirement/state area, size controls and multiplier.',
}
MULTIBLOCKS = {'GuiBlastFurnace','GuiDistillationTower','GuiFluidReplicator','GuiFusionReactor','GuiImplosionCompressor','GuiIndustrialGrinder','GuiIndustrialSawmill','GuiVacuumFreezer'}
HEADER_ACTION = MULTIBLOCKS | {'GuiAutoCrafting','GuiRollingMachine','GuiStorageUnit'}
SMALL_BOTTOM = {'GuiSolar':76,'GuiElevator':76,'GuiLaunchpad':76,'GuiPlayerDetector':76,'GuiIronFurnace':78,'GuiAlloyFurnace':75,'GuiBatbox':84}


def sha(p):
    return hashlib.sha256(Path(p).read_bytes()).hexdigest()


def sprite(name):
    with zipfile.ZipFile(CORE) as z:
        return Image.open(BytesIO(z.read('assets/reborncore/textures/gui/sprites/'+name+'.png'))).convert('RGBA')


def source_image(row):
    p=ROOT/row['capture']
    assert sha(p)==row['sha256']
    raw=Image.open(p).convert('RGBA')
    assert raw.size==(1280,900) and row['state']['siu_loaded'] is False
    return raw


def region(row,rect):
    b=row['state']['bounds']
    return source_image(row).crop(tuple(2*v for v in (b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3]))).resize((rect[2]-rect[0],rect[3]-rect[1]),N)


def physical_region(row,rect):
    b=row['state']['bounds']
    return source_image(row).crop(tuple(2*v for v in (b['x']+rect[0],b['y']+rect[1],b['x']+rect[2],b['y']+rect[3])))


def original(row,top=-4):
    # GuiBase draws a 176px frame even though inherited imageHeight reports 166.
    return physical_region(row,(-28,top,188 if row['id']=='shared-output-port-popup' else 180,180))


def tabs(row):
    slots=row['state']['slots'];upgrade=[s for s in slots if not s['player'] and s['x']<0]
    offset=86 if upgrade else 6
    icons=[]
    for k in range(3):
        r=region(row,(-24,offset+24*k,0,offset+24*k+24))
        if r.getpixel((1,12))[0]>220 or r.getpixel((2,12))[0]>220:
            icons.append(region(row,(-19,offset+24*k+4,-3,offset+24*k+20)))
        else:break
    assert icons,(row['id'],'No settings tabs detected')
    return upgrade,icons


def body(row,key=None,shared=False):
    s=row['state'];cls=s['screen'].split('.')[-1];title=s['title'];upgrades,icons=tabs(row)
    top=0 if cls in HEADER_ACTION and not shared else 14
    bottom=171 if shared else SMALL_BOTTOM.get(cls,92)
    y=24 if top==0 else 26
    w=184
    h=max(y+bottom-top+6,110 if upgrades else 26+len(icons)*24+6)
    mx=50 if upgrades else 22
    result=Image.new('RGBA',(mx+w,h))
    # Separate native settings and upgrade groups remain on the left. This avoids
    # a long column of tabs creating a large empty lower half in the machine.
    for i,icon in enumerate(icons):
        tab=ART.frame(24,24);tab.alpha_composite(icon,(4,4));result.alpha_composite(tab,(0,26+i*24))
    mapped=[]
    if upgrades:
        assert len(upgrades)==4
        tray=ART.frame(26,80)
        for i,slot in enumerate(sorted(upgrades,key=lambda a:a['y'])):
            rect=(slot['x']-1,slot['y']-1,slot['x']+17,slot['y']+17)
            art=region(row,rect);tray.alpha_composite(art,(4,4+18*i))
            mapped.append(dict(index=slot['index'],kind='upgrade',source=list(rect),dest=[30,30+18*i],sha256=hashlib.sha256(art.tobytes()).hexdigest()))
        result.alpha_composite(tray,(26,26))
    window=ART.frame(w,h)
    artwork=region(row,(4,top,172,bottom))
    if top==0:
        ImageDraw.Draw(artwork).rectangle((0,0,artwork.width-1,15),fill=BG)
        if cls in MULTIBLOCKS:
            artwork.alpha_composite(sprite('button_hologram_disabled'),(2,4))
        else:
            x={'GuiAutoCrafting':145,'GuiRollingMachine':130,'GuiStorageUnit':150}[cls]
            artwork.alpha_composite(sprite('button_unlocked'),(x-4,4))
    if cls=='GuiElevator' and not shared:
        # Source confirms that its only work graphic is the buffer gauge.
        w=108;mx=22;h=90;result=Image.new('RGBA',(mx+w,h));tab=ART.frame(24,24);tab.alpha_composite(icons[0],(4,4));result.alpha_composite(tab,(0,26));window=ART.frame(w,h)
        artwork=region(row,(7,17,25,71));bx=(w-artwork.width)//2;by=27;window.alpha_composite(artwork,(bx,by));transform=dict(source=[7,17,25,71],dest=[mx+bx,by])
    else:
        window.alpha_composite(artwork,(8,y));transform=dict(source=[4,top,172,bottom],dest=[mx+8,y])
        for slot in s['slots']:
            if slot['x']<0 or (slot['player'] and slot['y']>=93):continue
            rect=(slot['x']-1,slot['y']-1,slot['x']+17,slot['y']+17)
            assert rect[0]>=4 and rect[2]<=172 and rect[1]>=max(top,16) and rect[3]<=bottom,(row['id'],rect,top,bottom)
            dest=[mx+8+rect[0]-4,y+rect[1]-top]
            mapped.append(dict(index=slot['index'],kind='equipment'if slot['player']else'machine',source=list(rect),dest=dest,sha256=hashlib.sha256(region(row,rect).tobytes()).hexdigest()))
    controls=ART.header(window,title)
    result.alpha_composite(window,(mx,0))
    for slot in mapped:
        x,z=slot['dest'];assert hashlib.sha256(result.crop((x,z,x+18,z+18)).tobytes()).hexdigest()==slot['sha256'],(row['id'],slot)
    return result,dict(main_origin=[mx,0],main_size=[w,h],body=transform,slots=mapped,side_tabs=len(icons),chrome=controls,native_header_action=top==0,settings_icon_sources=[dict(source=[-19,(86 if upgrades else 6)+24*i+4,-3,(86 if upgrades else 6)+24*i+20],dest=[4,30+24*i])for i in range(len(icons))],settings_rail='Left of the native vertical upgrade tray'if upgrades else'Left of the parent')


def popup(row,kind):
    # Native frames measured against their original 84px popup geometry.
    rect={'input':(54,-7,138,130),'output':(100,-7,184,100),'fluid':(55,-19,139,103)}[kind]
    art=region(row,(rect[0]+4,rect[1]+4,rect[2]-5,rect[3]-5))
    # The parent-managed child gets its own ordinary SIU Close; remove only the
    # redundant native corner close, preserving the six faces and every checkbox.
    ImageDraw.Draw(art).rectangle((62,0,art.width-1,12),fill=BG)
    title={'input':'Item Input','output':'Item Output','fluid':'Fluid Sides'}[kind]
    w=max(100,ART.tw(title)+47);h=26+art.height+6
    im=ART.frame(w,h);controls=ART.header(im,title);x=(w-art.width)//2;im.alpha_composite(art,(x,26))
    return im,dict(main_origin=[0,0],main_size=[w,h],body=dict(source=[rect[0]+4,rect[1]+4,rect[2]-5,rect[3]-5],dest=[x,26]),slots=[],side_tabs=0,chrome=controls,temporary_child=True,native_close_replaced=True)


def redstone(row):
    art=region(row,(7,94,169,170));w=184;h=26+art.height+6
    im=ART.frame(w,h);controls=ART.header(im,'Redstone');im.alpha_composite(art,((w-art.width)//2,26))
    return im,dict(main_origin=[0,0],main_size=[w,h],body=dict(source=[7,94,169,170],dest=[11,26]),slots=[],side_tabs=0,chrome=controls,temporary_settings_view=True)


def finish_image(image,geometry,row):
    """Keep native 2x raster detail, including the 0.6-scale energy readouts."""
    result=image.resize((image.width*2,image.height*2),N)
    body=geometry['body'];art=physical_region(row,body['source'])
    if geometry.get('native_header_action'):
        ImageDraw.Draw(art).rectangle((0,0,art.width-1,31),fill=BG)
        cls=row['state']['screen'].split('.')[-1]
        if cls in MULTIBLOCKS:icon=sprite('button_hologram_disabled');x=2
        else:icon=sprite('button_unlocked');x={'GuiAutoCrafting':145,'GuiRollingMachine':130,'GuiStorageUnit':150}[cls]-4
        art.alpha_composite(icon.resize((40,24),N),(2*x,8))
    if geometry.get('native_close_replaced'):
        ImageDraw.Draw(art).rectangle((124,0,art.width-1,25),fill=BG)
        # The native close overlaps the upper-right of this 62px inset. Restore
        # that rim from its real sprite after relocating the close to SIU chrome.
        rim=sprite('slot_config_popup').resize((124,124),N)
        for rect in [(0,0,124,4),(0,120,124,124),(0,0,4,124),(120,0,124,124)]:
            art.alpha_composite(rim.crop(rect),(8+rect[0],6+rect[1]))
    result.alpha_composite(art,tuple(2*v for v in body['dest']))
    for slot in geometry['slots']:
        art=physical_region(row,slot['source']);xy=tuple(2*v for v in slot['dest']);result.alpha_composite(art,xy)
        assert result.crop((xy[0],xy[1],xy[0]+36,xy[1]+36)).tobytes()==art.tobytes()
    for icon in geometry.get('settings_icon_sources',[]):
        result.alpha_composite(physical_region(row,icon['source']),tuple(2*v for v in icon['dest']))
    geometry['image_pixels_per_gui_pixel']=2
    return result


def make_pages():
    rows=json.loads((OUT/'fresh-native.json').read_text());byid={r['id']:r for r in rows};old=json.loads((OUT/'native-references.json').read_text())['cases']
    pages=[];directory=OUT/'images';directory.mkdir(exist_ok=True)
    for oldrow in old:
        row=byid[oldrow['id']];cls=row['state']['screen'].split('.')[-1]
        assert cls in DESCRIPTIONS
        image,geometry=body(row)
        keep='Preserve the native apparatus as a single group. Keep the gray palette, complete slot rims, ghost icons, gauges and native controls. '
        if geometry['side_tabs'] and any(s['kind']=='upgrade'for s in geometry['slots']):
            keep+='The four upgrade wells stay vertical; settings form a second short rail on their left, avoiding empty window height.'
        else:keep+='Settings stay on the left; the separate SIU Inventory supplies transfers.'
        if cls in HEADER_ACTION:keep+=' Native H/lock controls have their own clear row below the SIU title.'
        if cls=='GuiElevator':keep='Center the complete native energy gauge in a narrow status window. The native redstone tab stays at the left. No storage or sorting actions are invented.'
        page=dict(id=row['id'],title=row['state']['title'],row=row,geometry=geometry,does=DESCRIPTIONS[cls],design=keep,kind='machine',screen=cls)
        if cls in MULTIBLOCKS:page['note']='Actual formed multiblock, idle. Its unformed warning and H control are accounted for in the appendix.'
        pages.append(page)
        finish_image(image,geometry,row).save(directory/(page['id']+'-window.png'));original(row).save(directory/(page['id']+'-native.png'))
    shared=[
      ('shared-item-configuration','Item Configuration','overview','Select a native machine slot to edit its external item IO. Red highlights, color meanings and copy/paste instructions are part of this view.','Expand the same parent while settings are open. Preserve the complete native help panel; it is not discarded with the player inventory.'),
      ('shared-item-port-popup','Item Input - Port Inspector','input','Edits the six machine sides, IO priorities, Auto Input, Auto Output and Filter Input for an applicable input slot.','Use a temporary parent-owned inspector with normal SIU chrome. Keep the native face diagram, letter row and all three checkboxes together. Close returns to item configuration.'),
      ('shared-output-port-popup','Item Output - Port Inspector','output','Edits an output slot. The native output-only variant omits input controls and has a shorter checkbox area.','Use the same inspector family with its native shorter height. Keep the permitted pencils and Auto Output; do not invent an Auto Input checkbox.'),
      ('shared-fluid-configuration','Fluid Side Configuration','fluid','Edits all six fluid sides, native mode pencils, Pull In and Pump Out.','Open a temporary inspector attached to the machine session. Preserve the complete diagram and both native toggles. Its Close returns to the machine; the help-only black void is omitted.'),
      ('shared-redstone-configuration','Redstone Configuration','redstone','Controls the native per-machine redstone modes. The number and names of rows depend on the machine.','Use a compact settings view in the parent session. Preserve every native row, icon, state and cycling action. The selected left tab returns to the machine view.'),
    ]
    for key,title,kind,does,design in shared:
        row=byid[key]
        if kind=='overview':image,geometry=body(row,shared=True)
        elif kind=='redstone':image,geometry=redstone(row)
        else:image,geometry=popup(row,kind)
        page=dict(id=key,title=title,row=row,geometry=geometry,does=does,design=design,kind='shared',screen=row['state']['trSelectedTab'])
        pages.append(page);finish_image(image,geometry,row).save(directory/(key+'-window.png'));original(row,top=-24).save(directory/(key+'-native.png'))
    for key,base,title,note in [('storage-unit-populated','techreborn-basic-storage-unit','Storage Unit - Populated','The name, amount, use percentage and wrench hint occupy space that looks empty in the idle capture.'),('tank-unit-populated','techreborn-basic-tank-unit','Tank Unit - Populated','The filled fluid name, amount and capacity readouts occupy the native status area. Keep this space.')]:
        row=byid[key];image,geometry=body(row);page=dict(id=key,title=title,row=row,geometry=geometry,does=note,design='Use the same window geometry as the empty state. Preserve the native live text, IN/OUT labels and complete wells. The reference was populated through ordinary slot clicks.',kind='state',screen=row['state']['screen'].split('.')[-1]);pages.append(page);finish_image(image,geometry,row).save(directory/(key+'-window.png'));original(row).save(directory/(key+'-native.png'))
    assert len(pages)==58
    registered=set(re.findall(r'(Gui\w+)::new',(SOURCE/'src/client/java/techreborn/client/ClientGuiType.java').read_text()))
    assert registered==set(p['screen']for p in pages if p['kind']=='machine') and len(registered)==51
    return pages,rows


def build_pdf(pages,rows):
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
    W,H=1191,842;c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    c.setTitle('Tech Reborn / SIU Window Mockups');c.setAuthor("Salt's Inventory Update / Project Everything")
    text_boxes=[]
    def text(s,x,y,size=12,bold=False,color='#223F48'):
        assert 25<=x and y>=20 and x+pdfmetrics.stringWidth(s,'SegoeB'if bold else'Segoe',size)<=W-25,(s,x,y)
        c.setFont('SegoeB'if bold else'Segoe',size);c.setFillColor(HexColor(color));c.drawString(x,y,s);text_boxes.append(dict(page=c.getPageNumber(),text=s,x=x,y=y,size=size))
    def para(s,x,y,width,size=12,leading=18):
        line=''
        for word in s.split():
            trial=(line+' '+word).strip()
            if pdfmetrics.stringWidth(trial,'Segoe',size)>width:
                text(line,x,y,size);y-=leading;line=word
            else:line=trial
        if line:text(line,x,y,size);y-=leading
        return y
    def base(title,sub='Minecraft 1.20.1 / Fabric / Tech Reborn 5.8.14 + Reborn Core 5.8.14'):
        c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
        text('PROJECT EVERYTHING / TECH REBORN / WINDOW DESIGN REVIEW',36,H-34,10,True,'#247C80')
        text(title,36,H-78,27,True);text(sub,36,H-102,11,color='#60727B')
        c.setStrokeColor(HexColor('#D6E3E6'));c.line(36,42,W-36,42)
        text('Native game captures + proposed mockups / Design only',36,25,9,color='#60727B')
        text(str(c.getPageNumber()),W-55,25,9,color='#60727B')
    def panel(x,y,w,h):
        c.setFillColor(HexColor('#EFF5F5'));c.roundRect(x,y,w,h,14,fill=1,stroke=0)
    def picture(path,x,y,w,h,scale=None):
        im=Image.open(path).convert('RGBA');pixel_scale=2 if str(path).endswith(('-native.png','-window.png'))else 1
        logical_w,logical_h=im.width/pixel_scale,im.height/pixel_scale;sc=min(w/logical_w,h/logical_h)if scale is None else min(scale,w/logical_w,h/logical_h)
        # Embed nearest-neighbor pixels at a useful fixed source resolution.
        enlarged=im.resize((im.width*2,im.height*2),N);c.drawImage(ImageReader(enlarged),x+(w-logical_w*sc)/2,y+(h-logical_h*sc)/2,logical_w*sc,logical_h*sc,mask='auto')
    base('Tech Reborn, carefully brought onto the desktop')
    text('51 MACHINE SCREENS  /  5 SHARED SETTINGS VIEWS  /  2 POPULATED EXAMPLES',36,686,13,True,'#247C80')
    para('Preserve Tech Reborn\'s apparatus, gray material, vertical energy buffers and native controls. Remove the embedded player inventory, fit normal SIU management controls, and give every exposed attachment a finished border.',36,651,1115,15,23)
    for i,key in enumerate(['techreborn-alloy-smelter','techreborn-industrial-blast-furnace','pr-techreborn-adjustable-su']):
        x=36+i*379;panel(x,255,361,300);picture(OUT/'images'/(key+'-window.png'),x+12,275,337,256,1.4)
    text('A compact work area',48,232,15,True);text('A real formed multiblock',427,232,15,True);text('All native controls retained',806,232,15,True)
    para('Upgrade wells remain vertical on the left. The settings tabs use a second short left rail so they do not force an empty lower half. Both rails have complete inner edges.',36,188,540,12)
    para('The next pages pair SIU-free originals with static proposals at matched magnification. No compatibility code or release jars are changed in this design stage.',615,188,540,12)
    para('All 51 registered screen types were opened in a minimal production Fabric client. The eight multiblocks were constructed using their own native structure definitions and passed native validation.',36,99,1115,11,16)
    c.showPage()
    base('Complete screen index')
    text('Each machine has its own comparison page. Tiers sharing a screen use the same window geometry.',36,686,12)
    for i,p in enumerate(pages[:51]):
        col=i//17;row=i%17;x=36+col*379;y=650-row*30
        text(f'{i+1:02}  {p["title"]}',x,y,12)
        text(str(3+i),x+335,y,11,color='#60727B')
        c.linkAbsolute(p['title'],p['id'],Rect=(x,y-3,x+355,y+16),thickness=0)
    para('Shared settings and populated states follow the 51 machine pages. The appendix accounts for all eight unformed multiblock states, the manual and the unused DestructoPack handler.',36,106,1115,12)
    c.showPage()
    for number,p in enumerate(pages):
        c.bookmarkPage(p['id']);c.addOutlineEntry(p['title'],p['id'],level=0,closed=False)
        base(p['title']);left=36;right=610;pw=545;py=233;ph=438
        text('ORIGINAL IN GAME / SIU ABSENT',left,690,11,True,'#247C80');text('PROPOSED SIU WINDOW / MOCKUP',right,690,11,True,'#247C80')
        panel(left,py,pw,ph);panel(right,py,pw,ph)
        lp=OUT/'images'/(p['id']+'-native.png');rp=OUT/'images'/(p['id']+'-window.png')
        a=Image.open(lp);b=Image.open(rp);scale=min(2.2,(pw-24)/max(a.width/2,b.width/2),(ph-20)/max(a.height/2,b.height/2))
        picture(lp,left+12,py+10,pw-24,ph-20,scale);picture(rp,right+12,py+10,pw-24,ph-20,scale)
        text('WHAT IT DOES / WHAT STAYS',left,207,11,True,'#247C80');text('WINDOW DESIGN',right,207,11,True,'#247C80')
        ly=para(p['does'],left,185,pw,12,17);ry=para(p['design'],right,185,pw,12,17)
        assert min(ly,ry)>61,(p['id'],ly,ry)
        if p.get('note'):text(p['note'],36,70,10,color='#60727B')
        else:
            slots=Counter(s['kind']for s in p['geometry']['slots']);details=f'{p["screen"]} / {slots.get("machine",0)} machine slots / {slots.get("equipment",0)} armor slots / {slots.get("upgrade",0)} upgrade slots'
            text(details,36,70,10,color='#60727B')
        source_url=SOURCE_URL+'/src/client/java/techreborn/client/gui/'+p['screen']+'.java'if p['kind']in['machine','state']else SOURCE_URL+'/RebornCore/src/client/java/reborncore/client/gui/config'
        c.linkURL(source_url,(36,61,560,85),relative=0,thickness=0)
        c.showPage()
    warning=[r for r in rows if r.get('variant')=='unformed']
    for start in [0,4]:
        base('Multiblocks: the unformed state is retained')
        para('These are genuine native warning states, not missing designs. Their corresponding window pages show valid assembled machines. Keep the native warning, coil requirements and H hologram action inside the working area when the structure is incomplete.',36,688,1115,12,18)
        for i,row in enumerate(warning[start:start+4]):
            x=36+(i%2)*574;y=95+(1-i//2)*250;panel(x,y,545,226)
            im=region(row,(-27,0,178,94));path=OUT/'images'/(row['id']+'-warning.png');im.save(path);picture(path,x+10,y+10,525,183,2.1);text(row['state']['title'],x+13,y+203,13,True)
        c.showPage()
    base('Scope, native-only screens and evidence')
    text('51 / 51 registered machine screens',36,677,19,True)
    para('The pinned ClientGuiType registry and all 51 native opening captures agree exactly. The five shared configuration designs are views/popups of those machines, not five additional machine classes. Two populated examples reuse the Storage Unit and Tank Unit designs.',36,645,1115,13,20)
    text('Keep the manual native',36,548,18,True)
    manual=next(r for r in rows if r['id']=='native-manual');raw=source_image(manual);raw.crop((425,250,855,650)).save(OUT/'images/native-manual.png');picture(OUT/'images/native-manual.png',765,288,360,288)
    para('Tech Reborn\'s Manual has wiki, Discord and optional refund controls, with no inventory work area. It keeps its normal book screen. External link confirmations also stay native; no SIU book window is proposed.',36,518,675,13,20)
    text('The DestructoPack handler is not an extra live UI',36,411,18,True)
    para('The pinned source includes DestructoPackScreenHandler, but no screen registration or opening reference uses it. It is excluded from the reachable inventory count rather than shown as an invented 52nd machine.',36,380,675,13,20)
    text('Evidence and limits',36,270,18,True)
    para('Originals: production Fabric, Tech Reborn 5.8.14 with embedded Reborn Core 5.8.14; SIU absent. Native settings and populated units were reached through ordinary UI actions. Structure blocks came from native blueprints, followed by native validity checks. These are layout and scope observations, not a complete functional/gameplay test.',36,238,1115,12,18)
    para('Mockups are static compositions from immutable game captures, native sprites and SIU\'s existing frame assets. They are not screenshots of an implemented compatibility adapter. The future implementation must preserve slot IDs, item/fluid rules, native packets, hover/tooltips, popup lifetime and the public API.',36,151,1115,12,18)
    text('Pinned publisher source: TechReborn/TechReborn / tag 5.8.14 / '+COMMIT[:12],36,72,10,color='#60727B');c.linkURL(SOURCE_URL,(36,60,850,87),relative=0,thickness=0)
    c.showPage();c.save()
    (OUT/'pdf-text-layout.json').write_text(json.dumps(text_boxes,indent=2))


def main():
    PDF.parent.mkdir(exist_ok=True,parents=True)
    pages,rows=make_pages();build_pdf(pages,rows)
    public=[]
    for p in pages:
        public.append({k:v for k,v in p.items()if k!='row'}|dict(native_capture=p['row']['capture'],native_sha256=p['row']['sha256'],native_image=str((OUT/'images'/(p['id']+'-native.png')).relative_to(ROOT)),window_image=str((OUT/'images'/(p['id']+'-window.png')).relative_to(ROOT))))
    (OUT/'manifest.json').write_text(json.dumps(dict(release='5.8.14',minecraft='1.20.1',loader='fabric',source_commit=COMMIT,jar_sha256=sha(JAR),reborn_core_sha256=sha(CORE),screen_types=51,comparison_plates=len(pages),native_captures=len(rows),pages=public),indent=2))
    reader=PdfReader(PDF);assert len(reader.pages)==63,len(reader.pages)
    text='\n'.join(p.extract_text()for p in reader.pages)
    assert 'DestructoPack' in text and 'Fluid Side Configuration' in text
    verification=dict(status='layout checks passed; rendered visual inspection required',pages=len(reader.pages),registered_screens=51,comparison_plates=len(pages),native_captures=len(rows),all_slot_crops_pixel_identical=True,native_sources_immutable=True,implementation_changed=False,pdf_sha256=sha(PDF))
    (OUT/'verification.json').write_text(json.dumps(verification,indent=2))
    print(json.dumps(verification,indent=2))


if __name__=='__main__':main()
