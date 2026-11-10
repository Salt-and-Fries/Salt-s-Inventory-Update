"""Schematicannon-only frame/input acceptance; not the complete Create suite."""
from pathlib import Path
import argparse,json,re
from fixtures import prepare_schematics,deployed_schematic
from suite import Suite

def run(output):
    s=Suite(output)
    s.results['scope']='Focused Schematicannon native-frame geometry, management and native controls; not the complete functional/gameplay suite'
    s.call('command',command='gamemode survival @a',ticks=2)
    s.call('gui-scale',scale=2,width=1280,height=900,ticks=3)
    s.open_block('create:schematicannon')
    s.call('move',x=2,y=2,ticks=3)
    s.snapshot('native-frame-idle')
    s.check('native-frame-size',lambda:s.assert_equal(s.window()['bounds']['width'],213))
    s.check('native-frame-height',lambda:s.assert_equal(s.window()['bounds']['height'],143))
    def title_controls():
        b=s.window()['bounds']
        for k,p in s.controls().items():
            # Observer points are left+1 at window y+9, not rectangle centers.
            assert b['x']+1<=p['x']-1 and p['x']+10<=b['x']+204,(k,p,b)
            assert b['y']<p['y']<b['y']+15,(k,p,b)
    s.check('controls-inside-native-title',title_controls)
    def info():return next(w for w in s.state['workspaceWindows'] if w['kind']=='CONTAINER')
    if info()['locked']=='true':s.tap(s.controls()['LOCK'])
    old=s.window()['bounds'];start={'x':old['x']+16,'y':old['y']+7};end={'x':start['x']+24,'y':start['y']+12}
    s.call('click',**start,button=0,ticks=1)
    s.call('drag',**end,button=0,dx=24,dy=12,ticks=1)
    s.call('release',**end,button=0,ticks=1)
    s.check('drag-native-title',lambda:s.assert_equal((s.window()['bounds']['x'],s.window()['bounds']['y']),(old['x']+24,old['y']+12)))
    initial=info()['pinMode'];s.tap(s.controls()['PIN'])
    s.check('pin-native-title',lambda:s.assert_equal(info()['pinMode']!=initial,True))
    s.tap(s.controls()['PIN'])
    focused=info()['focused'];s.tap(s.controls()['FOCUS'])
    s.check('focus-native-title',lambda:s.assert_equal(info()['focused']!=focused,True))
    s.tap(s.controls()['FOCUS']);s.tap(s.controls()['LOCK'])
    s.check('lock-native-title',lambda:s.assert_equal(info()['locked'],'true'))
    s.tap_native(17,120)
    s.call('move',x=2,y=2,ticks=3);s.snapshot('native-frame-settings')
    for mode in range(4):
        s.tap_native(42+18*mode,120)
        s.check('native-replacement-'+str(mode),lambda mode=mode:s.assert_contains(s.snapshot('native-mode-'+str(mode),capture=False)['server']['fixture'],'ReplaceMode:'+str(mode)))
    s.tap_native(120,120);s.tap_native(144,120)
    s.check('native-skip-missing',lambda:s.assert_contains(s.snapshot('native-options',capture=False)['server']['fixture'],'SkipMissing:1b'))
    s.check('native-toggle-block-entities',lambda:s.assert_contains(s.snapshot('native-protection',capture=False)['server']['fixture'],'ReplaceTileEntities:1b'))
    s.tap_native(78,120);s.tap_native(17,120)
    runtime=Path(json.loads((output/'launch.json').read_text())['cwd']);prepare_schematics(runtime)
    s.call('command',command='setblock 7 65 8 minecraft:chest{Items:[{Slot:0b,id:"minecraft:stone",Count:64b}]}',ticks=2)
    s.call('command',command='fill 12 65 4 12 65 19 minecraft:air',ticks=1)
    menu=s.window()['menu']
    for slot,item,count,tag in [(0,'create:schematic',1,deployed_schematic()),(2,'minecraft:book',1,None),(4,'minecraft:gunpowder',4,None)]:
        args={'menu':menu,'slot':slot,'item':item,'count':count}
        if tag:args['snbt']=tag
        s.call('reviewed-session-seed',**args,ticks=5)
    s.tap_native(84,94);s.call('wait',ticks=35)
    s.check('native-play',lambda:s.assert_contains(s.snapshot('native-frame-running')['server']['fixture'],'State:"RUNNING"'))
    s.tap_native(102,94);s.call('wait',ticks=15)
    paused=s.snapshot('native-frame-paused')['server']['fixture'];placed=re.search(r'AmountPlaced:(\d+)',paused).group(1)
    s.call('wait',ticks=20)
    s.check('native-pause',lambda:s.assert_contains(s.snapshot('native-paused-stable',capture=False)['server']['fixture'],'AmountPlaced:'+placed))
    s.tap_native(120,94)
    s.check('native-stop',lambda:s.assert_contains(s.snapshot('native-stopped',capture=False)['server']['fixture'],'AmountPlaced:0'))
    s.tap_native(84,94);s.call('wait',ticks=200)
    s.check('native-placement',lambda:s.assert_contains(s.block_probe('native-last-block',12,65,19)['block'],'minecraft:stone'))
    s.check('native-materials',lambda:s.assert_contains(s.block_probe('native-chest',7,65,8)['nbt'],'Count:48b'))
    s.check('native-checklist',lambda:s.assert_equal(s.window()['slots'][3]['item'],'minecraft:written_book'))
    s.tap_native(189,120)
    s.check('native-footer-confirm',lambda:s.assert_equal(s.state['sessions'],0))
    s.call('open-ray',ticks=8)
    s.tap(s.controls()['LINK']);s.call('move',x=2,y=2,ticks=1);s.snapshot('native-frame-link-selection');s.tap(s.controls()['LINK'])
    s.tap(s.controls()['CLOSE'])
    s.check('native-title-close',lambda:s.assert_equal(s.state['sessions'],0))
    s.call('open-ray',ticks=8)
    s.call('gui-scale',scale=4,width=1280,height=960,ticks=4)
    b=s.window()['bounds']
    s.check('small-gui-original-size',lambda:s.assert_equal((b['width'],b['height']),(213,143)))
    s.check('small-gui-title-controls',title_controls)
    s.check('small-gui-fits',lambda:s.assert_equal(b['x']>=0 and b['y']>=0 and b['x']+313<=s.state['guiWidth'] and b['y']+172<=s.state['guiHeight'],True))
    s.call('move',x=2,y=2,ticks=2);s.snapshot('native-frame-gui4')
    s.tap_native(17,120);s.tap_native(42,120);s.tap_native(17,120);s.tap_native(189,120)
    s.check('small-gui-native-footer',lambda:s.assert_equal(s.state['sessions'],0))
    s.call('gui-scale',scale=2,width=1280,height=900,ticks=3)
    s.open_block('create:schematicannon');s.call('move',x=2,y=2,ticks=3);s.snapshot('native-frame-final')
    s.results['groups']['schematicannon_frame']='native title controls and dragging, inline settings, playback and world placement, native footer/close, minimum GUI viewport'
    s.results['complete']=True;s.save()
    latest={c['id']:c for c in s.results['checks']}
    print(json.dumps({'focused_checks':len(latest),'passed':all(c['passed'] for c in latest.values()),'history_retained':True}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args();run(a.output)
