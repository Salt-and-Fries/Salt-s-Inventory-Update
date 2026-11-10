"""Inspect all packaged descriptors and their referenced classes/resources.

Read-only archive audit; native linkage and optional absence are separate live
gates. NeoForge 1.21.11 deliberately synthesizes per-type pack metadata.
"""
from pathlib import Path
import argparse, collections, datetime, hashlib, json, re, struct, tomllib, zipfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = {'1.20.1': 17, '1.21.1': 21, '1.21.11': 21,
        '26.1.2': 25, '26.2': 25, '26.3': 25}
PACK = dict(zip(JAVA, [15, 34, 75, 84, 88, 97]))
MODULES = {'core': ('salts_inventory_update', '0.1.6'),
           'compatibility': ('salts_inventory_compat', '0.1.3'),
           'controlify': ('salts_inventory_controlify', '0.1.1')}


def inspect(path, row):
    minecraft, loader, family = row['minecraft'], row['loader'], row['family']
    module, version = MODULES[family]
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        assert archive.testzip() is None
        metadata_name = ('fabric.mod.json' if loader == 'fabric' else
                         'META-INF/mods.toml' if loader == 'forge' else
                         'META-INF/neoforge.mods.toml')
        raw = archive.read(metadata_name).decode('utf-8-sig')
        assert '${' not in raw
        metadata = json.loads(raw) if loader == 'fabric' else tomllib.loads(raw)
        entrypoints = []
        if loader == 'fabric':
            assert metadata['id'] == module and metadata['version'] == version
            dependencies = metadata['depends']
            assert dependencies['minecraft'] == minecraft
            assert dependencies['java'] == '>=' + str(JAVA[minecraft])
            assert dependencies['fabricloader'].startswith('>=')
            if family != 'core':
                assert dependencies['salts_inventory_update'] == '=0.1.6'
            if family == 'controlify':
                assert metadata['environment'] == 'client'
                assert 'controlify' in dependencies
            if family == 'compatibility':
                assert set(dependencies) == {'minecraft', 'java', 'fabricloader',
                                             'fabric-api', 'salts_inventory_update'}
            for kind, values in metadata.get('entrypoints', {}).items():
                for value in values:
                    name = value if isinstance(value, str) else value['value']
                    name = name.split('::')[0]
                    assert name.replace('.', '/') + '.class' in names, (path, kind, name)
                    entrypoints.append(dict(kind=kind, name=name))
            declared_mixins = [v if isinstance(v, str) else v['config']
                               for v in metadata.get('mixins', [])]
            icon = metadata.get('icon')
        else:
            own = [v for v in metadata['mods'] if v['modId'] == module]
            assert len(own) == 1 and own[0]['version'] == version
            dependencies = metadata['dependencies'][module]
            by_id = {v['modId']: v for v in dependencies}
            assert len(by_id) == len(dependencies)
            assert by_id['minecraft']['versionRange'] == '[' + minecraft + ']'
            assert loader in by_id and metadata['loaderVersion']
            if family != 'core':
                assert by_id['salts_inventory_update']['versionRange'] == '[0.1.6]'
            if family == 'controlify':
                assert 'controlify' in by_id
                assert all(v['side'] == 'CLIENT' for v in dependencies)
            if family == 'compatibility':
                required = {v['modId'] for v in dependencies
                            if v.get('mandatory', v.get('type') == 'required')}
                assert required == {loader, 'minecraft', 'salts_inventory_update'}, required
            declared_mixins = [v['config'] for v in metadata.get('mixins', [])]
            icon = own[0].get('iconFile', own[0].get('logoFile'))
        icon_provider = row['file']
        if icon is None:
            icon_provider = None  # The descriptor's logo/icon is optional.
        elif icon not in names and family != 'core':
            label = {'fabric': 'Fabric', 'forge': 'Forge', 'neoforge': 'NeoForge'}[loader]
            core = ROOT / 'dist/core' / f'salts_inventory_update-{minecraft}-{label}-0.1.6.jar'
            with zipfile.ZipFile(core) as required_core:
                assert icon and icon in required_core.namelist(), (path, 'Icon missing from companion and required Core')
            icon_provider = str(core.relative_to(ROOT))
        else:
            assert icon and icon in names, (path, 'Missing declared icon')

        manifest_mixins = []
        if 'META-INF/MANIFEST.MF' in names:
            manifest = archive.read('META-INF/MANIFEST.MF').decode().replace('\r\n ', '').replace('\n ', '')
            for line in manifest.splitlines():
                if line.startswith('MixinConfigs: '):
                    manifest_mixins.extend(v.strip() for v in line.partition(': ')[2].split(',') if v.strip())
        declared_mixins = sorted(set(declared_mixins + manifest_mixins))
        mixes = []
        for config in sorted(n for n in names if n.endswith('.mixins.json')):
            data = json.loads(archive.read(config))
            minimum = data.get('minVersion')
            assert isinstance(minimum, str) and re.fullmatch(r'[0-9]+(?:\.[0-9]+){1,3}', minimum), (path, config, 'Missing or malformed minVersion')
            references = []
            for kind in ['mixins', 'client', 'server']:
                for relative in data.get(kind, []):
                    name = data['package'] + '.' + relative
                    assert name.replace('.', '/') + '.class' in names, (path, config, name)
                    references.append(dict(kind=kind, name=name))
            if data.get('plugin'):
                assert data['plugin'].replace('.', '/') + '.class' in names
            refmap_present = data.get('refmap') in names
            if data.get('refmap') and refmap_present:
                json.loads(archive.read(data['refmap']))
            if data.get('refmap') and not refmap_present:
                # Legacy Forge's production namespaces require its generated
                # AP map. Loom remaps Fabric annotations inline; named NeoForge
                # paths have no runtime map. Retain their stale declarations as
                # explicit diagnostic notes, never claim the map was packaged.
                assert loader != 'forge', (path, config, data['refmap'])
            mixes.append(dict(config=config, minimum_version=minimum, plugin=data.get('plugin'),
                              refmap=data.get('refmap'), refmap_present=refmap_present,
                              reference_map_note=(
                                  'Declared map absent: non-Forge inline-remapped/named path. Loader may log a missing-map warning. Actual selectors require the separate production-symbol and live gates.'
                                  if data.get('refmap') and not refmap_present else None),
                              classes=references))
        assert set(declared_mixins).issubset(names)
        assert declared_mixins, (path, 'No loader-declared mixin configurations')
        class_versions = collections.Counter()
        for name in names:
            if name.endswith('.class'):
                raw_class = archive.read(name)
                assert raw_class[:4] == b'\xca\xfe\xba\xbe'
                major = struct.unpack('>H', raw_class[6:8])[0]
                assert major <= JAVA[minecraft] + 44, (path, name, major)
                class_versions[major] += 1
        assert class_versions[JAVA[minecraft] + 44] > 0
        synthesized = minecraft == '1.21.11' and loader == 'neoforge'
        if synthesized:
            assert 'pack.mcmeta' not in names
            pack = dict(mode='NeoForge per-type synthesized metadata',
                        source='build.gradle.kts:561')
        else:
            pack = json.loads(archive.read('pack.mcmeta'))['pack']
            assert pack['pack_format'] == PACK[minecraft]
            assert pack['description']
        return dict(**row, metadata_name=metadata_name, java_minimum=JAVA[minecraft],
                    class_versions=dict(class_versions), dependencies=dependencies,
                    entrypoints=entrypoints, icon=icon, icon_provider=icon_provider,
                    declared_mixins=declared_mixins,
                    mixin_references=mixes, pack_metadata=pack)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--id', required=True)
    args = parser.parse_args()
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.id)
    out = ROOT / 'output/release-readiness' / ('metadata-' + args.id + '.json')
    assert not out.exists(), 'Preserve earlier audits'
    integrity = json.loads((out.parent / 'candidate-integrity.json').read_text())
    assert integrity['status'] == 'passed' and len(integrity['artifacts']) == 36
    results = []
    for row in integrity['artifacts']:
        path = ROOT / row['file']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == row['sha256']
        results.append(inspect(path, row))
    notes = [dict(file=v['file'], config=m['config'], note=m['reference_map_note'])
             for v in results for m in v['mixin_references'] if m['reference_map_note']]
    out.write_text(json.dumps(dict(status='passed-with-explicit-refmap-notes', artifacts=results,
        reference_map_notes=notes,
        timestamp=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        limits='Packaged descriptor/resource/class-reference inspection. Native symbol mappings, conditional class loading and actual interactions require their separate normal-build and live acceptance gates.'), indent=2) + '\n')
    print('All 36 packaged descriptors, dependency boundaries, entrypoints, mixin classes/plugins, icons, class versions and pack metadata passed;', len(notes), 'absent non-Forge refmap declarations explicitly recorded for selector/live qualification.')


if __name__ == '__main__':
    main()
