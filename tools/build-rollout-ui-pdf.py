"""Build the evolving, real-capture rollout atlas; unfinished coverage stays explicit."""
from pathlib import Path
import argparse,collections,hashlib,json,math,re,textwrap
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A3,landscape
from reportlab.lib.utils import ImageReader
from rollout_capture_provenance import same_ui_binary

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/pdf/siu-all-target-ui-verification.pdf'
WORK=ROOT/'build/all-target-rollout/pdf';WORK.mkdir(parents=True,exist_ok=True)
W,H=landscape(A3);M=42;GAP=24;PW=(W-2*M-GAP)/2
INK='#183D40';TEAL='#247B80';PALE='#EDF4F4';GRAY='#627276'
FINAL=False
NAMES={'accessories':'Accessories','bountiful':'Bountiful','toms-config':"Tom's Trading Network - Configuration",
 'toms-customer':"Tom's Trading Network - Customer",'minecells-crafter':'Mine Cells - Cell Crafter',
 'minecells-recipe-picker':'Mine Cells - Recipe Picker','aircraft-cargo-airship':'Immersive Aircraft - Cargo Airship',
 'trading-post':'Trading Post','travelers-backpack':"Traveler's Backpack",'travelers-tools-drawer':"Traveler's Backpack - Tools",
 'flower-pouch':'Botania - Flower Pouch','petal-pouch':'Botania - Petal Pouch','bauble-box':'Botania - Bauble Box',
 'universal-grave-storage':'Universal Graves - Grave Storage'}
NAMES.update({'easy-anvils':'Easy Anvils','easy-magic':'Easy Magic','visual-workbench':'Visual Workbench',
 'feeding-trough':'Animal Feeding Trough','bartering-station':'Bartering Station',
 'archeology-table':'Better Archeology - Archeology Table','nether-chest':'Nether Chested',
 'graveyard-ossuary':'The Graveyard - Ossuary','villagersplus-alchemist':'VillagersPlus - Alchemist Table',
 'villagersplus-ore-grinder':'VillagersPlus - Ore Grinder','toms-filing-cabinet':"Tom's Storage - Filing Cabinet",
 'toms-level-emitter':"Tom's Storage - Level Emitter",'toms-storage-terminal':"Tom's Storage - Storage Terminal",
 'toms-inventory-link':"Tom's Storage - Inventory Link",'toms-inventory-configurator':"Tom's Storage - Inventory Configurator",
 'toms-crafting-terminal':"Tom's Storage - Crafting Terminal",'spell-binding':'Spell Engine - Spell Binding',
 'sophisticated-chest':'Sophisticated Storage - Chest','sophisticated-barrel':'Sophisticated Storage - Barrel',
 'sophisticated-decoration':'Sophisticated Storage - Decoration Table','travelers-block':"Traveler's Backpack - Placed Backpack",
 'farmers-cooking-pot':"Farmer's Delight - Cooking Pot"})
NAMES.update({'wands-palette':'Building Wands - Palette','toms-item-filter':"Tom's Storage - Item Filter",
 'toms-tag-item-filter':"Tom's Storage - Tag Filter",'sophisticated-backpack':'Sophisticated Backpacks',
 'transmog':'Transmog - Transmogrification Table','rune-crafting':'Runes - Crafting Altar',
 'waystones-warp-plate':'Waystones - Warp Plate','betterend-smelter':'BetterEnd - End Stone Smelter'})
NAMES.update({'universal-sawmill':'Sawmill','illager-imbuing':'Illager Invasion - Imbuing Table'})
NAMES['alloy-forge']='Alloy Forgery - Alloy Forge'
NAMES.update({'betterarcheology-fossil-inventory':'Better Archeology - Villager Fossil Inventory',
 'deep-aether-combiner':'Deep Aether - Combiner','alloyed-forge':'Alloyed - Forge',
 'curios':'Curios - Equipment','travelers-settings':"Traveler's Backpack - Settings",
 'sophisticated-backpack-settings':'Sophisticated Backpacks - Settings',
 'sophisticated-storage-settings':'Sophisticated Storage - Settings',
 'sophisticated-limited-barrel-settings':'Sophisticated Storage - Limited Barrel Settings'})
