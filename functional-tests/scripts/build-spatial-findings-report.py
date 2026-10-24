"""Render the focused Spatial GUI problem investigation as a documentary PDF."""
from pathlib import Path
import json, hashlib
from xml.sax.saxutils import escape
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.utils import ImageReader
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'build/gameplay-qa/packaged-core/26.3-fabric-spatial-findings-r1'
HUD=ROOT/'build/gameplay-qa/packaged-core/26.3-fabric-spatial-findings-hotbar-r1'
OUT=ROOT/'output/pdf/Salts-Inventory-Spatial-GUI-Findings-Report.pdf'
OUT.parent.mkdir(parents=True,exist_ok=True)
for name,file in [('Body','segoeui.ttf'),('Bold','segoeuib.ttf'),('Mono','consola.ttf')]:pdfmetrics.registerFont(TTFont(name,'C:/Windows/Fonts/'+file))
W,H=960,820
INK=HexColor('#172A36');MUTED=HexColor('#526875');TEAL=HexColor('#087F83');RED=HexColor('#AC3C2D');BG=HexColor('#F5F8FA');LINE=HexColor('#D6E1E7')
BODY=ParagraphStyle('body',fontName='Body',fontSize=12,leading=17,textColor=INK)
SMALL=ParagraphStyle('small',parent=BODY,fontSize=10,leading=14,textColor=MUTED)
LABEL=ParagraphStyle('label',parent=SMALL,fontName='Bold',textColor=TEAL)
TITLE=ParagraphStyle('title',parent=BODY,fontName='Bold',fontSize=25,leading=30)
def text(c,s,x,y,w,style=BODY,limit=120):
 p=Paragraph(escape(s),style);_,h=p.wrap(w,limit)
 if h>limit:raise ValueError('Text overflow: '+s[:80])
 p.drawOn(c,x,y-h);return y-h
def pic(c,path,x,y,w,h):
 with Image.open(path) as im:iw,ih=im.size
 scale=min(w/iw,h/ih);dw,dh=iw*scale,ih*scale
 c.setFillColor(HexColor('#E9EEF1'));c.rect(x,y,w,h,fill=1,stroke=0)
 c.drawImage(ImageReader(str(path)),x+(w-dw)/2,y+(h-dh)/2,dw,dh)
 c.setStrokeColor(LINE);c.rect(x,y,w,h,fill=0,stroke=1)
def shot(n):return RUN/'screenshots'/('finding-'+n+'.png')
def manual(n):return RUN/'screenshots'/('manual-'+n+'.png')
def crop(path,label,box):
 target=ROOT/'tmp/pdfs/spatial-findings'/f'{label}.png';target.parent.mkdir(parents=True,exist_ok=True)
 with Image.open(path) as im:im.crop(box).save(target)
 return target
def meta(n):return json.loads((RUN/('finding-'+n+'.json')).read_text())
pages=[]
def hero(id,title,sub,image,repro,result,impact):pages.append(dict(kind='hero',id=id,title=title,sub=sub,image=image,repro=repro,result=result,impact=impact))
def pair(id,title,sub,left,right,ll,rr,body):pages.append(dict(kind='pair',id=id,title=title,sub=sub,left=left,right=right,ll=ll,rr=rr,body=body))
pages.append(dict(kind='cover',id='INVESTIGATION',title='Spatial GUI compatibility findings'))
pages.append(dict(kind='summary',id='FINDING REGISTER',title='All three reported issues reproduced'))
hero('F01 / HIGH','Windows can lose their controls off-screen','Confirmed in raw game frames and with an actual mouse drag.',shot('right-mid'),
 'In third-person mode at 1280 x 900, move the inventory to the right edge of Core\'s desktop. Repeat with a real title-bar drag.',
 'The inventory is still marked open and visible, but most of its contents and its right-hand title controls are outside the physical screen.',
 'Close, pin and other controls cannot be clicked where they are no longer visible. This is a game framing defect; the PDF embeds the full original framebuffer.')
pair('F01 / MANUAL REPRO','A normal mouse drag reproduces it','Recovery is possible when part of the title bar remains visible.',manual('offscreen-right'),manual('window-recovered'),'After dragging right','After dragging the visible title strip back',
 'Computer use dragged the title from approximately (810,260) to (1230,280), then captured the actual game with F2. The controls went off-screen. Dragging the remaining title strip left recovered the window. Recovery was demonstrated for this partially visible case; completely hidden windows were not tested.')
