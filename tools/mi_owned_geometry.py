"""Keep every MI geometry check while excluding the exact global FTB sidebar."""
import copy

FOREIGN = 'dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton'


def check_owned(check, state):
    scoped = copy.deepcopy(state)
    for window in scoped.get('windows', scoped.get('allWindows', [])):
        window['nativeWidgets'] = [widget for widget in window.get('nativeWidgets', []) if widget.get('type') != FOREIGN]
    return check(scoped)
