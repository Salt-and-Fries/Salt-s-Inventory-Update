# Raised HUD interoperability validation

Date: 2026-10-21. Core 0.1.5 / Compatibility 0.1.2 / additive API 6 changes.

## Delivered architecture

- Core exposes five new public types: `DesktopHudApi`, `DesktopHudContext`, `DesktopHudLayout`, `DesktopHudSlot` and `DesktopHudLayoutProvider`. They support deterministic provider composition, live state, per-slot translation/uniform scale, visibility, pointer targeting, registration teardown and transaction rollback.
- All six desktop implementations resolve the same layout model for detached hotbar/offhand drawing and hit testing. Slot identities and inventory networking are unchanged. Window-hosted inventory/creative hotbars and alternate workspace presentation keep their existing geometry.
- Compatibility owns one shared `compat.raised.RaisedHudIntegration`. Its guarded client bootstrap calls only Raised's public `RaisedApi` and Core's public HUD API. It follows the signed X/Y total for `minecraft:hotbar`; Core contains no Raised dependency or implementation.
- All twelve Compatibility artifacts have a separate adapter and optional Raised metadata. The corresponding official Raised 6.0.2 dependencies are compile-only and are never embedded. Native Raised selection repair and HUD rendering remain owned by Raised.
- Controlify production source was not changed for this work. Its window-hosted HUD remains outside the detached HUD provider path.

The [HUD API guide](../docs/markdown/hud_interoperability.md) defines the supported scope and includes a [public-API-only example](../api-examples/hud/src/main/java/example/hud/RearrangedHud.java). Changelogs and the generated API signature reference/site guide were updated.

## Builds and automated checks

Executed successfully:

```text
.\gradlew.bat :common:test --tests "*DesktopHudApiTest" :compatibility:26.3:fabric:test --tests "*OptionalIntegrationBootstrapTest" --console=plain
.\gradlew.bat writeModuleChecksums verifyApiExamples -x test --console=plain
python tools/test-compatibility-coverage.py
python tools/generate-api-reference.py --check
python tools/audit-compatibility-coverage.py --jars
python tools/verify-module-boundaries.py --sources --jars
```

- Seven HUD API tests passed: exact target edges, live offsets/viewport changes, independent scaled slots, hidden/disabled targets, ordering, immutable results, provider recovery, rollback, stale handles, invalid layouts and invalid transforms.
- Three optional-integration bootstrap tests and six availability/registration coverage regressions passed.
- Release build completed successfully: 305 tasks, 175 executed and 130 up to date. All twelve Core and twelve Compatibility production artifacts were rebuilt and distribution folders synchronized. The standard release pipeline also processed the separate Controlify artifacts.
- API examples, including the HUD example, compiled against all twelve loader/version classpaths. The HUD example additionally compiled with `javac --release 17` against the 1.20.1 public API JAR alone, without Core implementation classes.
- Compiler/source/production checks enforce the API-only boundary. All twelve Core JARs export the new HUD types, all twelve Compatibility JARs contain the adapter without duplicated Core API classes, and none bundles Raised. Optional metadata was inspected for every artifact.
- Reviewed coverage now includes 88 available integration cells and 32 unavailable cells across twelve targets and ten upstream projects. Twelve of the available cells are Raised. This is availability/package coverage, not a claim of gameplay-testing every cell.

The release command deliberately excluded general `test` tasks because the requested scope was focused validation. The targeted tests above ran separately. A complete functional/gameplay suite was not run.

## Focused gameplay evidence

All six completed profiles passed, with 1,214 passing assertions/stages and 48 captured game framebuffers. Four profiles explicitly skipped Mouse Tweaks wheel execution because Mouse Tweaks was absent. These counts include stage-completion records, not 1,214 unique test cases. Representative legacy/modern captures were visually inspected.

