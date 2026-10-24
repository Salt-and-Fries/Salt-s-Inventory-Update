"""Screenshot-only follow-up to the Trinkets visual tour."""
from pathlib import Path
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor, white
from reportlab.lib.utils import ImageReader
from pypdf import PdfReader

root=Path(__file__).resolve().parents[1]
shots=root/'output/screenshots/trinkets-1.21.1-fabric'
out=root/'output/pdf/trinkets-1.21.1-fabric-visual-fixes.pdf'
pdfmetrics.registerFont(TTFont('UI','C:/Windows/Fonts/segoeui.ttf'))
pdfmetrics.registerFont(TTFont('Bold','C:/Windows/Fonts/segoeuib.ttf'))
c=canvas.Canvas(str(out),pagesize=(1000,700),pageCompression=1)
c.setTitle('Salts Inventory + Trinkets - Visual Fixes')
c.setAuthor('Salts Inventory Update')
ink=HexColor('#142936'); teal=HexColor('#067C83'); gray=HexColor('#536875')

def text(x,y,s,size=14,bold=False,color=ink):
    c.setFillColor(color); c.setFont('Bold' if bold else 'UI',size); c.drawString(x,y,s)
def start(title,n):
    c.setFillColor(HexColor('#F5F8FA'));c.rect(0,0,1000,700,fill=1,stroke=0)
    text(40,665,'SALTS INVENTORY + TRINKETS / VISUAL FIXES',11,True,teal)
    text(40,625,title,29,True)
    text(40,23,'Minecraft 1.21.1 Fabric / Core 0.1.5 + Compatibility 0.1.2 / Trinkets 3.10.0',10,color=gray)
    text(910,23,f'{n:02d} / 03',10,color=gray)
def shot(file,crop,box):
    im=Image.open(shots/file).convert('RGB').crop(crop)
    x,y,w,h=box;s=min(w/im.width,h/im.height);dw,dh=im.width*s,im.height*s
    x+=(w-dw)/2;y+=(h-dh)/2
    c.setFillColor(white);c.setStrokeColor(HexColor('#D4E0E6'))
    c.roundRect(x-2,y-2,dw+4,dh+4,5,fill=1,stroke=1)
    im=im.resize((round(dw*2),round(dh*2)),Image.Resampling.NEAREST)
    c.drawImage(ImageReader(im),x,y,width=dw,height=dh)
    return lambda px,py:(x+(px-crop[0])*s,y+dh-(py-crop[1])*s)
def badge(p,xy,n):
    x,y=p(*xy);c.setFillColor(teal);c.setStrokeColor(white);c.setLineWidth(1.5)
    c.circle(x,y,11,stroke=1,fill=1);c.setFillColor(white);c.setFont('Bold',10);c.drawCentredString(x,y-3.5,str(n))

start('Panels now cover the player preview',1)
text(183,581,'BEFORE',13,True,gray);text(666,581,'AFTER',13,True,teal)
for name,title,crop,y in [
    ('head-group.png','Hat + face',(0,284,170,366),404),
    ('chest-group.png','Necklace',(31,320,171,399),231),
    ('legs-group.png','Belt',(31,356,171,435),58),
]:
    text(40,y+145,title,15,True)
    shot(name,crop,(150,y,290,140))
    shot('fixes/'+name,crop,(620,y,290,140))
c.showPage()

start('The real offhand, in both locations',2)
text(40,579,'HUD hotbar enabled',17,True)
p=shot('fixes/offhand-both-views.png',(20,110,615,480),(40,151,650,404))
badge(p,(200,318),1);badge(p,(209,458),2)
text(727,525,'1  Character offhand',16,True)
text(727,489,'2  HUD offhand',16,True)
text(727,447,'One shared stack of four',13,color=gray)
text(727,375,'Inventory hotbar enabled',15,True)
p=shot('fixes/offhand-inventory-layout.png',(175,391,285,448),(727,220,225,140))
text(727,184,'Character keeps the real slot.',13,color=gray)
text(40,90,'Placement, pickup, splitting and merging worked across the two views.',14,True)
text(40,62,'No blank accessory-only anchor. The original four ingots remained one native offhand stack.',12,color=gray)
c.showPage()

start('Consistent foreground layers',3)
text(40,579,'Panel frame, icons and items',17,True)
p=shot('fixes/ring-panel.png',(170,219,280,348),(40,160,310,390))
text(399,579,'A later window covers accessories',17,True)
shot('fixes/foreground-window.png',(25,143,410,475),(400,312,245,230))
text(688,579,'The recipe guide stays above',17,True)
shot('fixes/recipe-guide.png',(0,150,385,480),(689,312,270,230))
text(399,257,'Foreground windows and the recipe guide retain their input priority.',13,color=gray)
text(399,214,'Native tooltips and slot highlights remain above their panel backgrounds.',13,color=gray)
text(40,76,'Focused in-game checks only. Screenshots use sample accessory assignments and wearable QA items.',11,color=gray)
c.showPage();c.save()
assert len(PdfReader(out).pages)==3
print(out)
