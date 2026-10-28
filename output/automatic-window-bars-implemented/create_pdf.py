import json
from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
W,H=1000,650;o=Path('output/automatic-window-bars-implemented');rows=json.loads((o/'screenshot-manifest.json').read_text());c=canvas.Canvas('output/pdf/siu-automatic-window-bars-implemented.pdf',pagesize=(W,H));ink=HexColor('#20353b');muted=HexColor('#587078');accent=HexColor('#007b7b');pale=HexColor('#eef2f1')
def t(x,y,s,size=12,col=ink,b=False):c.setFillColor(col);c.setFont('Helvetica-Bold' if b else 'Helvetica',size);c.drawString(x,y,s)
def card(x,y,w,h):c.setFillColor(pale);c.roundRect(x,y,w,h,12,fill=1,stroke=0)
def image(path,x,y,w,h):
 a=ImageReader(path);iw,ih=a.getSize();k=min(w/iw,h/ih);c.drawImage(a,x+(w-iw*k)/2,y+(h-ih*k)/2,width=iw*k,height=ih*k,mask='auto')
def head(k,title,sub):t(35,615,k,10,accent,True);t(35,578,title,24,ink,True);t(35,550,sub,11,muted)
def foot(n):c.setStrokeColor(HexColor('#d5dfdc'));c.line(35,36,965,36);t(35,20,'ACTUAL GAMEPLAY CAPTURES  /  FOCUSED VALIDATION',8,muted,True);c.setFont('Helvetica',8);c.drawRightString(965,20,f'{n} / 4');c.showPage()
def pair(a,b):
 for x,r in [(35,a),(520,b)]:
  card(x,140,445,378);t(x+18,491,r['scope'].upper()+' / '+r['loader'].upper(),9,accent,True);t(x+18,470,r['label'],14,ink,True);image(r['crop'],x+15,175,415,275)
head('01 / FABRIC 1.20.1','Native bodies between SIU bars','Core-only automatic hosting; screenshots show the final implementation in-game.')
pair(rows[0],rows[1]);foot(1)
head('02 / FORGE 1.20.1','The same automatic treatment','Core-only automatic hosting; the Archeology Table shows a real processing result.')
pair(rows[2],rows[3]);foot(2)
head('03 / API SCOPE','Existing API styling remains separate','Same mod and game version, shown with and without Compatibility.')
pair(rows[1],rows[4]);foot(3)
head('04 / FOCUSED CHECKS','What was exercised','These checks cover the changed windows; they are not a complete gameplay suite.')
card(35,195,930,325);t(60,475,'AUTOMATIC WINDOWS',12,accent,True)
for y,line in zip([447,422,397],['Nether Chest: last-row item pickup and return preserved the stack and cursor on both loaders.','Dragging moved the hosted window together; the native body stayed joined to its bars.','Archeology Table: a real artifact was processed and its result taken to the cursor.']):t(60,y,line,12)
t(60,350,'API REFERENCE',12,accent,True);t(60,322,'Compatibility-hosted Archeology retained its existing parchment template on Fabric.',12)
t(60,273,'COVERAGE',12,accent,True);t(60,245,'Gameplay: Minecraft 1.20.1 Fabric and Forge. Forge used recorded mapped QA runtime copies.',12);t(60,222,'Other target builds and public API byte comparisons are documented in the validation records.',12)
t(35,165,'Images show build 05; build 06 retained the rendering and passed a carried-item footer-click check.',10,muted)
foot(4);c.save();print('saved')
