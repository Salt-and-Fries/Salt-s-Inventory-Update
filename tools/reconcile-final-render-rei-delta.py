"""Strict final correction delta: Core render/book plus exact REI policies.

Core's original inspector remains unchanged. Compatibility may add exactly
one public REI registration method and one independently compiled policy;
every other compiled member and resource remains equivalent. This grants
no native gameplay or evidence retention without the separate real checks.
"""
from pathlib import Path
import datetime, importlib.util, json, subprocess, sys, uuid

ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked, reference
spec=importlib.util.spec_from_file_location('unchanged_strict_Core_final_inspector',ROOT/'tools/reconcile-core-final-delta.py')
core=importlib.util.module_from_spec(spec);spec.loader.exec_module(core)
sha=core.sha

ADDITION=r'''
    static final String POLICY="com/salts_inventory_update/compat/rei/WorkspaceReiOverlayPolicy";
    static byte[] expectedPlugin(byte[] original) {
        ClassNode n=read(original);
        require(n.methods.stream().noneMatch(m->m.name.equals("registerScreens")),"Original plugin already declares screen registration");
        MethodNode method=new MethodNode(ACC_PUBLIC,"registerScreens","(Lme/shedaniel/rei/api/client/registry/screen/ScreenRegistry;)V",null,null);
        method.instructions.add(new VarInsnNode(ALOAD,1));
        method.instructions.add(new TypeInsnNode(Opcodes.NEW,POLICY));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL,POLICY,"<init>","()V",false));
        method.instructions.add(new MethodInsnNode(INVOKEINTERFACE,"me/shedaniel/rei/api/client/registry/screen/ScreenRegistry","registerDecider","(Lme/shedaniel/rei/api/client/registry/screen/OverlayDecider;)V",true));
        method.instructions.add(new InsnNode(RETURN));method.maxStack=3;method.maxLocals=2;
        n.methods.add(method);return bytes(n);
    }
    static void auditRei(Path before,Path after,Path expectedOld,Path expectedNew,Path expectedPolicy) throws Exception {
        byte[] oldPlugin=Files.readAllBytes(expectedOld),newPlugin=Files.readAllBytes(expectedNew),policy=Files.readAllBytes(expectedPolicy);
        ClassNode oldNode=read(oldPlugin),policyNode=read(policy);
        require(Set.of("com/salts_inventory_update/compat/rei/SaltsReiClientPlugin","com/salts_inventory_update/compat/rei/SaltsReiForgeClientPlugin").contains(oldNode.name),"Unexpected native plugin identity");
        require(policyNode.name.equals(POLICY),"Unexpected policy identity");
        require(Arrays.equals(expectedPlugin(oldPlugin),bytes(read(newPlugin))),"Freshly compiled proposal changes more than the one exact screen-registration method");
        String plugin=oldNode.name+".class",addition=POLICY+".class";
        try(ZipFile a=new ZipFile(before.toFile());ZipFile b=new ZipFile(after.toFile())) {
            var old=entries(a);var now=entries(b);var expected=new TreeSet<String>(old.keySet());
            require(expected.add(addition),"Original archive already contains the proposed policy");
            require(expected.equals(now.keySet()),"Compatibility archive member set changed beyond the one policy class");
            require(old.containsKey(plugin),"Original REI entrypoint is missing");
            require(Arrays.equals(bytes(read(entry(a,old.get(plugin)))),bytes(read(oldPlugin))),"Archived original plugin differs from the independently compiled original source");
            require(Arrays.equals(bytes(read(entry(b,now.get(plugin)))),bytes(read(newPlugin))),"Final plugin differs from the one exact proposed registration method");
            require(Arrays.equals(bytes(read(entry(b,now.get(addition)))),bytes(policyNode)),"Final policy differs from independently compiled pinned-API dispatch");
            int unchanged=0,debug=0;
            for(String name:old.keySet()) {
                if(name.equals(plugin))continue;
                byte[] x=entry(a,old.get(name)),y=entry(b,now.get(name));
                if(name.endsWith(".class")) {
                    require(Arrays.equals(bytes(read(x)),bytes(read(y))),"Unrelated Compatibility class/member/instruction changed: "+name);
                    if(!Arrays.equals(x,y))debug++;
                }else require(Arrays.equals(x,y),"Unrelated Compatibility resource changed: "+name);
                unchanged++;
            }
            System.out.println("EXACT_REI_WORKSPACE_POLICY_AND_ONE_REGISTRATION_VERIFIED");
            System.out.println("unchanged_other_members="+unchanged);System.out.println("debug_only_classes="+debug);
        }
    }
'''

