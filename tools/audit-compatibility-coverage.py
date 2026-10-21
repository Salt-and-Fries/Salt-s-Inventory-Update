"""Check every published integration/target cell against source and production artifacts.

coverage-targets.json is a reviewed upstream release snapshot, not a claim that
unexecuted gameplay scenarios passed. Network access is never required by builds.
"""
from pathlib import Path
import argparse
import json
import zipfile
import importlib.util

ROOT = Path(__file__).resolve().parents[1]
_spec = importlib.util.spec_from_file_location('module_boundaries', ROOT/'tools/verify-module-boundaries.py')
_boundaries = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_boundaries)
PREFIX = 'com/salts_inventory_update/compat/'
CLASSES = {
    'raised': ['raised/RaisedHudIntegration'],
    'ae2': ['ae2/AE2Bootstrap'],
    'toms-storage': ['toms_storage/client/TomsStorageClientCompat', 'toms_storage/server/TomsStorageServerCompat'],
    'jei': ['jei/RuntimeRecipeBrowserAccess', 'jei/SaltsJeiPlugin'],
    'rei': ['rei/RuntimeReiRecipeBrowserAccess'],
    'emi': ['emi/RuntimeEmiRecipeBrowserAccess', 'emi/EmiRecipeBrowserBootstrap'],
    'modmenu': ['modmenu/SaltsInventoryModMenu'],
    'sophisticated-core': ['sophisticated/SophisticatedCoreCompat'],
    'sophisticated-backpacks': ['sophisticated/SophisticatedBackpacksCompat'],
    'sophisticated-storage': ['sophisticated/SophisticatedStorageCompat'],
}
# Java permits a package-private class to retain a historical source filename.
SOURCE_FILES = {'jei/RuntimeRecipeBrowserAccess': 'jei/RuntimeJeiDesktopAccess'}
FABRIC_ENTRYPOINTS = {
    'jei': ('jei_mod_plugin', 'jei.SaltsJeiPlugin'),
    'rei': ('rei_client', 'rei.SaltsReiClientPlugin'),
    'emi': ('emi', 'emi.SaltsEmiClientPlugin'),
    'modmenu': ('modmenu', 'modmenu.SaltsInventoryModMenu'),
}

def verify_fabric_slot_selector(data, location):
    constants=set(_boundaries.utf8_constants(data))
    selectors={'method_2385','method_2385(Lnet/minecraft/class_332;Lnet/minecraft/class_1735;)V'}
    assert selectors.intersection(constants), f'{location}: missing production Sophisticated slot selector'

def source_paths(mc, loader):
    paths=[ROOT/f'compatibility/versions/{mc}/{loader}/src/main/java',
           ROOT/f'compatibility/versions/{mc}/fabric/src/main/java',
           ROOT/'compatibility/ae2/src/main/java', ROOT/'compatibility/raised/src/main/java',
           ROOT/f'compatibility/ae2/versions/{mc}/common/src/main/java',
           ROOT/f'compatibility/ae2/versions/{mc}/{loader}/src/main/java']
    if loader=='fabric':
        paths += [ROOT/'compatibility/fabric-modmenu/src/main/java',
                  ROOT/'compatibility/sophisticated-fabric/src/main/java',
                  ROOT/f'compatibility/sophisticated-fabric/versions/{mc}/src/main/java']
    return paths


