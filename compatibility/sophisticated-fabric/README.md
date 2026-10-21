# Sophisticated Fabric adapters

These adapters consume the maintained [Sophisticated Core](https://github.com/Salandora/SophisticatedCore), [Backpacks](https://github.com/Salandora/SophisticatedBackpacks) and [Storage](https://github.com/Salandora/SophisticatedStorage) Fabric ports on Minecraft 1.20.1 and 1.21.1. They are separate upstream builds with different APIs from the Forge and NeoForge releases.

The source roots under `versions/<Minecraft>/src/main` are included only in the corresponding Fabric compatibility module. Shared lifecycle code lives in `src/main/java`. They are deliberately outside the shared `compatibility/versions/<Minecraft>/fabric` root that Forge and NeoForge also compile.

All Salt imports use its public `api` packages. Native menu factories, drawing, controls, slot semantics and message codecs belong to the installed upstream mods. This extension does not bundle their classes, libraries or textures.

Common registration waits for server starting; client registration waits for the public client-start callback. Each integration registers transactionally and resolves optional classes only after its mod is present. Mixins remain dormant when their corresponding target is absent.

The native Fabric UI uses inventory parts and plain search text boxes. Widget translation includes visible children and hidden controls inside closed tabs. Native GUI packets retain their codecs and travel through authenticated public desktop session APIs. The 1.20.1 bridge checks message direction, payload size, trailing bytes and a fixed GUI-message allowlist. Its extended menu provider hook preserves the native opening data used by chests.

The 1.20.1 slot-rendering redirect declares explicit named and intermediary selectors. Upstream widens a private vanilla method into an override, whose name legacy Loom does not propagate to the external subclass. Keep both selectors; their parameter descriptors still remap normally. The production coverage audit rejects a JAR without the intermediary selector. This hook targets the upstream screen and Minecraft, with no access widener or mixin targeting Core internals.

The build declares native Fabric ports as compile-only inputs. Development runs can opt into their runtime and nested libraries; production JARs never include them. The checked-in [upstream release snapshot](../coverage-targets.json) records availability and exact QA inputs. Run `python tools/audit-compatibility-coverage.py` from the repository root to check source coverage, or add `--jars` after rebuilding distributions.

Run focused gameplay with current checked-in upstream pins:

```powershell
.\functional-tests\scripts\Run-GameplayQa.ps1 -Minecraft 1.20.1 -Loader fabric -Scenario sophisticated -IntegrationOnly -UpstreamSnapshot -RunId review-1
.\functional-tests\scripts\Run-GameplayQa.ps1 -Minecraft 1.21.1 -Loader fabric -Scenario sophisticated -IntegrationOnly -UpstreamSnapshot -RunId review-1
```

Gameplay evidence includes real native menus, authenticated mutations, authoritative server checks, attached upgrade tabs, settings transitions, storage windows and native framebuffer captures. Screenshot capture alone does not establish interaction coverage.
