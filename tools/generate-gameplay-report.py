"""Build the gameplay QA PDF from executed results, framebuffer captures and JUnit XML.

Run only after completing the selected QA matrix and a fresh production build.
No assertion is inferred from a successful Gradle launcher exit alone.
"""
from pathlib import Path
from collections import Counter
import hashlib
import json
import xml.etree.ElementTree as ET
from html import escape
from datetime import datetime
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image, KeepTogether
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.utils import ImageReader

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'output/pdf/salts-inventory-core-and-compatibility-qa.pdf'
QA = ROOT / 'build/gameplay-qa'
NAVY = colors.HexColor('#142B45')
TEAL = colors.HexColor('#087E8B')
GRAY = colors.HexColor('#526171')
PALE = colors.HexColor('#EDF3F6')
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='ReportTitle',fontName='Helvetica-Bold',fontSize=26,leading=31,textColor=NAVY,spaceAfter=16))
styles.add(ParagraphStyle(name='ReportH1',fontName='Helvetica-Bold',fontSize=17,leading=21,textColor=NAVY,spaceAfter=12))
styles.add(ParagraphStyle(name='ReportH2',fontName='Helvetica-Bold',fontSize=12,leading=16,textColor=TEAL,spaceBefore=11,spaceAfter=7))
styles.add(ParagraphStyle(name='ReportBody',fontName='Helvetica',fontSize=10,leading=14,textColor=NAVY,spaceAfter=8))
styles.add(ParagraphStyle(name='ReportSmall',fontName='Helvetica',fontSize=8,leading=11,textColor=GRAY,spaceAfter=6))
styles.add(ParagraphStyle(name='ReportCell',fontName='Helvetica',fontSize=8,leading=11,textColor=NAVY))
story=[]
def p(text,style='ReportBody'):
    return Paragraph(text,styles[style])
def para(text,style='ReportBody'):
    story.append(p(text,style))
def heading(text): para(text,'ReportH1')
def sub(text): para(text,'ReportH2')
def table(rows,widths,padding=6):
    data=[[p(escape(str(c)),'ReportCell') for c in row] for row in rows]
    t=Table(data,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND',(0,0),(-1,0),PALE),('VALIGN',(0,0),(-1,-1),'TOP'),
        ('LEFTPADDING',(0,0),(-1,-1),7),('RIGHTPADDING',(0,0),(-1,-1),7),
        ('TOPPADDING',(0,0),(-1,-1),padding),('BOTTOMPADDING',(0,0),(-1,-1),padding),
        ('LINEBELOW',(0,0),(-1,0),1,TEAL),('LINEBELOW',(0,1),(-1,-1),.35,colors.HexColor('#D7E0E6'))]))
    story.append(t);story.append(Spacer(1,12))
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

profiles=[]
for path in sorted(QA.glob('*/*/results.json')):
    value=json.loads(path.read_text())
    profile=path.parent.parent.name
    log=path.parent/'launch.log'
    if profile=='packaged-core':log=ROOT/'build/packaged-core-qa.log'
    if profile=='packaged-extension-empty':log=ROOT/'build/packaged-extension-qa.log'
    text=log.read_text(errors='replace') if log.exists() else ''
    valid=('SIU_GAMEPLAY_SUMMARY status=PASS' in text and value['failures']==0)
    value.update(profile=profile,path=path,log=log,valid=valid)
    profiles.append(value)

# A native crash or loader failure may never reach the driver. Include its launch
# as failed evidence instead of silently omitting the version from the matrix.
known_logs={v['log'].resolve() for v in profiles}
for log in sorted(QA.glob('*/*/launch.log')):
    if log.resolve() in known_logs:continue
    text=log.read_text(errors='replace')
    if 'BUILD FAILED' not in text:continue
    target=log.parent.name
    version,loader=target.rsplit('-',1)
    reason='Launch failed before an in-world result was written. See the retained launch log.'
    if '0xC0000409' in text:reason='Native process terminated with NTSTATUS 0xC0000409 before gameplay; no Java mod exception identifies a cause.'
    profiles.append(dict(profile=log.parent.parent.name,minecraft=version,loader=loader,results=[dict(name='launch',status='FAIL',details=reason)],failures=1,path=None,log=log,valid=False))
profiles.sort(key=lambda v:(v['profile'],v['minecraft'],v['loader']))

def find(profile,version='1.21.1',loader='fabric'):
    return next((v for v in profiles if v['profile']==profile and v['minecraft']==version and v['loader']==loader),None)
