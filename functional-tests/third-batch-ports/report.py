"""Produce a visual report from actual focused gameplay captures only."""
from pathlib import Path
import hashlib, json, shutil
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import landscape, A4
from reportlab.lib.utils import ImageReader

ROOT=Path(__file__).resolve().parents[2]
WORK=ROOT/'build/third-batch-ports/gameplay'
OUT=ROOT/'output/third-batch-all-targets'
PDF=ROOT/'output/pdf/siu-third-batch-all-targets-ui.pdf'
OUT.mkdir(parents=True,exist_ok=True); (OUT/'crops').mkdir(exist_ok=True); (OUT/'originals').mkdir(exist_ok=True);PDF.parent.mkdir(parents=True,exist_ok=True)

SELECTED=[
 ('1.21.1-fabric-port08','fabric-single-drawer-open','1.21.1 Fabric | Drawer'),
 ('1.21.1-fabric-port08','fabric-anvil-tabs-open','1.21.1 Fabric | Repair and name'),
 ('1.21.1-fabric-port08','fabric-link-ready','1.21.1 Fabric | Link Gear, ready'),
 ('1.21.1-fabric-port07','fabric-unlink-selected','1.21.1 Fabric | Unlink Gear'),
 ('1.21.1-fabric-port11','fabric-fletching-ready','1.21.1 Fabric | Fletching result'),
 ('1.21.1-fabric-port08','fabric-chunk-loader-open','1.21.1 Fabric | Chunk Loader'),
 ('1.21.1-neoforge-port02','neoforge-chipped-selected','1.21.1 NeoForge | Chipped, source selected'),
 ('1.21.1-neoforge-port02','neoforge-chipped-picker','1.21.1 NeoForge | Chipped, picker'),
 ('1.21.1-neoforge-port02','neoforge-runic-forge-configured','1.21.1 NeoForge | Runic Forge, configured'),
 ('1.21.1-neoforge-port05','neoforge-combiner-open','1.21.1 NeoForge | Combiner'),
 ('1.21.1-neoforge-port05','neoforge-combiner-wait','1.21.1 NeoForge | Combiner result'),
 ('26.3-neoforge-port02','neoforge-ossuary-bone','26.3 NeoForge | Ossuary recipes'),
 ('26.3-neoforge-port02','neoforge-urn-returned','26.3 NeoForge | Urn, item returned'),
 ('26.3-neoforge-port02','neoforge-runes-ready','26.3 NeoForge | Rune crafting'),
 ('26.3-fabric-port03','fabric-smelter-burning','26.3 Fabric | End Stone Smelter'),
 ('26.3-fabric-port02','fabric-end-furnace-open','26.3 Fabric | End Stone Furnace'),
 ('26.2-fabric-port01','fabric-smelter-lit','26.2 Fabric | Smelter burning'),
 ('1.21.1-fabric-port09','fabric-smelter-book','1.21.1 Fabric | Smelter recipe book'),
 ('1.21.11-fabric-port01','fabric-ossuary-selected','1.21.11 Fabric | Ossuary'),
 ('1.21.11-neoforge-port01','neoforge-ossuary-selected','1.21.11 NeoForge | Ossuary'),
 ('26.1.2-fabric-port01','fabric-rune-ready','26.1.2 Fabric | Rune crafting'),
 ('26.1.2-neoforge-port01','neoforge-rune-ready','26.1.2 NeoForge | Rune crafting'),
 ('26.2-fabric-port01','fabric-ossuary-selected','26.2 Fabric | Ossuary'),
 ('26.2-neoforge-port01','neoforge-ossuary-selected','26.2 NeoForge | Ossuary'),
]

