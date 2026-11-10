"""Focused original-panel and native conversion checks, not a gameplay suite."""
from pathlib import Path
import sys,json,os,re,math
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
RUN=ROOT/'build/remaining-ui-audit/gameplay'/os.environ.get('ALEX_RUN','1.20.1-forge-integrated-alex-transmutation02')
OUT=ROOT/'build/alexsmobs-transmutation'/os.environ.get('ALEX_EVIDENCE','evidence02');OUT.mkdir(parents=True,exist_ok=True)

def act(kind='capture',**kw):return send(RUN,kind,ticks=kw.pop('ticks',5),**kw)['client']
def cmd(command,**kw):return act('command',command=command,**kw)
def click(x,y,button=0):
    act('click',x=x,y=y,button=button,ticks=1);return act('release',x=x,y=y,button=button)
def table(s=None):return next(w for w in (s or act())['allWindows'] if w.get('menuId')=='alexsmobs:transmutation_table')
def workspace(w,s=None):return next(x for x in (s or act())['workspaceWindows'] if x['bounds']==w['bounds'])
def check(name,passed,details=None):
    p=OUT/'checks.json';rows=json.loads(p.read_text()) if p.exists() else [];rows.append({'name':name,'passed':bool(passed),'details':details});p.write_text(json.dumps(rows,indent=2));print(('PASS ' if passed else 'FAIL ')+name,flush=True);assert passed,(name,details)
def snapshot(name=None):
    act('snapshot-server',ticks=1);s=json.loads((RUN/'server-state.json').read_text());act('table-state',ticks=1);s['table']=json.loads((RUN/'table-state.json').read_text())
    if name:(OUT/(name+'.json')).write_text(json.dumps({'server':s,'client':act()},indent=2))
    return s
def input_stack(s=None):return next(m for m in (s or snapshot())['menus'] if m['class'].endswith('.MenuTransmutationTable'))['slots'][0]['item']
def item_id(nbt):return re.search(r'id:"([^"]+)"',nbt).group(1)
def amount(nbt):return int(re.search(r'Count:(\d+)b',nbt).group(1))
def inventory(index):
    w=next(w for w in act()['workspaceWindows'] if w['kind']=='INVENTORY');p=next(p for p in w['inventoryCells'] if p['inventorySlot']==index);return click(p['x'],p['y'])
def input_click():
    w=table();c=w['transmutationContent'];r=w['composition']['regions'][0];return click(c['x']+91-r['source']['x']+r['x'],c['y']+91-r['source']['y']+r['y'])
def choice(index):
    w=table();c=w['transmutationContent'];r=w['composition']['regions'][0];return click(c['x']+85-r['source']['x']+r['x'],c['y']+25+19*index-r['source']['y']+r['y'])
def capture(name):
    s=act(capture=name);(OUT/(name+'.json')).write_text(json.dumps(s,indent=2));return s
def fill():
    cmd('item replace entity @a inventory.0 with minecraft:stone 16');inventory(9);input_click();act(ticks=12)

cmd('gamemode survival @a');cmd('experience set @a 30 levels');act('fixture',block='alexsmobs:transmutation_table',ticks=12);act('open',ticks=30)
s=act();w=table(s)
check('explicit native table template selected',w.get('template','').endswith('TransmutationWindows$Template'),w.get('template'))
check('native panel is the complete 176x107 window without an added strip',w['bounds']['width']==176 and w['bounds']['height']==107 and w['transmutationContent']['y']==w['bounds']['y']+16,w['bounds'])
check('native body follows its own header and omits player inventory',len([v for v in w['slots'] if not v['player']])==1 and w['composition']['regions'][0]['source']=={'x':0,'y':16,'width':176,'height':91},w['composition'])
check('management controls fit inside original native title row',all(w['bounds']['y']<=v['y']<w['bounds']['y']+16 for v in workspace(w,s)['controls'].values()))
capture('transmutation-empty-original-panel')

