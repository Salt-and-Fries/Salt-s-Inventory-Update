# Public API signature reference — 0.1.5 / API 6

This inventory lists public consumer declarations from the shared API and the 26.3 native API. Signatures are reference notation, not standalone implementations. Record components supply their corresponding accessors and canonical constructor; enum values are listed. Bodies and private implementation members are omitted. The [API guide](desktop_window_api.md) specifies defaults, lifecycle, geometry, threading and ownership; the [transfer reference](recipe_transfer_api.md) specifies transfer validation and bounds.

The shared contracts apply to every supported target. Native declarations use 26.3 names/imports here. On 1.20.1/1.21.1, `Identifier` is `ResourceLocation` and input events use Salt’s legacy event package; 1.20.1 lacks typed payload codecs. `nativeGraphics()` is `GuiGraphics` through 1.21.11 and `GuiGraphicsExtractor` on 26.x. Slot action types are `ClickType` through 1.21.11 and `ContainerInput` on 26.x. Compile the examples on the matching target to resolve native binary differences.

Regenerate with `python tools/generate-api-reference.py`; verify without writing with `python tools/generate-api-reference.py --check`.

## DesktopChromeMetrics

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopChromeMetrics.java).

[Standard control placement and compression](desktop_window_api.md#matching-the-controls-on-salts-vanilla-windows) specifies ordering, full-title fit thresholds, the expanded-or-More/Close policy, trailing gap/right inset, title minimum and the supported mod-owned control subset.

```java
public record DesktopChromeMetrics(int titleBarHeight, int titleLeftPadding, int titleTextY, int controlWidth, int controlHeight, int controlGap, int controlRightInset, int controlTopInset, int resizeGripSize) {
    public static final DesktopChromeMetrics SALT = new DesktopChromeMetrics(16, 6, 5, 11, 11, 3, 2, 4, 9);
    public DesktopChromeMetrics(int titleBarHeight, int titleLeftPadding, int titleTextY, int controlWidth, int controlHeight, int controlGap, int controlRightInset, int controlTopInset, int resizeGripSize);
    public int controlsWidth(int count);
    public DesktopInsets contentInsets(int padding);
}
```

## DesktopControlState

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopControlState.java).

```java
public record DesktopControlState(DesktopWindowControl control, DesktopWindowBounds bounds, boolean hovered, boolean pressed, boolean enabled, boolean selected, boolean keyboardFocused) {
    public DesktopControlState(DesktopWindowControl control, DesktopWindowBounds bounds, boolean hovered, boolean pressed, boolean enabled, boolean selected);
}
```

## DesktopGestureEvent

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopGestureEvent.java).

```java
import java.util.Objects;
public record DesktopGestureEvent(long id, Outcome outcome, String reason) {
    public enum Outcome {
        STARTED, UPDATED, COMPLETED, CANCELLED
    }
    public DesktopGestureEvent(long id, Outcome outcome, String reason);
}
```

## DesktopGesturePhase

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopGesturePhase.java).

```java
public enum DesktopGesturePhase {
    NONE, PENDING, DISTRIBUTING
}
```

## DesktopHitRole

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopHitRole.java).

```java
public enum DesktopHitRole {
    NONE(0, 0, 0), MOVE(10, 0, 0), RESIZE_NORTH(20, 0, -1), RESIZE_SOUTH(20, 0, 1), RESIZE_WEST(20, -1, 0), RESIZE_EAST(20, 1, 0), RESIZE_NORTH_WEST(20, -1, -1), RESIZE_NORTH_EAST(20, 1, -1), RESIZE_SOUTH_WEST(20, -1, 1), RESIZE_SOUTH_EAST(20, 1, 1), CONTENT(5, 0, 0), SLOT(60, 0, 0), CONTROL(70, 0, 0), POPUP(80, 0, 0);
    public int priority();
    public boolean resizing();
    public int resizeX();
    public int resizeY();
}
```

## DesktopHudApi

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopHudApi.java).

```java
import com.salts_inventory_update.api.integration.DesktopDiagnostics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
public final class DesktopHudApi {
    public static synchronized RegistrationHandle register(String owner, int priority, DesktopHudLayoutProvider provider);
    public static DesktopHudLayout resolve(DesktopHudContext context, DesktopHudLayout base);
    @FunctionalInterface public interface RegistrationHandle extends AutoCloseable {
        @Override void close();
    }
}
```

## DesktopHudContext

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopHudContext.java).

```java
public record DesktopHudContext(int viewportWidth, int viewportHeight, double guiScale) {
    public DesktopHudContext(int viewportWidth, int viewportHeight, double guiScale);
}
```

## DesktopHudLayout

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopHudLayout.java).

```java
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
public record DesktopHudLayout(List<DesktopHudSlot> slots) {
    public DesktopHudLayout(List<DesktopHudSlot> slots);
    public static DesktopHudLayout standard(int width, int height, int offhandGap);
    public DesktopHudLayout mapSlots(UnaryOperator<DesktopHudSlot> mapper);
    public DesktopHudLayout transformed(DesktopTransform transform);
    public DesktopHudLayout translated(double x, double y);
}
```

## DesktopHudLayoutProvider

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopHudLayoutProvider.java).

```java
public interface DesktopHudLayoutProvider {
    DesktopHudLayout adapt(DesktopHudContext context, DesktopHudLayout layout);
}
```

## DesktopHudSlot

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopHudSlot.java).

```java
import java.util.Objects;
public record DesktopHudSlot(Kind kind, int index, DesktopTransform transform, boolean visible, boolean interactive) {
    public enum Kind {
        HOTBAR, OFFHAND
    }
    public DesktopHudSlot(Kind kind, int index, DesktopTransform transform, boolean visible, boolean interactive);
    public boolean contains(double screenX, double screenY);
    public DesktopHudSlot withTransform(DesktopTransform next);
    public DesktopHudSlot withVisibility(boolean next);
    public DesktopHudSlot withInteraction(boolean next);
    public DesktopHudSlot transformed(DesktopTransform outer);
}
```

## DesktopInsets

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopInsets.java).

```java
public record DesktopInsets(int left, int top, int right, int bottom) {
    public static final DesktopInsets ZERO = new DesktopInsets(0, 0, 0, 0);
    public static final DesktopInsets SALT = new DesktopInsets(8, 24, 8, 8);
    public DesktopInsets(int left, int top, int right, int bottom);
    public int horizontal();
    public int vertical();
    public DesktopWindowSize surfaceSize(int contentWidth, int contentHeight);
    public int contentWidth(int surfaceWidth);
    public int contentHeight(int surfaceHeight);
}
```

## DesktopPopup

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopPopup.java).

```java
import java.util.List;
import java.util.Objects;
public record DesktopPopup(String id, List<DesktopWindowRegion> regions, boolean modal, boolean dismissOnOutside, boolean dismissOnEscape, boolean dismissOnFocusLoss) {
    public enum Dismissal {
        OUTSIDE_PRESS, ESCAPE, FOCUS_LOST
    }
    public DesktopPopup(String id, List<DesktopWindowRegion> regions, boolean modal, boolean dismissOnOutside, boolean dismissOnEscape, boolean dismissOnFocusLoss);
    public boolean contains(int ownerX, int ownerY, double mouseX, double mouseY);
}
```

## DesktopSurfaceControl

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopSurfaceControl.java).

[Standard control placement and compression](desktop_window_api.md#matching-the-controls-on-salts-vanilla-windows) specifies ordering, full-title fit thresholds, the expanded-or-More/Close policy, trailing gap/right inset, title minimum and the supported mod-owned control subset.

```java
import java.util.Objects;
public record DesktopSurfaceControl(DesktopWindowControl control, DesktopWindowBounds bounds) {
    public DesktopSurfaceControl(DesktopWindowControl control, DesktopWindowBounds bounds);
}
```

## DesktopSurfaceMode

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopSurfaceMode.java).

```java
public enum DesktopSurfaceMode {
    SALT, MOD_OWNED
}
```

## DesktopTransform

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopTransform.java).

```java
public record DesktopTransform(double x, double y, double scale) {
    public DesktopTransform(double x, double y, double scale);
    public double screenX(double localX);
    public double screenY(double localY);
    public double localX(double screenX);
    public double localY(double screenY);
    public int pixelX(double localX);
    public int pixelY(double localY);
}
```

## DesktopViewportPolicy

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopViewportPolicy.java).

```java
public enum DesktopViewportPolicy {
    CLAMP, PRESERVE, ADAPT
}
```

## DesktopWindowAction

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowAction.java).

