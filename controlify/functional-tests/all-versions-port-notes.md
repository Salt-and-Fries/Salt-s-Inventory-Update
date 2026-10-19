# Controlify port adapters and scope

This port targets the twelve entries pinned in `controlify/targets.json`. The pins
include exact upstream artifact IDs and hashes; they do not promise compatibility
with arbitrary older or future Controlify releases. Validation results belong in
`controlify/VALIDATION.md` and the generated QA reports. This document describes
implementation boundaries and makes no packaged gameplay pass claims.

| Minecraft | Loaders | Pinned controller family | Native adaptations |
| --- | --- | --- | --- |
| 1.20.1 | Fabric, Forge | Controlify 2.0.4 on Fabric; Forgified 2.1.9 on Forge | Native recipe resource IDs, legacy input events, immediate GUI rendering, native 2.x keyboard and glyph guide |
| 1.21.1 | Fabric, NeoForge | Controlify 3.0.1 LTS | Native recipe resource IDs and crafting inputs, legacy input events, immediate GUI rendering, 3.0 keyboard and glyph guide |
| 1.21.11 | Fabric, NeoForge | Controlify 3.0.1 LTS | Native recipe display IDs, modern input events and queued rendering; 3.0 keyboard and glyph guide |
| 26.1.2 | Fabric, NeoForge | Controlify 3.5.3 for 26.1 | Native recipe display IDs, version-specific render submission and contextual guide APIs |
| 26.2 | Fabric, NeoForge | Controlify 3.5.3 for 26.2 | Native recipe display IDs, version-specific render submission and contextual guide APIs |
| 26.3 | Fabric, NeoForge | Controlify 3.5.3 for 26.3 | Reference native integration |

The Forge 1.20.1 target also uses the Mixin Args runtime dependency pinned in the
same manifest. Fabric and Forge/NeoForge each require their matching YACL and
loader dependencies; Fabric additionally requires its matching Fabric API.

## Shared behavior and editable assets

`controlify/common/src/main/java` contains the shared wheel, navigation, scrolling,
filter, layout, transition, guide-layout and sound-policy models. The concrete
Minecraft/Controlify integration stays under `controlify/versions/<version>/`.
Native version differences are translated at these boundaries rather than
replacing unavailable behavior with successful no-op methods.

The editable Salt textures, language files and audio have one canonical source:
`controlify/versions/26.3/common/src/main/resources/assets/salts_inventory_controlify/`.
Each companion's resource processing consumes those assets. User PNG/PDN paths
remain there; editing a wheel texture does not require maintaining twelve copies.
Controlify's own binding defaults and service metadata remain version-specific.

The companion uses Core's public `com.salts_inventory_update.api` surface for
rendering, pointer input, controls, scroll regions, text focus and native recipe
operations. Install the matching newly built Core and companion together. A
previously released Core JAR with the same `0.1.4` filename can lack these methods.
When using Extended, replace it with the matching rebuilt version as well. Core,
Extended, Controlify and YACL remain separate archives; controller presentation
and optional UI sound changes are confined to the companion. Inventory sounds
default to off.

## Native keyboard and rendering differences

Controlify 2.x has no `KeyboardOverlayScreen`. The 1.20.1 adapter hosts its actual
`ChatKeyboardWidget` in a modal screen, routes text to the owning API editor, and
retains the upstream keys, labels, shortcuts and texture. Its native Enter
shortcut is `GUI_ABSTRACT_ACTION_2` (normally Y). That is a keyboard action while
the overlay is open, not an inventory action. QA modal-isolation probes therefore
use the separately configured quick-move input instead of accidentally submitting
the keyboard. The adapter handles the configured Back binding explicitly because
focused 2.x keyboard keys consume native component button handling while the
virtual mouse is disabled.

The 2.0.4 keyboard draws textured vertices using an incompatible untextured GUI
shader. The companion selects Minecraft's matching textured shader only while
its keyboard is active, preserving the original gray key texture. The 3.x ports
use the native keyboard overlay and layouts provided by their pinned release.
All adapters quarantine opening inputs and closing held inputs until neutral.
The 2.x/3.0 adapters preserve the previous native virtual-mouse state. Short-lived
receipts distinguish exact native cursor-warp callbacks during owned keyboard
transitions from real mouse motion; these callbacks still update native hover.
Differing physical motion, buttons and wheel input retain their normal priority.
The 2.x adapter also records exact physical positions around native cursor-mode
changes, whose temporary grab/release can restore a previous cursor position.

The immediate renderers on 1.20.1/1.21.1 need balanced native shader opacity and
scissor transforms. The newer ports use their native queued render-state paths.
Controlify 2.x/3.0 guides use native binding glyphs with Salt's shared compact row
layout because they predate the 3.5 contextual guide API. The native glyph family
and keyboard appearance can consequently differ across pinned upstream versions.

## Menu and integration boundaries

The recipe wheels operate on supported native crafting, character 2x2 crafting,
furnace, smoker and blast-furnace stations, plus the native creative tabs. Recipes
remain native, unlocked recipe-book entries. Crafting and preview requests are
validated and executed by the matching server Core; cooking retains native fuel,
timing and result callbacks. The old recipe-ID protocols and newer recipe-display
protocols are adapted independently instead of sharing incompatible packet types.
Legacy snapshot recovery retains a window only when its authenticated lifetime,
native menu, opening metadata and physical slot topology still match. A state
revision refresh therefore preserves an active recipe preview; a new token,
replacement or changed menu follows the normal reconstruction path.
On 1.20.1 the token is captured from the separate session-authorization map. Four
native regression cases cover public-handle/preview retention, real closure and
new-lifetime rejection, changed slot/opening identities, and metadata/zero-token
rejection. Snapshot identities are cleared on closure, disconnect and dimension
change.

Unknown modded windows can expose controller targets, tabs, virtual entries,
scroll regions and editable bounds/focus through Core's API. This allows the
generic controller behavior to follow custom surfaces without a controller-only
adapter for every mod. Native owners retain item-click authority. Extended's
version-specific integrations provide additional declarations for supported mods;
an arbitrary custom recipe processor still requires an explicit recipe/menu
contract and does not become a crafting station merely by containing slots.

Contextual ingredient pools and destination simulation use each integration's
physical backing owner and index. Aliased views count once, distinct owners
sharing a dummy vanilla container stay distinct, and declared virtual slots cannot
act as real recipe resources or output storage. Individual mod combinations and
versions require their corresponding runtime evidence; compilation and generic
API fixtures alone do not establish every possible modpack combination.
