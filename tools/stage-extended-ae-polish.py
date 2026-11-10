"""Publish the four focused-tested Extended AE frame corrections."""
from pathlib import Path
import hashlib, json, shutil, zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'output/extended-ae-modern-polish'
read = lambda p: json.loads(p.read_text())
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    before = read(OUT/'dist-before.json')
    updates, targets, evidence = {}, [], []
    for mc, loader, label, run in [
        ('1.20.1','fabric','Fabric','extended-ae-polish-02'),
        ('1.20.1','forge','Forge','extended-ae-polish-01'),
        ('1.21.1','neoforge','NeoForge','extended-ae-polish-01'),
        ('26.1.2','neoforge','NeoForge','extended-ae-polish-01'),
    ]:
        folder = OUT/f'{mc}-{loader}'
        checks = read(folder/'checks.json')
        required = {'extended-io-port bottom aligns with full native tray',
            'mod-export-bus bottom aligns with full native tray',
            'mod-storage-bus exposed tab top is clickable',
            'extended-drive exposed tab top is clickable',
            'extended-pattern-provider exposed tab top is clickable',
            'moved-pattern-provider exposed tab top is clickable',
            'Parent window moved through title drag',
            'Existing AE2 Drive remains integrated'}
        assert checks and all(c['passed'] for c in checks)
        assert required <= {c['name'] for c in checks}
        runtime = ROOT/(f'build/remaining-ui-audit/gameplay/{mc}-{loader}-siu-{run}'
            if mc == '1.20.1' and loader == 'fabric' else f'build/all-target-rollout/gameplay/{mc}-{loader}-{run}')
        name = f'salts_inventory_compat-{mc}-{label}-0.1.3.jar'
        jar = ROOT/f'compatibility/versions/{mc}/{loader}/build/libs'/name
        tested = next(a for a in read(runtime/'staged-artifacts.json')['artifacts'] if a['family'] == 'compatibility')
        assert sha(jar) == tested['sha256'], ('Built jar differs from live-tested bytes', name)
        source = ROOT/f'compatibility/ae2/versions/{mc}'/('common' if mc == '1.20.1' else loader)
        profiles = source/'src/main/resources/assets/ae2/screens/desktop'
        with zipfile.ZipFile(jar) as z, zipfile.ZipFile(ROOT/'dist/compatibility'/name) as old:
            assert z.testzip() is None
            for profile in ['ex_io_port','mod_export_bus']:
                assert json.loads(z.read(f'assets/ae2/screens/desktop/{profile}.json')) == read(profiles/(profile+'.json'))
            assert 'com/salts_inventory_update/compat/ae2/nativeview/ExtendedAeLayout.class' in z.namelist()
            assert not any(n.startswith(('appeng/','com/salts_inventory_update/api/','com/salts_inventory_update/functionaltest/','com/siu_reference/')) for n in z.namelist())
            assert {n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.endswith('.png')} == {n:hashlib.sha256(old.read(n)).hexdigest() for n in old.namelist() if n.endswith('.png')}
        updates[f'{mc}/{loader}/{name}'] = jar
        targets.append(dict(minecraft=mc, loader=loader, jar='dist/compatibility/'+name,
            sha256=sha(jar), checks=checks, runtime=str(runtime.relative_to(ROOT)),
            production_runtime=loader == 'fabric' or mc != '1.20.1',
            runtime_note='Official SRG-to-named QA remap; original and derived bytes recorded' if loader == 'forge' else 'Shipping jar bytes in production/intermediary runtime'))
        for r in read(folder/'captures.json'):
            p = Path(r['capture'])
            evidence.append(dict(minecraft=mc, loader=loader, name=r['name'], path=str(p.relative_to(ROOT)), sha256=sha(p)))

    mutable = {'dist/compatibility/'+p.name for p in updates.values()} | {'dist/compatibility/SHA256SUMS.txt','dist/salts-inventory-all-latest.zip'}
    for path, digest in before.items():
        if path not in mutable:
            assert sha(ROOT/path) == digest, ('Unrelated artifact changed', path)
    sums = {p.name:sha(p) for p in (ROOT/'dist/compatibility').glob('*.jar')}
    sums.update({p.name:sha(p) for p in updates.values()})
    checksum = ''.join(f'{sums[n]}  {n}\n' for n in sorted(sums)).encode()
    bundle = ROOT/'dist/salts-inventory-all-latest.zip'
    temporary = bundle.with_suffix('.extended-ae-polish.tmp.zip')
    checkpath = 'checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle) as old:
        assert set(updates) <= set(old.namelist())
        untouched = {n:hashlib.sha256(old.read(n)).hexdigest() for n in old.namelist() if n not in updates and n != checkpath}
        with zipfile.ZipFile(temporary,'w') as z:
            z.comment = old.comment
            for info in old.infolist():
                z.writestr(info, updates[info.filename].read_bytes() if info.filename in updates else checksum if info.filename == checkpath else old.read(info))
    with zipfile.ZipFile(temporary) as z:
        assert z.testzip() is None
        assert all(hashlib.sha256(z.read(n)).hexdigest() == digest for n,digest in untouched.items())
        assert all(z.read(n) == p.read_bytes() for n,p in updates.items())
    for p in updates.values():
        shutil.copy2(p, ROOT/'dist/compatibility'/p.name)
    (ROOT/'dist/compatibility/SHA256SUMS.txt').write_bytes(checksum)
    temporary.replace(bundle)
    report = dict(status='pass', scope='Extended AE upgrade-tray frame height and original raised top-right tabs',
        targets=targets, focused_assertions=sum(len(t['checks']) for t in targets), captures=evidence,
        core_and_api_unchanged=True, unrelated_target_jars_unchanged=True, unrelated_bundle_entries_preserved=True,
        bundle_sha256=sha(bundle),
        limits='Focused geometry, pointer navigation, title-drag and existing AE2 Drive checks only. No complete gameplay suite, exhaustive recipes, dedicated server, controllers or resource-pack testing. Forge 1.20.1 uses the recorded named QA remap; Fabric 1.20.1 and the modern targets use production release bytes.')
    (OUT/'verification.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ['status','focused_assertions','core_and_api_unchanged']}, indent=2))


if __name__ == '__main__':
    main()
