"""Promote only exact, reviewed click-fix candidates after focused live verification."""
from pathlib import Path
import argparse, hashlib, io, json, zipfile

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/ae2-click-investigation'
SHA=lambda data:hashlib.sha256(data).hexdigest()


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--candidates',required=True)
    p.add_argument('--legacy-run',required=True)
    p.add_argument('--requester-run',required=True)
    p.add_argument('--stage',required=True)
    a=p.parse_args()
    report_path=OUT/(a.stage+'.json')
    backup=ROOT/'build/ae2-click-investigation'/a.stage
    assert not report_path.exists() and not backup.exists(),'Do not rerun a one-time promotion'
    candidates=json.loads((OUT/(a.candidates+'.json')).read_text())
    baseline=candidates['baseline']
    assert len(baseline)==24 and len(candidates['candidates'])==18
    for name,expected in baseline.items():assert SHA((ROOT/name).read_bytes())==expected,name
    subjects={Path(r['file']).name:r for r in candidates['candidates']}
    proofs=[]
    for run,target in [(a.legacy_run,'1.20.1/fabric'),(a.requester_run,'26.1.2/neoforge')]:
        folder=OUT/run
        r=json.loads((folder/'run.json').read_text())
        assert r['target']==target and r['exit_code']==0
        assert len(r['trials'])==r['planned_trials']>=3
        assert all(t['failed']==0 and t['entry_only']==0 and t['passed']==t['cases']>0 for t in r['trials'])
        assert r['latency_ms']==75 and r['jitter_ms']==25 and r['timing']=='varied'
        tested=[s for s in r['subjects']if s['family']in ['core','compatibility']]
        assert len(tested)==2
        for s in tested:assert subjects[s['filename']]['sha256']==s['sha256']
        events=[json.loads(line)for line in (folder/'click-probe.jsonl').read_text().splitlines()]
        assert not any(e['event']=='probe-error'for e in events)
        assert events[-1]['event']=='probe-footer' and events[-1]['overflow']==0
        clicks=[e for e in events if e['event']=='server-mutation']
        # The optional protection scenario deliberately clicks the locked
        # hotbar host (player-menu slot 36). Its refusal is required; every
        # ordinary transfer must reach its original handler without rejection.
        protected=[e for e in clicks if e.get('session')==0 and e.get('slot')==36]
        ordinary=[e for e in clicks if e not in protected]
        assert ordinary and all(e.get('handler_entered')for e in ordinary)
        assert all(e['before']['stack']==e['after']['stack'] and e['before']['carried']==e['after']['carried']for e in protected)
        stale=[e for e in ordinary if e.get('session')==0 and e.get('handler_entered')
               and e['expected_primary_revision']!=e['before']['revision']]
        assert stale,'Must exercise real stale-revision acceptance, not just obtain lucky passes'
        proofs.append({'run':run,'target':target,'run_sha256':SHA((folder/'run.json').read_bytes()),
            'trace_sha256':SHA((folder/'click-probe.jsonl').read_bytes()),'server_clicks':len(clicks),
            'intentionally_refused_source_pickups':len(protected),'revalidated_stale_clicks':len(stale)})
    replacements={};records=[];bundle_replacements={}
    for record in candidates['candidates']:
        candidate=ROOT/record['file'];data=candidate.read_bytes()
        assert SHA(data)==record['sha256']
        destination=ROOT/'dist'/record['family']/candidate.name
        original=destination.read_bytes()
        with zipfile.ZipFile(io.BytesIO(original))as old,zipfile.ZipFile(io.BytesIO(data))as new:
            assert new.testzip()is None
            assert set(new.namelist())==set(old.namelist())|set(record['added_entries'])
            assert {n for n in old.namelist()if old.read(n)!=new.read(n)}==set(record['changed_entries'])
            api=[n for n in old.namelist()if '/api/'in n]
            assert all(old.read(n)==new.read(n)for n in api)
            assert not any('UiClickProbe'in n or '/functionaltest/'in n for n in new.namelist())
        name=destination.relative_to(ROOT).as_posix();replacements[name]=data
        bundle_replacements[f"{record['minecraft']}/{record['loader']}/{candidate.name}"]=data
        records.append({**record,'file':name,'before_sha256':SHA(original),'public_api_entries_preserved':len(api)})
    for family in ['core','compatibility']:
        hashes={p.name:SHA(replacements.get(p.relative_to(ROOT).as_posix(),p.read_bytes()))for p in (ROOT/'dist'/family).glob('*.jar')}
        sums=''.join(f'{hashes[n]}  {n}\n'for n in sorted(hashes)).encode()
        replacements[f'dist/{family}/SHA256SUMS.txt']=sums
        bundle_replacements[f'checksums/{family}/SHA256SUMS.txt']=sums
    bundle=ROOT/'dist/salts-inventory-all-latest.zip'
    temporary=bundle.with_suffix('.passive-source.tmp.zip')
    assert not temporary.exists()
    with zipfile.ZipFile(bundle)as old:
        assert set(bundle_replacements)<=set(old.namelist())
        preserved={i.filename:SHA(old.read(i))for i in old.infolist()if i.filename not in bundle_replacements}
        with zipfile.ZipFile(temporary,'w')as new:
            new.comment=old.comment
            for info in old.infolist():new.writestr(info,bundle_replacements.get(info.filename,old.read(info)))
    with zipfile.ZipFile(temporary)as new:
        assert new.testzip()is None
        assert all(new.read(n)==data for n,data in bundle_replacements.items())
        assert all(SHA(new.read(n))==digest for n,digest in preserved.items())
    backup.mkdir(parents=True)
    for name,data in replacements.items():
        target=ROOT/name;copy=backup/target.relative_to(ROOT/'dist');copy.parent.mkdir(parents=True,exist_ok=True)
        copy.write_bytes(target.read_bytes());target.write_bytes(data)
    temporary.replace(bundle)
    report={'scope':'Exact candidate promotion; no upstream or public API changes. Focused runtime proof on the two reproduced targets, not a complete gameplay suite.',
        'artifacts':records,'proofs':proofs,'public_api_byte_identical':True,'qa_excluded':True,
        'unrelated_bundle_entries_preserved':len(preserved),'bundle_sha256':SHA(bundle.read_bytes())}
    report_path.write_text(json.dumps(report,indent=2)+'\n')
    print('Promoted 12 Core and 6 AE2 compatibility candidates; API and unrelated archive entries preserved.')


if __name__=='__main__':main()
