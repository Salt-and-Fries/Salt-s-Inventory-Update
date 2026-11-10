"""Five-page report made from actual native and integrated gameplay captures."""
from pathlib import Path
import json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'output/travelers-backpack-implemented'
SHOTS=BASE/'screenshots'
META=BASE/'functional-evidence'
PDF=ROOT/'output/pdf/siu-travelers-backpack-1.20.1.pdf'
W,H=792,612
INK='#20383D'; TEAL='#197D80'; GRAY='#607174'; PALE='#F0F5F5'
c=canvas.Canvas(str(PDF),pagesize=(W,H))
c.setTitle("Traveler's Backpack - SIU implementation and focused validation")
c.setAuthor("Salt's Inventory Update")

def text(s,x,y,size=10,color=INK,bold=False):
    c.setFillColor(HexColor(color));c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.drawString(x,y,s)

def para(s,x,y,width,size=10.5,color=INK):
    style=ParagraphStyle('body',fontName='Helvetica',fontSize=size,leading=size*1.4,textColor=HexColor(color))
    p=Paragraph(s,style);_,height=p.wrap(width,1000);p.drawOn(c,x,y-height);return y-height

def page(n,title,sub):
    c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
    text('PROJECT EVERYTHING / IMPLEMENTED WINDOW REVIEW',36,581,9,TEAL,True)
    text(title,36,548,25,bold=True);text(sub,36,527,10,GRAY)
    c.setStrokeColor(HexColor('#D4DEDE'));c.line(36,34,756,34)
    text('Minecraft 1.20.1 / Traveler\'s Backpack Fabric 9.1.55 / real gameplay screenshots',36,20,8,GRAY)
    text(str(n)+' / 5',726,20,8,GRAY)

def state(name):
    d=json.loads((META/(name+'.json')).read_text());return d.get('client',d)

def native_window(s):
    return next(w for w in reversed(s['allWindows']) if w.get('menuId','').startswith('travelersbackpack:'))

def crop(name,mode='window',widget=None):
    image=Image.open(SHOTS/(name+'.png')).convert('RGB')
    if mode=='full':return image
    if name=='travelers-original':box=(228,137,433,313);gw,gh=640,450
    elif name=='travelers-upgraded-original':box=(168,83,477,367);gw,gh=640,450
    else:
        s=state(name);w=native_window(s);b=w['bounds'];gw,gh=s['guiWidth'],s['guiHeight']
        boxes=[(b['x'],b['y'],b['x']+b['width'],b['y']+b['height'])]
        if widget:
            obj=next(a for a in w['nativeValues']['customWidgets'] if a['type']==widget)
            nb=w['nativeBounds'];sx=obj['bounds'][0]-nb['x'];sy=obj['bounds'][1]-nb['y']
            r=next(r for r in w['composition']['regions'] if r['source']['x']==sx and r['source']['y']==sy)
            boxes=[(b['x']+r['x'],b['y']+r['y'],b['x']+r['x']+r['source']['width'],b['y']+r['y']+r['source']['height'])]
        else:
            boxes += [(b['x']+r['x'],b['y']+r['y'],b['x']+r['x']+r['source']['width'],b['y']+r['y']+r['source']['height']) for r in w['composition']['regions'] if r.get('exterior')]
        box=(min(v[0] for v in boxes)-4,min(v[1] for v in boxes)-4,max(v[2] for v in boxes)+4,max(v[3] for v in boxes)+4)
    scale_x=image.width/gw;scale_y=image.height/gh
    return image.crop((max(0,int(box[0]*scale_x)),max(0,int(box[1]*scale_y)),min(image.width,int(box[2]*scale_x)),min(image.height,int(box[3]*scale_y))))

def picture(name,x,y,width,height,label,mode='window',widget=None):
    text(label,x,y+height+10,9,TEAL,True)
    c.setFillColor(HexColor(PALE));c.roundRect(x,y,width,height,8,fill=1,stroke=0)
    im=crop(name,mode,widget);scale=min((width-16)/im.width,(height-16)/im.height)
    dw,dh=im.width*scale,im.height*scale
    c.drawImage(ImageReader(im),x+(width-dw)/2,y+(height-dh)/2,dw,dh)

page(1,"Traveler's Backpack",'Storage and tools become SIU windows; the backpack keeps its own upgrade controls.')
picture('travelers-original',36,226,346,270,'WITHOUT SIU / ORIGINAL BACKPACK')
picture('tools-open',410,226,346,270,'WITH SIU / EDIT TOOLS OPEN')
para('<b>The four redundant buttons are removed:</b> Sort, Stack, Move Out and Move In. Edit Tools remains, with its original expandable tool drawer. Equipment actions and upgrade settings are separate native features and remain available.',36,203,720,11)
para('The original storage slots, item rules and gray artwork are retained. Player inventory opens in its usual separate SIU window. SIU supplies window management, sorting and transfers.',36,137,720,11)
text('Native capacity progression',36,71,10,TEAL,True)
text('Leather: 27   /   Iron: 45   /   Gold: 63   /   Diamond: 81   /   Netherite: 99 storage slots',192,71,10)
c.showPage()

