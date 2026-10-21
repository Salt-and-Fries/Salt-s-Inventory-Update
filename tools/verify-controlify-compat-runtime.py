"""Validate retained 26.3 controller/Extended/mod gameplay reports against final release bytes."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
from urllib.parse import unquote
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('controller_runtime', ROOT/'tools/verify-controlify-runtime.py')
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
PINS = json.loads((ROOT/'controlify/targets.json').read_text(encoding='utf-8-sig'))['targets']
MOD_PINS = json.loads((ROOT/'controlify/functional-tests/mod-fixtures-26.3.json').read_text())['profiles']
API_CHECKS = {
    'compat-api-tab-snapped', 'compat-api-native-tab-activated', 'compat-api-relayout-current-geometry',
    'compat-api-virtual-entry-retains-owner-and-no-real-slot', 'compat-api-edge-scroll-native-handler',
    'compat-api-right-stick-scrolls-native-list', 'compat-api-custom-text-opens-native-keyboard',
    'compat-api-custom-editor-receives-unicode', 'compat-api-keyboard-blocks-window-and-recipe-wheels',
    'compat-api-keyboard-returns-workspace', 'compat-api-normal-controller-resumes',
}
BROWSER_CHECKS = {'compat-browser-real-provider','compat-browser-correct-provider','compat-browser-tabs-selectable','compat-browser-tab-native-selection',
    'compat-browser-custom-keyboard-edits-native-filter','compat-browser-virtual-item-navigable','compat-browser-real-recipe-view',
    'compat-browser-recipe-ingredient-navigable','compat-browser-virtual-recipe-retains-inventory','compat-browser-recipe-geometry-uses-native-hit-test',
    'compat-browser-controls-inside-presentation-bounds','compat-browser-fitted-panes-bounded','compat-browser-fitted-panes-separate',
    'compat-browser-adjacent-ingredients-do-not-skip'}
REI_RENDER_CHECKS={f'compat-browser-{kind}-{check}' for kind in ['entry','recipe'] for check in [
    'native-items-extracted','retains-window-transform','retains-window-clip']}
FOCUSED_CHECKS = {
    'crafting-qol': {f'qol-{pair}-{direction}-exact-destination' for pair in ['table','character']
        for direction in ['main-to-hotbar','hotbar-to-main']} | {
        'qol-character-hotbar-priority-output-placement','qol-table-hotbar-priority-output-placement',
        'qol-table-full-hotbar-fallback-output-placement','qol-close-returns-character-inputs',
        'qol-full-inventory-drops-exact-overflow','qol-keyboard-close-also-clears-grid'},
    'wheel-stability': {f'stability-{kind}-{i}' for kind in ['center-unchanged','icons-opaque'] for i in range(18)} | {
        'stability-refresh-fresh-native-recipes','stability-refresh-reuses-alphabet-order',
        'stability-reopen-fresh-native-recipes','stability-reopen-reuses-alphabet-order',
        'stability-every-window-has-icon','stability-tabs-no-text-labels','stability-tabs-clipped-top-visible-icons',
        'stability-tabs-use-window-wheel-icons','stability-top-guide-clears-hanging-tabs','stability-tabs-restore-windows'},
    'wheel-polish': {f'polish-{station}-hover-builds-no-recipe-catalog' for station in ['crafting-table','furnace','character']} | {
        f'polish-{filter}-{check}' for filter in ['filter-parts','filter-craftable','filter-wraps-all','furnace-parts','furnace-craftable']
        for check in ['state','saved','membership']} | {
        'polish-all-includes-unavailable-recipe','polish-parts-includes-incomplete-oak-wood',
        'polish-craftable-excludes-incomplete-oak-wood','polish-reopen-retains-craftable','polish-center-inside-ring',
        'polish-coal-alone-does-not-match-smelting-recipes','polish-parts-refreshes-open-container-resources',
        'polish-parts-retains-open-wheel-membership','polish-parts-new-ingredient-visible-after-reopen',
        'polish-2x2-retains-craftable-filter'},
}

def sha(path, algorithm='sha256'):
    return hashlib.new(algorithm, path.read_bytes()).hexdigest()

def require(passed, expected, name):
    assert expected <= passed, f'{name}: missing required coverage: {sorted(expected-passed)}'

def validate(folder, artifacts, allow_core_description_correction=False):
    name = folder.name
    report = json.loads((folder/'results.json').read_text(encoding='utf-8-sig'))
    staging = json.loads((folder/'staged-artifacts.json').read_text(encoding='utf-8-sig'))
    loader, game = staging['loader'], staging['minecraft']
    assert game == '26.3' and loader in ['fabric','neoforge']
    pin = next(p for p in PINS if p['minecraft']==game and p['loader']==loader)
    records = report['results']
    assert report['failures']==0 and records and all(r['status'] in ['PASS','CAPTURED','OBSERVED'] for r in records), name
    passed = {r['name'] for r in records if r['status']=='PASS'}
    runtime.host_motion_isolation(report, name)
    inventory = {entry['file']:entry for entry in staging['artifacts']}
    assert len(inventory)==len(staging['artifacts']), name
    base = ROOT/f'controlify/versions/{game}/{loader}/build/runtime-smoke'
    candidates = [base/staging['runtimeProfile']] if 'runtimeProfile' in staging else [base/('packaged-controlify-'+name),base/'packaged-controlify']
    def matches(candidate):
        actual = list((candidate/'mods').glob('*.jar'))
        return {p.name for p in actual}==set(inventory) and all(sha(p)==inventory[p.name]['sha256'] for p in actual)
    profile = next((candidate for candidate in candidates if matches(candidate)),None)
    assert profile is not None, f'{name}: preserved staged jars differ from the launch inventory'
    for key in ['coreCodeSource','companionCodeSource','controlifyCodeSource']:
        source = unquote(report[key]).replace('\\','/').lower()
        assert '.jar' in source and '/classes/' not in source and profile.as_posix().lower() in source, (name,key,source)
    allowed = {(ROOT/f'controlify/versions/{game}/{loader}/build/{directory}/controlifyQa').resolve()
        for directory in ['classes/java','resources']} if loader=='fabric' else set()
    assert all(Path(p).resolve() in allowed for p in staging['projectClasspath']), name
    hashes = {sha(p,'sha512') for p in (profile/'mods').glob('*.jar')}
    for dependency in [pin['controller'],pin['yacl'],*pin.get('runtime_dependencies',[])]:
        assert dependency['sha512'] in hashes, (name,dependency)
    optional = None
    if (folder/'optional-mods.json').is_file():
        optional = json.loads((folder/'optional-mods.json').read_text(encoding='utf-8-sig'))
        expected = MOD_PINS[loader+'-'+optional['browser']]
        assert {(r['version_id'],r['file']['filename']) for r in optional['mods']} == {(r['version_id'],r['file']['filename']) for r in expected}
        for row in expected:
            assert sha(profile/'mods'/row['file']['filename'],'sha512') == row['file']['hashes']['sha512'], name
    mods = report['loadedMods']
    if isinstance(mods,list): mods = {row['id']:row['version'] for row in mods}
    assert 'salts_inventory_compat' in mods, name
    releases = {}
    artifact_lineage = {}
    for family, mod_id in [('core','salts_inventory_update'),('compatibility','salts_inventory_compat'),('controlify','salts_inventory_controlify')]:
        filename = f'{mod_id}-{game}-{runtime.LOADER_NAMES[loader]}-{mods[mod_id]}.jar'
        release = artifacts/family/filename
        assert release.is_file(), f'{name}: final {family} missing'
        artifact_lineage[family]=runtime.artifact_match(family,release,profile/'mods'/filename,
            allow_core_description_correction=allow_core_description_correction)
        releases[family] = sha(release)
        with ZipFile(release) as jar:
            assert jar.testzip() is None and not any('/qa/' in p or p.endswith('.pdn') for p in jar.namelist()), (name,family)
    for row in records:
        if row['status']=='CAPTURED':
            path = folder/'screenshots'/(row['name']+'.png')
            assert path.is_file() and path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n'), (name,row)
    scope = report.get('scope','all')
    if scope=='all':
        suffix = name[len(game+'-'+loader):]
        runtime.validate(pin,folder.parent,artifacts,profile.name,suffix,True,optional is not None)
    elif scope=='mod-compat':
        require(passed,API_CHECKS|BROWSER_CHECKS,name)
        if optional and optional['browser']=='rei': require(passed,REI_RENDER_CHECKS,name)
        for block in ['toms_storage:storage_terminal','toms_storage:crafting_terminal','toms_storage:inventory_link'] + ([] if loader=='fabric' else [
                'ae2:sky_stone_chest','ae2:cell_workbench','sophisticatedstorage:chest','sophisticatedstorage:barrel','sophisticatedstorage:limited_barrel_1']):
            require(passed,{f'compat-{block}-native-api-adapter',f'compat-{block}-Dpad-native-target'},name)
        if loader=='neoforge':
            require(passed,{'compat-ae2-native-ghost-slot-navigable','compat-ae2-ghost-copy-does-not-consume-cursor',
                'compat-ae2-real-stack-conserved','compat-backpack-held-source-cannot-be-picked-up','compat-backpack-native-controls-navigable'},name)
            require(passed,{'compat-ae2-network-virtual-slots-no-real-authority','compat-ae2-network-virtual-item-navigable',
                'compat-ae2-network-dpad-scrolls-one-row','compat-ae2-network-dpad-does-not-skip-adjacent-entries','compat-ae2-network-right-stick-native-scroll',
                'compat-ae2-network-search-native-keyboard','compat-ae2-network-keyboard-restores-controls','compat-ae2-network-keyboard-updates-native-filter',
                'compat-ae2-network-item-click-does-not-open-keyboard','compat-ae2-network-withdraws-real-stack',
                'compat-ae2-network-withdraw-conserves-items','compat-ae2-network-deposit-conserves-items'},name)
            require(passed,{f'compat-{owner}-editor-native-keyboard' for owner in ['backpack','sophisticatedstorage:chest','sophisticatedstorage:barrel']},name)
        for block in ['toms_storage:storage_terminal','toms_storage:crafting_terminal','toms_storage:inventory_link']:
            require(passed,{f'compat-{block}-editor-native-keyboard',f'compat-{block}-editor-controller-restored'},name)
    elif scope=='browser-render':
        require(passed,BROWSER_CHECKS,name)
        if optional and optional['browser']=='rei': require(passed,REI_RENDER_CHECKS,name)
    elif scope in FOCUSED_CHECKS:
        require(passed,FOCUSED_CHECKS[scope],name)
    elif scope!='creative-regression':
        raise AssertionError('Unknown controller QA scope: '+scope)
    result = {'report':str(folder.relative_to(ROOT)),'loader':loader,'scope':scope,'passes':sum(r['status']=='PASS' for r in records),
        'unique_checks':len(passed),'screenshots':sum(r['status']=='CAPTURED' for r in records),'failures':0,
        'artifact_sha256':releases,'artifact_lineage':artifact_lineage,'runtime_profile':str(profile.relative_to(ROOT)),'browser':optional['browser'] if optional else None}
    print(f'{name}: {result["unique_checks"]} distinct passing checks; production coverage and pinned upstream hashes verified')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report',action='append',required=True,type=Path)
    parser.add_argument('--artifacts',type=Path,default=ROOT/'dist')
    parser.add_argument('--summary',type=Path,required=True)
    parser.add_argument('--allow-core-description-correction',action='store_true')
    parser.add_argument('--require-matrix',action='store_true')
    args=parser.parse_args()
    rows=[validate(path.resolve(),args.artifacts.resolve(),args.allow_core_description_correction) for path in args.report]
    if args.require_matrix:
        assert {(r['loader'],r['browser']) for r in rows if r['scope']=='mod-compat'}=={
            (loader,browser) for loader in ['fabric','neoforge'] for browser in ['jei','rei']},'Incomplete optional mod matrix'
        assert {r['loader'] for r in rows if r['scope']=='all'}=={'fabric','neoforge'},'Both full loader runs required'
        assert {(r['loader'],r['scope']) for r in rows if r['scope'] in FOCUSED_CHECKS}=={
            (loader,scope) for loader in ['fabric','neoforge'] for scope in FOCUSED_CHECKS},'Both loaders need each recent feature scope'
    args.summary.parent.mkdir(parents=True,exist_ok=True)
    args.summary.write_text(json.dumps({'status':'passed','targets':rows},indent=2)+'\n',encoding='utf8')
