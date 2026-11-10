"""Recheck the retained 1.20.1 release against the latest original 5.8.15 jar."""
from pathlib import Path
import hashlib,json,shutil,urllib.request
ROOT=Path(__file__).resolve().parents[2]
old=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-tech-reborn-tray-fix-01'
new=old.with_name('1.20.1-fabric-integrated-tech-reborn-latest-01');assert not new.exists();new.mkdir()
launch=json.loads((old/'launch.json').read_text());previous=Path(launch['cwd']);runtime=previous.with_name(new.name)
assert not runtime.exists();(runtime/'mods').mkdir(parents=True)
(runtime/'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n')
target=next(r for r in json.loads((ROOT/'output/tech-reborn-all-targets/targets.json').read_text())if r['minecraft']=='1.20.1')
folder=ROOT/'build/tech-reborn-all-targets/1.20.1';meta=folder/'fabric-api-releases.json'
if not meta.exists():meta.write_bytes(urllib.request.urlopen('https://api.modrinth.com/v2/project/fabric-api/version',timeout=45).read())
release=next(r for r in json.loads(meta.read_text())if '1.20.1'in r['game_versions']and r['version_type']=='release')
file=next(f for f in release['files']if f['primary']);api=folder/file['filename']
if not api.exists():api.write_bytes(urllib.request.urlopen(file['url'],timeout=45).read())
assert hashlib.sha512(api.read_bytes()).hexdigest()==file['hashes']['sha512']
staged=json.loads((old/'staged-artifacts.json').read_text())
for row in staged['artifacts']:
 p=Path(row['file'])
 if row['family']=='upstream':p=ROOT/target['techreborn_jar']
 if row['family']=='runtime-api':p=api
 if row['family']=='compatibility':p=ROOT/'dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar'
 shutil.copy2(p,runtime/'mods'/p.name);row.update(file=str(p.resolve()),filename=p.name,sha256=hashlib.sha256(p.read_bytes()).hexdigest(),sha512=hashlib.sha512(p.read_bytes()).hexdigest())
text=(old/'java.args').read_text().replace(str(previous).replace('\\','/'),str(runtime).replace('\\','/')).replace(str(previous),str(runtime))
(new/'java.args').write_text(text)
launch.update(arguments=str(new/'java.args'),cwd=str(runtime),log=str(new/'launch.log'));launch['environment']['NATIVE_REFERENCE_OUTPUT']=str(new)
(new/'launch.json').write_text(json.dumps(launch,indent=2));(new/'staged-artifacts.json').write_text(json.dumps(staged,indent=2))
print('Prepared',new)
