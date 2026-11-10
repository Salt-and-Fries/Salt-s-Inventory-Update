"""Focused real-input checks against original Charm and shipping SIU JARs.

Requires the two disposable native/SIU production profiles to be running.
The excluded observer only reads widgets; it does not suppress or alter them.
"""
from pathlib import Path
import argparse,json,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send

WORK=ROOT/'build/redundant-sort-suppression'

def widgets(state):
    result=list(state.get('widgets',[]))
    for window in state.get('allWindows',[]):result.extend(window.get('nativeWidgets',[]))
    return result

def redundant(state):
    return [w for w in widgets(state) if any('charm:' in resource and
        any(name in resource for name in ('tidy','inventory_buttons'))
        for resource in w.get('imageResources',[]))]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--minecraft',required=True);p.add_argument('--run',default='charm-sort-global01')
    p.add_argument('--prefix',default='verified')
    a=p.parse_args();checks=[];observations=[]
    def check(condition,label):
        checks.append({'pass':bool(condition),'check':label})
        print(('PASS ' if condition else 'FAIL ')+label,flush=True)
        if not condition:raise AssertionError(label)
    def observe(run,side,name,screenshot=False,positive=True):
        stem=a.prefix+'-'+name
        reply=send(run,'capture' if screenshot else 'wait',capture=stem,ticks=16)
        state=reply['client'];path=run/(stem+'-observation.json')
        path.write_text(json.dumps(state,indent=2))
        count=len(redundant(state))
        observations.append({'side':side,'ui':name,'state':str(path),'charm_sort_widgets':count,
            'screenshot':str(run/'screenshots'/(stem+'.png')) if screenshot else None})
        check(not state.get('observerError'),side+' '+name+' observer reads actual screen')
        if side=='native':
            # Charm itself does not consistently restore its widget on native
            # GUI-scale reinitialization; initial menu opens are the positive control.
            if positive:check(count>0,side+' '+name+' has publisher Charm sort button (positive control)')
        else:
            check(state.get('screen','').endswith('InventoryDesktopScreen'),side+' '+name+' is a live SIU workspace')
            check(count==0,side+' '+name+' has no Charm sort widget')
        return state
    try:
        for side in ['native','siu']:
            run=ROOT/'build/remaining-ui-audit/gameplay'/f'{a.minecraft}-fabric-{side}-{a.run}'
            assert (run/'ready.json').exists(),run
            send(run,'close');send(run,'gui-scale',scale=2,ticks=8)
            send(run,'command',command='gamemode survival @a',ticks=5)
            send(run,'inventory',ticks=12)
            state=observe(run,side,'inventory',True)
            if side=='siu':
                inventory=next(w for w in state['workspaceWindows'] if w['kind']=='INVENTORY')
                check('SORT_INVENTORY' in inventory['controls'],'SIU inventory sort control retained')
            for block in ['minecraft:chest','minecraft:furnace','charm:kiln','charm:woodcutter']:
                send(run,'close');send(run,'fixture',block=block,ticks=12)
                send(run,'open',ticks=16)
                name=block.replace(':','-')
                state=observe(run,side,name,block in ['minecraft:chest','charm:kiln'])
                if side=='siu' and block=='minecraft:chest':
                    container=next(w for w in state['workspaceWindows'] if w['kind']=='CONTAINER')
                    check('SORT_INVENTORY' in container['controls'],'SIU container sort control retained')
                if block=='charm:kiln':
                    check(any('recipe' in resource for w in widgets(state) for resource in w.get('imageResources',[])),side+' Kiln recipe-book control retained')
                    send(run,'gui-scale',scale=3,ticks=16)
                    observe(run,side,'kiln-after-gui-reinit',positive=False)
                    send(run,'gui-scale',scale=2,ticks=16)
            send(run,'close');send(run,'open',ticks=16)
            observe(run,side,'woodcutter-reopened')
    finally:
        result={'minecraft':a.minecraft,'loader':'fabric','checks':checks,'observations':observations,
            'scope':'Focused sorting-widget presence/absence, SIU controls, recipe-book retention, GUI reinitialization and reopening only. Not a complete gameplay suite.'}
        (WORK/(a.minecraft+'-focused-checks.json')).write_text(json.dumps(result,indent=2))

if __name__=='__main__':main()
