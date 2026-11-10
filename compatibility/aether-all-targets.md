# The Aether across supported SIU targets

All five approved Aether interfaces are implemented wherever official Aether
releases overlap SIU's supported Minecraft/loader combinations. Core and the
public API are unchanged.

| Minecraft | Loader | Reviewed Aether release | Coverage |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | 1.5.2-beta.9-fabric | Existing implementation and focused live checks retained |
| 1.20.1 | Forge | 1.5.2-neoforge | Existing implementation and focused live checks retained |
| 1.21.1 | Fabric | 1.5.11-fabric | Five interfaces; 64 focused live assertions |
| 1.21.1 | NeoForge | 1.5.10-neoforge | Five interfaces; 71 focused live assertions |

Publisher availability was checked November 2, 2026 against the
[official releases](https://modrinth.com/mod/aether/versions). There are no
official Aether releases for SIU's 1.21.11, 26.1.2, 26.2 or 26.3 targets.
Minecraft releases outside SIU's existing target set and unofficial forks
are not additional Core ports.

## Preserved behavior

Altar, Freezer and Incubator keep their native slots, recipe books, item
validation and processing indicators. Book of Lore keeps its complete pages,
inspected-item slot and native Previous/Next actions.

Cloud switches the existing Character window into the native Aether upper
inventory arrangement. It opens no additional equipment window. Existing
crafting, armor and offhand slots remain real player-menu slots; the eight
Aether slots use an authenticated native equipment session. Its client-side
shadow inventory cannot overwrite the live wearable capability during sync.

The general Accessories/Curios panel and Aether Character mode replace each
other. Native visibility controls and the crown's Moa Skins screen remain
available. Returning from Moa Skins retains Character mode. Quark Oddities
Backpack remains attached to Inventory, with its existing close/move lifecycle.

## Modern adaptation

Aether 1.21.1 embeds Accessories on both loaders. Its native equipment menu
is now `AetherAccessoriesMenu`, its slots precede the ordinary inventory, and
its slot identifiers are namespaced. The adapter discovers actual native
`AccessoriesBasedSlot` instances and uses their original ghost icons rather
than relying on legacy slot indices or guessed names.

Modern Fabric and NeoForge payloads open the equipment session. Visibility
actions call Accessories' current server handler and native synchronization.
The launcher and crown use Aether's current GUI sprites; the small visibility
widgets use the same Minecraft sprites as the native Accessories renderer.
The private Character hooks remain version-local Compatibility mixins.

## Focused verification

Production Core/Compatibility JARs and unmodified publisher JARs were tested
in isolated integrated-server worlds at GUI scale 2. Fabric used Accessories
1.1.0-beta.48 embedded in Aether and owo-lib 0.12.15.4. NeoForge used Accessories
1.1.0-beta.54, owo-lib 0.12.15.6-beta.1, Quark 4.1-488, Zeta 1.1-40 and Quark
Oddities 1.21.1 on NeoForge 21.1.252, which that Quark release requires.

The NeoForge client uses the named development launcher with shipping subject
JAR bytes. Accessories beta.48's renderer targets a production synthetic
lambda name, preventing that launcher from starting. The unmodified current
beta.54 release includes the development-name target too. No publisher JAR
was patched and no Accessories copy is bundled in Compatibility.

The 135 focused live assertions cover all five interfaces: real processing
and result transfers, active incubation, Lore inspection/navigation, native
equipment restrictions, equip/remove/re-equip, visibility, crafting/cursor
preservation across modes, armor/offhand, recipe controls, crown return,
launcher exclusivity, repeated toggles, Character lifecycle and movement.
The NeoForge run also checks Backpack's Inventory attachment and 27 slots.

Six layout tests passed. Per loader, 6,518 logical pixels matched the exact
publisher artwork within one RGB level of framebuffer rounding: nine complete
workstation/Lore slot frames, the entire Incubator meter and all eight
Character equipment frames/ghosts. Native visibility widgets overlap the
equipment frames and are excluded from the bare-atlas comparison; their
rendering was inspected and their behavior tested separately.

These are affected-menu checks, not a complete gameplay suite. Dedicated
multiplayer, other GUI scales, arbitrary equipment datapacks, supporter-only
customization and a full Moa hatch cycle were not tested. Existing 1.20.1
evidence is retained without rerunning those clients. One Fabric probe was
resumed after adapting its cape lookup to the new namespaced slot identifier;
the full attempt journal is preserved alongside the distinct assertion count.

Evidence: [verification](../output/aether-all-targets/verification.json),
[all twelve targets](../output/aether-all-targets/compatibility-table.md),
[Fabric Character](../output/aether-all-targets/fabric-character.png),
[NeoForge Character and Backpack](../output/aether-all-targets/neoforge-character.png).
The [1.20.1 report](aether-1.20.1.md) retains the legacy implementation details.

Both new Compatibility JARs are staged in `dist/compatibility` and the latest
release bundle. Existing 1.20.1 JARs, Core, Controlify and other bundle entries
retain their bytes. The test observer is excluded from the delivered JARs.
