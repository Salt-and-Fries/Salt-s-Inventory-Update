"""Check captured native geometry against the independent approved design manifest."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
design=json.loads((ROOT/'output/ssn-window-design/manifest.json').read_text())
paths=['inventory','request','request_expanded','inventory_remote','crafting_remote','expanded_remote','import_filter_kabel','export_kabel','filter_kabel','collector','process_kabel','cradle','receiver']
case_for=dict(zip(paths,design['cases']))
records=[]
for target,run in [('1.20.1-forge','1.20.1-forge-siu-ssn-implemented-06'),('1.21.1-neoforge','1.21.1-neoforge-integrated-ssn-implemented-04'),('26.1.2-neoforge','26.1.2-neoforge-integrated-ssn-implemented-04')]:
    folder=ROOT/'build/remaining-ui-audit/gameplay'/run/'ssn-checks'
    for path,case in case_for.items():
        source=folder/(path.replace('_','-')+'-client.json')
        if not source.exists():continue
        state=json.loads(source.read_text());w=next(w for w in state['windows']if w['menuId']=='storagenetwork:'+path);plan=w['composition']
        regions=plan['regions'];left,top,right,bottom=case['rect'];assert regions[0]['source']==dict(x=left,y=top,width=right-left,height=bottom-top)
        expected=case['geometry']['logical_size'];assert w['bounds']['height']==expected[1],(target,path,w['bounds'],expected)
        assert w['bounds']['width']==expected[0],(target,path,w['bounds'],expected)
        def mapped(x,y):
            for r in reversed(regions):
                b=r['source']
                if b['x']<=x<b['x']+b['width']and b['y']<=y<b['y']+b['height']and not any(c['x']<=x<c['x']+c['width']and c['y']<=y<c['y']+c['height']for c in r['cutouts']):return(r['id'],r['x']+x-b['x'],r['y']+y-b['y'])
            return None
        cells=widgets=excluded=0
        for cell in w['nativeSlots']:
            b=cell['bounds'];pixels=[mapped(x,y)for y in range(b['y'],b['y']+b['height'])for x in range(b['x'],b['x']+b['width'])]
            if cell['player']:assert not any(pixels),(target,path,'embedded inventory leaked');excluded+=1
            else:assert all(pixels),(target,path,'slot clipped',b);cells+=1
        native=w['nativeBounds']
        for widget in w['nativeWidgets']:
            if not widget['visible']:continue
            x,y=widget['x']-native['x'],widget['y']-native['y']
            pixels=[mapped(px,py)for py in range(y,y+widget['height'])for px in range(x,x+widget['width'])]
            assert all(pixels),(target,path,'widget clipped',widget)
            # A control must remain one contiguous rectangle, including the narrow autofocus toggle.
            assert len({p[0]for p in pixels})==1,(target,path,'widget split',widget)
            widgets+=1
        records.append(dict(target=target,path=path,slots=cells,widgets=widgets,excluded_player_cells=excluded,approved_size=expected))
out=ROOT/'output/ssn-implemented';out.mkdir(parents=True,exist_ok=True)
(out/'geometry-verification.json').write_text(json.dumps(dict(status='pass',views=len(records),slot_outlines=sum(r['slots']for r in records),widgets=sum(r['widgets']for r in records),records=records),indent=2))
print('PASS',len(records),'native views,',sum(r['slots']for r in records),'complete slot outlines,',sum(r['widgets']for r in records),'complete widget rectangles')
