"""Register changed native menu IDs and repair Filing Cabinet geometry/scrolling."""
from pathlib import Path
import copy
import json

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'build/window-detail-fixes-03'
changes = []


def save(path, value):
    previous = path.read_text()
    if previous == value:
        return
    backup = WORK / 'before' / path.relative_to(ROOT)
    if not backup.exists():
        backup.parent.mkdir(parents=True, exist_ok=True)
        backup.write_bytes(path.read_bytes())
    path.write_text(value)
    changes.append(str(path.relative_to(ROOT)))


template = json.loads((ROOT / 'compatibility/versions/1.21.1/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json').read_text())
pigment = copy.deepcopy(next(p for p in template if p['id'] == 'spawn-pigment-shifter'))
pigment['regions'][0]['source'] = [7, 18, 169, 74]

filing = '''    public static final class FilingCabinet implements Definition<AbstractContainerMenu, FilingCabinetState> {
        private static final int COLUMNS = 9, ROWS = 5, GAP = 4;
        private static final int GRID_HEIGHT = ROWS * SLOT;

        @Override public FilingCabinetState createState(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return new FilingCabinetState();
        }

        @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(
                COLUMNS * SLOT + GAP + DesktopWidgets.SCROLLBAR_BACKGROUND_WIDTH, GRID_HEIGHT);
        }

        private static int maximum(AbstractContainerMenu menu) {
            // The menu exposes only 45 cells; its backing cabinet has many
            // more. Use the publisher's capacity, just as its own screen does.
            int capacity=((Number)com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess.invoke(menu,"getContainerSize")).intValue();
            return Math.max(0,(capacity+COLUMNS-1)/COLUMNS-ROWS);
        }

        @Override public void render(DesktopRenderContext<AbstractContainerMenu,FilingCabinetState> context) {
            int x=context.contentX()+1, y=context.contentY()+1;
            for(int row=0;row<ROWS;row++)for(int col=0;col<COLUMNS;col++)
                context.slot(row*COLUMNS+col,x+col*SLOT,y+row*SLOT);
            renderScrollbar(context,context.contentX()+COLUMNS*SLOT+GAP,context.contentY(),
                GRID_HEIGHT,context.state().scrollRow,maximum(context.model()));
        }

        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,FilingCabinetState> context,double mouseX,double mouseY) {
            int x=context.contentX()+1, y=context.contentY()+1;
            for(int i=0;i<ROWS*COLUMNS;i++) {
                var hit=context.menuSlotHit(i,x+i%COLUMNS*SLOT,y+i/COLUMNS*SLOT,mouseX,mouseY);
                if(hit!=null)return hit;
            }
            return null;
        }

        private static boolean row(DesktopInputContext<AbstractContainerMenu,FilingCabinetState> context,int requested) {
            int next=clamp(requested,0,maximum(context.model()));
            if(next==context.state().scrollRow)return true;
            if(!context.sendMenuButton(next))return false;
            // The native server pages its backing cabinet and synchronizes
            // the 45 visible slots. Keep the client projection at offset zero:
            // an offset on both sides makes padding cells on the final partial
            // page write beyond the publisher's client-side backing inventory.
            context.state().scrollRow=next;
            return true;
        }

        @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu,FilingCabinetState> context,
                double mouseX,double mouseY,double scrollX,double scrollY) {
            if(scrollY==0||maximum(context.model())==0)return false;
            return row(context,context.state().scrollRow+(scrollY<0?1:-1));
        }

        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,FilingCabinetState> context,
                MouseButtonEvent event,boolean twice) {
            int x=context.contentX()+COLUMNS*SLOT+GAP, y=context.contentY();
            int max=maximum(context.model());
            if(event.button()!=InputConstants.MOUSE_BUTTON_LEFT||max==0||event.x()<x||event.x()>=x+DesktopWidgets.SCROLLBAR_BACKGROUND_WIDTH
                    ||event.y()<y||event.y()>=y+GRID_HEIGHT)return false;
            int travel=GRID_HEIGHT-2-DesktopWidgets.SCROLLBAR_THUMB_HEIGHT;
            int thumb=y+1+Math.round((float)context.state().scrollRow/max*travel);
            context.state().scrollGrab=event.y()>=thumb&&event.y()<thumb+DesktopWidgets.SCROLLBAR_THUMB_HEIGHT
                ? event.y()-thumb : DesktopWidgets.SCROLLBAR_THUMB_HEIGHT/2.0;
            context.state().draggingScrollbar=true;
            return drag(context,event.y());
        }

        private static boolean drag(DesktopInputContext<AbstractContainerMenu,FilingCabinetState> context,double y) {
            int travel=GRID_HEIGHT-2-DesktopWidgets.SCROLLBAR_THUMB_HEIGHT;
            double fraction=(y-context.contentY()-1-context.state().scrollGrab)/travel;
            return row(context,(int)Math.round(fraction*maximum(context.model())));
        }

        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,FilingCabinetState> context,
                MouseButtonEvent event,double dx,double dy) {
            return context.state().draggingScrollbar&&event.button()==InputConstants.MOUSE_BUTTON_LEFT&&drag(context,event.y());
        }

        @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,FilingCabinetState> context,MouseButtonEvent event) {
            if(!context.state().draggingScrollbar||event.button()!=InputConstants.MOUSE_BUTTON_LEFT)return false;
            context.state().draggingScrollbar=false;
            return true;
        }

        @Override public List<com.salts_inventory_update.api.client.desktop.DesktopScrollRegion> controllerScrollRegions(
                DesktopWindowContext<AbstractContainerMenu,FilingCabinetState> context) {
            return List.of(new com.salts_inventory_update.api.client.desktop.DesktopScrollRegion(context.contentX(),context.contentY(),
                COLUMNS*SLOT,GRID_HEIGHT,SLOT,context.state().scrollRow>0,context.state().scrollRow<maximum(context.model())));
        }
    }

'''

