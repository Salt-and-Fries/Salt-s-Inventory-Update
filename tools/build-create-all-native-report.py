"""Publish actual-game evidence for the complete 1.20.1 native Create rollout."""
from pathlib import Path
import argparse, hashlib, json, shutil
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
NOTES={
 'toolbox':'The original compartment ring, central deposit control, leather panel and complete footer remain together. Only the title alignment and SIU management controls change.',
 'schematicannon':'The approved reference design: the original fuel, materials and playback panels retain their native frame and footer. SIU controls sit inside the original title strip.',
 'schematicannon-settings':'Settings remain an inline native Schematicannon state. Mode buttons, toggles and the original return action keep their native geometry.',
 'schematic-table':'The original selector and footer remain intact. The complete refresh button stays outside the right edge, together with the original-scale table model.',
 'frogport':'The original port body sits directly below its own title strip. Its native editable address and pencil stay on the left; compact SIU controls avoid covering them.',
 'postbox':'The taller original Postbox header is preserved. Its address editor, package cell, status controls, footer and outboard model retain their native arrangement.',
 'requester':'The paper-and-brass requester keeps its original cells, quantity controls, address field, fulfillment policy and pointed footer. The model remains outside the frame.',
 'filter':'Both reference rows, allow/deny and data modes, clear and confirmation stay in their original parchment frame. No replacement slot textures are drawn over them.',
 'attribute':'The original attribute selector, reference cell, rule count, add/invert actions and mode controls remain on the original parchment. The full companion fits at GUI scale 4.',
 'package-filter':'The original address field and clear/confirm actions stay on their native material. The paper companion remains at its original offset, without a rectangular picture card.',
 'controller':'The native two-column frequency layout, all twelve reference cells, clear action and confirmation keep the original panel and pointed footer.',
 'schedule':'The original timeline, instruction rows, condition columns, loop toggle, add and confirmation actions remain together. The original responsive UI is not repacked into new cards.',
 'schedule-editor':'The native nested editor keeps its wings, original controls and footer. Transparent corners no longer capture a rectangular piece of the modal dimmer. Station suggestions remain interactive outside the panel.',
 'ammo':'The original ammo and tracer cells, spacing control, capacities and confirmation remain on the original Create Big Cannons frame.',
 'creative-ammo':'The wider original creative frame, original cell rules, spacing control and footer remain. Long localized titles use an explicit ellipsis before SIU controls.',
 'timed':'The original fuze slider, display, explanatory symbols and confirmation keep their native panel. The outboard model stays at its original scale.',
 'proximity':'The native distance control and its original visual context remain intact. The original confirmation and held-item save behavior are retained.',
 'inertia':'The delayed-inertia fuze retains the original timing control, framing and footer. SIU changes the title treatment and adds management controls.',
 'impact':'The delayed-impact fuze retains its native control geometry, model and confirmation. Native wheel changes and both confirmation routes save correctly.',
 'categories':'Category rows, filters and original reorder/add controls keep the native material and layout. The separate player inventory no longer dictates the panel height.',
 'category-editor':'The original filter cell, category name and footer controls stay together. Transparent atlas edges are excluded from the captured modal dimmer; the original stock-ticker item is rendered without a dark backdrop.',
 'stock':'The full native stock board remains responsive to GUI size. Search, category hiding, basket quantities, destination, lock and checkout stay in their original positions; the stockkeeper remains beside the board.',
 'blueprint':'The original grid, arrow, overlapping recipe result pair, clear action and pointed footer remain. A compact SIU title menu leaves room for the full native title.',
 'mounted-toolbox':'Steam n Rails inherits the same native Toolbox treatment: original compartment ring, deposit action, footer and model. Actual conductor storage remains the backing inventory.',
 'gauge':'The Factory Gauge item-reference selector retains its original small brass frame, ghost cell and native confirmation. The main gauge settings screen remains outside this inventory-only rollout.',
}

