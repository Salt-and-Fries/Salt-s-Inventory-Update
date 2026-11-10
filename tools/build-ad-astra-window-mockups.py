"""Ad Astra design review: immutable native references and native-metal windows.

No implementation changes. Preserve full-width circuitry, every native slot,
external charging bars and monitors. The configuration drawer is an explicit
design state based on a fresh native Energy configuration capture.
"""
from pathlib import Path
from io import BytesIO
from collections import Counter
import hashlib,importlib.util,json,zipfile
from PIL import Image,ImageDraw,ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/ad-astra-window-design';TMP=ROOT/'tmp/pdfs/ad-astra'
PDF=ROOT/'output/pdf/siu-ad-astra-window-mockups.pdf'
JAR=ROOT/'build/ui-modpack-audit-20261025/jars/66ffb3d9e39ae5ab698a1528eab57ef0fdd1bfc4.jar'
RELEASE='https://modrinth.com/mod/ad-astra/version/gZClwdxY'
N=Image.Resampling.NEAREST;W,H=1191,842
INK='#203b3e';TEAL='#176f70';MUTED='#586b6c'
s=importlib.util.spec_from_file_location('ad_astra_siu_art',ROOT/'tools/build-enchanted-window-mockups.py')
ART=importlib.util.module_from_spec(s);s.loader.exec_module(ART)

SPECS=[
 dict(asset='coal_generator',id='ad-astra-coal-generator',title='Coal Generator',machine=True,
      does='Burns fuel to generate energy. Keep the fuel port, heat effect, energy gauge and the separate charging slot.',
      design='Preserve the light iron casing and every pipe that reaches its edge. Native settings, redstone and charging stay docked above the window.'),
 dict(asset='compressor',id='ad-astra-compressor',title='Compressor',machine=True,
      does='Compresses a recipe ingredient using energy. Its input, hammer animation, output and energy gauge form one working assembly.',
      design='Keep the entire piston, hammer and output surround. The dark metal shell gets readable window controls and a complete new bottom rim.'),
 dict(asset='etrionic_blast_furnace',id='adastra-gap-etrionic-blast-furnace',title='Etrionic Blast Furnace',machine=True,
      does='Processes alloy recipes through four input and four output cells, alongside the furnace animation, energy gauge and charging slot.',
      design='Preserve both 2 x 2 banks and the complete furnace. A compact More/Close header gives the full title room without covering the apparatus.'),
 dict(asset='nasa_workbench',id='ad-astra-nasa-workbench',title='Nasa Workbench',machine=True,battery=False,
      does='Builds rockets using fourteen ingredient cells. The large result is a native click-to-craft preview, not an ordinary pickup slot.',
      design='Keep the full rocket-shaped chassis and preview surround. Clean the incidental hover highlight; preserve native recipe and crafting actions.'),
 dict(asset='fuel_refinery',id='ad-astra-fuel-refinery',title='Fuel Refinery',machine=True,
      does='Refines oil into rocket fuel. Keep both container input/output pairs, both fluid gauges, the central vessel and the energy gauge.',
      design='Move no individual ports. Keep the complete left-to-right flow, pipe junctions and all three tall gauges on the original charcoal surface.'),
 dict(asset='cryo_freezer',id='ad-astra-cryo-freezer',title='Cryo Freezer',machine=True,
      does='Turns supported recipe ingredients into cryogenic fuel. Keep the ingredient port, fluid-container pair, progress effect and energy gauge.',
      design='Retain its distinctive rose-metal palette and every pipe. The dark slots stay native; a generic gray slot layer must never cover them.'),
 dict(asset='oxygen_loader',id='ad-astra-oxygen-loader',title='Oxygen Loader',machine=True,
      does='Converts water to oxygen and transfers fluid through native containers. Both paired port columns, fluid gauges and energy remain visible.',
      design='Keep the native vessel, full-height tanks and all container ports together. The charging cell stays in the attached machine toolbar.'),
 dict(asset='water_pump',id='adastra-gap-water-pump',title='Water Pump',machine=True,
      does='Pumps water using energy. The native red X reports the captured missing-water condition; the menu has a charging cell, not a cargo grid.',
      design='Preserve the complete pump diagram, status symbol, water tank and energy gauge. Keep the lower/right pipe bends above the new bottom edge.'),
 dict(asset='solar_panel',id='ad-astra-solar-panel',title='Solar Panel',machine=True,plate=41,
      does='Reports solar generation and stored energy. The captured native screen is active: its green monitor and sun indicator remain intact.',
      design='Keep the monitor above the metal title strip. Place window controls clear of its right-hand pipe, retaining the complete circuit below.'),
 dict(asset='oxygen_distributor',id='adastra-gap-oxygen-distributor',title='Oxygen Distributor',machine=True,plate=61,extra=True,
      does='Distributes oxygen to an area. Keep the green usage/coverage monitor, container ports, two fluid gauges, energy and show-area control.',
      design='Preserve the top-mounted monitor and its native toolbar beside it. Show the entire charging cell, including the part beyond the main plate.'),
 dict(asset='gravity_normalizer',id='adastra-gap-gravity-normalizer',title='Gravity Normalizer',machine=True,extra=True,
      does='Sets local gravity with the native slider. The target readout, energy use, affected-block count, energy gauge and area toggle stay available.',
      design='Keep the full slider travel and both cyan displays. Do not crop the bottom readout or turn its status-only body into an invented storage grid.'),
 dict(asset='rocket',id='own-adastra-tier-1-rocket',title='Tier 1 Rocket',vehicle=True,
      does='Provides eight cargo cells plus the fuel-container input and return slot. The tank remains beside its ports; higher tiers reuse this screen.',
      design='Keep the fuel module on the left and native 4 x 2 cargo bank on the right. Preserve cargo silhouettes and the take-only return slot.'),
 dict(asset='rover',id='own-adastra-tier-1-rover',title='Tier 1 Rover',vehicle=True,
      does='Combines sixteen cargo cells with fuel-container input, return and tank. The tall 4 x 4 cargo bank is part of the vehicle inventory.',
      design='Give the upper cargo row proper header clearance. Keep the sixteen-cell bank, fuel apparatus and original slot art without scaling them.'),
 dict(asset='lander',id='own-adastra-lander',title='Lander',vehicle=True,
      does='Retrieves landing/recovery contents through eight cargo and three recovery cells. All eleven native cells are take-only.',
      design='Keep the large recovery port and linked smaller ports beside the cargo bank. Preserve take-only rules; add no deposit or refueling action.'),
]

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def tex(name):
    with zipfile.ZipFile(JAR)as z:return Image.open(BytesIO(z.read('assets/ad_astra/textures/gui/'+name+'.png'))).convert('RGBA')
