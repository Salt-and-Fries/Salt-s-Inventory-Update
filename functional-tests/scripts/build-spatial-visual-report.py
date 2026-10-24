"""Build a screenshot-based Spatial GUI report from recorded, packaged-game QA runs.

Run with the bundled Python runtime (reportlab, Pillow and pypdf). Screenshots are
embedded without repainting UI or removing visible in-game defects.
"""
from pathlib import Path
import argparse
import hashlib
import json
from xml.sax.saxutils import escape
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, white
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.utils import ImageReader
from PIL import Image
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[2]
P = argparse.ArgumentParser()
P.add_argument('--run', default='spatial-visual-report-r1')
P.add_argument('--scale-run', default='spatial-visual-scale-r2')
P.add_argument('--output', default='output/pdf/Salts-Inventory-Spatial-GUI-Visual-Report.pdf')
A = P.parse_args()
RUN = ROOT / f'build/gameplay-qa/packaged-core/26.3-fabric-{A.run}'
SCALE = ROOT / f'build/gameplay-qa/packaged-core/26.3-fabric-{A.scale_run}'
SHOTS = RUN / 'screenshots'
OUT = ROOT / A.output
OUT.parent.mkdir(parents=True, exist_ok=True)
fontdir = Path('C:/Windows/Fonts')
for name, filename in [('Body','segoeui.ttf'), ('Bold','segoeuib.ttf'), ('Mono','consola.ttf')]:
    pdfmetrics.registerFont(TTFont(name, str(fontdir / filename)))

W, H = 960, 820
INK, MUTED, TEAL = map(HexColor, ['#172A36','#526875','#087F83'])
BG, LINE, SOFT = map(HexColor, ['#F5F8FA','#D6E1E7','#E7F2F2'])
AMBER = HexColor('#945C0A')
STYLE = ParagraphStyle('body', fontName='Body', fontSize=12, leading=17, textColor=INK)
SMALL = ParagraphStyle('small', parent=STYLE, fontSize=10, leading=14, textColor=MUTED)
LABEL = ParagraphStyle('label', parent=STYLE, fontName='Bold', fontSize=10, leading=14, textColor=TEAL)
TITLE = ParagraphStyle('title', parent=STYLE, fontName='Bold', fontSize=25, leading=30)

def para(c, text, x, top, width, style=STYLE, max_height=160):
    p = Paragraph(text, style)
    _, height = p.wrap(width, max_height)
    if height > max_height:
        raise ValueError(f'Text overflow ({height}>{max_height}): {text[:80]}')
    p.drawOn(c, x, top-height)
    return top-height

def picture(c, path, x, y, width, height):
    if not path.is_file(): raise FileNotFoundError(path)
    with Image.open(path) as im: iw, ih = im.size
    scale = min(width/iw, height/ih)
    dw, dh = iw*scale, ih*scale
    c.setFillColor(HexColor('#E9EEF1')); c.rect(x,y,width,height,fill=1,stroke=0)
    c.drawImage(ImageReader(str(path)),x+(width-dw)/2,y+(height-dh)/2,dw,dh,mask='auto')
    c.setStrokeColor(LINE);c.setLineWidth(.6);c.rect(x,y,width,height,fill=0,stroke=1)

def shot(name): return SHOTS / (name+'.png')
def reportshot(stage): return shot('report-'+stage)
def detail(name,box):
    path=ROOT/'tmp/pdfs/spatial-report'/f'{name}-detail.png'
    path.parent.mkdir(parents=True,exist_ok=True)
    with Image.open(shot(name)) as im: im.crop(box).save(path)
    return path

pages=[]
def hero(section,title,subtitle,image,visible,checked,method='Native game callbacks + state checks'):
    pages.append(dict(kind='hero',section=section,title=title,subtitle=subtitle,image=image,visible=visible,checked=checked,method=method))
def pair(section,title,subtitle,left,right,left_label,right_label,notes,method='Game captures + state checks'):
    pages.append(dict(kind='pair',section=section,title=title,subtitle=subtitle,left=left,right=right,left_label=left_label,right_label=right_label,notes=notes,method=method))

