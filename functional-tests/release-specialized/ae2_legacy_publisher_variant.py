"""Select the already-audited AE2 15.4.11 binary without patching its bytes."""
from pathlib import Path
import argparse,copy,hashlib,json,zipfile

ROOT=Path(__file__).resolve().parents[2]
FIXTURE='build/all-target-rollout/fixtures/1201-fabric-prominent-shop-tail-v1/fixture.json'
FILENAME='appliedenergistics2-fabric-15.4.11.jar'
SHA512='c3206fd416bb14db17af7198a711e51504a853cd6c55a7c4eca8e0700c755880fc9268c033a1b97e158be4bb2d2eed6d13b12f113456904fe4dfe03f00ac3dde'


def native_artifact(root=ROOT):
    root=Path(root).resolve();fixture=json.loads((root/FIXTURE).read_text())
    assert (fixture['minecraft'],fixture['loader'])==('1.20.1','fabric')
    found=[a for a in fixture['artifacts']if a['filename']==FILENAME]
    assert len(found)==1 and found[0]['sha512']==SHA512
    raw=(root/found[0]['path']).read_bytes();assert hashlib.sha512(raw).hexdigest()==SHA512
    with zipfile.ZipFile(root/found[0]['path'])as jar:
        meta=json.loads(jar.read('fabric.mod.json'));assert(meta['id'],meta['version'])==('ae2','15.4.11')
    return copy.deepcopy(found[0])


def substitute_manifest(original,artifact):
    """Only the AE2 pin and its matching descriptive version metadata change."""
    assert(original['minecraft'],original['loader'],original['scenario'])==('1.20.1','fabric','ae2-atlas')
    assert artifact['filename']==FILENAME and artifact['sha512']==SHA512
    value=copy.deepcopy(original)
    rows=[(i,a)for i,a in enumerate(value['artifacts'])if a['filename'].startswith('appliedenergistics2-fabric-')]
    assert len(rows)==1
    versions=[v for v in value['versions']if v['project_id']=='XxWD5pD3']
    assert len(versions)==1
    if rows[0][1]['filename']==FILENAME:
        # The normal preparator can already select the exact reviewed release.
        # Preserve its real version ID/coordinate and every original field.
        existing=rows[0][1];assert existing['sha512']==SHA512 and versions[0]['version']=='15.4.11'
        path=Path(existing['path']);path=path if path.is_absolute()else ROOT/path
        assert path.is_file()and hashlib.sha512(path.read_bytes()).hexdigest()==SHA512
        return value
    assert len(rows)==1 and rows[0][1]['filename']=='appliedenergistics2-fabric-15.4.12.jar'
    value['artifacts'][rows[0][0]]=copy.deepcopy(artifact)
    assert len(versions)==1 and versions[0]['version']=='15.4.12'
    for i,v in enumerate(value['versions']):
        if v['project_id']=='XxWD5pD3':
            value['versions'][i]=dict(project_id='XxWD5pD3',version_id=None,version='15.4.11',audited_local_fixture=FIXTURE)
    return value


def verify_runtime_artifacts(root,artifacts):
    """Require the real staged publisher, not a filename-only declaration."""
    rows=[a for a in artifacts if a.get('family')=='upstream'and a['filename'].startswith('appliedenergistics2-fabric-')]
    assert len(rows)==1 and rows[0]['filename']==FILENAME
    row=rows[0];raw=(Path(root)/row['file']).read_bytes()
    assert hashlib.sha512(raw).hexdigest()==SHA512 and hashlib.sha256(raw).hexdigest()==row['sha256']
    if 'sha512'in row:assert row['sha512']==SHA512
    return dict(filename=FILENAME,sha512=SHA512,sha256=hashlib.sha256(raw).hexdigest())


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--manifest',type=Path,required=True);a=p.parse_args()
    manifest=a.manifest.resolve();assert manifest.is_relative_to(ROOT/'build/gameplay-qa/packaged-native')
    raw=manifest.read_bytes();original=json.loads(raw);artifact=native_artifact()
    receipt=manifest.with_name('legacy-publisher-variant.json');assert not receipt.exists()
    before=manifest.with_name('upstream-fixtures-before-legacy-variant.json');assert not before.exists();before.write_bytes(raw)
    value=substitute_manifest(original,artifact);manifest.write_text(json.dumps(value,indent=2)+'\n')
    receipt.write_text(json.dumps(dict(status='exact-unmodified-native-publisher-selected',
        before=dict(file=before.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(raw).hexdigest()),
        after=dict(file=manifest.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(manifest.read_bytes()).hexdigest()),
        original_fixture=dict(file=FIXTURE,sha256=hashlib.sha256((ROOT/FIXTURE).read_bytes()).hexdigest()),
        publisher=artifact,limits='Fixture prerequisite only. No native execution or passing interaction asserted.'),indent=2)+'\n')


if __name__=='__main__':main()
