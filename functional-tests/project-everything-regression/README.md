# Project Everything automated regression tests

Current public-release qualification is tracked in
[RELEASE_READINESS.md](../../RELEASE_READINESS.md). The
[evidence reconciliation](../../output/release-readiness/reconciliation.json)
preserves historical attempts and links exact setup, equipment and AE2 follow-ups.
Earlier matrix totals are not acceptance of subsequently rebuilt artifact bytes.

Current build13 targeted replay is recorded in
[the focused queue](../../output/release-readiness/build13-followups.json).
The complete Core/publisher/input/server qualification is a separate campaign.
The dedicated multiplayer driver in `functional-tests/release-multiplayer`
uses separate client JVMs and a real production server without a server QA mod;
its prepared profiles are not passes until their own manifests say so.

These tests use Java/Python and the normal Minecraft input callbacks and network
handlers. They require no computer-use agent, screenshots or manual clicking.
Live integration tests launch a disposable Minecraft client and integrated
server; the JUnit layer runs without a client.

Synthetic block-use callbacks describe a point on their reported block face.
Authored ray hits are retained. MI's native face validation rejects an inside-block
point labelled as a top-face click; the geometry adapter has focused regression
checks for correct placement, authored-ray preservation and idempotence.
Progression and multiblock menus use native prerequisites: a nearby villager with
offers for Trading Post, Prominent's public unlock command, and 36 real reactor
items placed through Powah's own item-placement callback. No built/core state or
publisher menu provider is fabricated.
Native-only readers and selectors must close on Escape and reopen to the exact
same screen class. A missing menu ID is resolved to a real menu or screen class,
never compared as two null values. Earlier weaker proofs require reproduction.
Polymer's creative category browser is tested as an action selector: navigate
to the real building-block category, check that navigation grants no items,
close and reopen through its public command, then transfer tagged items through
an ordinary barrel. Its virtual category icons are excluded from physical slot
policy queries and conservation ledgers.

## Coverage layers

1. **Headless Core and compatibility tests:** all six mapped Minecraft versions.
   Includes gesture conservation, stale/unauthorized sessions, callback rollback,
   physical slot identity, restricted slots, sorting, native layout/texture
   boundaries, exact optional mixin descriptors and addon registration.
2. **Complete opt-in Core suite:** all twelve shipping version/loader targets.
   This exercises Core gameplay, menus, input, sorting, lifecycle, window
   controls, keybindings, HUD and bounds.
3. **Publisher integration cohorts:** ordinary block/item interactions open real
   menus. Native slot acceptance determines the test item. Real clicks transfer
   a tagged stack through the separate player inventory, a native mod slot,
   a moved window and another mod's storage when installed (otherwise a native
   barrel). Assertions check authoritative
   server counts, metadata, the shared cursor, sorting, equal slot indices in
   different sessions and independent closing. Right-click splitting is checked
in the current protocol as well. A real held-mouse drag across two native
storage cells checks equal distribution, the cursor remainder, recombination
and custom-data conservation once per applicable cohort.

Ordinary repeated left clicks use a recorded timing guard beyond SIU's native
250 ms double-click window. Without that distinction, two rapid single-click
steps can legitimately gather matching stock from another inventory cell.
Exact quantity/data assertions remain strict. Intentional Core pickup-all tests
use their separate input path and retain native double-click behavior.

The observer enumerates SIU's public `DesktopMenuBindings` slot access, including
custom cells outside Minecraft's vanilla slot list. Sophisticated limited barrels
use those extra native cells. Earlier opening-only records retain their original
coverage and require a focused replay before claiming transfers through them.
Limited-barrel placement first uses the ordinary teleport command to look
horizontally, preserving the player's position. A downward approach can place
the barrel facing up; its front-face interaction deposits items rather than
opening the inventory. The subsequent top-face menu use remains native.

The block ME-terminal scenario builds a powered native network and inserts a
real 1k cell into a drive by mouse. Deposits and withdrawals click AE2's virtual
repository cells through its own input and packets. Read-only native cell counts
verify the exact component-bearing item key; transfers to another mod's storage
and independent drive/terminal closing verify the shared cursor lifecycle.

