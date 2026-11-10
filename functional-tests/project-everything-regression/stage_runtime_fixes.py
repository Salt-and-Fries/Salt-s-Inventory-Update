"""Stage only the regression fixes, preserving unrelated release and API bytes."""
from pathlib import Path
import hashlib, io, json, zipfile
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/project-everything-regression'
SHA = lambda b: hashlib.sha256(b).hexdigest()

def original_changes():
    changes = []
    for mc in ['1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']:
        for loader in ['Fabric', 'NeoForge']:
            changes.append(('core', mc, loader, 'com/salts_inventory_update/mixin/server/ServerPlayerMixin.class'))
    for mc in ['1.20.1', '26.1.2', '26.2', '26.3']:
        for loader in ['Fabric', 'Forge' if mc == '1.20.1' else 'NeoForge']:
            cls = ('com/salts_inventory_update/compat/ae2/extended/mixin/ExtendedAeNativePacketsMixin.class' if mc == '1.20.1'
                   else 'com/salts_inventory_update/compat/nativewindows/NativeBatchLayouts.class')
            changes.append(('compatibility', mc, loader, cls))
    return changes

def main(changes=None, baseline='release-before.json', report_name='staged-fixes.json', backup_name='release-backup', additions=()):
    before = json.loads((OUT / baseline).read_text())
    for name, expected in before.items(): assert SHA((ROOT / name).read_bytes()) == expected, name
    changes = original_changes() if changes is None else changes
    replacements, records = {}, []
    backup = ROOT / 'build/project-everything-regression' / backup_name; backup.mkdir(parents=True, exist_ok=True)
    grouped = {}
    for family, mc, loader, cls in changes:
        grouped.setdefault((family, mc, loader), []).append(cls)
    for (family, mc, loader), classes in grouped.items():
        version = '0.1.6' if family == 'core' else '0.1.3'
        module = 'salts_inventory_update' if family == 'core' else 'salts_inventory_compat'
        filename = f'{module}-{mc}-{loader}-{version}.jar'
        destination = ROOT / 'dist' / family / filename
        built = ROOT / (f'versions/{mc}/{loader.lower()}/build/libs' if family == 'core'
                        else f'compatibility/versions/{mc}/{loader.lower()}/build/libs') / filename
        original = destination.read_bytes(); stream = io.BytesIO()
        with zipfile.ZipFile(io.BytesIO(original)) as old, zipfile.ZipFile(built) as new:
            changed = {cls: new.read(cls) for cls in classes}
            added={cls for cls in changed if cls not in old.namelist()}
            assert added <= set(additions) and all(cls.endswith('.class') and '/api/' not in cls for cls in added), (filename,added)
            assert all(cls in added or data != old.read(cls) for cls, data in changed.items()), filename
            with zipfile.ZipFile(stream, 'w') as output:
                output.comment = old.comment
                for info in old.infolist(): output.writestr(info, changed.get(info.filename, old.read(info)))
                for cls in sorted(added):output.writestr(new.getinfo(cls),changed[cls])
        data = stream.getvalue()
        with zipfile.ZipFile(io.BytesIO(data)) as patched, zipfile.ZipFile(io.BytesIO(original)) as old:
            assert patched.testzip() is None
            assert set(patched.namelist()) == set(old.namelist()) | added
            assert {n for n in old.namelist() if patched.read(n) != old.read(n)} == set(classes)-added
            api = [n for n in old.namelist() if n.startswith('com/salts_inventory_update/api/')]
            assert all(patched.read(n) == old.read(n) for n in api)
            assert not any('/functionaltest/' in n or n.startswith('com/siu_reference/') for n in patched.namelist())
        (backup / filename).write_bytes(original)
        replacements[destination.relative_to(ROOT).as_posix()] = data
        records.append({'family': family, 'minecraft': mc, 'loader': loader.lower(), 'file': destination.relative_to(ROOT).as_posix(),
                        'changed_entries': [cls for cls in classes if cls not in added], 'added_entries':sorted(added), 'before_sha256': SHA(original), 'sha256': SHA(data), 'public_api_entries_preserved': len(api)})
    bundle_replacements = {}
    for family in ['core', 'compatibility']:
        hashes = {p.name: SHA(replacements.get(p.relative_to(ROOT).as_posix(), p.read_bytes())) for p in (ROOT / 'dist' / family).glob('*.jar')}
        sums = ''.join(f'{hashes[n]}  {n}\n' for n in sorted(hashes)).encode()
        replacements[f'dist/{family}/SHA256SUMS.txt'] = sums
        bundle_replacements[f'checksums/{family}/SHA256SUMS.txt'] = sums
    for r in records: bundle_replacements[f"{r['minecraft']}/{r['loader']}/{Path(r['file']).name}"] = replacements[r['file']]
    bundle = ROOT / 'dist/salts-inventory-all-latest.zip'; temp = bundle.with_suffix('.regression.tmp.zip')
    with zipfile.ZipFile(bundle) as old:
        assert set(bundle_replacements).issubset(old.namelist())
        preserved = {i.filename: SHA(old.read(i)) for i in old.infolist() if i.filename not in bundle_replacements}
        with zipfile.ZipFile(temp, 'w') as new:
            new.comment = old.comment
            for info in old.infolist(): new.writestr(info, bundle_replacements.get(info.filename, old.read(info)))
    with zipfile.ZipFile(temp) as new:
        assert new.testzip() is None
        assert all(new.read(n) == b for n, b in bundle_replacements.items())
        assert all(SHA(new.read(n)) == expected for n, expected in preserved.items())
    for name, data in replacements.items(): (ROOT / name).write_bytes(data)
    temp.replace(bundle)
    report = {'artifacts': records, 'public_api_byte_identical': True, 'qa_classes_excluded': True,
              'unrelated_bundle_entries_preserved': len(preserved), 'bundle_sha256': SHA(bundle.read_bytes()),
              'validation': 'Class-scoped staging and archive integrity only; runtime/test results are recorded separately.'}
    (OUT / report_name).write_text(json.dumps(report, indent=2) + '\n')
    print('Staged', len(records), 'class-scoped jar updates; public API bytes preserved')

if __name__ == '__main__': main()
