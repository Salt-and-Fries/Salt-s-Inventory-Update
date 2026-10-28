"""Render curated Chipped proposed-versus-implemented evidence, not mockups as runtime."""
import argparse,json
from pathlib import Path
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
p=argparse.ArgumentParser();p.add_argument("manifest",type=Path);p.add_argument("output",type=Path);a=p.parse_args()
data=json.loads(a.manifest.read_text());rows=data["comparisons"];W,H=1000,660;a.output.parent.mkdir(parents=True,exist_ok=True);c=canvas.Canvas(str(a.output),pagesize=(W,H));INK=HexColor("#21343b");MUTED=HexColor("#60747a");TEAL=HexColor("#137c7b");PALE=HexColor("#f3eee7")
def text(x,y,s,size=11,color=INK,bold=False):c.setFillColor(color);c.setFont("Helvetica-Bold" if bold else "Helvetica",size);c.drawString(x,y,s)
def image(path,x,y,w,h):
 p=Path(path)
 if not p.is_file():raise FileNotFoundError(p)
 im=ImageReader(str(p));iw,ih=im.getSize();scale=min(w/iw,h/ih);c.drawImage(im,x+(w-iw*scale)/2,y+(h-ih*scale)/2,width=iw*scale,height=ih*scale,mask="auto")
def card(x,y,w,h):c.setFillColor(PALE);c.roundRect(x,y,w,h,9,fill=1,stroke=0)
def footer(n):
 c.setStrokeColor(HexColor("#d9dedc"));c.line(38,37,W-38,37);text(38,22,"Minecraft 1.20.1 | Proposed design and actual runtime evidence",8,MUTED);c.setFont("Helvetica",8);c.setFillColor(MUTED);c.drawRightString(W-38,22,f"{n:02d} / {len(rows)+3:02d}")
text(38,612,"CHIPPED / FOCUSED IMPLEMENTATION REVIEW",10,TEAL,True);text(38,562,"Chipped in SIU",28,INK,True);text(38,532,"Compare the approved design with captured in-game windows.",12,MUTED);text(38,505,"See validation.json for the tested interactions and coverage limits.",10,MUTED);footer(1);c.showPage()
for n,row in enumerate(rows,2):
 text(38,612,row["loader"].upper()+" / MINECRAFT 1.20.1 / BUILD "+row["runtime_build"],9,TEAL,True);text(38,584,row["label"],21,INK,True);text(38,556,"APPROVED DESIGN PROPOSAL",9,MUTED,True);text(510,556,"ACTUAL SIU RUNTIME CAPTURE",9,TEAL,True);card(38,105,452,432);card(510,105,452,432);image(row["proposal"],53,120,422,402);image(row["runtime_crop"],525,120,422,402);footer(n);c.showPage()
gallery=data["gallery"]
for page in range(2):
 text(38,612,"FABRIC / MINECRAFT 1.20.1 / BUILD 10",9,TEAL,True)
 text(38,584,"Seven stations, one shared workshop",21,INK,True)
 text(38,559,"Actual SIU runtime captures; each station retains its own title and recipe family.",10,MUTED)
 for j,row in enumerate(gallery[page*4:(page+1)*4]):
  x=38+(j%2)*472;y=305-(j//2)*235
  text(x,y+205,row["station"],11,INK,True);card(x,y,452,190);image(row["runtime_crop"],x+8,y+8,436,174)
 footer(len(rows)+2+page);c.showPage()
c.save();print("saved",a.output)
