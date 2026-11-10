"""GUI scale 4 frame/companion visibility checks for every supported Create menu."""
from pathlib import Path
import argparse,json
from native_all import NativeAll

def run(output,resume=False):
    s=NativeAll(output);s.call('gui-scale',scale=4,width=1280,height=960,ticks=6)
    s.call('command',command='gamemode survival @a',ticks=2)
    s.call('command',command='kill @e[type=!minecraft:player]',ticks=20) # Clear moving contraptions left by the connected-port fixture.
    s.call('command',command='fill 6 65 7 12 72 13 minecraft:air',ticks=6) # Clear the chain conveyor used by the earlier port test.
    cases=[]
    def record(name,width,height):
        s.call('wait',ticks=10);w=s.window();b=w['bounds'];gw=s.state['guiWidth'];gh=s.state['guiHeight']
        s.check(name+'-gui4-original-size',lambda:s.assert_equal((b['width'],b['height']),(width,height)))
        s.check(name+'-gui4-frame-fits',lambda:s.assert_equal(b['x']>=0 and b['y']>=0 and b['x']+width<=gw and b['y']+height<=gh,True))
        def native_regions():
            p=w['composition'];assert p['padding']==0 and not p['scrollable'],p
            assert not any(r['id'].startswith('extra-') for r in p['regions']),p
            for r in p['regions']:
                if r.get('exterior'):
                    assert b['x']+r['x']>=0 and b['y']+r['y']>=0,(name,'left/top',r,b)
                    assert b['x']+r['x']+r['source']['width']<=gw,(name,'right',r,b,gw)
                    assert b['y']+r['y']+r['source']['height']<=gh,(name,'bottom',r,b,gh)
        s.check(name+'-gui4-complete-native-regions',native_regions);s.photo('gui4-'+name);cases.append(name)
    if not resume:
        for name,block,w,h in [('toolbox','create:brown_toolbox',188,171),('schematicannon','create:schematicannon',213,143),('schematic-table','create:schematic_table',214,85),('frogport','create:package_frogport',220,99),('postbox','create:brown_postbox',220,106),('requester','create:redstone_requester',232,120)]:
            s.open_block(block);record(name,w,h)
        for name,item,shift,w,h in [('filter','create:filter',False,214,99),('attribute','create:attribute_filter',False,241,85),('package-filter','create:package_filter',False,218,79),('controller','create:linked_controller',True,179,109),('schedule','create:schedule',False,256,226),('ammo','createbigcannons:autocannon_ammo_container',False,179,83),('creative-ammo','createbigcannons:creative_autocannon_ammo_container',False,199,83),('timed','createbigcannons:timed_fuze',False,179,83),('proximity','createbigcannons:proximity_fuze',False,179,83),('inertia','createbigcannons:delayed_inertia_fuze',False,179,83),('impact','createbigcannons:delayed_impact_fuze',False,179,83)]:
            s.fixture();s.open_item(item,shift);record(name,w,h)
            if name=='schedule':s.tap_native(50,32);record('schedule-editor',256,89)
        s.stock_fixture();s.call('command',command='tp @a 8.5 65 12.5 180 18',ticks=3);s.call('open-ray',ticks=8);record('categories',200,131)
        s.tap_native(27,32);record('category-editor',200,89)
    else:
        cases=['toolbox','schematicannon','schematic-table','frogport','postbox','requester','filter','attribute','package-filter','controller','schedule','schedule-editor','ammo','creative-ammo','timed','proximity','inertia','impact','categories','category-editor']
        latest={c['id']:c for c in s.results['checks']}
        for case in cases:
            for suffix in ['original-size','frame-fits','complete-native-regions']:
                assert latest[case+'-gui4-'+suffix]['passed'],case
            assert (output/'features'/('gui4-'+case+'.json')).is_file(),case
    s.stock_fixture()
    s.call('close',ticks=2);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=3);s.call('command',command='tp @a 11.5 65 8.5',ticks=4);s.stock_open();s.call('wait',ticks=35);record('stock',256,216)
    s.fixture('minecraft:stone');s.call('held',item='create:crafting_blueprint',ticks=2);s.call('use-block-ray',ticks=3);s.call('held',item='minecraft:air',ticks=1);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=3);s.call('aim-entity',entity='create:crafting_blueprint',ticks=2);s.call('use-entity-ray',entity='create:crafting_blueprint',ticks=8);record('blueprint',179,109)
    s.fixture();s.call('command',command='kill @e[type=create:crafting_blueprint]',ticks=4);s.call('command',command='summon railways:conductor 8.5 65 8.5 {NoAI:1b}',ticks=3);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=3);s.call('held',item='create:brown_toolbox',ticks=2);s.call('aim-entity',entity='railways:conductor',ticks=2);s.call('use-entity-ray',entity='railways:conductor',ticks=3);s.call('held',item='minecraft:air',ticks=2);s.call('use-entity-ray',entity='railways:conductor',ticks=8);record('mounted-toolbox',188,171)
    s.fixture()
    for c in ['kill @e[type=railways:conductor]','setblock 8 65 7 minecraft:stone','setblock 8 65 8 create:factory_gauge[face=wall,facing=south]{bottom_left:{Freq:[I;1,2,3,4],FilterAmount:1},bottom_right:{Freq:[I;1,2,3,4],FilterAmount:1},top_left:{Freq:[I;1,2,3,4],FilterAmount:1},top_right:{Freq:[I;1,2,3,4],FilterAmount:1}}','tp @a 8.5 65 12.5 175.9 16']:s.call('command',command=c,ticks=3)
    s.call('wait',ticks=30);s.call('open-ray',ticks=10);record('gauge',184,88)
    s.call('gui-scale',scale=2,width=1280,height=900,ticks=4)
    s.results['groups']['native_responsive']='GUI scale 4 original sizes, no fallback extra strips, complete native body and companion bounds for '+str(len(cases))+' states';s.save()
    (output/'native-responsive-cases.json').write_text(json.dumps(cases,indent=2)+'\n')
    print(json.dumps({'native_responsive_states':len(cases),'passed':True}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--resume',action='store_true');a=p.parse_args();run(a.output,a.resume)