page(2,'The larger backpack stays familiar','The original grid, fluid columns, tool drawer and attached upgrade rail remain intact.')
picture('travelers-upgraded-original',36,155,346,341,'WITHOUT SIU / NETHERITE BACKPACK')
picture('crafting-open',410,155,346,341,'WITH SIU / NATIVE CRAFTING PANEL')
para('The backpack retains all 99 storage slots, six tool slots and six upgrade slots at the highest default tier. Expanding an upgrade uses its original tab artwork and controls. The player-inventory section is removed from the backpack body.',36,132,720,10.5)
para('<b>Crafting was exercised:</b> a log produces four planks, the result appears in the SIU panel, and taking it consumes the correct ingredient. Installing and removing an upgrade also rebuilds the live menu without reopening it.',36,78,720,10.5)
c.showPage()

page(3,'Original upgrade panels, preserved','Close-ups of the actual panels inside SIU. These are gameplay captures, not design mockups.')
panels=[('tanks-open','TankWidget','TANKS','Two tanks, fluid slots and native tank actions.'),('placed-smelting-active','AbstractSmeltingWidget','SMELTING','Original fuel, input, output and progress.'),('filter-settings','AutoPickupWidget','PICKUP / FILTERS','Ghost items, matching modes and enable switch.'),('upgrade-feeding','FeedingWidget','FEEDING','Native food filters and feeding settings.'),('upgrade-refill','UpgradeWidgetBase','REFILL','Original refill filters and enable control.'),('upgrade-jukebox','JukeboxWidget','JUKEBOX','Native record slot and playback controls.')]
for i,(name,widget,title,caption) in enumerate(panels):
    col=i%3;row=i//3;x=36+col*246;y=320-row*205
    picture(name,x,y,228,160,title,widget=widget)
    para(caption,x,y-8,228,9.5)
para('<b>Also retained:</b> Magnet and Void filter panels; Blast Furnace and Smoker variants of the smelting panel; and the Lantern tab with its native enable/remove controls. All twelve upgrade types were loaded and their available panels were checked.',36,66,720,9.5)
c.showPage()

page(4,'Settings and open modes','The backpack continues to use its original menu logic and save data.')
picture('settings-memory',36,270,228,219,'MEMORY / UNSORTABLE SETTINGS')
picture('worn-actions',282,270,228,219,'WORN BACKPACK / EXTRA ACTIONS')
picture('placed-smelting-done',528,270,228,219,'PLACED BACKPACK / SMELTING RESULT')
rows=[('Storage and tools','Native slot restrictions, tool editing, tier capacity and scrolling. The open physical backpack is locked against movement.'),('Memory and sorting exclusions','Memory selections and unsortable selections persist. SIU sorting uses storage cells and excludes tools, upgrades and unsortable cells.'),('Fluids and processing','Water fill/drain and independent lava storage were checked. A placed furnace smelted iron ore, synchronized the ingot and allowed it to be taken.'),('Equipment and native gameplay','Equip and the real B key open the worn backpack. More Actions remains available. The placed menu and its settings retain their native open/back behavior.'),('Other mod functionality','Hose modes, radial tool selection, sleeping bags, backpack abilities, colors and other gameplay systems continue through the original mod. Their complete gameplay behavior was not retested.')]
y=245
for title,body in rows:
    text(title,36,y,10,TEAL,True)
    y=para(body,222,y+2,534,10)-14
c.showPage()

page(5,'A backpack in the SIU workspace','Actual full-screen capture with the backpack and player inventory open together.')
picture('workspace-final',36,132,492,365,'WITH SIU / FULL GAME SCREEN',mode='full')
checks=json.loads((BASE/'focused-checks.json').read_text())
assert checks and all(r['passed'] for r in checks),'Unresolved focused check failure'
y=490
y=para('<b>'+str(len(checks))+' distinct focused live checks passed.</b> Validation covered the affected backpack menus and controls, including follow-up checks for issues found during testing.',550,y,206,11)-20
y=para('<b>Builds:</b> Fabric remapping and Forge compilation/reobfuscation passed. Live validation used Fabric 9.1.55; Forge gameplay was not tested.',550,y,206,10.5)-20
y=para('<b>Scope:</b> inventory operations, tool restrictions, crafting, fluid transfer, settings, upgrade panels and live upgrade changes, worn/placed opening, sorting and compact-GUI storage scrolling.',550,y,206,10.5)-20
para('<b>Not run:</b> the complete gameplay suite, dedicated-server multiplayer, every passive upgrade effect, third-party accessory integrations or recipe-browser transfer.',550,y,206,10.5)
para('Compatibility-only implementation. Core and the public API are unchanged. The adapter checks for the tested native packet/menu shape; a different unsupported protocol keeps its normal fallback path.',36,105,720,10.5)
text('Evidence: output/travelers-backpack-implemented/   |   Raw captures are retained alongside the report.',36,55,9,GRAY)
c.showPage();c.save()
print(PDF)