manifest=[]
for run,name,label in SELECTED:
 folder=WORK/run; source=folder/'screenshots'/f'{name}.png'; observations=folder/'observations.json'
 if not source.exists() or not observations.exists(): continue
 records=json.loads(observations.read_text(encoding='utf-8'))
 matches=[r for r in records if r.get('command',{}).get('capture')==name and r.get('client',{}).get('windows')]
 if not matches: continue
 obs=matches[-1];client=obs['client']; b=client['windows'][-1]['bounds'];img=Image.open(source)
 sx=img.width/client['guiWidth'];sy=img.height/client['guiHeight']
 top=26 if any(x in name for x in ('tabs','link-ready','unlink-selected')) else 4
 rect=(max(0,int((b['x']-6)*sx)),max(0,int((b['y']-top-4)*sy)),min(img.width,int((b['x']+b['width']+6)*sx)),min(img.height,int((b['y']+b['height']+6)*sy)))
 if 'chipped-picker' in name: rect=(max(0,int((b['x']-6)*sx)),max(0,int((b['y']-4)*sy)),min(img.width,int((b['x']+b['width']+6)*sx)),min(img.height,int((b['y']+b['height']+6)*sy)))
 if name=='fabric-smelter-book': rect=(max(0,int((b['x']-153)*sx)),max(0,int((b['y']-6)*sy)),min(img.width,int((b['x']+b['width']+6)*sx)),min(img.height,int((b['y']+166)*sy)))
 ident=run+'--'+name;crop=OUT/'crops'/f'{ident}.png';copy=OUT/'originals'/f'{ident}.png'
 img.crop(rect).save(crop);shutil.copyfile(source,copy)
 manifest.append({'label':label,'run':run,'original':str(copy),'crop':str(crop),'pixel_crop':rect,'original_sha256':hashlib.sha256(source.read_bytes()).hexdigest()})

results=[]
for f in sorted(WORK.glob('*/opening-observations.json')):
 rows=json.loads(f.read_text(encoding='utf-8'))
 results.append({'run':f.parent.name,'evidence_directory':str(f.parent.resolve()),
                 'observations_file':str(f.resolve()),
                 'staged_artifacts_file':str((f.parent/'staged-artifacts.json').resolve()) if (f.parent/'staged-artifacts.json').exists() else None,
                 'observations':rows})
(OUT/'screenshot-manifest.json').write_text(json.dumps({'captures':manifest},indent=2),encoding='utf-8')
manual={'1.21.1-fabric-port08':['link-ready.json','link-operated.json','link-taken.json','tab-and-source-checks.json'],
 '1.21.1-fabric-port11':['fletching-ready.json','fletching-taken.json'],
 '1.21.1-fabric-port09':['smelter-book.json'], '1.21.1-neoforge-port02':['chipped-select.json','chipped-craft.json','runic-configured.json','runic-commit.json'],
 '1.21.1-neoforge-port05':['combiner-ready.json','combiner-wait.json','combiner-taken.json'],
 '26.3-neoforge-port02':['ossuary-operation.json','ossuary-background.json','runes-operation.json','storage-operation.json'],
 '26.2-fabric-port01':['smelter-produced.json','smelter-taken.json']}
manual={run:[str((WORK/run/name).resolve()) for name in names if (WORK/run/name).exists()] for run,names in manual.items()}
(OUT/'validation.json').write_text(json.dumps({'scope':'Focused runtime checks, not a full gameplay suite','runs':results,'manual_operation_evidence':manual,'superseded':['1.21.1-fabric-port08 fletching used automatic fallback; corrected and retested in port11.','1.21.1-fabric-port07 logged missing QA Kotlin resources; Chipped, Runic Forge and Runes were retested with corrected resources in port12.'],'limitations':['1.21.1 Fabric uses a synthetic Loom-remapped nested-library QA bundle; original mod mixins retained. BetterEnd and Graveyard cohorts each use documented named-runtime selector corrections.','1.21.1 NeoForge Deep Aether QA remaps two named-runtime Accessories lambda selectors; all mixins retained.','Targets without runtime observations are not claimed as gameplay tested.']},indent=2),encoding='utf-8')

W,H=landscape(A4); c=canvas.Canvas(str(PDF),pagesize=(W,H))
def header(title,sub):
 c.setFillColorRGB(.12,.13,.16);c.rect(0,0,W,H,fill=1,stroke=0)
 c.setFillColorRGB(.94,.92,.87);c.setFont('Helvetica-Bold',22);c.drawString(35,H-43,title)
 c.setFillColorRGB(.73,.75,.77);c.setFont('Helvetica',10);c.drawString(36,H-62,sub)
