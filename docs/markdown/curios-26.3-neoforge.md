# Curios integration: Minecraft 26.3 NeoForge

For the completed rollout to Forge 1.20.1 and the other supported NeoForge versions, see the [general Curios guide](curios.md).

This experimental adapter targets **Curios 17.0.0-beta.3+26.3**, Core **0.1.5** and
Compatibility **0.1.2** on Minecraft **26.3 NeoForge**. Install the rebuilt Core and
Compatibility together on client and server, alongside Curios and the accessory mods or
datapacks that allocate its slots. Curios itself supplies no wearable content.

Curios remains optional. Neither Core nor Compatibility embeds its code or artwork.
The adapter stays inactive without Curios, and unsupported Curios versions retain native UI.
This work does not enable Curios integration on other Minecraft/loader targets.

## Character panel

- Character retains its normal inventory layout and gains the native Curios button beside
  the player preview. The configured Curios inventory key also toggles the attached panel
  from gameplay or an already-open desktop when desktop sessions are available. Text editing
  retains priority, and holding the key does not repeatedly toggle the panel. Native behavior
  remains available when desktop sessions are disabled.
- The panel always sits above Character at exactly the same width, with a four-GUI-pixel
  gap between them. Its cosmetic tab projects above the panel. The two windows move, raise
  and fit to the screen together.
- All allocated visible slots fill the row grid. Character moves down when extra vertical
  space is needed. When the complete grid still cannot fit, Character reaches the bottom
  and the panel uses a scrollbar. Wheel scrolling and dragging the thumb expose the other rows.
- The cosmetic tab switches between native equipment and cosmetic slots, using native
  purple artwork. Groups without cosmetic counterparts retain their equipment slots.
- Native type ordering, empty-slot icons, display-stack extensions, tooltips, inactive labels,
  visibility controls and equip/unequip permission checks remain authoritative.
  Rightmost visibility toggles retain all eight pixels and matching click bounds, including
  when the panel has a scrollbar.
- Closing Character also closes the accessory session. Closing the panel with its button or
  binding leaves Character available. The panel is not independently movable or pinnable.

## Public API and inventory ownership

All Curios code lives in `compatibility/versions/26.3/neoforge/.../compat/curios`.
Core implements general built-in control contributions and joined-window layout; it contains
no Curios imports, checks, mixins or dependency. The compatibility compiler receives only
Core's public API JAR.

The accessory menu uses native `CurioSlot` objects and the real Curios resource handlers on
the server. Equipment and cosmetic counterparts keep stable, separate menu indices. Core
owns the authenticated cursor and normal slot gestures across windows. Public menu bindings
identify each slot's real backing handler and index instead of the transfer slots' shared dummy
vanilla container. Client snapshots use separate native resource handlers so menu prediction
cannot overwrite the live wearable inventory.

Cosmetic and visibility actions use bounded, session-scoped Core payloads. Native Curios
render packets synchronize visibility with the owning player and tracking players. Capacity
or backing-inventory changes replace the detached session through the public transition API,
invalidating stale gesture tokens before the slot topology changes.

## Build and focused checks

```powershell
.\gradlew.bat :26.3:neoforge:assemble :compatibility:26.3:neoforge:assemble
.\gradlew.bat :compatibility:26.3:neoforge:testCuriosLayout
```

The target production artifacts are written under the two modules' `build/libs` directories.
The normal root release build continues to collect all targets into `dist/core` and
`dist/compatibility`. Native Curios is compile-only. For an isolated development client:

```powershell
.\gradlew.bat :compatibility:26.3:neoforge:runClient `
  -PincludeCuriosRuntime=true -PsaltsSmokeRun=true -PsaltsSmokeProfile=curios-character-focused
```

Use a disposable world with a Curios slot datapack. The native command for changing the sample
ring count is `/curios set ring @s 100`. Curios beta.3 currently throws while formatting that
command's success message after applying the change; this is an upstream command issue.

Focused executed checks and remaining coverage limits are recorded in
[Curios validation](../../functional-tests/CURIOS-26.3-NEOFORGE-VALIDATION.md).
