"""Real SSN master/cable/barrel storage and native request/insert packets."""

from pathlib import Path
import hashlib
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
import json

def exercise(row,profile,initial,blocks,act,expect,snapshot,management,total,tagged_total,cursor):
    row.setdefault("test_helper_sha256",{})[__name__]=SOURCE_SHA256
    menu=next(m for m in initial['menus'] if m['type'].startswith('com.lothrazar.storagenetwork.') and 'Network' in m['type'])
    sid=menu['session'];row['session']=sid
    item='minecraft:diamond';amount=7;before=total(snapshot(item),item)
    def stored():
        act('regression-native-container-ledger',dx=3,ticks=2)
        return json.loads((profile/'regression-native-container-ledger.json').read_text())['stacks']
    def balance(signature):
        return sum(v['count'] for v in stored() if v['item']==item and v['signature']==signature)
    def tap(wanted=''):
        act('regression-ssn-repository-point',session=sid,item=wanted,ticks=5)
        point=json.loads((profile/'regression-point.json').read_text())
        act('regression-pointer-position',**point,ticks=8)
        act('regression-ssn-hover',session=sid,ticks=2)
        hover=json.loads((profile/'regression-ssn-hover.json').read_text())
        if wanted:
            expect('native SSN renderer resolves the requested item under the callback pointer',hover['stack']['item']==wanted,
                   pointer=point,native_hover=hover)
            row['native_hover_contract']='Native MouseHandler.onMove callback, normal render settle, observed publisher hover before click; no hover-cache injection.'
        act('regression-tap',session=sid,**point,ticks=24)
    expect('native connected SSN storage starts empty',all(v['count']==0 for v in stored()))
    act('regression-seed-player',items=[{'slot':10,'item':item,'count':amount,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    signature=snapshot(item)['inventory'][10]['signature']
    old=management(sid)['bounds']
    if management(sid)['locked']:act('regression-control',session=sid,control='LOCK',ticks=4)
    movable=management(sid)['available']['MOVE'];act('regression-move',session=sid,dx=-30,dy=20,ticks=6)
    expect('SSN terminal follows its declared movement policy',(management(sid)['bounds']!=old)==movable)
    act('regression-player-click',slot=10,ticks=4);tap()
    data=snapshot(item)
    expect('native SSN insert packet stores the complete tagged cursor stack',cursor(data,item,0) and balance(signature)==amount and total(data,item)==before)
    partner=next((v for v in ['netherchested:nether_chest','sophisticatedstorage:barrel','supplementaries:sack'] if v in blocks),'minecraft:barrel')
    retained={m['session'] for m in data['menus']}
    act('regression-place',block=partner,nearPlayer=True,dx=-1,dz=1,ticks=6)
    act('regression-open',nearPlayer=True,dx=-1,dz=1,emptyHand=True,ticks=18)
    data=snapshot(item);other=next(m for m in data['menus'] if m['session'] not in retained)
    slot=next(s['index'] for s in other['slots'] if not s['player'] and not s['virtual'] and s['active'] and s.get('visible',True) and s['mayPlace'] and not s['count'])
    tap(item);data=snapshot(item)
    expect('native SSN request packet returns the same complete item data',cursor(data,item,amount) and balance(signature)==0 and tagged_total(data,item,signature)==amount)
    act('regression-slot-click',session=other['session'],slot=slot,ticks=4)
    data=snapshot(item)
    expect('SSN withdrawal transfers once into independent storage',cursor(data,item,0) and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    act('regression-slot-click',session=other['session'],slot=slot,ticks=4);tap()
    expect('storage return reaches the original SSN network',balance(signature)==amount and cursor(snapshot(item),item,0))
    act('regression-close',session=other['session'],ticks=6)
    tap(item);act('regression-close',session=sid,ticks=6)
    data=snapshot(item)
    expect('closing an SSN terminal with its withdrawal preserves every unit and component',balance(signature)==0 and total(data,item)==before+amount and tagged_total(data,item,signature)==amount)
    act('regression-player-click',slot=12,ticks=4)
    data=snapshot(item)
    expect('SSN withdrawal returns once to the requested player cell',cursor(data,item,0) and data['inventory'][12]['count']==amount and data['inventory'][12]['signature']==signature)
    row.update(status='passed',ssn_repository_checked=True,cross_mod_partner=partner,
               coverage='Native connected master/cable/barrel network, real repository insert/request packets after movement, exact tagged native storage balance, cross-storage transfer, independent close and terminal close with a carried stack. No exhaustive crafting or automation recipe claim.')
