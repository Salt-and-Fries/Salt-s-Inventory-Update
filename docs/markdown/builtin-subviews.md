# Built-in subviews and management policies

The release-readiness boundary cleanup needs three small, additive contracts.
Existing consumers retain their methods, defaults, packet formats and ownership.

1. `DesktopBuiltinAttachment` may replace a built-in window's content, expose
   relocated native player-slot bounds and guide-button bounds, and return a
   slot hit belonging to an already authenticated embedded menu session. Core
   continues to own hit validation, gestures, the frame, recipe guide and effects.
2. `DesktopEmbeddedSession` / `DesktopEmbeddedSessionHandler` bind a registered
   menu to an existing built-in view without creating a second window. The
   handle exposes only the native menu, session ID, liveness, authenticated
   payload sending and a normal close request. Core retains synchronization,
   source ownership, transition validation and exactly-once disposal callbacks.
3. Definitions may filter individual management controls. A registered geometry
   reset policy may preserve a joined pair during lock operations, including a
   built-in Inventory parent. Neither hook grants additional server permissions.

Registrations participate in integration rollback. Content replacement is opt-in;
the default attachment and definition behavior is unchanged. Returned cross-menu
hits must resolve to a live, registered embedded session and a real native slot.
No compatibility adapter receives a private Core window or session implementation.

The initial consumers are Aether's existing Character subview, Immersive Aircraft's
parent-owned cargo wings, and Quark's Inventory/backpack pair. Traveler's Backpack
uses Core's existing registered source-lock lifecycle without a private lease call.

Implementation acceptance requires source and bytecode boundaries, API consumer
compilation, focused contract/lifecycle tests and live regressions for those
existing presentations before the release checklist can mark this work complete.
