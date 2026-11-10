# Applied Botanics — Minecraft 1.20.1

The portable mana cell uses the existing SIU AE2 window implementation. Its
native menu is `ae2:portable_mana_cell` (`MEStorageMenu`), registered through
AE2's normal menu builder by Applied Botanics. The existing builder catalog,
server session, content renderer, packet transport and slot behavior are reused.
No separate mana-cell renderer or Core/API change was added.

The new desktop profile includes Applied Botanics' original
`terminals/portable_mana_cell.json`. It uses the same desktop geometry as AE2's
portable fluid cell, preserving the native mana renderer, nine-column catalog,
search, scrollbar, settings rail, upgrade sockets and settings navigation. Player
slots appear in the separate SIU Inventory window.

The AE2 compatibility callback now leaves its committed carried stack on the
native menu when it returns. Clearing that stack after committing it discarded
the shared cursor, including a mana tablet modified in place by Applied Botanics.
This correction uses the existing public cursor API.

## Focused verification

Real Minecraft 1.20.1 Fabric runtime: Applied Botanics 1.5.2, AE2 15.4.11,
Botania 456, SIU Core 0.1.6 and Compatibility 0.1.3.

All **36 focused live assertions passed** in the final acceptance run:

- All five cell tiers (1k, 4k, 16k, 64k and 256k) open the existing `AEWindow`
  and receive their actual stored mana through the server-backed repository.
- A tablet starting with 100,000 mana receives the cell's 250,000 mana, remains
  on the cursor, and returns to SIU Inventory with 350,000 mana. Depositing it
  back conserves all 350,000 mana and retains the empty tablet.
- Native search filters and finds mana. Both sort controls operate. An energy
  card can be inserted into and removed from the native upgrade socket.
- Settings open in the same window and their return tab restores the storage
  view. Movement and vertical row resizing work. Closing and reopening preserve
  stored mana and native sort settings.
- A focused regression check of an ordinary AE2 portable fluid cell deposits
  and withdraws a water bucket without losing the carried container.

Resizing was explicitly enabled in the disposable acceptance fixture; SIU's
ordinary default remains unchanged. The native terminal retains nine columns
when resized vertically.

Fabric remapping and Forge compilation/reobfuscation passed. The built JAR
contains the new profile and no upstream `appeng`/`appbot` implementation classes.
Forge gameplay, dedicated-server multiplayer, other Minecraft versions, every
AE2 menu and the complete gameplay suite were not tested for this change.

Acceptance evidence is in `output/applied-botanics-implemented/`. The clean
screenshots use the same Compatibility/Core hashes as the acceptance runtime
with the debug overlay disabled. Screenshots are unmodified game captures.

Reproduce the focused checks with
`functional-tests/applied-botanics/focused.py`; the scene capture is
`functional-tests/applied-botanics/capture.py`. QA observers remain excluded from
the released Compatibility JAR.
