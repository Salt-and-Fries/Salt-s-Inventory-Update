# Curios rollout: focused validation

Executed 2026-10-23 on `Spatial-GUI-support-test`. Release labels remain Core 0.1.5,
Compatibility 0.1.2 and Controlify 0.1.1.

## Scope

The completed 26.3 NeoForge presentation is now available on Forge 1.20.1 and
NeoForge 1.21.1, 1.21.11, 26.1.2 and 26.2. Exact native release pins and the API
contract are documented in the [Curios guide](../docs/markdown/curios.md).
There is no upstream Curios release for the six supported Fabric targets.

Core receives only reusable API and joined-window behavior. All native Curios
registration, menus, slots, textures, controls and networking remain in Compatibility.
Legacy Trinkets attachment callbacks are preserved. Native optional dependencies are
compile-only in production; the opt-in development runtime does not embed them.

These are focused affected-feature checks, not a complete functional/gameplay suite.

## Executed builds and automated checks

| Check | Result |
| --- | --- |
| Ten affected Core `assemble` tasks and five affected Compatibility `assemble` tasks | Passed; `build/curios-rollout/production-build.log` |
| Final Forge Compatibility assemble after legacy artwork fixes | Passed; `forge-final-assemble.log` |
| Final 1.21.1 Compatibility assemble with the same translucent rendering correction | Passed; `runtime-1.21.1-neoforge-final.log` |
| Five new targets' `testCuriosLayout` tasks | Passed; each checks 448 placements, explicit overflow/full-grid cases and 1,305 visibility-control layouts |
| `python tools/test-compatibility-coverage.py` | Six regression tests passed |
| `python tools/verify-module-boundaries.py --sources` | Passed: 1,010 Compatibility classes, 485 native patches and Core sources |
| `python tools/verify-module-boundaries.py --jars` | Passed: all twelve Core and twelve Compatibility artifacts |
| `python tools/audit-compatibility-coverage.py --jars` | Passed: twelve targets, 94 available and 38 explicitly unavailable integration cells |
| `python functional-tests/verify-curios-rollout.py` | Passed: all six native pairs, API parity on Fabric, Curios absent from Fabric extensions |

Geometry checks cover four viewport sizes, seven slot counts from zero to 1,000,
off-screen drag coordinates, equal widths, all four walls, tab clearance, the four-pixel
gap, full-row fitting and scrollbar allocation. Visibility checks include rightmost
controls and the scrolling viewport.

The production verifier checks exact mod/game versions, Core's required dependency,
optional Curios metadata, adapter class completeness, absence of embedded native/Core
code and artwork, absence of QA classes, and bytecode references to Core public API only.

## Focused in-game checks

Computer Use operated actual development Minecraft clients and integrated servers.
Each version used a separate disposable world and its pinned upstream Curios JAR.
The slot datapack supplies eleven native types, cosmetic counterparts and permissive
validation for vanilla test items. It does not emulate an accessory content mod.

| Target | Executed checks |
| --- | --- |
| Forge 1.20.1 / Curios 5.14.1 | World load; G from gameplay and in desktop; equipment stack round trip; cosmetic stack round trip; native launcher/tab, wing icons and gradient; item counts checked in saved NBT |
| NeoForge 1.21.1 / Curios 9.5.1 | World load; G opens, closes only the child, and reopens within desktop; normal/cosmetic panels; native slot and tab artwork |
| NeoForge 1.21.11 / Curios 14.0.0 | World load; native equipment stack round trip; far-right visibility click; cosmetics; saved NBT confirms ring index 1 visibility off, all stacks restored and native equipment/cosmetic inventories empty |
| NeoForge 26.1.2 / Curios 15.0.0 | World load; normal/cosmetic panels; dragging joined group against upper/right walls; complete rightmost toggles; gap and tab clearance; closing Character closes its child |
| NeoForge 26.2 / Curios 16.0.0 | World load; 127-slot allocation; wheel scrolling; scrollbar drag to final row; complete rightmost visibility control beside scrollbar; cosmetic view resets scroll safely; saved NBT confirms 127 slots and charm index 1 visibility off |
| Forge 1.20.1 without Curios | Core and Compatibility start with four mods loaded; the same disposable world loads after native Curios removal; inventory and Character open without the Curios launcher |

Final launch logs, original F2 screenshots, persistence checks and artifact hashes are
retained under `build/curios-rollout/` and each target's
`build/runtime-smoke/curios-rollout/`. Curated screenshots are in
`output/curios-rollout/`.

The legacy rendering checks found and corrected a launcher/cosmetic atlas mismatch
and an ordering/blending issue in translucent cosmetic overlays. Older ports preserve
separate native equipment and cosmetic slot subclasses, so native slot contexts and
equip callbacks are retained. Desktop menu indices are explicitly separated from
native item-handler indices.

Curios 16 emits an upstream deprecated `logoFile` warning on the selected 26.2
NeoForge runtime. The disposable fixture also logged legacy option/pack-metadata
fallback warnings; native slot loading and gameplay proceeded. These warnings do not
come from the release adapters. The fixture metadata was updated for future reuse.

## Artifacts

Fifteen JARs changed: both Core loaders on each of 1.20.1, 1.21.1, 1.21.11, 26.1.2
and 26.2, plus the five new native Compatibility adapters. The other twenty-one
distribution JARs, including all Controlify builds and the existing 26.3 pair, are
byte-for-byte unchanged against `build/curios-rollout/distribution-before.json`.

Production artifacts are in `dist/core/` and `dist/compatibility/`. The Core mirror in
`build/release/0.1.5/` and SHA-256 manifests are refreshed. The combined
`dist/salts-inventory-all-latest.zip` retains all 62 entries: fifteen JAR entries and
two checksum manifests are refreshed; the other 45 entries have identical contents.
`build/curios-rollout/artifacts.json` records exact before/after accounting.

## Coverage limits

No complete functional suite was run. This rollout does not claim fresh validation of
unrelated integrations, controllers, Spatial GUI, every accessory content mod, all
native equip restrictions, dedicated multiplayer, production-launcher installation,
or every capacity-change and stale-network interleaving. Pure geometry tests cover
many sizes; manual gameplay uses a small representative set. Existing 26.3 runtime
evidence remains in the [original validation record](CURIOS-26.3-NEOFORGE-VALIDATION.md).