def java_source():
    text=core.java_source();anchor='    public static void main(String[] args) throws Exception {'
    assert text.count(anchor)==1
    return text.replace(anchor,ADDITION+anchor+'''
        if(args.length==7&&args[0].equals("audit-jar")&&args[3].equals("rei-policy")) {
            auditRei(Path.of(args[1]),Path.of(args[2]),Path.of(args[4]),Path.of(args[5]),Path.of(args[6]));return;
        }
''')

MARKER_AUDIT=r'''
    static final String MARKER="com/salts_inventory_update/api/client/recipebrowser/DesktopRecipeBrowserScreen";
    static final String RADIAL="com/salts_inventory_update/controlify/RadialInventoryScreen.class";
    static byte[] expectedMarker(byte[] actual) {
        ClassNode original=read(actual),n=new ClassNode();n.version=original.version;
        n.access=ACC_PUBLIC|ACC_INTERFACE|ACC_ABSTRACT;n.name=MARKER;n.superName="java/lang/Object";
        return bytes(n);
    }
    static byte[] expectedControllerMarker(byte[] original) {
        ClassNode n=read(original);require(!n.interfaces.contains(MARKER),"Original already declares marker");
        n.interfaces.add(MARKER);
        if(n.signature!=null)n.signature+="L"+MARKER+";";
        return bytes(n);
    }
    static void auditPublicMarker(Path before,Path after,String scope) throws Exception {
        boolean core=scope.startsWith("core-"),book=scope.contains("book-1.21.11");
        try(ZipFile a=new ZipFile(before.toFile());ZipFile b=new ZipFile(after.toFile())) {
            var old=entries(a);var now=entries(b);var allowed=new TreeSet<String>(old.keySet());
            if(core)require(allowed.add(MARKER+".class"),"Marker already in original Core");
            require(allowed.equals(now.keySet()),"Unreviewed marker archive-member change");
            if(core)require(Arrays.equals(expectedMarker(entry(b,now.get(MARKER+".class"))),bytes(read(entry(b,now.get(MARKER+".class"))))),"Public marker has noncontract members");
            for(String name:old.keySet()) {
                byte[] x=entry(a,old.get(name)),y=entry(b,now.get(name));
                if(core&&name.equals(MAIN))require(Arrays.equals(book?expectedBook(x):expected(x),bytes(read(y))),"Unreviewed Core render change");
                else if(!core&&name.equals(RADIAL))require(Arrays.equals(expectedControllerMarker(x),bytes(read(y))),"Controller changes beyond one public marker declaration");
                else if(name.endsWith(".class"))require(Arrays.equals(bytes(read(x)),bytes(read(y))),"Unrelated marker class changed: "+name);
                else require(Arrays.equals(x,y),"Unrelated marker resource changed: "+name);
            }
            require(old.containsKey(core?MAIN:RADIAL),"Marker owner missing");
            System.out.println("EXACT_PUBLIC_MARKER_AND_ORIGINAL_METHODS_VERIFIED");
        }
    }
'''
_original_java_source=java_source
def java_source():
    text=_original_java_source();anchor='    public static void main(String[] args) throws Exception {'
    assert text.count(anchor)==1
    return text.replace(anchor,MARKER_AUDIT+anchor+'''
        if(args.length==4&&args[0].equals("audit-jar")&&Set.of("core-public-marker","core-book-1.21.11-public-marker","controller-public-marker").contains(args[3])) {
            auditPublicMarker(Path.of(args[1]),Path.of(args[2]),args[3]);return;
        }
''')

JAVA=java_source()

