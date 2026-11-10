"""Persist a bounded, reproducible addon coverage audit and implementation queue."""
from pathlib import Path
from datetime import datetime,timezone
import csv,hashlib,json,re

ROOT=Path(__file__).resolve().parents[1]
BUILD=ROOT/'build/create-addon-coverage-audit'
OUT=ROOT/'output/create-addon-coverage-audit'
KNOWN={'create','create-fabric','create-big-cannons','create-steam-n-rails','create-shuffle-filter'}
SHARED={
 'create-connected':'Changes existing Create features, including Attribute Filter; test with the addon present.',
 'create-connected-fabric':'Fabric port of Connected; test shared Create screens on a matching release.',
 'create-vibrant-vaults':'Color/variant regression coverage for existing Create package-port and requester UIs; no new screen class is required to change their appearance.',
 'create-contraption-terminals':"Uses Tom's Simple Storage terminals on moving contraptions; verify the owning mod integration and source context.",
 'create-sophisticated-backpacks-compat':'Separate bridge project; test placed/mounted backpack opening with the owning Sophisticated integration.',
 'interactive':'Contraption/container source-context regression; no dedicated screen declaration found.',
 'create-clockwork':'Physics/contraption source-context regression; no dedicated screen declaration found.',
 'create-cardboarded-conveynience':'Test shared package/container interactions; absence of a declared screen does not establish compatibility.',
 'create-fast-schematic-cannon':'Modifies a shared Schematicannon path; verify native-frame layout and widgets with the addon present.',
 'create-blueprint-tweaks':'Modifies shared Blueprint behavior; verify the native frame with this addon present.',
 'create-compatible-storage':'Storage/contraption bridge; requires owning-container regression checks.',
 'storage-drawers-create-compat':'Uses the owning drawer interface; verify mounted/source behavior rather than inventing another UI.',
}
SCOPE_ONLY={
 'create-ore-excavation':'Ore Vein Atlas is a map/navigation UI; account for it without assuming it needs an inventory window.',
 'aeroclaims':'Claim settings are outside the inventory/container scope despite their menu-backed implementation.',
}
LEGACY={
 'fxnt-create-storage':'Original Fabric 0.20 pins Create 0.5.1-f-build.1417; separate from the current Forge port and the Create 6 runtime.',
 'create-enchantment-industry-fabric-legacy':'Deprecated Fabric branch; keep separate from the current Enchantment Industry project.',
 'create-vintage-improvements':'Original older branch. The separate Create: Vintage project declares Create 6 support.',
 'create-northstar':'Older original release; keep separate from Northstar Redux.',
}
PRIORITY=['numismatics','create-storage-neo-forge','create-stuff-additions','create-utilities','create-central-kitchen','create-diesel-generators','create-tweaked-controllers','create-tfmg','createnuclear','create-food','create-big-cannons-advanced-technologies','create-vintage-unofficial-port','create-sound-of-steam','create-additional-logistics','create_factory_logistics','create-mobile-packages','sophisticated-backpacks-create-integration','sophisticated-storage-create-integration']
NOTES={
 'create-central-kitchen':'Cooking Guide and Miner\'s Cooking Guide are editable ingredient-filter menus: addSlots creates six/four ghost ingredient cells plus player inventory. Do not exclude these solely because their names contain "Guide".',
 'numismatics':'Vendor, sale points, depositors and banking require native UI work. Trust/subaccount lists are configuration states; assess inventory relevance per view.',
 'northstar-redux':'Astronomy/rocket menus need visual scope review; Space Atlas and telescope/navigation controls must not be converted merely because of their Java superclass.',
 'create-tfmg':'Engine Controller has six ghost frequency cells plus player inventory, confirmed by addSlots bytecode. Electrician\'s Wrench is a separate non-container control.',
 'create-mobile-packages':'Includes mobile packager/edit, portable stock ticker and Bee Port paths. Publisher marks 1.20.1 Forge no longer supported, but its release remains available.',
 'create-stock-bridge':'AEStockBridgeScreen extends AEBaseScreen; assess existing AE integration inheritance and native registration before adding another renderer.',
 'create-the-air-war':'Published GUI classes use opaque generated names; in-game opening and translation lookup are required to identify the actual menus.',
 'createlazytick':'Performance/configuration tool screens are menu-backed; visually assess whether any item filter needs a window, and keep pure settings outside inventory scope.',
 'create-biotech':'Wireless stockkeeper and courier/port variants require checks for both new menus and shared Create behavior.',
 'sophisticated-backpacks-create-integration':'Declares its own mounted backpack and settings menus/screens; do not assume a generic Sophisticated screen match covers them.',
 'sophisticated-storage-create-integration':'Declares mounted storage/limited-barrel and settings menus/screens; existing owning-mod coverage must be verified.',
}

