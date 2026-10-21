# Core and compatibility separation

## Architecture audit

This section records the starting architecture at the split. The [current compatibility audit](compatibility-coverage.md) covers subsequent additions and every supported loader/version target.

The starting project uses Prism 0.5.17 with six Minecraft targets (1.20.1,
1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3). Each target has Fabric and
Forge/NeoForge builds. The existing loader modules remain the core modules;
separate compatibility modules must preserve this matrix and Prism's production
remapping rather than replacing the multi-version build.

The audit found these integrations:

| Integration | Existing responsibility | Boundary work required |
| --- | --- | --- |
| Tom's Simple Storage | Eight adapted menus, search, virtual storage entries, filters, links, actions and bounded snapshots | Remove hardcoded bootstrap, diagnostics and terminal quick-move from core; expose generic native quick-move policy |
| JEI | Runtime recipe access, ingredient rendering, lookup, transfer and bookmarks | Make the shared browser contract public and provider-neutral; move the JEI plugin and implementation |
| REI | Recipe access, rendering, lookup, transfer and bookmarks | Move plugins and runtime implementation |
| EMI (1.21.1) | Recipe access, rendering, lookup, transfer and bootstrap | Move plugins and guarded bootstrap |
| Mod Menu (Fabric) | Core configuration screen and links | Add a public configuration-screen factory and move the plugin |
| Sophisticated Core, Backpacks and Storage (Forge/NeoForge through 26.2) | Hosted native screens, upgrade tabs, payload interception, settings transitions, opening data and backing-item protection | Replace direct session, synchronizer and item-lock implementation access with supported APIs; move conditional mixins and lifecycle initialization |
| Applied Energistics 2 (added after the split) | Native desktop views, detached menu transport, toolbox companions and CPU state | Move only fork-added integration code into the extension; preserve installed AE2 implementation and use the public API |

Shared browser models and the no-provider fallback belong to core as a reusable
public contract. Third-party runtime access belongs to the extension. Inventory
expansion, desktop networking/security, vanilla menus, persistence, rendering,
configuration and discovery of external API consumers remain core responsibilities.

The original compatibility packages imported core implementation classes for
logging, loader lifecycle, protocol limits, packets, session management, mutation
readiness, legacy input and rendering wrappers. Compile-time API isolation and
production bytecode checks will enforce the new boundary, including fully
qualified references and accidental packaged core classes.

## Core input interoperability

Core 0.1.5 also owns optional Mouse Tweaks input interoperability. This bridges the desktop screen and its existing public-API slot geometry to Mouse Tweaks' published custom-screen interface; it does not provide a third-party inventory UI. Core + Mouse Tweaks works without Compatibility. Optional mixins are selected by API bytecode presence, and upstream classes are compile-only dependencies. Neither Mouse Tweaks nor Compatibility is bundled or required by Core. The compatibility providers and their API-only dependency direction remain unchanged.

## Module and packaging contract

Core keeps the existing `versions/<Minecraft>/<loader>` modules and identifiers.
The extension lives under `compatibility/versions/<Minecraft>/<loader>` and has
its own entrypoints, metadata and `salts_inventory_compat` identifier. Keeping
the existing core paths avoids disrupting version tooling and API source links.
Both mods support the same twelve loader/version pairs.

See the [executed validation report](jar-separation-validation.md) for artifact
paths, automated test totals, actual runtime scenarios and remaining gameplay
coverage limits.

The root `./gradlew build` or `.\gradlew.bat build` creates all production JARs:

```text
dist/core/salts_inventory_update-<Minecraft>-<Loader>-0.1.5.jar
dist/compatibility/salts_inventory_compat-<Minecraft>-<Loader>-0.1.2.jar
```

