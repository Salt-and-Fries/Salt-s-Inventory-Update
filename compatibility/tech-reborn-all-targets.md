# Tech Reborn: supported SIU targets

The approved native Tech Reborn windows are available in Compatibility 0.1.3
for every official Tech Reborn release overlapping SIU's current targets.
Use matching Core 0.1.6 and Fabric API; install Tech Reborn separately.
The publisher's Tech Reborn jars include their matching Reborn Core.

| Minecraft | Loader | Pinned Tech Reborn / Reborn Core | Machine screen types |
| --- | --- | --- | ---: |
| 1.20.1 | Fabric | 5.8.15 | 51 |
| 1.21.1 | Fabric | 5.11.19 | 51 |
| 26.1.2 | Fabric | 6.0.5 | 52 |
| 26.2 | Fabric | 6.1.1 | 52 |
| 26.3 | Fabric | 6.2.1 | 52 |

There is no official matching Tech Reborn release for SIU's 1.21.11 target,
or for its Forge/NeoForge targets. Those Compatibility jars do not advertise
Tech Reborn support. Historical Forge releases outside SIU's supported versions
are outside this rollout. Availability is based on the publisher's
[Modrinth versions](https://modrinth.com/mod/techreborn/versions) and
[GitHub releases](https://github.com/TechReborn/TechReborn/releases).
Pinned publisher metadata is saved in
[the target manifest](../output/tech-reborn-all-targets/targets.json).

## Presentation and native behavior

- Settings tabs attach directly to the left; upgrade cells form a tray on the
  right. Its right and bottom gutters use opaque window material.
- Original apparatus, slot backgrounds, energy/fluid gauges, labels and
  controls come from the installed release. Embedded player inventory is
  separate; actual equipment charging cells remain part of the machine.
- Native item/fluid inspectors preserve face configuration, permitted pencils,
  checkboxes and help. Hologram, lock and machine-specific controls retain
  their native behavior.
- The 6.x Nuclear Reactor retains all 54 native cells, locked-column rules,
  heat display and its energy/hologram attachment.
- Manual and external-link screens keep their native presentation.

Typed releases retain their original opening data, authenticated menu,
payload codecs, registered action receivers and slot permissions. Property
updates are scoped to the owning SIU session. The 26.2+ property format and
26.3 input event changes are handled separately. Native machine-face picture
states on 26.x are submitted outside capture using the same native renderer.

## Focused verification

Each applicable target was launched with original publisher jars and the exact
shipping Compatibility bytes. The newer ports were checked across every
registered machine screen for complete slot envelopes, tab placement and opaque
upgrade gutters. Representative actions cover item and fluid configuration,
redstone, upgrade insertion/removal, pump/detector/energy controls and closing.
Formed blast-furnace heat was checked from server to client on every target.
The 6.x reactor was checked for shared-menu identity, fuel insertion, locked
columns and hologram controls. The latest 1.20.1 publisher patch reuses the
previously approved adapter and retained gutter fix.

See [per-target results and hashes](../output/tech-reborn-all-targets/verification.json)
and the [full SIU target table](../output/tech-reborn-all-targets/compatibility-table.md).
Observer code and upstream classes are excluded from distributed jars.

These are focused UI and representative action checks, not a complete
recipe, automation or gameplay suite. The 26.2/26.3 Core renderer additionally
rejects empty clipped native fragments before submission. Both Fabric and
NeoForge Core jars include that internal fix; public API class bytes remain
identical. The renderer classes match between loaders. Tech Reborn is tested
live on Fabric; no separate NeoForge Core runtime is claimed by this rollout.
The pinned 6.0.5 publisher resources also log missing `reborncore:padded`
recipe serializers at world load. This rollout checks its windows and native
configuration actions; it does not certify those recipes.

Only the four additional Fabric Compatibility jars and the four 26.2/26.3 Core jars
change in this rollout. The existing 1.20.1 Fabric Compatibility jar is retained.
Release checksums and the all-latest bundle are updated; unrelated release
artifacts and bundle contents are verified unchanged.