```java
public enum DesktopWindowAction {
    FOCUS, USER_CLOSE, PIN, UNPIN, MOVE, RESIZE, LOCK, UNLOCK, MANAGEMENT_MENU
}
```

## DesktopWindowBounds

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowBounds.java).

```java
public record DesktopWindowBounds(int x, int y, int width, int height) {
    public DesktopWindowBounds(int x, int y, int width, int height);
    public DesktopWindowBounds resizeGesture(DesktopHitRole role, int deltaX, int deltaY, int minWidth, int minHeight, int maxWidth, int maxHeight);
}
```

## DesktopWindowControl

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowControl.java).

```java
public enum DesktopWindowControl {
    FOCUS, CLOSE, ELLIPSIS, LINK, LOCK, PIN, SORT_WINDOWS, RESIZE_GRIP
}
```

## DesktopWindowEnvironment

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowEnvironment.java).

```java
public record DesktopWindowEnvironment(int viewportWidth, int viewportHeight, double guiScale, long resourceRevision) {
    public DesktopWindowEnvironment(int viewportWidth, int viewportHeight, double guiScale, long resourceRevision);
}
```

## DesktopWindowHandle

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowHandle.java).

```java
import java.util.UUID;
public interface DesktopWindowHandle {
    UUID instanceId();
    boolean isOpen();
    boolean focus();
    boolean close();
    DesktopWindowState state();
    boolean available(DesktopWindowAction action);
    boolean request(DesktopWindowAction action);
    boolean moveTo(int x, int y);
    boolean resizeTo(int width, int height);
    boolean cancelInput();
    boolean schedule(Runnable action);
}
```

## DesktopWindowOptions

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowOptions.java).

```java
public record DesktopWindowOptions(boolean movable, boolean pinnable, boolean closable, boolean lockable, boolean managementMenu) {
    public static final DesktopWindowOptions DEFAULT = new DesktopWindowOptions(true, true, true, true, true);
    public static final DesktopWindowOptions DRAG_ONLY = new DesktopWindowOptions(true, false, false, false, false);
    public DesktopWindowOptions(boolean movable, boolean pinnable, boolean closable);
}
```

## DesktopWindowRegion

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowRegion.java).

```java
import java.util.Objects;
public record DesktopWindowRegion(int x, int y, int width, int height, boolean visual, boolean interactive, boolean constrainsWindow, DesktopHitRole role) {
    public DesktopWindowRegion(int x, int y, int width, int height, boolean visual, boolean interactive, boolean constrainsWindow, DesktopHitRole role);
    public static DesktopWindowRegion visual(int x, int y, int width, int height);
    public static DesktopWindowRegion interactive(int x, int y, int width, int height);
    public static DesktopWindowRegion visualAndInteractive(int x, int y, int width, int height);
    public static DesktopWindowRegion transientVisualAndInteractive(int x, int y, int width, int height);
    public DesktopWindowRegion withRole(DesktopHitRole hitRole);
    public DesktopWindowRegion transientRegion();
    public boolean contains(int windowX, int windowY, double mouseX, double mouseY);
}
```

## DesktopWindowSize

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowSize.java).

```java
public record DesktopWindowSize(int width, int height) {
    public DesktopWindowSize(int width, int height);
    public static DesktopWindowSize of(int width, int height);
}
```

## DesktopWindowState

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowState.java).

```java
import java.util.UUID;
public record DesktopWindowState(UUID instanceId, boolean open, int x, int y, int width, int height, boolean pinned, boolean locked, boolean keyboardFocused, boolean inventoryFocused, boolean visible) {
}
```

## DesktopWindowSurface

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowSurface.java).

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
public record DesktopWindowSurface(DesktopSurfaceMode mode, DesktopInsets insets, boolean bodyInteractive, List<DesktopWindowRegion> regions, DesktopViewportPolicy viewportPolicy) {
    public static final DesktopWindowSurface SALT = new DesktopWindowSurface(DesktopSurfaceMode.SALT, DesktopInsets.SALT, true, List.of(), DesktopViewportPolicy.CLAMP);
    public DesktopWindowSurface(DesktopSurfaceMode mode, DesktopInsets insets, boolean bodyInteractive, List<DesktopWindowRegion> regions, DesktopViewportPolicy viewportPolicy);
    public static DesktopWindowSurface modOwned(List<DesktopWindowRegion> regions);
    public DesktopWindowSurface withRegions(List<DesktopWindowRegion> additional);
    public DesktopHitRole hitRole(int windowX, int windowY, int width, int height, double mouseX, double mouseY);
    public boolean occludes(int windowX, int windowY, int width, int height, double mouseX, double mouseY);
}
```

## DesktopWindowTasks

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowTasks.java).

```java
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
public final class DesktopWindowTasks implements AutoCloseable {
    public DesktopWindowTasks(DesktopWindowHandle owner);
    public <T> AutoCloseable watch(CompletionStage<T> source, Consumer<T> success, Consumer<Throwable> failure);
    public <T> AutoCloseable submit(Executor executor, Function<BooleanSupplier, T> work, Consumer<T> success, Consumer<Throwable> failure);
    public boolean isClosed();
    @Override public void close();
}
```

## DesktopWorkspaceWindow

`com.salts_inventory_update.api.client.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWorkspaceWindow.java).

```java
public record DesktopWorkspaceWindow(DesktopWindowHandle handle, Kind kind, String title, boolean closable) {
    public enum Kind {
        PLAYER_INVENTORY, CHARACTER, OTHER
    }
}
```

## DesktopScrollState

`com.salts_inventory_update.api.client.desktop.widget`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop/widget/DesktopScrollState.java).

```java
public final class DesktopScrollState {
    public void layout(int rows, int rowHeight, int viewportHeight);
    public double offset();
    public double maxOffset();
    public void offset(double pixels);
    public boolean scroll(double pixels);
    public VisibleRows visibleRows(int overscan);
    public double rowY(int row);
    public int rowAt(double localY);
    public record VisibleRows(int first, int endExclusive) {
    }
}
```

## DesktopInventoryIdentity

`com.salts_inventory_update.api.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/desktop/DesktopInventoryIdentity.java).

```java
import java.util.Objects;
import java.util.UUID;
public final class DesktopInventoryIdentity {
    public static DesktopInventoryIdentity reference(Object inventory);
    public static DesktopInventoryIdentity persistent(String namespace, UUID sourceId);
    @Override public boolean equals(Object other);
    @Override public int hashCode();
}
```

## DesktopPayloadLimits

`com.salts_inventory_update.api.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/desktop/DesktopPayloadLimits.java).

```java
public final class DesktopPayloadLimits {
    public static final int MAX_CUSTOM_DATA_BYTES = com.salts_inventory_update.protocol.DesktopProtocol.MAX_CUSTOM_DATA_BYTES;
}
```

## DesktopDiagnostics

`com.salts_inventory_update.api.integration`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/integration/DesktopDiagnostics.java).

```java
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
public final class DesktopDiagnostics {
    public enum Level {
        INFO, WARNING, ERROR
    }
    public record Event(Level level, String owner, String operation, String message, Throwable cause) {
    }
    public static void report(Level level, String owner, String operation, String message, Throwable cause);
    public static List<Event> snapshot();
    public static AutoCloseable listen(Consumer<Event> listener);
}
```

## DesktopIntegration

`com.salts_inventory_update.api.integration`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/integration/DesktopIntegration.java).

```java
public interface DesktopIntegration {
    void register(DesktopIntegrationContext context);
}
```

## DesktopIntegrationContext

`com.salts_inventory_update.api.integration`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/integration/DesktopIntegrationContext.java).

```java
import java.util.Objects;
import java.util.function.Predicate;
public record DesktopIntegrationContext(String modId, Phase phase, Predicate<String> loadedMods) {
    public static final int API_VERSION = 6;
    public static final String DESCRIPTOR_PATH = "META-INF/salts_inventory_update.integration.properties";
    public DesktopIntegrationContext(String modId, Phase phase, Predicate<String> loadedMods);
    public boolean isModLoaded(String id);
    public enum Phase {
        COMMON, CLIENT
    }
}
```

## DesktopCompanionLease

`com.salts_inventory_update.api.server.desktop`. Shared; no native Minecraft types. [Source](../../versions/common/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopCompanionLease.java).

```java
public interface DesktopCompanionLease extends AutoCloseable {
    int sessionId();
    boolean isOpen();
    @Override void close();
}
```

