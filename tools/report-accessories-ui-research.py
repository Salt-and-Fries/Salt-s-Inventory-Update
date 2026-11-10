"""Five-page native Accessories UI atlas from verified gameplay captures."""
from pathlib import Path
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle

ROOT=Path(__file__).resolve().parents[1]
SHOTS=ROOT/'output/accessories-ui-research/screenshots'
PDF=ROOT/'output/pdf/siu-accessories-ui-research-1.20.1.pdf'
PDF.parent.mkdir(parents=True,exist_ok=True)
W,H=792,612
INK='#20383D';TEAL='#197D80';GRAY='#607174';PALE='#F0F5F5';BLUE='#436CAA'
c=canvas.Canvas(str(PDF),pagesize=(W,H))
c.setTitle('Accessories - Native UI and access guide - Minecraft 1.20.1')
c.setAuthor("Salt's Inventory Update / Project Everything")

def text(s,x,y,size=10,color=INK,bold=False,font=None):
    c.setFillColor(HexColor(color));c.setFont(font or ('Helvetica-Bold' if bold else 'Helvetica'),size);c.drawString(x,y,s)

def para(s,x,y,width,size=10.5,color=INK):
    p=Paragraph(s,ParagraphStyle('p',fontName='Helvetica',fontSize=size,leading=size*1.35,textColor=HexColor(color)))
    _,height=p.wrap(width,1200);p.drawOn(c,x,y-height);return y-height

def page(n,title,sub):
    c.setFillColor(HexColor('#FFFFFF'));c.rect(0,0,W,H,fill=1,stroke=0)
    text('PROJECT EVERYTHING / NATIVE UI RESEARCH',36,582,9,TEAL,True)
    text(title,36,548,24,bold=True);text(sub,36,526,10,GRAY)
    c.setStrokeColor(HexColor('#D4DEDE'));c.line(36,34,756,34)
    text('Accessories Fabric 1.0.0-beta.48+1.20.1 | SIU absent | 30 October 2026',36,20,8,GRAY)
    text(f'{n} / 5',729,20,8,GRAY)

def figure(name,box,x,y,w,h,gui=True,pale=True):
    im=Image.open(SHOTS/(name+'.png')).convert('RGB')
    if gui:box=tuple(int(v*2) for v in box)
    im=im.crop(box)
    if pale:c.setFillColor(HexColor(PALE));c.roundRect(x,y,w,h,7,fill=1,stroke=0)
    scale=min(w/im.width,h/im.height);iw,ih=im.width*scale,im.height*scale
    ix,iy=x+(w-iw)/2,y+(h-ih)/2
    c.drawImage(ImageReader(im),ix,iy,iw,ih)
    def point(gx,gy):
        if gui:gx*=2;gy*=2
        return ix+(gx-box[0])*scale,iy+ih-(gy-box[1])*scale
    return point

def ring(point,gx,gy,label=None,color=TEAL,r=8):
    x,y=point(gx,gy);c.setStrokeColor(HexColor(color));c.setLineWidth(1.4);c.circle(x,y,r,stroke=1,fill=0)
    if label:
        c.setFillColor(HexColor('#FFFFFF'));c.roundRect(x-6,y+r+1,13,13,2,fill=1,stroke=0)
        text(label,x-3.2,y+r+4,9,color,True)

def link(label,url,x,y,size=9):
    text(label,x,y,size,TEAL)
    c.linkURL(url,(x,y-2,x+c.stringWidth(label,'Helvetica',size),y+size+2),relative=0)

release='https://modrinth.com/mod/accessories/version/A7wwzJ0e'
sources='https://cdn.modrinth.com/data/jtmvUHXj/versions/A7wwzJ0e/accessories-fabric-1.0.0-beta.48%2B1.20.1-sources.jar'
groups='https://docs.wispforest.io/legacy/accessories/general/slot_groups/'