def panel(entry,x,y,w,h):
 c.setFillColorRGB(.19,.20,.23);c.roundRect(x,y,w,h,8,fill=1,stroke=0)
 img=ImageReader(entry['crop']); iw,ih=img.getSize();factor=min((w-20)/iw,(h-36)/ih);dw,dh=iw*factor,ih*factor
 c.drawImage(img,x+(w-dw)/2,y+25+(h-36-dh)/2,dw,dh,mask='auto')
 c.setFillColorRGB(.92,.91,.87);c.setFont('Helvetica',9);c.drawString(x+10,y+9,entry['label'])
groups=[('Drawers and gear','Actual in-game windows; external tabs retain their joined edges.',manifest[:4]),
 ('Native controls','Fletching and chunk loading retain their native controls.',manifest[4:6]),
 ('Chipped and Runic Forge','Real gameplay captures; chosen inventory material and configured forge.',manifest[6:9]),
 ('Processing and storage','Actual gameplay captures; focused operations recorded alongside them.',manifest[9:14]),
 ('BetterEnd','Actual Fabric windows; 26.x uses the revised compact crop.',manifest[14:18]),
 ('Additional target examples','Actual selected-recipe windows on each additional version and loader.',manifest[18:24])]
for title,sub,items in groups:
 if not items:continue
 header(title,sub);cols=2;rows=(len(items)+1)//2;gap=12;pw=(W-82)/2;ph=(H-100-gap*(rows-1))/rows
 for i,e in enumerate(items):panel(e,35+(i%2)*(pw+gap),H-80-ph-(i//2)*(ph+gap),pw,ph)
 c.showPage()
header('Coverage and limits','Focused checks only; these screenshots do not certify every modpack or configuration.')
c.setFillColorRGB(.91,.91,.88);c.setFont('Helvetica',11)
lines=['1.21.1 Fabric: drawers, gear tabs and a component-based gem link were exercised.',
 '1.21.1 NeoForge: Chipped inventory conversion, Runic Forge preview/commit and Combiner processing.',
 '26.3 NeoForge: Ossuary selection/craft and background sync, Runes craft, urn pickup/return and source close.',
 'Other listed target runs are recorded in validation.json with their exact observed operations.',
 'Fabric 1.21.1 QA remaps nested libraries and selected BCLib/VillagersTradingPlus selectors; mixins remain enabled.',
 'NeoForge 1.21.1 Aether QA maps two Accessories lambda selectors; all mixins remain enabled.',
 'Original screenshots, crops, source hashes and per-run evidence accompany this PDF.']
for i,line in enumerate(lines):c.drawString(42,H-105-i*29,line)
c.showPage()
header('Runtime target matrix','Window counts are observed openings; action details are recorded in validation.json.')
c.setFillColorRGB(.85,.86,.86);c.setFont('Helvetica-Bold',10)
c.drawString(42,H-94,'Minecraft');c.drawString(170,H-94,'Loader');c.drawString(290,H-94,'Observed distinct windows');c.drawString(520,H-94,'Selected operation evidence')
for i,(mc,loader) in enumerate((v,l) for v in ('1.21.1','1.21.11','26.1.2','26.2','26.3') for l in ('fabric','neoforge')):
 rows=[r for run in results if run['run'].startswith(mc+'-'+loader+'-') for r in run['observations'] if r.get('status','').startswith('opened')]
 names={r.get('case') for r in rows}; actions=[r['case'] for r in rows if r.get('operation')]
 y=H-120-i*35;c.setFillColorRGB(.93,.93,.9);c.setFont('Helvetica',10)
 c.drawString(42,y,mc);c.drawString(170,y,loader);c.drawString(290,y,str(len(names)) if names else 'Not observed')
 c.setFont('Helvetica',9);c.drawString(520,y,', '.join(sorted(set(actions))) or 'See manual evidence')
c.showPage();c.save();print('PDF',PDF,'screenshots',len(manifest),'observed runs',len(results))
