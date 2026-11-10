"""Compare complete return controls with the installed AE2 atlas, not SIU artwork."""
from pathlib import Path
import json,zipfile,io
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
WORK=ROOT/'build/ae2-tab-join-fix'
LABELS=['trash','magnet-filter','wireless-settings','terminal-settings','priority']
def compare(frame,state,atlas):
    w=next(w for w in state.get('allWindows',state.get('windows',[])) if w.get('definition','').endswith('.AEWindow'))
    tab=next(t for t in w['nativeWidgets'] if t.get('visible') and t.get('tabIcon')=='BACK')
    im=Image.open(frame).resize((state['guiWidth'],state['guiHeight']),Image.Resampling.NEAREST).convert('RGBA')
    actual=im.crop((tab['x'],tab['y'],tab['x']+22,tab['y']+22))
    scores=[]
    for label,rect in [('normal',(160,192,180,212)),('focused',(160,224,182,246))]:
        expected=Image.new('RGBA',(22,22))
        expected.alpha_composite(atlas.crop(rect),(0,0))
        expected.alpha_composite(atlas.crop((96,16,112,32)),(2,1))
        pixels=[(a,e) for a,e in zip(actual.get_flattened_data(),expected.get_flattened_data()) if e[3]==255]
        scores.append((sum(a==e for a,e in pixels),len(pixels),label))
    return max(scores,key=lambda s:s[0]/s[1])

rows=[]
for v in ['1.21.1','26.1.2','26.2','26.3']:
    folder=ROOT/f'build/all-target-rollout/gameplay/{v}-neoforge-ae2-tab-join-01'
    inputs=json.loads((folder/'staged-artifacts.json').read_text())['artifacts']
    ae=next(a for a in inputs if a['filename'].startswith('appliedenergistics2-'))
    with zipfile.ZipFile(ae['file']) as z:atlas=Image.open(io.BytesIO(z.read('assets/ae2/textures/guis/states.png'))).convert('RGBA')
    checks=[]
    for label in LABELS:
        meta=json.loads((WORK/v/(label+'.json')).read_text());score=compare(folder/'screenshots'/(label+'.png'),meta['state'],atlas)
        assert score[0]==score[1],(v,label,score)
        checks.append({'name':label,'matched_pixels':score[0],'opaque_reference_pixels':score[1],'native_background':score[2],'pass':True})
    (WORK/v/'pixel-checks.json').write_text(json.dumps(checks,indent=2));rows.append({'minecraft':v,'checks':checks})
# Negative control: the exact pre-fix screenshot must fail this same comparison.
p=ROOT/'output/ae2-wireless-modern/26.3';old=json.loads((p/'trash.json').read_text())['state']
for w in old.get('allWindows',old.get('windows',[])):
    for t in w.get('nativeWidgets',[]):
        if t.get('message')=='Wireless Crafting Terminal':t['tabIcon']='BACK'
score=compare(p/'trash.png',old,atlas)
assert score[0]<score[1],('Negative control unexpectedly passed',score)
(WORK/'pixel-verification.json').write_text(json.dumps({'targets':rows,'pre_fix_negative_control':{'matched':score[0],'total':score[1],'failed_as_expected':True}},indent=2))
print('20 native-atlas comparisons pass; the original broken tab fails the same test.')