def figure(profile,name,caption,version='1.21.1',loader='fabric',width=450):
    value=find(profile,version,loader)
    if not value:return
    record=next((r for r in value['results'] if r.get('name')==name+'-framebuffer' and r.get('status')=='CAPTURED'),None)
    if not record:return
    path=Path(record['path'])
    if not path.is_file():raise FileNotFoundError(path)
    iw,ih=ImageReader(str(path)).getSize()
    story.append(KeepTogether([Image(str(path),width=width,height=width*ih/iw),Spacer(1,5),p(escape(caption),'ReportSmall'),Spacer(1,11)]))

tests=[]
for path in (ROOT/'versions/common/build/test-results/test').glob('TEST-*.xml'):
    t=ET.parse(path).getroot()
    tests.append(dict(path=str(path.relative_to(ROOT)),tests=int(t.get('tests',0)),failures=int(t.get('failures',0)),errors=int(t.get('errors',0)),skipped=int(t.get('skipped',0))))
for base in [ROOT/'versions',ROOT/'compatibility/versions']:
    for path in base.glob('*/fabric/build/test-results/test/TEST-*.xml'):
        t=ET.parse(path).getroot()
        tests.append(dict(path=str(path.relative_to(ROOT)),tests=int(t.get('tests',0)),failures=int(t.get('failures',0)),errors=int(t.get('errors',0)),skipped=int(t.get('skipped',0))))
totals={k:sum(t[k] for t in tests) for k in ['tests','failures','errors','skipped']}
jars=[]
for path in sorted((ROOT/'dist').glob('*/*.jar')):
    jars.append(dict(path=str(path.relative_to(ROOT)),size=path.stat().st_size,sha256=sha(path)))
passed=[v for v in profiles if v['valid']]
failed=[v for v in profiles if not v['valid']]
build_log=ROOT/'build/gameplay-final-build.log'
build_text=build_log.read_text(errors='replace') if build_log.exists() else ''
build_pass='BUILD SUCCESSFUL' in build_text and 'BUILD FAILED' not in build_text

para('Salts Inventory Update','ReportTitle')
para('Core and compatibility gameplay QA','ReportH1')
para('Evidence report | Experiment branch | 11 October 2026','ReportSmall')
para('This report covers the standalone core, the optional compatibility extension, actual in-world interfaces and synchronization, and production-JAR validation. Results describe the scenarios executed on this computer; they do not establish that every possible mod combination or gameplay action is bug-free.')
table([
    ['Result category','Executed outcome'],
    ['In-world profiles',f'{len(passed)} passed; {len(failed)} final profiles with failures'],
    ['JUnit regression suites',f"{totals['tests']} tests; {totals['failures']} failures; {totals['errors']} errors; {totals['skipped']} skipped"],
    ['Production artifacts',f'{len(jars)} JARs in dist/core and dist/compatibility'],
    ['Final normal root build','PASS' if build_pass else 'No completed final build result'],
], [170,346])
sub('Defects found and corrected')
para('The first inventory-expansion purchase on 1.21.1 could disconnect the client: its legacy topology capability state was never populated. The accepted handshake now installs that state, and connection reset/rejection clears it. The same core source serves Fabric and NeoForge.')
para("Tom's Storage terminal actions updated a detached native cursor without committing the shared desktop cursor. Server callback contexts now expose carriedStack() and setCarriedStack(), and terminal actions commit their cursor before broadcasting. Native tank cursor handling also uses this supported API.")
para('Newer NeoForge clients froze the resource reload listener list before the desktop installed its listener. Registration now uses the early NeoForge reload event. Forge source-set development launches now explicitly include the separate core mixin configuration; production manifests already identify each mod independently.')
para('On 1.20.1, a full content refresh of a menu with custom opening data omitted that data after the first snapshot. The client reconstructed the menu and disconnected. Every full snapshot now carries the factory data; the replacement-session marker is applied only on the first snapshot, matching the newer protocol implementations.')
sub('What the evidence proves')
para('Real game instances created disposable worlds, opened native menu providers, drove desktop mouse callbacks, exchanged authenticated packets, compared authoritative server item counts, and captured framebuffer images. Production launches removed project implementation output directories and loaded the release JARs as separate mods.')
para('Development smoke tests from the earlier refactor are separate evidence and are not included in the in-world counts below. Failed fixture attempts are retained in logs; final results use completed reruns.','ReportSmall')
story.append(PageBreak())

