# Curios joined Character panel — focused validation

Executed 2026-10-22 on branch `Spatial-GUI-support-test`.

## Target and scope

- Minecraft 26.3, NeoForge 26.3.0.76.
- Core 0.1.5 and Compatibility 0.1.2 development runtime and production artifacts.
- Curios 17.0.0-beta.3+26.3, with a disposable slot-datapack world.
- 35 visible equipment slots across 11 native types, cosmetic counterparts and sample
  vanilla items; a temporary 100-ring allocation exercises overflow.
- Integrated singleplayer server. Computer Use operated the actual Minecraft client.

This was a focused integration check, not the complete functional/gameplay suite.

## Executed automated and build checks

| Check | Result |
| --- | --- |
| `:26.3:neoforge:assemble` + `:compatibility:26.3:neoforge:assemble` | Passed |
| `:compatibility:26.3:neoforge:testCuriosLayout` | Passed: 448 placements plus explicit overflow/full-grid cases |
| `python tools/verify-module-boundaries.py --sources` | Passed: 975 compatibility classes, 485 native patches, Core sources |
| Selected target's actual `compileJava.classpath` | Passed: 101 entries, no Core implementation available |
| `python functional-tests/curios-26.3-neoforge/verify-artifacts.py` | Passed for both collected production JARs |
| Combined latest ZIP | Exactly the two 26.3 NeoForge entries replaced; other 60 entries byte-identical |

Geometry cases cover four viewport sizes, seven slot counts (0 through 1,000), negative
and far-outside drag positions, equal widths, all four walls, tab clearance, direct attachment,
full-row fitting and scrollbar presence only when rows overflow.

The JAR check verifies IDs/versions, Minecraft target, exact required Core version, optional
Curios dependency, no copied native classes/artwork, no Core classes inside Compatibility,
no internal Core references in Compatibility bytecode, and no QA fixture classes/metadata.

## Executed gameplay checks

| Case | Observed result |
| --- | --- |
| Character Curios button | Opens the real accessory session above Character |
| Native Curios inventory binding (`G`) | Toggles the panel and can open the Character workspace |
| Normal 35-slot allocation | All slots visible together in four rows, same width as Character, no scrollbar |
| Drag Character upward/right | Panel and Character move together and stop at the combined screen bounds |
| Cosmetic tab | Projects above the panel, switches to native purple cosmetic slots and back |
| Slot tooltips | Native type labels and inactive-item label displayed |
| Equipment removal permission | Curse of Binding helmet could not be removed in Survival; cursor stayed empty |
| Visibility control | Native render flag and displayed toggle changed |
| Shared cursor across windows | Picked up a cosmetic sample item, placed it in another cosmetic slot, then moved it into ordinary inventory without losing it |
| Overflow (127 visible slots) | Character pushed to the bottom; top tab retained; scrollbar appeared |
| Scrollbar thumb and mouse wheel | Reached the final rows and scrolled back upward |
| Close Character | Attached panel closed and its server session was released |
| Live slot growth/shrink while open | 100 → 105 → 8 rings replaced sessions 1 → 2 → 3, kept one visible panel and removed the scrollbar after shrinking |
| Curios absent | Started with only Minecraft, NeoForge, Core and Compatibility; loaded a world and opened normal Character with no Curios button/panel |

The live topology check used the separate opt-in fixture in `curios-26.3-neoforge/src/main`.
It changes native Curios capacity on the server tick; it is excluded from production source
sets and artifacts. Start it only in a disposable world with ring slots:

```powershell
.\gradlew.bat -I functional-tests/curios-qa.init.gradle `
  :compatibility:26.3:neoforge:runClient `
  -PincludeCuriosRuntime=true -PsaltsSmokeRun=true `
  -PsaltsSmokeProfile=curios-character-focused -PsaltsDesktopRunDiagnostics=false
