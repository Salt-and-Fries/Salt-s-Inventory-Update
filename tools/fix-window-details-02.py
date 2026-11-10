"""Compatibility-only fixes for camera, storage, binding and Sculptor windows."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'build/window-detail-fixes-02'
changed = []


def save(path, value):
    previous = path.read_text() if path.exists() else None
    if previous == value:
        return
    backup = WORK / 'before' / path.relative_to(ROOT)
    if previous is not None and not backup.exists():
        backup.parent.mkdir(parents=True, exist_ok=True)
        backup.write_bytes(path.read_bytes())
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(value)
    changed.append(str(path.relative_to(ROOT)))


decoration = '''    /** Keep the full decoration workspace, including its preview and native output rim. */
    public static DesktopNativeLayout decorationTable(DesktopNativeView view) {
        var players=view.slots().stream().filter(DesktopNativeView.Slot::player).toList();
        int playerTop=players.stream().mapToInt(s->s.bounds().y()).min().orElse(view.imageBounds().height());
        int playerLeft=players.stream().mapToInt(s->s.bounds().x()).min().orElse(45);
        var owned=view.slots().stream().filter(s->!s.player()&&s.active()).map(DesktopNativeView.Slot::bounds).toList();
        int top=owned.stream().mapToInt(DesktopWindowBounds::y).min().orElse(17)-1;
        // The preview extends below the last item cell. The inventory begins
        // five pixels after the native machine body's bottom edge.
        var body=box(6,top,view.imageBounds().width()-12,playerTop-5-top);
        return DesktopNativeLayout.builder().keep("decoration-workspace",body,0,0)
            .hide(box(playerLeft-1,playerTop-12,86,9)).build();
    }

'''

for mc in ['1.20.1', '1.21.1', '1.21.11', '26.1.2', '26.2', '26.3']:
    resources = ROOT / f'compatibility/versions/{mc}/common/src/main/resources'
    path = resources / 'salts_inventory_compat/native-windows/project-everything.json'
    data = json.loads(path.read_text())
    camera = next(p for p in data if p['id'] == 'exposure-camera')
    camera['menus'] = ['exposure:camera', 'exposure:camera_in_hand', 'exposure:camera_on_stand']
    camera['screens'] = ['io.github.mortuusars.exposure.client.gui.screen.camera.CameraAttachmentsScreen']
    binding = next(p for p in data if p['id'] == 'spell-engine-spell-binding')
    # Spell Engine draws this texture 18px above its menu image origin.
    # These are screen coordinates, rather than atlas coordinates.
    binding['regions'] = [dict(source=[13, -4, 172, 74], at=[0, 0], cutouts=[])]
    save(path, json.dumps(data, indent=2) + '\n')
    java = ROOT / f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/nativewindows'
    path = java / 'NativeMachineLayouts.java'
    text = path.read_text()
    if 'DesktopNativeLayout decorationTable(' not in text:
        text = text.replace('    public static DesktopNativeLayout bartering(', decoration + '    public static DesktopNativeLayout bartering(')
    save(path, text)
    path = java / 'ReviewedNativeWindows.java'
    text = path.read_text()
    marker = '        NativeWindowPolish.install("visualworkbench","visualworkbench:crafting",NativeBatchLayouts::visualWorkbench);'
    addition = '\n        NativeWindowPolish.install("sophisticatedstorage","sophisticatedstorage:decoration_table",NativeMachineLayouts::decorationTable);'
    assert marker in text
    if addition not in text:
        text = text.replace(marker, marker + addition)
    save(path, text)

paths = list((ROOT / 'compatibility/versions').glob('*/neoforge/src/main/java/com/salts_inventory_update/compat/sophisticated/client/SophisticatedHostedScreenDefinition.java'))
paths += list((ROOT / 'compatibility/sophisticated-fabric/versions').glob('*/src/main/java/com/salts_inventory_update/compat/sophisticated/client/SophisticatedHostedScreenDefinition.java'))
paths += [ROOT / 'compatibility/versions/1.20.1/forge/src/main/java/com/salts_inventory_update/compat/sophisticated/client/SophisticatedHostedScreenDefinition.java']
assert len(paths) == 8
for path in paths:
    text = path.read_text()
    marker = '            state.legacyScrollbarGutterMigrationPending = false;'
    addition = '''
            // Older saved windows included the rows but omitted the toolbar
            // height. Restore that known size before deciding to add a gutter.
            if (context.model() instanceof StorageContainerMenuBase<?> storage
                    && context.windowHeight() == SALT_VERTICAL_CHROME + storage.getNumberOfRows() * SLOT) {
                var complete = DesktopWindowSize.of(context.windowWidth(),
                    SALT_VERTICAL_CHROME + standardContentHeight(storage.getNumberOfRows()));
                return desiredSizeWithClosedTabs(context, state, complete);
            }'''
    assert marker in text
    if addition not in text:
        text = text.replace(marker, marker + addition)
    save(path, text)

mixin = '''package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The hosted menu snapshot may arrive before this screen creates its button. */
@Pseudo
@Mixin(targets="com.bobmowzie.mowziesmobs.client.gui.GuiSculptorTrade", remap=false)
abstract class SculptorButtonStateMixin {
    @Inject(method={"init", "m_7856_"}, at=@At("RETURN"), require=1)
    private void salts$refreshInitialEligibility(CallbackInfo ci) {
        var menu=((AbstractContainerScreen<?>)(Object)this).getMenu();
        var inventory=ForeignMenuAccess.invoke(menu,"getInventorySculptor");
        // Delegate payment and obstruction rules to the original mod.
        ForeignMenuAccess.invoke(this,"onChange",new Class<?>[]{Container.class},inventory);
    }
}
'''
for mc in ['1.20.1', '1.21.1']:
    java = ROOT / f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/mixin'
    save(java / 'SculptorButtonStateMixin.java', mixin)
    path = ROOT / f'compatibility/versions/{mc}/common/src/main/resources/salts_inventory_compat.reviewed-native.mixins.json'
    data = json.loads(path.read_text())
    if 'SculptorButtonStateMixin' not in data['client']:
        data['client'].append('SculptorButtonStateMixin')
    save(path, json.dumps(data, indent=2) + '\n')

(WORK / 'source-changes.json').write_text(json.dumps(changed, indent=2))
print('Updated', len(changed), 'compatibility source/resource files; Core API unchanged.')
