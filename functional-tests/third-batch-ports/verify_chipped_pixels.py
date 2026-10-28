"""Compare unmodified screenshot pixels with the installed Chipped sprites."""
from pathlib import Path
import json,zipfile,io
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'output/third-batch-all-versions-visual-fixes';OUT.mkdir(parents=True,exist_ok=True)
MODES=['single_block_button','horizontal_blocks_button','vertical_blocks_button','two_by_two_button']
proof=[]
for mc,loader in [('1.20.1','fabric'),('1.20.1','forge'),('1.21.1','fabric'),('1.21.1','neoforge')]:
 if mc=='1.20.1':
  run=ROOT/'build/project-everything-chipped-implemented/gameplay'/f'{mc}-{loader}-visual-fix01';fixture=ROOT/f'functional-tests/chipped-implemented/fixture-{loader}.json'
 else:
  run=ROOT/'build/third-batch-ports/gameplay'/f'{mc}-{loader}-visual-fix01';fixture=ROOT/('build/third-batch-ports/fixture-1.21.1-fabric-core-cohort.json' if loader=='fabric' else 'build/third-batch-ports/fixture-1.21.1-neoforge-no-aether.json')
 assets=json.loads(fixture.read_text(encoding='utf-8'))['artifacts'];jar=next(x for x in assets if x['filename'].lower().startswith('chipped-'));zipper=zipfile.ZipFile(jar['path']);obs=json.loads((run/'observations.json').read_text(encoding='utf-8'));entry=next(x for x in obs if x.get('command',{}).get('capture')=='fresh-chipped-craft-normal');client=entry['client'];window=(client.get('allWindows') or client['windows'])[-1]['bounds'];screen=Image.open(run/'screenshots/fresh-chipped-craft-normal.png');sx=screen.width/client['guiWidth'];sy=screen.height/client['guiHeight']
 if sx!=int(sx) or sy!=int(sy):raise ValueError('Expected integer GUI scale')
 row={'minecraft':mc,'loader':loader,'source_jar':jar['path'],'source_sha512':jar['sha512'],'gui_scale':[sx,sy],'craft':[],'modes':[]}
 def compare(image,asset,x0,y0,w,h,v=0,border=False,ignore_tooltip=False):
  good=total=0;unmatched=[]
  for y in range(h):
   for x in range(w):
    if border and x not in (0,w-1) and y not in (0,h-1):continue
    if ignore_tooltip and x==17 and y<8:continue
    r=asset.getpixel((x,y+v))
    if r[3]!=255:continue
    total+=1;actual=image.getpixel((int((window['x']+x0+x)*sx),int((window['y']+y0+y)*sy)))
    if actual==r[:3]:good+=1
    else:unmatched.append([x,y,list(r[:3]),list(actual)])
  return {'matching':good,'compared':total,'unmatched':unmatched}
 for hover in [False,True]:
  filename='button'+('_highlighted' if hover and mc!='1.20.1' else '')+'.png';asset=Image.open(io.BytesIO(zipper.read('assets/chipped/textures/gui/sprites/'+filename))).convert('RGBA');image=Image.open(run/'screenshots'/('fresh-chipped-craft-hover.png' if hover else 'fresh-chipped-craft-normal.png')).convert('RGB');r=compare(image,asset,10,127,72,18,18 if hover and mc=='1.20.1' else 0,True);r['hover']=hover;row['craft'].append(r)
 for i,name in enumerate(MODES):
  for hover in [False,True]:
   filename=name+('_highlighted' if hover and mc!='1.20.1' else '')+'.png';asset=Image.open(io.BytesIO(zipper.read('assets/chipped/textures/gui/sprites/'+filename))).convert('RGBA');image=Image.open(run/'screenshots'/f'fresh-chipped-mode-{i}{"-hover" if hover else ""}.png').convert('RGB');r=compare(image,asset,10+18*i,147,18,18,18 if hover and mc=='1.20.1' else 0,False,hover);r.update({'mode':name,'hover':hover,'tooltip_pixels_excluded':8 if hover else 0});row['modes'].append(r)
 proof.append(row)
bad=[(r['minecraft'],r['loader'],p) for r in proof for p in r['craft']+r['modes'] if p['matching']!=p['compared']]
(OUT/'chipped-pixel-proof.json').write_text(json.dumps({'scope':'Opaque Craft perimeter and full mode icon pixels; native tooltip overlay excluded','proof':proof,'mismatches':bad},indent=2),encoding='utf-8')
for r in proof:print(r['minecraft'],r['loader'],'craft',[(p['matching'],p['compared']) for p in r['craft']],'modes',[(p['matching'],p['compared']) for p in r['modes']])
if bad:raise AssertionError('Sprite pixel mismatch; see chipped-pixel-proof.json')
