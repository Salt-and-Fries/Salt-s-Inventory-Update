# Kibe windows - Minecraft 1.20.1 Fabric

Implements the approved Kibe designs for **Kibe 1.10.1-BETA+1.20** on Minecraft
1.20.1 Fabric. The thirteen custom container screen types cover twenty-two
views when the five tiers of both generator families are counted separately.
This pass does not port Kibe to other Minecraft releases or loaders.
All shipping changes are in Compatibility; Core and the public API are unchanged.
The later [all-target rollout](kibe-all-targets.md) adds Kibe 2.3.1 on 1.21.1
Fabric and NeoForge. This document retains the original baseline evidence.

| Screen type | Retained working area |
| --- | --- |
| Cooler, placed | Cyan frame and the native food slot |
| Cooler, held | Same compact design, original hand/tag and food storage |
| Big Torch | Complete native supply bar and nine torch slots |
| Block Generator | Cobblestone/Basalt MK1-MK5, native bars and all 27 output slots |
| Breaker | Native nine-slot storage grid |
| Placer | Native nine-slot block grid |
| Drawbridge | Charcoal/gold frame, supply slot and separate cover preview |
| Wither Builder | All seven native ingredient slots and their distinct colors |
| Vacuum Hopper | Teal frame, nine storage slots, complete XP gauge and conversion slots |
| Entangled Chest | Teal frame, all eight live rune colors and linked storage |
| Entangled Bag | Matching rune channel and the actual shared inventory |
| Trash Can | Charcoal frame and native disposal behavior |
| Pocket Trash Can | Same disposal behavior in the approved compact width |

The Pocket Crafting Table uses SIU's existing vanilla crafting integration.
The Chunk Loader configuration screen keeps its native presentation. Kibe's
other non-container interactions receive no invented inventory windows.

## Presentation and behavior

- Every custom view uses a dedicated native template with the approved window
  size, working-panel source rectangle and complete eighteen-pixel slot rims.
  Colored slots, ghost icons, bars and native rendering remain intact.
- The embedded player inventory is removed; the ordinary SIU Inventory window
  remains separate. The gallery crops the actual window from each game capture.
- Normal SIU window management remains available. Headers use the existing
  adaptive More/Close arrangement when the full control set, including valid
  storage actions, would not fit. No nonfunctional decorative controls are added.
- Native ExtendedScreenHandlerFactory opening data, menus, transactions and
  item permissions remain under Kibe's control. The integration preserves
  position-based and held-item opening payloads through Core's existing native
  transport rather than replacing Kibe's menus.
- Sorting eligibility is limited to homogeneous storage groups. Food, torch
  supply, Wither ingredients, Drawbridge cover, disposal and XP conversion
  cells are excluded. Vacuum Hopper storage alone is eligible.
- The held Cooler registers an existing public source lease, including offhand
  use. An optional Kibe-only tick guard preserves its native rule that feeding
  pauses while its item menu is open. Closing resumes Kibe's own feeding; valid
  food NBT writes do not invalidate the held source.
- Frame textures release on window close and regenerate after resource reload.
  Optional initialization and the mixin target guard keep Kibe optional.

## Focused verification

The [verification record](../output/kibe-implemented/verification.json) records
the final jar hashes, production-client journals and coverage limits. The
[all-window gallery](../output/kibe-implemented/all-windows.png) contains all
twenty-two real game views. Full-screen captures and active-state examples are
linked in the verification record.

All **184 focused checks passed**. They cover all twenty-two dedicated routes, exact approved bounds,
all 375 native slot envelopes, and exclusion of the embedded player inventory.
Functional checks exercise native insert/extract and disposal across the
applicable screen types; invalid-item rejection; representative shift transfer;
Cooler source protection, feeding pause/resume, persistence and offhand use;
both generator families producing items; filled Vacuum XP gauge, bottle/bucket
conversion and item collection; colored rune linkage and shared Chest/Bag
withdrawal; torch supply, Drawbridge cover limits and all Wither ingredients.
The inherited Pocket Crafting Table is exercised through result updates,
crafting and return of unused input. The Chunk Loader is checked as a native
screen. Window checks cover dragging, slot input after moving, pin/unpin,
lock/unlock, focus, More and independent close.

The [active-state gallery](../output/kibe-implemented/active-windows.png) shows
filled XP, supplied machines, generator output and non-default rune channels.

These are affected-window checks, not a complete functional or gameplay suite.
The test uses the original publisher jar and a disposable integrated-server
world with an excluded observation mod. Dedicated multiplayer, every world
automation/redstone configuration, spawning a Wither, all generator timing
profiles, alternate resource packs, translations, controller input and other
optional-mod combinations are not exhaustively tested. Storage sort membership
is reviewed in code; every possible sorting permutation is not exercised.

Publisher baseline: [release fhSgEP7Z](https://modrinth.com/mod/kibe/version/fhSgEP7Z)
and [source revision a4ba6fa](https://github.com/lucaargolo/kibe/tree/a4ba6fab55119d9fcf972ee402f11b74fdc2cd9a).
The approved design remains in [the mockup PDF](../output/pdf/siu-kibe-window-mockups.pdf).
Fixtures and observation helpers are not packaged in the shipping jar.
