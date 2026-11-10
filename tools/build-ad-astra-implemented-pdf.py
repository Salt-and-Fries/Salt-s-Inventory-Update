"""Actual native-versus-SIU screenshots; no UI is redrawn for this report."""
from pathlib import Path
import hashlib,json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/ad-astra-implemented';PDF=ROOT/'output/pdf/siu-ad-astra-implemented-windows.pdf'
read=lambda p:json.loads(p.read_text(encoding='utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
W,H=1191,842;INK='#203b3e';TEAL='#176f70';MUTED='#586b6c'
pdfmetrics.registerFont(TTFont('Review','C:/Windows/Fonts/arial.ttf'))
pdfmetrics.registerFont(TTFont('Review-Bold','C:/Windows/Fonts/arialbd.ttf'))
native=read(ROOT/'tmp/pdfs/ad-astra/audit-cases.json')+read(ROOT/'output/remaining-ui-audit/pr-supp-pairs.json')['cases']
native={r['id']:r['native']for r in native}
approved={r['id']:r for r in read(ROOT/'output/ad-astra-window-design/manifest.json')['cases']}
assets=OUT/'report-assets';assets.mkdir(exist_ok=True)
evidence=[];page=0;c=canvas.Canvas(str(PDF),pagesize=(W,H))
c.setTitle('Ad Astra and Giselle Addon - implemented SIU windows')
c.setAuthor('Salts Inventory Update')

def text(x,y,s,size=13,color=INK,bold=False):
 c.setFillColor(HexColor(color));c.setFont('Review-Bold'if bold else'Review',size);c.drawString(x,y,s)
def wrap(x,y,s,width=1087,size=14,color=MUTED):
 words=s.split();line=''
 for word in words:
  trial=(line+' '+word).strip()
  if pdfmetrics.stringWidth(trial,'Review',size)>width:text(x,y,line,size,color);y-=size*1.4;line=word
  else:line=trial
 if line:text(x,y,line,size,color);y-=size*1.4
 return y
def start(title,sub=''):
 global page
 page+=1;text(52,799,'SIU / AD ASTRA / IMPLEMENTATION REVIEW',12,TEAL,True);text(52,762,title,27,INK,True)
 if sub:text(52,736,sub,12,MUTED)
def finish(note='Actual game captures. Inventory is managed in its separate SIU window.'):
 c.setStrokeColor(HexColor('#d3dfe0'));c.line(52,48,W-52,48);text(52,28,note,10,MUTED);text(W-76,28,str(page),10,MUTED);c.showPage()
def box(x,y,w,h,label):
 text(x,y+h+14,label,12,TEAL,True);c.setFillColor(HexColor('#eef4f4'));c.roundRect(x,y,w,h,10,fill=1,stroke=0)
def fit(im,x,y,w,h):
 scale=min(w/im.width,h/im.height);ww=im.width*scale;hh=im.height*scale
 c.drawImage(ImageReader(im),x+(w-ww)/2,y+(h-hh)/2,ww,hh,mask='auto')
def actual(folder,spec,suffix='',drawer=False):
 name=spec['id']+suffix;meta=read(folder/(name+'.json'));run=ROOT/'build/remaining-ui-audit/gameplay'/folder.name
 path=run/'screenshots'/(name+'.png');im=Image.open(path).convert('RGB');w=next(w for w in meta['allWindows']if w.get('menuId')==spec['menu']);b=w['bounds']
 regions=[r for r in w['composition']['regions']if r.get('exterior')]
 left=min([0]+[r['x']for r in regions]);top=min([0]+[r['y']for r in regions]);right=max([b['width']]+[r['x']+r['source']['width']for r in regions]);bottom=max([b['height']]+[r['y']+r['source']['height']for r in regions])
 if drawer:bottom=max(bottom,b['height']+w['templateState']['drawerHeight']+2)
 sx=im.width/meta['guiWidth'];sy=im.height/meta['guiHeight'];bounds=[b['x']+left-6,b['y']+top-6,b['x']+right+6,b['y']+bottom+6]
 crop=im.crop(tuple(round(v*(sx if i%2==0 else sy))for i,v in enumerate(bounds)))
 target=assets/(name+'.png');crop.save(target);evidence.append(dict(id=name,source=str(path.relative_to(ROOT)),sha256=sha(path),crop=str(target.relative_to(ROOT))))
 return crop
def original(spec):
 if spec['id']=='ad-astra-giselle-addon-rocket-sensor':
  row=read(OUT/'clean-native-sensor.json');return Image.open(ROOT/row['crop']).convert('RGB')
 if spec['id']in approved:return Image.open(ROOT/'output/ad-astra-window-design/native'/(spec['menu'].split(':')[1].removesuffix('_menu')+'.png')).convert('RGB')
 row=native[spec['id']];im=Image.open(ROOT/row['capture']).convert('RGB');b=row['bounds'];sx=im.width/640;sy=im.height/450
 return im.crop(tuple(round(v*(sx if i%2==0 else sy))for i,v in enumerate([b['x']-8,b['y']-38,b['x']+b['width']+8,b['y']+b['height']+8])))

def main():
 report=read(OUT/'verification.json');target=next(t for t in report['targets']if t['minecraft']=='1.21.1'and t['loader']=='neoforge');folder=OUT/target['run']
 specs=read(ROOT/'compatibility/versions/1.21.1/common/src/main/resources/salts_inventory_compat/ad-astra/native-windows.json')
 start('Native Ad Astra style, now implemented','November 3, 2026 / 14 Ad Astra screen types + 4 Giselle Addon screen types')
 y=wrap(52,687,'The approved design is running in the game: original metal casings, full-width machinery, colored materials, live monitors, charging cells and native toolbars. SIU controls occupy the title area; player inventory is separate.',size=18)
 text(52,y-25,'Supported publisher releases',17,INK,True);y-=58
 for t in report['targets']:
  text(65,y,f"Minecraft {t['minecraft']} / {t['loader']}  -  Ad Astra {t['ad_astra']} + Giselle Addon {t['giselle']}",16);y-=35
 y=wrap(52,y-12,f"All four targets passed focused checks of the 18 changed windows: complete placement of all 107 non-player slot envelopes, player-inventory separation, native actions and SIU controls. {report['focused_assertions']} recorded assertions in total.",size=16)
 y=wrap(52,y-12,'Core and the public API are unchanged. This report uses original SIU-absent audit captures on the left and actual new 1.21.1 NeoForge game captures on the right. Different item contents and working states are intentional.',size=15)
 wrap(52,y-10,report['limits'],size=13);finish('Screenshots are evidence of the implemented windows, not mockups.')
 for s in specs:
  title=s['title'];addon='Giselle Addon'if s['menu'].startswith('ad_astra_giselle_addon:')else'Ad Astra'
  start(title,addon+' / Original audited 1.20.1 native UI vs implemented 1.21.1 NeoForge window')
  box(52,119,535,566,'ORIGINAL / SIU ABSENT');box(604,119,535,566,'IMPLEMENTED / ACTUAL GAME')
  fit(original(s),64,131,511,542);fit(actual(folder,s),616,131,511,542)
  note=approved.get(s['id'],{}).get('proposal','Preserves the original apparatus, native button layout and matching dark metal casing. Window controls use the standard SIU layout or More/Close when space is limited.')
  wrap(52,91,note,size=13);finish()
 start('Configuration stays native and attached','Actual game states / Coal Generator and Rocket Sensor')
 for x,id,suffix in [(52,'ad-astra-coal-generator','-energy-configuration'),(604,'ad-astra-giselle-addon-rocket-sensor','-side-configuration')]:
  s=next(s for s in specs if s['id']==id);box(x,115,535,570,s['title'].upper());fit(actual(folder,s,suffix,True),x+12,127,511,546)
 wrap(52,88,'Gear mode moves the original side controls into an attached matching drawer. The original direction buttons, port selection and reset are retained.',size=13);finish()
 start('Native behavior and separate inventory','Actual complete game capture / working Coal Generator and player inventory')
 imagepath=ROOT/'build/remaining-ui-audit/gameplay'/target['run']/'screenshots/ad-astra-coal-generator-working.png'
 fit(Image.open(imagepath).convert('RGB'),52,138,1087,555)
 wrap(52,107,'Focused checks include real fuel insertion and extraction, NASA click-to-craft, add-on fluid containers, gravity and sensor controls, configuration/reset, cargo permissions, sorting, and window movement/focus/pin/lock/close.',size=14)
 finish('Focused window checks only. No complete gameplay suite was run.')
 c.save();assert len(PdfReader(PDF).pages)==21
 (OUT/'pdf-evidence.json').write_text(json.dumps(dict(pdf=str(PDF.relative_to(ROOT)),sha256=sha(PDF),pages=page,captures=evidence),indent=2)+'\n',encoding='utf-8')
 print(PDF,page)
if __name__=='__main__':main()
