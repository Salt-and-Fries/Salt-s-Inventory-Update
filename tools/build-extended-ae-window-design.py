"""Design-only Extended AE catalog, from immutable original-game captures.

No production SIU code or release JAR is changed. Mockups preserve native
work-area pixels and retain the existing vintage AE2 window-control style.
"""
from pathlib import Path
from io import BytesIO
from zipfile import ZipFile
import json,hashlib,math
from PIL import Image,ImageDraw,ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/extended-ae-window-design'
PDF=ROOT/'output/pdf/siu-extended-ae-ui-window-mockups.pdf'
NEAR=Image.Resampling.NEAREST;BG=(198,198,198,255)
PUB=ROOT/'build/extended-ae-window-design/publishers'
SRC=ROOT/'build/extended-ae-window-design/source/forge-release/src/main/java/com/glodblock/github/extendedae'
FORGE_SOURCE='https://github.com/GlodBlock/ExtendedAE/tree/1694d22005a65bf73faed2aca4d5b33881a5386d'
FABRIC_SOURCE='https://github.com/GlodBlock/ExtendedAE/tree/190c62546deac0d60a2f19fc8193667b06fbfffa'
PROTECTED=[];QA=[]
with ZipFile(PUB/'ExtendedAE-1.20-1.4.18-forge.jar')as z:
 SKIN=Image.open(BytesIO(z.read('assets/ae2/textures/guis/ingredient_buffer.png'))).convert('RGBA')
with ZipFile(PUB/'appliedenergistics2-forge-15.4.12.jar')as z:
 PANEL=Image.open(BytesIO(z.read('assets/ae2/textures/guis/extra_panels.png'))).convert('RGBA')
with ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar')as z:
 ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
ATLAS=ROOT/'build/gameplay-qa/ae2-atlas/1.20.1-fabric/atlas'
CHROME=Image.open(ATLAS/'37-crafting-terminal.png').convert('RGBA')
CONTROLS=json.loads((ATLAS/'37-crafting-terminal.controls.json').read_text())
ICONS=[CHROME.crop((r['x']*2,r['y']*2,(r['x']+11)*2,(r['y']+11)*2)).resize((11,11),NEAR)for r in CONTROLS]
MORE=ICONS[-1].copy();d=ImageDraw.Draw(MORE);d.rectangle((2,2,8,8),fill=BG)
for x in [3,5,7]:d.rectangle((x,5,x,6),fill=(64,64,80,255))

# Every concrete inventory/configuration class is represented. Reused screen
# classes receive additional pages for visibly different work areas.
SPECS=[]
def spec(id,title,short,function,design,group='Machines',only=False,conditional='',**kw):
 SPECS.append(dict(id='forge-'+id,title=title,short=short,function=function,design=design,group=group,forge_only=only,conditional=conditional,**kw))

spec('ex-pattern-provider','Extended Pattern Provider','Pattern Provider',
 'Holds 36 encoded patterns and a separate nine-slot return inventory. Native toolbar controls govern pattern-provider behavior; the wrench opens priority.',
 'Keep the 4 x 9 pattern grid, pattern silhouettes, return label and complete return row together. Priority stays at the top right; SIU controls use More and Close when space is limited.')
spec('ex-interface','Extended Interface','Extended Interface',
 'Configures stocked items or fluids across two pages. Each page has request/amount controls and real buffered storage below the configured entries.',
 'Keep both configuration groups, amount arrows, actual storage and native page-switch toolbar. Hidden slots from the other page must remain hidden; they are not extra visible rows.')
spec('oversize-interface','Oversize Interface','Oversize Interface',
 'A higher-capacity interface using the Extended Interface screen and its two-page stock configuration workflow.',
 'Reuse the Interface window and retain its native page controls, amounts, stock cells and top-right priority button. Capacity changes do not justify rearranging the UI.',only=True)
spec('ingredient-buffer','Ingredient Buffer','Ingredient Buffer',
 'A 36-slot staging inventory for ingredients. It exposes a simple four-row buffer without another machine workflow.',
 'Use one complete 4 x 9 grid with the native slots and a small clear margin below the last row. The player inventory opens separately; no decorative blank lower half.')
spec('ex-drive','Extended ME Drive','Extended Drive',
 'Holds 20 storage cells in two separated 2 x 5 banks. The top-right wrench opens priority.',
 'Keep the two banks at their native spacing. Copy each complete frame with its ghost cell artwork in the same pass, preventing the alignment and bottom-edge problems of the earlier AE2 windows.')
spec('ex-io-port','Extended IO Port','Extended IO Port',
 'Transfers contents between storage cells and the ME network, with input/output cell banks, direction/status controls and upgrade slots.',
 'Keep both 2 x 3 banks, native transfer controls and direction arrow in their original order. Preserve the entire upgrade tray, including the inner edge below a short parent window.')
spec('ex-molecular-assembler','Extended Molecular Assembler','Molecular Assembler',
 'A multi-job assembler with a 3 x 3 crafting area, output, progress and native job/settings controls.',
 'Retain the native grid-to-output arrangement, job label and toolbar. Reserve the work area for any selected job, and leave its full four-slot upgrade tray attached.')
spec('ex-inscriber','Extended Inscriber','Extended Inscriber',
 'Processes four inscription jobs through a shared view: two plates, middle input, output and progress. Native controls switch the job shown.',
 'Keep the plate/input diagram, progress and result together. Preserve next/previous job controls in the left toolbar; do not display all four overlapping menu-slot sets at once.')
spec('caner','ME Caner','ME Caner',
 'Fills or empties supported fluid containers. The toolbar switches the native operating mode; input, direction and result remain visible.',
 'Keep the original input-arrow-output diagram and mode button in a shallow, padded window. Retain fluid/container behavior rather than adding a new transfer control.')
