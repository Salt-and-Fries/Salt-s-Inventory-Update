"""Package the implemented Create windows and their retained functional evidence.

Images are crops of actual game captures. No mockups or recreated controls are
used in the implementation gallery. Run with --pdf after the PDF operation marker.
"""
from pathlib import Path
import argparse,hashlib,json,shutil,zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/create-window-implementation'
GAME=ROOT/'build/remaining-ui-audit/gameplay'
PDF=ROOT/'output/pdf/siu-create-1.20.1-implemented-windows.pdf'

FEATURES=[
 ('Toolboxes / mounted toolbox','Real transfers, split withdrawal, deposit-all, grouped counts and reopening.'),
 ('Schematic Table / Schematicannon','Actual file upload, output item, four replace policies, skip/protect, play/pause/stop and world printing.'),
 ('Crafting Blueprint','Ghost recipe, inferred output, actual world crafting, wrench reopening and clear.'),
 ('Frogport / Postbox','Package restrictions, insertion/withdrawal, address save and both connected-port modes.'),
 ('Redstone Requester','Ghost item, quantity, address and partial/full request policy saved to the block entity.'),
 ('Stock requests / categories','Filled network, search, basket quantities, actual addressed package, lock, hide/show, names, reorder and filter refund.'),
 ('Factory Gauge','Ghost monitor item saved to the original panel.'),
 ('List / Attribute / Package filters','Reference assignment, allow/deny and NBT modes, rule add/invert/scroll, address editing, clear and persistence.'),
 ('Linked Controller','All twelve frequency references, original key labels, save, reopen and clear.'),
 ('Train Schedule','All five instructions, all nine conditions, AND/OR, reorder/duplicate/delete, progress, both scroll axes, suggestions and save.'),
 ('Ammo / creative ammo / four fuzes','Valid/invalid ammunition, native capacity, tracer spacing, withdrawal; fuze limits, debounce, checkmark and SIU close saves.'),
 ('SIU window management','Move/focus, pin, lock, link, close, backing-item protection and small-screen scrolling at GUI scales 3/4.'),
]
LIMITS=[
 'Minecraft 1.20.1 only. Create 6.0.8 (Fabric port 6.0.8.1 build 1744), Big Cannons 5.11.4 and Steam n Rails 1.7.3 were exercised on Fabric and Forge.',
 'Shuffle Filter 1.0.2 is Forge-only here and uses the existing List Filter window; its equal/weighted choice modes were tested. A separate later-version weighted-filter screen was not backported.',
 'The Simulated/typewriter interfaces from the wider reference set have no applicable retained 1.20.1 release. Non-inventory controls remain native, as requested.',
 'Fabric used a production intermediary client with original upstream release bytes. Forge used the official named QA launcher and traceable SRG-to-named copies; a production obfuscated Forge launch was not independently exercised.',
 'These are focused functional tests of the affected Create windows, not a complete modpack/gameplay, dedicated-server, multiplayer, controller, localization or accessibility suite.',
 'Native text entry and key events were exercised through the game fixture. Raw operating-system modifier shortcuts, every custom key binding and the external schematic-folder OS action were not exhaustively tested.',
 'One Fabric QA run ended in a native GLFW access violation during Windows automation, outside Java code. Its log is retained; subsequent fresh game checks completed. No claim of crash-free long-term gameplay is made.',
]

def read(path):return json.loads(path.read_text(encoding='utf-8'))
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def relative(path):return str(Path(path).resolve().relative_to(ROOT)).replace('\\','/')