def nine(texture,w,h,b=6):
    sw,sh=texture.size;im=Image.new('RGBA',(w,h))
    for src,dst in [((0,0,b,b),(0,0,b,b)),((sw-b,0,sw,b),(w-b,0,w,b)),((0,sh-b,b,sh),(0,h-b,b,h)),((sw-b,sh-b,sw,sh),(w-b,h-b,w,h)),
                    ((b,0,sw-b,b),(b,0,w-b,b)),((b,sh-b,sw-b,sh),(b,h-b,w-b,h)),((0,b,b,sh-b),(0,b,b,h-b)),((sw-b,b,sw,sh-b),(w-b,b,w,h-b)),((b,b,sw-b,sh-b),(b,b,w-b,h-b))]:
        im.alpha_composite(texture.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]),N),(dst[0],dst[1]))
    return im
def surface(atlas,plate=0):
    # Sample the known bare face, not the atlas mode: a cargo-heavy atlas can
    # contain more recessed-slot pixels than casing pixels (notably the Rover).
    color=atlas.getpixel((7,plate+16));assert color[3]==255;return color
def metal_frame(atlas,plate,h):
    width=atlas.width;skin=atlas.crop((0,plate,width,atlas.height));im=Image.new('RGBA',(width,h),surface(atlas,plate));b=6
    # A flat frame stretch cannot pull former player-slot artwork into the rim.
    for box,xy in [((0,0,width,b),(0,0)),((0,skin.height-b,width,skin.height),(0,h-b))]:im.alpha_composite(skin.crop(box),xy)
    for x in [0,width-b]:
        strip=skin.crop((x,16,x+b,17)).resize((b,h-2*b),N);im.alpha_composite(strip,(x,b))
    return im
def grab(row,box,capture=None):
    n=row['native'];p=ROOT/(capture or n['capture']);im=Image.open(p).convert('RGBA');assert im.size==(1280,900)
    x,y=n['bounds']['x'],n['bounds']['y'];l,t,r,b=box
    return im.crop((2*(x+l),2*(y+t),2*(x+r),2*(y+b))).resize((r-l,b-t),N)
