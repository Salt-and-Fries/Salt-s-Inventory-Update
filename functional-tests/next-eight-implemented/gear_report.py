"""Preserve actual gear screenshots, crop their complete windows, and create a PDF."""
import argparse,hashlib,json,shutil
from pathlib import Path
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import landscape,A4
from reportlab.lib.utils import ImageReader
p=argparse.ArgumentParser();p.add_argument("--run",required=True);a=p.parse_args()
root=Path("build/project-everything-eight-implemented/gameplay")/f"1.20.1-fabric-{a.run}"
out=Path("output/project-everything-gear-visual-fixes");(out/"screenshots").mkdir(parents=True,exist_ok=True);(out/"crops").mkdir(parents=True,exist_ok=True)
entries=[("gear-anvil-first","Repair and Name - selected"),("gear-anvil-second","Reforge Gear - selected"),("gear-smith-first","Upgrade Gear - selected"),("gear-smith-equipped","Upgrade Gear - equipped preview"),("gear-smith-second","Link Gear - selected"),("gear-grind-first","Repair and Disenchant - selected"),("gear-grind-second","Unlink Gear - selected")]
obs=json.loads((root/"observations.json").read_text());captures=[]
for name,label in entries:
 rows=[r for r in obs if r.get("command",{}).get("capture")==name]
 if not rows:raise ValueError("Missing runtime capture "+name)
 row=rows[-1];cl=row["client"];ws=cl["allWindows"]
 if len(ws)!=1:raise ValueError("Expected one window for "+name)
 b=ws[0]["bounds"];source=root/"screenshots"/(name+".png");img=Image.open(source);sx=img.width/cl["guiWidth"];sy=img.height/cl["guiHeight"]
 rect=(max(0,int((b["x"]-10)*sx)),max(0,int((b["y"]-34)*sy)),min(img.width,int((b["x"]+b["width"]+10)*sx)),min(img.height,int((b["y"]+b["height"]+10)*sy)))
 target=out/"screenshots"/(name+".png");crop=out/"crops"/(name+".png");shutil.copyfile(source,target);img.crop(rect).save(crop)
 captures.append({"label":label,"loader":"Fabric","screenshot":str(target.resolve()),"crop":str(crop.resolve()),"crop_pixels":rect,"source_sha256":hashlib.sha256(source.read_bytes()).hexdigest(),"observation_sequence":row["sequence"]})
(out/"screenshot-manifest.json").write_text(json.dumps({"scope":"Fresh 1.20.1 in-game SIU screenshots; crops retain actual pixels", "captures":captures},indent=2))
W,H=landscape(A4);dest=Path("output/pdf/siu-gear-window-visual-fixes-1.20.1.pdf");dest.parent.mkdir(parents=True,exist_ok=True);c=canvas.Canvas(str(dest),pagesize=(W,H))
def footer(n):
 c.setStrokeColor(HexColor("#dbe6e5"));c.line(34,32,W-34,32);c.setFont("Helvetica",8);c.setFillColor(HexColor("#627c84"));c.drawString(34,20,"Minecraft 1.20.1 / Fabric / actual in-game screenshots");c.drawRightString(W-34,20,f"{n:02d} / {len(captures)+1:02d}")
c.setFillColor(HexColor("#0a7978"));c.setFont("Helvetica-Bold",10);c.drawString(36,H-48,"FOCUSED VISUAL REVIEW / GEAR WINDOWS")
c.setFillColor(HexColor("#18353e"));c.setFont("Helvetica-Bold",28);c.drawString(36,H-92,"Gear windows in game")
c.setFont("Helvetica",12);c.drawString(36,H-123,"Fresh screenshots of the six tabbed screens and the equipped preview.")
c.drawString(36,H-148,"Selected tabs, artwork, and the armor stand are shown in their actual runtime state.")
c.setFont("Helvetica",10);c.drawString(36,H-181,"See validation.json for focused checks and their limits.");footer(1);c.showPage()
for n,e in enumerate(captures,2):
 c.setFillColor(HexColor("#0a7978"));c.setFont("Helvetica-Bold",10);c.drawString(34,H-35,"FABRIC / MINECRAFT 1.20.1")
 c.setFillColor(HexColor("#18353e"));c.setFont("Helvetica-Bold",18);c.drawString(34,H-60,e["label"])
 im=ImageReader(e["crop"]);iw,ih=im.getSize();scale=min((W-68)/iw,(H-125)/ih)
 c.drawImage(im,(W-iw*scale)/2,48+(H-125-ih*scale)/2,width=iw*scale,height=ih*scale,mask="auto");footer(n);c.showPage()
c.save();print("Saved",dest)