heading('1. Execution method and scope')
para('The opt-in InWorldGameplayHarness runs in the actual Minecraft client with an integrated server. Each profile uses a new flat world and a separate run directory under the module build directory. The driver and QA entrypoint are excluded from production mod artifacts.')
table([
    ['Check','Mechanism'],
    ['Opening / initialization','Native block or item opener, desktop session capture, public integration definition verification'],
    ['Rendering','1280 x 900 native framebuffer captures and visual inspection'],
    ['Item interactions','Mouse press, drag and release callbacks; asynchronous mutation acknowledgements'],
    ['Synchronization','Assertions on the authoritative server inventory and carried stack'],
    ['Optional targets','Extension-only profiles; real target-mod profiles; per-provider recipe browser checks'],
    ['Packaging','Release archives, own metadata, source/bytecode boundaries, production classpath isolation'],
], [155,361])
sub('Reporting conventions')
para('A recorded stage confirms that its scheduled action and wait condition completed. An assertion checks a specific invariant. A capture confirms an image was written. These are reported separately and are not counted as distinct JUnit tests. A successful Gradle launcher exit by itself is not a gameplay pass.')
sub('Commands and reproducibility')
para('Development gameplay: functional-tests/scripts/Run-GameplayQa.ps1 -Scenario core -Minecraft 1.21.1 -Loader fabric. Supported scenarios also include extension-empty, toms, browser-jei, browser-emi, browser-rei and sophisticated. Target fixtures and dependency versions remain pinned.')
para('Production gameplay: gradlew.bat -I functional-tests/packaged-qa.init.gradle :1.21.1:fabric:runPackagedGameplay. Add -PqaExtension=true for the production pair. The QA mod is separate and is not part of either distribution archive.')
para('Normal production verification: gradlew.bat build. API and archive audit: tools/verify-module-boundaries.py. Detailed logs, JSON and screenshots: build/gameplay-qa/.','ReportSmall')
story.append(PageBreak())

heading('2. Standalone core')
para('The core-only profiles install neither the compatibility extension nor its target mods. Completed scenarios exercise the inventory, character, creative, instruction and configuration interfaces; ordinary native containers; and mount screens.')
table([
    ['Feature family','Executed checks'],
    ['Inventory expansion','GUI purchase, exact XP cost, authoritative slot count, reopen, extra-slot pickup and return'],
    ['Item gestures','Pickup/place, right split, one-item placement, merge, left drag 10+10+10, right drag 1+1+1 with 27 remaining'],
    ['Window controls','Move, snapped resize, lock/unlock, pin/unpin, persistent hide/restore'],
    ['Recipe processing','Furnace completes iron smelting; crafting consumes four planks and yields one crafting table'],
    ['Storage / machines','Chest, trapped chest, barrel, ender chest, shulker, hopper, dispenser, dropper, furnace, blast furnace, smoker and brewing stand'],
    ['Other native menus','Crafting, enchanting, anvil, grindstone, loom, smithing, stonecutter, cartography, beacon and crafter where available'],
    ['Mounts','Horse, donkey, llama and camel: captured session, screen rendering and cleanup'],
], [137,379])
para('Opening a menu is not proof that every recipe, trade, enchantment, effect or button combination in that menu was exercised. The detailed limits are listed later in this report.','ReportSmall')
figure('packaged-core','expanded-slot-interaction','Production core only: purchased slot with 16 diamonds returned through the desktop UI.')
story.append(PageBreak())

heading('3. Core rendering evidence')
figure('packaged-core','character','Character window: player model, equipment and crafting interface.')
figure('packaged-core','furnace-output','Furnace output: a real integrated-server smelting operation produced the ingot.')
story.append(PageBreak())
heading('Core rendering evidence, continued')
figure('packaged-core','crafting-output','Crafting result: four planks produce one crafting table before the result is taken.')
figure('packaged-core','window-resized','Resizable storage: snapped grid and scrollbar remain visible after the gesture.')
story.append(PageBreak())

