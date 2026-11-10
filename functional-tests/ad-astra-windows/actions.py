"""Exercise native action families and real item clicks in the changed windows."""
from pathlib import Path
import argparse,json,re
from focused import Check

def spec(c,id):return next(s for s in c.specs if s['id']==id)
def cells(c,s):return c.inspect()['menus'][0]['slots']
def slot(c,s,index):
    w=c.window(s);b=next(s['bounds']for s in w['nativeSlots']if s['menuSlotId']==index)
    c.tap(c.point(s,b['x']+8,b['y']+8))
def inventory(c,index=9):
    if not any(w['kind']=='INVENTORY'for w in c.state.get('workspaceWindows',[])):c.call('inventory',ticks=5)
    w=next(w for w in c.state['workspaceWindows']if w['kind']=='INVENTORY')
    cell=next(x for x in w['inventoryCells']if x['inventorySlot']==index);c.tap(dict(x=cell['x'],y=cell['y']))
def main(c):
    s=spec(c,'ad-astra-coal-generator');c.open(s)
    c.cmd('item replace entity @a inventory.0 with minecraft:coal 3');inventory(c);slot(c,s,1)
    state=c.inspect();fuel=next(x for x in state['menus'][0]['slots']if x['index']==1)
    c.check('coal-real-fuel-insertion',fuel['item']=='minecraft:coal'and fuel['count']in[2,3],state)
    c.call('wait',ticks=25);c.capture(s,'-working')
    state=c.inspect();burn=re.search(r'CookTime:(\d+)',state['menus'][0]['nbt']);c.check('coal-native-generation-active',burn and int(burn[1])>0,state)
    slot(c,s,1);state=c.inspect();c.check('coal-real-fuel-withdrawal',next(x['count']for x in state['menus'][0]['slots']if x['index']==1)==0,state)
    inventory(c)
    s=spec(c,'ad-astra-nasa-workbench');c.open(s)
    ingredients=['rocket_nose_cone']+['steel_block']*6+['rocket_fin','steel_tank','steel_tank','rocket_fin','rocket_fin','steel_engine','rocket_fin']
    for i,item in enumerate(ingredients):c.call('ad-astra-menu-fill',menu='NasaWorkbenchMenu',slot=i,item='ad_astra:'+item,count=1,ticks=2)
    # Trigger the same native container-change notification as a player's last
    # ingredient placement; the setup writer deliberately bypasses menu.clicked.
    slot(c,s,13);slot(c,s,13);c.call('wait',ticks=5)
    for attempt in range(10):
        c.call('wait',ticks=20);state=c.inspect();result=next(x for x in state['menus'][0]['slots']if x['index']==14)
        if result['item']=='ad_astra:tier_1_rocket':break
    c.check('nasa-native-recipe-preview',result['item']=='ad_astra:tier_1_rocket',state)
    c.capture(s,'-recipe-ready');slot(c,s,14);state=c.inspect()
    c.check('nasa-original-click-to-craft',all(x['count']==0 for x in state['menus'][0]['slots']if not x['player']),state)
    s=spec(c,'ad-astra-giselle-addon-fuel-loader');c.open(s)
    # The +/- graphics denote fluid-container input/output slots, not buttons.
    c.cmd('item replace entity @a inventory.0 with ad_astra:fuel_bucket');inventory(c);slot(c,s,0)
    c.call('wait',ticks=30);after=c.inspect()
    c.check('giselle-native-fuel-container-insertion','ad_astra:fuel' in after['menus'][0]['nbt'],after)
    c.capture(s,'-supplied')
    s=spec(c,'ad-astra-giselle-addon-gravity-normalizer');c.open(s);before=c.inspect();c.tap(c.widget(s,'+'));after=c.inspect()
    c.check('giselle-native-gravity-dimension',before!=after,after);c.tap(c.widget(s,'Move to BottomCenter'));center=c.inspect()
    c.check('giselle-native-gravity-center',after!=center,center);c.capture(s,'-configured')
    s=spec(c,'adastra-gap-gravity-normalizer');c.open(s);before=c.inspect();w=c.window(s)
    slider=next(x for x in w['nativeWidgets']if 'Slider'in x['type']);b=w['nativeBounds']
    c.tap(c.point(s,slider['x']-b['x']+slider['width']*.8,slider['y']-b['y']+slider['height']/2));after=c.inspect()
    c.check('ad-astra-native-gravity-slider',before!=after,after);c.capture(s,'-configured')
    s=spec(c,'ad-astra-giselle-addon-rocket-sensor');c.open(s);before=c.inspect();c.tap(c.widget(s,'Invert Output'));after=c.inspect()
    c.check('giselle-native-sensor-invert',before!=after,after)
    c.tap(c.point(s,80,48));selected=c.inspect();c.check('giselle-native-sensor-condition',after!=selected,selected);c.capture(s,'-configured')
    c.tap(c.widget(s,'Side Config'));w=c.window(s)
    c.check('giselle-sensor-configuration-drawer',w['templateState']['configuring']and w['templateState']['virtualConfiguration'],w['templateState'])
    c.capture(s,'-side-configuration')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();c=Check(a.run);c.call('wait',ticks=2);main(c)
