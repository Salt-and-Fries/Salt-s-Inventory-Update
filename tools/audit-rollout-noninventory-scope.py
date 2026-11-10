"""Account explicitly excluded UI families against exact publisher binaries.

This never turns an empty class inventory into proof of an absent interface,
and never marks source review as a completed runtime test.
"""
from pathlib import Path
import hashlib,json,zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/all-target-rollout'
matrix=json.loads((OUT/'availability-and-registration.json').read_text())
plan=json.loads((OUT/'ui-evidence-plan.json').read_text())
lock=json.loads((ROOT/'build/all-target-rollout/availability.json').read_text())
families={
 'emi':('recipe-browser overlay','EMI supplies recipe browsing, search and recipe-display overlays. These remain browser interfaces rather than standalone inventory windows.'),
 'jei':('recipe-browser overlay','JEI supplies recipe browsing, ingredient lists and recipe-display overlays. These are accounted for under the existing browser integration, outside the requested inventory/container window comparisons.'),
 'rei':('recipe-browser overlay','REI supplies recipe browsing, ingredient lists and recipe-display overlays. These remain browser interfaces under the existing REI integration.'),
 'raised':('HUD and configuration','Raised repositions HUD layers and supplies layer-selection/editing settings. Its HUD and configuration screens do not become inventory windows.'),
 'modmenu':('full-screen settings','Mod Menu is the mod list and settings entry point. It remains a full-screen settings interface.'),
}
rows=[]
wolf_proof=json.loads((ROOT/'build/all-target-rollout/revampedwolf-publisher-ui-scope.json').read_text())
alloyed_proof=json.loads((ROOT/'build/all-target-rollout/scope-inspection/alloyed-1201-ui-ownership.json').read_text())
entry_proof=json.loads((ROOT/'build/all-target-rollout/scope-inspection/dockyard-and-bakery-scope.json').read_text())
for row in matrix:
 key=f"{row['minecraft']}/{row['loader']}";slug=row['slug']
 if slug=='revampedwolf' and row['minecraft']!='1.20.1':
  proof=next(value for value in wolf_proof if value['target']==key)
  assert proof['sha512']==row['upstream_sha512'] and not proof['ui_classes'] and not proof['menu_open_references']
  category='inventory removed by publisher'
  reason='The publisher removed the wolf inventory in this release. The retained bytecode audit finds no inventory classes or opening references; the newer armor mechanic uses direct item interaction. The 1.20.1 wolf container remains in scope.'
 elif slug=='create-alloyed' and row['minecraft']=='1.20.1':
  proof=next(value for value in alloyed_proof if value['target']==key)
  assert proof['sha512']==row['upstream_sha512'] and not proof['menu_open_references'] and proof['recipe_count']==199
  category='inherited Create processing'
  reason='This original Alloyed release adds recipes, materials and belt-casing extensions, with no separate menu-opening implementation. Its 199 recipe resources use existing processing interfaces. The corresponding Create interfaces are photographed separately; newer Alloyed releases with their own Forge remain in the comparison set.'
 elif slug=='lets-do-bakery-farmcharm-compat' and row['minecraft']=='1.21.1':
  proof=next(value for value in entry_proof if value['target']==key and value['slug']==slug)
  assert proof['sha512']==row['upstream_sha512'] and not proof['menu_open_or_base_references']
  category='direct block interaction and settings'
  reason=proof['review']
 elif slug in families:category,reason=families[slug]
 else:continue
 release=lock['projects'][slug]['targets'][key]
 path=ROOT/'build/all-target-rollout/upstream'/(release['sha512'][:16]+'-'+release['filename'])
 assert hashlib.sha512(path.read_bytes()).hexdigest()==row['upstream_sha512']
 # These known families are explicitly outside the user's inventory scope.
 # Retain actual UI class names as source inventory, not a zero-count inference.
 with zipfile.ZipFile(path) as archive:
  classes=sorted(n.removesuffix('.class').replace('/','.') for n in archive.namelist()
                 if n.endswith('.class') and any(w in n.lower() for w in ['screen','overlay','hud','config']))
 rows.append({'minecraft':row['minecraft'],'loader':row['loader'],'project':row['project'],
              'slug':slug,'version':row['upstream_version'],'sha512':row['upstream_sha512'],
              'publisher_binary':str(path.relative_to(ROOT)).replace('\\','/'),
              'release_url':row['release_url'],'category':category,'reason':reason,
              'ui_class_inventory':classes,
              'verification':'Source inventory and explicit scope classification; not a runtime or gameplay pass.'})
 row['ui_scope']='appendix: '+category
 row['scope_notes']=reason
 row['runtime_status']='appendix: outside inventory scope'

