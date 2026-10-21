"""Enforce the consumer boundary in source and production class constant pools."""
from pathlib import Path
import argparse
import json
import re
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PREFIX = 'com/salts_inventory_update/'
ALLOWED = (PREFIX + 'api/', PREFIX + 'compat/')
NATIVE_INTEGRATION_PREFIXES = (
    'dev/yurisuika/raised/',
    'appeng/', 'com/tom/storagemod/', 'net/p3pp3rf1y/sophisticated',
    'mezz/jei/', 'me/shedaniel/rei/', 'dev/emi/emi/', 'com/terraformersmc/modmenu/',
)
VERSIONS = ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']

def utf8_constants(data):
    assert data[:4] == b'\xca\xfe\xba\xbe', 'Invalid class file'
    count = struct.unpack_from('>H', data, 8)[0]
    offset, index = 10, 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            size = struct.unpack_from('>H', data, offset)[0]
            offset += 2
            yield data[offset:offset + size].decode('utf-8', errors='replace')
            offset += size
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): offset += 4
        elif tag in (5, 6): offset += 8; index += 1
        elif tag in (7, 8, 16, 19, 20): offset += 2
        elif tag == 15: offset += 3
        else: raise AssertionError(f'Unknown constant-pool tag {tag}')
        index += 1

def forbidden_references(text):
    # Includes descriptor classes, imports, fully qualified calls and reflective strings.
    names = re.findall(r'com[./]salts_inventory_update[./][\w.$/]+', text)
    return sorted({name for name in names if not name.replace('.', '/').startswith(ALLOWED)})

def verify_native_sync_schema(patch, location):
    if patch.get('class') == 'appeng/menu/me/crafting/CraftingCPUMenu':
        for field in patch['fields']:
            if field['name'] in ['cpuStatusReady', 'cpuAvailable', 'hasActiveJob', 'cpuStatusSerial']:
                annotations = field.get('visibleAnnotations', []) + field.get('invisibleAnnotations', [])
                assert not any(a['desc'] == 'Lappeng/menu/guisync/GuiSync;' for a in annotations), \
                    f'{location}: desktop CPU state changes the vanilla AE2 synchronization schema'

def verify_native_activation(manifest, location):
    if 'clientPortComplete' in manifest:
        assert manifest['clientPortComplete'] and manifest.get('pointerPortComplete') \
            and manifest.get('terminalKeyboardPortComplete'), f'{location}: staged AE2 adapter in release'
        assert manifest.get('runtimeValidationPending') is False, f'{location}: AE2 runtime validation pending'
        assert manifest.get('uiBaseline') == 'installed-native-version', f'{location}: wrong native UI baseline'

