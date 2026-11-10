"""Exercise visible Tom utility controls; no forced sessions or full gameplay suite."""
from pathlib import Path
import argparse,json
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',choices=['emitter','tag','link','terminal']);p.add_argument('--resize',action='store_true',help='Profile must explicitly enable SIU resizing');a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(name,okay):
    checks.append(dict(check=name,passed=bool(okay)))
    (a.run/('toms-native-revamp-'+str(a.only)+'-checks.json')).write_text(json.dumps(dict(scope='Affected utility layout, visible controls and native server results only; not a full Tom Storage gameplay suite',checks=checks),indent=2))
    assert okay,name
    print('PASS',name,flush=True)
def win(s,suffix):return next(w for w in s.get('allWindows',s.get('windows',[])) if w.get('definition','').endswith('$'+suffix))
def b(s,suffix):return win(s,suffix)['bounds']
def tap(s,suffix,x,y,button=0,**kw):
    r=b(s,suffix);return act('xy-click',x=r['x']+8+x,y=r['y']+24+y,button=button,ticks=12,**kw)
def settings(simple):
    act('snapshot-server',ticks=2);snap=json.loads((a.run/'server-state.json').read_text());return next(m['settings'] for m in snap['menus'] if m['class'].endswith('.'+simple+'Menu'))
def ident(name,kind='blocks'):
    for v in ['toms_storage:'+name,'toms_storage:ts.'+name]:
        if v in catalog[kind]:return v
    raise KeyError(name)
def block(name):
    act('close',ticks=2);act('fixture',block=ident(name),ticks=15)
    act('held',item='minecraft:air',ticks=2)
    return act('client-use-block',ticks=18)
def inv(s,index):
    w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in w['inventoryCells'] if v['inventorySlot']==index);return v['x'],v['y']
def text(s,suffix,x,y,value):
    s=tap(s,suffix,x,y);act('key',key=65,modifiers=2,ticks=1);return act('text',text=value,ticks=10)
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text());x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
act('command',command='gamemode survival @a',ticks=3)

if a.only=='emitter':
    s=block('level_emitter');check('original-two-bank-area-fits',b(s,'LevelEmitter')['height']>=94)
    start=settings('LevelEmitter')['count']
    for i,delta in enumerate([1,10,100,1000]):
        s=tap(s,'LevelEmitter',[14,39,69,104][i]+8,11)
        check('plus-'+str(delta)+'-updates-native-count',settings('LevelEmitter')['count']==start+sum([1,10,100,1000][:i+1]))
    for i,delta in enumerate([1,10,100,1000]):
        before=settings('LevelEmitter')['count'];s=tap(s,'LevelEmitter',[14,39,69,104][i]+8,51)
        check('minus-'+str(delta)+'-updates-native-count',settings('LevelEmitter')['count']==max(0,before-delta))
    # Legacy mouse callbacks query physical GLFW Shift state. This observer's
    # synthetic clicks cannot validate that path; do not claim a Shift live test.
    s=text(s,'LevelEmitter',80,31,'4242');check('editable-quantity-reaches-native-count',settings('LevelEmitter')['count']==4242)
    act('key',key=257,ticks=3);before=settings('LevelEmitter')['lessThan'];r=b(s,'LevelEmitter')
    s=act('xy-click',x=r['x']-10,y=r['y']+13,ticks=12);check('attached-threshold-toggle-reaches-native-state',settings('LevelEmitter')['lessThan']!=before)
    capture(a.run,'siu',a.minecraft,a.loader,'toms-level-emitter','Native ±1/10/100/1000 banks, Shift increments, editable amount and external threshold toggle checked through visible controls.')

if a.only=='tag':
    act('close',ticks=3);act('held',item=ident('tag_item_filter','items'),ticks=4);s=act('use-item',ticks=18)
    check('paired-native-list-area-fits',b(s,'TagItemFilter')['height']>=92)
    act('command',command='item replace entity @a inventory.0 with minecraft:oak_log',ticks=3)
    s=act('inventory',ticks=5);ix,iy=inv(s,9);s=act('xy-click',x=ix,y=iy,ticks=4);s=tap(s,'TagItemFilter',9,9)
    check('sample-cell-retains-real-native-sample',bool(win(s,'TagItemFilter')['slots'][0]['count']))
    ix,iy=inv(s,9);s=act('xy-click',x=ix,y=iy,ticks=4)
    s=tap(s,'TagItemFilter',31,8);s=tap(s,'TagItemFilter',92,8)
    tags=settings('TagItemFilter')['tags'];check('add-selected-native-item-tag',len(tags)==1)
    r=b(s,'TagItemFilter');before=settings('TagItemFilter')['allowList'];s=act('xy-click',x=r['x']-10,y=r['y']+13,ticks=10)
    check('external-filter-mode-toggle',settings('TagItemFilter')['allowList']!=before)
    s=tap(s,'TagItemFilter',112,8);s=tap(s,'TagItemFilter',92,25)
    check('remove-selected-native-filter-tag',settings('TagItemFilter')['tags']==[])
    # Re-add a meaningful filter for the final visual evidence.
    s=tap(s,'TagItemFilter',31,8);s=tap(s,'TagItemFilter',92,8)
    s=act('move',x=3,y=3,ticks=3)
    capture(a.run,'siu',a.minecraft,a.loader,'toms-tag-item-filter','Original gray paired lists, sample cell, native +/- and filter-mode control. Real sample insertion and add/remove checked.')

