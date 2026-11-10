# Zenith Renewed windows - Minecraft 1.20.1 Fabric

Implements the approved Zenith Renewed **2.0.2** mockups in the Compatibility
add-on. This publisher release targets 1.20.1 Fabric, the applicable SIU target.
Core and the public API are unchanged.

All-target availability was rechecked against the live publisher version list
on November 2, 2026. Its releases cover only 1.20.1 Fabric, so the existing
integration covers every applicable SIU version/loader target. The standalone
JAR, latest-bundle entry and both checksum records were verified against the
tested artifact. See the [availability table](../output/zenith-all-targets/compatibility-table.md)
and [packaging verification](../output/zenith-all-targets/verification.json).

| Workstation | Native screen | Window size |
| --- | --- | --- |
| Enchanting Table | Enchanting | 178 x 126, plus native information tab |
| Enchantment Library | Library | 190 x 176 |
| Library of Alexandria | Library variant | 190 x 176 |
| Fletching Table | Fletching | 176 x 93 |
| Salvaging Table | Salvaging | 190 x 120 |
| Gem Cutting Table | Gem cutting | 190 x 126 |
| Reforging Table | Reforging | 190 x 212 |
| Simple Reforging Table | Reforging variant | 190 x 212 |
| Augmenting Table | Augmenting | 190 x 168 |

These nine workstation views cover all seven concrete container screen types.
The player inventory remains separate. Engraved frames, wooden lists, circular
slots, rune animations, choice/cost artwork, the fletching arrow and enlarged
result border remain native. The SIU frame uses the approved sampled palettes.
The normal management controls appear when they fit; long titles use More and
Close. Processing, filter and candidate slots are not exposed as sortable storage.

Native menus still own ingredient restrictions, crafting, salvaging, upgrading,
reforging, sigil/material/experience costs, enchantment extraction and item return
on close. Extended menu factories receive the original block position. Library
actions are routed to the authenticated owning menu, preserving its point/level
checks and native search, scrolling and extraction behavior.

Enchanting stats and all offer clues are synchronized together to their owning
window. Native generation is scoped to the actual enchanting menu even when a
cross-window transfer originates in Inventory. The information tab opens the
native full-screen detail interface; Escape returns to the same desktop session.
The Chronicle guidebook and shared vanilla anvil/brewing/smithing/trading
workflows retain their existing presentation.

## Focused verification

- Six geometry tests passed, covering full slot envelopes, original inverse
  input mapping, retained artwork, both enlarged/custom slot shapes, the
  information tab and exclusion of the embedded player inventory.
- All **62 live assertions** passed in production Fabric with an integrated
  server and GUI scale 2. Four enchanting checks were repeated on the final
  JAR after refining only the information-screen handoff to use Zenith's own
  screen directly, preserving native widget navigation and narration.
- Live coverage includes all nine workstation layouts; fletching production;
  deposit/search/scroll/extract/withdraw in both libraries; gem rarity upgrades
  in place; salvage output; all three reforging choices and material costs at
  both tables; augmenting selection, upgrade and reroll with sigil costs;
  enchanting stats/clues, full-screen information return and enchantment
  application; and representative pin, lock, focus, drag, close/reopen and
  moved-window input checks.
- Three complete static working panels matched **252,736 pixels** from the
  unchanged native captures exactly. All nine active workstation designs were
  also visually reviewed.
- The staged JAR is byte-identical to the JAR used in the final focused rechecks.
  It contains no upstream classes or excluded observation fixture. The latest
  bundle and compatibility checksums are updated; every unrelated bundle entry
  retains its previous bytes.

This was focused verification of the affected menus, not a complete functional
or gameplay suite. Dedicated multiplayer, other GUI scales and exhaustive
recipe/enchantment combinations were not tested.

Evidence: [verification manifest](../output/zenith-implemented/verification.json),
[live assertion journal](../output/zenith-implemented/journal.json),
[native pixel comparison](../output/zenith-implemented/artwork-verification.json).

Reproduction:

- `tools/prepare-zenith-implementation.py`
- `functional-tests/zenith-windows/focused.py`
- `functional-tests/zenith-windows/verify_artwork.py`
- `tools/stage-zenith-release.py`

Primary references: [publisher release](https://modrinth.com/mod/zenith-renewed/version/T6bH9nd3),
[source](https://github.com/Stalemated/ZenithRenewed/tree/1.20).
