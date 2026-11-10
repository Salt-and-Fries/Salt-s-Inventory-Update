"""Add exact upstream 1.20.1 Ecologics releases to disposable comparison fixtures."""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
headers={'User-Agent':'SIU-Project-Everything-local-review/1.0'}
def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=45) as response:return response.read()
versions=json.loads(fetch('https://api.modrinth.com/v2/project/ecologics/version?game_versions=%5B%221.20.1%22%5D'))
out=ROOT/'build/project-everything-56-implementation'
releases=[]
for loader,cohort in [('fabric','bb'),('forge','bf')]:
    release=next(v for v in versions if loader in v['loaders'] and v['version_number'].lower()=='2.2.2-'+loader)
    file=next(f for f in release['files'] if f.get('primary'))
    target=out/'upstream'/file['filename'];target.parent.mkdir(parents=True,exist_ok=True)
    if not target.exists():target.write_bytes(fetch(file['url']))
    assert hashlib.sha512(target.read_bytes()).hexdigest()==file['hashes']['sha512']
    record=dict(sha1=file['hashes']['sha1'],sha512=file['hashes']['sha512'],path=str(target.relative_to(ROOT)),
                filename=file['filename'],original_path=file['filename'],mod_ids=['ecologics'],required_mod_ids=['minecraft',loader],nested=[],coordinate='modrinth:'+release['id'])
    fixture=json.loads((out/(cohort+'-combined-fixture.json')).read_text())
    fixture['artifacts'].append(record)
    fixture['roots'].append(dict(entry=192,title='Ecologics',minecraft='1.20.1',loader=loader,release=record))
    (out/(cohort+'-ecologics-fixture.json')).write_text(json.dumps(fixture,indent=2)+'\n')
    releases.append(dict(loader=loader,version_id=release['id'],version=release['version_number'],url=file['url'],**record))
(out/'ecologics-upstream-releases.json').write_text(json.dumps(releases,indent=2)+'\n')
print([(r['loader'],r['version'],r['filename']) for r in releases])
