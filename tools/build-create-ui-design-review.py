"""Create-family design review. Original captures and proposed pixel UI stay separate.

This authors documentation only; no compatibility implementation is changed.
"""
from pathlib import Path
from io import BytesIO
from collections import Counter
import hashlib, json, math, zipfile
from PIL import Image, ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT = Path(__file__).resolve().parents[1]
TMP = ROOT/'build/create-ui-design'
OUT = ROOT/'output/create-ui-design'
PDF = ROOT/'output/pdf/siu-create-family-window-design-review.pdf'
OUT.mkdir(parents=True, exist_ok=True)
(OUT/'mockups').mkdir(exist_ok=True)
(OUT/'native-crops').mkdir(exist_ok=True)
GUI = ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
FRAME = Image.open(GUI/'window.png').convert('RGBA')
CTRL = Image.open(GUI/'window_controls.png').convert('RGBA')
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
ROWS=json.loads((TMP/'native-case-ledger.json').read_text())
BY_ID={r['id']:r for r in ROWS}
RECORDS=json.loads((ROOT/'build/remaining-ui-audit/gameplay/1.21.1-neoforge-native-create-design02/manual-ui-records.json').read_text())
REC={r['id']:r for r in RECORDS}

def fresh(old, capture):
    r=BY_ID[old];n=REC[capture];c=n['observation']['client']
    assert c['siu_loaded'] is False
    r['native'].update(capture=n['capture'],sha256=n['sha256'],bounds=c['bounds'],slots=c['slots'],screen=c['screen'])
    r['minecraft']='1.21.1';r['loader']='neoforge'
    return r

for old,new in [('cplus-typewriter','typewriter-clean'),
 ('item-createbigcannons-timed-fuze','timed-clean'),
 ('item-createbigcannons-proximity-fuze','proximity-clean'),
 ('item-createbigcannons-delayed-inertia-fuze','inertia-clean'),
 ('item-createbigcannons-delayed-impact-fuze','impact-clean'),
 ('item-createshufflefilter-weighted-shuffle-filter','weighted-populated'),
 ('pr-create-attribute-filter','attribute-populated'),
 ('own-create-fabric-schedule','schedule-populated')]:fresh(old,new)

def extra(name, title, parent):
    n=REC[name];c=n['observation']['client'];r=json.loads(json.dumps(BY_ID[parent]))
    r.update(id=name,minecraft='1.21.1',loader='neoforge')
    r['native'].update(capture=n['capture'],sha256=n['sha256'],bounds=c['bounds'],slots=c['slots'],screen=c['screen'],title=title)
    BY_ID[name]=r;return r

extra('schedule-editor','Instruction Editor','own-create-fabric-schedule')
extra('typewriter-key-editor','Key Editor','cplus-typewriter')
extra('typewriter-bindings','Bindings','cplus-typewriter')
cat=json.loads((TMP/'category-extra.json').read_text());BY_ID[cat['id']]=cat

