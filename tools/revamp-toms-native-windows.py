"""Restore the publisher's Tom's Storage utility layouts without changing Core/API."""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/toms-native-window-revamp'
WORK.mkdir(exist_ok=True)

emitter='''    /** Native two-bank amount controls and editable count; only the inventory is detached. */
    public static final class LevelEmitter implements Definition<AbstractContainerMenu, LevelEmitterState>, com.salts_inventory_update.api.client.desktop.DesktopWindowPresentation {
        private static final int[] AMOUNTS={1,10,100,1000}, SHIFT_AMOUNTS={1,16,32,64};
        private static final int[] BUTTON_X={14,39,69,104}, BUTTON_W={20,25,30,35};
        @Override public LevelEmitterState createState(DesktopWindowSetupContext<AbstractContainerMenu> c) { return new LevelEmitterState(); }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) { return "native-emitter-two-banks-1"; }
        @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> c) { return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(164,62); }
        @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) { return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(164,62); }
        private int nativeCount(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) { return EMITTER_COUNT; }
        private boolean nativeBelow(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) { return EMITTER_BELOW; }
        private int count(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) { return c.state().pendingCount>=0?c.state().pendingCount:nativeCount(c); }
        private boolean below(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) { return c.state().pendingCount>=0?c.state().pendingBelow:nativeBelow(c); }
        @Override public void tick(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) {
            var s=c.state();
            // Core serializes optimistic mutations. Coalesce a pasted value
            // while its first digit is awaiting acknowledgement, then deliver
            // the final value through the same authenticated public API.
            if(s.pendingSend!=null&&com.salts_inventory_update.api.client.desktop.DesktopClientApi.canDispatchOptimisticMutationNow())s.pendingSend.run();
            if(s.pendingSend==null&&s.pendingCount==nativeCount(c)&&s.pendingBelow==nativeBelow(c))s.pendingCount=-1;
        }
        EMITTER_SNAPSHOT
        @Override public java.util.List<com.salts_inventory_update.api.client.desktop.DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> c) { return leftNativeControl(); }
        @Override @SuppressWarnings("unchecked") public void renderBehindFrame(DesktopRenderContext<?,?> raw) {
            var c=(DesktopRenderContext<AbstractContainerMenu,LevelEmitterState>)raw;
            renderCycleButton(c,c.windowX()-18,c.windowY()+5,TomsStorageCompat.id(below(c)?"icons/less_than":"icons/greater_than"),below(c));
        }
        @Override public java.util.List<com.salts_inventory_update.api.client.desktop.DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) {
            return List.of(new com.salts_inventory_update.api.client.desktop.DesktopWindowBounds(c.contentX()+63,c.contentY()+25,61,12));
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,LevelEmitterState> c) {
            int x=c.contentX(),y=c.contentY(); var s=c.state();
            // Retain the exact original cell and amount-field rims from the installed atlas.
            c.texture(TomsStorageCompat.id("textures/gui/level_emitter.png"),x,y,6,14,164,62,164,62,256,256);
            c.texturelessSlot(0,x+37,y+24);
            if(!s.amountFocused)s.amountBox.text(Integer.toString(count(c)));
            s.amountBox.focused(s.amountFocused);
            DesktopWidgets.renderTextBox(c,s.amountBox,x+63,y+25,61);
            for(int row=0;row<2;row++)for(int i=0;i<4;i++) {
                int v=(c.shiftDown()?SHIFT_AMOUNTS[i]:AMOUNTS[i])*(row==0?1:-1);
                nativeAmountButton(c,x+BUTTON_X[i],y+2+row*40,BUTTON_W[i],(v>0?"+":"")+v);
            }
        }
        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,LevelEmitterState> c,double x,double y) { return c.menuSlotHit(0,c.contentX()+37,c.contentY()+24,x,y); }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,LevelEmitterState> c,MouseButtonEvent e,boolean twice) {
            if(e.button()!=MOUSE_LEFT)return false;
            var s=c.state(); int x=c.contentX(),y=c.contentY();
            if(contains(e.x(),e.y(),x+63,y+25,61,12)) { s.amountFocused=true;s.amountBox.focused(true);s.amountBox.moveToEnd(false);return true; }
            s.amountFocused=false;s.amountBox.focused(false);
            if(contains(e.x(),e.y(),c.windowX()-18,c.windowY()+5,16,16)) { sendLevelEmitter(c,count(c),!below(c));return true; }
            for(int row=0;row<2;row++)for(int i=0;i<4;i++)if(contains(e.x(),e.y(),x+BUTTON_X[i],y+2+row*40,BUTTON_W[i],20)) {
                int delta=(e.hasShiftDown()||c.shiftDown()?SHIFT_AMOUNTS[i]:AMOUNTS[i])*(row==0?1:-1);
                int next=(int)Math.max(0L,Math.min(Integer.MAX_VALUE,(long)count(c)+delta));
                sendLevelEmitter(c,next,below(c));s.amountBox.text(Integer.toString(next));return true;
            }
            return false;
        }
        private void edited(DesktopInputContext<AbstractContainerMenu,LevelEmitterState> c) {
            String text=c.state().amountBox.text();
            if(text.isBlank())return;
            try { int value=Integer.parseInt(text);if(value>=0)sendLevelEmitter(c,value,below(c)); } catch(NumberFormatException invalid) { /* Partial numeric edits do not alter the native value. */ }
        }
        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,LevelEmitterState> c,KeyEvent e) {
            var s=c.state();if(!s.amountFocused)return false;
            var edit=DesktopWidgets.editTextBoxKey(s.amountBox,e,10);s.amountFocused=s.amountBox.focused();
            if(edit.changed())edited(c);return edit.consumed();
        }
        @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu,LevelEmitterState> c,CharacterEvent e) {
            if(!c.state().amountFocused)return false;
            var edit=DesktopWidgets.editTextBoxChar(c.state().amountBox,e,10);if(edit.changed())edited(c);return edit.consumed();
        }
        @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c) { return c.state().amountFocused; }
        @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,LevelEmitterState> c,int x,int y) {
            if(!contains(x,y,c.windowX()-18,c.windowY()+5,16,16))return false;
            c.tooltip(Component.translatable(EMITTER_TOOLTIP+(below(c)?"less_than":"greater_than")),x,y);return true;
        }
    }

'''