## DesktopClientApi

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopClientApi.java).

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
public final class DesktopClientApi {
    public static void setWorkspaceHost(DesktopWorkspaceHost host);
    public static DesktopWorkspaceHost workspaceHost();
    public static void openPlayerWorkspace();
    public static void onClientTick(java.util.function.Consumer<net.minecraft.client.Minecraft> action);
    public static java.util.Optional<DesktopWorkspace> currentWorkspace();
    public static boolean canDispatchOptimisticMutationNow();
    public static net.minecraft.client.gui.screens.Screen createConfigScreen(net.minecraft.client.gui.screens.Screen parent);
    public static void onClientStarted(java.util.function.Consumer<net.minecraft.client.Minecraft> action);
    public static synchronized void registerHelpPage(Identifier id, DesktopHelpPage page);
    public static synchronized List<DesktopHelpPage> helpPages();
    public static synchronized <T extends AbstractContainerMenu, S> void registerWindow(MenuType<T> menuType, DesktopWindowDefinition<T, S> definition);
    public static synchronized <T extends AbstractContainerMenu, S> void replaceWindow(MenuType<T> menuType, DesktopWindowDefinition<T, S> definition);
    public static synchronized void registerMatcher( Identifier id, int priority, Set<MenuType<?>> menuTypes, DesktopWindowMatcher matcher, DesktopWindowDefinitionFactory factory );
    public static synchronized @Nullable DesktopWindowDefinition<?, ?> findDefinition(DesktopWindowLookupContext context);
    public static synchronized List<String> supportedMenuIds();
    public static synchronized <M, S> void registerView(Identifier id, Class<M> modelType, DesktopWindowDefinition<M, S> definition);
    ) public static java.util.Optional<DesktopWindowHandle> openView(Identifier id, String instanceKey, Object model, net.minecraft.network.chat.Component title);
}
```

## DesktopContextualBookHint

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopContextualBookHint.java).

```java
public record DesktopContextualBookHint(DesktopContextualMenu.Kind kind, DesktopWindowBounds activationBounds, DesktopWindowBounds iconBounds) {
}
```

## DesktopContextualMenu

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopContextualMenu.java).

```java
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
public record DesktopContextualMenu(Kind kind, List<Category> categories, List<Recipe> recipes, List<Tab> tabs, Station station) {
    public static final DesktopContextualMenu NONE = new DesktopContextualMenu(Kind.NONE, List.of(), List.of(), List.of());
    public DesktopContextualMenu(Kind kind, List<Category> categories, List<Recipe> recipes, List<Tab> tabs);
    public DesktopContextualMenu(Kind kind, List<Category> categories, List<Recipe> recipes, List<Tab> tabs, Station station);
    public enum Kind {
        NONE, CRAFTING, COOKING, CREATIVE
    }
    public record Station(int gridWidth, int gridHeight, ItemStack input, ItemStack fuel, ItemStack output, float burnProgress, float litProgress) {
        public static final Station NONE = new Station(0, 0, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, 0, 0);
        public Station(int gridWidth, int gridHeight, ItemStack input, ItemStack fuel, ItemStack output, float burnProgress, float litProgress);
        @Override public ItemStack input();
        @Override public ItemStack fuel();
        @Override public ItemStack output();
    }
    public record Category(Identifier id, String title, ItemStack icon) {
        public Category(Identifier id, String title, ItemStack icon);
        @Override public ItemStack icon();
    }
    public record Ingredient(List<ItemStack> alternatives, int required, int available) {
        public Ingredient(List<ItemStack> alternatives, int required, int available);
        @Override public List<ItemStack> alternatives();
    }
    public record Recipe(RecipeDisplayId id, Identifier category, ItemStack output, int gridWidth, int gridHeight, List<Ingredient> grid, List<Ingredient> requirements, boolean craftable) {
        public Recipe(RecipeDisplayId id, Identifier category, ItemStack output, int gridWidth, int gridHeight, List<Ingredient> grid, List<Ingredient> requirements, boolean craftable);
        @Override public ItemStack output();
    }
    public record Tab(Identifier id, String title, ItemStack icon, boolean selected) {
        public Tab(Identifier id, String title, ItemStack icon, boolean selected);
        @Override public ItemStack icon();
    }
}
```

## DesktopControllerTarget

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopControllerTarget.java).

```java
public record DesktopControllerTarget(String group, int row, int column, int x, int y, int width, int height) {
    public DesktopControllerTarget(String group, int row, int column, int x, int y, int width, int height);
}
```

## DesktopGestureSnapshot

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopGestureSnapshot.java).

```java
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
public record DesktopGestureSnapshot(long id, DesktopGesturePhase phase, int button, int quickCraftType, @Nullable UUID ownerInstanceId, List<DesktopSlotPreview> slots, ItemStack carriedPreview, @Nullable String carriedCountText, boolean pointerCaptured) {
    public DesktopGestureSnapshot(long id, DesktopGesturePhase phase, int button, int quickCraftType, @Nullable UUID ownerInstanceId, List<DesktopSlotPreview> slots, ItemStack carriedPreview, @Nullable String carriedCountText, boolean pointerCaptured);
    @Override public ItemStack carriedPreview();
    public @Nullable DesktopSlotPreview slot(int sessionId, int menuSlotId);
}
```

## DesktopHelpPage

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopHelpPage.java).

```java
import java.util.List;
import java.util.Objects;
public record DesktopHelpPage(String title, String iconText, List<Section> sections) {
    public DesktopHelpPage(String title, String iconText, List<Section> sections);
    public record Section(String title, List<String> lines) {
        public Section(String title, List<String> lines);
    }
}
```

## DesktopInputContext

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopInputContext.java).

```java
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
public interface DesktopInputContext<T, S> extends DesktopWindowContext<T, S> {
    boolean shiftDown();
    boolean ctrlDown();
    boolean altDown();
    boolean mouseButtonDown(int button);
    boolean sendMenuButton(int buttonId);
    boolean sendRename(String name);
    boolean clickSlot(int menuSlotId, int button, ContainerInput input);
    boolean quickMoveSlot(int menuSlotId);
    boolean toggleRecipeBook();
    boolean setRecipeBookSearch(String search);
    boolean sendPayload(Identifier channel, byte[] data);
    <P> boolean sendPayload(Identifier channel, P payload, StreamCodec<? super RegistryFriendlyByteBuf, P> codec);
}
```

## DesktopMenuFactories

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopMenuFactories.java).

```java
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
public final class DesktopMenuFactories {
    public static synchronized void register(MenuType<?> menuType, Factory factory);
    public static synchronized @Nullable AbstractContainerMenu create( MenuType<?> menuType, Minecraft minecraft, LocalPlayer player, int containerId, byte[] openingData );
    public static synchronized boolean hasFactory(MenuType<?> menuType);
    @FunctionalInterface public interface Factory {
        AbstractContainerMenu create(Minecraft minecraft, LocalPlayer player, int containerId, byte[] openingData);
    }
}
```

## DesktopMouseButtons

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopMouseButtons.java).

```java
import com.mojang.blaze3d.platform.InputConstants;
public final class DesktopMouseButtons {
    public static int menuButton(int mouseButton);
}
```

## DesktopNativeRenderContext

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopNativeRenderContext.java).

```java
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
public interface DesktopNativeRenderContext {
    GuiGraphicsExtractor rawGraphics();
}
```

## DesktopRecipeGhost

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopRecipeGhost.java).

```java
import net.minecraft.world.item.ItemStack;
public record DesktopRecipeGhost(int slotIndex, ItemStack item) {
    public DesktopRecipeGhost(int slotIndex, ItemStack item);
    @Override public ItemStack item();
}
```

## DesktopRenderContext

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopRenderContext.java).

```java
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
public interface DesktopRenderContext<T, S> extends DesktopWindowContext<T, S>, DesktopNativeRenderContext {
    net.minecraft.client.gui.GuiGraphicsExtractor nativeGraphics();
    default void withClip(int x, int y, int width, int height, Runnable draw);
    default void withTransform(float x, float y, float scale, Runnable draw);
    int mouseX();
    int mouseY();
    void fill(int x1, int y1, int x2, int y2, int color);
    void text(String text, int x, int y, int color, boolean shadow);
    void scaledText(String text, int x, int y, int color, boolean shadow, float scale);
    void text(Component text, int x, int y, int color, boolean shadow);
    void sprite(Identifier sprite, int x, int y, int width, int height);
    void sprite(Identifier sprite, int sourceWidth, int sourceHeight, int sourceX, int sourceY, int x, int y, int width, int height);
    void texture(Identifier texture, int x, int y, int sourceX, int sourceY, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight);
    void windowNineSlice(Identifier texture, int x, int y, int width, int height);
    void onePixelNineSlice(Identifier texture, int x, int y, int width, int height);
    void item(ItemStack stack, int x, int y);
    void item(ItemStack stack, int x, int y, int seed);
    void virtualItem(ItemStack stack, long count, int x, int y);
    void slot(int menuSlotId, int x, int y);
    void slot(Slot slot, int x, int y);
    void texturelessSlot(int menuSlotId, int x, int y);
    void slotBackground(int x, int y);
    void slotHighlight(int x, int y);
    void entityPreview(LivingEntity entity, int x0, int y0, int x1, int y1, int scale, float mouseScale);
    void tooltip(ItemStack stack, int mouseX, int mouseY);
    void tooltip(Component text, int mouseX, int mouseY);
    void tooltip(List<Component> lines, int mouseX, int mouseY);
}
```

## DesktopResizePolicy

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopResizePolicy.java).

```java
public enum DesktopResizePolicy {
    FIXED, FREE, STORAGE_GRID
}
```

## DesktopScrollRegion

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopScrollRegion.java).

```java
public record DesktopScrollRegion(int x, int y, int width, int height, int rowHeight, boolean canScrollUp, boolean canScrollDown, double nativeScrollStep) {
    public DesktopScrollRegion(int x,int y,int width,int height,int rowHeight,boolean canScrollUp,boolean canScrollDown);
    public DesktopScrollRegion(int x, int y, int width, int height, int rowHeight, boolean canScrollUp, boolean canScrollDown, double nativeScrollStep);
    public boolean contains(double px, double py);
    public boolean accepts(double px, double py, int direction, boolean edgeOnly);
}
```

## DesktopSlotContext

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopSlotContext.java).

```java
import org.jspecify.annotations.Nullable;
import net.minecraft.world.inventory.AbstractContainerMenu;
public interface DesktopSlotContext<T, S> extends DesktopWindowContext<T, S> {
    boolean contains(double mouseX, double mouseY, int x, int y, int width, int height);
    @Nullable DesktopSlotHit menuSlotHit(int menuSlotId, int x, int y, double mouseX, double mouseY);
    @Nullable DesktopSlotHit containerSlotHit(int containerSlotIndex, int x, int y, double mouseX, double mouseY);
}
```

## DesktopSlotHit

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopSlotHit.java).

```java
public record DesktopSlotHit(int menuSlotId, int x, int y) {
    public DesktopSlotHit(int menuSlotId, int x, int y);
    public static DesktopSlotHit of(int menuSlotId, int x, int y);
}
```

## DesktopSlotPreview

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopSlotPreview.java).

```java
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
public record DesktopSlotPreview(int sessionId, int menuSlotId, ItemStack displayStack, @Nullable String countText, boolean eligible, boolean selected, boolean hovered) {
    public DesktopSlotPreview(int sessionId, int menuSlotId, ItemStack displayStack, @Nullable String countText, boolean eligible, boolean selected, boolean hovered);
    @Override public ItemStack displayStack();
}
```

## DesktopThemeAssets

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopThemeAssets.java).

```java
import net.minecraft.resources.Identifier;
public final class DesktopThemeAssets {
    public static final Identifier WINDOW = texture("window.png");
    public static final Identifier CONTAINER_WIDGETS = texture("container_widgets.png");
    public static final Identifier SCROLLBAR_BACKGROUND = texture("scroll_bar_behind.png");
    public static final Identifier SEARCH_BAR = texture("search_bar.png");
}
```

## DesktopWindowContext

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowContext.java).

```java
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
public interface DesktopWindowContext<T, S> {
    Minecraft minecraft();
    T model();
    boolean inventoryAvailable();
    boolean visible();
    boolean companion();
    java.util.List<Integer> companionOwnerSessionIds();
    DesktopWindowHandle windowHandle();
    DesktopWindowSurface surface();
    DesktopWindowEnvironment environment();
    DesktopGestureSnapshot gesture();
    boolean pointerCaptured();
    java.util.List<DesktopPopup> popups();
    DesktopWindowTasks tasks();
    default DesktopTransform surfaceTransform();
    default DesktopTransform contentTransform();
    Component originalTitle();
    int sessionId();
    String sourceKey();
    S state();
    List<Slot> containerSlots();
    int windowX();
    int windowY();
    int windowWidth();
    int windowHeight();
    int contentX();
    int contentY();
    int contentWidth();
    int contentHeight();
    boolean focused();
    boolean keyboardFocused();
    ItemStack carriedStack();
    boolean recipeBookVisible();
    boolean refreshRecipeBook();
    @Nullable Slot menuSlot(int menuSlotId);
    @Nullable Slot containerSlot(int containerSlotIndex);
    int menuSlotId(Slot slot);
    int fontWidth(String text);
    int fontWidth(Component text);
    String trimToWidth(String text, int maxWidth);
}
```

## DesktopWindowDecoration

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowDecoration.java).

```java
import java.util.List;
public interface DesktopWindowDecoration<T, S> {
    default DesktopChromeMetrics metrics();
    void render(DesktopRenderContext<T, S> context, List<DesktopControlState> controls);
}
```

## DesktopWindowDefinition

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowDefinition.java).

```java
import org.jspecify.annotations.Nullable;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
public interface DesktopWindowDefinition<T, S> {
    default java.util.List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<T, S> context);
    default java.util.List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<T,S> context);
    default java.util.List<DesktopSurfaceControl> managementControls(DesktopWindowContext<T, S> context);
    default void renderManagementControls(DesktopRenderContext<T, S> context, java.util.List<DesktopControlState> controls);
    default java.util.List<DesktopPopup> popups(DesktopWindowContext<T, S> context);
    default boolean dismissPopup(DesktopWindowContext<T, S> context, String id, DesktopPopup.Dismissal reason);
    default void gestureChanged(DesktopWindowContext<T, S> context, DesktopGestureEvent event);
    default boolean focusNext(DesktopWindowContext<T, S> context, boolean forward);
    default void narrate(DesktopWindowContext<T, S> context, net.minecraft.client.gui.narration.NarrationElementOutput output);
    default @Nullable DesktopWindowDecoration<T, S> decoration(DesktopWindowContext<T, S> context);
    default DesktopWindowSurface surface(DesktopWindowContext<T, S> context);
    default void prepareLayout(DesktopWindowContext<T, S> context);
    default void windowStateChanged(DesktopWindowContext<T, S> context, DesktopWindowState state);
    default String layoutRevision(DesktopWindowContext<T, S> context);
    default void environmentChanged(DesktopWindowContext<T, S> context, DesktopWindowEnvironment environment);
    default void surfaceUnavailable(DesktopWindowContext<T, S> context);
    default @Nullable S createState(DesktopWindowSetupContext<T> context);
    default void loadLocalState(DesktopWindowContext<T, S> context, CompoundTag tag);
    default void saveLocalState(DesktopWindowContext<T, S> context, CompoundTag tag);
    default Component title(DesktopWindowContext<T, S> context);
    default DesktopWindowSize defaultSize(DesktopWindowSetupContext<T> context);
    default DesktopWindowSize minSize(DesktopWindowContext<T, S> context);
    default @Nullable DesktopWindowSize maxSize(DesktopWindowContext<T, S> context);
    default DesktopResizePolicy resizePolicy(DesktopWindowContext<T, S> context);
    default @Nullable DesktopWindowSize snapSize(DesktopWindowContext<T, S> context);
    default void opened(DesktopWindowContext<T, S> context);
    default void closed(DesktopWindowContext<T, S> context);
    default void moved(DesktopWindowContext<T, S> context);
    default void resized(DesktopWindowContext<T, S> context);
    default void keyboardFocusChanged(DesktopWindowContext<T, S> context, boolean focused);
    default DesktopWindowOptions options(DesktopWindowContext<T, S> context);
    default boolean modal(DesktopWindowContext<T, S> context);
    default void renderOverlay(DesktopRenderContext<T, S> context);
    default void focusChanged(DesktopWindowContext<T, S> context, boolean focused);
    default void tick(DesktopWindowContext<T, S> context);
    default void render(DesktopRenderContext<T, S> context);
    default @Nullable RecipeBookComponent<?> createRecipeBook(DesktopWindowContext<T, S> context);
    default @Nullable DesktopSlotHit slotAt(DesktopSlotContext<T, S> context, double mouseX, double mouseY);
    default void mouseMoved(DesktopInputContext<T, S> context, double mouseX, double mouseY);
    default boolean mouseClicked(DesktopInputContext<T, S> context, MouseButtonEvent event, boolean doubleClick);
    default boolean mouseReleased(DesktopInputContext<T, S> context, MouseButtonEvent event);
    default boolean mouseDragged(DesktopInputContext<T, S> context, MouseButtonEvent event, double dx, double dy);
    default boolean mouseScrolled(DesktopInputContext<T, S> context, double mouseX, double mouseY, double scrollX, double scrollY);
    default boolean keyPressed(DesktopInputContext<T, S> context, KeyEvent event);
    default boolean keyReleased(DesktopInputContext<T, S> context, KeyEvent event);
    default boolean charTyped(DesktopInputContext<T, S> context, CharacterEvent event);
    default boolean wantsTextInput(DesktopWindowContext<T, S> context);
    default boolean appendTooltip(DesktopRenderContext<T, S> context, int mouseX, int mouseY);
    default void customPayload(DesktopWindowContext<T, S> context, Identifier channel, byte[] data);
}
```

## DesktopWindowDefinitionFactory

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowDefinitionFactory.java).

```java
public interface DesktopWindowDefinitionFactory {
    DesktopWindowDefinition<?, ?> create(DesktopWindowLookupContext context);
}
```

## DesktopWindowLookupContext

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowLookupContext.java).

```java
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
public record DesktopWindowLookupContext( AbstractContainerMenu menu, MenuType<?> menuType, Component title, int sessionId, String sourceKey, int specialKind, List<Slot> containerSlots, int defaultContentWidth, int defaultContentHeight ) {
}
```

## DesktopWindowMatcher

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowMatcher.java).

```java
public interface DesktopWindowMatcher {
    boolean matches(DesktopWindowLookupContext context);
}
```

## DesktopWindowPresentation

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowPresentation.java).

```java
import java.util.List;
import org.jspecify.annotations.Nullable;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;
public interface DesktopWindowPresentation {
    default void renderBehindFrame(DesktopRenderContext<?, ?> context);
    default @Nullable DesktopWindowSize desiredWindowSize(DesktopWindowContext<?, ?> context);
    default void updateTooltipState( DesktopWindowContext<?, ?> context, int mouseX, int mouseY, boolean active );
    default void cancelPointer(DesktopWindowContext<?, ?> context);
    default List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?, ?> context);
}
```

## DesktopWindowSetupContext

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWindowSetupContext.java).

```java
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
public record DesktopWindowSetupContext<T>( Minecraft minecraft, T model, Component originalTitle, int sessionId, String sourceKey, List<Slot> containerSlots, int defaultContentWidth, int defaultContentHeight, int defaultWindowWidth, int defaultWindowHeight ) {
    public DesktopWindowSetupContext( Minecraft minecraft, T model, Component originalTitle, int sessionId, String sourceKey, List<Slot> containerSlots, int defaultContentWidth, int defaultContentHeight, int defaultWindowWidth, int defaultWindowHeight );
    public boolean inventoryAvailable();
}
```

## DesktopWorkspace

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWorkspace.java).

```java
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public interface DesktopWorkspace {
    List<DesktopWorkspaceWindow> windows();
    DesktopWindowHandle ensureWindow(DesktopWorkspaceWindow.Kind kind);
    void restoreWindow(UUID id);
    void initialize(int width, int height);
    void tick();
    void renderWindow(UUID id, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);
    void renderWindowFrame(GuiGraphicsExtractor graphics, DesktopWindowBounds bounds, Component title);
    default ItemStack previewIcon(UUID id);
    void renderCarried(GuiGraphicsExtractor graphics, int mouseX, int mouseY);
    boolean containsPointer(UUID id, double x, double y);
    boolean dismissOutsidePointer(UUID id, double x, double y);
    boolean pointer(UUID id, PointerAction action, double x, double y, int button, double deltaX, double deltaY);
    boolean keyPressed(int key, int scanCode, int modifiers);
    boolean keyReleased(int key, int scanCode, int modifiers);
    boolean charTyped(char character, int modifiers);
    boolean charTyped(int codepoint, int modifiers);
    default boolean textInputActive(UUID id);
    default List<DesktopWindowBounds> textInputBounds(UUID id);
    net.minecraft.world.inventory.Slot hoveredSlot(UUID id, double x, double y);
    List<DesktopSlotHit> visibleSlots(UUID id);
    List<DesktopWindowBounds> visibleControls(UUID id);
    List<DesktopControllerTarget> controllerTargets(UUID id);
    boolean scrollController(UUID id, double x, double y, int direction, boolean edgeOnly);
    default boolean controllerScrollAvailable(UUID id, double x, double y);
    boolean creativeHotbarAt(UUID id, double x, double y, boolean stack);
    default boolean creativeCatalogAt(UUID id, double x, double y);
    default boolean creativeHotbarDeleteAt(UUID id, double x, double y);
    DesktopWindowBounds presentationBounds(UUID id);
    void clickAt(UUID id, double x, double y, int button, net.minecraft.world.inventory.ContainerInput type);
    void dropCarried(int button);
    default ItemStack carriedStack();
    void cancelInput();
    void transferPair(UUID first, UUID second);
    boolean sortWindow(UUID source, boolean force);
    default boolean canSortWindow(UUID source);
    DesktopContextualMenu contextualMenu(UUID id);
    boolean craftToWindow(UUID source, UUID destination, net.minecraft.world.item.crafting.display.RecipeDisplayId recipe);
    default boolean craftStackToWindow(UUID source, UUID destination, net.minecraft.world.item.crafting.display.RecipeDisplayId recipe);
    boolean prepareCookingRecipe(UUID source, net.minecraft.world.item.crafting.display.RecipeDisplayId recipe);
    boolean beginRecipeHover(UUID source);
    boolean updateRecipeHover(UUID source, net.minecraft.world.item.crafting.display.RecipeDisplayId recipe);
    void endRecipeHover(UUID source);
    List<DesktopRecipeGhost> recipeHoverGhosts(UUID source);
    default boolean recipeHoverRejected(UUID source);
    void setContextualRecipePresentation(boolean enabled);
    default void setCharacterEffectsPresentation(boolean enabled);
    DesktopContextualBookHint contextualBookHint(UUID id);
    boolean selectContextualTab(UUID id, net.minecraft.resources.Identifier tab);
    void protectWindow(UUID id);
    void forceInventoryHud(boolean enabled);
    long closeAllHoldMillis();
    void closeClosableWindows();
    boolean isValid();
    boolean isNativeScreenActive();
    enum PointerAction {
        PRESS, RELEASE, MOVE, DRAG, SCROLL
    }
}
```

## DesktopWorkspaceHost

`com.salts_inventory_update.api.client.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/DesktopWorkspaceHost.java).

