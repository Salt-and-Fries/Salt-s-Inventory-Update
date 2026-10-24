# Full validation - October 23-24, 2026

**Status: completed; all recorded acceptance checks passed.** The final normal
production rebuild passed with 1,637 fresh unit tests in 224 suites and zero failures,
errors or skips. The runtime matrix passed 220 client gameplay/UI profiles, 58 dedicated
server profiles and twelve separate source-bootstrap smoke profiles. Three additional
independent physical-input/multiplayer profiles also passed. All 36 final release JARs
are byte-identical to the artifacts accepted by those runtime checks.

An additional log audit found rejected 1.21.11 Core/Controlify pack metadata.
Fabric now omits deprecated `supported_formats` and retains exact modern bounds.
The two NeoForge JARs use the loader's synthesized client/server metadata, since its
resource and data codecs differ. This follows the [NeoForge metadata documentation](https://docs.neoforged.net/docs/resources/metadata/)
and was confirmed in the installed 21.11.42 `ResourcePackLoader`. The first intermediate
NeoForge correction passed gameplay but failed the stricter pack-error gate; it was
not accepted. All affected current-artifact profiles subsequently passed. Across the
220 accepted client logs and 58 accepted server profiles, the final audit found no
rejected Salts pack metadata. Classes and assets were unchanged by this packaging fix.

Versions remain **Core 0.1.5 / Compatibility 0.1.2 / Controlify 0.1.1** on
`Spatial-GUI-support-test`. No releases are published, committed or pushed by this goal.
This is the full validation explicitly requested by the user.

## Targets

| Minecraft | Loaders |
| --- | --- |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge |
| 1.21.11 | Fabric, NeoForge |
| 26.1.2 | Fabric, NeoForge |
| 26.2 | Fabric, NeoForge |
| 26.3 | Fabric, NeoForge |

Upstream releases/dependencies are pinned in per-run fixture manifests. Availability
is recorded in `compatibility/coverage-targets.json`. This does not establish support
for every upstream patch release or arbitrary modpack.

## Findings and fixes

| Area | Finding and correction | Scope |
| --- | --- | --- |
| Original 27 test failures | Unsafe-allocated fixtures bypassed constructor initialization of `builtinAttachments`. Fixtures now establish that invariant; no assertion was removed or production behavior relaxed. The corrected focused run passed all 42 tests. | Shared gesture and legacy session fixtures |
| Curios live shrink | Shrinking 100 native slots to eight crashed broadcasting. Guard obsolete slot reads and topology-dependent broadcasts; reject queued old-layout clicks until session replacement, preserving the shared cursor. | All six Curios adapters |
| NeoForge screen lookup | A legitimate null current screen triggered unsupported repeated fallback lookups. Return the null directly when the field exists. | All five NeoForge Core targets |
| AE2 remapped startup | Normalize dotted/slash class names before the native patch allowlist. Added a test for both forms and rejected targets. | AE2 15 / 1.20.1 Fabric |
| Forge production mixins | Deferred configuration replaced the complete owned mixin manifest with the Sophisticated list. Restore the complete list and audit manifest/metadata agreement. | 1.20.1 Forge Compatibility |
| Sophisticated server isolation | Common reflection resolved client factory signatures, producing caught initialization failures despite server readiness. Defer client factories into nested registrations; isolate legacy client message handlers too. | Eight applicable targets; legacy bridges on both 1.20.1 loaders |
| Sophisticated Forge refmaps | Explicit native synchronizer method descriptors remap both selector and vanilla invocation to SRG names. Audit both mappings. | 1.20.1 Forge Compatibility |
| Sophisticated popup ABI | Native bounds changed from Mojang Pair to Minecraft Tuple. Select separate hooks from bytecode metadata without initializing the target. Three tests cover Pair, Tuple and unsupported layouts. Actual Tuple popup bounds, hover detection and movement passed in game. | 26.1.2 NeoForge Compatibility |
| Controlify metadata | Add owned mixin configurations' missing `minVersion: 0.8`. Controller gameplay classes were unchanged during this validation. | All twelve Controlify artifacts |
| 1.21.11 resource packs | Fabric removes deprecated `supported_formats` and retains exact modern bounds. NeoForge uses synthesized client/server pack metadata. Artifact guards enforce both policies; preserved invalid JARs fail the new gate. | Fabric/NeoForge Core and Controlify (four JARs) |

Compatibility continues to use Core's supported API. Native integration mixins target
the integrated mods, rather than private Core implementations. The older popup Pair
branch is unit-tested, not separately gameplay-tested on an older upstream patch.
The Tuple fixture supplies preview choices to the real native UI; it does not
fabricate server recipes or prove every ambiguous-recipe crafting interaction.

The source-bootstrap launcher also used dotted Minecraft versions in a profile ID
that only accepts lowercase letters, digits and hyphens. A separate sanitized profile
name fixes that prelaunch rejection while retaining versioned log filenames. The
original failed attempt is preserved.

QA repairs also corrected stale texture/enum/packet/API expectations, menu sequencing,
authoritative fixture synchronization, native GUI accessor mappings, D-pad paths and
isolated runtime-directory verification. Disabled-animation checks observe the first
actual native radial frame. Curios wall checks require accepted movement and exact
right/bottom coordinates after render settling. Behavioral assertions remain intact.

The refreshed 26.2 AE2 atlas exposed a QA sequencing race: its server-side encoding
fixture update was still queued when the client clicked and checked the next mode.
The modern atlas fixture now waits for a bounded server completion before the native
click, captures the server instance safely, and retains the selected-tab assertion.
A focused pattern-terminal run passed all mode/settings variants without production
changes. The full 26.2 atlas then passed on an unchanged retry after one world-loading
timeout before atlas assertions. All twelve final atlas/CPU profiles passed.

## Runtime matrix

Only passes with production SHA-256 values matching current `dist` are accepted.
Changed input JARs invalidate earlier passes. Failed attempts remain preserved.
Records include setup, observations and screenshots, not only distinct assertions.

| Phase | Profiles | Current result |
| --- | ---: | --- |
| Complete Core gameplay | 12 | PASS; 9,094 records |
| Optional Compatibility integrations/absence | 66 | PASS; 3,745 records; zero caught Salts integration/mixin failures |
| Trinkets/Curios | 8 | PASS; 614 records; exact joined edges on all six Curios targets |
| Tom's Storage pickup | 12 | PASS; 600 records; 24 terminals and 72 required withdraw/cursor/deposit checks |
| Mouse Tweaks with Core only | 12 | PASS; 1,228 records |
| Raised with Mouse Tweaks | 12 | PASS; 2,544 records |
| Spatial GUI with Core only | 12 | PASS; 5,022 records and 1,044 PNG captures |
| Configuration entrypoints | 12 | PASS; 108 records |
| Ancient Remnants browser search | 8 | PASS; 358 records with applicable REI/JEI combinations |
| Controlify | 54 | PASS; 36,203 records across twelve standalone, twelve extension baselines, 26 focused and four native-mod/browser profiles |
| AE2 atlas/CPU lifecycle | 12 | PASS; six native atlases and six real crafting-job/cancellation profiles |
| Dedicated servers | 58 | PASS; six official production Forge, 52 release-JAR development server launcher profiles |
| Source-injected bootstrap smoke | 12 | PASS; 668 assertions; separate from production-JAR gameplay |

Matrix total: **290 main profiles** (220 client gameplay/UI, 58 servers, twelve
source-bootstrap smoke), plus three independent profiles below.

Core runs cover native workstations and applicable mounts; authoritative item gestures;
chest opener and persistent-window lifecycle; recipe-book reopening; Hold/Toggle and
remapped vanilla Inventory bindings; in-place and transfer sorting/protected slots;
HUD visibility and selection locks; and four-wall bounds for native/API windows.
Compatibility runs cover target absence, available JEI/REI/EMI providers, Tom's Storage,
Sophisticated Storage/Backpacks, AE2 and the applicable accessory mod. Trinkets checks
include popup stacking and shared offhand. Curios checks include G/repeat, equipment/
cosmetics, rightmost toggles, scrolling, growth/shrink, stale clicks and joined walls.

Mouse Tweaks covers configuration switches, native/API gestures, fractional wheel input,
protected/virtual/covered slots, crafting exhaustion and item conservation. Raised covers
live offsets, hits, world ownership, GUI scale, drag distribution and window-hosted HUD.
Spatial covers projection modes, open/close/interrupted transitions, rebound movement,
camera ownership, fixed desktop geometry, walls, HUD restoration and world interactions.

Controller profiles comprise twelve standalone, twelve extension baselines, 26 focused
and four native-mod/browser combinations. Real Controlify processing uses virtual
controller input and server assertions: navigation, workstations, recipe wheels, native
keyboards, API controls, layout/animation, onboarding and selected mod/browser combinations.
Tom, AE2 and Sophisticated are included where applicable; Curios/Trinkets plus Controlify
is not a selected combination. AE2 atlas profiles verify version-specific native menus,
presentations, geometry/controls and screenshots. Legacy has 39 menus/76 presentations; modern has 40 menus/90 presentations.
Comparing the six refreshed atlases against previous successful Salts atlases found
512/512 matching geometry and control layouts. There were 395 exactly matching body
crops and 81 exactly matching full visual crops; this is not full pixel identity and
the references are earlier Salts captures, not upstream golden images.
CPU profiles require two real jobs and desktop cancellation verified on the server,
including active/idle synchronization. Native AE2 observation code is grouped with the
original archive where required; its CodeSource is not claimed to be a plain production JAR.

## Independent physical input and multiplayer

**Physical Mouse Tweaks PASS, 26.3 NeoForge:** original Mouse Tweaks 2.31 plus shipping
Core, without the two extensions. Windows Computer Use exercised five simultaneous
windows (native chest/barrel, API chest, crafting, player). Seventeen read-only server
checkpoints cover collection across native/native and native/API windows, right split/
placement, double-click limits, crafting exhaustion, virtual/covered-slot wheel exclusion
and empty final cursor. Stone 152, dirt 42, iron 3 and inventory planks 12 were conserved;
four grid planks produced one table. Two F2 images are retained. Exploratory checkpoints
05-08 are ordinary virtual-slot clicks, not successful virtual-wheel/crafting checks;
corrected checks occur later. The physical drag did not visit every intervening slot.

**Separate local dedicated Spatial PASS, 26.3 Fabric, present and absent:** clients joined
a separate server process with original shipping Core only (no QA/Spatial/extensions).
Native mouse-pipeline transfers moved 32 dirt from slot 9 to 10; independent RCON reads
confirmed one destination stack, empty source/cursor. Both processes exited cleanly.
These are loopback multiplayer checks, rather than WAN/latency tests.

## Build and artifact verification

**Final forced production build: PASS.** All 1,637 unit tests in 224 suites were
written afresh during this build, with zero failures, errors or skips. The init script
forces normal Java compilation and tests, disables incremental Java compilation, and
excludes functional QA sources. No cached test results are counted as fresh tests.
The root build finished in 5m 48s: 423 tasks, including 156 executed tasks.

```powershell
.\gradlew.bat -I build/full-validation-20261023/final-production.init.gradle `
  -PincludeFunctionalTests=false build --continue --no-parallel --max-workers=2 `
  --no-build-cache --console=plain
```

The final distribution matched the recorded runtime hashes for all 36 JARs. Source/API,
compiled-class, removed-feature, coverage, Mouse Tweaks and controller gates passed.

Outputs contain twelve release JARs and `SHA256SUMS.txt` in each directory:

- `dist/core/salts_inventory_update-<Minecraft>-<Loader>-0.1.5.jar`
- `dist/compatibility/salts_inventory_compat-<Minecraft>-<Loader>-0.1.2.jar`
- `dist/controlify/salts_inventory_controlify-<Minecraft>-<Loader>-0.1.1.jar`
- `dist/salts-inventory-all-latest.zip`: all 36, arranged by target.

Gates check metadata/dependency pins, loader/Java targets, production remapping,
mixins/refmaps, required resources, no QA/source contamination or embedded Core/API copy,
removed-feature symbols/callers, source/API parity, examples, shared textures and exact
bundle contents. Final rebuilt hashes must match accepted runtime inputs. No root clean
is used; the evidence is preserved.

## Limits and diagnostics

- Physical gamepad/haptics, other OSes, arbitrary resource packs/modpacks, unlisted
  combinations and WAN conditions are unverified. Mouse Tweaks plus Controlify was not
  a selected combined profile.
- Most runtime clients/servers use shipping JARs with recorded development launcher/remap
  lineage. Six Forge servers use the official installed 47.4.0 production runtime with
  unmodified release JARs. This is not retail-launcher coverage for every target.
- Source-bootstrap smoke stops process trees after PASS; it does not establish graceful
  game shutdown or production-JAR gameplay behavior.
- Selected native screenshots were inspected; thousands of other captures were not all
  manually reviewed. Some native onboarding/QA overlays obscure parts of some images.
- Passing this coverage does not promise absolute bug freedom.

The client diagnostic inventory reviewed all 220 current logs and retains actual
messages. In the 26.3 NeoForge REI profile, 26 caught tooltip-worker exceptions
(four render-pass conflicts and 22 wrong-thread calls) have GuideMe font/progress-bar
code called from REI tooltip caching on every stack, with no Salts stack frame.
Their origin is attributed from those stacks; an upstream-only baseline was not run.
They remain documented diagnostics, not proof that this third-party combination has
error-free logs. Verbose atlas runs also report Mixin Java-level advisories and a
missing `@Final` annotation on the read-only Core options shadow; current runtime
hooks passed, and production behavior was not changed to silence these advisories.

Other reviewed upstream/environment diagnostics remain: Porting Lib server client-mixin
warnings, missing optional Chipped advancements, Tom's native advancement JSON warnings,
datafixer/remapper/refmap messages, offline authentication, SDL/binding version warnings,
and Windows OSHI/Perflib diagnostics. Logs are not described as error-free.
Tom's Forge development DistExecutor failure reproduced with unchanged Tom alone; the
official production runtime avoids that development validation and passed Tom-only and
Salts-plus-Tom checks. No upstream mod was patched to hide it. One Forge early-display
crash occurred before gameplay; an unchanged retry passed. Its crash report is retained.

## Evidence and reproduction

Resumable driver: `python tools/run-full-validation.py --phase <phase> --run-id fullval-20261023 --resume`.
Commands/targets are in the driver and per-phase manifests. Individual launchers live in
`functional-tests/scripts/` and `tools/Run-ControlifyReleaseQa.ps1`.

Evidence is under `build/full-validation-20261023/`:

- `fullval-20261023/<phase>/results.json`: accepted attempts, commands and JAR hashes.
- Per-phase `phase-summary.json` and visual-review indexes: aggregate/selected image evidence.
- `regressions-before/`, failed attempt logs, `validation-journal-before-final.md`: original
  failures and the complete intermediate journal.
- `validation-progress.json`: progress only; not final acceptance.
- `distribution-after-root-r16.json`: runtime artifact reference before final rebuild.

Physical evidence: `build/mouse-tweaks/gameplay/26.3-neoforge-fullval-20261023-manual-mouse-a3/`.
Separate-process evidence: `fullval-20261023/remote-spatial/results.json`.
Final evidence files:

- `final-root-build.log`, `final-build-start.json`, `final-build-end.json`: final build and timestamps.
- `junit-final.json`: fresh per-suite counts and source XML paths.
- `final-artifacts.json`: 36 final JAR checksums, metadata and exact bundle verification.
- `final-runtime-evidence.json`: complete current-hash matrix and independent proof references.
- `audit-*-final.log`, `removed-features-final.json`: source/production architecture gates.
- `client-diagnostics-reviewed.json`, `rei-guideme-diagnostic-origin.json`: preserved diagnostics and stack-origin evidence.
- `atlas-comparisons/summary.json`: all six refreshed AE2 atlas comparisons.
- `visual-index-final.json`: current hashes for 55 selected, previously inspected image entries across six feature areas; not a review of every capture.
- `startup/attempt1-bootstrap-logs/` under the run folder: preserved profile-name prelaunch failure.

The report covers executed scenarios; it does not promise that every possible modpack,
input device, configuration or future upstream release is free of bugs.
