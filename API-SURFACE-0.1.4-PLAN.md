# 0.1.4 surface API implementation and acceptance

This record audits the nine-part overhaul objective and the report's R1–R12 requirements. Earlier checkpoint logs and screenshots describe earlier artifacts; they are not the final acceptance result.

## Final design

Public integration API **4** separates desktop management from presentation. The wire protocol remains **3**. Salt supports its own decoration, provider-styled Salt decoration and completely mod-owned surfaces. Mod-owned presentation removes Salt's frame, backing fill, title, controls, resize decoration, padding and implicit title/grip hit targets together. Metadata titles and accessibility remain available.

Immutable surface, region, popup and decoration snapshots drive paint and hit testing. Explicit roles prioritize popup/control/slot interaction over movement and resizing; visual shape, hit shape and placement participation remain independent. Shared contracts define GUI pixels, insets, conversions, transforms, anchored edge resizing and viewport policies. Original artwork can retain its dimensions while its integration chooses a compact layout.

Managers validate actions against options, modality and current lifetimes. Computed read-only inventory gesture snapshots and terminal events let integrations paint native slots without modifying logical topology, shared cursor accounting or server privileges. Native widgets, popups, keyboard traversal/narration, resource/viewport notifications, preference revisions and lifetime-owned asynchronous work have focused contracts and examples.

Salt and AE2 remain separate sibling repositories. The existing work in both checkouts is preserved. No obsolete public API aliases or compatibility adapters were retained.

## Required delivery audit

| Goal requirement | Implementation and evidence | Status |
| --- | --- | --- |
| 1. Real consumer and report | Reviewed the API, guides, examples, R1–R12 report and separate AE fork; migrated all 40 registered AE containers | Verified: separate repository, exact-package native matrix |
| 2. Presentation ownership | Native/styled/mod-owned modes; complete suppression; independent metadata; typed paint states and metrics | Verified: engine contracts and native presentation captures |
| 3. Geometry/rendering | Coherent prepared geometry, independent shapes/placement, grouped rendering, clips/transforms, pixel preservation and author fallback | Verified: geometry tests and 270 reviewed captures |
| 4. Interaction/actions | Role priorities, eight-edge resizing, validated management/state, capture/cancellation, focus/modality and stale lifetimes | Verified: engine tests and native routing/actions |
| 5. Inventory correctness | Presentation-only inventory removal, stable logical slots/source ownership, copied computed previews and one-time terminal notifications | Verified: inventory regressions and native cursor/crafting checks |
| 6. Complex UI lifecycle | Per-instance native widget host, tabs/attachments/popups, dismissal, accessibility, revisions, reload/viewport and async lifetime cleanup | Verified: examples, lifecycle tests and native reload/modal checks |
| 7. Optional integration | Compile-only lazy providers, common/client isolation, rollback/diagnostics, scoped native networking | Verified: same packaged consumer absent/present, clients/servers |
| 8. Documentation | Markdown/HTML guide, public signatures/defaults/errors/limits/version availability, transfer reference and executable examples, READMEs and changelog | Verified: synchronized guide/reference and compiled examples |
| 9. Complete verification | All 12 targets and examples; release audit; 40 base AE containers, representative modes/forms/errors/populated views at scales 2/3/4 | Verified: all builds, package audits and named native/visual gates |

## Report coverage

