# Trading Post — Minecraft 1.20.1

The compact Trading Post window follows the approved vanilla merchant layout:
254 GUI pixels wide, four original 88×20 offer rows, the larger native result
well, merchant name/level and native XP bar. Its height is 136 GUI pixels, 24
more than the refined vanilla window, to put the search field below
the **Offers** caption. The separate SIU Inventory remains available.

This is implemented in Compatibility. Core and its public API are unchanged.
The original Trading Post menu, offer collection, search tree, filtered-index
mapping and native trade/payment methods remain authoritative. No new trading
system or rewritten search is introduced.

The adapter routes the original decoded merchant-data/build/removal packets to
the owning detached native screen, including updates arriving before the
window opens. It sends trade selections through SIU's authenticated menu-button
transport using the original unfiltered offer index. Search resets accept only
the installed upstream clear-payment message's exact encoded bytes in its own
session. Offer-use/price refreshes preserve the native offer identities and
their merchant/search maps. Ordinary native screens retain seven rows.

Validation: focused live Fabric checks cover geometry, aggregation from two
villagers, enchantment search, correct filtered selection, both payment slots,
single/two-input purchases and costs, output padding, XP and uses, empty search
and payment returns, sold-out offers, four-row wheel scrolling, scrollbar track
and thumb, and reopening. The actual checklist and unchanged full in-game
captures are saved in `output/trading-post-implemented/`. This is not the full
functional suite. Forge compilation/reobfuscation passed; Forge gameplay,
dedicated-server multiplayer and other Minecraft versions were not run.

Pinned live subject: Trading Post 8.0.2 / Fabric / Minecraft 1.20.1, with the
audited Puzzles Lib 8.1.33 and Forge Config API Port 8.0.3. See the saved runtime
manifest for exact inputs and hashes.

## Search-bar cleanup

The search field now draws its own complete inset border and the original
transparent magnifying-glass sprite. The old sidebar/window artwork is no
longer part of the displayed search bar. The original EditBox renders directly
inside the clean field, retaining native text, caret, selection, editing and
search behavior. Its complete border rectangle accepts clicks.

Eleven focused live Fabric checks passed for this refinement: existing size and
offer loading, both field edges, queries across villagers, caret edits, long
queries, losing keyboard focus, GUI scale 3 and correct offer selection after
clearing. Empty, filled, long-query and scaled screenshots were visually
inspected. Fabric remapping and Forge compilation/reobfuscation passed. The
earlier 21-check trading run and the full functional suite were not repeated.
Updated evidence/builds: `output/trading-post-search-fix/`.
