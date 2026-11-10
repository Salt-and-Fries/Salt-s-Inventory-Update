"""Pair original publisher UI captures with real implemented SIU captures."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader
import json,hashlib,math

ROOT=Path(__file__).resolve().parents[1]
DESIGN=ROOT/'output/extended-ae-window-design'
IMPL=ROOT/'output/extended-ae-implementation'
OUT=ROOT/'output/extended-ae-comparisons'
PDF=ROOT/'output/pdf/siu-extended-ae-implemented-comparisons.pdf'
W,H=1200,850
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
read=lambda p:json.loads(p.read_text())

def window(r,expected):
 return next(w for w in r['allWindows']if w.get('nativeScreen','').split('.')[-1]==expected)

def bounds_union(rects):
 return (min(b[0]for b in rects),min(b[1]for b in rects),max(b[2]for b in rects),max(b[3]for b in rects))

def crop_box(state,b,widgets,slots):
 """Include the complete native frame, external toolbars and every visible tray."""
 x,y,w,h=b['x'],b['y'],b['width'],b['height'];rects=[(x-4,y-4,x+w+4,y+h+4)]
 for a in widgets:
  if not a.get('visible',True):continue
  xx,yy,ww,hh=(a.get(k,0)for k in ['x','y','width','height'])
  if ww<=0 or hh<=0 or xx<-100 or yy<-100:continue
  rects.append((xx-4,yy-4,xx+ww+4,yy+hh+4))
 for s in slots:
  if s.get('player')or s.get('x',-9999)<-100 or s.get('y',-9999)<-100:continue
  xx=state.get('nativeBounds',b)['x']+s['x'];yy=state.get('nativeBounds',b)['y']+s['y']
  # The panel rim extends seven GUI pixels around its cells.
  if xx<x or yy<y or xx+16>x+w or yy+16>y+h:rects.append((xx-8,yy-8,xx+24,yy+24))
 q=bounds_union(rects);return tuple(round(v*2)for v in q)

def crop_source(source,box,dest):
 im=Image.open(source).convert('RGB');box=(max(0,box[0]),max(0,box[1]),min(im.width,box[2]),min(im.height,box[3]));assert box[2]>box[0]and box[3]>box[1]
 im.crop(box).save(dest);return box

def main():
 for d in [OUT/'original',OUT/'implemented',OUT/'review',PDF.parent]:d.mkdir(parents=True,exist_ok=True)
 design=read(DESIGN/'manifest.json')['pages'];records={}
 for file in IMPL.glob('*-extended-ae-*.json'):
  if not any(v in file.name for v in ['impl-06','impl-09','impl-10','plain-10']):continue
  for r in read(file):records[(file.stem,r['id'])]=r
 sources={r['id']:r for f in ['prior-captures.json','forge-block-captures.json','forge-extra-captures.json','forge-optional-extra-captures.json']for r in read(DESIGN/f)}
 def choose(p):
  ident=p['id'].removeprefix('forge-');loader='forge';desired=ident;screen=p['screen'].split('.')[-1]
  if ident.startswith('ex-crafting-terminal-'):desired=ident.replace('ex-crafting-terminal-','extended-terminal-')
  aliases={'own-eae-set-amount':'precise-amount-editor','ex-emc-interface':'emc-interface','universal-wireless-ex-pat':'wireless-ex-pat'}
  desired=aliases.get(ident,desired)
  if ident=='own-eae-set-amount':loader='fabric'
  if ident=='pattern-modifier-multiply':loader='fabric'
  if ident=='pattern-modifier-replace':desired='replace-functional'
  if ident=='pattern-modifier-clone':desired='clone-functional'
  candidates=[(k,r)for k,r in records.items()if k[0].startswith(loader+'-')and k[1]==desired and any(w.get('nativeScreen','').split('.')[-1]==screen for w in r.get('allWindows',[]))]
  assert candidates,(ident,screen,desired)
  key,r=max(candidates,key=lambda kr:(int(kr[0][0].split('-')[-1]),'plain'in kr[0][0]))
  return key,r,window(r,screen)
 pages=[]
 for p in design:
  key,r,w=choose(p);sid=p['id'];original=ROOT/p['original'];native=sources[sid]
  if sid=='forge-pattern-modifier-multiply':
   native=sources['own-eae-pattern-modifier'];n=native['native'];box=crop_box(n,n['bounds'],n.get('widgets',[]),n.get('slots',[]));original=OUT/'original'/(sid+'.png');crop_source(ROOT/n['capture'],box,original)
  else:
   # Reuse only the original, untouched left-hand reference crop.
   import shutil
   dst=OUT/'original'/(sid+'.png');shutil.copy2(original,dst);original=dst
  image=ROOT/r['capture'];assert sha(image)==r['sha256']
  box=crop_box(w,w['bounds'],w.get('nativeWidgets',[]),w.get('slots',[]));right=OUT/'implemented'/(sid+'.png');box=crop_source(image,box,right)
  notes=[]
  if sid.startswith('forge-pattern-modifier-'):notes.append('Complete mode controls and slot borders are retained.')
  if 'crafting-terminal-'in sid or sid=='forge-wireless-ex-ct':notes.append('Mode tabs remain outside the narrowed body; native side trays stay attached.')
  if sid=='forge-assembler-matrix-populated':notes.append('The pattern row is populated in a real, formed Assembler Matrix.')
  if sid=='forge-ex-pattern-access-part':notes.append('The list retains its native provider/search workflow; contents depend on the connected network.')
  if not notes:notes.append('Native work area retained; the embedded player inventory moves to SIU\'s separate inventory window.')
  pages.append(dict(id=sid,title=p['title'],group=p['group'],function=p['function'],note=' '.join(notes),
   original=str(original.relative_to(ROOT)),implemented=str(right.relative_to(ROOT)),native_loader=native.get('loader',p['loader']),siu_loader=r['loader'],
   native_source=native['native']['capture'],siu_source=r['capture'],native_sha256=sha(ROOT/native['native']['capture']),siu_sha256=r['sha256'],crop=list(box),implementation_run=key[0],screen=p['screen']))
 # Four real with-SIU captures, rather than repeated native-reference images.
 inspectors=read(DESIGN/'pattern-captures-fabric.json')
 for n in inspectors:
  kind=n['id'].removeprefix('eae-pattern-');path=ROOT/f'build/remaining-ui-audit/gameplay/1.20.1-fabric-siu-extended-ae-impl-09/screenshots/inspector-{kind}.png';assert path.is_file()
  raw=n['native'];b=raw['bounds'];box=crop_box(raw,b,raw.get('widgets',[]),raw.get('slots',[]));right=OUT/'implemented'/(n['id']+'.png');crop_source(path,box,right)
  original=DESIGN/'original'/(n['id']+'.png')
  pages.append(dict(id=n['id'],title=kind.title()+' Pattern Inspector',group='Appendix / native inspectors',function='Read-only pattern inspection; this interface has no item-transfer workflow.',note='SIU preserves the original full-screen inspector. The right image is a real with-SIU capture.',
   original=str(original.relative_to(ROOT)),implemented=str(right.relative_to(ROOT)),native_loader='fabric',siu_loader='fabric',native_source=n['capture'],siu_source=str(path.relative_to(ROOT)),native_sha256=sha(ROOT/n['capture']),siu_sha256=sha(path),crop=list(box),implementation_run='fabric-extended-ae-impl-09',screen=raw['screen']))
 assert len(pages)==44
 for name,file in [('UI','segoeui.ttf'),('UIB','segoeuib.ttf')]:pdfmetrics.registerFont(TTFont(name,'C:/Windows/Fonts/'+file))
 c=canvas.Canvas(str(PDF),pagesize=(W,H),pageCompression=1);c.setTitle('Extended AE - Original UI and Implemented SIU Windows');c.setAuthor('Salts Inventory Update');boxes=[]
 def text(s,x,y,size=12,bold=False,color='#203940'):
  c.setFont('UIB'if bold else'UI',size);c.setFillColor(HexColor(color));c.drawString(x,y,s);assert x+pdfmetrics.stringWidth(s,'UIB'if bold else'UI',size)<=W-28,(s,size)
 def para(s,x,y,width,size=12,leading=18):
  words=s.split();line='';lines=[]
  for word in words:
   candidate=(line+' '+word).strip()
   if pdfmetrics.stringWidth(candidate,'UI',size)>width and line:lines.append(line);line=word
   else:line=candidate
  if line:lines.append(line)
  for line in lines:text(line,x,y,size);y-=leading
  return y
 def footer(number,label):
  c.setStrokeColor(HexColor('#D4E2E4'));c.line(34,49,W-34,49);text(label,34,29,9,color='#65777D');text(str(number),W-56,29,10,color='#65777D');c.showPage()
 def frame(x,y,w,h):c.setFillColor(HexColor('#EEF4F5'));c.roundRect(x,y,w,h,8,fill=1,stroke=0)
 def picture(im,x,y,w,h,scale):
  dw,dh=im.width*scale,im.height*scale;assert dw<=w and dh<=h
  # No retouching, border replacement or authored UI pixels.
  c.drawImage(ImageReader(im),x+(w-dw)/2,y+(h-dh)/2,dw,dh)
 text('PROJECT EVERYTHING / IMPLEMENTED WINDOWS',34,815,10,True,'#247C80');text('Extended AE',34,758,34,True);text('Original UI and SIU window comparisons',34,719,22)
 text('40 window views',34,656,23,True);text('31 inventory/configuration screen types',34,624,15);text('+ 4 native inspectors in the appendix',34,596,15)
 para('Minecraft 1.20.1 / Fabric 1.0.2 and Forge 1.4.18. Each comparison places the original in-game UI on the left and an actual implemented SIU capture on the right.',34,545,490,14,21)
 para('Views are presented at the same pixel scale within each pair, so removing the embedded inventory does not make the remaining controls appear artificially larger. Empty and populated states can differ between captures.',34,431,490,13,20)
 para('The saved design review defines the complete catalog. Native references and implementation captures include conditional AE2WTLib and AppliedE interfaces. Capture passes 06, 09 and 10 document unchanged layouts and final fixes; they are identified on each page.',34,315,490,12,18)
 hero=Image.open(ROOT/'build/remaining-ui-audit/gameplay/1.20.1-forge-siu-extended-ae-impl-10/screenshots/crafting-with-output.png').convert('RGB');frame(564,228,602,449);picture(hero,574,238,582,429,min(582/hero.width,429/hero.height));text('Actual game screen: terminal and separate player inventory',574,202,11,True)
 text('Original captures. Implemented windows. No new mockups.',34,98,14,True)
 footer(1,'1.20.1 comparison catalog | Screen and source-image provenance retained in the companion manifest')
 for i,p in enumerate(pages,2):
  c.bookmarkPage(p['id']);c.addOutlineEntry(p['title'],p['id'],level=0)
  text('EXTENDED AE / '+p['group'].upper(),34,815,10,True,'#247C80')
  size=26
  while pdfmetrics.stringWidth(p['title'],'UIB',size)>1120:size-=1
  text(p['title'],34,770,size,True)
  text('Minecraft 1.20.1 | '+('Forge 1.4.18'if p['native_loader']=='forge'else'Fabric 1.0.2'),34,742,11,color='#65777D')
  text('ORIGINAL UI / SIU ABSENT',34,707,11,True,'#247C80');text('IMPLEMENTED UI / SIU INSTALLED',614,707,11,True,'#247C80')
  left=Image.open(ROOT/p['original']).convert('RGB');right=Image.open(ROOT/p['implemented']).convert('RGB');scale=min(1.05,520/max(left.width,right.width),480/max(left.height,right.height))
  for x,im in [(34,left),(614,right)]:frame(x,184,552,508);picture(im,x+12,196,528,484,scale)
  text('WHAT THIS INTERFACE DOES',34,156,9,True,'#247C80');yy=para(p['function'],34,135,1118,11,16);assert yy>=84,(p['id'],yy)
  text(p['note'],34,70,10,color='#546D74')
  p['page']=i;p['display_scale']=scale
  boxes.append(dict(page=i,id=p['id'],left=left.size,right=right.size,scale=scale))
  footer(i,'Original: '+p['native_loader'].title()+' | SIU: '+p['siu_loader'].title()+' | Capture: '+p['implementation_run']+' | '+p['screen'].split('.')[-1])
 c.save();reader=PdfReader(PDF);assert len(reader.pages)==45
 for p in pages:
  t=reader.pages[p['page']-1].extract_text();assert p['title']in t and 'IMPLEMENTED UI / SIU INSTALLED'in t
  page=reader.pages[p['page']-1]
  assert 1<=len(page.images)<=2
  assert sum(op==b'Do'for _,op in page.get_contents().operations)==2
 (OUT/'manifest.json').write_text(json.dumps(dict(pdf=str(PDF.relative_to(ROOT)),pages=45,window_comparisons=40,native_inspectors=4,screen_types=31,images_are_in_game=True,pairs=pages,pdf_sha256=sha(PDF),visual_review='pending'),indent=2)+'\n')
 (OUT/'layout.json').write_text(json.dumps(boxes,indent=2)+'\n')
 print(json.dumps(dict(pdf=str(PDF),pages=len(reader.pages),window_comparisons=40,native_inspectors=4,bytes=PDF.stat().st_size),indent=2))
if __name__=='__main__':main()