| Report request | Delivered contract |
| --- | --- |
| R1 decoration ownership | `DesktopWindowSurface`, `DesktopSurfaceMode`; complete mod-owned suppression |
| R2 surface metrics | `DesktopInsets`, size/conversion/transform helpers; separate metadata title |
| R3 transparent/shaped surfaces | Explicit visual/hit/placement regions; no mandatory opaque backing |
| R4 interaction roles | `DesktopHitRole`, committed role priorities and slot interception protection |
| R5 management actions | `DesktopWindowHandle`, `DesktopWindowAction`, `DesktopWindowState`; native management menu without an ellipsis dependency |
| R6 native skinning | `DesktopWindowDecoration`, `DesktopChromeMetrics`, `DesktopControlState` |
| R7 inventory extraction | `DesktopMenuBindings`, logical slot/source contracts and presentation-only visibility |
| R8 coherent layout | Candidate surface → `prepareLayout` → atomic surface/region/popup/metrics commitment |
| R9 pixels/transforms | GUI-coordinate rules, half-open regions, rounding/clips, `PRESERVE`/`ADAPT` fallback |
| R10 gesture appearance | `DesktopGestureSnapshot`, `DesktopSlotPreview`, `DesktopGestureEvent`; no mouse polling required |
| R11 native hosting | `DesktopWidgetHost`, native input/rendering, focus/cancel/narration hooks and AE adaptation |
| R12 consumer contracts | [Guide](docs/markdown/desktop_window_api.md), [signatures](docs/markdown/api_reference.md), [transfer reference](docs/markdown/recipe_transfer_api.md), [compiled examples](api-examples/README.md) |

## Current build evidence

- `build/surface-api4-final2-complete-matrix.log`: successful all-target/example/release build and 500 tests, no failures/errors/skips. Shared suite 60; Minecraft suites 73/74/73/73/73/74.
- `build/surface-api4-accepted-functional-compile.log`: successful functional harness compilation for all twelve targets.
- `build/surface-api4-accepted-source-parity.log`: source feature parity checks.
- `build/surface-api4-jar-audit.json`: all twelve release jars contain their intended API, API-version constant 4, `ADAPT`; removed public classes, examples and functional harnesses are absent.
- `tools/generate-api-reference.py --check`: current 60-type public signature inventory. Record constructors have explicit argument lists.
- `tools/sync-api-guide.py`: 16 synchronized chapters, four tables, eight preserved code samples and validated local links/navigation.

## Frozen native acceptance artifacts

AE2: sibling `build/libs/appliedenergistics2-0.0.0-surface-api4-verified.jar`.

SHA-256: `5937AB3F018A6584FF54DA4B04B9595513B82C336CFC7CC3AC1E91DD7CE3CF26`.

Salt: `build/release/0.1.4/salts_inventory_update-26.3-NeoForge-0.1.4.jar`.

SHA-256: `7DEFE61443D4D8185CC060400B50C41D52252322557DD6B76E9EDED91E8AF5FA`.

Final logs use the `surface-api4-verified` prefix in the sibling AE `build` directory. Assertion completion, process exit and visual inspection are separate gates. All five client runs completed their required assertions and exited 0; both dedicated-server configurations passed all 77 required tests and exited 0. All three atlases contain 40 base containers and 90 views. The frozen-artifact audit verifies hashes, completion/failure markers, manifests, geometry sidecars, PNG validity and actual framebuffer sizes.

## Resolved findings during verification

- The management popup depended on a native ellipsis anchor that disappears in mod-owned presentation. It now anchors to the surface when needed; Escape dismisses it before owner input/closure. Regression checks verify actual clickable popup bounds and retained owner lifetime.
- The legacy 1.20.1/1.21.1 public key entry points omitted API keyboard dispatch. They now use the same focus/modal routing as newer targets, with actual public-entry regression coverage.
- Hidden retained popups could still consume outside input. Hidden owners now participate in neither outside-popup routing nor narration.
- Hidden/disabled native widgets could retain focus/capture; reused widget instances could alias ownership. The host now clears unavailable ownership and rejects duplicate instances.
- A resize probe was initially configured with resizing disabled. Enabling it in the disposable QA configuration established native resizing; this was an acceptance setup issue.
- A small-window probe initially caused Minecraft to lower its effective GUI scale. The corrected probe uses a real 320×240 GUI viewport and verifies notifications, unscaled artwork, typing and retained topology.
- AE's Formation Plane left a synchronized enum null until its first tick. Its constructor now initializes the host's actual placement setting before any initial full synchronization. The guide documents construction/`createState` → synchronizer installation/full write → `opened` ordering. Current opening verification and both server configurations pass.
- A capture after resource reload included Minecraft's fading splash. Final lifecycle captures wait for the actual native overlay to clear. The earlier obscured capture is not visual proof.