Historical modern terminal block seeds contained only the part ID. Native AE2's
`KeyTypeSelection.readFromNBT` treats a missing enabled-type list as disabled
types (with a fallback type in newer releases), rather than a default terminal.
Those minimal world fixtures now explicitly enable the native item/fluid key
types. Existing authored filters and custom part NBT remain unchanged; legacy
1.20.1 seeds remain unchanged. Original actions and the fixture-normalizer hash
are retained. No filter is changed after menu opening, and no repository state
is assigned by the observer.

Every discovered layout gets an opening/policy check and an input round trip
where a physical input exists. Compound scenarios repeat for each distinct native
menu class. The JSON records the coverage tier of each case. Ghost-only and
status-only interfaces are recorded separately; opening them does not count as
testing their full functionality. This suite does not certify every machine
recipe, item upgrade, multiplayer topology or combination of optional mods.

Guard Villagers uses a separate, exact-publisher entity entry fixture because
its inventory is not opened from a block or held item. Setup grants the native
Hero of the Village permission, keeps AI enabled, and uses a public movement
attribute only to hold the disposable entity still. The ordinary entity-use
packet opens its native inventory. A zero-case profile remains a coverage gap
and requires a full replay; launching successfully is not a functional pass.

Held-source handoff releases the source lock before opening storage. A legacy
Sophisticated barrel needs main-hand activation: its offhand callback returns
success without opening a menu. That partner requests main-hand use explicitly;
other legacy partners retain the path that avoids a held tool's use-on-first
handler. The denied locked-source packet remains a separate negative check.

The current protocol also follows publisher child-menu widgets by type, label
and ordinal rather than by old screenshot coordinates. Where a native return tab
exists, it returns while carrying a tagged stack and checks the shared cursor.
Compact Sophisticated grids are scrolled with real wheel callbacks to reach
off-screen cells. Tom's emitter uses both native increment/decrement banks,
multi-digit text input and the attached threshold toggle; its block entity,
rather than a cached client data-slot field, supplies the authoritative values.

Each case rebuilds a bounded disposable work area before its authored setup.
This removes leftover fluid and fixture blocks from earlier cases and restores
the standing floor. The recorded commands use the observer's actual world
origin, including translated negative-height worlds. Cleanup never runs during
a measured menu interaction or populated-storage reopen.
NeoForge's block fixture also uses the ordinary teleport command to put the
player within native interaction range. Its older setup built a new block while
leaving the player at the previous case's location. Subsequent authored camera
or position steps still take precedence.

Sophisticated and Traveler's custom settings widgets are observed separately
from Minecraft's `AbstractWidget` list. Their entry selectors come from actual
native parent/child captures, then resolve the live publisher widget and its
current window placement. Settings-close scenarios preserve a tagged cursor
and reopen the native parent by ordinary interaction; they do not certify all
individual settings toggles.

SSN's native request button reads a hover cache populated during rendering.
The observer delivers Minecraft's native pointer-move callback, allows normal
frames to render and verifies the native hovered item before clicking. It does
not move the OS pointer or assign the publisher's hover cache directly.

Restricted input cells also receive a real forbidden-item click: the native
slot must reject it, preserve the cursor and retain its previous contents. The
modern metadata ledger uses canonical native item-codec values, rather than
component debug strings. Compact control popups are explicitly dismissed with
Escape before subsequent slot gestures, matching normal user interaction.
Once per applicable cohort, a populated partner is closed and reopened through
ordinary block interaction. Its resident tagged stack must stay out of player
inventory while closed, return with the exact quantity and complete data, and
withdraw once. This tests native menu save/restore without resetting the block.
Current 1.20.1 observers normalize the count and save the complete native item
stack, including serialized Forge capabilities, rather than inspecting its tag
alone. Each new case records `item_serialization_contract`; historical reports
without that field retain their narrower tag-only evidence.

