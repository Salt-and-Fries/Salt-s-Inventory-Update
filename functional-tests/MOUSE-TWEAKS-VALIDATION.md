# Core Mouse Tweaks validation

Scope: Core 0.1.5 on the twelve supported version/loader targets. Mouse Tweaks is optional, uses a compile-only upstream API, and is not bundled. Compatibility 0.1.2 and Controlify 0.1.1 remain unchanged.

The subsequent [Windows Computer Use playthrough](MOUSE-TWEAKS-COMPUTER-USE-VALIDATION.md) found and fixed an exhausted-crafting-output bug. It records real mouse input, five runtime retests against the rebuilt Core artifacts, and the limits of that coverage. The synthetic results below describe the implementation before that additional fix.

## Implementation

- The optional mixin plugin detects upstream API bytecode without loading client classes during discovery. Core's normal screen has no Mouse Tweaks type dependency when the mod is absent.
- Mouse Tweaks' own custom-screen interface and algorithms operate on desktop slots. Core resolves native and public-API slot geometry, physical aliases, player inventory ownership, hidden/occluded windows, protected backing items and API-declared virtual slots.
- Temporary input snapshots let synchronous gestures calculate several native clicks while the server retains inventory authority. Cursor references remain coherent when an upstream algorithm keeps an earlier `ItemStack` reference. Rendering continues to use authoritative inventory updates.
- Minecraft 1.20.1 negotiates an additional optional capability for bounded click sequences. The server validates every source/target token, revision, visibility and permission before the first action, uses its own carried stack, and acknowledges the result after inventory synchronization. Targeted Core Shift-moves retain their chosen destination and share this lane. There is no public API signature or protocol-version bump. Older servers without the capability keep the prior Core behavior and do not enable desktop Mouse Tweaks gestures.
- Empty targets use their real slot capacity instead of an empty item's component default, preserving multi-item wheel transfers on versions where the empty-item limit is one. Camera, popup and API input ownership pause gestures without permanently opting the cached screen out.
- Later targets use their existing authenticated mutation queue. Dropped/rejected work invalidates cached input predictions.

## Reproduce focused checks

```powershell
python tools/prepare-mouse-tweaks-qa.py
.\gradlew.bat -I functional-tests/core-artifacts-qa.init.gradle stageCoreQaArtifacts --console=plain
.\gradlew.bat -I functional-tests/chest-close-qa.init.gradle -PqaMouseTweaksOnly=true -PqaMinecraft=26.3 -PqaLoader=fabric -PqaRunId=my-fresh-run :26.3:fabric:runMouseTweaksQa --console=plain
```

Quote each `-P...` argument in a PowerShell argument array if your shell splits dotted values. Add `-PqaMouseTweaksAbsent=true` to run the standalone Core case. Choose a fresh run ID to preserve previous evidence. Forge inputs are copied from the original production Core and upstream artifacts, then converted with ModDev's actual SRG-to-named tool for the development runtime; the launcher records original and converted checksums.

The harness runs rendered Minecraft clients and disposable integrated servers. It invokes the released upstream mouse callbacks in native loader order and Core's GUI callbacks; wheel events exercise the production Core scroll hook. Shift is supplied by a QA-only input mixin. This is synthetic input in the real game, not a manual mouse/keyboard playthrough. QA classes are excluded from production JARs.

## Runtime results

All six cases passed against the Core JAR hashes staged for that earlier synthetic run. Those hashes precede the later crafting-output guard. The counts below include stage records and diagnostics, not only assertions.

| Minecraft | Loader | Mouse Tweaks | Result | Records | Evidence |
| --- | --- | --- | --- | --- | --- |
| 1.20.1 | forge | Absent | PASS | 29 | `build/mouse-tweaks/gameplay/1.20.1-forge-mt-standalone/results.json` |
| 26.3 | fabric | Absent | PASS | 29 | `build/mouse-tweaks/gameplay/26.3-fabric-mt-standalone/results.json` |
| 1.21.1 | fabric | Installed | PASS | 102 | `build/mouse-tweaks/gameplay/1.21.1-fabric-mt-complete/results.json` |
| 1.20.1 | forge | Installed | PASS | 104 | `build/mouse-tweaks/gameplay/1.20.1-forge-mt-complete/results.json` |
| 26.3 | fabric | Installed | PASS | 102 | `build/mouse-tweaks/gameplay/26.3-fabric-mt-complete/results.json` |
| 26.3 | neoforge | Installed | PASS | 102 | `build/mouse-tweaks/gameplay/26.3-neoforge-mt-complete2/results.json` |

Installed-mod cases cover LMB collection, Shift-LMB transfers, RMB placement/revisits and the final carried item, disabled LMB/RMB options, multi-item wheel push/pull, fractional scrolling, disabled wheel settings, crafting output consumption, a custom public-API window layout, excluded virtual slots, popup suspension and a first event in camera mode followed by resumed inventory input. Inventory counts are checked on the actual server. The 1.20.1 case also verifies that a batch containing an invalid source token is rejected before its first click. Standalone cases verify startup, normal pickup/return and Core's Shift-transfer/hotbar destination without Mouse Tweaks.

Framebuffer captures for the custom API layout and crafting output were visually inspected. Earlier failed prototype and test-fixture runs remain in separate directories; they are not passing release evidence. The fixture now explicitly synchronizes both inventory contents and the final authoritative player revision after direct setup writes. NeoForge's QA Shift input mixin is declared in its loader metadata, while Forge uses the manifest and Fabric uses its JSON metadata.

Runtime tests cover four of the twelve targets. The other eight targets were compiled and packaged against their pinned upstream Mouse Tweaks release, but were not launched in this focused test run.

## Artifact checks

All twelve production Core targets built. `tools/verify-mouse-tweaks-artifacts.py` validates mod metadata, optional mixins, absence of bundled upstream/QA code, release copies, checksums and the 36-JAR download bundle. With the local pre-change baseline it also verified unchanged public API class bytes and all 24 companion JAR hashes. `tools/verify-module-boundaries.py --sources --jars` verifies the existing Core/Compatibility boundaries.

Coverage is focused on the affected inventory gestures. The complete functional/gameplay suite, every third-party API consumer, high-latency dedicated multiplayer and every Mouse Tweaks configuration permutation have not been tested during this task.
