"""Prepare exact upstream inventory fixtures and current platform runtime pins."""
from pathlib import Path
import argparse,json,subprocess,sys,urllib.request,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];WORK=ROOT/'build/all-target-rollout'
PROJECTS=['ae2','animal_feeding_trough','bartering-station','better-archeology','betterend','building-wands',
 'curios','easy-anvils','easy-magic','farmers-delight-refabricated','farmers-delight','guard-villagers',
 'guard-villagers-fabricquilt','illager-invasion','immersive-aircraft','nether-chested','niftycarts','revampedwolf','runes',
 'sophisticated-backpacks','sophisticated-storage','sophisticated-backpacks-unoffical-fabric-port',
 'sophisticated-storage-unofficial-fabric-port','spell-engine','the-bumblezone','the-bumblezone-fabric',
 'the-graveyard-fabric','the-graveyard-forge','toms-storage','toms-trading-network','trading-post',
 'transmog','travelersbackpack','universal-graves','universal-sawmill','villagersplus','visual-workbench','waystones']
PROJECT_GROUPS={
 'inventory':PROJECTS,
 'themed':['lets-do-bakery-farmcharm-compat','lets-do-candlelight-farmcharm-compat','lets-do-farm-charm',
  'lets-do-herbalbrews','lets-do-meadow','lets-do-nethervinery','lets-do-vinery','archon-renewed','charm-forked',
  'exposure','galosphere','item-filters','supplementaries','tieredz','tierify','spawn-mod','small-ships','mythic-mounts',
  'blue-skies','aether-protect-your-moa','deep-aether','gallery','mowzies-mobs','stalwart-dungeons'],
 'specialized':['accessories','bountiful','chipped','inmis','ironchests','extendeddrawersaddon','simply-swords',
  'minecells','brick-and-mortar','expanded-delight','crabbers-delight','farmers-delight','farmers-delight-refabricated','fabric-seasons-extras','smitherz','oxidized'],
 'create':['create','create-fabric','create-alloyed','create-big-cannons','create-steam-n-rails','create-steam-n-rails-1.21.1'],
 'ae2-addons':['ae2','applied-botanics','botania','mega','merequester','ad-astra-giselle-addon'],
}
PROJECT_GROUPS['create-and-ae2']=list(dict.fromkeys(PROJECT_GROUPS['create']+PROJECT_GROUPS['ae2-addons']))
PROJECT_GROUPS['remaining-reviewed']=['supplementaries','revampedwolf','gallery','galosphere',
 'l_enders-cataclysm','miners-delight','aether-protect-your-moa','additionz','tieredz','extendeddrawersaddon']

def fetch(url):
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'SIU-Compatibility-Rollout/1.0'}),timeout=40) as r:return r.read()

def main():
 p=argparse.ArgumentParser();p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True)
 p.add_argument('--omit-project',action='append',default=[]);p.add_argument('--include-project',action='append',default=[]);p.add_argument('--name');p.add_argument('--group',choices=PROJECT_GROUPS,default='inventory');a=p.parse_args()
 key=a.minecraft+'/'+a.loader;lock=json.loads((WORK/'availability.json').read_text())
 projects=[name for name in dict.fromkeys([*PROJECT_GROUPS[a.group],*a.include_project]) if name not in a.omit_project and key in lock['projects'].get(name,{}).get('targets',{})]
 name=a.name or a.minecraft.replace('.','')+'-'+a.loader+'-'+a.group+'-batch'
 args=[sys.executable,'-X','utf8',str(ROOT/'tools/prepare-rollout-fixture.py'),'--minecraft',a.minecraft,'--loader',a.loader,'--name',name]
 if a.minecraft=='1.20.1':args+=['--dependency-project','puzzles-lib']
 if 'accessories' in projects and a.loader in ['forge','neoforge']:args+=['--dependency-project','cloth-config']
 if a.minecraft=='1.21.1' and a.loader=='neoforge' and any('aether' in name for name in projects):
  # Aether embeds Accessories beta.16, whose common networking references
  # Fabric PlayerLookup on NeoForge. Use the current original Accessories
  # release satisfying its declared JarJar range, without editing either JAR.
  args+=['--dependency-project','accessories','--dependency-project','cloth-config']
 if 'bountiful' in projects:args+=['--dependency-project','kambrik']
 if 'bountiful' in projects and a.loader=='fabric':args+=['--dependency-project','fabric-language-kotlin']
 if 'ad-astra-giselle-addon' in projects and a.minecraft=='1.21.1':
  for dependency in ['ad-astra','common-storage-lib','resourceful-config']:
   args+=['--dependency-project',dependency]
 if 'universal-graves' in projects:args+=['--dependency-project','polymer']
 if a.minecraft=='26.1.2' and any(p.startswith('the-bumblezone') for p in projects):args+=['--dependency-project','resourceful-lib']
 for project in projects:args+=['--project',project]
 subprocess.run(args,cwd=ROOT,check=True)
 path=WORK/'fixtures'/name/'fixture.json';data=json.loads(path.read_text());runtime=data.setdefault('runtime',{})
 if a.loader=='fabric':
  raw=fetch('https://api.modrinth.com/v2/project/fabric-api/version?game_versions='+urllib.parse.quote(json.dumps([a.minecraft]))+'&loaders=%5B%22fabric%22%5D')
  (WORK/'metadata'/('fabric-api-'+a.minecraft+'-batch-versions.json')).write_bytes(raw)
  releases=json.loads(raw);chosen=next((r for r in releases if r['version_type']=='release'),releases[0])
  runtime['fabric_api']=chosen['version_number'];runtime['fabric_loader']='0.19.5'
 elif a.loader=='neoforge':
  raw=fetch('https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml')
  (WORK/'metadata/neoforge-batch-maven-metadata.xml').write_bytes(raw)
  prefix={'1.21.1':'21.1.','1.21.11':'21.11.'}.get(a.minecraft,a.minecraft+'.')
  matching=[x.text for x in ET.fromstring(raw).findall('.//version') if x.text.startswith(prefix)]
  runtime['neoforge']=matching[-1]
 data['runtime_pin_reason']='Current exact-game-version official platform releases, selected separately from unchanged release compile baselines.'
 path.write_text(json.dumps(data,indent=2));print(json.dumps({'fixture':str(path),'projects':projects,'runtime':runtime},indent=2))

if __name__=='__main__':main()