Native processing is distinguished from storage. Conditioning modules must
consume one fuel and start their native burn timer; the remaining tagged fuel
must be recoverable. Simply Swords' forge must retain its complete native
editing original and restore the complete item on withdrawal. A valid Bountiful
decree player fixture is constructed through its public native item factory.
That factory's static Kotlin companion is read with Java's public-field
reflection; the production observer's instance-field helper cannot read it.
The generic exact-data ledger includes matching stacks already owned before the
new test seed. For example, six tagged Chiller fuel items left by its processing
check plus seven new tagged inputs must remain thirteen items with that exact
signature; a changed signature or a missing item still fails.
Conditioning module switches are clicked through their actual SIU extraction
after the burn check. Disabling reserve feeding prevents another legitimate
fuel consumption from being mistaken for lost storage items; the original
switch states are restored through the same controls before closing.
Bountiful's final close check also reads the actual block entity's decree
inventory. Closing the window does not relinquish ownership of resident world
stock. Its complete before/after data must match, and the closed stock is added
to the conservation ledger only after the owned menu has actually closed.
Preview results and filter examples are not counted as owned physical items.
Policy observations never call acceptance or pickup callbacks on virtual cells.
ProjectE's native `SlotGhost.mayPlace`, for example, writes a filter example as
part of a real click. Every snapshot fingerprints native menu contents, cursors
and player inventory before and after its observations; an observer-induced
change fails the run and retains `observer-policy-side-effects.json`. Older
ProjectE results without this read-only contract require a fresh reproduction.
The collector scenario uses real ghost copy/clear actions and charges a Klein
Star, then preserves the complete charged item through a second storage menu.
Relay tests first convert one native diamond into EMC, then charge and recover
a Klein Star. Matter-furnace input tests account for the publisher compacting
its smelting-input handler while keeping the fuel handler separate.

Simple Storage Network terminals are opened only after a real adjacent master,
storage cable and barrel have formed their native network. The repository test
projects the publisher's own displayed item cells into the SIU extraction,
uses ordinary pointer/click callbacks, and verifies native insert/request
packets against the connected barrel's complete item data. Moving the window,
cross-storage transfers and closing while carrying a withdrawal are checked.
This is not a claim of exhaustive SSN crafting or automation coverage.

Tech Reborn storage units use their native buffered quantity ledger: input can
move immediately to the output buffer, so an empty input cell is not item loss.
The scenario checks a tagged stack below one output-stack capacity, withdrawal,
movement, transfer through another source and close lifetime. The solid fuel
generator scenario separately verifies one native fuel consumption and energy
production before strict transfers during the active burn. These checks do not
certify storage overflow or every generator fuel and recipe.

Powah's reactor also consumes a carbon item while idle, before nuclear generation
starts. Its scenario places one real coal block through the native input and
requires exactly one conversion into the publisher's carbon ticker. With no
nuclear fuel, that reserve must stay stable before strict tagged oak-log
transfers. Missing fuel without a matching reserve still fails. This checks the
carbon-input interaction, not a complete nuclear-generation or coolant cycle.
The reactor is placed using the ordinary 36-item multiblock action. Its native
builder places one surrounding part every six ticks, so setup allows 240 ticks
and the fuel scenario requires the observed `builder.built` flag. The observer
only reads this flag; it never invokes the mutating `isDone` method or assigns
assembly state. An accessible input on an unfinished core is not proof of a
working reactor, and an unassembled or unobserved builder cannot pass the test.
Powah Furnators similarly consume one real coal block through their native
fuel slot. Native carbon reserve and stored energy prove that conversion;
the subsequent tagged-coal round trip runs during the resulting long burn.

Some native items lazily add their own data on first use. The publisher-pinned
`fixtures/native-item-specimens.json` records initialized player inputs copied
from authoritative native state for those cases. This keeps the full-data
conservation assertion strict; it does not treat legitimate native initialization
as lost data. The original failures and specimen provenance are retained, and
these fixtures never seed publisher menu slots.

Kibe trash cans have a separate disposal scenario. Their native inventory
deliberately discards inserts, so a storage-withdrawal oracle would be wrong.
Real right/left clicks must delete exactly one item and then the six-item
remainder. Header movement, empty-slot pickup, closing with an uncommitted
carried stack, and recovery through an independent barrel check that disposal
does not affect unrelated ownership or create phantom items.

Character launchers use the public attachment hit regions and actual input
callbacks. Their scenarios check repeated open/close, parent cleanup, the shared
cursor, Aether/general-equipment exclusivity and cape persistence. The worn Quark
backpack scenario checks that it follows the player inventory, stays independent
of Character movement, has no child close action, and keeps stored items across
parent close/reopen. These scenarios do not override native equipment rules.

## Reproduce

Run from the repository root with Python and the configured JDKs/Gradle caches.
The fixture manifests pin publisher jars and verify their hashes. A missing
artifact or platform pin is an error; the runner does not substitute a newer mod.

To run the complete mapped unit/contract layer (JDK 26 for Gradle):