spec('circuit-cutter','Circuit Slicer','Circuit Slicer',
 'Slices printed circuits using its native input, blade/tool cell, output, progress indicator and upgrades.',
 'Keep both adjacent input/tool cells, the full arrow, result and progress strip. The native circuit-slicing setting remains on the left.',only=True)
spec('wireless-connect','ME Wireless Connector','Wireless Connector',
 'Shows link state, energy use, channels and a remote-world preview for a paired connector. The status indicator, tooltip, conditional locate action and upgrades remain available.',
 'Keep the status lines and full preview viewport. A disconnected black preview is the real native idle state; preserve its space for the remote view instead of trimming it away.',group='Network & terminals')
spec('wireless-hub','ME Wireless Hub','Wireless Hub',
 'Manages remote wireless connections, showing power/channels plus per-port remote icons, locate/highlight actions and disconnect actions.',
 'Keep every native port group, including lower rows, at its original spacing. Retain the full upgrade panel; do not replace port actions with SIU movement controls.',group='Network & terminals',only=True)
spec('ex-pattern-access-part','Extended Pattern Access Terminal','Pattern Access',
 'Searches pattern inputs and outputs across connected providers. Its list contains native provider headers and editable pattern rows when providers are connected.',
 'Retain both independent search fields, their input/output icons, native provider-list renderer, scrollbar and left toolbar. The empty captured list must remain a list, not become a fabricated storage grid.',group='Network & terminals',kind='access')
spec('wireless-ex-pat','Extended Wireless Pattern Access Terminal','Pattern Access',
 'The linked, charged item version of Pattern Access. It retains the two searches and provider rows, and adds its own upgrade/toolbox support.',
 'Reuse the Pattern Access work area. Keep item upgrades on the right in a complete native panel; conditional toolbox content must remain reachable when present.',group='Network & terminals',kind='access')
spec('universal-wireless-ex-pat','Universal Wireless Pattern Access','Pattern Access',
 'With AE2WTLib installed, the Extended Wireless Pattern Access Terminal uses its Universal-capable screen and exposes terminal cycling/Quantum Bridge support.',
 'Keep both searches and the native list. Preserve the separate Quantum Bridge and upgrade panels, the terminal-cycle toolbar button and any conditional toolbox. Do not merge visually similar native actions.',group='Network & terminals',kind='access',conditional='Requires AE2WTLib')
for mode in ['Crafting','Stonecutter','Smithing','Anvil']:
 descriptions={
 'Crafting':('Craft from ME storage using the persistent 3 x 3 grid and output.','Keep the 3 x 3 grid, output, arrow and native Store Items / Take Items controls in their original relative positions.'),
 'Stonecutter':('Select a stonecutting result from the native recipe list and process the selected network material.','Keep the ingredient/result column, entire recipe-selection list and its scrollbar. Preserve both native return-item controls on the right.'),
 'Smithing':('Use the smithing template, base item and addition to produce the upgraded result.','Keep all three smithing inputs, their ghost placeholders, the arrow and output. Do not turn hidden crafting/anvil slots into visible cells.'),
 'Anvil':('Rename or combine items with the native anvil inputs, result and text field.','Keep the full rename field above the anvil inputs and output. Retain the original mode tabs and return-item actions; do not clip the lowest Anvil tab.')}
 f,n=descriptions[mode]
 spec('ex-crafting-terminal-'+mode.lower(),'Extended Crafting Terminal / '+mode,'Terminal',
 f+' Four mode tabs share the same searchable ME catalog.',
 'Give search a complete row beneath the SIU title, then move the catalog and selected work panel together. '+n,group='Network & terminals',only=True,kind='crafting')
spec('wireless-ex-ct','Extended Wireless Crafting Terminal','Terminal',
 'The linked, charged item version of the Extended Crafting Terminal. It supports the same crafting, stonecutting, smithing and anvil modes.',
 'Use exactly the four mode layouts shown on the preceding pages. Preserve the native top-right identity/status tab, full five-slot upgrade tray, mode tabs and original action-button positions.',group='Network & terminals',only=True,kind='crafting')
spec('assembler-matrix-populated','Assembler Matrix','Assembler Matrix',
 'A formed, powered multiblock presents remote pattern rows, search, running-job count, cancellation and pattern-visibility controls.',
 'Keep the real native pattern-row grid/list and scrollbar, the search and running-job counter. Preserve its cancellation and visibility toolbar. The encoded pattern shown here is in an actual formed matrix.',group='Network & terminals',only=True,kind='matrix')
for id,title in [('ex-export-bus-part','Extended Export Bus'),('ex-import-bus-part','Extended Import Bus'),('active-formation-plane','Active Formation Plane'),('precise-export-bus','Precise Export Bus'),('precise-storage-bus','Precise Storage Bus'),('threshold-export-bus','Threshold Export Bus')]:
 desc={
 'ex-export-bus-part':'Exports configured items/fluids from ME storage to the attached inventory. Native upgrades unlock additional filter cells and behaviors.',
 'ex-import-bus-part':'Imports from the attached inventory into ME storage, using the native filter, settings and upgrade support.',
 'active-formation-plane':'Places configured items/blocks from the network into the world. Retain the placement filter, priority and native settings.',
 'precise-export-bus':'Exports up to configured per-entry amounts. Middle-click a populated filter to edit its quantity in Select Amount.',
 'precise-storage-bus':'Exposes attached storage with exact per-entry limits/comparison modes. Supports attachment details, priority, partitioning and amount editing.',
 'threshold-export-bus':'Exports using configured thresholds, with native threshold/filter settings and upgrades.'}[id]
 spec(id,title,title,desc,
  'Keep the full native filter field, enabled/disabled cell shading and complete right upgrade tray. Retain the original left settings toolbar and any top-right priority action; SIU must not replace ghost filters with ordinary item slots.',group='Buses & filters')
