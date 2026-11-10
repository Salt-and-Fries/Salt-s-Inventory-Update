"""Second Create window design set: rebuilt components, not cropped panels.

Documentation/mockup authoring only. Native game evidence and the prior review
remain untouched. Illustrative content is always identified as proposed data.
"""
from pathlib import Path
from io import BytesIO
from collections import Counter
import hashlib,json,math,re,zipfile
from PIL import Image,ImageDraw
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from pypdf import PdfReader,PdfWriter
from pypdf.generic import NameObject,BooleanObject

ROOT=Path(__file__).resolve().parents[1]
TMP=ROOT/'build/create-window-overhaul'
OUT=ROOT/'output/create-window-overhaul'
PDF=ROOT/'output/pdf/siu-create-family-complete-window-overhaul.pdf'
for p in [TMP,OUT,OUT/'windows',OUT/'states',OUT/'assets']:p.mkdir(parents=True,exist_ok=True)
OLD=json.loads((ROOT/'output/create-ui-design/design-manifest.json').read_text())['cases']
CASES={x['id']:x for x in OLD}
AUDIT=json.loads((ROOT/'output/create-ui-visual-audit/findings.json').read_text())
GUI=ROOT/'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
FRAME=Image.open(GUI/'window.png').convert('RGBA');CTRL=Image.open(GUI/'window_controls.png').convert('RGBA')
CREATE=zipfile.ZipFile(ROOT/'build/ui-modpack-audit-20261025/jars/f750d019bc1230c8c7a8a4ccf549d1db192d7a69.jar')
MC=zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar')
CBC=zipfile.ZipFile(ROOT/'build/ui-modpack-audit-20261025/jars/b209fe9879b27e74b742db9127a1b88d49323abe.jar')
SIM=zipfile.ZipFile(ROOT/'build/remaining-ui-audit/nested/eb02fbe3c51cb5de996cf56d871670545e1f46e8.jar')
ASCII=Image.open(BytesIO(MC.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
ASSET_CACHE={};GEOMETRY=[];MAIN={};STATES=[];LABELS=[]

def png(z,path):return Image.open(BytesIO(z.read(path))).convert('RGBA')
def asset(name):
    if name not in ASSET_CACHE:ASSET_CACHE[name]=png(CREATE,'assets/create/textures/gui/'+name+'.png')
    return ASSET_CACHE[name]
def parse_atlas(path):
    out={}
    for name,sheet,args in re.findall(r'^\s*(\w+)\("([^"]+)", ([\d, ]+)\)[,;]',path.read_text(),re.M):
        v=[int(x.strip()) for x in args.split(',')]
        if len(v)==2:v=[0,0,*v]
        if len(v)>=4:out[name]=(sheet,v[:4])
    return out
ATLAS=parse_atlas(TMP/'source-fabric/com/simibubi/create/foundation/gui/AllGuiTextures.java')
CB_ATLAS=parse_atlas(TMP/'source-icons/rbasamoyai/createbigcannons/index/CBCGuiTextures.java')
def sprite(name):
    sheet,(x,y,w,h)=ATLAS[name];return asset(sheet).crop((x,y,x+w,y+h))
def cbc_sprite(name):
    sheet,(x,y,w,h)=CB_ATLAS[name];return png(CBC,'assets/createbigcannons/textures/gui/'+sheet+'.png').crop((x,y,x+w,y+h))

# The original icon order is a native 16x16 atlas; no screenshot backdrop.
ICON_MAP={};ix=0;iy=-1
for name,op in re.findall(r'public static final AllIcons (\w+) = (newRow|next)\(\);',(TMP/'source-icons/com/simibubi/create/foundation/gui/AllIcons.java').read_text()):
    if op=='newRow':ix=0;iy+=1
    ICON_MAP[name]=(ix*16,iy*16);ix+=1
def icon(name,dark=False):
    x,y=ICON_MAP[name];im=asset('icons').crop((x,y,x+16,y+16))
    if dark:
        px=[]
        for r,g,b,a in im.getdata():px.append((31,34,34,a) if a else (0,0,0,0))
        im.putdata(px)
    return im

def glyph(ch):
    n=ord(ch) if ord(ch)<128 else ord('?');g=ASCII.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8));bb=g.getbbox()
    return g,3 if ch==' ' else (bb[2] if bb else 5)
def tw(t):return sum(glyph(c)[1]+1 for c in str(t))-1
def ink(im,t,x,y,color='#252727',shadow=False):
    if shadow:ink(im,t,x+1,y+1,'#202020',False)
    for ch in str(t):
        g,w=glyph(ch);c=Image.new('RGBA',(8,8),color);c.putalpha(g.getchannel('A'));im.alpha_composite(c,(int(x),int(y)));x+=w+1
def tile(im,rect,tex,phase=(0,0)):
    x,y,w,h=rect;patch=Image.new('RGBA',(w,h))
    for yy in range(-phase[1]%tex.height-tex.height,h,tex.height):
        for xx in range(-phase[0]%tex.width-tex.width,w,tex.width):patch.alpha_composite(tex,(xx,yy))
    im.alpha_composite(patch,(x,y))

def region(id,box):
    r=CASES[id]['native'];p=ROOT/r['capture'];assert hashlib.sha256(p.read_bytes()).hexdigest()==r['sha256']
    b=r['bounds'];return Image.open(p).convert('RGBA').crop(tuple(2*v for v in (b['x']+box[0],b['y']+box[1],b['x']+box[2],b['y']+box[3]))).resize((box[2]-box[0],box[3]-box[1]),Image.Resampling.NEAREST)

# Only clean material swatches are tiled. They contain no controls/frame/world.
TILES={
 'leather':asset('toolbox').crop((8,20,16,28)),
 'wood':asset('stock_keeper_categories').crop((40,38,56,46)),
 'paper':asset('filters').crop((4,16,12,24)),
 'blue':region('own-create-fabric-blueprint',[6,18,14,26]),
 'gray':asset('frogport_and_mailbox').crop((12,60,20,68)),
 'dark':region('pr-create-linked-controller',[3,18,7,22]),
 'olive':cbc_sprite('AUTOCANNON_AMMO_CONTAINER_BG').crop((4,16,12,24)),
 'fuze':cbc_sprite('TIMED_FUZE_BG').crop((4,16,12,24)),
 'proximity':cbc_sprite('PROXIMITY_FUZE_BG').crop((4,16,12,24)),
 'keys':png(SIM,'assets/simulated/textures/gui/linked_typewriter/linked_typewriter.png').crop((12,24,20,32)),
}
def rgb(im):
    c=Counter(p[:3] for p in im.getdata() if p[3]);return c.most_common(1)[0][0]
SKINS={
 'leather':('leather',(226,183,99),'#332610','#f4eddd'),
 'wood':('wood',(102,130,151),'#152430','#fff7e8'),
 'paper':('paper',(164,166,159),'#252b26','#3b3427'),
 'blue':('blue',(105,130,184),'#ffffff','#172a49'),
 'gray':('gray',(102,130,151),'#ffffff','#303331'),
 'dark':('dark',(149,151,145),'#222823','#f2f1e9'),
 'olive':('olive',(201,161,74),'#2c2716','#f2edce'),
 'fuze':('fuze',(201,161,74),'#2c2716','#f2f2ee'),
 'proximity':('proximity',(201,161,74),'#2c2716','#f1f1fa'),
 'keys':('keys',(226,183,99),'#332610','#fff2da'),
}

class Pane:
    def __init__(self,w,h,skin='paper',name=''):
        self.w=w;self.h=h;self.skin=skin;self.name=name;self.im=Image.new('RGBA',(w,h));tile(self.im,(0,0,w,h),TILES[SKINS[skin][0]]);self.boxes=[]
    def box(self,kind,name,x,y,w,h,overlap=False):
        assert 0<=x and 0<=y and x+w<=self.w and y+h<=self.h,(self.name,kind,name,(x,y,w,h),(self.w,self.h))
        self.boxes.append(dict(kind=kind,name=name,rect=[x,y,w,h],overlap=overlap))
    def label(self,t,x,y,color=None,shadow=None):
        color=color or SKINS[self.skin][3];shadow=shadow if shadow is not None else color.startswith('#f')
        self.box('text',str(t),x,y,max(1,tw(t)),8,True);ink(self.im,t,x,y,color,shadow)
    def blit(self,im,x,y,name='native-art',overlap=True):
        self.box('sprite',name,x,y,im.width,im.height,overlap);self.im.alpha_composite(im,(x,y))

def rect(im,box,fill,light='#eeeeee',dark='#383838'):
    x,y,w,h=box;d=ImageDraw.Draw(im);d.rectangle((x,y,x+w-1,y+h-1),fill=fill)
    d.line((x,y+h-1,x,y,x+w-1,y),fill=dark);d.line((x+1,y+h-1,x+w-1,y+h-1,x+w-1,y+1),fill=light)

