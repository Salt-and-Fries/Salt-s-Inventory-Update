"""Re-derive original Gallery search, selection, source protection and item persistence."""
from pathlib import Path
import hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from sensor_feature_evidence import typed
from native_publisher_index import index,matches

SCHEMA='native-gallery-held-source-and-window-addressed-selection-v1'
MENU='com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu'
SCREEN='com.teamabnormals.gallery.client.gui.screens.inventory.PaintingSelectorScreen'
PAIRS={('1.20.1/forge','bf-gallery-painting-selector'),('1.21.1/neoforge','atlas-bf-gallery-painting-selector')}
LABELS=['original','initial','protected-source','unrelated-item-picked','unrelated-item-returned','moved','search-7','search-9','scrolled','scrolled-choice','search-12','chosen','closed','reopened','source-invalid']
NAMES=['ordinary-held-painting-opens-original-zero-slot-menu','native-opening-initializes-only-native-variant-data-with-full-original-ownership','native-painting-catalog-populated-with-scrollable-choices','open-selector-protects-complete-held-source-stack','painting-source-lock-leaves-other-inventory-items-interactive','unrelated-item-recovers-exactly-with-source-still-protected','selector-moves-before-native-search-and-selection','native-search-exact-results-7','native-no-match-is-empty-without-item-mutation','native-search-exact-results-9','native-wheel-scrolls-exactly-one-row','moved-scrolled-hit-selects-exact-server-and-client-variant','native-search-exact-results-12','title-search-includes-native-kebab','actual-native-selection-reaches-authoritative-menu','close-applies-only-native-variant-to-original-full-stack','ordinary-reopening-loads-persisted-native-variant','external-source-removal-closes-selector-without-recreating-item']
DRIVER='build/release-readiness/gallery_source_features_714.py'
DRIVER_SHA='0540389b09b1e945131519193b4b8e1d4fbbaa2f547e87671d9d9364ef17819f'
QA_VARIANTS={'1.20.1/forge': {'build/release-readiness/run_gallery_source_714.py': 'b7491f06840d5bec97c69d7f693fa626213834bfd3f181e65dfabd28b1aab7c3', 'build/release-readiness/gallery_source_features_714.py': '0540389b09b1e945131519193b4b8e1d4fbbaa2f547e87671d9d9364ef17819f'}, '1.21.1/neoforge': {'build/release-readiness/run_gallery_source_named_714.py': 'b368d11dd9b9fefbb7267e976a3e2d053d6332566c4a953bc41f41184286da3c', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430', 'build/release-readiness/gallery_source_features_714.py': '0540389b09b1e945131519193b4b8e1d4fbbaa2f547e87671d9d9364ef17819f', 'build/release-readiness/readiness-gallery-1211-neoforge-01/toms-observer/toms.init.gradle': '50c19b0de986133ff346b2d80bb3875d5d911f49837ad9301987311b7f0f52ed', 'build/release-readiness/readiness-gallery-1211-neoforge-01/toms-observer/java/com/salts_inventory_update/functionaltest/EverythingAuditQa.java': 'e678fd728b6368728a20f486a0fb8567ca4a7cca1225b1145b05bde436c47a82', 'build/release-readiness/readiness-gallery-1211-neoforge-01/toms-observer/generated-metadata/resources/pack.mcmeta': 'f6d3f88cbc12aa4513806560d80852c42f60081133149515dadd88773d64514e', 'build/release-readiness/readiness-gallery-1211-neoforge-01/toms-observer/generated-metadata/resources/META-INF/neoforge.mods.toml': 'b934b45a453d81742f7c5f781801ebdc6e5f9fe9055c3fa5fa4194f7cfeab6c8', 'build/release-readiness/readiness-gallery-1211-neoforge-01/toms-observer/generated-metadata/java/com/salts_inventory_update/functionaltest/ChestCloseQaMod.java': '8786a886e1862c900902e1c73111c3842cf3cd4ffc1443032c6b292c58f31d42', 'build/release-readiness/readiness-gallery-1211-neoforge-01/toms-observer/artifact/chest-close-qa-1.21.1-neoforge.jar': '22c389dcfbd8a9cda261cc373892ba1d79d8c10aef70f57b9d57f24a9e80f689'}}

def item(text):
    result=nbtlib.parse_nbt(text);assert isinstance(result,nbtlib.Compound);return result
def stack(text):return typed(item(text))
def inv(state):
    rows=state['inventory'];assert [v['index']for v in rows]==list(range(len(rows)))and len(rows)>=36
    return [stack(v['item'])for v in rows]
def empty_cursors(state):assert state['cursors']and all(not item(v['item'])for v in state['cursors'])
def without_variant(text):
    value=item(text);variant=None
    if 'Count'in value:
        tag=value.get('tag',{});entity=tag.get('EntityTag',{})
        if 'variant'in entity:variant=str(entity.pop('variant'))
        if 'EntityTag'in tag and not entity:del tag['EntityTag']
    else:
        components=value.get('components',{});entity=components.get('minecraft:entity_data',{})
        if 'variant'in entity:variant=str(entity.pop('variant'))
        if 'id'in entity:assert str(entity.pop('id'))=='minecraft:painting'
        if 'minecraft:entity_data'in components and not entity:del components['minecraft:entity_data']
    return typed(value),variant
def search(catalog,query):return[v for v in catalog if any(query.lower()in v[k].lower()for k in ['title','author','dimensions'])]

def validate(target,journal,states):
    assert target in {t for t,c in PAIRS}and set(states)==set(LABELS)
    assert journal['complete']is True and journal['cases']==['gallery-painting-selector']and journal['driver_sha256']==DRIVER_SHA
    assert journal['checks']==[dict(name=n,passed=True)for n in NAMES]
    original,initial=states['original'],states['initial'];base=inv(initial);assert len(base)==len(inv(original))
    for slot,id,count in [(0,'minecraft:painting',3),(9,'minecraft:diamond',7)]:
        v=item(original['inventory'][slot]['item']);assert str(v['id'])==id
        key='Count'if 'Count'in v else'count';assert int(v[key])==count
        data=v['tag']if key=='Count'else v['components']['minecraft:custom_data']
        assert type(data['release_gallery_marker'])is nbtlib.Int and int(data['release_gallery_marker'])==61
        assert type(data['typed'])is nbtlib.ByteArray and list(data['typed'])==[4,8]
    assert all(not item(v['item'])for v in original['inventory']if v['index']not in [0,9])
    expected=inv(original);expected[0]=base[0];assert base==expected
    assert without_variant(original['inventory'][0]['item'])==without_variant(initial['inventory'][0]['item'])
    empty_cursors(original)
    catalog=initial['client']['allPaintings'];assert len(catalog)>20 and len({v['id']for v in catalog})==len(catalog)
    session=initial['client']['session'];assert type(session)is int and session>0
    open_labels=LABELS[1:12]+['reopened']
    for label in open_labels:
        s=states[label];c=s['client'];assert s['menus']==1 and c['menu']==MENU and c['screen']==SCREEN and c['slots']==0
        assert c['allPaintings']==catalog and type(c['locked'])is bool
        assert c['paintings']==search(catalog,c['search'])
        assert type(c['startIndex'])is int and c['startIndex']>=0
        b=c['bounds'];assert set(b)=={'x','y','width','height'}and all(type(v)is int for v in b.values())and min(b['width'],b['height'])>0
        if label!='reopened':assert c['session']==session
        if label not in ['unrelated-item-picked','reopened']:assert inv(s)==base;empty_cursors(s)
    held=list(base);held[9]=stack('{}');picked=states['unrelated-item-picked'];assert inv(picked)==held
    carried=[stack(v['item'])for v in picked['cursors']if item(v['item'])];assert carried and all(v==base[9]for v in carried)
    before,moved=states['protected-source']['client']['bounds'],states['moved']['client']['bounds']
    assert(moved['x'],moved['y'],moved['width'],moved['height'])==(before['x']+16,before['y']+12,before['width'],before['height'])
    for label,query in [('initial',''),('search-7','no-siu-painting-match-917'),('search-9','')]:
        c=states[label]['client'];assert c['search']==query and c['startIndex']==0
        if label!='initial':assert c['searchFocused']is True
    assert states['search-7']['client']['paintings']==[]
    scrolled=states['scrolled']['client'];assert scrolled['search']==''and scrolled['startIndex']==4 and scrolled['scrollOffs']>0
    wanted=scrolled['paintings'][4]['id'];assert states['scrolled-choice']['selected']==states['scrolled-choice']['client']['selected']==wanted
    kebab=next(v for v in catalog if v['id']=='minecraft:kebab');filtered=states['search-12']['client'];assert filtered['search']==kebab['title']and filtered['searchFocused']is True and filtered['startIndex']==0
    assert any(v['id']=='minecraft:kebab'for v in filtered['paintings'])
    chosen=states['chosen'];assert chosen['selected']==chosen['client']['selected']=='minecraft:kebab'
    closed,reopened,invalid=[states[k]for k in ['closed','reopened','source-invalid']]
    expected=list(base);expected[0]=stack(closed['inventory'][0]['item']);assert closed['menus']==0 and inv(closed)==expected
    assert without_variant(closed['inventory'][0]['item'])==(without_variant(initial['inventory'][0]['item'])[0],'minecraft:kebab')
    assert reopened['client']['session']!=session and reopened['selected']==reopened['client']['selected']=='minecraft:kebab'and inv(reopened)==expected
    expected[0]=stack('{}');assert invalid['menus']==0 and inv(invalid)==expected
    for s in [closed,reopened,invalid]:empty_cursors(s)
    return dict(assertions=18,states=15,exact_native_selection_persistence=True,original_source_protected=True)

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target'];assert(target,case)in PAIRS and original_classes==[MENU]
    verify_sealed(root,sealed,expected)
    qa={k:v for k,v in sealed['archived_inputs'].items()if k.startswith('build/release-readiness/')};assert qa==QA_VARIANTS[target]
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];r=row['results'];assert summary['status']=='passed'and row['exit_code']==0 and r['gallery_features_checked']is True and r['checks']==18 and r['cases']==1 and r['failed']==0
    journal_path=checked(root,sealed['acceptance']);assert journal_path==(root/row['profile']).resolve()/'gallery-release/summary.json'
    journal=json.loads(journal_path.read_text());states={};refs=[];files=sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json'));assert len(files)==15
    for i,(label,path)in enumerate(zip(LABELS,files),1):
        assert path.name==f'{i:03}-{label}.json';ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref);states[label]=json.loads(path.read_text())
    validation=validate(target,journal,states);publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith('gallery-'):continue
        p=Path(artifact['file']);p=p if p.is_absolute()else root/p;raw=p.read_bytes();assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if'sha512'in artifact:assert digest==artifact['sha512']
        locations=matches(index(p,digest,target.split('/')[1]),[MENU,SCREEN]);assert locations
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    return dict(schema=SCHEMA,target=target,case=case,subjects=sealed['subjects'],sealed_evidence=reference(root,Path(sealed_path)),acceptance=reference(root,journal_path),full_states=refs,publishers=publishers,validation=validation,scope='Native painting selector: actual search, no-match, scroll, moved selection, complete source protection, unrelated inventory use, exact native close/reopen variant and source invalidation.',limits='Two exact publisher targets only; not every painting placement, resource pack or localization.')
def verify(root,record,expected):
    assert record==derive(root,checked(root,record['sealed_evidence']),expected,record['case'],[MENU]);return record
