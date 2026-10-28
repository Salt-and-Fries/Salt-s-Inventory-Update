"""Create the 45-combination visual gallery from original gameplay PNGs."""
from pathlib import Path
import hashlib,json,math,shutil
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import landscape,A4
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/third-batch-all-versions-visual-fixes';PDF=ROOT/'output/pdf/siu-third-batch-all-versions-visual-fixes.pdf'
EIGHT=ROOT/'build/project-everything-eight-implemented/gameplay';CHIP=ROOT/'build/project-everything-chipped-implemented/gameplay';PORT=ROOT/'build/third-batch-ports/gameplay';SWORDS=ROOT/'build/simply-swords-implemented/gameplay'
TARGETS=json.loads((ROOT/'build/third-batch-ports/availability.json').read_text(encoding='utf-8'))['targets']
FAMILY={'extendeddrawersaddon':'Extended Drawers Addon','the-graveyard-fabric':'The Graveyard','the-graveyard-forge':'The Graveyard','betterend':'BetterEnd','runes':'Runes','deep-aether':'Deep Aether','tieredz':'TieredZ','smitherz':'SmitherZ','additionz':'AdditionZ','chipped':'Chipped','simply-swords':'Simply Swords'}
# Later 1.21.1 Chipped/gear and legacy 1.20.1 Chipped/gear must come from fresh fix captures.
FRESH_120='visual-fix01';FRESH_121='visual-fix01'

def pick(mc,loader,family):
 if mc=='1.20.1':
  if family=='extendeddrawersaddon':return EIGHT/f'{mc}-fabric-{FRESH_120}','fresh-drawer-single',None
  if family=='tieredz':return EIGHT/f'{mc}-fabric-{FRESH_120}','fresh-gear-anvil-second',None
  if family=='smitherz':return EIGHT/f'{mc}-fabric-{FRESH_120}','fresh-gear-smith-second',None
  if family=='chipped':return CHIP/f'{mc}-{loader}-{FRESH_120}','fresh-chipped-selected',None
  if family=='simply-swords':return SWORDS/('1.20.1-fabric-simply-fabric-02' if loader=='fabric' else '1.20.1-forge-simply-forge-final03'),('fabric02-configured' if loader=='fabric' else 'forge03-configured'),None
  if family=='betterend':return EIGHT/'1.20.1-fabric-eight-integration-03','fabric-end-smelter','4.0.11'
  if family=='additionz':return EIGHT/'1.20.1-fabric-eight-integration-03','fabric-fletching',None
  if family=='the-graveyard-fabric':return EIGHT/'1.20.1-fabric-eight-variants-01','fabric-ossuary','4.0.1+mc1.20.1-fabric'
  if family=='the-graveyard-forge':return EIGHT/'1.20.1-forge-eight-integration-03','forge-ossuary','3.1'
  if family=='runes':return EIGHT/('1.20.1-fabric-eight-variants-01' if loader=='fabric' else '1.20.1-forge-eight-integration-03'),('fabric-rune-altar' if loader=='fabric' else 'forge-rune-result'),'1.3.2'
  if family=='deep-aether':return EIGHT/'1.20.1-forge-eight-integration-03','forge-combiner',None
 if mc=='1.21.1':
  if family=='extendeddrawersaddon':return PORT/f'{mc}-fabric-{FRESH_121}','fresh-drawer-single',None
  if family=='tieredz':return PORT/f'{mc}-fabric-{FRESH_121}','fresh-gear-anvil-second',None
  if family=='smitherz':return PORT/f'{mc}-fabric-{FRESH_121}','fresh-gear-smith-second',None
  if family=='chipped':return PORT/f'{mc}-{loader}-{FRESH_121}','fresh-chipped-selected',None
  if family=='additionz':return PORT/f'1.21.1-fabric-{FRESH_121}','fresh-fletching',None
  if family=='betterend':return PORT/'1.21.1-fabric-visual-fix-native','fabric-end-smelter-open',None
  if family=='deep-aether':return PORT/'1.21.1-neoforge-visual-fix-combiner','neoforge-combiner-open',None
  if family=='simply-swords':return PORT/('1.21.1-fabric-port12' if loader=='fabric' else '1.21.1-neoforge-port02'),('fabric-runic-forge-open' if loader=='fabric' else 'neoforge-runic-forge-configured'),None
  if family.startswith('the-graveyard'):return PORT/('1.21.1-fabric-visual-fix-native' if loader=='fabric' else f'1.21.1-neoforge-{FRESH_121}'),('fabric-ossuary-selected' if loader=='fabric' else 'fresh-ossuary'),None
  if family=='runes':return PORT/f'1.21.1-{loader}-{FRESH_121}','fresh-rune-altar',None
 run='26.3-fabric-port03' if (mc,loader,family)==('26.3','fabric','betterend') else f'{mc}-{loader}-'+('port02' if mc=='26.3' else 'port01')
 stem={'betterend':f'{loader}-end-smelter-open','runes':f'{loader}-rune-ready'}.get(family,f'{loader}-ossuary-selected')
 if (mc,loader,family)==('26.3','neoforge','runes'):stem='neoforge-runes-ready'
 return PORT/run,stem,None