hero('F01 / GEOMETRY','Desktop bounds do not guarantee screen bounds','The logical window rectangle fits, yet its projection does not.',shot('bottom-right'),
 'Use the bottom-right desktop position in the same third-person setup.',
 'At the right-middle probe, Core reports GUI 640 x 450 and inventory (x=454, y=110, width=178, height=86): x+width=632, inside the GUI. At bottom-right, y=356 and y+height=442 also fit. Both screenshots visibly lose content.',
 'The current boundary rules constrain a 2D desktop rectangle. A usable projected region must also account for the camera, perspective and visible physical screen. This is an architectural inference from the measured state and screenshots, not a completed fix.')
pair('F01 / ASPECT RATIO','A narrower frame clips a previously readable window','Camera parallax disabled for this comparison.',shot('wide-frame'),shot('tall-frame'),'1280 x 720 frame','1000 x 900 frame',
 'Window positions remained (280,110) and (30,130). The inventory was readable in the wide frame but its right side left the narrower frame. The tall-frame GUI was 500 x 450, and the inventory rectangle still fit inside it. Resizing back restored the previous view. The raw images have different aspect ratios and are fully letterboxed here.')
pair('F01 / GUI SCALE','GUI-scale changes also need layout recovery','A related bounds failure; not counted as a separate root cause.',shot('hotbar-scale-two'),shot('hotbar-scale-three'),'GUI scale 2','GUI scale 3',
 'At scale 3 the GUI shrank to 427 x 300 while the retained inventory state was x=280, width=178: its right edge 458 exceeded even the logical width 427. The window clipped. Scale 2 restored the view. This reproduces in the combined client; a Core-only scale comparison was not run, so this report does not attribute that retention behavior exclusively to Spatial GUI.')
hero('F02 / MEDIUM','Detached hotbar is separated from the workspace','The user-reported low placement is present in the full game frame.',shot('baseline'),
 'Disable Return Hotbar to Inventory. Open a chest and player window near desktop y=110 in third person.',
 'The windows end around framebuffer y=390 while the upper HUD group starts around y=738, leaving roughly 350 pixels of empty vertical separation in the 900-pixel-high frame. The item bar sits near the bottom edge.',
 'This is a reproducible layout/ergonomic concern rather than evidence of item loss. The integration captures the normal screen-bottom HUD placement into the entire desktop surface; it has no compact workspace-relative HUD anchor.')
pair('F03 / MEDIUM','Left-side windows overlap the avatar','Confirmed with controlled layout and actual dragging.',shot('left-bottom'),manual('avatar-overlap'),'Controlled left/bottom placement','Actual leftward title drag',
 'Move the chest to the desktop\'s left edge and lower it toward the avatar arm. Core clamps the logical x position to 8, yet the projected chest still covers part of the player. Manual dragging reproduced the overlap. No avatar exclusion region is reserved. This is a presentation/occlusion finding; this run did not establish a wrong world-target or server-state effect.')
pair('F04 / HIGH TRIGGER','Pointer movement can push more UI out of view','Same window positions and held stack; third-person parallax enabled.',shot('carry-interior'),shot('carry-outside'),'Pointer within the panel','Pointer outside at physical (0,0)',
 'Only the pointer position changed between these carried-stack scenes. Camera/parallax changes pushed the inventory farther right and the detached hotbar below the lower screen boundary. The fixed low HUD anchor and lack of projected bounds are amplified by mouse movement. Treat this as a related trigger for F01/F02, not proof of another independent root bug.')
pair('F04 / FIRST PERSON','First-person mouse mode can lose framing too','The corner-pointer case reproduces the earlier PDF\'s right-edge loss.',shot('first-mouse-center'),shot('first-mouse-corner'),'Pointer centered','Pointer in the top-left corner',
 'Enable first-person inventory/container mode with free mouse input. Move the physical pointer from the center to (0,0). The camera/plane presentation shifts and the inventory becomes partly off-screen. The raw framebuffer already contains the loss. Returning the pointer or changing the parallax setting alters the framing; mode activation alone was insufficient as a visual pass criterion in the first report.')
