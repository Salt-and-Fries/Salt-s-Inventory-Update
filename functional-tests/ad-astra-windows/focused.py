"""Focused affected-window acceptance; never a complete gameplay suite."""
from pathlib import Path
import argparse,json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

class Check:
    def __init__(self,run):
        self.run=run.resolve();self.out=ROOT/'output/ad-astra-implemented'/run.name;self.out.mkdir(exist_ok=True)
        self.path=self.out/'journal.json';self.data=json.loads(self.path.read_text())if self.path.exists()else dict(scope='Focused Ad Astra and Giselle windows',checks=[],captures=[])
        self.specs=json.loads((ROOT/f'compatibility/versions/{run.name.split("-")[0]}/common/src/main/resources/salts_inventory_compat/ad-astra/native-windows.json').read_text())
        self.state={}
    def save(self):self.path.write_text(json.dumps(self.data,indent=2),encoding='utf-8')
    def log(self):
        path=self.run/'launch.log'
        return (path if path.exists()else ROOT/'build/ad-astra-implementation/neoforge.log').read_text(errors='replace')
    def call(self,kind,**kw):self.state=send(self.run,kind,**kw)['client'];return self.state
    def cmd(self,text):return self.call('command',command=text,ticks=4)
    def check(self,id,ok,detail=None):
        self.data['checks']=[c for c in self.data['checks']if c['id']!=id]+[dict(id=id,passed=bool(ok),detail=detail)];self.save()
        assert ok,(id,detail)
        print('PASS',id,flush=True)
    def window(self,spec):return next(w for w in self.state.get('allWindows',[])if w.get('menuId')==spec['menu'])
    def point(self,spec,x,y):
        w=self.window(spec);b=w['bounds']
        for r in w['composition']['regions']:
            a=r['source']
            if a['x']<=x<a['x']+a['width']and a['y']<=y<a['y']+a['height']:
                return dict(x=b['x']+r['x']+x-a['x'],y=b['y']+(0 if r.get('exterior')else spec['header'])+r['y']+y-a['y'])
        raise AssertionError((spec['menu'],x,y))
    def tap(self,p,button=0):
        self.call('move',**p,ticks=1);self.call('click',**p,button=button,ticks=1);self.call('release',**p,button=button,ticks=5)
    def open(self,spec):
        self.call('close');self.cmd('kill @e[type=!minecraft:player]');self.cmd('gamemode survival @a')
        if spec['vehicle']:
            self.call('fixture',block='minecraft:air',ticks=4)
            kind=spec['menu'].split(':')[1].removesuffix('_menu');entity='ad_astra:'+('tier_1_'+kind if kind!='lander'else kind)
            self.cmd('summon '+entity+' 8.5 65 8.5 {NoGravity:1b,Invulnerable:1b,OnGround:1b}')
            self.call('sneak',down=True,ticks=4)
            self.call('use-entity',entity=entity,shift=True,ticks=10)
            self.call('sneak',down=False,ticks=3)
        else:
            block=spec['menu'].removesuffix('_menu')if spec['menu'].startswith('ad_astra:')else spec['menu']
            self.call('fixture',block=block,ticks=4);self.call('open',ticks=10)
    def capture(self,spec,suffix=''):
        self.call('move',x=2,y=2,ticks=1);name=spec['id']+suffix;self.call('capture',capture=name,ticks=3)
        w=self.window(spec);(self.out/(name+'.json')).write_text(json.dumps(self.state,indent=2),encoding='utf-8')
        self.data['captures']=[c for c in self.data['captures']if c['id']!=name]+[dict(id=name,path=str((self.run/'screenshots'/(name+'.png')).relative_to(ROOT)),bounds=w['bounds'],composition=w['composition'])];self.save()
    def geometry(self,only=None):
        for spec in self.specs:
            if only and only not in spec['id']:continue
            self.open(spec);w=self.window(spec);name=spec['id'];plan=w['composition']
            self.check(name+'-native-template',w.get('templateClass')=='com.salts_inventory_update.compat.adastra.AdAstraClient$Template'and not w.get('layoutError'),w.get('layoutError'))
            self.check(name+'-approved-size',[w['bounds']['width'],w['bounds']['height']]==[spec['width'],spec['height']],w['bounds'])
            placements=[]
            for slot in w['nativeSlots']:
                b=slot['bounds'];matches=[]
                for r in plan['regions']:
                    a=r['source']
                    if a['x']<=b['x']and a['y']<=b['y']and b['x']+b['width']<=a['x']+a['width']and b['y']+b['height']<=a['y']+a['height']:
                        if not any(h['x']<b['x']+b['width']and b['x']<h['x']+h['width']and h['y']<b['y']+b['height']and b['y']<h['y']+h['height']for h in r['cutouts']):matches.append(r)
                if slot['player']:assert not matches,(name,'embedded player slot',slot)
                else:assert len(matches)==1,(name,slot,matches)
                if matches:placements.append(slot['menuSlotId'])
            self.check(name+'-complete-native-slots',len(placements)==len([s for s in w['nativeSlots']if not s['player']]),placements)
            self.capture(spec)
    def inspect(self):
        self.call('ad-astra-state');state=json.loads((self.run/'ad-astra-state.json').read_text())
        for row in state['menus']:row.pop('worldTime',None)
        return state
    def widget(self,spec,message):
        w=self.window(spec);a=w['nativeBounds']
        widget=next(w for w in w['nativeWidgets']if (message in w.get('message','')or any(message.lower()in str(r).lower()for r in w.get('imageResources',[])))and w.get('active',True)and w.get('visible',True))
        return self.point(spec,widget['x']-a['x']+widget['width']/2,widget['y']-a['y']+widget['height']/2)
    def configuration(self):
        spec=next(s for s in self.specs if s['id']=='ad-astra-coal-generator');self.open(spec)
        before=self.inspect();self.tap(self.widget(spec,'Redstone Control'));after=self.inspect()
        self.check('coal-native-redstone-action',before!=after,after)
        self.tap(self.widget(spec,'Side Config'));w=self.window(spec)
        self.check('coal-configuration-drawer-opens',w['templateState']['configuring']and any(r['id']=='native-side-configuration'for r in w['composition']['regions']),w['templateState'])
        self.tap(self.point(spec,152,55));self.capture(spec,'-energy-configuration')
        before=self.inspect()
        w=self.window(spec);a=w['nativeBounds'];button=next(b for b in w['nativeWidgets']if b['type'].endswith('.ConfigurationButton')and b.get('visible',True)and b.get('active',True))
        self.tap(self.point(spec,button['x']-a['x']+button['width']/2,button['y']-a['y']+button['height']/2));after=self.inspect()
        self.check('coal-native-energy-side-action',before!=after,after)
        self.tap(self.widget(spec,'Reset'));reset=self.inspect()
        self.check('coal-native-configuration-reset',reset!=after,reset)
        self.tap(self.widget(spec,'Side Config'));w=self.window(spec)
        self.check('coal-configuration-drawer-closes',not w['templateState']['configuring'])

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--only');p.add_argument('--configuration',action='store_true');a=p.parse_args();c=Check(a.run);c.call('wait',ticks=3)
    if a.configuration:c.configuration()
    else:c.geometry(a.only)