def glyph(ch):
    n=ord(ch);im=ASCII.crop(((n%16)*8,(n//16)*8,(n%16)*8+8,(n//16)*8+8));bb=im.getbbox()
    return im,3 if ch==' ' else (bb[2] if bb else 5)

def tw(s):return sum(glyph(ch)[1]+1 for ch in s)

def text(im,s,x,y,color='#eeeeee'):
    for ch in s:
        g,w=glyph(ch);ink=Image.new('RGBA',(8,8),color);ink.putalpha(g.getchannel('A'))
        im.alpha_composite(ink,(int(x),int(y)));x+=w+1

def frame(w,h,bg):
    out=Image.new('RGBA',(w,h));sheet=FRAME.copy()
    def tint(c):
        if c[3]==0 or max(c[:3])<10:return c
        v=sum(c[:3])/3;delta=v-198
        return tuple(max(0,min(255,round(k+delta))) for k in bg)+(c[3],)
    sheet.putdata([tint(c) for c in sheet.getdata()])
    for src,dst in [((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]:
        out.alpha_composite(sheet.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]),Image.Resampling.NEAREST),dst[:2])
    return out

def native(r,rect,absolute=False):
    p=ROOT/r['native']['capture'];assert hashlib.sha256(p.read_bytes()).hexdigest()==r['native']['sha256']
    im=Image.open(p).convert('RGBA');b=r['native']['bounds'];x,y=(0,0) if absolute else (b['x'],b['y'])
    a=[2*(x+rect[0]),2*(y+rect[1]),2*(x+rect[2]),2*(y+rect[3])]
    assert a[0]>=0 and a[1]>=0 and a[2]<=im.width and a[3]<=im.height,(r['id'],a)
    return im.crop(a).resize((rect[2]-rect[0],rect[3]-rect[1]),Image.Resampling.NEAREST)

def tiled(tile,w,h):
    im=Image.new('RGBA',(w,h))
    for y in range(0,h,tile.height):
        for x in range(0,w,tile.width):im.alpha_composite(tile,(x,y))
    return im.crop((0,0,w,h))

def button(label,w=None,bg=(143,143,137)):
    w=w or tw(label)+12;b=frame(w,18,bg);text(b,label,(w-tw(label))//2,5);return b

def window(body,title,bg,actions=None,standard=False):
    actions=actions or []
    # Full title, even padding, a distinct control row and no title/art collisions.
    count=5 if standard else 2
    w=max(body.width+16,tw(title)+16+count*13,124)
    footer=24 if actions else 0;h=body.height+28+footer
    im=frame(w,h,bg)
    ink='#f3ecdd' if sum(bg)/3<135 else '#352d22'
    text(im,title,7,7,ink)
    cols=[0,6,5,9,2] if standard else [3,2]
    for i,col in enumerate(cols):im.alpha_composite(CTRL.crop((col*11,0,col*11+11,11)),(w-5-(len(cols)-i)*13,5))
    im.alpha_composite(body,((w-body.width)//2,21))
    if actions:
        d=ImageDraw.Draw(im);y=h-26;d.line((6,y,w-7,y),fill=tuple(max(0,v-32) for v in bg))
        left=7;right=w-7
        ordered=[a for a in actions if a[1]=='left']+list(reversed([a for a in actions if a[1]=='right']))
        for label,align,icon in ordered:
            b=button(label,bg=tuple(min(210,v+12) for v in bg))
            if icon:
                b=icon.copy()
            x=left if align=='left' else right-b.width
            im.alpha_composite(b,(x,h-23))
            if align=='left':left+=b.width+3
            else:right=x-3
    return im

BRASS=(190,154,95);WOOD=(145,102,77);BLUE=(116,139,174);PAPER=(183,165,136);DARK=(88,85,74)
SPECS=[]
def add(id,title,family,kind,keep,change,interaction,**opts):
    r=BY_ID[id]
    SPECS.append(dict(id=id,title=title,family=family,kind=kind,keep=keep,change=change,interaction=interaction,**opts))

add('pr-create-toolbox','Toolbox','Create','toolbox',
 'Eight compartment summaries, their native circular arrangement, aggregate quantities and the central disposal action.',
 'One brass SIU window hugs the leather compartment board. The separate inventory tray and floating model are omitted.',
 'Keep native compartment transfer/filter behavior; each summary represents four internal stacks. Never flatten these into 32 unrelated slots.',bg=BRASS)
add('pr-railways-mounted-toolbox','Mounted Toolbox','Steam n Rails','toolbox',
 'The same eight compartment groups and quantity/filter behavior as a placed toolbox.',
 'Reuse the toolbox design and give the window its mounted identity. The conductor/world behind it stays outside the UI.',
 'Keep the menu bound to the native mounted toolbox. The 1.20.1 Fabric and retained 1.21.1 NeoForge variants share this layout.',bg=BRASS)
add('pr-create-schematicannon','Schematicannon','Create','schematicannon',
 'Schematic input, material status, checklist processing, every owned socket, progress and Play/Pause/Stop.',
 'Keep the blue machine board; place Settings and Done beneath it inside the frame. No detached footer or clipped output.',
 'Settings opens a page within this window. Progress and status remain live; the machine is still operated by native Create actions.',bg=BLUE)
add('pr-create-schematic-table','Schematic Table','Create','schematic-table',
 'Schematic file picker, refresh, input/output slots, enable toggle and transfer arrow.',
 'Place refresh beside the file picker, then the full input/output row beneath it. Done becomes an ordinary internal action.',
 'The dropdown opens over the window without being clipped. Empty and populated file lists use the same layout.',bg=BLUE)
add('own-create-fabric-blueprint','Crafting Blueprint','Create','blueprint',
 'The complete 3x3 recipe, result and the smaller overlapping auxiliary cell, plus Clear and Done.',
 'Preserve the blueprint-blue canvas and full cell borders. Leave space around both result cells instead of treating one as decoration.',
 'All eleven native non-player cells keep their meaning and hit regions. This is a ghost recipe editor, not an extra crafting inventory.',bg=BLUE)
add('pr-create-frogport','Package Frogport','Create','frogport',
 'All eighteen package cells, their order, and the native editable title/name action.',
 'A compact blue-gray package window with the name action in a utility row above the full 9x2 grid.',
 'Retain native package transfer. Frogport/mailbox naming and any shared variants use this same window structure.',bg=BLUE,standard=True)
add('pr-create-redstone-requester','Redstone Requester','Create','requester',
 'Nine request entries, the entire wooden rack, paper destination field and both native request-mode buttons.',
 'Use a warm wood frame. Keep the address below the request row and place the mode actions in a clear internal footer.',
 'Request entries remain ghost items with native quantities and scroll behavior; the destination field and tooltips remain editable.',bg=WOOD)
add('pr-create-stock-ticker','Stock Ticker - Categories','Create','category',
 'Category creation, the native category board and the Done action.',
 'Use a shorter wood window for the empty state. Category rows expand the board only when categories exist.',
 'The plus action opens the Category Editor shown on the next page. Do not create a tall empty list.',bg=WOOD)
add('cplus-stock-category-editor','Category Editor','Create','category-editor',
 'The category icon socket, complete name field and native confirmation.',
 'Put icon and name on one row inside the same warm wood family. Keep the full input outline.',
 'The icon is a native reference cell; editing a category must not move or consume an unrelated player inventory item.',bg=WOOD)
add('own-create-stock-request-real','Stock Keeper','Create','stock',
 'Search, native stock-lock control, category grouping, stock quantities, scrollable results, nine-cell request basket, package address and Send.',
 'Replace the very tall empty list with a bounded stock pane. Pin the basket, address and Send below the results.',
 'As stock grows, results scroll within the pane. Basket contents and Send stay visible; category selection does not shift their hit regions.',bg=WOOD)
add('own-create-gauge-set-item','Factory Gauge - Monitor Item','Create','gauge',
 'The native monitor-item reference cell and its two directional pointers.',
 'One small warm-wood body, with a readable title and Done underneath. Keep this picker distinct from the standalone gauge settings.',
 'Selecting the item still configures the original gauge. The main Factory Panel control screen is accounted for in the appendix.',bg=WOOD)
add('pr-create-filter','List Filter','Create','filter',
 'All eighteen ghost entries, the two native mode pairs, Clear and Done.',
 'Preserve the parchment grid. Move the mode icons to a labeled utility band, keeping every slot border clear.',
 'Keep allow/deny and data-matching behavior in the native controls. A filter cell represents a rule, not stored inventory.',bg=PAPER)
add('pr-create-attribute-filter','Attribute Filter','Create','attribute',
 'Reference item, attribute dropdown, Add and Add Inverted, current-rule count, three matching modes, Clear and Done.',
 'Separate the reference row from the mode footer. This populated reference shows one rule, so the design keeps its live rule count.',
 'The dropdown stays full width. Attribute rules and Any/All/None modes remain native; do not replace the rule badge with a storage slot.',bg=PAPER)
add('pr-create-package-filter','Package Filter','Create','package',
 'The package identity icon, full paper/address field, Clear and Done.',
 'One compact blue-gray address window. The field keeps its original height and complete left and right edges.',
 'Typing, cursor selection and address completion remain native. This editor has no ordinary storage grid.',bg=BLUE)
add('pr-create-linked-controller','Linked Controller','Create','controller',
 'Six control groups, all twelve frequency cells, key labels and the wider gap between the four and two groups.',
 'Keep the dark textured panel, brass frame and key labels. Clear and Done sit below the frequency pairs.',
 'Each pair retains native frequency/reference behavior and actual key labels. No frequency cell is hidden by the window title.',bg=BRASS)
add('own-create-fabric-schedule','Train Schedule','Create','schedule',
 'The brass timeline, full instruction card, waiting condition, add/reorder controls, loop mode and Done.',
 'Height follows the visible instruction cards. This one-card state keeps the complete Wait condition and next-entry control.',
 'Open instruction/condition editing inside the schedule window. Longer schedules scroll; empty schedules shrink without losing Add.',bg=DARK)
add('schedule-editor','Schedule - Instruction Editor','Create','schedule-editor',
 'Instruction type dropdown, complete destination field, reference-item area, delete and confirm actions.',
 'Use an attached editor view within the same SIU window instead of stacking a second title and player inventory over it.',
 'Keep native instruction types and condition-specific item cells. Returning to the schedule retains the current scroll and selected row.',bg=DARK)
add('createbigcannons-autocannon-ammo-container','Autocannon Ammo Container','Create Big Cannons','ammo',
 'Both native ammunition cells, the capacity/status bar and ammunition identity indicators.',
 'A small brass/olive window keeps the complete native ammunition row. Remove the unused player inventory and wide gray footer.',
 'Keep both ammo roles and native capacity rules. This is not a generic chest and should not gain an arbitrary sort action.',bg=BRASS)
for suffix,title,value,unit in [('timed','Timed Fuze','1s 0 ticks','time'),('proximity','Proximity Fuze','1 block','distance'),('delayed-inertia','Delayed Inertia Fuze','1s 0 ticks','time'),('delayed-impact','Delayed Impact Fuze','1s 0 ticks','time')]:
    add('item-createbigcannons-'+suffix+'-fuze',title,'Create Big Cannons','fuze',
      'The full native gold ruler, its marker, current '+unit+' setting and confirmation.',
      'Give the current value its own line above the ruler, so it can be read without hovering over and covering the slider.',
      'Retain native scroll adjustment and Shift acceleration. Time and detonation distance keep their distinct units and native ranges.',bg=BRASS,value=value)
add('item-createshufflefilter-shuffle-filter','Equal Shuffle Filter','Create Shuffle Filter','shuffle',
 'All eighteen ghost entries, equal-choice behavior, Clear and Done.',
 'A parchment window around the full 9x2 grid; keep its spacing and use the footer only for actual actions.',
 'Do not invent storage counts or weights here. Every configured entry participates in the native equal-shuffle behavior.',bg=PAPER)
add('item-createshufflefilter-weighted-shuffle-filter','Weighted Shuffle Filter','Create Shuffle Filter','weighted',
 'All eighteen entries and the weight readout beneath each populated entry. The native example has Stone at weight 100.',
 'Keep the extra vertical gap for weights. Each number stays directly under its own cell instead of being squeezed into an item-count corner.',
 'Native per-entry weight adjustment and selection behavior remain intact. Empty entries do not show invented weight values.',bg=PAPER)
add('cplus-typewriter','Linked Typewriter','Create Simulated','typewriter',
 'The full five-row keyboard, original key widths, binding-list action, reset and Done.',
 'A brass SIU frame surrounds the entire keyboard. Hover popovers remain temporary; they are not baked into the keyboard texture.',
 'Selecting a key opens its editor. The bindings view is another state of this same window, with the original mapping data.',bg=BRASS)
add('typewriter-key-editor','Typewriter - Key Editor','Create Simulated','typewriter-key',
 'The complete key text field, two colored frequency/reference cells, confirm and delete actions.',
 'Keep this compact editor attached to the typewriter. Fields and colored cells have their own row below the title.',
 'Preserve focus, native key capture and frequency editing. Returning to the keyboard keeps every mapping and its key identity.',bg=BRASS)
add('typewriter-bindings','Typewriter - Bindings','Create Simulated','typewriter-list',
 'Each binding row with key field, both frequencies, Edit/Delete, plus Add, Reset and Done.',
 'Shrink the empty part of the list; show complete rows and add a scrollbar only when the visible list needs it.',
 'The shown native state contains one binding. Additional rows use the same row height and remain in the same window.',bg=BRASS)

def mock(s):
    r=BY_ID[s['id']];k=s['kind'];bg=s['bg'];actions=[('Done','right',None)]
    if k=='toolbox':
        body=native(r,[33,16,209,142]);actions=[]
    elif k=='schematicannon':
        body=native(r,[2,15,203,105]);actions=[('Settings','left',None),('Done','right',None)]
    elif k=='schematic-table':
        body=Image.new('RGBA',(210,65),bg)
        body.alpha_composite(native(r,[16,20,185,39]),(0,0))
        body.alpha_composite(native(r,[209,20,227,38]),(190,0))
        body.alpha_composite(native(r,[16,54,191,81]),(14,31))
    elif k=='blueprint':
        body=native(r,[8,16,165,78]);actions=[('Clear','left',None),('Done','right',None)]
    elif k=='frogport':
        body=Image.new('RGBA',(166,60),bg);text(body,'Name',0,1,'#283444')
        body.alpha_composite(button('Edit',35,bg),(131,0));body.alpha_composite(native(r,[26,8,190,45]),(1,20))
    elif k=='requester':
        body=native(r,[8,19,222,86]);actions=[('', 'left',native(r,[12,95,48,113])),('Done','right',None)]
    elif k=='category':
        body=native(r,[3,16,188,60]);actions=[('Done','right',None)]
    elif k=='category-editor':body=native(r,[10,17,181,53])
    elif k=='stock':
        body=Image.new('RGBA',(226,176),bg)
        body.alpha_composite(native(r,[46,17,202,35]),(35,0))
        board=tiled(native(r,[90,100,122,116]),214,80);body.alpha_composite(board,(6,25))
        body.alpha_composite(native(r,[23,39,43,57]),(10,30))
        d=ImageDraw.Draw(body);d.rectangle((216,25,220,107),fill='#473729');d.rectangle((216,25,220,46),fill='#ad9272')
        body.alpha_composite(native(r,[12,357,212,390]),(13,110))
        body.alpha_composite(native(r,[1,391,142,414]),(0,149))
        body.alpha_composite(native(r,[145,391,225,414]),(145,149));actions=[]
    elif k=='gauge':body=native(r,[50,18,121,49])
    elif k=='filter':
        body=native(r,[17,20,187,62]);actions=[('', 'left',native(r,[18,75,96,93])),('Clear','right',None),('Done','right',None)]
    elif k=='attribute':
        body=native(r,[10,19,223,48]);actions=[('', 'left',native(r,[15,60,94,79])),('Clear','right',None),('Done','right',None)]
    elif k=='package':
        body=native(r,[11,19,204,43]);actions=[('Clear','left',None),('Done','right',None)]
    elif k=='controller':
        body=native(r,[8,20,165,74]);actions=[('Clear','left',None),('Done','right',None)]
    elif k=='schedule':
        body=native(r,[16,15,236,117]);actions=[('', 'left',native(r,[21,197,81,215])),('Done','right',None)]
    elif k=='schedule-editor':
        body=native(r,[54,64,200,107]);actions=[('Delete','left',None),('Apply','right',None)]
    elif k=='ammo':body=native(r,[22,18,148,44])
    elif k=='fuze':
        body=Image.new('RGBA',(170,56),bg);text(body,s['value'],(170-tw(s['value']))//2,1,'#392c1d')
        body.alpha_composite(native(r,[26,20,153,52]),(21,16))
        actions=[('Apply','right',None)]
    elif k=='shuffle':
        body=native(r,[20,24,198,63]);actions=[('Clear','left',None),('Done','right',None)]
    elif k=='weighted':
        body=native(r,[18,18,212,82]);actions=[('Clear','left',None),('Done','right',None)]
    elif k=='typewriter':
        body=native(r,[3,17,241,98]);actions=[('Reset','left',None),('Bindings','left',None),('Done','right',None)]
    elif k=='typewriter-key':
        body=native(r,[224,158,339,182],absolute=True);actions=[('Delete','left',None),('Apply','right',None)]
    elif k=='typewriter-list':
        body=native(r,[203,139,425,177],absolute=True);actions=[('Reset','left',None),('Add','left',None),('Done','right',None)]
    else:raise ValueError(k)
    header={'gauge':'Monitor Item','category':'Stock Ticker','typewriter-key':'Key Editor','typewriter-list':'Bindings','schedule-editor':'Instruction Editor','ammo':'Ammo Container'}.get(k,s['title'])
    result=window(body,header,bg,actions,s.get('standard',False))
    return result

def original_crop(s):
    r=BY_ID[s['id']];im=Image.open(ROOT/r['native']['capture']).convert('RGB');b=r['native']['bounds']
    x0=b['x']-7;y0=b['y']-7;x1=b['x']+b['width']+104;y1=b['y']+b['height']+15
    # Secondary editors alter their painted bounds without changing menu bounds.
    if s['kind']=='typewriter-list':x0,y0,x1,y1=188,114,458,321
    if s['kind']=='typewriter-key':x0,y0,x1,y1=188,125,450,312
    if s['kind']=='stock':x0,y0,x1,y1=195,0,458,447
    if s['kind']=='schedule-editor':x0,y0,x1,y1=187,145,450,340
    if s['kind']=='category-editor':x0,y0,x1,y1=217,148,480,360
    if s['kind']=='frogport':y0=b['y']-23
    return im.crop((max(0,x0*2),max(0,y0*2),min(im.width,x1*2),min(im.height,y1*2)))

MANIFEST=[]
for s in SPECS:
    r=BY_ID[s['id']];m=mock(s);p=OUT/'mockups'/(s['id']+'.png');m.resize((m.width*4,m.height*4),Image.Resampling.NEAREST).save(p)
    n=OUT/'native-crops'/(s['id']+'.png');original_crop(s).save(n)
    MANIFEST.append({**s,'native':r['native'],'minecraft':r['minecraft'],'loader':r['loader'],'mockup':str(p.relative_to(ROOT)),
      'mockup_sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'native_report_crop':str(n.relative_to(ROOT)),
      'gui_size':list(m.size),'status':'design-only; not an implemented SIU screenshot'})
(OUT/'design-manifest.json').write_text(json.dumps({'scope':'retained Create-family inventory/container screens; non-inventory interfaces in appendix',
 'unique_container_screen_classes':23,'design_pages':len(SPECS),'cases':MANIFEST},indent=2)+'\n')

# Report authoring. Consistent fixed regions leave room for images and readable notes.
for font,name in [('C:/Windows/Fonts/arial.ttf','Body'),('C:/Windows/Fonts/arialbd.ttf','Bold')]:pdfmetrics.registerFont(TTFont(name,font))
W,H=1190,842
C=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
C.setTitle('Create Family - SIU Window Design Review')
C.setAuthor("Salt's Inventory Update")
TEAL='#217c7c';INK='#223738';MUTED='#627477';LIGHT='#eef3f2';GOLD='#ba9357'
PAGES=[]

def txt(t,x,y,size=12,color=INK,bold=False):
    C.setFillColor(HexColor(color));C.setFont('Bold' if bold else 'Body',size);C.drawString(x,y,t)

def para(t,x,y,width,size=12,leading=18,color=INK,bold=False):
    words=t.split();line='';yy=y;name='Bold' if bold else 'Body'
    for word in words:
        cand=(line+' '+word).strip()
        if pdfmetrics.stringWidth(cand,name,size)>width and line:
            txt(line,x,yy,size,color,bold);yy-=leading;line=word
        else:line=cand
    if line:txt(line,x,yy,size,color,bold);yy-=leading
    return yy

def start(title,sub,section='DESIGN REVIEW'):
    C.setFillColor(HexColor('#ffffff'));C.rect(0,0,W,H,fill=1,stroke=0)
    txt('SIU / CREATE FAMILY',42,801,11,TEAL,True);txt(section,920,801,10,MUTED,True)
    txt(title,42,756,29,INK,True);para(sub,42,728,1100,12,18,MUTED)

def end(label):
    n=len(PAGES)+1;PAGES.append(label)
    C.setStrokeColor(HexColor('#d7e1df'));C.line(42,40,W-42,40)
    txt('Native captures without SIU  |  Proposed windows are design mockups',42,24,9,MUTED)
    C.setFont('Body',10);C.drawRightString(W-42,24,str(n));C.showPage()

def fit(im,box,max_scale=None):
    x,y,w,h=box;im=Image.open(im).convert('RGBA') if isinstance(im,(str,Path)) else im
    factor=min(w/im.width,h/im.height)
    if max_scale:factor=min(factor,max_scale)
    iw,ih=im.width*factor,im.height*factor
    C.drawImage(ImageReader(im),x+(w-iw)/2,y+(h-ih)/2,iw,ih,mask='auto')

def card(x,y,w,h,color=LIGHT):
    C.setFillColor(HexColor(color));C.roundRect(x,y,w,h,10,fill=1,stroke=0)

start('Create, made into windows','27 design views covering 23 distinct inventory/container screen classes and their key editor states.')
txt('One familiar frame. Create still feels like Create.',42,677,23,INK,True)
para('Keep native wood, parchment, brass, blueprint panels and custom sockets. Consolidate the useful controls into compact SIU windows with clean edges and deliberate spacing.',42,644,685,16,23)
for idx in [0,2,23]:
    s=MANIFEST[idx];i=[0,2,23].index(idx);x=42+i*373
    card(x,258,355,282)
    fit(ROOT/s['mockup'],(x+15,285,325,230))
    txt(s['title'],x+16,273,12,TEAL,True)
para('Original game UI is always on the left. The right is a proposed SIU design, assembled from native assets and SIU frame sprites. No compatibility code is implemented by this review.',42,203,1090,14,21)
txt('SCOPE',42,119,11,TEAL,True)
para('Create + Big Cannons + Simulated + Shuffle Filter + Steam n Rails. Other Create-family addons and non-inventory interfaces are accounted for in the appendix. Reference releases are named on each page.',42,97,1095,12,18)
end('Cover')

start('Review map','Each entry links to its full original-versus-proposal comparison. Shared layouts and excluded controls are accounted for at the back.')
for i,s in enumerate(SPECS):
    col=0 if i<14 else 1;j=i if i<14 else i-14;x=42+col*566;y=671-j*38
    txt(f'{i+3:02d}',x,y,12,TEAL,True);txt(s['title'],x+40,y,13,INK,True);txt(s['family'],x+40,y-15,10,MUTED)
    C.linkRect('',str(i+3),(x,y-18,x+520,y+15),relative=0,thickness=0)
txt('30  Other Create controls',608,151,12,TEAL,True)
txt('31  Addon controls',608,129,12,TEAL,True)
txt('32  Family coverage and shared layouts',608,107,12,TEAL,True)
txt('33  Reference releases and review limits',608,85,12,TEAL,True)
end('Review map')

for idx,s in enumerate(MANIFEST):
    page=idx+3;C.bookmarkPage(str(page))
    loader='NeoForge' if s['loader']=='neoforge' else 'Fabric'
    start(s['title'],f"{s['family']}  /  Native reference: Minecraft {s['minecraft']} - {loader}")
    txt('ORIGINAL IN GAME / SIU ABSENT',42,679,11,TEAL,True)
    txt('PROPOSED SIU WINDOW / MOCKUP',608,679,11,TEAL,True)
    card(42,237,540,424);card(608,237,540,424,'#e8efef')
    fit(ROOT/s['native_report_crop'],(54,249,516,400))
    fit(ROOT/s['mockup'],(624,259,508,380))
    txt('Preserve',42,207,11,TEAL,True)
    para(s['keep'],42,187,520,12,17)
    txt('Window design',608,207,11,TEAL,True)
    para(s['change'],608,187,520,12,17)
    para('Interaction: '+s['interaction'],42,112,1100,11.5,17,MUTED)
    txt(f"Reference: {s['id']}  |  SHA-256 {s['native']['sha256'][:16]}...",42,58,9,MUTED)
    end(s['title'])

def appendix(title,sub,groups):
    start(title,sub,'APPENDIX')
    for col,items in enumerate(groups):
        x=42+col*566;y=674
        for head,body in items:
            txt(head,x,y,13,TEAL,True)
            y=para(body,x,y-22,520,12,18)-23
    end(title)

appendix('Other Create controls','Accounted for, preserved as native controls. They are outside the requested inventory/container mockup scope.',[
 [('Railway and contraption setup','Train Station and Assembly screens; Elevator Contact floor setup; Sequenced Gearshift programming. These configure the world rather than move inventory.'),
  ('Displays and signals','Display Link, Threshold Switch, Factory Panel settings and Value Settings. Keep their original behavior and mod-specific interactions.'),
  ('Construction tools','Schematic Edit, Schematic Prompt and Tool Selection; Symmetry Wand and Worldshaper. These are native tool/configuration screens.'),
  ('Computer integration','ComputerScreen is a conditional computer/control integration screen. A terminal is not a container storage grid.')],
 [('Radial and direct control','Radial Wrench Menu, Radial Toolbox Menu and linked-controller control overlay stay native. No floating inventory window is proposed for them.'),
  ('Notes and client options','Clipboard, Goggle Config, Create Main Menu and Fabric Pojav Warning. These remain outside inventory window conversion.'),
  ('Nested states included in this review','Schedule Instruction Editor; typewriter keyboard, Key Editor and Bindings; Stock Ticker Category Editor. These are states within audited container screens, so they receive full comparisons.'),
  ('Conditions and responsive content','Schedule conditions, dropdown menus, hover explanations, additional stock/categories and item variants retain native behavior. A finite set of reference states cannot enumerate every possible data combination.')]
])
appendix('Addon controls','Non-inventory interfaces and embedded configuration remain native. The report does not propose new windows for them.',[
 [('Create Big Cannons','Fixed Cannon Mount value control remains native. The ammunition container and all four registered fuze editors are shown in the main review.'),
  ('Create Simulated','Altitude Sensor, Nameplate, Modulating Linked Receiver and Diagram controls stay native. The linked typewriter container and its nested editors are fully designed.'),
  ('Steam n Rails','Bogey style selection and the radial tag-cycle menu stay native. The mounted toolbox reuses the Create toolbox window with its native mounted session.')],
 [('Pattern Schematics','The pattern schematic tool-selection screen is a native tool interface, accounted for here without a container window.'),
  ('Testosterone','The retained addon contributes a client settings screen rather than a separate container screen. Keep its settings and HUD behavior native.'),
  ('Create-linked base screens','An addon can use a Create or vanilla menu without declaring a new screen class. Shared menus inherit their matching design; that does not imply an unseen custom screen was tested.')]
])
appendix('Family coverage and shared layouts','Coverage is tied to the retained releases from the project audit, rather than a claim about every release ever made.',[
 [('23 unique container screen classes','Create: 14. Big Cannons: 5. Simulated: 1. Shuffle Filter: 2. Steam n Rails: 1. All have an original capture and a proposed design in this report.'),
  ('27 comparison views','Four additional nested states receive their own comparison: Stock Ticker Category Editor, Schedule Instruction Editor, Typewriter Key Editor and Typewriter Bindings.'),
  ('Shared variants','Toolbox colors share the same compartment layout. Mounted toolboxes share it too. Frogport/mailbox and data-dependent item states retain their own native title, identity and menu data.')],
 [('No separate container screen found in retained audit','Aeronautics, Offroad, Questing, Crafts & Additions, Slice & Dice, Copycats+, Dragons Plus, Enchantment Industry, Estrogen, Extended Wrenches, Factory and New Age. Their retained audit entries declare no separate container screen; shared Create/vanilla menus still apply.'),
  ('Other family entries','Pattern Schematics and Testosterone have their non-inventory controls accounted for on the preceding page. Create Alloyed was already in the earlier implementation batch and is not a new distinct screen here.'),
  ('Loader duplicates','The audited Create Fabric and NeoForge sets declare the same 14 container classes. The two audited Steam n Rails variants declare the same mounted-toolbox class. Identical layouts are shown once, with the captured release explicitly labeled.')]
])
appendix('References and review limits','The evidence and editable design plan are saved beside this report in output/create-ui-design.',[
 [('Create / Minecraft 1.20.1 Fabric','Create Fabric 6.0.8.1+build.1744-mc1.20.1. Used for the blueprint, toolbox, schematic tools, package screens, monitor-item picker, stock keeper and other retained native captures.'),
  ('Create / Minecraft 1.21.1 NeoForge','Create 6.0.10+mc1.21.1. Used for fresh schedule and attribute-filter states and the retained category editor. Matching screen classes were checked across the audited core releases.'),
  ('Addon releases','Big Cannons 5.11.6; Shuffle Filter 2.1.0; Simulated 1.3.0 embedded in the Aeronautics bundle: Minecraft 1.21.1 NeoForge. Steam n Rails 1.7.3+fabric-mc1.20.1 supplies the mounted-toolbox reference.')],
 [('Evidence integrity','Original native PNGs remain unchanged and are validated by SHA-256. Report crops only reduce surrounding world pixels. Every right-hand image is separately authored and labeled as a mockup.'),
  ('Focused review performed','Reviewed the 23 retained container layouts and refreshed the four fuze views, weighted/attribute filter states, schedule/card editor and typewriter editors in a native client with SIU absent. Inspected every rendered PDF page and every mockup for visual clipping and layout defects.'),
  ('Limits','This is a design review, not an implemented compatibility build or a gameplay certification. No complete gameplay suite was run. Native networking, focus, slot routing, animated content and responsive states must be validated during a future implementation.'),
  ('Local source ledger','output/create-ui-design/design-manifest.json records each capture path, SHA-256, native screen class, mockup path, proposed GUI size and per-screen design notes. build/create-ui-design/native-case-ledger.json preserves the selected audit cases.')]
])
C.save()
(OUT/'page-index.json').write_text(json.dumps({'pdf':str(PDF.relative_to(ROOT)),'pages':PAGES},indent=2)+'\n')
print(json.dumps({'pdf':str(PDF),'pages':len(PAGES),'design_views':len(SPECS),'unique_container_classes':23},indent=2))
