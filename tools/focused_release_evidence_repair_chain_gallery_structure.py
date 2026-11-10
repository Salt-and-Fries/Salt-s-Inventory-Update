"""Revalidate the prior ten edges and the two exact native-qualified Gallery segments."""
from pathlib import Path
import hashlib,json,shutil,zipfile
from focused_release_evidence import checked,reference
from focused_release_evidence_core_log_transition import PRODUCTION,ROOT_BUILD
from focused_release_evidence_repair_chain_analyzer_structure import structure as prior_structure
from focused_release_evidence_gallery_delta import inspect
PREPARATION={'file': 'output/release-readiness/repair-prefix-twelve-stage-inputs-739-01/manifest.json', 'sha256': 'e9f4f68bf1e64e17fd821a86377003660d871b58882f18e338d19534e9282fc1'}

def snapshot(root,stage):
    source=json.loads(checked(root,stage['source_snapshot']).read_text());files=source['source_files'];digest=lambda raw:hashlib.sha256(raw).hexdigest()
    tree=digest(json.dumps(files,sort_keys=True,separators=(',',':')).encode());assert tree==source['source_tree_sha256']==stage['source_tree_sha256']and source['review_source_archive']==stage['review_source_archive']
    with zipfile.ZipFile(checked(root,stage['review_source_archive']))as archive:
        names=archive.namelist();assert len(names)==len(set(names))and set(names)==set(files)|{'CANDIDATE-SOURCE-IDENTITY.json'}
        assert json.loads(archive.read('CANDIDATE-SOURCE-IDENTITY.json'))==dict(source_tree_sha256=tree,source_files=files)
        for name,value in files.items():assert digest(archive.read(name))==value,name
    return source

def structure(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    prior=prior_structure(root,folder/'prior-ten-edges');graph=json.loads(checked(root,PREPARATION).read_text());previous=json.loads(checked(root,graph['previous_preparation']).read_text())
    assert len(graph['stages'])==12 and len(graph['edges'])==11 and graph['stages'][:-1]==previous['stages']and graph['edges'][:-1]==previous['edges']
    stage,edge=graph['stages'][-1],graph['edges'][-1];assert stage['stage']=='726'and(edge['before'],edge['after'],edge['inspector_mode'])==('613','726','gallery-source-selection')
    source=snapshot(root,stage);old=json.loads(checked(root,graph['stages'][-2]['source_snapshot']).read_text());files=source['source_files']
    assert {n:v for n,v in files.items()if n.startswith('functional-tests/')}=={n:v for n,v in old['source_files'].items()if n.startswith('functional-tests/')}
    selected=lambda d:{n:v for n,v in d.items()if n.startswith(PRODUCTION)or n in ROOT_BUILD};left,right=selected(old['source_files']),selected(files)
    changed={n for n in set(left)|set(right)if left.get(n)!=right.get(n)};expected=set()
    for mc in ['1.20.1','1.21.1']:
        base='compatibility/versions/'+mc+'/'
        expected.add(base+'common/src/main/resources/salts_inventory_compat.reviewed-native.mixins.json')
        expected.update(base+'fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/'+n for n in ['GalleryItemSources.java','GallerySelectionActions.java','ReviewedNativeWindows.java','mixin/GallerySourceMixin.java','mixin/GallerySelectionTransportMixin.java'])
    assert changed==expected=={v['file']for v in edge['production_sources']}and len(changed)==12
    for row in edge['production_sources']:assert left.get(row['file'])==row['before']and right[row['file']]==row['after']
    middle_source=json.loads(checked(root,edge['intermediate_source']).read_text());middle_stage=dict(source_snapshot=edge['intermediate_source'],source_tree_sha256=middle_source['source_tree_sha256'],review_source_archive=middle_source['review_source_archive']);snapshot(root,middle_stage)
    middle_artifacts={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in middle_source['artifacts']}
    for receipt in [edge['original_compiled_receipt'],edge['source_binding_receipt'],edge['native_qualification']]:checked(root,receipt)
    inspector=checked(root,edge['reviewed_inspector_python']);assert inspector.read_bytes()==Path(__file__).with_name('focused_release_evidence_gallery_delta.py').read_bytes()
    work=folder/'613-726';work.mkdir();fresh=work/'gallery_delta.py';shutil.copy2(inspector,fresh)
    before={(r['target'],r['family']):r for r in graph['stages'][-2]['artifacts']};after={(r['target'],r['family']):r for r in stage['artifacts']};middle={(r['target'],r['family']):r for r in edge['intermediate_artifacts']}
    candidate={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in source['artifacts']};assert len(before)==len(after)==len(middle)==len(candidate)==36
    pairs=[]
    for key,a in before.items():
        b,c=middle[key],after[key];assert candidate[key]==(c['distribution'],c['sha256'])and middle_artifacts[key]==(b['distribution'],b['sha256'])
        first,second,last=[checked(root,v['preserved'])for v in [a,b,c]];assert all(v['preserved']['sha256']==v['sha256']for v in [a,b,c]);log=None
        if a['sha256']==c['sha256']:assert first.read_bytes()==second.read_bytes()==last.read_bytes()
        else:
            assert key[0]in ['1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge']and key[1]=='compatibility'
            result=inspect(root,first,second,last,work/key[0].replace('/','-'),edge['primary_inspections']);p=work/(key[0].replace('/','-')+'.json');p.write_text(json.dumps(result,indent=2,sort_keys=True)+'\n');log=reference(root,p)
        pairs.append(dict(target=key[0],family=key[1],before=a['preserved'],intermediate=b['preserved'],after=c['preserved'],scope='byte-identical'if log is None else'exact-original-reviewed-inspector',log=log))
    last=dict(before='613',after='726',mode='gallery-source-selection',exact_production_sources=edge['production_sources'],original_compiled_receipt=edge['original_compiled_receipt'],fresh_inspector_source=reference(root,fresh),original_tool=dict(language='python-with-two-source-pinned-Java-inspectors',primary_inspections=edge['primary_inspections'],source_binding_receipt=edge['source_binding_receipt'],intermediate_source=edge['intermediate_source']),rows=pairs)
    return dict(status='twelve-source-archives-eleven-fresh-structural-edges-verified-no-native-retention',preparation=PREPARATION,stages=prior['stages']+[dict(stage='726',source_snapshot=stage['source_snapshot'],source_archive=stage['review_source_archive'],source_file_count=len(files),canonical_functional_QA_count=sum(n.startswith('functional-tests/')for n in files),artifacts=stage['artifacts'])],edges=prior['edges']+[last],limits=prior['limits']+' Edge613to726adds only the source-pinned Gallery ownership and authenticated-selection path through two freshly compiled exact inspectors.32complete archives and all Core/Controlify unchanged. Intermediate718is not native acceptance; both actual original Gallery target workflows qualify separately.')
