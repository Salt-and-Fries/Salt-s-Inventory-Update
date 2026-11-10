"""Read-only observations and ordinary UI input; no forced screen construction."""
from pathlib import Path
import hashlib,json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

class Check:
    def __init__(self,run,mod):
        self.run=ROOT/'build/remaining-ui-audit/gameplay'/run
        self.mod=mod;self.out=ROOT/'output/native-screen-preservation'/mod;self.out.mkdir(parents=True,exist_ok=True)
        path=self.out/'checks.json'
        self.data=json.loads(path.read_text())if path.exists()else{'mod':mod,'runtime':str(self.run.relative_to(ROOT)),'checks':[],'captures':[],'observations':[],'status':'running'}
    def save(self):
        (self.out/'checks.json').write_text(json.dumps(self.data,indent=2)+'\n')
    def call(self,kind,**kwargs):
        reply=send(self.run,kind,**kwargs);self.state=reply['client']
        self.data['observations'].append({'command':{'type':kind,**kwargs},'state':self.state});self.save();return self.state
    def check(self,name,condition):
        self.data['checks'].append({'name':name,'passed':bool(condition)});self.save();assert condition,name
        print('PASS',self.mod,name,flush=True)
    def native(self,expected):
        s=self.call('wait',ticks=3)
        self.check('Original '+expected+' owns screen',s['screen'].endswith(expected))
        self.check('No SIU windows or sessions',s.get('sessions',0)==0 and not s.get('allWindows')and not s.get('workspaceWindows'))
        return s
    def widget(self,label):
        return next(w for w in self.state['widgets']if w['message']==label and w['visible'])
    def tap(self,x,y):
        self.call('move',x=x,y=y,ticks=1);self.call('click',x=x,y=y,button=0,ticks=1);return self.call('release',x=x,y=y,button=0,ticks=7)
    def press(self,label):
        w=self.widget(label);assert w['active'],w
        return self.tap(w['x']+w['width']/2,w['y']+w['height']/2)
    def capture(self,name):
        s=self.call('capture',capture=name,ticks=5);p=self.run/'screenshots'/(name+'.png')
        self.data['captures'].append({'file':str(p.relative_to(ROOT)),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'state':s});self.save()
    def escape(self):
        s=self.call('key',key=256,ticks=8)
        self.check('Escape returns to game',s['screen']=='none')
    def finish(self):
        self.data['status']='passed';self.save()
