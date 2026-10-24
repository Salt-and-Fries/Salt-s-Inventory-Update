"""Focused checks for shared texture ownership and the processed resources/JARs that consume it."""
import argparse
import hashlib
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SHARED = ROOT / 'artwork/shared-resources'
VERSIONS = ('1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3')
MODULES = ('core', 'compatibility', 'controlify')


def texture(name):
    return '/textures/' in name and name.endswith(('.png', '.mcmeta'))


def digest(data):
    return hashlib.sha256(data).hexdigest()


def tree(folder):
    return {p.relative_to(folder).as_posix(): digest(p.read_bytes())
            for p in folder.rglob('*') if p.is_file() and texture(p.relative_to(folder).as_posix())}


def sources():
    for module in MODULES:
        for name in tree(SHARED / module):
            assert name.startswith('assets/'), (module, 'Invalid resource path', name)
            if module != 'compatibility':
                namespace = 'salts_inventory_update' if module == 'core' else 'salts_inventory_controlify'
                assert name.startswith(f'assets/{namespace}/'), (module, 'Foreign mod texture', name)
    for root in (ROOT/'versions', ROOT/'controlify/versions', ROOT/'compatibility/versions'):
        for folder in root.glob('*/common/src/main/resources'):
            assert not tree(folder), f'Edit the shared texture source instead of version copies: {folder}'


def check(resources=False, jars=False):
    sources()
    count = 0
    for version in VERSIONS:
        for loader in ('fabric', 'forge' if version == '1.20.1' else 'neoforge'):
            for module in MODULES:
                expected = tree(SHARED/module)
                base = ROOT / ('versions' if module == 'core' else f'{module}/versions') / version / loader
                if resources:
                    folder = base/'build/resources/main'
                    assert folder.is_dir(), f'Run processResources first: {base}'
                    actual = tree(folder)
                    assert actual == expected, (module, version, loader,
                        'Missing', sorted(expected.keys()-actual.keys()), 'Extra', sorted(actual.keys()-expected.keys()),
                        'Changed', sorted(n for n in actual.keys() & expected.keys() if actual[n] != expected[n]))
                    assert not any(folder.rglob('*.pdn')), (module, version, loader, 'Packaged editor source')
                if jars:
                    folder = ROOT/'dist'/module
                    matches = list(folder.glob(f'*-{version}-{loader.capitalize() if loader != "neoforge" else "NeoForge"}-*.jar'))
                    assert len(matches) == 1, (module, version, loader, matches)
                    with ZipFile(matches[0]) as jar:
                        actual = {n: digest(jar.read(n)) for n in jar.namelist() if texture(n)}
                    assert actual == expected, (module, version, loader, 'Production texture mismatch')
                count += 1
    print(f'Shared textures verified: {sum(len(tree(SHARED/m)) for m in MODULES)} textures; {count} module targets; resources={resources}; jars={jars}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--resources', action='store_true')
    parser.add_argument('--jars', action='store_true')
    args = parser.parse_args()
    check(args.resources, args.jars)
