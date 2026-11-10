"""Focused actual before/fixed screenshots for the second UI correction pass."""
from pathlib import Path
import importlib.util,json
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('artwork_report',ROOT/'tools/build-reviewed-56-artwork-fixes-report.py')
helpers=importlib.util.module_from_spec(spec);spec.loader.exec_module(helpers)
OUT=ROOT/'output/project-everything-56-dark-layout-fixes';helpers.OUT=OUT
RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-reviewed-dark-layout03'
PDF=ROOT/'output/pdf/siu-barrel-dark-windows-xp-fixes.pdf'
CASES=[
 ('vinery-fermentation-barrel','Vinery / Fermentation Barrel','All four bottle slots retain their original positions underneath the pipes.'),
 ('ad-astra-giselle-addon-automation-nasa-workbench','Giselle Addon / Automation NASA Workbench','Window face matches the original texture exactly: #4D4A4E / RGB 77, 74, 78.'),
 ('ad-astra-giselle-addon-fuel-loader','Giselle Addon / Fuel Loader','Matching native background and bevel palette, with a readable title.'),
 ('ad-astra-giselle-addon-gravity-normalizer','Giselle Addon / Gravity Normalizer','Matching native window palette and complete lower energy-status text.'),
 ('ad-astra-giselle-addon-rocket-sensor','Giselle Addon / Rocket Sensor','The shared dark-window correction also applies to Rocket Sensor.'),
 ('item-archon-experience-pouch','Archon / Experience Pouch','Complete rounded panel outline and capacity-box border, with bottom padding.'),
]

def main():
 OUT.mkdir(parents=True,exist_ok=True)
 old={r['id']:r for r in json.loads((ROOT/'output/project-everything-56-implementation/implementation-evidence.json').read_text())['cases']}
 new={r['id']:r for r in json.loads((RUN/'audit-core-cases.json').read_text())}
 rows=[]
 c=canvas.Canvas(str(PDF),pagesize=(840,594),pageCompression=1);c.setTitle('SIU - Barrel, native dark windows and XP panel corrections')
 for n,(ident,title,note) in enumerate(CASES,1):
  r=new[ident];assert not r.get('error'),r
  before=helpers.crop(old[ident]['core'],ident+'-before');fixed=helpers.crop(r,ident+'-fixed')
  rows.append({'id':ident,'before':before,'fixed':fixed})
  helpers.text(c,'SIU / FOCUSED UI CORRECTIONS',32,560,9,True,'#177b80')
  helpers.text(c,title,32,524,22,True)
  helpers.text(c,'Minecraft 1.20.1 / Fabric production screenshots / Core 0.1.6 + Compatibility 0.1.3',32,500,10)
  for x,image,label in ((32,before,'BEFORE FIX'),(438,fixed,'FIXED / ACTUAL IN-GAME WINDOW')):
   helpers.text(c,label,x,464,10,True,'#177b80');c.setFillColor(HexColor('#eef4f3'));c.roundRect(x,142,370,306,8,fill=1,stroke=0)
   helpers.picture(c,image['crop'],(x+8,152,354,286))
  helpers.text(c,note,34,113,11)
  helpers.text(c,'Shared 1.20.1 Fabric/Forge compatibility changes. Focused checks; no complete functional suite.',34,87,9,color='#64767c')
  c.setStrokeColor(HexColor('#cbd7d8'));c.line(32,40,808,40)
  helpers.text(c,'Only affected windows shown. Original screenshot pixels; no mockup artwork.',32,23,8,color='#64767c')
  helpers.text(c,f'{n} / {len(CASES)}',780,23,8,color='#64767c');c.showPage()
 c.save();(OUT/'evidence.json').write_text(json.dumps({'pdf':str(PDF.relative_to(ROOT)),'sha256':helpers.sha(PDF),'pages':len(rows),'cases':rows},indent=2)+'\n')
 print(json.dumps({'pdf':str(PDF),'pages':len(rows)}))

if __name__=='__main__':main()
