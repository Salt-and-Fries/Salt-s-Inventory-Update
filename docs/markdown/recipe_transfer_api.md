# Server-authorized recipe transfers

These contracts are in `com.salts_inventory_update.api.server.desktop` on every supported Minecraft/loader target. Recipe interpretation belongs to the integrating mod. Salt resolves the recipe on the server and asks the registered menu handler to approve its normalized ingredient requirements and destination slots.

## Handler and request

```java
public interface DesktopServerWindowHandler<T extends AbstractContainerMenu, S> {
    default DesktopTransferDecision validateTransfer(DesktopTransferRequest<T, S> request) {
        return DesktopTransferDecision.unsupported();
    }
}

public record DesktopTransferRequest<T extends AbstractContainerMenu, S>(
    DesktopServerSessionContext<T, S> session,
    Recipe<?> recipe,
    boolean maxTransfer
) { }
```

The default disables custom-menu transfers. Override the method to opt in. Returning `null` is an error. The callback runs on the server thread after session authorization; do not retain its context or modify menus asynchronously. `recipe()` is the server-resolved recipe instance, not a client-supplied ingredient list. `maxTransfer()` indicates the client's request to transfer multiple crafts; it does not authorize a particular quantity.

The session supplies:

```java
ServerPlayer player();
T menu();
int sessionId();
String sourceKey();
boolean visible();
boolean ghostPinned();
S state();
void broadcastChanges();
```

It also has `sendToClient(channel, byte[])`. The channel is `ResourceLocation` on 1.20.1/1.21.1 and `Identifier` on later targets. From 1.21.1 onward it additionally accepts a payload and `StreamCodec<? super RegistryFriendlyByteBuf, P>`.

Check your mod's permissions, supported recipe types, current machine/storage configuration, exact logical destination ids and relevant slot restrictions. Approving a transfer is separate from processing a recipe or granting remote-storage access. Keep the validated menu and session paired; never substitute the player's unrelated active menu.

## Requirements and decisions

```java
public record DesktopTransferRequirement(
    int targetSlotId,
    int count,
    List<ItemStack> alternatives
) { }

DesktopTransferDecision.unsupported();
DesktopTransferDecision.denied(String reason);
DesktopTransferDecision.allowed(
    List<DesktopTransferRequirement> requirements,
    List<Integer> destinationSlots,
    int maximumCrafts
);
```

`targetSlotId` is the logical menu slot id used by Salt's slot binding. `count` is the positive quantity needed for one craft. Alternatives are server-approved equivalent item choices; their stacks are copied on construction and when read. The list must be nonempty and contain at most 32 alternatives. Do not derive this list from untrusted client catalog entries.

`unsupported()` means the menu/recipe has no transfer implementation. `denied(reason)` means the implementation recognizes the request but refuses it; the reason is non-null and limited to 256 characters. `allowed(...)` declares normalized requirements, all approved destination slots and a craft cap from 1 through 64.

An allowed decision requires:

- Between 1 and 128 requirements, with distinct nonnegative target ids.
- At most 512 alternatives across all requirements.
- At most 128 unique, nonnegative destination ids, including every requirement target.
- A positive craft cap no greater than 64.

Invalid construction throws `IllegalArgumentException`; numeric overflow can throw `ArithmeticException`. The decision defensively copies its lists. Slot existence, placement/pickup restrictions and current availability still apply during execution; a decision does not make an invalid slot usable.

Read a decision with `status()`, `requirements()`, `destinationSlots()`, `maximumCrafts()`, `reason()`, `supported()` and `allowed()`. Status is `UNSUPPORTED`, `DENIED` or `ALLOWED`. `supported()` is true for both allowed and denied decisions.

## Consumer pattern

The mod backend should first normalize a server recipe into the real menu's destinations. Then the optional provider translates those ordinary backend results into Salt requirements:

```java
@Override
public DesktopTransferDecision validateTransfer(
        DesktopTransferRequest<MyMachineMenu, MySessionState> request) {
    var session = request.session();
    if (!session.menu().mayConfigure(session.player())) {
        return DesktopTransferDecision.denied("Machine access denied");
    }

    // Mod-owned logic interprets the server recipe and current machine configuration.
    var plan = session.menu().normalizeRecipe(request.recipe());
    if (plan == null) return DesktopTransferDecision.unsupported();

    var requirements = plan.inputs().stream()
        .map(input -> new DesktopTransferRequirement(
            input.logicalSlotId(), input.countPerCraft(), input.serverAlternatives()))
        .toList();
    int cap = request.maxTransfer() ? Math.min(64, plan.maximumCrafts()) : 1;
    if (cap < 1) return DesktopTransferDecision.denied("Machine cannot accept this recipe");
    return DesktopTransferDecision.allowed(requirements, plan.destinationSlotIds(), cap);
}
```

The `MyMachineMenu`/plan methods above represent your backend responsibilities. They are not additional Salt API methods. Preserve server-side recipe/configuration validation there, and register the resulting handler through `DesktopServerApi.registerWindow` in the optional common provider. Keep normal mod entrypoints free of Salt types.

## Complete compiling example

[`MachineTransferHandler`](../../api-examples/ui/src/main/java/example/desktop/MachineTransferHandler.java) is registered by `ExampleCommonIntegration`. It approves only a server-resolved smelting recipe accepted by the ordinary [`MachineMenu.normalizeRecipe`](../../api-examples/ui/src/main/java/example/desktop/MachineMenu.java) backend. It validates access and active input, supplies one logical destination with one item per craft, and caps a single/max request at 1/64 crafts. Fuel and output slots are not destinations. Recipes with more than 32 alternatives are refused explicitly. Salt still checks actual source availability, slot restrictions and cursor conservation during execution.

The canonical backend uses modern `placementInfo().ingredients()` and ingredient item holders. The build adapts the two legacy targets to `getIngredients()`/`getItems()`. Run `./gradlew verifyApiExamples` to compile the complete provider/backend with every supported loader's actual classpath. Keep the original menu and normal server opening registered; with Salt present the optional handler enables recipe-browser transfer, and with Salt absent the ordinary backend loads without Salt types.

Consumer packaging and native execution are separate acceptance gates recorded in `API-SURFACE-0.1.4-PLAN.md`; compiling this source alone is not a runtime inventory proof.
