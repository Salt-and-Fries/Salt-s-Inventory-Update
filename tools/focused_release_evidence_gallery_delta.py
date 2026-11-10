"""Freshly inspect the two exact compiled Gallery deltas inside the613-to726 edge."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess
from focused_release_evidence import checked,reference

def inspect(root,before,middle,after,folder,primary):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    files=list(map(Path,[before,middle,after]));digests=[hashlib.sha256(p.read_bytes()).hexdigest()for p in files];results=[]
    assert len(primary)==2
    for i,(ref,class_name)in enumerate(zip(primary,['GalleryInitializerDelta','GallerySelectionDelta'])):
        proof=json.loads(checked(root,ref).read_text());assert len(proof['records'])==4 and len(proof['negative_guards'])==4 and all(v['refused']for v in proof['negative_guards'])
        match=[v for v in proof['records']if v['before']['sha256']==digests[i]and v['after']['sha256']==digests[i+1]];assert len(match)==1
        checked(root,match[0]['log']);checked(root,proof['inspector']);checked(root,proof['binary'])
        source=checked(root,proof['source']);libraries=[checked(root,v)for v in proof['libraries']];assert len(libraries)==2
        work=folder/str(i);work.mkdir();copy=work/(class_name+'.java');shutil.copy2(source,copy);classes=work/'classes';classes.mkdir();cp=os.pathsep.join(map(str,libraries));jdk=Path('C:/Program Files/Java/jdk-26/bin')
        result=subprocess.run([str(jdk/'javac.exe'),'--release','17','-cp',cp,'-d',str(classes),str(copy)],capture_output=True,text=True);assert result.returncode==0,result.stderr
        binary=classes/(class_name+'.class');assert hashlib.sha256(binary.read_bytes()).hexdigest()==proof['binary']['sha256']
        result=subprocess.run([str(jdk/'java.exe'),'-cp',os.pathsep.join([str(classes),cp]),class_name,'audit',str(files[i]),str(files[i+1])],capture_output=True,text=True);assert result.returncode==0,result.stderr
        results.append(dict(primary=ref,source=proof['source'],binary_sha256=proof['binary']['sha256'],libraries=proof['libraries'],before_sha256=digests[i],after_sha256=digests[i+1],fresh_result=result.stdout.strip()))
    return dict(status='two-exact-Gallery-segments-freshly-recompiled-and-rechecked',before_sha256=digests[0],intermediate_sha256=digests[1],after_sha256=digests[2],segments=results,limits='Original613to718source binding plus718to726authenticated native selection only. Added bodies are source-pinned and native-qualified separately. No new execution, coverage or API allowance.')
