"""Native automatic input/output routing and fuel conversion, not chest oracles."""

from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def prime_solid_fuel(row,initial,act,expect,snapshot,total):
    row.setdefault("test_helper_sha256",{})[__name__]=SOURCE_SHA256
    item='minecraft:coal';data=snapshot(item);source=data['menus'][-1];sid=source['session']
    slot=next(s['index'] for s in source['slots'] if not s['player'] and not s['virtual'] and s['active']
              and s.get('visible',True) and s['mayPlace'] and not s['count'])
    before=total(data,item);energy=data['nativeHost']['techrebornBurn']['energy']
    act('regression-seed-player',items=[{'slot':9,'item':item,'count':1}],ticks=4)
    act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=sid,slot=slot,ticks=4)
    act('wait',ticks=20);data=snapshot(item);burn=data['nativeHost']['techrebornBurn']
    expect('native solid-fuel generator consumes one coal and produces energy',total(data,item)==before
           and burn['remaining']>0 and burn['total']>=burn['remaining'] and burn['energy']>energy,native_burn=burn)
    row['native_recipe_checked']='One coal consumed by the native generator, positive native burn timer and increased native energy; subsequent transfers run during that burn.'
    return sid,slot,item,7


def storage_unit(row,initial,blocks,act,expect,snapshot,management,total,tagged_total,cursor):
    row.setdefault("test_helper_sha256",{})[__name__]=SOURCE_SHA256
    item='minecraft:diamond';amount=7;data=snapshot(item);source=data['menus'][-1];sid=source['session']
    inputs=[s for s in source['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True)]
    entry=next(s['index'] for s in inputs if s['mayPlace'] and not s['count'])
    output=next(s['index'] for s in inputs if s['type'].endswith('.SlotOutput'))
    before=total(data,item);row.update(session=sid,item=item,amount=amount,slot=entry,native_output_slot=output)
    act('regression-seed-player',items=[{'slot':9,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    signature=snapshot(item)['inventory'][9]['signature']
    def stored(label):
        data=snapshot(item);menu=next(m for m in data['menus'] if m['session']==sid)
        expect(label,data['nativeHost']['techrebornStorageAmount']==amount and menu['slots'][entry]['count']==0
               and menu['slots'][output]['count']==amount and cursor(data,item,0)
               and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
        return data
    act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=sid,slot=entry,ticks=4)
    stored('native storage-unit input routes once into its quantity and output buffer')
    act('regression-slot-click',session=sid,slot=output,ticks=4);act('regression-player-click',slot=10,ticks=4)
    data=snapshot(item)
    expect('native storage-unit output withdraws the stored quantity exactly once',data['nativeHost']['techrebornStorageAmount']==0
           and data['inventory'][10]['count']==amount and cursor(data,item,0) and total(data,item)==before+amount
           and tagged_total(data,item,signature)==amount)
    old=management(sid)
    if old['locked'] and (old['available']['LOCK'] or old['available']['UNLOCK']):act('regression-control',session=sid,control='LOCK',ticks=4)
    old=management(sid);act('regression-move',session=sid,dx=-30,dy=15,ticks=6)
    expect('storage-unit header drag follows its declared movement policy',(management(sid)['bounds']!=old['bounds'])==old['available']['MOVE'])
    act('regression-player-click',slot=10,ticks=4);act('regression-slot-click',session=sid,slot=entry,ticks=4)
    data=stored('moved storage-unit input and output keep their native routing')
    previous={m['session'] for m in data['menus']}
    partner=next((v for v in ['netherchested:nether_chest','sophisticatedstorage:barrel','supplementaries:sack'] if v in blocks),'minecraft:barrel')
    act('regression-place',block=partner,nearPlayer=True,dx=-1,dz=1,ticks=5)
    act('regression-open',nearPlayer=True,dx=-1,dz=1,emptyHand=True,ticks=16);data=snapshot(item)
    expect('opening another storage retains the native storage unit',any(m['session']==sid for m in data['menus']) and len(data['menus'])>=2)
    other=next(m for m in data['menus'] if m['session'] not in previous)
    dest=next(s['index'] for s in other['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count'])
    act('regression-slot-click',session=sid,slot=output,ticks=4);act('regression-slot-click',session=other['session'],slot=dest,ticks=4)
    data=snapshot(item)
    expect('storage-unit output transfers through the shared cursor into the other source',data['nativeHost']['techrebornStorageAmount']==0
           and next(m for m in data['menus'] if m['session']==other['session'])['slots'][dest]['count']==amount
           and cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    act('regression-slot-click',session=other['session'],slot=dest,ticks=4);act('regression-slot-click',session=sid,slot=entry,ticks=4)
    stored('return transfer restores exactly one native storage-unit quantity')
    act('regression-close',session=other['session'],ticks=4);stored('independent storage close preserves the storage-unit quantity')
    act('regression-slot-click',session=sid,slot=output,ticks=4);act('regression-close',session=sid,ticks=4);data=snapshot(item)
    expect('storage-unit source close preserves its withdrawn cursor and metadata',all(m['session']!=sid for m in data['menus'])
           and cursor(data,item,amount) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    row.update(status='passed',native_storage_unit_checked=True,cross_mod_partner=partner,
               coverage='Native input-to-output routing, authoritative stored quantity, full metadata, moved-window input, cross-storage round trip and independent/carried-stack source closing. Quantity tested below one output stack.')