pages.append(dict(kind='cover',section='Visual report',title="Salt's Inventory + Spatial GUI"))
pages.append(dict(kind='overview',section='Reading guide',title='What was captured and what was checked'))
hero('Desktop','Two windows on one spatial desktop','Third-person presentation; container and player inventory stay independent.',
     shot('manual-window-drag'),
     'A chest and a player inventory share the same angled world-space surface. The avatar and world remain visible around them.',
     'The inventory title was dragged with actual computer-use input. Window contents and the other window stayed in place.', 'Manual computer use; fresh F2 capture')
pair('Window geometry','Drag and resize inside the panel','The desktop is spatial; its existing window layout still behaves as a desktop.',
     shot('manual-window-drag'),shot('manual-window-resize'),'After a title-bar drag','After a corner resize',
     'The right window was moved down into view, then enlarged by dragging its bottom-right corner. The native inventory reflowed its slots inside the new frame. These are manual mouse actions, rather than a synthetic illustration.', 'Manual computer use; fresh F2 captures')
hero('Item input','Pick up items at their projected slot','The physical mouse is mapped to the desktop once.',
     shot('spatial-carried-item'),
     'A carried stack and the container slot are shown on the spatial desktop. Hover highlights/tooltips follow the projected pointer.',
     'The native projected click picked up all 32 dirt. The integrated server confirmed source count 0 and canonical carried count 32.')
pair('Visual concern','Carried-item drawing needs a follow-up','An observed visual concern is kept in this report, despite passing transaction checks.',
     shot('spatial-carried-item'),detail('spatial-carried-item',(320,90,650,320)),'Original pickup capture','Unretouched pixel crop of the same capture',
     'The carried-item icon near the left/top of the chest appears partially cut off. This capture does not establish the cause or frequency. Item pickup/place and authoritative counts pass, but that does not prove the carried-stack rendering is visually correct. This is flagged for a targeted follow-up; no production fix was made during this screenshot report.', 'Observed screenshot concern; cause and reproducibility not established')
pair('Item input','Transfer between different windows','Items move between a container and the player inventory while both stay open.',
     shot('manual-item-pickup'),shot('manual-item-placed'),'Picked up from the chest','Placed into the player window',
     'Manual clicks moved the stack from the chest to the resized player inventory. The chest is empty afterward and the player slot displays dirt. Independent automated pickup/place and dedicated-server checks supply the authoritative transaction evidence.', 'Manual computer use; automated transactions checked separately')
hero('Selection rules','Hidden hotbar: selection stays locked','Return Hotbar to Inventory enabled; desktop input owns the mouse.',
     reportshot('spatial-hidden-selection'),
     'The hotbar is hosted inside the inventory window. The normal screen hotbar and its survival indicators are hidden.',
     'Sending number-key and mouse-wheel input did not change the selected held slot while the world hotbar was hidden. A still image cannot prove this; the state assertion did.')
hero('Movement control','Return to the gameplay camera','Window Movement Control active; the workspace remains open.',
     shot('spatial-movement-gameplay-camera'),
     'The view returns to the player camera and the normal hotbar, hearts and hunger become available.',
     'The camera matched the stored gameplay perspective and player eye; the cursor was grabbed. Native mouse deltas turned the player, and number keys/wheel changed the held slot.')
hero('Movement control','Release back to the spatial desktop','Returning control keeps the same windows and container session.',
     shot('spatial-movement-desktop-restored'),
     'The angled desktop and avatar presentation return. Inventory-window hotbar mode hides the normal HUD again.',
     'Movement control exited, the workspace stayed open, the server container session stayed alive, and Core resumed hiding the normal hotbar.')
pair('Movement control','Hold and remapped controls','Default Alt and a custom P binding use the same integration.',
     reportshot('spatial-hold-poll-down'),reportshot('spatial-hold-poll-up'),'Hold active: gameplay view','Hold released: desktop view',
     'Sustained Hold used a QA-only key-polling seam in the real client; production camera, HUD and control-state code still ran. Alt Hold and remapped P Hold passed. P Toggle also passed, and the previous Alt binding stopped toggling after rebinding. Extended physical-key Hold remains unverified.', 'Simulated held-key polling; native Toggle callbacks')
