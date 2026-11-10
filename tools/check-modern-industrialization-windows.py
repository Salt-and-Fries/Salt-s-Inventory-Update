"""Focused MI opening/layout observations against real publisher menus."""
from pathlib import Path
import argparse,json,sys,traceback
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send

def windows(state):return state.get('windows',state.get('allWindows',[]))
def retained(regions,b):
    """Every source pixel must survive the region's intentional cutouts."""
    return all(any((s:=r['source'])['x']<=x<s['x']+s['width'] and s['y']<=y<s['y']+s['height']
        and not any(q['x']<=x<q['x']+q['width'] and q['y']<=y<q['y']+q['height'] for q in r.get('cutouts',[]))
        for r in regions) for y in range(b['y'],b['y']+b['height']) for x in range(b['x'],b['x']+b['width']))

def check(state):
    ws=windows(state);assert len(ws)==1,('window count',len(ws))
    w=ws[0];assert w['definition'].endswith('.NativeTemplateWindow'),w['definition']
    assert not w.get('layoutError'),w.get('layoutError')
    layout=w['template'];regions=layout['regions'];checked=0
    for cell in w['nativeSlots']:
        if cell['player'] or not cell['active']:continue
        b=cell['bounds']
        assert retained(regions,b),('cut cell',b)
        checked+=1
    for widget in w.get('nativeWidgets',[]):
        if not widget.get('visible',True):continue
        nb=w['nativeBounds'];b=dict(x=widget['x']-nb['x'],y=widget['y']-nb['y'],width=widget['width'],height=widget['height'])
        assert retained(regions,b),('cut widget',b,widget.get('message'))
    values=w.get('nativeValues',{})
    assert 'miObserverError' not in values,values.get('miObserverError')
    for component in values.get('miComponentBounds',[]):
        assert retained(regions,component),('cut component',component)
    return w,checked

def main():
    ap=argparse.ArgumentParser();ap.add_argument('run');ap.add_argument('--limit',type=int);ap.add_argument('--only');args=ap.parse_args()
    run=ROOT/'build/remaining-ui-audit/gameplay'/args.run
    out=ROOT/'build/modern-industrialization-implementation'/args.run;out.mkdir(parents=True,exist_ok=True)
    cases=json.loads((ROOT/'output/modern-industrialization-design/mockup-manifest.json').read_text())['cases']
    cases=[c for c in cases if c['id'].startswith('modern-industrialization-')]
    if args.only:cases=[c for c in cases if c['id'].endswith(args.only)]
    if args.limit:cases=cases[:args.limit]
    send(run,'catalog',namespaces=['modern_industrialization'],ticks=1);catalog=json.loads((run/'catalog.json').read_text())
    results=json.loads((out/'geometry-results.json').read_text()) if args.only and (out/'geometry-results.json').exists() else []
    for i,c in enumerate(cases):
        slug=c['id'].removeprefix('modern-industrialization-');block='modern_industrialization:'+slug.replace('-','_')
        if block not in catalog['blocks']:
            results.append(dict(id=c['id'],status='absent from publisher release'));continue
        try:
            send(run,'fixture',block=block,ticks=3)
            send(run,'command',command='tp @a 8.5 65 12.5 180 16',ticks=7)
            s=send(run,'open' if slug=='forge-hammer' else 'open-ray',ticks=6)['client'];w,n=check(s)
            send(run,'capture',capture='mi-'+slug,ticks=1)
            (out/(slug+'.json')).write_text(json.dumps(s,indent=2))
            results=[r for r in results if r['id']!=c['id']]
            results.append(dict(id=c['id'],status='pass',cells=n,bounds=w['bounds'],native=w['nativeBounds'],menu=w['menuId'],components=w.get('nativeValues',{}).get('miComponentBounds',[]),capture=str((run/'screenshots'/('mi-'+slug+'.png')).relative_to(ROOT))))
        except Exception as e:
            results.append(dict(id=c['id'],status='FAIL',error=str(e)))
            (out/(slug+'-failure.json')).write_text(json.dumps(send(run,'wait',ticks=1),indent=2))
        (out/'geometry-results.json').write_text(json.dumps(results,indent=2))
        print(args.run,i+1,len(cases),slug,results[-1]['status'],flush=True)
    print('DONE',sum(r['status']=='pass' for r in results),'FAIL',sum(r['status']=='FAIL' for r in results),flush=True)

if __name__=='__main__':main()
