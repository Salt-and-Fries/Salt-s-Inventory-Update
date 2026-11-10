"""Focused native-frame checks for five Create menus, using their real actions."""
from pathlib import Path
import argparse,json
from suite import Suite
from fixtures import prepare_schematics

class NativeFive(Suite):
    def capture(self,label):
        self.call('move',x=2,y=2,ticks=3);return self.snapshot(label)
    def native_frame(self,name,width,height):
        w=self.window();b=w['bounds']
        self.check(name+'-native-size',lambda:self.assert_equal((b['width'],b['height']),(width,height)))
        self.check(name+'-no-padding',lambda:self.assert_equal((w['composition']['padding'],w['composition']['scrollable']),(0,False)))
    def small(self,name,width,height,confirm):
        self.call('gui-scale',scale=4,width=1280,height=960,ticks=5);b=self.window()['bounds']
        self.check(name+'-gui4-native-size',lambda:self.assert_equal((b['width'],b['height']),(width,height)))
        def fits():
            w=self.window();b=w['bounds'];gw=self.state['guiWidth'];gh=self.state['guiHeight']
            assert b['x']>=0 and b['y']>=0 and b['x']+width<=gw and b['y']+height<=gh
            for r in w['composition']['regions']:
                if r.get('exterior'):
                    assert b['x']+r['x']>=0 and b['y']+r['y']>=0
                    assert b['x']+r['x']+r['source']['width']<=gw,(name,'horizontal',r,b,gw)
                    assert b['y']+r['y']+r['source']['height']<=gh,(name,'vertical',r,b,gh)
        self.check(name+'-gui4-complete-extents',fits)
        self.capture(name+'-gui4');self.close_native(*confirm)
        self.check(name+'-gui4-footer-confirm',lambda:self.assert_equal(self.state['sessions'],0))
        self.call('gui-scale',scale=2,width=1280,height=900,ticks=4)
    def toolbox_frame(self):
        self.open_block('create:brown_toolbox');self.native_frame('toolbox',188,171);self.capture('toolbox-empty')
        # The native title remains a usable manager-owned drag bar.
        self.tap(self.controls()['LOCK']);b=self.window()['bounds'];a={'x':b['x']+18,'y':b['y']+7};z={'x':a['x']+20,'y':a['y']+8}
        self.call('click',**a,button=0,ticks=1);self.call('drag',**z,button=0,dx=20,dy=8,ticks=1);self.call('release',**z,button=0,ticks=1)
        self.check('toolbox-title-drag',lambda:self.assert_equal((self.window()['bounds']['x'],self.window()['bounds']['y']),(b['x']+20,b['y']+8)))
        cell=self.prepare_reference();self.tap(cell);slot=next(a for a in self.window()['slots'] if not a['player'] and a['x']>=0)
        self.tap_native(slot['x']+8,slot['y']+8)
        self.check('toolbox-insert',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),64))
        self.tap(self.native_point(slot['x']+8,slot['y']+8),button=1);self.tap(cell)
        self.check('toolbox-split-withdraw',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),32))
        self.tap_native(120,78)
        self.check('toolbox-native-deposit',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),64))
        self.capture('toolbox-filled');self.close_native(194,156);self.call('open-ray',ticks=7)
        self.check('toolbox-reopen',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),64))
        self.small('toolbox',188,171,(194,156))
    def table_frame(self):
        self.open_block('create:schematic_table');self.native_frame('table',214,85);self.capture('table-empty')
        prepare_schematics(Path(json.loads((self.output/'launch.json').read_text())['cwd']))
        self.tap_native(215,32)
        self.check('table-exterior-refresh',lambda:self.assert_equal(any(a['visible'] and a['x']-self.window()['nativeBounds']['x']==45 and a['width']==139 for a in self.window()['nativeWidgets']),True))
        self.capture('table-refreshed');self.call('scroll',**self.native_point(100,32),amount=-1,ticks=3)
        self.check('table-selector-wheel',lambda:self.assert_equal(any(r['id']=='native-refresh' and r['interactive'] and r['exterior'] for r in self.window()['composition']['regions']),True))
        cell=self.prepare_reference('create:empty_schematic',1);self.tap(cell);self.tap_native(29,67);self.tap_native(53,67);self.call('wait',ticks=65)
        self.check('table-real-upload',lambda:self.assert_equal(self.window()['slots'][1]['item'],'create:schematic'))
        self.capture('table-finished');self.tap_native(174,67);self.tap(self.player_cell(10))
        self.check('table-take-output',lambda:self.assert_equal(self.window()['slots'][1]['count'],0))
        def complete_filename():
            value=self.snapshot('table-output-filename',capture=False)['server']['inventory'][10]['item']
            assert 'File:"siu-create-functional-stone.nbt"' in value,value
        self.check('table-upload-preserves-full-filename',complete_filename)
        self.check('table-wheel-selects-second-file',complete_filename)
        self.table_small()
    def table_small(self):
        # This native screen's checkmark starts an upload, rather than closing.
        self.call('gui-scale',scale=4,width=1280,height=960,ticks=5);b=self.window()['bounds']
        self.check('table-gui4-native-size',lambda:self.assert_equal((b['width'],b['height']),(214,85)))
        self.tap_native(215,32);self.capture('table-gui4');self.tap(self.controls()['CLOSE'])
        self.check('table-gui4-close',lambda:self.assert_equal(self.state['sessions'],0))
        self.call('gui-scale',scale=2,width=1280,height=900,ticks=4)
    def filter_frame(self):
        self.fixture();self.open_item('create:filter');self.native_frame('filter',214,99);self.capture('filter-empty');self.ghost(31,33)
        self.check('filter-reference-owned',lambda:self.assert_contains(self.snapshot('filter-ownership',capture=False)['server']['inventory'][9]['item'],'Count:64b'))
        for name,x,tag in [('deny',27,'Blacklist:1b'),('allow',45,'Blacklist:0b'),('data',69,'RespectNBT:1b'),('ignore-data',87,'RespectNBT:0b')]:
            self.tap_native(x,84);self.close_native(190,84)
            self.check('filter-native-'+name,lambda name=name,tag=tag:self.assert_contains(self.saved_item('filter-'+name),tag))
            self.call('use-item',shift=False,ticks=7)
        self.check('filter-reference-reopen',lambda:self.assert_equal(self.window()['slots'][36]['item'],'minecraft:stone'))
        self.capture('filter-filled');self.tap_native(161,84)
        self.check('filter-native-clear',lambda:self.assert_equal(self.window()['slots'][36]['count'],0))
        self.small('filter',214,99,(190,84))
    def attribute_frame(self):
        self.fixture();self.open_item('create:attribute_filter');self.native_frame('attribute',241,85);self.capture('attribute-empty');self.ghost(24,35)
        self.tap_native(191,35)
        self.check('attribute-native-add',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],1))
        self.call('scroll',**self.native_point(100,35),amount=-1,ticks=2);self.tap_native(209,35)
        self.check('attribute-native-inverted-add',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],2))
        for name,x,mode in [('and',65,1),('deny',83,2),('or',47,0)]:
            self.tap_native(x,70);self.close_native(217,70)
            self.check('attribute-native-'+name,lambda name=name,mode=mode:self.assert_contains(self.saved_item('attribute-'+name),'WhitelistMode:'+str(mode)))
            self.call('use-item',shift=False,ticks=7)
        self.check('attribute-rules-reopen',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],2))
        self.capture('attribute-filled');self.tap_native(188,70)
        self.check('attribute-native-clear',lambda:self.assert_equal(self.window()['templateState']['ruleCount'],0))
        self.small('attribute',241,85,(217,70))
    def blueprint_frame(self):
        self.fixture('minecraft:stone');self.call('command',command='kill @e[type=create:crafting_blueprint]',ticks=2)
        self.call('held',item='create:crafting_blueprint',ticks=2);self.call('use-block-ray',ticks=3);self.call('held',item='minecraft:air',ticks=1)
        self.call('command',command='tp @a 8.5 65 10.5 180 18',ticks=2)
        self.call('aim-entity',entity='create:crafting_blueprint',ticks=1);self.call('use-entity-ray',entity='create:crafting_blueprint',ticks=6);self.window('create:crafting_blueprint')
        self.native_frame('blueprint',179,109);self.capture('blueprint-empty')
        controls=self.controls()
        if 'ELLIPSIS' in controls:
            self.tap(controls['ELLIPSIS']);self.capture('blueprint-title-menu');self.tap(self.controls()['ELLIPSIS'])
        cell=self.prepare_reference('minecraft:oak_planks',64);self.tap(cell)
        for x,y in [(37,29),(55,29),(37,47),(55,47)]:self.tap_native(x,y)
        self.tap(cell);self.call('wait',ticks=4)
        self.check('blueprint-inferred-result',lambda:self.assert_equal(self.window()['slots'][45]['item'],'minecraft:crafting_table'))
        self.check('blueprint-ghost-preserves-stack',lambda:self.assert_contains(self.snapshot('blueprint-ownership',capture=False)['server']['inventory'][9]['item'],'Count:64b'))
        self.capture('blueprint-filled');self.close_native(155,94);self.call('close',ticks=2);self.call('aim-entity',entity='create:crafting_blueprint',ticks=3);self.call('use-entity-ray',entity='create:crafting_blueprint',ticks=8)
        self.check('blueprint-world-craft',lambda:self.assert_equal(sum('id:"minecraft:crafting_table"' in a['item'] for a in self.snapshot('blueprint-crafted',capture=False)['server']['inventory']),1))
        self.call('held',item='create:wrench',ticks=1);self.call('aim-entity',entity='create:crafting_blueprint',ticks=3);self.call('use-entity-ray',entity='create:crafting_blueprint',ticks=6)
        self.check('blueprint-wrench-reopen',lambda:self.window('create:crafting_blueprint'))
        self.tap_native(126,94)
        self.check('blueprint-native-clear',lambda:self.assert_equal(sum(a['count'] for a in self.window()['slots'] if not a['player']),0))
        self.small('blueprint',179,109,(155,94))

def run(output,groups):
    s=NativeFive(output);s.results['scope']='Focused checks for five new native-frame menus only; not the complete Create functional/gameplay suite'
    s.call('command',command='gamemode survival @a',ticks=3);s.call('gui-scale',scale=2,width=1280,height=900,ticks=4)
    if not s.results['groups']:
        s.call('wait',ticks=80);s.call('inventory',ticks=6);s.call('close',ticks=2)
    for group in groups:
        getattr(s,group+'_frame')();s.results['groups'][group]='native-frame geometry, affected native controls and GUI scale 4';s.save()
    s.results['complete']=all(g in s.results['groups'] for g in ['toolbox','table','filter','attribute','blueprint']);s.save()
    latest={c['id']:c for c in s.results['checks']};print(json.dumps({'focused_checks':len(latest),'passed':all(c['passed'] for c in latest.values()),'history_retained':True}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--groups',nargs='+',default=['toolbox','table','filter','attribute','blueprint']);a=p.parse_args();run(a.output,a.groups)
