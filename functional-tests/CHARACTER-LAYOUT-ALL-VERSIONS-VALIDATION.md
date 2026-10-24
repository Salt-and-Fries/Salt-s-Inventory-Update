# Character window — all Core targets

Validated on 2026-10-22 for Core 0.1.5. Ports the approved [26.3 Fabric preview](CHARACTER-LAYOUT-26.3-FABRIC-VALIDATION.md) to every supported Core version and loader.

## Scope

| Minecraft | Core loaders built and package-checked |
| --- | --- |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge |
| 1.21.11 | Fabric, NeoForge |
| 26.1.2 | Fabric, NeoForge |
| 26.2 | Fabric, NeoForge |
| 26.3 | Fabric, NeoForge |

All six shared version sources now use the same 176 × 102 character window: vanilla upper-inventory slot positions, vanilla player-preview scale, four additional GUI pixels at the bottom, and a dedicated shared 16 × 16 crafting-arrow PNG. Health/Hunger/XP text and its rendering helpers are removed. No Crafting label is added. The existing offhand rendering and interaction gates remain tied to the inventory-hosted hotbar/workspace presentation.

The temporary Fabric-only layout gate is removed. Forge and NeoForge consume their version's shared source. The 1.20.1 preview uses vanilla's legacy baseline `(51, 75)` and relative mouse offsets; newer renderers use inset bounds `(26, 8)` to `(75, 78)`. No public API or protocol changes were needed. Compatibility and Controlify code and versions are unchanged.

## Executed checks

- `./gradlew.bat writeReleaseChecksums --console=plain` passed. This assembled all twelve Core targets, using the configured production remapping/reobfuscation tasks where needed, then passed `verifyNonFabricModJars` and `verifyReleaseBundle`. Log: `build/character-layout/all-core-assemble.log`. This command did not run Gradle test tasks or the gameplay suite.
- `functional-tests/scripts/Test-SourceFeatureParity.ps1` passed after updating its obsolete character-layout expectations. It includes the offhand render/hit-test gates and current character geometry. Log: `build/character-layout/all-core-source-parity.log`.
- A focused geometry audit checked all six shared sources for window bounds, bottom clearance, two GUI pixels on either side of the arrow, retained offhand gates and removed stats text. Evidence: `build/character-layout/all-core-layout-checks.json`.
- `python tools/verify-shared-textures.py --resources --jars` passed: 62 shared texture resources across 36 module targets, including the exact new arrow bytes in all twelve Core JARs.
- Refreshed `dist/core/`, `build/release/0.1.5/`, their checksum manifests, and all twelve matching Core entries plus the Core checksum entry in `dist/salts-inventory-all-latest.zip`. Verified the archive's replacement bytes and that the other 24 distribution JARs were unchanged. Artifact names and SHA-256 values: `build/character-layout/all-core-artifacts.json`.

## Focused in-game check

Launched rebuilt development classes in Forge 1.20.1 with only Core and the native loader components, using a new isolated `character-layout-port` profile and a copied disposable test world. Opened the character window, moved it to the desktop center, and visually checked the legacy player-preview position, armor/crafting/result/offhand positions, new arrow, bottom margin and absent stats text. The recipe book opened and closed with one click each at its relocated button. Return Hotbar to Inventory was enabled for this check.

Screenshot crop: `output/screenshots/character-layout-all-versions/1.20.1-forge-character.png`. The full framebuffer and crop metadata are alongside it. Client log: `build/character-layout/legacy-forge-client.log`.

The earlier 26.3 Fabric preview separately checked both offhand visibility settings and the padded layout. Its captures remain in `output/screenshots/26.3-fabric-character-layout/`.

## Coverage limits

This was a focused UI port, compile and packaging validation, not a complete functional/gameplay suite. The other ten targets were built and inspected statically but were not launched for this port. The Forge client used development classes rather than the reobfuscated production JAR. Actual crafting, armor/offhand item transfers, potion-effect side panels, multiplayer and Spatial GUI presentation were not exercised during this port. No Trinkets or Curios integration is implemented by this layout change.