```

While the accessory panel is open, write `grow` or `shrink` to
`build/curios-implementation/capacity-command.txt`. The fixture consumes the command and
writes `capacity-result.txt`. `shrink` also removes the native `curios:command` capacity
modifier from the disposable ring fixture.

## Artifacts and visual evidence

- `dist/core/salts_inventory_update-26.3-NeoForge-0.1.5.jar`
  - SHA-256: `1142931f79b54ed97fcea1e1c2088f3a3238f398b9dcdfe86a397dc25439bfe1`
- `dist/compatibility/salts_inventory_compat-26.3-NeoForge-0.1.2.jar`
  - SHA-256: `46f9a2538aefae40819bc539d8b9b0410f9103cf56a9dd621c8c20096132dc74`

Real F2 screenshots and nearest-neighbor crops are in
`output/screenshots/curios-character-26.3-neoforge/`: `equipment.png`, `cosmetics.png`,
their `*-full.png` originals, and `curios-absent-full.png`.
These screenshots predate the small-gap follow-up below.

### Four-pixel gap follow-up

The adapter now requests a four-GUI-pixel gap using the existing public attachment API.
The joined-layout checks use this gap for all 448 placements, including combined screen
bounds and overflow. Only the 26.3 NeoForge Compatibility JAR requires rebuilding;
Core's existing attachment geometry already accounts for the gap. This small follow-up
passed compilation, all 448 layout placements and artifact checks; gameplay was not repeated.
The rebuilt Compatibility entry and its checksum were refreshed in `dist` and the combined
ZIP; the other 61 ZIP entries were unchanged. Build log: `build/curios-implementation/build-gap.log`.

Build, runtime and compiler-classpath logs are retained locally under
`build/curios-implementation/` (`build-release.log`, `runtime-r1.log`, `runtime-r2.log`,
`runtime-absent.log`, `compiler-check.log`, `compiler-classpath.txt`). Build output is ignored
by Git; the focused check sources and this record are repository files.

## Coverage limits and upstream observations

- No complete suite, other Minecraft/loader ports, dedicated server, separate remote client,
  Controlify or Spatial GUI interaction test was run for this change.
- Native slot mechanics were exercised with a slot datapack and vanilla sample items.
  Individual accessory content mods, their custom equip effects and specialized slot
  extension implementations remain unverified.
- Unsupported Curios versions intentionally retain native UI. That version-gate fallback
  and Core-disabled native UI fallback were inspected in code, not exercised in game.
- The Curios beta.3 `/curios set` command changes capacity but throws while formatting its
  success response. The live growth/shrink check used native inventory APIs instead.
- Windows performance-counter/OSHI diagnostics appeared during startup in both presence
  and absence runs. They did not prevent loading or gameplay; no adapter exception or
  slot-index exception was observed in the executed scenarios.

## Visual showcase follow-up

The screenshot session captured 17 fresh F2 images after the four-pixel gap change, in a
separate `curios-visual-showcase` disposable profile. It covered both views, a real
inventory-to-cosmetic transfer, native labels/tooltips, binding-curse removal restriction,
visibility toggle, recipe-guide coexistence, combined bounds, live capacity growth/shrink,
compact scrollbar endpoints and closing the owner. The QA fixture's `overflow` command
adds 92 native ring slots (100 rings total) without the upstream command-response error.

One follow-up was observed: `G` opened the workspace from gameplay, but did not reopen
the accessory panel while the desktop was already active. The Character button reopened it.
This limited the earlier shortcut result. The controls-fix follow-up below resolves it.
No production code or JAR was changed for the showcase capture request itself.

The labeled [visual showcase](../output/pdf/curios-26.3-neoforge-visual-showcase.pdf)
includes this observation and the coverage limits. Its original screenshots are under
`output/screenshots/curios-26.3-neoforge-showcase/`; a PDF-adjacent JSON manifest records
the native F2 filenames, hashes and exact crop rectangles.

## Shortcut and visibility-button fixes — 2026-10-23

Only the **26.3 NeoForge Compatibility 0.1.2** implementation changed. Core's existing
public workspace API supplies the Character handle; NeoForge's unconsumed screen-key
event handles the configured Curios binding inside an active desktop. Gameplay still uses
the normal key-mapping click queue. Text fields and modal handlers receive input first.
A physical-key latch prevents screen key repeat from retoggling, including the transition
from a held gameplay binding into the desktop, and clears on release or focus loss.

The panel viewport now includes the native toggle's two-pixel overhang beyond its slot
cell. Rendering, visibility-control hit testing and slot hit testing use the same width,
while retaining frame and scrollbar clearance. The grid dimensions and Character width
are unchanged.

Focused checks executed:

| Check | Result |
| --- | --- |
| Compatibility `assemble` | Passed, including the final gameplay-to-desktop key guard |
| `testCuriosLayout` | Passed: 448 joined placements plus 1,305 visibility layouts over widths 176–320, ordinary/scrolling heights and slot counts |
| Public API source-boundary audit | Passed: 975 Compatibility classes, 485 native patches and Core sources |
| Actual Computer Use: G from gameplay | Opened Character and Curios |
| Actual Computer Use: G in an open desktop | Closed and reopened Curios while Character/Inventory remained open; repeated on the final build |
| Actual Computer Use: focused accessory/cosmetic panel | G closed the panel correctly |
| Actual Computer Use: recipe search focus | Typed `g` into the native search field without opening Curios |
| Actual Computer Use: rightmost visibility control | Complete square displayed; its right edge showed the native tooltip and accepted visibility toggling |
| Actual Computer Use: cosmetic view | Rightmost native controls remained complete over cosmetic artwork |
| Actual Computer Use: 127-slot overflow | Full controls remained visible beside the scrollbar, after wheel scrolling and at the dragged bottom endpoint |
| Actual Computer Use: shrink to 35 visible slots | Complete ordinary grid returned without a scrollbar |
| Collected production JAR and bundle check | Passed: correct metadata/API boundary, no QA code; exactly one of 36 JARs changed |

The first UI pass used the opt-in capacity fixture and a copied `curios-controls-fix`
profile. A second launch without that fixture checked world/open-desktop toggling on the
final compiled implementation. Both clients were saved and closed after the checks.
The fixture did not enter the release artifact.

The updated artifact is
`dist/compatibility/salts_inventory_compat-26.3-NeoForge-0.1.2.jar`, SHA-256
`ac7b74d88ec9bb7b63393ed34d99f463121ac80c5ea08a95bac6e34139b01a70`.
The Compatibility checksum manifest and matching ZIP entry were refreshed. All twelve
Core JARs, all twelve Controlify JARs and the other eleven Compatibility JARs retain
their pre-change hashes. The other 60 entries in the combined ZIP are byte-identical.

Native F2 captures and their source/hash manifest are under
`output/screenshots/curios-controls-fix-26.3-neoforge/`. Build, source-boundary, runtime
and artifact-check records are under `build/curios-controls-fix/`.

Coverage is limited to these controls on 26.3 NeoForge with an integrated server and the
existing slot datapack. No complete suite, dedicated-server/client pairing, remapped or
mouse-button inventory binding, long physical key hold, other accessory content mods,
Controlify or Spatial GUI scenario was run for this patch. The held-key guard was
reviewed and compiled; it is not claimed as a physical-hold gameplay test. The existing
Windows OSHI startup diagnostics also appeared in this run without blocking gameplay.
