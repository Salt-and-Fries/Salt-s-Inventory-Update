# Installation and remote-session evidence

The final candidate's measured tests are recorded in [FINAL_VERIFICATION.md](FINAL_VERIFICATION.md).
Independent review status is tracked in the release manifest. These are representative 26.3 Fabric
installation and cross-process checks, not an all-loader multiplayer guarantee.

| Installation | Expected behavior | Accepted evidence |
| --- | --- | --- |
| Matching modules on both sides | Negotiated desktop sessions and authoritative shared storage/cursor edits | [42 checks, normal Java compilation](../../output/release-readiness/readiness-build14-multiplayer-java25-update-01/manifest.json) |
| Client only | Native storage/menu use with an unmodified server | [22 final-jar checks](../../output/release-readiness/readiness-final-installation-client-only-01/manifest.json) |
| Server only | Native storage/menu use from an unmodified client | [18 final-jar checks](../../output/release-readiness/readiness-final-installation-server-only-02/manifest.json) |
| Initial obsolete protocol | Refusal without storage mutation | [16 final-jar checks](../../output/release-readiness/readiness-final-installation-protocol-mismatch-02/manifest.json) |

The [final installation campaign](../../output/release-readiness/final-installation-campaign-01.json)
archives its exact subjects, original 22/18/16 assertions, native pointer input,
observer copies, launch settings and shutdown results. One-sided checks establish
native fallback; full desktop features still require matching modules on both
sides. The mismatch peer sends a valid initial hello with obsolete wire version
4, rather than installing an older published Core or sending a second hello.

The remote campaign uses an official dedicated server, two separate player
JVMs and a reconnect JVM, read-only RCON/NBT ledgers and no server QA mod. It
checks simultaneous withdrawals and furnace output, sorting, complete item
components/counts, owner/session/connection isolation, foreign credentials,
source invalidation and carried items across disconnect/reconnect. Vanilla
26.3 drops the disconnected carried stack as a world item; the test verifies
its saved complete data and ordinary recovery. A pending-mutation disconnect
branch and additional loader/WAN combinations are not claimed.

## Runtime history and current compiler settings

The current 42-check run passed on isolated Oracle Java **25.0.5.0.3** with
`vm_arguments=[]`, using normal compilation. System Java and user launchers
were not changed. The exact [isolated toolchain](../../output/release-readiness/isolated-java25-update-01.json)
is independently hashed. The [earlier accepted diagnostic run](../../output/release-readiness/readiness-multiplayer-modern-13/manifest.json)
used Java 26 with `-XX:TieredStopAtLevel=1`; it is retained as history, not the
current normal-compilation pass or a recommended user JVM option.

Default-compilation attempts 07, 08, 11 and 14 intermittently exited with native
`jvm.dll` access violations, including a client without SIU before connection.
The [Windows error record](../../output/release-readiness/readiness-multiplayer-modern-07/native-jvm-application-error.json)
remains intact. A later successful run does not establish their root cause.
The first final server-only installation attempt also stopped during client resource startup with a native `jvm.dll` access violation on isolated Java 25.0.5.0.3, with no SIU modules installed in that client and normal compiler arguments. Its [failed manifest](../../output/release-readiness/readiness-final-installation-server-only-01/manifest.json) and [Windows event](../../output/release-readiness/readiness-final-installation-server-only-01/native-jvm-application-error.json) remain preserved. The named [server-only follow-up](../../output/release-readiness/readiness-final-installation-server-only-02/manifest.json) retains the original eighteen checks. The initial [protocol-mismatch attempt](../../output/release-readiness/readiness-final-installation-protocol-mismatch-01/manifest.json) also ended with the same native startup exit before its protocol input; its named [follow-up](../../output/release-readiness/readiness-final-installation-protocol-mismatch-02/manifest.json) retains the original sixteen checks. A later pass establishes that scoped installation result, not a JVM root cause or an SIU repair.

Final artifact retention is bound by the [strict transition](../../output/release-readiness/session-registration-evidence-build16-01.json).

## Native publisher diagnostics

The completed normal-build14 Mouse Tweaks and Raised/HUD phases each pass all
twelve targets. [Mouse Tweaks runtime preservation](../../output/release-readiness/mouse-tweaks-runtime-preservation-157-01/manifest.json)
and [Raised/HUD runtime preservation](../../output/release-readiness/raised-runtime-preservation-158-01/manifest.json)
retain their original reports, runtime subjects and logs, with 96 Raised/HUD
framebuffers. Shared QA binaries copied afterward are labelled as post-run
observations, rather than invented launch-time measurements. These are input
interoperability results on build14, separately scoped from the final focused feature, server and artifact qualification.

The [24-log input diagnostic review](../../output/release-readiness/completed-input-diagnostic-review-161-01/review.json)
examines 46 SIU-named warning groups and twelve native error groups. It retains
the audited missing-refmap declarations, disposable Forge defaulting, native
development-path diagnostics and one measured 1.219-second SIU deferred startup
task. That duration is an observation, not a controlled performance benchmark.
The SIU-named caller lines are ordinary INFO/DEBUG registration or QA messages.
Other native warnings remain available without a blanket harmlessness claim.

Raised **6.0.2 on 1.21.11 Fabric and NeoForge** ships a `pack.mcmeta` containing
`supported_formats` with pack format 75. Minecraft reports that exact field as
deprecated and rejects the upstream pack metadata. The review preserves both
publisher hashes and their original metadata bytes. The HUD assertions pass;
this does not establish a clean load of Raised's resource pack. This is an
exact-jar/log correlation, with no new SIU-absent control or publisher patch.

