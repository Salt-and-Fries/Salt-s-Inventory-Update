"""Retain the exact embedded publisher binary and its official pack provenance."""
from pathlib import Path
import hashlib,io,json,shutil,zipfile

ROOT=Path(__file__).resolve().parents[1];WORK=ROOT/'build/all-target-rollout'
releases=json.loads((WORK/'metadata/project-prominence-ii-pack-versions.json').read_text())
pack=releases[0];file=next(f for f in pack['files'] if f['primary'])
archive=WORK/'upstream'/file['filename']
assert hashlib.sha512(archive.read_bytes()).hexdigest()==file['hashes']['sha512']
with zipfile.ZipFile(archive) as z:
    outer_name=next(n for n in z.namelist() if n.startswith('overrides/mods/Prominent-GLOBAL-') and n.endswith('.jar'))
    with zipfile.ZipFile(io.BytesIO(z.read(outer_name))) as outer:
        inner_name=next(n for n in outer.namelist() if n.startswith('META-INF/jars/prominentbase-') and n.endswith('.jar'))
        data=outer.read(inner_name)
with zipfile.ZipFile(io.BytesIO(data)) as z:meta=json.loads(z.read('fabric.mod.json'))
digest=hashlib.sha512(data).hexdigest();filename=Path(inner_name).name
cached=WORK/'upstream'/(digest[:16]+'-'+filename);cached.write_bytes(data)
assert pack['game_versions']==['1.20.1'] and 'fabric' in pack['loaders']
slug='prominentbase-embedded';version_id='embedded-'+pack['id']+'-'+meta['version']
source=file['url']+'!/'+outer_name+'!/'+inner_name
release={'version_id':version_id,'version':meta['version'],'date':pack['date_published'],
         'filename':filename,'sha512':digest,'sha1':hashlib.sha1(data).hexdigest(),
         'download':source,'release_url':'https://modrinth.com/modpack/prominence-2-fabric/version/'+pack['id'],
         'source_kind':'publisher-embedded-jar','dependencies':[],
         'provenance':{'pack_version':pack['version_number'],'pack_sha512':file['hashes']['sha512'],
                       'pack_download':file['url'],'archive_path':[outer_name,inner_name],
                       'native_dependencies':meta.get('depends',{}),
                       'note':'Exact publisher nested JAR; support limited to the pack-declared Minecraft and loader.'}}
record={'project':slug,'project_id':'EGs3lC8D','slug':slug,'title':'Prominent Base (embedded)',
        'mod_ids':['prominentbase'],'evidence':['Exact official pack archive and both unmodified nested JAR descriptors'],
        'targets':{'1.20.1/fabric':release}}
manifest=WORK/'publisher-embedded-inputs.json'
rows=json.loads(manifest.read_text()) if manifest.exists() else []
rows=[r for r in rows if r['project']!=slug]+[record];manifest.write_text(json.dumps(rows,indent=2))
lock_path=WORK/'availability.json';lock=json.loads(lock_path.read_text());lock['projects'][slug]=record
lock['mod_projects']['prominentbase']=[slug];lock['embedded_source_manifest']=str(manifest.relative_to(ROOT))
lock['unmapped_integration_mod_ids']=[m for m,names in lock['mod_projects'].items() if not names]
lock_path.write_text(json.dumps(lock,indent=2,ensure_ascii=False))
# The normal fixture resolver reads its release catalog from this cache. Native
# dependency setup remains explicit; nested libraries retain publisher bytes.
version={'id':version_id,'project_id':'EGs3lC8D','version_number':meta['version'],
         'game_versions':['1.20.1'],'loaders':['fabric'],'version_type':'release','dependencies':[],
         'files':[{'primary':True,'filename':filename,'url':source,'hashes':{'sha512':digest,'sha1':release['sha1']}}]}
(WORK/('metadata/project-'+slug+'.json')).write_text(json.dumps({'id':'EGs3lC8D','slug':slug,'title':record['title']}))
(WORK/('metadata/project-'+slug+'-version.json')).write_text(json.dumps([version],indent=2))
print('Registered',record['title'],meta['version'],'1.20.1/fabric',digest)
print('Unmapped integration IDs:',lock['unmapped_integration_mod_ids'])
