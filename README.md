# Salt's Inventory Update

Salt's Inventory Update is a multi-version, multi-loader inventory framework. It builds three independent mods for each of twelve supported targets: **Salt's Inventory Update** (`salts_inventory_update`, core), **Salt's Inventory Compatibility** (`salts_inventory_compat`, optional extension), and **Salt's Inventory Controlify** (`salts_inventory_controlify`, optional controller client).

Salt's Inventory Controlify provides the controller workspace across all twelve Minecraft and loader targets. Its [installation, controls and build guide](controlify/README.md) describes the radial presentation, separate JARs and pinned upstream dependencies. Install the matching rebuilt core from this branch alongside the native Controlify build, YACL and the controller JAR.

Core provides inventory expansion, vanilla desktop windows, configuration, networking, persistence and the public API. Compatibility contains Tom's Simple Storage, JEI, REI, EMI, Mod Menu, Applied Energistics 2, Raised and the supported Sophisticated integrations, including the maintained Fabric ports on 1.20.1 and 1.21.1. The [coverage audit](docs/markdown/compatibility-coverage.md) records upstream availability for every target. AE2 support follows the [reviewed upstream version matrix](compatibility/ae2/README.md#reviewed-upstream-targets). Current builds use Core 0.1.5, Compatibility 0.1.2 and Controlify companion 0.1.1 on every target. Match the Minecraft version and loader across installed files; Core works on its own, and both companions and supported third-party mods remain optional.

Mouse Tweaks input support is included in **Core** on every supported target. Install the matching upstream Mouse Tweaks JAR alongside Core; the Compatibility extension is not required. Drag collection, right-drag placement and wheel transfers use Mouse Tweaks' own settings and also work with real inventory slots exposed by public-API windows. See the [API integration notes](docs/markdown/desktop_window_api.md#mouse-tweaks-input-interoperability).

## Layout

```text
versions/
  common/              Shared Java used by every version and loader
  1.20.1/
    common/            Code/resources shared by Fabric and Forge on 1.20.1
    fabric/            Fabric entrypoint and fabric.mod.json
    forge/             Forge entrypoint and mods.toml
  1.21.1/
    common/            Code/resources shared by Fabric and NeoForge on 1.21.1
    fabric/
    neoforge/
  1.21.11/
    common/
    fabric/
    neoforge/
  26.1.2/
    common/
    fabric/
    neoforge/
  26.2/
    common/
    fabric/
    neoforge/
  26.3/
    common/
    fabric/
    neoforge/
compatibility/
  ae2/                 Shared desktop additions and version-specific native AE2 adapters
  fabric-modmenu/       Shared Fabric Mod Menu consumer
  sophisticated-fabric/ Native Fabric port adapters, separate from Forge/NeoForge
  versions/<version>/
    common/            Compatibility module's loader-neutral source/resource root
    fabric/            Shared integration implementations and Fabric metadata/entrypoints
    forge|neoforge/    Loader-specific plugins, conditional mixins and metadata
  tests/               Optional integration initialization regressions
controlify/
  common/              Controller state, layout, animation, input routing and configuration
  versions/<version>/  Controller presentation, Controlify processor and loader entrypoints
  model/               Loader-neutral controller tests
  functional-tests/   Excluded native workspace regression fixtures
dist/
  core/                Twelve production core JARs and checksums
  compatibility/       Twelve production extension JARs and checksums
  controlify/          Twelve production controller JARs and checksums
```

The version and loader matrix is declared in [settings.gradle.kts](settings.gradle.kts). All three mods use Prism's existing loader toolchains and production remapping. Dependency versions and mod metadata live in [build.gradle.kts](build.gradle.kts); API-only compilation, automated tests and distribution tasks live in [gradle/compatibility.gradle](gradle/compatibility.gradle) and [gradle/controlify.gradle](gradle/controlify.gradle). Existing core project paths remain stable.

## Common Code Pattern

- Put loader-free Java in `versions/common`.
- Put Minecraft-version-specific shared code in `versions/<mc-version>/common`.
- Put Fabric, Forge, or NeoForge hooks in `versions/<mc-version>/<loader>`.
- Put integrations in `compatibility/versions/<mc-version>/fabric/.../compat/<mod>` and loader-specific integrations in the extension's Forge/NeoForge source root.

The extension compiles against a generated JAR containing only `com.salts_inventory_update.api.*`. It has no access to implementation classes, and does not package that API JAR or core into its own artifact. Source and production bytecode checks enforce this boundary. Core has no integration dependency.

This keeps version API differences contained while still letting you share most of your mod logic.

## Useful Commands

Build with JDK 25 installed. Gradle daemon criteria select Java 25 for Prism 0.6.4 and Loom 1.18.2; individual Minecraft compilation and game runtimes retain their version-specific toolchains. Gradle 9.7 is supplied by the wrapper. This toolchain can remap current upstream Fabric dependencies, including JEI releases built with static mixin remapping.