for id,title in [('mod-export-bus','Mod Export Bus'),('mod-storage-bus','Mod Storage Bus')]:
 spec(id,title,title,
  ('Exports items/fluids matching a mod namespace.'if'export'in id else'Exposes attached storage filtered by mod namespace, with priority and native storage settings.')+' A text field edits the mod ID rather than a nine-column item filter.',
  'Keep the complete native text-field rim and any attachment label. Use a compact body, preserve the left settings toolbar and right upgrade tray, and keep priority at the top right when supplied.',group='Buses & filters')
for id,title in [('tag-export-bus','Tag Export Bus'),('tag-storage-bus','Tag Storage Bus')]:
 spec(id,title,title,
  'Filters '+('exported network contents'if'export'in id else'attached storage')+' with tag expressions. The UI has separate main and whitelist/blacklist filter fields plus operator hints.',
  'Keep both full text areas, their labels and the complete syntax-help block. Preserve priority/settings and the full upgrade tray; the black fields are native editors, not blank space to remove.',group='Buses & filters')
spec('threshold-level-emitter','Threshold Level Emitter','Threshold Emitter',
 'Emits redstone using separate upper/lower thresholds for the configured item or fluid. Native redstone and fuzzy controls remain available.',
 'Keep both numeric fields, full labels and the configured-resource slot at their native relative positions. Leave validation, units and mode behavior with the original widgets.',group='Buses & filters')
spec('renamer','Interface Renamer','Rename',
 'The Certus Quartz Cutting Knife opens a name editor for supported AE2 interfaces. This is an inventory-related configuration dialog with no player slots of its own.',
 'Keep the native Custom Name label and complete editor beneath the SIU title. Preserve normal typing, confirmation and parent return behavior; do not add an unused inventory grid.',group='Tools & dialogs')
for mode in ['multiply','replace','clone']:
 f={
 'multiply':'Multiplies or divides encoded-pattern quantities, or clears patterns, using five native buttons above a 3 x 9 pattern inventory.',
 'replace':'Replaces a chosen ingredient with another across the pattern inventory. Native source/target cells and Replace action sit above the 3 x 9 pattern grid.',
 'clone':'Clones the target pattern into blank patterns, with target/result cells, clone action and a row for blank patterns.'}[mode]
 spec('pattern-modifier-'+mode,'Pattern Modifier / '+mode.title(),'Pattern Modifier',f,
  'Retain the mode switch in the left toolbar. Give the selected mode a small subtitle row, then preserve this mode\'s entire native control group and cells without overlapping hidden slots from the other pages.',group='Tools & dialogs',kind='modifier',mode=mode)
SPECS.append(dict(id='own-eae-set-amount',title='Select Amount',short='Select Amount',group='Tools & dialogs',forge_only=False,conditional='',kind='amount',
 function='An inventory-related amount editor used by precise buses and interface stock controls. Item and fluid number-entry behavior follows the selected resource.',
 design='Keep all native increment/decrement buttons, numeric editor, resource display, Set action and top-right return tab. Return to the originating window; do not create another player inventory.'))
spec('ex-emc-interface','Extended EMC Interface','Extended EMC Interface',
 'With AppliedE and ProjectE installed, configures stock supplied through EMC transmutation. It shares the paged interface-style request/stock workflow.',
 'Keep the native request arrows, stock cells, page switching and right upgrade panel. Use a concise complete title, with More/Close only when needed; do not retain the native overlong title bleeding into the panel.',group='Optional AppliedE',only=True,conditional='Requires AppliedE + ProjectE',short_override='EMC Interface')
for id,title in [('ex-emc-import-bus-part','Extended EMC Import Bus'),('ex-emc-export-bus-part','Extended EMC Export Bus')]:
 spec(id,title,'EMC '+('Import Bus'if'import'in id else'Export Bus'),
  'With AppliedE and ProjectE installed, '+('imports'if'import'in id else'exports')+' through the native EMC integration. It reuses the Extended IO Bus screen with an EMC-specific style.',
  'Keep the complete filter, capacity-card shading and tall upgrade tray. Use a concise title that fits inside the window; preserve any native left toolbar actions.',group='Optional AppliedE',only=True,conditional='Requires AppliedE + ProjectE')

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def load():
 rows={}
 for f in ['prior-captures.json','forge-block-captures.json','forge-extra-captures.json','forge-optional-extra-captures.json']:
  for r in json.loads((OUT/f).read_text()):rows[r['id']]=r
 return rows
def crop(r,box,high=False):
 n=r['native'];b=n['bounds'];p=ROOT/n['capture'];im=Image.open(p).convert('RGBA')
 q=tuple(2*v for v in (b['x']+box[0],b['y']+box[1],b['x']+box[2],b['y']+box[3]))
 assert 0<=q[0]<q[2]<=im.width and 0<=q[1]<q[3]<=im.height,(r['id'],box,q)
 return im.crop(q)if high else im.crop(q).resize((box[2]-box[0],box[3]-box[1]),NEAR)
def glyph(ch):
 n=ord(ch);g=ASCII.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8));b=g.getbbox()
 return g,3 if ch==' 'else b[2]if b else 5
def tw(s):return sum(glyph(c)[1]+1 for c in s)
def text(im,s,x,y,color=(64,64,64,255)):
 assert x>=0 and y>=0 and x+tw(s)<=im.width and y+8<=im.height,(s,im.size,x,y,tw(s))
 for c in s:
  g,w=glyph(c);ink=Image.new('RGBA',(8,8),color);ink.putalpha(g.getchannel('A'));im.alpha_composite(ink,(x,y));x+=w+1