tag_filter='''    /** Original paired gray lists, guide cell and native +/- arrangement. */
    public static final class TagItemFilter implements Definition<AbstractContainerMenu,TagFilterState>, com.salts_inventory_update.api.client.desktop.DesktopWindowPresentation {
        @Override public TagFilterState createState(DesktopWindowSetupContext<AbstractContainerMenu> c) { return new TagFilterState(); }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,TagFilterState> c) { return "native-tag-lists-1"; }
        @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> c) { return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(164,60); }
        @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu,TagFilterState> c) { return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(164,60); }
        @Override public List<com.salts_inventory_update.api.client.desktop.DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> c) { return leftNativeControl(); }
        @Override public void renderBehindFrame(DesktopRenderContext<?,?> c) { renderAllowToggle(c,c.windowX()-18,c.windowY()+5,TomsStorageReflect.booleanField(c.model(),"allowList",false)); }
        @Override public void tick(DesktopWindowContext<AbstractContainerMenu,TagFilterState> c) {
            Slot slot=c.menuSlot(0); List<String> tags=slot==null||slot.getItem().isEmpty()?List.of():slot.getItem().getTags().map(t->t.location().toString()).toList();
            var s=c.state();if(!s.availableTags.equals(tags)) { s.availableTags.clear();s.availableTags.addAll(tags);s.selectedAvailable=null;s.availableScroll.row=0; }
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,TagFilterState> c) {
            int x=c.contentX(),y=c.contentY();var s=c.state();
            c.texture(TomsStorageCompat.id("textures/gui/tag_filter.png"),x,y,6,14,164,59,164,59,256,256);
            c.texturelessSlot(0,x+2,y+1);
            nativeList(c,s.availableTags,s.selectedAvailable,s.availableScroll,x+22,y+1,61,58,14);
            renderCycleButton(c,x+84,y,TomsStorageCompat.id("icons/add"),false);
            renderCycleButton(c,x+84,y+17,TomsStorageCompat.id("icons/deny"),false);
            nativeList(c,s.filterTags,s.selectedFilter,s.filterScroll,x+103,y+1,61,58,14);
        }
        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,TagFilterState> c,double x,double y) { return c.menuSlotHit(0,c.contentX()+2,c.contentY()+1,x,y); }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,TagFilterState> c,MouseButtonEvent e,boolean twice) {
            if(e.button()!=MOUSE_LEFT)return false;
            int x=c.contentX(),y=c.contentY();var s=c.state();
            if(contains(e.x(),e.y(),c.windowX()-18,c.windowY()+5,16,16))return c.sendMenuButton(TomsStorageReflect.booleanField(c.model(),"allowList",false)?0:1);
            if(contains(e.x(),e.y(),x+84,y,16,16)) { if(s.selectedAvailable!=null&&!s.filterTags.contains(s.selectedAvailable)) { s.filterTags.add(s.selectedAvailable);sendTagFilter(c,s.filterTags); } return true; }
            if(contains(e.x(),e.y(),x+84,y+17,16,16)) { if(s.selectedFilter!=null) { s.filterTags.remove(s.selectedFilter);s.selectedFilter=null;sendTagFilter(c,s.filterTags); } return true; }
            for(int side=0;side<2;side++) {
                int lx=x+(side==0?22:103);var list=side==0?s.availableTags:s.filterTags;var scroll=side==0?s.availableScroll:s.filterScroll;
                if(nativeListPress(scroll,list.size(),e.x(),e.y(),lx,y+1,61,58,14))return true;
                if(contains(e.x(),e.y(),lx,y+1,53,56)) { int index=scroll.row+(int)(e.y()-y-1)/14;
                    if(index<list.size()) { if(side==0)s.selectedAvailable=list.get(index);else s.selectedFilter=list.get(index); } return true;
                }
            }
            return false;
        }
        @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu,TagFilterState> c,double x,double y,double sx,double sy) {
            var s=c.state();if(contains(x,y,c.contentX()+22,c.contentY()+1,60,56))return nativeListWheel(s.availableScroll,s.availableTags.size(),4,sy);
            if(contains(x,y,c.contentX()+103,c.contentY()+1,60,56))return nativeListWheel(s.filterScroll,s.filterTags.size(),4,sy);return false;
        }
        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,TagFilterState> c,MouseButtonEvent e,double dx,double dy) {
            var s=c.state();boolean a=nativeListDrag(s.availableScroll,s.availableTags.size(),e.y(),c.contentY()+1,58,14);
            return nativeListDrag(s.filterScroll,s.filterTags.size(),e.y(),c.contentY()+1,58,14)||a;
        }
        @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,TagFilterState> c,MouseButtonEvent e) { var s=c.state();boolean held=s.availableScroll.dragging||s.filterScroll.dragging;s.availableScroll.dragging=s.filterScroll.dragging=false;return held; }
        @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,TagFilterState> c,int x,int y) {
            if(contains(x,y,c.windowX()-18,c.windowY()+5,16,16)) { c.tooltip(Component.translatable(ALLOW_TOOLTIP+(TomsStorageReflect.booleanField(c.model(),"allowList",false)?"allowList":"denyList")),x,y);return true; }
            var s=c.state();for(int side=0;side<2;side++) { int lx=c.contentX()+(side==0?22:103);if(contains(x,y,lx,c.contentY()+1,52,56)) {
                var list=side==0?s.availableTags:s.filterTags;int i=(side==0?s.availableScroll:s.filterScroll).row+(y-c.contentY()-1)/14;
                if(i<list.size()){c.tooltip(Component.literal(list.get(i)),x,y);return true;}
            }}return false;
        }
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,TagFilterState> c,RESOURCE channel,byte[] data) {
            if(!TomsStorageCompat.TAG_SNAPSHOT_CHANNEL.equals(channel))return;
            var s=c.state();s.filterTags.clear();s.filterTags.addAll(TomsStoragePayloads.readTagSnapshot(data));
            if(!s.filterTags.contains(s.selectedFilter))s.selectedFilter=null;
        }
    }

'''