def collect():
    OUT.mkdir(parents=True,exist_ok=True)
    evidence={'scope':'Focused Create-family affected-window feature acceptance, Minecraft 1.20.1','loaders':{},'limits':LIMITS,'features':FEATURES,'history_preserved':True}
    for loader in ['fabric','forge']:
        checks={};history=[];groups={}
        for p in sorted(GAME.glob('1.20.1-'+loader+'-integrated-create-overhaul*/features/journal.json')):
            j=read(p)
            for check in j['checks']:
                c={**check,'journal':relative(p)};checks[c['id']]=c;history.append(c)
            for group,description in j.get('groups',{}).items():groups[group]={'description':description,'journal':relative(p)}
        failures=[c for c in checks.values() if not c['passed']]
        assert not failures,failures
        visual=lambda check:check['id'].startswith('final-') or check['id']=='controller-complete-key-labels-final-build'
        evidence['loaders'][loader]={'latest_assertions':len(checks),'functional_assertions':sum(not visual(c) for c in checks.values()),'capture_assertions':sum(visual(c) for c in checks.values()),'passed':True,'groups':groups,'checks':list(checks.values()),'history':history}
    pairs=[]
    fb=read(GAME/'1.20.1-fabric-integrated-create-overhaul13/final-captures.json')
    fg={c['id']:c for c in read(GAME/'1.20.1-forge-integrated-create-overhaul08/final-captures.json')['cases']}
    for c in fb['cases']:
        pair={'id':c['id'],'title':c['title'],'menu':c['menu'],'screens':{}}
        for loader,case in [('fabric',c),('forge',fg[c['id']])]:
            if c['id'] in ('controller','stock'):
                run='15' if loader=='fabric' else '09'
                p=GAME/f'1.20.1-{loader}-integrated-create-overhaul{run}'
                label='final-controller-polished' if c['id']=='controller' else 'final-stock-filled'
                case={**case,'image':str(p/'screenshots'/(label+'.png')),'evidence':str(p/'features'/(label+'.json'))}
            state=read(Path(case['evidence']))['client'];windows=[w for w in state['allWindows'] if w['menuId']==pair['menu']];assert len(windows)==1
            assert windows[0]['template']=='com.salts_inventory_update.compat.create.CreateWindows$Template'
            b=windows[0]['bounds'];source=Path(case['image']);im=Image.open(source);rx=im.width/state['guiWidth'];ry=im.height/state['guiHeight']
            crop=im.crop((round(b['x']*rx),round(b['y']*ry),round((b['x']+b['width'])*rx),round((b['y']+b['height'])*ry)))
            image_out=OUT/'screenshots'/loader/(pair['id']+'.png');image_out.parent.mkdir(parents=True,exist_ok=True);crop.save(image_out)
            raw=OUT/'evidence'/loader/(pair['id']+'.json');raw.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(case['evidence'],raw)
            pair['screens'][loader]={'crop':relative(image_out),'original':relative(source),'original_sha256':digest(source),'evidence':relative(raw)}
        pairs.append(pair)
    assert len({p['menu'] for p in pairs})==20
    artifacts=[]
    for loader,label in [('fabric','Fabric'),('forge','Forge')]:
        for family,base,name in [('core',ROOT/'versions','salts_inventory_update-1.20.1-'+label+'-0.1.6.jar'),('compatibility',ROOT/'compatibility/versions','salts_inventory_compat-1.20.1-'+label+'-0.1.3.jar')]:
            source=base/'1.20.1'/loader/'build/libs'/name;dest=OUT/'builds'/loader/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest)
            with zipfile.ZipFile(dest) as z:
                assert not any(n.startswith('com/siu_reference/') for n in z.namelist()),'QA fixture leaked into release'
            assert digest(source)==digest(dest)
            artifacts.append({'loader':loader,'family':family,'file':relative(dest),'sha256':digest(dest)})
    evidence.update({'unique_menu_classes':20,'gallery_views':len(pairs),'artifacts':artifacts,'gallery':pairs,'publisher_matrix':read(ROOT/'build/create-window-implementation/target-matrix.json')})
    (OUT/'acceptance-evidence.json').write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
    lines=['# Create-family windows - Minecraft 1.20.1','',
        'Implemented: 20 original menu classes on Fabric and Forge, with 25 captured interface states per loader. Create contributes 14 classes, Big Cannons five, and Steam n Rails one. Postboxes and creative ammunition containers reuse their native menu classes.','',
        '## Builds','',
        'Install the matching Core and Compatibility JARs together. These builds include the current workspace. No QA observer classes are packaged.','',
        *['- `'+a['file'].split('output/create-window-implementation/',1)[1]+'` - SHA-256 `'+a['sha256']+'`' for a in artifacts],'',
        '## Validation','',
        *['- '+loader.capitalize()+': '+str(v['functional_assertions'])+' unique functional assertions and '+str(v['capture_assertions'])+' template/capture assertions passed.' for loader,v in evidence['loaders'].items()],
        '- Focused JVM checks: CreateLayoutsTest, NativeStorageRolesTest, NativeTemplateContextsTest and NativeTemplateLayoutTest (15 test cases total). Both loader JAR builds and the source/module boundary check passed.',
        '- Earlier exploratory failures remain in the acceptance JSON history; only the most recent execution of each named assertion is used for acceptance. A passing screenshot assertion proves the selected template, not the behavior of every visible control.','',
        'The JSON contains exact journals, original screenshot paths and hashes, cropped-image lineage, publisher versions, and the feature matrix. Source journals contain authoritative inventory/block-entity snapshots.','',
        '## Tested behavior','',*['- **'+title+':** '+body for title,body in FEATURES],'',
        '## Scope and limits','',*['- '+line for line in LIMITS],'',
        '## Repeating focused checks','',
        'Use the disposable original-menu observer, production Fabric preparer and Forge named QA launcher retained in the repository. With a live fixture, run `functional-tests/create-window-overhaul/suite.py <output-directory> --group <group>`. The journal is append-only by assertion; its `complete:false` flag identifies an individual development run, while this aggregate records final acceptance across runs.','',
        'The QA fixture seeds world/items, then uses ordinary block/item/entity opening and the original input methods through SIU mappings. It does not construct fake menus or substitute test widgets. `capture.py` records the menu gallery.','',
        'No unrelated full-modpack suite, commit or push was performed.','']
    (OUT/'README.md').write_text('\n'.join(lines),encoding='utf-8')
    return evidence