act('inventory');iw=next(w for w in act()['workspaceWindows'] if w['kind']=='INVENTORY')
if iw['locked']=='true':click(**iw['controls']['LOCK'])
iw=next(w for w in act()['workspaceWindows'] if w['kind']=='INVENTORY');b=iw['bounds'];x=b['x']+18;y=b['y']+8;dx=230-b['x'];dy=320-b['y']
act('click',x=x,y=y,button=0,ticks=1);act('drag',x=x+dx,y=y+dy,dx=dx,dy=dy,button=0,ticks=1);act('release',x=x+dx,y=y+dy,button=0)
fill();s=snapshot('native-input-inserted')
check('input accepted through separate SIU Inventory',item_id(input_stack(s))=='minecraft:stone' and amount(input_stack(s))==16)
check('all three native previews synchronized to owning window',table()['transmutationChoices']==s['table']['choices'] and all(item_id(v)!='minecraft:air' for v in s['table']['choices']))
check('all three original choice buttons visible',sum(v.get('visible',False) for v in table()['nativeWidgets'] if v['type'].endswith('ButtonTransmute'))==3)

tw=workspace(table())
if tw['locked']=='true':
    click(**tw['controls']['ELLIPSIS']);tw=workspace(table());click(**tw['popupControls']['LOCK'])
w=table();b=w['bounds'];x=b['x']+12;y=b['y']+7
act('click',x=x,y=y,button=0,ticks=1);act('drag',x=x+31,y=y+31,dx=31,dy=31,button=0,ticks=1);act('release',x=x+31,y=y+31,button=0)
check('original native title row drags the whole panel and input',table()['bounds']['x']==b['x']+31 and table()['bounds']['y']==b['y']+31)

for index in range(3):
    if index:
        input_click();inventory(10+index);fill()
    before=snapshot();expected=before['table']['choices'][index];cost=table()['transmutationCost'];choice(index);act(ticks=15);after=snapshot('choice-'+str(index))
    check('choice '+str(index)+' produces native selected item',item_id(input_stack(after))==item_id(expected),{'expected':expected,'actual':input_stack(after)})
    check('choice '+str(index)+' charges original XP cost',after['table']['xpLevel']==before['table']['xpLevel']-cost,{'before':before['table']['xpLevel'],'after':after['table']['xpLevel'],'cost':cost})
    check('choice '+str(index)+' retains native learning and reroll feedback','TotalCount:'+str(16*(index+1)) in after['table']['nbt'] and table()['transmutationChoices']==after['table']['choices'])

input_click();inventory(14);fill();cmd('experience set @a 0 levels');before=snapshot();choice(0);after=snapshot()
check('insufficient XP leaves native input and level unchanged',input_stack(before)==input_stack(after) and after['table']['xpLevel']==0)
capture('transmutation-insufficient-xp')
cmd('experience set @a 30 levels');capture('transmutation-native-panel-with-inventory')
act('gui-scale',scale=3,ticks=15);check('native panel retains geometry at GUI scale 3',table()['bounds']['width']==176 and table()['bounds']['height']==107);capture('transmutation-native-panel-scale3')
act('gui-scale',scale=2,ticks=15)
before=snapshot();held=input_stack(before);tw=workspace(table());click(**tw['controls']['CLOSE']);act('snapshot-server',ticks=1);after=json.loads((RUN/'server-state.json').read_text())
check('parent close returns its native input to player inventory',not any(w.get('menuId')=='alexsmobs:transmutation_table' for w in act()['allWindows']) and sum(amount(v['item']) for v in after['inventory'] if item_id(v['item'])==item_id(held))>=amount(held))
act('close');act('open',ticks=25);check('ordinary block reopen restores original table panel',table()['bounds']['height']==107 and table()['slots'][0]['count']==0)
act('inventory');fill();capture('transmutation-final-fullscreen')
print('Focused Transmutation Table checks completed.',flush=True)