# Shared UI libraries are recorded separately and remain pending until their
# actual consumer windows and all requested variants have been accounted for.
for row in matrix:
 if row['slug'].startswith('sophisticated-core'):
  row['ui_scope']='shared UI library; see Backpack and Storage consumers'
  row['scope_notes']='The library supplies common storage/settings components. Photographs belong to the mod and item/block that opens each actual interface; this is not a no-UI or runtime-pass claim.'

exclusions=[]
construction_path=ROOT/'build/all-target-rollout/ui-construction-audit/summary.json'
construction=json.loads(construction_path.read_text()) if construction_path.exists() else []
spell_choice=json.loads((ROOT/'build/all-target-rollout/scope-inspection/spell-choice-scope.json').read_text())
embedded_ownership=json.loads((ROOT/'build/all-target-rollout/scope-inspection/netherchest-library-ownership.json').read_text())
prominent_scope=json.loads((ROOT/'build/all-target-rollout/scope-inspection/prominent-native-scope.json').read_text())
waystone_settings={'AdminSelectionScreen','ManageWaystoneGroupsScreen','ManageWaystonesScreen',
 'PersonalWaystoneSettingsScreen','SharestoneSelectionScreen','WaystoneEditScreen',
 'WaystoneGroupEditScreen','WaystoneSelectionScreen','WaystoneSettingsScreen'}
