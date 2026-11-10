"""Record the publisher's newer CurseForge files alongside the older Modrinth mirror."""
from pathlib import Path
import hashlib,json,urllib.request
ROOT=Path(__file__).resolve().parents[1];WORK=ROOT/'build/all-target-rollout'
lock=json.loads((WORK/'availability.json').read_text(encoding='utf-8'))
records=WORK/'publisher-supplemental-inputs.json';rows=json.loads(records.read_text())
for loader,file_id in [('fabric',4838265),('forge',4838266)]:
 filename=f'item-filters-{loader}-2001.1.0-build.59.jar'
 url=f'https://edge.forgecdn.net/files/{file_id//1000}/{file_id%1000}/{filename}'
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'SIU-Compatibility-Rollout/1.0'}),timeout=40) as response:blob=response.read()
 assert blob[:4]==b'PK\x03\x04','Publisher response is not a JAR'
 digest=hashlib.sha512(blob).hexdigest();(WORK/'upstream'/(digest[:16]+'-'+filename)).write_bytes(blob)
 original=lock['projects']['item-filters']['targets'][f'1.20.1/{loader}']
 release={**original,'version':'2001.1.0-build.59','version_id':None,'date':'2023-11-03',
  'filename':filename,'sha512':digest,'sha1':hashlib.sha1(blob).hexdigest(),'download':url,
  'release_url':f'https://www.curseforge.com/minecraft/mc-mods/item-filters/files/{file_id}',
  'source_kind':'official-publisher-file','dependency_baseline_version_id':original.get('dependency_baseline_version_id') or original['version_id'],
  'hash_provenance':'SHA-512 and SHA-1 recorded from the exact HTTPS CurseForge CDN file; not an externally published SHA-512'}
 record={'project':'item-filters','target':f'1.20.1/{loader}','release':release}
 rows=[r for r in rows if (r['project'],r['target'])!=(record['project'],record['target'])]+[record]
 lock['projects']['item-filters']['targets'][record['target']]=release
 print(loader,filename,digest[:20])
records.write_text(json.dumps(rows,indent=2));(WORK/'availability.json').write_text(json.dumps(lock,indent=2,ensure_ascii=False),encoding='utf-8')