```powershell
$testTasks = foreach ($mc in @('1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3')) {
    ':{0}:fabric:test' -f $mc
    ':compatibility:{0}:fabric:test' -f $mc
}
.\gradlew.bat --console=plain --max-workers=2 @testTasks
python functional-tests/project-everything-regression/collect_headless.py
python -m unittest discover -s functional-tests/project-everything-regression -p 'test_*.py' -v
```

The mapped tests are shared implementation checks; they do not replace the
separate live Fabric, Forge and NeoForge runs below.

```powershell
python functional-tests/project-everything-regression/prepare_recent_fixtures.py
python functional-tests/project-everything-regression/prepare_coverage_fill.py
python functional-tests/project-everything-regression/check_fixture_coverage.py
python functional-tests/project-everything-regression/install_observer.py
python functional-tests/project-everything-regression/native_entries.py
python functional-tests/project-everything-regression/run_core_matrix.py --run my-core-run
python functional-tests/project-everything-regression/run_everything.py --run my-integration-run
python functional-tests/project-everything-regression/collect_results.py --integration my-integration-run
python functional-tests/project-everything-regression/verify_release.py
```

Use a fresh run ID. `--target 1.21.1/neoforge` limits a matrix to one target;
`--round 0` runs only the first mixed cohort on every target. Individual drivers
accept `--cohort` and `--only` for failure reproduction. The master integration
runner holds an exclusive lock and runs clients serially. Do not start a Core
client while an integration client is running: desktop focus is part of some
Core behavior.

The tested subjects come from `dist/core` and `dist/compatibility`. Fabric and
Forge production integration runs retain original publisher and shipping SIU jars.
NeoForge development runs record any namespace adaptation required by the
development game. Original native entry workflows are gated by the publisher
jar hash and expected menu class. The excluded observer supplies only disposable fixtures and
observations; it neither supplies fake publisher menus nor overrides recipes.

## Evidence and failures

Results live in `output/project-everything-regression`. Launch logs live in
`build/project-everything-regression`; individual runtime profiles contain:

- `staged-artifacts.json`: exact artifact identities and hashes.
- `mod-interaction-checks.json`: case actions, checks, outcomes and coverage.
- `regression-state.json`: authoritative physical inventory and cursor state.
- `regression-click-inputs.json`: recent requested and actual slot hit targets,
  with the client's pending-mutation state and discard counter before each click.
- `regression-mutation-state.json`: client mutation/cursor observation immediately
  before the separate authoritative server snapshot. This helps distinguish a
  pending network intent from an executed click; it never retries a failed click.
- `regression-failures/<case>/`: state/reply snapshots retained on failure.
- Production runs also retain the complete compressed command journal.

The main Python driver records its hash at import time. Specialized scenarios
also retain the hash of the helper module actually loaded for that case;
subsequent edits are not represented as having run in an earlier client.

Targeted synchronization diagnostics can additionally record the actual vanilla
menu cache lists, state IDs and native `ItemStack.matches` results. These reads
do not refresh or acknowledge the caches. The complete server snapshots remain
in `regression-state-history.jsonl.gz`, including successful cases.
`MenuRevisionTrace.java` is a separate, opt-in JDI debugger for the exact
1.20.1 Fabric namespace. It reads real revision-increment call stacks without
invoking game methods or assigning game state. Its thread breakpoints affect
timing, so a debugger run is diagnostic evidence and never a functional pass.
The ordinary runs and their failures remain the functional evidence. The first
JDI attempt hit a native `jdwp.dll` fault before any test command completed; see
`native-runtime-faults.json`. Later diagnostics use the separate, bounded
`NativeRevisionAgent.java` stack logger instead. Its bytecode self-check verifies
unchanged revision returns and cache replacement plus bounded log output. This
instrumentation is excluded from shipping jars and its runs remain diagnostic.

`run_native_charm_probe.py` uses the failing mixed cohort's exact publisher/API
binaries with SIU absent. It distinguishes a native kiln construction failure
from SIU's menu path; its result is not a shipping-SIU compatibility pass.

Both shipping subject hashes must match the current Core and compatibility jars.
If either subject changed, the cohort is replayed in full. Targeted results are
never merged across changed subjects or publisher binaries, and older positive
results remain unverified for the current release until replaced by fresh proof.

`specialized-entry-coverage.csv` accounts for child/configuration entries from
the original UI atlas. It requires the exact publisher binary, screen class,
version/loader and current shipping subjects. Entry-only evidence remains
entry-only, and a passed scenario retains its specific scope.

