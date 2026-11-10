# Complete native Create window rollout - Minecraft 1.20.1

All 20 retained Create-family inventory/container menu types use the approved
native-frame treatment. The 25 primary comparison views cover the six earlier
native-frame implementations and every remaining applicable retained menu.
Non-inventory controls retain their native behavior, as previously agreed.

This completes the retained base/Create Big Cannons/Steam n Rails menu set, not
the entire community-addon ecosystem. The follow-up release audit in
`create-addon-ui-coverage-1.20.1.md` identifies additional addon menus and shared
UI variants that still need review. Its 270-project checklist and implementation
queue are saved under `output/create-addon-coverage-audit/`.

Create supplies Toolbox, Schematicannon/settings, Schematic Table, Crafting
Blueprint, List Filter, Attribute Filter, Package Filter, Linked Controller,
Frogport/Postbox, Redstone Requester, Stock Keeper, Stock Ticker categories and
its editor, Train Schedule and its nested editor, and Factory Gauge's item
selector. Create Big Cannons supplies regular/creative ammo containers and the
timed, proximity, delayed-inertia and delayed-impact fuze menus. Steam n Rails
supplies the mounted Toolbox. Forge Shuffle Filter 1.0.2 shares Create's Filter
screen and inherits its native frame.

The original title-strip textures, body artwork, slot art, borders, controls,
pointed footers, spacing and outboard models remain. Titles are left aligned;
SIU management controls occupy the native strip. Native panels have zero added
padding and no replacement outer frame. Player inventory remains independent.
Port address fields retain their original EditBox and pencil, with compact
management controls. Stock Keeper retains its original responsive height.
Nested editors exclude only transparent atlas edges that would otherwise copy
their modal gradient into an abrupt rectangular backdrop. Overlapped native
Toolbox and Schedule editor footer pixels are restored from upstream atlases.

All product changes for this rollout are in the shared 1.20.1 Compatibility
Create integration. No Core or public API edits were made for this rollout.
Pre-existing unrelated workspace changes were preserved.

Focused acceptance on the production Fabric runtime passed 114 affected-feature
assertions plus 11 final Schedule header checks, 72 GUI-scale assertions across
24 states, and 25 final native-window
capture checks. Two layout geometry tests passed. Fabric remapJar and Forge
jar/reobfJar builds passed, and source module boundaries were verified. The
matching Fabric artifacts are byte-identical to the tested runtime JARs.

The final title-inset correction accounts for the Schedule atlas's transparent
side margins. Its controls and title remain inside the visible native strip.
Only CreateNativeFrame.class changed in this final correction; native action
and layout classes remain byte-identical to the build used for the larger
feature/scale matrix. All 25 primary captures use the final binary. Twenty-one
named static-art landmarks (138,044 pixels) match the original captures exactly;
this excludes world pixels, transparent corners, titles and animated models.

Checks covered actual package transfers and restrictions, original port name
editing/save, compact management, package-filter address/clear, all controller
references, Requester quantity/address/policy, category ownership and editing,
stock-network search/basket/order creation, mounted storage, gauge save, ammo
rules/spacing and four fuze save routes. Schedule checks covered all five native
instruction forms, nine conditions, autocomplete selection, timeline navigation
and condition alternatives. GUI scale 4 used 320 x 240 logical pixels.

This is focused acceptance, not the complete Create gameplay suite. Earlier
six-menu gameplay acceptance remains in its existing packages; those menus
received fresh captures and scale checks here. Forge was build-checked only.
External OS folder launching and unchanged fuze endpoints were not repeated.
Later-release Simulated typewriter and distinct weighted Shuffle Filter screens
were not backported. The PDF appendix records the other non-inventory controls.

`output/create-native-all/` contains paired originals/final captures, raw full
screenshots, observations, populated and GUI-scale states, focused check history,
build logs, provenance and matching Fabric/Forge JARs. The complete PDF is
`output/pdf/siu-create-all-native-window-comparisons.pdf`.
