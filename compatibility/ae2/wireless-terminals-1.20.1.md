# AE2 Wireless Terminals — Minecraft 1.20.1

The Fabric and Forge compatibility extensions implement all five approved
inventory windows through the existing AE2 native desktop adapter. Core and
the public API are unchanged. No upstream mod JAR is redistributed or modified.

| Interface | Desktop behavior |
| --- | --- |
| Wireless Crafting Terminal | Native searchable catalog, 3×3 crafting grid, result and clear-grid actions. SIU Character and Inventory replace the embedded player preview and equipment. Native view-cell, singularity and upgrade panels remain attached. |
| Trash | All 27 native slots, with the purple return tab in the top-right corner and SIU controls to its left. |
| Magnet Filter | Both 27-slot ghost filters, both list-mode buttons, both copy buttons and swap. The native return tab stays top-right. |
| Wireless Pattern Encoding Terminal | Native catalog, blank/encoded pattern slots and crafting, processing, smithing and stonecutting modes. Native mode tabs and side panels remain intact. |
| Wireless Pattern Access Terminal | Native provider list, search, scrolling and pattern transfer actions. Its old player-inventory transition is replaced with a continuous native-colored footer. |

Native screen documents provide the artwork; independent desktop documents
remove only the embedded player inventory and arrange SIU's controls. Trash
and Magnet Filter copy their slot wells from the installed mod's own atlas,
including resource-pack replacements. No generic incompatible-container bars
are used.

AE2WTLib 15.2.1 Fabric keeps its conditional Magnet Card mode and filter
shortcuts in the original left toolbar, followed by Trash. Its default desktop
catalog has enough rows to keep that toolbar in one column. AE2WTLib 15.3.3
Forge keeps Wireless Terminal Settings, Magnet Filter and Trash below crafting,
in that original order and with the native horizontal spacing. The compact
footer retains padding below the buttons. Native code continues to control
visibility, tooltips and actions; SIU does not move buttons between the toolbar
and the crafting body. The native settings page and return navigation remain
available.

Pattern Access's client-side provider slots are dispatched through AE2's
original inventory-action packets, rather than ordinary container-slot clicks.
This preserves the native provider ID and pattern index.

## Verification

Production clients used unmodified AE2 15.4.11 with AE2WTLib 15.2.1 on Fabric
and AE2WTLib 15.3.3 on Forge. The observer and fixture helpers are excluded
from shipping artifacts. The two loader builds and focused in-game checks
cover the affected interfaces; this is not a complete gameplay suite.

The latest button-placement checks and artifact hashes are recorded in
[placement verification](../../output/ae2-wireless-button-placement/verification.json).
These focused checks cover native placement/order, conditional visibility,
mode cycling, Filter/Trash navigation and Forge settings return. The earlier
[implementation verification](../../output/ae2-wireless-implemented/verification.json)
records the other feature checks and historical screenshots.

| Window | Fabric screenshot | Forge screenshot |
| --- | --- | --- |
| Crafting (current button placement) | [Open](../../output/ae2-wireless-button-placement/fabric-native-button-placement.png) | [Open](../../output/ae2-wireless-button-placement/forge-native-button-placement.png) |
| Trash | [Open](../../output/ae2-wireless-implemented/fabric-trash.png) | [Open](../../output/ae2-wireless-implemented/forge-trash.png) |
| Magnet Filter | [Open](../../output/ae2-wireless-implemented/fabric-magnet-filter.png) | [Open](../../output/ae2-wireless-implemented/forge-magnet-filter.png) |
| Pattern Encoding | [Open](../../output/ae2-wireless-implemented/fabric-pattern-encoding.png) | [Open](../../output/ae2-wireless-implemented/forge-pattern-encoding.png) |
| Pattern Access | [Open](../../output/ae2-wireless-implemented/fabric-pattern-access.png) | [Open](../../output/ae2-wireless-implemented/forge-pattern-access.png) |

Coverage limits: optional Curios/Trinkets-hosted terminal locators, universal
terminal radial selection, long-distance Quantum Bridge operation and a full
autocrafting CPU job, and interactive resizing are outside this focused pass. Existing native behavior
and adapters continue to handle those paths; they are not claimed as tested.
