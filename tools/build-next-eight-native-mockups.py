"""Build a visual proposal from native-only captures and existing SIU sprites.

This is report tooling, not an integration implementation. Original screenshots
are kept intact; separately named mockups compose their UI regions in SIU chrome.
"""
from pathlib import Path
from io import BytesIO
import json
import hashlib
import shutil
import zipfile
from PIL import Image, ImageDraw
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'build/project-everything-eight-mockups'
OUT = ROOT / 'output/project-everything-eight-mockups'
PDF = ROOT / 'output/pdf/siu-next-eight-native-ui-and-proposed-windows.pdf'
OUT.mkdir(parents=True, exist_ok=True)
PDF.parent.mkdir(parents=True, exist_ok=True)
GUI = ROOT / 'artwork/shared-resources/core/assets/salts_inventory_update/textures/gui'
FRAME = Image.open(GUI / 'window.png').convert('RGBA')
CONTROLS = Image.open(GUI / 'window_controls.png').convert('RGBA')
FABRIC_FIXTURE=json.loads((BASE/'fixture-fabric.json').read_text(encoding='utf-8'))
def native_asset(resource):
    for artifact in FABRIC_FIXTURE['artifacts']:
        with zipfile.ZipFile(artifact['path']) as z:
            if resource in z.namelist():
                return Image.open(BytesIO(z.read(resource))).convert('RGBA')
    raise FileNotFoundError(resource)