NAMES['guard-villager']='Guard Villagers - Guard Inventory'
NAMES['trinkets-equipment']='Trinkets - Character Equipment Attachment'
NAMES.update({'tierify-reforge':'Tierify - Reforging','tieredz-reforge':'TieredZ - Reforging','curios-legacy':'Curios - Legacy Equipment Menu','merequester-wireless-terminal':'ME Requester - Wireless Terminal','blue-skies-arcs':'Blue Skies - Arcs Equipment','blue-skies-toolbox':'Blue Skies - Tool Box','spawn-octoduct':'Spawn - Octoduct','spawn-pigment-shifter':'Spawn - Pigment Shifter','bountiful-decree-analyzer':'Bountiful - Decree Analyzer'})
NAMES.update({'prominent-forge-upgrade':'Prominent - Primordial Forge Upgrade','prominent-forge-armor':'Prominent - Armor Selection','prominent-cinder-shop':'Prominent - Cinder Shop (Buy)','prominent-cinder-shop-sell':'Prominent - Cinder Shop (Sell)','prominent-cinder-shop-trade':'Prominent - Cinder Shop (Trade)'})
NAMES.update({'minecells-unbreakable-crafter':'Mine Cells - Unbreakable Cell Crafter',
 'minecells-unbreakable-crafter-recipe-picker':'Mine Cells - Unbreakable Crafter Recipe Picker'})
NAMES.update({'bb-spawn-hamster':'Spawn - Hamster','bf-moa-clean':'Protect Your Moa - Moa Inventory',
 'own-supp-red-merchant':'Supplementaries - Red Merchant','own-supp-cannon':'Supplementaries - Cannon',
 'own-supp-present':'Supplementaries - Present','own-supp-trapped-present':'Supplementaries - Trapped Present',
 'own-bf-bag-of-spoils-aligned':'Blue Skies - Bag of Spoils','own-bf-camel-real-aligned':'Blue Skies - Camel Inventory',
 'own-bf-umvuthi-aligned':"Mowzie's Mobs - Umvuthi",'own-bf-minion-trade-real':"Mowzie's Mobs - Umvuthana",'bf-sculptor-clean':"Mowzie's Mobs - Sculptor"})
NAMES['waystones-modifiers']='Waystones - Warp Modifiers'
for name in ['brigg','caravel','cog','dhow','drakkar','galleon','galley']:
 NAMES['smallships-'+name]='Small Ships - '+name.title()
NAMES.update({'archon-scripture-table':'Archon Renewed - Scripture Table',
 'item-archon-experience-pouch':'Archon Renewed - Experience Pouch',
 'smitherz-smither':'SmitherZ - Link Gear','smitherz-grinder':'SmitherZ - Grinder',
 'drawer-single':'Extended Drawers Addon - Single Drawer',
 'drawer-double':'Extended Drawers Addon - Double Drawer',
 'drawer-quad':'Extended Drawers Addon - Quad Drawer'})
for kind in ['plow','seed-drill','supply-cart','hand-cart','reaper','wagon','animal-cart']:NAMES['niftycarts-'+kind]='NiftyCarts - '+kind.replace('-',' ').title()
for i in range(1,4):NAMES[f'niftycarts-wagon-{i}']=f'NiftyCarts - Wagon ({i*36} slots)'
NAMES.update({'bumblezone-flower':'The Bumblezone - Crystalline Flower',
 'bumblezone-cocoon':'The Bumblezone - Honey Cocoon','bumblezone-briefcase':'The Bumblezone - Buzzing Briefcase',
 'bumblezone-flower-enchanting':'The Bumblezone - Flower Enchantment Selection'})
for i in range(1,4):NAMES[f'wands-magic-bag-{i}']=f'Building Wands - Magic Bag Tier {i}'
for name in ['airship','biplane','cargo-airship','gyrodyne','quadrocopter','bamboo-hopper','warship']:
 NAMES['aircraft-'+name]='Immersive Aircraft - '+name.replace('-',' ').title()
