# Core config-menu access

Date: 2026-10-22. Core remains 0.1.5. Scope: configuration entrypoint registration, screen construction/rendering and close/reopen behavior. This is a focused menu regression, not the complete functional/gameplay suite.

## Cause and correction

The preceding 26.3 Fabric production JAR reproduced the missing Core factory with Core + Mod Menu and no Compatibility extension. Mod Menu reported no config screen for `salts_inventory_update`. The factory had been supplied only by Compatibility's optional Mod Menu plugin.

Every Fabric Core target now owns its optional `modmenu` entrypoint. The shared adapter in `core-integrations/modmenu/` calls the existing public `DesktopClientApi.createConfigScreen` factory. Mod Menu is compile-only, is not bundled, and remains optional. Compatibility retains its own API-only config link and its older-Core fallback; Mod Menu uses `putIfAbsent` for provided factories, so Core's own factory takes priority. No API, protocol or config-schema changes are required.

Forge and NeoForge already register native factories on Core's own mod container. Their production implementation is unchanged; the focused checks inspect that registration and use it with the real native mod-list screen as parent.

## Focused execution

```powershell
.\functional-tests\scripts\Run-ConfigMenuQa.ps1 -Minecraft <version> -Loader <fabric|forge|neoforge>
```

The previous 26.3 JAR is selected by `-UseDistribution -ExpectMissing` for reproduction. `-WithoutModMenu` checks standalone Fabric Core and the public config-screen factory.

Fabric runs the production Core JAR with a separate QA mod and original upstream Mod Menu files. Forge/NeoForge use the source-classpath launcher with test sources enabled. The profile loads no world and selects no unrelated gameplay tests. Each target checks factory availability, opens Core settings, verifies the parent and rendered controls, closes back to the mod list, and repeats the open/close cycle. Mod Menu 7's older direct factory path and the newer safe-open path are both covered. Screenshots capture the actual framebuffer; 26.3 Fabric's settings capture was visually inspected.

All twelve targets passed **nine menu assertions each**, with zero failures:

| Minecraft | Fabric + Mod Menu | Native loader |
| --- | --- | --- |
| 1.20.1 | PASS | Forge: PASS |
| 1.21.1 | PASS | NeoForge: PASS |
| 1.21.11 | PASS | NeoForge: PASS |
| 26.1.2 | PASS | NeoForge: PASS |
| 26.2 | PASS | NeoForge: PASS |
| 26.3 | PASS | NeoForge: PASS |

A separate **26.3 Fabric Core-only** run, without Mod Menu or Compatibility installed, passed another nine assertions for startup, public config-screen access and close/reopen behavior. The optional entrypoint does not make Mod Menu mandatory.

All six Fabric module `build` tasks passed, including source archives, remapping and existing checks. The corrected **Core 0.1.5** files are staged in [dist/core](../dist/core/) with refreshed [checksums](../dist/core/SHA256SUMS.txt) and matching [release mirrors](../build/release/0.1.5/). No version number changed.

The [artifact comparison](../build/config-menu-qa/artifact-verification.json) confirms exactly six changed Fabric Core JARs and **30 unchanged production JARs**: six native Core, twelve Compatibility and twelve Controlify. Final Fabric class files and non-loader resources match their successful menu fixtures. Spatial GUI class bytes also match the preceding spatial gameplay fixtures. Source and production boundary audits passed; Mod Menu references are restricted to the optional Core config adapter, no Mod Menu implementation is embedded, and no QA classes are distributed. Native config factories needed no implementation changes.

## Evidence and limits

- [Reproduction result](../build/gameplay-qa/packaged-core/26.3-fabric-config-menu-before/results.json)
- [Fabric menu results and captures](../build/gameplay-qa/packaged-core/)
- [Native-loader results and captures](../build/config-menu-qa/)
- [Standalone Core result](../build/gameplay-qa/packaged-core/26.3-fabric-config-menu-standalone/results.json)
- [Successful Fabric builds](../build/config-menu-qa/production-build.log)
- [Artifact comparison](../build/config-menu-qa/artifact-verification.json)
- [Source and production boundaries](../build/config-menu-qa/boundaries.log)

These are automated factory/UI callback checks, not physical mouse clicks on every mod-list button. Native tests use source classpaths, rather than packaged-JAR launches. In-world settings access with every other optional presentation/mod combination was not retested; the configuration implementation and Spatial GUI classes are unchanged.
