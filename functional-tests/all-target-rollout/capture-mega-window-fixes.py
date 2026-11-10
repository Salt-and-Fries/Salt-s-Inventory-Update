"""Natural MEGA Cells entry, native titles and exact lower-edge artwork checks."""
from pathlib import Path
import argparse,json,subprocess,sys,zipfile
from io import BytesIO
from PIL import Image
from evidence import send,capture

p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--fixture',type=Path,required=True);p.add_argument('--validate-captured',action='store_true')
a=p.parse_args();checks=[]

def check(name,okay):
    checks.append(dict(check=name,passed=bool(okay)))
    (a.run/'mega-window-focused-checks.json').write_text(json.dumps(dict(scope='Affected MEGA Cells menu entry, native titles and footer pixels only; feature interactions recorded separately; no full gameplay suite',checks=checks),indent=2))
    assert okay,name
    print('PASS',name,flush=True)

manifest=f'build/all-target-rollout/themed-reviewed-cases-{"1211-neoforge" if a.minecraft=="1.21.1" else "1201-"+a.loader}.json'
args=[sys.executable,'-X','utf8',str(Path(__file__).with_name('capture-reviewed-manifests.py')),str(a.run),'--side','siu','--minecraft',a.minecraft,'--loader',a.loader,'--manifest',manifest,'--record','mega-captures']
for name in ['megacells-mega-interface','megacells-mega-pattern-provider','own-ae-part-cell-dock']:args+=['--only-case',name]
if not a.validate_captured:subprocess.run(args,check=True)
captures=json.loads((a.run/'mega-captures.json').read_text())
check('three-original-mega-menus-open',len(captures)==3 and all(r['status']=='ordinary-entry-and-picture-pass' for r in captures))
send(a.run,'catalog',ticks=1);items=json.loads((a.run/'catalog.json').read_text())['items']
if 'megacells:portable_cell_workbench' in items:
    send(a.run,'close',ticks=3);send(a.run,'held',item='megacells:portable_cell_workbench',ticks=4)
    s=send(a.run,'use-item',ticks=18)['client'];w=s['windows'][0]
    check('portable-workbench-has-native-screen-and-full-workspace',w.get('nativeScreen','').endswith('.PortableCellWorkbenchScreen') and w['bounds']['width']==176 and w['bounds']['height']==162)
    check('portable-workbench-title-is-native',w.get('aeManagementTitle')=='Portable Cell Workbench')
    capture(a.run,'siu',a.minecraft,a.loader,'megacells-portable-cell-workbench','Original handheld item opens its complete native workbench, cell/filter slots and toolbar in the SIU AE2 window.')

rows=json.loads((Path(__file__).resolve().parents[2]/'build/all-target-rollout/ui-capture-ledger.json').read_text())
selected=[r for r in rows if r['side']=='siu' and Path(r['run']).resolve()==a.run.resolve()]
for ui in ['megacells-mega-interface','megacells-mega-pattern-provider','own-ae-part-cell-dock']:
    r=next(r for r in reversed(selected) if r['ui']==ui);w=r['state'].get('allWindows',r['state'].get('windows'))[0]
    check(ui+'-uses-original-ae-screen',w.get('definition','').endswith('.AEWindow') and bool(w.get('nativeScreen')))
    check(ui+'-has-readable-native-title',bool(w.get('aeManagementTitle')) and 'gui.ae2.DesktopWindow.' not in w['aeManagementTitle'])
    if ui=='own-ae-part-cell-dock':check('dock-title-is-me-cell-dock',w['aeManagementTitle']=='ME Cell Dock')

fixture=json.loads(a.fixture.read_text());jar=next(Path(v['path']) for v in fixture['artifacts'] if v['filename'].startswith('megacells'))
with zipfile.ZipFile(jar) as z:
    for ui,name in [('megacells-mega-interface','mega_interface'),('megacells-mega-pattern-provider','mega_pattern_provider')]:
        r=next(r for r in reversed(selected) if r['ui']==ui);w=r['state'].get('allWindows',r['state'].get('windows'))[0];b=w['bounds']
        native=Image.open(BytesIO(z.read(f'assets/ae2/textures/guis/megacells/{name}.png'))).convert('RGBA');source_bottom=native.getbbox()[3]
        frame=Image.open(r['screenshot']).convert('RGB');sx=frame.width/r['state']['guiWidth'];sy=frame.height/r['state']['guiHeight']
        samples=[]
        if a.minecraft=='1.20.1':
            # AE2 15 intentionally uses its existing vintage Salt contour,
            # including rounded corners, rather than the modern lilac PNG rim.
            for dy,value in enumerate([198,85,85,0]):
                for x in [8,60,100,165]:
                    actual=frame.getpixel((int((b['x']+x+.5)*sx),int((b['y']+b['height']-4+dy+.5)*sy)))
                    samples.append(all(abs(channel-value)<=1 for channel in actual))
            check(ui+'-complete-vintage-frame-footer',all(samples))
            continue
        for dy in range(4):
            for x in [0,1,2,8,60,100,170,173,174,175]:
                actual=frame.getpixel((int((b['x']+x+.5)*sx),int((b['y']+b['height']-4+dy+.5)*sy)))
                expected=native.getpixel((x,source_bottom-4+dy))[:3]
                samples.append(actual==expected)
        check(ui+'-complete-four-native-footer-rows',all(samples))

print('PASS affected MEGA window checks',len(checks),flush=True)