def toolbar(spec,atlas):
    if not spec.get('machine'):return None
    count=2+int(spec.get('battery',True))+int(spec.get('extra',False));w=count*18+(count-1)*3+12
    x,y=(98,0)if spec['asset']=='oxygen_distributor'else(atlas.width-w,-32)
    return (x,y,x+w,y+30)
def original(row,spec):
    atlas=tex('container/'+spec['asset']);x=-8 if spec.get('vehicle')else 0;bar=toolbar(spec,atlas)
    l=min(x,bar[0]if bar else x);t=min(0,bar[1]if bar else 0);r=max(x+atlas.width,bar[2]if bar else x+atlas.width);b=atlas.height
    im=Image.new('RGBA',(r-l,b-t));body=grab(row,(x,0,x+atlas.width,atlas.height));body.putalpha(atlas.getchannel('A'));im.alpha_composite(body,(x-l,-t))
    if bar:
        part=grab(row,bar);part.putalpha(nine(tex('sprites/options_bar').crop((0,0,30,30)),part.width,part.height).getchannel('A'));im.alpha_composite(part,(bar[0]-l,bar[1]-t))
    return im,dict(rendered_native_bounds=[l,t,r,b],slot_origin_offset_x=-x)
def controls(im,title,y=5,right_reserve=0,storage=False,light=False):
    full=[1,10,0,6,4,9,2]if storage else[0,6,4,9,2]
    right=im.width-5-right_reserve;columns=full if 7+ART.tw(title)+7<=right-len(full)*14 else[3,2]
    start=right-len(columns)*14;assert 7+ART.tw(title)+6<=start,(title,im.width,start,ART.tw(title))
    ART.text(im,title,7,y+2,(42,38,43,255)if light else(223,219,227,255))
    for i,column in enumerate(columns):
        sy=22 if column==0 else 0;im.alpha_composite(ART.CONTROLS.crop((column*11,sy,column*11+11,sy+11)),(start+i*14,y))
    return dict(columns=columns,x=start,y=y,right_reserve=right_reserve)