pair('Optional integration','Disable Spatial GUI and restore it','The desktop does not require the spatial presentation to stay usable.',
     shot('spatial-disabled-normal-desktop'),shot('spatial-reenabled'),'Spatial GUI disabled: flat desktop','Spatial GUI enabled again',
     'Disabling the upstream integration returned the desktop to normal screen-space presentation without discarding its windows. Re-enabling hooked the same desktop again.')
hero('View modes','First-person mouse mode','Spatial GUI first-person presentation with free mouse input.',
     shot('spatial-first-person-mouse'),
     'The desktop uses the first-person placement instead of the third-person avatar arrangement.',
     'The upstream effective-first-person state became true. Container and player windows remained on the captured desktop.')
hero('View modes','Curved first-person panel','The optional cylindrical surface uses its own pointer projection.',
     shot('spatial-first-person-curved'),
     'The desktop bends as one surface; the windows are not converted into separate 3D objects.',
     'The cylinder basis was active. A native projected click picked up 32 dirt and a second projected click placed them back.')
hero('View modes','First-person crosshair mode','Spatial GUI crosshair presentation remains available.',
     shot('spatial-first-person-crosshair'),
     'The first-person panel is shown using Spatial GUI crosshair mode.',
     'The upstream crosshair-mode state was active and Core allowed the crosshair to render. This page demonstrates mode/visibility, not a full controller or gaze-input test.')
pair('World interaction','Camera visibility does not bypass a wall','The displayed camera and the player eye may have different sightlines.',
     reportshot('spatial-world-blocked-no-menu'),shot('spatial-world-opened-barrel'),'Obstructed player sightline','Wall removed: reachable barrel opens',
     'A stone block was placed on the player-eye path while leaving the displaced-camera path clear. Use did not open a menu. After removing that block, the physical-pointer ray opened the reachable barrel in the same desktop. The trace also rejected a farther target outside player reach.')
pair('World interaction','Drop carried items outside the panel','An outside drop takes priority over interacting with the world.',
     reportshot('spatial-outside-drop-one'),reportshot('spatial-outside-drop-rest'),'After right-click: one item remains','After left-click: cursor empty',
     'The test seeded a canonical carried count of 2. Right-click outside the panel reduced it to 1, confirmed on the server; left-click dropped the remainder. The desktop did not open an extra world menu and a clamped texture edge did not resolve to a slot.')
pair('Window stacking','Overlap and change focus','Three windows can occupy the same projected desktop.',
     reportshot('spatial-report-overlap'),reportshot('spatial-report-front-window'),'Three-window overlap','A different window brought forward',
     'The chest, player inventory and barrel coexist. The public window focus request succeeded and changed stacking on the shared desktop. Manual dragging and resizing are shown earlier; these two layouts were arranged through the public window controls.', 'Public window controls + live game rendering')
hero('Panel boundary','A window at the panel edge','An intentional boundary placement shows what can leave the visible surface.',
     reportshot('spatial-report-panel-edge'),
     'A window was deliberately moved to the desktop boundary. Extreme placement and perspective can push part of a window toward or beyond the visible camera frame.',
     'With the physical pointer outside the panel, the clamped edge query returned no slot. This does not claim automatic camera reframing or guarantee that every extreme placement stays fully visible.', 'Public layout placement + native pointer/state check')
pair('Screen settings','Respect explicit screen exclusions','The excluded desktop uses normal Core presentation.',
     reportshot('spatial-report-explicit-exclusion'),reportshot('spatial-report-exclusion-restored'),'Desktop class excluded','Exclusion removed',
     'Adding the desktop class to Spatial GUI disabledScreens deactivated the integration while retaining all three windows. Removing the exclusion reactivated the panel.')
pair('Screen settings','Respect the container category switch','A workspace containing a container follows the container setting.',
     reportshot('spatial-report-containers-disabled'),reportshot('spatial-report-containers-restored'),'Container category disabled','Container category enabled',
     'Turning off Spatial GUI containers returned this multi-window workspace to the normal desktop; turning it back on restored capture. The player-only category is checked separately.')
pair('Screen settings','Respect the player inventory switch','A player-only workspace follows the inventory category.',
     reportshot('spatial-report-inventory-disabled'),reportshot('spatial-report-inventory-restored'),'Inventory category disabled','Inventory category enabled',
     'After all server-backed containers were closed, a single player inventory was opened. The inventory category switch controlled capture independently of the container category.')