page(1,'Where Accessories adds UI','One equipment-screen family, a settings screen, inventory buttons and contextual tooltips.')
text('SURVIVAL INVENTORY',36,495,11,TEAL,True)
text('CREATIVE INVENTORY',406,495,11,TEAL,True)
a=figure('survival-entry',(228,139,409,309),36,214,350,264)
b=figure('creative-entry',(218,128,420,318),406,214,350,264)
ring(a,302,155);ring(b,322,168)
para('<b>Open:</b> Press E, then click the tiny Accessories button beside the player preview. The circled button opens the equipment screen.',36,195,350,11)
para('<b>Open:</b> Press E, select the bottom-right Inventory tab (chest icon), then click Accessories. Its button is hidden on the other creative tabs.',406,195,350,11)
c.setFillColor(HexColor(PALE));c.roundRect(36,72,720,53,6,fill=1,stroke=0)
para('<b>Direct shortcut: H.</b> Rebind it under Options &gt; Controls &gt; Key Binds &gt; Accessories &gt; Open Accessories Screen. Crouch + H targets a permitted entity instead (page 4).',49,111,694,10.5)
text("Source-verified extra hook: Creative's existing clear-inventory action also clears accessory equipment.",36,51,8.5,GRAY)
c.showPage()

page(2,'The equipment screen, mapped','The normal and cosmetic slots share one screen; they are not separate inventories to open.')
p=figure('accessories-cosmetics',(147,140,409,310),36,186,458,303)
for gx,gy,label,col in [(214,228,'A',TEAL),(194,228,'B',BLUE),(204,152,'C',TEAL),(392,155,'D',TEAL),(221,176,'E',BLUE),(377,155,'F',TEAL)]:ring(p,gx,gy,label,col,7)
items=[
 ('A  Functional slots','Right-hand column: equip and remove actual accessories.'),
 ('B  Cosmetic slots','Left-hand column: appearance overrides paired with the functional cells.'),
 ('C  Cosmetic-column toggle','Thin bar above the drawer: Show / Hide Cosmetic Slots.'),
 ('D  Unused-slot filter','Top-right control: Show / Hide Unused Slots. It rebuilds the visible slot types.'),
 ('E  Per-slot visibility','Tiny switches beside the functional cells: Show / Hide Accessory rendering.'),
 ('F  Return button','Small button above the preview: returns to the player inventory.'),
]
y=495
for title,body in items:
    text(title,516,y,10.5,TEAL,True);y=para(body,516,y-8,240,9.5)-15
para('<b>Player equipment stays here too:</b> four armor cells, offhand, 27 main-inventory cells and nine hotbar cells. The two previews show the front and back and follow the mouse.',36,165,458,11)
text('CONDITIONAL CONTROLS',516,149,9.5,TEAL,True)
para('A Unique Slots toggle appears only if Show Unique Rendering is enabled and an addon defines unique slots. More than nine groups can add tab-page arrows. Both are source-verified conditions, absent in this base-mod capture.',516,135,240,9.3)
text('Native screen class: AccessoriesScreen. Native menu: AccessoriesMenu.',36,53,8.5,GRAY)
c.showPage()

page(3,'Slot browsing and the default groups','Scroll the side panel, drag its scrollbar, or click a body-group tab to jump to that group.')
text('TOP OF LIST',36,489,10,TEAL,True);text('SCROLLED DOWN',188,489,10,TEAL,True)
figure('accessories-scroll-top',(148,141,230,309),36,200,132,273)
figure('accessories-scroll-bottom',(148,141,230,309),188,200,132,273)
text('Group',354,483,10,TEAL,True);text('Default slot types',440,483,10,TEAL,True);text('Cells',723,483,10,TEAL,True)
rows=[('Misc','Charm','1'),('Head','Hat, Face','2'),('Chest','Necklace, Cape, Back','3'),('Arms','Hand x2, Ring x2, Wrist','5'),('Legs','Belt','1'),('Feet','Shoes, Anklet','2')]
y=460
for i,(group,names,count) in enumerate(rows):
    if i%2==0:c.setFillColor(HexColor(PALE));c.rect(348,y-12,408,29,fill=1,stroke=0)
    text(group,354,y,10.5,bold=True);text(names,440,y,10);text(count,728,y,10.5);y-=31