def make(row,spec,capture=None,configured=False):
    atlas=tex('container/'+spec['asset']);width=atlas.width;plate=spec.get('plate',0);shiftx=8 if spec.get('vehicle')else 0
    inv=min(s['y']for s in row['native']['slots']if s['player']);cap=inv-1;bg=surface(atlas,plate)
    region=atlas.crop((6,max(plate+14,14),width-6,cap));mask=Image.new('L',region.size)
    mask.putdata([255 if p[3]and p[:3]!=bg[:3]else 0 for p in region.getdata()]);bbox=mask.getbbox();assert bbox
    slots=[s for s in row['native']['slots']if not s['player']];battery=spec.get('machine')and spec.get('battery',True)
    body_slots=[s for s in slots if not(battery and s['index']==0)]
    end=max(max(plate+14,14)+bbox[3]+4,max([s['y']+17 for s in body_slots],default=0)+4)
    end=min(cap,end);start=plate+16 if plate else 14;head=16 if plate else 22
    height=head+end-start+6;assert end>start
    root=metal_frame(atlas,plate,height)
    if plate:root.alpha_composite(atlas.crop((0,plate,width,plate+16)),(0,0))
    # Use menu-origin coordinates even where VehicleScreen draws its plate at -8.
    box=(-shiftx,start,width-shiftx,end);patch=grab(row,box,capture)
    canonical=patch.copy();old_label=inv-12
    old_title_y=9 if spec['asset']in['compressor','etrionic_blast_furnace','gravity_normalizer']else 6
    for yy in range(start,end):
        for xx in range(width):
            menu_x=xx-shiftx
            old_title=(old_title_y<=yy<old_title_y+9 and -3<=menu_x<8+ART.tw(row['native']['title']))
            label_right=width-7 if configured else 8+ART.tw('Inventory')
            inventory_label=(old_label<=yy<old_label+9 and -3<=menu_x<label_right)
            hover=(spec['asset']=='nasa_workbench'and 83<=menu_x<99 and 110<=yy<126)
            if old_title or inventory_label or hover:canonical.putpixel((xx,yy-start),atlas.getpixel((xx,yy)))
    root.alpha_composite(canonical,(0,head))
    header=controls(root,spec['title'],y=3 if plate else 5,right_reserve=14 if plate else 0,storage=bool(spec.get('vehicle')),light=spec['asset']=='coal_generator')
    assert root.crop((0,head,width,head+canonical.height)).tobytes()==canonical.tobytes()
    bar=toolbar(spec,atlas);fragments=[]
    if plate:
        monitor=grab(row,(0,0,width,plate),capture);monitor.putalpha(atlas.crop((0,0,width,plate)).getchannel('A'));fragments.append((monitor,0,-plate,'native-status-monitor',[0,0,width,plate]))
    if bar:
        part=grab(row,bar,capture);part.putalpha(nine(tex('sprites/options_bar').crop((0,0,30,30)),part.width,part.height).getchannel('A'))
        fragments.append((part,bar[0],bar[1]-plate,'native-options-toolbar',list(bar)))
    top=min([0]+[y for _,x,y,_,_ in fragments]);right=max([width]+[x+p.width for p,x,y,_,_ in fragments]);im=Image.new('RGBA',(right,height-top))
    im.alpha_composite(root,(0,-top))
    for p,x,y,_,_ in fragments:im.alpha_composite(p,(x,y-top))
    moved=[]
    for slot in slots:
        if battery and slot['index']==0:
            dest=[slot['x']-1,slot['y']-1-plate,18,18];area=bar
            assert area[0]<=slot['x']-1 and area[1]<=slot['y']-1 and slot['x']+17<=area[2]and slot['y']+17<=area[3]
        else:
            dest=[slot['x']-1+shiftx,head+slot['y']-1-start,18,18]
            assert start<=slot['y']-1 and slot['y']+17<=end,(spec['asset'],slot,start,end)
            assert 0<=dest[0]and dest[0]+18<=width
        assert top<=dest[1]and dest[1]+18<=height
        final=im.crop((dest[0],dest[1]-top,dest[0]+18,dest[1]-top+18))
        assert final.getchannel('A').getextrema()==(255,255),(spec['asset'],slot['index'],'slot or rim clipped by alpha')
        moved.append(dict(index=slot['index'],native=[slot['x']-1,slot['y']-1,18,18],destination=dest,role='charging'if battery and slot['index']==0 else'working'))
    return im,dict(root_size=[width,height],render_bounds=[0,top,right,height],header=header,body_source=list(box),body_destination=[0,head],native_body_pixel_preservation=True,
                   native_slots=moved,complete_slot_alpha_checks=True,attachments=[dict(id=id,source=box,destination=[x,y],size=list(p.size))for p,x,y,id,box in fragments],
                   palette_surface_rgba=list(bg),native_plate_start=plate,vehicle_plate_offset_corrected=bool(shiftx),incidental_hover_removed=spec['asset']=='nasa_workbench')

def para(c,text,x,top,width,size=11,color=INK,bold=False,leading=None):
    assert text.isascii(),text;font='SegoeBold'if bold else'Segoe';leading=leading or size*1.3;words=text.split();lines=[];line=''
    for word in words:
        trial=(line+' '+word).strip()
        if pdfmetrics.stringWidth(trial,font,size)>width:assert line;lines.append(line);line=word
        else:line=trial
    if line:lines.append(line)
    c.setFont(font,size);c.setFillColor(HexColor(color))
    for i,line in enumerate(lines):c.drawString(x,H-top-size*.8-i*leading,line)
    return top+len(lines)*leading
def label(c,text,x,y):return para(c,text.upper(),x,y,1100,9.5,TEAL,True)
def foot(c,page):
    c.setStrokeColor(HexColor('#d2dddd'));c.line(42,41,W-42,41)
    para(c,'Native game references + proposed window designs | Mockups only; no implementation or gameplay certification',42,815,1060,8,MUTED)
    para(c,str(page),W-70,814,35,9,MUTED)
def picture(c,im,x,top,w,h,scale=2):
    factor=min(scale,w/im.width,h/im.height);assert factor>0
    if factor==int(factor):draw=im.resize((int(im.width*factor),int(im.height*factor)),N)
    else:draw=im
    dw,dh=im.width*factor,im.height*factor
    c.drawImage(ImageReader(draw),x+(w-dw)/2,H-top-h+(h-dh)/2,dw,dh,mask='auto')
    return factor
