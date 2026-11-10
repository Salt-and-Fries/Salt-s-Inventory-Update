"""Review exact publisher entry code for two non-inventory UI families."""
from pathlib import Path
import hashlib,json,subprocess,zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/all-target-rollout/scope-inspection'
lock=json.loads((ROOT/'build/all-target-rollout/availability.json').read_text())
java='C:/Program Files/Microsoft/jdk-21.0.10.7-hotspot/bin/javap.exe'
rows=[]
for slug in ['small-ships','lets-do-bakery-farmcharm-compat']:
 for target,release in lock['projects'][slug]['targets'].items():
  if slug=='small-ships' and target not in ['1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge']:continue
  if slug!='small-ships' and not target.startswith('1.21.1/'):continue
  jar=ROOT/'build/all-target-rollout/upstream'/(release['sha512'][:16]+'-'+release['filename'])
  assert hashlib.sha512(jar.read_bytes()).hexdigest()==release['sha512']
  archive=zipfile.ZipFile(jar);classes=[n for n in archive.namelist() if n.endswith('.class')]
  row={'target':target,'slug':slug,'sha512':release['sha512']}
  def code(name):
   result=subprocess.run([java,'-p','-c','-classpath',str(jar),name],capture_output=True,text=True,check=True)
   (OUT/(target.replace('/','-')+'-'+slug+'-'+name.rsplit('.',1)[-1]+'.txt')).write_text(result.stdout)
   return result.stdout
  if slug=='small-ships':
   menu=code('com.talhanation.smallships.world.inventory.DockyardMenu')
   screen=code('com.talhanation.smallships.client.gui.screens.inventory.DockyardScreen')
   row['slot_constructor_references']=[line for line in menu.splitlines() if '// class net/minecraft/world/inventory/Slot' in line or '// class net/minecraft/class_1735' in line or '// Method addSlot:' in line]
   assert not row['slot_constructor_references']
   assert 'ContainerData' in menu or 'class_3913' in menu
   assert all(x in screen for x in ['gui.smallships.dockyard.tab.build','gui.smallships.dockyard.tab.modify','NameField','MaterialList','UpgradeList'])
   row['review']='Dockyard is ship building, repair and modification configuration: ship preview, wood/material selection, naming and upgrade lists. Its menu carries synchronized configuration data without mutable inventory cells. The separate ShipContainerScreen cargo inventories remain in the paired screenshot set.'
  else:
   row['screen_or_menu_name_candidates']=[n for n in classes if any(t in n.rsplit('/',1)[-1].lower() for t in ['screen','menu','handler'])]
   # These two similarly named classes are packet registration and the optional
   # external Mod Menu settings entry; neither is a container screen.
   allowed={'net/satisfy/bakery/core/network/PacketHandler.class','net/satisfy/bakery/fabric/core/config/ModMenu.class'}
   assert set(row['screen_or_menu_name_candidates'])<=allowed
   needles=[b'openHandledScreen',b'openMenu',b'method_17355',b'ScreenHandler',b'AbstractContainerMenu',b'net/minecraft/class_1703',b'net/minecraft/class_1720']
   row['menu_open_or_base_references']=[n for n in classes if any(t in archive.read(n) for t in needles)]
   assert not row['menu_open_or_base_references']
   code('net.satisfy.bakery.core.block.SmallCookingPotBlock')
   code('net.satisfy.bakery.core.block.BakerStationBlock')
   row['review']='This publisher release uses direct world/block interactions for the small cooking pot and baker station, including ingredients, tools and jar/bottle collection. Reviewed original block interaction methods and the complete root-class menu-reference inventory provide no container opening. Mod Menu settings remain native. Older Bakery container releases remain in the comparisons.'
  rows.append(row);print(target,slug,'source scope reviewed',flush=True)
(OUT/'dockyard-and-bakery-scope.json').write_text(json.dumps(rows,indent=2))