def prepare():
 (OUT/'screenshots').mkdir(parents=True,exist_ok=True);(OUT/'crops').mkdir(exist_ok=True);PDF.parent.mkdir(parents=True,exist_ok=True)
 entries=[]
 for target in TARGETS:
  mc=target['minecraft'];loader=target['loader']
  for release in target['integrations']:
   family=release['project'];folder,stem,override=pick(mc,loader,family);source=folder/'screenshots'/f'{stem}.png';obs_path=folder/'observations.json'
   if not source.exists() or not obs_path.exists():raise FileNotFoundError(f'{mc}/{loader}/{family}: {source}')
   data=json.loads(obs_path.read_text(encoding='utf-8'));obs=next((r for r in reversed(data) if r.get('command',{}).get('capture')==stem),None)
   if not obs:raise ValueError(f'No screenshot observation for {source}')
   client=obs['client'];wins=client.get('allWindows') or client.get('windows') or []
   if not wins:raise ValueError(f'No window in {source}')
   b=wins[-1]['bounds'];img=Image.open(source);sx=img.width/client['guiWidth'];sy=img.height/client['guiHeight'];top=28 if family in ('tieredz','smitherz') else 6
   rect=(max(0,int((b['x']-6)*sx)),max(0,int((b['y']-top)*sy)),min(img.width,int((b['x']+b['width']+6)*sx)),min(img.height,int((b['y']+b['height']+6)*sy)))
   key=f'{mc}-{loader}-{FAMILY[family].lower().replace(" ","-")}'
   original=OUT/'screenshots'/f'{key}.png';crop=OUT/'crops'/f'{key}.png';shutil.copyfile(source,original);img.crop(rect).save(crop)
   override_ids={('1.20.1','fabric','betterend'):'7QwyTILr',('1.20.1','fabric','the-graveyard-fabric'):'EOb8Is0J',('1.20.1','fabric','runes'):'C0i3Ewtv',('1.20.1','forge','the-graveyard-forge'):'Q66WyQ3F',('1.20.1','forge','runes'):'j6Djy4m6'}
   release_id=release['version_id'] if override is None else override_ids.get((mc,loader,family))
   stage=folder/'staged-artifacts.json';forge_map=folder/'forge-remap-lineage.json'
   entries.append({'minecraft':mc,'loader':loader,'family':family,'title':FAMILY[family],'upstream_version':override or release['version'],'source_release_id':release_id,'source_gameplay':str(folder.resolve()),'source_observation':str(obs_path.resolve()),'source_staged_artifacts':str(stage.resolve()) if stage.exists() else None,'source_forge_remap_lineage':str(forge_map.resolve()) if forge_map.exists() else None,'source_screenshot':str(source.resolve()),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'crop_pixels':rect,'original':str(original.resolve()),'crop':str(crop.resolve())})
 if len(entries)!=45:raise ValueError('Expected 45 actual screenshot records, got '+str(len(entries)))
 (OUT/'screenshot-manifest.json').write_text(json.dumps({'scope':'Actual screenshots; unchanged-image reuse and fresh changed controls are documented per row','captures':entries},indent=2),encoding='utf-8')
 return entries

