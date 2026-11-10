"""Final screenshots from original release menus opened by ordinary interactions."""
from pathlib import Path
import argparse,json
from suite import Suite

def capture_all(output,suite_type=Suite):
    s=suite_type(output);s.call('command',command='gamemode survival @a',ticks=3);s.call('gui-scale',scale=2,width=1280,height=900,ticks=3)
    for command in ['weather clear','time set day','gamerule doWeatherCycle false']:
        s.call('command',command=command,ticks=3)
    s.call('command',command='kill @e[type=!minecraft:player]',ticks=25) # Disposable audit world; remove earlier moving contraptions.
    s.call('command',command='fill 6 65 7 12 72 13 minecraft:air',ticks=6) # Clear the chain conveyor used by the earlier port test.
    cases=[]
    def record(name,title):
        s.call('wait',ticks=25)
        s.call('move',x=5,y=5,ticks=2)
        window=s.window();label='final-'+name;s.snapshot(label)
        cases.append({'id':name,'title':title,'menu':window['menuId'],'native_screen':window['nativeScreen'],'image':str((output/'screenshots'/(label+'.png')).resolve()),'evidence':str((s.evidence/(label+'.json')).resolve())})
        s.check(label+'-native-window',lambda:s.window())
    for name,title,block in [('toolbox','Toolbox','create:brown_toolbox'),('schematicannon','Schematicannon','create:schematicannon'),('schematic-table','Schematic Table','create:schematic_table'),('frogport','Package Frogport','create:package_frogport'),('postbox','Postbox','create:brown_postbox'),('requester','Redstone Requester','create:redstone_requester')]:
        s.open_block(block);record(name,title)
        if name=='schematicannon':s.tap_native(17,120);record('schematicannon-settings','Schematicannon - settings')
    for name,title,item,shift in [('filter','List Filter','create:filter',False),('attribute','Attribute Filter','create:attribute_filter',False),('package-filter','Package Filter','create:package_filter',False),('controller','Linked Controller','create:linked_controller',True),('schedule','Train Schedule','create:schedule',False),('ammo','Autocannon Ammo Container','createbigcannons:autocannon_ammo_container',False),('creative-ammo','Creative Ammo Container','createbigcannons:creative_autocannon_ammo_container',False),('timed','Timed Fuze','createbigcannons:timed_fuze',False),('proximity','Proximity Fuze','createbigcannons:proximity_fuze',False),('inertia','Delayed Inertia Fuze','createbigcannons:delayed_inertia_fuze',False),('impact','Delayed Impact Fuze','createbigcannons:delayed_impact_fuze',False)]:
        s.fixture();s.open_item(item,shift);record(name,title)
        if name=='schedule':s.tap_native(50,32);record('schedule-editor','Schedule - nested instruction editor')
    s.stock_fixture();s.call('command',command='tp @a 8.5 65 12.5 180 18',ticks=3);s.call('open-ray',ticks=8);record('categories','Stock Ticker - categories');s.tap_native(27,32);record('category-editor','Stock Ticker - category editor')
    s.call('close',ticks=2);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=3);s.call('command',command='tp @a 11.5 65 8.5',ticks=4);s.stock_open();s.call('wait',ticks=60);record('stock','Stock Ticker - filled request catalogue')
    s.fixture('minecraft:stone');s.call('held',item='create:crafting_blueprint',ticks=2);s.call('use-block-ray',ticks=3);s.call('held',item='minecraft:air',ticks=1);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=3);s.call('aim-entity',entity='create:crafting_blueprint',ticks=3);s.call('use-entity-ray',entity='create:crafting_blueprint',ticks=8);record('blueprint','Crafting Blueprint')
    s.fixture();s.call('command',command='kill @e[type=create:crafting_blueprint]',ticks=3);s.call('wait',ticks=25);s.call('command',command='summon railways:conductor 8.5 65 8.5 {NoAI:1b}',ticks=3);s.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=3);s.call('held',item='create:brown_toolbox',ticks=2);s.call('aim-entity',entity='railways:conductor',ticks=3);s.call('use-entity-ray',entity='railways:conductor',ticks=3);s.call('held',item='minecraft:air',ticks=2);s.call('use-entity-ray',entity='railways:conductor',ticks=8);record('mounted-toolbox','Mounted Toolbox')
    s.fixture()
    for command in ['kill @e[type=railways:conductor]','setblock 8 65 7 minecraft:stone','setblock 8 65 8 create:factory_gauge[face=wall,facing=south]{bottom_left:{Freq:[I;1,2,3,4],FilterAmount:1},bottom_right:{Freq:[I;1,2,3,4],FilterAmount:1},top_left:{Freq:[I;1,2,3,4],FilterAmount:1},top_right:{Freq:[I;1,2,3,4],FilterAmount:1}}','tp @a 8.5 65 12.5 175.9 16']:s.call('command',command=command,ticks=3)
    s.call('wait',ticks=30);s.call('open-ray',ticks=10)
    if not s.state.get('sessions') and not s.state.get('menuClass'):s.call('wait',ticks=5);s.call('open-ray',ticks=10)
    record('gauge','Factory Gauge - monitored item')
    manifest=output/'final-captures.json';manifest.write_text(json.dumps({'cases':cases,'unique_menus':len({c['menu'] for c in cases})},indent=2)+'\n',encoding='utf-8')
    assert len({c['menu'] for c in cases})==20
    return cases

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args();capture_all(a.output)