heading('4. Compatibility extension')
table([
    ['Integration','Checks exercised'],
    ["Tom's Storage",'Opening/rendering all eight adapted menu families; connected network with two item types; real withdrawal/deposit; server item conservation; filter, configurator, level emitter, filing cabinet and beacon-backed link interfaces'],
    ['Sophisticated', 'Backpack item source lock and both drag modes; crafting upgrade installation/panel; settings tab navigation/back; opening/rendering chest, barrel and limited barrel'],
    ['JEI / EMI / REI','Separate provider profiles; item tab selection, item search, recipe categories and results, recipe rendering and bookmark toggle'],
    ['Mod Menu','Loaded alongside REI; core configuration factory and rendered configuration screen'],
    ['Absent targets','Extension remains active while target integrations are absent; vanilla functionality still passes'],
], [125,391])
para('Recipe search and bookmark mutations are driven through the public recipe provider contract. Window/slot actions use the game UI callbacks. The report does not substitute registration counts for actual rendered menu checks.')
sub('Target versions')
for profile,version,loader in [('toms','1.21.1','fabric'),('browser-jei','1.21.1','fabric'),('browser-emi','1.21.1','fabric'),('browser-rei','1.21.1','fabric'),('sophisticated','1.21.1','neoforge'),('sophisticated','1.20.1','forge')]:
    value=find(profile,version,loader)
    if value:
        selected={k:v for k,v in value.get('loadedMods',{}).items() if k in ['toms_storage','jei','emi','roughlyenoughitems','modmenu','sophisticatedcore','sophisticatedbackpacks','sophisticatedstorage']}
        para(escape(profile+' '+version+'/'+loader+': '+', '.join(f'{k} {v}' for k,v in selected.items())),'ReportSmall')
story.append(PageBreak())

heading("Tom's Storage evidence")
figure('toms','toms-connected-terminal','Connected terminal: live storage snapshot contains the seeded inventory.')
figure('toms','toms-terminal-deposit','Withdrawal and return: exact item conservation was checked on the authoritative server.')
story.append(PageBreak())
heading('Sophisticated evidence')
figure('sophisticated','backpack-crafting-upgrade-panel','Crafting upgrade: installed through the upgrade rail and expanded native crafting controls.',loader='neoforge')
figure('sophisticated','backpack-settings','Backpack settings: native settings icons and storage cells hosted within the desktop.',loader='neoforge')
story.append(PageBreak())
heading('Recipe browser evidence')
figure('browser-jei','recipe-search','JEI item search after selecting the item ingredient tab.')
figure('browser-rei','recipe-view','REI recipe category and ingredient layout rendered within the desktop.')
story.append(PageBreak())

heading('5. Executed profile matrix')
rows=[['Profile','MC / loader','Stages','Assertions','Images','Outcome']]
for value in profiles:
    records=value['results']
    stages=sum(r.get('details')=='completed in-world stage' for r in records)
    checks=sum(str(r.get('details','')).startswith('assertion') for r in records)
    captures=sum(r.get('status')=='CAPTURED' for r in records)
    rows.append([value['profile'],value['minecraft']+' / '+value['loader'],stages,checks,captures,'PASS' if value['valid'] else f"FAIL ({value['failures']})"])
table(rows,[132,130,49,65,50,90])
for value in failed:
    para(escape(value['profile']+' '+value['minecraft']+' '+value['loader']),'ReportH2')
    for record in value['results']:
        if record.get('status')=='FAIL':para(escape(record['name']+': '+record.get('details','')),'ReportSmall')
story.append(PageBreak())

heading('6. Packaging and regression evidence')
para('The normal root build completed successfully after the gameplay fixes.' if build_pass else 'A completed final normal root build has not been recorded; archive results require that verification.')
para(f"The final JUnit XML records contain {totals['tests']} executed tests, {totals['failures']} failures, {totals['errors']} errors and {totals['skipped']} skipped tests. They cover existing sorting, slot gestures, pickup-all, API contracts, recipe-provider arbitration, initialization isolation and the new live-context cursor guards.")
para('The extension compiles against an API-only archive; it receives the full core only at runtime. Source and bytecode audits inspect forbidden implementation imports and archive contents. Production distributions contain only release task outputs, with independent metadata and dependency direction Compatibility -> Core Public API.')
reload_profiles=[v for v in profiles if any(r.get('name')=='resource-pack-reload' and r.get('status')=='PASS' for r in v['results'])]
if reload_profiles:
    para(escape('Resource-pack reload and the subsequent inventory rendering also passed in: '+', '.join(v['profile']+' '+v['minecraft']+'/'+v['loader'] for v in reload_profiles)+'.'),'ReportSmall')
sub('Production artifact inventory')
rows=[['Artifact','Size (KiB)','SHA-256 prefix']]
for jar in jars:rows.append([jar['path'],round(jar['size']/1024,1),jar['sha256'][:16]])
table(rows,[345,68,103],padding=3)
para('Full artifact SHA-256 values are in dist/core/SHA256SUMS.txt and dist/compatibility/SHA256SUMS.txt. The report manifest records its source results, logs and image hashes.','ReportSmall')
story.append(PageBreak())