def read(p):return json.loads(Path(p).read_text(encoding='utf-8-sig'))
def write(p,v):Path(p).write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()

def publish(runtime,prior_runtime=None):
    dest=ROOT/'output/create-native-all';dest.mkdir(parents=True,exist_ok=True)
    manifest=read(runtime/'final-captures.json');assert len(manifest['cases'])==25 and manifest['unique_menus']==20
    prior=read(ROOT/'output/create-native-comparison-review/comparison-manifest.json')
    references={p['id']:p for p in prior['pairs']};pairs=[]
    for case in manifest['cases']:
        ident=case['id'];folder=dest/'pairs'/ident;folder.mkdir(parents=True,exist_ok=True)
        ref=ROOT/'output/create-native-comparison-review/pairs'/ident
        old=read(ref/'original-evidence.json')['client'];new=read(case['evidence'])['client']
        assert not old['siu_loaded'] and new['siu_loaded']
        window=next(w for w in new['allWindows'] if w['menuId']==case['menu']);b=window['bounds']
        assert window['composition']['padding']==0 and not window['composition']['scrollable']
        assert not any(r['id'].startswith('extra-') for r in window['composition']['regions'])
        for name in ['original-full.png','original-ui.png','original-evidence.json']:shutil.copy2(ref/name,folder/name)
        shutil.copy2(case['image'],folder/'fixed-full.png');shutil.copy2(case['evidence'],folder/'fixed-evidence.json')
        regions=[r for r in window['composition']['regions'] if r.get('exterior')]
        left=min([0]+[r['x'] for r in regions]);top=min([0]+[r['y'] for r in regions])
        right=max([b['width']]+[r['x']+r['source']['width'] for r in regions]);bottom=max([b['height']]+[r['y']+r['source']['height'] for r in regions])
        if ident=='category-editor':right=max(right,252);bottom=max(bottom,145)
        full=Image.open(case['image']);scale=full.width/new['guiWidth']
        crop=[max(0,round((b['x']+left-4)*scale)),max(0,round((b['y']+top-4)*scale)),min(full.width,round((b['x']+right+4)*scale)),min(full.height,round((b['y']+bottom+4)*scale))]
        full.crop(crop).save(folder/'fixed-ui.png')
        pairs.append({**case,'folder':str(folder),'bounds':b,'crop':crop,'note':NOTES[ident],
            'original_sha256':sha(folder/'original-full.png'),'fixed_sha256':sha(folder/'fixed-full.png')})
    validation=prior_runtime or runtime
    current_journal=read(runtime/'features/journal.json')
    prior_journal=read(validation/'features/journal.json') if prior_runtime else {'checks':[],'groups':{}}
    history=[{**r,'source_runtime':str(validation.resolve())} for r in prior_journal['checks'] if not r['id'].startswith('final-')]
    history += [{**r,'source_runtime':str(runtime.resolve())} for r in current_journal['checks']]
    latest={r['id']:r for r in history}
    assert all(r['passed'] for r in latest.values())
    responsive=read(validation/'native-responsive-cases.json');assert len(responsive)==24
    current_journal['complete']=True;write(runtime/'features/journal.json',current_journal)
    journal={'complete':True,'scope':'Focused acceptance across the native-frame build and final title-inset-only correction; not the complete Create gameplay suite','checks':history,'groups':{**prior_journal['groups'],**current_journal['groups']}}
    write(dest/'check-history.json',journal)
    captures=dest/'captures';captures.mkdir(exist_ok=True)
    names=['frogport-header-editing','postbox-header-editing','frogport-management-menu','postbox-management-menu','package-filter-address','controller-filled','requester-filled','ammo-filled','creative-ammo-filled','mounted-toolbox-filled','gauge-filled','categories-reordered','stock-basket','stock-sent','schedule-logical-conditions','schedule-station-suggestions']
    names+=['gui4-'+ident for ident in responsive]
    for name in names:
        for sub,extension in [('screenshots','.png'),('features','.json')]:
            source=validation/sub/(name+extension)
            if source.exists():shutil.copy2(source,captures/(name+extension))
    for source in (runtime/'screenshots').glob('schedule-header-*.png'):shutil.copy2(source,captures/source.name)
    for source in (runtime/'features').glob('schedule-header-*.json'):shutil.copy2(source,captures/source.name)
    stage=read(runtime/'staged-artifacts.json');mods=Path(read(runtime/'launch.json')['cwd'])/'mods';builds=[]
    for loader in ['fabric','forge']:
        for project,mod,version,family in [('versions','salts_inventory_update','0.1.6','core'),('compatibility/versions','salts_inventory_compat','0.1.3','compatibility')]:
            name=f'{mod}-1.20.1-{loader.title()}-{version}.jar';source=mods/name if loader=='fabric' else ROOT/project/'1.20.1'/loader/'build/libs'/name
            if loader=='fabric':assert sha(source)==next(a['sha256'] for a in stage['artifacts'] if a['family']==family)
            target=dest/'builds'/loader/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
            builds.append({'loader':loader,'file':str(target.relative_to(dest)),'sha256':sha(target)})
    shutil.copy2(runtime/'staged-artifacts.json',dest/'runtime-artifacts.json')
    if prior_runtime:shutil.copy2(validation/'staged-artifacts.json',dest/'prior-runtime-artifacts.json')
    evidence=dest/'validation';evidence.mkdir(exist_ok=True)
    for name in ['build-refined.log','build-final.log','module-boundary.log','remaining-checks02.log','responsive02.log','responsive02-resumed.log','final-header03.log','final-class-delta.json']:
        shutil.copy2(ROOT/'build/create-native-all'/name,evidence/name)
    features=sum(not c['id'].startswith('final-') and '-gui4-' not in c['id'] for c in latest.values())
    header_checks=sum(c['id'].startswith('schedule-header-') for c in latest.values())
    # Header follow-ups contain '-gui4-' in their names but are feature checks.
    features=sum(not c['id'].startswith('final-') and (c['id'].startswith('schedule-header-') or '-gui4-' not in c['id']) for c in latest.values())
    acceptance={'scope':'All retained 1.20.1 Create-family inventory/container menus: Create, Create Big Cannons and Steam n Rails',
        'menu_types':20,'primary_views':25,'focused_feature_checks':features,'final_header_checks':header_checks,'responsive_views':24,'responsive_checks':72,'capture_checks':25,
        'total_unique_checks':len(latest),'all_passed':True,'groups':journal['groups'],'builds':builds,
        'upstream_release_sha256':prior['upstream_release_sha256'],'runtime':str(runtime.resolve()),'prior_feature_runtime':str(validation.resolve()),
        'validation':'Production Fabric actual-game checks; two layout geometry tests; Fabric remapJar and Forge jar/reobfJar; source module boundaries verified.',
        'limits':'Focused checks, not a complete Create functional/gameplay suite. Forge build-checked only in this rollout. The earlier six windows retain their existing functional acceptance; they received fresh captures and GUI-scale checks here. External OS folder launching was not activated. Fuze endpoint limits were not repeated.',
        'api':'No Core or public API edits in this rollout. Shared 1.20.1 Compatibility sources implement the native frame changes.',
        'final_revision':'The final Schedule title-inset correction changes only CreateNativeFrame.class. Native layout/action classes are byte-identical to the build with the 114 feature checks and 24-state scale matrix. The final binary received separate header checks at GUI scales 2 and 4 plus all 25 fresh primary captures.',
        'excluded':'Non-inventory controls remain native. Later-version Simulated typewriter screens and distinct weighted Shuffle Filter are not backported to 1.20.1.',
        'capture_notes':'Raw screenshots are unchanged; UI images are documented crops. Original references are matched production 1.20.1 captures without SIU. Fuze originals contain inherited hover tooltips. Empty versus populated test fixtures and animated models can differ. The original Stock Keeper responds to GUI height.'}
    write(dest/'acceptance.json',acceptance);write(dest/'manifest.json',pairs)
    (dest/'README.md').write_text(f'''# Native Create windows - Minecraft 1.20.1

All 20 retained inventory/container menu types now use the approved native-frame design, with 25 primary states captured. Original Create / Create Big Cannons title materials, bodies, pointed footers, widgets and outboard models remain. SIU titles and management controls occupy the original title strip. Player inventory remains a separate SIU window.

This rollout extends the six earlier native-frame menus to the remaining 14 menu types, including Frogport/Postbox, Requester, Stock Keeper/category editor, Package Filter, Controller, Schedule/editor, Gauge, ammo containers, four fuze menus and mounted Toolbox. Forge uses the same Compatibility sources. No Core or public API edits were made for this rollout.

Validation: {features} focused feature assertions (114 before the last title-inset correction, plus {header_checks} checks on that final correction), 72 GUI-scale-4 assertions across 24 states, 25 final native-window capture checks, and two layout geometry tests passed. Fabric and Forge builds passed. Fabric was tested in game; Forge was build-checked only. This is not the complete Create gameplay suite. Earlier six-menu gameplay acceptance remains in its existing evidence packages. See acceptance.json for coverage limits.

The final correction changes only CreateNativeFrame.class; all native layout/action implementation classes are byte-identical to the previously tested build. Prior populated/scale captures and their artifact provenance are retained separately. Every primary comparison capture uses the final binary. validation/final-class-delta.json and both runtime artifact records document this boundary.

The scale-check setup initially hit an earlier connected-port chain conveyor in the disposable world. The world fixture was cleared and the remaining four states were resumed successfully; no UI assertion failed. Original failed setup logs are retained. A transient dark-world rendering artifact observed in earlier sessions was not isolated or changed by this Compatibility-only rollout.

pairs/ holds all 25 SIU-absent originals and final implemented full screenshots, JSON observations and complete window crops. captures/ holds populated states, compact title menus, editable port headers and all 24 GUI-scale-4 captures. validation/ records build and focused-check logs. manifest.json and acceptance.json record provenance and SHA-256 values.

builds/ contains Core 0.1.6 and Compatibility 0.1.3 for Fabric and Forge. Install the pair for your loader, replacing existing copies. Fabric artifacts are byte-identical to those used by the production runtime. Upstream mod JARs were not altered. The PDF is output/pdf/siu-create-all-native-window-comparisons.pdf in the workspace.

Inventory-only scope follows the previously approved scope. Non-inventory station, display, radial and configuration interfaces remain native. Simulated typewriter screens in the older cross-version design PDF have no applicable retained 1.20.1 release. Forge Shuffle Filter 1.0.2 uses Create's shared List Filter screen and inherits its native frame; no later-version distinct weighted screen was backported.
''',encoding='utf-8')
    return dest,pairs,acceptance

