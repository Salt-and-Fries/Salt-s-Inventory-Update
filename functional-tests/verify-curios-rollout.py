"""Inspect every Curios production adapter and the shared Core API boundary."""
from pathlib import Path
import importlib.util
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('boundaries', ROOT / 'tools/verify-module-boundaries.py')
boundaries = importlib.util.module_from_spec(spec)
spec.loader.exec_module(boundaries)
PREFIX = 'com/salts_inventory_update/'
VERSIONS = ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']

for mc in VERSIONS:
    loader = 'Forge' if mc == '1.20.1' else 'NeoForge'
    metadata_path = 'META-INF/mods.toml' if loader == 'Forge' else 'META-INF/neoforge.mods.toml'
    for kind, mod, version in [('core', 'salts_inventory_update', '0.1.5'),
                               ('compatibility', 'salts_inventory_compat', '0.1.2')]:
        artifact = ROOT / 'dist' / kind / f'{mod}-{mc}-{loader}-{version}.jar'
        with zipfile.ZipFile(artifact) as jar:
            names = set(jar.namelist())
            classes = [name for name in names if name.endswith('.class')]
            assert classes, f'Empty artifact: {artifact}'
            assert not any(name.startswith(boundaries.NATIVE_INTEGRATION_PREFIXES) for name in classes), artifact
            assert not any(name.startswith(PREFIX + 'qa/') for name in names), artifact
            metadata = tomllib.loads(jar.read(metadata_path).decode())
            assert metadata['mods'][0]['modId'] == mod
            assert metadata['mods'][0]['version'] == version
            deps = {dep['modId']: dep for dep in metadata['dependencies'][mod]}
            assert deps['minecraft']['versionRange'] == f'[{mc}]'
            if kind == 'core':
                assert 'curios' not in deps and 'salts_inventory_compat' not in deps
                for api in ['DesktopBuiltinAttachment', 'DesktopWindowAttachment']:
                    assert PREFIX + f'api/client/desktop/{api}.class' in names
                for name in classes:
                    for value in boundaries.utf8_constants(jar.read(name)):
                        assert 'top/theillusivec4/curios/' not in value
                        assert 'top.theillusivec4.curios.' not in value
                        assert PREFIX + 'compat/' not in value
            else:
                required = lambda dep: dep.get('mandatory', dep.get('type') == 'required')
                assert required(deps['salts_inventory_update'])
                assert deps['salts_inventory_update']['versionRange'] == '[0.1.5]'
                assert not required(deps['curios']) and deps['curios']['side'] == 'BOTH'
                for cls in ['CuriosCompatBootstrap', 'CuriosIntegration', 'CuriosClientIntegration',
                            'CuriosDesktopMenu', 'CuriosPanelDefinition', 'CuriosPanelLayout', 'CuriosCharacterButton']:
                    assert PREFIX + f'compat/curios/{cls}.class' in names, artifact
                assert not any(name.startswith('assets/curios/') for name in names), artifact
                assert not any(name.endswith('.jar') for name in names), artifact
                for name in classes:
                    assert not name.startswith(PREFIX) or name.startswith(PREFIX + 'compat/'), artifact
                    for value in boundaries.utf8_constants(jar.read(name)):
                        assert not boundaries.forbidden_references(value), (artifact, name, value)
            print(f'Verified {artifact.relative_to(ROOT)}')

    # API parity is shared with Fabric, but Curios itself must remain absent there.
    with zipfile.ZipFile(ROOT / 'dist/core' / f'salts_inventory_update-{mc}-Fabric-0.1.5.jar') as jar:
        assert PREFIX + 'api/client/desktop/DesktopWindowAttachment.class' in jar.namelist()
    with zipfile.ZipFile(ROOT / 'dist/compatibility' / f'salts_inventory_compat-{mc}-Fabric-0.1.2.jar') as jar:
        assert not any('/compat/curios/' in name for name in jar.namelist())

print('All six native pairs: metadata, optional Curios, public API, class ownership and Fabric exclusion verified')
