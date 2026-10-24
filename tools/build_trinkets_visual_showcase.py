"""Create a screenshot-led tour from real 1.21.1 Fabric gameplay captures."""
from pathlib import Path
from io import BytesIO
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, Color, white
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.utils import ImageReader
from pypdf import PdfReader
import json

ROOT = Path(__file__).resolve().parents[1]
SHOTS = ROOT / 'output/screenshots/trinkets-1.21.1-fabric'
OUT = ROOT / 'output/pdf/trinkets-1.21.1-fabric-visual-showcase.pdf'
TMP = ROOT / 'tmp/pdfs/trinkets-visual-showcase'
TMP.mkdir(parents=True, exist_ok=True)
OUT.parent.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont('UI', 'C:/Windows/Fonts/segoeui.ttf'))
pdfmetrics.registerFont(TTFont('UI-Bold', 'C:/Windows/Fonts/segoeuib.ttf'))

W, H = 1000, 700
INK = HexColor('#142936')
MUTED = HexColor('#536875')
ACCENT = HexColor('#067C83')
BG = HexColor('#F5F8FA')
LINE = HexColor('#D4E0E6')
c = canvas.Canvas(str(OUT), pagesize=(W,H), pageCompression=1)
c.setTitle('Salts Inventory + Trinkets - Visual Tour')
c.setAuthor('Salts Inventory Update')
c.setSubject('Labeled gameplay screenshots - Minecraft 1.21.1 Fabric')
manifest = []
page = 0

def text(x, y, value, size=14, color=INK, bold=False):
    c.setFillColor(color)
    c.setFont('UI-Bold' if bold else 'UI', size)
    c.drawString(x,y,value)

def start(title, subtitle):
    global page
    page += 1
    c.setFillColor(BG); c.rect(0,0,W,H,fill=1,stroke=0)
    text(40,666,'SALTS INVENTORY + TRINKETS',11,ACCENT,True)
    text(40,626,title,29,INK,True)
    text(40,600,subtitle,13,MUTED)
    c.setStrokeColor(LINE); c.line(40,584,960,584)
    text(40,23,'1.21.1 Fabric  /  Core 0.1.5  /  Compatibility 0.1.2  /  Trinkets 3.10.0',10,MUTED)
    c.setFont('UI',10); c.setFillColor(MUTED); c.drawRightString(960,23,f'{page:02d} / 07')

def end():
    c.showPage()

def shot(name, crop, box):
    """Fit a real screenshot crop, retaining its aspect ratio and pixel edges."""
    im = Image.open(SHOTS / name).convert('RGB')
    if crop: im = im.crop(crop)
    x,y,bw,bh = box
    scale = min(bw/im.width,bh/im.height)
    dw,dh = im.width*scale, im.height*scale
    dx,dy = x+(bw-dw)/2,y+(bh-dh)/2
    c.setFillColor(white); c.setStrokeColor(LINE)
    c.roundRect(dx-2,dy-2,dw+4,dh+4,6,fill=1,stroke=1)
    # Nearest-neighbor enlargement keeps Minecraft's original pixels sharp.
    enlarged = im.resize((round(dw*2),round(dh*2)), Image.Resampling.NEAREST)
    c.drawImage(ImageReader(enlarged),dx,dy,width=dw,height=dh)
    origin = crop or (0,0,Image.open(SHOTS/name).width,Image.open(SHOTS/name).height)
    manifest.append({'page':page,'file':name,'crop':origin})
    def point(px,py): return dx+(px-origin[0])*scale,dy+dh-(py-origin[1])*scale
    return point

def badge(x,y,num):
    c.setStrokeColor(white); c.setLineWidth(1.5); c.setFillColor(ACCENT)
    c.circle(x,y,11,fill=1,stroke=1)
    c.setFillColor(white); c.setFont('UI-Bold',10); c.drawCentredString(x,y-3.6,str(num))

def label(x,y,num,value):
    badge(x+11,y+4,num); text(x+30,y,value,13,INK,True)

def ring(point,rect):
    x1,y1 = point(rect[0],rect[1]); x2,y2 = point(rect[2],rect[3])
    c.setStrokeColor(ACCENT); c.setLineWidth(2.3)
    c.roundRect(x1,y2,x2-x1,y1-y2,5,fill=0,stroke=1)

def note(value,y=47): text(40,y,value,10,MUTED)

# 1 - one desktop, basic Character structure.
start('Character + inventory','A familiar player layout with accessory groups in the same desktop.')
p=shot('overview.png',(12,105,615,470),(112,151,776,421))
for number,xy in [(1,(58,342)),(2,(129,350)),(3,(254,338)),(4,(352,350)),(5,(257,418)),(6,(305,264))]:
    badge(*p(*xy),number)
for x,y,n,v in [(40,115,1,'Armor anchors'),(350,115,2,'Player preview'),(675,115,3,'2 x 2 crafting'),
                (40,78,4,'Crafting output'),(350,78,5,'Recipe guide'),(675,78,6,'Window controls')]: label(x,y,n,v)
note('Real gameplay captures. Accessory assignments and sample wearable items use the isolated QA fixture.')
end()

# 2 - every armor-linked group represented by the fixture.
start('Armor-linked accessory groups','Hover an armor anchor to reveal its native accessory panel.')
cards=[('head-group.png','Head','Hat + face', (7,284,118,336)),
       ('chest-group.png','Chest','Necklace', (38,320,118,371)),
       ('legs-group.png','Legs','Belt', (38,356,118,407)),
       ('feet-group.png','Feet','Shoes', (38,392,118,443))]