def sources():
    forge_pack = ROOT / 'compatibility/versions/1.20.1/forge/src/main/resources/pack.mcmeta'
    assert json.loads(forge_pack.read_text())['pack']['pack_format'] == 15, \
        'Forge 1.20.1 requires pack metadata to discover extension resources'
    legacy_chest = ROOT / 'compatibility/ae2/versions/1.20.1/common/src/main/resources/assets/ae2/screens/desktop/chest.json'
    assert json.loads(legacy_chest.read_text())['includes'] == ['../chest.json'], \
        'AE2 15 chest factory must resolve its original native style'
    for manifest in (ROOT / 'compatibility' / 'ae2' / 'versions').glob('**/src/main/resources/ae2-native-patches*/manifest.json'):
        verify_native_activation(json.loads(manifest.read_text()), manifest.relative_to(ROOT))
    failures = []
    count = 0
    for p in (ROOT / 'compatibility').rglob('*.java'):
        if 'build' in p.parts: continue
        count += 1
        for name in forbidden_references(p.read_text(encoding='utf-8')):
            if name == 'com.salts_inventory_update.compat': continue  # package declaration
            failures.append(f'{p.relative_to(ROOT)}: forbidden reference {name}')
    for p in (ROOT / 'versions').rglob('*.java'):
        if 'build' in p.parts or 'tests' in p.parts: continue
        text = p.read_text(encoding='utf-8')
        if re.search(r'com\.salts_inventory_update\.compat\.|\bappeng\.|\bdev\.yurisuika\.raised\.|["\'](?:raised|ae2|toms_storage|sophisticatedcore|sophisticatedstorage|sophisticatedbackpacks|roughlyenoughitems|jei|emi)["\']', text):
            failures.append(f'{p.relative_to(ROOT)}: core references a moved integration')
    patch_count = 0
    patch_files = [p for folder in ['ae2-native-patches', 'ae2-native-patches-runtime']
                   for root in (ROOT / 'compatibility' / 'ae2').rglob(folder)
                   for p in (root / 'appeng').rglob('*.json')]
    for p in patch_files:
        patch_count += 1
        patch = json.loads(p.read_text(encoding='utf-8'))
        verify_native_sync_schema(patch, p.relative_to(ROOT))
        if not patch.get('class', '').startswith('appeng/'):
            failures.append(f'{p.relative_to(ROOT)}: patch targets a non-AE2 class')
        for name in forbidden_references(p.read_text(encoding='utf-8')):
            failures.append(f'{p.relative_to(ROOT)}: forbidden encoded core reference {name}')
    assert count > 0, 'No compatibility sources inspected'
    assert not failures, '\n'.join(failures)
    print(f'Source boundary verified: {count} compatibility classes, {patch_count} native patches, all core sources')

def classpaths(manifest):
    files = {Path(line) for line in Path(manifest).read_text(encoding='utf-8').splitlines() if line}
    for path in files:
        if path.is_dir():
            names = [p.relative_to(path).as_posix() for p in (path / 'com/salts_inventory_update').rglob('*.class')]
        elif path.is_file() and path.suffix == '.jar':
            with zipfile.ZipFile(path) as z: names = z.namelist()
        else:
            continue
        forbidden = [name for name in names if name.endswith('.class') and name.startswith(PREFIX) and not name.startswith(ALLOWED)]
        assert not forbidden, f'{path}: compatibility compiler can access core implementation {forbidden[:10]}'
    print(f'Compiler boundary verified: {len(files)} classpath entries, no core implementation available')

