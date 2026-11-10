"""Launch ordinary Forge release binaries; remap only the excluded observer."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,shutil,subprocess,zipfile
from controlify_classfile import read_class
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('modern',ROOT/'tools/prepare-rollout-production-modern.py')
modern=importlib.util.module_from_spec(spec);spec.loader.exec_module(modern);modern.MC='1.20.1';base=modern.base
CACHE=ROOT/'build/all-target-rollout/production-forge-cache';INSTALL=Path.home()/'.cache/siu-rollout-forge-install/47.4.0'
MAPPINGS=ROOT/'versions/1.20.1/forge/build/moddev/artifacts/namedToIntermediate.tsrg'
JAVA=Path('C:/Program Files/Microsoft/jdk-21.0.10.7-hotspot/bin/java.exe')

def prepare_qa(classpath_file):
 aliases={'classes':{},'members':{}};owner=None
 for line in MAPPINGS.read_text().splitlines():
  parts=line.split()
  if not parts:continue
  if not line.startswith('\t'):
   owner=parts[1].replace('/','.');aliases['classes'][parts[0].replace('/','.')]=owner;aliases['members'][owner]={'fields':{},'methods':{}}
  else:
   group=aliases['members'][owner]['methods' if len(parts)==3 else 'fields'];group.setdefault(parts[0],[]).append(parts[-1])
 _,source=modern.qa_source('siu');encoded=json.dumps(aliases,separators=(',',':'))
 key=hashlib.sha256((source+modern.RESOLVER+encoded+'tiny-v2-field-descriptors-1').encode()).hexdigest()[:16]
 folder=CACHE/'qa'/key;mapped=folder/'observer-srg.jar'
 if mapped.exists() and (folder/'lineage.json').exists():return mapped
 sources=folder/'source/com/siu_reference';sources.mkdir(parents=True,exist_ok=True)
 (sources/'NativeUiReference.java').write_text(source);(sources/'RuntimeNames.java').write_text(modern.RESOLVER)
 (sources/'NativeReferenceForge.java').write_text('''package com.siu_reference;
@net.minecraftforge.fml.common.Mod("native_ui_reference")
public final class NativeReferenceForge {
 public NativeReferenceForge(){net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent e)->{if(e.phase==net.minecraftforge.event.TickEvent.Phase.END)NativeUiReference.tick();});}
}
''')
 cp=base.read(classpath_file)['runtimeClasspath'];classes=folder/'classes';classes.mkdir(exist_ok=True)
 args=['--release','17','-proc:none','-encoding','UTF-8','-cp',';'.join(cp),'-d',str(classes),*map(str,sources.glob('*.java'))]
 argfile=folder/'javac.args';argfile.write_text('\n'.join(map(base.java_argument,args)))
 with (folder/'compile.log').open('w') as log:subprocess.run([str(JAVA.with_name('javac.exe')),'@'+str(argfile.resolve())],stdout=log,stderr=subprocess.STDOUT,check=True)
 original=folder/'observer-named.jar'
 with zipfile.ZipFile(original,'w',zipfile.ZIP_DEFLATED) as archive:
  for path in classes.rglob('*.class'):archive.write(path,path.relative_to(classes).as_posix())
  archive.writestr('runtime-names.json',encoded)
  archive.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[47,)"\nlicense="Test fixture"\n[[mods]]\nmodId="native_ui_reference"\nversion="1.0.0"\ndisplayName="Excluded UI reference observer"\n')
  archive.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':'Excluded original/SIU UI reference observer'}}))
 tiny=next(p for p in (base.GRADLE/'net.fabricmc/tiny-remapper/0.14.1').glob('*/*.jar') if not p.name.endswith('-sources.jar'))
 mapping=next(p for p in (base.GRADLE/'net.fabricmc/mapping-io/0.8.0').glob('*/*.jar') if not p.name.endswith('-sources.jar'))
 remapper_cp=[str(tiny),str(mapping)]+[s for s in cp if 'org.ow2.asm' in s]
 minecraft=next(s for s in cp if Path(s).name=='forge-1.20.1-47.4.0.jar')
 # TSRG1 omits field descriptors, which TinyRemapper needs. Obtain each
 # descriptor from the exact named Minecraft/Forge class used for compilation.
 lines=['tiny\t2\t0\tnamed\tsrg'];owner=None;fields={}
 with zipfile.ZipFile(minecraft) as mc:
  members=set(mc.namelist())
  for line in MAPPINGS.read_text().splitlines():
   parts=line.split()
   if not parts:continue
   if not line.startswith('\t'):
    owner=parts[0];lines.append('c\t'+parts[0]+'\t'+parts[1])
    fields=dict(read_class(mc.read(owner+'.class')).fields) if owner+'.class' in members else {}
   elif len(parts)==3:lines.append('\tm\t'+parts[1]+'\t'+parts[0]+'\t'+parts[2])
   elif parts[0] in fields:lines.append('\tf\t'+fields[parts[0]]+'\t'+parts[0]+'\t'+parts[1])
 tiny_map=folder/'named-to-srg.tiny';tiny_map.write_text('\n'.join(lines)+'\n')
 temporary=folder/'observer-srg.pending.jar'
 with (folder/'remap.log').open('w') as log:
  subprocess.run([str(JAVA),'-cp',';'.join(remapper_cp),'net.fabricmc.tinyremapper.Main',str(original),str(temporary),str(tiny_map),'named','srg',minecraft],stdout=log,stderr=subprocess.STDOUT,check=True)
 with zipfile.ZipFile(temporary) as remapped:
  assert remapped.testzip() is None
  refs=read_class(remapped.read('com/siu_reference/NativeUiReference.class')).references
  assert sum(owner.startswith('net/minecraft/') and name.startswith(('f_','m_')) for _,owner,name,_ in refs)>50
 temporary.replace(mapped)
 with zipfile.ZipFile(original) as original_zip,zipfile.ZipFile(mapped) as mapped_zip:
  for name in original_zip.namelist():
   if not name.endswith('.class'):assert original_zip.read(name)==mapped_zip.read(name)
 (folder/'lineage.json').write_text(json.dumps({'scope':'Only the excluded observer is remapped named to SRG. Original publishers and shipping Core/Compatibility JARs retain their exact release bytes.','named_qa_sha256':base.digest(original),'srg_qa_sha256':base.digest(mapped),'mappings_sha256':base.digest(MAPPINGS)},indent=2))
 return mapped

def main():
 p=argparse.ArgumentParser();p.add_argument('--fixture',required=True,type=Path);p.add_argument('--side',required=True,choices=['native','siu']);p.add_argument('--run',required=True)
 p.add_argument('--classpath-from',type=Path,default=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-forge-native-rollout-inventory-batch-03/staged-artifacts.json')
 p.add_argument('--core-jar',type=Path);p.add_argument('--compat-jar',type=Path);p.add_argument('--launch',action='store_true');p.add_argument('--export-mixin',help='One class filter, in this disposable runtime only');a=p.parse_args()
 fixture=base.read(a.fixture);assert (fixture['minecraft'],fixture['loader'])==('1.20.1','forge');assert re.fullmatch('[a-z0-9-]+',a.run)
 profile=base.read(INSTALL/'versions/1.20.1-forge-47.4.0/1.20.1-forge-47.4.0.json');vanilla=base.read(INSTALL/'versions/1.20.1/1.20.1.json')
 output=ROOT/'build/remaining-ui-audit/gameplay'/f'1.20.1-forge-{a.side}-{a.run}';runtime=Path.home()/'.cache/siu-rollout-runtime'/f'1.20.1-forge-{a.side}-{a.run}'
 assert not output.exists() and not runtime.exists(),'Preserve prior evidence; choose a fresh run ID'
 qa=prepare_qa(a.classpath_from)
 cp=[]
 for record in profile['libraries']:
  artifact=record.get('downloads',{}).get('artifact',{});path=INSTALL/'libraries'/artifact.get('path',base.coordinate_path(record['name']))
  if not path.exists():base.fetch(artifact['url'],path,artifact.get('sha1'))
  if artifact.get('sha1'):assert base.digest(path,'sha1')==artifact['sha1']
  cp.append(path)
 cp += [base.library(r) for r in vanilla['libraries'] if base.allowed(r) and 'artifact' in r.get('downloads',{})]
 client=INSTALL/'versions/1.20.1/1.20.1.jar';assert base.digest(client,'sha1')==vanilla['downloads']['client']['sha1'];cp.append(client)
 assets=Path.home()/'.gradle/caches/fabric-loom/assets';index='rollout-1.20.1-'+vanilla['assetIndex']['id'];base.fetch(vanilla['assetIndex']['url'],assets/'indexes'/f'{index}.json',vanilla['assetIndex']['sha1'])
 runtime.mkdir(parents=True);output.mkdir(parents=True);mods=runtime/'mods';mods.mkdir();inputs=[]
 for record in fixture['artifacts']:
  if record.get('coordinate'):
   path=ROOT/record['path'];assert base.digest(path,'sha512')==record['sha512'];inputs.append(('upstream',path,record['filename']))
 inputs.append(('qa',qa,'native-ui-reference.jar'))
 if a.side=='siu':
  for family,path in [('core',a.core_jar),('compatibility',a.compat_jar)]:
   assert path and path.is_file();inputs.append((family,path,path.name))
 staged=[]
 for family,path,name in inputs:
  assert name not in {r['filename'] for r in staged};shutil.copy2(path,mods/name)
  staged.append({'family':family,'file':str(path.resolve()),'filename':name,'sha256':base.digest(path),'sha512':base.digest(path,'sha512')})
 (runtime/'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n')
 if any(record['filename'].lower().startswith('supplementaries-') for record in fixture['artifacts']):
  # These profiles create fresh disposable worlds, with no migrated blocks.
  # Match the Fabric reference setup without accepting an old-world warning.
  config=runtime/'config/supplementaries-client.toml';config.parent.mkdir(parents=True,exist_ok=True)
  config.write_text('[general]\nno_amendments_screen = true\n')
  (output/'offline-reference-settings.json').write_text(json.dumps({'scope':'Fresh disposable test world only; publisher and shipping JAR bytes unchanged','supplementaries':'Suppress the optional Amendments migration reminder before world creation. There is no existing world or migrated block data.'},indent=2))
 replacements={'library_directory':str(INSTALL/'libraries'),'classpath_separator':';','version_name':'1.20.1','natives_directory':str(runtime/'natives')}
 def expand(value):
  for key,replacement in replacements.items():value=value.replace('${'+key+'}',replacement)
  assert '${' not in value,value
  return value
 args=['-Xms512m','-Xmx2g','-XX:ActiveProcessorCount=2','-Djava.library.path='+str(runtime/'natives'),'-Dorg.lwjgl.system.SharedLibraryExtractPath='+str(runtime/'natives')]
 if a.export_mixin:
  assert re.fullmatch(r'[A-Za-z0-9.$_*]+',a.export_mixin)
  args += ['-Dmixin.debug.export=true','-Dmixin.debug.export.filter='+a.export_mixin]
 args += [expand(v) for v in profile['arguments']['jvm']]
 args += ['-cp',';'.join(map(str,cp)),profile['mainClass'],'--username','UIAudit','--version',profile['id'],'--gameDir',str(runtime),'--assetsDir',str(assets),'--assetIndex',index,'--uuid','14ea713fe12d3d8bb7b6baf52d52d598','--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','900']
 args += profile['arguments']['game']
 argfile=output/'java.args';argfile.write_text('\n'.join(map(base.java_argument,args)))
 environment={'NATIVE_REFERENCE_OUTPUT':str(output),'NATIVE_REFERENCE_LOADER':'forge','SIU_AUDIT_CORE':str(a.side=='siu').lower()}
 launch={'java':str(JAVA),'arguments':str(argfile.resolve()),'cwd':str(runtime),'environment':environment,'log':str(output/'launch.log')}
 (output/'launch.json').write_text(json.dumps(launch,indent=2));(output/'staged-artifacts.json').write_text(json.dumps({'minecraft':'1.20.1','loader':'forge','runtime':'Official installed Forge 47.4.0 production client, original subject JAR bytes','siu_loaded':a.side=='siu','artifacts':staged},indent=2))
 if a.launch:
  with (output/'launch.log').open('w') as log:process=subprocess.Popen([str(JAVA),'@'+str(argfile.resolve())],cwd=runtime,env={**os.environ,**environment},stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
  (output/'process.json').write_text(json.dumps({'pid':process.pid,'launch':launch},indent=2));print('STARTED',process.pid,output)
 else:print('PREPARED',output)
if __name__=='__main__':main()