```java
public interface DesktopWorkspaceHost {
    boolean enabled();
    void opened(DesktopWorkspace workspace);
}
```

## DesktopTextBoxState

`com.salts_inventory_update.api.client.desktop.widget`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/widget/DesktopTextBoxState.java).

```java
public final class DesktopTextBoxState {
    public String text();
    public void text(String text);
    public boolean focused();
    public void focused(boolean focused);
    public int cursor();
    public void cursor(int cursor);
    public int selectionAnchor();
    public int selectionStart();
    public int selectionEnd();
    public boolean hasSelection();
    public String selectedText();
    public int viewStart();
    public void viewStart(int viewStart);
    public void selectAll();
    public void clearSelection();
    public void moveToStart(boolean selecting);
    public void moveToEnd(boolean selecting);
    public void moveByCodePoints(int amount, boolean selecting);
    public void moveByWords(int amount, boolean selecting);
    public boolean insert(String value);
    public boolean insert(String value, int maxLength);
    public boolean deletePrevious();
    public boolean deleteNext();
    public boolean deletePreviousWord();
    public boolean deleteNextWord();
    public boolean deleteSelection();
    public static String filterText(String text);
    public static boolean isAllowedTextCodePoint(int codePoint);
}
```

## DesktopVirtualItem

`com.salts_inventory_update.api.client.desktop.widget`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/widget/DesktopVirtualItem.java).

