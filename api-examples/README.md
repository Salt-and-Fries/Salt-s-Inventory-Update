# Optional API examples for 0.1.4

`optional-mod` is the external-style loading fixture. Its ordinary entrypoint contains no Salt references. The common test suite compiles its providers against the public API, removes Salt from the runtime classpath for an absence test, then verifies phased discovery and dedicated-server client-class safety.

`ui/src/main/java/example/desktop` contains the original-style machine, irregular virtual terminal, native client-only settings and styled-decoration examples. Their ordinary models/menus/hooks contain no Salt references. Register real `MenuType`s in your normal Fabric/Forge/NeoForge mod lifecycle and call `ExampleMenus.bind(...)` before Salt's deferred discovery. Register your original screens normally. Copy the providers into your mod alongside the descriptor below, replacing its owner id with your own.

```properties
modId=developer_examples
apiVersion=5
common=example.desktop.ExampleCommonIntegration
client=example.desktop.ExampleClientIntegration
```

Save this as `META-INF/salts_inventory_update.integration.properties`. Do not install the providers as ordinary mod entrypoints. Keep Salt compile-only and optional in loader metadata. When your preferences action runs, call `ExampleUiHooks.openPreferences.test(instanceKey, model)`; if it returns false, open your usual preferences screen.

Run `./gradlew verifyApiExamples` (with `CI=true` for NeoForge) to compile every example on all twelve targets. The canonical source uses 26.3 native names; `prepareApiExamples` produces each target's input/graphics/identifier adaptation in `build/generated/api-examples`. `compileApiExamples` compiles against that loader's production output and real Minecraft classpath. These are reusable integration sources, not a complete distributable test mod, and they do not enter Salt's release jars.

The terminal uses a reproducible 10,000-entry catalog and a server-validated selection action. It demonstrates virtual rendering and session-targeted synchronization without manufacturing inventory items. Replace its catalog/selection backend with your actual storage permissions and transaction logic for withdrawals.

See [the API guide](../docs/markdown/desktop_window_api.md) for ownership, threading, discovery, lifecycle and verification rules.

The machine uses `OriginalSlotArtwork` for computed gesture previews and exposes a Menu button using `MANAGEMENT_MENU`. `MachineTransferHandler` enables server-validated smelting transfers into input slot 0; fuel/output remain outside its destination plan. The terminal leaves its upper corners transparent and hosts protruding tabs, an inspector and a declared modal popup. Native settings widgets use an instance-owned `DesktopWidgetHost` for focus, narration and cancellation. Call `ExampleUiHooks.openStyledPreferences` to try the styled variant.

`ui/src/main/java/example/companion` demonstrates two independent main sessions acquiring one real nine-slot Network Tool companion, separate sources, source replacement authorization, drag-only lifetime policy and opt-in controls in original title artwork. The ordinary backend and menu types contain no Salt references. Bind `CompanionMenus` alongside your normal registry registration, then use the optional companion providers named in `companion-demo` metadata. See the [public companion contract](../docs/markdown/companion_windows.md).

Build the standalone NeoForge 26.3 consumer with `./gradlew :26.3:neoforge:companionDemoJar`. It packages the example backend, optional providers, original textures and normal fallback screens, without Salt classes or a required Salt dependency. See [the runnable demonstration README](companion-demo/README.md). The all-target `verifyApiExamples` task also compiles its shared companion integration sources on all twelve supported targets.
