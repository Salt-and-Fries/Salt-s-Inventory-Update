"""Read pending original UI construction sites without loading publisher code."""
from pathlib import Path
import collections,hashlib,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
cp=';'.join(str(next((cache/group/name/version).glob('**/'+name+'-'+version+'.jar')))
            for group,name,version in [('org.ow2.asm','asm','9.11'),('org.ow2.asm','asm-tree','9.11'),('com.google.code.gson','gson','2.14.0')])
out=ROOT/'build/all-target-rollout/ui-construction-audit';out.mkdir(exist_ok=True)
subprocess.run(['C:/Program Files/Java/jdk-26/bin/javac','-cp',cp,'-d',str(out),str(ROOT/'tools/UiConstructionAudit.java')],check=True)
plan=json.loads((ROOT/'output/all-target-rollout/ui-evidence-plan.json').read_text())
lock=json.loads((ROOT/'build/all-target-rollout/availability.json').read_text())
groups=collections.defaultdict(list)
for row in plan:
 if row.get('ui_scope','').startswith('appendix:') or row.get('native_capture') and row.get('siu_capture'):continue
 groups[(row['minecraft']+'/'+row['loader'],row['slug'])].append(row['screen'].replace('.','/'))
rows=[]
for (target,slug),classes in groups.items():
 release=lock['projects'][slug]['targets'][target]
 jar=ROOT/'build/all-target-rollout/upstream'/(release['sha512'][:16]+'-'+release['filename'])
 assert hashlib.sha512(jar.read_bytes()).hexdigest()==release['sha512']
 file=out/('pending-'+target.replace('/','-')+'-'+slug.replace('(','').replace(')','')+'.json')
 subprocess.run(['C:/Program Files/Java/jdk-26/bin/java','-cp',str(out)+';'+cp,'UiConstructionAudit',str(jar),str(file),*classes],check=True)
 data=json.loads(file.read_text());rows.append({'target':target,'slug':slug,'sha512':release['sha512'],'classes':data})
 for name,info in data.items():
  counts={k:len(info[k]) for k in ['allocations','constructor_handles','class_literals','named_strings','subclasses']}
  print(target,slug,name.rsplit('/',1)[-1],counts,flush=True)
(out/'pending-summary.json').write_text(json.dumps(rows,indent=2))
