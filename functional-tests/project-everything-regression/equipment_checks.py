"""Character launchers and shared-cursor transitions through public hit regions."""

from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
import json

def cases(installed,items=None):
    rows=[]
    for owner,attachment,target,model in [
        ('curios','.curios.CuriosCharacterButton','curios_accessories','.CuriosDesktopMenu'),
        ('accessories','.accessories.AccessoriesClient$Button','accessories_equipment','.AccessoriesDesktopMenu'),
        ('aether','.aether.AetherEquipmentClient$Cloud','aether_equipment','.AetherEquipmentMenu')]:
        if owner not in installed:continue
        rows.append({'id':'integration-'+owner+'-character','mod_id':owner,'scenario':'equipment-launcher',
            'attachment':attachment,'target_menu':'salts_inventory_compat:'+target,'target_model':model,
            'actions':[{'type':'fixture','block':'minecraft:stone','ticks':8},
                       {'type':'command','command':'gamemode survival @a','ticks':3},
                       {'type':'regression-character','ticks':8},
                       {'type':'regression-attachment-click','attachment':attachment,'ticks':18}]})
    if 'quark' in installed and (items is None or 'quark:backpack' in items):
        rows.append({'id':'integration-quark-worn-backpack','mod_id':'quark','scenario':'quark-backpack',
            'actions':[{'type':'fixture','block':'minecraft:stone','ticks':8},
                       {'type':'command','command':'gamemode survival @a','ticks':3},
                       {'type':'command','command':'item replace entity @a armor.chest with quark:backpack','ticks':6},
                       {'type':'inventory','ticks':24}, {'type':'regression-character','ticks':8}]})
    return rows