for i,(name,title,sub,area) in enumerate(cards):
    x=40+(i%2)*470; y=319 if i<2 else 67
    text(x,y+231,title,18,INK,True); text(x+95,y+232,sub,13,MUTED)
    p=shot(name,(0,244,386,452),(x,y,450,218)); ring(p,area)
end()

# 3 - horizontal vs vertical native presentation.
start('Hand groups + stacked instances','Native group artwork, equipment stacks and expandable ring slots.')
text(40,550,'01  Horizontal group',19,INK,True)
p=shot('hand-group.png',(24,245,387,452),(40,249,446,280)); ring(p,(175,354,264,410))
label(40,214,1,'Glove slot'); label(260,214,2,'Ring slot')
for number,px in [(1,199),(2,236)]:
    x1,y1=p(px,355); x2,y2=p(px,372)
    c.setStrokeColor(ACCENT); c.setLineWidth(1.5); c.line(x1,y1,x2,y2)
    badge(x1,y1,number)
text(530,550,'02  Multiple ring instances',19,INK,True)
p=shot('four-ring-instances.png',(169,313,286,480),(570,123,340,400))
ring(p,(211,321,261,477))
text(530,79,'Four instances after a native capacity increase',13,INK,True)
text(40,151,'One group opens sideways.',15,MUTED)
text(40,126,'Additional instances open vertically.',15,MUTED)
end()

# 4 - Core offhand visibility setting remains distinct from accessory slots.
start('Offhand follows your setting','Accessory access remains available with either hotbar layout.')
text(40,550,'Return Hotbar to Inventory: ON',18,INK,True)
p=shot('four-ring-instances.png',(28,246,386,480),(40,231,446,293))
x1,y1=p(166,417); x2,y2=p(190,417)
c.setStrokeColor(ACCENT); c.setLineWidth(1.5); c.line(x1,y1,x2,y2)
badge(x1,y1,1)
label(40,190,1,'Real offhand slot in Character')
text(528,550,'Return Hotbar to Inventory: OFF',18,INK,True)
p=shot('hidden-offhand-accessories.png',(28,246,386,480),(528,231,432,293))
badge(*p(200,418),2); badge(*p(236,453),3)
label(528,190,2,'Accessory-only group anchor')
label(528,153,3,'Offhand-linked accessory instances')
note('The blank accessory anchor is separate from the real offhand inventory slot.',95)
end()

# 5 - a real carried stack across two windows, then native equipment insertion.
start('One shared cursor','Inventory pickup, cross-window carrying, then native accessory placement.')
text(40,550,'01  Pick up in Inventory',18,INK,True)
p=shot('inventory-pickup.png',(242,113,610,365),(40,289,440,250)); ring(p,(262,159,298,195))
text(528,550,'02  Carry into Character',18,INK,True)
p=shot('accessory-cursor.png',(28,245,386,450),(528,289,432,250)); ring(p,(182,400,218,435))
text(40,248,'03  Place into a native accessory slot',18,INK,True)
p=shot('accessory-equipped.png',(136,353,563,480),(40,69,630,166)); ring(p,(218,400,254,436))
text(700,175,'Same carried stack',17,INK,True)
text(700,141,'Native equip validation',14,MUTED)
text(700,114,'Native item tooltip',14,MUTED)
end()

# 6 - magnified native artwork and tooltips, no fabricated UI.
start('Native details','Original slot icons, panel frames, item stacks and tooltip text.')
text(40,550,'Item tooltip',18,INK,True)
p=shot('native-tooltip.png',(170,345,536,414),(40,365,920,174))
text(40,324,'Slot silhouettes',18,INK,True)
p=shot('head-group.png',(0,284,120,336),(40,141,356,160))
text(40,101,'Hat / armor anchor / face',13,MUTED)
text(475,324,'Native multi-instance panel',18,INK,True)
p=shot('hidden-offhand-accessories.png',(137,352,266,479),(475,83,260,220))
text(765,217,'Glove icon',14,INK,True)
text(765,180,'Ring icons',14,INK,True)
text(765,143,'Native frame',14,INK,True)
note('Gold, emeralds and diamonds are sample wearable test items, not items added by Trinkets.')
end()

# 7 - a correctly layered book over the attached native panels.
start('Recipe guide + desktop layers','The recipe guide stays above covered accessory artwork and input.')
p=shot('recipe-book-occlusion.png',(0,111,614,480),(40,141,709,426))
badge(*p(210,210),1); badge(*p(198,384),2); badge(*p(579,133),3)
label(778,480,1,'Recipe guide')
label(778,402,2,'Covered accessories')
label(778,324,3,'Inventory controls')
text(40,93,'Separate movable windows',15,INK,True)
text(420,93,'Native recipe guide',15,INK,True)
text(710,93,'No second inventory cursor',15,INK,True)
end()

c.save()
reader=PdfReader(OUT)
assert len(reader.pages)==7
assert all((p.extract_text() or '').strip() for p in reader.pages)
(TMP/'screenshot-manifest.json').write_text(json.dumps(manifest,indent=2))
print(f'{OUT}\n{len(reader.pages)} pages; {OUT.stat().st_size} bytes')
