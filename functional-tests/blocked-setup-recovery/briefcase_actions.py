"""Exercise modern Bumblezone bee-care controls through SIU's mapped mouse area."""
from pathlib import Path
import json, sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send


def run(profile):
    checks=[]
    def act(kind,**args):return send(profile,kind,**args)
    def expect(label,passed):
        checks.append({'check':label,'passed':bool(passed)})
        (profile/'briefcase-action-checks.json').write_text(json.dumps({'scope':'Real bee capture, mapped SIU pointer controls, care resource consumption, disabled actions, release and recapture. No synthesized stored-bee component or direct menu-button call.','checks':checks},indent=2)+'\n')
        assert passed,label
        print('PASS',label,flush=True)
    def snapshot():
        act('regression-state',ticks=3)
        return json.loads((profile/'regression-state.json').read_text())
    def stored():
        cell=next(s for s in snapshot()['inventory'] if s['item']=='the_bumblezone:buzzing_briefcase')
        data=json.loads(cell['signature'])
        return data.get('components',{}).get('the_bumblezone:buzzing_briefcase_data',{}).get('BeesStored',[])
    def amount(item):return sum(s['count'] for s in snapshot()['inventory'] if s['item']==item)
    def window():
        state=act('wait',ticks=3)['client']
        return next(w for w in state.get('windows',state.get('allWindows',[])) if w.get('menuId')=='the_bumblezone:buzzing_briefcase')
    def tap(source_x,source_y):
        w=window();b=w['bounds'];plan=w['composition']
        region=next(r for r in reversed(plan['regions']) if r['source']['x']<=source_x<r['source']['x']+r['source']['width'] and r['source']['y']<=source_y<r['source']['y']+r['source']['height'])
        source=region['source']
        x=b['x']+plan['padding']+region['x']+source_x-source['x']-w.get('hostScrollX',0)
        y=b['y']+23+plan['padding']+region['y']+source_y-source['y']-w.get('hostScrollY',0)
        act('move',x=x,y=y,ticks=2)
        act('click',x=x,y=y,button=0,ticks=1)
        act('release',x=x,y=y,button=0,ticks=6)
    def open_case():
        act('sneak',down=True,ticks=4);act('client-use-item',shift=True,ticks=16);act('sneak',down=False,ticks=3)
        return window()

    act('close',ticks=4)
    act('fixture',block='minecraft:stone',ticks=8)
    act('command',command='kill @e[type=!minecraft:player]',ticks=4)
    act('command',command='gamemode survival @a',ticks=4)
    act('command',command='item replace entity @a weapon.mainhand with the_bumblezone:buzzing_briefcase',ticks=4)
    act('command',command='tp @a 8.5 65 10.5 180 0',ticks=4)
    act('command',command='summon minecraft:bee 8.5 66 8.5 {NoAI:1b,NoGravity:1b,Health:4.0f,Age:-12000,HasStung:1b,HasNectar:0b}',ticks=4)
    act('client-use-entity',entity='minecraft:bee',shift=False,ticks=14)
    bees=stored();expect('ordinary item use captures one injured juvenile bee',len(bees)==1 and bees[0]['Health']==4 and bees[0]['Age']<0 and bees[0]['HasStung']==1)
    w=open_case();expect('populated briefcase opens its integrated SIU window',w.get('templateClass','').endswith('.BumblezoneWindows$Template') and not w.get('layoutError'))

    # Publisher's first bee begins at native x=-11,y=46. Its care buttons are 11px.
    heal=(-6,73);stinger=(5,73);grow=(-6,84);pollen=(5,84);release=(16,51)
    before=stored();tap(*heal)
    expect('healing without a honey bottle changes no stored bee data',stored()==before)
    act('regression-seed-player',items=[{'slot':9,'item':'minecraft:honey_bottle','count':6},{'slot':10,'item':'the_bumblezone:bee_stinger','count':3},{'slot':11,'item':'the_bumblezone:pollen_puff','count':3}],ticks=4)
    for health in [6,8,10]:
        tap(*heal);expect('healing updates authoritative bee health to '+str(health),stored()[0]['Health']==health)
    expect('three heals consume three honey bottles and produce three glass bottles',amount('minecraft:honey_bottle')==3 and amount('minecraft:glass_bottle')==3)
    tap(*stinger);expect('restore-stinger control restores the bee and consumes one stinger',stored()[0]['HasStung']==0 and amount('the_bumblezone:bee_stinger')==2)
    tap(*grow);expect('grow-up control matures the bee and consumes one honey bottle',stored()[0]['Age']==0 and amount('minecraft:honey_bottle')==2 and amount('minecraft:glass_bottle')==4)
    tap(*pollen);expect('pollen control grants nectar and consumes one pollen puff',stored()[0]['HasNectar']==1 and amount('the_bumblezone:pollen_puff')==2)
    before=stored()
    for point in [heal,stinger,grow,pollen]:tap(*point)
    expect('completed care actions preserve bee data and consume no extra resources',stored()==before and amount('minecraft:honey_bottle')==2 and amount('the_bumblezone:bee_stinger')==2 and amount('the_bumblezone:pollen_puff')==2)
    act('close',ticks=5);open_case()
    expect('native bee data survives closing and reopening the held source',stored()==before)
    tap(*release);expect('release control removes the captured bee from the case',not stored())
    act('close',ticks=5)
    act('client-use-entity',entity='minecraft:bee',shift=False,ticks=14)
    bees=stored();expect('released world bee can be captured again with all care changes intact',len(bees)==1 and bees[0]['Health']==10 and bees[0]['Age']==0 and bees[0]['HasStung']==0 and bees[0]['HasNectar']==1)
    open_case();expect('recaptured bee is visible in a fresh functional session',bool(stored()) and bool(window()))
