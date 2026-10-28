# Simply Swords: Runic Forge

Minecraft **1.20.1 Fabric and Forge**, Compatibility **0.1.3**, matching Core **0.1.6**. The reference release is Simply Swords **1.70.2-1.20.1**, with its normal dependencies installed separately.

The approved design combines [option B's two bars with option C's charcoal palette](../output/pdf/siu-simply-swords-ui-analysis-and-design.pdf). The 212 x 88 window retains the native torn stone sides, three upper sockets, eight tablet cells and animation corridor. The lower player-inventory bag is omitted; SIU's player inventory remains separate. The bottom bar is blank, and ordinary SIU management controls remain in the title bar.

## Original workflow

The original `RunicForgeScreenHandler` remains the actual menu. Its real slot objects decide which weapons, gems and tablets are accepted. Its original callbacks apply weapon edits when the weapon is removed or the menu closes, refund unused material and preserve weapon data. Native quick-move remains responsible for Shift transfers. There is no added Craft/Apply button, output slot or tab.

The window reads the handler's synchronized preview for the central weapon tooltip. Slot 47 remains an inactive, virtual preview entry; it is never drawn or exposed as a physical output. Counts in the real gem/tablet sockets drive the same 800 ms traveling-channel and slot-pulse presentation as the reviewed native screen.

## API boundary and artwork

`SimplySwordsWindow` is an ordinary public `DesktopWindowDefinition`. It uses supported decoration, slot-hit, textureless-slot, tooltip and lifecycle callbacks. Public menu bindings preserve native quick-move and declare the preview virtual. The original native menu is reconstructed from its BlockPos opening data. An optional mixin targets only Simply Swords' own block interaction and routes through `DesktopSessions.openMenuSession`; the native opener remains the fallback when capture is unavailable.

No Core or Controlify implementation, API signature, access widener or reflected Core field is changed or used. Reflection is limited to Simply Swords' constructor, original source position and preview getter. The optional mixin is skipped when the upstream block class is absent.

The native slab and symbols are read from the installed mod's current resource pack at runtime, rather than bundled in Compatibility. For the exact reviewed native texture, a small cosmetic cleanup replaces the removed inventory bag's lip with nearby stone pixels while retaining the gold tablet artwork. Unknown resource-pack artwork is retained intact instead of applying this color-specific cleanup. Resource reloads invalidate the generated body; higher-resolution artwork uses the same native GUI scale. The small charcoal bar texture is editable in `artwork/shared-resources/compatibility/assets/salts_inventory_compat/textures/gui/simplyswords/window.png`.

## Focused validation

Build, artifact hashes and focused checks are recorded under `build/simply-swords-implemented/`. The targeted layout tests verify complete visible-cell bounds, exclusion of the hidden preview and the reviewed cosmetic mask. Actual runtime screenshots and executed checks are recorded in the [implementation PDF](../output/pdf/siu-simply-swords-implemented-windows-1.20.1.pdf) and its accompanying validation record under `output/simply-swords-implemented/`.

This scope covers the Runic Forge on 1.20.1. It does not claim later-version ports, complete modpack validation, all awakening forms or all third-party tooltip combinations. Named development-runtime transformations used for gameplay checks must be distinguished from shipping production JARs.

Source reference: [Simply Swords 1.20.1 screen and handler](https://github.com/Sweenus/SimplySwords/tree/5132625f7ef0916168b53999587b73490b8876d4/common/src/main/java/net/sweenus/simplyswords), [Fabric release](https://modrinth.com/mod/simply-swords/version/Tq7tcDiO), [Forge release](https://modrinth.com/mod/simply-swords/version/Na6e94J1).
