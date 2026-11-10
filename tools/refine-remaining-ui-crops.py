"""Remove unrelated FTB sidebar buttons from auto-derived report crops.

Raw captures are never changed. Explicit visual-review crops are preserved.
Only an exact match to the former automatic crop is eligible for refinement.
"""
import argparse
import json
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]

def proposed(row,ignore_sidebar=False):
    state=row['observation']['client'];windows=state.get('allWindows',[])
    bounds=(windows[-1].get('bounds') if windows else None) or state.get('bounds')
    path=Path(row['capture'])
    if not path.is_absolute():path=ROOT/path
    with Image.open(path) as img:width,height=img.size
    if not bounds:return [0,0,width,height]
    sx,sy=width/state['guiWidth'],height/state['guiHeight']
    x,y=bounds['x'],bounds['y'];right,bottom=x+bounds['width'],y+bounds['height']
    for widget in state.get('widgets',[]):
        if not widget.get('visible',True):continue
        if ignore_sidebar and widget.get('type')=='dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton':continue
        x,y=min(x,widget['x']),min(y,widget['y'])
        right=max(right,widget['x']+widget['width']);bottom=max(bottom,widget['y']+widget['height'])
    return [max(0,round((x-8)*sx)),max(0,round((y-8)*sy)),min(width,round((right+8)*sx)),min(height,round((bottom+8)*sy))]

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--apply',action='store_true');args=p.parse_args()
    records={}
    for f in (ROOT/'build/remaining-ui-audit/gameplay').rglob('*.json'):
        if not ((f.name.startswith('audit-') and f.name.endswith('-cases.json')) or 'records' in f.name):continue
        try:rows=json.loads(f.read_text(encoding='utf8'))
        except (ValueError,UnicodeError):continue
        if not isinstance(rows,list):continue
        for r in rows:
            if not isinstance(r,dict) or not r.get('capture') or not r.get('observation',{}).get('client'):continue
            records[r['capture'].replace('\\','/')]=r
    changes=[]
    for f in (ROOT/'output/remaining-ui-audit').glob('*-pairs.json'):
        data=json.loads(f.read_text(encoding='utf8'));changed=False
        for case in data['cases']:
            for side in ('native','core'):
                s=case[side];record=records.get(s['capture'].replace('\\','/'))
                if not record or record.get('sha256')!=s.get('sha256'):continue
                if not any(w.get('type')=='dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton' for w in record['observation']['client'].get('widgets',[])):continue
                before,after=proposed(record),proposed(record,True)
                if s.get('crop_rect')!=before or before==after:continue
                changes.append({'file':str(f.relative_to(ROOT)),'id':case['id'],'side':side,'before':before,'after':after})
                s['crop_rect']=after;changed=True
        if changed and args.apply:f.write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
    dest=ROOT/'build/remaining-ui-audit/crop-refinement-review.json';dest.write_text(json.dumps(changes,indent=2),encoding='utf8')
    print(json.dumps({'eligible':len(changes),'applied':args.apply,'review':str(dest.relative_to(ROOT))}))

if __name__=='__main__':main()
