# Approved compact layouts: focused validation

Minecraft **1.20.1 Fabric and Forge**, Core **0.1.5**, Compatibility **0.1.3**. This pass implements the approved UI correction review. No version numbers were increased, and other Minecraft targets and Controlify were not changed.

## Executed checks

- Built the two affected Core and two affected Compatibility production JARs, including Fabric remapping and Forge reobfuscation. Compiled the public-API-only native-template example for both loaders.
- Passed **23 focused unit tests**: native capabilities (3), native layout/profile/exterior mapping (10), viewport/scrollbar geometry (6), and Compatibility crops/all backpack shapes (4). No failures, errors or skips.
- Passed **107 recorded focused gameplay assertions** in the final Fabric/Forge test and clean-photo runs. These are individual assertions from a command-driven fixture, not 107 independent gameplay suites.
- Fabric: Copper Kiln processing/output pickup, native recipe-book opening/closing and movement; Juicer processing/output transfer; 512-item Nether Chest relocation; exterior kiln opening with a stable body; all three native hatch state changes after movement; all four window boundaries.
- Fabric: every default Inmis tier matched its approved dimensions, supported last-slot pickup/placement, and preserved contents after reopening the same backpack. Endless also fit at GUI scale 3; Ender Pouch retained its ordinary compact route.
- Forge: 512-item transfers between two Nether Chest windows; Crab Trap bait rejection and catch sorting without moving valid bait; Cabinet initial sizing after replacing a different block/menu, resize snapping and reopen; both exterior kiln states and all three hatch changes; all four boundaries.
- Computer Use: clicked the exterior Forge preview with real mouse input and observed it collapse without moving/resizing the body.
- Source and production-JAR boundary checks passed. Compatibility continues to consume only Core's public API; no QA/example classes or duplicate Core are packaged in the extension. All previous public API members/constructors were retained.
- Compared production hashes with the staged JAR hashes in **all four final gameplay/photo runs**. The distribution files exactly match those tested artifacts. Source hashes, capture IDs and crop rectangles are recorded alongside the PDF.

The reused Gradle launch task is named `runFullGameplayQa`, but these runs use a focused observational command driver. A complete functional/gameplay suite was **not** executed.

## Issues discovered and resolved during validation

- Generic source-only saved state let a newly substituted Cabinet inherit the preceding Crab Trap's dimensions, causing a narrower grid and an unnecessary scrollbar. Ordinary container state is now menu-specific; legacy placement/pin data migrates without unrelated dimensions. The final Forge run passed first-open, resize and reopen checks.
- Initial kiln layout now uses the known closed tab extent before the first native render observation, avoiding a transient fallback body size.
- An initial Juicer test attempted output pickup before its processing cycle completed. The fixture now waits for a full native cycle; the final tests passed. No gameplay code was changed to shorten processing.
- The tutorial window remained behind two early context captures in the disposable test world. It was closed through its UI, and those images were retaken. The final report contains the clean context captures.

## Evidence

Final runs:

- `build/native-ui-approved/gameplay/1.20.1-fabric-approved-fabric-02`
- `build/native-ui-approved/gameplay/1.20.1-forge-approved-forge-02`
- `build/native-ui-approved/gameplay/1.20.1-fabric-approved-fabric-03` (clean recipe-book context)
- `build/native-ui-approved/gameplay/1.20.1-forge-approved-forge-03` (clean two-window context)

`build/native-ui-approved/final-validation.json` records the test summaries and artifact identities. Build logs, boundary-check results, unit-test XML, authoritative client/server observations and source screenshots remain available locally.

The [final UI PDF](../../output/pdf/siu-six-mods-approved-ui-1.20.1.pdf) contains **12 pages and 31 actual in-game figures**. All pages were rendered and visually checked; screenshots are cropped/enlarged without repainting the game UI. Proposed mockups from the review are not used as implementation evidence.

## Coverage limits

Dedicated remote multiplayer, exhaustive resource packs/configurations, controller/Spatial GUI combinations, arbitrary oversized custom backpack configurations and future upstream mod releases were not exhaustively validated. The focused tests do not establish that the entire mod is bug-free.

Use the matching rebuilt 1.20.1 Core and Compatibility JARs. Older published artifacts with identical version numbers may predate these experimental API/layout additions.
