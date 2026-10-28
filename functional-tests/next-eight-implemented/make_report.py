"""Create a screenshot report from a manually curated JSON evidence list.

Each entry needs label, loader, screenshot and crop. Only actual SIU runtime
captures may be included. The caller must visually verify each screenshot.
"""
import argparse, json
from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import landscape, A4
from reportlab.pdfbase.pdfmetrics import stringWidth

p=argparse.ArgumentParser();p.add_argument("manifest",type=Path);p.add_argument("output",type=Path);a=p.parse_args()
evidence=json.loads(a.manifest.read_text()); pages=evidence["captures"]
a.output.parent.mkdir(parents=True,exist_ok=True)
c=canvas.Canvas(str(a.output),pagesize=landscape(A4));W,H=landscape(A4)
def footer(n):
 c.setStrokeColor(HexColor("#dbe6e5"));c.line(34,32,W-34,32)
 c.setFont("Helvetica",8);c.setFillColor(HexColor("#627c84"));c.drawString(34,20,"Minecraft 1.20.1 | actual disposable-world runtime screenshots")
 c.drawRightString(W-34,20,f"{n:02d} / {len(pages)+1:02d}")
c.setFillColor(HexColor("#0a7978"));c.setFont("Helvetica-Bold",10);c.drawString(36,H-48,"PROJECT EVERYTHING / IMPLEMENTED UI")
c.setFillColor(HexColor("#18353e"));c.setFont("Helvetica-Bold",28);c.drawString(36,H-92,"Eight integrations in game")
c.setFont("Helvetica",12);c.drawString(36,H-122,"Actual runtime captures from focused disposable-world checks.")
c.drawString(36,H-144,"Each page identifies its loader and the state that was captured.")
c.drawString(36,H-165,"The gravestone editor remains a native sign-style screen and is excluded from window pages.")
c.setFont("Helvetica",10);c.drawString(36,H-195,"See validation.json for focused checks, version limits, and Forge QA mapping details.")
footer(1);c.showPage()
for n,entry in enumerate(pages,2):
 path=Path(entry["crop"])
 if not path.is_file(): raise FileNotFoundError(path)
 c.setFillColor(HexColor("#0a7978"));c.setFont("Helvetica-Bold",10);c.drawString(34,H-35,entry["loader"].upper()+" / MINECRAFT 1.20.1")
 c.setFillColor(HexColor("#18353e"));c.setFont("Helvetica-Bold",18);c.drawString(34,H-60,entry["label"])
 img=ImageReader(str(path));iw,ih=img.getSize();scale=min((W-68)/iw,(H-125)/ih)
 c.drawImage(img,(W-iw*scale)/2,48+(H-125-ih*scale)/2,width=iw*scale,height=ih*scale,mask="auto")
 footer(n);c.showPage()
c.save()