```java
import net.minecraft.world.item.ItemStack;
public record DesktopVirtualItem(ItemStack stack, long count) {
}
```

## DesktopWidgetHost

`com.salts_inventory_update.api.client.desktop.widget`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/widget/DesktopWidgetHost.java).

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
public final class DesktopWidgetHost {
    public DesktopWidgetHost add(String id, AbstractWidget widget, Component label);
    public DesktopWidgetHost add(String id, AbstractWidget widget, Component label, Runnable cancellation);
    public boolean focusNext(boolean forward);
    public void clearFocus();
    public void hoverActive(boolean active);
    public @Nullable String focusedId();
    public void render(DesktopRenderContext<?, ?> context);
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick);
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy);
    public boolean mouseReleased(MouseButtonEvent event);
    public void cancelPointer();
    public boolean keyPressed(KeyEvent event);
    public boolean keyReleased(KeyEvent event);
    public boolean charTyped(CharacterEvent event);
    public void narrate(NarrationElementOutput output);
}
```

## DesktopWidgetRect

`com.salts_inventory_update.api.client.desktop.widget`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/widget/DesktopWidgetRect.java).

```java
public record DesktopWidgetRect(int x, int y, int width, int height) {
    public boolean contains(double mouseX, double mouseY);
}
```

## DesktopWidgets

`com.salts_inventory_update.api.client.desktop.widget`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/widget/DesktopWidgets.java).

```java
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
public final class DesktopWidgets {
    public static final int SLOT_SIZE = 18;
    public static final int SEARCH_BAR_HEIGHT = 12;
    public static final int SCROLLBAR_BACKGROUND_WIDTH = 14;
    public static final int SCROLLBAR_THUMB_WIDTH = 12;
    public static final int SCROLLBAR_THUMB_HEIGHT = 15;
    public static final Identifier SEARCH_BAR_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/search_bar.png");
    public static final Identifier SCROLLBAR_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/scroll_bar_behind.png");
    public static final Identifier SCROLLER_SPRITE = Identifier.parse("container/creative_inventory/scroller");
    public static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.parse("container/creative_inventory/scroller_disabled");
    public static void renderTextBox(DesktopRenderContext<?, ?> context, DesktopTextBoxState state, int x, int y, int width);
    public static void renderTextBox(DesktopRenderContext<?, ?> context, String text, boolean focused, int x, int y, int width);
    public static boolean clickTextBox(DesktopTextBoxState state, MouseButtonEvent event, int x, int y, int width);
    public static boolean keyPressedTextBox(DesktopTextBoxState state, KeyEvent event);
    public static boolean keyPressedTextBox(DesktopTextBoxState state, KeyEvent event, int maxLength);
    public static TextEditResult editTextBoxKey(DesktopTextBoxState state, KeyEvent event);
    public static TextEditResult editTextBoxKey(DesktopTextBoxState state, KeyEvent event, int maxLength);
    public static boolean charTypedTextBox(DesktopTextBoxState state, CharacterEvent event);
    public static boolean charTypedTextBox(DesktopTextBoxState state, CharacterEvent event, int maxLength);
    public static TextEditResult editTextBoxChar(DesktopTextBoxState state, CharacterEvent event);
    public static TextEditResult editTextBoxChar(DesktopTextBoxState state, CharacterEvent event, int maxLength);
    public static void renderIconButton(DesktopRenderContext<?, ?> context, Identifier icon, int x, int y, int size, boolean active, boolean hovered);
    public static void renderTextButton(DesktopRenderContext<?, ?> context, Component text, int x, int y, int width, int height, boolean active, boolean hovered);
    public static void renderScrollbar(DesktopRenderContext<?, ?> context, int x, int y, int height, int scroll, int maxScroll);
    public static void renderScrollbarBackground(DesktopRenderContext<?, ?> context, int x, int y, int height);
    public static int scrollByWheel(int scroll, int maxScroll, double scrollY);
    public static void renderDropdown(DesktopRenderContext<?, ?> context, int x, int y, int width, List<Component> labels, List<Component> states, int hoveredIndex);
    public static void renderVirtualItemGrid( DesktopRenderContext<?, ?> context, List<DesktopVirtualItem> items, int firstIndex, int x, int y, int columns, int rows );
    public static int virtualItemIndexAt(double mouseX, double mouseY, int x, int y, int columns, int rows, int firstIndex, int itemCount);
    public static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height);
    public static int rowsForCount(int count, int columns);
    public static int clamp(int value, int min, int max);
    public static boolean wantsTextInput(DesktopTextBoxState state);
    public static void unfocus(DesktopTextBoxState state);
    public record TextEditResult(boolean consumed, boolean changed) {
    }
}
```

## RecipeBrowserAccess

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserAccess.java).

```java
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
public interface RecipeBrowserAccess {
    RecipeBrowserSource source();
    boolean isAvailable();
    String filterText();
    void setFilterText(String text);
    List<RecipeBrowserTab> tabs();
    List<RecipeBrowserEntry> filteredEntries(RecipeBrowserTab tab);
    void renderTabIcon(GuiGraphicsExtractor graphics, RecipeBrowserTab tab, int x, int y);
    void render(GuiGraphicsExtractor graphics, RecipeBrowserEntry entry, int x, int y);
    List<Component> tooltip(RecipeBrowserEntry entry);
    RecipeBrowserEntry entryForItemStack(ItemStack stack);
    boolean matchesRecipeKey(int key);
    boolean matchesUsesKey(int key);
    void addLookupHistory(RecipeBrowserEntry entry);
    List<RecipeBrowserCategory> recipeCategories(RecipeBrowserEntry entry, RecipeBrowserMode mode);
    List<RecipeBrowserRecipe> recipes(RecipeBrowserEntry entry, RecipeBrowserMode mode, RecipeBrowserCategory category);
    List<RecipeBrowserEntry> craftingStations(RecipeBrowserCategory category);
    void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, RecipeBrowserCategory category, int x, int y);
    void renderRecipe(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY);
    void renderRecipeOverlays(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY);
    void renderRecipeSlotHighlights(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, List<Integer> inputIndexes, int color);
    void tickRecipe(RecipeBrowserRecipe recipe);
    boolean canBookmarkRecipe(RecipeBrowserRecipe recipe);
    boolean isRecipeBookmarked(RecipeBrowserRecipe recipe);
    void toggleRecipeBookmark(RecipeBrowserRecipe recipe);
    default @Nullable RecipeBrowserView expandedViewAt(RecipeBrowserRecipe recipe, int x, int y, double mouseX, double mouseY);
    boolean isRecipeSortStageEnabled(RecipeBrowserSortStage stage);
    void toggleRecipeSortStage(RecipeBrowserSortStage stage);
    RecipeBrowserTransferPlan recipeTransferPlan(RecipeBrowserRecipe recipe, AbstractContainerMenu menu);
    RecipeBrowserEntry recipeIngredientAt(RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY);
    default List<com.salts_inventory_update.api.client.desktop.DesktopWindowBounds> recipeIngredientBounds(RecipeBrowserRecipe recipe,int x,int y);
    boolean handleRecipeMouseScrolled(RecipeBrowserRecipe recipe, int x, int y, double mouseX, double mouseY, double scrollX, double scrollY);
    boolean handleRecipeMouseClicked(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, boolean doubleClick);
    boolean handleRecipeMouseReleased(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event);
    boolean handleRecipeMouseDragged(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, double dragX, double dragY);
}
```

## RecipeBrowserBridge

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserBridge.java).

```java
import java.util.LinkedHashMap;
import java.util.Map;
public final class RecipeBrowserBridge {
    public static synchronized RecipeBrowserAccess access();
    public static synchronized void install(RecipeBrowserSource source, RecipeBrowserAccess nextAccess);
    public static synchronized void clear(RecipeBrowserSource source, RecipeBrowserAccess currentAccess);
    public static synchronized void clear(RecipeBrowserSource source);
    public static synchronized void clear();
}
```

## RecipeBrowserCategory

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserCategory.java).

```java
import net.minecraft.network.chat.Component;
public record RecipeBrowserCategory(String uid, Component title, int width, int height, Object category) {
}
```

## RecipeBrowserEntry

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserEntry.java).

```java
public record RecipeBrowserEntry(String typeUid, Object type, Object ingredient, Object opaque) {
    public RecipeBrowserEntry(String typeUid, Object type, Object ingredient);
}
```

## RecipeBrowserMode

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserMode.java).

```java
public enum RecipeBrowserMode {
    INGREDIENTS, RECIPES, USES
}
```

## RecipeBrowserRecipe

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserRecipe.java).

```java
import org.jspecify.annotations.Nullable;
public record RecipeBrowserRecipe(String uid, String categoryUid, @Nullable String recipeId, int width, int height, Object recipe, Object layout) {
}
```

## RecipeBrowserSortStage

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserSortStage.java).

```java
public enum RecipeBrowserSortStage {
    BOOKMARKED, CRAFTABLE
}
```

## RecipeBrowserSource

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserSource.java).

```java
import java.util.Objects;
public record RecipeBrowserSource(String id, String displayName, int priority) {
    public RecipeBrowserSource(String id, String displayName, int priority);
}
```

## RecipeBrowserTab

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserTab.java).

```java
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
public record RecipeBrowserTab(String uid, Component title, @Nullable RecipeBrowserEntry icon, RecipeBrowserTabKind kind) {
    public RecipeBrowserTab(String uid, Component title, @Nullable RecipeBrowserEntry icon);
}
```

## RecipeBrowserTabKind

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserTabKind.java).

```java
public enum RecipeBrowserTabKind {
    INGREDIENTS, FAVORITES, RECENT
}
```

## RecipeBrowserTransferPlan

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserTransferPlan.java).

```java
import java.util.List;
public record RecipeBrowserTransferPlan(RecipeBrowserTransferRect button, List<Integer> recipeSlotIds, List<RecipeBrowserTransferSlot> requirements) {
    public RecipeBrowserTransferPlan(RecipeBrowserTransferRect button, List<Integer> recipeSlotIds, List<RecipeBrowserTransferSlot> requirements);
}
```

## RecipeBrowserTransferRect

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserTransferRect.java).

```java
public record RecipeBrowserTransferRect(int x, int y, int width, int height) {
}
```

## RecipeBrowserTransferSlot

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserTransferSlot.java).

```java
import java.util.List;
import net.minecraft.world.item.ItemStack;
public record RecipeBrowserTransferSlot(int inputIndex, int targetSlotId, List<ItemStack> alternatives) {
    public RecipeBrowserTransferSlot(int inputIndex, int targetSlotId, List<ItemStack> alternatives);
}
```

## RecipeBrowserView

`com.salts_inventory_update.api.client.recipebrowser`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/recipebrowser/RecipeBrowserView.java).

```java
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
public interface RecipeBrowserView {
    Component title();
    void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickProgress);
    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick);
    boolean mouseReleased(MouseButtonEvent event);
    boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY);
    boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY);
    default @Nullable RecipeBrowserEntry entryAt(double mouseX, double mouseY);
    default List<Component> tooltipAt(double mouseX, double mouseY);
    default void close();
}
```

## DesktopItemSources

`com.salts_inventory_update.api.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/desktop/DesktopItemSources.java).

