"""Native film rename input/apply/cancel, without invoking publisher callbacks."""
from pathlib import Path
import copy, hashlib, json, sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send


def without_name(signature):
    """Allow only the intended native name edit; keep every other typed value."""
    try:
        value=json.loads(signature)
    except json.JSONDecodeError:
        value=nbtlib.parse_nbt(signature)
        tag=value.get('tag',{})
        display=tag.get('display',{})
        name=str(display.pop('Name')) if 'Name' in display else None
        if 'display' in tag and not display: del tag['display']
        def typed(v):
            if isinstance(v,dict):return (type(v).__name__,tuple((k,typed(x)) for k,x in sorted(v.items())))
            if isinstance(v,list):return (type(v).__name__,tuple(typed(x) for x in v))
            return (type(v).__name__,str(v))
        return typed(value),name
    value=copy.deepcopy(value)
    components=value.get('components',{})
    name=components.pop('minecraft:custom_name',None)
    return value,name


def plain_name(value):
    if isinstance(value,str):
        try:value=json.loads(value)
        except json.JSONDecodeError:return value
    if isinstance(value,dict):return value.get('text','')+''.join(plain_name(v) for v in value.get('extra',[]))
    return str(value) if value is not None else None


class Film:
    def __init__(self,profile):
        self.profile=Path(profile);self.folder=self.profile/'film-rename-release';self.folder.mkdir()
        self.checks=[];self.state={}
    def act(self,kind='wait',**kw):
        response=send(self.profile,kind,**kw);self.state=response.get('client',{});return response
    def expect(self,name,value,**details):
        self.checks.append(dict(name=name,passed=bool(value),**details))
        (self.folder/'checks.json').write_text(json.dumps(dict(scope='Native film rename only',checks=self.checks),indent=2)+'\n')
        assert value,name
        print('PASS',name,flush=True)
    def snapshot(self,name):
        self.act('regression-state',ticks=3)
        data=json.loads((self.profile/'regression-state.json').read_text())
        (self.folder/(name+'.json')).write_text(json.dumps(data,indent=2)+'\n');return data
    def source(self,data):
        return [m for m in data['menus'] if m.get('menuType')=='exposure:item_rename']
    def held(self,data):
        cells=[v for v in data['inventory'] if v['item']=='exposure:black_and_white_film']
        self.expect('one-native-film-remains-in-player-inventory',len(cells)==1 and cells[0]['count']==1 and data['cursor']['count']==0)
        return cells[0]
    def open(self):
        self.act('regression-use-held-item',shift=False,ticks=20)
        data=self.snapshot('opened-'+str(len(self.checks)));menus=self.source(data)
        self.expect('ordinary-film-use-opens-one-native-rename-menu',len(menus)==1)
        self.session=menus[0]['session']
        self.act('wait',ticks=3)
        windows=self.state.get('windows',self.state.get('allWindows',[]))
        window=next(v for v in windows if v.get('sessionId')==self.session)
        if window.get('locked') in [True,'true']:
            self.act('regression-control',session=self.session,control='LOCK',ticks=4)
        before=window['bounds']
        self.act('regression-move',session=self.session,dx=18,dy=12,ticks=5)
        self.act('wait',ticks=3)
        after=next(v for v in self.state.get('windows',self.state.get('allWindows',[])) if v.get('sessionId')==self.session)
        self.expect('rename-window-moves-before-native-controls-are-tested',after['bounds']!=before)
        return data
    def widgets(self):
        self.act('regression-native-widget-catalog',ticks=3)
        rows=json.loads((self.profile/'regression-native-widget-catalog.json').read_text())
        return [v for v in rows if v.get('source_session')==self.session and v.get('visible',True)]
    def tap_widget(self,widget):
        self.expect('native-widget-is-enabled-and-belongs-to-extraction',widget.get('active',True)
                    and widget.get('source_definition','').endswith('.NativeTemplateWindow'))
        self.act('regression-template-widget-tap',session=self.session,widget_type=widget['type'],
                 message=widget.get('message',''),ordinal=0,ticks=12)
    def type_name(self,name):
        candidates=[v for v in self.widgets() if v['type'].endswith(('.EditBox','.class_342'))]
        self.expect('native-rename-text-field-is-uniquely-observable',len(candidates)==1)
        self.tap_widget(candidates[0]);self.act('key',key=269,ticks=1)
        # Native EditBox checks physical Ctrl state for Ctrl+A on these
        # releases. Use bounded ordinary End/Backspace input instead of
        # pretending that a key-event modifier flag changes GLFW state.
        for _ in range(50):self.act('key',key=259,ticks=0)
        self.act('text',text=name,ticks=8)
    def button(self,message):
        candidates=[v for v in self.widgets() if v.get('message')==message]
        self.expect('native-'+message.lower()+'-button-is-unique',len(candidates)==1)
        self.tap_widget(candidates[0])
    def run(self):
        self.act('close',ticks=3);self.act('command',command='gamemode survival @a',ticks=3)
        self.act('command',command='clear @a',ticks=3);self.act('regression-reset-hand',ticks=3)
        self.act('regression-debug-capture',enabled=True,ticks=2)
        self.act('held',item='exposure:black_and_white_film',ticks=3)
        self.act('regression-seed-player',items=[dict(slot=0,item='exposure:black_and_white_film',count=1,snbt='{siu_regression_marker:42}')],ticks=3)
        original=self.held(self.snapshot('original'))['signature'];self.open()
        self.type_name('Release film proof')
        self.act('capture',capture='release-film-before-apply',ticks=3)
        self.button('Apply')
        saved=self.snapshot('applied');stack=self.held(saved)
        old_data,_=without_name(original);new_data,name=without_name(stack['signature'])
        self.expect('apply-persists-exact-native-name-and-preserves-all-other-data',
                    plain_name(name)=='Release film proof' and new_data==old_data)
        self.expect('native-apply-closes-the-rename-source',not self.source(saved))
        self.open();self.type_name('This rename must be cancelled');self.button('Cancel')
        cancelled=self.snapshot('cancelled')
        self.expect('native-cancel-retains-the-complete-prior-stack',self.held(cancelled)['signature']==stack['signature'])
        self.expect('native-cancel-closes-the-source',not self.source(cancelled))
        self.open();self.type_name('x'*60);self.button('Apply')
        bounded=self.snapshot('bounded');final=self.held(bounded)
        data,name=without_name(final['signature'])
        self.expect('native-fifty-character-limit-and-other-components-are-preserved',plain_name(name)=='x'*50 and data==old_data)
        self.act('close',ticks=3)
        result=dict(status='passed',minecraft=None,scope='Named film rename focus/apply/cancel/length and complete component checks on a moved SIU window; not every Exposure feature.',
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),checks=self.checks)
        (self.folder/'summary.json').write_text(json.dumps(result,indent=2)+'\n')
        return dict(cases=1,failed=0,checks=len(self.checks),film_rename_checked=True)


def run(profile,*args,**kwargs):return Film(profile).run()