Failures are preserved. A launch failure, a missing menu, a fixture problem, a
native consumption mismatch and a failed conservation assertion must be
investigated separately. Do not turn an untested interface into a passing
functional result. Any report should state its exact coverage and remaining
gaps instead of claiming all possible interactions are proven safe.

`--max-cases 3` performs a platform smoke check. Smoke attempts are retained as
evidence but never satisfy a complete cohort in `collect_results.py`. The native
entry catalog separately records specialized child-menu entry workflows that
still require semantic tests; it does not count them as passing scenarios.

The Forge 1.20.1 Sophisticated cohort preloads a publisher-generated default
enabled-item cache, gated by the exact publisher hashes and an all-enabled value
check. This avoids the publisher's first-run concurrent config-write race during
recipe loading. It changes no recipe or inventory setting and is recorded in the
disposable profile. A selected held-menu source intentionally blocks world use.
The partner scenario closes that source, opens storage, then reopens the same
held menu through ordinary item use. Its native source identity, complete slot
shape, host item and tagged contents must survive. Reopening existing populated
storage never replaces its block. AE2's tool keeps its bound inventory cell when
selecting an empty hotbar cell for native block activation.
Partner observations retain the actual transferred item as their policy probe.
An accepted diamond must not stand in for a blacklisted Create package. When a
partner refuses that item, the scenario verifies a real rejected click and exact
ownership before continuing its lifetime checks through ordinary storage.
Where the server's locked-use hook is present, a separate deliberately denied
packet must leave the host/cursor intact and no pending block target behind.
This negative packet test is not presented as an allowed player workflow.
Aircraft source identity comes from the authoritative owned menu, including
the publisher's original menu class, vehicle UUID and native slot indices.
Across attached panes those indices must cover the original inventory exactly
once. A missing optional renderer menu-ID field is not a missing native source.

`stage_runtime_fixes.py`, `stage_sort_fix.py`, `stage_toms_fix.py` and
`stage_header_slot_fix.py` and `stage_provider_data_fix.py` are guarded, one-time release
staging tools for defects found by this audit. They preserve public API bytes,
unrelated jar entries, checksums and bundle consistency. They are not required
to rerun the tests and must not be run against an already staged baseline.

`stage_aether_fix.py` stages the optional Curios launcher guard. The Forge
themed fixture also records a full-cohort revision: its original Aether1.4.2
dependency lacked the field used by Deep Aether1.1.7's creative-tab callback;
the cached native Aether1.5.2 supplies it. Both manifests and the initial native
failure remain available. Targeted proofs are never merged across changed
publisher binaries.

`stage_provider_fallback_fix.py` and `stage_opening_priority_fix.py` retain the
native fallback writer and the existing priority of explicitly captured or
registered SIU opening data. They are also one-time staging tools. Public API
bytes are checked against the saved initial release, and QA classes are excluded
from every shipping jar.

The source-boundary guard is a separate repository check. The initial 31
violations remain preserved; the current source and production-bytecode guards
pass after migration to additive public contracts. Focused compilation or
packaging is not reported as a successful complete build or gameplay suite.

`verify_release.py` now verifies all 36 normal-build module jars, the exact bundle
and checksums, test-code exclusion, license presence and existing public binary
members against the preserved pre-qualification distribution. New defaulted API
contracts are allowed; existing signatures must remain. The old exact-entry,
byte-identical API audit is retained only as `--historical-scoped`. It cannot
qualify a complete source rebuild. Runtime acceptance is a separate hash-matched
gate; artifact integrity alone is not release approval.

`stage_native_writer_provenance_fix.py` is another guarded, one-time staging
tool. Native `openMenu` fragments use a publisher wire format; registered SIU
factories may require a different complete payload. Explicit SIU API callers
still own their supplied complete payload. The mapped regression tests verify
both precedence rules, exact byte counts and absence of duplicate provider data.
`stage_registered_native_writer_fix.py` extends that selection to explicitly
integrated windows, which bypass the automatic-native branch. Its separate
regressions and staging evidence preserve the unsuccessful intermediate replay.

`NativeSelectorEligibilityTest` invokes the actual Core eligibility path and
registered API callbacks. The same vanilla chest type must accept ordinary
storage, decline an action selector and fail closed when its eligibility
callback throws. `stage_native_selector_fix.py` is a one-time staging tool for
that fix. Its per-instance compatibility predicate only activates with Polymer;
it does not disable ordinary chest or barrel windows.

