"""Explicit native-selector acceptance from sealed, exact-publisher evidence."""
from pathlib import Path
import hashlib,json,zipfile
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches

CASE='bb-polymer-creative-browser'
TARGET='1.21.1/fabric'
MENU='eu.pb4.polymer.core.impl.ui.MicroUi$InternalScreenHandler'
CHECKS={
    'Polymer browser retains its real native action handler',
    'creative category selector remains native instead of owned storage',
    'native category click replaces the category grid with its item list',
    'category navigation grants no physical items and keeps the cursor',
    'native creative browser closes without leaving a desktop source',
    'public Polymer command reopens the same category browser',
    'native selector veto preserves ordinary vanilla barrel windows',
    'ordinary barrel still accepts the exact tagged seven-item stack',
    'vanilla-shaped storage still returns items once with their metadata',
}


def validate_case(case,checks):
    assert case['id']==CASE and case['status']=='passed'and case['polymer_browser_checked']is True
    assert case['mod_id']=='polymer-core'and case['entry_native_player_menu']['class']==MENU
    assert case['native_screen_roundtrip']['menu']==MENU
    assert any(a.get('type')=='command'and a.get('command','').endswith('run polymer creative')for a in case['actions'])
    nav=case['native_browser_navigation'];assert nav['handler']==MENU
    hashes=[nav[k]for k in ['category_grid_sha256','item_grid_sha256']]
    assert hashes[0]!=hashes[1]and all(len(v)==64 and all(c in '0123456789abcdef'for c in v)for v in hashes)
    assert {'minecraft:oak_log','minecraft:oak_planks'}<=set(nav['item_sample'])
    selected=[v for v in checks if v['case']==CASE]
    assert selected and all(v.get('passed')is True for v in selected)
    names=[v['check']for v in selected]
    assert len(names)==len(set(names))and CHECKS<=set(names),'Missing or duplicated native feature assertion'
    return len(selected)


def exact_module_alias(path,expected_sha512,module_hashes):
    raw=Path(path).read_bytes();assert hashlib.sha512(raw).hexdigest()==expected_sha512
    if hashlib.sha256(raw).hexdigest()not in module_hashes:return False
    with zipfile.ZipFile(path)as jar:return MENU.replace('.','/')+'.class'in jar.namelist()


def derive(root,sealed_path,expected):
    root=Path(root).resolve();sealed_path=Path(sealed_path);sealed=json.loads(sealed_path.read_text(encoding='utf-8'))
    assert sealed['target']==TARGET
    verify_sealed(root,sealed,expected)
    report=checked(root,sealed['acceptance']);data=json.loads(report.read_text(encoding='utf-8'))
    cases=[v for v in data['cases']if v['id']==CASE];assert len(cases)==1
    count=validate_case(cases[0],data['checks'])
    staged=json.loads(checked(root,sealed['staged']).read_text(encoding='utf-8'))['artifacts'];publishers=[]
    for artifact in staged:
        if artifact['family']!='upstream':continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path
        assert hashlib.sha256(path.read_bytes()).hexdigest()==artifact['sha256']
        locations=matches(index(path,artifact['sha512'],'fabric'),[MENU])
        if locations:publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=artifact['sha512'],class_locations=locations))
    assert publishers,'Native selector handler must exist in the exact staged publisher'
    # The raw launcher omits a standalone dependency when its exact bytes are
    # already declared inside a staged parent. Qualify only that byte-identical
    # root module, never a same-version file or a different enclosing publisher.
    module_hashes={loc['embedded_chain'][-1]['sha256']if loc['embedded_chain']else p['sha256']
        for p in publishers for loc in p['class_locations']}
    from run_mod_matrix import FIXTURES
    aliases=[];manifests=[];seen=set()
    for fixture in FIXTURES[TARGET]:
        manifest=root/'build/all-target-rollout/fixtures'/fixture/'fixture.json';manifests.append(reference(root,manifest))
        for artifact in json.loads(manifest.read_text())['artifacts']:
            key=(artifact['filename'],artifact['sha512'])
            if key in seen or any(p['sha512']==artifact['sha512']for p in publishers):continue
            seen.add(key);path=root/artifact['path']
            if exact_module_alias(path,artifact['sha512'],module_hashes):
                aliases.append(dict(filename=artifact['filename'],sha512=artifact['sha512'],sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                    equivalence='Complete standalone native jar bytes equal a hash-verified declared module in the staged runtime. No version-name equivalence.'))
    return dict(target=TARGET,case=CASE,disposition='native-only-qualified',native_classes=[MENU],
        sealed_evidence=reference(root,sealed_path),report=reference(root,report),checks=count,publishers=publishers,subjects={k:list(v)for k,v in expected.items()},
        exact_module_aliases=aliases,native_alias_manifests=manifests,
        classification_note='Historical command attempts in other cohorts had no Polymer publisher installed. The applicable exact publisher cohort entered and exercised the original native MicroUi selector. Those absent-publisher fixture attempts do not describe an unavailable Polymer interface or a container defect.',
        scope='Native category navigation, physical inventory/cursor conservation, Escape/public-command reopening and tagged ordinary-barrel transfers after the native selector closes.',
        limits='This is an intentionally native creative action selector, not owned storage. Creative item granting, other native editor actions and other versions are not certified by this acceptance.')


def verify(root,record,expected):
    path=checked(root,record['sealed_evidence'])
    assert derive(root,path,expected)==record,'Native selector acceptance differs from its sealed evidence'
    return True
