"""Verify native artwork and publish five actual-game Create window comparisons."""
from pathlib import Path
from io import BytesIO
from zipfile import ZipFile
import argparse,hashlib,json,shutil
from PIL import Image,ImageDraw,ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4,landscape
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
SPECS=[
    dict(id='toolbox',ref='toolbox',name='Toolbox',origin=30,width=188,height=171,asset='toolbox',uv=(0,0),note='The native ring of compartments and deposit button stay together. The complete original footer is restored where the separate player inventory used to overlap it.'),
    dict(id='table',ref='schematic-table',name='Schematic Table',origin=0,width=214,height=85,asset='schematics',uv=(10,139),note='The refresh button stays outside the native frame. Uploading and Finished titles remain native. Refresh-from-empty now registers the filename label; long display names stay inside the field.'),
    dict(id='blueprint',ref='blueprint',name='Crafting Blueprint',origin=0,width=179,height=109,asset='curiosities_2',uv=(0,109),note='The original grid, arrow, overlapping display cells, clear action and footer stay in place. SIU uses its compact title-controls menu to preserve the complete title at the native width.'),
    dict(id='filter',ref='filter',name='List Filter',origin=0,width=214,height=99,asset='filters',uv=(0,0),note='The native parchment, two rows of reference cells and original allow/deny, data, clear and confirm controls keep their original positions.'),
    dict(id='attribute',ref='attribute',name='Attribute Filter',origin=0,width=241,height=85,asset='filters',uv=(0,99),note='The native selector, reference cell, add/invert actions, mode controls and rule count stay intact. The complete parchment companion also fits the smallest GUI viewport.'),
]
def read(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def write(path,data):path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def publish(runtime):
    dest=ROOT/'output/create-native-five';dest.mkdir(parents=True,exist_ok=True)
    fixture=read(ROOT/'build/create-window-implementation/fabric-fixture.json')
    jar=ROOT/next(a['path'] for a in fixture['artifacts'] if 'create' in a['mod_ids'])
    checks=[];pairs=[]
    with ZipFile(jar) as z:
        for s in SPECS:
            ref=ROOT/'output/create-native-comparison-review/pairs'/s['ref']
            original=read(ref/'original-evidence.json')['client'];assert original['siu_loaded'] is False
            fixed=read(runtime/'features'/(s['id']+'-empty.json'))['client'];w=fixed['allWindows'][0];b=w['bounds'];n=original['bounds'];assert fixed['siu_loaded']
            assert (b['width'],b['height'])==(s['width'],s['height'])
            old=Image.open(ref/'original-full.png').convert('RGB');new=Image.open(runtime/'screenshots'/(s['id']+'-empty.png')).convert('RGB')
            a=old.crop(((n['x']+s['origin'])*2,n['y']*2,(n['x']+s['origin']+s['width'])*2,(n['y']+s['height'])*2))
            f=new.crop((b['x']*2,b['y']*2,(b['x']+s['width'])*2,(b['y']+s['height'])*2))
            atlas=Image.open(BytesIO(z.read('assets/create/textures/gui/'+s['asset']+'.png'))).convert('RGBA');u,v=s['uv'];art=atlas.crop((u,v,u+s['width'],v+s['height']))
            mask=art.getchannel('A').resize(a.size,Image.Resampling.NEAREST)
            points=[(x,y) for y in range(28,s['height']*2) for x in range(s['width']*2) if mask.getpixel((x,y))==255 and not(s['id']=='toolbox' and y>=294 and x<292)]
            different=sum(a.getpixel(p)!=f.getpixel(p) for p in points)
            assert different==0,(s['id'],'changed native body pixels',different)
            bevel=[(x,y) for y in [0,1,2,3,26,27] for x in range(s['width']*2) if mask.getpixel((x,y))==255]
            bevel_diff=sum(a.getpixel(p)!=f.getpixel(p) for p in bevel)
            assert bevel_diff==0,(s['id'],'changed title bevel',bevel_diff)
            row={'window':s['id'],'native_body_pixels':len(points),'different_body_pixels':different,'native_bevel_pixels':len(bevel),'different_bevel_pixels':bevel_diff}
            if s['id']=='toolbox':
                atlas2=art.resize(a.size,Image.Resampling.NEAREST).convert('RGB')
                restored=[(x,y) for y in range(294,342) for x in range(292) if mask.getpixel((x,y))==255]
                bad=sum(atlas2.getpixel(p)!=f.getpixel(p) for p in restored);assert bad==0,('Toolbox restored footer',bad)
                row.update(restored_footer_pixels=len(restored),different_restored_footer_pixels=bad)
            checks.append(row)
            folder=dest/'pairs'/s['id'];folder.mkdir(parents=True,exist_ok=True)
            for file in ['original-full.png','original-ui.png','original-evidence.json']:shutil.copy2(ref/file,folder/file)
            shutil.copy2(runtime/'screenshots'/(s['id']+'-empty.png'),folder/'fixed-full.png')
            shutil.copy2(runtime/'features'/(s['id']+'-empty.json'),folder/'fixed-evidence.json')
            ex=[r for r in w['composition']['regions'] if r.get('exterior')]
            right=max([b['width']]+[r['x']+r['source']['width'] for r in ex]);bottom=max([b['height']]+[r['y']+r['source']['height'] for r in ex])
            new.crop((max(0,(b['x']-4)*2),max(0,(b['y']-4)*2),min(new.width,(b['x']+right+4)*2),min(new.height,(b['y']+bottom+4)*2))).save(folder/'fixed-ui.png')
            pairs.append({**s,'folder':str(folder),'runtime':str(runtime.resolve())})
    captures=dest/'captures';captures.mkdir(exist_ok=True)
    for name in ['toolbox-filled','table-refreshed','table-finished','blueprint-filled','blueprint-title-menu','filter-filled','attribute-filled']+[s['id']+'-gui4' for s in SPECS]:
        for sub,suffix in [('screenshots','.png'),('features','.json')]:shutil.copy2(runtime/sub/(name+suffix),captures/(name+suffix))
    journal=read(runtime/'features/journal.json');latest={c['id']:c for c in journal['checks']};assert journal['complete'] and all(c['passed'] for c in latest.values())
    shutil.copy2(runtime/'features/journal.json',dest/'check-history.json');shutil.copy2(runtime/'staged-artifacts.json',dest/'runtime-artifacts.json')
    artifacts=[];stage=read(runtime/'staged-artifacts.json');mods=Path(read(runtime/'launch.json')['cwd'])/'mods'
    for loader in ['fabric','forge']:
        target=dest/'builds'/loader;target.mkdir(parents=True,exist_ok=True)
        for project,mod,version,family in [('versions','salts_inventory_update','0.1.6','core'),('compatibility/versions','salts_inventory_compat','0.1.3','compatibility')]:
            file=f'{mod}-1.20.1-{loader.title()}-{version}.jar';source=mods/file if loader=='fabric' else ROOT/project/'1.20.1'/loader/'build/libs'/file
            if loader=='fabric':assert sha(source)==next(a['sha256'] for a in stage['artifacts'] if a['family']==family)
            shutil.copy2(source,target/file);artifacts.append({'loader':loader,'file':str((target/file).relative_to(dest)),'sha256':sha(source)})
    acceptance={'scope':'Five additional native Create frames on 1.20.1; Core and public API unchanged by this task','runtime':'Production Fabric; unmodified audited Create 6.0.8.1+build.1744-mc1.20.1 JAR',
        'runtime_evidence':str(runtime.resolve()),'checks':list(latest.values()),'checks_passed':True,'pixel_checks':checks,'builds':artifacts,
        'validation':f'{len(latest)} focused runtime checks and two native layout geometry tests passed; Fabric remapJar and Forge jar/reobfJar passed',
        'limits':'Not the complete Create functional/gameplay suite. Forge build-checked only. OS folder-launch button retained, but external folder launching not invoked.',
        'intentional_changes':'Left-aligned native titles, SIU management controls, independent player inventory, full restored Toolbox footer, and a compact Blueprint title menu. Table display-only filename trimming does not alter uploaded names or data.',
        'pixel_scope':'Opaque native body and title bevel. Excludes moved titles, new management controls, transparent world pixels and animated companions. Toolbox footer compared against upstream atlas where originally obscured by player inventory.'}
    write(dest/'acceptance.json',acceptance);write(dest/'manifest.json',pairs)
    (dest/'README.md').write_text(f'''# Five additional native Create windows - Minecraft 1.20.1

Implemented Toolbox, Schematic Table, Crafting Blueprint, List Filter and Attribute Filter using their original frame, title material, body, layout, footer and outboard models. SIU management controls occupy each native title strip. Blueprint uses the existing compact controls menu so its full title fits. Player inventory remains a separate SIU window.

Toolbox restores the native footer where the old player inventory overlapped it. Schematic Table keeps its refresh control outside the frame, preserves Uploading/Finished titles, restores the native filename label after refresh-from-empty, and trims only long display labels inside the existing field. The original complete filename is still used for uploads.

{len(latest)} focused in-game checks passed on production Fabric, covering the changed menus' native actions, persistence and GUI scale 4. Two native geometry tests passed. Fabric and Forge builds passed; Forge was build-checked only. This is not the complete Create functional/gameplay suite. External OS folder launching was not invoked.

`pairs/` contains original SIU-absent references, final full captures and complete window crops. `captures/` contains uncropped live screens with populated interfaces, separate inventory windows, the compact title popup and GUI scale 4. `acceptance.json` records pixel checks and artifact hashes. The comparison PDF is in `output/pdf/siu-create-native-five-comparisons.pdf` in the project workspace.

`builds/` contains Core 0.1.6 and updated Compatibility 0.1.3 for Fabric and Forge. Install the pair for your loader, replacing existing copies. No Core or public API changes were made for this work. Previously implemented Schematicannon retains its native-frame style; other Create-family designs are unchanged.
''',encoding='utf-8')
    return dest,pairs,acceptance

def wrap(c,text,x,y,width,font='Body',size=10,leading=15):
    words=text.split();line='';c.setFont(font,size)
    for word in words:
        candidate=(line+' '+word).strip()
        if pdfmetrics.stringWidth(candidate,font,size)>width and line:c.drawString(x,y,line);y-=leading;line=word
        else:line=candidate
    if line:c.drawString(x,y,line);y-=leading
    return y

def picture(c,path,x,y,w,h):
    im=Image.open(path);scale=min(w/im.width,h/im.height);iw,ih=im.width*scale,im.height*scale
    c.drawImage(ImageReader(im),x+(w-iw)/2,y+(h-ih)/2,iw,ih,mask='auto')

def report(dest,pairs,acceptance):
    pdf=ROOT/'output/pdf/siu-create-native-five-comparisons.pdf';pdf.parent.mkdir(parents=True,exist_ok=True)
    pdfmetrics.registerFont(TTFont('Body','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('Heading','C:/Windows/Fonts/segoeuib.ttf'))
    W,H=landscape(A4);c=canvas.Canvas(str(pdf),pagesize=(W,H));c.setTitle('SIU - Five additional native Create windows - 1.20.1');c.setAuthor('Salt\'s Inventory Update')
    for index,p in enumerate(pairs,1):
        c.setFillColor(colors.HexColor('#137d80'));c.setFont('Heading',9);c.drawString(32,H-30,'SIU / CREATE NATIVE WINDOWS / IMPLEMENTED IN GAME')
        c.setFillColor(colors.HexColor('#203638'));c.setFont('Heading',23);c.drawString(32,H-63,p['name'])
        c.setFont('Body',10);c.drawString(32,H-84,'Minecraft 1.20.1 - Fabric production capture - original Create resources and controls')
        col=(W-84)/2;top=H-119;box_y=130;box_h=top-box_y-20
        for x,label in [(32,'ORIGINAL CREATE / SIU ABSENT'),(52+col,'IMPLEMENTED SIU / NATIVE FRAME')]:
            c.setFillColor(colors.HexColor('#137d80'));c.setFont('Heading',9);c.drawString(x,top,label)
            c.setFillColor(colors.HexColor('#eff4f4'));c.roundRect(x,box_y,col,box_h,8,fill=1,stroke=0)
        folder=Path(p['folder']);picture(c,folder/'original-ui.png',42,box_y+10,col-20,box_h-20);picture(c,folder/'fixed-ui.png',62+col,box_y+10,col-20,box_h-20)
        c.setFillColor(colors.HexColor('#203638'));wrap(c,p['note'],32,108,W-64,size=10)
        c.setFillColor(colors.HexColor('#66797a'));c.setFont('Body',8);c.drawString(32,43,'Player inventory moves into its own SIU window. Images are actual game captures; no replacement UI is drawn into them.')
        c.setStrokeColor(colors.HexColor('#cedcdd'));c.line(32,32,W-32,32);c.drawString(32,19,'Native style preserved / title and management adapted');c.drawRightString(W-32,19,f'{index} / 6');c.showPage()
    c.setFillColor(colors.HexColor('#137d80'));c.setFont('Heading',9);c.drawString(32,H-30,'SIU / FOCUSED VALIDATION')
    c.setFillColor(colors.HexColor('#203638'));c.setFont('Heading',23);c.drawString(32,H-63,'Native features retained')
    y=H-98
    for text in ['Toolbox: real insert, split withdrawal, deposit-all, original footer confirmation, reopen and title dragging.',
        'Schematic Table: original exterior refresh, selector wheel, refresh-from-empty filename label, real upload and output withdrawal. Long filenames stay inside the field; uploaded names and data remain complete.',
        'Crafting Blueprint: ghost references preserve owned items, inferred recipe output, actual world crafting, wrench reopen and clear. The compact title menu exposes the management controls.',
        'List Filter: preserved ghost reference ownership, allow/deny and data modes, save/reopen and clear.',
        'Attribute Filter: regular/inverted rules, OR/AND/deny mode persistence, reopen, native count and clear.',
        'Every changed window: original dimensions at GUI scale 4, complete frame/companion extents and native confirmation or close.']:
        y=wrap(c,text,32,y,W-64,size=11,leading=17)-12
    y-=5;c.setFont('Heading',12);c.drawString(32,y,f"{len(acceptance['checks'])} focused runtime checks + 2 layout geometry tests passed");y-=25
    total=sum(r['native_body_pixels']+r['native_bevel_pixels']+r.get('restored_footer_pixels',0) for r in acceptance['pixel_checks'])
    y=wrap(c,f'{total:,} selected native-art pixels matched exactly. Pixel checks cover opaque body artwork, native title bevels and the restored Toolbox footer. Moved titles, added SIU controls, world pixels and animated companions are excluded.',32,y,W-64,size=10)-12
    wrap(c,'Fabric was tested in game. Fabric and Forge builds passed; Forge was build-checked only. These are focused checks of the five changed interfaces, not a complete Create gameplay suite. The OS folder launcher was not activated. Core and the public API were not changed.',32,y,W-64,size=10)
    c.setFillColor(colors.HexColor('#66797a'));c.setFont('Body',8);c.drawString(32,19,'Full captures, observations, pixel-check results and matching builds accompany this report.');c.drawRightString(W-32,19,'6 / 6');c.save()
    print(json.dumps({'pdf':str(pdf),'output':str(dest),'focused_checks':len(acceptance['checks']),'selected_pixels':total}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('runtime',type=Path);a=p.parse_args();dest,pairs,acceptance=publish(a.runtime);report(dest,pairs,acceptance)
