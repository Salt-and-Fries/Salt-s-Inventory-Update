"""Reconcile the reviewed native Polymer selector from its existing sealed run."""
from pathlib import Path
import datetime,json,sys
from native_selector_evidence import derive,TARGET
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects


def main():
    out=ROOT/'output/release-readiness/coverage-native-selectors-build13-02.json';assert not out.exists()
    sealed=ROOT/'output/release-readiness/focused-evidence-readiness-archived-publisher-1211-fabric-c0-01.json'
    row=derive(ROOT,sealed,current_subjects(TARGET))
    out.write_text(json.dumps(dict(status='reviewed-native-selector-acceptances-reconciled',generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        records=[row],limits='Reconciliation of existing sealed gameplay assertions only; no new game execution or broader feature claim.'),indent=2)+'\n')
    print('One exact native-only selector reconciled;',row['checks'],'existing scoped assertions')


if __name__=='__main__':main()
