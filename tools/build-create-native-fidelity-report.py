"""Publish unchanged original captures against actual repaired Create windows."""
from pathlib import Path
from collections import Counter
import argparse,hashlib,json,shutil,zipfile
from PIL import Image,ImageDraw,ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import landscape,A4
from reportlab.lib.colors import HexColor
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph,Table,TableStyle
from reportlab.lib.styles import ParagraphStyle
from xml.sax.saxutils import escape

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/create-native-fidelity-fixes'
PDF=ROOT/'output/pdf/siu-create-native-fidelity-fixed-comparisons.pdf'
REPAIRS=[
 ('CC-01','All 23 affected states','Complete original-scale companion artwork restored beside the SIU frame. Nested category artwork uses the native item renderer without the modal shade.'),
 ('CC-02','All 23 affected states','Original light action bands, bevels and terminal motifs restored inside the working panels. Player inventory remains separate.'),
 ('TB-01','Toolbox / Mounted Toolbox','Original ring positions and spacing retained together. The native footer is restored from its upstream atlas where inventory originally overlapped it.'),
 ('SN-01','Schematicannon','Complete fuel field and its right cap retained.'),
 ('SN-02','Schematicannon','Continuous original fuel/printer, process and playback sections replace the isolated material slices.'),
 ('SN-03','Schematicannon','Original schematic/list printer pictogram restored before its slot.'),
 ('SN-04','Schematicannon settings','Original inline settings toolbar remains under the visible process panel.'),
 ('ST-01','Schematic Table','Arrow returns to its continuous gray output/action strip, beneath the original blue file-picker band.'),
 ('RQ-01','Redstone Requester','Complete tray apron, hook and native footer restored; detached rule removed.'),
 ('LF-01','List Filter','Native compact four-button grouping restored on the footer.'),
 ('AF-01','Attribute Filter','Native counter precedes the three policy modes. The original selected-rule tooltip remains available.'),
 ('PF-01','Package Filter','Delete again sits immediately before confirm on the right.'),
 ('LC-01','Linked Controller','Original complete key pictograms replace the abbreviated added captions. Native binding names remain in tooltips.'),
 ('SC-01','Train Schedule','Complete native viewport and lower action-strip closure restored.'),
 ('SE-01','Instruction Editor','Native side wings and enclosing lower rail restored in a fitted body; unrelated sentence and blank surround removed.'),
 ('CA-01','Creative Ammo','Continuous native charcoal skin, purple controls and matching magenta window accent replace the olive surround.'),
 ('SK-01','Stock request catalogue','Native header, stone uprights, tray, address and lower rail retained as one continuous board at the compact native viewport height.'),
 ('BP-01','Crafting Blueprint','Complete original arrow restored; stray vertical fragment removed. Native overlapping output squares preserved.'),
 ('FG-01','Factory Gauge','Original centered arrow/slot group restored, with confirm back on the lower action band.'),
]
def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def save(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')

def collect(run):
    base=ROOT/'build/remaining-ui-audit/gameplay'/run
    native=read(ROOT/'output/create-native-comparison-review/comparison-manifest.json')
    captures={p['id']:p for p in read(base/'final-captures.json')['cases']}
    review=read(ROOT/'output/create-native-comparison-review/agent/review.json')
    assert {r[0] for r in REPAIRS}=={f['id'] for f in review['cross_cutting_findings']+review['window_findings']}
    stage=read(base/'staged-artifacts.json');assert stage['siu_loaded'] and stage['compatibility_loaded']
    assert {a['filename']:a['sha256'] for a in stage['artifacts'] if a['family']=='upstream'}==native['upstream_release_sha256']
    font=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',25);small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',19)
    pairs=[]
    for original in native['pairs']:
        case=captures[original['id']];e=read(case['evidence'])['client'];w=next(w for w in e['allWindows'] if w['menuId']==original['menu_id'])
        assert w['template']=='com.salts_inventory_update.compat.create.CreateWindows$Template'
        assert w['nativeScreen']==original['native_screen']
        dest=OUT/'pairs'/original['id'];dest.mkdir(parents=True,exist_ok=True)
        shutil.copy2(original['original']['full_image'],dest/'original-full.png');shutil.copy2(case['image'],dest/'fixed-full.png')
        shutil.copy2(original['original']['evidence'],dest/'original-evidence.json');shutil.copy2(case['evidence'],dest/'fixed-evidence.json')
        assert sha(dest/'original-full.png')==original['original']['full_image_sha256']
        im=Image.open(dest/'fixed-full.png');b=w['bounds'];x0,y0=b['x'],b['y'];x1,y1=x0+b['width'],y0+b['height']
        for region in w['composition']['regions']:
            if region.get('exterior'):
                x1=max(x1,b['x']+region['x']+region['source']['width']);y1=max(y1,b['y']+region['y']+region['source']['height'])
        if original['id'] in ('categories','category-editor'):
            x1=max(x1,b['x']+b['width']+70);y1=max(y1,b['y']+max(24,b['height']-70)+64)
        assert min(x0,y0)>=0 and x1<=e['guiWidth'] and y1<=e['guiHeight'],(original['id'],b,x1,y1,e['guiWidth'],e['guiHeight'])
        scale=im.width/e['guiWidth'];rect=[max(0,round(x0*scale)-4),max(0,round(y0*scale)-4),min(im.width,round(x1*scale)+4),min(im.height,round(y1*scale)+4)]
        fixed=im.crop(rect);fixed.save(dest/'fixed-ui.png')
        old=Image.open(dest/'original-full.png').crop(original['original']['crop_rectangle_pixels']);old.save(dest/'original-ui.png')
        column=max(old.width,fixed.width)+32;h=max(old.height,fixed.height)+100
        board=Image.new('RGB',(column*2,h),'#edf3f4');draw=ImageDraw.Draw(board)
        draw.text((16,12),'ORIGINAL UI - SIU ABSENT',font=font,fill='#23797c');draw.text((column+16,12),'UPDATED SIU WINDOW',font=font,fill='#23797c')
        board.paste(old,((column-old.width)//2,58));board.paste(fixed,(column+(column-fixed.width)//2,58))
        draw.text((16,h-30),'Minecraft 1.20.1 Fabric | Same upstream releases / GUI scale 2 | Actual game captures',font=small,fill='#52656b');board.save(dest/'comparison.png')
        findings=next(w for w in review['windows'] if w['pair_id']==original['id'])['finding_ids']
        pair={'id':original['id'],'title':original['title'],'menu_id':original['menu_id'],'native_screen':original['native_screen'],'board':str(dest/'comparison.png'),'original_full':str(dest/'original-full.png'),'fixed_full':str(dest/'fixed-full.png'),'original_sha256':sha(dest/'original-full.png'),'fixed_sha256':sha(dest/'fixed-full.png'),'fixed_crop_pixels':rect,'findings_addressed':findings}
        pairs.append(pair)
    assert len(pairs)==25
    journal=read(base/'features/journal.json');latest={c['id']:c for c in journal['checks']};functional=[c for c in latest.values() if not c['id'].startswith('final-')];captures_checks=[c for c in latest.values() if c['id'].startswith('final-')]
    artifacts=[]
    for loader,loader_label in [('fabric','Fabric'),('forge','Forge')]:
        for family,prefix,project in [('core','salts_inventory_update','versions'),('compatibility','salts_inventory_compat','compatibility/versions')]:
            version='0.1.6' if family=='core' else '0.1.3';source=ROOT/project/'1.20.1'/loader/'build/libs'/f'{prefix}-1.20.1-{loader_label}-{version}.jar'
            target=OUT/'builds'/loader/source.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
            with zipfile.ZipFile(target) as z:assert not any(n.startswith('com/siu_reference/') for n in z.namelist())
            artifacts.append({'loader':loader,'family':family,'file':str(target),'sha256':sha(target)})
    # Fabric production captures must use exactly the published candidate bytes.
    for a in artifacts:
        if a['loader']=='fabric':assert a['sha256']==next(s['sha256'] for s in stage['artifacts'] if s['family']==a['family'])
    evidence={'scope':'Minecraft 1.20.1; Fabric screenshot and focused interaction evidence; Fabric and Forge builds','capture_run':run,'pairs':pairs,'repairs':[{'id':i,'window':w,'action':a,'status':'addressed'} for i,w,a in REPAIRS],'functional_checks':functional,'capture_checks':captures_checks,'functional_checks_passed':bool(journal.get('complete')) and all(c['passed'] for c in functional),'artifacts':artifacts,'upstream_release_sha256':native['upstream_release_sha256'],'limits':['Not a complete functional or gameplay suite. Only controls affected by the composition change were checked.','Forge shares the changed Compatibility sources and its JAR build passed; fresh visual and interaction evidence in this report is Fabric only.','Full source screenshots and crop coordinates are retained. World scene, animation phase, viewport height and item order can differ.','Original output-square overlap and the Category Editor field ending are preserved intentionally.','At small GUI viewports, companion artwork moves into the scrollable canvas so its complete source remains reachable.','No public API or Core implementation was changed for this repair.']}
    evidence['assertion_history']=journal['checks']
    evidence['qa_refinements']=['The original category ownership assertion required a literal AIR count of zero. The cursor serialized AIR with count one while the real filter was correctly owned by the native proxy slot. The check now verifies an AIR cursor and additionally asserts the owned filter in that slot. The initial result is preserved in assertion_history.']
    evidence['qa_refinements'].append('Minecraft clamps requested GUI scale 4 to effective scale 3 at a 1280x900 framebuffer. Scale 4 was repeated at 1280x960, with the observed GUI explicitly checked as 320x240; clicking the horizontal scrollbar reaches the complete companion artwork. The earlier inapplicable scroll assertion remains in history.')
    evidence['changed_implementation_files']=[{'file':str(path),'sha256':sha(path)} for path in (ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/create').glob('Create*.java') if path.stem in ['CreateLayouts','CreateWindows','CreateChrome']]
    evidence['limits'].append('The gallery principally shows default/empty states, plus the filled stock catalogue. Dense lists, long text in every field and all animation frames were not exhaustively captured.')
    evidence['limits'].append('Villagers and barrels behind the updated windows are test-world scenery. Outboard UI artwork is retained. Example field contents can differ after checks, including the selected local schematic filename.')
    save(OUT/'acceptance-evidence.json',evidence);return evidence

def publish(e):
    assert e['functional_checks_passed'] and all(c['passed'] for c in e['capture_checks'])
    for name,path in [('F','C:/Windows/Fonts/segoeui.ttf'),('B','C:/Windows/Fonts/segoeuib.ttf')]:pdfmetrics.registerFont(TTFont(name,path))
    width,height=landscape(A4);m=36;bw=width-2*m;teal=HexColor('#23797c');ink=HexColor('#243b40');muted=HexColor('#64767c')
    style=ParagraphStyle('p',fontName='F',fontSize=10.3,leading=14,textColor=ink);small=ParagraphStyle('s',fontName='F',fontSize=8.5,leading=11,textColor=muted)
    PDF.parent.mkdir(parents=True,exist_ok=True);c=canvas.Canvas(str(PDF),pagesize=(width,height));c.setTitle('Create - original UI versus repaired SIU windows');c.setAuthor('Codex / Salt\'s Inventory Update')
    def para(value,x,y,w=bw,sty=style):
        p=Paragraph(escape(str(value)),sty);_,h=p.wrap(w,1000);p.drawOn(c,x,y-h);return y-h-9
    def page(title,subtitle=''):
        c.setFillColor(teal);c.setFont('B',8.5);c.drawString(m,height-24,'SIU / CREATE / NATIVE FIDELITY REPAIRS')
        c.setFillColor(ink);c.setFont('B',21);c.drawString(m,height-55,title)
        c.setStrokeColor(HexColor('#d5e1e2'));c.line(m,29,width-m,29);c.setFillColor(muted);c.setFont('F',8);c.drawString(m,17,'Original left / updated SIU right | Actual game screenshots | Minecraft 1.20.1 Fabric');c.drawRightString(width-m,17,str(c.getPageNumber()))
        return para(subtitle,m,height-67,sty=small) if subtitle else height-77
    y=page('Original Create UI versus repaired SIU windows')
    for value in ['All 19 review findings have been addressed across 25 pictured states and 20 native menu classes. These are actual captures of the implemented windows, rather than mockups.',
        'The repairs preserve complete original panels, native material sections, action bands and control order. Original-scale companion artwork remains beside the frame. SIU retains its window controls and separate player inventory.',
        'The original side uses the same retained upstream mod releases as the new Fabric captures: Create 6.0.8.1 build 1744, Create Big Cannons 5.11.4 and Steam n Rails 1.7.3. Both sides use GUI scale 2.',
        'Native details identified as inherited by the review remain intact, including the overlapping Blueprint output slots and open-looking Category Editor field end.']:
        y=para(value,m,y)
    y=para(f"Validation: {len(e['functional_checks'])} focused interaction assertions and {len(e['capture_checks'])} captured-state/template assertions passed. Two focused JVM geometry tests passed. Fabric and Forge Compatibility builds passed.",m,y)
    for value in e['limits']:y=para('- '+value,m,y,sty=small)
    c.showPage()
    for start in [0,10]:
        y=page('Review findings addressed',f'Repair record {start+1}-{min(start+10,len(REPAIRS))} of {len(REPAIRS)}. The following pages show every original/updated comparison.')
        for identifier,window,action in REPAIRS[start:start+10]:
            c.setFillColor(teal);c.setFont('B',10);c.drawString(m,y-10,identifier+' | '+window);y-=17
            y=para(action,m,y,sty=small)-4
        c.showPage()
    for i,pair in enumerate(e['pairs'],1):
        c.bookmarkPage(pair['id']);c.addOutlineEntry(pair['title'],pair['id'],0)
        y=page(pair['title'],'Original left / updated SIU right. Both UI crops retain the same source-pixel scale. Original attached inventories are shown where present.')
        im=Image.open(pair['board']);ratio=min(bw/im.width,365/im.height);iw,ih=im.width*ratio,im.height*ratio
        c.drawImage(pair['board'],m+(bw-iw)/2,y-ih,width=iw,height=ih,mask='auto');y-=ih+12
        if pair['findings_addressed']:
            y=para('Review items addressed: '+', '.join(pair['findings_addressed'])+'.',m,y,sty=small)
            actions=[a for identifier,_,a in REPAIRS if identifier in pair['findings_addressed'] and identifier not in ['CC-01','CC-02']]
            for action in actions[:1]:y=para(action,m,y,sty=small)
            if pair['id']=='schematic-table':y=para('The updated picker contains the local schematic used for the upload check; the original reference had no saved schematics.',m,y,sty=small)
        else:y=para('Native component ordering, enclosing rails and material continuity restored.',m,y,sty=small)
        assert y>38,(pair['id'],y)
        c.showPage()
    y=page('Focused verification and coverage')
    groups=Counter(c['id'].split('-')[0] for c in e['functional_checks'])
    for value in ['Checked in the actual Fabric client: inventory transfer and native capacities; package rename and save; filter and controller references, policies and clear; requester quantities/address; schematic upload, inline settings and playback; category editing; stock search, basket/send and lock; native schedule suggestions; one scroll/confirm-save per fuze; and GUI scales 3 and 4.',
        'The two JVM tests check complete cells and inverse pointer routing, plus every visible native control from 23 supplied reference states. Nested editor states are additionally exercised in game.',
        'This is focused verification of the changed window composition. It is not the complete functional or gameplay suite. Source captures and authoritative server snapshots are retained with the report evidence.',
        'Install the matching Core and Compatibility JARs together. The builds directory contains the unchanged current Core build alongside the repaired Compatibility build for each loader. Public API signatures and Core source were not changed by this task.']:
        y=para(value,m,y)
    for loader in ['fabric','forge']:
        for a in [x for x in e['artifacts'] if x['loader']==loader]:y=para(loader.capitalize()+' / '+a['family']+' - SHA-256 '+a['sha256'],m,y,sty=small)
    y=para('Exploratory QA assertions were refined for semantic empty-cursor checking and the actual GUI scale. Latest passing results are counted once per assertion; the complete earlier result history is preserved.',m,y,sty=small)
    y=para('Evidence: output/create-native-fidelity-fixes/acceptance-evidence.json. Full captures, comparison crops, review-to-fix records, focused assertions and build hashes are retained.',m,y,sty=small)
    c.save();print(json.dumps({'pdf':str(PDF),'comparisons':len(e['pairs']),'repairs':len(REPAIRS),'focused_assertions':len(e['functional_checks'])}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--run',default='1.20.1-fabric-integrated-create-fidelity03');p.add_argument('--pdf',action='store_true');a=p.parse_args();e=collect(a.run)
    if a.pdf:publish(e)
    else:print(json.dumps({'comparisons':len(e['pairs']),'evidence':str(OUT/'acceptance-evidence.json')}))
