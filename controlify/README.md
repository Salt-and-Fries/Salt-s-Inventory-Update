# Salt's Inventory Controlify

This client mod is the third, independent Salt's Inventory Update artifact. Install
the matching core JAR, Controlify, its YACL dependency, and this JAR. Fabric also
requires Fabric API and Fabric Loader 0.18.2 or later. The existing extended
compatibility JAR is optional.

The [illustrated feature guide](docs/salts-inventory-controlify-feature-guide.pdf)
covers every controller mode and added feature with verified in-game images,
control reference, installation steps and validation scope.

Controller code lives exclusively in `controlify/`. The integration compiles only
against the core's public `com.salts_inventory_update.api` classes. Core runtime
classes and the Controlify/YACL dependencies remain separate mods; none are copied
into the controller JAR.

The matching rebuilt 1.21.1 core supplies generic workspace API hooks for enumerating,
rendering and routing input to existing windows. It contains no controller layouts,
inputs or animations. Older core 0.1.4 archives without these hooks are insufficient;
use the core JAR produced by this branch's build together with the controller JAR.

## Controls

Opening a supported container or UI focuses player inventory on the left and places
the opened window on the right. Player inventory and Character remain available and
cannot be closed individually. Controlify's usual cursor, scrolling, select,
quick-move and take-half actions retain their configured bindings.

| Input | Action |
| --- | --- |
| LB / RB | Select the previous / next window on the right |
| Hold LT | Show other open windows around the focused center window |
| Left stick while holding LT | Highlight the nearest radial window; bottom selects Tuck Away |
| Release LT | Put the highlighted window on the right, or tuck the workspace upward |
| A while holding LT | Swap the highlighted window into focus |
| Configured close-inventory button (normally B), while holding LT | Close the highlighted closable window on release |
| Hold the configured close-inventory button | Close all closable windows using Core's existing hold duration |
| Configured inventory button (normally Y), while tucked | Restore the same windows and sessions |

While either inventory mode is visible, the screen owns character and world input.
Tucking immediately restores normal gameplay. New supported containers join the same
workspace while tucked and remain out of view until inventory is restored. Quick-move
routes to the other displayed window; a UI without storage cannot receive a stack.

`config/salts_inventory_controlify.properties` contains `animations=true` by default.
Set `animations=false` and restart the client to make all presentation transitions
immediate. The controller presentation forces Core's inventory HUD/offhand arrangement
while active without overwriting the keyboard/mouse preference on disk.

The input adapter subclasses Controlify's native container screen processor. Its
API was checked against [the upstream LTS source](https://github.com/isXander/Controlify/tree/fd816773a67cc41d4fa242911eae4cc6e36bca12)
and the actual pinned loader JARs.

## Build and development

Run these commands from the repository root:

```powershell
.\gradlew.bat :controlify:model:test
.\gradlew.bat buildControlify
.\gradlew.bat collectControlifyJars
.\gradlew.bat collectControlifyTestJars
.\gradlew.bat :controlify:1.21.1:fabric:runClient
.\gradlew.bat :controlify:1.21.1:neoforge:runClient
```

`buildControlify` builds both supported loader JARs, tests the controller workspace
model and real core workspace/transfer hooks, and checks source, compiler classpath,
and production bytecode boundaries.
`collectControlifyJars` places verified artifacts in `dist/controlify/`. The normal
repository release task also keeps `dist/core`, `dist/compatibility`, and
`dist/controlify` separate. Development client tasks launch the core and Controlify
as separate mods.
See [validation evidence](VALIDATION.md) for the final test totals, packaged
gameplay scenarios, artifact isolation checks and SHA-256 hashes.

`collectControlifyTestJars` gathers the rebuilt matching core and controller JARs
in separate folders under `dist/controlify-test/`. The public workspace API added
for this integration requires this rebuilt core. For disposable in-world QA,
use the separate test-only initializer and packaged launch profile:

```powershell
.\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:1.21.1:fabric:runPackagedControlifyQa
```

That profile removes project implementation directories from the game's classpath
and loads the production core/controller JARs from its isolated `mods` folder.
The harness compiles in its own source set and is excluded from all release JARs.
It also stages the original published Controlify and YACL JARs, so Fabric loads
their nested HID, SDL, and JSON-parser dependencies. The regular Fabric development
client uses the same upstream staging. Each QA launch clears only its previous
report and PNG screenshots; preserved attempt folders remain available.
The disposable fixture uses a synthetic Controlify driver with real input bindings
and Controlify's supported mixed input mode, which prevents operating-system mouse
events from interrupting the automated controller sequence. Production input
behavior is unchanged; this is not a physical controller hardware test.

`WorkspaceScissorMixin` adapts Minecraft 1.21.1's native clipping while a controller
window is transformed. It maps all four rectangle corners through the current GUI
pose and rounds the clipping bounds outward. A nested render scope limits the
adaptation to controller windows; other screens retain their native clipping.
The pure tests cover transformed bounds, rotation, reflection, fractional edges,
empty clips, nested scopes, and exception cleanup.

The built target is Minecraft **1.21.1**, with **Fabric** and **NeoForge**, using
Controlify **3.0.1+lts**. The pinned upstream Controlify versions are
[Fabric y9bu5RxH](https://modrinth.com/mod/controlify/version/y9bu5RxH) and
[NeoForge RqsNKKLK](https://modrinth.com/mod/controlify/version/RqsNKKLK).

## Other Minecraft versions

Controlify publishes versions for the repository's other Minecraft releases.
These are upstream availability, rather than built targets of this integration:

| Minecraft | Available Controlify loaders | Upstream Controlify |
| --- | --- | --- |
| 1.20.1 | Fabric | [2.1.2](https://modrinth.com/mod/controlify/version/gYWWawgz) |
| 1.21.11 | Fabric, NeoForge | [3.0.1+lts](https://modrinth.com/mod/controlify/version/qZW7FPjm) |
| 26.1.2 | Fabric, NeoForge | [3.5.3+mc26.1](https://modrinth.com/mod/controlify/version/CCtUQSV4) |
| 26.2 | Fabric, NeoForge | [3.5.3+mc26.2](https://modrinth.com/mod/controlify/version/VDw4mGkG) |
| 26.3 | Fabric, NeoForge | [3.5.3+mc26.3](https://modrinth.com/mod/controlify/version/fo76vL20) |

Controlify does not publish a Forge build for 1.20.1. Additional integration
targets require adapting the Minecraft screen/render/input signatures and, for
26.x, the Controlify 3.5 API before claiming support. They are not substituted
with an empty artifact or a binary from a different game version.
