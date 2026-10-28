# Compact native container windows - Minecraft 1.20.1

This is the original 1.20.1 design and validation record. The subsequent Core **0.1.6** all-target port is documented in [the current coverage and validation record](native-containers-all-targets.md).

Compatibility **0.1.3** consumes Core's public native-template API to implement the approved compact layouts. Install the matching **rebuilt Core 0.1.5**; previously published files with these version numbers may predate the template API and these corrections. Other Minecraft targets and Controlify are unchanged by this pass.

## Layouts

Dimensions below are scaled GUI units, including the normal SIU frame/header, before any viewport scrolling.

| Mod / upstream release | Loader | Layout |
| --- | --- | --- |
| Oxidized 1.8.2 | Fabric | Copper Kiln: 133 x 86, retaining the recipe button, flame, arrow, output frame and all three real slots. |
| Expanded Delight 0.3.2 | Fabric | Juicer: 114 x 93, retaining the press handle/base, arrow, bottle/drop, buffer and result frames and all five real slots. |
| Brick & Mortar 1.2.1 / 1.2.2 | Fabric / Forge | Kiln body: 139 x 88, with the native 92-unit preview outside the top border. Its closed/expanded heights are 8/45; opening does not enlarge or shift the main body. All three hatches remain native. |
| Crabber's Delight 1.2.3 | Forge | Crab Trap: 178 x 71; the centered bait cell and all nine catch cells remain in their original arrangement. Palm Cabinet uses the ordinary 178 x 86 SIU storage layout. |
| Nether Chested 8.0.4 | Fabric / Forge | 178 x 140 with 54 standard SIU/vanilla cells and the real large-stack item/count renderer. |
| Inmis 2.7.2 | Fabric | Baby 70 x 50; Frayed 178 x 50; Plated 178 x 68; Gilded 178 x 86; Bejeweled 178 x 122; Blazing 178 x 140; Withered 214 x 140; Endless 286 x 140. Ender Pouch retains its existing 178 x 86 Ender Chest route. |

There is one frame-inclusive inset, rather than stacked template padding and hidden scrollbar gutters. Native template viewports reserve scrollbar strips only on actual overflow, resolving cross-axis overflow before painting or routing input. Narrow windows use the existing compact title/management menu rather than adding empty body width.

Machine crops include essential decorative frames, handles and controls, not just slot bounds. All empty inventory cells remain present. Recipes, slot permissions, synchronization and processing remain authoritative in the original mods.

## Public API and Core behavior

The Compatibility consumers import only `com.salts_inventory_update.api.*` from Core. They neither use reflection nor access implementation classes. No original mod textures or renderer implementations are copied into the extension.

- `DesktopNativeLayout.Builder.exterior(...)` retains native fragments outside the outer frame, in window-local coordinates. They paint behind the frame, keep original input/tooltip/slot mapping, and do not enlarge or scroll with the body canvas.
- `DesktopNativeRegion.exterior()` and `DesktopNativeLayout.Placement.exterior()` describe the coordinate space. Existing constructors retain canvas behavior. JSON format-1 profiles accept an optional `exterior` flag and preserve it on round-trip.
- `DesktopNativeTemplate.nativeSlotBackground(...)` allows controlled replacement of native cell backgrounds with standard SIU slots, retaining real items/counts. The default continues to preserve native backgrounds.
- A nonvisual, noninteractive `DesktopWindowRegion` can reserve placement extent with `constrainsWindow=true`. The kiln reserves its full upward opening extent without creating an invisible occluder. Its visible fragment alone owns actual input/occlusion.
- Ordinary source-based container state is now menu-specific. Legacy positions and pins migrate while a newly substituted menu chooses its own grid size. Restored ordinary storage sizes snap to whole-cell layouts, preventing the Cabinet from inheriting another block's dimensions or blank right strips.

Ordinary API window definitions retain priority and do not enter native capture unless explicitly registered as templates. Existing ordinary API state keys are unchanged. See [the native template API](native-window-templates-1.20.1.md) for contracts and examples.

## Validation and artifacts

Focused geometry tests cover fitting/cross-axis viewports, exterior forward/inverse mapping and profile round-trips, all default backpack shapes, art-aware machine crops and stable kiln body geometry before the first native render and through expansion. `verifyNativeTemplateExample` compiles the public-API-only examples for both loaders.

Focused in-game observations and artifact hashes for this pass are retained under `build/native-ui-approved/`. The final screenshot report is [the approved UI showcase](../../output/pdf/siu-six-mods-approved-ui-1.20.1.pdf). Its images are new Minecraft captures, cropped/enlarged without repainting UI pixels. The report records the executed focused checks and their limits; it is not a complete gameplay-suite result.

See [the executed validation record](native-container-compact-validation.md) for the 23 focused unit tests, 107 gameplay assertions, discoveries, final artifact identities and coverage limits.

Release paths (versions unchanged):

- `dist/core/salts_inventory_update-1.20.1-Fabric-0.1.5.jar`
- `dist/core/salts_inventory_update-1.20.1-Forge-0.1.5.jar`
- `dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar`
- `dist/compatibility/salts_inventory_compat-1.20.1-Forge-0.1.3.jar`

Dedicated remote multiplayer, all resource packs/configurations, controller/Spatial GUI combinations and arbitrary oversized custom backpack configurations are not exhaustively validated by this visual-layout pass.
