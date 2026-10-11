package example.desktop;

import java.util.function.BiPredicate;

/** The normal client entrypoint calls this and opens its usual screen when it returns false. */
public final class ExampleUiHooks {
    public static BiPredicate<String, PreferencesModel> openStyledPreferences = (key, model) -> false;
    public static BiPredicate<String, PreferencesModel> openPreferences = (key, model) -> false;
}
