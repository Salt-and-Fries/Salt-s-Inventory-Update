# Chipped workshop integration

Minecraft **1.20.1**, Fabric and Forge; Compatibility **0.1.3** with matching Core **0.1.6**. The reference release is Chipped **3.0.7**, with its normal Athena and Resourceful Lib dependencies. This integration uses the same public Core API available to external mods.

The seven Chipped workbenches share one 270 × 181 window: Mason Table, Carpenters Table, Loom Table, Glassblower, Botanist Workbench, Alchemy Bench and Tinkering Table. The source block supplies the title and recipe type. The brown/beige frame follows the approved [design study](../output/pdf/siu-chipped-ui-analysis-and-window-design.pdf).

## Workflow

- **Choose** opens a bounded, scrollable picker of eligible stacks already in the player's inventory. It selects a reference; it does not transfer an item or create an input/output inventory.
- The source bar shows the material and count. The nine-column, six-row variant browser retains native name filtering and adds a scrollbar. Its visible and clickable bounds agree, including the entire bottom row.
- The original Chipped renderer supplies the block preview, connected shapes and four arrangement modes. Mode buttons and variant/craft/search artwork come from Chipped's resources.
- **Craft** converts the selected inventory stack in place, preserving its count. **Shift + Craft All** converts matching stacks in the player inventory, following Chipped's own recipe logic. Other open containers are not part of that conversion.
- Crafting resets the selection as it does natively. A moved or changed source stack invalidates the selection instead of converting a different stack. Material pickers and searches are local to their owning window.

## API and source ownership

`ChippedWindow` is a dedicated `DesktopWindowDefinition`; the native interface contains only player-inventory selection slots, so a blind upper-screen crop cannot supply this workflow. It uses supported decoration, popup, text-input, scrolling and payload hooks. `DesktopMenuFactories` reconstructs the original Chipped menu from its opening data.

Selection and crafting travel through authenticated desktop session payloads. The server revalidates the current inventory reference and permitted native recipe result before invoking Chipped's own crafting method. The shared desktop cursor is not used as a material slot.

Optional mixins target **Chipped's own** block interaction, menu provider and menu classes. They retain the actual MAIN/SIDE source pair across native menu construction and validate the lifetime of an established desktop window. They do not target Core classes. No Core internals, reflection into Core, access wideners, shaded Core copy or Controlify changes are required. Reflection is limited to Chipped's menu/preview classes; those classes are resolved only for the optional integration.

On Forge, Chipped's `NetworkHooks` opener is adapted through the public `DesktopSessions.openMenuSession` entry point. The native path remains available when the public opener does not handle the request. This adapter is scoped to Chipped's own opener.

## Focused evidence

Build, packaging and regression records are under `build/project-everything-chipped-implemented/`. The two targeted logic tests cover inventory-index mapping and the bottom-row hit area/scroll boundary. Runtime observations and actual screenshots are kept separately from the design-study mockups. Native-reference captures in the earlier study do not prove SIU integration functionality.

The [implemented screenshot report](../output/pdf/siu-chipped-implemented-windows-1.20.1.pdf) compares the approved proposal with actual runtime captures. The [focused validation record](../output/project-everything-chipped-implemented/validation.json) identifies each run and its coverage limits. The material picker uses a separate public rendering stratum so native search text and preview depth do not show through its opaque frame.

The affected release artifacts are `dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar` and `dist/compatibility/salts_inventory_compat-1.20.1-Forge-0.1.3.jar`. Read the final runtime validation record for executed cases and remaining limits; focused checks are not a complete functional suite.

The focused Fabric run exercised all seven workbench openings, duplicate source stacks, single/Craft All conversions, four preview modes, no-result search, scrollbar dragging, the last row's bottom edge, stale source changes, ordinary inventory/cursor use, two independent windows and source/range closure. Forge exercised all seven openings, material selection, a real in-place conversion, positive search and actual mouse-wheel scrolling. The tests use mapped development runtimes with original/remapped release artifacts as documented in their provenance records; they do not certify every modpack, input device, locale, upstream release or dedicated server configuration.