def jars():
    for kind in ['core', 'compatibility']:
        artifacts = sorted((ROOT / 'dist' / kind).glob('*.jar'))
        assert len(artifacts) == 12, f'Expected 12 {kind} production JARs, found {len(artifacts)}'
        for p in artifacts:
            assert not any(x in p.name for x in ['-dev', '-sources', '-api']), p
            with zipfile.ZipFile(p) as z:
                names = set(z.namelist())
                classes = [name for name in names if name.endswith('.class')]
                assert classes, f'{p}: empty artifact'
                assert not any(name.startswith(NATIVE_INTEGRATION_PREFIXES) for name in classes), \
                    f'{p}: bundled third-party integration implementation'
                assert not any(name.startswith('example/') or '/functionaltest/' in name for name in names), p
                assert not any('/compat/ae2/qa/' in name or 'NativeAtlasHooks' in name or 'ae2-atlas.mixins' in name for name in names), f'{p}: packaged AE2 atlas instrumentation'
                if kind == 'core':
                    assert not any(name.startswith('assets/ae2/') for name in names), f'{p}: bundled AE2 desktop resources in core'
                    assert not any(name.startswith(PREFIX + 'compat/') or 'sophisticated.mixins' in name for name in names), p
                    assert PREFIX + 'api/server/desktop/DesktopSessions.class' in names, p
                    assert PREFIX + 'api/client/recipebrowser/RecipeBrowserBridge.class' in names, p
                    for name in classes:
                        for value in utf8_constants(z.read(name)):
                            assert not re.search(r'com[./]salts_inventory_update[./]compat[./]|appeng[./]|dev[./]yurisuika[./]raised[./]|com[./]tom[./]storagemod[./]|net[./]p3pp3rf1y[./]sophisticated|mezz[./]jei[./]|me[./]shedaniel[./]rei[./]|dev[./]emi[./]|com[./]terraformersmc[./]modmenu[./]', value), f'{p.name}!{name}: core depends on a moved integration'
                else:
                    if '1.20.1-Forge-' in p.name:
                        assert json.loads(z.read('pack.mcmeta'))['pack']['pack_format'] == 15, \
                            f'{p}: Forge cannot discover the extension resource pack'
                    if '1.20.1-' in p.name:
                        assert 'assets/ae2/screens/desktop/chest.json' in names, f'{p}: missing native AE2 15 chest alias'
                    reference = json.loads(z.read('META-INF/ae2-desktop-reference.json'))
                    assert len(reference['styles']) == 42, f'{p}: incomplete AE2 desktop resource inventory'
                    import hashlib
                    for resource, expected in reference['styles'].items():
                        assert hashlib.sha256(z.read(resource)).hexdigest() == expected, f'{p}!{resource}: fork geometry changed'
                    assert hashlib.sha256(z.read('assets/ae2/lang/en_us.json')).hexdigest() == reference['translationSha256'], f'{p}: altered AE2 desktop translations'
                    assert 'salts_inventory_compat.ae2.mixins.json' in names, f'{p}: missing optional AE2 hooks'
                    for resource in names:
                        if resource in ['ae2-native-patches/manifest.json', 'ae2-native-patches-runtime/manifest.json']:
                            verify_native_activation(json.loads(z.read(resource)), f'{p}!{resource}')
                        if resource.startswith(('ae2-native-patches/appeng/', 'ae2-native-patches-runtime/appeng/')) and resource.endswith('.json'):
                            patch_text = z.read(resource).decode('utf-8')
                            patch = json.loads(patch_text)
                            assert patch['class'].startswith('appeng/'), f'{p}!{resource}: non-AE2 patch target'
                            verify_native_sync_schema(patch, f'{p}!{resource}')
                            assert not forbidden_references(patch_text), f'{p}!{resource}: encoded core implementation access'
                    assert not any(name.startswith(PREFIX) and not name.startswith(PREFIX+'compat/') for name in classes), f'{p}: bundled core code'
                    for name in classes:
                        for value in utf8_constants(z.read(name)):
                            bad = forbidden_references(value)
                            assert not bad, f'{p.name}!{name}: forbidden core bytecode references {bad}'
                    assert not any(name.endswith('.jar') for name in names), f'{p}: nested JAR'
                if 'fabric.mod.json' in names:
                    metadata = json.loads(z.read('fabric.mod.json'))
                    assert metadata['id'] == ('salts_inventory_update' if kind == 'core' else 'salts_inventory_compat'), p
                    assert 'fabric-api' in metadata['depends'], p
                    assert ('salts_inventory_update' in metadata['depends']) == (kind == 'compatibility'), p
                    assert 'salts_inventory_compat' not in metadata['depends'], p
                    for entries in metadata['entrypoints'].values():
                        for entry in entries:
                            assert entry.replace('.', '/') + '.class' in names, f'{p}: missing entrypoint {entry}'
                    if kind == 'core': assert set(metadata['entrypoints']) == {'main', 'client'}, p
                else:
                    metadata = z.read('META-INF/mods.toml' if 'META-INF/mods.toml' in names else 'META-INF/neoforge.mods.toml').decode()
                    assert f'modId = "{ "salts_inventory_update" if kind == "core" else "salts_inventory_compat" }"' in metadata, p
                    if kind == 'compatibility':
                        assert 'modId = "salts_inventory_update"' in metadata and 'ordering = "AFTER"' in metadata, p
                    else: assert 'sophisticated' not in metadata and 'roughlyenoughitems' not in metadata, p
    print('Production boundary verified: 12 core + 12 compatibility JARs')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--sources', action='store_true')
    parser.add_argument('--jars', action='store_true')
    parser.add_argument('--classpath-manifest')
    args = parser.parse_args()
    if args.sources: sources()
    if args.jars: jars()
    if args.classpath_manifest: classpaths(args.classpath_manifest)