```java
import java.util.function.Predicate;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
public final class DesktopItemSources {
    @FunctionalInterface public interface Resolver {
        @Nullable Source resolve(Player player, AbstractContainerMenu menu);
    }
    public record Source(int inventorySlot, String identity, ItemStack fingerprint, Predicate<ItemStack> identityMatcher) {
        public Source(int inventorySlot, String identity, ItemStack fingerprint);
        public Source(int inventorySlot, String identity, ItemStack fingerprint, Predicate<ItemStack> identityMatcher);
        @Override public ItemStack fingerprint();
    }
    public static void register(MenuType<?> type, Resolver resolver);
    public static boolean isInventorySlotLocked(Player player, int slot);
    public static boolean isSelectedSlotLocked(Player player);
    public static boolean isOffhandSlotLocked(Player player);
    public static boolean isHandLocked(Player player, InteractionHand hand);
    public static boolean isSlotLocked(Player player, @Nullable Slot slot);
    public static boolean hasLockedSourceInRange(Player player, int from, int to);
    public static boolean matchesLockedSource(Player player, ItemStack stack);
    public static boolean anyLockedSourceMatches(Player player, Predicate<ItemStack> predicate);
    public static boolean shouldRejectClick(Player player, AbstractContainerMenu menu, int slot, int button, ContainerInput type, ItemStack carried);
}
```