def backpack(case,row,profile,act,expect,snapshot,total,tagged_total,cursor):
    row.setdefault("test_helper_sha256",{})[__name__]=SOURCE_SHA256
    def owned(data):return [m for m in data['menus'] if m['type'].endswith('.QuarkBackpackMenu')]
    def builtin(kind):
        act('regression-builtin-info',kind=kind,ticks=2);return json.loads((profile/'regression-builtin.json').read_text())
    def panel():
        state=act('wait',ticks=2)['client'];return next(w for w in state['windows']
            if w.get('definition','').endswith('.QuarkBackpackWindow') and w.get('sessionId')==sid)
    data=snapshot();expect('worn Quark backpack opens exactly one inventory-owned source',len(owned(data))==1)
    source=owned(data)[0];sid=source['session'];row.update(menu_ids=['salts_inventory_compat:quark_backpack'],session=sid)
    expect('worn backpack retains all 27 native cells',sum(not s['player'] for s in source['slots'])==27)
    act('regression-management',session=sid,ticks=2);management=json.loads((profile/'regression-management.json').read_text())
    expect('attached backpack does not expose an independent user close action',not management['available']['USER_CLOSE'])
    before=total(data,'minecraft:diamond');item='minecraft:diamond';amount=7
    act('regression-seed-player',items=[{'slot':9,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    signature=next(v['signature'] for v in snapshot(item)['inventory'] if v['index']==9)
    act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=sid,slot=0,ticks=4)
    expect('worn backpack accepts native storage input without loss',cursor(snapshot(item),item,0) and total(snapshot(item),item)==before+amount)
    old=panel()['bounds'];character=builtin('CHARACTER')
    if character['locked']:act('regression-builtin-control',kind='CHARACTER',control='LOCK',ticks=4)
    act('regression-builtin-move',kind='CHARACTER',dx=-30,dy=20,ticks=8)
    expect('moving Character does not move the inventory-owned backpack',panel()['bounds']==old)
    inventory=builtin('INVENTORY')
    if inventory['locked']:act('regression-builtin-control',kind='INVENTORY',control='LOCK',ticks=4)
    old=panel()['bounds'];before_move=builtin('INVENTORY')['bounds']
    act('regression-builtin-move',kind='INVENTORY',dx=-30,dy=20,ticks=8)
    after=builtin('INVENTORY')['bounds'];now=panel()['bounds']
    expect('real inventory header drag moves its backpack by the same delta',after!=before_move and now['x']-old['x']==after['x']-before_move['x'] and now['y']-old['y']==after['y']-before_move['y'])
    act('regression-slot-click',session=sid,slot=0,ticks=4)
    act('regression-builtin-control',kind='INVENTORY',control='CLOSE',ticks=16);data=snapshot(item)
    expect('inventory close removes its backpack source and preserves carried data',not owned(data) and cursor(data,item,amount) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    expect('backpack parent close keeps Character independent',builtin('CHARACTER')['instance']==character['instance'])
    act('inventory',ticks=22);data=snapshot(item)
    expect('inventory reopen restores a single backpack source',len(owned(data))==1 and cursor(data,item,amount))
    sid=owned(data)[0]['session'];act('regression-slot-click',session=sid,slot=26,ticks=4)
    act('regression-builtin-control',kind='INVENTORY',control='CLOSE',ticks=16);act('inventory',ticks=22);data=snapshot(item)
    expect('stored backpack contents survive its parent close and reopen',len(owned(data))==1 and owned(data)[0]['slots'][26]['count']==amount and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    sid=owned(data)[0]['session'];act('regression-slot-click',session=sid,slot=26,ticks=4);act('regression-player-click',slot=10,ticks=4)
    expect('reopened backpack returns the tagged stack once',cursor(snapshot(item),item,0) and snapshot(item)['inventory'][10]['count']==amount)
    row.update(status='passed',coverage='Real equipped Quark backpack, 27 native cells, no child X, Character independence, real inventory drag/group following, shared cursor on close, stored-item persistence and reopen recovery.')

def exercise(case,row,profile,installed,items,act,expect,snapshot,total,tagged_total,cursor):
    row.setdefault("test_helper_sha256",{})[__name__]=SOURCE_SHA256
    target=case['target_model'];launcher=case['attachment']
    def matching(data):return [m for m in data['menus'] if m['type'].endswith(target)]
    def click(attachment=launcher):act('regression-attachment-click',attachment=attachment,ticks=16)
    def info():
        act('regression-character-info',ticks=2)
        return json.loads((profile/'regression-character.json').read_text())
    before_info=json.loads((profile/'regression-character.json').read_text());data=snapshot()
    expect('character launcher opens exactly one owned equipment source',len(matching(data))==1,target=target)
    model=matching(data)[0];row.update(menu_ids=[case['target_menu']],session=model['session'],native_equipment_slots=sum(not s['player'] for s in model['slots']))
    current=info()
    expect('launcher retains the same Character instance and size',current['instance']==before_info['instance'] and all(current['bounds'][k]==before_info['bounds'][k] for k in ['width','height']))
    aether=case['mod_id']=='aether'
    if aether:
        expect('cloud preserves Character placement',current['bounds']==before_info['bounds'])
        state=act('wait',ticks=2)['client']
        expect('cloud reformats Character without adding a separate equipment window',not any(w.get('menuId')==case['target_menu'] for w in state.get('windows',[])))
        expect('Aether preserves its eight native equipment slots',row['native_equipment_slots']==8)
    cape_signature=None;cape_before=0;cape_slot=None
    if aether and 'aether:red_cape' in items:
        item='aether:red_cape';data=snapshot(item);model=matching(data)[0]
        allowed=[s['index'] for s in model['slots'] if not s['player'] and s['active'] and s['mayPlace'] and not s['count']]
        expect('native Aether rules expose a valid empty cape cell',bool(allowed))
        cape_slot=allowed[0];cape_before=total(data,item)
        act('regression-seed-player',items=[{'slot':9,'item':item,'count':1,'snbt':'{siu_regression_marker:42}'}],ticks=4)
        cape_signature=next(s['signature'] for s in snapshot(item)['inventory'] if s['index']==9)
        act('regression-player-click',slot=9,ticks=4);act('regression-slot-click',session=model['session'],slot=cape_slot,ticks=4)
        data=snapshot(item)
        expect('cape equips through the reformatted Character hit area',cursor(data,item,0) and total(data,item)==cape_before+1 and tagged_total(data,item,cape_signature)==1)
        act('regression-slot-click',session=model['session'],slot=cape_slot,ticks=4);act('regression-player-click',slot=10,ticks=4)
        expect('native cape can be removed without duplication',snapshot(item)['inventory'][10]['count']==1)
        act('regression-player-click',slot=10,ticks=4);act('regression-slot-click',session=model['session'],slot=cape_slot,ticks=4)
    item='minecraft:diamond';amount=7;before=total(snapshot(item),item)
    act('regression-seed-player',items=[{'slot':12,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    signature=next(s['signature'] for s in snapshot(item)['inventory'] if s['index']==12)
    act('regression-player-click',slot=12,ticks=4)
    def preserved(label):
        data=snapshot(item)
        expect(label,cursor(data,item,amount) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
        return data
    def cape_persisted():
        if cape_signature:
            data=snapshot('aether:red_cape')
            expect('reopened Aether source retains the equipped cape and its metadata',total(data,'aether:red_cape')==cape_before+1 and tagged_total(data,'aether:red_cape',cape_signature)==1)
    if aether:
        general=('.accessories.AccessoriesClient$Button' if 'accessories' in installed else '.curios.CuriosCharacterButton' if 'curios' in installed else None)
        for _ in range(3):
            click(general or launcher);data=preserved('equipment view change preserves the shared tagged cursor')
            expect('general or normal view closes the Aether backing source first',not matching(data))
            if general:expect('general equipment source opens exclusively',sum(m['type'].endswith(('.AccessoriesDesktopMenu','.CuriosDesktopMenu')) for m in data['menus'])==1)
            click();data=preserved('cloud restores one equipment source without changing the cursor')
            expect('cloud closes the general source before reopening Aether',len(matching(data))==1 and not any(m['type'].endswith(('.AccessoriesDesktopMenu','.CuriosDesktopMenu')) for m in data['menus']))
            cape_persisted()
    else:
        slots=row['native_equipment_slots'];old=model['session']
        click();data=preserved('closing equipment through its launcher preserves the carried stack')
        expect('launcher closes only its equipment source',not matching(data))
        expect('equipment toggle leaves Character open',info()['instance']==before_info['instance'])
        click();data=preserved('reopening equipment preserves the shared cursor')
        expect('equipment reopens once with its native slot count',len(matching(data))==1 and matching(data)[0]['session']!=old and sum(not s['player'] for s in matching(data)[0]['slots'])==slots)
        for _ in range(3):click();click()
        expect('repeated launcher clicks do not create duplicate equipment sources',len(matching(snapshot(item)))==1)
    act('regression-character-control',control='CLOSE',ticks=16);data=preserved('closing Character preserves the cursor in the independent inventory')
    expect('Character close disposes its equipment source',not matching(data))
    state=act('wait',ticks=2)['client']
    expect('closing equipment parent leaves player inventory available',any(w['kind']=='INVENTORY' for w in state.get('workspaceWindows',[])))
    act('regression-player-click',slot=14,ticks=4)
    data=snapshot(item);expect('equipment lifecycle returns the shared stack exactly once',cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    if cape_signature:
        act('regression-character',ticks=8);click();cape_persisted();model=matching(snapshot('aether:red_cape'))[0]
        act('regression-slot-click',session=model['session'],slot=cape_slot,ticks=4);act('regression-player-click',slot=11,ticks=4)
        expect('native cape is recoverable after closing and reopening Character',snapshot('aether:red_cape')['inventory'][11]['item']=='aether:red_cape')
        act('regression-character-control',control='CLOSE',ticks=12)
    row.update(status='passed',coverage=('Native Character launcher and declared public hit regions, single-source lifecycle, repeated toggles, tagged shared cursor, independent inventory and parent-close cleanup.'+
        (' Aether cape equip/remove, view exclusivity with installed general equipment and persistent gear after reopening.' if aether else ' Native equipment slot count retained; native item-specific equip rules not exhaustively tested.')))