def nine(source,sw,sh,w,h,k=5):
 im=Image.new('RGBA',(w,h),BG)
 for a,b in [((0,0,k,k),(0,0,k,k)),((sw-k,0,sw,k),(w-k,0,w,k)),((0,sh-k,k,sh),(0,h-k,k,h)),((sw-k,sh-k,sw,sh),(w-k,h-k,w,h)),((k,0,k+1,k),(k,0,w-k,k)),((k,sh-k,k+1,sh),(k,h-k,w-k,h)),((0,k,k,k+1),(0,k,k,h-k)),((sw-k,k,sw,k+1),(w-k,k,w,h-k))]:
  im.paste(source.crop(a).resize((b[2]-b[0],b[3]-b[1]),NEAR),b[:2])
 return im
def frame(w,h):return nine(SKIN,176,192,w,h)
def mask_panel(w,h):return nine(PANEL,32,32,w,h,7).getchannel('A')
def clear_outside_world(part):
 # AE2 chrome is greyscale. Colored pixels connected to the outside of a
 # rounded native tab are the captured world, not its inset item icon.
 part=part.copy();todo=[(x,y)for x in range(part.width)for y in [0,part.height-1]]+[(x,y)for y in range(part.height)for x in [0,part.width-1]];seen=set()
 while todo:
  x,y=todo.pop()
  if(x,y)in seen or not(0<=x<part.width and 0<=y<part.height):continue
  seen.add((x,y));r,g,b,a=part.getpixel((x,y))
  if a and max(r,g,b)-min(r,g,b)<=3:continue
  part.putpixel((x,y),(0,0,0,0));todo.extend([(x-1,y),(x+1,y),(x,y-1),(x,y+1)])
 # Remove grey world remnants only at the far outer edge. Never flood
 # through a selected tab's grey face or into its central item sprite.
 for y in range(part.height):
  for x in range(part.width):
   if x<part.width-3 and y<part.height-1:continue
   r,g,b,a=part.getpixel((x,y))
   if a and max(r,g,b)-min(r,g,b)<=3 and r not in {0,55,85,139,171,198,255}:part.putpixel((x,y),(0,0,0,0))
 return part
def header(im,title,badge=0):
 reserve=26 if badge else 0;start=im.width-5-reserve-67
 if 8+tw(title)+5<=start:
  icons=ICONS;names=['focus','pin','lock','link','close'];start=im.width-5-reserve-67
 else:
  icons=[MORE,ICONS[-1]];names=['more','close'];start=im.width-5-reserve-25
 assert 8+tw(title)+5<=start,(title,im.size,start,tw(title))
 text(im,title,8,7)
 for i,icon in enumerate(icons):im.alpha_composite(icon,(start+14*i,5))
 return names
def paste(im,r,box,xy,label,erase=()):
 part=crop(r,box)
 for area in erase:
  x0=max(area[0],box[0])-box[0];y0=max(area[1],box[1])-box[1];x1=min(area[2],box[2])-box[0];y1=min(area[3],box[3])-box[1]
  if x1>x0 and y1>y0:ImageDraw.Draw(part).rectangle((x0,y0,x1-1,y1-1),fill=BG)
 x,y=xy;assert x>=3 and y>=22 and x+part.width<=im.width-3 and y+part.height<=im.height-6,(r['id'],label,im.size,box,xy)
 im.alpha_composite(part,xy);assert im.crop((x,y,x+part.width,y+part.height)).tobytes()==part.tobytes(),label
 PROTECTED.append(dict(id=r['id'],label=label,source=list(box),destination=list(xy),size=list(part.size),identical_pixels=True,excluded_header_badges=list(erase)))
def native_badges(r):
 b=r['native']['bounds'];out=[]
 if r['id']=='own-eae-set-amount':return[(154,0,176,22)]
 if r['id']=='forge-wireless-connect':return[(152,6,168,22)]
 for w in r['native'].get('widgets',[]):
  x=w['x']-b['x'];y=w['y']-b['y']
  if w.get('visible',True)and x>b['width']-40 and y<5 and w['width']>=16:
   # The native TabButton renders a 22-pixel backing even when its click
   # rectangle is only 16 pixels. Capture the whole native backing.
   width=max(22,w['width'],b['width']-x if y<0 else 0)
   out.append((x,y,x+width,y+max(22,w['height'])))
 return out