pair('F04 / DIAGNOSTIC CONTROL','Disabling first-person parallax improves this case','A diagnostic setting comparison, not an implementation fix.',shot('first-mouse-corner'),shot('first-no-parallax'),'Parallax enabled','disableFirstPersonParallax enabled',
 'With the corner pointer retained, disabling first-person parallax returned these windows to a readable view. This supports camera/parallax involvement in the clipping. It does not solve arbitrary desktop positions, reserve avatar space, move the low hotbar anchor, or establish that all upstream settings work correctly.')
pair('F05 / MEDIUM','Carried items clip at the captured panel boundary','Interior rendering is intact; the edge case is reproducible.',shot('carry-interior'),shot('carry-panel-left'),'Carried stack at an interior point','Carried stack at logical x=1',
 'Pick up the real chest stack of 32, then move its projected pointer from (200,210) to (1,210). The interior sprite and count are complete. At the left surface boundary only a narrow portion is drawn. Moving outside also shows a clipped fragment at a clamped panel edge. The stack was returned afterward; the server confirmed chest 32 and carried 0. This narrows the earlier PDF concern to boundary visibility, not a general pickup failure.')
pair('F05 / PIXEL DETAIL','Close-up: complete icon versus an edge fragment','Unretouched crops from the two full frames on the preceding page.',
 crop(shot('carry-interior'),'carry-interior-detail',(580,375,680,475)),crop(shot('carry-panel-left'),'carry-edge-detail',(380,395,480,495)),
 'Interior sprite and count','At the captured surface boundary',
 'The crops enlarge the actual framebuffer pixels without repainting the item. The interior stack is intact; the edge sample contains a narrow slice of its sprite/count. The recorded carried count remained 32 in both scenes, and the server confirmed the full stack was returned afterward. This is a feedback/visibility defect at the panel edge, rather than evidence that part of the stack was lost.')
pair('CONTROLS / NO NEW DEFECT CONFIRMED','Movement mode and return remain usable','Focused control observations; these do not erase the layout findings.',shot('gameplay-camera'),shot('desktop-return'),'Movement control active','Desktop control restored',
 'Alt Toggle returned to the gameplay view with the normal HUD, then returned to the same open desktop. First-person no-parallax, curved and crosshair scene captures also completed. They supplied no additional visual defect in this layout. This run did not exhaustively exercise combat, controller input or every modded window.')
pages.append(dict(kind='hud',id='HOTBAR INPUT PROBE',title='Does the low hotbar still accept items?'))
pages.append(dict(kind='actions',id='PRIORITIES',title='What should be addressed first'))
pages.append(dict(kind='evidence',id='METHOD AND LIMITS',title='Evidence, scope and unresolved questions'))

