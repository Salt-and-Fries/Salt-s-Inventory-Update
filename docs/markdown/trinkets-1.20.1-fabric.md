# Trinkets integration: Minecraft 1.20.1 Fabric

The 1.20.1 Fabric Compatibility build supports **Trinkets 3.7.2**. Install the rebuilt
Core **0.1.5** and Compatibility **0.1.2** together, with matching Fabric API and Trinkets
on the client and server. Trinkets supplies its Cardinal Components dependencies in the
upstream release. Neither Salt's JAR embeds Trinkets. This adapter is Fabric only.

Accessories use Character's vanilla armor/offhand anchors and native Trinkets group layouts.
Native hover panels, icons, multiple instances, item validators, equip/unequip callbacks,
removal restrictions and synchronization remain authoritative. Trinkets itself adds no
wearable items; install an accessory content mod or suitable slot/tag datapack.

Character always shows the real offhand when the integration is active. If the HUD also
shows it, both locations edit the same server-owned stack. Popup frames and items render
above the player preview and inactive accessory slots, below later windows and the recipe
guide. Expanded inventory slots retain their identity across native equipment rebuilds.

All native Trinkets code lives in Compatibility. It consumes Core's public
`DesktopBuiltinAttachment` and `DesktopPlayerMenuApi` interfaces, including lifecycle,
armor/offhand bounds, real menu slot rendering/hits, occlusion, placement reservations,
equipment transfer rules and managed slot rebuild hooks. Core has no Trinkets dependency.
The native helper ABI is pinned to 3.7.2; absent or unsupported releases do not initialize
the adapter or its native mixin.

```powershell
.\gradlew.bat :1.20.1:fabric:assemble :compatibility:1.20.1:fabric:assemble
```

The root build distributes the production JARs to:

```text
dist/core/salts_inventory_update-1.20.1-Fabric-0.1.5.jar
dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.2.jar
```

The development-only fixture is separate from both production JARs:

```powershell
.\gradlew.bat -I functional-tests/trinkets120-qa.init.gradle :compatibility:1.20.1:fabric:runClient `
  -PincludeTrinketsRuntime=true -PsaltsSmokeRun=true -PsaltsSmokeProfile=trinkets120-focused
```

It supplies native test slots and wearable tags, a removal-locked item and a capacity
modifier. Commands `/trinketsqa seed`, `/trinketsqa rebuild` and `/trinketsqa grow` exercise
real server inventories. `/trinketsqaclient equipment <menu-slot-id>` uses the authenticated
desktop transport. It never substitutes client-only inventory writes for transfers.

See [the 1.21.1 integration](trinkets-1.21.1-fabric.md) for the shared design and
[Desktop window API](desktop_window_api.md) for the public extension points.
Executed checks and coverage limits are recorded in the
[1.20.1 focused validation](../../functional-tests/TRINKETS-1.20.1-FABRIC-VALIDATION.md).
