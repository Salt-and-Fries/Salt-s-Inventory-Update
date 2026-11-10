"""Focused live opening/geometry probe, using ordinary world/item interactions.
No menus, screens, templates or native widget actions are constructed by QA.
These are development checks, not the final native-feature acceptance suite.
"""
from pathlib import Path
import argparse,sys,json
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

def run(output,kind,**args):
    return send(output,kind,**args)['client']

def probe(output,case,opener,value):
    run(output,'close',ticks=2)
    run(output,'fixture',block=value if opener=='block' else 'minecraft:air',ticks=2)
    run(output,'command',command='tp @a 8.5 65 12.5 180 18',ticks=2)
    if opener=='block':run(output,'open-ray',ticks=12)
    else:
        run(output,'held',item=value,ticks=3)
        run(output,'use-item',shift=False,ticks=12)
    state=run(output,'capture',capture='probe-'+case,ticks=3)
    (output/('probe-'+case+'.json')).write_text(json.dumps(state,indent=2),encoding='utf-8')
    windows=state.get('allWindows',[])
    print(case,state.get('screen'),[(w.get('menuId'),w.get('bounds'),w.get('template'),[(r['id'],r['x'],r['y']) for r in w.get('composition',{}).get('regions',[]) if r['id'].startswith('extra')]) for w in windows],flush=True)
    return state

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args()
    for case,item in [('toolbox','create:brown_toolbox'),('schematicannon','create:schematicannon'),('schematic-table','create:schematic_table'),('frogport','create:package_frogport'),('requester','create:redstone_requester'),('stock-ticker','create:stock_ticker')]:
        try:probe(a.output,case,'block',item)
        except Exception as e:print(case,'ERROR',str(e),flush=True)
    for case,item in [('filter','create:filter'),('attribute','create:attribute_filter'),('package-filter','create:package_filter'),('controller','create:linked_controller'),('schedule','create:schedule'),('ammo','createbigcannons:autocannon_ammo_container'),('creative-ammo','createbigcannons:creative_autocannon_ammo_container'),('timed','createbigcannons:timed_fuze'),('proximity','createbigcannons:proximity_fuze'),('inertia','createbigcannons:delayed_inertia_fuze'),('impact','createbigcannons:delayed_impact_fuze')]:
        try:probe(a.output,case,'item',item)
        except Exception as e:print(case,'ERROR',str(e),flush=True)
