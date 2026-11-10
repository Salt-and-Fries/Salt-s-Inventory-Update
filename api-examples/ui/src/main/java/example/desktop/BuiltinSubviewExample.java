package example.desktop;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Additive API example: bind a native menu to an existing Character subview. */
public final class BuiltinSubviewExample<T extends AbstractContainerMenu> {
    private final Map<Integer, DesktopEmbeddedSession<T>> sessions = new HashMap<>();
    private UUID requestedOwner;

    /** Call before the integration's authenticated native opener requests the menu. */
    public void requestFor(UUID characterWindow) { requestedOwner = characterWindow; }

    public void register(MenuType<T> nativeType) {
        DesktopClientApi.registerEmbeddedSession(nativeType, new DesktopEmbeddedSessionHandler<T>() {
            @Override public boolean opened(DesktopEmbeddedSession<T> session) {
                // A response must still have its original, visible presentation owner.
                boolean ownerExists = requestedOwner != null && DesktopClientApi.currentWorkspace()
                    .map(workspace -> workspace.windows().stream()
                        .anyMatch(window -> window.handle().instanceId().equals(requestedOwner)))
                    .orElse(false);
                if (!ownerExists) return false;
                sessions.put(session.sessionId(), session);
                return true;
            }

            @Override public void closed(int sessionId) { sessions.remove(sessionId); }
        });
    }

    /** Use the native slot's real ID; Core validates the live session and cell. */
    public DesktopSessionSlotHit hit(DesktopEmbeddedSession<T> session, DesktopSlotHit nativeHit) {
        return session.isOpen() ? new DesktopSessionSlotHit(session.sessionId(), nativeHit) : null;
    }

    /** Menu close is a normal authenticated server request, never a local disposal. */
    public void closeSubview() {
        requestedOwner = null;
        for (var session : java.util.List.copyOf(sessions.values())) session.close();
    }

    /** Opt-in geometry example: Core keeps its frame and transport. */
    public static final class RelocatedArmor implements DesktopBuiltinAttachment {
        @Override public Optional<DesktopWindowBounds> builtinSlotBounds(
                DesktopWindowContext<AbstractContainerMenu, Void> context, int menuSlotId) {
            if (menuSlotId < 5 || menuSlotId > 8) return Optional.empty();
            return Optional.of(new DesktopWindowBounds(context.contentX() + 59,
                context.contentY() + 8 + (menuSlotId - 5) * 18, 16, 16));
        }
    }
}
