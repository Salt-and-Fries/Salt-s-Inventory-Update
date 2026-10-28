"""Create Chipped visual proposals from native captures and SIU frame sprites.

These images are design compositions, not live SIU screenshots or implementation.
"""
from pathlib import Path
from io import BytesIO
import json,zipfile,hashlib,shutil
from PIL import Image,ImageDraw
root=Path('build/project-everything-chipped-design/gameplay');out=Path('output/project-everything-chipped-design');(out/'native-full').mkdir(parents=True,exist_ok=True);(out/'native-crops').mkdir(exist_ok=True);(out/'proposals').mkdir(exist_ok=True)
lookup={}
for loader in ['fabric','forge']:
 folder=root/(loader+'-chipped-reference-01');obs=json.loads((folder/'observations.json').read_text())
 for row in obs:
  n=row.get('command',{}).get('capture');path=folder/'screenshots'/(str(n)+'.png')
  if n and path.is_file():lookup[(loader,n)]=(path,row['client'],row['sequence'])
def region(loader,name,rect):
 path,state,_=lookup[(loader,name)];im=Image.open(path).convert('RGBA');b=state['bounds'];sx=im.width/state['guiWidth'];sy=im.height/state['guiHeight'];box=(round((b['x']+rect[0])*sx),round((b['y']+rect[1])*sy),round((b['x']+rect[2])*sx),round((b['y']+rect[3])*sy));return im.crop(box).resize((rect[2]-rect[0],rect[3]-rect[1]),Image.Resampling.NEAREST)
def keep(loader,name):
 path,state,seq=lookup[(loader,name)];dest=out/'native-full'/(loader+'-'+name+'.png');shutil.copyfile(path,dest);crop=region(loader,name,(0,0,256,256));crop.resize((768,768),Image.Resampling.NEAREST).save(out/'native-crops'/(loader+'-'+name+'.png'));return {'loader':loader,'name':name,'full':str(dest.resolve()),'crop':str((out/'native-crops'/(loader+'-'+name+'.png')).resolve()),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'sequence':seq,'siu_loaded':state['siu_loaded'],'title':state.get('title')}
stations=['mason-table','carpenters-table','loom-table','glassblower','botanist-workbench','alchemy-bench','tinkering-table']
native=[]
for s in stations:
 for state in ['empty-clean','material-clean']:native.append(keep('fabric',s+'-'+state))