for i in range(1,5):NAMES[f'sophisticated-limited-barrel-{i}']=f'Sophisticated Storage - Limited Barrel {i}'
for name in ['inscriber','cell-workbench','io-port','condenser','molecular-assembler','vibration-chamber','drive',
             'sky-stone-chest','wireless-access-point','spatial-io-port','spatial-anchor','terminal','crafting-terminal',
             'pattern-encoding-terminal','pattern-access-terminal','export-bus','import-bus','storage-bus',
             'formation-plane','level-emitter','energy-level-emitter','interface-part','pattern-provider-part',
             'network-tool','quartz-cutting-knife','portable-item-cell','portable-fluid-cell','wireless-terminal',
             'wireless-crafting-terminal','controller-network-status','network-tool-status','crafting-cpu',
             'quantum-network-bridge','priority','terminal-settings','crafting-status','stock-amount',
             'terminal-populated','craft-amount','craft-confirm','pattern-processing-patterns',
             'pattern-stonecutting-patterns','pattern-smithing-table-patterns']:
    NAMES['ae2-'+name]='Applied Energistics 2 - '+name.replace('-',' ').title()

def read(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def relative(path):
    try:return str(path.relative_to(ROOT)).replace('\\','/')
    except ValueError:return str(path)
def collect():
    pairs={}
    roots=[(ROOT/'build/remaining-ui-audit/gameplay/1.21.1-fabric-native-rollout-baseline02','native'),
           (ROOT/'build/remaining-ui-audit/gameplay/1.21.1-fabric-integrated-rollout-prod08','siu')]
    for folder,side in roots:
        for row in read(folder/'rollout-capture-journal.json'):
            if 'ui' in row:
                pairs.setdefault(('1.21.1','fabric',row['ui']),{})[side]={**row,'run':folder}
        if side=='native':
            path=folder/'screenshots/native-travelers-tools-drawer-1211-fabric.png'
            # This later real capture is retained in the observer's own history.
            if path.exists():
                row=next(r for r in reversed(read(folder/'observations.json')) if r.get('command',{}).get('capture')==path.stem)
                pairs.setdefault(('1.21.1','fabric','travelers-tools-drawer'),{})['native']={'screenshot':str(path),'state':row['client'],'run':folder}
    for folder,side in [(ROOT/'build/remaining-ui-audit/gameplay/1.21.1-neoforge-native-rollout-appbot-publisher-baseline01','native'),
                        (ROOT/'build/all-target-rollout/gameplay/1.21.1-neoforge-rollout-appbot-publisher-04','siu')]:
        for row in read(folder/'botania-pouches-capture-journal.json'):
            pairs.setdefault(('1.21.1','neoforge',row['id']),{})[side]={**row,'run':folder,'note':'Botania publisher snapshot 454-20260621.181850-47; Applied Botanics dependency baseline.'}
    for folder,side in [(ROOT/'build/remaining-ui-audit/gameplay/1.21.1-fabric-native-rollout-graves-native01','native'),
                        (ROOT/'build/remaining-ui-audit/gameplay/1.21.1-fabric-integrated-rollout-graves05','siu')]:
        for row in read(folder/'grave-entry-journal.json'):
            if row.get('command',{}).get('capture')==side+'-universal-grave-storage-1211-fabric':
                pairs.setdefault(('1.21.1','fabric','universal-grave-storage'),{})[side]={'screenshot':str(folder/'screenshots'/(row['command']['capture']+'.png')),'state':row['client'],'run':folder}
    ledger=ROOT/'build/all-target-rollout/ui-capture-ledger.json'
    if ledger.exists():
        for row in read(ledger):
            path=Path(row['screenshot'])
            if hashlib.sha256(path.read_bytes()).hexdigest()!=row['sha256']:
                raise ValueError('Capture changed after recording: '+str(path))
            pairs.setdefault((row['minecraft'],row['loader'],row['ui']),{})[row['side']]=row
    accepted={};issues=[];provenance=[]
    for key,sides in pairs.items():
        if set(sides)!={'native','siu'}:continue
        valid,check=same_ui_binary(sides)
        record={'minecraft':key[0],'loader':key[1],'ui':key[2],**check}
        (provenance if valid else issues).append(record)
        if valid:accepted[key]=sides
    (WORK/'comparison-input-provenance.json').write_text(json.dumps(provenance,indent=2))
    (WORK/'comparison-input-issues.json').write_text(json.dumps(issues,indent=2))
    return accepted

def bounds(row,side):
    state=row['state'];rects=[]
    if side=='native':
        b=state.get('bounds')
        if b:
            rects.append((b['x'],b['y'],b['x']+b['width'],b['y']+b['height']))
            if state.get('screen','').startswith('elocindev.prominentbase.client.gui.'):
                # Native Owo roots extend beyond their nominal container
                # rectangle; preserve the complete carousel, stone caps and
                # shop tabs rather than cropping to vanilla imageWidth.
                rects.append((b['x']-64,b['y']-45,b['x']+b['width']+64,b['y']+b['height']+12))
            if state.get('screen','').endswith('.AccessoriesExperimentalScreen'):
                # Owo places its character panel above the nominal vanilla
                # menu bounds; it is not an AbstractWidget or menu slot.
                rects.append((b['x']-28,b['y']-38,b['x']+b['width']+28,b['y']+b['height']+38))
            if row.get('ui','').startswith(('smitherz-','tieredz-')) or state.get('screen','').startswith(('net.smitherz.','net.tieredz.')):
                # LibZ draws its tabs directly, outside the menu rectangle;
                # they are not AbstractWidgets and cannot appear in that list.
                rects.append((b['x'],b['y']-28,b['x']+50,b['y']))
            if state.get('screen','').startswith('net.p3pp3rf1y.sophisticated'):
                # Native upgrade/settings tabs are drawn outside the menu and
                # are not all AbstractWidgets. Keep both complete tab rails.
                rects.append((b['x']-32,b['y'],b['x']+b['width']+36,b['y']+b['height']))
            if '.curios.' in state.get('screen',''):
                # Curios' own side rail/toggle is drawn outside the vanilla
                # inventory rectangle and is not always an AbstractWidget.
                rects.append((b['x']-32,b['y']-4,b['x']+b['width']+32,b['y']+b['height']+4))
            for s in state.get('slots',[]):
                x,y=s['x'],s['y']
                if -100<x<b['width']+100 and -100<y<b['height']+100:
                    rects.append((b['x']+x-1,b['y']+y-1,b['x']+x+18,b['y']+y+18))
        for widget in state.get('widgets',[]):
            if widget.get('visible',True):rects.append((widget['x'],widget['y'],widget['x']+widget['width'],widget['y']+widget['height']))
    else:
        windows=state.get('allWindows',state.get('windows',[]))
        equipment=any(w.get('menuId','').startswith('salts_inventory_compat:') and ('accessories' in w.get('menuId','') or 'curios' in w.get('menuId','')) for w in windows)
        for window in windows:
            b=window['bounds']
            rects.append((b['x'],b['y'],b['x']+b['width'],b['y']+b['height']) if equipment else (b['x']-25,b['y']-20,b['x']+b['width']+27,b['y']+b['height']+4))
            for region in (window.get('template') or {}).get('regions',[]):
                if region.get('exterior'):
                    # Exterior fragments are relative to the window itself,
                    # including native toolbars, recipe drawers and side tabs.
                    x=b['x']+region['x'];y=b['y']+region['y'];source=region['source']
                    rects.append((x,y,x+source['width'],y+source['height']))
            if window.get('definition','').endswith('.AEWindow'):
                # AE2 can wrap its native toolbar into several columns. These
                # widgets already use the dedicated window's screen coordinates;
                # a fixed margin can clip the outer columns in the report.
                for widget in window.get('nativeWidgets',[]):
                    if widget.get('visible',True):
                        rects.append((widget['x'],widget['y'],widget['x']+widget['width'],widget['y']+widget['height']))
        if row.get('ui')=='trinkets-equipment':
            for window in state.get('workspaceWindows',[]):
                if window.get('kind')=='CHARACTER':
                    b=window['bounds'];rects.append((b['x']-26,b['y']-4,b['x']+b['width']+26,b['y']+b['height']+4))
        if equipment:
            # The approved accessory panel is attached above Character. Keep
            # that whole parent in the comparison, rather than a clipped sliver.
            for window in state.get('workspaceWindows',[]):
                if window.get('kind')=='CHARACTER':
                    b=window['bounds'];rects.append((b['x'],b['y'],b['x']+b['width'],b['y']+b['height']))
    if not rects:return (0,0,state['guiWidth'],state['guiHeight'])
    return (max(0,min(r[0] for r in rects)-8),max(0,min(r[1] for r in rects)-8),
            min(state['guiWidth'],max(r[2] for r in rects)+8),min(state['guiHeight'],max(r[3] for r in rects)+8))

def crop(row,side):
    path=Path(row['screenshot']);path=path if path.is_absolute() else ROOT/path
    im=Image.open(path).convert('RGB');b=bounds(row,side)
    sx=im.width/row['state']['guiWidth'];sy=im.height/row['state']['guiHeight']
    box=(math.floor(b[0]*sx),math.floor(b[1]*sy),math.ceil(b[2]*sx),math.ceil(b[3]*sy))
    return im.crop(box),path,box

def text(c,string,x,y,size=11,color=INK,bold=False):
    string=re.sub(r'\u00a7[0-9a-fk-or]','',string,flags=re.IGNORECASE)
    c.setFillColor(colors.HexColor(color));c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.drawString(x,y,string)
def paragraph(c,string,x,y,width,size=12,leading=None):
    leading=leading or size*1.45
    # Measure words with the actual PDF font, so notes cannot overrun the page.
    words=string.replace('\n',' \n ').split(' ');line=''
    for word in words:
        while c.stringWidth(word,'Helvetica',size)>width:
            if line:text(c,line,x,y,size);y-=leading;line=''
            cut=max(1,int(len(word)*width/c.stringWidth(word,'Helvetica',size)))
            while c.stringWidth(word[:cut],'Helvetica',size)>width:cut-=1
            text(c,word[:cut],x,y,size);y-=leading;word=word[cut:]
        if word=='\n' or c.stringWidth((line+' '+word).strip(),'Helvetica',size)>width:
            text(c,line,x,y,size);y-=leading;line='' if word=='\n' else word
        else:line=(line+' '+word).strip()
    if line:text(c,line,x,y,size);y-=leading
    return y
def footer(c,page):
    c.setStrokeColor(colors.HexColor('#D3E0E0'));c.line(M,34,W-M,34)
    text(c,('UI confirmation' if FINAL else 'Working evidence')+' • Real game captures • Focused checks only',M,20,9,GRAY)
    c.setFont('Helvetica',9);c.drawRightString(W-M,20,str(page))
def heading(c,title,subtitle=''):
    text(c,'SIU / ALL-VERSION WINDOW ROLLOUT',M,H-39,11,TEAL,True)
    text(c,title,M,H-80,27,INK,True)
    if subtitle:text(c,subtitle,M,H-106,12,GRAY)

def main():
    global FINAL
    parser=argparse.ArgumentParser();parser.add_argument('--final',action='store_true');args=parser.parse_args()
    FINAL=args.final
    matrix=read(ROOT/'output/all-target-rollout/availability-and-registration.json')
    if args.final and any(r.get('inventory_ui_coverage') not in ['complete','appendix'] for r in matrix):
        raise SystemExit('Final PDF refused: inventoried UI comparison coverage is incomplete.')
    pairs=collect();OUT.parent.mkdir(parents=True,exist_ok=True)
    titles={}
    for candidate in read(ROOT/'output/all-target-rollout/ui-evidence-plan.json'):
        titles.setdefault((candidate['minecraft'],candidate['loader'],candidate['screen']),set()).add(candidate['project'])
    c=canvas.Canvas(str(OUT),pagesize=(W,H));c.setTitle('SIU all-version UI '+('confirmation' if FINAL else 'verification - working evidence'))
    c.setAuthor("Salt's Inventory Update");page=1
    heading(c,'Version and loader UI confirmation','All inventoried inventory interfaces pictured • 12 targets • Focused checks; full gameplay suite not run' if FINAL else 'WORKING DRAFT - capture and functional verification are still in progress')
    y=H-165
    for p in [f'This atlas contains {len(pairs)} original/SIU screenshot comparisons across the supported version and loader targets. Every inventoried in-scope container-screen class is represented; non-inventory interfaces and shared implementation classes are accounted for in the appendix.' if FINAL else f'This evolving atlas currently contains {len(pairs)} original/SIU screenshot comparisons. It is not the final all-UI confirmation.',
              'The rollout covers Minecraft 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 on their supported Fabric, Forge or NeoForge loaders. Availability is pinned to exact publisher binaries and recorded hashes.',
              'The latest complete packaging pass built all 24 updated Core/Compatibility JARs. Twenty-seven Core native contract tests and twelve Anvil/layout regressions passed. Cross-version source parity and public API example compilation checks also passed. Runtime coverage and subsequent focused fixes are tracked separately below.',
              'Pictures are cropped from full, real game screenshots. No mockups or reconstructed textures are used. Native player inventory remains visible in original interfaces; SIU separates it into its own window.',
              'A screenshot confirms the pictured state. Focused checks cover affected entries, layouts and controls; the complete functional/gameplay suite was not run. Alternate resource packs, every progression state and exhaustive feature combinations are not certified by these pictures.',
              'Some earlier block render checks entered the publisher\'s native menu provider directly. Those frames confirm appearance. Newer pages identify ordinary client block interactions; normal entry and gameplay need their separate focused checks.',
              'AE2 keeps the installed release’s own textures and screen resources. Applied Botanics 1.6.0-alpha.3 is checked with its published AE2 19.2.17 / Botania 454 snapshot dependency baseline; newer Botania and AE2 releases are separate verification cases.']:
        y=paragraph(c,p,M,y,W-2*M,size=15);y-=17
    footer(c,page);c.showPage()
    evidence=[];last_target=None
    for (mc,loader,ui),sides in sorted(pairs.items()):
        title=NAMES.get(ui)
        if title is None:
            native=sides['native']['state'];owners=titles.get((mc,loader,native.get('screen','')),set())
            title=(next(iter(owners))+' - '+native['title'] if len(owners)==1 and native.get('title') else ui.replace('-',' ').title())
        page+=1;heading(c,title,f'Minecraft {mc} / {loader.capitalize()} / actual game captures')
        if (mc,loader)!=last_target:
            target_key='target-'+mc+'-'+loader;c.bookmarkPage(target_key)
            c.addOutlineEntry(mc+' / '+loader.capitalize(),target_key,level=0);last_target=(mc,loader)
        capture_key='comparison-'+mc+'-'+loader+'-'+ui;c.bookmarkPage(capture_key)
        c.addOutlineEntry(title,capture_key,level=1)
        images={};meta={}
        for side in ['native','siu']:
            images[side],path,box=crop(sides[side],side);meta[side]={'source':relative(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'crop_pixels':box}
        top=H-156;bottom=160;panel_h=top-bottom
        scale=min(PW/max(im.width for im in images.values()),(panel_h-28)/max(im.height for im in images.values()),1.8)
        for side,x,label in [('native',M,'ORIGINAL / SIU ABSENT'),('siu',M+PW+GAP,'SIU WINDOW')]:
            text(c,label,x,top+16,12,TEAL,True)
            c.setFillColor(colors.HexColor(PALE));c.roundRect(x,bottom,PW,panel_h,10,fill=1,stroke=0)
            im=images[side];iw,ih=im.width*scale,im.height*scale
            c.drawImage(ImageReader(im),x+(PW-iw)/2,bottom+(panel_h-ih)/2,width=iw,height=ih,mask='auto')
            text(c,'Frame SHA-256: '+meta[side]['sha256'][:24]+'…',x,135,9,GRAY)
            paragraph(c,meta[side]['source'],x,119,PW,8,11)
        note=sides['siu'].get('note','Full screenshot files and observation records are retained with the capture run. The page shows only the pictured UI state.')
        if sides['native'].get('note','').startswith('Original reference uses Easy Anvils'):
            note=sides['native']['note']
        paragraph(c,note,M,69,W-2*M,10,13)
        footer(c,page);c.showPage();evidence.append({'page':page,'minecraft':mc,'loader':loader,'ui':ui,**meta})
    groups=collections.defaultdict(list)
    for row in matrix:groups[(row['minecraft'],row['loader'])].append(row)
    for (mc,loader),rows in groups.items():
        # Availability listings are separate from the pictured, validated states.
        for offset in range(0,len(rows),30):
            page+=1;heading(c,f'{mc} / {loader.capitalize()} - availability ledger','Exact publisher releases • Inventory comparisons complete • Focused checks only' if FINAL else 'Registration and compilation are not a substitute for runtime confirmation. Pending entries are intentional.')
            y=H-147
            for x,label in [(M,'MOD / PROJECT'),(M+420,'PUBLISHER VERSION'),(M+760,'UI COVERAGE' if FINAL else 'RUNTIME CONFIRMATION')]:text(c,label,x,y,10,TEAL,True)
            y-=23
            for row in rows[offset:offset+30]:
                title=row['project'];version=row['upstream_version']
                if len(title)>58:title=title[:55]+'…'
                if len(version)>43:version=version[:40]+'…'
                text(c,title,M,y,11);text(c,version,M+420,y,10,GRAY)
                text(c,row['runtime_status'],M+760,y,10,GRAY)
                c.linkURL(row['release_url'],(M,y-3,M+400,y+11),relative=0,thickness=0)
                y-=20
            footer(c,page);c.showPage()
    notes_path=ROOT/'build/all-target-rollout/runtime-notes.json'
    scope_path=ROOT/'output/all-target-rollout/noninventory-scope.json'
    if scope_path.exists():
        scope=read(scope_path)
        for offset in range(0,len(scope['projects']),12):
            page+=1;heading(c,'Appendix - non-inventory projects','Browser overlays, HUDs and full-screen settings are accounted for here; they are not inventory-window runtime passes.')
            y=H-148
            for entry in scope['projects'][offset:offset+12]:
                text(c,f"{entry['project']} / {entry['minecraft']} / {entry['loader'].capitalize()}",M,y,12,TEAL,True);y-=18
                y=paragraph(c,entry['reason'],M,y,W-2*M,size=10,leading=13);y-=13
            footer(c,page);c.showPage()
        exclusions=scope['screen_exclusions']
        for offset in range(0,len(exclusions),18):
            page+=1;heading(c,'Appendix - interfaces outside screenshot scope','Readers and settings remain native. Shared base classes are represented by their concrete inventory interfaces.')
            y=H-148
            for entry in exclusions[offset:offset+18]:
                text(c,f"{entry['project']} / {entry['minecraft']} / {entry['loader'].capitalize()}",M,y,11,TEAL,True);y-=15
                y=paragraph(c,entry['screen'],M,y,W-2*M,size=10,leading=12);y-=10
            footer(c,page);c.showPage()
    if notes_path.exists():
        for note in read(notes_path):
            page+=1;heading(c,note['title'],note['target'])
            y=H-160
            for entry in note['details']:
                y=paragraph(c,entry,M,y,W-2*M,size=16);y-=24
            y-=20;text(c,'Retained source logs',M,y,12,TEAL,True);y-=25
            for source in note.get('sources',[]):
                y=paragraph(c,source,M,y,W-2*M,size=11);y-=10
            footer(c,page);c.showPage()
    c.save();(WORK/'page-evidence.json').write_text(json.dumps(evidence,indent=2))
    print(json.dumps({'pdf':str(OUT),'pages':page,'comparison_pages':len(pairs),'status':'inventoried UI comparison coverage complete; focused checks only' if FINAL else 'working draft; final coverage pending'},indent=2))

if __name__=='__main__':main()
