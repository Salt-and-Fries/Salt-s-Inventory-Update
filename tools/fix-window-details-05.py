"""Separate Configurator controls/filter slot and retain AdditionZ's South glyph."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/window-detail-fixes-05'
WORK.mkdir(exist_ok=True)
changes=[]
def save(path,text):
    if path.read_text()==text:return
    backup=WORK/'before'/path.relative_to(ROOT)
    if not backup.exists():
        backup.parent.mkdir(parents=True,exist_ok=True);backup.write_bytes(path.read_bytes())
    path.write_text(text);changes.append(str(path.relative_to(ROOT)))

configurator='''    public static final class InventoryConfigurator implements Definition<AbstractContainerMenu, Void> {
        private static final int CONTENT_WIDTH=160, CONTENT_HEIGHT=108;
        private static final int FILTER_X=84, FILTER_Y=46, CLEAR_X=143;

        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> context) {
            return "configurator-separated-column-2";
        }
        @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(CONTENT_WIDTH,CONTENT_HEIGHT);
        }
        @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu,Void> context) {
            return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(CONTENT_WIDTH,CONTENT_HEIGHT);
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> context) {
            int x=context.contentX(),y=context.contentY();
            int fx=x+FILTER_X,fy=y+FILTER_Y;
            // Preserve the publisher's complete empty-filter guide. Painting a
            // regular slot over it would erase the checker icon in the centre.
            context.texture(TomsStorageCompat.id("textures/gui/inventory_configurator.png"),
                fx-1,fy-1,79,31,18,18,18,18,256,256);
            context.texturelessSlot(0,fx,fy);
            renderCycleButton(context,x+1,y+1,priorityIcon(context.model()),false);
            renderCycleButton(context,x+1,y+19,TomsStorageCompat.id("icons/add"),false);
            renderCycleButton(context,x+1,y+37,TomsStorageCompat.id("icons/deny"),false);
            renderCycleButton(context,x+1,y+55,sideIcon(context.model()),false);
            renderCycleButton(context,x+1,y+73,TomsStorageCompat.id(skip(context.model())?"icons/skip_inv":"icons/include_inv"),skip(context.model()));
            renderCycleButton(context,x+1,y+91,TomsStorageCompat.id(keepLast(context.model())?"icons/keep_last_1":"icons/keep_last_off"),keepLast(context.model()));
            renderCycleButton(context,x+CLEAR_X,y+1,TomsStorageCompat.id("icons/deny"),false);
        }
        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,Void> context,double x,double y) {
            return context.menuSlotHit(0,context.contentX()+FILTER_X,context.contentY()+FILTER_Y,x,y);
        }
        private static int controlAt(DesktopWindowContext<AbstractContainerMenu,Void> context,double x,double y) {
            int left=context.contentX()+1,top=context.contentY()+1;
            for(int i=0;i<6;i++)if(contains(x,y,left,top+i*18,BUTTON,BUTTON))return i;
            return contains(x,y,context.contentX()+CLEAR_X,top,BUTTON,BUTTON)?6:-1;
        }
        private static boolean editableFilter(ItemStack stack) {
            try {
                return Class.forName("com.tom.storagemod.item.IItemFilter",false,
                    stack.getItem().getClass().getClassLoader()).isInstance(stack.getItem());
            } catch(ClassNotFoundException absent) { return false; }
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> context,MouseButtonEvent event,boolean twice) {
            if(event.button()==MOUSE_RIGHT && contains(event.x(),event.y(),
                    context.contentX()+FILTER_X-1,context.contentY()+FILTER_Y-1,18,18)
                    && editableFilter(context.model().getSlot(0).getItem()))
                return context.sendMenuButton(6);
            if(event.button()!=MOUSE_LEFT)return false;
            int index=controlAt(context,event.x(),event.y());
            return switch(index) {
                case 0 -> context.sendMenuButton(((TomsStorageReflect.enumOrdinalField(context.model(),"priority",2)+1)%5)<<3);
                case 1 -> context.sendMenuButton(1);
                case 2 -> context.sendMenuButton(2);
                case 3 -> context.sendMenuButton((((TomsStorageReflect.enumOrdinalField(context.model(),"side",0)+1)%6)<<3)|3);
                case 4 -> context.sendMenuButton(((skip(context.model())?0:1)<<3)|4);
                case 5 -> context.sendMenuButton(((keepLast(context.model())?0:1)<<3)|5);
                case 6 -> context.sendMenuButton(7);
                default -> false;
            };
        }
        @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,Void> context,int x,int y) {
            String key=switch(controlAt(context,x,y)) {
                case 0 -> "tooltip.toms_storage.priority_"+TomsStorageReflect.enumNameField(context.model(),"priority","NORMAL").toLowerCase(Locale.ROOT);
                case 1 -> "tooltip.toms_storage.inv_config.add_connected";
                case 2 -> "tooltip.toms_storage.inv_config.remove_all_connected";
                case 3 -> "tooltip.toms_storage.side_"+TomsStorageReflect.enumNameField(context.model(),"side","DOWN").toLowerCase(Locale.ROOT);
                case 4 -> skip(context.model())?"tooltip.toms_storage.inv_config.skip":"tooltip.toms_storage.inv_config.include";
                case 5 -> keepLast(context.model())?"tooltip.toms_storage.keepLast_on":"tooltip.toms_storage.keepLast_off";
                case 6 -> "tooltip.toms_storage.inv_config.remove_filter";
                default -> null;
            };
            if(key==null)return false;
            context.tooltip(Component.translatable(key),x,y);return true;
        }
    }

'''

for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    root=ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat'
    if mc!='1.20.1':
        path=root/'toms_storage/client/TomsStorageDesktopWindows.java';text=path.read_text()
        start=text.index('    public static final class InventoryConfigurator implements')
        end=text.index('    public static final class LevelEmitter implements',start)
        modern='import com.mojang.blaze3d.platform.InputConstants;' in text
        left='InputConstants.MOUSE_BUTTON_LEFT' if modern else 'GLFW.GLFW_MOUSE_BUTTON_LEFT'
        right='InputConstants.MOUSE_BUTTON_RIGHT' if modern else 'GLFW.GLFW_MOUSE_BUTTON_RIGHT'
        save(path,text[:start]+configurator.replace('MOUSE_LEFT',left).replace('MOUSE_RIGHT',right)+text[end:])
    path=root/'nativewindows/NativeBatchLayouts.java';text=path.read_text()
    old='chunkLoader(DesktopNativeView view) { return crop(view,4,5,165,67); }'
    new='chunkLoader(DesktopNativeView view) { return crop(view,4,5,165,77); }'
    assert old in text or new in text,path
    save(path,text.replace(old,new))

manifest=WORK/'source-changes.json'
previous=json.loads(manifest.read_text()) if manifest.exists() else []
manifest.write_text(json.dumps(sorted(set(previous+changes)),indent=2))
print('Updated',len(changes),'Compatibility sources; legacy nine-slot configurator and Core/API unchanged.')
