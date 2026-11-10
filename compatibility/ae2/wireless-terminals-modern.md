# AE2 Wireless Terminals - newer targets

The optional Compatibility extension adds the approved wireless terminal
windows to the newer NeoForge targets below. Core and the public API are
unchanged. The existing 1.20.1 Fabric/Forge implementation remains intact.

| Minecraft | Loader | AE2WTLib release | AE2 baseline |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge | 19.5.1 | 19.2.18 |
| 26.1.2 | NeoForge | 26.1.2-beta | 26.1.13-beta |
| 26.2 | NeoForge | 26.2.0-beta | 26.2.1-alpha |
| 26.3 | NeoForge | 26.3.1-beta | 26.3.1-alpha |

Official availability was checked against the publisher's
[Modrinth releases](https://modrinth.com/mod/applied-energistics-2-wireless-terminals/versions)
on November 1, 2026. There is no matching official release for 1.21.1 Fabric,
1.21.11 Fabric/NeoForge, or 26.1.2/26.2/26.3 Fabric. Those targets are not
advertised as supporting this add-on.

## Native artwork and controls

Six desktop documents include the installed release's own screen documents:
Crafting, Trash, Magnet Filter, Pattern Encoding, Pattern Access and the
Wireless Terminal Settings subview. No publisher textures, ordinary screen
documents or native implementation classes are bundled in Compatibility.

The modern crafting footer uses the installed modern AE2 crafting atlas, not
the vintage 1.20.1 artwork. The native action buttons retain their original
instances, order, conditional visibility, tooltips and callbacks in the compact
crafting area. The embedded equipment slots and player preview are replaced
by SIU's Character/Inventory windows. Native toolbars, scrolling upgrades,
universal-terminal selector and conditional attachments remain upstream-owned.

Trash and Magnet Filter retain all 27 and 54 slots respectively, sampling
their wells from the installed add-on's atlas. Their return tab stays in the
top-right, with SIU controls to its left. Modern AE2 itself uses a back-arrow
glyph there instead of the older item icon; that native change is retained.

Pattern Encoding keeps all four native mode panels. Pattern Access forwards
client-side provider-slot clicks to AE2's provider-ID action handler; they are
not treated as ordinary numbered container slots. Its blank inventory join
is replaced with a continuous native-colored footer below the final list row.

## Focused verification

The disposable clients use unmodified publisher JARs and the production Core
and Compatibility JARs. Excluded observers record real menu state and real
mouse input. The focused checks cover native window selection, geometry,
crafting output/transfer, conditional card controls, settings/filter/Trash
navigation, top-right return tabs, four encoding modes, pattern encoding and
provider withdrawal/reinsertion. These are not a full gameplay suite.

The current report and screenshots are in
`output/ae2-wireless-modern/verification.json` and the adjacent target folders.

Coverage limits: optional Curios terminal locations, universal-terminal
selection with multiple installed terminal types, long-distance Quantum Bridge
operation, a complete autocrafting CPU job and interactive resizing were not
qualified in this focused pass. Their native controls and adapters are retained.

The subsequent shared tab fix uses AE2's framed background for modern white
return arrows, avoiding the broad white highlight on the corner background.
It also draws generic joining strips before icons, using the actual tab size
and the owning background color. Current tab-specific screenshots and release
hashes are in `output/ae2-tab-join-fix/verification.json`; they supersede the
return-tab appearance in the earlier rollout pictures.
