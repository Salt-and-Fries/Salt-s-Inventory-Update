# Bountiful — Minecraft 1.20.1

The Bounty Board uses one 348×120 SIU window inspired by the compact merchant
and Trading Post composition. Four original 160×20 bounty previews remain
visible at a time. The original list filtering, objective/reward icons and
quantities, 21 board cells, three decree cells, selection highlight and
reputation bar remain native. Player inventory opens separately.

The SIU frame uses the original board's brown face `#563426`, highlight
`#794A36`, shadow `#21140F`, and original title color `#EADAB5`. Native slot and
preview artwork keeps its own palette. No standard gray backgrounds cover it.

Implementation lives entirely in Compatibility. Core and its public API are
unchanged. The adapter uses the original menu, board inventory, per-player
bounty masking, decree restrictions and server slot methods. Bounties and the
hidden selected-offer alias are virtual offers rather than sortable storage.

Preview selections use SIU's authenticated menu-session payload and the native
inventory's `select(index)`. They do not supply a player UUID or depend on the
active vanilla menu. Only the selected-offer alias's off-screen coordinate is
normalized; its original slot type, container and index are preserved.

Two optional client mixins remember native board instances and preserve the
native quantity renderer's GUI pose when captured. Only hosted boards use the
four-row viewport. Native-only screens retain seven rows. A local pointer
capture updates the native scrollbar position because the upstream screen
discards its reactor's consumed-click result.

## Validation

Nineteen focused live checks passed on Fabric with the retained Bountiful
6.0.4+1.20.1 release: window geometry, four rows, all native board/decree slots,
offer loading, first/fourth/last preview selection, exact selected bounty data,
selection without taking offers, wheel/scrollbar navigation, native bounty
pickup into the separate inventory, taken-offer masking after reopening,
decree withdrawal/reinsertion, non-decree rejection, reputation synchronization
and GUI scale 3. Real mouse input also confirmed native bounty and reputation
tooltips. Quantity rendering and complete native borders were visually checked
against the original board. The final text-color-only adjustment was rebuilt
and visually checked in a fresh client.

Fabric remapping and Forge compilation/reobfuscation passed. Forge gameplay,
dedicated-server multiplayer, bounty completion/reward redemption, other
Minecraft versions and the complete functional/gameplay suite were not run.
An earlier observation client crashed in native `glfw.dll` after completing its
checks; the evidence records that crash and the subsequent fresh-client check.

Screenshots, pinned runtime inputs, focused results and both Compatibility
builds are saved in `output/bountiful-implemented/`.
