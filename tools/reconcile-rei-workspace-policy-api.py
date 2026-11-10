"""Compile and execute pure policy dispatch against twelve exact REI APIs.

Minimal Minecraft type declarations supply only identity/enum constants for
this isolated test. This is not a Minecraft render or gameplay run. Actual
native browser checks remain mandatory before the proposed fix is accepted.
"""
from pathlib import Path
import argparse, datetime, hashlib, json, re, subprocess, sys, zipfile

parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--id',required=True);args=parser.parse_args();assert re.fullmatch('[a-z0-9-]+',args.id)
r=Path(__file__).resolve().parents[1];sys.path.insert(0,str(r/'tools'))
from focused_release_evidence import checked, reference
out=r/'output/release-readiness'/('rei-workspace-policy-api-contracts-'+args.id);out.mkdir()
build=r/'build/release-readiness'/('rei-workspace-policy-api-contracts-'+args.id);build.mkdir()
PROPOSAL={'file': 'output/release-readiness/rei-public-marker-recovery-1529-01/manifest.json', 'sha256': '48318bd2c186a70d07bc72dc4d3869e71c3b5abc52cf220c9b41a09e40d063b5'}
BASELINE_LEDGER={'file': 'output/release-readiness/candidate-integrity-build14-requester-forge-network-1280-01.json', 'sha256': '9ab653d48443f9f15465c0b88591dbbf65a3a9087ae0f71542098899805c9f1c'}
BASELINE_SOURCE={'file': 'output/release-readiness/candidate-build14-red-merchant-provenance-1518.json', 'sha256': '2a057fb13285fc211ff5481550436f15e93c2dbcfb143f042dd62f4edfd38f83'}
proposal=checked(r,PROPOSAL);p=json.loads(proposal.read_text())
baseline=json.loads(checked(r,BASELINE_LEDGER).read_text())
source_snapshot=json.loads(checked(r,BASELINE_SOURCE).read_text())
assert p['status']=='prepared-and-applied-not-compiled' and p['schema']=='exact-REI-public-workspace-marker-policy-v2'
jdk=Path('C:/Program Files/Java/jdk-26/bin')
record=dict(status='running-isolated-API-contracts',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    driver=reference(r,Path(__file__)),proposal=reference(r,proposal),baseline_ledger=BASELINE_LEDGER,baseline_source=BASELINE_SOURCE,rows=[],scope=__doc__)
destination=out/'manifest.json'
def save():destination.write_text(json.dumps(record,indent=2)+'\n')
save()
for pin in p['primary_jars']:
    target=pin['target'];mc,loader=target.split('/');jar=checked(r,pin['jar'])
    base=build/target.replace('/','-');src=base/'src';classes=base/'classes';src.mkdir(parents=True);classes.mkdir()
    result=subprocess.run([str(jdk/'javap.exe'),'-p','-c','-classpath',str(jar),
        'me.shedaniel.rei.api.client.registry.screen.OverlayDecider','me.shedaniel.rei.RoughlyEnoughItemsCoreClient'],capture_output=True,text=True)
    assert result.returncode==0,result.stderr
    primary=out/(target.replace('/','-')+'-primary-api.txt');primary.write_text(result.stdout)
    api=result.stdout
    screen=re.search(r'public abstract <R extends ([\w.$]+)> boolean isHandingScreen',api)[1]
    interaction=re.search(r'public default ([\w.$]+) shouldScreenBeOverlaid\(java.lang.Class',api)[1]
    fields=re.findall(r'getstatic\s+#\d+\s+// Field '+re.escape(interaction.replace('.','/'))+r'\.([\w$]+):(L[^;]+;)',api)
    assert fields
    pass_name,pass_desc=fields[0]
    candidates=[f for f in fields if f!=(pass_name,pass_desc)]
    assert len(set(candidates))==1,(target,fields)
    fail_name,fail_desc=candidates[0]
    policy_row=next(v for v in p['rows']if v['kind']=='new-exact-screen-policy'and f'/versions/{mc}/'in v['source']['file'])
    text=checked(r,policy_row['prepared_source']).read_text()
    text=text.replace('net.minecraft.client.gui.screens.Screen',screen).replace('net.minecraft.world.InteractionResult',interaction)
    screen_simple=screen.rsplit('.',1)[-1];result_simple=interaction.rsplit('.',1)[-1]
    text=re.sub(r'\bScreen\b',screen_simple,text);text=re.sub(r'\bInteractionResult\b',result_simple,text)
    text=text.replace(result_simple+'.FAIL',result_simple+'.'+fail_name).replace(result_simple+'.PASS',result_simple+'.'+pass_name)
    def write(name,text):
        f=src/(name.replace('.','/')+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text);return f
    write(screen,'package '+screen.rsplit('.',1)[0]+'; public class '+screen_simple+' {}')
    native_type=interaction.replace('.','/')
    declarations=[];constants=[]
    for field,desc in [(pass_name,pass_desc),(fail_name,fail_desc)]:
        typ=desc[1:-1]
        if typ==native_type:type_name=result_simple
        else:
            assert typ.startswith(native_type+'$');type_name=typ.split('$')[-1]
            declarations.append('public static final class '+type_name+' extends '+result_simple+' {}')
        constants.append('public static final '+type_name+' '+field+' = new '+type_name+'();')
    write(interaction,'package '+interaction.rsplit('.',1)[0]+'; public class '+result_simple+' {'+' '.join(set(declarations))+' '.join(constants)+'}')
    write('com.salts_inventory_update.compat.rei.WorkspaceReiOverlayPolicy',text)
    write('com.salts_inventory_update.api.client.recipebrowser.DesktopRecipeBrowserScreen','package com.salts_inventory_update.api.client.recipebrowser; public interface DesktopRecipeBrowserScreen {}')
    write('com.salts_inventory_update.controlify.RadialInventoryScreen','package com.salts_inventory_update.controlify; public class RadialInventoryScreen extends '+screen+' implements com.salts_inventory_update.api.client.recipebrowser.DesktopRecipeBrowserScreen {}')
    harness='''package com.salts_inventory_update.compat.rei;
public class PolicyContract {
    static class NativeScreen extends SCREEN {}
    static void require(boolean x) { if(!x) throw new AssertionError(); }
    public static void main(String[] args) {
        var policy=new WorkspaceReiOverlayPolicy();
        var workspace=new com.salts_inventory_update.controlify.RadialInventoryScreen();
        var ordinary=new NativeScreen();
        require(policy.isHandingScreen(workspace.getClass()));
        require(!policy.isHandingScreen(ordinary.getClass()));
        require(policy.shouldScreenBeOverlaid(workspace.getClass())==RESULT.FAIL_FIELD);
        require(policy.shouldScreenBeOverlaid(workspace)==RESULT.FAIL_FIELD);
        require(policy.shouldScreenBeOverlaid(ordinary.getClass())==RESULT.PASS_FIELD);
        require(policy.shouldScreenBeOverlaid(ordinary)==RESULT.PASS_FIELD);
        require(policy.getPriority()==1000.0);
        System.out.println("SEVEN_EXACT_REI_POLICY_CONTRACTS_PASSED");
    }
}
'''.replace('SCREEN',screen).replace('RESULT',interaction).replace('FAIL_FIELD',fail_name).replace('PASS_FIELD',pass_name)
    write('com.salts_inventory_update.compat.rei.PolicyContract',harness)
    artifact=next(v for v in baseline['artifacts']if v['minecraft']==mc and v['loader']==loader and v['family']=='compatibility')
    old_copy=r/'output/release-readiness/normal-build14-subjects-preserved-01'/artifact['file']
    old_jar=old_copy if old_copy.exists() else r/artifact['file']
    assert hashlib.sha256(old_jar.read_bytes()).hexdigest()==artifact['sha256']
    plugin='SaltsReiClientPlugin'if loader=='fabric'else'SaltsReiForgeClientPlugin'
    with zipfile.ZipFile(old_jar)as z:b=z.read('com/salts_inventory_update/compat/rei/'+plugin+'.class')
    release=int.from_bytes(b[6:8],'big')-44
    compile_args=[str(jdk/'javac.exe'),'--release',str(release),'-cp',str(jar),'-d',str(classes),*[str(f)for f in src.rglob('*.java')]]
    compiled=subprocess.run(compile_args,capture_output=True,text=True);log=out/(target.replace('/','-')+'-compile.log');log.write_text(compiled.stdout+compiled.stderr)
    assert compiled.returncode==0,(target,compiled.stderr)
    run=subprocess.run([str(jdk/'java.exe'),'-cp',str(classes)+';'+str(jar),'com.salts_inventory_update.compat.rei.PolicyContract'],capture_output=True,text=True)
    runtime_log=out/(target.replace('/','-')+'-contracts.log');runtime_log.write_text(run.stdout+run.stderr)
    assert run.returncode==0 and run.stdout.strip()=='SEVEN_EXACT_REI_POLICY_CONTRACTS_PASSED',(target,run.stderr)
    policy_class=classes/'com/salts_inventory_update/compat/rei/WorkspaceReiOverlayPolicy.class'
    old_ledger=baseline
    old_artifacts={}
    for family in ['core','compatibility']:
        old=next(v for v in old_ledger['artifacts']if v['minecraft']==mc and v['loader']==loader and v['family']==family)
        preserved=r/'output/release-readiness/normal-build14-subjects-preserved-01'/old['file']
        source=preserved if preserved.exists()else r/old['file']
        assert hashlib.sha256(source.read_bytes()).hexdigest()==old['sha256']
        old_artifacts[family]=source
    plugin_row=next(v for v in p['rows']if v['source']['file'].endswith('/'+plugin+'.java')and '/versions/'+mc+'/'+loader+'/'in v['source']['file'])
    snapshot=source_snapshot
    with zipfile.ZipFile(checked(r,snapshot['review_source_archive']))as archive:
        old_text=archive.read(plugin_row['source']['file'])
    assert hashlib.sha256(old_text).hexdigest()==plugin_row['source']['sha256']
    plugin_compilations={}
    for label,data in [('old',old_text),('new',checked(r,plugin_row['prepared_source']).read_bytes())]:
        plugin_src=base/(label+'-plugin-src')/(plugin+'.java');plugin_src.parent.mkdir();plugin_src.write_bytes(data)
        plugin_classes=base/(label+'-plugin-classes');plugin_classes.mkdir()
        cp=';'.join(str(v)for v in [classes,jar,old_artifacts['core'],old_artifacts['compatibility']])
        command=[str(jdk/'javac.exe'),'--release',str(release),'-cp',cp,'-d',str(plugin_classes),str(plugin_src)]
        result=subprocess.run(command,capture_output=True,text=True)
        log=out/(target.replace('/','-')+'-'+label+'-plugin-compile.log');log.write_text(result.stdout+result.stderr)
        assert result.returncode==0,(target,label,result.stderr)
        binary=plugin_classes/('com/salts_inventory_update/compat/rei/'+plugin+'.class')
        plugin_compilations[label]=dict(source=reference(r,plugin_src),binary=reference(r,binary),exit_code=result.returncode,log=reference(r,log),command=command)
    record['rows'].append(dict(target=target,actual_primary_jar=pin['jar'],primary_api=reference(r,primary),source_policy=policy_row['prepared_source'],
        native_types=dict(screen=screen,result=interaction,pass_field=[pass_name,pass_desc],fail_field=[fail_name,fail_desc]),
        plugin_compilations=plugin_compilations,java_release=release,compile_exit=compiled.returncode,compile_log=reference(r,log),contract_exit=run.returncode,contract_log=reference(r,runtime_log),checks=7,
        compiler_command=compile_args,isolated_expected_policy=reference(r,policy_class)))
    save();print('POLICY_CONTRACT_PASS',target,flush=True)
assert len(record['rows'])==12
record.update(status='twelve-pinned-API-policy-contracts-passed-not-native-gameplay',checks=84,
    finished=datetime.datetime.now(datetime.timezone.utc).isoformat(),limits='Pure policy dispatch using exact REI interfaces plus minimal Minecraft type declarations. Proposed production sources remain unapplied. No native overlay, recipe browser or rendering acceptance.')
save();print(record['status'])
