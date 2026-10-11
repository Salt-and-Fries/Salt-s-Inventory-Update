# Runnable shared inventory companion consumer

This standalone **NeoForge 26.3** mod is built from the complete public consumer sources under `api-examples/ui/src/main/java/example/companion`. It has no required Salt dependency and no bundled Salt classes. Its ordinary menu/source/loader/screen code contains no Salt references. Optional providers are discovered only when Salt is present.

Build using:

```powershell
$env:CI = 'true'
./gradlew :26.3:neoforge:companionDemoJar
```

The artifact is `build/api-examples/companion-example-26.3-NeoForge-api5.jar`. Install that same jar on a matching client/server. Test Salt present by adding the matching API5 Salt jar to both sides; test absence by removing Salt. The consumer's metadata does not make Salt required.

Use a disposable demonstration world: the first command puts test tool items in hotbar slots 0 and 1. These own two distinct, real nine-slot inventories. Main windows contain no copies or proxies of those tool slots.

```text
/companiondemo main1
/companiondemo main2
/companiondemo other
/companiondemo tool1
/companiondemo tool2
/companiondemo invalidate
/companiondemo replace
```

`main1` and `main2` use the same tool; `other` uses the second tool. With Salt installed, the first two main windows share one independently draggable companion. Its lifetime follows both owners; close either parent and continue using the companion, then close the final parent to dispose it. Opening `other` produces a separate companion and inventory. The first tool starts with sixteen redstone; the second starts with twelve paper.

The companion accepts redstone/paper, limits slot 7 to one item and makes slot 8 read-only. Pickup, placement, Shift-click, dragging and shared cursor actions use its own genuine server menu. Replacing the item in hotbar slot 0 invalidates the old source; `invalidate` explicitly revokes that source. Source replacement is deliberately tied to the actual tool object, not just its item type/title.

The original main artwork is 176×64 GUI pixels and the companion artwork is 176×52, with retained original borders and padding. The main surface's title includes custom Close, Pin/Unpin, Minimize/Restore and More controls. More opens Salt's manager menu. The companion has no independent management actions. Every control and original texture is painted by the consumer; Salt adds no extra frame.

Without Salt, normal main/tool screens use the same textures, ordinary menu synchronization and vanilla input. Use `tool1`/`tool2` to open the tool inventory directly in this mode. Dedicated servers register common menus/commands and never resolve the ordinary client screen initializer. Salt's common provider contains no client imports.

For automated native acceptance, start a matching NeoForge client with the JVM argument `-Dcompanion.demo.qa=true`. This creates a disposable `CompanionAPI5QA` world and shuts the client down when the assertions finish. Use a fresh game directory for each run and set `guiScale:2`, `guiScale:3` or `guiScale:4` in `options.txt`; the native evidence includes all three. Enable unlocked/minimizable windows in Salt's configuration and dismiss its first-run instructions before the run. Successful Salt-present runs print `COMPANION NATIVE ACCEPTANCE COMPLETE`; absent runs print `COMPANION ABSENT CLIENT COMPLETE`. A `COMPANION NATIVE FAILED` line or a nonzero process exit is a failure.

The native suite checks real server inventory counts and cursor conservation, restrictions, Shift-click, dragging/cancellation, both parent closing orders, separate sources, position retention, management controls, keyboard access, foreground and modal blocking, resource reload, smaller viewports, source invalidation/replacement, dimension travel and disconnect. It invokes ordinary Minecraft input and public Salt handles; it does not call Salt implementation classes.

For dedicated acceptance, start a disposable matching NeoForge server with `-Dcompanion.demo.serverQA=true`, an accepted EULA and both the consumer and optional Salt jar. Repeat using the same consumer jar with Salt removed. `COMPANION DEDICATED COMPLETE` and exit 0 confirm common-only loading and actual menu/backend assertions using a server FakePlayer. This probe does not connect a remote client; authenticated desktop packet routing is exercised by the integrated native client suite.

The demonstration's inventories are intentionally ephemeral and removed at logout. A production tool integration supplies its actual persisted inventory and source locator; it does not use the demonstration's in-memory source registry or test commands.

`generate-artwork.py` regenerates the deterministic original fixture textures. The [public guide](../../docs/markdown/desktop_window_api.md#shared-inventory-companions-and-original-surface-controls) documents source identity, every constructor/method, thread/focus/lifetime contracts and inventory authorization.

Native acceptance and final artifact hashes are recorded in [the implementation audit](../../API-COMPANIONS-0.1.4-PLAN.md). A successful build alone does not prove native lifecycle, inventory/network or visual behavior.