`ScopedUseTargetTest` invokes the actual temporary block-identity scope. Success,
exceptions, nested calls, uncaptured nested actions and separate players must
restore the correct context. The packet mixin scopes the real game-mode call
after packet validation instead of recording a target before a cancellable
handler. `stage_scoped_target_fix.py` is its guarded one-time staging tool;
source locks remain enabled and public API bytes remain identical.

`ScopedUseTargetMixinTargetTest` also reads the real mapped vanilla packet
handler bytecode and requires the redirect descriptor to identify exactly one
game-mode call. The Forge 1.20.1 production jar additionally needs its SRG
refmap entry. `stage_scoped_target_refmap_fix.py` stages that entry once, after
proving the complete built refmap adds only the required mapping in the default
and `searge` tables. These checks supplement the live packet-path tests.

To run a new complete publisher sweep, use a unique run ID:

```powershell
python functional-tests/project-everything-regression/run_everything.py --run project-everything-new-01
```

The runner holds a single-client lock. Do not launch another runtime test while
it is active. Headless Python infrastructure tests can run independently:

```powershell
python -m unittest discover -s functional-tests/project-everything-regression -p 'test_*.py' -v
```

The legacy GeckoLib/Galosphere regression also checks the actual merged native
ID-comparison helper with eight absent/zero/present-ID combinations. An empty
player inventory must retain the same revision during an idle interval after
opening the Combustion Table. Ordinary transfers then exercise that window and
the subsequent Meadow windows without a debugger or logging agent.
`stage_geckolib_fix.py` is a guarded one-time release staging tool, not a test
reproduction step. Its two new internal classes are explicitly allowlisted;
unrelated archive entries and public API bytes remain unchanged.

Later audit profiles record `qa-display-settings.json`: ordinary 60 FPS,
pause-on-focus-loss disabled, and the idle frame limit set to minimized-only
where supported. This avoids treating callback-driven tests as AFK. It changes
no game clock, action wait, packet barrier, inventory setting or shipping jar.
Earlier runs retain their original options and evidence.

Atlas seed commands are checked against the actual runtime registries, including
item IDs embedded in component/NBT commands. An absent optional item is recorded
as an invalid historical workflow, never a functional pass. ME Requester's
wireless terminal, for example, requires AE2WTLib. The two wireless cohorts add
the exact Requester publishers from the native references; their unchanged AE2
and AE2WTLib dependency hashes, original manifests and required full replays are
recorded under `output/project-everything-regression/fixture-revisions`.

Moving a native wireless fixture also moves its known overworld link component.
Unrelated integer arrays and other dimensions are preserved. Pigment Shifter
fixtures seed its valid waterlogged block state through a public world command;
the menu still opens by native block use. Explicit Shift-item interactions send the native modifier packet, perform
normal item use and release the modifier in `finally`, so an open desktop cannot
silently turn a camera-attachments request into camera activation. These are
test-input corrections, with separate live confirmations and retained failures.

## Focused intermittent AE2 click reproductions

`run_click_investigation.py` repeats only the affected portable mana-cell or
Requester workflows. It holds the shared single-client lock and preserves each
trial's assertions, click observations and failures separately. It never retries
a failed click or treats a later successful trial as a fix.

```powershell
python functional-tests/project-everything-regression/run_click_investigation.py --run ae2-click-new-01 --target 1.20.1/fabric --repeat 4 --quiet --context
python functional-tests/project-everything-regression/run_click_investigation.py --run requester-click-new-01 --target 26.1.2/neoforge --repeat 4 --quiet --context
```

Use a fresh run ID. `--context` includes the immediately preceding related native
menus; it does not run the complete publisher cohort. `--quiet` disables the
observer's detailed diagnostic logging without changing input callbacks, game
ticks, packet handling or assertions. Separate `--trace` runs enable the existing
Core trace logger and can affect timing. These diagnostic passes must remain
distinguishable from quiet functional evidence. Ghost-only menu entries retain
their opening/policy coverage.

The final passive-host reproduction and fix are recorded in
[`passive-source-fix.json`](../../output/ae2-click-investigation/passive-source-fix.json)
and the [click-fix explanation](../../docs/markdown/ae2-requester-missed-click-fix.md).
The older `output/ae2-click-investigation/summary.json` predates that fix.
An individual historical failed click remains unresolved if its matching path
cannot be reproduced and explained; repeated passes alone are not a causal fix.