def button(p,x,y,label=None,ico=None,w=None,selected=False,muted=False,name=None):
    w=w or (max(18,tw(label)+12) if label else 18);p.box('button',name or label or ico,x,y,w,18)
    # Square native Create bevel, horizontally extended with one uniform metric.
    b=sprite('BUTTON_GREEN' if selected else 'BUTTON_DISABLED' if muted else 'BUTTON')
    if w!=18:
        o=Image.new('RGBA',(w,18));o.alpha_composite(b.crop((0,0,3,18)),(0,0));o.alpha_composite(b.crop((3,0,15,18)).resize((w-6,18),Image.Resampling.NEAREST),(3,0));o.alpha_composite(b.crop((15,0,18,18)),(w-3,0));b=o
    p.im.alpha_composite(b,(x,y))
    if label:
        color='#ffffff' if selected else '#555752' if muted else '#202520';ink(p.im,label,x+(w-tw(label))//2,y+5,color,selected)
        LABELS.append(dict(text=label,selected=selected,foreground=color,face='native-green' if selected else 'native-gray'))
    if ico:
        ic=icon(ico,dark=not selected);p.im.alpha_composite(ic,(x+(w-16)//2,y+1))
    return w

def field(p,x,y,w,t='',style='gray',focused=False,tail=False,name='text field'):
    h=27 if style=='paper' else 18
    p.box('field',name,x,y,w,h)
    if style=='paper':
        # Complete native curly hook and paper bevel. Remove only the uniform
        # wood background, then extend the paper's plain middle horizontally.
        a=asset('requester');b=a.crop((42,74,202,101))
        for yy in range(b.height):
            bg=a.getpixel((30,74+yy))
            for xx in range(b.width):
                if b.getpixel((xx,yy))==bg:b.putpixel((xx,yy),(0,0,0,0))
        q=Image.new('RGBA',(w,27));q.alpha_composite(b.crop((0,0,23,27)))
        q.alpha_composite(b.crop((30,0,31,27)).resize((w-27,27),Image.Resampling.NEAREST),(23,0));q.alpha_composite(b.crop((156,0,160,27)),(w-4,0));p.im.alpha_composite(q,(x,y));tx=x+24;right=x+w-5
    elif style=='weight':
        rect(p.im,(x,y,w,18),'#8b8b8b');tx=x+1;right=x+w-1
    elif style=='package':
        d=ImageDraw.Draw(p.im);d.polygon([(x+4,y),(x+w-1,y),(x+w-1,y+17),(x+4,y+17),(x+4,y+12),(x,y+9),(x+4,y+5)],fill='#8b8b8b',outline='#393c39');d.line((x+4,y+17,x+w-1,y+17,x+w-1,y+1),fill='#eeeeee');tx=x+6;right=x+w-16
        p.im.alpha_composite(sprite('CLIPBOARD_ADDRESS'),(x+w-12,y+5))
    else:
        rect(p.im,(x,y,w,18),'#8b8b8b');ImageDraw.Draw(p.im).line((x+1,y+1,x+w-2,y+1),fill='#626662');tx=x+4;right=x+w-4
    layer=Image.new('RGBA',(max(1,right-tx),12));shown=t
    if tail:
        while shown and tw(shown)>layer.width-3:shown=shown[1:]
    ink(layer,shown,0,2,'#30352f' if style=='paper' else '#ffffff',style!='paper')
    if focused:
        cx=min(layer.width-1,tw(shown)+1);ImageDraw.Draw(layer).line((cx,1,cx,9),fill='#30352f' if style=='paper' else '#ffffff')
    p.im.alpha_composite(layer,(tx,y+(8 if style=='paper' else 2)))
    if focused:ImageDraw.Draw(p.im).rectangle((tx-2,y+(6 if style=='paper' else 0),right+1,y+h-(4 if style=='paper' else 1)),outline='#e5c985')

def item(name):
    # Vanilla flat item sprites and small code-native cubes from vanilla textures.
    cbcpath='assets/createbigcannons/textures/item/'+name+'.png'
    if cbcpath in CBC.namelist():return png(CBC,cbcpath).resize((16,16),Image.Resampling.NEAREST)
    path='assets/minecraft/textures/item/'+name+'.png'
    if path in MC.namelist():return png(MC,path).resize((16,16),Image.Resampling.NEAREST)
    texname={'planks':'oak_planks','log':'oak_log','chest':'oak_planks','package':'oak_planks','workbench':'crafting_table_top'}.get(name,name)
    path='assets/minecraft/textures/block/'+texname+'.png'
    if path not in MC.namelist():path='assets/minecraft/textures/block/stone.png'
    tex=png(MC,path).crop((0,0,16,16));out=Image.new('RGBA',(16,16))
    # Inverse map three complete parallelogram faces; no half-cube artifacts.
    for origin,a,b,shade in [((1,5),(7,4),(0,7),.8),((8,9),(7,-4),(0,7),.6),((8,1),(7,4),(-7,4),1.0)]:
        det=a[0]*b[1]-a[1]*b[0]
        for yy in range(16):
            for xx in range(16):
                dx=xx+.5-origin[0];dy=yy+.5-origin[1];u=(dx*b[1]-dy*b[0])/det;v=(dy*a[0]-dx*a[1])/det
                if 0<=u<1 and 0<=v<1:
                    c=tex.getpixel((min(15,int(u*16)),min(15,int(v*16))));out.putpixel((xx,yy),tuple(round(k*shade) for k in c[:3])+(255,))
    return out

def slot(p,x,y,it=None,count=None,size=18,kind='normal',name='slot',allow=False):
    p.box('slot',name,x,y,size,size,allow)
    if kind in ['red','blue']:
        a=png(SIM,'assets/simulated/textures/gui/linked_typewriter/linked_typewriter.png');sx=0 if kind=='red' else 18;p.im.alpha_composite(a.crop((sx,127,sx+18,145)),(x,y))
    elif kind=='basket':rect(p.im,(x,y,size,size),'#af8a67',light='#d9b98f',dark='#6e5139')
    else:rect(p.im,(x,y,size,size),'#8b8b8b')
    if it:
        ic=item(it)
        if kind=='ghost':ic.putalpha(ic.getchannel('A').point(lambda a:round(a*.7)))
        p.im.alpha_composite(ic,(x+(size-16)//2,y+max(1,(size-16)//2)))
    if count is not None:
        t=str(count);ink(p.im,t,x+size-2-tw(t),y+size-9,'#ffffff',True)

def arrow(p,x,y,w=28,direction='right',color=None):
    h=12;p.box('art','process arrow',x,y,w,h,True);c=color or '#70756d';d=ImageDraw.Draw(p.im)
    pts=[(0,4),(w-9,4),(w-9,0),(w-1,6),(w-9,11),(w-9,8),(0,8)]
    if direction=='left':pts=[(w-1-a,b) for a,b in pts]
    d.polygon([(x+a,y+b) for a,b in pts],fill=c)

def scrollbar(p,x,y,h,thumb=18,at=0):
    p.box('scrollbar','scroll track',x,y,6,h);rect(p.im,(x,y,6,h),'#4b4f4b',light='#333a34',dark='#2b312b');rect(p.im,(x+1,y+at,4,min(h,thumb)),'#aaa99a',light='#e7e4d4',dark='#6c7067')

def inset(p,x,y,w,h,fill=None):
    p.box('inset','closed viewport',x,y,w,h,True);rect(p.im,(x,y,w,h),fill or '#62655f',light='#b5b8af',dark='#30392f')

def nine_frame(w,h,bg):
    out=Image.new('RGBA',(w,h));sheet=FRAME.copy()
    def tint(c):
        if c[3]==0 or max(c[:3])<10:return c
        delta=sum(c[:3])/3-198;return tuple(min(255,max(0,round(x+delta))) for x in bg)+(c[3],)
    sheet.putdata([tint(c) for c in sheet.getdata()])
    for src,dst in [((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]:
        out.alpha_composite(sheet.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]),Image.Resampling.NEAREST),dst[:2])
    return out

def wrap(p,title,minw=0,full_title=None,edit_name=False):
    extra=16 if edit_name else 0
    w=max(p.w+12,minw,tw(title)+43+extra);h=p.h+26;tile_name,header,title_ink,_=SKINS[p.skin];bg=rgb(TILES[tile_name]);im=nine_frame(w,h,bg)
    tile(im,(4,19,w-8,h-24),TILES[tile_name]);d=ImageDraw.Draw(im);d.rectangle((4,3,w-5,17),fill=header);d.line((4,18,w-5,18),fill=tuple(round(x*.5) for x in header))
    # Existing SIU rule: full management controls only when title and controls fit.
    cols=[0,6,4,9,2] if tw(title)+18+65+extra<=w else [3,2]
    ink(im,title,7,6,title_ink,title_ink=='#ffffff')
    if edit_name:im.alpha_composite(sprite('FROGPORT_EDIT_NAME'),(tw(title)+11,4))
    for j,col in enumerate(cols):im.alpha_composite(CTRL.crop((col*11,0,col*11+11,11)),(w-5-(len(cols)-j)*13,4))
    dx=(w-p.w)//2;im.alpha_composite(p.im,(dx,20))
    GEOMETRY.append(dict(name=p.name,title=title,full_title=full_title or title,gui_size=[w,h],skin=p.skin,controls='full' if len(cols)==5 else 'compact',widgets=p.boxes))
    return im

def tooltip(lines_,skin='dark'):
    w=max(tw(t) for t in lines_)+10;p=Pane(w,8+len(lines_)*11,skin,'tooltip');inset(p,0,0,p.w,p.h,'#252622')
    for i,t in enumerate(lines_):p.label(t,5,4+i*11,'#f8f4e3',True)
    return p.im

def attached(parent,child,left=10,top=None):
    # Child sits under its parent with a short hinge. Parent remains visible.
    top=top if top is not None else parent.height+5;w=max(parent.width,left+child.width);h=top+child.height
    im=Image.new('RGBA',(w,h));im.alpha_composite(parent,(0,0));ImageDraw.Draw(im).line((left+12,parent.height-2,left+12,top+2),fill='#e8d5a6',width=2);im.alpha_composite(child,(left,top));return im

def make_toolbox(pop=False,mounted=False,hover=False):
    p=Pane(150,108,'leather','mounted toolbox' if mounted else 'toolbox')
    coords=[(39,8),(69,2),(99,8),(105,38),(99,68),(69,74),(39,68),(33,38)]
    names=['iron_ingot','gold_ingot','redstone','stone','planks','copper_ingot','coal','diamond']
    for i,(x,y) in enumerate(coords):slot(p,x,y,names[i] if pop else None,[256,128,64,192,64,64,32,16][i] if pop else None,size=24,name='compartment '+str(i+1))
    button(p,72,41,ico='I_TRASH',name='native disposal')
    im=wrap(p,'Brown Toolbox - Mounted' if mounted else 'Brown Toolbox')
    if hover:
        tip=tooltip(['Iron Ingots: 256','4 stacks in this compartment']);o=Image.new('RGBA',(max(im.width,tip.width+14),im.height+tip.height+4));o.alpha_composite(im,(0,0));o.alpha_composite(tip,(14,im.height+4));return o
    return im

def make_cannon(settings=False,pop=False):
    p=Pane(200,171 if settings else 104,'blue','schematicannon settings' if settings else 'schematicannon')
    if settings:
        p.label('Printer settings',4,2)
        opts=["Don't replace solid blocks",'Replace solid with solid','Replace solid with any','Replace solid with empty']
        for i,t in enumerate(opts):button(p,3,15+i*21,label=t,w=194,selected=i==0)
        button(p,3,103,ico='I_CONFIRM',selected=True);p.label('Skip missing blocks',27,108)
        button(p,3,124,ico='I_SKIP_BLOCK_ENTITIES',selected=True);p.label('Protect block entities',27,129)
        button(p,3,148,label='Back',w=48);button(p,179,148,ico='I_CONFIRM')
    else:
        slot(p,4,9,'gunpowder' if pop else None,name='gunpowder')
        p.label('Fuel',27,2);inset(p,27,13,48,8,'#303630');ImageDraw.Draw(p.im).rectangle((29,15,65 if pop else 29,18),fill='#93b660')
        p.label('Checklist',95,2);slot(p,94,13,'book' if pop else None,name='checklist input');arrow(p,116,16,22);slot(p,143,13,'paper' if pop else None,name='checklist output')
        p.label('Building' if pop else 'Idle',81 if pop else 91,37)
        slot(p,4,45,'paper' if pop else None,size=24,name='schematic input');arrow(p,34,51,116,color='#596da4');slot(p,167,43,None,size=26,name='schematic output')
        inset(p,34,66,116,5,'#30363e')
        if pop:ImageDraw.Draw(p.im).rectangle((35,67,92,69),fill='#dc9451')
        button(p,69,73,ico='I_PLAY',selected=pop);button(p,88,73,ico='I_PAUSE');button(p,107,73,ico='I_STOP')
        button(p,3,83,ico='I_CONFIG_OPEN',name='Printer settings');button(p,179,83,ico='I_CONFIRM')
    return wrap(p,'Schematicannon')

def make_table(filled=False,long=False):
    p=Pane(202,65,'blue','schematic table')
    button(p,3,2,ico='I_OPEN_FOLDER');field(p,24,2,151,'Harbor_Station_and_Warehouse.nbt' if long else 'Harbor.nbt' if filled else 'No schematics saved',focused=long,tail=long,name='complete file picker');button(p,179,2,ico='I_REFRESH')
    slot(p,3,35,'paper' if filled else None,name='blank schematic');button(p,26,35,ico='I_CONFIRM',selected=filled,name='save schematic toggle');arrow(p,50,38,108);slot(p,173,31,'paper' if filled else None,size=26,name='saved schematic')
    return wrap(p,'Schematic Table')

def make_blueprint(pop=False,hover=False):
    p=Pane(160,89,'blue','blueprint')
    for i in range(9):slot(p,3+i%3*18,3+i//3*18,'planks' if pop and i in [0,1,3,4] else None,kind='ghost',name='recipe '+str(i))
    arrow(p,65,22,31,color='#536ba8');slot(p,104,17,'workbench' if pop else None,size=26,name='result',allow=True);slot(p,117,38,None,name='auxiliary native cell',allow=True)
    button(p,3,67,ico='I_TRASH');p.label('Ghost recipe',31,72);button(p,139,67,ico='I_CONFIRM')
    im=wrap(p,'Crafting Blueprint')
    if hover:
        tip=tooltip(['Crafting recipe reference','The smaller native cell stays.']);o=Image.new('RGBA',(max(im.width,tip.width+8),im.height+tip.height+4));o.alpha_composite(im);o.alpha_composite(tip,(8,im.height+4));return o
    return im

def make_frogport(pop=False,rename=False):
    p=Pane(164,47,'gray','frogport')
    for i in range(18):slot(p,1+i%9*18,4+i//9*18,'package' if pop else None,name='package '+str(i))
    im=wrap(p,'Foundry Receiving' if pop else 'Package Frogport',edit_name=True)
    # Native title pencil, isolated clean sprite in the first body-free corner.
    # Name editing is demonstrated separately rather than adding a useless Name row.
    if rename:
        q=Pane(204,47,'gray','frogport rename');q.label('Name',3,1);field(q,3,14,174,'Foundry Receiving Dock',focused=True,tail=True);button(q,183,14,ico='I_CONFIRM');im=attached(im,wrap(q,'Rename Frogport'))
    return im

def make_requester(pop=False,long=False):
    p=Pane(198,85,'wood','redstone requester')
    # Complete rack: caps and all nine custom wood sockets share the body material.
    inset(p,1,1,194,26,'#7b5b44')
    for i in range(9):slot(p,16+i*18,5,['iron_ingot','copper_ingot','redstone','coal','gold_ingot','diamond','planks','stone','paper'][i] if pop else None,[64,64,32,16,32,8,64,64,16][i] if pop else None,kind='basket',name='request '+str(i))
    field(p,2,30,192,'Foundry / North Warehouse / Loading Dock' if long else 'Foundry' if pop else '',style='paper',focused=long,tail=long,name='destination')
    button(p,2,63,ico='I_PARTIAL_REQUESTS',name='allow partial requests');button(p,23,63,ico='I_FULL_REQUESTS',selected=True,name='require full requests');p.label('Request mode',49,68);button(p,175,63,ico='I_CONFIRM')
    return wrap(p,'Redstone Requester')

def make_categories(pop=False,selected=False):
    p=Pane(232 if pop else 200,110 if pop else 36,'wood','stock categories')
    if pop:
        for i,t in enumerate(['Building Blocks','Metals','Factory Components','Railway Supplies']):
            y=3+i*22;p.blit(item(['stone','iron_ingot','redstone','gold_ingot'][i]),4,y+1,'category icon '+str(i));button(p,25,y,label=t,w=155,name='edit category '+str(i));button(p,198,y,ico='I_TRASH',name='delete category '+str(i))
            for delta,sn in [(0,'UP'),(9,'DOWN')]:
                if (delta==0 and i>0) or (delta==9 and i<3):
                    p.box('button','move category '+sn,184,y+delta,8,8);p.blit(sprite('STOCK_KEEPER_CATEGORY_'+sn),184,y+delta,'native order '+sn)
            if selected and i==2:ImageDraw.Draw(p.im).rectangle((2,y-1,218,y+18),outline='#e8c783')
        scrollbar(p,224,3,82,24,12);button(p,3,91,ico='I_ADD');p.label('Add category',27,96);button(p,211,91,ico='I_CONFIRM')
    else:
        button(p,3,7,label='Add category',w=151);button(p,169,7,ico='I_CONFIRM')
    return wrap(p,'Stock Ticker')

def make_category_editor(pop=False,context=False):
    p=Pane(190,63,'wood','category editor');p.label('Category icon and name',3,1);slot(p,3,16,'iron_ingot' if pop else None,name='category icon');field(p,25,16,140,'Factory Components & Railway' if pop else 'New Category',focused=pop,tail=pop,name='category name');button(p,3,41,label='Back',w=44);button(p,169,41,ico='I_CONFIRM')
    im=wrap(p,'Category - Stock Ticker')
    return attached(make_categories(True,True),im) if context else im

def make_stock(pop=False,long=False):
    p=Pane(220,199 if pop else 147,'wood','stock keeper')
    field(p,3,2,190,'Search items...',name='stock search');button(p,198,2,ico='I_CONFIG_LOCKED',name='stock lock')
    viewh=105 if pop else 53;inset(p,3,25,211,viewh,'#744e39')
    things=['iron_ingot','gold_ingot','copper_ingot','redstone','coal','diamond','stone','planks','paper','emerald','book','quartz','amethyst_shard','lapis_lazuli','slime_ball','brick','flint','glowstone_dust']
    if pop:
        p.label('All stock',7,29)
        for i in range(24):slot(p,7+i%8*24,43+i//8*25,things[i%len(things)],[256,128,64,512,32,64,192,64][i%8],size=22,kind='basket',name='stock item '+str(i))
        scrollbar(p,207,27,viewh-4,25,22)
    else:
        slot(p,8,30,'gold_ingot',16,size=22,kind='basket',name='gold in stock')
    ry=25+viewh+7
    inset(p,3,ry,211,23,'#735139')
    for i in range(9):slot(p,25+i*18,ry+3,things[i] if pop else None,[64,32,16,64,16,8,64,64,16][i] if pop else None,kind='basket',name='basket '+str(i))
    fy=ry+29;field(p,3,fy,159,'Foundry / Loading Dock / East Warehouse' if long else 'Foundry' if pop else '',style='paper',focused=long,tail=long,name='package destination');button(p,167,fy+4,label='Send',w=47)
    return wrap(p,'Stock Keeper')

def make_gauge(pop=False):
    p=Pane(130,37,'wood','factory gauge picker');p.label('Monitor item',3,0);arrow(p,9,20,22,color='#654331');slot(p,38,15,'iron_ingot' if pop else None,name='monitor reference');arrow(p,64,20,22,direction='left',color='#654331');button(p,107,15,ico='I_CONFIRM')
    return wrap(p,'Factory Gauge')

def make_filter(pop=False,deny=False,data=False):
    p=Pane(182,91,'paper','list filter')
    for i in range(18):slot(p,10+i%9*18,3+i//9*18,['iron_ingot','gold_ingot','copper_ingot','stone','planks','coal','redstone','diamond','emerald'][i%9] if pop else None,kind='ghost',name='filter '+str(i))
    p.label('Items',3,43);p.label('Data',87,43)
    button(p,3,55,ico='I_BLACKLIST',selected=deny);button(p,24,55,ico='I_WHITELIST',selected=not deny);button(p,86,55,ico='I_RESPECT_NBT',selected=data);button(p,107,55,ico='I_IGNORE_NBT',selected=not data);button(p,139,55,ico='I_TRASH');button(p,160,55,ico='I_CONFIRM')
    p.label(('Deny' if deny else 'Allow')+' list',3,78);p.label('Match data' if data else 'Ignore data',86,78)
    return wrap(p,'List Filter')

def make_attribute(pop=False,drop=False):
    p=Pane(224,151 if pop else 82,'paper','attribute filter')
    slot(p,3,3,'stone',name='reference item');field(p,25,3,151,'is placeable',name='attribute picker');button(p,180,3,ico='I_ADD');button(p,201,3,ico='I_WHITELIST_NOT')
    if pop:
        p.label('Selected attributes: 3',3,28);inset(p,3,40,218,62,'#b7ad97')
        for i,t in enumerate(['is placeable','is from Minecraft','not in tag: logs']):p.label(t,8,45+i*18,'#30362c',False)
        scrollbar(p,213,42,58,22,12)
    y=109 if pop else 30
    for i,t in enumerate(['Any','All','Deny']):button(p,3+i*44,y,label=t,w=41,selected=i==0)
    button(p,180,y,ico='I_TRASH');button(p,201,y,ico='I_CONFIRM');p.label('3 rules' if pop else '1 rule',3,y+24)
    im=wrap(p,'Attribute Filter')
    if drop:
        q=Pane(180,67,'paper','attribute dropdown');inset(q,0,0,180,67,'#b7ad97')
        for i,t in enumerate(['is placeable','is stackable','is from Minecraft']):button(q,3,3+i*21,label=t,w=174,selected=i==0)
        o=Image.new('RGBA',(im.width,max(im.height,58+q.h)));o.alpha_composite(im);o.alpha_composite(q.im,(31,58));im=o
    return im

def make_package(pop=False,long=False,focused=False):
    p=Pane(194,61,'gray','package filter');p.label('Destination',3,0);p.im.alpha_composite(item('package'),(3,18));field(p,24,14,145,'Foundry / East Warehouse / Receiving' if long else 'Foundry' if pop else '',style='package',focused=focused,tail=focused,name='package address');button(p,173,14,ico='I_CONFIRM');button(p,3,39,ico='I_TRASH');p.label('Clear destination',26,44)
    im=wrap(p,'Package Filter')
    if long and not focused:
        tip=tooltip(['Foundry / East Warehouse / Receiving']);o=Image.new('RGBA',(max(im.width,tip.width+8),im.height+tip.height+4));o.alpha_composite(im);o.alpha_composite(tip,(8,im.height+4));im=o
    return im

def make_controller(pop=False,hover=False):
    p=Pane(190,92,'dark','linked controller');labels=['W','S','A','D','Space','Shift'];xs=[3,27,51,75,119,158]
    for i,x in enumerate(xs):
        p.label(labels[i],x if i<4 else x-2,1);slot(p,x,15,'redstone' if pop else None,name='frequency '+str(i)+'a');slot(p,x,33,['iron_ingot','gold_ingot','copper_ingot','coal','diamond','emerald'][i] if pop else None,name='frequency '+str(i)+'b')
    ImageDraw.Draw(p.im).line((106,0,106,53),fill='#b5b8aa');button(p,3,65,ico='I_TRASH');p.label('Frequencies',28,70);button(p,169,65,ico='I_CONFIRM')
    im=wrap(p,'Linked Controller')
    if hover:
        tip=tooltip(['Forward [W]','Frequency 1: Redstone','Frequency 2: Iron Ingot']);o=Image.new('RGBA',(max(im.width,tip.width+8),im.height+tip.height+4));o.alpha_composite(im);o.alpha_composite(tip,(8,im.height+4));return o
    return im

def make_schedule(multi=False,empty=False):
    p=Pane(224,199 if multi else 129 if not empty else 67,'dark','train schedule')
    vh=160 if multi else 90 if not empty else 28;inset(p,2,2,220,vh,'#3c4039');d=ImageDraw.Draw(p.im)
    d.line((15,6,15,vh-6),fill='#bb9156',width=2);d.rectangle((13,5,17,7),fill='#d5b77e');d.rectangle((13,vh-6,17,vh-4),fill='#d5b77e')
    if not empty:
        for i in range(3 if multi else 1):
            y=8+i*47;rect(p.im,(23,y,188,40),'#696d65',light='#a5aa9f',dark='#30372e');p.label(['Harbor Station','Foundry','North Warehouse'][i] if multi else 'Travel to station',31,y+4,'#ffffff',True);button(p,31,y+18,label='Wait: 5s',w=126,name='edit wait condition '+str(i));button(p,164,y+18,ico='I_ADD',name='add wait condition '+str(i));button(p,188,y+8,ico='I_CONFIG_OPEN',name='edit instruction '+str(i))
        d.polygon([(5,17),(11,21),(5,25)],fill='#ffffff')
        if not multi:button(p,23,65,ico='I_ADD')
        else:button(p,182,vh+11,ico='I_ADD')
    else:button(p,23,6,ico='I_ADD')
    y=vh+11;button(p,2,y,ico='I_ACTIVE',selected=True,name='loop');button(p,23,y,ico='I_PRIORITY_HIGH',name='restart');button(p,44,y,ico='I_PRIORITY_LOW',name='skip');p.label('Loop / Restart / Skip' if not multi else 'Schedule actions',69,y+5);button(p,203,y,ico='I_CONFIRM')
    if multi:scrollbar(p,215,5,153,30,22)
    return wrap(p,'Train Schedule')

def make_instruction(pop=False,condition=False,context=False):
    p=Pane(204,107 if condition else 85,'dark','schedule condition' if condition else 'instruction editor')
    button(p,3,2,label='Item Cargo Condition' if condition else 'Travel to Station',w=198)
    if condition:
        p.label('Train holds more than',3,27);field(p,3,39,47,'64',name='cargo amount');button(p,54,39,label='Items',w=54);slot(p,118,39,'iron_ingot',name='cargo reference');p.label('Reference item',3,63)
    else:
        p.label('Station',3,27);slot(p,3,40,'workbench' if pop else None,name='station reference');field(p,25,40,176,'Harbor Station / North Warehouse' if pop else '',focused=pop,tail=pop,name='station pattern')
    y=82 if condition else 62;button(p,3,y,ico='I_TRASH',name='delete instruction');button(p,27,y,label='Back',w=45);button(p,183,y,ico='I_CONFIRM',name='apply instruction')
    im=wrap(p,'Condition - Schedule' if condition else 'Instruction - Schedule')
    return attached(make_schedule(),im) if context else im

def make_ammo(pop=False):
    p=Pane(156,55,'olive','ammo container');p.label('Ammo',3,0);p.label('Capacity',75,0);slot(p,3,15,'autocannon_cartridge' if pop else None,name='ammo input');slot(p,27,15,'tracer_tip' if pop else 'tracer_slot',name='tracer socket');inset(p,74,19,75,9,'#2a3418')
    if pop:ImageDraw.Draw(p.im).rectangle((76,21,120,25),fill='#b5bc6b')
    p.label('32 rounds' if pop else 'Empty',71,34);button(p,132,33,ico='I_CONFIRM')
    return wrap(p,'Autocannon Ammo',full_title='Autocannon Ammo Container')

def make_fuze(id,value=None,extreme=None,hover=False):
    s=CASES[id];prox='proximity' in id;skin='proximity' if prox else 'fuze';p=Pane(162,51,skin,s['title'])
    v=value or ('1 block' if prox else '1s 0 ticks');p.label('Distance:' if prox else 'Fuze time:',2,0);p.label(v,81 if prox else 66,0)
    bg=cbc_sprite('PROXIMITY_FUZE_BG' if prox else 'TIMED_FUZE_BG');ruler=bg.crop((36,29,138,47));p.blit(ruler,5,17,'complete native ruler')
    marker=cbc_sprite('PROXIMITY_FUZE_SELECTOR' if prox else 'TIMED_FUZE_SELECTOR');pos=0 if extreme=='min' else 99 if extreme=='max' else 4
    p.blit(marker,3+pos,9,'complete native marker');button(p,141,16,ico='I_CONFIRM',name='apply fuze value');p.label('Scroll / Shift: faster',2,41)
    fuze_item=id.removeprefix('item-createbigcannons-').replace('-','_');p.blit(item(fuze_item),119,17,'native fuze variant icon')
    im=wrap(p,s['title'])
    if hover:
        tip=tooltip(['Scroll to modify','Shift: faster adjustment']);o=Image.new('RGBA',(max(im.width,tip.width+6),im.height+tip.height+4));o.alpha_composite(im);o.alpha_composite(tip,(6,im.height+4));return o
    return im

def make_shuffle(weighted=False,pop=False,full=False):
    p=Pane(200 if weighted else 176,120 if weighted else 78,'paper','weighted shuffle' if weighted else 'equal shuffle')
    step=21 if weighted else 18;sx=5 if weighted else 7;y0=14 if weighted else 3;stepy=41 if weighted else 18
    if weighted:p.label('Weight (total 100)',5,0)
    things=['stone','planks','iron_ingot','copper_ingot','gold_ingot','coal','redstone','diamond','emerald']
    for i in range(18):
        x=sx+i%9*step;y=y0+i//9*stepy;filled=full or pop and i==0;slot(p,x,y,things[i%9] if filled else None,kind='ghost',name='shuffle '+str(i))
        if weighted and filled:
            val=(10 if i in [0,17] else 5) if full else 100;field(p,x,y+19,20,str(val),style='weight',name='weight '+str(i))
    by=99 if weighted else 56;button(p,3,by,ico='I_TRASH');p.label('Weighted' if weighted else 'Equal choice',29,by+5);button(p,p.w-21,by,ico='I_CONFIRM')
    return wrap(p,'Weighted Shuffle Filter' if weighted else 'Equal Shuffle Filter')

def make_typewriter(hover=False):
    p=Pane(244,101,'keys','typewriter keyboard')
    # Rebuild the native widths from KeyRow using the clean native cap pieces.
    source=(TMP/'source/dev/simulated_team/simulated/content/blocks/redstone/linked_typewriter/screen/LinkedTypewriterScreen.java').read_text()
    a=png(SIM,'assets/simulated/textures/gui/linked_typewriter/linked_typewriter.png')
    for row,key in enumerate(['first','second','third','fourth','fifth']):
        x=9
        for width,glfw in re.findall(r'this\.'+key+r'KeyRow.add\((\d+), (\d+), [^;]+\);',source):
            width=int(width);cap=Image.new('RGBA',(width,14));sx,sm,se=(60,66,68) if hover and glfw=='32' else (73,79,81);cap.alpha_composite(a.crop((sx,127,sx+6,141)));cap.alpha_composite(a.crop((sm,127,sm+2,141)).resize((width-12,14),Image.Resampling.NEAREST),(6,0));cap.alpha_composite(a.crop((se,127,se+6,141)),(width-6,0))
            arrow_key={'265':'^','263':'<','264':'v','262':'>'}.get(glfw)
            if arrow_key:ink(cap,arrow_key,(width-tw(arrow_key))//2,4,'#5c341f')
            p.blit(cap,x,3+row*14,'native keycap '+glfw);x+=width
    button(p,3,81,ico='I_TRASH',name='native confirmed reset');button(p,26,81,label='Bindings',w=58);p.label('Select a key',92,86);button(p,223,81,ico='I_CONFIRM')
    im=wrap(p,'Linked Typewriter')
    if hover:
        tip=tooltip(['Space','Frequency 1: Redstone','Frequency 2: Diamond']);o=Image.new('RGBA',(im.width,im.height+tip.height+4));o.alpha_composite(im);o.alpha_composite(tip,(18,im.height+4));return o
    return im

def make_key_editor(pop=False,context=False):
    p=Pane(194,75,'keys','typewriter key editor');p.label('Key',3,0);p.label('Freq. 1',71,0);p.label('Freq. 2',136,0)
    field(p,3,14,57,'Space' if pop else '`',name='key capture');slot(p,74,14,'redstone' if pop else None,kind='red',name='frequency 1');slot(p,143,14,'diamond' if pop else None,kind='blue',name='frequency 2')
    button(p,3,51,ico='I_TRASH',name='native confirmed delete');button(p,27,51,label='Back',w=44);button(p,173,51,ico='I_CONFIRM')
    im=wrap(p,'Key - Space' if pop else 'Key Editor')
    return attached(make_typewriter(True),im,left=15) if context else im

def make_bindings(multi=False):
    p=Pane(224,172 if multi else 63,'keys','typewriter bindings');n=5 if multi else 1
    for i in range(n):
        y=2+i*27;inset(p,2,y,212,24,'#595c55');p.label(['Space','W','A','S','D'][i] if multi else '`',8,y+8,'#ffffff',True);slot(p,74,y+3,'redstone' if multi else None,kind='red',name='frequency 1 '+str(i));slot(p,97,y+3,['diamond','iron_ingot','copper_ingot','gold_ingot','emerald'][i] if multi else None,kind='blue',name='frequency 2 '+str(i));button(p,166,y+3,ico='I_CONFIG_OPEN',name='edit key '+str(i));button(p,187,y+3,ico='I_TRASH',name='confirmed delete '+str(i))
    y=148 if multi else 38;button(p,3,y,ico='I_TRASH',name='confirmed reset all');button(p,27,y,ico='I_ADD');button(p,51,y,label='Keyboard',w=59);button(p,201,y,ico='I_CONFIRM')
    if multi:scrollbar(p,217,3,133,31,26)
    return wrap(p,'Bindings - Typewriter')

def make_reset_confirmation():
    q=Pane(198,48,'keys','native reset confirmation');q.label('Reset all mappings?',3,1);button(q,3,23,label='Cancel',w=65);button(q,121,23,label='Reset',w=74)
    return attached(make_typewriter(),wrap(q,'Confirm Reset'),left=9)

def render_all_main():
    funcs={
     'pr-create-toolbox':make_toolbox,'pr-railways-mounted-toolbox':lambda:make_toolbox(mounted=True),
     'pr-create-schematicannon':make_cannon,'pr-create-schematic-table':make_table,'own-create-fabric-blueprint':make_blueprint,
     'pr-create-frogport':make_frogport,'pr-create-redstone-requester':make_requester,'pr-create-stock-ticker':make_categories,
     'cplus-stock-category-editor':make_category_editor,'own-create-stock-request-real':make_stock,'own-create-gauge-set-item':make_gauge,
     'pr-create-filter':make_filter,'pr-create-attribute-filter':make_attribute,'pr-create-package-filter':make_package,
     'pr-create-linked-controller':make_controller,'own-create-fabric-schedule':make_schedule,'schedule-editor':make_instruction,
     'createbigcannons-autocannon-ammo-container':make_ammo,'item-createshufflefilter-shuffle-filter':make_shuffle,
     'item-createshufflefilter-weighted-shuffle-filter':lambda:make_shuffle(weighted=True,pop=True),
     'cplus-typewriter':make_typewriter,'typewriter-key-editor':make_key_editor,'typewriter-bindings':make_bindings}
    for id in CASES:
        im=make_fuze(id) if 'fuze' in id else funcs[id]();MAIN[id]=im
        im.resize((im.width*4,im.height*4),Image.Resampling.NEAREST).save(OUT/'windows'/(id+'.png'))

def add_state(id,title,im,case_ids,note):
    path=OUT/'states'/(id+'.png');im.resize((im.width*4,im.height*4),Image.Resampling.NEAREST).save(path)
    STATES.append(dict(id=id,title=title,image=str(path.relative_to(ROOT)),gui_size=list(im.size),cases=case_ids,note=note,illustrative=True))

def render_states():
    add_state('toolbox-filled','Filled toolbox and aggregate count',make_toolbox(True,hover=True),['pr-create-toolbox'],'Illustrative quantities; the summary represents four native stacks, not a single slot limit.')
    add_state('mounted-filled','Mounted toolbox with variant identity',make_toolbox(True,True),['pr-railways-mounted-toolbox'],'Same material and grouping; the full mounted/variant name remains visible.')
    add_state('cannon-options','All six printer-setting controls',make_cannon(True),['pr-create-schematicannon'],'Four exclusive replacement policies plus Skip Missing and Protect Block Entities; labels verified from native code.')
    add_state('cannon-running','Filled machine and playback state',make_cannon(pop=True),['pr-create-schematicannon'],'Illustrative running/status data; all five owned sockets and native playback roles stay distinct.')
    add_state('table-file','Populated filename and outputs',make_table(True),['pr-create-schematic-table'],'Complete widget frames are rebuilt; no file-row/refresh world crop remains.')
    add_state('table-long-file','Long filename, focused and scrolled',make_table(True,True),['pr-create-schematic-table'],'Only field text is clipped internally; the full border and refresh button remain intact.')
    add_state('blueprint-filled','Filled ghost recipe and native auxiliary cell',make_blueprint(True,True),['own-create-fabric-blueprint'],'Illustrative oak-plank recipe; the smaller overlapping native cell remains, with no extra storage role invented.')
    add_state('frogport-filled','All eighteen package cells',make_frogport(True),['pr-create-frogport'],'Illustrative packages; the actual name is in the title, not an empty Name row.')
    add_state('frogport-rename','Attached name editor',make_frogport(True,True),['pr-create-frogport'],'Shows native name-edit purpose; proposed attachment keeps the parent visible.')
    add_state('requester-filled','Nine requests and long destination',make_requester(True,True),['pr-create-redstone-requester'],'Illustrative requested quantities. Long destination text scrolls within the complete paper field.')
    add_state('category-list','Populated categories and scrolling',make_categories(True),['pr-create-stock-ticker'],'Illustrative category names; full row edges and scroll area replace the old open outline.')
    add_state('category-edit','Selected icon and long name in context',make_category_editor(True,True),['pr-create-stock-ticker','cplus-stock-category-editor'],'Parent and editor remain connected; text selection/scrolling stays inside the full field border.')
    add_state('stock-filled','Category, many stock items and full basket',make_stock(True),['own-create-stock-request-real'],'Illustrative stock quantities and nine request entries; complete rack and pinned actions share content rails.')
    add_state('stock-long-address','Full basket and focused long address',make_stock(True,True),['own-create-stock-request-real'],'Illustrative address; the native stock-lock control retains its role and Send stays separate.')
    add_state('gauge-selected','Selected monitor item',make_gauge(True),['own-create-gauge-set-item'],'The gauge identity and complete reference socket remain in a compact picker.')
    add_state('filter-allow','Filled allow list, ignoring data',make_filter(True),['pr-create-filter'],'Illustrative ghost items; data and allow/deny roles verified from native FilterScreen.')
    add_state('filter-deny','Filled deny list, matching data',make_filter(True,True,True),['pr-create-filter'],'Alternate active states use the same button frames; green selection remains explicit.')
    add_state('attribute-rules','Several native attribute rules',make_attribute(True),['pr-create-attribute-filter'],'Illustrative selected rules and count. Any/All/Deny retain their native meanings.')
    add_state('attribute-picker','Candidate attribute dropdown',make_attribute(True,True),['pr-create-attribute-filter'],'The dropdown opens as a foreground overlay with its own complete border; it is not cut to the form row.')
    add_state('package-long','Long destination with complete hover text',make_package(True,True),['pr-create-package-filter'],'Illustrative full value is provided separately from the bounded text field.')
    add_state('package-focused','Focused destination, scrolled text',make_package(True,True,True),['pr-create-package-filter'],'Illustrative editing state: only text may clip, never the paper field edge.')
    add_state('controller-pairs','All six key/frequency pairs and tooltip',make_controller(True,True),['pr-create-linked-controller'],'Illustrative default keys and items; actual configured key names remain dynamic during implementation.')
    add_state('schedule-empty','Compact empty schedule',make_schedule(empty=True),['own-create-fabric-schedule'],'The Add action and complete capped viewport remain even when no entries exist.')
    add_state('schedule-scroll','Several entries and a complete scroll track',make_schedule(True),['own-create-fabric-schedule'],'Illustrative stops; transport arrows are Restart and Skip, not list-reorder commands.')
    add_state('schedule-attached','Instruction editor attached to schedule',make_instruction(True,context=True),['own-create-fabric-schedule','schedule-editor'],'Illustrative long station pattern; parent context, Back and native apply/delete actions are visible.')
    add_state('schedule-cargo','Item Cargo Condition editor',make_instruction(True,True),['schedule-editor'],'Alternate form uses native condition labels and a complete reference socket; the layout grows for its controls.')
    add_state('ammo-filled','Ammunition roles and capacity state',make_ammo(True),['createbigcannons-autocannon-ammo-container'],'Illustrative ammunition quantity; no generic chest sort action is invented for these roles.')
    for id in CASES:
        if 'fuze' not in id:continue
        prox='proximity' in id
        add_state(id+'-min',CASES[id]['title']+' - lower endpoint',make_fuze(id,'1 block' if prox else '0s 1 tick',extreme='min'),[id],'Endpoint formatting follows native scroll-input semantics; marker and labels remain within their full bounds.')
        add_state(id+'-max',CASES[id]['title']+' - upper endpoint / hover',make_fuze(id,'32 blocks' if prox else '24s 15 ticks',extreme='max',hover=True),[id],'Illustrative endpoint derived from native exclusive ranges (1..32 distance; time steps up to 495 ticks).')
    add_state('equal-filled','All eighteen equal-choice entries',make_shuffle(pop=True,full=True),['item-createshufflefilter-shuffle-filter'],'Illustrative ghost blocks/items; no fake weight or item-count label is added to equal mode.')
    add_state('weighted-both-rows','All eighteen weights, including both last cells',make_shuffle(True,True,True),['item-createshufflefilter-weighted-shuffle-filter'],'Illustrative normalized weights: first/last 10 and sixteen values of 5 sum to 100. Every second-row field is complete.')
    add_state('typewriter-hover','Native keycaps and selected-key explanation',make_typewriter(True),['cplus-typewriter'],'Blank keycaps remain native; the Space mapping appears in a separate hover view.')
    add_state('typewriter-attached','Space key editor attached to its keyboard',make_key_editor(True,True),['cplus-typewriter','typewriter-key-editor'],'Illustrative reference items retain red/blue frequency surfaces; Back/confirm/delete preserve context.')
    add_state('bindings-scroll','Several bindings with scrolling and return',make_bindings(True),['typewriter-bindings'],'Illustrative key mappings. Every row has equal padding, Edit/Delete and an explicit Keyboard return action.')
    add_state('typewriter-reset-confirm','Destructive action remains a separate confirmation',make_reset_confirmation(),['cplus-typewriter','typewriter-bindings'],'Proposed presentation of the existing native confirmation requirement; Cancel and Reset are distinct from accepting a key edit.')

RESPONSES={
'pr-create-toolbox':[
 'The leather continues to one closed SIU edge; the old native right/bottom border is removed.',
 'Leather carries the body and surround. A narrow native gold header supplies the brass accent.',
 'Brown Toolbox remains the title; custom names would replace it using the same sizing rule.',
 'Filled compartments, aggregate quantities and hover feedback are illustrated in the state study. Native transfer testing remains pending.'],
'pr-railways-mounted-toolbox':[
 'The mounted design uses the same closed construction as the placed toolbox.',
 'Brown Toolbox remains visible, with Mounted added as context.',
 'Both toolbox designs share their leather, header, margins and controls.',
 'A filled mounted layout is drawn. A matching populated native capture is still needed during implementation.'],
'pr-create-schematicannon':[
 'Five complete owned sockets and machine controls are rebuilt on the blue material without old side strips.',
 'Playback, settings and accept all use the same 18-pixel Create button family.',
 'Playback sits beside the utility controls rather than in a separate blank footer band.',
 'The settings proposal draws all four replacement policies and both independent toggles. A native settings capture remains a follow-up.'],
'pr-create-schematic-table':[
 'The file picker is a complete field with its bottom and right bevel intact, including in the long-text example.',
 'Refresh uses its clean native icon in a complete button. No surrounding screenshot pixels are copied.',
 'Input, save toggle, process arrow and larger result are separate components on continuous blue.',
 'The two rows share deliberate rails; the file operation remains below the title-management controls.'],
'own-create-fabric-blueprint':[
 'Native blue extends through the canvas and frame, with no pasted rectangular panel boundary.',
 'The Ghost recipe cue identifies reference editing; all eleven owned cells, including the native overlap, stay.',
 'Labels use dark ink on blue and buttons use dark ink on gray, with green only for selected state.',
 'A populated ghost recipe and explanatory hover are illustrated. Exact native result/reference behavior still needs runtime validation.'],
'pr-create-frogport':[
 'A clean native material sample surrounds complete two-by-nine cells. No neighboring grid fringe is retained.',
 'The current name is the title and its native pencil remains available; an attached rename example is included.',
 'The shared SIU fit rule chooses compact or full management controls. No sorting action is invented for configuration data.',
 'The name-only and Done-only rows are removed; the compact window ends directly after the complete grid.'],
'pr-create-redstone-requester':[
 'A continuous native wood body holds a closed rack with symmetric end caps.',
 'Both native Partial/Full Request mode icons share the same button skin as accept.',
 'Nine populated requests and a long destination are illustrated with the complete curly paper hook. Native interaction testing is pending.'],
'pr-create-stock-ticker':[
 'The category board uses a continuous wood surface and closed row fields, without a partial old outline.',
 'The empty state shrinks to Add category and accept; it no longer reserves a tall blank board.',
 'Populated rows, scrolling, a selected row and a long-name editor are illustrated. Native overflow interaction remains to test.'],
'cplus-stock-category-editor':[
 'The former double separator and sampled panel boundary are gone.',
 'Icon, complete name field and actions are individual widgets on the shared wood surface.',
 'The attached study highlights the selected parent category and separates Back from accept.',
 'A chosen icon and horizontally scrolled long name show field/caret clearance. Native focus and selection behavior remain to test.'],
'own-create-stock-request-real':[
 'Paper destination, native lock and Send are separate components without the old footer or world fragments.',
 'The nine-entry basket is enclosed by complete, symmetric rack ends and has clearance from the address.',
 'Search, list viewport, basket and destination follow shared left/right content rails; scrolling stays inside the results.',
 'Many items, counts, a full basket and a focused long address are illustrated. Actual stock/network behavior is not certified by these designs.'],
'own-create-gauge-set-item':[
 'Native wood continues behind the reference socket and both pointers.',
 'A compact accept button completes the one-item picker without a full-width Done band.',
 'Factory Gauge remains in the title and Monitor item labels the task; the picker is shown at the same scale as other proposals.'],
'pr-create-filter':[
 'The eighteen ghost cells sit on one continuous native material with even margins.',
 'Both native mode pairs are rendered individually; their sampled gray footer is removed.',
 'Dark icon/label ink on gray and white on selected green give clear mode and action contrast.',
 'Filled Allow/Ignore Data and Deny/Match Data states are illustrated. Native item tooltip and click behavior remain implementation checks.'],
'pr-create-attribute-filter':[
 'Mode, clear and accept controls are individual widgets on the shared body; rule count remains explicit.',
 'The reference field, picker and Add/Add Opposite remain complete on one material surface.',
 'A shared button bevel and readable ink replace the competing frames and pale labels.',
 'Three rules, an inverted rule and an open picker are illustrated. The visible rule list is a proposed presentation, not an existing native feature claim.'],
'pr-create-package-filter':[
 'The native gray material fills the body. The complete address-tip field and paper-end icon remain.',
 'Clear and accept have separate compact actions, with dark labels on the common gray face.',
 'Long-address hover and focused horizontal scrolling are illustrated; text alone clips inside the field, ahead of the end icon.'],
'pr-create-linked-controller':[
 'The dark material continues through the frame; a restrained header joins it without a bright outer shell.',
 'Common button faces replace the old footer strip. All twelve frequency backgrounds and the 4+2 divider remain.',
 'Six populated pairs, readable key labels and hover context are illustrated. Actual configured key labels must come from the client at runtime.'],
'own-create-fabric-schedule':[
 'The current-entry chevron is drawn inside a clear viewport margin and remains whole.',
 'The shortened brass timeline has deliberate end caps; the list has a complete boundary.',
 'Loop, Restart and Skip use the shared individual buttons. The arrows are not relabeled as list reordering.',
 'Empty and scrolling schedules, an attached editor and a type-specific condition are illustrated. Selection/scroll restoration needs runtime validation.'],
'schedule-editor':[
 'The station-reference socket includes its full one-pixel frame, separate from item content.',
 'Fields and type control sit directly on a continuous dark body with complete widget bevels.',
 'The attached proposal keeps the schedule visible; Back, apply and subordinate delete have different roles.',
 'The Item Cargo Condition example includes amount, unit mode and reference item. Other native condition forms remain variable-size implementation cases.'],
'createbigcannons-autocannon-ammo-container':[
 'Both ammunition-role sockets have complete borders and equal clearance from surrounding content.',
 'The native olive material continues through one window with a restrained brass header.',
 'The title retains Autocannon Ammo; the tracer cue and capacity/status area stay distinct. Filled content is illustrative, not a claim of a fixed capacity.'],
'item-createshufflefilter-shuffle-filter':[
 'Native-style cells sit on a continuous filter material inside one complete SIU frame.',
 'Small clear and accept controls replace the tall text-button footer, using readable shared icons.',
 'All eighteen equal-choice entries are illustrated without weights or stack counts. Native selection behavior remains to validate.'],
'item-createshufflefilter-weighted-shuffle-filter':[
 'Weight (total 100) labels the values, which stay below their ghost entries rather than in count corners.',
 'Even column/row spacing and complete weight fields share the common material and action skin.',
 'Both rows and their final cells are populated. The 100 extreme is shown in the main view; 10/5 normalized weights sum to 100 in the full example.'],
'cplus-typewriter':[
 'All five rows are rebuilt from clean native keycap pieces at their native widths; no former panel edges are copied.',
 'Reset is a small utility action. Its existing native confirmation requirement is shown separately from accept.',
 'A selected Space key, mapping explanation, attached key editor and scrolling bindings list are illustrated. Live feedback remains to validate.'],
'typewriter-key-editor':[
 'The complete key field and native-color frequency pair sit on one continuous brown material.',
 'Freq. 1/Freq. 2 labels and real reference-item examples explain the two frequency surfaces.',
 'The selected key appears in the title; parent keyboard, Back, apply and subordinate delete remain distinct in the attached view.'],
'typewriter-bindings':[
 'Every binding uses the same complete row surface with equal top/bottom clearance.',
 'Edit, Delete, Add, Keyboard return and accept use one control vocabulary; reset keeps a separate confirmation.',
 'Five rows and an internal scrollbar are illustrated, with explicit Keyboard return and an attached mapping editor elsewhere. Runtime return/selection preservation remains to test.'],
}
FUZE_RESPONSES=[
 'The native dark or blue material continues across the body and surround, with a small brass header.',
 'Ruler, value, native item cue and accept fit in a compact layout; the native ruler/marker height is preserved.',
 'The full variant name, native item sprite and scroll/Shift cue distinguish each setting.',
 'Lower/upper endpoints and an adjustment explanation are drawn from the native ranges. Actual scrolling and saved values remain runtime checks.']
for id in CASES:
    if 'fuze' in id:RESPONSES[id]=FUZE_RESPONSES

def verify():
    assert len(MAIN)==27 and len({c['native']['screen'] for c in OLD})==23
    assert len(STATES)==41
    assert set(RESPONSES)==set(CASES)
    bad=[]
    for g in GEOMETRY:
        boxes=[b for b in g['widgets'] if b['kind'] in ['button','field','slot','scrollbar'] and not b['overlap']]
        for i,a in enumerate(boxes):
            x,y,w,h=a['rect']
            for b in boxes[i+1:]:
                xx,yy,ww,hh=b['rect']
                if min(x+w,xx+ww)>max(x,xx) and min(y+h,yy+hh)>max(y,yy):bad.append([g['name'],a['name'],b['name']])
        labels=[b for b in g['widgets'] if b['kind']=='text']
        for i,a in enumerate(labels):
            x,y,w,h=a['rect']
            for b in labels[i+1:]:
                xx,yy,ww,hh=b['rect']
                if min(x+w,xx+ww)>max(x,xx) and min(y+h,yy+hh)>max(y,yy):bad.append([g['name'],a['name'],b['name']])
    assert not bad,bad
    for c in OLD:
        assert hashlib.sha256((ROOT/c['native']['capture']).read_bytes()).hexdigest()==c['native']['sha256']
        assert hashlib.sha256((ROOT/c['mockup']).read_bytes()).hexdigest()==c['mockup_sha256']
    assert sum([10]+[5]*16+[10])==100
    assert tw('100')<=18
    for r in AUDIT['reviews']:assert len(RESPONSES[r['id']])==len(r['findings']),r['id']
    assert sum(len(r['findings']) for r in AUDIT['reviews'])==97
    return dict(primary_views=27,distinct_container_classes=23,illustrative_states=41,
      audit_findings=97,source_counts=AUDIT['counts'],widget_bounds='passed by per-widget assertions',
      unintended_widget_intersections=bad,text_label_intersections=bad,
      native_and_prior_mockup_hashes='all 27 pairs unchanged',weighted_total=100,
      full_weight_100_fits=True,validation_scope='Focused design and document checks only; no game/client test or implementation performed.')

# Document coordinates use a top-left origin for predictable spacing.
W,H=1080,760
TEAL='#256c68';INK='#203330';MUTED='#64736f';LIGHT='#eef3f1';GOLD='#b48b4e'
PAGES=[];PLACEMENTS=[];TEXT_LINES=[];C=None
STATE_PAGES={}
for i,s in enumerate(STATES):STATE_PAGES[s['id']]=30+i//2

def textline(t,x,y,size=11,color=INK,bold=False):
    assert all(ord(c)<128 for c in t),t
    font='Bold' if bold else 'Body';width=pdfmetrics.stringWidth(t,font,size)
    assert 0<=x and x+width<=W-20 and y>=15 and y+size<=H-14,(t,x,y,width)
    C.setFont(font,size);C.setFillColor(HexColor(color));C.drawString(x,H-y-size,t)
    TEXT_LINES.append(dict(page=len(PAGES)+1,text=t,rect=[x,y,width,size]))

def paragraph(t,x,y,width,size=11,leading=15,color=INK,bold=False):
    line='';yy=y;fn='Bold' if bold else 'Body'
    for word in t.split():
        cand=(line+' '+word).strip()
        if line and pdfmetrics.stringWidth(cand,fn,size)>width:
            textline(line,x,yy,size,color,bold);yy+=leading;line=word
        else:line=cand
    if line:textline(line,x,yy,size,color,bold);yy+=leading
    return yy

def card(x,y,w,h,color=LIGHT):
    C.setFillColor(HexColor(color));C.roundRect(x,H-y-h,w,h,8,fill=1,stroke=0)

def picture(im,box,scale=None,name=''):
    if isinstance(im,(str,Path)):im=Image.open(im).convert('RGBA')
    x,y,w,h=box;s=min(w/im.width,h/im.height)
    if scale:s=min(s,scale)
    iw,ih=im.width*s,im.height*s;xx=x+(w-iw)/2;yy=y+(h-ih)/2
    C.drawImage(ImageReader(im),xx,H-yy-ih,iw,ih,mask='auto')
    PLACEMENTS.append(dict(page=len(PAGES)+1,name=name,rect=[xx,yy,iw,ih],source_size=list(im.size),scale=s))
    return s

def start(title,subtitle,section='OVERHAULED WINDOW'):
    C.setFillColor(HexColor('#ffffff'));C.rect(0,0,W,H,fill=1,stroke=0)
    textline('SIU / CREATE FAMILY',40,27,10,TEAL,True)
    textline(section,765,27,10,MUTED,True)
    textline(title,40,52,27,INK,True)
    paragraph(subtitle,40,91,1000,11,15,MUTED)

def end(title):
    n=len(PAGES)+1;PAGES.append(dict(page=n,title=title));C.bookmarkPage('page'+str(n))
    C.setStrokeColor(HexColor('#d4ded8'));C.line(40,40,1040,40)
    textline('Archived native evidence / SIU images are design proposals, not implemented game captures',40,731,8,MUTED)
    textline(str(n),1015,731,9,MUTED);C.showPage()

def pages_for(id):return sorted({STATE_PAGES[s['id']] for s in STATES if id in s['cases']})

def make_pdf(checks):
    global C,STATE_PAGES
    for path,name in [('C:/Windows/Fonts/arial.ttf','Body'),('C:/Windows/Fonts/arialbd.ttf','Bold')]:pdfmetrics.registerFont(TTFont(name,path))
    C=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
    C.setTitle('Create Family - Complete SIU Window Mockup Overhaul');C.setAuthor("Salt's Inventory Update")
    STATE_PAGES={s['id']:30+i//2 for i,s in enumerate(STATES)}
    start('Create windows, rebuilt','A complete replacement design set based on the visual audit.','COMPLETE MOCKUP OVERHAUL')
    paragraph('Native materials. Complete widgets. Clearer actions.',40,136,1000,22,29,INK,True)
    paragraph('27 original-versus-proposal views cover all 23 retained container screen classes. Another 41 proposed state views explore the content, editors and adjustment extremes the review could not assess.',40,178,970,14,21)
    for i,(num,label) in enumerate([('27','rebuilt comparison views'),('97','findings given a design response'),('41','additional state mockups')]):
        x=40+i*341;card(x,247,318,73);textline(num,x+16,258,27,TEAL,True);textline(label,x+70,279,10,INK)
    for i,id in enumerate(['pr-create-redstone-requester','cplus-typewriter','item-createbigcannons-proximity-fuze']):
        x=40+i*341;card(x,342,318,226);picture(MAIN[id],(x+12,357,294,180),1.12,id);textline(CASES[id]['title'],x+15,543,11,TEAL,True)
    textline('What changed',40,598,13,TEAL,True)
    paragraph('Old panel/frame remnants are removed. Native materials extend to SIU edges. Buttons share one metric, fields keep their full bevels, and attached editors retain their parent context.',40,622,1000,13,19)
    paragraph('This PDF specifies designs. Additional filled/hover/editor examples use illustrative data. Native networking, focus, clicks, dragging and responsive behavior still require an implementation pass.',40,679,1000,10.5,15,MUTED)
    end('Create windows, rebuilt')

    start('How to read this set','Original captures stay on the left. Every right-hand window is a new proposal.','REVIEW MAP + DESIGN RULES')
    rules=[
      ('One frame, native material','Leather, wood, blueprints, parchment and fuze surfaces continue through one closed SIU window.'),
      ('One control vocabulary','Native icons use complete 18-pixel Create buttons. Accept, Back, clear and destructive confirmation have distinct roles.'),
      ('One title-control sizing rule','Full SIU management controls appear only when title space allows; otherwise ellipsis and close carry the compact mode.'),
      ('Scale and evidence','Primary proposals share 1.5 points per GUI pixel. Originals are fitted for context. State-page scales are printed under each image.'),
      ('Protect the native details','Keep toolbox grouping, blueprint overlap, paper hooks, frequency pairs, timeline markers, complete rulers and native key widths.')]
    y=137
    for head,body in rules:
        textline(head,40,y,12,TEAL,True);y=paragraph(body,40,y+20,430,11,15)+19
    textline('CONTENTS',530,138,11,TEAL,True)
    for i,c in enumerate(OLD):
        y=164+i*18;textline(f'{i+3:02d}',530,y,10,TEAL,True);textline(c['title'],560,y,10,INK)
        C.linkRect('', 'page'+str(i+3),(530,H-y-14,1035,H-y+2),relative=0,thickness=0)
    textline('30-50   Filled, editor, hover and range studies',530,670,11,TEAL,True)
    textline('51         Family scope, evidence and validation limits',530,692,11,TEAL,True)
    end('How to read this set')

    ledger=[]
    for i,(c,r) in enumerate(zip(OLD,AUDIT['reviews'])):
        assert c['id']==r['id'];id=c['id'];page=i+3
        loader='NeoForge' if c['loader']=='neoforge' else 'Fabric'
        start(c['title'],f"{c['family']} / native reference: Minecraft {c['minecraft']} - {loader}")
        textline('ORIGINAL / SIU ABSENT',40,124,10,TEAL,True);textline('OVERHAULED SIU PROPOSAL',550,124,10,TEAL,True)
        card(40,146,490,352);card(550,146,490,352,'#e8efec')
        picture(ROOT/c['native_report_crop'],(50,156,470,332),name='native '+id)
        picture(MAIN[id],(560,156,470,332),1.5,'new '+id)
        textline('Native reference retained unaltered; surrounding inventory provides context.',40,508,8.5,MUTED)
        textline(f"Proposed window: {MAIN[id].width} x {MAIN[id].height} GUI px / common scale",550,508,8.5,MUTED)
        textline('RESPONSE TO EVERY FINDING',40,536,10,TEAL,True)
        for j,(f,response) in enumerate(zip(r['findings'],RESPONSES[id])):
            x=40+j%2*510;y=560+j//2*62;key=f'{i+1:02d}.{f["kind"]}{j+1}'
            textline(key+'  '+f['title'],x,y,10,INK,True)
            yend=paragraph(response,x,y+17,487,9.5,13,MUTED)
            assert yend<=y+61,(id,key,yend,y)
            ledger.append(dict(finding_id=key,case=id,kind=f['kind'],title=f['title'],response=response,comparison_page=page,state_pages=pages_for(id),
              design_status='Illustrated proposal; native/client verification pending' if f['kind']=='S' else 'Revised visual proposal; implementation pending'))
        plist=', '.join(str(x) for x in pages_for(id))
        textline(('Additional proposed states: pages '+plist) if plist else 'Native content preserved; no extra independent state required by this audit.',40,696,9,TEAL,True)
        textline('Reference '+id+' / native SHA-256 '+c['native']['sha256'][:16]+'...',40,713,8,MUTED)
        end(c['title'])

    for ix in range(0,len(STATES),2):
        batch=STATES[ix:ix+2]
        start('Content and interaction studies',f'Proposed states {ix+1}-{min(ix+2,len(STATES))} of {len(STATES)}. Illustrative data and presentation; these are not new in-game captures.','ADDITIONAL PROPOSED STATES')
        for k,s in enumerate(batch):
            x=40+k*510;card(x,139,490,424)
            textline(s['title'],x+13,153,12,TEAL,True)
            im=Image.open(ROOT/s['image']).convert('RGBA');scale=picture(im,(x+13,183,464,366),.375,s['id'])*4
            textline(f"{s['gui_size'][0]} x {s['gui_size'][1]} GUI px / {scale:.2f} pt per GUI px",x+13,580,9,MUTED)
            endy=paragraph(s['note'],x+13,608,464,11,16)
            assert endy<695,(s['id'],endy)
            refs=', '.join(str(list(CASES).index(id)+3) for id in s['cases'])
            textline('Related native comparisons: pages '+refs,x+13,698,9,TEAL)
        if len(batch)==1:
            textline('Confirmation is part of the native flow',565,200,17,INK,True)
            paragraph('Reset remains a separate utility action. This study changes its presentation while retaining the existing need to confirm. It must not silently become a one-click destructive action during implementation.',565,240,430,13,20)
        end('State studies '+str(ix+1)+'-'+str(min(ix+2,len(STATES))))

    start('Scope, evidence and next validation','All retained inventory/container windows are represented; non-inventory Create controls remain outside window conversion.','APPENDIX')
    groups=[
      ('Family coverage','23 unique screen classes: Create 14, Big Cannons 5, Simulated 1, Shuffle Filter 2 and Steam n Rails 1. Four nested editors produce the 27 comparison views.'),
      ('Shared layouts and addons','Color variants and mounted toolboxes share the compartment design. Audited addons with no separate container class inherit applicable Create/vanilla layouts; no unseen custom menu is claimed as tested.'),
      ('Other interfaces accounted for','Station/assembly and elevator setup; display/signal/value controls; schematic tools, symmetry/worldshaper, radial menus, clipboard, goggles and client options stay native. This also covers bogey/tag selection, cannon mounts, Simulated sensor/nameplate/diagram controls, Pattern Schematics tools and Testosterone settings.'),
      ('Release references','Create Fabric 6.0.8.1+build.1744 for Minecraft 1.20.1; Create 6.0.10 for 1.21.1 NeoForge. Big Cannons 5.11.6, Shuffle Filter 2.1.0 and Simulated 1.3.0 supply the retained 1.21.1 references. Steam n Rails 1.7.3 supplies the mounted toolbox.'),
      ('Evidence integrity','All 27 native source SHA-256 hashes and all prior mockup hashes are unchanged. The source audit and initial design PDFs are preserved. New crops, state assets, geometry and all 97 finding responses have a separate ledger.'),
      ('Focused verification','Checked 27 primary views, 41 proposed states, complete widget bounds, unintended widget/text overlaps, weight formatting/total, source integrity and the rendered PDF. The deliberate blueprint result overlap and intentional dropdown overlay remain.'),
      ('Limits and implementation follow-up','No compatibility or API code is changed. No client or gameplay suite was run for this design task. The 22 state findings now have visual studies, but require native/client checks for transfers, focus, caret/selection, hover, scroll persistence, responsive sizing and networking. Native populated mounted-toolbox and Schematicannon-settings captures still need matching validation.')]
    y=140
    for head,body in groups:
        textline(head,40,y,12,TEAL,True);y=paragraph(body,40,y+19,1000,11,15)+15
    assert y<712,y
    end('Scope, evidence and next validation')
    C.save()
    # Disable image interpolation so native bitmap edges stay sharp in viewers.
    reader=PdfReader(str(PDF));writer=PdfWriter();writer.clone_document_from_reader(reader)
    for page in writer.pages:
        resources=page.get('/Resources',{});objects=resources.get('/XObject',{})
        for ref in objects.values():
            obj=ref.get_object()
            if obj.get('/Subtype')=='/Image':obj[NameObject('/Interpolate')]=BooleanObject(False)
    tmp=PDF.with_suffix('.tmp.pdf')
    with tmp.open('wb') as f:writer.write(f)
    tmp.replace(PDF)
    checks.update(pdf_pages=len(PAGES),page_count_verified=len(PdfReader(str(PDF)).pages),native_client_tests_run=False)
    manifest=dict(pdf=str(PDF.relative_to(ROOT)),primary_views=27,distinct_container_classes=23,illustrative_states=STATES,
      audit_responses=ledger,pages=PAGES,checks=checks,
      cases=[dict(id=c['id'],title=c['title'],native=c['native'],native_report_crop=c['native_report_crop'],
                  mockup=str((OUT/'windows'/(c['id']+'.png')).relative_to(ROOT)),gui_size=list(MAIN[c['id']].size),comparison_page=i+3,state_pages=pages_for(c['id'])) for i,c in enumerate(OLD)])
    for name,data in [('overhaul-manifest.json',manifest),('review-checks.json',checks),('placements.json',PLACEMENTS),('text-layout.json',TEXT_LINES)]:
        (OUT/name).write_text(json.dumps(data,indent=2)+'\n')
    print(json.dumps(dict(pdf=str(PDF),**checks),indent=2))

if __name__=='__main__':
    render_all_main();render_states()
    (OUT/'geometry.json').write_text(json.dumps(GEOMETRY,indent=2)+'\n')
    (OUT/'states.json').write_text(json.dumps(STATES,indent=2)+'\n')
    make_pdf(verify())
