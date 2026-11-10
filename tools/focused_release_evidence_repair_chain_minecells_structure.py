"""Revalidate nine immutable source stages and eight exact compiled repair edges, including the native Mine Cells recipe ID."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,sys,zipfile
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked,reference,sha
from focused_release_evidence_core_log_transition import PRODUCTION,ROOT_BUILD
from focused_release_evidence_repair_chain_previous_structure import structure as prior_structure
PREPARATION=dict(file='output/release-readiness/repair-prefix-nine-stage-inputs-435-01/manifest.json',sha256='80f20f70406ee36c6898078eb12776b9eaa029b0b4091a60fcf4574ec588910c')

def structure(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve()
    assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    prior=prior_structure(root,folder/'prior-seven-edges')
    graph=json.loads(checked(root,PREPARATION).read_text())
    assert [v['stage']for v in graph['stages']]==['239','250','261','279','303','314','344','381','431']
    assert len(graph['edges'])==8
    original=json.loads(checked(root,graph['previous_preparation']).read_text())
    assert graph['stages'][:-1]==original['stages']and graph['edges'][:-1]==original['edges']
    stage=graph['stages'][-1];edge=graph['edges'][-1]
    assert (edge['before'],edge['after'],edge['inspector_mode'])==('381','431','minecells-id')
    snapshot=json.loads(checked(root,stage['source_snapshot']).read_text());files=snapshot['source_files']
    digest=lambda raw:hashlib.sha256(raw).hexdigest()
    tree=digest(json.dumps(files,sort_keys=True,separators=(',',':')).encode())
    assert tree==snapshot['source_tree_sha256']==stage['source_tree_sha256']and snapshot['review_source_archive']==stage['review_source_archive']
    with zipfile.ZipFile(checked(root,stage['review_source_archive']))as z:
        names=z.namelist();assert len(names)==len(set(names))
        assert set(names)==set(files)|{'CANDIDATE-SOURCE-IDENTITY.json'}
        assert json.loads(z.read('CANDIDATE-SOURCE-IDENTITY.json'))==dict(source_tree_sha256=tree,source_files=files)
        for name,value in files.items():assert digest(z.read(name))==value,name
    previous=graph['stages'][-2];oldsource=json.loads(checked(root,previous['source_snapshot']).read_text())
    assert {n:d for n,d in files.items()if n.startswith('functional-tests/')}=={n:d for n,d in oldsource['source_files'].items()if n.startswith('functional-tests/')}
    selected=lambda d:{n:v for n,v in d.items()if n.startswith(PRODUCTION)or n in ROOT_BUILD}
    old,new=selected(oldsource['source_files']),selected(files)
    changed={n for n in set(old)|set(new)if old.get(n)!=new.get(n)}
    expected={'compatibility/versions/1.21.1/fabric/src/main/java/com/salts_inventory_update/compat/minecells/MineCellsClient.java'}
    assert changed==expected=={v['file']for v in edge['production_sources']}
    for v in edge['production_sources']:assert old[v['file']]==v['before']and new[v['file']]==v['after']
    rows={(v['target'],v['family']):v for v in stage['artifacts']};before={(v['target'],v['family']):v for v in previous['artifacts']}
    candidate={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in snapshot['artifacts']}
    assert len(rows)==len(stage['artifacts'])==len(candidate)==36
    for key,row in rows.items():assert candidate[key]==(row['distribution'],row['sha256'])and row['preserved']['sha256']==row['sha256'];checked(root,row['preserved'])
    checked(root,edge['original_compiled_receipt']);tool=edge['tool'];source=checked(root,edge['reviewed_inspector_java'])
    assert sha(source)==tool['source_sha256']
    for name in ['java','javac','binary']:assert sha(Path(tool[name]))==tool[name+'_sha256']
    work=folder/'381-431';classes=work/'classes';classes.mkdir(parents=True)
    fresh=work/'CoreDiagnosticDelta.java';shutil.copy2(source,fresh);libraries=[]
    for library in tool['libraries']:
        p=Path(library['file']);assert sha(p)==library['sha256'];q=work/'libraries'/p.name;q.parent.mkdir(exist_ok=True);shutil.copy2(p,q);libraries.append(q)
    cp=os.pathsep.join(map(str,libraries));compile_log=work/'compile.log'
    with compile_log.open('x')as stream:subprocess.run([tool['javac'],'--release','17','-proc:none','-cp',cp,'-d',str(classes),str(fresh)],stdout=stream,stderr=subprocess.STDOUT,check=True)
    assert sha(classes/'CoreDiagnosticDelta.class')==tool['binary_sha256']
    checked_rows=[];classpath=os.pathsep.join(map(str,[classes,*libraries]))
    for key,left in before.items():
        right=rows[key];a=checked(root,left['preserved']);b=checked(root,right['preserved']);log=None
        if left['sha256']!=right['sha256']:
            assert key[0]in['1.21.1/fabric','1.21.1/neoforge']and key[1]=='compatibility'
            p=work/(key[0].replace('/','-')+'-'+key[1]+'.log')
            with p.open('x')as stream:subprocess.run([tool['java'],'-cp',classpath,'CoreDiagnosticDelta','minecells-id',str(a),str(b)],stdout=stream,stderr=subprocess.STDOUT,check=True)
            log=reference(root,p)
        else:assert a.read_bytes()==b.read_bytes()
        checked_rows.append(dict(target=key[0],family=key[1],before=left['preserved'],after=right['preserved'],scope='byte-identical'if log is None else'exact-original-reviewed-inspector',log=log))
    last=dict(before='381',after='431',mode='minecells-id',exact_production_sources=edge['production_sources'],original_compiled_receipt=edge['original_compiled_receipt'],
        fresh_inspector_source=reference(root,fresh),fresh_inspector_binary=reference(root,classes/'CoreDiagnosticDelta.class'),original_tool=tool,compile_log=reference(root,compile_log),rows=checked_rows)
    return dict(status='nine-source-archives-eight-fresh-structural-edges-verified-no-native-retention',preparation=PREPARATION,
        stages=prior['stages']+[dict(stage='431',source_snapshot=stage['source_snapshot'],source_archive=stage['review_source_archive'],source_file_count=len(files),canonical_functional_QA_count=970,artifacts=stage['artifacts'])],
        edges=prior['edges']+[last],limits=prior['limits']+' Additional edge381to431allows only the exact native withId invocation; no extra method body allowance.')
