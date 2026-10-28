# Project Everything: second batch on Minecraft 1.20.1

This batch uses Core 0.1.6 and Compatibility 0.1.3. It targets Minecraft **1.20.1 only**, using the available Fabric and Forge releases. The existing eight integrations and their other-version ports are unchanged.

## UI inventory and release pins

| Family | Fabric release | Forge release | UI scope |
| --- | --- | --- | --- |
| Bartering Station | 8.0.0 | 8.0.0 | Gold inputs, piglin model/count, cooldown arrows and returned-item grid |
| Alloy Forgery | 2.1.2+1.20 | Unavailable | Shared forge menu across controller materials; temperature, fuel, smelting and exterior lava reservoir |
| Fabric Seasons: Extras | 1.3.2-BETA+1.20 | Unavailable | Heater and chiller share a menu; nine inputs, three fuel modules, valves, burn indicators and particle switch |
| Alloyed | 2.1.5+1.20.1-fabric | 2.1.4+1.20.1-forge | These legacy Create add-ons own **no menu or screen**. Their materials use ordinary inventories; processing remains in Create |
| Farmer's Delight / Refabricated | 1.20.1-2.5.7 | 1.20.1-1.3.4 | Cooking pot, vanilla-menu cabinets, canvas sign and hanging canvas sign editors |
| Miner's Delight | Unavailable | 1.20.1-1.4.5-backport | Copper pot, cooking progress and copper-cup serving workflow |
| Better Archeology | 1.2.1-1.20.1 | 1.2.1-1.20.1 | Identifying table and the villager fossil's inventory socket |
| Illager Invasion | 8.0.7 | 8.0.7 | Imbuing ingredients, native scroll illustration and enchantment result |
| Galosphere | 1.20.1-1.5.2 | 1.20.1-1.5.2 | Combustion table and configured bomb result |
| Sawmill | 1.20-1.4.11 | 1.20-1.4.11 | Native recipe tiles, scrollbar, search and both configured widths |

The committed [release inventory](../../compatibility/native-windows-1.20.1.json) records all 17 available mod/loader combinations. The official release records and downloaded artifact hashes are also recorded by `tools/prepare-project-everything-ten.py` in the ignored `build/project-everything-ten-1.20.1/*-fixtures.json` evidence. Alloyed's Fabric artifact has an older internal mod-version label; the table identifies its download release. The later standalone Alloyed forge UI does not exist in its 1.20.1 releases. Other fossil types do not expose inventory menus. Calendars, seasonal detectors and ordinary world interactions stay with their original mods.

Standalone sign editors retain their native screens. They are not converted into inventory windows. Cabinets use the ordinary storage host. These choices preserve the original behavior without manufacturing duplicate menus.

## Implementation boundary

Each integration lives under its own Compatibility package. `NativeMachineLayouts` provides compact source regions; `NativeWindowPolish` registers optional matchers through the public `DesktopClientApi.registerNativeMatcher` contract. Slots retain their original menu IDs, restrictions and server logic. Crops fall back to the general host if an active non-player slot would be clipped.

The templates remove native title/player-inventory bands and unused outer space. Cooking and copper-pot recipe buttons sit in a compact extra column inside their widened frames. The Alloy Forgery lava reservoir is an exterior region. Better Archeology's identifying table uses a Compatibility-owned parchment nine-slice frame through the public decoration API, with matching title and manager controls; its fossil socket retains its gray styling. The original recipe component, native progress artwork, decorative fossil socket, Imbuing scroll and Sawmill search/recipe surface are retained. Forge Better Archeology has different menu IDs and appends machine slots after the player inventory; the layout follows slot ownership rather than assuming slot zero.

Three optional Fabric-side bridges address upstream implementation details:

- Refabricated opens its cooking pot through Porting Lib. The bridge sends that opening through public `DesktopSessions` APIs while retaining its original extra-data writer.
- Alloy Forgery's Owo renderer uses OpenGL scissor coordinates. Its optional bridge accounts for the actual rendering viewport so the original artwork appears in the captured window.
- Seasons: Extras normally sends buttons to the current vanilla menu. The template routes its valves and particle switch through the public menu-button API, and the server bridge invokes upstream's original button behavior on the correct detached menu.

Bartering Station's original public piglin-model renderer bypasses native GUI capture. Its template reuses that renderer and the menu's public count getter on the API render surface. It does not copy the model implementation or access Core internals.

No new public API surface was necessary. Three **general 1.20.1 Core fixes** were needed: retain the menu's in-flight carried stack when native code broadcasts during a click; copy Forge's complete written opening payload after its reader index has advanced; and position explicitly supplied recipe components for foreign recipe menus beside the occupied window bounds, including exterior controls. Recipe toggling, reopening, search, ghost previews and available-ingredient placement were checked. None of these fixes identifies an integration or imports third-party classes. Controlify is unchanged.