helpers='''    /** Attached native controls stay outside the frame, as in Tom's original screens. */
    private static List<com.salts_inventory_update.api.client.desktop.DesktopWindowRegion> leftNativeControl() {
        return List.of(com.salts_inventory_update.api.client.desktop.DesktopWindowRegion.interactive(-18,5,16,16)
            .withRole(com.salts_inventory_update.api.client.desktop.DesktopHitRole.CONTROL));
    }
    private static void renderAllowToggle(DesktopRenderContext<?,?> c,int x,int y,boolean allow) { ALLOW_DRAW }
    private static void nativeListRow(DesktopRenderContext<?,?> c,int x,int y,int width,int height,boolean selected) {
        boolean hovered=contains(c.mouseX(),c.mouseY(),x,y,width,height);
        NATIVE_ROW_DRAW
    }
    private static void nativeAmountButton(DesktopRenderContext<?,?> c,int x,int y,int width,String label) {
        boolean hovered=contains(c.mouseX(),c.mouseY(),x,y,width,20);
        AMOUNT_DRAW
        c.text(label,x+(width-c.fontWidth(label))/2,y+6,0xFFFFFFFF,true);
    }
    private static final class NativeListState { int row;boolean dragging;double grab; }
    private static void nativeList(DesktopRenderContext<?,?> c,List<String> values,String selected,NativeListState s,int x,int y,int width,int height,int rowHeight) {
        int max=Math.max(0,values.size()-height/rowHeight);s.row=clamp(s.row,0,max);
        c.withClip(x,y,width-8,height,()->{for(int i=0;i<height/rowHeight&&s.row+i<values.size();i++) {
            String text=values.get(s.row+i);int yy=y+i*rowHeight;
            nativeListRow(c,x,yy,width-8,rowHeight,text.equals(selected));
            c.text(c.trimToWidth(text,width-12),x+1,yy+3,0xFFFFFFFF,false);
        }});
        nativeListThumb(c,s,values.size(),x+width-6,y,height,rowHeight);
    }
    private static int nativeThumbHeight(int height,int rows,int size) { return Math.min(9,height); }
    private static void nativeListThumb(DesktopRenderContext<?,?> c,NativeListState s,int size,int x,int y,int height,int rowHeight) {
        int rows=height/rowHeight,max=Math.max(0,size-rows),h=nativeThumbHeight(height,rows,size);
        int yy=y+(max==0?0:(height-h)*s.row/max);
        NATIVE_THUMB_DRAW
    }
    private static boolean nativeListWheel(NativeListState s,int size,int rows,double amount) {
        if(amount==0)return false;int before=s.row;s.row=clamp(s.row+(amount<0?1:-1),0,Math.max(0,size-rows));return s.row!=before;
    }
    private static boolean nativeListPress(NativeListState s,int size,double mx,double my,int x,int y,int width,int height,int rowHeight) {
        if(!contains(mx,my,x+width-6,y,6,height))return false;
        int max=Math.max(0,size-height/rowHeight),h=nativeThumbHeight(height,height/rowHeight,size);if(max==0)return true;
        int yy=y+(height-h)*s.row/max;s.grab=contains(mx,my,x+width-8,yy,8,h)?my-yy:h/2.0;s.dragging=true;
        return nativeListDrag(s,size,my,y,height,rowHeight);
    }
    private static boolean nativeListDrag(NativeListState s,int size,double my,int y,int height,int rowHeight) {
        if(!s.dragging)return false;int max=Math.max(0,size-height/rowHeight),h=nativeThumbHeight(height,height/rowHeight,size);
        s.row=height<=h?0:clamp((int)Math.round((my-y-s.grab)*max/(height-h)),0,max);return true;
    }

'''

