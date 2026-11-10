# Prominent Base Cinder Shop — Minecraft 1.20.1 Fabric

The shop follows the approved forge approach: a matching original stone strip
above the native UI, with SIU controls at the right. The original Cinders panel,
live trader portrait, Buy/Sell/Trade tabs, offer rows and scrolling remain in
their original layout. The native player inventory is omitted; SIU Inventory
opens separately. The resulting window is 242 × 159 GUI pixels.

The extraction includes the Trade tab's two-pixel overhang and the original
black closing row immediately above the former inventory. It uses the installed
`trader_screen.png` directly, with the same extended `primordial_forge.png`
header as the forge windows. The native offers remain inside their original
scrolling viewport. The header shows the native restock countdown when present,
and otherwise reads “Cinder Shop.”

## Native behavior

The original `CinderTraderScreenHandler` and `CinderTradeScreen` remain in use.
The original Owo trade record is decoded and checked for a valid offer index
before dispatching it in the authenticated owning menu's context. Original
checksums, advancement/quest gating, currency and item checks, special prices,
stock handling, payments and rewards remain authoritative.

Owo's original property format and property observers synchronize the native
offer list and portrait entity ID over an addressed SIU session payload. A
narrow injection suppresses Owo's unaddressed property packet only for shop
menus already owned by a live SIU session. Standalone Owo menus keep their
original transport. When native offers become empty, only the owning shop
window closes. Native removal still unregisters its trader handler.

The original Owo root is blanked during capture to keep the world visible.
On a live GUI-dimension change the adapter requests the screen's own rebuild
routine, which recomputes its absolutely positioned widgets. Native group
selection, offer updates and portrait rendering stay within the original UI.
The server's original entity-alive, world and eight-block validity checks stay
in force. These menus have no storage of their own; hidden player cells are
not classified as shop storage. Core and its public API are unchanged.

## Focused fixture and limits

The production-namespace Fabric fixture uses original embedded Base **4.1.5**,
Core **0.1.6** and Compatibility **0.1.3**, with the same excluded original
parent data resources used for the forge audit. The original parent treasurer
definition is restored byte-for-byte in a separate code-free QA resource jar,
with its original audited item-provider mods. No replacement offers, textures,
subject classes or crafting logic are installed in the fixture. The native
vanilla End advancement is granted through an operator command to make the
original Sell category available. Screens open through ordinary entity-ray
interaction with a real summoned Serkonid.

The excluded observer records native offer/property state, component bounds,
server inventory, Cinders and registered trader handlers. Test input uses
ordinary screen pointer, click, scroll and drag events. Operator commands
prepare disposable inventory and progression.

Focused validation covers all three categories, native payment refusal, one
successful original transaction per category, offer scrolling and synchronization,
live GUI scales 2/3, movement, separate Inventory, close/reopen and native reach.
Fabric remapping and Forge reobfuscation pass; Forge is a compile/isolation
check, not a runtime claim for this embedded Fabric release.

This is not a complete functional/gameplay suite. Dedicated-server multiplayer,
every merchant/offer, timed restocking, finite-stock exhaustion, special-price
trades, resource packs and controller input are outside these focused checks.

Artifacts, full-screen captures, focused results and original-data lineage are
saved in `output/prominent-shop-implemented`.
