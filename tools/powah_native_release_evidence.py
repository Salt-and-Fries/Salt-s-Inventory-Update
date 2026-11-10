"""Reconstruct original Powah controls and full resource ledgers from sealed states."""
from pathlib import Path
from collections import Counter
import json
from focused_release_evidence import checked,reference,verify as verify_sealed
import powah_legacy_features as legacy
import powah_modern_features as modern
from powah_native_cursor_ownership import conserved as canonical_only_conserved

TARGETS={'1.20.1/fabric':74,'1.20.1/forge':74,'1.21.1/neoforge':110,'26.1.2/neoforge':74}
SCHEMA='powah-native-controls-and-canonical-only-modern-cursor-v2'
BASE_CASES=[v[0] for v in legacy.CASES]

def check_names(case):
    cable=case=='energy_cable'
    names=['exact typed declared resources and empty actual source prerequisites',
           'original publisher menu owns the complete expected cells and synchronized native settings']
    if cable:names+=['actual native ray connection direction is preserved by both menus']
    if case=='player_transmitter':names += [
        'original native item use binds exactly the player UUID and name preserving all other typed data',
        'ordinary player input moves the full bound card away from the opening hand']
    names += ['initial native face'+str(i)+' control changes only the applicable published direction' for i in ([5] if cable else range(6))]
    if not cable:names += ['original all-face action retains its exact native transition and applicability']
    names += ['redstone'+str(i)+' follows the original exact native enum cycle' for i in range(3)]
    names += ['all three original redstone modes are reachable',
        'window drags before its exterior controls are exercised',
        'moved native face'+str(5 if cable else 0)+' control changes only the applicable published direction',
        'original native settings and complete world items persist after ordinary reopening',
        'final closure releases every source with complete items and empty canonical cursor']
    if case=='player_transmitter':names += [
        'unbound native card is refused without changing its complete cursor or world slot',
        'foreign typed item is refused without changing its complete cursor or world slot',
        'native bound-card slot and actual world inventory own the same full typed item',
        'the full typed bound card recovers exactly once through ordinary input']
    if case=='solar_panel':names += [
        'native night fixture contains no assigned solar energy',
        'actual unobstructed daylight ticks produce native positive energy',
        'original client and server detect actual sky obstruction',
        'obstructed native energy remains stable and synchronizes exactly',
        'removing the actual obstruction resumes native solar production']
    if case=='energy_discharger':names += [
        'discharger native ON mode pauses without a powered neighbor',
        'declared native battery contains exact typed components and1000FE',
        'discharger refuses foreign typed item without changing cursor or real cells',
        *['discharger cell'+str(i)+' owns exact charged battery in native world and menu' for i in range(7)],
        *['discharger cell'+str(i)+' recovers the entire charged item exactly once' for i in range(7)],
        'native discharger energy gain balances exact item energy loss with every other component preserved',
        'discharged native item persists across closure and recovers without loss or duplication']
    return names

