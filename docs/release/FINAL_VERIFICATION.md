# Final candidate verification

Updated November 10, 2026. The recorded tests and archives passed their scoped
acceptance gates. Independent review status is recorded in the release manifest.
No commit, tag, push or publication is performed by this qualification task.

## Candidate and artifact identity

Core **0.1.6**, Compatibility **0.1.3** and Controlify **0.1.1** provide 36 jars:
Fabric and Forge for 1.20.1; Fabric and NeoForge for 1.21.1, 1.21.11, 26.1.2,
26.2 and 26.3. Choose one matching target; optional integrations follow the
[exact upstream table](compatibility-pins.md) and [dependencies](UPSTREAM_DEPENDENCIES.md).

The [candidate pointer](../../output/release-readiness/candidate-current.json) identifies an immutable
source archive and the exact 36-jar ledger. Git HEAD alone does not identify
this uncommitted source tree. The [final acceptance receipt](../../output/release-readiness/final-acceptance-1450-01/manifest.json)
retains the compiled candidate identity before the final documentation freeze.
The [durable release manifest](../../output/release-readiness/release-manifest-final.json) records the
reviewed source snapshot, build, checksums, toolchains and original evidence.

All jars came from the normal `gradlew.bat build` tasks. Source parity, public
ABI and examples, optional-integration classpath boundaries, bytecode/mapping
gates, archive members, metadata and bundled checksums passed. The public API
remains version 6 and wire protocol 5; additive subview defaults preserve prior
consumer signatures. [Build](../../output/release-readiness/normal-build16/manifest.json),
[integrity](../../output/release-readiness/candidate-integrity-build16-01.json),
[metadata](../../output/release-readiness/metadata-build16-01.json),
[licenses and artwork provenance](../../output/release-readiness/provenance-build16-01.json).

## Complete campaign and scoped retention

| Complete phase | Accepted / planned |
| --- | ---: |
| core | 12 / 12 |
| compatibility | 66 / 66 |
| toms-pickup | 12 / 12 |
| mouse-tweaks | 12 / 12 |
| raised | 12 / 12 |
| spatial | 12 / 12 |
| controller | 1 / 1 |
| ae2-atlas | 12 / 12 |
| config | 12 / 12 |
| ancient-remnants | 8 / 8 |
| startup | 1 / 1 |
| accessories | 8 / 8 |
| dedicated-server | 58 / 58 |

All 65 publisher profiles are accounted for: 37 retained-explicit-reviewed-repair-chain, 28 retained-original-scope-and-explicit-reviewed-repair-chain. Original runtime jars,
QA sources and failed attempts retain their measured identities. These counts
do not represent a new execution of every suite after every small correction.
The [final transition](../../output/release-readiness/session-registration-evidence-build16-01.json)
independently checks the exact source/compiled deltas and required new native
checks before permitting unaffected older results to be retained. The
[dashboard](../../output/release-readiness/report.md) and [machine-readable qualification](../../output/release-readiness/qualification-current.json)
show the original and final subjects, rather than relabelling old executions.

The [full-phase QA history](../../output/release-readiness/final-phase-QA-history-1713-01/manifest.json)
retains the original 1,144-input source archive, two independently reviewed
helper revisions and fourteen explicitly listed later tools. Those later tools
are not retroactively attributed to old executions. The original native,
derived-input, runtime-copy and archive predicates remain enforced; three
[focused reader tests](../../output/release-readiness/full-phase-history-tests-1714-01/manifest.json)
also reject unknown changes and check restoration of live source readers.
The two named Powah sealing-context sidecars remain support records rather than
additional native executions. [Focused schema checks](../../output/release-readiness/focused-archive-selection-tests-1722-01/manifest.json)
reject unknown schemas and mixed subject identities. The accepted coverage's
[dashboard-only source alias](../../output/release-readiness/final-dashboard-schema-1724-01/coverage-source-only-alias.json)
preserves all 399 rows and their original evidence without replaying gameplay.

Build16 removes one duplicate replacement-session insertion in each of the six shared Core sources. All12 Core archive deltas were independently checked; the24 Compatibility/Controlify jars remain byte-identical to build15. The actual NeoForge cargo regression passed all six checks and ten observations with unique client sessions. The previous build failures and failed native attempt remain preserved.

The mapped JUnit archive contains **2007** tests; its scope is
separate from native gameplay. The original 174-job specialized native campaign
passed. Later corrections and missing publisher pins have named focused runs.
The 88 formerly unlinked native UI aliases are reconciled from 24 original focused native runs and six original Curios target runs; those aliases add no gameplay executions. The final CPU proof contains ten real executions: nine genuine job/control
workflows and one idle selector workflow. CPU cancellation is distinct from
completed-output crafting, which has its own separate native proof.