heading('7. Limits and remaining validation')
para('These results provide regression evidence, not an exhaustive guarantee of zero bugs. Integrated-server tests do not exercise dedicated-server hosting, remote latency/loss, multiplayer races or reconnect/dimension/death transitions. Arbitrary third-party versions and unrelated mod combinations are not certified.')
para('Vanilla coverage includes opening/rendering every listed menu, but does not cover every trade, enchantment, smithing recipe, beacon effect, brewing recipe, redstone behavior, mount equipment action or creative search combination. Inventory persistence is checked across interface reopening; a saved-world reload is not claimed unless explicitly recorded.')
para('Sophisticated coverage includes the crafting upgrade and listed settings views. Other upgrade families (tank, battery, advanced filters, nested backpacks, mob catcher and every transfer configuration) need additional scenario-specific checks. Recipe transfer, catalysts and every recipe category/provider combination are not exhaustively exercised.')
para('Production-pair launches in this report use Fabric 1.21.1 through the development game launcher, with the actual production JARs remapped by Fabric Loader as needed. They validate archive loading and classpath independence; standalone consumer launchers and Forge/NeoForge production installation are separate scenarios.')
para('Version-specific launch failures and test-driver limitations must be assessed from the final profile matrix and retained launch logs. Successful graphics control launches only establish startup; they do not establish in-world gameplay coverage.','ReportSmall')
sub('26.3 graphics controls')
para('An early instrumented 26.3 launch terminated natively without a Java mod exception. Separate vanilla and production-core startup controls completed. The driver now waits for the client-started lifecycle before creating its world; the subsequent in-world retest is recorded in the matrix.','ReportSmall')
for filename,label in [('graphics-control-263.log','Vanilla + Fabric API'),('graphics-control-core-263.log','Production core + Fabric API')]:
    control=ROOT/'build'/filename
    text=control.read_text(errors='replace') if control.exists() else ''
    ok='SIU_VANILLA_GRAPHICS_CONTROL_FINISH' in text and 'BUILD SUCCESSFUL' in text
    para(escape(label+': '+('PASS: 60-second startup control completed.' if ok else 'No completed control result.')),'ReportSmall')
sub('Evidence locations')
para('build/gameplay-qa/&lt;profile&gt;/&lt;version-loader&gt;/results.json and screenshots/ contain the in-world evidence. launch.log contains each development launch. Production logs: build/packaged-core-qa.log and build/packaged-extension-qa.log. Diagnostic controls: build/graphics-control-263.log and build/graphics-control-core-263.log.')
para('The public shared-cursor API is documented in docs/markdown/jar-separation.md and the generated API signature reference. Test sources and launch scripts live under functional-tests/.')

class NumberedCanvas(canvas.Canvas):
    def __init__(self,*args,**kwargs):super().__init__(*args,**kwargs);self.saved=[]
    def showPage(self):self.saved.append(dict(self.__dict__));self._startPage()
    def save(self):
        count=len(self.saved)
        for state in self.saved:
            self.__dict__.update(state)
            self.setStrokeColor(TEAL);self.setLineWidth(.6);self.line(40,37,556,37)
            self.setFont('Helvetica',8);self.setFillColor(GRAY)
            self.drawString(40,24,'Salts Inventory Update | Gameplay QA | 2026-10-11')
            self.drawRightString(556,24,f'{self._pageNumber} / {count}')
            super().showPage()
        super().save()

OUT.parent.mkdir(parents=True,exist_ok=True)
doc=SimpleDocTemplate(str(OUT),pagesize=(596,842),leftMargin=40,rightMargin=40,topMargin=40,bottomMargin=52,title='Salts Inventory Core and Compatibility - Gameplay QA',author='Repository QA')
doc.build(story,canvasmaker=NumberedCanvas)
manifest=dict(pdf=str(OUT.relative_to(ROOT)),pdf_sha256=sha(OUT),junit=totals,junit_sources=tests,artifacts=jars,profiles=[],root_build=dict(passed=build_pass,path=str(build_log.relative_to(ROOT)),sha256=sha(build_log) if build_log.exists() else None))
for value in profiles:
    manifest['profiles'].append(dict(profile=value['profile'],minecraft=value['minecraft'],loader=value['loader'],passed=value['valid'],results=str(value['path'].relative_to(ROOT)) if value['path'] else None,sha256=sha(value['path']) if value['path'] else None,log=str(value['log'].relative_to(ROOT)),log_sha256=sha(value['log']),screenshots=[dict(path=str(Path(r['path']).relative_to(ROOT)),sha256=sha(Path(r['path']))) for r in value['results'] if r.get('status')=='CAPTURED']))
(OUT.parent/'qa-evidence-manifest.json').write_text(json.dumps(manifest,indent=2))
print(f'{OUT}: {len(profiles)} profiles; JUnit {totals}; {len(jars)} artifacts')