changes=[]
for version in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    path=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat/toms_storage/client/TomsStorageDesktopWindows.java'
    old=path.read_text();backup=WORK/'before'/path.relative_to(ROOT)
    text=backup.read_text() if backup.exists() else old
    legacy=version=='1.20.1';sdl=version.startswith('26.')
    key='InputConstants.MOUSE_BUTTON_LEFT' if sdl else 'GLFW.GLFW_MOUSE_BUTTON_LEFT'
    resource='Identifier' if 'import net.minecraft.resources.Identifier;' in text else 'ResourceLocation'
    # Keep version-specific wire format/snapshot support while sharing native geometry.
    e=emitter.replace('EMITTER_COUNT','c.state().count' if legacy else 'TomsStorageReflect.intField(c.model(),"count",0)')
    e=e.replace('EMITTER_BELOW','c.state().lessThan' if legacy else 'TomsStorageReflect.booleanField(c.model(),"lessThan",false)')
    e=e.replace('EMITTER_SNAPSHOT','''@Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,LevelEmitterState> c,ResourceLocation channel,byte[] data) {
            if(TomsStorageCompat.EMITTER_SNAPSHOT_CHANNEL.equals(channel)) { CompoundTag tag=TomsStoragePayloads.readNbt(data);c.state().count=tag.getInt("count");c.state().lessThan=tag.getBoolean("lessThan"); }
        }''' if legacy else '')
    e=e.replace('EMITTER_TOOLTIP+(below(c)?"less_than":"greater_than")','"tooltip.toms_storage.lvlEm_lt_"+(below(c)?"1":"0")' if legacy else '"tooltip.toms_storage.level_emitter."+(below(c)?"less_than":"greater_than")')
    e=e.replace('MOUSE_LEFT',key)
    e=e.replace('c.shiftDown()?SHIFT_AMOUNTS','net.minecraft.client.gui.screens.Screen.hasShiftDown()?SHIFT_AMOUNTS' if version in ['1.20.1','1.21.1'] else 'c.minecraft().hasShiftDown()?SHIFT_AMOUNTS')
    text=text[:text.index('    public static final class LevelEmitter')]+e+text[text.index('    public static final class InventoryLink'):]
    tag=tag_filter.replace('MOUSE_LEFT',key).replace('RESOURCE',resource).replace('ALLOW_TOOLTIP','"tooltip.toms_storage."')
    if legacy:tag=tag.replace('"tooltip.toms_storage."+(TomsStorageReflect.booleanField(c.model(),"allowList",false)?"allowList":"denyList")','"tooltip.toms_storage.allowList_"+(TomsStorageReflect.booleanField(c.model(),"allowList",false)?"1":"0")')
    if sdl:tag=tag.replace('slot.getItem().getTags()','slot.getItem().tags()')
    text=text[:text.index('    public static final class TagItemFilter')]+tag+text[text.index('    public static final class FilingCabinet'):]
    # Schema migration discards old QA-sized 3-column terminal dimensions once, not on every open.
    text=text.replace('private static final int STORAGE_TERMINAL_ROWS = 7;','private static final int STORAGE_TERMINAL_ROWS = 6;')
    text=text.replace('            context.text(Component.literal("Craft"), x, y - 10, 0xFF404040, false);\n','')
    marker='        private final boolean crafting;'
    text=text.replace('public static final class Terminal implements Definition<AbstractContainerMenu, TerminalState> {','public static final class Terminal implements Definition<AbstractContainerMenu, TerminalState>, com.salts_inventory_update.api.client.desktop.DesktopWindowPresentation {',1)
    text=text.replace(marker,marker+'''
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,TerminalState> c) { return crafting?"native-crafting-terminal-2":"native-storage-terminal-2"; }
        @Override public void loadLocalState(DesktopWindowContext<AbstractContainerMenu,TerminalState> c,CompoundTag tag) { c.state().initialNativeSize=!tag.contains("toms:native_size"); }
        @Override public void saveLocalState(DesktopWindowContext<AbstractContainerMenu,TerminalState> c,CompoundTag tag) { tag.putBoolean("toms:native_size",true); }
        @Override public DesktopWindowSize desiredWindowSize(DesktopWindowContext<?,?> raw) {
            if(!((TerminalState)raw.state()).initialNativeSize)return null;
            return DesktopWindowSize.of(STORAGE_TERMINAL_WINDOW_WIDTH,crafting?CRAFTING_TERMINAL_WINDOW_HEIGHT:STORAGE_TERMINAL_WINDOW_HEIGHT);
        }
        @Override public void opened(DesktopWindowContext<AbstractContainerMenu,TerminalState> c) { c.state().initialNativeSize=false; }
'''.replace('BOOLEAN_DEFAULT',',false' if version not in ['1.20.1','1.21.1'] else ''),1)
    text=text.replace('    public static final class TerminalState {','    public static final class TerminalState {\n        private boolean initialNativeSize=true;',1)
    # Restore Inventory Link's original panel, native rim and dedicated scrollbar gutter.
    start=text.index('    public static final class InventoryLink');end=text.index('    public static final class ItemFilter')
    link=text[start:end]
    link=link.replace('context.contentX()+PAD,context.contentY()+PAD+14,106,13','context.contentX()+4,context.contentY()+16,109,12')
    link=link.replace('return DesktopWindowSize.of(192, 174);','return com.salts_inventory_update.api.client.desktop.DesktopInsets.SALT.surfaceSize(166,148);')
    link=link.replace('        @Override public List<','        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,InventoryLinkState> c) { return "native-link-list-1"; }\n        @Override public List<',1)
    link=link.replace('int x = context.contentX() + PAD;','int x = context.contentX();').replace('int y = context.contentY() + PAD;','int y = context.contentY();')
    renderstart=link.index('            context.text(Component.literal("Beacon ')
    renderend=link.index('\n        }',renderstart)
    level='state.beaconLevel' if legacy else 'TomsStorageReflect.intField(context.model(),"beaconLvl",0)'
    remote='''
            nativeButton(context,linkTexture,x+149,y,176,16);
            nativeButton(context,linkTexture,x+149,y,176+(state.remote?16:0),32);''' if legacy else ''
    lockdraw='''nativeButton(context,linkTexture,x+149,y+14,176,16);
            nativeButton(context,linkTexture,x+149,y+14,208+(state.publicChannel?16:0),0);''' if legacy else '''renderCycleButton(context,x+149,y+14,TomsStorageCompat.id(state.publicChannel?"icons/lock_off":"icons/lock_on"),false);'''
    link=link[:renderstart]+f'''            context.text(Component.translatable("{'ts.inventory_connector.beacon_level' if legacy else 'label.toms_storage.inventory_connector.beacon_level'}",{level}),x,y,0xFF404040,false);
            var linkTexture=TomsStorageCompat.id("textures/gui/inventory_link.png");
            // Exact original gray search/list artwork, without its outer frame or title.
            context.texture(linkTexture,x,y+14,6,24,166,132,166,132,256,256);{remote}
            state.nameBox.text(state.name);state.nameBox.focused(state.nameFocused);
            DesktopWidgets.renderTextBox(context,state.nameBox,x+4,y+16,109);
            renderCycleButton(context,x+115,y+14,TomsStorageCompat.id("icons/add"),false);
            renderCycleButton(context,x+132,y+14,TomsStorageCompat.id("icons/deny"),false);
            {lockdraw}
            state.listScroll.row=clamp(state.listScroll.row,0,Math.max(0,state.channels.size()-6));
            context.withClip(x+6,y+32,106,96,()->{{for(int i=0;i<6&&state.listScroll.row+i<state.channels.size();i++) {{
                LinkChannel channel=state.channels.get(state.listScroll.row+i);int yy=y+32+i*16;
                nativeListRow(context,x+6,yy,106,16,channel.id().equals(state.selected));
                ROW_LOCK_DRAW
                String name=context.trimToWidth(channel.name(),84);
                context.text(name,x+7+(86-context.fontWidth(name))/2,yy+4,0xFFFFFFFF,true);
            }}}});
            nativeListThumb(context,state.listScroll,state.channels.size(),x+114,y+31,100,16);
''' +link[renderend:]
    # One committed coordinate map for visible controls and pointer routing.
    link=link.replace('ROW_LOCK_DRAW','nativeButton(context,linkTexture,x+96,yy,208+(channel.publicChannel()?16:0),0);' if legacy else 'context.sprite(TomsStorageCompat.id(channel.publicChannel()?"icons/lock_off":"icons/lock_on"),x+97,yy+1,14,14);')
    if legacy:link=link.replace('x+146,y-4','x+149,y')
    link=link.replace('x, y + 14, 106, 13','x+4, y+16, 109, 12')
    link=link.replace('x + 110, y + 13','x+115, y+14').replace('x + 128, y + 13','x+132, y+14').replace('x + 146, y + 13','x+149, y+14')
    link=link.replace('int listY = y + 34;','int listY = y+32;')
    link=link.replace('contains(event.x(), event.y(), x, listY, 164, 96)','contains(event.x(), event.y(), x+6, listY, 106, 96)')
    link=link.replace('int index = state.scroll +','int index = state.listScroll.row +')
    # Scrollbar click precedes row selection; it cannot activate a channel.
    link=link.replace('            int listY = y+32;','            if(nativeListPress(state.listScroll,state.channels.size(),event.x(),event.y(),x+6,y+31,114,100,16))return true;\n            int listY = y+32;')
    scrollstart=link.index('            InventoryLinkState state = context.state();',link.index('public boolean mouseScrolled'))
    scrollend=link.index('\n        }',scrollstart)
    link=link[:scrollstart]+'''            return contains(mouseX,mouseY,context.contentX()+4,context.contentY()+30,125,114)
                &&nativeListWheel(context.state().listScroll,context.state().channels.size(),6,scrollY);'''+link[scrollend:]
    nextmethod=link.index('        @Override\n        public boolean keyPressed')
    link=link[:nextmethod]+'''        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,InventoryLinkState> c,MouseButtonEvent e,double dx,double dy) { return nativeListDrag(c.state().listScroll,c.state().channels.size(),e.y(),c.contentY()+28,114,19); }
        @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,InventoryLinkState> c,MouseButtonEvent e) { boolean held=c.state().listScroll.dragging;c.state().listScroll.dragging=false;return held; }

'''.replace('c.contentY()+28,114,19','c.contentY()+31,100,16')+link[nextmethod:]
    link=link.replace('            InventoryLinkState state = context.state();\n            int x = context.contentX();\n            int y = context.contentY();\n            if (','            if(event.button()!='+key+')return false;\n            InventoryLinkState state = context.state();\n            int x = context.contentX();\n            int y = context.contentY();\n            if (',1)
    link=link.replace('context.state().scroll = clamp(context.state().scroll,','context.state().listScroll.row = clamp(context.state().listScroll.row,')
    text=text[:start]+link+text[end:]
    # UI-only state; native server and payload validation remain authoritative.
    statepos=text.index('    public static final class LevelEmitterState {')
    text=text[:statepos]+text[statepos:].replace('    public static final class LevelEmitterState {','    public static final class LevelEmitterState {\n        private final DesktopTextBoxState amountBox=new DesktopTextBoxState();\n        private boolean amountFocused;\n        private int pendingCount=-1;\n        private boolean pendingBelow;\n        private Runnable pendingSend;',1)
    text=text.replace('    public static final class InventoryLinkState {','    public static final class InventoryLinkState {\n        private final NativeListState listScroll=new NativeListState();',1)
    text=text.replace('    public static final class TagFilterState {','    public static final class TagFilterState {\n        private final NativeListState availableScroll=new NativeListState(),filterScroll=new NativeListState();',1)
    h=helpers
    if legacy:
        h=h.replace('ALLOW_DRAW','nativeButton(c,TomsStorageCompat.id("textures/gui/filter_buttons.png"),x,y,allow?16:0,0);')
        h=h.replace('AMOUNT_DRAW','''var atlas=new ResourceLocation("minecraft","textures/gui/widgets.png");int v=hovered?86:66;
        c.texture(atlas,x,y,0,v,width/2,20,width/2,20,256,256);
        c.texture(atlas,x+width/2,y,200-(width-width/2),v,width-width/2,20,width-width/2,20,256,256);''')
        h=h.replace('NATIVE_ROW_DRAW','''boolean link=c.model().getClass().getSimpleName().equals("InventoryLinkMenu");
        var atlas=TomsStorageCompat.id("textures/gui/"+(link?"inventory_link":"tag_filter")+".png");
        int u=link?(selected?106:0):176,v=link?182+(hovered?16:0):41+(selected?28:0)+(hovered?14:0);
        c.texture(atlas,x,y,u,v,width,height,width,height,256,256);''')
        h=h.replace('NATIVE_THUMB_DRAW','c.texture(TomsStorageCompat.id("textures/gui/tag_filter.png"),x,yy,176+(max>0?5:0),32,5,h,5,h,256,256);')
    else:
        h=h.replace('ALLOW_DRAW','renderCycleButton(c,x,y,TomsStorageCompat.id(allow?"icons/allow":"icons/deny"),false);')
        rid=resource+'.fromNamespaceAndPath'
        h=h.replace('AMOUNT_DRAW',f'c.sprite({rid}("minecraft",hovered?"widget/button_highlighted":"widget/button"),x,y,width,20);')
        h=h.replace('NATIVE_ROW_DRAW','c.sprite(TomsStorageCompat.id("widget/small_button"+(selected?"_selected":"")+(hovered?"_hovered":"")),x,y,width,height);')
        h=h.replace('NATIVE_THUMB_DRAW','c.sprite(TomsStorageCompat.id("widget/small_scroll"+(max==0?"_disabled":contains(c.mouseX(),c.mouseY(),x,yy,5,h)?"_hovered":"")),x,yy,5,h);')
    text=text.replace('    private static void renderTagList(',h+'    private static void renderTagList(',1)
    send_start=text.index('    private static void sendLevelEmitter(')
    send_end=text.index('    private static void sendTagFilter(',send_start)
    text=text[:send_start]+'''    private static void sendLevelEmitter(DesktopInputContext<AbstractContainerMenu,LevelEmitterState> c,int count,boolean below) {
        var s=c.state();s.pendingCount=count;s.pendingBelow=below;
        s.pendingSend=()->{
            if(!c.windowHandle().isOpen()||!c.inventoryAvailable()){s.pendingSend=null;return;}
            CompoundTag tag=new CompoundTag();tag.putInt("count",s.pendingCount);tag.putBoolean("lessThan",s.pendingBelow);
            if(c.sendPayload(TomsStorageCompat.NBT_CHANNEL,TomsStoragePayloads.writeNbt(tag)))s.pendingSend=null;
        };
        s.pendingSend.run();
    }

'''+text[send_end:]
    text='\n'.join('' if not line.strip() else line for line in text.split('\n'))
    if old==text:continue
    backup=WORK/'before'/path.relative_to(ROOT);backup.parent.mkdir(parents=True,exist_ok=True)
    if not backup.exists():backup.write_bytes(path.read_bytes())
    path.write_text(text);changes.append(str(path.relative_to(ROOT)))
    
# Fabric production obfuscates TagKey.location(). A reflective name lookup
# displayed TagKey[...] in filter rows and sent invalid IDs back on removal.
for version in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    p=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat/toms_storage/TomsStorageReflect.java'
    text=p.read_text();needle='    private static String tagLocationString(Object tag) {'
    if 'tag instanceof net.minecraft.tags.TagKey' in text:continue
    backup=WORK/'before'/p.relative_to(ROOT);backup.parent.mkdir(parents=True,exist_ok=True)
    if not backup.exists():backup.write_bytes(p.read_bytes())
    text=text.replace(needle,needle+'\n        if(tag instanceof net.minecraft.tags.TagKey<?> key)return key.location().toString();',1)
    p.write_text(text);changes.append(str(p.relative_to(ROOT)))
(WORK/'changed-files.json').write_text(json.dumps([str(p.relative_to(WORK/'before')) for p in (WORK/'before').rglob('*.java')],indent=2))
print('Updated',len(changes),'Tom Storage source branches')
