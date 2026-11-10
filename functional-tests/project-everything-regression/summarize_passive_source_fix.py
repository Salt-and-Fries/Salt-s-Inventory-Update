"""Summarize immutable before/after evidence without erasing unsuccessful attempts."""
from pathlib import Path
import hashlib, json, zipfile

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/ae2-click-investigation'
SHA=lambda data:hashlib.sha256(data).hexdigest()


def summarize(name,green=False):
    folder=OUT/name;r=json.loads((folder/'run.json').read_text())
    assert r.get('finished_at') and r.get('exit_code')==0
    checks=[]
    for trial in sorted(folder.glob('trial-*/mod-interaction-checks.json')):
        check=json.loads(trial.read_text());checks.extend(check['checks'])
    if green:
        assert len(r['trials'])==r['planned_trials']
        assert all(t['cases']>0 and t['failed']==0 and t['entry_only']==0 and t['passed']==t['cases']for t in r['trials'])
        assert all(c['passed']for c in checks)
    trace=folder/'click-probe.jsonl';events=[]
    if trace.exists():
        events=[json.loads(line)for line in trace.read_text().splitlines()]
        assert not any(e['event']=='probe-error'for e in events)
        assert events[-1].get('overflow')==0
    clicks=[e for e in events if e['event']=='server-mutation']
    ordinary=[e for e in clicks if not(e.get('session')==0 and e.get('slot')==36)]
    stale=[e for e in ordinary if e['session']==0 and e['expected_primary_revision']!=e['before']['revision']]
    return {'run':name,'target':r['target'],'run_sha256':SHA((folder/'run.json').read_bytes()),
        'trials':len(r['trials']),'menu_executions':sum(t['cases']for t in r['trials']),
        'passed':sum(t['passed']for t in r['trials']),'failed':sum(t['failed']for t in r['trials']),
        'assertions':len(checks),'failed_assertions':[{'case':c['case'],'check':c['check']}for c in checks if not c['passed']],
        'probe':r['click_probe'],'quiet':r['quiet_capture'],'latency_ms':r['latency_ms'],'jitter_ms':r['jitter_ms'],
        'wait_pattern':r['timing'],'source_lock_pickup_checks':r.get('require_portable_lock',False),
        'ordinary_server_clicks':len(ordinary),'ordinary_rejections':sum(not e['handler_entered']for e in ordinary),
        'accepted_stale_revision_clicks':sum(e['handler_entered']for e in stale),
        'rejected_stale_revision_clicks':sum(not e['handler_entered']for e in stale),
        'protected_source_clicks':len(clicks)-len(ordinary),
        'trace_sha256':SHA(trace.read_bytes())if trace.exists()else None,
        'subjects':[s for s in r['subjects']if s['family']in ['core','compatibility']]}


def main():
    red=[summarize(n)for n in ['legacy-probe-latency-red-03','requester-probe-latency-red-02']]
    green=[summarize(n,True)for n in ['legacy-passive-source-green-05','requester-passive-source-green-02',
        'legacy-passive-source-ordinary-02','requester-passive-source-ordinary-02']]
    assert all(r['failed']>0 and r['rejected_stale_revision_clicks']>0 for r in red)
    assert all(r['ordinary_rejections']==0 for r in green)
    assert all(r['accepted_stale_revision_clicks']>0 for r in green if r['probe'])
    promotion=json.loads((OUT/'passive-source-promoted-01.json').read_text())
    current={Path(a['file']).name:a['sha256']for a in promotion['artifacts']}
    for r in green:
        for s in r['subjects']:assert current[s['filename']]==s['sha256']
    for a in promotion['artifacts']:assert SHA((ROOT/a['file']).read_bytes())==a['sha256']
    distribution=[]
    with zipfile.ZipFile(ROOT/'dist/salts-inventory-all-latest.zip')as bundle:
        assert bundle.testzip()is None
        for family in ['core','compatibility']:
            checksum_file=ROOT/'dist'/family/'SHA256SUMS.txt'
            hashes={line.split('  ',1)[1]:line.split('  ',1)[0]for line in checksum_file.read_text().splitlines()}
            assert len(hashes)==12
            for name,digest in hashes.items():
                data=(checksum_file.parent/name).read_bytes();assert SHA(data)==digest
                _,version,loader,*_=name.split('-')
                assert bundle.read(f'{version}/{loader.lower()}/{name}')==data
            assert bundle.read(f'checksums/{family}/SHA256SUMS.txt')==checksum_file.read_bytes()
            distribution.append({'family':family,'verified_jars':len(hashes),'checksums_sha256':SHA(checksum_file.read_bytes())})
    counts={key:sum(r[key]for r in green)for key in ['trials','menu_executions','assertions','ordinary_server_clicks','accepted_stale_revision_clicks','protected_source_clicks']}
    report={'conclusion':'Reproduced and fixed; passive host-energy changes must not invalidate an otherwise exact player-inventory click.',
        'red':red,'green':green,'passing_focused_checks':counts,
        'headless':json.loads((OUT/'locked-source-headless-01.json').read_text()),
        'promotion':promotion,
        'distribution_verification':distribution,
        'build_logs':['build/ae2-click-investigation/core-packaging-01.log',
            'build/ae2-click-investigation/core-packaging-03.log',
            'build/ae2-click-investigation/compat-packaging-01.log',
            'build/ae2-click-investigation/ae2-symbols-01.log'],
        'unsuccessful_attempts_preserved':{
            'legacy-passive-source-green-01':'Core-only candidate: missing AE2 source registration; rejected ordinary clicks.',
            'requester-passive-source-green-01':'Core-only candidate: missing AE2 source registration; rejected ordinary clicks.',
            'legacy-passive-source-green-02':'Registration candidate exposed missing legacy server source-lease acquisition; rejected ordinary clicks.',
            'legacy-passive-source-green-03':'Source lock assertion passed; new pickup test targeted crafting-result slot 0. Invalid pickup-test setup.',
            'legacy-passive-source-ordinary-01':'Same incorrect slot 0 in the new pickup test; not evidence about transfers.',
            'requester-passive-source-ordinary-01':'Same incorrect slot 0 in the new pickup test; not evidence about transfers.',
            'legacy-passive-source-green-04':'Correct hotbar test reproduced missing legacy ordinary-click source guard. Candidate rejected.',
            'legacy-probe-latency-02':'Original reproduction remains: separate server-branch evidence is valid, but unused modern packet accessor produced legacy probe errors. Clean trace repeated as legacy-probe-latency-red-03.'},
        'coverage_limit':'Live gameplay on 1.20.1 Fabric and 26.1.2 NeoForge only. Focused snapshot-validator tests on six mapped versions; Core packaging on twelve targets and AE2 native-symbol/packaging checks on its six supported targets. Not a complete functional/gameplay suite; arbitrary mod combinations and all network conditions are not certified.'}
    destination=OUT/'passive-source-fix.json';assert not destination.exists()
    destination.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(counts));print('Updated jars',len(promotion['artifacts']))


if __name__=='__main__':main()