TAB_SHEET=native_asset('assets/libz/textures/gui/icons.png')
with zipfile.ZipFile(Path.home() / '.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:
    ASCII = Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
FONT_CHARS = list(
    '\0'*32 + ' !"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\0'
)

def glyph(ch):
    n = ord(ch) if ord(ch) < 128 else ord('?')
    g = ASCII.crop(((n % 16)*8, (n // 16)*8, (n % 16)*8+8, (n // 16)*8+8))
    box = g.getbbox()
    width = 3 if ch == ' ' else (box[2] if box else 5)
    return g, width

def text_width(s):
    return sum(glyph(ch)[1]+1 for ch in s)

def mc_text(im, s, x, y, color=(64, 64, 64, 255)):
    for ch in s:
        g, w = glyph(ch)
        ink = Image.new('RGBA', (8, 8), color)
        ink.putalpha(g.getchannel('A'))
        im.alpha_composite(ink, (int(x), int(y)))
        x += w+1

def nine_slice(w, h):
    im = Image.new('RGBA', (w, h))
    pieces = [((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),
              ((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),
              ((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),
              ((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),
              ((5,5,6,6),(5,5,w-5,h-5))]
    for src, dst in pieces:
        im.alpha_composite(FRAME.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]), Image.Resampling.NEAREST), dst[:2])
    return im

captures = {}
for folder in sorted((BASE/'gameplay').iterdir()):
    obs = folder / 'observations.json'
    if not obs.exists():
        continue
    for row in json.loads(obs.read_text(encoding='utf-8')):
        name = row['command'].get('capture')
        path = folder/'screenshots'/f'{name}.png'
        if name and path.exists():
            captures[name] = (path, row['client'])

def region(name, rect, absolute=False):
    path, state = captures[name]
    im = Image.open(path).convert('RGBA')
    sx, sy = im.width/state['guiWidth'], im.height/state['guiHeight']
    b = state.get('bounds', {'x':0,'y':0})
    ox, oy = (0,0) if absolute else (b['x'], b['y'])
    box = tuple(round(v) for v in ((ox+rect[0])*sx,(oy+rect[1])*sy,(ox+rect[2])*sx,(oy+rect[3])*sy))
    return im.crop(box).resize((rect[2]-rect[0],rect[3]-rect[1]),Image.Resampling.NEAREST)

def crop_native(name, book=False, editor=False, tabs=False):
    path, state = captures[name]
    im = Image.open(path).convert('RGB')
    sx, sy = im.width/state['guiWidth'], im.height/state['guiHeight']
    if editor:
        box = (state['guiWidth']/2-112, 31, state['guiWidth']/2+112, state['guiHeight']/4+171)
    else:
        b = state['bounds']
        box = (b['x']-(153 if book else 4), b['y']-(27 if tabs else 4), b['x']+b['width']+4,b['y']+b['height']+4)
    return im.crop(tuple(round(v*s) for v,s in zip(box,(sx,sy,sx,sy))))

def mockup(item):
    name, rect = item['name'], item['rect']
    editor = item.get('editor',False)
    if item.get('chunks'):
        content = Image.new('RGBA',tuple(item['content_size']),FRAME.getpixel((5,5)))
        for chunk in item['chunks']:
            content.alpha_composite(region(name,chunk['rect']),tuple(chunk['at']))
    else:
        content = region(name,rect,absolute=editor)
    title = item['window_title']
    icon = item.get('icon')
    controls_width = 25
    w = max(content.width+12, text_width(title)+controls_width+16+(14 if icon else 0), 212 if editor else 0)
    h = 20+content.height+6
    frame = nine_slice(w,h)
    icon_offset = 0
    if icon:
        native_icon = region(name,icon)
        native_icon.thumbnail((12,12),Image.Resampling.NEAREST)
        frame.alpha_composite(native_icon,(6,3));icon_offset=14
    mc_text(frame,title,6+icon_offset,5)
    for col,dx in [(3,w-27),(2,w-14)]:
        frame.alpha_composite(CONTROLS.crop((col*11,0,col*11+11,11)),(dx,3))
    frame.alpha_composite(content,((w-content.width)//2,20))
    if editor:
        # The original model preview remains intact in its dark preview tile.
        button=region(name,(220,256,420,276),absolute=True)
        h=20+content.height+6+button.height+6
        frame2=nine_slice(w,h)
        frame2.alpha_composite(frame,(0,0))
        # Repaint the temporary lower edge so this is a single frame.
        fill=FRAME.getpixel((5,5))
        ImageDraw.Draw(frame2).rectangle((5,20+content.height,w-6,h-6),fill=fill)
        frame2.alpha_composite(button,((w-button.width)//2,h-6-button.height))
        frame=frame2
    if item.get('tabs'):
        combined=Image.new('RGBA',(w,h+24))
        combined.alpha_composite(frame,(0,24))
        prefix,first,second=('tiered','anvil','reforge') if item['mod']=='TieredZ' else (
            ('smitherz','smithing','smither') if name in ['smith-entry','link-gear'] else ('smitherz','grindstone','grinder'))
        selected=0 if name.endswith('-entry') else 1
        for j,icon_name in enumerate([first,second]):
            active=j==selected
            u=(0 if active else 24) if j==0 else (48 if active else 72)
            background=TAB_SHEET.crop((u,0,u+24,27 if active else (25 if j==0 else 21)))
            combined.alpha_composite(background,(4+j*25,1 if active else 3))
            tab_icon=native_asset(f'assets/{prefix}/textures/gui/{icon_name}_tab_icon.png')
            combined.alpha_composite(tab_icon,(9+j*25,8))
        frame=combined
    if item.get('book'):
        b=captures[name][1]['bounds']
        book=region(name,(-149,0,-2,166))
        combined=Image.new('RGBA',(book.width+4+frame.width,max(book.height,frame.height)))
        combined.alpha_composite(book,(0,0));combined.alpha_composite(frame,(book.width+4,0));frame=combined
    return frame

items=[]
def add(mod,name,label,rect,title=None,note='',**extra):
    items.append(dict(mod=mod,name=name,label=label,rect=rect,window_title=title or label,note=note,**extra))

for creative in [False,True]:
    for shape,rect in [('single',(52 if creative else 61,18,124 if creative else 115,71)),
                       ('double',(50,18,126,71)),('quad',(50,8,126,71))]:
        add('ExtendedDrawersAddon','drawer-'+shape+('' if creative else '-survival'),
            shape.title()+' drawer / '+('creative' if creative else 'survival'),rect,'Drawer',
            'Keep every compartment and utility control; remove the native inventory band.')
add('The Graveyard','ossuary-recipes','Ossuary',(19,14,160,71),note='Preserve the complete recipe grid, selection state and scrollbar.')
for name,title in [('urn','Urn'),('small-urn','Small urn'),('coffin','Coffin'),('sarcophagus','Sarcophagus')]:
    add('The Graveyard',name,title,(7,17,169,125),title,
        'Keep all 54 storage slots; omit the duplicate player inventory.',storage=True)
add('The Graveyard','gravestone','Gravestone editor',(260,58,380,122),'Edit Gravestone',
    'A compact modal proposal: retain the native preview, four text lines and Done control.',editor=True)
add('BetterEnd','end-smelter-closed','End Stone Smelter',(19,16,153,72),
    note='Recipe-book toggle stays inside; retain both inputs, heat and progress artwork.')
add('BetterEnd','end-smelter-book','Smelter / recipe book open',(19,16,153,72),'End Stone Smelter',
    'Keep the book attached outside, with its search, filters and recipe results area intact.',book=True)
add('BetterEnd','end-furnace','Furnace variants',(19,16,153,72),'End Stone Furnace',
    'The native furnace family shares this layout; the recipe toggle remains inside.')
add('Runes','rune-altar','Rune Crafting Altar',(26,46,152,65),'Rune Crafting',
    'Keep the three-slot crafting row; use the native rune emblem in the title bar.',icon=(10,10,48,44))
add('TieredZ','reforge-entry','Anvil / repair and name',(8,23,169,73),'Repair & Name',
    'Tabs remain outside; center the native name field and reserve the dynamic repair-cost line.',tabs=True,icon=(6,7,46,43),
    content_size=(161,50),chunks=[{'rect':(61,23,166,37),'at':(28,0)},
        {'rect':(8,45,169,67),'at':(0,20)},{'rect':(69,68,166,73),'at':(61,43)}])
add('TieredZ','reforge','Reforge Gear',(44,33,132,74),
    note='Retain the original slot arrangement and reforge button, with external tabs.',tabs=True,icon=(6,7,46,43),
    content_size=(88,41),chunks=[{'rect':(44,46,62,64),'at':(0,13)},
        {'rect':(79,33,97,51),'at':(35,0)},{'rect':(114,46,132,64),'at':(70,13)},
        {'rect':(79,56,97,74),'at':(35,23)}])
add('SmitherZ','smith-entry','Smithing / upgrade gear',(7,27,168,77),'Upgrade Gear',
    'Keep the armor preview area and all four slots; the preview space is intentional.',tabs=True,icon=(6,7,46,43),
    content_size=(161,50),chunks=[{'rect':(7,47,116,66),'at':(0,20)},
        {'rect':(139,27,168,77),'at':(132,0)}])
add('SmitherZ','link-gear','Link Gear',(34,18,166,60),
    note='Retain the six socket positions and linking controls; keep the tab strip outside.',tabs=True,icon=(6,7,46,43),
    content_size=(132,42),chunks=[{'rect':(79,18,97,36),'at':(45,0)},
        {'rect':(105,18,123,36),'at':(71,0)},{'rect':(148,18,166,36),'at':(114,0)},
        {'rect':(34,42,142,60),'at':(0,24)}])
add('SmitherZ','grind-entry','Grindstone / repair',(30,16,146,74),'Repair & Disenchant',
    'Preserve the native grindstone artwork and input/output geometry.',tabs=True)
add('SmitherZ','unlink-gear','Unlink Gear',(44,16,146,74),
    note='Keep the native unlinking slots and grindstone artwork, with external tabs.',tabs=True)
add('AdditionZ','fletching-populated','Fletching Table',(29,16,154,71),
    note='Keep all ingredients, potion input, arrow and output; remove unused side padding.')
add('AdditionZ','chunk-loader-active','Chunk Loader',(4,5,169,72),
    note='Keep compass labels, the 3 x 3 chunk selector, fuel meter and every upgrade socket.')
add('Deep Aether','combiner-active','Combiner',(19,16,121,72),
    note='Keep all three inputs, combining progress and output; place the book toggle inside.')
add('Deep Aether','combiner-book','Combiner / recipe book open',(19,16,121,72),'Combiner',
    'Keep the full recipe panel attached outside the compact machine window.',book=True)

W,H=1000,660
C=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1)
C.setTitle('Project Everything - Eight native interfaces and proposed SIU windows')
C.setAuthor("Salt's Inventory Update")
TEAL=HexColor('#147c7b'); INK=HexColor('#17333c'); MUTED=HexColor('#577078')
def txt(x,y,s,size=11,color=INK,bold=False):
    C.setFillColor(color);C.setFont('Helvetica-Bold' if bold else 'Helvetica',size);C.drawString(x,y,s)
def footer(n):
    C.setStrokeColor(HexColor('#dbe4e5'));C.line(38,39,W-38,39)
    txt(38,24,'Minecraft 1.20.1 | Native capture with all SIU mods absent | Proposed layouts are mockups',8,MUTED)
    C.setFont('Helvetica',8);C.drawRightString(W-38,24,f'{n:02d} / {len(items)+1:02d}')
def image_fit(im,x,y,w,h,max_scale=None):
    s=min(w/im.width,h/im.height)
    if max_scale is not None:s=min(s,max_scale)
    iw,ih=im.width*s,im.height*s
    C.drawImage(ImageReader(im),x+(w-iw)/2,y+(h-ih)/2,width=iw,height=ih,mask='auto')
    return s
def card(x,y,w,h):
    C.setFillColor(HexColor('#eef3f3'));C.roundRect(x,y,w,h,10,stroke=0,fill=1)

txt(40,612,'PROJECT EVERYTHING / VISUAL DESIGN REVIEW',10,TEAL,True)
txt(40,563,'Native interfaces, proposed SIU windows',29,INK,True)
txt(40,531,'Eight mods. Real screenshots first, compact window concepts alongside them.',15,MUTED)
card(40,410,920,85)
txt(60,467,'LEFT: ORIGINAL MINECRAFT SCREENSHOT',11,TEAL,True)
txt(60,441,'Captured in game without SIU Core, Compatibility or Controlify.',12)
txt(60,422,'RIGHT: DESIGN PROPOSAL - composed from native artwork and existing SIU frame sprites.',11)
mods=[('ExtendedDrawersAddon','Three drawer sizes; survival and creative controls'),
      ('The Graveyard','Ossuary, urns, coffin, sarcophagus and text editor'),
      ('BetterEnd','Smelter, recipe panel and furnace family'),('Runes','Rune Crafting Altar'),
      ('TieredZ','Anvil and reforging tabs'),('SmitherZ','Upgrade, link, repair and unlink tabs'),
      ('AdditionZ','Fletching table and chunk loader'),('Deep Aether','Combiner and recipe panel')]
for i,(mod,scope) in enumerate(mods):
    col=i//4;row=i%4;x=43+col*470;y=365-row*55
    txt(x,y,mod,13,INK,True);txt(x,y-18,scope,10,MUTED)
txt(40,103,'Chipped and Simply Swords are excluded.',12,TEAL,True)
txt(40,82,'Scope: distinct workstation/container layouts, plus the gravestone editor. No implementation or gameplay pass is claimed.',9,MUTED)
txt(40,64,'Native captures use a disposable fixture world. This is a 1.20.1 design reference, not an all-version validation report.',9,MUTED)
footer(1);C.showPage()

manifest=[]
for i,item in enumerate(items,2):
    name=item['name'];assert name in captures,f'Missing real native capture: {name}'
    path,state=captures[name];assert state['siu_loaded'] is False
    native=crop_native(name,item.get('book'),item.get('editor'),item.get('tabs'))
    mock=mockup(item)
    native_path=OUT/f'{name}-native-crop.png';mock_path=OUT/f'{name}-siu-proposal.png'
    native.save(native_path);mock.save(mock_path)
    originals=OUT/'native-full';originals.mkdir(exist_ok=True)
    preserved=originals/f'{name}.png';shutil.copy2(path,preserved)
    txt(40,615,item['mod'].upper()+' / 1.20.1 '+('FORGE' if item['mod']=='Deep Aether' else 'FABRIC'),10,TEAL,True)
    txt(40,578,item['label'],26,INK,True)
    txt(40,548,'Native screenshot',12,INK,True);txt(520,548,'Proposed SIU mockup',12,TEAL,True)
    card(40,145,440,382);card(520,145,440,382)
    image_fit(native,53,156,414,358,max_scale=1.5)
    image_fit(mock,533,156,414,358,max_scale=3)
    txt(40,125,'Original artwork and native controls',10,MUTED)
    txt(520,125,f'Proposed group: {mock.width} x {mock.height} GUI units',10,TEAL)
    # One concise design instruction per page, split on word boundaries if needed.
    words=item['note'].split();line='';y=91
    for word in words:
        if C.stringWidth((line+' '+word).strip(),'Helvetica',11)>900:
            txt(40,y,line,11);y-=16;line=word
        else:line=(line+' '+word).strip()
    if line:txt(40,y,line,11)
    footer(i);C.showPage()
    manifest.append({**item,'native_source':str(preserved.relative_to(ROOT)),
        'native_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'screen':state['screen'],
        'native_capture_metadata':state,
        'siu_loaded':False,'native_crop':str(native_path.relative_to(ROOT)),
        'proposal':str(mock_path.relative_to(ROOT)),'proposed_size':[mock.width,mock.height]})
C.save()
(OUT/'design-manifest.json').write_text(json.dumps({'minecraft':'1.20.1','kind':'visual proposals, not implemented UI',
    'excluded':['Chipped','Simply Swords'],'pages':len(items)+1,'designs':manifest},indent=2),encoding='utf-8')
print(json.dumps({'pdf':str(PDF),'pages':len(items)+1,'designs':len(items)},indent=2))
