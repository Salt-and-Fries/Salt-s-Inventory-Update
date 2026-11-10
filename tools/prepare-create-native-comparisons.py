"""Prepare neutral, version-matched original-vs-SIU visual evidence."""
from pathlib import Path
import json,hashlib,shutil
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/create-native-comparison-review'
NATIVE=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-create-comparison-native01'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def copy(src,dest):dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest);assert sha(src)==sha(dest);return str(dest)

def main():
    native=read(NATIVE/'final-captures.json');siu=read(ROOT/'output/create-window-implementation/acceptance-evidence.json')
    ns={x['id']:x for x in native['cases']};native_stage=read(NATIVE/'staged-artifacts.json')
    assert native_stage['siu_loaded'] is False and not any(x['family'] in ('core','compatibility') for x in native_stage['artifacts'])
    native_jars={x['filename']:x['sha256'] for x in native_stage['artifacts'] if x['family']=='upstream'}
    font=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',23);small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',17)
    pairs=[]
    for old in siu['gallery']:
        case=ns[old['id']];n_image=Path(case['image']);n_record=Path(case['evidence'])
        if old['id']=='stock':n_image=NATIVE/'screenshots/final-stock-filled-native.png';n_record=NATIVE/'features/final-stock-filled-native.json'
        raw_n=read(n_record);n=raw_n['client'];assert n['siu_loaded'] is False
        side=old['screens']['fabric'];s_image=ROOT/side['original'];s_record=ROOT/side['evidence'];raw_s=read(s_record);s=raw_s['client']
        assert s['siu_loaded'] is True
        win=next(w for w in s['allWindows'] if w['menuId']==old['menu'])
        assert n['screen']==win['nativeScreen']==case['native_screen'],old['id']
        assert n['guiWidth']==s['guiWidth']==640 and n['guiHeight']==s['guiHeight']==450
        stage=read(s_image.parents[1]/'staged-artifacts.json');s_jars={a['filename']:a['sha256'] for a in stage['artifacts'] if a['family']=='upstream'}
        assert s_jars==native_jars,'Upstream release mismatch'
        folder=OUT/'pairs'/old['id'];folder.mkdir(parents=True,exist_ok=True)
        n_full=Path(copy(n_image,folder/'original-full.png'));s_full=Path(copy(s_image,folder/'siu-full.png'))
        copy(n_record,folder/'original-evidence.json');copy(s_record,folder/'siu-evidence.json')
        ni=Image.open(n_full);sii=Image.open(s_full);rx=ni.width/n['guiWidth'];ry=ni.height/n['guiHeight'];b=n['bounds']
        x0,y0,x1,y1=b['x'],b['y'],b['x']+b['width'],b['y']+b['height']
        for widget in n.get('widgets',[]):
            if widget.get('visible') and widget['width']>0 and widget['height']>0:
                x0=min(x0,widget['x']);y0=min(y0,widget['y']);x1=max(x1,widget['x']+widget['width']);y1=max(y1,widget['y']+widget['height'])
        for slot in n.get('slots',[]):
            if slot['x']>=0 and slot['y']>=0:
                x0=min(x0,b['x']+slot['x']-1);y0=min(y0,b['y']+slot['y']-1);x1=max(x1,b['x']+slot['x']+17);y1=max(y1,b['y']+slot['y']+17)
        # Generous external padding preserves floating native models/tabs. The
        # complete screenshot is also supplied so a crop cannot prove clipping.
        nr=[max(0,round((x0-80)*rx)),max(0,round((y0-32)*ry)),min(ni.width,round((x1+80)*rx)),min(ni.height,round((y1+32)*ry))]
        nb=ni.crop(nr);nb.save(folder/'original-ui.png')
        b=win['bounds'];sx=sii.width/s['guiWidth'];sy=sii.height/s['guiHeight'];sr=[max(0,round(b['x']*sx)-4),max(0,round(b['y']*sy)-4),min(sii.width,round((b['x']+b['width'])*sx)+4),min(sii.height,round((b['y']+b['height'])*sy)+4)]
        sb=sii.crop(sr);sb.save(folder/'siu-ui.png')
        col=max(nb.width,sb.width)+24;hh=max(nb.height,sb.height)+136
        board=Image.new('RGB',(col*2+16,hh),'#edf3f4');d=ImageDraw.Draw(board)
        d.text((12,8),old['title'],font=font,fill='#203c42');d.text((12,42),'ORIGINAL - SIU ABSENT',font=small,fill='#205a83');d.text((col+28,42),'SIU WINDOW - REVIEW THIS RESULT',font=small,fill='#9b4d22')
        board.paste(nb,(12+(col-24-nb.width)//2,78));board.paste(sb,(col+28+(col-24-sb.width)//2,78))
        d.text((12,hh-44),'Same original mod releases / Minecraft 1.20.1 Fabric / GUI scale 2 / images not rescaled',font=small,fill='#4b6167')
        d.text((12,hh-22),'Full screenshots supplied separately. Original includes attached inventory and external native decoration.',font=small,fill='#4b6167')
        board.save(folder/'comparison.png')
        pairs.append({'id':old['id'],'title':old['title'],'menu_id':old['menu'],'native_screen':case['native_screen'],'minecraft':'1.20.1','loader':'fabric','gui_scale':2,'framebuffer':[ni.width,ni.height],'comparison_image':str(folder/'comparison.png'),'original':{'full_image':str(n_full),'ui_image':str(folder/'original-ui.png'),'evidence':str(folder/'original-evidence.json'),'original_capture':str(n_image),'full_image_sha256':sha(n_full),'crop_rectangle_pixels':nr},'siu':{'full_image':str(s_full),'ui_image':str(folder/'siu-ui.png'),'evidence':str(folder/'siu-evidence.json'),'original_capture':str(s_image),'full_image_sha256':sha(s_full),'crop_rectangle_pixels':sr},'state_note':'Same named native menu and default/empty state, except the stock catalogue is filled with stone and iron on both sides. Native viewport height, world scene, animation phase and item ordering may differ. UI detail crops have different margins; use full images to distinguish actual clipping from crop boundaries.'})
    assert len(pairs)==25 and len({p['menu_id'] for p in pairs})==20
    result={'review_subject':'The SIU-windowed version, judged against its corresponding original native UI','rubric':['No sprites are cut off.','Interfaces should not look cut and pasted with abrupt borders.','Match the original layout as closely as possible while retaining its original visual style.'],'scope':'25 states shown in the implemented-window report, now compared using matched 1.20.1 Fabric original releases. This evidence does not independently establish Forge parity.','expected_platform_changes':'SIU title-bar/window-management controls and moving the player inventory into a separate SIU inventory window are platform behavior. Evaluate the retained mod interface, not the mere presence of the SIU frame or separation of player inventory.','comparison_rules':['Attribute criticism to original-only, inherited unchanged, SIU-introduced, SIU-amplified, intentional platform adaptation, or uncertain.','Judge original material/layout conventions by the provided original; do not invent a cleaner redesign as the baseline.','Before claiming clipping, inspect the original and SIU full screenshots, and distinguish complete omission, intentional viewport clipping, crop boundary and an actually severed sprite.','Report what is preserved well as well as regressions. Do not claim runtime failures from stills.'],'upstream_release_sha256':native_jars,'mod_versions':read(ROOT/'build/create-window-implementation/target-matrix.json')[:3],'pairs':pairs}
    OUT.mkdir(parents=True,exist_ok=True);(OUT/'comparison-manifest.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print(json.dumps({'pairs':len(pairs),'menus':20,'manifest':str(OUT/'comparison-manifest.json')}))
if __name__=='__main__':main()
