"""Pin the publisher's 26.1.2 SSN/FLib binaries for the isolated live checks."""
from pathlib import Path
import json,hashlib,urllib.request
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'tmp/pdfs/ssn'
versions=json.loads((folder/'publisher-versions.json').read_text())
flibs=json.loads((folder/'flib-versions.json').read_text())
artifacts=[]
for version in [next(v for v in versions if v['id']=='YznUL5Pm'),next(v for v in flibs if v['id']=='uMEW9ig6')]:
    record=next(f for f in version['files']if f['primary']);path=folder/record['filename']
    if not path.exists():path.write_bytes(urllib.request.urlopen(record['url']).read())
    assert hashlib.sha512(path.read_bytes()).hexdigest()==record['hashes']['sha512']
    artifacts.append(dict(coordinate='publisher-exact:'+path.name,path=str(path.relative_to(ROOT)),filename=path.name,sha512=record['hashes']['sha512']))
fixture=dict(minecraft='26.1.2',loader='neoforge',runtime=dict(neoforge='26.1.2.78'),artifacts=artifacts)
(folder/'fixture-2612.json').write_text(json.dumps(fixture,indent=2));print('Pinned',[(a['filename'])for a in artifacts])