def design(s,r):
 n=r['native'];b=n['bounds'];w=b['width'];kind=s.get('kind','default');parts=[]
 badges=native_badges(r);first=min([t['y']for t in n['slots']if t['player']and t['y']>0],default=None)
 end=first-11 if first else b['height']-5
 # Avoid a fragment of the old title in Set Amount's native raised return tab.
 if kind=='access':
  nw=195;box=(21,23,203,end);dy=25-23;height=25+(end-23)+7
 elif kind=='crafting':
  nw=203;box=(7,17,195,max(end,144));dy=43-17;height=43+(max(end,144)-17)+7
 elif kind=='modifier':
  nw=w;box=(5,17,w-5,end);dy=38-17;height=38+(end-17)+7
 elif kind=='matrix':
  nw=w;box=(5,17,w-5,end);dy=25-17;height=25+(end-17)+7
 else:
  nw=w;box=(5,14,w-5,end);dy=25-14;height=25+(end-14)+7
 nw=max(nw,tw(s.get('short_override',s['short']))+13+5+(26 if badges else 0)+25)
 im=frame(nw,height)
 # The Crafting tab silhouette ends on the old right edge. Keep the entire
 # sprite, now with an 8-pixel rim, rather than slicing the final tab.
 erase=list(badges);extra_native=[]
 if kind=='crafting':
  erase.extend([(8,first-11,173,first-3),(170,17,195,144)])
 bodyxy=(8 if kind=='access'else box[0],box[1]+dy)
 paste(im,r,box,bodyxy,'complete native work area',erase)
 if kind=='crafting':
  # Native search used to share the title row. Preserve its exact text-field
  # pixels on a dedicated row under SIU chrome, separate from the catalog.
  field=crop(r,(81,4,163,16));wide=Image.new('RGBA',(162,12),BG)
  wide.paste(field.crop((0,0,55,12)),(0,0));wide.paste(field.crop((65,0,66,12)).resize((104,12),NEAR),(55,0));wide.paste(field.crop((79,0,82,12)),(159,0));im.alpha_composite(wide,(8,25))
  PROTECTED.append(dict(id=r['id'],label='full search-field rims and placeholder; stretch blank center only',source=[81,4,163,16],destination=[8,25],size=[162,12],complete_rims=True))
  # Retain actual scroll/tab sprites, without carrying world pixels from
  # the original indented outer silhouette into the new rectangular root.
  sc=(174,17,188,53);xy=(174,17+dy);patch=crop(r,sc);im.alpha_composite(patch,xy);extra_native.append((sc,xy,None))
  for widget in n['widgets']:
   if widget['message']not in ['Crafting','Stonecutter','Smithing','Anvil']:continue
   sy=widget['y']-b['y'];h=widget['height'];rc=(173,sy,195,sy+h);patch=crop(r,rc)
   alpha=Image.new('L',(22,h));ImageDraw.Draw(alpha).polygon([(0,0),(18,0),(20,2),(21,3),(21,h-4),(20,h-2),(18,h-1),(0,h-1)],fill=255)
   patch.putalpha(alpha);patch=clear_outside_world(patch);alpha=patch.getchannel('A');xy=(173,sy+dy);im.alpha_composite(patch,xy);extra_native.append((rc,xy,alpha))
  PROTECTED.append(dict(id=r['id'],label='all four full mode-tab sprites plus scrollbar; outside world masked',complete_rims=True,world_outside_native_silhouette_removed=True))
 if kind=='modifier':text(im,s['mode'].title()+' mode',8,24)
 controls=header(im,s.get('short_override',s['short']),len(badges))
 for box0 in badges:
  part=crop(r,box0);mask=Image.new('L',part.size,255);dm=ImageDraw.Draw(mask)
  # Standard AE2 raised-tab top corners are transparent outside the outline.
  for x,y in [(0,0),(1,0),(0,1),(part.width-1,0),(part.width-2,0),(part.width-1,1)]:dm.point((x,y),fill=0)
  target=(nw-22,4)if r['id']=='forge-wireless-connect'else(nw-25,-4)
  if r['id']!='forge-wireless-connect':part.putalpha(mask);part=clear_outside_world(part)
  parts.append((part,target))
  PROTECTED.append(dict(id=r['id'],label='native top-right badge or status indicator kept complete',source=list(box0),destination=list(target),native_pixels_preserved=True))
 # Native left-toolbar buttons remain on the left, including controls that
 # extend below a short main window. No generic rectangular world crops.
 for widget in n.get('widgets',[]):
  if not widget.get('visible',True):continue
  x=widget['x']-b['x'];y=widget['y']-b['y']
  if x<0 and 0<widget['width']<=22 and widget['height']<=22:
   rect=(x,y,x+widget['width'],y+widget['height']);parts.append((crop(r,rect),(x,y+3)))
 # Find real external slot columns. Panels have 7-pixel native rims around
 # 18-pixel cell envelopes, independent of the root window's height.
 external={}
 for sl in n['slots']:
  if not sl['player']and w<=sl['x']<w+80 and 0<=sl['y']<400:external[(sl['x'],sl['y'])]=sl
 columns={}
 for x,y in external:columns.setdefault(x,[]).append(y)
 for x,ys in columns.items():
  groups=[]
  for y in sorted(set(ys)):
   if groups and y-groups[-1][-1]<=18:groups[-1].append(y)
   else:groups.append([y])
  for group in groups:
   rect=(x-8,group[0]-8,x+24,group[-1]+24);part=crop(r,rect);part.putalpha(mask_panel(part.width,part.height))
   repair=False
   if s['group']=='Optional AppliedE':
    # The original overlong EMC title spills onto the first upgrade cell.
    # Use clean native AE2 upgrade-frame/ghost pixels from an identical tray.
    clean=load()['forge-ex-interface'if len(group)==1 else'forge-ex-export-bus-part']
    cb=clean['native']['bounds'];top=crop(clean,(cb['width']+2,0,cb['width']+34,32));top.putalpha(mask_panel(32,32)if len(group)==1 else mask_panel(32,len(group)*18+14).crop((0,0,32,32)))
    part.paste(top,(0,0));repair=True
   xy=(nw+2,rect[1]);parts.append((part,xy))
   PROTECTED.append(dict(id=r['id'],label='complete native auxiliary tray and inner rim',source=list(rect),destination=list(xy),full_rim=True,native_pixels_preserved=not repair,native_title_bleed_repaired_with_clean_ae2_upgrade_art=repair))
 minx=min([0]+[xy[0]for p,xy in parts]);miny=min([0]+[xy[1]for p,xy in parts]);maxx=max([nw]+[xy[0]+p.width for p,xy in parts]);maxy=max([height]+[xy[1]+p.height for p,xy in parts])
 final=Image.new('RGBA',(maxx-minx+8,maxy-miny+8));off=(4-minx,4-miny);final.alpha_composite(im,off)
 for part,(x,y)in parts:final.alpha_composite(part,(x+off[0],y+off[1]))
 # Preserve fractional-scale native labels at their actual captured 2x
 # resolution. They must not be quantized down to one GUI pixel per glyph.
 final=final.resize((final.width*2,final.height*2),NEAR);body=crop(r,box,True)
 for area in erase:
  x0=max(area[0],box[0])-box[0];y0=max(area[1],box[1])-box[1];x1=min(area[2],box[2])-box[0];y1=min(area[3],box[3])-box[1]
  if x1>x0 and y1>y0:ImageDraw.Draw(body).rectangle((x0*2,y0*2,x1*2-1,y1*2-1),fill=BG)
 pos=((off[0]+bodyxy[0])*2,(off[1]+bodyxy[1])*2);final.alpha_composite(body,pos)
 assert final.crop((pos[0],pos[1],pos[0]+body.width,pos[1]+body.height)).tobytes()==body.tobytes()
 for rc,xy,alpha in extra_native:
  patch=crop(r,rc,True)
  if alpha is not None:patch.putalpha(alpha.resize(patch.size,NEAR))
  final.alpha_composite(patch,((off[0]+xy[0])*2,(off[1]+xy[1])*2))
 return final,dict(root_size=[nw,height],root_origin=list(off),controls=controls,native_work_source=list(box),native_work_offset=[(8-21)if kind=='access'else 0,dy],top_right_badges=len(badges),full_auxiliary_trays=len(external)>0,native_work_preserved_at_capture_resolution=True)
