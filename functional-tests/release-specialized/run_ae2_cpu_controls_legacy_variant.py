"""Keep both genuine CPU jobs/control assertions while selecting native15.4.11."""
from pathlib import Path
import json,sys,traceback
import run_ae2_cpu_controls as base
from ae2_legacy_publisher_variant import FIXTURE,FILENAME,SHA512,native_artifact

ROOT=Path(__file__).resolve().parents[2]
ORIGINAL_PREPARE=base.prepare


def prepare(folder,mc):
    assert mc=='1.20.1';native_artifact()
    result=ORIGINAL_PREPARE(folder,mc);script=result['script'];text=script.read_text()
    needle="if($LASTEXITCODE -ne 0){throw 'AE2 fixture preparation failed'}"
    assert text.count(needle)==1
    helper=Path(__file__).with_name('ae2_legacy_publisher_variant.py')
    insertion="\n& python '"+str(helper).replace("'","''")+"' --manifest $manifest\nif($LASTEXITCODE -ne 0){throw 'Exact legacy AE2 publisher selection failed'}"
    script.write_text(text.replace(needle,needle+insertion,1))
    result['inputs'] += [helper,Path(__file__),ROOT/FIXTURE]
    return result


def verify_run(root,summary,expected):
    base.verify_run(root,summary,expected)
    upstream=summary['runtime_evidence']['dependency_evidence']['upstream']
    found=[v for v in upstream if v.get('sha512')==SHA512]
    assert len(found)==1,'Actual legacy AE2 publisher missing from native runtime evidence'
    profile=Path(root)/summary['results'][0]['profile'];receipt=profile/'legacy-publisher-variant.json'
    value=json.loads(receipt.read_text());assert value['status']=='exact-unmodified-native-publisher-selected'
    assert value['publisher']['filename']==FILENAME and value['publisher']['sha512']==SHA512
    for key in['before','after','original_fixture']:
        assert base.sha(Path(root)/value[key]['file'])==value[key]['sha256']
    from ae2_legacy_publisher_variant import substitute_manifest
    assert substitute_manifest(json.loads((Path(root)/value['before']['file']).read_text()),value['publisher'])==json.loads((Path(root)/value['after']['file']).read_text())
    assert value['original_fixture']['file']==FIXTURE
    assert summary['legacy_variant_receipt']==dict(file=receipt.relative_to(Path(root)).as_posix(),sha256=base.sha(receipt))
    return True


def main():
    target=sys.argv[sys.argv.index('--target')+1];assert target=='1.20.1/fabric'
    run=sys.argv[sys.argv.index('--run')+1]
    base.prepare=prepare
    base.main()
    if '--plan-only'in sys.argv:return
    path=ROOT/'output/release-readiness'/run/'summary.json';summary=json.loads(path.read_text())
    try:
        receipt=ROOT/summary['results'][0]['profile']/'legacy-publisher-variant.json'
        summary['legacy_variant_receipt']=dict(file=receipt.relative_to(ROOT).as_posix(),sha256=base.sha(receipt))
        verify_run(ROOT,summary,base.current_subjects(target))
        summary['publisher_variant']=dict(filename=FILENAME,sha512=SHA512,family='cpu-controls')
    except BaseException as error:
        summary.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
    finally:path.write_text(json.dumps(summary,indent=2)+'\n')


if __name__=='__main__':main()
