"""Distinguish native fuel conversion from loss during ordinary transfers."""
from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def prime_reactor_carbon(row,initial,act,expect,snapshot,total,cursor):
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    item='minecraft:coal_block';data=snapshot(item)
    source=next(m for m in data['menus'] if m['type'] in {
        'owmii.powah.inventory.ReactorContainer','owmii.powah.inventory.ReactorMenu'})
    sid=source['session'];reserve=data['nativeHost']['powahReactor'];slot=2
    expect('native reactor has completed its ordinary multiblock assembly',
           reserve.get('assembled') is True,native_reserve=reserve)
    expect('native reactor fixture starts without fuel or carbon reserve',
           reserve['carbon']==0 and reserve['fuel']==0 and not reserve['running']
           and source['slots'][slot]['count']==0,native_reserve=reserve)
    before=total(data,item)
    act('regression-seed-player',items=[{'slot':9,'item':item,'count':1}],ticks=4)
    act('regression-player-click',slot=9,ticks=4)
    act('regression-slot-click',session=sid,slot=slot,ticks=4)
    act('wait',ticks=20);data=snapshot(item);reserve=data['nativeHost']['powahReactor']
    expect('native reactor converts exactly one coal block into its real carbon reserve',
           total(data,item)==before and cursor(data,item,0) and reserve['carbon']>0
           and reserve['carbonCapacity']>=reserve['carbon'] and reserve['fuel']==0
           and not reserve['running'],native_reserve=reserve)
    carbon=reserve['carbon'];act('wait',ticks=20);reserve=snapshot(item)['nativeHost']['powahReactor']
    expect('reactor carbon reserve stays stable without nuclear fuel',
           reserve['carbon']==carbon and reserve['fuel']==0 and not reserve['running'],native_reserve=reserve)
    row['native_recipe_checked']='One coal block converted through native ticks into an idle carbon reserve; subsequent exact-data oak-log transfers use the carbon input. Nuclear generation and coolant cycles are not certified.'
    return sid,slot,'minecraft:oak_log',7


def prime_furnator(row,initial,act,expect,snapshot,total,cursor):
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    item='minecraft:coal_block';data=snapshot(item);source=data['menus'][-1];sid=source['session']
    slot=next(s['index'] for s in source['slots'] if not s['player'] and not s['virtual'] and s['active']
              and s.get('visible',True) and s['mayPlace'] and not s['count'])
    before=total(data,item);energy=data['nativeHost']['powahBurn']['energy']
    act('regression-seed-player',items=[{'slot':9,'item':item,'count':1}],ticks=4)
    act('regression-player-click',slot=9,ticks=4)
    act('regression-slot-click',session=sid,slot=slot,ticks=4)
    act('wait',ticks=20);data=snapshot(item);burn=data['nativeHost']['powahBurn']
    expect('native Powah Furnator consumes one coal block into a long burn and stored energy',
           total(data,item)==before and cursor(data,item,0) and burn['remaining']>8000
           and burn['total']>=burn['remaining'] and burn['energy']>energy,native_burn=burn)
    row['native_recipe_checked']='One coal block consumed by the native Furnator; positive carbon reserve and energy production verified before exact tagged-coal transfers during the long burn.'
    return sid,slot,'minecraft:coal',7
