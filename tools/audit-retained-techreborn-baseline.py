"""Seal exact Tech Reborn baselines for explicit per-feature equivalence review."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, io, json, shutil, sys, traceback, zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('retained_shared', ROOT / 'tools/audit-retained-mi-baseline.py')
shared = importlib.util.module_from_spec(spec); spec.loader.exec_module(shared)


def ref(path): return dict(file=str(path.relative_to(ROOT)), sha256=shared.digest(path))


def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--id', required=True); args = parser.parse_args()
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.id)
    out = ROOT / 'output/release-readiness' / ('retained-techreborn-baseline-' + args.id + '.json'); assert not out.exists()
    copies = out.with_suffix(''); assert not copies.exists(); copies.mkdir()
    source = ROOT / 'output/tech-reborn-all-targets/verification.json'
    original = json.loads(source.read_text()); assert original['status'] == 'passed'
    rows = []
    for target in original['coverage']:
        mc = target['minecraft']; journals = []; subjects = []
        for key, name in [('checks', 'checks.json'), ('heat_checks', 'heat-checks.json'), ('nuclear_checks', 'nuclear-checks.json')]:
            if not target[key]: continue
            run = target['heat_run'] if key == 'heat_checks' else target['run']
            profile = ROOT / 'build/remaining-ui-audit/gameplay' / run
            manifest = profile / 'staged-artifacts.json'; staged = json.loads(manifest.read_text())['artifacts']
            runtime = Path(json.loads((profile / 'launch.json').read_text())['cwd'])
            artifact = next(v for v in staged if v['filename'].startswith('salts_inventory_compat-'))
            old = runtime / 'mods' / artifact['filename']; assert shared.digest(old) == artifact['sha256']
            if key == 'checks': assert artifact['sha256'] == target['compatibility_sha256']
            current = ROOT / 'dist/compatibility' / artifact['filename']
            comparison = shared.compare(old, current, ('com/salts_inventory_update/compat/techreborn/',))
            assert not comparison['changed'], (mc, key, comparison['changed'])
            retained = copies / (artifact['sha256'] + '-' + artifact['filename'])
            if retained.exists(): assert shared.digest(retained) == artifact['sha256']
            else: shutil.copy2(old, retained)
            assert shared.digest(retained) == artifact['sha256']
            native = [v for v in staged if v['filename'].lower().startswith(('techreborn-', 'reborncore-'))]
            tech = [v for v in native if v['filename'].lower().startswith('techreborn-')]
            reborn = [v for v in native if v['filename'].lower().startswith('reborncore-')]
            assert len(tech) == 1 and len(reborn) <= 1, native
            for row in native: assert shared.digest(runtime / 'mods' / row['filename']) == row['sha256']
            embedded = []
            if not reborn:
                # These exact publisher releases ship RebornCore as a
                # Fabric-declared nested jar, not a second top-level mod.
                with zipfile.ZipFile(runtime / 'mods' / tech[0]['filename']) as jar:
                    metadata = json.loads(jar.read('fabric.mod.json'))
                    for declared in metadata.get('jars', []):
                        raw = jar.read(declared['file'])
                        with zipfile.ZipFile(io.BytesIO(raw)) as nested:
                            child = json.loads(nested.read('fabric.mod.json'))
                        if child['id'] == 'reborncore':
                            embedded.append(dict(parent=tech[0]['filename'], entry=declared['file'],
                                id=child['id'], version=child['version'], sha256=hashlib.sha256(raw).hexdigest()))
                assert len(embedded) == 1, 'Missing or ambiguous declared native RebornCore'
            subjects.append(dict(group=key, profile=str(profile.relative_to(ROOT)), baseline_manifest=ref(manifest),
                old_archive=str(old), retained_copy=ref(retained), baseline_sha256=artifact['sha256'],
                current_archive=ref(current), publishers=[dict(filename=v['filename'], sha256=v['sha256']) for v in native],
                embedded_publishers=embedded, comparison=comparison))
            journal = ROOT / 'output/tech-reborn-all-targets' / run / name
            data = json.loads(journal.read_text()); assert data['status'] == 'passed'
            assert len(data['checks']) == target[key] and all(v['passed'] for v in data['checks'])
            captures = []
            for capture in data['captures']:
                picture = ROOT / capture['capture']; assert shared.digest(picture) == capture['sha256']
                with Image.open(picture) as image: image.verify()
                captures.append(dict(id=capture['id'], capture=ref(picture)))
            assert captures
            journals.append(dict(group=key, journal=ref(journal), checks=len(data['checks']), captures=captures))
        rows.append(dict(target=mc + '/fabric', subjects=subjects, journals=journals, geometry=target['geometry']))
    result = dict(status='retained-inputs-sealed-explicit-acceptance-pending',
        generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), baseline_verification=ref(source), targets=rows,
        limits='Exact retained native typed-action/geometry/formed-heat journals and binaries; not new gameplay. Current matching publisher entry and changed Core/API paths remain separate acceptance requirements. Nuclear evidence covers native fuel restrictions and hologram, not reactor energy production. Upstream 6.0.5 padded-serializer warnings remain explicit and are not certified recipes.')
    out.write_text(json.dumps(result, indent=2) + '\n')
    for row in rows:
        print(row['target'], sum(v['checks'] for v in row['journals']), 'retained checks;',
              len(row['subjects'][0]['comparison']['classes']), 'unchanged transitive classes; explicit acceptance pending')


if __name__ == '__main__':
    try: main()
    except BaseException as error:
        if '--id' in sys.argv:
            name = sys.argv[sys.argv.index('--id') + 1]
            if name and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in name):
                marker = ROOT / 'output/release-readiness' / ('retained-techreborn-baseline-' + name + '.failure.json')
                if not marker.exists(): marker.write_text(json.dumps(dict(status='failed', error=repr(error),
                    trace=traceback.format_exc(), scope='Offline retained-evidence audit failed; no acceptance established'), indent=2) + '\n')
        raise
