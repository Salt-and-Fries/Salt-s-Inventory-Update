"""Revalidate the original eight source stages and seven exact compiled repair edges; grants no native coverage."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,sys,zipfile
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked,reference,sha
from focused_release_evidence_core_log_transition import PRODUCTION,ROOT_BUILD
PREPARATION=dict(file='output/release-readiness/repair-prefix-input-preparation-410-01/manifest.json',sha256='48388f5b067d7f559e422c9018dc9923e2bc45c30994050a4b7f15cd6cb6ae16')
EDGES=(('239','250','ae2-side'),('250','261','native-rename'),('261','279','burst'),('279','303','publication'),('303','314','tracking'),('314','344','address'),('344','381','stock'))

def structure(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve()
    assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    graph=json.loads(checked(root,PREPARATION).read_text())
    assert [v['stage']for v in graph['stages']]==['239','250','261','279','303','314','344','381']
    assert [(v['before'],v['after'],v['inspector_mode'])for v in graph['edges']]==list(EDGES)
    snapshots={};artifacts={};qa=None;stages=[];edges=[]
    digest=lambda raw:hashlib.sha256(raw).hexdigest()
    for stage in graph['stages']:
        snapshot=json.loads(checked(root,stage['source_snapshot']).read_text());files=snapshot['source_files']
        tree=digest(json.dumps(files,sort_keys=True,separators=(',',':')).encode())
        assert tree==snapshot['source_tree_sha256']==stage['source_tree_sha256']
        assert snapshot['review_source_archive']==stage['review_source_archive']
        with zipfile.ZipFile(checked(root,stage['review_source_archive']))as z:
            names=z.namelist();assert len(names)==len(set(names))
            assert set(names)==set(files)|{'CANDIDATE-SOURCE-IDENTITY.json'}
            assert json.loads(z.read('CANDIDATE-SOURCE-IDENTITY.json'))==dict(source_tree_sha256=tree,source_files=files)
            for name,value in files.items():assert digest(z.read(name))==value,(stage['stage'],name)
        actual_qa={n:d for n,d in files.items()if n.startswith('functional-tests/')}
        if qa is None:qa=actual_qa
        assert actual_qa==qa,'Canonical functional QA changed'
        expected={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in snapshot['artifacts']}
        rows={(v['target'],v['family']):v for v in stage['artifacts']}
        assert len(expected)==len(snapshot['artifacts'])==len(rows)==len(stage['artifacts'])==36
        for key,row in rows.items():
            assert expected[key]==(row['distribution'],row['sha256'])
            assert row['preserved']['sha256']==row['sha256'];checked(root,row['preserved'])
        snapshots[stage['stage']]=snapshot;artifacts[stage['stage']]=rows
        stages.append(dict(stage=stage['stage'],source_snapshot=stage['source_snapshot'],source_archive=stage['review_source_archive'],source_file_count=len(files),canonical_functional_QA_count=len(qa),artifacts=stage['artifacts']))
    for edge in graph['edges']:
        before,after=edge['before'],edge['after'];tool=edge['tool']
        selected=lambda d:{n:v for n,v in d.items()if n.startswith(PRODUCTION)or n in ROOT_BUILD}
        old=selected(snapshots[before]['source_files']);new=selected(snapshots[after]['source_files'])
        changed={n for n in set(old)|set(new)if old.get(n)!=new.get(n)}
        assert changed=={v['file']for v in edge['production_sources']}
        for row in edge['production_sources']:assert old.get(row['file'])==row['before']and new.get(row['file'])==row['after']
        checked(root,edge['original_compiled_receipt'])
        source=checked(root,edge['reviewed_inspector_java']);assert sha(source)==tool['source_sha256']
        for name in ['java','javac','binary']:assert sha(Path(tool[name]))==tool[name+'_sha256']
        work=folder/(before+'-'+after);classes=work/'classes';classes.mkdir(parents=True)
        fresh_source=work/'CoreDiagnosticDelta.java';shutil.copy2(source,fresh_source)
        libraries=[]
        for library in tool['libraries']:
            original=Path(library['file']);assert sha(original)==library['sha256']
            copy=work/'libraries'/original.name;copy.parent.mkdir(exist_ok=True);shutil.copy2(original,copy)
            assert sha(copy)==library['sha256'];libraries.append(copy)
        cp=os.pathsep.join(map(str,libraries));compile_log=work/'compile.log'
        with compile_log.open('x')as stream:subprocess.run([tool['javac'],'--release','17','-proc:none','-cp',cp,'-d',str(classes),str(fresh_source)],stdout=stream,stderr=subprocess.STDOUT,check=True)
        assert sha(classes/'CoreDiagnosticDelta.class')==tool['binary_sha256'],'Fresh inspector binary differs'
        rows=[];classpath=os.pathsep.join(map(str,[classes,*libraries]))
        for key,left in artifacts[before].items():
            right=artifacts[after][key];a=checked(root,left['preserved']);b=checked(root,right['preserved']);log=None
            if left['sha256']!=right['sha256']:
                target,family=key;path=work/(target.replace('/','-')+'-'+family+'.log')
                args=[tool['java'],'-cp',classpath,'CoreDiagnosticDelta',edge['inspector_mode'],str(a),str(b)]
                if edge['inspector_mode']=='native-rename':args.append('compat'if family=='compatibility'else family)
                with path.open('x')as stream:subprocess.run(args,stdout=stream,stderr=subprocess.STDOUT,check=True)
                log=reference(root,path)
            else:assert a.read_bytes()==b.read_bytes()
            rows.append(dict(target=key[0],family=key[1],before=left['preserved'],after=right['preserved'],scope='byte-identical'if log is None else'exact-original-reviewed-inspector',log=log))
        edges.append(dict(before=before,after=after,mode=edge['inspector_mode'],exact_production_sources=edge['production_sources'],
            original_compiled_receipt=edge['original_compiled_receipt'],fresh_inspector_source=reference(root,fresh_source),fresh_inspector_binary=reference(root,classes/'CoreDiagnosticDelta.class'),
            original_tool=tool,compile_log=reference(root,compile_log),rows=rows))
    return dict(status='sources-and-seven-fresh-structural-edges-verified-no-native-retention',preparation=PREPARATION,stages=stages,edges=edges,
        limits='Source archives and artifact deltas only. Original approved new-method allowances remain identified as such; this does not independently prove their method bodies or assign native coverage. Structural intermediate303 is not an accepted native subject.')
