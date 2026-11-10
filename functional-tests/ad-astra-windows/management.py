"""Focused custom chrome, attached drawer and native cargo permission checks."""
from pathlib import Path
import argparse
from focused import Check
from actions import spec,slot,inventory

def main(c):
 s=spec(c,'ad-astra-coal-generator');c.open(s)
 def info():return next(w for w in c.state['workspaceWindows']if w['kind']=='CONTAINER')
 def control(name):
  if name not in info()['controls']:
   if not info().get('popupControls'):c.tap(info()['controls']['ELLIPSIS'])
   c.tap(info()['popupControls'][name])
  else:c.tap(info()['controls'][name])
 if info()['locked']=='true':control('LOCK')
 old=info()['bounds'];start=dict(x=old['x']+12,y=old['y']+9);end=dict(x=start['x']+22,y=start['y']+18)
 c.call('click',**start,button=0,ticks=1);c.call('drag',**end,button=0,dx=22,dy=18,ticks=1);c.call('release',**end,button=0,ticks=2)
 c.check('native-frame-drag',info()['bounds']['x']==old['x']+22,info()['bounds'])
 before=info()['focused'];control('FOCUS');c.check('native-frame-focus',info()['focused']!=before)
 before=info()['pinMode'];control('PIN');c.check('native-frame-pin',info()['pinMode']!=before);control('PIN');c.check('native-frame-unpin',info()['pinMode']==before)
 control('LOCK');c.check('native-frame-lock',info()['locked']=='true');control('LOCK');c.check('native-frame-unlock',info()['locked']=='false')
 c.tap(c.widget(s,'Side Config'));c.check('drawer-has-native-six-directions',len([w for w in c.window(s)['nativeWidgets']if w['type'].endswith('.ConfigurationButton')])==6)
 c.capture(s,'-drawer-and-inventory')
 control('CLOSE');c.check('native-frame-close-session',not c.state['allWindows'])
 s=spec(c,'own-adastra-tier-1-rover');c.open(s);c.cmd('item replace entity @a inventory.0 with minecraft:stone 3');inventory(c);slot(c,s,2)
 st=c.inspect();c.check('rover-real-cargo-insertion',st['menus'][0]['slots'][2]['count']==3,st['menus'][0]['slots'][:4]);slot(c,s,2);inventory(c)
 c.check('rover-real-cargo-withdrawal',c.inspect()['menus'][0]['slots'][2]['count']==0)
 for i,item,count in [(0,'minecraft:stone',3),(1,'minecraft:stone',2),(17,'minecraft:bucket',1)]:c.call('ad-astra-menu-fill',menu='RoverMenu',slot=i,item=item,count=count,ticks=2)
 control('SORT_INVENTORY');st=c.inspect()['menus'][0]['slots']
 c.check('rover-sort-combines-first-two-cargo-cells',st[0]['item']=='minecraft:stone'and st[0]['count']==5 and st[1]['count']==0,st[:2])
 c.check('rover-sort-preserves-native-fuel-return',st[17]['item']=='minecraft:bucket'and st[17]['count']==1,st[16:18])
 c.check('rover-cargo-only-storage-classification',[x['index']for x in st if x['storage']]==list(range(16)))
 c.capture(s,'-with-player-inventory')
 s=spec(c,'own-adastra-lander');c.open(s);c.cmd('item replace entity @a inventory.0 with minecraft:stone 3');inventory(c);slot(c,s,0)
 c.check('lander-native-rejects-insertion',c.inspect()['menus'][0]['slots'][0]['count']==0);inventory(c)
 c.call('ad-astra-menu-fill',menu='LanderMenu',slot=0,item='minecraft:apple',count=2,ticks=3);slot(c,s,0)
 c.check('lander-native-recovery-extraction',c.inspect()['menus'][0]['slots'][0]['count']==0);inventory(c)
 c.check('lander-no-deposit-or-sort-storage',not any(x['storage']for x in c.inspect()['menus'][0]['slots']))
 c.check('no-native-template-callback-failures','native template callback failed'not in c.log())

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();c=Check(a.run);c.call('wait',ticks=2);main(c)