## DesktopMenuBindings

`com.salts_inventory_update.api.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/desktop/DesktopMenuBindings.java).

```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
public final class DesktopMenuBindings {
    public static void registerStorage(MenuType<?> type, java.util.function.BiPredicate<AbstractContainerMenu, Slot> slots);
    public static synchronized void register(MenuType<?> menuType, SlotAccess slotAccess, boolean requiresOpeningData);
    public static synchronized void register( MenuType<?> menuType, SlotAccess slotAccess, boolean requiresOpeningData, @Nullable SourceKeyResolver sourceKeyResolver );
    public static synchronized void register( MenuType<?> menuType, SlotAccess slotAccess, boolean requiresOpeningData, @Nullable SourceKeyResolver sourceKeyResolver, @Nullable SnapshotPreparer snapshotPreparer );
    public static synchronized void register( MenuType<?> menuType, SlotAccess slotAccess, boolean requiresOpeningData, @Nullable SourceKeyResolver sourceKeyResolver, @Nullable SnapshotPreparer snapshotPreparer, @Nullable GesturePolicy gesturePolicy );
    public static void prepareSnapshot(AbstractContainerMenu menu, List<ItemStack> items);
    public static int size(AbstractContainerMenu menu);
    public static @Nullable Slot slot(AbstractContainerMenu menu, int slotId);
    public static int indexOf(AbstractContainerMenu menu, Slot slot);
    public static List<Slot> all(AbstractContainerMenu menu);
    public static boolean isVirtualSlot(AbstractContainerMenu menu, Slot slot);
    public static boolean isPlayerInventorySlot(AbstractContainerMenu menu, Slot slot, net.minecraft.world.entity.player.Inventory inventory);
    public static boolean requiresOpeningData(AbstractContainerMenu menu);
    public static @Nullable String sourceKey(ServerPlayer player, AbstractContainerMenu menu, byte[] openingData);
    public static boolean canQuickCraft(AbstractContainerMenu menu, Slot slot, ItemStack carried);
    public static int quickCraftPlaceCount( AbstractContainerMenu menu, Slot slot, int targetCount, int quickCraftType, ItemStack carried );
    public static int quickCraftMaxStackSize(AbstractContainerMenu menu, Slot slot, ItemStack carried);
    public static boolean allowsPickupAll(AbstractContainerMenu menu, ItemStack carried, Slot slot);
    public static boolean allowsPickupAllAnchor(AbstractContainerMenu menu, ItemStack carried, Slot slot);
    public static int repeatedQuickMoveLimit(AbstractContainerMenu menu, Slot slot, ItemStack stack);
    public static boolean useNativeQuickMove(AbstractContainerMenu menu);
    public static boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu, Slot slot);
    public static Object physicalOwner(AbstractContainerMenu menu, Slot slot);
    public static int physicalIndex(AbstractContainerMenu menu, Slot slot);
    public static boolean isSamePhysicalInventory( AbstractContainerMenu firstMenu, Slot first, AbstractContainerMenu secondMenu, Slot second );
    public static boolean isSamePhysicalSlot( AbstractContainerMenu firstMenu, Slot first, AbstractContainerMenu secondMenu, Slot second );
    @FunctionalInterface public interface SlotAccess {
        int size(AbstractContainerMenu menu);
        default boolean isVirtualSlot(AbstractContainerMenu menu, Slot slot);
        default boolean isPlayerInventorySlot(AbstractContainerMenu menu, Slot slot, net.minecraft.world.entity.player.Inventory inventory);
        default Slot slot(AbstractContainerMenu menu, int slotId);
    }
    @FunctionalInterface public interface SourceKeyResolver {
        @Nullable String resolve(ServerPlayer player, AbstractContainerMenu menu, byte[] openingData);
    }
    @FunctionalInterface public interface SnapshotPreparer {
        void prepare(AbstractContainerMenu menu, List<ItemStack> items);
    }
    public interface GesturePolicy {
        default boolean canQuickCraft(AbstractContainerMenu menu, Slot slot, ItemStack carried);
        default int quickCraftPlaceCount( AbstractContainerMenu menu, Slot slot, int targetCount, int quickCraftType, ItemStack carried );
        default int quickCraftMaxStackSize(AbstractContainerMenu menu, Slot slot, ItemStack carried);
        default boolean allowsPickupAll(AbstractContainerMenu menu, ItemStack carried, Slot slot);
        default boolean allowsPickupAllAnchor(AbstractContainerMenu menu, ItemStack carried, Slot slot);
        default int repeatedQuickMoveLimit(AbstractContainerMenu menu, Slot slot, ItemStack stack);
        default boolean useNativeQuickMove(AbstractContainerMenu menu);
        default boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu, Slot slot);
        default Object physicalOwner(AbstractContainerMenu menu, Slot slot);
        default int physicalIndex(AbstractContainerMenu menu, Slot slot);
    }
}
```

## DesktopPayloadCodecs

`com.salts_inventory_update.api.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/desktop/DesktopPayloadCodecs.java).

```java
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
public final class DesktopPayloadCodecs {
    public static <P> byte[] encode(RegistryAccess registryAccess, StreamCodec<? super RegistryFriendlyByteBuf, P> codec, P payload);
    public static <P> P decode(RegistryAccess registryAccess, StreamCodec<? super RegistryFriendlyByteBuf, P> codec, byte[] data);
}
```

