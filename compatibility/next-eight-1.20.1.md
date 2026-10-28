# Compact native windows: third Project Everything batch

Compatibility 0.1.3 with Core 0.1.6, for Minecraft **1.20.1**. Install matching client/server files and each upstream mod's own dependencies. These integrations do not add dependencies to Core or Controlify.

| Mod | SIU target | Interfaces |
| --- | --- | --- |
| ExtendedDrawersAddon | Fabric | Single, double and quad drawer upgrade panels; survival/creative utility controls and native content previews |
| The Graveyard | Fabric and Forge | Ossuary recipe grid and scrollbar; ordinary urn, coffin and sarcophagus storage |
| BetterEnd | Fabric | End Stone Smelter and its native recipe book; ordinary furnace variants retain existing Core furnace support |
| Runes | Fabric and Forge | Rune Crafting Altar |
| Deep Aether | Forge | Combiner and its native recipe book |
| TieredZ | Fabric | Anvil entry tab, name field and Reforge Gear |
| SmitherZ | Fabric | Smithing/linking and grindstone/unlinking tabs, native gear preview, socket slots and link action |
| AdditionZ | Fabric | Fletching Table and Chunk Loader |

**Gravestone editing stays native**, like editing a sign. It is not a desktop window. Chipped and Simply Swords were subsequently implemented in their own approved designs; see the [complete cross-target matrix](third-batch-all-targets.md). This batch does not claim support for the entire Aether parent mod, optional Trinkets content, or every historical/configured upstream layout.

## Design and ownership

- The Compatibility JAR crops and composes the original upper-panel artwork using the public native-template API. Slots keep their native identities and permissions; animations, previews and recipe components remain native.
- Small balanced frame insets match the approved mockups. Native lower inventory bands are omitted because SIU already supplies the player inventory.
- Gear tabs sit outside the frame and participate in the window's placement bounds. Recipe-book toggles stay inside the machine window; the open book remains attached beside it.
- Selected gear tabs retain their native connector over the top bevel, joining their body to the window. Unlinking retains both grindstone supports; the smithing crop includes the complete live armor stand and equipped-result preview area.
- The gear workstation emblem moves into the title bar. The anvil's original name field is represented by a normal Minecraft `EditBox`, using the public rename action. Input is not sent against an unrelated player menu.
- Gear transitions and reforge/link actions use the public authenticated payload/session APIs. The server validates the originating session and source position, and the original mod menu performs the actual operation and determines readiness.
- The drawer's original extended opening bytes are reconstructed with `DesktopMenuFactories`. Its native utility/upgrade callbacks remain in charge. A compatibility-side upstream menu hook closes a persistent drawer window when its source disappears, is replaced or goes out of reach.
- Conventional Graveyard storage and BetterEnd furnace variants already fit existing Core windows. They do not require overriding unrelated vanilla menus.

No Core implementation imports, reflective Core access, access wideners, shaded Core classes or Core-specific mixins are used. Reflection in the optional bridges targets the owning third-party mod's menu constructors/methods only. The vanilla anvil hook is scoped to this integration's own hosted menu lifetimes.

## Extending a native layout

Each mod has its own package under `compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/`. Shared loader-neutral sources are also compiled into Forge where the relevant mod exists. The per-mod bootstrap installs only when that mod is loaded.

`NativeBatchLayouts` describes native source regions; `MockupNativeTemplate` supplies compact sizing/chrome through `DesktopNativeTemplate`. Use complete slot cells and keep pointer mapping paired with every moved region. A changed upstream slot topology falls back to its native host instead of clipping a real slot. Configurable SmitherZ socket counts widen the retained socket strip rather than discard cells.

Other integrations can use these same public APIs: `DesktopClientApi.registerNativeMatcher`, `DesktopNativeLayout`, `DesktopNativeRegion`, `DesktopMenuFactories`, `DesktopServerApi`, `DesktopSessions` and `DesktopMenuBindings`. No new Core API is required for this batch. See the [native-template guide](../docs/markdown/native-window-templates-1.20.1.md).

## Evidence scope

The approved visual reference is `output/pdf/siu-next-eight-native-ui-and-proposed-windows.pdf`; its proposed panels are designs, not implemented screenshots. Current gameplay evidence is collected separately under `build/project-everything-eight-implemented/`, with final screenshots/PDF under `output/project-everything-eight-implemented/` and `output/pdf/`.

The reference fixture uses ExtendedDrawersAddon 1.0.2, The Graveyard 3.1, BetterEnd 4.0.11, Runes 0.9.13 Fabric / 1.3.2 Forge, TieredZ 1.3.3, SmitherZ 1.0.4, AdditionZ 1.3.2 and Deep Aether 1.1.7. A separate focused Fabric fixture covers Runes 1.3.2 and The Graveyard 4.0.1. Read the final report for executed checks, exact artifact hashes and limitations; these pins are not a promise about every upstream release.

See the [focused validation record](next-eight-1.20.1-validation.md) and [implemented screenshot PDF](../output/pdf/siu-next-eight-implemented-windows-1.20.1.pdf).