def audit(jars=False):
    manifest=json.loads((ROOT/'compatibility/coverage-targets.json').read_text())
    expected={(mc,loader) for mc in ('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3') for loader in ('fabric','forge' if mc=='1.20.1' else 'neoforge')}
    seen=set(); failures=[]; available=unavailable=0
    for target in manifest['targets']:
        mc,loader=target['minecraft'],target['loader']
        key=(mc,loader)
        if key in seen: failures.append(f'duplicate target {key}')
        seen.add(key)
        paths=source_paths(mc,loader)
        artifact_names=None
        artifact_metadata=None
        if jars:
            display={'fabric':'Fabric','forge':'Forge','neoforge':'NeoForge'}[loader]
            p=ROOT/f"dist/compatibility/salts_inventory_compat-{mc}-{display}-{manifest['compatibility_version']}.jar"
            if not p.is_file(): failures.append(f'missing production artifact: {p}')
            else:
                with zipfile.ZipFile(p) as z:
                    artifact_names=set(z.namelist())
                    if loader=='fabric': artifact_metadata=json.loads(z.read('fabric.mod.json'))
                    if loader=='fabric' and target['integrations']['sophisticated-core']['available']:
                        entry=PREFIX+'sophisticated/mixin/StorageScreenBaseMixin.class'
                        if entry not in artifact_names: failures.append(f'{key}: missing Sophisticated slot mixin')
                        else:
                            try: verify_fabric_slot_selector(z.read(entry),p.name)
                            except AssertionError as failure: failures.append(str(failure))
        if set(target['integrations']) != set(CLASSES): failures.append(f'{key}: incomplete integration inventory')
        for mod,row in target['integrations'].items():
            if not row['available']:
                unavailable+=1
                if not row.get('reason'): failures.append(f'{key}/{mod}: missing unavailable reason')
                continue
            available+=1
            if not row.get('version_id') or len(row.get('sha512',''))!=128: failures.append(f'{key}/{mod}: missing upstream pin/hash')
            for name in CLASSES[mod]:
                relative=Path(PREFIX+SOURCE_FILES.get(name,name)+'.java')
                if not any((base/relative).is_file() for base in paths): failures.append(f'{key}/{mod}: missing adapter source {name}')
                if artifact_names is not None and PREFIX+name+'.class' not in artifact_names: failures.append(f'{key}/{mod}: missing production class {name}')
            if loader=='fabric' and mod in FABRIC_ENTRYPOINTS:
                metadata=json.loads((ROOT/f'compatibility/versions/{mc}/fabric/src/main/resources/fabric.mod.json').read_text())
                entrypoint,implementation=FABRIC_ENTRYPOINTS[mod]
                implementation='com.salts_inventory_update.compat.'+implementation
                if implementation not in metadata.get('entrypoints',{}).get(entrypoint,[]): failures.append(f'{key}/{mod}: Fabric provider entrypoint not registered')
                if artifact_metadata is not None and implementation not in artifact_metadata.get('entrypoints',{}).get(entrypoint,[]): failures.append(f'{key}/{mod}: production Fabric provider entrypoint not registered')
            if mod=='ae2':
                native=ROOT/f"compatibility/ae2/versions/{mc}/{loader}/src/main/resources/ae2-native-patches/manifest.json"
                if not native.is_file(): failures.append(f'{key}/ae2: missing native adapter contract')
            if mod.startswith('sophisticated-') and loader=='fabric':
                config=ROOT/f'compatibility/sophisticated-fabric/versions/{mc}/src/main/resources/salts_inventory_compat.sophisticated-fabric.mixins.json'
                meta=json.loads((ROOT/f'compatibility/versions/{mc}/fabric/src/main/resources/fabric.mod.json').read_text())
                if not config.is_file() or config.name not in meta['mixins']: failures.append(f'{key}/{mod}: Fabric hooks not registered')
                if row.get('project') not in ('sophisticated-core-unofficial-fabric-port','sophisticated-backpacks-unoffical-fabric-port','sophisticated-storage-unofficial-fabric-port'): failures.append(f'{key}/{mod}: wrong upstream loader variant')
        print(f"{mc:8} {loader:8} " + ', '.join(name for name,row in target['integrations'].items() if row['available']))
    if seen!=expected: failures.append(f'target matrix differs: {seen ^ expected}')
    if failures: raise AssertionError('\n'.join(failures))
    print(f'Coverage verified: {len(seen)} targets, {available} available integration cells, {unavailable} explicitly unavailable cells'+(' (production JARs)' if jars else ' (sources)'))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jars',action='store_true')
    args=parser.parse_args()
    audit(args.jars)
