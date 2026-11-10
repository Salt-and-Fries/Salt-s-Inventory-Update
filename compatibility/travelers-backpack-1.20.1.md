# Traveler's Backpack - Minecraft 1.20.1

The backpack uses the Sophisticated-style SIU composition: its native storage
grid inside a Salt window, original tool drawer on the left, and original
upgrade/settings tabs attached on the right. Player inventory stays in its own
ordinary SIU window. Original slot textures and restrictions are retained.

The native Sort, Stack, Move Out and Move In controls are omitted from both
composition and input. Edit Tools remains. Native equip/unequip, More Actions,
sleeping-bag and ability controls are separate features and remain available
when the original mod exposes them.

## Preserved functionality

- Native 27/45/63/81/99-slot tier layouts, tool counts, upgrade counts and
  storage scrolling. Hidden rows remain reachable with the original scrollbar.
- Crafting, tanks, furnace/blast furnace/smoker, pickup, magnet, feeding,
  refill, void, jukebox and lantern upgrade widgets, including native settings,
  filters, enable switches, removal actions and slot rules.
- Memory and unsortable settings, native selection overlays and return action.
- Handheld/inventory-slot, worn and placed backpack menus. Original owner,
  range, inventory validity and equipment rules remain authoritative.
- The native radial tool screen, hose, abilities, cosmetics and world gameplay
  stay with Traveler's Backpack. They are not converted into inventory windows.

The integration changes Compatibility only; Core and the public API are
unchanged. Original decoded packet actions run in the authenticated session.
Tank actions use the shared desktop cursor. A session presentation message
supplies native upgrade/settings metadata and the crafting result, whose
upstream result slot deliberately ignores ordinary `Slot.set` updates.

Optional mixins remember native screens, extend wrapper menu-update notices to
detached menus, and scope native widget packets. The placed settings return
clears the original settings-user flag before the replacement factory runs,
matching vanilla's close-before-create ordering. A live session restores its
native wrapper-user membership after a menu transition.

The physical source has a stable UUID and uses Core's existing source-lock
lease. Compatibility calls that existing lifecycle reflectively because it is
not exported by the public-API compile artifact; no API surface was added.
The native disabled carrier slot is a virtual, read-only alias. Its changing
storage NBT is not treated as a separate sortable ingredient or an unexpected
protected-slot mutation. Native storage, tool and upgrade cells remain real.

Registration requires the tested original packet/menu shape. An unsupported
native protocol keeps the ordinary fallback rather than receiving an adapter
with incompatible packet actions.

## Focused validation

The live subject is the retained, unmodified Traveler's Backpack Fabric 9.1.55
release for 1.20.1, with Cloth Config 11.1.136 and Cardinal Components 5.2.3.
The output manifest and focused-check records contain the exact inputs and
results. Checks cover storage/tool restrictions and source locking, all twelve
upgrade widgets, real crafting and smelting, bucket fill/drain, virtual filter
copying, filter modes, upgrade enable/install/remove, persistent memory and
unsortable selections, native equip/B-key opening, placed settings transitions,
SIU stack sorting and matching transfers, and compact-GUI scrolling.

Checks were focused and iterative. Earlier checks were not all rerun after
every refinement; changed behavior received additional checks. The B-key check
used actual Windows keyboard input, since the QA bridge's key-down flag does
not generate the mod's `consumeClick` event. No full gameplay suite ran.

Fabric remapping and Forge compilation/reobfuscation passed. Forge gameplay,
dedicated-server multiplayer, every passive upgrade effect, third-party
accessory integrations and recipe-browser transfers were not tested. The
delivered live evidence establishes Fabric 9.1.55 behavior, not universal
compatibility with all Traveler's Backpack releases/loaders.

See `output/travelers-backpack-implemented/` for builds, raw screenshots,
per-check evidence and the manifest, and
`output/pdf/siu-travelers-backpack-1.20.1.pdf` for the visual report.