for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    path=ROOT/f'compatibility/versions/{mc}/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json'
    data=json.loads(path.read_text())
    mount=next(p for p in data if p['id']=='pr-mythic-direwolf-inventory')
    mount['menus']=['mythicmounts:mountscreen','mythicmounts:mount_inventory']
    basket=next(p for p in data if p['id']=='supplementaries-lunch-basket')
    basket['menus']=['supplementaries:variable_size','supplementaries:lunch_basket','supplementaries:sack']
    found=next((p for p in data if p['id']=='spawn-pigment-shifter'),None)
    if found is None:data.append(copy.deepcopy(pigment))
    else:found['regions']=copy.deepcopy(pigment['regions'])
    save(path,json.dumps(data,indent=2)+'\n')

    path=ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/toms_storage/client/TomsStorageDesktopWindows.java'
    text=path.read_text()
    start=text.index('    public static final class FilingCabinet implements')
    end=text.index('    public static final class TerminalState',start)
    version_filing=filing if 'import com.mojang.blaze3d.platform.InputConstants;' in text else filing.replace('InputConstants.MOUSE_BUTTON_LEFT','GLFW.GLFW_MOUSE_BUTTON_LEFT')
    text=text[:start]+version_filing+text[end:]
    old='    public static final class FilingCabinetState {\n        private int scrollRow;\n    }'
    new='    public static final class FilingCabinetState {\n        private int scrollRow;\n        private boolean draggingScrollbar;\n        private double scrollGrab;\n    }'
    assert old in text or new in text
    save(path,text.replace(old,new))

manifest=WORK/'source-changes.json'
previous=json.loads(manifest.read_text()) if manifest.exists() else []
manifest.write_text(json.dumps(sorted(set(previous+changes)),indent=2))
print('Updated',len(changes),'Compatibility resources/sources; Core API unchanged.')