for candidate in plan:
 screen=candidate['screen'];name=screen.rsplit('.',1)[-1]
 reason=None
 if candidate['slug']=='waystones' and name in waystone_settings:
  reason='Destination selection and waystone/group configuration remain native, as requested. The separate item-bearing Warp Plate and Waystone Modifier interfaces remain in the inventory comparison scope.'
 elif candidate['slug']=='exposure' and name in {'AlbumScreen','LecternAlbumScreen'}:
  reason='The photo book/album remains native, as explicitly requested. Film editing and other inventory-related Exposure interfaces remain in the comparison scope.'
 elif candidate['slug']=='charm-forked' and name in {'AtlasScreen'}:
  # This atlas is also an item-bearing map/paper editor. Its appearance/name
  # must not classify its actual container slots as a read-only book.
  proofs=json.loads((ROOT/'build/all-target-rollout/scope-inspection/charm-atlas-inventory-scope.json').read_text())
  proof=next(value for value in proofs if value['target']==candidate['minecraft']+'/'+candidate['loader'])
  release=next(value for value in matrix if (value['minecraft'],value['loader'],value['slug'])==(candidate['minecraft'],candidate['loader'],'charm-forked'))
  assert proof['sha512']==release['upstream_sha512']
  candidate['ui_scope']='inventory: map and paper storage'
  candidate['scope_notes']=proof['review']
 elif candidate['slug']=='small-ships' and name=='DockyardScreen':
  proof=next(value for value in entry_proof if value['target']==candidate['minecraft']+'/'+candidate['loader'] and value['slug']=='small-ships')
  release=next(value for value in matrix if (value['minecraft'],value['loader'],value['slug'])==(candidate['minecraft'],candidate['loader'],'small-ships'))
  assert proof['sha512']==release['upstream_sha512'] and not proof['slot_constructor_references']
  reason=proof['review']
 elif candidate['slug']=='prominentbase-embedded' and name in {'LotteryScreen','WeeklyQuestScreen','DialogueScreen'}:
  proof=next(value for value in prominent_scope if value['screen']==name)
  release=next(value for value in matrix if (value['minecraft'],value['loader'],value['slug'])==(candidate['minecraft'],candidate['loader'],'prominentbase-embedded'))
  assert proof['sha512']==release['upstream_sha512']
  reason=proof['review']
 elif candidate['slug']=='spell-engine' and name=='SpellChoiceScreen':
  proof=next(value for value in spell_choice if value['target']==candidate['minecraft']+'/'+candidate['loader'])
  release=next(value for value in matrix if (value['minecraft'],value['loader'],value['slug'])==(candidate['minecraft'],candidate['loader'],'spell-engine'))
  assert proof['sha512']==release['upstream_sha512'] and proof['review']
  reason='The held-item spell chooser is a selection/reader interface: exact publisher bytecode exposes one read-only, non-highlightable preview cell, no mutable inventory cells and no player-inventory grid. It remains native under the inventory-only scope. The separate Bind Spells inventory remains in the comparisons.'
 elif candidate['slug']=='nether-chested' and name in {'ArmorStandEquipmentScreen','LimitlessChestScreen'}:
  proof=next(value for value in embedded_ownership if value['target']==candidate['minecraft']+'/'+candidate['loader'])
  release=next(value for value in matrix if (value['minecraft'],value['loader'],value['slug'])==(candidate['minecraft'],candidate['loader'],'nether-chested'))
  assert proof['sha512']==release['upstream_sha512'] and not any(proof['unrelated_embedded_library_ui_references'].values())
  reason='Embedded shared-library interface, rather than another Nether Chested container. Exact publisher root classes do not reference this screen or activate the Statues UI. Nether Chested\'s concrete NetherChestScreen remains in the comparison set. This does not claim that other consumers of Statues API or Limitless Containers have no UI.'
 elif candidate['slug']=='ae2' and screen in {'appeng.client.gui.AESubScreen','appeng.client.gui.implementations.UpgradeableScreen'}:
  proof=next((value for value in construction if value['target']==candidate['minecraft']+'/'+candidate['loader']),None)
  if proof:
   release=next(value for value in matrix if (value['minecraft'],value['loader'],value['slug'])==(candidate['minecraft'],candidate['loader'],'ae2'))
   assert proof['sha512']==release['upstream_sha512']
   code=proof['classes'][screen.replace('.','/')]
   assert code['present'] and all(not code[field] for field in ['allocations','constructor_handles','class_literals','named_strings'])
   assert all(any(site.startswith(child+'.<init>(') for child in code['subclasses']) for site in code['super_constructor_calls'])
   reason='Shared AE2 base class, rather than another concrete interface. Exact publisher bytecode has only subclass constructor calls and no direct allocation, constructor factory handle, reflective class literal or class-name lookup for this base. Its concrete terminal and machine screens remain in scope.'
 if reason:
  candidate['ui_scope']=('appendix: shared base class' if candidate['slug']=='ae2' else
                         'appendix: embedded shared library' if candidate['slug']=='nether-chested' else
                         'appendix: non-inventory reader or settings')
  candidate['scope_notes']=reason
  exclusions.append({k:candidate[k] for k in ['minecraft','loader','project','slug','version','screen','scope_notes']})

(OUT/'noninventory-scope.json').write_text(json.dumps({'scope':'Explicit non-inventory exclusions, not automated zero-screen classification. Runtime coverage remains separate.','projects':rows,'screen_exclusions':exclusions},indent=2))
(OUT/'availability-and-registration.json').write_text(json.dumps(matrix,indent=2))
(OUT/'ui-evidence-plan.json').write_text(json.dumps(plan,indent=2))
print(json.dumps({'appendix_project_targets':len(rows),'explicit_reader_or_settings_candidates':len(exclusions),'runtime_passes_added':0}))
