# Automatic native container windows (Core 0.1.6)

Core can host a conventional modded container's existing screen in an SIU window on **all twelve supported Minecraft/loader targets**. The screen supplies its own textures, progress indicators, animations, controls and real slot positions. Core supplies the title/control bar, blank bottom bar, desktop placement and shared inventory interactions. No texture extraction, copied mod artwork, container-specific renderer or per-container public API registration is needed.

This implements the bounded native-screen approach investigated in `output/pdf/siu-dynamic-native-ui-research.pdf`. Its default selection is limited to non-API automatic windows. Integrations can explicitly opt into the same native base through the additive [native template API](native-window-templates-1.20.1.md). Ordinary API definitions retain their original behavior. The historical filename is retained for existing documentation links.

## API isolation

1. The existing public API registry is consulted first, with the same lookup context as before.
2. A registered server window handler prevents automatic server admission. A matching client definition also takes precedence.
3. Negotiated automatic sessions and explicitly opted-in native templates may construct the native host. Ordinary API definitions retain their original rendering/input path.
4. Native rendering mixins check the exact owned screen instance in that scope before changing anything. Background, slot, cursor, tooltip and recipe-book interception is inactive elsewhere.
5. Existing public API signatures and API version 6 remain intact. The later native-template extension adds opt-in contracts without converting existing definitions. Compatibility continues to compile against the API-only artifact. Controlify source is unchanged.

## Admission and layout

The client advertises modded menu types that have native screen factories. The server considers actual slot ownership and geometry: non-player content must occupy an upper region above the player's inventory. Vanilla menu types continue using SIU's existing implementations. Extended menu factories and opening-data writers are bridged by their structural method signatures; Forge's extra-data opening path adopts an already-constructed eligible menu. API menus continue on their original Forge opening path.

The client retains the original native screen, initializes it, and infers a crop from machine slots, player slots, image dimensions, the inventory-label boundary and upper widget bounds. Unconfigured automatic windows retain the full lateral extent of the native panel, including its side artwork. A 20-pixel SIU title/control bar and a 16-pixel blank bottom bar cap that live native slice; each bar overhangs it by 6 GUI pixels per side. No SIU side frame or gray backing is painted around the native body. The native background plate is clipped vertically to exclude the original title and lower inventory artwork. Small outboard native decorations can expand the region; the region contracts when those decorations disappear.

This presentation is internal to `AutomaticNativeWindow`. `NativeTemplateWindow` explicitly opts out: its existing inferred crop, author-supplied surface, insets, decoration, sizing and region mapping remain unchanged. Public API signatures and defaults are unchanged. The [approved design proposal](../../output/pdf/siu-automatic-window-bars-design.pdf) documents the intended appearance; the [implemented screenshot report](../../output/pdf/siu-automatic-window-bars-implemented.pdf) records the focused runtime examples and limits.

Real native slot IDs and slot objects stay intact. Slots are not reordered, deleted, copied into fake inventories or rebuilt as generic grids. Native item restrictions, result callbacks and menu data remain authoritative. The separate SIU player inventory exposes the lower inventory without drawing it twice.

## Interaction and lifetime

- Mouse coordinates are transformed back into the retained screen's coordinate system. Native widgets receive click/release/drag/scroll and focused text input.
- For the unconfigured fallback, clicks and scrolling on the blank bars or side overhangs do not reach cropped-out native controls. A drag begun in the visible native body still receives its release. API templates retain their existing input routing.
- Real slot interactions use Core's existing authenticated desktop transactions and shared cursor. Vanilla menu-button requests from a hosted widget are routed to that widget's owning session.
- Native tooltips are deferred to the desktop's tooltip pass so the content crop does not cut them off.
- Screens implementing Minecraft's recipe-book listener provide their original book to SIU's attached recipe panel. Standard recipe buttons toggle that panel without shifting the native machine coordinates.
- Resize, tick, pointer cancellation, focus loss and removal are delivered to the retained screen. Native Forge extended-menu opening/closing events are paired for adopted menus.
- Native `stillValid` remains required. Detectable block-entity sources also close when removed, replaced, unloaded or out of reach. Detectable backing ItemStack sources are locked while open and close when their inventory source is replaced.
- Homogeneous repeated storage grids without machine data can expose SIU sorting. Isolated bait/filter/result cells are excluded from the selected group. Existing slot permissions, transaction validation and explicit API storage roles retain precedence.

## Counts and protocol compatibility

On **1.20.1**, the ordinary legacy item encoding cannot represent counts above 127. A separately negotiated envelope preserves full counts in automatic-window snapshots and shared-cursor updates, including transfers to another desktop window. This is used only between peers that explicitly acknowledge the feature. Later targets use Minecraft's full-integer ItemStack stream codec directly. Existing authentication, session tokens, state checks and mutation validation still apply. The normal API packet encoding and shared protocol version are unchanged.

Automatic hosting has its own optional readiness handshake on every target. A peer that does not acknowledge it retains the ordinary API and native-screen paths.

## Version-specific bridges

- 1.20.1 and 1.21.1 capture native artwork through an isolated framebuffer, restoring the caller's framebuffer and render state.
- 1.21.11 and 26.x extract the native GUI once into an isolated render-state tree, then replay clipped fragments with translated geometry, text and preview items. They do not reuse the legacy OpenGL framebuffer implementation.
- Modern Fabric extended factories serialize their typed opening data with the menu's own stream codec. NeoForge's extra-data opener retains its original writer. These bridges do not embed upstream mods in Core.
- A native menu that refuses desktop synchronization stays on its original screen. An optional integration can use `DesktopSynchronization.isDesktopSynchronizer` to route authenticated desktop synchronizers through the upstream mod's own setter. The extension never imports Core implementation types.

## Conservative fallback

If native menu reconstruction or layout inference fails, the client declines that authorized automatic session. The server verifies its nonce, token and source, returns the shared cursor to inventory, closes detached sessions and reopens the original native interface through its normal loader path. The declined menu type remains native for that connection. Explicit user-forced windows retain their existing override semantics.

## Supported shape and limits

This is a reusable host for conventional upper-container/lower-player layouts, rather than a universal interpreter of arbitrary UI code. A UI with interleaved player and machine regions, independent tabs, virtual inventories, a non-container screen, unusual framebuffer/scissor rendering, or custom packets that assume `player.containerMenu` is the active native menu can still require a public API integration. Geometry inference cannot determine every possible custom drawing or packet's meaning.

Unconfigured automatic panels retain their own layout without invented grid reflow or scrollbars. Use a suitable GUI scale or an integration when that native layout cannot fit. Opt-in native templates provide a bounded scrolling canvas and explicit multi-region composition. Ordinary API windows keep their author-defined sizing, viewport policy and scrolling behavior.

The focused implementation and gameplay record is in [automatic-native-containers-validation.md](automatic-native-containers-validation.md).
