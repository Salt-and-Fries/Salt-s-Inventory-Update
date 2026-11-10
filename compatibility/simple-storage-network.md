# Simple Storage Network windows

Implements the approved mockups using the publisher's **SSN 1.13.2** screens,
menus, codecs and action handlers. All shipping changes are in Compatibility;
Core and the public API are unchanged.

| Minecraft | Loader | Container screen types | FLib used for live checks |
| --- | --- | ---: | --- |
| 1.20.1 | Forge | 10 | 0.0.16 |
| 1.21.1 | NeoForge | 13 | 0.2.9 |
| 26.1.2 | NeoForge | 13 | 0.2.11 |

These are the published targets overlapping SIU's supported target set, checked
against the publisher version list on November 2, 2026. SSN has no published
Fabric build. The three newer containers are not present in the 1.20.1 release.

The ten base screen types are Storage Inventory, Storage Request Table,
Expanded Request Table, Storage Remote, Crafting Remote, Expanded Remote,
Filtered Import Cable, Export Cable, Filtered Link Cable and Network Collection
Interface. The newer releases add Processing Cable, Network Storage Cradle
and Sub-Network Receiver.

## Presentation and behavior

- One gray SIU frame surrounds the original working panel. The player inventory
  opens separately. Titles use the publisher's existing translated names.
- Native network list capacities remain 8 x 9, 4 x 9 and 21 x 25, respectively.
  Expanded views retain all 525 visible entries and require a sufficiently large
  logical viewport; the reference and verification captures use GUI scale 1.
- Search, display order, direction, autofocus, crafting inputs/output, clear-grid
  controls, upgrades, ghost filters, priority, redstone, operation selectors and
  integer limits retain the original controls and relative placement. There is
  no invented export allowlist toggle. Virtual-list ordering is distinct from
  sorting an actual chest, so its native controls are retained.
- Native menus retain item restrictions, quick move, crafting and remote access
  checks. Held inventory remotes are protected as item sources while open.
- Native mouse, keyboard, focus and ghost-scroll actions route to the
  authenticated owning session. The publisher's explicit item response is
  committed through SIU's public shared-cursor API. List updates are associated
  with their owning window and fragmented within the existing payload bound.
- Legacy item counts use the local GUI pose when captured. Integer widgets
  receive an empty narration label so their missing native label cannot make
  the native host decline the screen.

## Focused verification

The saved [verification record](../output/ssn-implemented/verification.json)
contains exact tested JAR hashes, per-target assertion counts and capture paths.
The [comparison PDF](../output/pdf/siu-simple-storage-network-implemented.pdf)
contains all thirteen native/implemented designs plus a full-screen example.

Focused checks cover each available menu, full native slot/widget envelopes,
exclusion of embedded player cells, populated list synchronization, search,
withdrawal/deposit, crafting output and grid clearing, ghost edits and scrolling,
nonconsuming filters, priority/allowlist controls where present, operation
upgrades and typed limits, cradle/receiver insertion and retrieval, processing
controls and both ghost grids, and representative pin/unlock/move/close/reopen
behavior with another window open. These are affected-menu checks, not a full
functional or gameplay suite.

The three Compatibility targets assemble successfully. The source coverage
audit passes. The broader repository module-boundary source audit still reports
pre-existing references to Core internals in other integrations, including
Aether, Travelers Backpack, Quark and Aircraft. The SSN sources have no forbidden
Core implementation references.

Dedicated multiplayer, alternate resource packs and optional JEI/Curios
combinations were not exercised. Native crafting recipes, automation throughput,
every possible remote range/dimension configuration and every item adapter are
not exhaustive gameplay coverage. In-memory native filter/capability state is
inspected directly by the excluded test observer; some publisher fields are
not represented in the block's serialized NBT.

Publisher references: [source](https://github.com/Lothrazar/Storage-Network),
[release inventory](https://modrinth.com/mod/simple-storage-network/versions).

The excluded fixtures and observer are not packaged in the release JARs.