def render(entries):
 W,H=landscape(A4);c=canvas.Canvas(str(PDF),pagesize=(W,H));order=list(dict.fromkeys(x['title'] for x in entries))
 for title in order:
  group=[x for x in entries if x['title']==title]
  for start in range(0,len(group),6):
   subset=group[start:start+6];c.setFillColorRGB(.12,.13,.16);c.rect(0,0,W,H,fill=1,stroke=0)
   c.setFillColorRGB(.95,.93,.89);c.setFont('Helvetica-Bold',22);c.drawString(35,H-43,title)
   c.setFont('Helvetica',10);c.setFillColorRGB(.73,.75,.77);c.drawString(36,H-62,'Actual in-game windows  |  Minecraft version / loader / upstream release')
   cols=min(3,len(subset));rows=math.ceil(len(subset)/cols);gap=10;pw=(W-70-gap*(cols-1))/cols;ph=(H-96-gap*(rows-1))/rows
   for i,e in enumerate(subset):
    x=35+(i%cols)*(pw+gap);y=H-78-ph-(i//cols)*(ph+gap);c.setFillColorRGB(.2,.21,.24);c.roundRect(x,y,pw,ph,7,fill=1,stroke=0)
    im=ImageReader(e['crop']);iw,ih=im.getSize();scale=min((pw-16)/iw,(ph-40)/ih);dw,dh=iw*scale,ih*scale;c.drawImage(im,x+(pw-dw)/2,y+28+(ph-40-dh)/2,dw,dh,mask='auto')
    c.setFillColorRGB(.94,.94,.9);c.setFont('Helvetica-Bold',9);c.drawString(x+8,y+15,e['minecraft']+'  /  '+e['loader'].title())
    c.setFont('Helvetica',7);c.setFillColorRGB(.75,.77,.78);c.drawString(x+8,y+5,e['upstream_version'][:55])
   c.showPage()
 # Close-ups are pixel-preserving crops of actual normal-state captures.
 controls=[e for e in entries if e['family']=='chipped'];(OUT/'closeups').mkdir(exist_ok=True)
 c.setFillColorRGB(.12,.13,.16);c.rect(0,0,W,H,fill=1,stroke=0);c.setFillColorRGB(.95,.93,.89);c.setFont('Helvetica-Bold',22);c.drawString(35,H-43,'Chipped | complete controls')
 c.setFillColorRGB(.73,.75,.77);c.setFont('Helvetica',10);c.drawString(36,H-62,'Actual Craft and all four view buttons, cropped from their original screenshots.')
 closeups=[]
 for i,e in enumerate(controls):
  folder=Path(e['source_gameplay']);name='fresh-chipped-craft-normal';src=folder/'screenshots'/f'{name}.png';obs=json.loads((folder/'observations.json').read_text(encoding='utf-8'));r=next(x for x in reversed(obs) if x.get('command',{}).get('capture')==name);client=r['client'];b=(client.get('allWindows') or client['windows'])[-1]['bounds'];im=Image.open(src);sx=im.width/client['guiWidth'];sy=im.height/client['guiHeight'];rect=(int((b['x']+8)*sx),int((b['y']+125)*sy),int((b['x']+84)*sx),int((b['y']+167)*sy));dst=OUT/'closeups'/f"{e['minecraft']}-{e['loader']}-controls.png";im.crop(rect).save(dst)
  closeups.append({'minecraft':e['minecraft'],'loader':e['loader'],'source':str(src.resolve()),'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'crop_pixels':rect,'crop':str(dst.resolve())})
  pw=(W-82)/2;ph=(H-110)/2;x=35+(i%2)*(pw+12);y=H-78-ph-(i//2)*(ph+12);c.setFillColorRGB(.2,.21,.24);c.roundRect(x,y,pw,ph,7,fill=1,stroke=0);obj=ImageReader(str(dst));iw,ih=obj.getSize();scale=min((pw-25)/iw,(ph-45)/ih);dw,dh=iw*scale,ih*scale;c.drawImage(obj,x+(pw-dw)/2,y+30+(ph-45-dh)/2,dw,dh,mask='auto');c.setFillColorRGB(.94,.94,.9);c.setFont('Helvetica-Bold',10);c.drawString(x+10,y+10,e['minecraft']+'  /  '+e['loader'].title())
 c.showPage();(OUT/'closeup-manifest.json').write_text(json.dumps({'captures':closeups},indent=2),encoding='utf-8')
 c.save();print('PDF:',PDF,'screenshots:',len(entries),'closeups:',len(closeups))

if __name__=='__main__':render(prepare())
