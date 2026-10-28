package com.salts_inventory_update.compat.graveyard;

import com.salts_inventory_update.compat.nativewindows.*;

public final class GraveyardNativeUi {
    private GraveyardNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("graveyard","graveyard:ossuary_screen_handler",OssuarySync.Template::new);
        NativeWindowPolish.installTemplate("graveyard","graveyard:ossuary",OssuarySync.Template::new);
        // Urns/coffins/sarcophagi use the existing compact vanilla 54-slot storage window.
        // GravestoneScreen intentionally remains the ordinary native sign-style editor.
    }
}
