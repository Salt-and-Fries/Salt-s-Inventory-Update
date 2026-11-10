# Bumblezone windows — Minecraft 1.20.1

Implements the three designs from the small-interface mockup review in the
Compatibility add-on. Core and the public SIU API are unchanged.

## Presentation

- **Crystalline Flower:** honey-colored SIU frame; native XP tower, feeding
  controls, item cells, enchantment selection, sort control and scrollbar.
- **Honey Cocoon:** compact honey-colored native storage cells. Player inventory
  remains an independent SIU window. All six strict chest row counts are matched.
- **Buzzing Briefcase:** complete native wooden case silhouette, honey latches,
  handle, fourteen bee panels and native bee actions. A wooden SIU title strip
  replaces the original title; the transparent case exterior remains transparent.

The native renderers and menu operations remain responsible for the contents.
Compatibility adapts the briefcase's hidden synchronization cell to avoid
integer overflow in snapshot geometry. A flower-specific packet bridge delivers
the original server enchantment list to the hosted menu and routes the original
selection payload through SIU's authenticated session.

## Focused verification

Tested in a real Minecraft 1.20.1 **Fabric** client with Bumblezone **7.9.8**,
SIU Core **0.1.6** and Compatibility **0.1.3**.

Twenty focused live assertions passed on the final build:

- Flower: three native item cells, book insertion, XP tier growth, server-backed
  enchantment selection, native sort click, window movement and enchanted-book
  retrieval into the separate player inventory.
- Cocoon: eighteen owned cells, item insertion/withdrawal, window movement,
  closing and reopening.
- Briefcase: native template/full width, healing, replacing a stinger, growing a
  bee, pollen action, releasing the last bee in the second row, window movement,
  closing and reopening.

Three geometry tests passed, including all strict chest sizes, flower control
coverage and the complete briefcase silhouette/input mapping. Fabric remap and
Forge compilation/reobfuscation succeeded. Forge gameplay, dedicated-server
multiplayer, every GUI scale, food-based flower feeding and every flower list
scroll state were not tested. This was not the complete functional suite.

Real screenshots and the final focused test journal are saved in
`output/bumblezone-implemented/`. The full-screen workspace screenshot contains
all three live mod windows and the separate SIU Inventory window. Screenshots
are unchanged game captures; the fixtures use real upstream blocks and items.

Reproduction scripts:

- `functional-tests/bumblezone-windows/focused.py`
- `functional-tests/bumblezone-windows/capture.py`

The Crystalline Flower test fixture uses amethyst support, as required by
Bumblezone itself. The disposable observation helper is not in the shipped mod.

## XP bar border correction

The flower's consumption-region crop now includes native row 103, restoring
the bottom row of its five-pixel XP bar. The corrected build and full-screen
capture are in `output/bumblezone-xp-bar-fix/`. The flower geometry test and seven
flower interaction checks passed again. A live pixel comparison additionally
matched all 261 opaque pixels across all five rows to the upstream empty-bar
sprite. Other menus and the full gameplay suite were not rerun for this fix.