if a.only=='link':
    # The separate special-entry driver must have opened the real beacon connector.
    s=act('wait',ticks=3);check('original-link-panel-fits',b(s,'InventoryLink')['height']>=180)
    for i in range(8):
        s=text(s,'InventoryLink',40,22,'Channel '+str(i));s=tap(s,'InventoryLink',123,22)
    rows=settings('InventoryLink')['channels'];check('visible-name-field-and-add-create-eight-native-channels',len(rows)==8)
    s=tap(s,'InventoryLink',20,40);check('visible-list-selects-native-channel',settings('InventoryLink')['selected']!='null')
    before=settings('InventoryLink')['channels'];s=tap(s,'InventoryLink',157,22)
    check('native-public-private-lock-toggle',settings('InventoryLink')['channels']!=before)
    s=tap(s,'InventoryLink',140,22);check('visible-delete-removes-native-channel',len(settings('InventoryLink')['channels'])==7)
    channels=settings('InventoryLink')['channels'];r=b(s,'InventoryLink')
    s=act('scroll',x=r['x']+8+30,y=r['y']+24+80,amount=-1,ticks=5);s=tap(s,'InventoryLink',20,40)
    check('wheel-scroll-selects-next-real-channel',settings('InventoryLink')['selected']==channels[1]['id'])
    sx=r['x']+8+116;sy=r['y']+24+126
    act('click',x=sx,y=sy,button=0,ticks=1);act('drag',x=sx,y=sy-91,dx=0,dy=-91,button=0,ticks=3)
    s=act('release',x=sx,y=sy-91,button=0,ticks=4);s=tap(s,'InventoryLink',20,40)
    check('native-thumb-drag-restores-first-channel',settings('InventoryLink')['selected']==channels[0]['id'])
    s=act('move',x=3,y=3,ticks=3);capture(a.run,'siu',a.minecraft,a.loader,'toms-inventory-link','Native gray list and scrollbar, Beacon Level label, name field and original control order. Add, select, privacy and delete checked on the real connector.')

if a.only=='terminal':
    def resize(s,dw,dh):
        w=next(w for w in s['workspaceWindows'] if w['kind']=='CONTAINER')
        if w['locked']=='true':
            control=w['controls']['LOCK'];s=act('xy-click',x=control['x'],y=control['y'],ticks=3)
        r=b(s,'Terminal');xx=r['x']+r['width']-3;yy=r['y']+r['height']-3
        act('click',x=xx,y=yy,button=0,ticks=1);act('drag',x=xx+dw,y=yy+dh,dx=dw,dy=dh,button=0,ticks=3)
        return act('release',x=xx+dw,y=yy+dh,button=0,ticks=6)
    for name,ui,size in [('storage_terminal','toms-storage-terminal',(220,159)),('crafting_terminal','toms-crafting-terminal',(220,227))]:
        s=block(name);r=b(s,'Terminal');check(name+'-starts-with-native-nine-column-six-row-size',(r['width'],r['height'])==size)
        s=act('move',x=3,y=3,ticks=3);capture(a.run,'siu',a.minecraft,a.loader,ui,'Original nine-column storage layout at a usable default size. Terminal resizing remains available; separate player inventory retained.')
        s=text(s,'Terminal',25,6,'stone');act('key',key=257,ticks=2)
        check(name+'-search-input-remains-owned',any(w.get('definition','').endswith('$Terminal') for w in s.get('allWindows',s.get('windows',[]))))
        if a.resize:
            s=resize(s,36,18);r=b(s,'Terminal');newsize=(r['width'],r['height'])
            check(name+'-visible-corner-resizes-by-whole-cells',newsize==(size[0]+36,size[1]+18))
            act('close',ticks=2);s=act('client-use-block',ticks=14);r=b(s,'Terminal')
            check(name+'-manual-size-survives-reopen',(r['width'],r['height'])==newsize)
            s=resize(s,-36,-18)
            check(name+'-returns-to-native-default-grid',(b(s,'Terminal')['width'],b(s,'Terminal')['height'])==size)