def build_pdf(data):
    from reportlab.pdfgen import canvas
    from reportlab.lib.colors import HexColor
    from reportlab.lib.pagesizes import landscape,A4
    from reportlab.lib.utils import ImageReader
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    from reportlab.platypus import Paragraph
    from reportlab.lib.styles import ParagraphStyle
    pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'))
    pdfmetrics.registerFont(TTFont('SegoeBold','C:/Windows/Fonts/segoeuib.ttf'))
    PDF.parent.mkdir(parents=True,exist_ok=True);w,h=landscape(A4);c=canvas.Canvas(str(PDF),pagesize=(w,h),pageCompression=1)
    c.setTitle('SIU Create windows - Minecraft 1.20.1 implementation and verification')
    teal=HexColor('#267c80');ink=HexColor('#24383b');muted=HexColor('#617175');panel=HexColor('#eef4f3')
    def text(t,x,y,size=10,font='Segoe',color=ink):c.setFillColor(color);c.setFont(font,size);c.drawString(x,y,t)
    def para(t,x,y,width,size=10,color=ink):
        p=Paragraph(t,ParagraphStyle('body',fontName='Segoe',fontSize=size,leading=size*1.35,textColor=color));_,hh=p.wrap(width,1000);p.drawOn(c,x,y-hh);return y-hh
    page=0
    def start(title,subtitle):
        nonlocal page;page+=1;c.setFillColor(HexColor('#ffffff'));c.rect(0,0,w,h,fill=1,stroke=0)
        text('SALT\'S INVENTORY UPDATE / CREATE FAMILY',36,h-31,8.5,'SegoeBold',teal);text(title,36,h-61,23,'SegoeBold');text(subtitle,36,h-82,10,color=muted)
        c.setStrokeColor(HexColor('#d9e4e2'));c.line(36,32,w-36,32);text('Minecraft 1.20.1 | Actual game captures | 29 October 2026',36,18,8,color=muted);text(str(page),w-49,18,8,color=muted)
    def end():c.showPage()
    start('Implemented windows and functional verification','20 menu classes across Create, Big Cannons and Steam n Rails; Forge Shuffle Filter shares its native Filter screen.')
    text('IMPLEMENTED ON FABRIC + FORGE',36,h-121,11,'SegoeBold',teal)
    y=para('The approved overhaul is implemented using the original menus, widgets, item ownership and native packet handlers. SIU supplies the window frame and composition, while native actions continue to configure and process the real block entities and held items.',36,h-137,w-72,11)
    y=para('The gallery contains 25 actual interface states on each loader, including nested editors and shared-menu variants. Images are cropped from retained in-game screenshots; they are not mockups.',36,y-14,w-72,11)
    for i,(loader,title) in enumerate([('fabric','Fabric production runtime'),('forge','Forge named QA runtime')]):
        x=36+i*390;c.setFillColor(panel);c.roundRect(x,246,375,106,8,fill=1,stroke=0)
        text(title,x+14,329,14,'SegoeBold');n=data['loaders'][loader]
        text(str(n['functional_assertions'])+' unique functional assertions passed',x+14,302,12,'SegoeBold',teal)
        text(str(n['capture_assertions'])+' menu/template capture checks passed',x+14,280,10)
        text('Latest result per assertion; earlier failures retained.',x+14,261,8.5,color=muted)
    y=para('Verified outcomes include real schematic upload and printing, an addressed stock order that removes items and creates a package, actual blueprint crafting, held-item save/reopen, native ghost semantics, nested schedule editors and small-screen operation.',36,224,w-72,11)
    y=para('The implementation also fixes scoped native payload routing, final-save ordering, native-screen rendering context, grouped/virtual storage roles and mounted-toolbox source validity. The integration uses the existing public SIU boundary.',36,y-15,w-72,11)
    para('Builds and machine-readable evidence are saved beside this report in output/create-window-implementation/. Install the matching Core and Compatibility JARs together. Full hashes and source screenshot links are in acceptance-evidence.json.',36,y-15,w-72,10,color=muted)
    end()
    start('Feature coverage','Focused checks of the affected menus and shared SIU window controls. Both loaders passed the listed acceptance paths.')
    y=h-110
    for title,body in FEATURES:
        text(title,36,y,10,'SegoeBold');y=para(body,265,y+2,w-301,9.2)-8
    end()
    start('Scope and coverage limits','Read these alongside the passing feature matrix. The original evidence and exploratory failures remain available.')
    y=h-115
    for i,body in enumerate(LIMITS,1):
        text(str(i).zfill(2),36,y,11,'SegoeBold',teal);y=para(body,66,y+3,w-102,10.2)-17
    end()
    cases=[p for p in data['gallery'] if p['id']!='stock']
    sheets=[cases[index:index+2] for index in range(0,len(cases),2)]+[[next(p for p in data['gallery'] if p['id']=='stock')]]
    for rows in sheets:
        tall=rows[0]['id']=='stock'
        start('Implementation gallery','Fabric on the left. Forge on the right. Native visual identity and controls are retained inside SIU windows.')
        for row,p in enumerate(rows):
            top=h-108-row*222;text(p['title'],36,top,12,'SegoeBold');text(p['menu'],w-330,top,8,color=muted)
            for col,loader in enumerate(['fabric','forge']):
                box_h=371 if tall else 178;image_h=341 if tall else 148
                x=36+col*390;iy=top-box_h-15;c.setFillColor(panel);c.roundRect(x,iy,375,box_h,6,fill=1,stroke=0)
                text(loader.capitalize(),x+10,top-28,8.5,'SegoeBold',teal)
                im=Image.open(ROOT/p['screens'][loader]['crop']);scale=min(349/im.width,image_h/im.height)
                dw,dh=im.width*scale,im.height*scale;c.drawImage(ImageReader(im),x+(375-dw)/2,iy+8+(image_h-dh)/2,dw,dh,mask='auto')
        end()
    c.save()
    return PDF

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--pdf',action='store_true');a=p.parse_args();d=collect()
    print(json.dumps({'menus':d['unique_menu_classes'],'views':d['gallery_views'],'checks':{k:v['latest_assertions'] for k,v in d['loaders'].items()},'builds':len(d['artifacts'])}))
    if a.pdf:print(build_pdf(d))