```powershell
.\gradlew.bat projects
.\gradlew.bat tasks
.\gradlew.bat build
```

The ordinary root `build` compiles and tests all three mods on all twelve targets, producing **36 production JARs**. It also compiles the API examples, checks source parity, remaps production artifacts and verifies JAR contents. The 26.3 artifact task uses ModDevGradle's supported binary patch pipeline automatically; `CI=true` is no longer needed.

Production JARs appear in `dist/core`, `dist/compatibility` and `dist/controlify`, with filenames such as `salts_inventory_update-1.21.1-Fabric-0.1.5.jar`, `salts_inventory_compat-1.21.1-Fabric-0.1.2.jar` and `salts_inventory_controlify-1.21.1-Fabric-0.1.1.jar`. Each directory has `SHA256SUMS.txt`. The root release build clears `dist` before building and synchronizes exact production task outputs, preventing old, development or sources JARs from entering a release. `build/release/0.1.5` is the current Core-only release bundle.

The [combined latest archive](dist/salts-inventory-all-latest.zip) groups all 36 production JARs by Minecraft version and loader. Select the folder matching your installation; the extension and controller companion are optional. The archive contains JARs rather than gameplay screenshots, and upstream mod dependencies are installed separately as described in their guides.

Build individual targets with `./gradlew :1.21.1:fabric:build`, `./gradlew :compatibility:1.21.1:fabric:build` or `./gradlew :controlify:1.21.1:fabric:build`. Individual artifacts remain in the target's `build/libs`; use the root build to refresh `dist`. Use `:1.21.1:fabric:runClient` for core alone, `:compatibility:1.21.1:fabric:runClient` for the extension plus core, and `:controlify:1.21.1:fabric:runClient` for the controller companion plus core. Extension development runtimes retain the optional `-PincludeJeiRuntime=true`, `-PincludeReiRuntime=true`, `-PincludeEmiRuntime=true` and `-PincludeSophisticatedRuntime=true` flags on supported targets.

For publishing, configure the existing Prism `publishing` block for core and a separate `publishing` block inside `mod("compatibility")` for the extension, using separate project IDs. Set the relevant publishing tokens in your environment and declare the matching core as a required extension release dependency. Upload the production JARs from the matching `dist` directory.

Standalone release notes: [Core 0.1.5](changelogs/changelog-0.1.5.md), [Compatibility 0.1.2](changelogs/changelog-compatibility-0.1.2.md), and [Controlify 0.1.1](changelogs/changelog-controlify-0.1.1.md).

## Public API for 0.1.4

The [API guide](docs/markdown/desktop_window_api.md) covers optional providers, menu and client-only windows, native UI reuse, shared real-inventory companions, original-surface management controls and session-aware actions. [Compilable examples](api-examples/README.md) include a standalone nine-slot companion consumer. The public API is version 6 and desktop wire protocol is version 4; previous public API entrypoints were removed.

`./gradlew verifyApiExamples` compiles the examples against all twelve loader/version targets. `build` also verifies these examples and checks that every release jar contains the redesigned API.

See [the separation and extension guide](docs/markdown/jar-separation.md) for the architecture audit, new API hooks and rules for adding integrations without changing core.

Core 0.1.5 uses public integration API 6. Minimize and Ghost Pins have been removed, including settings and API members; ordinary pins and Persistent Windows remain available. See the [API migration guide](docs/markdown/desktop_window_api.md#migrating-to-015--api-6) and [window cleanup validation](functional-tests/WINDOW-CLEANUP-VALIDATION.md).

Window Movement Control defaults to **Left Alt**. Rebind it in Minecraft's **Controls → Key Binds → Salt's Inventory Update**, alongside the mod's character and recipe-browser bindings. **Window Movement Key** in Salt's Inventory settings opens that standard Key Binds screen. Minecraft's Controls screen contains only the **Hold / Toggle** selector; the mod settings share the same saved preference. Hold remains the default. Toggle switches camera/world control while windows are open and resets when you leave the workspace, open another menu, enter text, or lose game focus. It does not change the existing movement-key behavior. Holding the vanilla **Open/Close Inventory** binding closes the workspace after the configured duration, including when that binding is remapped to another key or mouse button. Help displays the selected binding; Persistent Windows retains its existing hide/restore behavior.

With **Return Hotbar to Inventory** enabled, the native hotbar and its status indicators hide while windows are open. Activating Window Movement Control temporarily restores them and allows wheel and hotbar-key selection. Returning to cursor control locks the newly selected hotbar slot again. Window scrolling and inventory slot operations remain available.

HUD mods can use the [public HUD layout API](docs/markdown/hud_interoperability.md) for independent slot placement, scaling, visibility and aligned pointer geometry. Raised 6.0.2 support is supplied by the optional Compatibility extension on all twelve supported targets.