## Final focused gates

| Gate | Scope |
| --- | --- |
| Core detailed-render logger | Twelve build15 native smokes, retained through the exact build16 registration correction |
| Enchanting book | Both 1.21.11 loaders on build15; native resource/render and reload retained through the exact correction |
| Powah | Two modern native runs added to two retained legacy runs; four executions cover thirteen historical UI aliases |
| REI | Two native browser/workspace runs; twelve exact adapter/policy archive deltas, an additive public browser-screen marker and controller declarations, and 84 pinned-API checks |
| Installation | 26.3 Fabric client-only 22, server-only 18, initial obsolete-protocol 16 checks |
| Small Ships | Three build15 native runs plus the new build16 1.21.1 NeoForge run; six checks each. Page replacement registers one session, and modern cursor ownership stays canonical |
| GUI boundaries | Twelve Core targets at GUI 1/2/3 and resolution changes |
| Additional AE2 patch pins | 1.20.1 Fabric and Forge 15.4.12 genuine CPU controls, preserving the earlier 15.4.11 runs |
| Additional legacy publisher workflows | AE2 15.4.11 stock, processing and crafting; Supplementaries 3.1.43 Red Merchant. Four executions, 73 native assertions; no repeat of completed full suites |
| Accessories publisher selection | Three historical aliases select the advertised beta.54 actually loaded in their original sealed runs. One additional Fabric bundled-beta.48 workflow passed 19 checks; NeoForge beta.48 startup failed with and without SIU and is not a functional pass |

See the [final focused queue](../../output/release-readiness/serial-final-Core-and-REI-corrections-221-01/manifest.json),
[CPU exact-publisher checks](../../output/release-readiness/CPU-exact15412-native-1479-01/manifest.json),
[minimum GUI evidence](../../output/release-readiness/final-minimum-gui-543-01/manifest.json), and
[installation evidence](INSTALLATION_ACCEPTANCE.md). GUI wall tests qualify
Core geometry; they do not claim every third-party sprite at every resolution.
Linked panels, native controls, live ingredients, power, output, refusal,
cancellation and persistence retain each feature proof's explicit limits.

## Coverage dispositions and known limits

| Disposition | Historical target/case records |
| --- | ---: |
| functional-pass | 367 |
| native-only-qualified | 1 |
| read-only-status-pass | 31 |

The [failure/follow-up index](../../output/release-readiness/failure-index-final.json) retains named historical attempts and qualified successors without inventing root causes.

The [Accessories publisher-scope receipt](../../output/release-readiness/accessories-publisher-scope-1706-01/manifest.json)
preserves the full historical standalone/nested class catalogue. Class presence
alone does not qualify every bundled version. The advertised beta.54 owner is
verified against exact upstream hashes, native staging, loader logs and sealed
equipment workflows. The supplemental NeoForge beta.48 renderer failure also
occurs [without SIU](../../output/release-readiness/accessories-beta48-native-control-1705-01/manifest.json)
on the same named loader/fixture. It is an upstream/runtime limitation, not an
SIU fix or a beta.48 functional pass. The earlier control used the wrong loader
and remains recorded as an inconclusive setup attempt.

There are no unresolved release-blocking SIU entries in the authoritative
[backlog](../../output/release-readiness/coverage-backlog.json). Historical rows are not necessarily
distinct windows or additional executions. Read-only/native acceptance,
unavailable pinned releases and documented upstream incompatibilities are
kept separate from functional inventory acceptance.

The six deliberately native Project Everything mods retain their own screens.
Three sealed ownership/input runs cover sixteen screens and 89 assertions:
1.20.1 Fabric (Things, DungeonZ, Sortilege, Wraith Waystones), 1.20.1 Forge
(ElevatorMod) and 1.21.1 NeoForge (Armor Statues). This does not certify unrelated
world generation, every teleport condition or other upstream releases.

Separate-process multiplayer is representative **26.3 Fabric**, not an
all-loader or WAN guarantee. Its 42 checks passed with isolated Java
**25.0.5.0.3**, normal compilation and `vm_arguments=[]`. Earlier native JVM
crashes and the older C1 diagnostic run remain preserved; no root cause for
those crashes is claimed. [Current remote receipt](../../output/release-readiness/readiness-build14-multiplayer-java25-update-01/manifest.json).

The installation guide and release notes retain exact known upstream limits:
Charm/Aether and Bumblezone/Polymer incompatible partitions, Small Ships
config-unload errors, Raised resource metadata, native missing-honey refusal,
Rocket Sensor lifetime and affected Tech Reborn recipe serializers. None is
represented as an SIU fix. Deferred Create addons, unknown publisher patches,
arbitrary modpacks, WAN conditions and untested hardware remain outside this
qualification. No automated campaign can guarantee every possible modpack.
