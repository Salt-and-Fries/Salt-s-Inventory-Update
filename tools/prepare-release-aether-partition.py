"""Preserve the exact Aether publisher pin outside the native Charm conflict."""
from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/'build/all-target-rollout/fixtures/1211-fabric-themed-v1/fixture.json'
output=ROOT/'build/blocked-setup-recovery/fixtures/aether-themed-without-charm.json'
original=json.loads(source.read_text());result=json.loads(source.read_text());removed=[];kept=[]
for row in result['artifacts']:
    with zipfile.ZipFile(row['path']) as jar:
        metadata=json.loads(jar.read('fabric.mod.json').decode('utf-8-sig'),strict=False) if 'fabric.mod.json' in jar.namelist() else {}
    assert hashlib.sha512(Path(row['path']).read_bytes()).hexdigest()==row['sha512']
    if metadata.get('id')=='charm':removed.append([row['filename'],row['sha512']])
    else:kept.append(row)
assert len(removed)==1
for row in kept:
    with zipfile.ZipFile(row['path']) as jar:
        metadata=json.loads(jar.read('fabric.mod.json').decode('utf-8-sig'),strict=False) if 'fabric.mod.json' in jar.namelist() else {}
    assert 'charm' not in metadata.get('depends',{}),'Required dependent needs its own partition disposition'
result['artifacts']=kept
result['setup_recovery']=dict(source_fixture='1211-fabric-themed-v1',
    scope='Disposable isolation of the previously reproduced native Charm+Aether KILN failure; every retained publisher binary/platform pin unchanged.',removed=removed,
    source_manifest_sha256=hashlib.sha256(source.read_bytes()).hexdigest())
text=json.dumps(result,indent=2)+'\n'
if output.exists():assert output.read_text()==text,'Preserve any earlier partition; do not replace evidence'
else:output.write_text(text)
print(output,len(kept),'exact original artifact rows retained')
