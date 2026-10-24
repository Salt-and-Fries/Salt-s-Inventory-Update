# Curios integration

Salt's Inventory Compatibility 0.1.2 integrates Curios with Character on every supported
Minecraft/loader combination that has an upstream Curios release. Install the matching
rebuilt Core 0.1.5 and Compatibility 0.1.2 on both client and server, plus the listed native
Curios release and the accessory mods or datapacks that allocate its slots.

| Minecraft | Loader | Reviewed Curios release |
| --- | --- | --- |
| 1.20.1 | Forge | 5.14.1+1.20.1 |
| 1.21.1 | NeoForge | 9.5.1+1.21.1 |
| 1.21.11 | NeoForge | 14.0.0+1.21.11 |
| 26.1.2 | NeoForge | 15.0.0+26.1.2 |
| 26.2 | NeoForge | 16.0.0+26.2 |
| 26.3 | NeoForge | 17.0.0-beta.3+26.3 |

The [upstream release listing](https://modrinth.com/mod/curios/versions) and
[Maven repository](https://maven.theillusivec4.top/top/theillusivec4/curios/) were reviewed
on 2026-10-23. There are no Curios releases for our six Fabric targets. Trinkets remains
available on [1.20.1 Fabric](trinkets-1.20.1-fabric.md) and
[1.21.1 Fabric](trinkets-1.21.1-fabric.md). Curios is optional: its absence leaves the normal
Character window available, and unsupported releases retain native Curios UI.

## Panel and controls

- Character's native Curios button or the configured Curios inventory binding opens a
  panel above Character, at the same width and with a four-GUI-pixel gap.
- Both windows move, raise and remain on screen as one group. Character moves down to fit
  all slots when possible. Only an allocation that still cannot fit receives a scrollbar.
- The native cosmetic tab sticks out above the panel. It switches equipment and cosmetic
  views without changing the server-owned cursor or creating another item inventory.
- The inventory binding works from gameplay and inside an open desktop. Unconsumed key
  events preserve text-editing priority; a held-key guard prevents repeat toggling.
- Native slot order, icons, tooltips, display stacks where supported, active states,
  visibility controls, equip/unequip rules and synchronization remain authoritative.
- Rightmost visibility controls render their complete eight-pixel width and share the
  rendering viewport's hit boundaries, including beside a scrollbar.
- Closing Character closes the attached session. Closing only the accessory panel keeps
  Character available. Capacity changes replace the session and invalidate stale input.

Artwork comes from each installed Curios release. The 1.20.1 port uses that release's
native texture sheets; later versions use their native GUI sprites and slot icons. Neither
Core nor Compatibility copies or embeds Curios classes or textures.

## Public API boundary

All adapters live in `compatibility/versions/<minecraft>/<loader>/.../compat/curios` and
compile against Core's public API JAR. Core has no Curios import or dependency.

The reusable Core hooks are available across supported mappings:

- `DesktopClientApi.registerBuiltinAttachment`: contributes the Character launcher and
  receives its render, click, tooltip and lifecycle callbacks.
- `DesktopWindowDefinition.attachment` and `DesktopWindowAttachment`: joins a separately
  authenticated menu above an owner, with preferred height, tab clearance and gap. Core
  fits the group and closes the child when its owner becomes unavailable.
- `DesktopClientApi.isDesktopAvailable`: checks enabled, negotiated desktop availability.
- `DesktopMenuBindings.maxMenuSlots`: reports the protocol's bounded menu capacity.
- Existing menu factories, server opening data, bounded session payloads, physical-slot
  bindings and session transitions retain ownership and synchronization.

Older Curios releases use separate native `CurioSlot` and `CosmeticCurioSlot` classes.
Those ports retain both classes so cosmetic slot contexts and native callbacks stay intact.
Client snapshot handlers are independent of the live wearable inventory. The adapter uses
the desktop menu index for network actions and the native handler index for physical item
ownership; these indices are deliberately distinct.

## Build and validation

A normal root build packages both modules for all targets. To build an individual pair:

```powershell
.\gradlew.bat :1.20.1:forge:assemble :compatibility:1.20.1:forge:assemble
.\gradlew.bat :compatibility:1.20.1:forge:testCuriosLayout
```

Replace the version and loader with another row from the table. The opt-in development
property `-PincludeCuriosRuntime=true` adds that target's pinned upstream Curios JAR to
`runClient`; it never embeds Curios in a release artifact. Legacy Forge's development
classpath remaps this runtime dependency to named Minecraft members.

Production Core and Compatibility JARs are collected under `dist/core/` and
`dist/compatibility/`. Match their Minecraft version and loader. Curios remains optional
and compile-only in normal release builds.

See the [rollout validation record](../../functional-tests/CURIOS-ROLLOUT-VALIDATION.md)
for executed checks and coverage limits. The original
[26.3 guide](curios-26.3-neoforge.md) and
[focused visual record](../../functional-tests/CURIOS-26.3-NEOFORGE-VALIDATION.md) remain
available for that initial implementation.
