"""Check all twelve distributed Core sort implementations and their bundle entries."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSIONS = ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']
PACKAGE = 'com/salts_inventory_update/'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def verify():
    rows = []
    checksums = dict(line.split('  ', 1)[::-1] for line in
                     (ROOT/'dist/core/SHA256SUMS.txt').read_text().splitlines() if line)
    jars = list((ROOT/'dist/core').glob('*.jar'))
    assert len(jars) == 12, 'Expected twelve production Core JARs'
    with zipfile.ZipFile(ROOT/'dist/salts-inventory-all-latest.zip') as bundle:
        assert sum(n.endswith('.jar') for n in bundle.namelist()) == 36
        assert bundle.read('checksums/core/SHA256SUMS.txt') == (ROOT/'dist/core/SHA256SUMS.txt').read_bytes()
        for version in VERSIONS:
            for loader in ['fabric', 'forge' if version == '1.20.1' else 'neoforge']:
                display = {'fabric': 'Fabric', 'forge': 'Forge', 'neoforge': 'NeoForge'}[loader]
                jar = ROOT/f'dist/core/salts_inventory_update-{version}-{display}-0.1.5.jar'
                data = jar.read_bytes()
                assert checksums[jar.name] == digest(data), jar.name
                assert (ROOT/'build/release/0.1.5'/jar.name).read_bytes() == data, jar.name
                assert bundle.read(f'{version}/{loader}/{jar.name}') == data, jar.name
                with zipfile.ZipFile(jar) as archive:
                    names = archive.namelist()
                    assert not any('/functionaltest/' in n or '/qa/' in n or n.endswith('Test.class') for n in names), jar.name
                    assert PACKAGE+'internal/desktop/SmartInventoryOrder.class' in names, jar.name
                    assert PACKAGE+'internal/desktop/InventorySortPlan.class' in names, jar.name
                    payload = archive.read(PACKAGE+'network/DesktopPackets$DesktopSortInventoryPayload.class')
                    assert b'desktop_sort_inventory' in payload, jar.name
                    assert b'canSortInventory' in archive.read(PACKAGE+'client/DesktopContainerClient.class'), jar.name
                    assert b'executeInventorySort' in archive.read(PACKAGE+'server/DesktopContainerSessions.class'), jar.name
                    assert b'SORT_INVENTORY' in archive.read(PACKAGE+'client/InventoryDesktopScreen$WindowControl.class'), jar.name
                    language = json.loads(archive.read('assets/salts_inventory_update/lang/en_us.json'))
                    assert language['tooltip.salts_inventory_update.window_control.sort_inventory'] == 'Sort this inventory'
                    if loader == 'fabric':
                        metadata = json.loads(archive.read('fabric.mod.json'))
                        assert metadata['id'] == 'salts_inventory_update' and metadata['version'] == '0.1.5'
                        assert 'fabric-api' in metadata['depends'], jar.name
                    else:
                        assert 'fabric.mod.json' not in names, jar.name
                        metadata = archive.read('META-INF/mods.toml' if loader == 'forge' else 'META-INF/neoforge.mods.toml').decode()
                        assert 'salts_inventory_update' in metadata and '0.1.5' in metadata, jar.name
                rows.append({'minecraft': version, 'loader': loader, 'jar': str(jar), 'sha256': digest(data)})
        # A sorting-only Core release must retain the exact existing companion binaries.
        baseline = ROOT/'build/sort-all-targets/companion-sha256-before.json'
        if baseline.exists():
            for relative, original in json.loads(baseline.read_text()).items():
                assert digest((ROOT/relative).read_bytes()) == original, relative
    return rows


if __name__ == '__main__':
    report = verify()
    destination = ROOT/'build/sort-all-targets/artifacts.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2)+'\n')
    print(f'PASS: {len(report)} Core JARs contain sorting, correct metadata, no QA classes, matching checksums and bundle copies.')
