"""Keep historical flags intact while linking their precise follow-up dispositions."""
from pathlib import Path
import csv
import hashlib
import io
import json
import shutil

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/release-readiness'
AUDIT=ROOT/'output/project-everything-regression'
FILES=['report.md','failures.md','summary.json','case-results.csv','specialized-entry-coverage.csv','changes-and-limits.md']


def main():
    backup=OUT/'historical-audit';backup.mkdir(parents=True,exist_ok=True)
    for name in FILES:
        if not (backup/name).exists():shutil.copy2(AUDIT/name,backup/name)
    dispositions=[
        dict(target='1.21.1/fabric',cases=['charm-kiln','atlas-charm-kiln'],
             disposition='Upstream combination isolated; matching Charm cases passed in compatible partitions. Original Charm+Aether combination remains unsupported.',
             evidence='output/blocked-setup-recovery/summary.json'),
        dict(target='1.21.1/neoforge',cases=['integration-curios-character'],
             disposition='Launcher ownership and exclusive equipment swapping fixed; focused native follow-up passed. Final candidate replay remains required.',
             evidence='output/curios-accessories-swap/summary.json'),
        dict(target='1.20.1/fabric',cases=['appbot-portable-mana-cell','megacells-portable-mana-cell'],
             disposition='Matching passive-host click race reproduced and fixed; original decisions observed without input retries. Final candidate replay remains required.',
             evidence='output/ae2-click-investigation/passive-source-fix.json'),
        dict(target='26.1.2/neoforge',cases=['atlas-merequester-wireless-terminal'],
             disposition='Matching passive-host click race reproduced and fixed under ordinary and delayed input. Final candidate replay remains required.',
             evidence='output/ae2-click-investigation/passive-source-fix.json')]
    record=dict(status='release-qualification-pending',historical_files={n:hashlib.sha256((backup/n).read_bytes()).hexdigest() for n in FILES},
        authoritative_checklist='RELEASE_READINESS.md',work_state='output/release-readiness/work-state.json',
        current_dashboard='output/release-readiness/report.md',current_coverage_backlog='output/release-readiness/coverage-backlog.json',
        current_candidate_pointer='output/release-readiness/candidate-current.json',
        case_dispositions=dispositions,
        native_startup_disposition=dict(target='26.1.2/fabric',evidence='output/blocked-setup-recovery/summary.json',
            disposition='Bumblezone+Polymer native failure isolated without SIU; both dependency-complete partitions passed. Original combination remains unsupported.'),
        boundary=dict(original_violations=31,current_source='passed',current_production_bytecode='passed',
            logs=['build/release-readiness/source-boundaries-after.log','build/release-readiness/source-build-06.log'],
            integration_runtime_acceptance='pending'),
        limits='Historical passes do not qualify rebuilt bytes. Current-artifact gameplay, specialized-entry reconciliation and independent review are still pending.')
    for row in dispositions:
        row['evidence_sha256']=hashlib.sha256((ROOT/row['evidence']).read_bytes()).hexdigest()
    (OUT/'reconciliation.json').write_text(json.dumps(record,indent=2)+'\n')
    summary=json.loads((backup/'summary.json').read_text());summary['release_readiness']=record
    (AUDIT/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    report=(backup/'report.md').read_text()
    report=report.replace('Status: **incomplete-or-needs-attention**.',
        'Status: **historical matrix; current release qualification is pending**.\n\n'
        'The counts below describe preserved earlier subjects. The [current checklist](../../RELEASE_READINESS.md) '
        'and [evidence reconciliation](../release-readiness/reconciliation.json) identify resolved flags and work '
        'still required on rebuilt jars. Equipment ownership, setup recovery and the passive-host AE2 click fix '
        'supersede their matching old failures; they do not qualify later artifact hashes.')
    report=report.replace('Additional repository check: **failed-preexisting**; 31 source-boundary violations. See [source-boundary-audit.json](source-boundary-audit.json). No rule was relaxed to hide this failure.',
        'Historical repository check: 31 source-boundary violations in [the preserved audit](source-boundary-audit.json). '
        'Current source and production-bytecode boundaries pass after public-API migration. '
        'Integration runtime acceptance remains open in the current checklist.')
    (AUDIT/'report.md').write_text(report)
    failures=(backup/'failures.md').read_text().replace('# Cases awaiting resolution','# Historical failure flags and current dispositions',1)
    failures=failures.replace('These cases remain flagged until a matching reproduction passes. Some flags identify test setup or observation problems; the original attempts remain preserved. This list is not a claim that every flag is an SIU defect.',
        'The original flags below are preserved. Their current dispositions are recorded in '
        '[the release reconciliation](../release-readiness/reconciliation.json), including exact follow-up file hashes. '
        'The [current candidate dashboard](../release-readiness/report.md) and '
        '[explicit coverage backlog](../release-readiness/coverage-backlog.csv) track the rebuilt jars. '
        'The remaining current work is in [RELEASE_READINESS.md](../../RELEASE_READINESS.md); '
        'historical results are not final-candidate qualification.')
    failures=failures.replace("Four Tom's Storage cases retain opening/policy coverage only.",
        "At that stage, four Tom-family workflows retained opening/policy coverage only; the customer view belongs to Tom's Trading Network. "
        'Later native terminal/configurator/link/customer workflows passed 49 focused assertions on 26.1.2 Fabric, '
        'with their source-provenance replay still pending as described in '
        '[checklist section 2](../../RELEASE_READINESS.md#2-finish-the-four-specific-tom-family-workflows). '
        'The equipment swap, portrait inset and AE2 follow-ups likewise retain their named scopes in the checklist.')
    failures=failures.replace('All 24 shipping jars remained unchanged.','All 24 shipping jars at that recovery stage remained unchanged.')
    failures=failures.replace('| Target | Case | Latest measured problem |','| Target | Case | Original measured problem |')
    (AUDIT/'failures.md').write_text(failures)
    # Retain original per-row statuses; add explicit follow-up columns instead
    # of converting an opening-only or historical result into a current pass.
    for name in ['case-results.csv','specialized-entry-coverage.csv']:
        reader=csv.DictReader(io.StringIO((backup/name).read_text()));fields=reader.fieldnames
        rows=list(reader)
        for row in rows:
            match=next((d for d in dispositions if d['target']==row.get('target') and row.get('id',row.get('case')) in d['cases']),None)
            row['release_disposition']=match['disposition'] if match else 'Current-artifact coverage disposition pending; original status retained.'
            row['disposition_evidence']=match['evidence'] if match else 'RELEASE_READINESS.md'
        with (AUDIT/name).open('w',newline='') as stream:
            writer=csv.DictWriter(stream,fieldnames=fields+['release_disposition','disposition_evidence']);writer.writeheader();writer.writerows(rows)
    print('Preserved historical audit; linked exact equipment/setup/AE2 follow-ups without promoting opening-only coverage.')


if __name__=='__main__':main()