Each directory contains twelve JARs and SHA-256 checksums. Compatibility has its
own release version, configured by `compatibilityVersion` in `gradle.properties`.
Compatibility 0.1.2 and the separate Controlify companion 0.1.1 require Core
0.1.5 on every supported target, always for the same Minecraft version and
loader. The `coreVersion1_20_1` property allows a separate legacy hotfix when
needed; it currently matches the common release. Metadata and release checks use the corresponding target's
version independently of the extension version. Optional target mods are not embedded or required to be
installed. The extension's production task performs the same remapping as core. API development JARs live
under a core target's `build/api` and never enter distribution folders.

`gradle/compatibility.gradle` compiles each extension target against an API-only
JAR built by the corresponding core target. Gradle tracks that JAR as a compiler
input. The development runtime adds core as a separate mod; packaging never
includes core output. `tools/verify-module-boundaries.py` checks imports, fully
qualified calls, reflective class names, class constant pools, JAR entries and
metadata. Both source checks and mapped-game regression tests are build gates.

## Public extension points

All supported names below begin with `com.salts_inventory_update.api`.

| API | Contract |
| --- | --- |
| `client.recipebrowser.RecipeBrowserAccess`, models and `RecipeBrowserBridge` | Register a recipe backend with `install(source, access)`; withdraw it with `clear(source, access)`. `RecipeBrowserSource(id, displayName, priority)` accepts external providers. The highest-priority available provider supplies the generic browser window. The default is a no-provider fallback. Registrations participate in integration rollback. |
| `server.desktop.DesktopServerWindowHandler.usesNativePlayerInventoryQuickMove` | Opt into the native menu's player-inventory quick-move semantics, useful for virtual storage. Core handles inventory-slot matching, cursor preservation and synchronization. Default behavior stays unchanged. |
| `server.desktop.DesktopSessions` | Capture native menu openings with serialized opening data; run a native transition within an authenticated detached session; obtain an active routing session; send a bounded session payload; derive a block source identity; query open menus for native ownership checks. Server-thread use only. `openMenuSession` preserves the existing nullable result: `null` means use the original opener, an empty result means the desktop handled an opening without a new session. |
| `desktop.DesktopItemSources` | Register a resolver for an item-backed menu. Return an inventory slot, stable identity, copied fingerprint and optional identity predicate. Core acquires/releases reference-counted protections with session lifetime. Query methods let native interactions respect the same protection. No resolver means no locking. |
| `desktop.DesktopSynchronization` | `preserveDesktop(proposed)` retains the active desktop synchronizer during a native menu's synchronizer installation and otherwise returns the proposed synchronizer unchanged. |
| `server.desktop.DesktopServerSessionContext.carriedStack / setCarriedStack` | Read a copy of the shared desktop cursor and commit the result of a native menu action. Both operations require a live session on the server thread. Commit before calling `broadcastChanges`; the core clears detached cursor copies and synchronizes the canonical cursor. |
| `desktop.DesktopMenuBindings.registerStorage` | Declare the slots eligible for storage sorting; shared by the authoritative server and client. |
| `integration.DesktopEnvironment` | Loader-neutral mod-presence checks, server-start callbacks and transactional `registerIntegration(owner, action)`. Resolve optional classes only after the target is present; keep client code in the client phase. |
| `client.desktop.DesktopClientApi` | Client-start callbacks, mutation readiness, configuration-screen factory, and registration of `DesktopHelpPage` guidance. Existing window/menu/view APIs remain available. |
| `desktop.DesktopPayloadLimits` | Supported maximum size for authenticated custom payloads. Target-specific wire formats and limits belong to their integration. |
| `client.desktop.DesktopThemeAssets` | Supported skin texture identifiers for external renderers: window frame (11×11, use `windowNineSlice`), container widgets (48×30), scrollbar background (14×3) and search bar (3×12). Consumers obtain resources through these API constants instead of assuming private core asset paths. |
| `client.gui`, `client.input` on legacy targets | Supported native rendering and input wrappers previously exposed from implementation packages. Update consumer imports to these public packages. Newer targets keep Minecraft's native types. |

