"""Capture and care for real native bees; the briefcase is not physical storage."""
from pathlib import Path
import hashlib,json
import nbtlib

SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def stored_bees(signature):
    try:value=json.loads(signature)
    except json.JSONDecodeError:
        native=nbtlib.parse_nbt(signature).unpack()
        value=json.loads(json.dumps(native,default=lambda v:v.tolist()))
    if 'components' in value:return value['components'].get('the_bumblezone:buzzing_briefcase_data',{}).get('BeesStored',[])
    return value.get('tag',{}).get('BeesStored',[])

def exercise(row,profile,act,expect,snapshot):
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    origin=json.loads((profile/'regression-catalog.json').read_text())['origin'];x,y,z=origin
    def bees():
        cell=next(s for s in snapshot()['inventory'] if s['item']=='the_bumblezone:buzzing_briefcase')
        return stored_bees(cell['signature'])
    def amount(item):return sum(s['count'] for s in snapshot()['inventory'] if s['item']==item)
    def window():
        state=act('wait',ticks=3)['client']
        return next(w for w in state.get('windows',state.get('allWindows',[])) if w.get('menuId')=='the_bumblezone:buzzing_briefcase')
    def tap(sx,sy):
        w=window();plan=w['composition'];b=w['bounds']
        region=next(r for r in reversed(plan['regions']) if r['source']['x']<=sx<r['source']['x']+r['source']['width'] and r['source']['y']<=sy<r['source']['y']+r['source']['height'])
        source=region['source'];px=b['x']+plan['padding']+region['x']+sx-source['x']-w.get('hostScrollX',0)
        py=b['y']+23+plan['padding']+region['y']+sy-source['y']-w.get('hostScrollY',0)
        # Native bee-care hit targets depend on the preceding hover render.
        # These are live transformed source coordinates, not atlas screen pixels.
        act('move',x=px,y=py,ticks=2);act('click',x=px,y=py,button=0,ticks=1);act('release',x=px,y=py,button=0,ticks=6)
    def open_case():return act('client-use-item',shift=True,ticks=16)
    act('close',ticks=4);act('fixture',block='minecraft:stone',ticks=8)
    commands=['kill @e[type=!minecraft:player]','gamemode survival @a',
        'item replace entity @a weapon.mainhand with the_bumblezone:buzzing_briefcase',
        f'tp @a {x+.5} {y} {z+2.5} 180 0',
        f'summon minecraft:bee {x+.5} {y+1} {z+.5} '+'{NoAI:1b,NoGravity:1b,Health:4.0f,Age:-12000,HasStung:1b,HasNectar:0b}']
    row['additional_native_fixture_commands']=commands
    for command in commands:act('command',command=command,ticks=4)
    act('client-use-entity',entity='minecraft:bee',shift=False,ticks=14)
    stored=bees();expect('ordinary native item use captures the injured juvenile bee',len(stored)==1 and stored[0]['Health']==4 and stored[0]['Age']<0 and stored[0]['HasStung']==1)
    open_case();w=window();expect('populated bee case uses its integrated native template',w.get('templateClass','').endswith('.BumblezoneWindows$Template') and not w.get('layoutError'))
    heal=(-6,73);stinger=(5,73);grow=(-6,84);pollen=(5,84);release=(16,51)
    before=bees();tap(*heal);expect('native healing without honey changes no bee data',bees()==before)
    act('regression-seed-player',items=[{'slot':9,'item':'minecraft:honey_bottle','count':6},
        {'slot':10,'item':'the_bumblezone:bee_stinger','count':3},{'slot':11,'item':'the_bumblezone:pollen_puff','count':3}],ticks=4)
    for health in [6,8,10]:tap(*heal);expect('native healing reaches authoritative health '+str(health),bees()[0]['Health']==health)
    expect('three native heals consume three honey bottles and return glass',amount('minecraft:honey_bottle')==3 and amount('minecraft:glass_bottle')==3)
    tap(*stinger);expect('native stinger restoration consumes exactly one stinger',bees()[0]['HasStung']==0 and amount('the_bumblezone:bee_stinger')==2)
    tap(*grow);expect('native growth consumes one honey bottle and returns glass',bees()[0]['Age']==0 and amount('minecraft:honey_bottle')==2 and amount('minecraft:glass_bottle')==4)
    tap(*pollen);expect('native pollination consumes one pollen puff',bees()[0]['HasNectar']==1 and amount('the_bumblezone:pollen_puff')==2)
    before=bees()
    for point in [heal,stinger,grow,pollen]:tap(*point)
    expect('completed care actions neither change data nor consume extra resources',bees()==before and amount('minecraft:honey_bottle')==2
        and amount('the_bumblezone:bee_stinger')==2 and amount('the_bumblezone:pollen_puff')==2)
    act('close',ticks=5);open_case();expect('cared-for native bee data persists after source reopen',bees()==before)
    tap(*release);expect('native release removes the stored bee exactly once',not bees());act('close',ticks=5)
    act('client-use-entity',entity='minecraft:bee',shift=False,ticks=14);stored=bees()
    expect('released world bee is recaptured with all completed care changes',len(stored)==1 and stored[0]['Health']==10 and stored[0]['Age']==0 and stored[0]['HasStung']==0 and stored[0]['HasNectar']==1)
    open_case();expect('recaptured bee opens a fresh native editing session',bool(bees()) and bool(window()))
    row.update(status='passed',native_bee_care_checked=True,
        coverage='Ordinary native world-bee capture, integrated populated panel, all care actions and resource accounting, refused/finished actions, persistence, release and native recapture. No fabricated stored-bee data or direct button invocation.')