result=json.loads((RUN/'results.json').read_text());hud=json.loads((HUD/'results.json').read_text())
assert result['failures']==0 and hud['failures']==0
sha=hashlib.sha256((ROOT/'dist/core/salts_inventory_update-26.3-Fabric-0.1.5.jar').read_bytes()).hexdigest()
assert sha=='b8edfa2a06290138f9b541c805e46ef7a48886cb780fb6669692051dc298a5a6'
reach=next(r for r in hud['results'] if r['name']=='finding-hud-target-reachable')
on_screen=reach['status']=='PASS'
server_ok=any(r['name']=='finding-hud-server-stack32' and r['status']=='PASS' for r in hud['results'])
c=canvas.Canvas(str(OUT),pagesize=(W,H),pageCompression=1)
c.setTitle("Salt's Inventory + Spatial GUI - Findings Report")
c.setAuthor("Salt's Inventory Update")
manifest=[]
for i,p in enumerate(pages,1):
 c.setFillColor(BG);c.rect(0,0,W,H,fill=1,stroke=0)
 c.bookmarkPage(f'p{i}');c.addOutlineEntry(p['title'],f'p{i}',0,False)
 c.setFont('Bold',10);c.setFillColor(RED if p['id'].startswith('F0') else TEAL);c.drawString(40,790,p['id'])
 c.setFont('Body',9);c.setFillColor(MUTED);c.drawRightString(920,790,'26.3 FABRIC / CORE 0.1.5 / SPATIAL GUI 1.8')
 text(c,p['title'],40,757,880,TITLE,65)
 if p['kind']=='cover':
  text(c,'Follow-up investigation - October 21, 2026',40,716,880,SMALL,25)
  pic(c,manual('offscreen-right'),40,174,880,520)
  text(c,'CONFIRMED GAME FRAMING PROBLEMS',40,148,880,LABEL,20)
  text(c,'The low hotbar, avatar overlap and right-edge window loss reproduce in-game. Additional probes identify boundary, parallax and resize variants.',40,124,880,BODY,55)
  text(c,'Findings only: production code and JARs were preserved. Full original frames are shown; screenshots are not retouched.',40,64,880,SMALL,25)
 elif p['kind']=='summary':
  text(c,'The earlier report\'s passing assertions did not catch these visual defects. This investigation separates visible problems, functional checks and root-cause hypotheses.',40,699,880,BODY,55)
  y=620
  rows=[('F01','HIGH','Projected window bounds','Right/bottom clipping; off-screen title controls; narrower frames and GUI-scale variants.'),('F02','MEDIUM','Detached hotbar anchor','Large gap below windows and little bottom margin.'),('F03','MEDIUM','Avatar overlap','Left-edge windows can render over the player; no reserved avatar area.'),('F04','HIGH TRIGGER','Parallax-sensitive framing','Pointer motion amplifies F01/F02 in first and third person. Related trigger, not a separate proven root cause.'),('F05','MEDIUM','Carried-stack boundary clipping','Interior sprite is complete; near/outside the captured edge it becomes a fragment.')]
  for id,sev,title,desc in rows:
   c.setFillColor(HexColor('#E8EFF3'));c.roundRect(40,y-88,880,88,5,fill=1,stroke=0)
   text(c,id+' / '+sev,55,y-14,150,LABEL,30);text(c,title,220,y-14,680,BODY,25);text(c,desc,220,y-40,680,SMALL,42);y-=101
  text(c,'Severity describes user impact: HIGH can hide controls; MEDIUM affects presentation or feedback. Five finding groups are listed, not five independently proven root defects. No crash, item loss or networking failure was found in these focused probes.',40,100,880,SMALL,55)
 elif p['kind']=='hero':
  text(c,p['sub'],40,716,880,SMALL,30);pic(c,p['image'],40,225,880,468)
  y=202
  for label,value in [('REPRODUCE',p['repro']),('OBSERVED',p['result']),('IMPACT / INTERPRETATION',p['impact'])]:
   text(c,label,40,y,160,LABEL,30);end=text(c,value,205,y,715,SMALL,65);y=end-12
  manifest.append({'page':i,'images':[str(p['image'].relative_to(ROOT))]})
 elif p['kind']=='pair':
  text(c,p['sub'],40,716,880,SMALL,30);pic(c,p['left'],40,340,430,350);pic(c,p['right'],490,340,430,350)
  text(c,p['ll'],40,321,430,LABEL,40);text(c,p['rr'],490,321,430,LABEL,40)
  c.setFillColor(HexColor('#E7F0F2'));c.roundRect(40,125,880,155,6,fill=1,stroke=0)
  text(c,p['body'],58,258,844,BODY,120)
  manifest.append({'page':i,'images':[str(p[k].relative_to(ROOT)) for k in ('left','right')]})
 elif p['kind']=='hud':
  text(c,'Additional focused interaction probe; physical screen bounds are checked before injecting a click.',40,716,880,SMALL,30)
  pic(c,HUD/'screenshots/finding-hud-probe-hover.png',40,340,430,350);pic(c,HUD/'screenshots/finding-hud-probe-result.png',490,340,430,350)
  text(c,'After moving toward the HUD slot',40,321,430,LABEL,40);text(c,'Placement probe result',490,321,430,LABEL,40)
  detail=('The target remained within the monitor. The projected click emptied the carried stack, and the authoritative server inventory held 32 dirt in hotbar slot 2. The low placement is a visual/ergonomic defect in this case; a generic broken hotbar-transfer claim is not supported.' if on_screen and server_ok else 'After pointer movement settled, the projected HUD target was outside the physical monitor. The harness did not inject an impossible off-screen click. This confirms an accessibility problem in this setup; it does not establish that an on-screen HUD slot has incorrect transaction logic.')
  text(c,detail,40,265,880,BODY,100);text(c,'Recorded probe: '+reach['details'],40,143,880,SMALL,50)
 elif p['kind']=='actions':
  y=697
  for label,body in [
   ('1. Projected safe region','Constrain important window content and title controls to a camera-visible usable region, including right/bottom limits. Recompute it for frame size, GUI scale, camera mode and parallax. Reserve avatar space if the avatar should remain unobscured.'),
   ('2. A workspace-relative HUD anchor','Place detached hotbar and survival indicators near the usable window cluster or a deliberate desktop dock. Keep their rendered geometry and slot interaction coordinates derived from the same layout.'),
   ('3. Boundary feedback for carried items','Keep the carried item/count legible as the cursor leaves the captured surface, while preserving outside-drop precedence and the existing server cursor. Verify interior, panel-edge and outside positions separately.'),
   ('Temporary recovery observed','When a title strip remained on-screen, dragging it left recovered the window. Disabling first-person parallax improved the corner-pointer case. These are diagnostic workarounds, not completed fixes or universal recovery guarantees.'),
   ('Verification after fixes','Revisit the exact raw-frame cases, manual rightward drag, aspect/scale changes and hotbar placement. A mode-active or item-count assertion must be accompanied by a visual check of controls, HUD and carried-stack feedback.')]:
   text(c,label.upper(),40,y,880,LABEL,25);y=text(c,body,40,y-23,880,BODY,85)-29
  text(c,'These are proposed correction directions. This task intentionally produced findings rather than changing production behavior.',40,95,880,SMALL,40)
 elif p['kind']=='evidence':
  y=703
  for label,body in [
   ('Executed scope',f"Fresh findings run: {len(result['results'])} result records, zero harness failures. Dedicated hotbar probe: {len(hud['results'])} records, zero harness failures. Records include scene completion, assertions and captures. A passing scene means it was exercised; it does not mean its layout was acceptable."),
   ('Real game evidence','Raw frame sizes: 1280 x 900, 1280 x 720 and 1000 x 900. GUI scales 2/3; first/third-person; free-mouse, curved and crosshair modes; parallax on/off; window edges, larger size, carried-item positions; Alt Toggle and return. Manual computer use reproduced avatar overlap, rightward clipping and partial-title recovery.'),
   ('Control results','The real chest stack was picked up and returned. The server confirmed chest count 32 and carried count 0. The hotbar probe is reported on its own page. No production Java/config resources were edited for this investigation; only opt-in QA/report tooling changed.'),
   ('Unverified / not inferred','No complete gameplay suite, new dedicated-server session, controller/Controlify integration, shader/camera-mod matrix, exhaustive combat/world actions, every upstream setting, extreme FOV matrix or other Minecraft/loader targets. No claim of a crash, duplication or item loss. Attribution to Core versus upstream remains an investigation question for some resize cases.'),
   ('Reproducibility','Run Run-SpatialGuiQa.ps1 -Findings -Manual -RunId spatial-findings-r1, then the -Findings -HotbarProbe profile with run ID spatial-findings-hotbar-r1. Results, PNGs and per-scene geometry JSON are in build/gameplay-qa/packaged-core/26.3-fabric-<run-id>/. Rebuild this document with functional-tests/scripts/build-spatial-findings-report.py.'),
   ('Original PDF reviewed','Reviewed the earlier 30-page visual report, especially pages 6, 13, 19 and 26 and their source PNGs. The full native images already contain the clipping. This report preserves that distinction and replaces visual-readiness assumptions with specific findings.')]:
   text(c,label.upper(),40,y,880,LABEL,20);y=text(c,body,40,y-21,880,SMALL,95)-24
  text(c,'UNCHANGED PRODUCTION CORE SHA-256',40,107,880,LABEL,20)
  text(c,sha,40,84,880,ParagraphStyle('mono',parent=SMALL,fontName='Mono',fontSize=9),22)
  text(c,'Windows | Java 25 | Loader 0.19.5 | Fabric API 0.161.0+26.3 | Spatial-GUI-support-test',40,60,880,SMALL,22)
 c.setStrokeColor(LINE);c.line(40,38,920,38);c.setFillColor(MUTED);c.setFont('Body',8)
 c.drawString(40,23,'Findings and impact / Full original frames / No production fixes in this report task')
 c.drawRightString(920,23,f'{i:02d} / {len(pages):02d}');c.showPage()
c.save()
r=PdfReader(OUT);assert len(r.pages)==len(pages)
OUT.with_suffix('.manifest.json').write_text(json.dumps({'pages':len(pages),'core_sha256':sha,'hotbar_on_screen':on_screen,'hotbar_server_ok':server_ok,'images':manifest},indent=2)+'\n')
print(json.dumps({'pdf':str(OUT),'pages':len(pages),'bytes':OUT.stat().st_size,'hotbar_on_screen':on_screen,'hotbar_server_ok':server_ok},indent=2))