pair('Perspective settings','Respect first- and third-person exclusions','Exclusion is based on the stored gameplay perspective.',
     reportshot('spatial-report-perspective-exclusion'),reportshot('spatial-report-third-exclusion'),'First-person perspective excluded','Third-person perspective excluded',
     'Both perspective exclusion settings deactivated the integration in their corresponding gameplay camera modes. The desktop stayed available in normal presentation; restoring each setting reactivated the panel.')
pair('Perspective settings','Automatic camera-mode detection','The upstream auto-detect setting follows the gameplay perspective.',
     reportshot('spatial-report-autodetect-first'),reportshot('spatial-report-autodetect-third'),'Stored perspective: first person','Stored perspective: third person',
     'With autoDetectCameraMode enabled, the upstream effective mode changed to first person for FIRST_PERSON and to third person for THIRD_PERSON_BACK. This checks mode selection; animation timing was not measured frame by frame.')
pair('GUI scale','Change GUI scale with windows open','A narrow follow-up uses Minecraft 26.3 current resize method.',
     SCALE/'screenshots/report-spatial-report-gui-scale-three.png',SCALE/'screenshots/report-spatial-report-gui-scale-two.png','GUI scale 3','GUI scale 2 restored',
     'The first visual run called an obsolete test-harness resizeDisplay method, producing two fixture errors. The harness was corrected to resizeGui and only this focused scale probe was repeated. The follow-up passed, retained the inventory and kept spatial capture active. No production code changed for this correction.')
hero('HUD alignment','Detached hotbar and indicators share the plane','Return Hotbar to Inventory disabled.',
     shot('spatial-detached-hotbar-on-panel'),
     'The detached hotbar and its survival indicators are rendered on the same tilted surface as their desktop interaction targets.',
     'The detached-hotbar option remained active. Earlier visual checks confirmed there was no duplicate flat hotbar/indicator group; movement control restores the normal gameplay HUD.')
pair('HUD settings','Respect Spatial GUI HUD hiding','The upstream hide settings still control the desktop presentation.',
     reportshot('spatial-report-hide-hud'),reportshot('spatial-report-hide-hotbar'),'Hide HUD enabled','Hide hotbar enabled',
     'Both upstream hide settings were enabled separately. The corresponding upstream state returned true and the captures record each view. These are desktop-mode settings; movement control intentionally restores gameplay HUD availability.')
pair('Multiplayer','A Core-only server accepts both clients','Recorded dedicated-server checks from the implementation validation.',
     ROOT/'build/gameplay-qa/packaged-core/26.3-fabric-spatial-remote-present-final/screenshots/remote-spatial-desktop.png',
     ROOT/'build/gameplay-qa/packaged-core/26.3-fabric-spatial-remote-absent-r1/screenshots/remote-core-desktop.png',
     'Client: Core + Spatial GUI','Client: Core alone',
     'The localhost dedicated server contained only production Core plus the normal Fabric runtime. Both clients connected and moved one 32-item dirt stack from slot 9 to slot 10. Independent RCON reads confirmed one destination stack and no remaining source stack. The same production Core hash was used on both sides.', 'Earlier dedicated-server captures + independent RCON inventory checks')
hero('Lifecycle','Close the desktop and return to play','No open desktop or retained server container session after cleanup.',
     shot('spatial-closed-gameplay'),
     'The workspace is closed and the game has returned to the normal player view.',
     'All container sessions were closed, the native screen was cleared, and the desktop no longer owned movement control. Closing was exercised after both automation and the manual window/item actions.')
pages.append(dict(kind='evidence',section='Evidence',title='Results, artifact identity and coverage limits'))

visual=json.loads((RUN/'results.json').read_text())
scales=json.loads((SCALE/'results.json').read_text())
assert scales['failures']==0, 'Scale follow-up failed'
failed=[r for r in visual['results'] if r['status']=='FAIL']
assert {r['name'] for r in failed}=={'spatial-report-gui-scale-three','spatial-report-gui-scale-two'},failed
for r in failed: assert 'resizeDisplay' in r['details']
core=ROOT/'dist/core/salts_inventory_update-26.3-Fabric-0.1.5.jar'
sha=hashlib.sha256(core.read_bytes()).hexdigest()
assert sha=='b8edfa2a06290138f9b541c805e46ef7a48886cb780fb6669692051dc298a5a6'

