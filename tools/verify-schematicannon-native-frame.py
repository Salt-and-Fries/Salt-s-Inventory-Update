"""Publish actual Schematicannon captures and verify its retained native pixels."""
from pathlib import Path
from io import BytesIO
from zipfile import ZipFile
import argparse,hashlib,json,shutil
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,data):p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')

def run(runtime):
    dest=ROOT/'output/schematicannon-native-frame';dest.mkdir(parents=True,exist_ok=True)
    reference=ROOT/'output/create-native-comparison-review/pairs/schematicannon'
    original=read(reference/'original-evidence.json')['client'];native=original['bounds']
    assert original['siu_loaded'] is False
    fixed=read(runtime/'features/native-frame-final.json')['client'];window=fixed['allWindows'][0]['bounds']
    assert fixed['siu_loaded'] is True and (window['width'],window['height'])==(213,143)
    scale=Image.open(runtime/'screenshots/native-frame-final.png').width//fixed['guiWidth'];assert scale==2
    a=Image.open(reference/'original-full.png').convert('RGB').crop((native['x']*scale,(native['y']-2)*scale,(native['x']+213)*scale,(native['y']+141)*scale))
    b=Image.open(runtime/'screenshots/native-frame-final.png').convert('RGB').crop((window['x']*scale,window['y']*scale,(window['x']+213)*scale,(window['y']+143)*scale))
    fixture=read(ROOT/'build/create-window-implementation/fabric-fixture.json')
    jar=ROOT/next(r['path'] for r in fixture['artifacts'] if 'create' in r['mod_ids'])
    with ZipFile(jar) as z:atlas=Image.open(BytesIO(z.read('assets/create/textures/gui/schematics_2.png'))).convert('RGBA')
    # Use upstream alpha only to exclude world pixels in the native pointed outline.
    # The published pictures remain unmodified framebuffer crops.
    frame=Image.new('RGBA',(213,143))
    for source,point in [((0,77,213,119),(0,2)),((0,119,213,218),(0,44)),((0,0,205,15),(0,0))]:
        frame.alpha_composite(atlas.crop(source),point)
    alpha=frame.getchannel('A').resize(a.size,Image.Resampling.NEAREST)
    points=[(x,y) for y in range(15*scale,143*scale) for x in range(213*scale) if alpha.getpixel((x,y))==255]
    diff=sum(a.getpixel(p)!=b.getpixel(p) for p in points)
    assert diff==0,('Native body pixels changed',diff)
    bevel=[(x,y*scale+sy) for y in [0,1,13,14] for sy in range(scale) for x in range(205*scale) if alpha.getpixel((x,y*scale+sy))==255]
    bevel_diff=sum(a.getpixel(p)!=b.getpixel(p) for p in bevel)
    assert bevel_diff==0,('Native title bevel changed',bevel_diff)
    a.crop((0,0,426,274)).save(dest/'original.png');b.crop((0,0,426,274)).save(dest/'schematicannon.png')
    canvas=Image.new('RGB',(924,350),'#eff4f4');draw=ImageDraw.Draw(canvas)
    font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',20)
    draw.text((24,16),'Original Create',font=font,fill='#203638');draw.text((474,16),'SIU — native frame',font=font,fill='#203638')
    canvas.paste(a.crop((0,0,426,274)),(24,54));canvas.paste(b.crop((0,0,426,274)),(474,54));canvas.save(dest/'comparison.png')
    captures=dest/'captures';captures.mkdir(exist_ok=True)
    for label in ['native-frame-idle','native-frame-final','native-frame-settings','native-frame-gui4','native-frame-running','native-frame-paused']:
        shutil.copy2(runtime/'screenshots'/(label+'.png'),captures/(label+'.png'))
        shutil.copy2(runtime/'features'/(label+'.json'),captures/(label+'.json'))
    shutil.copy2(reference/'original-evidence.json',dest/'original-evidence.json')
    shutil.copy2(reference/'original-full.png',dest/'original-full.png')
    shutil.copy2(runtime/'features/journal.json',dest/'check-history.json')
    journal=read(runtime/'features/journal.json');latest={r['id']:r for r in journal['checks']}
    assert all(r['passed'] for r in latest.values())
    builds=[]
    for loader in ['fabric','forge']:
        folder=dest/'builds'/loader;folder.mkdir(parents=True,exist_ok=True)
        for project,mod,version in [('versions','salts_inventory_update','0.1.6'),('compatibility/versions','salts_inventory_compat','0.1.3')]:
            file=f'{mod}-1.20.1-{loader.title()}-{version}.jar';source=ROOT/project/'1.20.1'/loader/'build/libs'/file
            shutil.copy2(source,folder/file)
            builds.append({'loader':loader,'file':str((folder/file).relative_to(dest)),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
    acceptance={'scope':'Schematicannon only, Minecraft 1.20.1; existing Core/API and other window designs unchanged by this task',
        'runtime':'Production Fabric; upstream Create 6.0.8.1+build.1744-mc1.20.1 release JAR unchanged',
        'runtime_evidence':str(runtime.resolve()),'focused_checks':list(latest.values()),'passed':True,
        'native_art':{'opaque_body_pixels_compared':len(points),'different_body_pixels':diff,'title_bevel_pixels_compared':len(bevel),'different_title_bevel_pixels':bevel_diff,
            'includes':'Original body, slots, status, playback, footer, edges and pointed end at the same native offsets',
            'excludes':'Moved title text, added SIU controls, transparent world/background and animated outboard model'},
        'build_validation':{'fabric':'remapJar and two CreateLayoutsTest geometry tests passed','forge':'jar/reobfJar passed; no Forge runtime check'},
        'coverage_limit':'Focused Schematicannon checks only; not the complete Create functional/gameplay suite',
        'check_history_note':'An initial geometry assertion treated observer hit points as control centers; corrected the test interpretation. All latest checks pass; initial history retained.',
        'builds':builds}
    write(dest/'acceptance.json',acceptance)
    shutil.copy2(runtime/'staged-artifacts.json',dest/'runtime-artifacts.json')
    (dest/'README.md').write_text('''# Schematicannon native-frame prototype — 1.20.1

Create supplies the original 205×15 title texture, body, footer, pointed end and native controls. SIU moves the title left and puts its existing management controls in that strip. No extra SIU border, body fill or body padding is drawn. The complete native body and outboard model retain their original offsets. The player inventory remains an independent SIU window.

`comparison.png` contains real framebuffer crops: SIU absent on the left, implemented window on the right. Only the main Schematicannon is cropped; the separate player inventory is outside this comparison. `captures/` retains full live screenshots and their observations, including expanded settings and GUI scale 4.

25 focused in-game checks passed on production Fabric: title dragging/focus/pin/lock/close, inline settings and four replacement modes, play/pause/stop, actual block placement/materials/checklist, original footer confirmation, and GUI scale 4. Two layout geometry tests passed. Fabric and Forge Compatibility builds passed; Forge was build-checked only. This is not the complete functional/gameplay suite.

99,592 opaque body pixels match the original screenshot exactly, including the native footer and pointed edge. The original title bevel also matches. The moved title, added controls, transparent world pixels and animated companion are excluded from that pixel comparison. See `acceptance.json` for checks, evidence paths and artifact checksums.

`builds/` contains the matching Core 0.1.6 and updated Compatibility 0.1.3 for each loader. Core and the public API were not edited for this prototype. Install the pair for the matching loader; replace the older copies instead of adding duplicate versions.
''',encoding='utf-8')
    print(json.dumps({'published':str(dest),'focused_checks':len(latest),'different_body_pixels':diff,'title_bevel_difference':bevel_diff}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('runtime',type=Path);a=p.parse_args();run(a.runtime)
