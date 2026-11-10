# AE2 / ME Requester missed-click investigation

**Historical stage report.** The results below identify the original scoped
passive-host fix. Current release qualification is tracked in
[RELEASE_READINESS.md](../../RELEASE_READINESS.md); subsequent rebuilt jars do
not inherit these passes automatically.

Release qualification on November 6 found two additional publication gaps.
Core now broadcasts native player changes before retaining their immutable
revision. A later 26.2 delayed replay also exposed native callbacks publishing
a temporary cursor before commit, and modern clients ignoring the revision in
the authenticated SIU cursor payload. A candidate suppresses that temporary
publication and applies the canonical revision before deferred client input.
It also reserves a new immutable revision when a native cursor-only transport
does not advance one. The corresponding headless regression covers both native
revision behaviors; unchanged publications do not allocate extra revisions.
The normal build13 six-target follow-up queue and final exact-build13 26.2
delayed replay passed **1,380 focused live assertions**. The
[replay journal](../../output/release-readiness/build13-followups.json) records
the first 1,250; the [final 26.2 run](../../output/ae2-click-investigation/readiness-build13-262-delayed-01/run.json)
adds two delayed trials and 130 assertions on the exact current Core and
Compatibility bytes. Every integration target received both ordinary and
delayed shipping-jar input. Earlier failed and intermediate-artifact trials
remain separate. The 324 focused tests
across six mappings cover publication, intermediate cursor handling and
gestures. These are focused results; final complete qualification is still
running. Original failed trials
and the source delta are retained under `output/release-readiness/` and
`output/ae2-click-investigation/readiness-262-delayed-01/`.

The intermittent click failure was reproduced. It is an SIU synchronization issue, not evidence of items disappearing in AE2.

## Cause

A portable menu drains energy while it remains open. That updates the host item's NBT/components in the player's inventory. Minecraft advances the player-menu revision, even when the slot being clicked and the shared cursor have not changed. SIU previously rejected the arriving player-inventory click solely because its revision was behind.

This was observed with Applied Botanics and MEGA Cells portable mana cells on Minecraft 1.20.1 Fabric, and the ME Requester wireless terminal on 26.1.2 NeoForge. Server traces record the original input, the stale/current revisions, the rejection, and the unchanged clicked stack/cursor. The observer's authoritative history separately records passive energy changes in the host slot.

## Fix

Core retains a bounded history of published player-menu snapshots: at most 32 revisions, expiring after two seconds. An ordinary left/right pickup click from an older revision can proceed only when its complete cursor, clicked slot, every editable slot and slot topology still match. Metadata changes are allowed only in other, currently protected source slots whose item type and count remain unchanged. Session tokens, connection authorization, normal native-menu revision checks and other gestures retain their existing validation.

The AE2 adapter now registers native inventory-backed item hosts through the existing `DesktopItemSources` API. Their identity follows the exact live source object rather than a full metadata fingerprint that becomes invalid when charge changes. Multiple views of the same source share its identity; closing releases the reference-counted protection.

The focused source-pickup test also exposed missing legacy enforcement: 1.20.1 acquired source leases only for automatic windows and skipped the ordinary-click source guard. Both now follow the protections already present on newer targets. A player cannot take the portable source out from under its open detached menu.

No public API or packet format changed. Candidate/promotion reports verify public API bytes and unrelated archive entries remain identical. Test agents and observers are excluded from distributed jars.

## Verification

The main reproduction uses real game screen callbacks and networking without computer use. A disposable Netty fixture introduces 75 ms outbound delay with deterministic ±25 ms jitter, preserving FIFO order. Click waits vary between one and six normal game ticks. The observation fence waits for delivery and each original server decision; it never retries a click or waits specifically for acceptance.

Checks cover restricted-item refusal, tagged insertion/withdrawal, right-click splitting, moved-window coordinates, cross-mod shared-cursor transfers, sorting, independent close/reopen, quantity conservation and full transfer-item metadata. Additional checks verify the source lock and attempt a real pickup of the open portable host. Separate runs disable both the diagnostic agent and artificial delay.

The snapshot validator has 16 focused headless tests on each of six mapped Minecraft versions. These include changed editable items, metadata/count/type changes, cursor mismatches, expired/unknown revisions, replaced topology and cleared history. Core packages build for all twelve targets; native AE2 member references resolve on all six targets that currently contain that integration.

Final results: ten independent trials, fifteen passing menu executions and 626 live assertions, plus 96 headless validator tests. The instrumented passing runs contain 234 ordinary server click decisions, including nineteen safely accepted stale-revision clicks, with no unexpected rejections. The final legacy run uses the promoted distribution jars with neither an agent nor artificial delay. Twelve Core jars and six AE2 compatibility jars were updated; all twenty-four distribution jars match their checksums and the combined archive.

The machine-readable [final evidence](../../output/ae2-click-investigation/passive-source-fix.json) records exact run IDs, counts, hashes, accepted stale clicks, rejected baseline clicks, distribution promotion and coverage limits. Every failed candidate and test-setup failure remains available in the investigation directory; later passes do not overwrite them.

## Coverage limits

The original stage below the release-qualification update was focused on two
targets. Current build13 checks cover all six reviewed AE2 integration targets
with ordinary and delayed input, plus the six-mapping validator/publication
tests. These remain focused checks. They do not replace the complete AE2 atlas
or certify every mod combination, recipe, network condition or interaction.