## Integration responsibilities and limits

Integrations own their native model, artwork, layout, popup painting, domain permissions and cached asset reconstruction. They must adapt code that assumes a single global screen/menu and cancel old subview work. The API supplies hooks and helpers rather than an automatic adapter for every arbitrary `Screen`. Variable-height catalogs and IME composition are not additional guarantees of this release; existing native controls/custom frameworks remain usable through the documented hooks.

No publishing, committing or pushing has occurred. The nine goal requirements and R1–R12 have been audited against the current implementation, documentation and final evidence below; no required acceptance gate remains open.

## Final native and visual evidence — 10 October 2026 UTC

| Gate | Final evidence | Result |
| --- | --- | --- |
| Salt-present client | AE `build/surface-api4-verified-lifecycle-packaged-present-client.log` and its `-exit.json` | Terminal and lifecycle COMPLETE; assertions true; exit 0 |
| Salt-absent client | AE `build/surface-api4-verified-optional-packaged-absent-client.log` and its `-exit.json` | Terminal COMPLETE; assertions true; exit 0 |
| Present dedicated server | AE `build/surface-api4-verified-packaged-present-server.log` | All 77 required tests pass; terminal exit 0 |
| Absent dedicated server | AE `build/surface-api4-verified-packaged-absent-server.log` | All 77 required tests pass; terminal exit 0 |
| GUI scale 2 | AE `build/surface-api4-verified-scale2` and corresponding client log/exit JSON | 40 containers, 90 views; GUI 640×480, PNG 1280×960; assertions true; exit 0 |
| GUI scale 3 | AE `build/surface-api4-verified-scale3` and corresponding client log/exit JSON | 40 containers, 90 views; GUI 640×480, PNG 1920×1440; assertions true; exit 0 |
| GUI scale 4 | AE `build/surface-api4-verified-scale4` and corresponding client log/exit JSON | 40 containers, 90 views; GUI 640×480, PNG 2560×1920; assertions true; exit 0 |
| Package identity/optional loading | AE `build/surface-api4-verified-consumer-audit.json` | Current hash, API 4, 41 profiles; no bundled Salt classes, ordinary-class Salt references or required dependency |
| Combined exact-package audit | Salt `build/surface-api4-verified-native-audit.json` | Five native client completions, both server configurations, 270 valid images and matching artifact hashes |

The terminal suite checks native search/filtering, scrolling/sorting, original row pitch, logical topology, native title movement and border resizing, real item extraction/insertion with cursor reconciliation, manual crafting, settings return, craft amount/plan/CPU synchronization, autocrafting and two independent same-type windows. It verifies overlapping keyboard ownership, pointer selection, focus transfer and owner-removal cancellation.

The lifecycle suite performs actual resource-pack reload and restoration, verifies changed/restored palette values and environment revisions, retains all 41 slot objects/contents and search state, exercises a real 320×240 GUI viewport with original width and re-prepared typing hitboxes, and checks management Escape, modal input blocking, one-time popup dismissal and parent lifetime retention. Screenshots are under AE `build/surface-lifecycle-native`. The compact-view capture establishes the original compact layout; management behavior is established by assertions. The separate modal capture establishes popup appearance.

Visual review inspected all 36 diagnostic contact sheets covering all 270 final captures. Full original screenshots were also inspected for processing patterns at scale 2, smithing patterns at scale 3 and stonecutting patterns at scale 4 where diagnostic crops could conceal content. Reviewed features include surviving original artwork, absence of added Salt chrome/backing/padding, transparent stepped gaps, protruding tabs, detached Upgrades/Toolbox panels, native slot grids, settings/key-type/amount forms and returns, populated catalogs and crafting error views. Live world pixels, inventory counts and hover states are not expected to match across runs. The retained compact AE renderer can crowd long title text next to its search box; integrations remain responsible for their own typography/layout.

Assertion completion, process exit and visual review were checked independently. Earlier atlases with an opening exception, obscured reload captures or disabled-resize configurations are historical diagnostics and are excluded from these final results.
