"""Resolve patched AE2 native member references against each exact upstream ABI."""
from pathlib import Path
import argparse,json,subprocess,os,zipfile
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/all-target-rollout'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--minecraft',required=True);parser.add_argument('--loader',default='neoforge');args=parser.parse_args();v,l=args.minecraft,args.loader
    exported=json.loads((OUT/f'classpaths/{v}-{l}.json').read_text());availability=json.loads((OUT/'availability.json').read_text());release=availability['projects']['ae2']['targets'][v+'/'+l]
    upstream=OUT/'upstream'/(release['sha512'][:16]+'-'+release['filename'])
    if v=='1.20.1':
        # These contracts use Mojang's named descriptors. Original Fabric and
        # Forge release archives use intermediary/SRG names, respectively;
        # indexing either last would replace the compiler's correctly mapped
        # publisher classes with incompatible verification inputs.
        candidates=[]
        for name in exported['compile']:
            candidate=Path(name)
            if candidate.suffix=='.jar' and candidate.is_file() and 'ae2-desktop-declarations' not in candidate.name:
                with zipfile.ZipFile(candidate) as archive:
                    if 'appeng/client/gui/AEBaseScreen.class' in archive.namelist():candidates.append(candidate)
        assert len(candidates)==1,('Expected the exact Gradle-mapped AE2 compiler dependency',candidates)
        upstream=candidates[0]
    classes=ROOT/'build/ae2-port/patch-tools'
    # The verification VM is JDK 25+, even when the game target is older.
    # ASM must also read inherited JDK collection/Enum members on that VM.
    tool_exports=json.loads((OUT/'classpaths/26.3-neoforge.json').read_text()) if v=='1.20.1' else exported
    tool_libraries=[p for p in tool_exports['compile'] if Path(p).name.startswith(('asm-','gson-'))]
    tool_cp=os.pathsep.join([str(classes)]+tool_libraries)
    inputs=[p for p in exported['compile'] if 'ae2-desktop-declarations.jar' not in p and Path(p).exists()]
    inputs+=exported['output']+[str(ROOT/f'versions/{v}/{l}/build/classes/java/main'),str(upstream)]
    patch=ROOT/f'compatibility/ae2/versions/{v}/{l}/src/main/resources/ae2-native-patches'
    commands=['-Xmx2G','-cp',tool_cp,'VerifyAe2NativeSymbols',str(patch)]+inputs
    argfile=OUT/f'ae2-symbols-{v}-{l}.args'
    argfile.write_text('\n'.join('"'+arg.replace('\\','/')+'"' for arg in commands))
    log=OUT/f'ae2-symbols-{v}-{l}.log'
    java='C:/Program Files/Java/'+('jdk-25.0.3' if not v.startswith('26.') else 'jdk-26')+'/bin/java.exe'
    with log.open('w') as f:result=subprocess.run([java,'@'+str(argfile)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
    print(v,l,'PASS' if result.returncode==0 else 'FAIL',log)
    if result.returncode:print('\n'.join(log.read_text().splitlines()[-14:]))
    raise SystemExit(result.returncode)

if __name__=='__main__':main()