para('<b>Baseline:</b> 12 slot types, 14 functional positions and 14 cosmetic partners, across six populated groups. Only eight rows are visible at once. Mods, data packs and slot modifiers can change this layout.',354,253,402,11)
text('WHY THE DRAWER CAN LOOK EMPTY',36,168,11,TEAL,True)
para('Hide Unused Slots filters slot <i>types</i> according to compatible items, rather than simply hiding empty cells. Accessories alone ships no ordinary accessory items, so the initial drawer can have no slots. Use Show Unused Slots to inspect the complete baseline layout.',36,150,720,11)
para('Group tabs are navigation shortcuts, not separate storage screens. Unsorted groups, unique groups and their extra tab pages depend on the installed content.',36,81,720,10,GRAY)
c.showPage()

page(4,'Entity equipment and contextual tooltips','These are two additional places to account for, even though neither adds a workstation screen.')
text('A PERMITTED ENTITY: ARMOR STAND',36,495,10.5,TEAL,True)
text('ITEM TOOLTIPS IN ORDINARY INVENTORY',406,495,10.5,TEAL,True)
figure('armor-stand-command',(147,140,409,310),36,263,350,218)
figure('accessory-item-tooltip',(230,140,410,309),406,263,350,218)
para('<b>Open:</b> Look at an armor stand within reach, hold crouch and press H. Both previews switch to the target; its accessory slots occupy the side panel.',36,246,350,10.5)
text('Operator alternative (also respects modification rules):',36,183,9.5,GRAY)
text('/accessories edit @e[type=armor_stand,limit=1,sort=nearest]',36,166,8.4,INK,font='Courier')
para('Players and armor stands receive the default slot bindings. Other living entities need slot bindings and permission from the mod or an integration. This is the same menu family, not a second standalone design.',36,146,350,10)
c.setFillColor(HexColor(PALE));c.roundRect(36,49,350,44,5,fill=1,stroke=0)
para('<b>Important:</b> the four armor cells, offhand and inventory still belong to the player, even while the preview and accessory drawer target the armor stand.',46,83,330,9.5)
para('<b>Reach:</b> Hover an equip-compatible item in a normal inventory/container. Accessories adds slot names and applicable attribute information. Hover an empty accessory cell for its slot name.',406,246,350,10.5)
text('EMPTY COSMETIC SLOT TOOLTIP',406,176,9.5,TEAL,True)
figure('cosmetic-slot-tooltip',(83,140,231,205),406,72,350,95)
para('The diamond is an operator-tagged Ring example for this research. Ordinary diamonds are not supplied as accessories.',406,62,350,8.5,GRAY)
c.showPage()

page(5,'Configuration and version boundaries','Open: Esc > Mods > Accessories > Configure. This route needs Mod Menu and Cloth Config.')
figure('accessories-config',(28,140,1212,802),36,244,480,260,gui=False)
text('Client options - one label is untranslated in the original build.',36,234,8.5,GRAY)
figure('config-slot-entry',(70,449,1210,832),36,63,480,157,gui=False)
text('Inline render/slot list editors; sample rows were canceled.',36,52,8.5,GRAY)
y=500
sections=[
 ('Client options','Group tabs, unique-slot visibility, button offsets for both inventory modes, slot scrolling, the empty-screen warning and hand-equip mode.'),
 ('Hover options','Expand Hover Options: hovered-item brightness, cycling and line effects; non-hovered rendering, dimming and opacity.'),
 ('Inline list editors','Disabled Default Rendering: slot name + render type. Slot Amount Modifiers: slot name + amount offset. Expand a list and use its + control.'),
 ('Version boundary','This beta.48 build has no experimental-screen selector, paginated layout selector or dark-theme UI. Newer Accessories screenshots should not be treated as this version.'),
]
for title,body in sections:
    text(title,536,y,10.5,TEAL,True);y=para(body,536,y-8,220,9.6)-15
text('PRIMARY SOURCES',536,y,9.5,TEAL,True);y-=18
link('Pinned beta.48 release',release,536,y);y-=15
link('Author-supplied source archive',sources,536,y);y-=15
link('Official slot-group documentation',groups,536,y);y-=19
para('Actual native UI, hotkeys and routes were inspected. Addon-only controls are marked as conditional. This is research, not a complete gameplay test or an SIU implementation.',536,y,220,8.8,GRAY)
c.showPage()
c.save()
print(PDF)