def wrap(c,text,x,y,width,size=10,leading=15,font='Body'):
    c.setFont(font,size);line=''
    for word in text.split():
        candidate=(line+' '+word).strip()
        if line and pdfmetrics.stringWidth(candidate,font,size)>width:c.drawString(x,y,line);y-=leading;line=word
        else:line=candidate
    if line:c.drawString(x,y,line);y-=leading
    return y

def picture(c,path,x,y,w,h):
    im=Image.open(path);s=min(w/im.width,h/im.height);iw,ih=im.width*s,im.height*s
    c.drawImage(ImageReader(im),x+(w-iw)/2,y+(h-ih)/2,iw,ih,mask='auto')

def report(dest,pairs,a):
    pdf=ROOT/'output/pdf/siu-create-all-native-window-comparisons.pdf';pdf.parent.mkdir(parents=True,exist_ok=True)
    pdfmetrics.registerFont(TTFont('Body','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('Heading','C:/Windows/Fonts/segoeuib.ttf'))
    W,H=landscape(A4);c=canvas.Canvas(str(pdf),pagesize=(W,H));c.setTitle('SIU - Complete native Create window rollout - 1.20.1');c.setAuthor("Salt's Inventory Update")
    count=len(pairs)+4;page=0
    def start(title,subtitle=''):
        nonlocal page
        page+=1;c.setFillColor(colors.HexColor('#137d80'));c.setFont('Heading',9);c.drawString(32,H-30,'SIU / CREATE NATIVE WINDOWS / IMPLEMENTED IN GAME')
        c.setFillColor(colors.HexColor('#203638'));c.setFont('Heading',23);c.drawString(32,H-63,title)
        if subtitle:c.setFont('Body',10);c.drawString(32,H-84,subtitle)
    def finish(label='Minecraft 1.20.1 / actual production Fabric captures'):
        c.setStrokeColor(colors.HexColor('#cedcdd'));c.line(32,32,W-32,32);c.setFillColor(colors.HexColor('#66797a'));c.setFont('Body',8);c.drawString(32,19,label);c.drawRightString(W-32,19,f'{page} / {count}');c.showPage()
    start('Create, kept recognizably Create','Complete 1.20.1 inventory/container rollout - 20 menu types, 25 primary views')
    y=H-121
    for title,text in [
      ('The native frame is the window','Original title strips, textured bodies, borders, footer points and outboard models remain. SIU adds left-aligned titles and window controls inside the native strip. The player inventory becomes a separate SIU window.'),
      ('The remaining menus now follow the six approved examples','Package ports, Requester, Stock Keeper and category editing, Package Filter, Linked Controller, Schedule and its editors, Gauge selector, ammo and fuze menus, and mounted Toolbox use the same implementation approach.'),
      ('Actual game evidence','Each following page pairs an original 1.20.1 UI without SIU on the left with the implemented SIU window on the right. These are real game captures. Raw full screens, cropped views and observations accompany the report.'),
      ('Coverage boundaries','Create Fabric 6.0.8.1+build.1744, Big Cannons 5.11.4 and Steam n Rails 1.7.3 supply the runtime references. Both loader builds passed; this rollout was exercised in game on Fabric. Non-inventory interfaces are accounted for in the appendix.')]:
        y=wrap(c,title,32,y,W-64,size=13,font='Heading',leading=18)-4;y=wrap(c,text,32,y,W-64,size=11,leading=17)-20
    finish()
    for p in pairs:
        start(p['title'],'Original layout and materials retained; title and SIU window management adapted')
        col=(W-84)/2;top=H-119;by=129;bh=top-by-20
        for x,label,file in [(32,'ORIGINAL / SIU ABSENT','original-ui.png'),(52+col,'IMPLEMENTED SIU / NATIVE FRAME','fixed-ui.png')]:
            c.setFillColor(colors.HexColor('#137d80'));c.setFont('Heading',9);c.drawString(x,top,label)
            c.setFillColor(colors.HexColor('#eff4f4'));c.roundRect(x,by,col,bh,8,fill=1,stroke=0)
            picture(c,Path(p['folder'])/file,x+9,by+9,col-18,bh-18)
        c.setFillColor(colors.HexColor('#203638'));wrap(c,p['note'],32,108,W-64,size=10,leading=14)
        c.setFillColor(colors.HexColor('#66797a'));c.setFont('Body',8)
        caption='Original fuze references include native hover tooltips; the implemented capture has the pointer moved away.' if p['id'] in ['timed','proximity','inertia','impact'] else 'Window crops include the complete frame and outboard model. Raw full-screen captures are saved alongside this report.'
        c.drawString(32,43,caption);finish()
    start('The window in its workspace','Full game screen - original populated Requester plus independent SIU inventory')
    picture(c,dest/'captures/requester-filled.png',32,77,W-64,H-182)
    c.setFillColor(colors.HexColor('#203638'));wrap(c,'Native quantity, address and fulfillment controls remain in the Requester. The player inventory uses its own SIU window. The original-scale model stays beside the native frame.',32,59,W-64,size=10,leading=14);finish()
    start('Focused validation','Affected features tested on the production Fabric build; shared Forge sources build-checked')
    y=H-116
    for text in [
      f"{a['focused_feature_checks']} focused feature assertions passed for the remaining menus. Package restrictions/transfers, native address editing and persistence, controller ghost references, Requester quantities and policy, ammo rules and spacing, mounted storage, Gauge save and fuze save routes were exercised.",
      'Stock Keeper checks covered a real stock network, search, category hiding, basket quantities, lock, destination and actual package creation. Category checks covered filter ownership, naming, adding, reordering, deleting, refund and persistence.',
      'Schedule checks covered all five instruction forms and nine condition forms, reference cells, loop and save, duplicate/move/delete/skip/restart, condition alternatives, navigation and station autocomplete pointer selection.',
      '72 responsive assertions passed across 24 states at GUI scale 4 (320 x 240 logical pixels): original dimensions, frame fit and complete captured companion bounds. All 25 primary states passed final native-template checks. Two layout geometry tests and both loader builds passed.',
      f"The final Schedule header-inset correction received {a['final_header_checks']} focused checks at GUI scales 2 and 4. Native action/layout classes are byte-identical to the build with the 114 feature checks and 24-state scale matrix. All primary comparisons use the final binary.",
      'Limits: focused checks, not the full Create gameplay suite. Earlier six-window gameplay acceptance is retained. Forge runtime parity, external folder launching and unchanged fuze endpoints were not re-tested. No Core or public API changes were made in this rollout.'
    ]:y=wrap(c,text,32,y,W-64,size=11,leading=17)-17
    finish('Focused coverage / raw evidence, check history, exact tested JARs and build logs included')
    start('Appendix - interfaces outside this rollout','Previously approved inventory/container scope retained; other Create controls stay native')
    y=H-116
    for title,text in [
      ('Create controls','Station setup and assembly, Elevator Contact, Sequenced Gearshift, Display Link, Threshold Switch, main Factory Panel settings, value settings, schematic tools, Symmetry Wand, Worldshaper, clipboard, goggles configuration, computer and radial overlays are non-inventory interfaces. They keep their existing native behavior.'),
      ('Addon controls and shared screens','Big Cannons mount controls and Steam n Rails bogey/tag radial controls remain native. Forge Shuffle Filter 1.0.2 shares the original Create Filter menu and inherits the new List Filter frame; later-version distinct weighted-filter screens were not backported.'),
      ('Cross-version design entries','The older design report included three Simulated typewriter states from a later release. No applicable retained 1.20.1 release was available, so this implementation does not claim those screens. Create Alloyed has no new distinct menu in this rollout.'),
      ('Other family entries in the source audit','Aeronautics, Offroad, Questing, Crafts & Additions, Slice & Dice, Copycats+, Dragons Plus, Enchantment Industry, Estrogen, Extended Wrenches, Factory and New Age supplied no additional retained distinct inventory screen here. Pattern Schematics tool selection and Testosterone settings remain non-inventory entries.'),
      ('Evidence and interpretation','Originals are matched production 1.20.1 SIU-absent captures. Worlds, item fixtures, animations and native hover tooltips can differ between captures. The Stock Keeper naturally changes height with GUI size. No screenshot has replacement UI artwork painted into it.')]:
        y=wrap(c,title,32,y,W-64,size=12,font='Heading',leading=17)-3;y=wrap(c,text,32,y,W-64,size=10,leading=15)-14
    finish('20 retained menu types / 25 primary views / no claim about every Create release or modpack')
    assert page==count;c.save();print(json.dumps({'pdf':str(pdf),'pages':page,'output':str(dest),'checks':a['total_unique_checks']}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('runtime',type=Path);p.add_argument('--prior-runtime',type=Path);args=p.parse_args();dest,pairs,a=publish(args.runtime,args.prior_runtime);report(dest,pairs,a)
