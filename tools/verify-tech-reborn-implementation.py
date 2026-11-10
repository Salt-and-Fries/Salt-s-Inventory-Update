"""Focused geometry, immutable evidence and native cell-art verification."""
from pathlib import Path
import json,hashlib,importlib.util
from PIL import Image,ImageDraw,ImageFont,ImageChops
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/tech-reborn-implementation'
spec=importlib.util.spec_from_file_location('report',ROOT/'tools/build-tech-reborn-implemented-report.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def main():
    records=json.loads((OUT/'captures.json').read_text());cases=[r for r in records if r.get('variant')in('idle','formed-idle')];assert len(cases)==51
    native={r['id']:r for r in json.loads((ROOT/'output/tech-reborn-window-design/fresh-native.json').read_text())}
    review=OUT/'review';review.mkdir(exist_ok=True);font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',15)
    cells=widgets=exact=0;differences=[];checks=[]
    for row in cases:
        w=row['state']['allWindows'][-1];b=w['bounds'];regions=w['composition']['regions'];assert regions[0]['id']=='native-machine'
        raw=Image.open(ROOT/row['capture']).convert('RGB');orig=Image.open(ROOT/native[row['id']]['capture']).convert('RGB');nb=native[row['id']]['state']['bounds']
        assert hashlib.sha256((ROOT/row['capture']).read_bytes()).hexdigest()==row['sha256']
        for s in w['slots']:
            if s['player']and s['y']>=92:continue
            sx,sy=s['x']-1,s['y']-1
            full=[r for r in regions if r['source']['x']<=sx and r['source']['y']<=sy and r['source']['x']+r['source']['width']>=sx+18 and r['source']['y']+r['source']['height']>=sy+18]
            assert len(full)==1,(row['id'],s,full);cells+=1;r=full[0];q=r['source']
            dx=b['x']+r['x']+(0 if r['exterior']else 6)+sx-q['x'];dy=b['y']+r['y']+(0 if r['exterior']else 22)+sy-q['y']
            a=raw.crop((dx*2,dy*2,(dx+18)*2,(dy+18)*2));c=orig.crop(((nb['x']+sx)*2,(nb['y']+sy)*2,(nb['x']+sx+18)*2,(nb['y']+sy+18)*2))
            if ImageChops.difference(a,c).getbbox()is None:exact+=1
            else:
                diff=ImageChops.difference(a,c).getbbox()
                assert diff==(2,2,34,34) and row['id']=='techreborn-industrial-electrolyzer'and s['index']==41,(row['id'],s,diff)
                differences.append(dict(id=row['id'],slot=s['index'],player=s['player'],rim_pixel_identical=True,reason='Original reference contains a hover highlight in the 16px cell interior; implemented capture is unhovered'))
        for v in w['nativeWidgets']:
            if not v['visible']:continue
            n=w['nativeBounds'];sx,sy=v['x']-n['x'],v['y']-n['y']
            assert any(q['source']['x']<=sx and q['source']['y']<=sy and q['source']['x']+q['source']['width']>=sx+v['width']and q['source']['y']+q['source']['height']>=sy+v['height']for q in regions),(row['id'],v)
            widgets+=1
        for q in regions:
            if q['id'].startswith('native-settings/'):assert q['x']==-22
            if q['id']=='native-upgrade-cells':assert q['x']==b['width']-2
        if row['id']=='techreborn-industrial-blast-furnace':assert w.get('cachedHeat')==1860,w.get('cachedHeat')
        m.crop_window(row).save(review/(row['id']+'.png'))
        checks.append(dict(id=row['id'],cells=sum(not s['player']or s['y']<92 for s in w['slots']),widgets=len(w['nativeWidgets'])))
    for start in range(0,len(cases),9):
        sheet=Image.new('RGB',(1452,1032),'#edf4f4');draw=ImageDraw.Draw(sheet)
        for i,r in enumerate(cases[start:start+9]):
            x=16+(i%3)*480;y=12+(i//3)*340;im=m.crop_window(r);im.thumbnail((454,286),Image.Resampling.NEAREST);sheet.paste(im,(x+(454-im.width)//2,y+32+(286-im.height)//2));draw.text((x,y),r['state']['allWindows'][-1]['nativeScreen'].split('.')[-1],font=font,fill='#173f46')
        sheet.save(review/f'windows-{start//9+1:02}.png')
    run=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-tech-reborn-implemented-10'
    log=(run/'launch.log').read_text(errors='replace')
    assert not any(s in log for s in ['native template callback failed','native template layout declined','Unknown sync pair']), 'Native adapter diagnostic in final run'
    result=dict(status='passed',screen_types=51,complete_cells=cells,native_widgets=widgets,exact_native_cell_art=exact,pixel_differences=differences,left_tabs_attached=True,right_upgrades=True,live_blast_furnace_heat=1860,clean_adapter_log=True,cases=checks)
    (OUT/'geometry-checks.json').write_text(json.dumps(result,indent=2));print({k:v for k,v in result.items()if k!='cases'})
if __name__=='__main__':main()
