# Extended AE windows — Minecraft 1.20.1

The approved Extended AE designs are implemented through the existing AE2
native-window integration. The union covers 31 inventory/configuration screen
types and the 40 approved design views. Fabric 1.0.2 supplies 25 types including
its optional AE2WTLib screen; Forge 1.4.18 supplies 31 including AE2WTLib and
AppliedE variants. No newer Minecraft port is included in this change.

The original machinery, slots, ghost patterns, filters, amount editor and
native actions remain in charge. Player inventory is separate. Pattern
Modifier gets a separate title/mode band and complete Replace wells. Its two
18 × 18 Replace wells were compared against the publisher atlas pixel for pixel
in the live screenshot. Crafting-terminal mode tabs remain outside the narrowed
native body; upgrade/toolbox trays and native top-right badges stay attached.

The four read-only pattern inspectors keep their native screens.

## Native behavior

- Version-local AE2 style documents retain the original slot semantics and
  resource-pack artwork. Directly-created controls receive stable desktop
  registrations, preserving their native callbacks when windows move.
- Extended AE/Glodium actions use their original codecs and handlers, addressed
  to a live player/menu session. Original AE2 actions from addon screens use
  the existing scoped transport. Crafting result and Matrix slots dispatch
  through their native action overrides rather than vanilla pickup handling.
- Native Pattern Access full/incremental/clear updates go to the independent
  addon content instance. Matrix updates and the native generic responses are
  delivered without installing an active full-screen screen.
- The public API and Core are unchanged. Optional hooks cover both upstream
  package names; normal non-desktop native transport is preserved.

## Focused verification

Built both Compatibility jars and opened every audited screen type, including
plain/AE2WTLib wireless screens and AppliedE's EMC variants. Live checks covered
crafting output pickup, Matrix pattern withdrawal, Pattern Access withdrawal
and reinsertion, amount-editor navigation, machine page changes, modifier
multiplication/clearing/modes, text filters/renaming, window movement/close,
four crafting modes with external tabs and four native inspectors.

This is focused verification of the affected menus, not a complete gameplay
suite. Every recipe, multiplayer/dedicated servers, controller input, resource
packs and arbitrary optional-mod combinations are not exhaustive.

Evidence and build lineage: [verification](../../output/extended-ae-implementation/verification.json),
[in-game screenshots](../../output/extended-ae-implementation/README.md).
