# Enchanted: Witchcraft across supported targets

The approved five-window design is available in every official Enchanted
release target that overlaps SIU's supported Minecraft/loader combinations.
Core and the public API are unchanged.

| Minecraft | Loader | Reviewed Enchanted release | Windows | Verification |
| --- | --- | --- | --- | --- |
| 1.20.1 | Fabric | 3.1.14 | All five | Prior focused live checks retained |
| 1.20.1 | Forge | 3.1.14 | All five | Prior build/reobfuscation and native-symbol checks retained |
| 1.21.1 | Fabric | 4.2.7 | All five | 33 focused live assertions passed |
| 1.21.1 | NeoForge | 4.2.7 | All five | 33 focused live assertions passed |

The windows are Witch's Oven, Spinning Wheel, Distillery, Poppet Shelf and the
slot-free Altar status display. Their compact native layouts, slot artwork,
processing indicators and ordinary SIU controls follow the approved design.
Player inventory remains separate; guidebooks retain their native presentation.

Publisher availability was checked November 2, 2026 against the
[official release list](https://modrinth.com/mod/enchanted-witchcraft/versions).
There are no official releases for SIU's 1.21.11, 26.1.2, 26.2 or 26.3 targets.
Older Minecraft versions supported by Enchanted but outside SIU's target set
are not new Core ports. Unofficial forks are not claimed as supported.

## Modern adaptation

Enchanted 4.x moves the five native atlases to `textures/gui/menus/` and changes
its client menu constructors to accept `BlockPos` directly. The modern adapter
retains the original owning position and calls those constructors. The Altar
and Shelf now expose their position through private `ContainerLevelAccess`
fields; the legacy public Altar owner and Shelf inventory accessor are not
used on modern releases.

The native Altar renderer retains its new decimal recharge multiplier. All
native recipe, item-component, shelf-persistence and power behavior remains in
the upstream menus. No copy of Enchanted's implementation or textures is bundled
in Compatibility.

## Focused verification

Both modern loaders were checked with original publisher Enchanted 4.2.7,
StateObserver 1.6.4, Modopedia 1.1.11 and GeckoLib 4.9.3 JARs. The Fabric
fixture additionally uses the publisher Forge Config API Port. The delivered
Compatibility JAR hashes match those recorded by the live test profiles.

For each modern loader, 33 focused live assertions covered:

- Opening all five native menus with their correct owners.
- Oven fuel/progress synchronization, real charcoal production and retrieval.
- All four Shelf positions and transfers of functional poppets.
- Live Altar capacity, power and decimal multiplier synchronization.
- Wheel processing, poppet production and result retrieval.
- Distillery processing, all four recipe outputs and transfers to Inventory.
- Title movement and Close/reopen behavior for all five windows.

Five geometry tests passed on the modern shared source. All 20 empty slots and
ghost icons matched 6,480 native texture pixels on each loader. Both complete
five-window workspaces were visually inspected. The modern power fixtures use
Enchanted's native saved-data format in disposable worlds; they do not validate
every environmental power source or every poppet's combat effect.

This was focused testing, not the complete gameplay suite. Dedicated-server
multiplayer and exhaustive GUI-scale coverage were not run. The existing
1.20.1 Forge evidence remains build-level; this port does not claim a new live
Forge gameplay run.

Evidence and the twelve-target applicability table:
[verification summary](../output/enchanted-all-targets/verification.json),
[target table](../output/enchanted-all-targets/compatibility-table.md),
[Fabric screenshot](../output/enchanted-all-targets/fabric-all-five-windows.png),
[NeoForge screenshot](../output/enchanted-all-targets/neoforge-all-five-windows.png).
The [original implementation report](enchanted-1.20.1.md) retains the legacy
coverage and approved dimensions.

Only the two 1.21.1 Compatibility JARs, their checksums and corresponding bundle
entries changed. Existing 1.20.1 JARs, Core and Controlify retain their original
bytes. The runtime observer is excluded from shipped JARs.
