"""Prepare isolated, original-publisher native-screen checks with shipping SIU."""
from pathlib import Path
import hashlib, importlib.util, io, json, sys, zipfile

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
OUT=ROOT/'output/native-screen-preservation'
WORK=ROOT/'build/native-screen-preservation'
OUT.mkdir(exist_ok=True);WORK.mkdir(exist_ok=True)

def module(name,path):
    s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m

def metadata(data):
    records=[]
    with zipfile.ZipFile(io.BytesIO(data))as z:
        if 'fabric.mod.json'not in z.namelist():return records
        d=json.loads(z.read('fabric.mod.json'),strict=False)
        if d['id']=='owo-sentinel':return records
        records.append(d)
        for nested in d.get('jars',[]):records+=metadata(z.read(nested['file']))
    return records

def main():
    side=sys.argv[1]if len(sys.argv)>1 else'fabric'
    if side=='fabric':
        prep=module('native_fabric',ROOT/'tools/prepare-production-fabric-audit.py')
        sources=[]
        for run in ['1.20.1-fabric-core-prod-own-app03','1.20.1-fabric-core-prod-ag02']:
            sources+=prep.read(ROOT/'build/remaining-ui-audit/gameplay'/run/'staged-artifacts.json')['artifacts']
        candidates={}
        for row in sources:
            if row['family']!='upstream':continue
            path=Path(row['file']);records=metadata(path.read_bytes())
            for r in records:
                for key in [r['id'],*r.get('provides',[])]:
                    if key not in candidates or records[0]['id']==key:candidates[key]=(path,records)
        selected={};pending=['dungeonz','sortilege','things','fwaystones']
        present={'minecraft','java','fabricloader','fabric','fabric-api'}
        while pending:
            key=pending.pop()
            if key in present:continue
            path,records=candidates[key];selected[path]=records
            present.update(key for r in records for key in [r['id'],*r.get('provides',[])])
            pending.extend(k for r in records for k in r.get('depends',{})if k not in present and not k.startswith('fabric-'))
        artifacts=[{'path':str(p.relative_to(ROOT)),'filename':p.name,'sha512':hashlib.sha512(p.read_bytes()).hexdigest(),'coordinate':'pinned-original:'+records[0]['id'],'mod_ids':[r['id']for r in records]}for p,records in selected.items()]
        # Retained cache paths can be digest filenames; use publisher filenames.
        files=prep.read(ROOT/'build/ui-modpack-audit-20261025/files.json')
        names={r['hashes']['sha512']:Path(r['path']).name for r in files.values()if'sha512'in r.get('hashes',{})}
        for r in artifacts:r['filename']=names.get(r['sha512'],r['filename'])
        fixture={'minecraft':'1.20.1','loader':'fabric','pack_id':'EGs3lC8D','artifacts':artifacts}
        path=WORK/'fabric-fixture.json';path.write_text(json.dumps(fixture,indent=2))
        old=prep.qa_source
        def qa(s):
            text=old(s)
            return text.replace('return out;\n    }\n    static void auditWidgets','if(mc.screen!=null&&windows.isEmpty()){var widgets=new ArrayList<Object>();auditWidgets(mc.screen,widgets,0);out.put("widgets",widgets);}\n        return out;\n    }\n    static void auditWidgets',1)
        prep.qa_source=qa
        sys.argv=[sys.argv[0],'--fixture',str(path),'--side','integrated','--run','native-preservation-02','--core-jar',str(ROOT/'dist/core/salts_inventory_update-1.20.1-Fabric-0.1.6.jar'),'--compat-jar',str(ROOT/'dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar')]
        prep.main()
    elif side in ('forge','neoforge'):
        files=json.loads((ROOT/'build/ui-modpack-audit-20261025/files.json').read_text());wanted=['elevatorid']if side=='forge'else['ArmorStatues-','PuzzlesLib-v21.1.51']
        artifacts=[]
        for sha,r in files.items():
            if any(s.lower()in r['path'].lower()for s in wanted):
                p=ROOT/'build/ui-modpack-audit-20261025/jars'/(sha+'.jar')
                assert hashlib.sha512(p.read_bytes()).hexdigest()==r['hashes']['sha512']
                artifacts.append({'path':str(p.relative_to(ROOT)),'filename':Path(r['path']).name,'sha512':r['hashes']['sha512'],'coordinate':'pinned-original:'+sha})
        assert len(artifacts)==(1 if side=='forge'else 2),artifacts
        mc='1.20.1'if side=='forge'else'1.21.1';path=WORK/(side+'-fixture.json');path.write_text(json.dumps({'minecraft':mc,'loader':side,'artifacts':artifacts},indent=2))
        if side=='forge':
            prep=module('native_forge',ROOT/'tools/prepare-rollout-production-forge.py')
            sys.argv=[sys.argv[0],'--fixture',str(path),'--side','siu','--run','native-preservation-01','--core-jar',str(ROOT/'dist/core/salts_inventory_update-1.20.1-Forge-0.1.6.jar'),'--compat-jar',str(ROOT/'dist/compatibility/salts_inventory_compat-1.20.1-Forge-0.1.3.jar')]
            prep.main()
        else:
            prep=module('native_neo',ROOT/'tools/prepare-rollout-production-modern.py');prep.MC='1.21.1'
            _,text=prep.qa_source('siu');text=text.replace('net.minecraftforge.fml.ModList','net.neoforged.fml.ModList')
            folder=WORK/'neo-source/com/siu_reference';folder.mkdir(parents=True,exist_ok=True)
            (folder/'NativeUiReference.java').write_text(text)
            (folder/'RuntimeNames.java').write_text('package com.siu_reference;import java.lang.reflect.Field;import java.util.Set;final class RuntimeNames{static String className(String n){return n;}static Set<String> names(Class<?> t,String n,String k){return Set.of(n);}static Field field(Class<?> t,String n)throws NoSuchFieldException{return t.getDeclaredField(n);}}')
            script=(ROOT/'functional-tests/remaining-ui-audit/native/qa.init.gradle').read_text()
            script=script.replace("if(integrated && !(minecraft=='1.20.1' && loader=='forge'))throw new GradleException('Reviewed integrated launcher is Forge 1.20.1 only')",'')
            script=script.replace("integrated?'build/project-everything-56-implementation/forge-observer':minecraft=='1.20.1'?'functional-tests/remaining-ui-audit/native/java':'functional-tests/remaining-ui-audit/native/java-1.21.1'","'build/native-screen-preservation/neo-source'")
            (WORK/'neo.init.gradle').write_text(script)
            print('Prepared NeoForge fixture/source')

if __name__=='__main__':main()
