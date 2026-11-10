# Project Everything - current remaining work

Updated November 4, 2026, after completing the six native-screen preservation
checks and the Tech Reborn all-target rollout. Earlier milestones include
implementing Ad Astra's fourteen approved
inventory screen types and overhauling all four Giselle Addon windows on
1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge, and after auditing the current
1.20.1 Extended AE releases and implementing their approved window designs.
Tech Reborn's approved 1.20.1 Fabric windows are now also implemented.
Completed integrations and the deferred Create
addon group are excluded from this implementation queue.

| Mod | Audited inventory UI types |
| --- | ---: |
| No remaining inventory-window mods in the current 1.20.1 queue | 0 |
| **Total: 0 mods** | **0** |

Counts describe Minecraft 1.20.1 concrete screen types, not every tier,
texture or captured state, and do not imply one new renderer per type.
Other releases may differ. Extended AE's 31-type union is complete for
1.20.1 Fabric/Forge; the available 1.21.1 and 26.1.2 NeoForge ports are also
implemented with their modern AE2 designs. See the
[complete target table](../../compatibility/ae2/extended-ae-all-targets.md).
Its four read-only pattern inspectors retain their native presentation.

## Native-screen preservation checks: complete

These six entries retain their normal native presentation. Fresh checks with
shipping SIU confirmed original screen ownership, input and closing on their
audited targets:

- DungeonZ
- ElevatorMod
- Sortilege
- Armor Statues
- Things
- Wraith Waystones (the user's Wraith waypoints entry)

See [results, screenshots and coverage limits](../../compatibility/native-screen-preservation.md).
No production changes were required. This records focused UI checks, not a
complete gameplay or all-version test.

Additional Create addons remain deferred at the user's request.

## Recent completions

- Tech Reborn: all 51 approved machine screens on 1.20.1 Fabric, with the
  upgrade tray on the right and settings tabs directly on the left. Native
  inspectors, equipment cells, live properties and populated units are retained.
  The newer official Fabric releases are also covered on 1.21.1, 26.1.2, 26.2
  and 26.3, including the 6.x Nuclear Reactor. See
  [all-target coverage](../../compatibility/tech-reborn-all-targets.md).

- Extended AE: all 31 audited inventory/configuration screen types across
  1.20.1 Fabric 1.0.2 and Forge 1.4.18, including the conditional AE2WTLib
  and AppliedE variants. Replace wells have complete native rims; terminal
  mode tabs stay outside the body. See [implementation and focused coverage](../../compatibility/ae2/extended-ae-1.20.1.md).
  Modern 1.21.1/26.1.2 NeoForge ports include their native additional tools and
  configuration views; all available SIU targets are covered. See the
  [target table](../../compatibility/ae2/extended-ae-all-targets.md).

- Ad Astra: all fourteen approved inventory screen types, plus all four
  overhauled Giselle Addon windows, on 1.20.1 Fabric/Forge and 1.21.1
  Fabric/NeoForge. Native frames, circuitry, monitors and controls are
  preserved. See [implementation and focused coverage](../../compatibility/ad-astra-all-targets.md).

- Kibe: all thirteen custom container screen types on 1.20.1 Fabric and
  1.21.1 Fabric/NeoForge, covering twenty-two views per target, plus existing
  Pocket Crafting integration. Chunk Loader remains native. These are all
  official Kibe release targets overlapping SIU. See
  [implementation and focused coverage](../../compatibility/kibe-all-targets.md).

- Powah!: all eleven audited container screen types on 1.20.1 Fabric/Forge,
  1.21.1 NeoForge and 26.1.2 NeoForge, plus Ender Gate using the Ender Cell
  screen. These are all official Powah releases overlapping SIU's target set.
  See [applicable targets and focused coverage](../../compatibility/powah-all-targets.md).

- Simple Storage Network: all ten 1.20.1 Forge screen types, plus the three
  newer containers on 1.21.1 and 26.1.2 NeoForge. See
  [implementation and focused coverage](../../compatibility/simple-storage-network.md).

- Zenith Renewed: all seven container screen types across nine workstation
  views on 1.20.1 Fabric, preserving native artwork and actions. See
  [implementation and focused coverage](../../compatibility/zenith-1.20.1.md).

- The Aether: five audited UI types implemented on 1.20.1 Fabric/Forge and
  1.21.1 Fabric/NeoForge, plus
  the conditional Quark Oddities Backpack using its Inventory attachment.
  These are all official release targets overlapping SIU's target set. See
  [implementation and focused coverage](../../compatibility/aether-all-targets.md).
- Enchanted: Witchcraft: five approved windows on 1.20.1 Fabric/Forge and
  1.21.1 Fabric/NeoForge, including adaptation to Enchanted 4.x. See
  [applicable targets and focused coverage](../../compatibility/enchanted-all-targets.md).
- Modern Industrialization: approved windows implemented for its applicable targets.
- Quark: completed 1.20.1 Forge and 1.21.1 NeoForge layouts, including the attached Backpack.
- AE2 Wireless Terminals: completed 1.20.1 Fabric/Forge and 1.21.1,
  26.1.2, 26.2 and 26.3 NeoForge; shared return-tab fixes shipped in all six
  affected builds. See [modern implementation](../../compatibility/ae2/wireless-terminals-modern.md)
  and `output/ae2-tab-join-fix/verification.json`.

The [historical audit scope](project-everything-remaining-window-scope-1.20.1.md)
retains the original shortlist, captured-view counts and conditional evidence.
Tech Reborn's 51-screen count was reconfirmed against its 5.8.14 registry and
fresh native openings. Its approved 1.20.1 Fabric designs are now implemented
and captured in game; [the design report](tech-reborn-window-design-1.20.1.md)
remains historical design evidence. Extended AE's count is
from the current release audit described above; implementation checks for
completed mods are linked above.
