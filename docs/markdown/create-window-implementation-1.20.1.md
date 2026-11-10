# Create-family window implementation - Minecraft 1.20.1

Implemented and verified on Fabric and Forge, 29 October 2026. The approved
reference is `output/pdf/siu-create-family-complete-window-overhaul.pdf`.

## Delivered scope

- Create: 14 original menu classes - Toolbox, Schematicannon, Schematic Table,
  Crafting Blueprint, package ports/postboxes, Redstone Requester, stock request
  and category menus, Factory Gauge item selection, List Filter, Attribute
  Filter, Package Filter, Linked Controller and Train Schedule.
- Create Big Cannons: ammunition containers and all four fuze menus. The
  creative ammunition container retains its original variant behavior.
- Steam n Rails: the conductor's mounted toolbox.
- Forge Shuffle Filter 1.0.2: equal/weighted modes in its original shared
  Create Filter screen. This release has no separate weighted-filter screen.
- Non-inventory Create controls stay native. Simulated/typewriter interfaces
  from the broader reference have no applicable retained 1.20.1 release.

The exercised versions were Create 6.0.8 (Fabric port 6.0.8.1 build 1744), Big
Cannons 5.11.4 and Steam n Rails 1.7.3, with their retained original dependencies.
Exact publisher metadata: `build/create-window-implementation/target-matrix.json`.

## Implementation

Compatibility owns the Create window definitions, palettes, composition maps,
native opening adapters and explicit native-action packet allowlists under
`compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/create/`.
Forge shares those sources. One geometry map drives native pixels and inverse
input coordinates. Dynamic controls, complete cells and schedule suggestion
popups retain their native geometry.

Original menus retain ownership and behavior: toolbox compartments stay grouped,
filters/controller/schedule references stay virtual, category filters are real
owned items, and ammo/package inventories retain their original restrictions.
Only actual package storage opts into ordinary container sorting.

Core internal fixes preserve scoped native payloads and native close/save order,
render original screens in their expected context, retain input context, consume
wheel events at native limits, protect held backing items and accept virtual
mounted-toolbox providers through native entity validity. No new public API was
required for this integration.

The retained Create stock request factory constructs the wrong server menu class;
captured requests now use the original matching RequestMenu constructor. The
requester menu saves its ghost rack on removal, so final native configuration is
applied after that removal to preserve quantities. Native receivers keep their
original permission, range and ownership validation.

## Validation

The acceptance aggregate records **165 unique functional assertions on Fabric
and 168 on Forge**, plus 26 template/capture assertions per loader. It preserves
earlier exploratory failures and the journal for each latest result. The gallery
has 25 actual interface states per loader across all 20 menu classes.

The feature matrix covers opening, inventory transfer/splitting, toolbox deposits,
virtual references, clearing, filter policies, all twelve controller references,
requester quantity/address/policy, category ownership/refunds/reorder/hide/show,
filled stock search/order/locking, actual package creation, schematic upload and
printing, blueprint crafting, connected ports, regular/creative ammunition, all
four fuze ranges/save paths, every native schedule instruction/condition, nested
editors, station suggestions, AND/OR, both scroll axes, progress, reorder,
duplicate/delete, SIU management and GUI scales 3/4.

Both loader JAR builds passed. Focused JVM checks passed: CreateLayoutsTest,
NativeStorageRolesTest, NativeTemplateContextsTest and NativeTemplateLayoutTest
(15 test cases total). The source/module boundary check passed. Packaged JARs
were checked for accidental QA observer classes and copied with SHA-256 hashes.

This is an affected-window functional matrix, not a full-modpack gameplay suite.
Fabric used a production intermediary runtime with unchanged upstream release
bytes. Forge used the official named QA runtime with recorded remap lineage;
an independently obfuscated production Forge launch was not run. Raw OS modifier
shortcuts, every custom binding, external folder opening, dedicated multiplayer,
controller input, localization and accessibility were not exhaustively tested.
One Fabric run ended in a native GLFW access violation during Windows automation;
its log remains available, and subsequent fresh game checks completed.

## Artifacts and reproduction

- `output/pdf/siu-create-1.20.1-implemented-windows.pdf`: rendered and visually
  inspected 16-page implementation gallery and coverage report.
- `output/create-window-implementation/builds/`: matching Core and Compatibility
  JARs for both loaders. These contain the current workspace.
- `output/create-window-implementation/README.md` and `acceptance-evidence.json`:
  hashes, assertion history, versions, coverage, screenshot lineage and limits.
- `functional-tests/create-window-overhaul/suite.py`: focused groups against a
  live disposable original-menu observer; `capture.py` records the menu gallery.
- `tools/prepare-production-fabric-audit.py`,
  `tools/prepare-reviewed-forge-observer.py` and
  `functional-tests/remaining-ui-audit/native/qa.init.gradle`: retained launchers.

Fixtures seed disposable world/items. Tested operations use ordinary original
opening and screen input through SIU's mapping, with authoritative inventory and
block-entity snapshots. Fake menus and substitute widgets are not used. No
unrelated full-modpack suite, commit or push was performed for this task.
