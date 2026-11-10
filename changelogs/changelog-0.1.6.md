# Salt's Inventory Update — Core 0.1.6

Candidate verification, independent review and publication status are recorded in [RELEASE_READINESS.md](../RELEASE_READINESS.md) and the [release manifest](../output/release-readiness/release-manifest-final.json).

- Publishes native slot/cursor changes before retaining their immutable revision,
  reserves revisions for cursor-only commits, and applies authenticated cursor
  revisions on modern clients. Portable-host power updates no longer invalidate
  otherwise unchanged input; editable slots, cursor contents, topology, expiry
  and session credentials still receive strict validation.
- Coalesces complete modern field edits and publishes inventory changes
  around the last acknowledged player revision. Native typing and subsequent
  clicks preserve their order without relaxing stale-input rejection.
- Adds public built-in subview, embedded-menu lifetime, hit-routing and control
  policy contracts with backward-compatible defaults. Existing exported API
  signatures are preserved. Optional integrations use these contracts in place
  of accessing Core implementation classes.
- Preserves equipment visibility controls and the recipe guide in Character
  replacements, with distinct controller targets for embedded equipment and
  adjacent native controls.

- Sorting checks pickup restrictions on occupied slots, allowing valid empty
  native inputs to receive sorted items.
- Clicking a native title-area slot no longer starts a window drag.
- Retains provider-owned NeoForge menu opening data while preserving the
  priority of explicitly captured and registered SIU serializers. Native
  fallback opening keeps its original data writer.
- Matches the optional modern ServerPlayer opening hook to its exact native
  method overload, avoiding startup failures on affected releases.

- Skips empty clipped native fragments on 26.2/26.3 Fabric/NeoForge, preventing
  zero-size scissor crashes when rendering native help text and attachments.
  The public API is unchanged.

- Fixes darkened native windows and dark rectangles around transparent tabs on Minecraft 1.21.1.
- Compatible conventional mod containers can open automatically as desktop windows, retaining their native artwork, animations, controls and real inventory slots.
- Automatic windows now use an SIU title-and-controls bar above the original mod artwork and a blank bar below it, preserving native side borders and colors. Manually designed and API windows retain their existing appearance.
- Added public native-window templates for adapting selected parts of an existing interface. Integrations can move or hide sections, add their own controls, keep attached panels outside the frame, and choose standard slot backgrounds.
- Added compact native-window sizing and scrollbars when content cannot fit on screen.
- Added an in-game native-interface inspector and reloadable local layout profiles through `/saltsinventory nativeui`.
- Native windows share desktop inventory interactions, tooltips, recipe-book handling and source lifetime checks.
- Fixes native windows losing a picked-up item when the original menu broadcasts during the click, and preserves Forge 1.20.1 extended-menu opening data.
- Native recipe books fit beside their desktop window and exterior controls, with reliable single-click toggling after reopening.
- Keeps extended-menu opening data and custom native-menu synchronization working with newer Fabric releases.
- Retains the separate native background pass used by newer Minecraft screens, including native slot artwork when Compatibility is absent.
- Saved ordinary container sizes now belong to their menu type, preventing unrelated containers at the same location from inheriting incorrect dimensions.
- These additions are available across all supported Minecraft versions and loaders. Existing API window definitions retain their priority.

Core works independently. Use matching Minecraft and loader builds when installing the optional Compatibility 0.1.3 or Controlify 0.1.1 companions. Fabric requires Fabric API.

Use these same notes for every Core 0.1.6 artifact.
