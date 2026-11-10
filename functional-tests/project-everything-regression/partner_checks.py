"""Open storage and held menus in the order permitted by SIU's source lock."""
from pathlib import Path
import hashlib
import json
import math
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()


def same_locked_host(before, after):
    if before == after:
        return True
    # A real powered AE2 terminal drains while an unrelated action is refused.
    # Permit only decreasing native energy; compare every other component and
    # the outer inventory identity/count exactly. Never normalize crafting data.
    if before.get('item') != 'ae2:wireless_crafting_terminal':
        return False
    if {k:v for k,v in before.items() if k != 'signature'} != {k:v for k,v in after.items() if k != 'signature'}:
        return False
    try:
        old, new = json.loads(before['signature']), json.loads(after['signature'])
        old_energy = old['components'].pop('ae2:stored_energy')
        new_energy = new['components'].pop('ae2:stored_energy')
        return (old == new and isinstance(old_energy, (float, int)) and isinstance(new_energy, (float, int))
                and math.isfinite(old_energy) and math.isfinite(new_energy) and 0 < new_energy <= old_energy)
    except (ValueError, KeyError, TypeError):
        return False


def open_partner(row,data,sid,partner,act,expect,snapshot,*,place=True,**position):
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    # Preserve the real transfer item when inspecting a new partner. A sack
    # accepts diamonds but can reject a Create package; querying the default
    # diamond would wrongly classify that native refusal as a lost transfer.
    probe_item=data.get('candidate','minecraft:diamond')
    def observe():return snapshot(probe_item)
    source=next(m for m in data['menus'] if m['session']==sid)
    policy=data['inputPolicy'];locked=policy['selectedSourceLocked'];hand=policy['mainHand']
    if source['type']=='appeng.menu.me.networktool.NetworkToolMenu':
        act('regression-select-empty-hand',ticks=4);data=observe();policy=data['inputPolicy'];locked=policy['selectedSourceLocked']
        expect('AE2 tool stays in its bound inventory cell while selecting an empty hand',not locked and any(m['session']==sid for m in data['menus']))
    if locked:
        expect('held-source ordering starts with an empty shared cursor',data['cursor']['count']==0)
        if place and policy['serverRejectsLockedBlockUse']:
            act('regression-place',block=partner,**position,ticks=8)
            # Deliberately denied server packet, not an allowed world UI action.
            act('regression-open',**position,emptyHand=False,ticks=10)
            denied=observe()
            expect('locked-hand block-use packet is denied without changing the host or cursor',
                {m['session'] for m in denied['menus']}=={m['session'] for m in data['menus']}
                and same_locked_host(hand,denied['inputPolicy']['mainHand']) and denied['cursor']==data['cursor'])
            expect('denied block use cannot lend a stale block identity to a later menu',denied['inputPolicy']['pendingBlockTarget']=='none')
            row['locked_block_use_rejected']=True
        act('regression-close',session=sid,ticks=6)
        closed=observe()
        expect('closing a held source releases its world-use lock without consuming the host',
            not any(m['session']==sid for m in closed['menus']) and not closed['inputPolicy']['selectedSourceLocked']
            and closed['inputPolicy']['mainHand']['item']==hand['item'] and closed['inputPolicy']['mainHand']['count']==hand['count']
            and closed['cursor']['count']==0)
    previous={m['session'] for m in observe()['menus']}
    if place:act('regression-place',block=partner,**position,ticks=8)
    # The host lock has been released (or an empty hotbar cell selected).
    # Sophisticated's legacy barrel acknowledges offhand clicks without
    # opening. Other legacy partners retain their proved offhand path, which
    # avoids held tools with their own main-hand use-on-first behavior.
    hand_options={'allowHeldMainHand':True} if partner.startswith('sophisticatedstorage:') else {}
    act('regression-open',**position,emptyHand=True,**hand_options,ticks=18)
    opened=observe()
    other=next((m for m in opened['menus'] if m['session'] not in previous and any(not s['player'] and not s['virtual'] for s in m['slots'])),None)
    expect('ordinary block activation opens the partner storage',other is not None)
    if source['type']=='appeng.menu.me.networktool.NetworkToolMenu':
        expect('network-tool block use opens storage rather than a second view of the same held tool',
               other['type']!=source['type'],actual=other['type'])
    if locked:
        entry=next(a for a in reversed(row['actions']) if a['type'] in {'use-item','client-use-item'})
        request=dict(entry);kind=request.pop('type');act('client-use-item' if kind=='use-item' else kind,**request)
        reopened=observe()
        candidates=[m for m in reopened['menus'] if m['session']!=other['session'] and m['type']==source['type'] and m.get('menuType')==source.get('menuType')]
        expect('ordinary item use reopens exactly the original held source beside storage',len(candidates)==1)
        fresh=candidates[0]
        expect('reopened held menu keeps the same native source identity and complete slot shape',
            bool(source.get('sourceKey')) and fresh.get('sourceKey')==source['sourceKey']
            and len(fresh['slots'])==len(source['slots']) and reopened['inputPolicy']['selectedSourceLocked']
            and reopened['inputPolicy']['mainHand']['item']==hand['item'] and reopened['inputPolicy']['mainHand']['count']==hand['count'])
        row.setdefault('held_source_partner_orders',[]).append({'old_session':sid,'new_session':fresh['session'],'source_key':source['sourceKey'],
            'reason':'Selected source is locked; close it, activate storage, then reopen the held menu. No locked-hand world-use bypass.'})
        sid=fresh['session'];row['session']=sid;opened=reopened
    return opened,sid,next(m for m in opened['menus'] if m['session']==other['session'])
