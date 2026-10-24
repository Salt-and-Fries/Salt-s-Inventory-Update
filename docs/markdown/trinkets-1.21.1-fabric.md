# Trinkets integration: Minecraft 1.21.1 Fabric

The experimental 1.21.1 Fabric Compatibility build supports **Trinkets 3.10.0**.
Install the rebuilt Core **0.1.5**, Compatibility **0.1.2**, Trinkets and the matching Fabric API
on the client and server. Trinkets supplies Cardinal Components in its distributed JAR.
Trinkets is optional; neither Core nor Compatibility embeds it. The same integration is also
available for [1.20.1 Fabric with Trinkets 3.7.2](trinkets-1.20.1-fabric.md).

Trinkets itself supplies no wearable items. Install an accessory mod or a datapack that assigns
slots and wearable tags. Its native group definitions, ordering, icons, instance counts, item
validators, removal restrictions, effects, persistence and synchronization remain authoritative.

## Character behavior

- Accessories attach to Character's vanilla inventory layout. Hover an armor anchor or the
  visible free-group slot to expose its native horizontal group and vertical instances.
- The normal Character crafting grid, recipe guide, player preview and armor remain available.
  Accessory panels own their input area. Their frames, backgrounds, icons and items render
  above the player preview and owner controls, below later desktop windows and the recipe guide.
  Inactive accessory slots render behind the complete active popup; active-group slots and
  its native anchor retain hit priority where groups overlap.
- Core routes interactions through its single authenticated cursor. Native `canEquip`,
  `canUnequip`, slot validators and quick-move predicates still apply.
- With Trinkets active, Character always shows the real offhand slot. **Return Hotbar to
  Inventory** continues to control the HUD layout; when the HUD also shows the offhand, both
  locations edit the same server-owned stack. No blank accessory-only anchor is needed.
- Expandable accessory bounds reserve placement space, so ordinary multi-slot groups fit
  beside the window without moving it when hovered.

## API boundary

All native code lives in `compatibility/.../compat/trinkets`. The adapter registers a
`DesktopBuiltinAttachment` through `DesktopClientApi` and common equipment rules through
`DesktopPlayerMenuApi`. It never imports Core implementation classes. Its conditional mixin
brackets **Trinkets' native slot rebuild**, calling only Core's public managed-slot hooks.

Core's reusable additions provide built-in attachment lifecycle, native armor/offhand anchors,
real menu-backed rendering/hits, owner-local occlusion, placement reservations, equipment
transfer destinations and stable dynamic-slot identities. They contain no Trinkets imports or
mod-ID checks. Other developers can register their own providers through these same APIs;
see [Desktop window API](desktop_window_api.md#built-in-character-attachments-1211).

The native hover helper ABI is pinned to Trinkets 3.10.0. Unsupported releases are not initialized.
The optional mixin plugin also checks this pin before resolving the native implementation.

## Builds and focused validation

```powershell
.\gradlew.bat :1.21.1:fabric:assemble :compatibility:1.21.1:fabric:assemble
```

The normal root build still builds all release artifacts. Its two relevant production outputs are:

```text
dist/core/salts_inventory_update-1.21.1-Fabric-0.1.5.jar
dist/compatibility/salts_inventory_compat-1.21.1-Fabric-0.1.2.jar
```

The opt-in fixture under `functional-tests/trinkets-1.21.1-fabric` uses actual native accessory
inventories, wearable tags, a removal-locked item and capacity modifiers. It is a separate QA mod
and never enters either production JAR. Run its isolated development profile with:

```powershell
.\gradlew.bat -I functional-tests/trinkets-qa.init.gradle :compatibility:1.21.1:fabric:runClient `
  -PincludeTrinketsRuntime=true -PsaltsSmokeRun=true -PsaltsSmokeProfile=trinkets-focused
```

The development runtime explicitly installs the native bundled CCA modules and aligns Fabric's
resource-loader module with the declared 0.116.1+1.21.1 API. Otherwise optional compile-only
dependencies can select a newer resource-loader ABI without the field used by Trinkets.
Production users install the normal upstream mods; no copied libraries are distributed here.

Focused observations and coverage limits are recorded in
[`TRINKETS-1.21.1-FABRIC-VALIDATION.md`](../../functional-tests/TRINKETS-1.21.1-FABRIC-VALIDATION.md).