def prepare(folder,jdk):
    folder=Path(folder)
    saved=core.original.JAVA;core.original.JAVA=java_source()
    try:tool=core.original.prepare(folder,jdk)
    finally:core.original.JAVA=saved
    identity='strict-'+uuid.uuid4().hex
    generator=ROOT/'tools/reconcile-rei-workspace-policy-api.py'
    log=folder/'independent-policy-compilation.log'
    with log.open('x')as stream:result=subprocess.run([sys.executable,str(generator),'--id',identity],cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
    assert result.returncode==0,'Independent pinned-API/plugin compiler failed: '+str(log)
    manifest=ROOT/'output/release-readiness'/('rei-workspace-policy-api-contracts-'+identity)/'manifest.json'
    data=json.loads(manifest.read_text());assert data['status']=='twelve-pinned-API-policy-contracts-passed-not-native-gameplay'and len(data['rows'])==12 and data['checks']==84
    tool['independent_REI_compilation']=reference(ROOT,manifest)
    tool['independent_REI_compiler']=reference(ROOT,generator)
    tool['independent_REI_compilation_log']=reference(ROOT,log)
    return tool

def mode(target,family):
    if family=='compatibility':return 'rei-policy:'+target
    if family=='controlify':return 'controller-public-marker'
    return core.mode(target,family)+'-public-marker'

def execute(tool,*args):
    args=list(args)
    if args[0]=='audit-jar'and args[3].startswith('rei-policy:'):
        target=args[3].split(':',1)[1];manifest=json.loads(checked(ROOT,tool['independent_REI_compilation']).read_text())
        rows=[v for v in manifest['rows']if v['target']==target];assert len(rows)==1
        row=rows[0];assert row['checks']==7 and row['contract_exit']==row['compile_exit']==0
        proof=[row['plugin_compilations']['old']['binary'],row['plugin_compilations']['new']['binary'],row['isolated_expected_policy']]
        paths=[checked(ROOT,p)for p in proof]
        args[3]='rei-policy';args.extend(paths)
    return core.execute(tool,*args)

def main():
    import argparse
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True);p.add_argument('--before-ledger',type=Path,required=True);p.add_argument('--after-ledger',type=Path,required=True);p.add_argument('--before-root',type=Path,required=True)
    args=p.parse_args();before=json.loads(args.before_ledger.read_text());after=json.loads(args.after_ledger.read_text())
    assert before['status']==after['status']=='passed' and len(before['artifacts'])==len(after['artifacts'])==36
    key=lambda v:(v['minecraft'],v['loader'],v['family']);old={key(v):v for v in before['artifacts']};new={key(v):v for v in after['artifacts']}
    assert len(old)==len(new)==36 and set(old)==set(new)
    folder=ROOT/'build/release-readiness'/('final-render-rei-bytecode-'+args.id);tool=prepare(folder,Path('C:/Program Files/Java/jdk-26'))
    destination=ROOT/'output/release-readiness'/('final-render-rei-bytecode-'+args.id+'.json');assert not destination.exists()
    record=dict(status='running-strict-final-correction-delta',tool=tool,records=[],inspector=reference(ROOT,Path(__file__).resolve()),limits=__doc__)
    try:
        for k,x in old.items():
            y=new[k];a=args.before_root/x['file'];b=ROOT/y['file'];assert sha(a)==x['sha256']and sha(b)==y['sha256']
            target=x['minecraft']+'/'+x['loader'];scope=mode(target,x['family']);result=execute(tool,'audit-jar',a,b,scope)
            log=folder/(x['family']+'-'+target.replace('/','-')+'.log');log.write_text(result.stdout+result.stderr)
            record['records'].append(dict(target=target,family=x['family'],scope=scope,before=reference(ROOT,a),after=reference(ROOT,b),exit_code=result.returncode,log=reference(ROOT,log)))
            destination.write_text(json.dumps(record,indent=2)+'\n');assert result.returncode==0,'Unreviewed final archive change: '+str(log)
        record['status']='exact-twelve-Core-render-corrections-public-markers-twelve-REI-policies-twelve-Controlify-marker-only-declarations'
    except BaseException as error:record.update(status='failed',error=repr(error));raise
    finally:destination.write_text(json.dumps(record,indent=2)+'\n')
    print(record['status'])

if __name__=='__main__':main()