c=canvas.Canvas(str(OUT),pagesize=(W,H),pageCompression=1)
c.setTitle("Salt's Inventory + Spatial GUI - Visual Compatibility Report")
c.setAuthor("Salt's Inventory Update")
c.setSubject('Real Minecraft 26.3 Fabric screenshots, focused edge cases and verification limits')
manifest=[]
for index,page in enumerate(pages,1):
    c.setFillColor(BG);c.rect(0,0,W,H,fill=1,stroke=0)
    c.bookmarkPage(f'page{index}');c.addOutlineEntry(page['title'],f'page{index}',level=0,closed=False)
    c.setFillColor(TEAL);c.setFont('Bold',10);c.drawString(40,H-30,page['section'].upper())
    c.setFont('Body',9);c.setFillColor(MUTED);c.drawRightString(W-40,H-30,'26.3 FABRIC  /  CORE 0.1.5 + SPATIAL GUI 1.8')
    if page['kind']=='cover':
        para(c,page['title'],40,756,880,TITLE,70)
        para(c,'Visual compatibility report - October 21, 2026',40,718,880,STYLE,35)
        picture(c,shot('manual-window-resize'),40,145,880,550)
        para(c,'REAL GAMEPLAY CAPTURES',40,121,420,LABEL,20)
        para(c,'One spatial surface. Existing windows, item interactions and server sessions.',40,98,880,STYLE,50)
        para(c,'Focused edge cases with captions, before/after comparisons and explicit coverage limits.',40,63,880,SMALL,30)
    elif page['kind']=='overview':
        para(c,page['title'],40,756,880,TITLE,45)
        y=697
        for label,text in [
            ('Scope','Minecraft 26.3 Fabric only. The production Core JAR is tested alongside the published Spatial GUI 1.8 release. Compatibility and Controlify are not part of these scenes.'),
            ('Fresh captures','The main visual run records first/third person, curved and crosshair modes, input/camera handoff, world reach/obstruction, outside drops, overlap/focus, exclusions, GUI scale and HUD settings.'),
            ('Evidence labels','Manual: actual computer-use mouse actions. Native callbacks: Minecraft input handlers dispatched by a separate QA mod. Polling seam: sustained Hold simulated at key polling. Public controls: window geometry/focus set through supported window handles.'),
            ('What a picture proves','A screenshot shows presentation at one instant. Transaction counts, camera coordinates, selected slots, exclusions and server state are checked separately and described in each caption.'),
            ('Capture integrity','Images are real game framebuffer captures, embedded at their original resolution. They are scaled to fit the page, not repainted or retouched. Some boundary scenes intentionally show partial visibility or clipping.'),
            ('Focused testing','This is a broad visual review of the affected integration, not the complete mod gameplay suite. A still-image PDF does not demonstrate continuous animation, input latency or every possible mod combination.')]:
            para(c,label.upper(),40,y,880,LABEL,22)
            y=para(c,escape(text),40,y-22,880,STYLE,85)-28
        para(c,'Use the PDF bookmarks to jump directly to an edge case. The last page records fixture errors, the corrected follow-up, exact artifact identity and remaining limits.',40,154,880,SMALL,65)
    elif page['kind']=='hero':
        para(c,page['title'],40,756,880,TITLE,45)
        para(c,escape(page['subtitle']),40,718,880,SMALL,35)
        picture(c,page['image'],40,170,880,525)
        para(c,'WHAT YOU CAN SEE',40,147,425,LABEL,20)
        para(c,escape(page['visible']),40,127,425,SMALL,85)
        para(c,'BEHAVIOR CHECKED',495,147,425,LABEL,20)
        para(c,escape(page['checked']),495,127,425,SMALL,85)
        manifest.append({'page':index,'title':page['title'],'images':[str(page['image'].relative_to(ROOT))]})
    elif page['kind']=='pair':
        para(c,page['title'],40,756,880,TITLE,45)
        para(c,escape(page['subtitle']),40,718,880,SMALL,35)
        picture(c,page['left'],40,340,430,350);picture(c,page['right'],490,340,430,350)
        para(c,escape(page['left_label']),40,322,430,LABEL,40)
        para(c,escape(page['right_label']),490,322,430,LABEL,40)
        c.setFillColor(SOFT);c.roundRect(40,132,880,145,8,fill=1,stroke=0)
        para(c,'READING THIS COMPARISON',58,256,844,LABEL,20)
        para(c,escape(page['notes']),58,232,844,STYLE,90)
        manifest.append({'page':index,'title':page['title'],'images':[str(page[k].relative_to(ROOT)) for k in ('left','right')]})
    elif page['kind']=='evidence':
        para(c,page['title'],40,756,880,TITLE,45)
        y=702
        blocks=[
            ('Fresh visual run',f"{sum(r['status']=='PASS' for r in visual['results'])} passing assertion/stage records; {sum(r['status']=='CAPTURED' for r in visual['results'])} automatic framebuffer records. Two GUI-scale fixture errors called the obsolete resizeDisplay method. They are retained in the original results and are not described as production failures or a clean full-run pass."),
            ('Corrected scale follow-up',f"{len(scales['results'])} total records, zero failures. Only the setup, scale change/restoration and cleanup were repeated after fixing the test harness. No production code was changed. Result records count stages, assertions and captures, not independent test cases."),
            ('Other executed evidence','Earlier focused production checks passed: 96 gameplay result records; 54 control result records; three movement-state unit tests; six compatibility-coverage regressions; target build and production boundary audit. Both Core-only-server client configurations passed authoritative inventory checks.'),
            ('Still unverified','Extended physical keyboard Hold; every animation frame and timing; controller/Controlify presentation; shaders and unrelated camera/HUD mods; every modded container or upstream setting combination; other Minecraft versions/loaders; long multiplayer stress sessions. Entity combat and every world action were not exhaustively tested.'),
            ('Visible boundary behavior','Extreme window positions can reach or leave the visible camera frame. Those images are retained rather than cropped to conceal the result. No automatic camera reframing guarantee is implied.'),
            ('Visual follow-up','The pickup capture shows a possibly clipped carried-item icon. It is enlarged earlier in this report. Transaction correctness passed, but the visual concern needs a focused follow-up; its cause and frequency were not established.'),
            ('Reproducibility','Main: '+str(RUN.relative_to(ROOT))+'; follow-up: '+str(SCALE.relative_to(ROOT))+'. Each contains results.json, launch.log and screenshots. The production artifact was unchanged during this report; all capture instrumentation is in the separate QA mod.')]
        for label,text in blocks:
            para(c,label.upper(),40,y,880,LABEL,20)
            y=para(c,escape(text),40,y-20,880,SMALL,90)-20
        para(c,'PRODUCTION CORE SHA-256',40,112,880,LABEL,20)
        para(c,sha,40,91,880,ParagraphStyle('hash',parent=SMALL,fontName='Mono',fontSize=9),25)
        para(c,'Loader 0.19.5 | Fabric API 0.161.0+26.3 | Java 25 | Windows | Branch: Spatial-GUI-support-test',40,62,880,SMALL,25)
    c.setStrokeColor(LINE);c.line(40,38,W-40,38)
    c.setFillColor(MUTED);c.setFont('Body',8)
    method=page.get('method','Visual compatibility review | Focused scope and explicit limits')
    c.drawString(40,23,method)
    c.drawRightString(W-40,23,f'{index:02d} / {len(pages):02d}')
    c.showPage()
c.save()
reader=PdfReader(OUT)
assert len(reader.pages)==len(pages)
text='\n'.join(p.extract_text() or '' for p in reader.pages)
assert 'Still unverified'.upper() in text and 'resizeDisplay' in text
manifest_path=OUT.with_suffix('.manifest.json')
manifest_path.write_text(json.dumps({'pdf':str(OUT),'pages':len(pages),'core_sha256':sha,'visual_run':A.run,'scale_run':A.scale_run,'screenshots':manifest},indent=2)+'\n')
print(json.dumps({'pdf':str(OUT),'pages':len(pages),'bytes':OUT.stat().st_size,'manifest':str(manifest_path)},indent=2))
