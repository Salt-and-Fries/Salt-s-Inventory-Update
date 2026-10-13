# Salt's Inventory Update

Salt's Inventory Update is a multi-version, multi-loader inventory framework. It builds two independent mods for every supported target: **Salt's Inventory Update** (`salts_inventory_update`, core) and **Salt's Inventory Compatibility** (`salts_inventory_compat`, optional extension).

Core provides inventory expansion, vanilla desktop windows, configuration, networking, persistence and the public API. Compatibility contains Tom's Simple Storage, JEI, REI, EMI, Mod Menu, Applied Energistics 2 and the supported Forge/NeoForge Sophisticated integrations. AE2 support follows the [reviewed upstream version matrix](compatibility/ae2/README.md#reviewed-upstream-targets). Install matching core and extension versions to enable these integrations; core works on its own. Supported third-party mods remain optional.

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
  versions/<version>/
    common/            Compatibility module's loader-neutral source/resource root
    fabric/            Shared integration implementations and Fabric metadata/entrypoints
    forge|neoforge/    Loader-specific plugins, conditional mixins and metadata
  tests/               Optional integration initialization regressions
dist/
  core/                Twelve production core JARs and checksums
  compatibility/       Twelve production extension JARs and checksums
```

The version and loader matrix is declared in [settings.gradle.kts](settings.gradle.kts). Both mods use Prism's existing loader toolchains and production remapping. Dependency versions and mod metadata live in [build.gradle.kts](build.gradle.kts); API-only compilation, automated tests and distribution tasks live in [gradle/compatibility.gradle](gradle/compatibility.gradle). Existing core project paths remain stable.

## Common Code Pattern

- Put loader-free Java in `versions/common`.
- Put Minecraft-version-specific shared code in `versions/<mc-version>/common`.
- Put Fabric, Forge, or NeoForge hooks in `versions/<mc-version>/<loader>`.
- Put integrations in `compatibility/versions/<mc-version>/fabric/.../compat/<mod>` and loader-specific integrations in the extension's Forge/NeoForge source root.

The extension compiles against a generated JAR containing only `com.salts_inventory_update.api.*`. It has no access to implementation classes, and does not package that API JAR or core into its own artifact. Source and production bytecode checks enforce this boundary. Core has no integration dependency.

This keeps version API differences contained while still letting you share most of your mod logic.

## Useful Commands

```powershell
.\gradlew.bat projects
.\gradlew.bat tasks
.\gradlew.bat build
```

The ordinary root `build` compiles and tests both mods on all twelve targets, compiles the API examples, checks source parity, remaps production artifacts and verifies JAR contents. The 26.3 artifact task uses ModDevGradle's supported binary patch pipeline automatically; `CI=true` is no longer needed.

Production JARs appear in `dist/core` and `dist/compatibility`, with filenames such as `salts_inventory_update-1.21.1-Fabric-0.1.4.jar` and `salts_inventory_compat-1.21.1-Fabric-0.1.4.jar`. Each directory has `SHA256SUMS.txt`. The root release build clears `dist` before building and synchronizes exact production task outputs, preventing old, development or sources JARs from entering a release. `build/release/0.1.4` remains the legacy core-only release bundle.

Build individual targets with `./gradlew :1.21.1:fabric:build` or `./gradlew :compatibility:1.21.1:fabric:build`. Individual artifacts remain in the target's `build/libs`; use the root build to refresh `dist`. Use `:1.21.1:fabric:runClient` for core alone and `:compatibility:1.21.1:fabric:runClient` for the extension plus core. Extension development runtimes retain the optional `-PincludeJeiRuntime=true`, `-PincludeReiRuntime=true`, `-PincludeEmiRuntime=true` and `-PincludeSophisticatedRuntime=true` flags on supported targets.

For publishing, configure the existing Prism `publishing` block for core and a separate `publishing` block inside `mod("compatibility")` for the extension, using separate project IDs. Set the relevant publishing tokens in your environment and declare the matching core as a required extension release dependency. Upload the production JARs from the matching `dist` directory.

## Public API for 0.1.4

The [API guide](docs/markdown/desktop_window_api.md) covers optional providers, menu and client-only windows, native UI reuse, shared real-inventory companions, original-surface management controls and session-aware actions. [Compilable examples](api-examples/README.md) include a standalone nine-slot companion consumer. The public API is version 5 and desktop wire protocol is version 4; previous public API entrypoints were removed.

`./gradlew verifyApiExamples` compiles the examples against all twelve loader/version targets. `build` also verifies these examples and checks that every release jar contains the redesigned API.

See [the separation and extension guide](docs/markdown/jar-separation.md) for the architecture audit, new API hooks and rules for adding integrations without changing core.