| Minecraft | Loader | Installed combination | Runtime input | PASS records | Captures |
| --- | --- | --- | --- | --- | --- |
| 26.3 | fabric | Core + Compatibility + Raised + Mouse Tweaks | packaged production JARs | [204](../build/gameplay-qa/packaged-extension-empty/26.3-fabric-raised-modern-r2/results.json) | 8 |
| 1.20.1 | fabric | Core + Compatibility + Raised | packaged production JARs | [202](../build/gameplay-qa/packaged-extension-empty/1.20.1-fabric-raised-legacy-r1/results.json) | 8 |
| 26.3 | neoforge | Core + Compatibility + Raised + Mouse Tweaks | development classes | [204](../build/raised-qa/runtime/26.3-neoforge-raised-native-r1/results.json) | 8 |
| 1.20.1 | forge | Core + Compatibility + Raised | development classes | [202](../build/raised-qa/runtime/1.20.1-forge-raised-legacy-r1/results.json) | 8 |
| 26.3 | fabric | Core + Compatibility; Raised absent | packaged production JARs | [201](../build/gameplay-qa/packaged-extension-empty/26.3-fabric-raised-absent-r1/results.json) | 8 |
| 1.20.1 | fabric | Core alone | packaged production JARs | [201](../build/gameplay-qa/packaged-core/1.20.1-fabric-raised-core-r1/results.json) | 8 |

Checks exercised native client/server startup and desktop authentication, all nine hotbar targets plus offhand, default offsets, live combined groups with horizontal/vertical movement, GUI scales 2 and 3, old-position rejection, zero offsets, matching Mouse Tweaks proxy coordinates, pointer/world ownership, actual pickup/place between the moved HUD and a chest window, offhand pickup/return, drag distribution with server conservation, a custom scaled slot, a hidden offhand target, and Return Hotbar to Inventory.

The modern Fabric and NeoForge profiles loaded the actual upstream Mouse Tweaks mod and performed wheel transfer at the moved hotbar with authoritative server count checks. Legacy/presence-absence profiles still checked the Core bridge's target geometry but did not execute upstream wheel behavior.

Fabric profiles loaded production JARs copied from `dist`. The evidence verifier matched the launch-time shipping hashes to the unchanged current distribution artifacts. Forge/NeoForge gameplay used mapped development classes and the real upstream Raised JARs; these runs are not represented as direct gameplay tests of the production Forge/NeoForge JARs.

For reproduction use `functional-tests/scripts/Run-RaisedHudQa.ps1` with the matching `-Minecraft`, `-Loader`, `-Mode raised|extension-empty|core`, optional `-MouseTweaks`, and a new `-RunId`. Fabric requires existing distribution artifacts. Native-loader profiles explicitly include QA sources but do not package them for release.

The [machine-readable verification](../build/raised-qa/verified-runtime.json) records each report, input mode and artifact hash. `tools/verify-raised-runtime.py` verifies required checks, expected mod presence, capture integrity, Fabric artifact lineage and all 24 unchanged shipping hashes.

## Earlier failed fixture retained

The first modern Fabric attempt is retained at [raised-modern-r1](../build/gameplay-qa/packaged-extension-empty/26.3-fabric-raised-modern-r1/results.json). The fixture used an obsolete resize method and placed the inventory window over the chest, preventing its slot from being targeted. That run recorded four failures, including the explicit abort. The fixture was corrected to use the actual resize method and separate window positions; the subsequent `raised-modern-r2` run passed. The failed attempt is excluded from the six passing profiles.

## Artifacts

- Twelve Core JARs: `dist/core/salts_inventory_update-<Minecraft>-<Loader>-0.1.5.jar`.
- Twelve Compatibility JARs: `dist/compatibility/salts_inventory_compat-<Minecraft>-<Loader>-0.1.2.jar`.
- Each directory contains current `SHA256SUMS.txt`. [All 24 artifact paths and hashes](../build/raised-qa/artifacts.json) were captured after the release build and checked again after gameplay.

## Coverage limits

- Minecraft 1.21.1, 1.21.11, 26.1.2 and 26.2 were compiled, example-compiled and package/API-validated for both loaders; this task did not run their eight gameplay targets.
- Production Forge/NeoForge JARs passed remapping, packaging, optional-dependency and bytecode-boundary checks. Their focused gameplay runs used development outputs.
- A full Controlify gameplay pass, controller/keyboard transitions, creative/spectator variants, dominant-hand changes, every selection-repair mode, third-party resource packs, resource-pack reload, mid-gesture configuration changes and complex overlapping custom HUD layouts were not exhaustively tested here.
- The API supports per-slot translation and positive uniform scale, not arbitrary rotation, shaders or replacement of Minecraft's native HUD. Visibility/interactivity controls pointer targeting rather than storage permissions or already dispatched actions.
- This report does not replace prior full integration reports or claim that the entire mod is bug-free.