## DesktopSynchronization

`com.salts_inventory_update.api.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/desktop/DesktopSynchronization.java).

```java
import net.minecraft.world.inventory.ContainerSynchronizer;
public final class DesktopSynchronization {
    public static ContainerSynchronizer preserveDesktop(ContainerSynchronizer proposed);
}
```

## DesktopEnvironment

`com.salts_inventory_update.api.integration`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/integration/DesktopEnvironment.java).

```java
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
public final class DesktopEnvironment {
    public static boolean diagnosticsEnabled();
    public static boolean traceDiagnosticsEnabled();
    public static boolean isModLoaded(String id);
    public static void onServerStarting(Consumer<MinecraftServer> action);
    public static void registerIntegration(String owner, Runnable action);
}
```

## DesktopCompanionSpec

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopCompanionSpec.java).

```java
import java.util.Objects;
import java.util.function.Predicate;
import com.salts_inventory_update.api.desktop.DesktopInventoryIdentity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
public record DesktopCompanionSpec(DesktopInventoryIdentity identity, MenuProvider provider, Predicate<ServerPlayer> authorization) {
    public DesktopCompanionSpec(DesktopInventoryIdentity identity, MenuProvider provider, Predicate<ServerPlayer> authorization);
}
```

## DesktopServerApi

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopServerApi.java).

```java
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import com.salts_inventory_update.api.desktop.DesktopPayloadCodecs;
public final class DesktopServerApi {
    public static synchronized <T extends AbstractContainerMenu, S> void registerWindow( MenuType<T> menuType, DesktopServerWindowHandler<T, S> handler );
    public static synchronized <T extends AbstractContainerMenu, S> void replaceWindow( MenuType<T> menuType, DesktopServerWindowHandler<T, S> handler );
    public static synchronized <T extends AbstractContainerMenu> void registerPayload( MenuType<T> menuType, Identifier channel, DesktopServerPayloadHandler<T> handler );
    public static synchronized <T extends AbstractContainerMenu, P> void registerPayload( MenuType<T> menuType, Identifier channel, StreamCodec<? super RegistryFriendlyByteBuf, P> codec, DesktopTypedServerPayloadHandler<T, P> handler );
    @SuppressWarnings("unchecked") public static synchronized @Nullable DesktopServerPayloadHandler<AbstractContainerMenu> findPayloadHandler(MenuType<?> menuType, Identifier channel);
    @SuppressWarnings("unchecked") public static synchronized @Nullable DesktopServerWindowHandler<AbstractContainerMenu, Object> findWindowHandler(MenuType<?> menuType);
    public static synchronized <T extends AbstractContainerMenu> void registerOpeningData(MenuType<T> type, OpeningDataWriter<T> writer);
    @SuppressWarnings("unchecked") public static synchronized @Nullable OpeningDataWriter<AbstractContainerMenu> findOpeningDataWriter(MenuType<?> type);
    @FunctionalInterface public interface OpeningDataWriter<T extends AbstractContainerMenu> {
        void write(net.minecraft.server.level.ServerPlayer player, T menu, net.minecraft.network.RegistryFriendlyByteBuf buffer);
    }
    public static boolean supportsWindow(net.minecraft.server.level.ServerPlayer player, AbstractContainerMenu menu);
    public static synchronized boolean hasWindowSupport(MenuType<?> menuType);
}
```

## DesktopServerPayloadContext

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopServerPayloadContext.java).

```java
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
public interface DesktopServerPayloadContext<T extends AbstractContainerMenu> extends DesktopServerSessionContext<T, Object> {
    Identifier channel();
    byte[] data();
    default void reject(String reason);
}
```

## DesktopServerPayloadHandler

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopServerPayloadHandler.java).

```java
import net.minecraft.world.inventory.AbstractContainerMenu;
public interface DesktopServerPayloadHandler<T extends AbstractContainerMenu> {
    void handle(DesktopServerPayloadContext<T> context);
}
```

## DesktopServerSessionContext

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopServerSessionContext.java).

```java
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
public interface DesktopServerSessionContext<T extends AbstractContainerMenu, S> {
    ServerPlayer player();
    T menu();
    int sessionId();
    String sourceKey();
    boolean visible();
    S state();
    void sendToClient(Identifier channel, byte[] data);
    default <P> void sendToClient(Identifier channel, P payload, StreamCodec<? super RegistryFriendlyByteBuf, P> codec);
    java.util.Optional<DesktopCompanionLease> acquireCompanion(DesktopCompanionSpec spec);
    default net.minecraft.world.item.ItemStack carriedStack();
    default void setCarriedStack(net.minecraft.world.item.ItemStack stack);
    void broadcastChanges();
}
```

## DesktopServerWindowHandler

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopServerWindowHandler.java).

```java
import org.jspecify.annotations.Nullable;
import net.minecraft.world.inventory.AbstractContainerMenu;
public interface DesktopServerWindowHandler<T extends AbstractContainerMenu, S> {
    default boolean supports(net.minecraft.server.level.ServerPlayer player, T menu);
    default boolean usesNativePlayerInventoryQuickMove(T menu);
    default @Nullable S createState(DesktopServerSessionContext<T, S> context);
    default void opened(DesktopServerSessionContext<T, S> context);
    default void tick(DesktopServerSessionContext<T, S> context);
    default void closed(DesktopServerSessionContext<T, S> context);
    default void visibilityChanged(DesktopServerSessionContext<T, S> context, boolean visible);
    default DesktopTransferDecision validateTransfer(DesktopTransferRequest<T, S> request);
}
```

## DesktopSessions

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopSessions.java).

```java
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
public final class DesktopSessions {
    public static boolean shouldCapture(ServerPlayer player);
    public static @Nullable OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> openingData);
    public static void withSessionTransition(ServerPlayer player, int sessionId, Runnable action);
    public static int activeNetworkSessionId(ServerPlayer player);
    public static boolean hasActiveSessionTransition(ServerPlayer player);
    public static boolean hasOpenSessionMatching(Player player, Predicate<AbstractContainerMenu> predicate);
    public static String blockSourceKey(ServerPlayer player, BlockPos position);
    public static boolean sendSessionPayload(ServerPlayer player, int sessionId, Identifier channel, byte[] data);
}
```

## DesktopTransferDecision

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopTransferDecision.java).

```java
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
public final class DesktopTransferDecision {
    public enum Status {
        UNSUPPORTED, DENIED, ALLOWED
    }
    public static DesktopTransferDecision unsupported();
    public static DesktopTransferDecision denied(String reason);
    public static DesktopTransferDecision allowed( List<DesktopTransferRequirement> requirements, List<Integer> destinationSlots, int maximumCrafts );
    public Status status();
    public List<DesktopTransferRequirement> requirements();
    public List<Integer> destinationSlots();
    public int maximumCrafts();
    public String reason();
    public boolean supported();
    public boolean allowed();
}
```

## DesktopTransferRequest

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopTransferRequest.java).

```java
import java.util.Objects;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.Recipe;
public record DesktopTransferRequest<T extends AbstractContainerMenu, S>( DesktopServerSessionContext<T, S> session, Recipe<?> recipe, boolean maxTransfer ) {
    public DesktopTransferRequest( DesktopServerSessionContext<T, S> session, Recipe<?> recipe, boolean maxTransfer );
}
```

## DesktopTransferRequirement

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopTransferRequirement.java).

```java
import java.util.List;
import net.minecraft.world.item.ItemStack;
public record DesktopTransferRequirement(int targetSlotId, int count, List<ItemStack> alternatives) {
    public DesktopTransferRequirement(int targetSlotId, int count, List<ItemStack> alternatives);
    @Override public List<ItemStack> alternatives();
}
```

## DesktopTransferValidators

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopTransferValidators.java).

```java
import java.lang.reflect.Method;
import java.util.Objects;
public final class DesktopTransferValidators {
    public static boolean supports(DesktopServerWindowHandler<?, ?> handler);
    public static DesktopTransferDecision validate( DesktopServerWindowHandler<?, ?> handler, DesktopTransferRequest<?, ?> request );
}
```

## DesktopTypedServerPayloadHandler

`com.salts_inventory_update.api.server.desktop`. Native 26.3; available with the version-specific types described above. [Source](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop/DesktopTypedServerPayloadHandler.java).

```java
import net.minecraft.world.inventory.AbstractContainerMenu;
public interface DesktopTypedServerPayloadHandler<T extends AbstractContainerMenu, P> {
    void handle(DesktopServerPayloadContext<T> context, P payload);
}
```
