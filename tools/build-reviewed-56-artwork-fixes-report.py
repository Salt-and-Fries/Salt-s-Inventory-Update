"""Five affected windows only; before/fixed images are actual captured pixels."""
from pathlib import Path
import hashlib,json
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/project-everything-56-artwork-fixes'
RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-reviewed-artwork01'
PDF=ROOT/'output/pdf/siu-meadow-vinery-artwork-fixes.pdf'
CASES=[
 ('meadow-cooking-cauldron','Meadow / Cooking Cauldron','Complete upper cauldron graphic, output outline and heater artwork.'),
 ('meadow-fondue','Meadow / Fondue','Complete pot silhouette, heater base and oversized output-slot border.'),
 ('meadow-woodcutter','Meadow / Woodcutter','Original tan output-slot artwork retained, with no standard gray slot overlay.'),
 ('nethervinery-crimson-fermentation-barrel','NetherVinery / Crimson Aging Barrel','Full left barrel outline and top cap retained.'),
 ('nethervinery-warped-fermentation-barrel','NetherVinery / Warped Aging Barrel','Full left barrel outline and top cap retained.'),
]

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def crop(row,name):
 image=ROOT/row['capture'];assert sha(image)==row['sha256']
 state=row.get('state') or row['observation']['client']
 windows=[w for w in state['allWindows'] if 'NativeTemplateWindow' in w.get('definition','')]
 assert len(windows)==1,(name,windows)
 b=windows[0]['bounds'];im=Image.open(image).convert('RGB')
 sx,sy=im.width/state['guiWidth'],im.height/state['guiHeight']
 rect=[max(0,int((b['x']-2)*sx)),max(0,int((b['y']-2)*sy)),
       min(im.width,int((b['x']+b['width']+2)*sx)),min(im.height,int((b['y']+b['height']+2)*sy))]
 target=OUT/(name+'.png');im.crop(rect).save(target)
 return {'capture':str(image.relative_to(ROOT)),'sha256':sha(image),'crop_rect':rect,'crop':str(target.relative_to(ROOT))}

def text(c,s,x,y,size=11,bold=False,color='#243c42'):
 c.setFont('Helvetica-Bold' if bold else 'Helvetica',size);c.setFillColor(HexColor(color));c.drawString(x,y,s)
def picture(c,p,box):
 im=Image.open(ROOT/p);x,y,w,h=box;scale=min(w/im.width,h/im.height)
 c.drawImage(ImageReader(im),x+(w-im.width*scale)/2,y+(h-im.height*scale)/2,im.width*scale,im.height*scale,mask='auto')

def main():
 OUT.mkdir(parents=True,exist_ok=True)
 old={r['id']:r for r in json.loads((ROOT/'output/project-everything-56-implementation/implementation-evidence.json').read_text())['cases']}
 new={r['id']:r for r in json.loads((RUN/'audit-core-cases.json').read_text())}
 result=[]
 for ident,title,note in CASES:
  r=new[ident];assert not r.get('error'),r
  result.append({'id':ident,'title':title,'note':note,'before':crop(old[ident]['core'],ident+'-before'),
                 'fixed':crop(r,ident+'-fixed')})
 c=canvas.Canvas(str(PDF),pagesize=(840,594),pageCompression=1)
 c.setTitle('SIU - Meadow and NetherVinery artwork fixes - 1.20.1')
 for n,r in enumerate(result,1):
  text(c,'SIU / FOCUSED ARTWORK CORRECTIONS',32,560,9,True,'#177b80')
  text(c,r['title'],32,524,24,True)
  text(c,'Minecraft 1.20.1 / Fabric production screenshots / Core 0.1.6 + Compatibility 0.1.3',32,500,10)
  for x,key,label in ((32,'before','BEFORE FIX'),(438,'fixed','FIXED / ACTUAL IN-GAME WINDOW')):
   text(c,label,x,464,10,True,'#177b80');c.setFillColor(HexColor('#eef4f3'));c.roundRect(x,142,370,306,8,fill=1,stroke=0)
   picture(c,r[key]['crop'],(x+8,152,354,286))
  text(c,r['note'],34,113,12)
  text(c,'Shared 1.20.1 Fabric/Forge compatibility profiles updated. Focused visual and slot checks only.',34,87,9,color='#64767c')
  c.setStrokeColor(HexColor('#cbd7d8'));c.line(32,40,808,40)
  text(c,'Only the five affected UIs are included. Original screenshots preserved; no mockup artwork.',32,23,8,color='#64767c')
  text(c,str(n)+' / 5',780,23,8,color='#64767c');c.showPage()
 c.save()
 manifest={'pdf':str(PDF.relative_to(ROOT)),'pdf_sha256':sha(PDF),'pages':len(result),'cases':result,
           'checks':'7 focused profile tests passed; Fabric and Forge compatibility builds succeeded; see focused gameplay record.'}
 (OUT/'evidence.json').write_text(json.dumps(manifest,indent=2)+'\n')
 print(json.dumps({'pdf':str(PDF),'pages':len(result)}))

if __name__=='__main__':main()