def envelope(r):
 n=r['native'];b=n['bounds'];xs=[0,b['width']];ys=[0,b['height']]
 for w in n.get('widgets',[]):
  if w.get('visible',True):xs +=[w['x']-b['x'],w['x']-b['x']+w['width']+6];ys +=[w['y']-b['y'],w['y']-b['y']+w['height']+6]
 for s in n['slots']:
  if not s['player']and b['width']<=s['x']<b['width']+80 and 0<=s['y']<400:xs +=[s['x']-8,s['x']+24];ys +=[s['y']-8,s['y']+24]
 return(min(xs)-4,min(ys)-4,max(xs)+4,max(ys)+4)

def main():
 for d in ['original','mockups','review']: (OUT/d).mkdir(parents=True,exist_ok=True)
 rows=load();pages=[]
 for s in SPECS:
  r=rows[s['id']];n=r['native'];p=ROOT/n['capture'];assert sha(p)==n['sha256'],s['id'];assert n.get('siu_loaded',False)is False
  full=Image.open(p);assert full.size==(1280,900)
  original=crop(r,envelope(r),True);mock,meta=design(s,r)
  op=OUT/'original'/(s['id']+'.png');mp=OUT/'mockups'/(s['id']+'.png');original.save(op);mock.save(mp)
  page={**s,**meta,'original':op.relative_to(ROOT).as_posix(),'mockup':mp.relative_to(ROOT).as_posix(),'native_capture':n['capture'],'native_sha256':n['sha256'],'screen':n['screen'],'opening':r.get('opening','Middle-click a populated precise-bus ghost filter; also used by Interface stock controls.'),'loader':r.get('loader','fabric'),'release':r.get('release','1.20-1.0.2-fabric'),'source_url':FORGE_SOURCE if r.get('loader')=='forge'else FABRIC_SOURCE}
  pages.append(page)
 inspectors=json.loads((OUT/'pattern-captures-fabric.json').read_text())
 for r in inspectors:
  r['native']['capture']=r['capture'];r['native']['sha256']=r['sha256']
  assert sha(ROOT/r['native']['capture'])==r['native']['sha256'];crop(r,envelope(r),True).save(OUT/'original'/(r['id']+'.png'))
 types=set(p['screen'].split('.')[-1]for p in pages);assert len(types)==31,(len(types),sorted(types))
 assert len(set(r['native']['screen'].split('.')[-1]for r in inspectors))==4
 manifest={'scope':'Minecraft 1.20.1; actual native references and design-only window proposals','inventory_screen_types':31,'inventory_mockup_views':len(pages),'read_only_inspectors':4,'fabric_inventory_types':25,'forge_inventory_types':31,'pages':pages,'read_only':inspectors,'protected_native_regions':PROTECTED,'production_changes':False}
 (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2))
 pdfmetrics.registerFont(TTFont('Segoe','C:/Windows/Fonts/segoeui.ttf'));pdfmetrics.registerFont(TTFont('SegoeB','C:/Windows/Fonts/segoeuib.ttf'))
 W,H=1191,842;c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
 c.setTitle('Extended AE - Native UI analysis and SIU window mockups');c.setAuthor("Salt's Inventory Update / Project Everything")
 page_no=0;boxes=[]
 def txt(t,x,y,size=11,bold=False,color='#19373D'):
  assert x>=25 and x+pdfmetrics.stringWidth(t,'SegoeB'if bold else'Segoe',size)<=W-25,(t,x,size)
  c.setFillColor(HexColor(color));c.setFont('SegoeB'if bold else'Segoe',size);c.drawString(x,y,t);boxes.append([page_no,x,y,size,t])
 def para(t,x,y,width,size=12,leading=17):
  line=''
  for word in t.split():
   trial=(line+' '+word).strip()
   if pdfmetrics.stringWidth(trial,'Segoe',size)>width:
    txt(line,x,y,size);y-=leading;line=word
   else:line=trial
  if line:txt(line,x,y,size);y-=leading
  return y
 def start(title,sub,section='NATIVE UI / PROPOSED WINDOWS'):
  nonlocal page_no;page_no+=1
  txt('PROJECT EVERYTHING / EXTENDED AE / '+section,34,H-31,10,True,'#247C80');txt(title,34,H-75,27,True);txt(sub,34,H-101,11,color='#5E7076')
 def end(label='Original in-game capture + proposed mockup | Design only; not implemented'):
  c.setStrokeColor(HexColor('#D7E2E3'));c.line(34,51,W-34,51);txt(label,34,31,9,color='#5E7076');txt(str(page_no),W-52,31,10,color='#5E7076');c.showPage()
 def imagebox(im,x,y,w,h,scale=None):
  if scale is None:scale=min((w-24)/im.width,(h-24)/im.height)
  dw,dh=im.width*scale,im.height*scale;assert dw<=w and dh<=h
  im=im.resize((round(dw*2),round(dh*2)),NEAR);c.drawImage(ImageReader(im),x+(w-dw)/2,y+(h-dh)/2,dw,dh,mask='auto')
 def panel(x,y,w,h):c.setFillColor(HexColor('#EEF4F4'));c.roundRect(x,y,w,h,7,fill=1,stroke=0)
 start('Extended AE, as SIU windows','Minecraft 1.20.1 | Fabric 1.0.2 + Forge 1.4.18 | Vintage AE2 15.4.x visual family','DESIGN REVIEW')
 txt('31 inventory/configuration screen types',34,688,23,True);txt(str(len(pages))+' mockup views + 4 native pattern inspectors',34,654,16,color='#247C80')
 para('Preserve the mod\'s work areas, ghost slots, item/fluid amounts, native controls and attached panels. Remove the embedded player inventory, add the established SIU / AE2 title controls, and keep all functional content inside complete borders.',34,611,520,15,22)
 para('These are proposed designs. The left-hand images were captured in the real game with SIU absent; the right-hand images are authored mockups. No compatibility implementation, Core/API change or release-JAR rebuild is included.',34,473,520,13,19)
 for key,x,y,w,h in [('forge-ex-crafting-terminal-crafting',625,410,530,270),('forge-assembler-matrix-populated',625,130,300,250),('forge-ex-inscriber',34,130,530,250)]:
  panel(x,y,w,h);imagebox(Image.open(OUT/'mockups'/(key+'.png')),x,y,w,h)
 txt('Same AE2 family. Original layout. Complete sprites.',34,93,13,True)
 end('Research and visual proposals only | Original artwork retained in the working areas')
 # Two compact indexes also make differences between current 1.20.1 releases explicit.
 for offset in [0,20]:
  subset=pages[offset:offset+20]
  start('Interface index'+(' / continued'if offset else''),'F = Fabric 1.0.2 | G = Forge 1.4.18 | Optional dependencies are listed per view.','COVERAGE')
  txt('WINDOW / DISTINCT VIEW',44,697,10,True,'#247C80');txt('RELEASE SCOPE',690,697,10,True,'#247C80');txt('PAGE',1100,697,10,True,'#247C80')
  y=669
  for idx,p in enumerate(subset,offset):
   c.setStrokeColor(HexColor('#E1E9E9'));c.line(40,y-10,1152,y-10)
   txt(p['title'],44,y,12,True);txt(('G only'if p['forge_only']else'F + G')+(' | '+p['conditional']if p['conditional']else''),690,y,10);txt(str(idx+4),1103,y,11);y-=28
  para('Counts describe concrete screens, not tiers or every item/fluid state. Import/export variants, Oversize Interface, three Pattern Modifier modes, four crafting modes and EMC bus styles receive separate mockup views. The older 24-screen audit omitted newly available and conditional screens.',44,97,1090,11,16)
  end('31 inventory/configuration types in the union | Fabric: 25 including WT | Forge: 31 including WT and AppliedE')
 for p in pages:
  start(p['title'],'Reference: Minecraft 1.20.1 / '+p['loader'].title()+' / Extended AE '+p['release']+' / SIU absent',p['group'].upper())
  c.bookmarkPage(p['id']);c.addOutlineEntry(p['title'],p['id'],level=0)
  imgs=[Image.open(ROOT/p[k]).convert('RGBA')for k in ['original','mockup']]
  scale=min(1.025,511/max(i.width for i in imgs),418/max(i.height for i in imgs))
  for x,label,im in [(34,'ORIGINAL IN GAME / SIU ABSENT',imgs[0]),(610,'PROPOSED SIU WINDOW / MOCKUP',imgs[1])]:
   txt(label,x,708,10,True,'#247C80');panel(x,257,547,436);imagebox(im,x,257,547,436,scale)
  txt('WHAT IT DOES',34,231,9,True,'#247C80');a=para(p['function'],34,210,540,12,17)
  txt('WINDOW DESIGN',610,231,9,True,'#247C80');b0=para(p['design'],610,210,540,12,17)
  assert min(a,b0)>=104,(p['id'],a,b0)
  # Keep provenance concise on every page; the manifest has full hashes/paths.
  txt('Open: '+(p['opening']if len(p['opening'])<155 else p['opening'][:151]+'...'),34,96,10)
  txt('Native: '+p['screen'].split('.')[-1]+' | SHA-256 '+p['native_sha256'][:16]+' | Player inventory: separate SIU window',34,76,9,color='#5E7076')
  txt('Release source',1035,76,9,color='#247C80');c.linkURL(p['source_url'],(1035,73,1110,87),relative=0,thickness=0)
  end()
 # The previously unclassified screens are actual read-only pattern viewers.
 for r in inspectors:
  kind=r['id'].replace('eae-pattern-','').title();start(kind+' Pattern Inspector','Native read-only recipe inspection | Not an item-transfer window','APPENDIX / KEEP NATIVE')
  im=Image.open(OUT/'original'/(r['id']+'.png'));scale=min(1.025,511/im.width,418/im.height)
  for x,label in [(34,'ACTUAL NATIVE INSPECTOR / SIU ABSENT'),(610,'RECOMMENDED WITH SIU / KEEP NATIVE')]:
   txt(label,x,708,10,True,'#247C80');panel(x,257,547,436);imagebox(im,x,257,547,436,scale)
  para('Opened from a real encoded '+kind.lower()+' pattern. Ingredients, results, substitutions and quantities are display-only. The original menu rejects placement and pickup, has no player inventory and provides no item-transfer workflow.',34,224,540,13,19)
  para('Retain the original inspector instead of wrapping it in SIU chrome. The right image deliberately repeats the unmodified native view: it is a preservation recommendation, not a proposed inventory window.',610,224,540,13,19)
  txt('Classification: ContainerPattern.DisplayOnlySlot rejects mayPlace / mayPickup; set is a no-op.',34,103,11)
  txt('Capture: original mod packet selected the real screen from an encoded pattern; raw GLFW hotkey not exercised.',34,81,10,color='#5E7076')
  end('Read-only interface accounted for | Native presentation retained under the inventory-only scope')
 start('Shared dialogs and interfaces that stay native','No extra container window is invented for an in-world interaction or a display-only overlay.','APPENDIX / BOUNDARIES')
 groups=[('Existing AE2 windows','Priority, terminal settings, crafting status/plan, naming and other shared AE2 screens reuse their existing SIU family implementation. Opening one must preserve the Extended AE parent context; these are not new Extended AE screen classes.'),('Read-only pattern inspection','Crafting, processing, smithing and stonecutting inspectors were opened and classified. Keep the original recipe-inspection presentation. Their display-only cells must not become transferable SIU slots.'),('World actions and previews','Extended Charger activates in the world, and Crystal Fixer refuels through a world interaction; neither supplies a new container screen. Wireless Connector\'s remote-world preview remains inside its configuration window.'),('Conditional integrations','AE2WTLib adds the Universal-capable Pattern Access screen and Quantum Bridge/toolbox support. AppliedE + ProjectE add the Extended EMC Interface plus EMC import/export styles. These pages show real conditional native screens, not speculative stand-ins.'),('Mode, upgrade and size states','Interface pages, inscriber jobs, filter-capacity upgrades and item/fluid quantity widgets keep their original behavior. Four crafting modes and three Pattern Modifier pages are shown explicitly. Screenshots include idle states; blank remote previews and empty provider lists are not missing artwork.')]
 y=688
 for title,body in groups:txt(title,44,y,16,True);y=para(body,44,y-27,1095,14,21)-29
 assert y>75
 end('Boundary decisions follow the inventory/container scope; no book, recipe overlay or radial menu redesign')
 start('Evidence, release scope and implementation handoff','Original-game reference captures, publisher artifacts and matching release source.','APPENDIX / SOURCES & CHECKS')
 y=690
 for title,body in [('Scope','Minecraft 1.20.1 is the established Project Everything design baseline. Fabric and Forge are audited at their latest published 1.20.1 Extended AE releases as checked November 3, 2026. This PDF is not a later-Minecraft compatibility rollout.'),('Release differences','Fabric 1.20-1.0.2: 25 inventory/configuration classes including optional WT. Forge 1.20-1.4.18: 31 including WT and AppliedE. Forge adds Assembler Matrix, Circuit Slicer, Extended Crafting Terminal, its wireless screen, Wireless Hub and the conditional EMC Interface.'),('Native evidence','Fresh publisher-byte Forge/Fabric profiles contain no SIU Core or Compat JARs. All 31 inventory/configuration classes and four inspectors have native references. Select Amount uses a previously captured original Fabric 1.0.2 view. Fixtures supply world blocks and sample items; original mod menus render the UIs.'),('Visual checks','Mockups retain unchanged native work-area pixels, keep all external trays inside their full 7-pixel rims and reserve top-right badges. All native capture hashes are checked. The final PDF is rendered and reviewed page by page for clipping and layout.'),('Coverage limits','This is design analysis, not a complete gameplay test. Not every recipe, upgrade combination, network load or numeric amount is exercised. A future implementation must route clicks, tooltips, focus, scrolling and hidden-mode slots through the original handlers and then run focused checks.')]:
  txt(title,44,y,15,True);y=para(body,44,y-25,1090,12,18)-19
 assert y>150
 for label,url in [('Extended AE Forge publisher release','https://modrinth.com/mod/extendedae/version/uq3lO4ER'),('Extended AE Fabric publisher release','https://modrinth.com/mod/extendedae/version/Wmmw6SrO'),('Matching Forge 1.4.18 source',FORGE_SOURCE),('Matching Fabric 1.0.2 source',FABRIC_SOURCE)]:
  txt(label,44,y,11,False,'#247C80');c.linkURL(url,(44,y-3,360,y+12),relative=0,thickness=0);y-=19
 end('Machine-readable evidence: output/extended-ae-window-design/manifest.json | No production files changed')
 c.save();(OUT/'pdf-text-layout.json').write_text(json.dumps(boxes,indent=2))
 # Contact sheets are supplementary; Poppler-rendered pages are the final QA.
 font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',14)
 for k in range(0,len(pages),12):
  sheet=Image.new('RGB',(1200,1080),'#edf3f3');dr=ImageDraw.Draw(sheet)
  for j,p in enumerate(pages[k:k+12]):
   im=Image.open(ROOT/p['mockup']);im.thumbnail((280,300),NEAR);x=j%4*300;y=j//4*360;sheet.paste(im,(x+(300-im.width)//2,y+40),im);dr.text((x+8,y+8),p['title'][:32],font=font,fill='#19373d')
  sheet.save(OUT/'review'/f'mockup-contact-{k//12+1}.png')
 (OUT/'verification.json').write_text(json.dumps(dict(inventory_types=len(types),mockup_views=len(pages),native_inspectors=4,pdf_pages=page_no,protected_regions=len(PROTECTED),capture_hashes=True,visual_pdf_review='pending',pdf_sha256=sha(PDF),production_changes=False),indent=2))
 print(json.dumps({'pdf':str(PDF),'pages':page_no,'mockup_views':len(pages),'inventory_types':len(types),'protected_regions':len(PROTECTED)},indent=2))
if __name__=='__main__':main()
