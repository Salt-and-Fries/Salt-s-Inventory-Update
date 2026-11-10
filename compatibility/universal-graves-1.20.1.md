# Universal Graves — Minecraft 1.20.1

The approved grave-storage mockup is implemented as a compact Salt window:
four rows of nine loot cells, a thin divider, and the original nine-position
action footer without slot frames. Player inventory remains a separate SIU
window. The window is 190 × 138 GUI pixels; the extra four pixels over the
mockup leave room for the complete footer inside Salt's standard frame insets.

The grave list and the all-graves list remain native full-screen selection
menus. This is the **Universal Graves** entry called “Gravestone” in the
Project Everything backlog, not a blanket adapter for other grave mods.

## Native behavior

Universal Graves uses server-side SGUI menus whose loot and action icons share
a chest-shaped wire protocol. Compatibility supplies dedicated menu identities
for this family, preserving the original server handler and its callbacks.
Other SGUI menus and ordinary vanilla chests keep their existing behavior.

- Real loot cells preserve the mod's withdrawal-only restrictions. Empty
  virtual cells and footer icons are excluded from SIU storage operations.
- Opening data and live role masks keep real loot and pickup permissions in
  sync when paging or changing protection/access.
- Info/compass, unlock, protection removal, Quick Pickup, teleport, Fetch,
  previous/next page and Back use the original callbacks and native costs,
  permissions and confirmation rules. Availability is decided by the mod.
- Grave ticking, slot refresh, close/save and removed-grave transitions remain
  active while the menu is detached into SIU. Back restores the native list.
- The detached footer callback temporarily supplies SGUI's expected active
  menu context. Native menu replacements survive that callback. Core retains
  ownership of the shared cursor and its normal return-to-inventory behavior.

Only Compatibility was changed. Core and the public API were not modified.

## Focused validation

Live checks used the unmodified audited **Universal Graves Fabric
3.0.3+1.20.1** release, its bundled SGUI/Polymer dependencies, Fabric API,
production Core 0.1.6 and the built Compatibility 0.1.3 artifact. Graves were
created by actual deaths in a disposable world, rather than invented storage
menus. The evidence manifest records exact input/build hashes.

Focused checks covered direct and list-based opening, native list opt-out,
read-only views, withdrawal and insertion rejection, separate inventory
transfer, held-item conservation across Back, grave-compass conversion,
Quick Pickup and empty-grave cleanup, 41-stack paging and slot-role changes,
protection confirmation, recovery with occupied inventory space, Fetch,
Break confirmation, close/reopen, unlocked dragging, GUI scales 2/3, footer
icon protection and an unrelated vanilla chest.

The final live run contains no adapter eligibility/callback exceptions.
Earlier runtime findings were fixed before this run. Raw iterative check
records also retain three incorrect test assumptions: Back returns carried
items to inventory rather than retaining the cursor, locked windows cannot be
dragged, and a different protected grave was selected after an incidental
test-world fall. Corrected checks cover those behaviors; the fall was not an
adapter failure.

Fabric remapping and Forge compilation/reobfuscation passed. Universal Graves
was tested live on Fabric only. Dedicated-server multiplayer, other owners'
graves, payment/unlock and teleport configurations, resource-pack variants,
every alternate click gesture and other Universal Graves releases were not
tested. No complete gameplay or functional suite was run.

See `output/universal-graves-implemented/` for the built artifact, raw
screenshots, focused evidence and manifest.