def compare(c,title,original,mock,does,design,page,keep='Complete native metalwork, indicators and slot artwork',caption='Same 2x GUI-pixel scale on both sides'):
    label(c,'PROJECT EVERYTHING / AD ASTRA WINDOW DESIGN',42,28)
    para(c,title,42,54,1107,28,bold=True)
    para(c,'Minecraft 1.20.1 / Fabric / Ad Astra 1.15.20',42,96,1100,11,MUTED)
    para(c,keep,42,120,1100,11,TEAL)
    label(c,'ORIGINAL IN GAME / SIU ABSENT',42,146);label(c,'PROPOSED SIU WINDOW / MOCKUP',610,146)
    for x in [42,610]:c.setFillColor(HexColor('#edf4f4'));c.roundRect(x,H-166-540,539,540,10,fill=1,stroke=0)
    left=picture(c,original,46,170,531,532);right=picture(c,mock,614,170,531,532)
    assert left==right==2,(title,left,right,original.size,mock.size)
    para(c,caption,52,715,1090,9,MUTED)
    label(c,'NATIVE FUNCTION',42,738);label(c,'WINDOW TREATMENT',610,738)
    a=para(c,does,42,755,531,10.5);b=para(c,design,610,755,539,10.5);assert max(a,b)<799,(title,a,b)
    foot(c,page);c.showPage()

def cover(c,designs):
    label(c,'PROJECT EVERYTHING / WINDOW DESIGN REVIEW',42,34)
    para(c,'Ad Astra',42,77,1100,48,bold=True)
    para(c,'The original spacecraft and machinery, made into windows',42,143,1100,23)
    para(c,'14 inventory screen types / 11 machines + 3 vehicle inventories / 1 shared configuration-drawer proposal',42,191,1100,14,TEAL,True)
    para(c,'Keep the native metal housings, distinctive slot wells, pipes and backlit instruments. Add familiar SIU controls, remove the embedded player inventory and close the shell with a complete matching bottom rim.',42,232,1090,15)
    for x,key,name in [(42,'ad-astra-nasa-workbench','Original rocket-building chassis'),(610,'ad-astra-solar-panel','Native readouts and attached controls')]:
        c.setFillColor(HexColor('#edf4f4'));c.roundRect(x,H-315-380,539,380,10,fill=1,stroke=0)
        picture(c,designs[key]['mock'],x+10,326,519,320)
        para(c,name,x+18,662,503,13,TEAL,True)
    para(c,'Every main comparison uses a real native game capture on the left and a proposed window on the right, at the same GUI-pixel scale. Planet selection and the guidebook stay native; tiers and repeated states are accounted for in the appendix.',42,724,1090,13)
    foot(c,1);c.showPage()

def configuration(c,coal,row,page):
    ref=json.loads((TMP/'configuration-reference.json').read_text(encoding='utf-8'));capture=ref['native_capture'];assert sha(ROOT/capture)==ref['sha256']
    # The native reset + six-side diagram replaces its embedded player inventory.
    src=grab(row,(7,95,169,183),capture);atlas=tex('container/coal_generator');drawer=metal_frame(atlas,0,102);drawer.alpha_composite(src,(7,7))
    parent,meta=make(row,SPECS[0],capture,True);top=meta['render_bounds'][1];root_h=meta['root_size'][1]
    joined=Image.new('RGBA',(parent.width,parent.height+104));joined.alpha_composite(parent);joined.alpha_composite(drawer,(0,parent.height+2))
    original_image,_=original(row,SPECS[0]);n=row['native'];old=n['capture'];row['native']['capture']=capture
    original_image,_=original(row,SPECS[0]);row['native']['capture']=old
    joined.save(OUT/'mockups/configuration-drawer.png');original_image.save(OUT/'native/configuration-energy.png')
    compare(c,'Machine side configuration - attached drawer',original_image,joined,
        'Native Gear mode replaces the player grid with a selected-role label, reset control and six direction buttons. The highlighted energy port is selected here.',
        'Gear opens an attached drawer below its own machine. Preserve native port selection, side modes and reset. The drawer follows the parent and has no separate X or movement controls.',page,
        'A real native configuration state / optional drawer opens only while configuring')
    return dict(native_capture=capture,native_sha256=ref['sha256'],selected_role='Energy',source=[7,95,162,88],drawer_size=[176,102],independent_close=False,independent_movement=False,implemented=False)

