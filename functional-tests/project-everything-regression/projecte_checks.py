"""Native ProjectE ghost configuration, EMC conversion and charging."""

from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
import re

def collector(row,initial,items,blocks,act,expect,snapshot,management,total,tagged_total,cursor):
    row.setdefault("test_helper_sha256",{})[__name__]=SOURCE_SHA256
    native=next(m for m in initial['menus'] if m['type'].startswith('moze_intel.projecte.') and any(v in m['type'] for v in ['Collector','Relay']))
    sid=native['session'];row['session']=sid
    relay='Relay' in native['type']
    if not relay:
        ghost=next(s['index'] for s in native['slots'] if s['type'].endswith('.SlotGhost'))
        coal='minecraft:coal';before_coal=total(initial,coal)
        act('regression-seed-player',items=[{'slot':16,'item':coal,'count':1,'snbt':'{siu_regression_marker:77}'}],ticks=4)
        act('regression-player-click',slot=16,ticks=4)
        act('regression-slot-click',session=sid,slot=ghost,ticks=4)
        state=snapshot(coal);example=next(m for m in state['menus'] if m['session']==sid)['slots'][ghost]
        expect('collector ghost lock copies a fuel example without consuming or duplicating physical fuel',
               example['virtual'] and not example['policyQueried'] and example['item']==coal and example['count']==1
               and cursor(state,coal,1) and total(state,coal)==before_coal+1)
        act('regression-player-click',slot=16,ticks=4)
        act('regression-slot-click',session=sid,slot=ghost,ticks=4)
        state=snapshot(coal)
        expect('collector native ghost clear preserves the physical fuel',
               next(m for m in state['menus'] if m['session']==sid)['slots'][ghost]['count']==0
               and state['inventory'][16]['count']==1 and total(state,coal)==before_coal+1)
    else:
        item='minecraft:diamond';policy=snapshot(item)
        source=next(s['index'] for s in native['slots'] if not s['player'] and not s['virtual'] and s['active'] and s['mayPlace'])
        prior=policy['nativeHost']['projecteRelay']['storedEmc'];before=total(policy,item)
        act('regression-seed-player',items=[{'slot':16,'item':item,'count':1}],ticks=4)
        act('regression-player-click',slot=16,ticks=4)
        act('regression-slot-click',session=sid,slot=source,ticks=4)
        act('wait',ticks=10);burned=snapshot(item)
        expect('native relay consumes one real diamond and stores its EMC',total(burned,item)==before
               and burned['nativeHost']['projecteRelay']['storedEmc']==prior+8192)
    star=next((v for v in ['projecte:klein_star_ein','projecte:klein_star_zwei'] if v in items),None)
    expect('native ProjectE charging item is registered',star is not None)
    policy=snapshot(star);native=next(m for m in policy['menus'] if m['session']==sid)
    slot=next(s['index'] for s in native['slots'] if not s['player'] and not s['virtual'] and s['active']
              and s.get('visible',True) and s['mayPlace'] and not s['count'])
    before=total(policy,star);old=management(sid)['bounds']
    if management(sid)['locked']:act('regression-control',session=sid,control='LOCK',ticks=4)
    movable=management(sid)['available']['MOVE']
    act('regression-move',session=sid,dx=-30,dy=20,ticks=6)
    expect('ProjectE energy window follows its declared movement policy',(management(sid)['bounds']!=old)==movable)
    act('regression-seed-player',items=[{'slot':9,'item':star,'count':1,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    act('regression-player-click',slot=9,ticks=4)
    act('regression-slot-click',session=sid,slot=slot,ticks=4)
    act('wait',ticks=40)
    charged=snapshot(star);machine=charged['nativeHost'].get('projecteRelay' if relay else 'projecteCollector',{})
    expect('native ProjectE machine charges one real Klein Star through its moved input',machine.get('chargeProportion' if relay else 'itemCharge',0)>0
           and cursor(charged,star,0) and total(charged,star)==before+1,native_collector=machine)
    act('regression-slot-click',session=sid,slot=slot,ticks=4)
    charged=snapshot(star);signature=charged['cursor']['signature']
    expect('charged native item withdraws once and retains its ordinary custom data',cursor(charged,star,1)
           and total(charged,star)==before+1 and re.search(r'\bsiu_regression_marker["\x27]?\s*:\s*42\b',signature))
    partner=next((b for b in ['netherchested:nether_chest','sophisticatedstorage:barrel','supplementaries:sack'] if b in blocks),'minecraft:barrel')
    retained={m['session'] for m in charged['menus']}
    act('regression-place',block=partner,nearPlayer=True,dx=-1,dz=1,ticks=6)
    act('regression-open',nearPlayer=True,dx=-1,dz=1,emptyHand=True,ticks=18)
    state=snapshot(star)
    expect('opening ProjectE partner preserves the source and charged cursor',any(m['session']==sid for m in state['menus'])
           and cursor(state,star,1) and state['cursor']['signature']==signature)
    other=next(m for m in state['menus'] if m['session'] not in retained)
    dest=next(s['index'] for s in other['slots'] if not s['player'] and not s['virtual'] and s['active']
              and s.get('visible',True) and s['mayPlace'] and not s['count'])
    act('regression-slot-click',session=other['session'],slot=dest,ticks=4)
    state=snapshot(star)
    expect('ProjectE-to-storage transfer preserves complete charged item data',cursor(state,star,0)
           and total(state,star)==before+1 and tagged_total(state,star,signature)==1)
    row['cross_mod_partner']={'block':partner,'menu':other['type'],'session':other['session'],'slot':dest,'same_slot_index':slot==dest}
    act('regression-close',session=sid,ticks=6)
    state=snapshot(star)
    expect('ProjectE source close leaves the charged item and partner independent',all(m['session']!=sid for m in state['menus'])
           and any(m['session']==other['session'] for m in state['menus']) and tagged_total(state,star,signature)==1)
    old_partner=other['session'];act('regression-close',session=old_partner,ticks=6)
    act('regression-open',nearPlayer=True,dx=-1,dz=1,emptyHand=True,ticks=18)
    state=snapshot(star);other=next(m for m in state['menus'] if m['session'] not in retained and m['session']!=old_partner)
    resident=next(s for s in other['slots'] if not s['player'] and not s['virtual'] and s['item']==star and s['count'])
    expect('native partner reopen retains the charged item exactly once',resident['count']==1 and resident['signature']==signature
           and total(state,star)==before+1 and tagged_total(state,star,signature)==1)
    act('regression-slot-click',session=other['session'],slot=resident['index'],ticks=4)
    act('regression-player-click',slot=10,ticks=4)
    state=snapshot(star)
    expect('charged item returns to separate inventory without losing EMC or custom data',cursor(state,star,0)
           and state['inventory'][10]['count']==1 and state['inventory'][10]['signature']==signature and tagged_total(state,star,signature)==1)
    act('regression-close',session=other['session'],ticks=4)
    row.update(status='passed',projecte_energy_checked='relay' if relay else 'collector',
               native_partner_reopen_checked={'block':partner,'closed_session':old_partner,'reopened_session':other['session']},
               coverage=('Native diamond-to-EMC conversion; ' if relay else 'Native ghost lock copy/clear with untouched physical fuel; ')+ 'real Klein Star charging through moved input; complete charged-item data preserved through another storage, independent source close and populated partner reopen. No exhaustive fuel-upgrade recipe claim.')
