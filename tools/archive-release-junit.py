"""Preserve current mapped JUnit evidence independently of mutable build directories."""
from pathlib import Path
import argparse,hashlib,json,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
def sha(raw):return hashlib.sha256(raw).hexdigest()

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True)
    p.add_argument('--build-log',type=Path,default=ROOT/'build/release-readiness/source-build-13.log')
    a=p.parse_args()
    output=ROOT/'output/release-readiness'/('junit-'+a.id);output.mkdir(exist_ok=False)
    log=a.build_log.resolve();log.relative_to(ROOT);assert 'BUILD SUCCESSFUL' in log.read_text()
    integrity=json.loads((ROOT/'output/release-readiness/candidate-integrity.json').read_text());assert integrity['status']=='passed'
    for row in integrity['artifacts']:assert sha((ROOT/row['file']).read_bytes())==row['sha256']
    rows=[]
    for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
        for project in [f'versions/{mc}/fabric',f'compatibility/versions/{mc}/fabric']:
            files=sorted((ROOT/project/'build/test-results/test').glob('TEST-*.xml'));assert files,project
            row=dict(project=project,tests=0,failures=0,skipped=0,errors=0,xml_files=[])
            for file in files:
                raw=file.read_bytes();suite=ET.fromstring(raw)
                for key in ['tests','failures','skipped','errors']:row[key]+=int(suite.attrib.get(key,0))
                target=output/'xml'/project/file.name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
                assert file.read_bytes()==raw
                row['xml_files'].append(dict(file=str(target.relative_to(ROOT)),sha256=sha(raw),origin=str(file.relative_to(ROOT)),suite=suite.attrib['name']))
            rows.append(row)
    report=dict(status='passed' if all(r['tests'] and not any(r[k] for k in ['failures','errors','skipped']) for r in rows) else 'failed',
        scope='Current six-mapping Core/Compatibility JUnit sets accepted by the named successful source build. These are unit/contract checks, not live gameplay or separate loader/hardware executions.',
        source_build=dict(file=str(log.relative_to(ROOT)),sha256=sha(log.read_bytes())),
        artifacts=integrity['artifacts'],tests=sum(r['tests'] for r in rows),projects=rows)
    (output/'summary.json').write_text(json.dumps(report,indent=2)+'\n');assert report['status']=='passed'
    print(report['tests'],'mapped unit/contract checks archived without re-running tests;',output)

if __name__=='__main__':main()