def validate(target,journal,frames,legacy_hash,modern_hash=None):
    assert target in TARGETS
    mc=target.split('/')[0];old=mc=='1.20.1';base=legacy if old else modern.base
    original_conserved=base.conserved
    conserved=original_conserved if old else lambda server,expected:canonical_only_conserved(original_conserved,base.item,server,expected)
    cases=BASE_CASES+(['energy_discharger'] if mc=='1.21.1' else [])
    assert journal['complete'] is True and journal['cases']==cases
    assert journal['driver_sha256']==(legacy_hash if old else modern_hash)
    if not old:
        assert journal['target']==target and journal['original_assertions_source_sha256']==legacy_hash==modern.LEGACY_SHA256
    wanted=Counter((case,name,True)for case in cases for name in check_names(case))
    actual=Counter((v['case'],v['name'],v['passed'])for v in journal['checks'])
    assert len(journal['checks'])==TARGETS[target] and actual==wanted and all(v['passed'] is True for v in journal['checks'])
    grouped={case:[] for case in cases};order=[]
    for label,state in frames:
        assert set(state)=={'client','server','ray'}
        matches=[case for case in cases if label.startswith(case+'-')];assert len(matches)==1
        case=matches[0];grouped[case].append((label[len(case)+1:],state))
        if not order or order[-1]!=case:order.append(case)
    assert order==cases
    classes={};summaries=[]
    marker=base.expected_item('1.20.1','minecraft:diamond',7,base.DIAMOND)
    for case,rows in grouped.items():
        count=7 if case=='energy_discharger' else next(v[3]for v in legacy.CASES if v[0]==case)
        stem='DischargerContainer' if case=='energy_discharger' else next(v[2]for v in legacy.CASES if v[0]==case)
        menu='owmii.powah.inventory.'+(stem.replace('Container','Menu') if mc=='26.1.2' else stem)
        labels=Counter(label for label,_ in rows)
        expected_labels=Counter(['independent-prerequisites','native-entry','native-entry','actually-moved',
            'closed-world-owns-real-items','all-owned-sources-closed',*['redstone-'+str(i)for i in range(3)],
            *['face-'+str(i)for i in ([5] if case=='energy_cable' else range(6))],
            'face-'+str(5 if case=='energy_cable'else 0)])
        if case!='energy_cable':expected_labels['all-faces']=1
        if case=='player_transmitter':expected_labels.update(['original-native-item-binding','native-bound-card-moved',
            'refused-10','refused-12','refused-input-restored','refused-input-restored','native-bound-card-inserted','full-bound-card-recovered'])
        if case=='solar_panel':expected_labels.update(['native-daylight-generation','actual-sky-obstruction','native-obstructed-stable','original-generation-resumes'])
        if case=='energy_discharger':
            expected_labels.update(['declared-charged-native-battery-prerequisite','discharger-refuses-foreign-item','discharger-foreign-item-restored',
                *['native-discharger-cell'+str(i) for i in range(7)],*['native-discharger-cell'+str(i)+'-recovered'for i in range(7)],
                'paused-battery-for-real-discharge','actual-native-battery-discharge','native-discharge-paused-again',
                'discharger-closed-native-world-retains-battery','native-entry','discharger-post-discharge-item-recovered'])
            assert 0<=labels['discharger-native-pause-click']<=2
            expected_labels['discharger-native-pause-click']=labels['discharger-native-pause-click']
        assert labels==+expected_labels,(case,labels,+expected_labels)
        unique=lambda name:next(v for label,v in rows if label==name)
        first=rows[0][1]['server'];assert rows[0][0]=='independent-prerequisites'
        assert not rows[0][1]['client'] and first['ownedSessions']==0 and not base.items([v['item']for v in first['world']['items']])
        bound=base.bound_card(first['uuid'],first['name']) if case=='player_transmitter' else None
        card_bound=False;charged=False;remaining=modern.ENERGY;previous=None;last_before_close=None;screens=set();entries=0
        for label,state in rows:
            server=state['server'];clients=state['client'];menus=server['menus']
            expected=base.items([marker])
            if case=='player_transmitter':
                if label=='original-native-item-binding':card_bound=True
                expected+=base.items([bound if card_bound else base.expected_item('1.20.1','powah:binding_card',1,base.CARD),base.expected_item('1.20.1','powah:binding_card',1,base.BAD)])
            if case=='energy_discharger':
                if label=='declared-charged-native-battery-prerequisite':charged=True
                if label=='actual-native-battery-discharge':
                    assert previous and len(menus)==1
                    gained=menus[0]['values']['stored']-previous['server']['menus'][0]['values']['stored']
                    assert type(gained)is int and 0<gained<=modern.ENERGY
                    remaining=modern.ENERGY-gained
                if charged:expected+=base.items([modern.battery(remaining)])
            assert conserved(server,expected),(case,label,'full typed ownership')
            assert len(clients)==len(menus)<=1
            if menus:
                c,m=clients[0],menus[0];v=m['values']
                assert c['session']==m['session'] and c['menu']==m['menu']==menu and m['valid']is True
                assert base.settings_equal(c['values'],v)
                assert v['tile']==server['world']['tile'] and v['tile'].startswith('owmii.powah.')
                assert len([s for s in m['slots']if s['virtual']is False])==count
                assert c['screen'].startswith('owmii.powah.');screens.add(c['screen'])
                if label=='native-entry':
                    entries+=1
                    if case=='energy_cable':assert base.ray_side(state['ray'])==c['values']['face']==v['face']=='east'
                    if last_before_close is not None:
                        assert all(v[k]==last_before_close[k]for k in ['redstone','faces','transfer','available'])
                        last_before_close=None
                if label.startswith('face-') or label=='all-faces':
                    before=previous['server']['menus'][0]['values'];index=int(label[5:])if label.startswith('face-')else None
                    assert v['faces']==base.next_faces(before,index) and v['redstone']==before['redstone']
                if label.startswith('redstone-'):
                    before=previous['server']['menus'][0]['values']
                    assert v['redstone']==base.REDSTONE[(base.REDSTONE.index(before['redstone'])+1)%3] and v['faces']==before['faces']
                if label=='actually-moved':
                    before=previous['client'][0]['bounds'];after=c['bounds']
                    assert(after['x'],after['y'])==(before['x']+16,before['y']+12)
            else:assert server['ownedSessions']==0
            if label=='closed-world-owns-real-items':
                last_before_close=previous['server']['menus'][0]['values']
                assert server['world']['items']==previous['server']['world']['items'] and not clients and not menus
            if label=='all-owned-sources-closed':
                assert not menus and not clients and server['ownedSessions']==0 and not base.items([v['item']for v in server['world']['items']]) and base.item(server['cursor'])[0]==0
            previous=state
        assert len(screens)==1 and entries==(3 if case=='energy_discharger' else 2)
        if case=='player_transmitter':
            for label,index in [('original-native-item-binding',0),('native-bound-card-moved',9),('full-bound-card-recovered',9)]:
                assert base.item(unique(label)['server']['inventory'][index]['item'])==base.item(bound)
            for label,wanted_item in [('refused-10',base.expected_item('1.20.1','powah:binding_card',1,base.BAD)),('refused-12',marker)]:
                server=unique(label)['server'];assert base.item(server['cursor'])==base.item(wanted_item) and not base.items([v['item']for v in server['world']['items']])
            inserted=unique('native-bound-card-inserted')['server'];assert base.item(inserted['world']['items'][0]['item'])==base.item(bound) and base.item(inserted['cursor'])[0]==0
        if case=='solar_panel':
            entry=next(v for label,v in rows if label=='native-entry');assert entry['client'][0]['values']['stored']==entry['server']['menus'][0]['values']['stored']==0
            day=unique('native-daylight-generation');assert day['server']['menus'][0]['values']['canSeeSky']is True and day['server']['menus'][0]['values']['stored']>0 and day['client'][0]['values']['stored']>0
            stop=unique('actual-sky-obstruction');stable=unique('native-obstructed-stable');resume=unique('original-generation-resumes')
            assert stop['server']['menus'][0]['values']['canSeeSky']is stop['client'][0]['values']['canSeeSky']is False
            value=stop['server']['menus'][0]['values']['stored'];assert stable['server']['menus'][0]['values']['stored']==stable['client'][0]['values']['stored']==value
            assert resume['server']['menus'][0]['values']['canSeeSky']is True and resume['server']['menus'][0]['values']['stored']>value and resume['client'][0]['values']['stored']>value
        if case=='energy_discharger':
            charged_state=unique('declared-charged-native-battery-prerequisite');assert base.item(charged_state['server']['inventory'][9]['item'])==base.item(modern.battery(modern.ENERGY))
            assert charged_state['server']['menus'][0]['values']['redstone']=='ON'
            for i in range(7):
                ins=unique('native-discharger-cell'+str(i))['server'];back=unique('native-discharger-cell'+str(i)+'-recovered')['server']
                assert base.item(ins['world']['items'][i]['item'])==base.item(modern.battery(modern.ENERGY)) and base.item(ins['cursor'])[0]==0
                assert base.item(back['inventory'][9]['item'])==base.item(modern.battery(modern.ENERGY)) and not base.items([v['item']for v in back['world']['items']])
            recovered=unique('discharger-post-discharge-item-recovered')['server'];assert base.item(recovered['inventory'][9]['item'])==base.item(modern.battery(remaining))
            actual=unique('actual-native-battery-discharge');assert actual['client'][0]['values']['stored']==actual['server']['menus'][0]['values']['stored'] and actual['server']['menus'][0]['values']['redstone']=='OFF'
        classes[case]=[menu,*sorted(screens)];summaries.append(dict(case=case,assertions=len(check_names(case)),states=len(rows),native_cells=count))
    return dict(assertions=TARGETS[target],states=len(frames),cases=summaries,native_classes=classes)

