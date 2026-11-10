"""Real Chipped picker/catalog/search/preview/craft and source-lifetime checks."""
from pathlib import Path
import copy,hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

STATIONS=[('mason_table','minecraft:stone'),('carpenters_table','minecraft:oak_planks'),
    ('loom_table','minecraft:white_wool'),('glassblower','minecraft:glass'),
    ('botanist_workbench','minecraft:oak_leaves'),('alchemy_bench','minecraft:amethyst_block'),
    ('tinkering_table','minecraft:iron_bars')]


def typed(value):
    if isinstance(value,dict):return (type(value).__name__,tuple((k,typed(v))for k,v in sorted(value.items())))
    if isinstance(value,list):return (type(value).__name__,tuple(typed(v)for v in value))
    return (type(value).__name__,str(value))


def stack(signature,count=None):
    value=nbtlib.parse_nbt(signature)
    if count is not None:
        key='Count' if 'Count' in value else 'count'
        assert key in value,signature
        value[key]=type(value[key])(count)
    return typed(value)


class Chipped:
    def __init__(self,profile,mc):
        self.profile=Path(profile);self.mc=mc;self.out=self.profile/'chipped-release';self.out.mkdir()
        self.state={};self.checks=[];self.completed=[]
    def call(self,kind='wait',**kw):
        self.state=send(self.profile,kind,**kw)['client'];return self.state
    def check(self,name,condition,**details):
        self.checks.append(dict(name=name,passed=bool(condition),**details));self.save()
        assert condition,name
        print('PASS',name,flush=True)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,checks=self.checks,stations=self.completed,
            scope='Seven Chipped stations, real picker/catalog/preview/search/craft, exact native stack conversion, duplicate material handling, moved input and source invalidation. Craft All uses the explicitly attested QA-only synthetic Shift input query; not every decorative variant or physical keyboard hardware.',
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()),indent=2)+'\n')
    def w(self):
        rows=[v for v in self.state['allWindows']if v.get('menu')=='earth.terrarium.chipped.common.menus.WorkbenchMenu']
        assert len(rows)==1,rows
        w=rows[0];assert w['definition']=='com.salts_inventory_update.compat.chipped.ChippedWindow'
        assert 'chipped'in w,w
        return w
    def local(self):return self.w()['chipped']
    def tap(self,x,y):
        self.call('move',x=x,y=y,ticks=1);self.call('click',x=x,y=y,button=0,ticks=1)
        self.call('release',x=x,y=y,button=0,ticks=6)
    def at(self,x,y):
        b=self.w()['bounds'];self.tap(b['x']+x,b['y']+y)
    def server(self,label):
        self.call('snapshot-server',ticks=2);data=json.loads((self.profile/'server-state.json').read_text())
        (self.out/(label+'.json')).write_text(json.dumps(dict(client=self.state,server=data),indent=2)+'\n');return data
    def seed(self,index,item,count,marked=False):
        marker='{release_chipped_marker:42,typed_bytes:[B;3b,5b,8b]}'
        suffix=('{release_chipped_marker:42,typed_bytes:[B;3b,5b,8b]}'if self.mc=='1.20.1'else '[minecraft:custom_data='+marker+']')if marked else ''
        self.call('command',command=f'item replace entity @a container.{index} with {item}{suffix} {count}',ticks=5)
    def choose(self,index):
        local=self.local();self.check('eligible-material-index-'+str(index),index in local['eligible'])
        self.at(230,32);self.check('material-popup-opens',self.local()['picker'])
        row=self.local()['eligible'].index(index)-int(self.local()['pickerScroll']);assert 0<=row<5
        b=self.w()['bounds'];self.tap(b['x']+125,b['y']+43+30+20*row)
        self.check('picker-selects-reference-without-carrying',self.local()['selected']==index and not self.local()['pending'] and not self.local()['picker'])
    def variant(self,source):
        values=self.local()['results'];index=next(i for i,v in enumerate(values[:54])if v['id']!=source)
        self.at(91+(index%9)*18+8,64+(index//9)*18+8)
        chosen=copy.deepcopy(self.local()['chosen'])
        self.check('catalog-selects-the-observed-native-variant',chosen==values[index])
        return chosen
    def search(self,text):
        self.at(116,54);self.call('key',key=269,ticks=0)
        for _ in range(len(self.local()['search'])):self.call('key',key=259,ticks=0)
        self.check('search-clears-through-ordinary-backspace-input',self.local()['search']=='')
        if text:self.call('text',text=text,ticks=5)
        self.check('native-search-text-focus',self.local()['search']==text and self.local()['searchFocused'])
    def move(self):
        w=self.w();b=w['bounds'];info=next(v for v in self.state['workspaceWindows']if v['kind']=='CONTAINER'and v['bounds']==b)
        if info.get('locked')in[True,'true']:self.tap(**info['controls']['LOCK'])
        b=self.w()['bounds'];x,y=b['x']+20,b['y']+8
        self.call('click',x=x,y=y,button=0,ticks=1);self.call('drag',x=x+18,y=y+12,dx=18,dy=12,button=0,ticks=1)
        self.call('release',x=x+18,y=y+12,button=0,ticks=4)
        self.check('custom-header-moves-as-one-window',self.w()['bounds']!=b)
    def run(self):
        self.call('gui-scale',scale=2,width=1280,height=900,ticks=5)
        for station,item in STATIONS:
            self.call('close',ticks=2);self.call('fixture',block='chipped:'+station,ticks=5)
            self.call('command',command='gamemode survival @a',ticks=3)
            self.seed(9,item,16,True);self.seed(10,item,8);self.seed(11,'minecraft:diamond',3,True)
            self.call('open',ticks=18);self.w();before=self.server(station+'-before')
            self.check(station+'-real-station-and-complete-frame',self.w()['bounds']['width']==270 and self.w()['bounds']['height']==181)
            self.choose(9);chosen_before=self.server(station+'-selected')
            self.check(station+'-material-selection-is-reference-only',chosen_before['inventory']==before['inventory']and all(nbtlib.parse_nbt(v['carried'])=={} for v in chosen_before['menus']))
            self.search('unmatchable_release_query');self.check(station+'-no-match-list-is-empty',not self.local()['results'])
            self.at(40,136);no_output=self.server(station+'-no-match')
            self.check(station+'-empty-selection-cannot-craft',no_output['inventory']==before['inventory'])
            self.search('');self.call('key',key=256,ticks=2);chosen=self.variant(item)
            for mode in range(4):
                self.at(19+18*mode,156);self.check(station+'-native-preview-mode-'+str(mode),self.local()['mode']==mode)
            self.move();self.at(40,136);after=self.server(station+'-crafted')
            self.check(station+'-craft-converts-the-complete-selected-native-stack',stack(after['inventory'][9]['item'])==stack(chosen['data'],16))
            self.check(station+'-duplicate-and-unrelated-complete-data-stay-owned',after['inventory'][10:]==before['inventory'][10:])
            self.check(station+'-native-craft-resets-selection',self.local()['selected']==-1 and not self.local()['pending'])
            self.call('capture',capture='release-chipped-'+station.replace('_','-'),ticks=2)
            if station=='mason_table':
                self.seed(12,item,5);self.choose(10);chosen=self.variant(item)
                try:
                    self.call('specialized-shift',down=True,ticks=2)
                    shift=json.loads((self.profile/'specialized-shift-state.json').read_text())
                    self.check('synthetic-shift-fixture-is-attested-before-native-click',shift['matchedQueries']==1 and shift['requested']and shift['observed'])
                    self.at(40,136)
                finally:self.call('specialized-shift',down=False,ticks=2)
                all_result=self.server(station+'-all')
                # Native Craft All converts both matching stacks in their own
                # inventory slots; it is not a single-recipe result-slot move.
                self.check('native-shift-craft-all-converts-both-matching-stacks',
                    stack(all_result['inventory'][10]['item'])==stack(chosen['data'],8)and stack(all_result['inventory'][12]['item'])==stack(chosen['data'],5))
                self.check('craft-all-preserves-unrelated-typed-components',all_result['inventory'][11]==before['inventory'][11])
            self.call('command',command='setblock 8 65 8 minecraft:air',ticks=12)
            self.check(station+'-native-source-invalidation-closes-owner',not any('chipped'in v.get('menu','').lower()for v in self.state.get('allWindows',[])))
            self.completed.append(station);self.save()
        self.call('close',ticks=2);self.save(True)
        return dict(cases=len(STATIONS),failed=0,checks=len(self.checks),chipped_features_checked=True,stations=self.completed)


def run(profile,mc,loader,*args,**kwargs):return Chipped(profile,mc).run()