def write(p,data):p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def main():
    OUT.mkdir(parents=True,exist_ok=True);source=json.loads((BUILD/'release-ui-scan.json').read_text());assert len(source)==270 and not any(r.get('error') for r in source)
    rows=[]
    for p in source:
        releases=p['releases'];containers=sorted({s['class'] for r in releases for s in r['screens'] if s['container_hierarchy'] and not s['class'].rsplit('/',1)[-1].startswith('Abstract')})
        menus=sorted({s['class'] for r in releases for s in r['menus'] if not s['class'].rsplit('/',1)[-1].startswith(('Abstract','GhostItemMenu','HeldItemGhostItemMenu'))})
        other=sorted({s['class'] for r in releases for s in r['screens'] if not s['container_hierarchy'] and s['parent']!='java/lang/Object'})
        slug=p['slug'];note=NOTES.get(slug,'')
        if slug in KNOWN:status='implemented_base_or_shared_menu'
        elif slug=='create-fabric-sodium-fix':status='alternate_older_core_fork';note='Alternative older Create distribution, not a new addon UI or a tested Create 6 target.'
        elif slug in SCOPE_ONLY:status='appendix_scope_review';note=SCOPE_ONLY[slug]
        elif containers or menus:status='pending_addon_ui_review'
        elif slug in SHARED:status='shared_ui_regression_pending';note=SHARED[slug]
        elif other:status='native_controls_appendix'
        elif not releases:status='no_selected_fabric_forge_release'
        else:status='no_declared_custom_ui_in_selected_releases'
        if slug in LEGACY:note=(note+' '+LEGACY[slug]).strip()
        variants=[]
        for r in releases:
            constraints=[]
            for loader,m in r['declared_metadata'].items():
                if loader=='fabric' and m.get('create') is not None:constraints.append('fabric: '+str(m['create']))
                elif isinstance(m.get('create'),list):constraints += [loader+': '+d['versionRange'] for d in m['create']]
            variants.append({'version':r['version'],'version_id':r['version_id'],'published':r['date_published'],'loaders':r['loaders'],'create_constraints':constraints,'jar_url':r['url'],'sha512':r['sha512'],'local_evidence_jar':r['file']})
        rows.append({'name':p['name'],'slug':slug,'downloads_snapshot':p['downloads'],'status':status,'priority':PRIORITY.index(slug)+1 if slug in PRIORITY else None,'legacy_or_version_separate':slug in LEGACY,
            'container_backed_screen_candidates':containers,'declared_menu_candidates':menus,'other_screen_declarations':other,'notes':note,'project_url':p['project_url'],'source_url':p.get('source_url'),'releases':variants})
    candidates=[r for r in rows if r['status']=='pending_addon_ui_review'];appendix=[r for r in rows if r['status']=='native_controls_appendix'];shared=[r for r in rows if r['status']=='shared_ui_regression_pending']
    summary={'checked_at_utc':datetime.now(timezone.utc).isoformat(),'minecraft':'1.20.1','implemented_create_line':'6.0.8 / Fabric port 6.0.8.1 build 1744','discovery':'Top 400 Modrinth search hits for Create on 1.20.1, filtered to 269 Create-related project titles, plus explicit Vault UI lookup; primary CurseForge/publisher cross-checks.',
        'projects_release_scanned':len(rows),'release_jars_inspected':sum(len(p['releases']) for p in source),'pending_addon_project_entries':len(candidates),'shared_ui_regressions':len(shared),'other_screen_only_project_entries':len(appendix),'coverage_complete':False,
        'method':'Static superclass/menu declarations and publisher release metadata; JAR SHA-512 verified. Newest non-alpha Fabric/Forge release per project, or alpha if no non-alpha release exists. Published beta releases count. No JAR executed.',
        'limits':['This is a bounded discovery audit, not proof that every Create addon ever released was found. Download counts are a selection aid, not an official endorsement.','Container-screen inheritance is a candidate signal, not proof every view is inventory-related. Read-only books/maps and pure configuration remain outside the agreed window scope.','No new addon was launched or functionally tested in this audit. A pending native design does not imply SIU fallback is broken.','Declared version ranges do not prove Create 6 binary compatibility, especially old releases with unbounded minimum constraints.','A project with no declared custom screen may reuse or modify another mod\'s UI; this is not a compatibility pass.','The installed game/modpack was not changed; no public API or runtime implementation changed.']}
    protection={'name':'Create: Protection Pixel','status':'publisher_documented_ui_pending_binary_review','project_url':'https://www.curseforge.com/minecraft/mc-mods/protection-pixel','target_release':'protection_pixel-2.2.1-forge-1.20.1.jar','interfaces':['Armor Load Platform menu','Combustion reactor equipment controls/HUD; distinguish inventory/configuration from overlay'],'evidence':'Publisher description explicitly documents the Armor Load Platform menu; release JAR not included in the 270-project scan.'}
    write(OUT/'coverage.json',{'summary':summary,'additional_publisher_candidate':protection,'projects':rows});write(OUT/'implementation-queue.json',{'scope':'Inventory-related native addon windows first; verify view relevance and release compatibility before implementation','projects':sorted(candidates,key=lambda r:(r['priority'] is None,r['priority'] or 999,-r['downloads_snapshot'])),'publisher_candidates':[protection],'shared_regressions':shared})
    with (OUT/'coverage.csv').open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.writer(f);w.writerow(['Mod','Status','Priority','Container-backed UI candidates','Menu candidates','Other UI','Selected releases','Create requirements','Notes','Publisher'])
        for r in sorted(rows,key=lambda r:-r['downloads_snapshot']):w.writerow([r['name'],r['status'],r['priority'] or '',', '.join(s.rsplit('/',1)[-1] for s in r['container_backed_screen_candidates']),', '.join(s.rsplit('/',1)[-1] for s in r['declared_menu_candidates']),', '.join(s.rsplit('/',1)[-1] for s in r['other_screen_declarations']),'; '.join(v['version']+' ('+'/'.join(v['loaders'])+')' for v in r['releases']),'; '.join(c for v in r['releases'] for c in v['create_constraints']),r['notes'],r['project_url']])
    doc=['# Create addon UI coverage - Minecraft 1.20.1','',
        'The native Create rollout covers its retained 20 base/CBC/Rails menu types. It does **not** establish complete community-addon coverage. This follow-up checked publisher metadata and '+str(summary['release_jars_inspected'])+' original JARs across 270 projects without installing or executing them. '+str(len(candidates))+' additional project entries have container-backed screen/menu candidates requiring review. This includes alternative branches and mounted third-party integrations; it is not a count of unique new inventory windows.','',
        'The full machine-readable inventory, selected versions, loaders, Create requirements, exact class names, publisher links and release hashes are saved under `output/create-addon-coverage-audit/`. The implementation queue is `implementation-queue.json`, with a spreadsheet-readable `coverage.csv`.','',
        '## First native-window review batch','',
        '| Addon | Declared screen/menu candidates | Release notes |','|---|---|---|']
    for slug in PRIORITY:
        r=next(x for x in rows if x['slug']==slug);names=[s.rsplit('/',1)[-1].removesuffix('Screen') for s in r['container_backed_screen_candidates']] or [s.rsplit('/',1)[-1] for s in r['declared_menu_candidates']]
        note=r['notes'] or 'Original UI capture, native-style design and focused gameplay acceptance still required.'
        doc.append('| ['+r['name']+']('+r['project_url']+') | '+', '.join(names)+' | '+note+' |')
    doc += ['', '## Additional UI candidates','']
    for r in sorted(candidates,key=lambda r:-r['downloads_snapshot']):
        if r['slug'] in PRIORITY:continue
        names=', '.join(s.rsplit('/',1)[-1] for s in r['container_backed_screen_candidates']) or ', '.join(s.rsplit('/',1)[-1] for s in r['declared_menu_candidates'])
        doc.append('- ['+r['name']+']('+r['project_url']+'): '+names+'. '+r['notes'])
    doc += ['', '## Publisher-only follow-up','',
        '[Create: Protection Pixel](https://www.curseforge.com/minecraft/mc-mods/protection-pixel) documents an Armor Load Platform menu in its 1.20.1 Forge release. Its UI still needs a release-level/in-game review; it is additional to the 270 scanned projects.',
        'Create Robotics has a public repository, but no applicable 1.20.1 release was established in this check. Keep it on a version-verification watchlist instead of claiming coverage.',
        '', '## Shared screens and visual variants','',
        'The following have no separately declared menu requiring a new template in the scanned JARs, but need regression checks with their owning UI. They are not marked compatible merely because the base Create screen is covered.','']
    for r in shared:doc.append('- ['+r['name']+']('+r['project_url']+'): '+r['notes'])
    doc += ['', 'Sophisticated Backpacks/Storage Create Integration are in the main pending queue because they actually declare mounted menus/screens. Their settings variants must also be accounted for.',
        '', '## Controls, books and version boundaries','',
        'Connected/Pulse Generator, Railways Navigator, Radars, Encased/Brass Shaft, Ender Transmission, Extra Gauges, Optical/Hologram Source, track/door/bogey selectors and other configuration-only screens remain native under the agreed inventory-only scope. The CSV records the exact classes. An Ore Vein Atlas or claim settings screen is not made inventory-related by extending a container superclass.',
        '', 'Central Kitchen is a useful exception to name-based filtering: its Cooking Guide and Miner\'s Cooking Guide menus create ingredient ghost cells plus player inventory. They are editable filter/configuration menus, not merely read-only recipe-book pages. Their visual design still needs an original in-game capture.',
        '', 'Original Fabric Create: Storage 0.20 pins Create 0.5.1-f-build.1417. The Forge Create: Storage port is a separate release line supporting Create 6. Likewise distinguish original Vintage Improvements from the newer Create: Vintage port, and original Northstar from Northstar Redux. Do not install all branches in one runtime.',
        '', '## Verification limits','']
    doc += ['- '+s for s in summary['limits']]
    text='\n'.join(doc)+'\n';(ROOT/'docs/markdown/create-addon-ui-coverage-1.20.1.md').write_text(text,encoding='utf-8');(OUT/'README.md').write_text(text,encoding='utf-8')
    write(OUT/'verification.json',{'summary':summary,'raw_scan_sha256':hashlib.sha256((BUILD/'release-ui-scan.json').read_bytes()).hexdigest(),'registered_create_menu_types':len(re.findall(r'Map.entry\("', (ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/create/CreateWindows.java').read_text())),'publisher_evidence_jar_digests_present':all(v['sha512'] for p in rows for v in p['releases'])})
    assert len(rows)==270 and len({r['slug'] for r in rows})==270
    print(json.dumps(summary,indent=2))

if __name__=='__main__':main()