Install matching Core and Compatibility builds on the client and server when using these server-side opening/button bridges. All target mods and their dependencies remain optional, separate downloads.

## Known upstream co-installation conflict

Create 6.0.8 on Forge supplies smelting/blasting recipes whose result is `galosphere:silver_ingot`. Galosphere 1.5.2 removed that item. Installing both prevents world data loading before any SIU window opens. The original upstream JARs are not altered by this work.

Forge UI evidence therefore uses one fixture without Alloyed/Create for Galosphere, and a second without Galosphere for Alloyed/Create. The preparation tool supports `--fixture-set without-alloyed` and `--fixture-set without-galosphere`. This report does not claim those particular upstream versions can create a world together.

## Build and evidence

Build affected modules with:

```powershell
.\gradlew.bat :1.20.1:fabric:build :1.20.1:forge:build :compatibility:1.20.1:fabric:build :compatibility:1.20.1:forge:build
```

The standard root build also includes them. Production outputs retain the normal names under `dist/core` and `dist/compatibility`; no upstream mods or QA fixtures are embedded. The API-only boundary is checked in source and production constant pools by `tools/verify-module-boundaries.py`.

Focused test drivers are excluded from shipping code under `functional-tests/project-everything-ten`. Their JSON records and original screenshots are retained under `build/project-everything-ten-1.20.1/gameplay`. The final screenshot report is `output/pdf/siu-project-everything-ten-mods-1.20.1.pdf`. These are affected-menu checks, not the complete gameplay suite. Dedicated multiplayer, Spatial GUI and controller interaction are outside this batch's runtime coverage.

### Completed focused checks

- All four affected loader modules built successfully with the normal production configuration. The six native-machine layout regression tests passed, including Forge's player-first fossil slot order.
- Source and production constant-pool boundary checks passed across the existing twelve Core and twelve Compatibility artifacts. The four affected 1.20.1 production JARs and their distribution checksums were refreshed.
- Fabric evidence is in `ten-final-fabric-09`, with the recipe-position/toggle/placement follow-up in `ten-recipes-fabric-10`. Forge evidence is in `ten-forge-03`, `ten-forge-04` and `ten-recipes-forge-05`. The latter also verifies the final configured-bomb and Unbreaking IV item metadata after transfer.
- Pickup, return, move/reopen and authoritative snapshots passed for each owned menu on its available loader. Native processing and output transfer passed for the processing workbenches. Both Seasons skins, all six Alloy Forgery materials, Sawmill's two widths/search modes, standalone sign editors and cabinets were reviewed. A focused Fabric wheel check also selected a different native Sawmill recipe after scrolling.
- Seasons controls operated independently across two open windows. Provided cooking/copper recipe books passed repeated toggles and full-window reopening without covering the recipe toggle. Their search and available-ingredient placement worked; unavailable recipes kept their native ghost previews.
- A separate Fabric fixture with every optional target absent started successfully and retained ordinary chest pickup/place behavior. Integrated test worlds and clients were stopped after collecting evidence.

The consolidated local `validation-summary.json` records 260 latest distinct passing assertions, with no unresolved failed assertion in those selected runs. Earlier failures and corrected fixture mistakes remain in the logs. This count is not a claim of exhaustive gameplay coverage.

The final PDF contains 19 visually reviewed pages and 51 real screenshot crops. Alloyed is represented by its actual material inventory workflow, rather than an invented standalone screen.

### Cooking and parchment follow-up

The cooking/copper pot toggle was moved from an exterior fragment to a 24-pixel in-frame column (20-pixel button plus a four-pixel gap). It retains the native click target and recipe component. Without a visible native toggle, no extra column is reserved. The identifying table now owns an 11-pixel parchment frame and matching title/control palette through `DesktopWindowDecoration`; the gray fossil socket is unchanged. Both adjustments are entirely in Compatibility, using the existing public API.

Focused follow-up evidence is under `ten-polish-forge-06` and `ten-polish-fabric-11`. The tests cover pot processing, serving/output transfer, repeated book toggles, window reopening, recipe search/selection, identifying progress/output, and moving/reopening the themed window. The complete functional suite was not repeated.

See the [updated in-game comparison](../../output/images/siu-pot-archeology-1.20.1-updated.png). The custom close control also passed a real-mouse check. Core's two 1.20.1 production JARs remain byte-identical to the preceding build; Controlify was not changed. Compatibility packages were refreshed to include the canonical shared texture on all targets, although only the 1.20.1 adapters use it.
