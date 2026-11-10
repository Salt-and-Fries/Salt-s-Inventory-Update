# Extended AE — available SIU targets

Publisher availability checked November 3, 2026 against the
[official release list](https://modrinth.com/mod/extended-ae/versions).
These are all published Extended AE targets overlapping SIU's supported builds.

| Minecraft | Loader | Extended AE release | UI family | Compatibility jar |
| --- | --- | --- | --- | --- |
| 1.20.1 | Fabric | [1.0.2](https://modrinth.com/mod/extended-ae/version/Wmmw6SrO) | Vintage AE2 15 | [Jar](../../dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar) |
| 1.20.1 | Forge | [1.4.18](https://modrinth.com/mod/extended-ae/version/uq3lO4ER) | Vintage AE2 15 | [Jar](../../dist/compatibility/salts_inventory_compat-1.20.1-Forge-0.1.3.jar) |
| 1.21.1 | NeoForge | [2.2.35](https://modrinth.com/mod/extended-ae/version/bC302UUP) | Modern AE2 19 | [Jar](../../dist/compatibility/salts_inventory_compat-1.21.1-NeoForge-0.1.3.jar) |
| 26.1.2 | NeoForge | [1.0.3](https://modrinth.com/mod/extended-ae/version/KpNzac5y) | Modern AE2 26 | [Jar](../../dist/compatibility/salts_inventory_compat-26.1.2-NeoForge-0.1.3.jar) |

No matching publisher release exists for SIU's other Fabric/NeoForge targets
(1.21.1 Fabric, 1.21.11, 26.1.2 Fabric, 26.2 or 26.3). An upstream development
branch or unpublished tag does not establish release support. SIU has no
1.20.1 NeoForge target.

## Window coverage

The approved [1.20.1 implementation](extended-ae-1.20.1.md) remains in the
Fabric and Forge jars. The modern ports use the installed releases' screen
documents, palettes, slot artwork, sidebars, top-right badges and widgets;
vintage textures are not copied into modern jars.

The modern inventory/configuration views include:

- Extended Pattern Provider, Extended/Oversize Interface, Extended Drive,
  Ingredient Buffer, Caner, Extended IO Port, Crystal Assembler and Circuit Slicer.
- Extended Molecular Assembler and the 1.21.1 Extended Inscriber, including
  their eight/four native job pages respectively.
- Import/export buses, active formation and smart annihilation planes,
  mod/tag/precise/threshold filters and threshold emitter.
- Pattern Modifier's Multiply, Replace, Properties and Clone modes; Renamer;
  Configuration Modifier; Void Cell; Wireless Connector and Wireless Hub.
- Extended Pattern Access Terminal and its wireless counterpart, preserving
  native provider rows, highlighting, search and provider-ID item actions.
- Native amount-entry and output-side selection subviews.
- The three AppliedE variants when the published 1.21.1 integration is present.

There are 33 concrete window screen classes on 1.21.1 including the two subviews
and conditional EMC interface; 26.1.2 has 31. These are class counts, not
one renderer per tier or one screenshot per state. Extended Crafting Terminal's
four 1.20.1 modes are absent from the audited modern releases and are not
invented as new interfaces there.

Read-only crafting, processing, smithing and stonecutting pattern inspectors
retain their native screens. The optional FramedBlocks and Amadron inspectors
on 1.21.1 also remain native.

## Integration details

The original menus, native action maps, item validation and packet codecs still
perform the work. The adapter addresses actions and updates to the authenticated
window lifetime, restores the active menu/cursor scope, retains custom action
slots, and delays initial addon snapshots until the session is registered.

Modern Glodium 26 uses typed byte payloads; 1.21.1 uses its earlier parameter
arrays. The ports use the corresponding native codecs. A bounded client queue
retains actions while SIU awaits acknowledgement; repeated pending text edits
coalesce to their latest complete value. Rename confirmation closes its own
window, and the configuration tool initializes its native host state before the
first GuiSync snapshot. Crystal Assembler and Circuit Slicer load their native
output-side settings before opening, so the initial side selection matches the
machine. Parameterless actions retain the native empty-parameter behavior.

The addon's Pattern Access screens receive native provider snapshots through
the addressed session transport. Rebuilt highlight controls replace their old
registrations, including AE2 26's stricter duplicate-ID handling.

Extended IO Port and Mod Export Bus frames extend to the bottom of their native
upgrade trays. Top-right priority and return tabs retain the original raised
anchor above the frame, including their exposed hit areas. The taller frame
does not expose the removed player inventory.

Replacement slot rims stay complete. Native controls added outside AE2's named
widget collection are retained explicitly. Window movement, pinning, locking,
focus, title controls and the separate Inventory continue through SIU's existing
desktop integration. Core and the public API are unchanged.

## Verification and release evidence

The current jars and frame-polish checks are recorded in
[frame verification](../../output/extended-ae-modern-polish/verification.json).
The earlier [rollout verification](../../output/extended-ae-all-targets/verification.json)
retains its exact historical jars, publisher inputs, captures and focused assertions.
The two modern targets use actual publisher jars in disposable game worlds:
AE2 19.2.18 / Glodium 2.2 and AE2 26.1.13-beta / Glodium 1.2 respectively.
The frame correction was also checked on 1.20.1 Fabric and Forge.

The earlier modern rollout passed 350 focused assertions: 180 on 1.21.1
and 170 on 26.1.2. The runs captured 45/43 views covering 32/31 native screen
classes respectively. Checks include native paging, rapid text edits, Rename
confirmation, Pattern Modifier actions, populated Pattern Access search and
withdrawals, Matrix withdrawals, output-side selection, amount editing,
connected wireless access, slot rims, and an existing AE2 Drive window.

Coverage is limited to the affected menus and transport paths. It is not a
complete functional or gameplay suite. Every machine recipe, multiplayer or
dedicated server, controller input, resource pack, and optional-mod combination
has not been exhaustively tested. Conditional AppliedE layouts on 1.21.1 are
checked structurally against their exact native documents; that optional stack
is not part of the modern live fixture.

The release staging step verifies that Core/API artifacts, unrelated target
jars, and unrelated entries in the distribution bundle remain unchanged.
