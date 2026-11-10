"""Hash and recheck retained feature inputs; never label them fresh gameplay."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def ref(path):return dict(file=str(path.relative_to(ROOT)),sha256=sha(path))
def read(path):return json.loads(path.read_text())


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True);a=p.parse_args()
    assert a.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in a.id)
    out=ROOT/'output/release-readiness'/('retained-feature-inputs-'+a.id+'.json');assert not out.exists()
    spec=importlib.util.spec_from_file_location('mi_geometry_recheck',ROOT/'tools/check-modern-industrialization-windows.py')
    geometry=importlib.util.module_from_spec(spec);spec.loader.exec_module(geometry)
    mi_verification=ROOT/'output/modern-industrialization-implementation/verification.json'
    mi_compare=ROOT/'output/release-readiness/retained-mi-baseline-build13-01.json'
    powah_compare=ROOT/'output/release-readiness/retained-powah-baseline-build13-01.json'
    rows=[]
    for target in read(mi_verification)['targets']:
        identity=target['minecraft']+'/'+target['loader']
        binary=next(v for v in read(mi_compare)['targets']if v['target']==identity)
        assert not binary['comparison']['changed']
        assert sha(ROOT/binary['current_archive'])==binary['current_sha256']
        profile=Path(target['final_profile']).name
        cases=[]
        assert len(target['cases'])==110 and all(v['status']=='pass'for v in target['cases'])
        for case in target['cases']:
            if 'observation'in case:state_file=ROOT/case['observation']
            else:
                slug=case['id'].removeprefix('modern-industrialization-')
                state_file=ROOT/'build/modern-industrialization-implementation'/profile/(slug+'.json')
            state=read(state_file)
            window,cells=geometry.check(state)
            assert cells==case['cells']and window['menuId']==case['menu'],case['id']
            assert window['bounds']==case['bounds'],case['id']
            case_record=dict(id=case['id'],geometry_rechecked=True,cells=cells,observation=ref(state_file))
            if 'capture'in case:
                picture=ROOT/case['capture']
                with Image.open(picture)as image:image.verify()
                case_record['capture']=ref(picture)
            cases.append(case_record)
        action_dir=ROOT/'build/modern-industrialization-implementation'/profile
        action_files=[v for v in sorted(action_dir.glob('*-server.json'))if v.is_file()]
        action_files += [v for v in sorted(action_dir.glob('*-client.json'))if v.is_file()]
        assert len(action_files)>=8
        for v in action_files:read(v)
        rows.append(dict(family='modern-industrialization',target=identity,binary_comparison=ref(mi_compare),
            views=cases,retained_action_states=[ref(v)for v in action_files],
            limits='Rerun of pure geometry assertions against preserved native observations, plus hashes of retained action states. No new live machine processing was performed.'))
    for binary in read(powah_compare)['targets']:
        assert not binary['comparison']['changed']
        assert sha(ROOT/binary['current_archive'])==binary['current_sha256']
        journal=ROOT/binary['baseline_journal']['file'];assert sha(journal)==binary['baseline_journal']['sha256']
        data=read(journal);assert len(data['checks'])==125 and all(v['passed']for v in data['checks'])
        screenshots=[]
        for capture in data['captures']:
            picture=ROOT/capture['screenshot']
            with Image.open(picture)as image:image.verify()
            screenshots.append(dict(id=capture['id'],capture=ref(picture)))
        assert screenshots
        rows.append(dict(family='powah',target=binary['target'],binary_comparison=ref(powah_compare),
            journal=ref(journal),checks=len(data['checks']),screenshots=screenshots,
            limits='Exact retained passing focused journal and capture hashes. No new live Powah actions were performed by this audit.'))
    result=dict(status='retained-inputs-rechecked-not-fresh-gameplay',generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        mi_verification=ref(mi_verification),geometry_checker=ref(ROOT/'tools/check-modern-industrialization-windows.py'),targets=rows,
        acceptance='These hashes and unchanged Compatibility classes support an explicit per-feature equivalence review only. Current matching publisher entry/input and changed shared Core paths remain separate requirements.')
    out.write_text(json.dumps(result,indent=2)+'\n')
    print('Retained input recheck:',[(v['family'],v['target'],len(v.get('views',[])),v.get('checks'))for v in rows])


if __name__=='__main__':main()
