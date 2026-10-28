# Native template example

Minecraft **all twelve supported Minecraft/loader targets only**. `FurnaceNativeTemplate<T>` accepts a registered furnace-like modded menu type whose machine slots are input 0, fuel 1 and output 2. Call `register(YOUR_MENU_TYPE)` from an optional SIU client provider after registry setup. Do not register it for an unrelated menu or use a vanilla furnace as the automatic-host example: vanilla furnaces retain Core's built-in definition.

The example inherits automatic native artwork, moves the output frame and native recipe button when present, adds space/padding, and adds a functional Details button using its own state. It imports only SIU's supported API and Minecraft classes. It is an example integration source, not a standalone test mod, and never enters release JARs.

Run `./gradlew verifyNativeTemplateExample` to compile it against the same API-only classpath used by Compatibility on both loaders. See [the full API/profile/inspector guide](../../docs/markdown/native-window-templates-1.20.1.md).
