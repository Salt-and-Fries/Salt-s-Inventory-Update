"""Preserve exact modern Powah feature baselines for explicit equivalence review."""
from pathlib import Path
import datetime, hashlib, importlib.util, json

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('mi_baseline',ROOT/'tools/audit-retained-mi-baseline.py')
shared=importlib.util.module_from_spec(spec);spec.loader.exec_module(shared)


def main():
    verification=ROOT/'output/powah-all-targets/verification.json'
    original=json.loads(verification.read_text());rows=[]
    for target in original['targets']:
        mc=target['minecraft'];assert target['loader']=='neoforge'
        profile=ROOT/target['run'];run=profile.name.split('-integrated-',1)[1]
        runtime=ROOT/'versions'/mc/'neoforge/build/remaining-audit-native'/run
        staged=profile/'staged-artifacts.json';data=json.loads(staged.read_text())
        name=f'salts_inventory_compat-{mc}-NeoForge-0.1.3.jar'
        old=runtime/'mods'/name;wanted=next(a['sha256'] for a in data['artifacts'] if a['filename']==name)
        assert shared.digest(old)==wanted==target['sha256']
        current=ROOT/target['jar'];result=shared.compare(old,current,('com/salts_inventory_update/compat/powah/',))
        journal=ROOT/f'output/powah-all-targets/{mc}-journal.json'
        checks=json.loads(journal.read_text())['checks']
        assert len(checks)==target['assertions'] and all(c['passed'] for c in checks)
        native=next(a for a in data['artifacts'] if a['filename'].startswith('Powah-'))
        assert shared.digest(runtime/'mods'/native['filename'])==native['sha256']
        rows.append(dict(target=mc+'/neoforge',old_archive=str(old.relative_to(ROOT)),baseline_sha256=wanted,
            current_archive=str(current.relative_to(ROOT)),current_sha256=shared.digest(current),
            publisher=dict(filename=native['filename'],sha256=native['sha256']),
            baseline_manifest=dict(file=str(staged.relative_to(ROOT)),sha256=shared.digest(staged)),
            baseline_journal=dict(file=str(journal.relative_to(ROOT)),sha256=shared.digest(journal),checks=len(checks)),
            comparison=result))
    out=ROOT/'output/release-readiness/retained-powah-baseline-build13-01.json'
    assert not out.exists()
    out.write_text(json.dumps(dict(status='comparison-complete-explicit-acceptance-pending',
        generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),targets=rows,
        baseline_verification=dict(file=str(verification.relative_to(ROOT)),sha256=shared.digest(verification)),
        limits='Exact retained Powah runtime bytes and original scoped assertion journals, plus transitive Compatibility class comparisons. This is not fresh current-artifact gameplay. Core changed; current shared-path input, same publisher and per-feature equivalence need explicit acceptance before closing a coverage row.'),indent=2)+'\n')
    for row in rows:print(row['target'],row['baseline_journal']['checks'],'retained checks;',len(row['comparison']['classes']),'transitive Compatibility classes;',len(row['comparison']['changed']),'changed; acceptance explicit')


if __name__=='__main__':main()