for n in ['mason-search-brick','mason-search-no-results','mason-row6-upper','mason-row6-lower','mason-row6-other-upper','tinkering-preview-mode-1','tinkering-preview-mode-2','tinkering-preview-mode-3','tinkering-preview-mode-4','alchemy-bench-amethyst']:native.append(keep('fabric',n))
for n in ['forge-mason-empty','forge-mason-clear','forge-mason-scrolled-clear','forge-mason-search-clear','forge-mason-crafted']:native.append(keep('forge',n))
G=Path('artwork/shared-resources/core/assets/salts_inventory_update/textures/gui');FRAME=Image.open(G/'window.png').convert('RGBA');CONTROLS=Image.open(G/'window_controls.png').convert('RGBA')
with zipfile.ZipFile(Path.home()/'.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar') as z:ASCII=Image.open(BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
def glyph(ch):
 n=ord(ch) if ord(ch)<128 else 63;g=ASCII.crop(((n%16)*8,(n//16)*8,(n%16)*8+8,(n//16)*8+8));b=g.getbbox();return g,3 if ch==' ' else (b[2] if b else 5)
def mctext(im,s,x,y,color=(52,39,25,255)):
 for ch in s:
  g,w=glyph(ch);ink=Image.new('RGBA',(8,8),color);ink.putalpha(g.getchannel('A'));im.alpha_composite(ink,(int(x),int(y)));x+=w+1

def frame(w,h):
 im=Image.new('RGBA',(w,h));pieces=[((0,0,5,5),(0,0,5,5)),((6,0,11,5),(w-5,0,w,5)),((0,6,5,11),(0,h-5,5,h)),((6,6,11,11),(w-5,h-5,w,h)),((5,0,6,5),(5,0,w-5,5)),((5,6,6,11),(5,h-5,w-5,h)),((0,5,5,6),(0,5,5,h-5)),((6,5,11,6),(w-5,5,w,h-5)),((5,5,6,6),(5,5,w-5,h-5))]
 for src,dst in pieces:im.alpha_composite(FRAME.crop(src).resize((dst[2]-dst[0],dst[3]-dst[1]),Image.Resampling.NEAREST),dst[:2])
 # Proposed palette adaptation of existing SIU chrome to the native Chipped workshop.
 colors={(198,198,198):(193,157,104),(255,255,255):(247,221,177),(85,85,85):(104,72,41),(0,0,0):(38,30,26)}
 px=im.load()
 for y in range(h):
  for x in range(w):
   r,g,b,a=px[x,y];px[x,y]=(*colors.get((r,g,b),(r,g,b)),a)
 return im
button=Image.open(out/'native-assets/button.png').convert('RGBA').crop((0,0,72,18))
def mini_button(w):
 left=button.crop((0,0,3,18));mid=button.crop((3,0,69,18));right=button.crop((69,0,72,18));z=Image.new('RGBA',(w,18));z.alpha_composite(left,(0,0));z.alpha_composite(mid.resize((w-6,18),Image.Resampling.NEAREST),(3,0));z.alpha_composite(right,(w-3,0));return z

def draw(state,base='forge-mason-clear',loader='forge'):
 w,h=270,181;im=frame(w,h);d=ImageDraw.Draw(im);mctext(im,'Tinkering Table' if state=='preview-vertical' else 'Mason Table',7,5)
 for col,dx in [(3,w-27),(2,w-14)]:im.alpha_composite(CONTROLS.crop((col*11,0,col*11+11,11)),(dx,3))
 # Material is a read-only reference, not a Minecraft menu slot.
 d.rectangle((7,22,262,41),fill=(103,78,48,255),outline=(75,52,30,255));d.rectangle((8,23,261,40),outline=(218,184,128,255))
 if state=='empty':mctext(im,'No material selected',12,28,(243,226,199,255))
 else:
  icon=region('fabric','tinkering-preview-mode-3',(85,166,103,184)) if state=='preview-vertical' else region('forge','forge-mason-clear',(85,166,103,184));im.alpha_composite(icon,(10,23));mctext(im,'Iron Bars  x32' if state=='preview-vertical' else 'Stone  x32',32,28,(255,236,200,255))
 im.alpha_composite(mini_button(47),(213,23));mctext(im,'Choose',218,28)
 # Preserve actual native pixels for preview, Craft and all four mode buttons.
 src='forge-mason-empty' if state=='empty' else base;src_loader='forge' if state=='empty' else loader
 d.rectangle((6,45,85,124),fill=(103,78,48,255))
 im.alpha_composite(region(src_loader,src,(9,26,81,98)),(10,49))
 im.alpha_composite(region(src_loader,src,(9,101,81,119)),(10,127));im.alpha_composite(region(src_loader,src,(9,121,81,139)),(10,147))
 im.alpha_composite(region(src_loader,src,(85,26,247,40)),(91,48))
 grid=region(src_loader,src,(85,41,247,149))
 if state in ('empty','no-results'):grid.alpha_composite(grid.crop((0,0,18,18)),(36,72))
 if state=='preview-vertical':grid.alpha_composite(grid.crop((0,54,18,72)),(36,72))
 im.alpha_composite(grid,(91,64))
 # Optional 5-pixel scrollbar is a clearly labeled design addition.
 d.rectangle((255,64,259,171),fill=(92,65,38,255));d.rectangle((256,65 if state!='scrolled' else 144,258,88 if state!='scrolled' else 168),fill=(219,191,145,255))
 if state=='picker':
  pop=frame(143,62);mctext(pop,'Choose material',7,5);ImageDraw.Draw(pop).rectangle((6,20,136,54),fill=(94,69,43,255));pop.alpha_composite(icon,(9,23));mctext(pop,'Stone x32',32,28,(255,235,202,255));mctext(pop,'Player inventory',10,44,(223,199,165,255));im.alpha_composite(pop,(111,43))
 return im
states=[('empty','forge-mason-empty','forge'),('populated','forge-mason-clear','forge'),('picker','forge-mason-clear','forge'),('search','forge-mason-search-clear','forge'),('no-results','mason-search-no-results','fabric'),('scrolled','forge-mason-scrolled-clear','forge'),('preview-vertical','tinkering-preview-mode-3','fabric')]
proposals=[]
for state,base,loader in states:
 im=draw(state,base,loader);path=out/'proposals'/(state+'.png');im.resize((im.width*3,im.height*3),Image.Resampling.NEAREST).save(path);proposals.append({'state':state,'file':str(path.resolve()),'type':'composed visual proposal, not implemented SIU','base_native_capture':loader+':'+base,'geometry_gui':[270,181]})
(out/'design-manifest.json').write_text(json.dumps({'minecraft':'1.20.1','kind':'native screenshots and visual proposals; no SIU implementation','native_captures':native,'proposals':proposals,'constraints':['Material picker references existing player inventory; no owned input/output slot','Nine columns, six visible rows; matching render and hit bounds','Scrollbar is proposed, native uses wheel','No station tabs; each source block supplies the title and filter']},indent=2))
print('native captures',len(native),'proposals',len(proposals))
