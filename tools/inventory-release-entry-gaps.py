"""Locate historical specialized screens in the exact current publisher cohorts."""
from pathlib import Path
import collections
import hashlib
import json
import sys
import zipfile

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES

def main():
    gaps=json.loads((ROOT/'output/project-everything-regression/native-entry-cases.json').read_text())['specialized_entry_gaps']
    ledger=json.loads((ROOT/'build/all-target-rollout/ui-capture-ledger.json').read_text())
    names=collections.defaultdict(set)
    for row in ledger:
        for artifact in row.get('staged_artifacts',{}).get('artifacts',[]):
            if artifact.get('sha256') and artifact.get('filename'):names[artifact['sha256']].add(artifact['filename'])
    candidates={}
    for target,fixtures in FIXTURES.items():
        rows=[]
        for fixture in fixtures:
            path=ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json'
            if not path.exists():continue
            for artifact in json.loads(path.read_text())['artifacts']:
                rows.append(dict(fixture=fixture,filename=artifact['filename'],path=artifact['path'],sha512=artifact['sha512']))
        candidates[target]=rows
    hashes={};contents={};result=[]
    for gap in gaps:
        wanted=gap['screen'].replace('.','/')+'.class';possible_names=set().union(*(names[h] for h in gap['publisher_sha256']))
        found=[]
        for artifact in candidates[gap['minecraft']+'/'+gap['loader']]:
            if artifact['filename'] not in possible_names:continue
            path=ROOT/artifact['path']
            if path not in hashes:
                hashes[path]=hashlib.sha256(path.read_bytes()).hexdigest()
                with zipfile.ZipFile(path) as archive:contents[path]=set(archive.namelist())
            if hashes[path] not in gap['publisher_sha256']:continue
            if wanted in contents[path]:found.append(dict(**artifact,sha256=hashes[path]))
        result.append(dict(**gap,current_exact_cohorts=found,
            classification='native-screen-present-in-current-exact-publisher' if found else 'captured-publisher-not-located-in-current-cohorts',
            acceptance='pending-functional-disposition',
            note='Presence or absence in this inventory is not a gameplay pass and does not establish upstream unavailability. Approved replacement surfaces need an explicit matching acceptance record.'))
    out=ROOT/'output/release-readiness/specialized-entry-inventory.json'
    out.write_text(json.dumps(dict(scope='Historical specialized-entry inventory reconciled to current publisher pins, without promoting entry-only coverage.',
        counts=dict(collections.Counter(r['classification'] for r in result)),entries=result),indent=2)+'\n')
    print(collections.Counter(r['classification'] for r in result));print(out)

if __name__=='__main__':main()
