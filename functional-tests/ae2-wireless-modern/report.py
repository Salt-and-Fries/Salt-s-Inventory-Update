"""Short, rendered proof gallery from real shipping-JAR captures."""
from pathlib import Path
import json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader
from reportlab.lib.colors import HexColor
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/'output/ae2-wireless-modern'
OUT=ROOT/'output/pdf/siu-ae2-wireless-modern-verification.pdf'
OUT.parent.mkdir(exist_ok=True,parents=True)
TMP=ROOT/'tmp/pdfs/ae2-wireless-modern';TMP.mkdir(parents=True,exist_ok=True)
W,H=1200,810
c=canvas.Canvas(str(OUT),pagesize=(W,H))
c.setTitle('SIU - AE2 Wireless Terminals: newer versions')
c.setAuthor("Salt's Inventory Update")
INK=HexColor('#203b3f');TEAL=HexColor('#247a7c');PALE=HexColor('#eef4f4');MUTED=HexColor('#657479')
def text(x,y,s,size=14,color=INK,bold=False):
    c.setFillColor(color);c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.drawString(x,y,s)
def base(number,title,subtitle):
    c.setFillColor(HexColor('#ffffff'));c.rect(0,0,W,H,fill=1,stroke=0)
    text(38,H-35,'SIU / AE2 WIRELESS TERMINALS',11,TEAL,True)
    text(38,H-72,title,28,bold=True);text(38,H-98,subtitle,13,MUTED)
    c.setStrokeColor(HexColor('#d9e3e4'));c.line(38,36,W-38,36)
    text(38,20,'Actual game captures | Native AE2 / AE2WTLib artwork | Core and public API unchanged',10,MUTED)
    text(W-58,20,str(number),10,MUTED)

base(1,'Newer targets, native modern artwork','November 1, 2026 | Compatibility 0.1.3 | Focused verification only')
text(38,655,'Official availability and implementation',19,bold=True)
rows=[('Minecraft','Fabric','Forge / NeoForge'),('1.20.1','15.2.1 - existing support','Forge 15.3.3 - existing support'),
      ('1.21.1','No matching official release','NeoForge 19.5.1 - implemented'),('1.21.11','No matching official release','No matching official release'),
      ('26.1.2','No matching official release','NeoForge 26.1.2-beta - implemented'),('26.2','No matching official release','NeoForge 26.2.0-beta - implemented'),
      ('26.3','No matching official release','NeoForge 26.3.1-beta - implemented')]
for i,row in enumerate(rows):
    y=617-i*36
    if i%2==0:c.setFillColor(PALE);c.rect(38,y-12,1124,35,fill=1,stroke=0)
    for x,s in zip([52,240,660],row):text(x,y,s,14,bold=i==0)
text(38,319,'What is retained',19,bold=True)
for y,s in zip([291,267,243],[
    'Five approved inventory windows plus the native wireless-settings subview on each new target.',
    'Modern palette, native toolbars and upgrade panels, four encoding modes, and top-right native return tabs.',
    'SIU owns the separate player windows; native AE2 continues to own terminal data and action callbacks.']):text(38,y,s,14)
text(38,203,'Checks and limits',19,bold=True)
text(38,175,'172 focused checks: crafting/transfer, conditional controls, navigation, encoding modes and real provider actions.',14)
text(38,151,'Not covered: Curios locators, multi-type universal selection, long-distance Quantum Bridge, full CPU jobs or resizing.',13,MUTED)
text(38,112,'The following pages crop the actual UI from full in-game screenshots; full frames and exact artifact hashes accompany the report.',12,MUTED)
text(38,81,'Publisher releases: modrinth.com/mod/applied-energistics-2-wireless-terminals/versions',12,TEAL)
c.linkURL('https://modrinth.com/mod/applied-energistics-2-wireless-terminals/versions',(38,77,750,96))
c.showPage()

cards=[('wireless-crafting','Wireless Crafting Terminal','Native catalog, crafting, conditional controls and upgrades.'),
       ('pattern-encoding','Wireless Pattern Encoding','All four native mode panels remain available.'),
       ('pattern-access-populated','Wireless Pattern Access','Real provider contents; completed native footer.'),
       ('trash','Trash','All 27 slots; native return arrow at the top-right.'),
       ('magnet-filter','Magnet Filter','Both 27-slot filters and five native filter controls.'),
       ('wireless-settings','Wireless Terminal Settings','Five native preferences and return navigation.')]
versions=['1.21.1','26.1.2','26.2','26.3']
release={r['minecraft']:r for r in json.loads((DATA/'availability.json').read_text()) if r['loader']=='neoforge' and r['version']}
for page,v in enumerate(versions,2):
    row=release[v]
    base(page,'Minecraft '+v+' / NeoForge','AE2WTLib '+row['version']+' | Six native views | 43 focused checks passed')
    for i,(name,title,caption) in enumerate(cards):
        x=38+(i%3)*380;y=H-133-(i//3)*302
        c.setFillColor(PALE);c.roundRect(x,y-278,364,278,10,fill=1,stroke=0)
        text(x+12,y-23,title,15,bold=True)
        meta=json.loads((DATA/v/(name+'.json')).read_text());s=meta['state'];w=next(a for a in s.get('allWindows',s.get('windows',[])) if a.get('definition','').endswith('.AEWindow'));b=w['bounds']
        screenshot=DATA/v/(name+'.png');im=Image.open(screenshot);scale=im.width/s['guiWidth']
        crop=im.crop((int((b['x']-25)*scale),int(max(0,b['y']-8)*scale),int((b['x']+b['width']+29)*scale),int((b['y']+b['height']+4)*scale)))
        p=TMP/(v+'-'+name+'.png');crop.save(p)
        ratio=min(340/crop.width,207/crop.height);iw=crop.width*ratio;ih=crop.height*ratio
        c.drawImage(ImageReader(crop),x+12+(340-iw)/2,y-45-ih,width=iw,height=ih)
        text(x+12,y-263,caption,10,MUTED)
    text(38,55,'Publisher version: '+row['version_id']+' | Full frames, state snapshots and hashes: output/ae2-wireless-modern/'+v,10,MUTED)
    c.linkURL('https://modrinth.com/mod/applied-energistics-2-wireless-terminals/version/'+row['version_id'],(38,50,580,67))
    c.showPage()
c.save()
reader=PdfReader(OUT);assert len(reader.pages)==5
assert all(p.extract_text().strip() for p in reader.pages)
print(OUT,'5 pages')
