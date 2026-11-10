# Remaining UI audit fixture

These are disposable observation tools, excluded from production. Use the exact
audited artifact fixture for each cohort and retain the original files and
SHA-512 checks. Run a native client with SIU entirely absent and a separate
client with the shipping Core jar only. Compatibility and Controlify are
forbidden. Forge development-namespace remapping must be retained in its
`forge-remap-lineage.json` and described with the evidence.

The named development launchers have been used for 1.20.1 Fabric/Forge and
1.21.1 NeoForge captures. Some original Fabric releases could not be reliably
mapped into the development namespace. `tools/prepare-production-fabric-audit.py`
therefore also prepares a normal 1.20.1 Fabric runtime: original subject, Core
and dependency JAR bytes remain untouched; only the disposable observer is
compiled/remapped. Native and Core production runs both use creative mode.
The earlier named captures used native creative and Core survival; preserve
that limitation when interpreting their controls.

`run_cases.py` accepts a JSON object with `minecraft`, `loader`, and `cases`.
Each case has a lowercase-hyphen `id`, `family`, and explicit `native_actions`
and `core_actions`, each an array of existing driver commands. Include the
actual source setup and the original block, item, or entity interaction in both
lists. The runner records each response and the full, untouched screenshot's
SHA-256. It deliberately rejects forced providers and template registration.
Review the observed screen and menu classes, slots, and Core window definition
before calling a pair valid. An unavailable native UI or failed opening is an
audit outcome, never a fabricated screenshot.

Use a fresh run id for each launch, wait for its `ready.json`, then invoke
`python functional-tests/remaining-ui-audit/run_cases.py CASES OUTPUT native`
or `... core`. Close each client before launching the other. Preserve launch
logs, the input manifest, staged artifact hashes, and all raw captures.

`open-ray` uses the actual client BlockHitResult and verifies that it hits the
fixture block before using it. The older `open` command uses a synthetic hit
at the block center and is unsuitable for screens that validate the clicked
face or position. A failed simplified driver interaction is not evidence
that a real player cannot open the UI.

`use-block-ray` performs the same ordinary client interaction at the actual
crosshair BlockHitResult without restricting the hit to the fixed fixture
coordinate. This allows genuine placement of cable parts against an adjacent
support and subsequent use of the placed part. It does not fabricate a hit,
invoke a menu provider, or construct a screen.

`key-state` sustains an existing ordinary key binding by its translation name,
for mods that poll a held binding rather than buffered key presses. It changes
only the vanilla key-binding state and is released explicitly or by `close`.
This is disclosed simulated held input, not a physical held-key claim, direct
network payload, or call to a mod's menu-opening method.

The design-only `native-wands-tools-request` follow-up invokes the upstream
Building Wands `MENU` key packet with Shift set while a real wand is held.
The original server creates its own menu and screen. This is explicitly a
native visual-reference fallback for the mockup atlas; it is not a physical
Shift+Y input test and does not change the older audit's keyboard coverage.
It never constructs a target menu or screen and is excluded from production.

`use-entity-ray` (native and production universal observer) and
`client-use-entity-ray` (named Core observer) require the actual crosshair
EntityHitResult. They run vanilla `interactAt` first, then ordinary `interact`
only if the first result is not consumed. The generic entity command alone
does not exercise hit-location events. `sneak` sustains the ordinary key-binding
and input state for a later interaction; this is disclosed QA input simulation,
not a physical held-key claim or a forced menu. Close releases the input state.
The NeoForge named observer's legacy `open` defaults to a provider shortcut;
the comparison runner rejects it unless `realUse` is explicitly selected.

`tools/index-remaining-ui-captures.py` verifies paired screenshot hashes and
matching upstream artifact sets, rejects manual Compatibility templates, and
produces the coverage ledger. `output/remaining-ui-audit/entry-reviews.json`
records source-reviewed shared helpers and internal recipe menus that should
not be mistaken for separately openable content screens. Related entries can
be declared on an observed pair when the same actual workflow demonstrates
an installed add-on; matching class names alone do not establish that.

These observations establish opening and captured appearance only. They do
not certify every possible recipe, animation, packet, item transfer, multiplayer
state, or supported Minecraft/loader combination. Uncaptured reachable screens
remain explicit coverage gaps, even when another screen in the mod was captured.

Prominent Base is an embedded independently initialized mod whose outer
GLOBAL initializer assumes the entire original pack. Its isolated Base fixture
is identified separately from a complete GLOBAL runtime. For lottery/trader
observations, `tools/prepare-prominent-audit-resources.py` stages two original
parent-provided data definitions byte for byte in a code-free support JAR. The
lineage records their source and hashes; no screen, menu, adapter, class file or
asset is supplied by that wrapper. Both comparison sides use the same data.
The earlier broad parent-assets attempt failed because of font overrides and
is preserved as setup evidence, not a subject-mod compatibility failure.

The named Core observer also supports `client-use-any-block-ray`, which forwards only the actual current BlockHitResult to vanilla item-on-block use. Unlike the older origin-guarded command it can place a real adjacent network master. It does not manufacture a hit, construct a menu, or call a target mod opener. Named Core fixture origins come from the actual test player spawn; world/camera coordinates must be translated from the native fixture origin and never assumed to be identical.
