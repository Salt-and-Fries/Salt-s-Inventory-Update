"""Seal named current-subject acceptances without rerunning or broadening them."""
from pathlib import Path
import datetime, hashlib, json, sys, zipfile

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects, subjects_match

CASES=[
    ('readiness-final-toms-terminal-01','terminal','toms-storage-terminal','terminal-connected.json',17,
     ['com.tom.storagemod.menu.StorageTerminalMenu']),
    ('readiness-final-toms-configurator-01','configurator','registry-item-toms_storage-inventory_configurator','configurator-open.json',11,
     ['com.tom.storagemod.menu.InventoryConfiguratorMenu']),
    ('readiness-final-toms-link-01','link','atlas-toms-inventory-link','link-open.json',11,
     ['com.tom.storagemod.menu.InventoryLinkMenu']),
    ('readiness-final-toms-vending-01','vending','atlas-toms-customer','vending-customer.json',10,
     ['com.tom.trading.menu.VendingMachineConfigMenu','com.tom.trading.menu.VendingMachineTradingMenu'])]


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def ref(path):return dict(file=str(path.relative_to(ROOT)),sha256=sha(path))


def main():
    out=ROOT/'output/release-readiness/coverage-standalone-build13-02.json';assert not out.exists()
    archive=out.with_suffix('.qa-inputs.zip');assert not archive.exists()
    inputs={};records=[]
    for run,workflow,case,snapshot,count,classes in CASES:
        summary=ROOT/'output/release-readiness'/run/'summary.json';data=json.loads(summary.read_text())
        assert data['status']=='passed'and data['exit_code']==0 and data['checks_exit_code']==0
        assert data['target']=='26.1.2/fabric'and data['workflow']==workflow
        assert subjects_match(data['subjects'],current_subjects(data['target']))
        profile=ROOT/data['profile'];staged=profile/'staged-artifacts.json'
        actual=json.loads(staged.read_text())['artifacts'];assert subjects_match(actual,current_subjects(data['target']))
        for path,expected in data['qa_inputs'].items():
            assert sha(ROOT/path)==expected,path
            key=Path(path).as_posix();raw=(ROOT/path).read_bytes()
            assert key not in inputs or inputs[key]==raw;inputs[key]=raw
        journal=profile/'toms-release/checks.json';checks=json.loads(journal.read_text())
        assert len(checks['checks'])==count and all(v['passed']for v in checks['checks'])
        observed=profile/'toms-release'/snapshot;state=json.loads(observed.read_text())
        assert all(any(m['type']==cls and m['valid'] for m in state['menus'])for cls in classes)
        publishers=[]
        for row in actual:
            native=Path(row['file'])
            if not native.is_absolute():native=ROOT/native
            if row['family']!='upstream':continue
            with zipfile.ZipFile(native)as jar:
                if not any(cls.replace('.','/')+'.class'in jar.namelist()for cls in classes):continue
            assert sha(native)==row['sha256']
            publishers.append(dict(filename=row['filename'],sha256=row['sha256'],sha512=row['sha512']))
        assert publishers
        records.append(dict(target=data['target'],case=case,disposition='functional-pass',native_classes=classes,
            corrected_historical_classification=(workflow=='configurator'),
            classification_note=('Historical registry-item capture was incorrectly labelled with a vending menu. The named current workflow enters the actual InventoryConfiguratorMenu through the configurator item.'if workflow=='configurator'else None),
            summary=ref(summary),staged=ref(staged),acceptance=ref(journal),native_entry=ref(observed),
            full_state_inputs=[ref(p)for p in sorted((profile/'toms-release').glob('*.json'))],
            checks=count,publishers=publishers,subjects=current_subjects(data['target']),
            scope=checks['scope'],limits='Named focused workflow and exact native publisher only; no other target or complete mod gameplay claim.'))
    with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED)as bundle:
        for path,raw in sorted(inputs.items()):bundle.writestr(path,raw)
    result=dict(status='named-current-artifact-acceptances-reconciled',generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        qa_source_archive=ref(archive),qa_inputs={p:hashlib.sha256(v).hexdigest()for p,v in inputs.items()},records=records,
        limits='Reconciliation of four already-passing exact-build13 focused workflows, not a new test run. Incorrect historical capture metadata remains recorded alongside the corrected native entry.')
    out.write_text(json.dumps(result,indent=2)+'\n')
    print(len(records),'named current-artifact acceptances sealed;',sum(v['checks']for v in records),'existing focused checks')


if __name__=='__main__':main()