def appendix(c,rows,page):
    label(c,'APPENDIX / COVERAGE AND REPEATED INTERFACES',42,30);para(c,'Fourteen layouts, with their real slot roles',42,64,1100,29,bold=True)
    para(c,'The audited count is screen types, rather than every rocket tier or machine state.',42,111,1100,14,MUTED)
    y=156
    for i,spec in enumerate(SPECS):
        row=rows[spec['id']];slots=[s for s in row['native']['slots']if not s['player']]
        if spec.get('vehicle'):roles='8 cargo + 2 fuel ports'if spec['asset']=='rocket'else'16 cargo + 2 fuel ports'if spec['asset']=='rover'else'11 take-only cargo/recovery cells'
        elif spec['asset']=='nasa_workbench':roles='14 ingredients + 1 click-to-craft preview'
        else:roles=f"{len(slots)} native slot"+('s'if len(slots)!=1 else'')+', including charging'+(' / status + slider'if spec['asset']=='gravity_normalizer'else'')
        c.setFillColor(HexColor('#edf4f4'if i%2==0 else'#ffffff'));c.rect(42,H-y-31,1107,31,fill=1,stroke=0)
        para(c,spec['title'],54,y+9,350,11,bold=True);para(c,roles,420,y+9,700,11);y+=31
    label(c,'REPEATED TIERS AND LIVE STATES',42,638)
    para(c,'Rocket tiers 1-4 use the same RocketScreen layout in this release; the native Tier 1 reference represents that shared design. Nasa Workbench recipes reuse its fixed chassis. Configuring ports, charge/fill levels, progress, coverage and day/night displays are states of these screens, not extra storage windows.',42,662,1100,13)
    para(c,'Captured native machinery states include solar generation, gravity readouts, the water-pump warning and an actual Energy configuration page. Mockups reserve the full original gauge and animation envelopes; no new powered-operation test is claimed.',42,738,1100,12,MUTED)
    foot(c,page);c.showPage()

def native_scope(c,planets,page):
    label(c,'APPENDIX / KEEP NON-INVENTORY INTERFACES NATIVE',42,30);para(c,'Planet selection stays a full-screen interface',42,63,1100,29,bold=True)
    p=ROOT/planets['native']['capture'];assert sha(p)==planets['native']['sha256'];im=Image.open(p).convert('RGB')
    picture(c,im,42,127,700,505,scale=1)
    label(c,'NO INVENTORY WINDOW PROPOSED',778,141)
    para(c,'Planet navigation, destination details and space-station choices remain part of the original full-screen map. They have no machine or cargo slots in the audited menu.',778,172,371,16)
    label(c,'GUIDEBOOK AND WORLD INTERACTIONS',778,344)
    para(c,'The Astrodux/guidebook, overlays, launch interaction and other world controls keep their native flows. Recipe-viewer pages are not converted into new inventory windows.',778,375,371,15)
    label(c,'VEHICLE INVENTORY IS A DIFFERENT VIEW',42,681)
    para(c,'Rocket, Rover and Lander cargo menus are included in the main comparisons. Launching or navigating to a destination does not turn the planet map into a cargo window. Preserve original screens whenever that native flow is required.',42,709,1100,14)
    foot(c,page);c.showPage()

def sources(c,page,verification):
    label(c,'APPENDIX / SOURCES AND DESIGN CONTRACT',42,30);para(c,'What was checked before drawing',42,63,1100,29,bold=True)
    blocks=[
      ('Pinned native references','Minecraft 1.20.1 Fabric / Ad Astra 1.15.20. Fourteen inventory references and the planet screen come from the retained SIU-absent game audit. A new unmodified-publisher client supplies the Energy configuration reference.'),
      ('Native artwork and geometry','The publisher jar, menu registrations, published bytecode and matching-version source were inspected. All 88 native non-player slot envelopes are mapped, including exterior charging cells. Vehicle plates are drawn eight pixels left of their menu origin; the mockups account for that offset.'),
      ('No clipped machinery','Keep the full native working width, including pipes that meet the metal shell. The new bottom rim is taken from the safe final six texture rows, which contain no former inventory slots. Complete monitor and toolbar alpha masks exclude darkened world pixels.'),
      ('Interaction contract','Use applicable normal SIU controls, or More/Close where space is needed. Machine Gear/redstone/area controls and charging remain attached. Native slot restrictions, take-only recovery, Nasa click-to-craft, slider travel and selected-port configuration must survive implementation.'),
      ('Limits','These are proposed visuals, not implemented windows. This pass inspects native screen opening and one configuration state; it is not a full functional/gameplay suite. Later implementation must verify active fills, progress, all native actions, transfers and attached input routing.'),
    ];y=124
    for title,body in blocks:label(c,title,42,y);y=para(c,body,42,y+22,1100,13)+23
    assert y<680,y
    label(c,'PRIMARY REFERENCES',42,680)
    provenance=json.loads((TMP/'source-provenance.json').read_text(encoding='utf-8'))
    for i,(text,url)in enumerate([('Pinned publisher release',RELEASE),('Publisher source tree declaring 1.15.20',provenance['source'])]):
        yy=705+i*28;para(c,text,42,yy,1100,12,TEAL,True);c.linkURL(url,(42,H-yy-18,680,H-yy+2),relative=0,thickness=0)
    para(c,'Source branch snapshot predates the 1.15.21 version bump; published 1.15.20 bytecode and captures remain authoritative.',42,763,1100,9,MUTED)
    foot(c,page);c.showPage()