All 65 accepted original publisher logs have now been extracted with their
exact subjects and original fresh/retained labels. The
[scoped SIU diagnostic review](../../output/release-readiness/all-accepted-SIU-diagnostic-review-143-01/review.json)
classifies 316 SIU-name/message warning groups: 256 declared non-Forge missing
refmaps, 22 explicit opt-in built-in-QA notices, 16 detailed-render format
warnings covered by the final compiled Core correction, 12 nullable native ship-menu
provider declines, and 10 Bumblezone missing-honey warnings. This is a review
of those exact messages, not acceptance of every native diagnostic or any
unexecuted gameplay. In particular a passed Brigg workflow does not certify
Galley interaction. The other native diagnostics remain in their original logs.

The [subsequent exact ship-provider correlation](../../output/release-readiness/ship-provider-diagnostic-correlation-149-01/classification.json)
resolves those twelve null-menu messages using all four pinned native binaries
and the matching original cargo cases. Each native entity provider opens the
real menu through its platform provider, then deliberately returns `null`.
The records pair seven Brigg and five independent Galley entries with their own
passing original checks. Galley's case ID is `ag-entity-galley`, which the first
all-log review did not associate. The earlier four-message/two-publisher review
is preserved too. This explains those exact warnings without suppressing them,
inventing new executions or certifying other ship features.

The [complete-log caller review](../../output/release-readiness/all-accepted-native-caller-review-146-01/review.json)
also scans every original log beyond the initial excerpt limit. The 45
SIU-named callee frames belong to nine native recipe/pack-load diagnostics
reached through the private QA world-creation callback and its tick adapter;
they are not additional SIU menu-operation exceptions. This is scoped caller
inspection, not a claim that every external recipe or asset is valid.

Small Ships **2.0.2 on 1.21.1 NeoForge** logs a real
`smallships-server.toml` config-unload exception after the current profile's
menu checks complete. The same exact native handler signature and publisher
SHA-256 occur in the recorded SIU-absent baseline, revalidated by that caller
review. The passing assertions and outer process exit do not erase this
shutdown exception. It remains an upstream limitation, with no publisher patch
or claim of a new controlled A/B execution.

The [six-profile Bumblezone correlation](../../output/release-readiness/all-accepted-bee-refusal-classification-142-01/classification.json)
binds all ten warnings to six recorded ordinary heal clicks with no honey,
unchanged bee/player/cursor state, and the same exact publisher handler's
unguarded failed inventory search. Each original care workflow preserves its
21 passing assertions and later successful supplied healing. This attribution
uses archived observations and exact native bytecode; it does not create six
new throw captures, SIU-absent controls or additional gameplay executions.

An unchanged 1.20.1 Fabric Bumblezone 7.15.2 care workflow passed its 21 original
checks, including the missing-honey refusal. Read-only JFR captured that native
handler calling vanilla inventory lookup with index -1; SIU's button dispatcher
appears below the upstream throw. The native receiver lacks a guard for the
missing-resource lookup. [Actual throw and exact subjects](../../output/release-readiness/readiness-legacy-bee-native-exception-01/manifest.json).
This classifies the recorded warning; it is not a SIU fix, an additional
normal-timing run, or an all-loader gameplay certification. No publisher code
was patched and no warning was hidden.

The 26.1.2 NeoForge profile with Bumblezone 7.18.5 also passed all 21 native
care checks, within an 88-case/2,324-check profile that exited successfully.
This does not qualify that profile's entry-only cases. Its missing-honey warning
was correlated with the recorded ordinary heal click, zero honey, unchanged
full bee and player-inventory data, and successful healing after supplies were
provided. The exact native handler reads the inventory search result without
checking for `-1`; the classpath inventory implementation returns `-1` for a
missing item. This attribution uses recorded states and bytecode, rather than
a new throw stack or SIU-absent run on this target. The [profile-specific
classification](../../output/release-readiness/bee7185-missing-honey-final-classification-03/classification.json)
preserves both logged lines and their evidence. It does not exempt similar
exceptions in other actions or profiles, and adds no execution or assertion
counts. No native code, fixture or SIU behavior was changed for this diagnosis.

A separate raw SIU-absent 26.3 Fabric startup control could not reach world
readiness: an installed Alloyed recipe referenced Create's absent mechanical
crafting serializer. The Compatibility jar normally supplies a conditional
recipe resource for that optional combination; removing SIU also removes that
resource. The [failed control](../../output/release-readiness/readiness-publisher-263-fabric-native-startup-01/manifest.json)
therefore establishes neither a successful native startup nor a cause for the
intermittent native JVM access violation. Both original failures remain kept.


## Accessories bundled-version scope

The advertised 1.21.1 Fabric and NeoForge Accessories **1.1.0-beta.54** pins
passed their named equipment workflows. A historical class catalogue also
contains older standalone and Aether-bundled copies. Those copies do not inherit
functional acceptance just because they contain the same screen classes.

The additional exact Aether/Fabric bundled-beta.48 check passed 19 assertions.
The NeoForge beta.48 fixture failed during Accessories renderer-mixin startup on
NeoForge 21.1.252. The [same failure without SIU](../../output/release-readiness/accessories-beta48-native-control-1705-01/manifest.json)
is preserved alongside the SIU attempt. This is a named upstream/runtime
limitation, not a claim about all beta.48 installations. It is not an SIU fix or
a functional pass. The [scope receipt](../../output/release-readiness/accessories-publisher-scope-1706-01/manifest.json)
links exact pins, loader logs, native subjects and the separate Fabric pass.
The initial native-only control selected NeoForge 21.1.229, below Quark's
requirement; that control is inconclusive and is not the no-SIU reproduction.
