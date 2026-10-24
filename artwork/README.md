# Shared artwork and textures

**Edit production textures in `artwork/shared-resources/`.** One file is shared by every supported Minecraft version and loader of its mod. The old per-version texture copies have been removed.

| Mod | Editable texture folder |
| --- | --- |
| Core | `shared-resources/core/assets/salts_inventory_update/textures/` |
| Controlify | `shared-resources/controlify/assets/salts_inventory_controlify/textures/` |
| Compatibility | `shared-resources/compatibility/` — add extension-owned textures under `assets/<namespace>/textures/` |

For example, Core's window buttons are now edited in:

```
artwork/shared-resources/core/assets/salts_inventory_update/textures/gui/window_controls.png
```

Controlify's editable wheel sprites and their existing PDN original are together in:

```
artwork/shared-resources/controlify/assets/salts_inventory_controlify/textures/gui/contextual/wheel/
```

The compact character window's crafting arrow has its own editable **16 × 16 pixel RGBA PNG**:

```
artwork/shared-resources/core/assets/salts_inventory_update/textures/gui/character_crafting_arrow.png
```

Keep its transparent background and 16 × 16 canvas. Every Core version and loader draws it at native GUI size between the character window's crafting grid and output, with two GUI pixels of clearance on each side. Other container arrows keep their existing atlas sprites. Edit the PNG directly; builds do not regenerate it.

Save the PNG, then run `./gradlew build` (Windows: `.\gradlew.bat build`) to rebuild all release JARs. Individual module builds work too; for example `:26.3:fabric:build` rebuilds only that Core target. Gradle tracks these resource folders as inputs, so editing a shared PNG invalidates the affected resource/JAR tasks automatically. Source JARs use the same resource inputs. You do not need to copy a file between versions or loaders.

Only the owning mod packages its shared folder. Core textures are not copied into Compatibility or Controlify. Public resource identifiers and atlas coordinates have not changed. Preserve the expected atlas size/layout when editing a sheet. `.pdn` files and the empty-folder marker are excluded from runtime resources; export a PNG after editing a PDN. Builds do not regenerate or overwrite manually edited artwork.

Compatibility currently uses the installed third-party mods' native textures. Those keep their native version-specific designs, including AE2's legacy and modern artwork. Salt-owned extension PNGs added later belong in the Compatibility folder above; common artwork does not force different upstream designs to become identical.

Editor references and generation originals remain outside the runtime resources: Core's `window_controls.pdn` is here, and Controlify's preserved generation originals remain in `controlify/versions/26.3/common/art-source/`. Its optional export tools now target the shared PNG folder. Do not run an export over manual PNG edits unless you intend to replace them. See [creative paging buttons](CREATIVE-BUTTONS.md) for their sheet layout and license.

Do not edit generated copies in `build/`, `dist/`, or the old version resource folders. Already installed or uploaded JARs change only after rebuilding and replacing them.

Builds run `verifySharedTextures` to prevent duplicate version-local texture sources. The focused audit is `python tools/verify-shared-textures.py --resources --jars`; run resource processing/builds before checking outputs. See [validation](../functional-tests/SHARED-TEXTURES-VALIDATION.md) for the executed scope.