def main():
    (OUT/'mockups').mkdir(parents=True,exist_ok=True);(OUT/'native').mkdir(exist_ok=True);PDF.parent.mkdir(parents=True,exist_ok=True)
    rows=json.loads((TMP/'audit-cases.json').read_text(encoding='utf-8'));byid={r['id']:r for r in rows};designs={};manifest=[]
    for spec in SPECS:
        row=byid[spec['id']];assert sha(ROOT/row['native']['capture'])==row['native']['sha256']
        orig,om=original(row,spec);mock,meta=make(row,spec);orig.save(OUT/'native'/(spec['asset']+'.png'));mock.save(OUT/'mockups'/(spec['asset']+'.png'))
        designs[spec['id']]=dict(original=orig,mock=mock,meta=meta)
        manifest.append(dict(id=spec['id'],title=spec['title'],asset=spec['asset'],screen=row['native']['screen'],menu_class=row['native']['menu_class'],native_capture=row['native']['capture'],native_sha256=row['native']['sha256'],purpose=spec['does'],proposal=spec['design'],**om,**meta))
    assert len(manifest)==14 and len({c['screen']for c in manifest})==14
    assert sum(len(c['native_slots'])for c in manifest)==88
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('SegoeBold','C:/Windows/Fonts/segoeuib.ttf'))
    c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1);c.setTitle('Ad Astra - original UI and proposed SIU windows');c.setAuthor("Salt's Inventory Update / Project Everything")
    cover(c,designs);page=2
    for spec in SPECS:
        d=designs[spec['id']];compare(c,spec['title'],d['original'],d['mock'],spec['does'],spec['design'],page);page+=1
    config=configuration(c,designs[SPECS[0]['id']],byid[SPECS[0]['id']],page);page+=1
    appendix(c,byid,page);page+=1;native_scope(c,byid['own-adastra-planets'],page);page+=1
    verification=dict(screen_types=14,machine_types=11,vehicle_types=3,native_references=15,additional_configuration_reference=1,native_slot_envelopes=88,
        source_capture_hashes_checked=True,native_palette_preserved=True,complete_exterior_charging_cells=True,implementation_changed=False,functional_test_run=False)
    sources(c,page,verification);c.save();assert len(PdfReader(PDF).pages)==19
    record=dict(baseline='Minecraft 1.20.1 Fabric / Ad Astra 1.15.20',release=RELEASE,publisher_jar=str(JAR.relative_to(ROOT)),publisher_sha256=sha(JAR),
        source=json.loads((TMP/'source-provenance.json').read_text(encoding='utf-8')),pdf=str(PDF.relative_to(ROOT)),cases=manifest,configuration=config,verification=verification)
    (OUT/'manifest.json').write_text(json.dumps(record,indent=2),encoding='utf-8')
    contact=Image.new('RGB',(1500,4*280),'#edf4f4');draw=ImageDraw.Draw(contact);font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',18)
    for i,spec in enumerate(SPECS):
        im=designs[spec['id']]['mock'];factor=min(1.7,355/im.width,230/im.height);tile=im.resize((round(im.width*factor),round(im.height*factor)),N);x=i%4*375;y=i//4*280
        contact.paste(tile,(x+(375-tile.width)//2,y+35),tile);draw.text((x+10,y+10),spec['title'],font=font,fill=INK)
    contact.save(OUT/'mockup-contact.png');print(json.dumps(dict(pdf=str(PDF),pages=19,verification=verification),indent=2))

if __name__=='__main__':main()
