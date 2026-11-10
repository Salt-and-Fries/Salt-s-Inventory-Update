"""Prove the exact render-log correction; never grant gameplay acceptance here.

The embedded ASM helper reads shipping classfiles without loading Minecraft.
It normalizes debug tables/frames, removes only the two known constant-false
array arguments from the old detailed log, and compares the result with the
new class. Every other instruction/member/resource must remain unchanged.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,subprocess,sys,zipfile

ROOT=Path(__file__).resolve().parents[1]
MAIN='com/salts_inventory_update/client/InventoryDesktopScreen.class'
JAVA=r'''
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

final class CoreDiagnosticDelta implements Opcodes {
    static final String MAIN="com/salts_inventory_update/client/InventoryDesktopScreen.class";
    static final String OLD="client render container desktop={} window={} menu={} slots={} api={} resizable={} false={} mouse={},{}";
    static final String NEW="client render container desktop={} window={} menu={} slots={} api={} resizable={} mouse={},{}";
    static void require(boolean value,String reason) { if (!value) throw new IllegalStateException(reason); }
    static ClassNode read(byte[] bytes) {
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return node;
    }
    static byte[] bytes(ClassNode node) { ClassWriter w=new ClassWriter(0);node.accept(w);return w.toByteArray(); }
    static int integer(AbstractInsnNode n) {
        int op=n.getOpcode();
        if (op>=ICONST_M1&&op<=ICONST_5) return op-ICONST_0;
        if (n instanceof IntInsnNode value&&(op==BIPUSH||op==SIPUSH)) return value.operand;
        throw new IllegalStateException("Expected a literal array index/size");
    }
    static AbstractInsnNode next(AbstractInsnNode n) {
        n=n.getNext();require(n!=null&&n.getOpcode()>=0,"Unexpected label/metadata inside argument construction");return n;
    }
    static boolean falseBox(List<AbstractInsnNode> value) {
        return value.size()==5&&value.get(0).getOpcode()==DUP&&integer(value.get(1))>=0
            &&value.get(2).getOpcode()==ICONST_0&&value.get(3) instanceof MethodInsnNode method
            &&method.getOpcode()==INVOKESTATIC&&method.owner.equals("java/lang/Boolean")
            &&method.name.equals("valueOf")&&method.desc.equals("(Z)Ljava/lang/Boolean;")
            &&value.get(4).getOpcode()==AASTORE;
    }
    static byte[] expected(byte[] original) {
        ClassNode node=read(original);int changes=0;
        for (MethodNode method:node.methods) for (AbstractInsnNode at:method.instructions.toArray()) {
            if (!(at instanceof LdcInsnNode literal)||!OLD.equals(literal.cst)) continue;
            require(method.name.equals("renderContainerWindow"),"Log moved into an unreviewed method");
            require(changes++==0,"Duplicate original logging statement");
            AbstractInsnNode size=next(at);require(integer(size)==10,"Original logging array is not size10");
            AbstractInsnNode array=next(size);require(array instanceof TypeInsnNode type&&array.getOpcode()==ANEWARRAY
                &&type.desc.equals("java/lang/Object"),"Original logging array type changed");
            var chunks=new ArrayList<List<AbstractInsnNode>>();AbstractInsnNode cursor=next(array);
            for (int index=0;index<10;index++) {
                require(cursor.getOpcode()==DUP,"Argument does not begin with DUP");
                var chunk=new ArrayList<AbstractInsnNode>();chunk.add(cursor);cursor=next(cursor);
                require(integer(cursor)==index,"Unexpected original argument index");chunk.add(cursor);
                cursor=next(cursor);
                while (cursor.getOpcode()!=AASTORE) {
                    require(cursor.getOpcode()!=ANEWARRAY&&cursor.getOpcode()!=NEWARRAY
                        &&!(cursor instanceof JumpInsnNode)&&!(cursor instanceof TableSwitchInsnNode)
                        &&!(cursor instanceof LookupSwitchInsnNode),"Unreviewed argument control flow/array");
                    chunk.add(cursor);cursor=next(cursor);
                }
                chunk.add(cursor);chunks.add(chunk);cursor=next(cursor);
            }
            require(cursor instanceof MethodInsnNode call&&call.getOpcode()==INVOKESTATIC
                &&call.owner.equals("com/salts_inventory_update/debug/DesktopDebug")&&call.name.equals("detail")
                &&call.desc.equals("(Ljava/lang/String;[Ljava/lang/Object;)V"),"Original logging callee changed");
            require(falseBox(chunks.get(6))&&falseBox(chunks.get(7)),"Only the two original constant-false arguments may be removed");
            literal.cst=NEW;method.instructions.set(size,new IntInsnNode(BIPUSH,8));
            for (int index:new int[]{6,7}) for (AbstractInsnNode n:chunks.get(index)) method.instructions.remove(n);
            method.instructions.set(chunks.get(8).get(1),new IntInsnNode(BIPUSH,6));
            method.instructions.set(chunks.get(9).get(1),new IntInsnNode(BIPUSH,7));
        }
        require(changes==1,"Missing unique original logging contract");return bytes(node);
    }
    static byte[] entry(ZipFile jar,ZipEntry member) throws Exception {
        byte[] bytes=jar.getInputStream(member).readAllBytes();CRC32 crc=new CRC32();crc.update(bytes);
        require(crc.getValue()==member.getCrc(),"CRC mismatch: "+member.getName());return bytes;
    }
    static Map<String,ZipEntry> entries(ZipFile jar) {
        var result=new TreeMap<String,ZipEntry>();var all=jar.entries();
        while(all.hasMoreElements()) { ZipEntry n=all.nextElement();
            require(result.put(n.getName(),n)==null,"Duplicate jar member: "+n.getName()); }
        return result;
    }
    static void audit(Path before,Path after,boolean core) throws Exception {
        if (!core) {
            require(Arrays.equals(Files.readAllBytes(before),Files.readAllBytes(after)),"Unrelated module archive changed");
            System.out.println("NONCORE_BYTE_IDENTICAL");return;
        }
        try(ZipFile a=new ZipFile(before.toFile());ZipFile b=new ZipFile(after.toFile())) {
            var old=entries(a);var now=entries(b);require(old.keySet().equals(now.keySet()),"Jar member set changed");
            require(old.containsKey(MAIN),"Core screen class missing");int classes=0;var debugOnly=new ArrayList<String>();
            for (String name:old.keySet()) {
                byte[] x=entry(a,old.get(name)),y=entry(b,now.get(name));
                if(name.equals(MAIN)) {
                    require(!Arrays.equals(x,y),"The diagnostic correction was not compiled");
                    require(Arrays.equals(expected(x),bytes(read(y))),"Compiled screen changed beyond the exact logging correction");
                } else if(name.endsWith(".class")) {
                    classes++;require(Arrays.equals(bytes(read(x)),bytes(read(y))),"Unrelated compiled class/member/instruction changed: "+name);
                    if(!Arrays.equals(x,y))debugOnly.add(name);
                } else require(Arrays.equals(x,y),"Unrelated resource changed: "+name);
            }
            System.out.println("CORE_LOG_ONLY_DELTA_VERIFIED");System.out.println("other_classes="+classes);
            System.out.println("debug_only_classes="+debugOnly.size());for(String n:debugOnly)System.out.println("debug_only_class="+n);
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length==2&&args[0].equals("inspect-old")) {
            try(ZipFile jar=new ZipFile(args[1])) { expected(entry(jar,jar.getEntry(MAIN))); }
            System.out.println("OLD_LOG_CONTRACT_INSPECTED_NO_NEW_ARTIFACT_ACCEPTED");return;
        }
        require(args.length==4&&args[0].equals("audit-jar"),"Expected audit-jar BEFORE AFTER core|unchanged");
        require(args[3].equals("core")||args[3].equals("unchanged"),"Unknown audit scope");
        audit(Path.of(args[1]),Path.of(args[2]),args[3].equals("core"));
    }
}
'''


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def prepare(folder,jdk):
    """Copy exact local ASM9.9.1 bytes and compile a separate inspection helper."""
    folder=Path(folder);folder.mkdir(parents=True,exist_ok=False);libs=folder/'libraries';libs.mkdir();copies=[]
    cache=Path(os.environ['USERPROFILE'])/'.gradle/caches/modules-2/files-2.1/org.ow2.asm'
    for artifact in ['asm','asm-tree']:
        jars=list((cache/artifact/'9.9.1').glob('*/'+artifact+'-9.9.1.jar'));assert len(jars)==1
        dest=libs/jars[0].name;shutil.copy2(jars[0],dest);assert sha(dest)==sha(jars[0]);copies.append(dest)
    source=folder/'CoreDiagnosticDelta.java';source.write_text(JAVA)
    classes=folder/'classes';classes.mkdir();classpath=os.pathsep.join(str(p)for p in copies)
    compiler=Path(jdk)/'bin/javac.exe';runtime=Path(jdk)/'bin/java.exe';assert compiler.is_file()and runtime.is_file()
    command=[str(compiler),'--release','17','-cp',classpath,'-d',str(classes),str(source)]
    result=subprocess.run(command,capture_output=True,text=True);log=folder/'compile.log';log.write_text(result.stdout+result.stderr)
    assert result.returncode==0,'Inspection helper compilation failed: '+str(log)
    binary=classes/'CoreDiagnosticDelta.class';assert binary.is_file()
    return dict(java=str(runtime),java_sha256=sha(runtime),javac=str(compiler),javac_sha256=sha(compiler),
        classpath=os.pathsep.join([str(classes),classpath]),source=str(source),source_sha256=sha(source),
        binary=str(binary),binary_sha256=sha(binary),libraries=[dict(file=str(p),sha256=sha(p))for p in copies],compile_log=str(log))


def execute(tool,*args):
    assert sha(tool['java'])==tool['java_sha256']and sha(tool['binary'])==tool['binary_sha256']
    assert all(sha(v['file'])==v['sha256']for v in tool['libraries'])
    return subprocess.run([tool['java'],'-cp',tool['classpath'],'CoreDiagnosticDelta',*map(str,args)],capture_output=True,text=True)


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True)
    p.add_argument('--jdk',type=Path,default=Path('C:/Program Files/Java/jdk-26'))
    p.add_argument('--prepare-only',action='store_true');p.add_argument('--before-ledger',type=Path);p.add_argument('--after-ledger',type=Path)
    p.add_argument('--before-root',type=Path);a=p.parse_args();assert a.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in a.id)
    folder=ROOT/'build/release-readiness'/('core-log-bytecode-'+a.id);tool=prepare(folder,a.jdk)
    output=ROOT/'output/release-readiness'/('core-log-bytecode-'+a.id+'.json');assert not output.exists()
    result=dict(status='inspection-helper-compiled-no-artifact-acceptance',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        inspector=dict(file=Path(__file__).relative_to(ROOT).as_posix(),sha256=sha(__file__)),tool=tool,
        limits='Exact diagnostic-only source/compiled delta evidence, not new gameplay execution, automatic evidence retention, public ABI acceptance or release approval.')
    if not a.prepare_only:
        assert a.before_ledger and a.after_ledger and a.before_root
        before=json.loads(a.before_ledger.read_text());after=json.loads(a.after_ledger.read_text())
        assert before['status']==after['status']=='passed'and len(before['artifacts'])==len(after['artifacts'])==36
        result.update(status='running',before_ledger=dict(file=str(a.before_ledger),sha256=sha(a.before_ledger)),
            after_ledger=dict(file=str(a.after_ledger),sha256=sha(a.after_ledger)),records=[])
        output.write_text(json.dumps(result,indent=2)+'\n')
        try:
            for old in before['artifacts']:
                new=next(v for v in after['artifacts']if all(v[k]==old[k]for k in ['family','minecraft','loader']))
                x=a.before_root/old['file'];y=ROOT/new['file'];assert sha(x)==old['sha256']and sha(y)==new['sha256']
                check=execute(tool,'audit-jar',x,y,'core'if old['family']=='core'else'unchanged')
                log=folder/(old['family']+'-'+old['minecraft']+'-'+old['loader']+'.log');log.write_text(check.stdout+check.stderr)
                record=dict(target=old['minecraft']+'/'+old['loader'],family=old['family'],before=dict(file=str(x),sha256=sha(x)),
                    after=dict(file=str(y),sha256=sha(y)),exit_code=check.returncode,log=dict(file=str(log),sha256=sha(log)))
                result['records'].append(record);output.write_text(json.dumps(result,indent=2)+'\n')
                assert check.returncode==0,'Unexpected compiled/resource change: '+str(log)
            result['status']='exact-12-Core-log-only-deltas-and-24-byte-identical-modules'
        except BaseException as error:result.update(status='failed',error=repr(error));raise
        finally:output.write_text(json.dumps(result,indent=2)+'\n')
    else:output.write_text(json.dumps(result,indent=2)+'\n')
    print(result['status'])


if __name__=='__main__':main()