Use each target's API types when compiling: 1.20.1/1.21.1 use
`ResourceLocation`; newer targets use `Identifier`. 1.20.1 opening data uses
`FriendlyByteBuf`, later targets use `RegistryFriendlyByteBuf`. Native action
types likewise follow the game's existing version-specific API.

## Version-specific recipe rendering hooks

The 1.20.1 API now provides the same default `RecipeBrowserAccess.usesCompactRecipeSpacing` and `renderRecipeTooltip` hooks already available on 1.21.1. Providers can request compact native recipe layouts and draw their widget tooltips in the browser tooltip phase. Defaults preserve existing providers. `GuiGraphicsExtractor.setClientTooltipForNextFrame` draws native text and image tooltip components through the supported rendering API.

## Adding an integration

1. Put a new implementation in its own `compat/<mod>` extension package. Its
   Salts imports must all come from `api.*`; interactions with the target mod
   remain inside that package.
2. Register menu bindings and server handlers from the common phase, then menu
   factories/window definitions from the client phase. A recipe backend installs
   through the browser API instead of changing the core screen.
3. Gate initialization on the target's presence and supported version. The
   extension's `CompatibilityBootstrap` demonstrates lazy resolution and
   transaction isolation; optional loader-discovered recipe plugins retain
   their native third-party lifecycle. Conditional mixins must target Minecraft
   or the third-party mod, never Salts implementation classes.
4. Add compile-only target dependencies and optional metadata to the extension
   target only. Use runtime dependencies only for opt-in development profiles.
5. Add targeted regressions and run the root build. A new integration must
   compile with the same API-only classpath as the existing implementations.

External developers can instead publish their own Fabric/Forge/NeoForge mod
and use the [API discovery descriptor](desktop_window_api.md) or call the APIs
from their existing lifecycle. They do not need the bundled compatibility
extension. Existing configuration files, desktop protocol IDs and persisted
recipe-window keys are retained; historical `jei` names in generic UI/config
and wire fields do not imply a JEI runtime dependency.

The extension keeps the core's detailed-console-log preference through the
public environment diagnostics flags. Repeated world starts reuse Tom's
existing server registration. Sophisticated's shared helper registers payload
and sorting behavior; each concrete Backpacks/Storage integration registers its
own slot/source bindings exactly once, avoiding duplicate-registration failures.

## Runtime verification

Smoke runs can use an isolated game directory with `-PsaltsSmokeRun=true` and
`-PsaltsSmokeProfile=<name>`. Each target uses its own
`build/runtime-smoke/<name>` directory. Set `SIU_FUNCTIONAL_TESTS=true`,
`SIU_FUNCTIONAL_TEST_EXIT=true` and `SIU_FUNCTIONAL_TEST_LOADER` to the loader,
then pass `-PincludeFunctionalTests=true` to `runClient`. This harness is never
included in a normal production build. Optional mods can be supplied through
the development runtime flags or an isolated profile's `mods` directory.

The runtime harness checks client startup, configuration, keybindings, vanilla
screen registration, built-in API definitions, packet contracts, text input and
inventory-expansion limits. With optional targets present it also checks their
announced menu counts/server support and EMI provider activation. It does not
exercise actual world interactions, multiplayer synchronization or every native
third-party widget. Those scenarios must be reported separately from successful
startup and headless regression checks.

## AE2 extension consumer

The [AE2 integration guide](../../compatibility/ae2/README.md) documents the native
version matrix, API boundary, instruction-delta validation and runtime evidence.
Each adapter uses its upstream version's native design. All twelve extension
JARs include the shared additions; native support requires an available, reviewed
upstream AE2 release for that Minecraft/loader target. No AE2 classes or resources
enter Core, and neither artifact embeds upstream AE2.

## Raised HUD interoperability

Core owns the reusable public HUD layout API and its synchronized render/input geometry. Compatibility owns the optional Raised 6.0.2 adapter on all twelve targets, using only public Core and Raised APIs. Raised is compile-only and never embedded. See [HUD interoperability](hud_interoperability.md).
