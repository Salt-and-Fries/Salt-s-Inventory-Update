package example.companion;

/** Optional observer for the runnable fixture. The normal mod/backend never resolves Salt classes. */
public final class CompanionUiHooks {
    public interface Observer {
        default void opened(Object callbackContext) { }
        default void tick(Object callbackContext) { }
        default void closed(Object callbackContext) { }
        default void frame() { }
        default void controls(Object callbackContext, java.util.List<?> states) { }
    }
    public static Observer observer = new Observer() { };
    private CompanionUiHooks() { }
}
