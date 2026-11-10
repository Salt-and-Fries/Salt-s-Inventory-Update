"""Intentional native disposal is not a recoverable storage round trip."""
from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def generator(row,initial,act,expect,snapshot,management,total,tagged_total,cursor):
    """Native generation/output semantics, rather than probing output cells as inputs."""
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    source=next(m for m in initial['menus'] if m['type']=='io.github.lucaargolo.kibe.blocks.miscellaneous.BlockGeneratorScreenHandler')
    sid=source['session'];block=next(a['block'] for a in row['actions'] if a['type']=='fixture')
    item='minecraft:basalt' if 'basalt_generator' in block else 'minecraft:cobblestone'
    act('wait',ticks=120);data=snapshot(item);menu=next(m for m in data['menus'] if m['session']==sid)
    slots=[s for s in menu['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True)]
    expect('native generator exposes all twenty-seven output cells',len(slots)==27)
    cells=[s for s in slots if s['item']==item and s['count'] and s['mayPickup']]
    expect('native generator produces its own block without a fake inventory fixture',bool(cells))
    slot=cells[0]['index'];signature=cells[0]['signature'];row.update(session=sid,slot=slot,item=item)
    # Production continues while this UI is open, so no fixed source-total
    # assertion is meaningful. The withdrawn native stack is then conserved
    # exactly across the shared cursor, separate inventory and source close.
    before=sum(s['count'] for s in data['inventory'] if s['item']==item)
    act('regression-slot-click',session=sid,slot=slot,ticks=4);data=snapshot(item);amount=data['cursor']['count']
    expect('native generated output is withdrawn as one complete original stack',1<=amount<=64 and data['cursor']['item']==item and data['cursor']['signature']==signature)
    act('regression-player-click',slot=9,ticks=4);data=snapshot(item)
    expect('withdrawn generated output reaches separate inventory exactly once',cursor(data,item,0) and data['inventory'][9]['count']==amount
        and data['inventory'][9]['signature']==signature and sum(s['count'] for s in data['inventory'] if s['item']==item)==before+amount)
    act('regression-seed-player',items=[{'slot':10,'item':'minecraft:apple','count':3,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    apple=snapshot('minecraft:apple')['inventory'][10]['signature'];act('regression-player-click',slot=10,ticks=4)
    act('regression-slot-click',session=sid,slot=slot,ticks=4);data=snapshot('minecraft:apple')
    expect('native generator output refuses foreign input without consuming the cursor',cursor(data,'minecraft:apple',3)
        and tagged_total(data,'minecraft:apple',apple)==3 and all(s['item']!='minecraft:apple' for m in data['menus'] if m['session']==sid for s in m['slots'] if not s['player']))
    act('regression-player-click',slot=10,ticks=4)
    old=management(sid)
    if old['locked'] and (old['available']['LOCK'] or old['available']['UNLOCK']):act('regression-control',session=sid,control='LOCK',ticks=4)
    old=management(sid);act('regression-move',session=sid,dx=-25,dy=15,ticks=6)
    expect('generator output window follows its declared movement policy',(management(sid)['bounds']!=old['bounds'])==old['available']['MOVE'])
    act('regression-close',session=sid,ticks=4);data=snapshot(item)
    expect('closing the generator preserves withdrawn output and separate inventory',all(m['session']!=sid for m in data['menus'])
        and data['inventory'][9]['count']==amount and data['inventory'][9]['signature']==signature and cursor(data,item,0))
    act('client-use-block',ticks=20);data=snapshot(item)
    reopened=[m for m in data['menus'] if m['type']==source['type']]
    expect('ordinary reopening retains the native producing machine',len(reopened)==1 and reopened[0]['session']!=sid
        and any(s['item']==item and s['count']>0 for s in reopened[0]['slots'] if not s['player']) and data['inventory'][9]['count']==amount)
    row.update(status='passed',native_output_generation_checked=dict(block=item,withdrawn=amount),
        coverage='Real native block generation, physical output withdrawal, exact withdrawn-stack metadata/quantity in separate inventory, foreign-input refusal, moved chrome, close and ordinary reopen. Ongoing production is intentionally not treated as storage loss/duplication.')

def trash_can(row,initial,act,expect,snapshot,management,total,tagged_total,cursor):
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    source=next(m for m in initial['menus'] if m.get('menuType') in {'kibe:trash_can','kibe:pocket_trash_can'}
                or m['type'].endswith(('.trashcan.TrashCanScreenHandler','.trashcan.PocketTrashCanScreenHandler')))
    sid=source['session'];item='minecraft:diamond';before=total(snapshot(item),item)
    slot=next(s['index'] for s in source['slots'] if not s['player'] and not s['virtual'] and s['active'] and s['mayPlace'])
    row.update(session=sid,slot=slot,item=item,amount=7)
    act('regression-seed-player',items=[{'slot':9,'item':item,'count':7,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    signature=snapshot(item)['inventory'][9]['signature']
    act('regression-player-click',slot=9,ticks=4)
    act('regression-slot-click',session=sid,slot=slot,button=1,ticks=8)
    data=snapshot(item)
    expect('native trash right-click disposes exactly one tagged item and keeps the remainder',
           cursor(data,item,6) and total(data,item)==before+6 and tagged_total(data,item,signature)==6
           and not next(m for m in data['menus'] if m['session']==sid)['slots'][slot]['count'])
    old=management(sid)
    if old['locked'] and (old['available']['LOCK'] or old['available']['UNLOCK']):
        act('regression-control',session=sid,control='LOCK',ticks=4)
    old=management(sid);act('regression-move',session=sid,dx=-30,dy=15,ticks=6)
    expect('trash header drag follows its normal movement policy',
           (management(sid)['bounds']!=old['bounds'])==old['available']['MOVE'])
    data=snapshot(item)
    expect('moving trash preserves its uncommitted carried remainder',cursor(data,item,6) and tagged_total(data,item,signature)==6)
    act('regression-slot-click',session=sid,slot=slot,ticks=8);data=snapshot(item)
    expect('moved trash left-click disposes only the remaining deposited stack',
           cursor(data,item,0) and total(data,item)==before and tagged_total(data,item,signature)==0)
    act('regression-slot-click',session=sid,slot=slot,ticks=4)
    expect('empty trash cannot return a phantom disposed item',cursor(snapshot(item),item,0) and total(snapshot(item),item)==before)
    prior={m['session'] for m in snapshot(item)['menus']}
    act('regression-place',block='minecraft:barrel',nearPlayer=True,dx=-1,dz=1,ticks=5)
    act('regression-open',nearPlayer=True,dx=-1,dz=1,emptyHand=True,ticks=18)
    data=snapshot(item);others=[m for m in data['menus'] if m['session'] not in prior]
    expect('ordinary secondary block use opens a vanilla barrel beside the live trash source',
           len(others)==1 and others[0].get('menuType')=='minecraft:generic_9x3' and any(m['session']==sid for m in data['menus']))
    other=others[0]
    act('regression-seed-player',items=[{'slot':10,'item':item,'count':4,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    act('regression-player-click',slot=10,ticks=4)
    act('regression-close',session=sid,ticks=8);data=snapshot(item)
    expect('closing trash never disposes an item still on the shared cursor or closes another source',
           cursor(data,item,4) and total(data,item)==before+4 and tagged_total(data,item,signature)==4
           and all(m['session']!=sid for m in data['menus']) and any(m['session']==other['session'] for m in data['menus']))
    act('regression-slot-click',session=other['session'],slot=0,ticks=4)
    act('regression-slot-click',session=other['session'],slot=0,ticks=4)
    act('regression-player-click',slot=11,ticks=4);data=snapshot(item)
    expect('uncommitted trash remainder still round-trips through independent storage with full data',
           cursor(data,item,0) and data['inventory'][11]['count']==4 and data['inventory'][11]['signature']==signature
           and total(data,item)==before+4 and tagged_total(data,item,signature)==4)
    act('regression-close',session=other['session'],ticks=4)
    expect('closing the remaining source preserves recovered items after intentional disposal',total(snapshot(item),item)==before+4)
    row.update(status='passed',native_disposal_checked={'right_click':1,'left_click':6,'uncommitted_recovered':4},
               cross_mod_partner='minecraft:barrel',coverage='Native trash right/left deletion, moved hit area, no phantom recovery, shared cursor on close, independent barrel lifetime and exact-data recovery of uncommitted items.')
