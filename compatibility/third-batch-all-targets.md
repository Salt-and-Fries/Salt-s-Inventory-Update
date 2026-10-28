# Third Project Everything batch across supported targets

Compatibility **0.1.3**, with matching Core **0.1.6**. This extends the approved 1.20.1 designs to every exact upstream Minecraft/loader release found in the audit. The 1.21.1 Core builds also include the shared native-capture transparency fix described below; Controlify requires no changes.

| Integration | 1.20.1 | 1.21.1 | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
| --- | --- | --- | --- | --- | --- | --- |
| ExtendedDrawersAddon | Fabric | Fabric | — | — | — | — |
| The Graveyard | Fabric, Forge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge |
| BetterEnd | Fabric | Fabric | — | Fabric | Fabric | Fabric |
| Runes | Fabric, Forge | Fabric, NeoForge | — | Fabric, NeoForge | Fabric, NeoForge | Fabric, NeoForge |
| Deep Aether: Combiner | Forge | NeoForge | — | — | — | — |
| Chipped | Fabric, Forge | Fabric, NeoForge | — | — | — | — |
| Simply Swords: Runic Forge | Fabric, Forge | Fabric, NeoForge | — | — | — | — |
| TieredZ | Fabric | Fabric | — | — | — | — |
| SmitherZ | Fabric | Fabric | — | — | — | — |
| AdditionZ | Fabric | Fabric | — | — | — | — |

An em dash means no exact matching upstream release was available for that SIU target. It does not mean that a neighboring Minecraft release can be installed instead. The [pinned release manifest](third-batch-all-targets.json) records all 45 matching combinations, their source links and SHA-512 hashes; 31 combinations are beyond 1.20.1. Each upstream mod remains optional, together with its own required libraries.

## Preserved designs and behavior

- Drawer utility controls, content previews and upgrades keep their original slot permissions and configurable compartment counts.
- Ossuary recipe cells and scrolling use the native renderer. On newer releases its recipe list is synchronized to the owning desktop window, including when another menu has focus. Urns, coffins and sarcophagi retain ordinary storage windows; gravestone text editing remains native.
- BetterEnd's End Stone Smelter, Runes' altar and Deep Aether's Combiner retain native ingredients, output, indicators and recipe components. This is not a claim that the entire Aether parent mod is adapted.
- Recipe guides follow the installed upstream release. BetterEnd's 1.20.1/1.21.1 smelter has its native recipe book; the audited 26.x smelter screen no longer provides one, so those windows omit the empty book column. Ordinary BetterEnd furnaces still use Core's vanilla furnace/recipe-book support.
- TieredZ and SmitherZ retain their connected exterior tabs, compact machine regions, complete grindstone outline and live smithing preview. Rename, tab switching, socket selection and workstation actions address the owning server session. SmitherZ's 1.21.1 opening data preserves the configured socket count and source position.
- AdditionZ retains Fletching Table and Chunk Loader controls, including the loader's typed opening data through the existing automatic native bridge.
- AdditionZ's 1.21.1 fletching adapter uses that release's `additionz:fletching` menu ID; 1.20.1 retains `minecraft:fletching`. Runtime checks distinguish an intentionally registered template from the generic automatic fallback.
- Chipped retains its brown window, material picker, search, scrolling, four native preview modes and native Craft/Craft All operations. Its 1.21.1 adapter uses that release's unified workbench recipe system, item components and actual two-block workstation source.
- Simply Swords retains the approved compact Runic Forge layout, torn native page edges, charcoal title/end bars, native item permissions and slot animations. Its inactive preview remains a virtual slot, not an additional output or item source.

Per-mod adapters remain entirely in Compatibility. The subsequent 1.21.1 Core capture fix suppresses the inherited transparent/blurred backdrop only while a native screen is hosted, preventing dark drawers and rectangular tab backings. All interactions with SIU use the public `api` packages; reflection and optional mixins target the owning upstream mod or a scoped vanilla screen. Core is not embedded in the extension.

## Build and validation

The normal root build still produces both the existing modules and their production distributions. Individual compatibility modules can be built with, for example, `gradlew.bat :compatibility:1.21.1:fabric:assemble`. Remapped release artifacts belong in `dist/compatibility`; development and source JARs do not.

`verifyThirdBatchPorts` checks the pinned release matrix, bootstrap registration and optional dependency metadata. `verifyThirdBatchPortJars` additionally checks packaged implementation classes and optional mixin configurations. The module boundary verifier remains authoritative for API-only communication. Focused geometry, source-coordinate mapping, Chipped catalog and Runic Forge layout regressions cover the changed 1.21.1 interfaces. Runtime observations and screenshots are recorded separately; opening a window alone is not a successful crafting, animation or conservation test.

See the [focused validation record and coverage limits](third-batch-all-targets-validation.md) for executed checks.

The [corrected all-version screenshot gallery](../output/pdf/siu-third-batch-all-versions-visual-fixes.pdf) shows all ten mods across their 45 supported version/loader combinations, including the original 1.20.1 targets and fresh button, drawer and tab captures.
