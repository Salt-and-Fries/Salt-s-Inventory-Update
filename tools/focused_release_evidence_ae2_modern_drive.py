"""Exact modern Drive QA derivation; no production layout change or weak pass."""
from pathlib import Path
import json,re
from focused_release_evidence import checked
from full_suite_provenance import sources,digest_inputs,sha,verify_bundle
import focused_release_evidence_ae2_drive_gutter as native

KIND='modern-AE2-Drive-native-safe-gutter-v1'
MANIFEST=dict(file='output/release-readiness/modern-drive-gutter-223-01/manifest.json',sha256='a10730b29247b17f0611acd9af4c7827ad977608d17eb45b43ca353b2a48ea7a')
DRIVER='build/release-readiness/run-full-with-modern-drive-224-01.py'

def expected_inputs(root,case):
    root=Path(root).resolve();m=json.loads(checked(root,MANIFEST).read_text())
    assert m['schema']==KIND and m['status']=='explicit-fixture-correction-prepared-not-qualified'
    assert case['qa_derivation']==dict(kind=KIND,base_script=native.BASE,manifest=MANIFEST)
    assert case['minecraft']in {'1.21.1','26.1.2','26.2','26.3'}and case['loader']=='neoforge'and case['name']=='ae2-atlas'
    assert case['key']==case['minecraft']+':neoforge:ae2-atlas'
    assert case['script']==m['wrapper']['file']and case['kinds']==['core','compatibility']and case['gradle']is False
    assert case['args']==['-Minecraft',case['minecraft'],'-Loader','neoforge','-RunId','{runid}']
    assert [v['minecraft']for v in m['sources']]==['1.21.1','26.3']
    for row in m['sources']:
        text=checked(root,row['original_source']).read_text()
        for change in m['substitutions']:
            assert text.count(change['before'])==1;text=text.replace(change['before'],change['after'])
        assert checked(root,row['derived_source']).read_text()==text
    for key in ['original_failure','diagnostic','actual_bounds','original_wrapper']:checked(root,m[key])
    expected=sources(root,dict(case,script=native.BASE))
    assert len(expected)==1144 and digest_inputs(expected)==m['original_qa_inputs_sha256']=='57c834108359e803dece14e906db7add54bbc09fdd1ae2582acde106c9d04698'
    for ref in [*m['derived_files'],m['preparer'],MANIFEST]:checked(root,ref);expected[ref['file']]=ref['sha256']
    for name in [native.DRIVER,native.OBSERVER,DRIVER,'tools/focused_release_evidence_ae2_modern_drive.py']:expected[name]=sha(root/name)
    return dict(sorted(expected.items()))

def verify_attempt(root,entry,case,original_verify):
    root=Path(root).resolve();assert entry['qa_inputs']==expected_inputs(root,case)
    original_verify(root,entry,case)
    runtime=entry['runtime_evidence'];folder=root/'build/gameplay-qa/packaged-native'/(case['minecraft']+'-'+case['loader']+'-'+entry['run_id'])
    assert runtime['report']['file']==(folder/'results.json').relative_to(root).as_posix()
    verify_bundle(root,runtime['atlas_archive'],runtime['atlas_files'])
    report=json.loads(checked(root,runtime['report']).read_text());actual=Path(report['runDirectory']).resolve()
    assert runtime['FML_group']==native.atlas_group(root,folder,actual,runtime['runtime_copies'],runtime['loaded_code_sources'])
    completed=(folder/'atlas/completed.txt').read_text();match=re.fullmatch(r'Registered containers: (\d+)\s*',completed)
    assert match and int(match[1])>20,'A passive/focused Drive probe cannot qualify the complete atlas'
    controls=list((folder/'atlas').glob('*-me-drive.controls.json'));assert len(controls)==1
    geometry=controls[0].with_name(controls[0].name.replace('.controls.json','.geometry.json'))
    lines=(folder/'atlas/manifest.csv').read_text().splitlines();header=lines[0].split(',')
    rows=[dict(zip(header,line.split(',')))for line in lines[1:]if ',me-drive,'in line];assert len(rows)==1
    native.verify_drive_geometry(json.loads(controls[0].read_text()),json.loads(geometry.read_text()),[int(rows[0][k])for k in ['x','y','width','height']])
    return True
