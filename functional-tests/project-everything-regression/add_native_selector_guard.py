"""Apply the per-instance native selector veto without changing public API."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    path=ROOT/f'versions/{mc}/fabric/src/main/java/com/salts_inventory_update/server/DesktopContainerSessions.java'
    text=path.read_text()
    start=text.index('    private static boolean isDesktopSupportedMenu(')
    at=text.index('        if (isKnownVanillaDesktopMenu(type)) {',start)
    guard='''        // An explicit per-instance rejection also applies to vanilla-shaped
        // menus. Virtual selectors may intentionally use a vanilla chest type.
        if (DesktopServerApi.findWindowHandler(type) != null && !DesktopServerApi.supportsWindow(player, menu)) {
            return false;
        }

'''
    if guard not in text:text=text[:at]+guard+text[at:]
    path.write_text(text)
    path=ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/ReviewedNativeWindows.java'
    text=path.read_text()
    old='''            for (String id : NATIVE_ONLY) {
'''
    new='''            var nativeTypes = new HashSet<>(NATIVE_ONLY);
            // Polymer's MicroUi presents category/action icons in chest cells;
            // it is a native browser, not a physical storage container.
            if (DesktopEnvironment.isModLoaded("polymer-core"))
                for (int rows = 1; rows <= 6; rows++) nativeTypes.add("minecraft:generic_9x" + rows);
            for (String id : nativeTypes) {
'''
    if new not in text:
        assert text.count(old)==1,path
        text=text.replace(old,new)
        old='@Override public boolean supports(net.minecraft.server.level.ServerPlayer player, AbstractContainerMenu menu) { return false; }'
        new='''@Override public boolean supports(net.minecraft.server.level.ServerPlayer player, AbstractContainerMenu menu) {
                            var key = BuiltInRegistries.MENU.getKey(menu.getType());
                            return key != null && !NATIVE_ONLY.contains(key.toString())
                                && !menu.getClass().getName().equals("eu.pb4.polymer.core.impl.ui.MicroUi$InternalScreenHandler");
                        }'''
        assert text.count(old)==1,path
        text=text.replace(old,new)
    path.write_text(text)
