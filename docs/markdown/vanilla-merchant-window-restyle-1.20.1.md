# Compact vanilla merchant window - 1.20.1

Implements the independent villager trading review in Core, with the user's
explicit exception: **four visible offers are retained**. The initial restyle
kept the 108-pixel height; the subsequent spacing refinement increases it to
112 GUI pixels to clear the bottom offer. The window grows six pixels wider to accommodate the
native transaction proportions and full output frame. SIU management controls
and the separate player Inventory remain available. The public API is unchanged.

## Review changes

- **F1:** native Minecraft nine-sliced, textured 88x20 offer buttons replace flat
  tinted rectangles. Native item, arrow and count positions are retained.
- **F2:** the native 88-pixel offer width is adopted; the seven-row proposal is
  deliberately excluded. The scrollbar remains aligned with four complete rows.
- **F3:** the result receives a 26x26 well with its original-size item centered
  inside. Its padding is clickable as part of the result slot. Input wells and
  the transaction arrow share its centerline.
- **F4:** native 102x5 XP artwork, shaded fill and pale prospective-XP segment.
  Preview uses the menu's actual prospective XP delta when payment is valid;
  selecting an offer alone no longer invents a preview.
- **F5:** selected available and exhausted offers use the same light frame.
  Exhausted rows retain vivid native red X sprites without a dark overlay.
- **F6:** Trades and merchant name/level align on one content heading line.
  Existing vertical margins accommodate that line without increasing height.
  The window's title identifies the merchant.

The in-game checks also found stale XP and offer-use counts after purchases.
Core now sends its existing merchant payload when authoritative offers, XP,
level or progress/restock flags change. Full snapshots still send initial
merchant state. Unchanged state does not generate a merchant payload every tick.
No new protocol or API fields were introduced.

## Initial restyle validation

Minecraft 1.20.1 Fabric, Core 0.1.6, final staged build:

- All **21 focused live assertions passed**, covering unchanged height, four-row
  scrolling/clamping, one- and two-input purchases with exact costs, clicking
  output padding, no extraction from sold-out offers, prospective XP appearing
  and disappearing with payment, XP increasing after purchases, live exhaustion
  on the final permitted purchase, moving/unlocking, GUI scale 3, reopening,
  persistence of uses/XP and Master-level presentation.
- Three complete XP-bar comparisons (empty, loaded two-input trade, sold out)
  match the vanilla captures pixel-for-pixel.
- A complete unselected bookshelf offer row and a selected exhausted ink row
  also match vanilla pixel-for-pixel, including the button, icons, counts,
  arrows, light selection frame and red X. Total: 5,050 GUI pixels compared,
  zero mismatches.
- Fabric remapping and Forge compilation/reobfuscation passed.

This was not the complete gameplay suite. Forge gameplay, dedicated-server
multiplayer, scheduled restocking, custom resource packs, other Minecraft
versions and unrelated menus were not exercised for this change.

Evidence and builds: `output/vanilla-merchant-restyle/`.
Runtime: `build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-villager-fix04/`.
Focused driver: `functional-tests/villager-window-implementation/focused.py`.
Screenshots are unchanged real game captures; fixture trades and payment stacks
match the preceding vanilla/SIU review.

## Spacing refinement

At the user's request, the window gains four GUI pixels of bottom clearance.
The right-hand heading, XP bar, payment slots, arrow and result well move
**8 pixels left and 16 pixels down** as one group. Drawing and slot hit testing
use the same offsets. Trade rows keep their original dimensions and four-row
capacity.

Eight focused assertions passed on the refined Fabric Core-only build: height,
four-row page retention, valid two-input result, output-padding click, exact
payment costs, both shifted input hitboxes and last-page selection. The refined
screenshots were visually inspected for footer clearance and group placement.
Fabric remapping and Forge compilation/reobfuscation passed. Other controls,
Forge gameplay and the complete functional suite were not rerun for this small
layout adjustment. Refined evidence: `output/vanilla-merchant-spacing/`.
