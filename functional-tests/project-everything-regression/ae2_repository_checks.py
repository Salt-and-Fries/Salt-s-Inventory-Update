"""Native repository packets and real ME storage, without seeding machine slots."""

from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
import json

def exercise(row,profile,initial,blocks,act,expect,snapshot,total,tagged_total,cursor):
    row.setdefault("test_helper_sha256",{})[__name__]=SOURCE_SHA256
    terminal=next(m for m in initial['menus'] if m['type']=='appeng.menu.me.common.MEStorageMenu');sid=terminal['session']
    row['session']=sid
    act('regression-place',block='ae2:drive',dx=-1,dz=0,ticks=18)
    act('regression-open',dx=-1,dz=0,emptyHand=True,ticks=20)
    data=snapshot();drive=next(m for m in data['menus'] if m['type']=='appeng.menu.implementations.DriveMenu')
    drive_slot=next(s['index'] for s in drive['slots'] if not s['player'] and s['active'] and not s['count'])
    act('regression-seed-player',items=[{'slot':9,'item':'ae2:item_storage_cell_1k','count':1}],ticks=4)
    act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=drive['session'],slot=drive_slot,ticks=24)
    data=snapshot();expect('ME drive receives a real cell through its native slot',next(m for m in data['menus'] if m['session']==drive['session'])['slots'][drive_slot]['item']=='ae2:item_storage_cell_1k')
    item='minecraft:diamond';amount=7;before=total(snapshot(item),item)
    act('regression-seed-player',items=[{'slot':10,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    signature=next(s['signature'] for s in snapshot(item)['inventory'] if s['index']==10)
    def stored():
        act('regression-ae2-cell-ledger',item=item,snbt='{siu_regression_marker:42}',dx=-1,dz=0,cell=0,ticks=3)
        return json.loads((profile/'regression-ae2-cell-ledger.json').read_text())
    expect('new ME cell is empty and connected to native power',stored()['stored']==0 and stored()['powered'])
    act('regression-player-click',slot=10,ticks=4)
    act('regression-ae2-repository-tap',session=sid,ticks=24)
    repository=json.loads((profile/'regression-ae2-repository-state.json').read_text())
    row['ae2_repository_initial_filter']=repository.get('native',{})
    if row.get('native_key_type_fixture'):
        expect('native terminal fixture enables the item key type before repository transfer',
               'ae2:i' in repository.get('native',{}).get('keyTypes',''))
    data=snapshot(item);network=stored()
    expect('native repository deposit stores all tagged items in ME storage',cursor(data,item,0) and network['stored']==amount and total(data,item)==before)
    partner=next((b for b in ['netherchested:nether_chest','sophisticatedstorage:barrel','supplementaries:sack'] if b in blocks),'minecraft:barrel')
    old={m['session'] for m in data['menus']}
    act('regression-place',block=partner,dx=-1,dz=1,ticks=6);act('regression-open',dx=-1,dz=1,emptyHand=True,ticks=18)
    data=snapshot(item);other=next(m for m in data['menus'] if m['session'] not in old)
    slot=next(s['index'] for s in other['slots'] if not s['player'] and not s['virtual'] and s['active'] and s['mayPlace'] and not s['count'])
    act('regression-ae2-repository-tap',session=sid,item=item,ticks=12);data=snapshot(item)
    row['ae2_repository_filter_after_partner_open']=json.loads((profile/'regression-ae2-repository-state.json').read_text()).get('native',{})
    expect('native repository withdrawal returns the original tagged stack',cursor(data,item,amount) and stored()['stored']==0 and tagged_total(data,item,signature)==amount)
    act('regression-slot-click',session=other['session'],slot=slot,ticks=6);data=snapshot(item)
    expect('ME withdrawal transfers into another publisher storage once',cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    act('regression-slot-click',session=other['session'],slot=slot,ticks=4)
    act('regression-ae2-repository-tap',session=sid,ticks=16)
    expect('return transfer restores the exact component-bearing ME key',stored()['stored']==amount and cursor(snapshot(item),item,0))
    act('regression-close',session=drive['session'],ticks=8)
    expect('closing the drive window leaves terminal storage and its cell alive',stored()['stored']==amount and any(m['session']==sid for m in snapshot(item)['menus']))
    act('regression-close',session=other['session'],ticks=6)
    act('regression-ae2-repository-tap',session=sid,item=item,ticks=12);act('regression-close',session=sid,ticks=10)
    data=snapshot(item)
    expect('closing a terminal carrying its ME withdrawal preserves all data',stored()['stored']==0 and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    act('regression-player-click',slot=11,ticks=4)
    expect('ME withdrawal returns once to the independent player inventory',cursor(snapshot(item),item,0) and snapshot(item)['inventory'][11]['count']==amount)
    row.update(status='passed',ae2_repository_checked=True,cross_mod_partner=partner,
               coverage='Powered native ME network, real 1k drive cell inserted by mouse, repository deposit/withdrawal packets, exact component-bearing key count from native cell inventory, cross-storage transfer, independent drive close and terminal close with a carried stack.')