def derive(root,seal_path,expected):
    root=Path(root).resolve();seal_path=Path(seal_path);seal=json.loads(seal_path.read_text());verify_sealed(root,seal,expected)
    target=seal['target'];assert target in TARGETS
    if not target.startswith('1.20.1/'):
        policy=reference(root,root/'tools/powah_native_cursor_ownership.py')
        assert seal['archived_inputs']['build/release-readiness/powah_native_cursor_ownership_1570.py']==policy['sha256'], 'Actual modern QA must archive the exact canonical-only cursor ledger policy'
    summary=json.loads(checked(root,seal['summary']).read_text());row=summary['results'][0]
    flag='powah_legacy_features_checked'if target.startswith('1.20.1/')else'powah_modern_features_checked'
    assert summary['status']==row['status']=='passed' and row['exit_code']==0 and row['results'][flag]is True
    assert row['results']['failed']==0 and row['results']['checks']==TARGETS[target]
    p=checked(root,seal['acceptance']);frames=[];refs=[]
    for i,path in enumerate(sorted(p.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert path.name.startswith(str(i).zfill(3)+'-');ref=reference(root,path);assert ref in seal['full_states'];refs.append(ref);frames.append((path.name[4:-5],json.loads(path.read_text())))
    assert len(refs)==len(seal['full_states'])
    inputs=seal['archived_inputs'];validation=validate(target,json.loads(p.read_text()),frames,inputs['functional-tests/release-specialized/powah_legacy_features.py'],inputs.get('functional-tests/release-specialized/powah_modern_features.py'))
    return dict(schema=SCHEMA,status='original-native-powah-controls-and-complete-typed-states-qualified',target=target,subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,p),validation=validation,native_states=refs,scope='Original native ray/face and redstone controls, moved exterior controls, player-bound card refusal/ownership/recovery, ordinary reopening and actual solar generation/obstruction/resumption. The 1.21.1 discharger additionally retains seven physical-cell round trips and exact observed machine/item energy balance.',limits='Exact sealed publisher and original feature workflow only. No arbitrary energy networks, transmitter charging, automation, endurance, every visual variant or new gameplay execution is claimed.')
