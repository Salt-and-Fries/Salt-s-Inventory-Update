import json
from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
W,H=1000,650;o=Path('output/simply-swords-implemented');rows=json.loads((o/'screenshot-manifest.json').read_text());c=canvas.Canvas('output/pdf/siu-simply-swords-implemented-windows-1.20.1.pdf',pagesize=(W,H));ink=HexColor('#25343b');muted=HexColor('#596c72');accent=HexColor('#936d36');pale=HexColor('#f2f0eb')
def t(x,y,s,size=12,color=ink,bold=False):c.setFillColor(color);c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.drawString(x,y,s)
def lines(x,y,a,size=12,lead=20,color=muted):
 for i,s in enumerate(a):t(x,y-i*lead,s,size,color)
def box(x,y,w,h):c.setFillColor(pale);c.roundRect(x,y,w,h,12,stroke=0,fill=1)
def image(path,x,y,w,h):
 a=ImageReader(str(path));iw,ih=a.getSize();k=min(w/iw,h/ih);c.drawImage(a,x+(w-iw*k)/2,y+(h-ih*k)/2,width=iw*k,height=ih*k,mask='auto')
def head(k,title,subtitle):t(36,615,k,10,accent,True);t(36,580,title,24,ink,True);t(36,551,subtitle,11,muted)
def foot(n):c.setStrokeColor(HexColor('#d9deda'));c.line(36,36,964,36);t(36,20,'FOCUSED GAMEPLAY EVIDENCE  /  MINECRAFT 1.20.1',8,muted,True);c.setFont('Helvetica',8);c.drawRightString(964,20,f'{n} / 4');c.showPage()
head('01 / APPROVED DESIGN AND ACTUAL WINDOW','The two-bar Runic Forge','The left image is the approved mockup; the right image is an actual Fabric capture.')
for x,label,path in [(36,'APPROVED DESIGN / MOCKUP','output/simply-swords-design/mockups/hybrid-empty.png'),(517,'ACTUAL FABRIC / BUILD 02',rows[0]['crop'])]:
 box(x,175,447,345);t(x+16,493,label,9,accent,True);image(path,x+16,260,415,190)
lines(45,145,['The original torn stone edge remains visible between the charcoal title and blank lower bar.'],11,18)
foot(1)
head('02 / FABRIC GAMEPLAY','Real items and channel feedback','Actual captures from the focused Fabric run, not reconstructed UI.')
for x,r,label in [(36,rows[1],'WEAPON + GEMS + ONE TABLET'),(517,rows[2],'EIGHT TABLETS / CHANNEL PULSE')]:
 box(x,175,447,345);t(x+16,493,label+' / BUILD 02',9,accent,True);image(r['crop'],x+16,260,415,190)
lines(45,145,['All 11 visible cells were used; the 800 ms channel feedback is visible in the right capture.'],11,18)
foot(2)
head('03 / FORGE PREVIEW','The configured weapon tooltip','Actual Forge build 03; the tooltip is rendered with Simply Tooltips enabled.')
box(36,110,928,410);image(rows[4]['crop'],53,125,894,380)
foot(3)
head('04 / FOCUSED CHECKS','What was verified','This is targeted UI validation, not a complete gameplay suite.')
box(36,168,928,352);t(58,486,'NATIVE WORKFLOW',11,accent,True)
lines(58,458,['A real slumbering lichblade accepted both gem types and individual tablets on Fabric.','Taking the weapon committed its level and gem data; closing committed all eight tablets.','The client preview matched server NBT. Source removal closed the window and returned the weapon.'],12,21,ink)
t(58,365,'FORGE AND OPTIONAL MOD',11,accent,True)
lines(58,337,['Forge build 03 accepted a real weapon, tablet and gem, then committed its preview.','Fabric build 03 started and opened an ordinary chest with Simply Swords absent.'],12,21,ink)
t(58,264,'COVERAGE LIMITS',11,accent,True)
lines(58,239,['Fabric used a tooltip-only QA mixin workaround; Forge used recorded named-runtime annotation mapping.','Quick-move was exercised through a QA helper, not a physical Shift key. Full details: validation.json.'],10,19)
lines(36,134,['Fabric images are build 02; final build 03 changes only the server registration guard.'],10,18)
foot(4);c.save();print('saved')
