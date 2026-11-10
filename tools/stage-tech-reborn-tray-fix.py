"""Stage the upgrade-tray rendering fix after its focused production checks."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/tech-reborn-tray-fix'
sha = lambda data: hashlib.sha256(data).hexdigest()


def main():
    checks = json.loads((OUT / 'checks.json').read_text())
    assert checks['status'] == 'passed' and checks['screens'] == 28
    name = 'salts_inventory_compat-1.20.1-Fabric-0.1.3.jar'
    source = ROOT / 'compatibility/versions/1.20.1/fabric/build/libs' / name
    data = source.read_bytes()
    run = ROOT / checks['runtime']
    tested = next(r for r in json.loads((run / 'staged-artifacts.json').read_text())['artifacts']
                  if r['family'] == 'compatibility')
    assert sha(data) == tested['sha256'], 'Built jar differs from the live-tested bytes'
    mutable = {'dist/compatibility/' + name, 'dist/compatibility/SHA256SUMS.txt', 'dist/salts-inventory-all-latest.zip'}
    for path, expected in json.loads((OUT / 'baseline.json').read_text()).items():
        if path not in mutable:
            assert sha((ROOT / path).read_bytes()) == expected, path
    with zipfile.ZipFile(source) as jar:
        assert jar.testzip() is None
        assert not any(p.startswith(('com/siu_reference/', 'techreborn/', 'reborncore/', 'com/salts_inventory_update/api/'))
                       for p in jar.namelist())
    sums = {p.name: sha(p.read_bytes()) for p in (ROOT / 'dist/compatibility').glob('*.jar')}
    sums[name] = sha(data)
    checksum = ''.join(f'{sums[n]}  {n}\n' for n in sorted(sums)).encode()
    key, checksum_key = '1.20.1/fabric/' + name, 'checksums/compatibility/SHA256SUMS.txt'
    bundle = ROOT / 'dist/salts-inventory-all-latest.zip'
    temp = bundle.with_suffix('.tech-reborn-tray.tmp.zip')
    with zipfile.ZipFile(bundle) as old:
        assert key in old.namelist() and checksum_key in old.namelist()
        unchanged = {i.filename: sha(old.read(i)) for i in old.infolist() if i.filename not in (key, checksum_key)}
        with zipfile.ZipFile(temp, 'w') as new:
            new.comment = old.comment
            for info in old.infolist():
                new.writestr(info, data if info.filename == key else checksum if info.filename == checksum_key else old.read(info))
    with zipfile.ZipFile(temp) as new:
        assert new.testzip() is None and new.read(key) == data
        assert all(sha(new.read(path)) == expected for path, expected in unchanged.items())
    shutil.copy2(source, ROOT / 'dist/compatibility' / name)
    (ROOT / 'dist/compatibility/SHA256SUMS.txt').write_bytes(checksum)
    temp.replace(bundle)
    report = dict(**checks, jar_sha256=sha(data), bundle_sha256=sha(bundle.read_bytes()),
                  core_and_api_unchanged=True, unrelated_artifacts_preserved=True)
    (OUT / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
