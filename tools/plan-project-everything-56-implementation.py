"""Freeze approved native-region recipes as live API profiles, never as screenshot textures.

This catalog is deliberately 1.20.1-only. Reference evidence stays outside the JAR.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
read = lambda p: json.loads((ROOT / p).read_text(encoding="utf-8-sig"))
design = read("output/project-everything-56-design/design-manifest.json")
plan = read("output/project-everything-56-design/native-reference-plan.json")
coverage = {r["id"]: r for r in read("output/remaining-ui-audit/coverage.json")["cases"]}
entries = {r["entry"]: r for r in read("output/remaining-ui-audit/inventory.json")["entries"]}
native = {r["id"]: r for r in plan["cases"]}
excluded = {"item-exposure-album", "own-exposure-lectern", "bf-waystone", "own-bf-sharestone-real-aligned"}
profiles = {}
test_views = {}
matrix = []
for item in design["cases"]:
    key = item["id"]
    row = native.get(key)
    if not row:  # Supplemental states exercise their parent profile.
        continue
    state = row["native_state"]
    screen = state["screen"]
    before = coverage.get(key, {}).get("core") or {}
    menu_id = before.get("menu_id")
    if key in ('smallships-brigg','ag-entity-galley'):menu_id='minecraft:ship_container'
    if key=='bb-spawn-hamster':menu_id='salts_inventory_compat:hamster'
    if key=='bf-moa-clean':menu_id='salts_inventory_compat:moa'
    if key=='bf-forge-guard':menu_id='salts_inventory_compat:guard'
    if key=='bf-wolf-isolated':menu_id='salts_inventory_compat:wolf'
    if key=='pr-tierify-reforge':menu_id='tiered:reforge'
    menu_id={
        'own-bf-bag-of-spoils-aligned':'blue_skies:bag_of_spoils',
        'own-bf-camel-real-aligned':'blue_skies:mount_inventory',
        'own-bf-umvuthi-aligned':'mowziesmobs:umvuthi_trade',
        'own-bf-minion-trade-real':'mowziesmobs:umvuthana_trade',
    }.get(key,menu_id)
    disposition = "native" if key in excluded else "profile"
    if screen.startswith(("net.minecraft.", "appeng.")) or key == "own-supp-red-merchant":
        disposition = "existing" if key not in excluded else "native"
    matrix.append(dict(id=key, family=row["family"], loader=row["loader"], disposition=disposition,
                       screen=screen, menu=menu_id, reference=item["native_capture"]))
    if disposition != "profile":
        continue
    q = item["recipe"]
    rect = q.get("rect")
    chunks = q.get("chunks") or ([[rect, [0, 0]]] if rect else [])
    if not chunks:
        continue
    regions = [dict(source=list(r), at=list(at), cutouts=[]) for r, at in chunks]
    if key=='item-candlelight-letter-open':regions[1]['source']=[88,8,149,42]
    # Native artwork extends beyond the 18-pixel interactive cells. Preserve
    # complete silhouettes, heater bases and oversized decorative slot frames.
    canvas=q.get("size") or [0,0]
    if key=='meadow-cooking-cauldron':
        regions=[dict(source=[5,25,25,43],at=[0,25],cutouts=[]),
                 dict(source=[28,8,149,66],at=[25,0],cutouts=[])]
        canvas=[146,58]
    if key=='meadow-fondue':
        regions=[dict(source=[35,7,141,70],at=[0,0],cutouts=[])]
        canvas=[106,63]
    if key=='meadow-woodcutter':
        regions.append(dict(source=[140,30,164,54],at=[108,15],cutouts=[]))
    if screen=='net.satisfy.vinery.client.gui.FermentationBarrelGui':
        regions=[dict(source=[5,25,25,43],at=[0,19],cutouts=[]),
                 dict(source=[27,14,149,72],at=[20,0],cutouts=[])]
        canvas=[142,58]
    if key=='item-archon-experience-pouch':
        regions=[dict(source=[6,3,224,103],at=[0,0],cutouts=[])]
        canvas=[218,100]
    if key=='item-wands-palette':
        # Wands draws these controls itself, outside Screen.children(). Keep
        # the whole row, including the spinner shown only in Gradient mode.
        regions=[dict(source=[49,3,151,17],at=[0,0],cutouts=[]),
                 dict(source=[7,17,169,126],at=[0,18],cutouts=[])]
        canvas=[162,127]
    if key=='exposure-film-rename':
        regions=[dict(source=[3,15,21,33],at=[0,0],cutouts=[]),
                 dict(source=[28,17,178,33],at=[25,1],cutouts=[]),
                 dict(source=[132,41,174,62],at=[133,25],cutouts=[])]
        canvas=[175,46]
    if key=='bf-gallery-painting-selector':
        regions[0]['source']=[11,8,169,116]
    if key=='own-bf-minion-trade-real':
        regions[1]['source']=[69,18,171,76]
    if key=='spell-engine-spell-binding':
        regions[0]['source']=[13,13,169,73]
    if key=='ad-astra-giselle-addon-gravity-normalizer':
        regions=[dict(source=[5,17,173,114],at=[0,0],cutouts=[])]
    if key=='exposure-lightroom':
        regions[1]['source']=[4,14,172,111]  # Complete rounded filmstrip border, not just its three inner cells.
        regions.append(dict(source=[-19,91,-1,109],at=[0,78],cutouts=[]))
    # Complete native cells, including lower decorative slot edges, take precedence
    # over a proposal's crop. The runtime checks the same invariant for changed states.
    for slot in state.get("slots", []):
        if slot.get("player"):
            continue
        cell = [slot["x"]-1, slot["y"]-1, slot["x"]+17, slot["y"]+17]
        def overlap(region):
            r=region['source']
            return max(0,min(r[2],cell[2])-max(r[0],cell[0]))*max(0,min(r[3],cell[3])-max(r[1],cell[1]))
        chosen=max(regions,key=overlap)
        if overlap(chosen):
            r=chosen['source'];dx,dy=max(0,r[0]-cell[0]),max(0,r[1]-cell[1])
            chosen['source']=[min(r[0],cell[0]),min(r[1],cell[1]),max(r[2],cell[2]),max(r[3],cell[3])]
            chosen['at']=[chosen['at'][0]-dx,chosen['at'][1]-dy]
            for other in regions:
                if other is not chosen and overlap(other):other['cutouts'].append(cell)
    # Rebase expansions without changing the relative distances between chunks.
    minx = min(0, min(r["at"][0] for r in regions))
    miny = min(0, min(r["at"][1] for r in regions))
    for r in regions:
        r["at"] = [r["at"][0]-minx,r["at"][1]-miny]
    profile = dict(id=key, mods=entries[row["entry"]]["mod_ids"], screen=screen,
                   menu=menu_id, regions=regions, canvas=canvas,
                   title={'meadow-cheese-form':'Cheese Form','meadow-cooking-cauldron':'Cooking Cauldron','meadow-fondue':'Fondue','item-candlelight-letter-open':'Letter'}.get(key,state.get('title') or row['label']),
                   color=([132,132,132] if key=='item-wands-magic-bag-1' else [77,74,78] if screen.startswith('ad_astra_giselle_addon.') else [198,198,198] if key=='item-archon-experience-pouch' else q.get("bg", [198,198,198])),
                   darkFrame=screen.startswith('ad_astra_giselle_addon.') or key=='item-wands-magic-bag-1', clean=[], recipeBook=(row["entry"] in [7,9,10,11,13,14]),
                   machineUtility=q.get("machine_utility_strip",False))
    test_views.setdefault(screen,dict(id=key,screen=screen,bounds=state.get("bounds"),slots=state.get("slots",[]),widgets=state.get("widgets",[])))
    # Different blocks can use exactly the same native class and menu (e.g. kettles).
    previous = profiles.get(screen)
    if previous:
        previous["mods"] = sorted(set(previous["mods"]+profile["mods"]))
    else:
        profiles[screen] = profile

clean = {
    "villagersplus-alchemist-table": [[7,72,64,82]],
    "blue-skies-food-prep-table": [[7,72,61,82]],
    "ad-astra-giselle-addon-gravity-normalizer": [[0,102,91,114],[0,113,174,114]],
    "herbalbrews-copper-tea-kettle": [[56,5,140,15]],
    "herbalbrews-tea-kettle": [[56,5,140,15]],
    "bf-gallery-painting-selector": [[8,5,78,15]],
    "blue-skies-alchemy-table": [[156,71,170,83]],
}
for p in profiles.values():
    p["clean"] = clean.get(p["id"], [])

# The supplementary wand-tool design intentionally retains the inventory: its
# cells select tool sources, rather than transferring item stacks.
tool = next(r for r in design['cases'] if r['id']=='design-wands-tools-request')
tool_view = read('output/project-everything-56-design/wands-tool-native-state.json')
profiles[tool_view['screen']] = dict(id=tool['id'],mods=['wands'],screen=tool_view['screen'],
    menu='wands:wand_menu',title='Wand tools',regions=[dict(source=r,at=at,cutouts=[]) for r,at in tool['recipe']['chunks']],
    canvas=tool['recipe']['size'],color=[198,198,198],clean=[],recipeBook=False,machineUtility=False,playerSlots=True)
test_views[tool_view['screen']] = dict(id=tool['id'],screen=tool_view['screen'],bounds=tool_view['bounds'],
    slots=tool_view['slots'],widgets=tool_view.get('widgets',[]))

observed_path=ROOT/'output/project-everything-56-implementation/observed-menu-ids.json'
observed=json.loads(observed_path.read_text()) if observed_path.exists() else {}
for p in profiles.values():
    p['menus']=sorted(set(([p['menu']] if p.get('menu') else [])+list(observed.get(p['screen'],{}))))
    if p['id']=='pr-tierify-reforge':p['menus']=['tiered:reforge']
    if p['menus']:p['menu']=p['menus'][0]

target=ROOT/"compatibility/versions/1.20.1/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json"
target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(json.dumps(list(profiles.values()),indent=2)+"\n",encoding="utf-8")
tests=ROOT/"compatibility/versions/1.20.1/fabric/src/test/resources/reviewed-native-reference-geometry.json"
tests.parent.mkdir(parents=True,exist_ok=True)
tests.write_text(json.dumps(list(test_views.values()),indent=2)+"\n",encoding="utf-8")
out=ROOT/"output/project-everything-56-implementation"
out.mkdir(parents=True,exist_ok=True)
(out/"coverage-plan.json").write_text(json.dumps(matrix,indent=2)+"\n",encoding="utf-8")
print(f"{len(profiles)} live native profiles; {len(matrix)} reference views accounted for")
