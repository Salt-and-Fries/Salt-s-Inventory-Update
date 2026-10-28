package com.salts_inventory_update.compat.chipped;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Chipped workshop: selection-only inventory references and the original native preview renderer. */
public final class ChippedWindow implements DesktopWindowDefinition<AbstractContainerMenu,ChippedWindow.State> {
    public static final int WIDTH=270,HEIGHT=181,GRID_X=91,GRID_Y=64,COLUMNS=9,ROWS=6,CELL=18;
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("chipped","textures/gui/container/workbench.png");
    private static final ResourceLocation BUTTON=ResourceLocation.fromNamespaceAndPath("chipped","textures/gui/sprites/button.png");
    private static final ResourceLocation BUTTON_HIGHLIGHTED=ResourceLocation.fromNamespaceAndPath("chipped","textures/gui/sprites/button_highlighted.png");
    private static final ResourceLocation SLOT=ResourceLocation.fromNamespaceAndPath("chipped","textures/gui/sprites/slot.png");
    private static final String[] MODE_NAMES={"SINGLE_BLOCK","HORIZONTAL_BLOCK","VERTICAL_BLOCK","TWO_BY_TWO"};
    private static final String[] MODE_ICONS={"single_block_button","horizontal_blocks_button","vertical_blocks_button","two_by_two_button"};
    private static final String[] MODE_LABELS={"single","horizontal","vertical","two_by_two"};
    private static final DesktopWindowSurface SURFACE=new DesktopWindowSurface(DesktopSurfaceMode.SALT,
        new DesktopInsets(6,20,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);
    public static final class State {
        public int selected=-1,mode=3;
        public ItemStack source=ItemStack.EMPTY;
        public boolean picker,pending;
        public double scroll;
        public int pickerScroll;
        private boolean dragging;
        private EditBox search;
        private AbstractWidget preview;
        private Object[] nativeModes;
        private List<ItemStack> inventory=List.of();
        public List<Integer> eligible=List.of();
    }
    @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c) {
        var s=new State();s.search=new EditBox(c.minecraft().font,0,0,115,11,Component.literal("Search variants"));
        s.search.setBordered(false);s.search.setMaxLength(50);s.search.setTextColor(0xFFFFFFFF);s.search.setEditable(false);
        s.search.setResponder(text->{ChippedNative.filter(c.model(),text);s.scroll=0;});
        try {
            var type=Class.forName("earth.terrarium.chipped.client.screens.RenderWindowWidget$Mode");s.nativeModes=type.getEnumConstants();
            s.preview=(AbstractWidget)Class.forName("earth.terrarium.chipped.client.screens.RenderWindowWidget")
                .getConstructor(int.class,int.class,int.class,int.class,Supplier.class,Supplier.class)
                .newInstance(0,0,72,72,(Supplier<Object>)()->s.nativeModes[s.mode],(Supplier<BlockState>)()->previewState(c.model()));
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Chipped native preview unavailable",e);}
        return s;
    }
    private static BlockState previewState(AbstractContainerMenu m) {
        var block=Block.byItem(ChippedNative.chosen(m).getItem());var state=block.defaultBlockState();
        if(block instanceof IronBarsBlock)state=state.setValue(IronBarsBlock.NORTH,true).setValue(IronBarsBlock.SOUTH,true);
        return state.isAir()?null:state;
    }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return SURFACE;}
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> c){return DesktopWindowSize.of(WIDTH,HEIGHT);}
    @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu,State> c){return DesktopWindowSize.of(WIDTH,HEIGHT);}
    @Override public DesktopWindowSize maxSize(DesktopWindowContext<AbstractContainerMenu,State> c){return minSize(c);}
    @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "chipped-workshop-1";}
    private void widgets(DesktopWindowContext<AbstractContainerMenu,State> c){
        var s=c.state();s.search.setX(c.windowX()+111);s.search.setY(c.windowY()+49);
        s.preview.setX(c.windowX()+10);s.preview.setY(c.windowY()+49);
    }
    @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){widgets(c);}
    @Override public void tick(DesktopWindowContext<AbstractContainerMenu,State> c){
        var s=c.state();widgets(c);
        var inv=c.minecraft().player.getInventory();boolean changed=s.inventory.size()!=36;
        if(!changed)for(int i=0;i<36;i++)if(!ItemStack.matches(s.inventory.get(i),inv.getItem(i))){changed=true;break;}
        if(changed){
            var stacks=new ArrayList<ItemStack>();var eligible=new ArrayList<Integer>();
            for(int i=0;i<36;i++){var item=inv.getItem(i);stacks.add(item.copy());if(ChippedNative.eligible(c.model(),item))eligible.add(i);}
            s.inventory=stacks;s.eligible=List.copyOf(eligible);s.pickerScroll=Math.min(s.pickerScroll,Math.max(0,eligible.size()-5));
            if(s.selected>=0&&!ItemStack.matches(s.source,inv.getItem(s.selected)))clear(c);
        }
    }
    private void clear(DesktopWindowContext<AbstractContainerMenu,State> c){
        var s=c.state();s.selected=-1;s.source=ItemStack.EMPTY;s.pending=false;s.scroll=0;
        ChippedNative.reset(c.model());s.search.setValue("");s.search.setEditable(false);s.search.setFocused(false);
    }
    @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,ResourceLocation channel,byte[] data){
        if(!channel.equals(ChippedIntegration.STATE)||data.length>8192)return;
        var b=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data),c.minecraft().player.registryAccess());
        try {
            int status=b.readUnsignedByte(),index=b.readVarInt();var stack=ItemStack.OPTIONAL_STREAM_CODEC.decode(b);
            if(b.isReadable()||index< -1||index>=36)return;
            clear(c);
            if(index>=0){var s=c.state();s.selected=index;s.source=stack.copy();ChippedNative.select(c.model(),index);ChippedNative.filter(c.model(),"");s.search.setEditable(true);}
            if(status==2)c.minecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_TAKE_RESULT,1,1));
        }finally{b.release();}
    }
    @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){return this::chrome;}
    private static void frame(DesktopRenderContext<?,?> c,int x,int y,int w,int h){
        // Rounded pixel chrome in the approved brown/beige palette; native controls remain native.
        c.fill(x+2,y,x+w-3,y+h,0xFF261E1A);c.fill(x+1,y+1,x+w-1,y+h-1,0xFF261E1A);
        c.fill(x,y+2,x+w,y+h-3,0xFF261E1A);
        c.fill(x+2,y+1,x+w-4,y+h-4,0xFFF7DDB1);c.fill(x+1,y+2,x+w-4,y+h-4,0xFFF7DDB1);
        c.fill(x+3,y+3,x+w-3,y+h-3,0xFF684829);c.fill(x+3,y+3,x+w-5,y+h-5,0xFFC19D68);
    }
    private void chrome(DesktopRenderContext<AbstractContainerMenu,State> c,List<DesktopControlState> controls){
        frame(c,c.windowX(),c.windowY(),c.windowWidth(),c.windowHeight());
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-3).min().orElse(c.windowX()+c.windowWidth()-6);
        c.text(c.trimToWidth(c.originalTitle().getString(),Math.max(0,right-c.windowX()-7)),c.windowX()+7,c.windowY()+5,0xFF352719,false);
        var tex=ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
        for(var state:controls){
            int col=switch(state.control()){case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;};
            if(col>=0){var b=state.bounds();c.texture(tex,b.x(),b.y(),col*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);}
        }
    }
    private static boolean inside(double x,double y,int bx,int by,int w,int h){return x>=bx&&y>=by&&x<bx+w&&y<by+h;}
    private void button(DesktopRenderContext<?,?> c,int x,int y,int width,String label,boolean enabled){
        // Chipped 4 uses separate 18px-high images, not Chipped 3's stacked states.
        var texture=enabled&&inside(c.mouseX(),c.mouseY(),x,y,width,18)?BUTTON_HIGHLIGHTED:BUTTON;
        c.texture(texture,x,y,0,0,3,18,3,18,72,18);
        c.texture(texture,x+3,y,3,0,width-6,18,66,18,72,18);
        c.texture(texture,x+width-3,y,69,0,3,18,3,18,72,18);
        c.text(label,x+(width-c.fontWidth(label))/2,y+5,enabled?0xFF403326:0xFF7C6749,false);
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){
        widgets(c);var s=c.state();int x=c.windowX(),y=c.windowY();
        c.fill(x+7,y+22,x+263,y+42,0xFF4B341E);c.fill(x+8,y+23,x+262,y+41,0xFFDAB880);c.fill(x+9,y+24,x+261,y+40,0xFF674E30);
        if(s.selected>=0){c.virtualItem(s.source,s.source.getCount(),x+10,y+24);c.text(c.trimToWidth(s.source.getHoverName().getString()+"  x"+s.source.getCount(),176),x+32,y+28,0xFFFFECC8,false);}
        else c.text(s.pending?"Selecting material...":"No material selected",x+12,y+28,0xFFF3E2C7,false);
        button(c,x+213,y+23,47,"Choose",!s.pending);
        c.fill(x+6,y+45,x+86,y+125,0xFF674E30);
        c.withClip(x+6,y+45,80,80,()->s.preview.render(c.nativeGraphics(),c.mouseX(),c.mouseY(),c.minecraft().getTimer().getGameTimeDeltaPartialTick(false)));
        button(c,x+10,y+127,72,Component.translatable(net.minecraft.client.gui.screens.Screen.hasShiftDown()?"text.chipped.craft_all":"text.chipped.craft").getString(),s.selected>=0&&!s.pending&&!ChippedNative.chosen(c.model()).isEmpty());
        for(int i=0;i<4;i++){
            int bx=x+10+i*18;
            var tex=ResourceLocation.fromNamespaceAndPath("chipped","textures/gui/sprites/"+MODE_ICONS[i]
                +(inside(c.mouseX(),c.mouseY(),bx,y+147,18,18)?"_highlighted":"")+".png");
            c.texture(tex,bx,y+147,0,0,18,18,18,18,18,18);
        }
        c.texture(ATLAS,x+91,y+48,85,26,162,14,162,14,256,256);
        s.search.render(c.nativeGraphics(),c.mouseX(),c.mouseY(),c.minecraft().getTimer().getGameTimeDeltaPartialTick(false));
        var list=ChippedNative.results(c.model());s.scroll=Math.min(s.scroll,maxScroll(list.size()));
        c.withClip(x+GRID_X,y+GRID_Y,162,108,()->{
            int first=(int)s.scroll/18,last=Math.min(Math.max(6,(list.size()+8)/9),first+7);
            for(int row=first;row<last;row++)for(int col=0;col<9;col++){
                int index=row*9+col,bx=x+GRID_X+col*18,by=y+GRID_Y+row*18-(int)s.scroll;
                c.texture(SLOT,bx,by,0,0,18,18,18,18,18,18);
                if(index<list.size())c.item(list.get(index),bx+1,by+1);
            }
            int hovered=resultIndex(c,c.mouseX(),c.mouseY());
            if(hovered>=0&&hovered<list.size()){int bx=x+GRID_X+(hovered%9)*18,by=y+GRID_Y+(hovered/9)*18-(int)s.scroll;c.fill(bx+1,by+1,bx+17,by+17,0x80FFFFFF);}
        });
        if(s.selected<0||list.isEmpty())c.text(s.selected<0?"Choose a material":"No matching variants",x+98,y+111,0xFFF3E2C7,false);
        c.fill(x+255,y+64,x+260,y+172,0xFF5C4126);
        int max=maxScroll(list.size()),thumb=thumb(list.size()),top=max==0?0:(int)(s.scroll*(108-thumb)/max);
        c.fill(x+256,y+64+top,x+259,y+64+top+thumb,0xFFDBBF91);
    }
    public static int maxScroll(int count){return Math.max(0,((count+8)/9)*18-108);}
    private static int thumb(int count){return Math.max(12,Math.min(108,count==0?108:108*108/Math.max(108,((count+8)/9)*18)));}
    private int resultIndex(DesktopWindowContext<AbstractContainerMenu,State> c,double mx,double my){
        double x=mx-c.windowX()-GRID_X,y=my-c.windowY()-GRID_Y;
        return catalogIndex(x,y,(int)c.state().scroll);
    }
    public static int catalogIndex(double x,double y,int scroll){
        if(!inside(x,y,0,0,162,108))return -1;
        return ((int)(y+scroll)/18)*9+(int)x/18;
    }
    private record Picker(int x,int y,int width,int height,int rows){}
    private Picker picker(DesktopWindowContext<AbstractContainerMenu,State> c){
        int rows=Math.min(5,c.state().eligible.size()),w=143;
        for(int i:c.state().eligible){var stack=c.minecraft().player.getInventory().getItem(i);w=Math.max(w,Math.min(230,c.fontWidth(stack.getHoverName().getString()+" x"+stack.getCount())+43));}
        int h=rows==0?62:39+rows*20;
        int px=Math.max(2,Math.min(c.windowX()+111,c.minecraft().getWindow().getGuiScaledWidth()-w-2));
        int py=Math.max(2,Math.min(c.windowY()+43,c.minecraft().getWindow().getGuiScaledHeight()-h-2));
        return new Picker(px,py,w,h,rows);
    }
    @Override public List<DesktopPopup> popups(DesktopWindowContext<AbstractContainerMenu,State> c){
        if(!c.state().picker)return List.of();var p=picker(c);
        return List.of(new DesktopPopup("material",List.of(DesktopWindowRegion.transientVisualAndInteractive(p.x-c.windowX(),p.y-c.windowY(),p.width,p.height)),true,true,true,true));
    }
    @Override public boolean dismissPopup(DesktopWindowContext<AbstractContainerMenu,State> c,String id,DesktopPopup.Dismissal reason){if(!id.equals("material"))return false;c.state().picker=false;return true;}
    @Override public void renderOverlay(DesktopRenderContext<AbstractContainerMenu,State> c){
        if(!c.state().picker)return;var p=picker(c);var s=c.state();
        // Native EditBox text and item models leave depth behind. Start the modal's
        // own public render stratum so its opaque frame covers the workshop below.
        c.rawGraphics().nextStratum();
        frame(c,p.x,p.y,p.width,p.height);c.text("Choose material",p.x+7,p.y+5,0xFF352719,false);
        c.fill(p.x+6,p.y+20,p.x+p.width-6,p.y+p.height-20,0xFF5E452B);
        for(int row=0;row<p.rows;row++){
            int n=s.pickerScroll+row;if(n>=s.eligible.size())break;int index=s.eligible.get(n),by=p.y+22+row*20;
            var item=c.minecraft().player.getInventory().getItem(index);
            if(inside(c.mouseX(),c.mouseY(),p.x+7,by,p.width-14,20))c.fill(p.x+7,by,p.x+p.width-7,by+20,0xFF8C713E);
            if(index==s.selected)c.fill(p.x+8,by+1,p.x+26,by+19,0x70FFFF00);
            c.virtualItem(item,item.getCount(),p.x+9,by+2);
            c.text(c.trimToWidth(item.getHoverName().getString()+" x"+item.getCount(),p.width-43),p.x+32,by+5,0xFFFFEBCA,false);
        }
        if(p.rows==0)c.text("No usable materials",p.x+9,p.y+27,0xFFFFEBCA,false);
        c.text("Player inventory"+(s.eligible.size()>5?"  (scroll)":"") ,p.x+9,p.y+p.height-15,0xFFDFC7A5,false);
    }
    @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice){
        var s=c.state();double x=e.x()-c.windowX(),y=e.y()-c.windowY();widgets(c);
        if(s.picker){
            var p=picker(c);if(e.button()!=0)return true;
            int row=(int)(e.y()-p.y-22)/20;
            if(inside(e.x(),e.y(),p.x+7,p.y+22,p.width-14,p.rows*20)&&row>=0&&row<p.rows){
                int n=s.pickerScroll+row;if(n<s.eligible.size()){
                    int index=s.eligible.get(n);var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),c.minecraft().player.registryAccess());
                    try{b.writeByte(0);b.writeVarInt(index);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,c.minecraft().player.getInventory().getItem(index));s.pending=c.sendPayload(ChippedIntegration.ACTION,bytes(b));}
                    finally{b.release();}
                    s.picker=false;
                }
            }return true;
        }
        if(e.button()!=0)return false;
        if(inside(x,y,213,23,47,18)&&!s.pending){s.picker=true;s.search.setFocused(false);return true;}
        if(s.search.mouseClicked(e.x(),e.y(),e.button())){s.search.setFocused(true);return true;}s.search.setFocused(false);
        if(inside(x,y,10,127,72,18)){
            if(s.selected>=0&&!s.pending&&!ChippedNative.chosen(c.model()).isEmpty()){
                var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),c.minecraft().player.registryAccess());try{b.writeByte(1);b.writeBoolean(c.shiftDown());ItemStack.OPTIONAL_STREAM_CODEC.encode(b,ChippedNative.chosen(c.model()));s.pending=c.sendPayload(ChippedIntegration.ACTION,bytes(b));}finally{b.release();}
            }return true;
        }
        if(inside(x,y,10,147,72,18)){s.mode=(int)(x-10)/18;return true;}
        int index=resultIndex(c,e.x(),e.y());var list=ChippedNative.results(c.model());
        if(index>=0){if(index<list.size())ChippedNative.choose(c.model(),list.get(index));return true;}
        if(inside(x,y,255,64,5,108)){s.dragging=true;dragScroll(c,e.y());return true;}
        return false;
    }
    private static byte[] bytes(FriendlyByteBuf b){byte[] data=new byte[b.readableBytes()];b.readBytes(data);return data;}
    private void dragScroll(DesktopWindowContext<AbstractContainerMenu,State> c,double y){int n=ChippedNative.results(c.model()).size(),h=thumb(n);c.state().scroll=Math.max(0,Math.min(maxScroll(n),(y-c.windowY()-64-h/2d)/Math.max(1,108-h)*maxScroll(n)));}
    @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,double dx,double dy){if(!c.state().dragging)return false;dragScroll(c,e.y());return true;}
    @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e){boolean drag=c.state().dragging;c.state().dragging=false;return drag;}
    @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu,State> c,double x,double y,double sx,double sy){
        var s=c.state();if(s.picker){s.pickerScroll=Math.max(0,Math.min(Math.max(0,s.eligible.size()-5),s.pickerScroll+(int)-Math.signum(sy)));return true;}
        if(!inside(x-c.windowX(),y-c.windowY(),GRID_X,GRID_Y,169,108))return false;
        s.scroll=Math.max(0,Math.min(maxScroll(ChippedNative.results(c.model()).size()),s.scroll-sy*8));return true;
    }
    @Override public List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<AbstractContainerMenu,State> c){
        int max=maxScroll(ChippedNative.results(c.model()).size());return List.of(new DesktopScrollRegion(c.windowX()+GRID_X,c.windowY()+GRID_Y,162,108,18,c.state().scroll>0,c.state().scroll<max,2.25));
    }
    @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,State> c,KeyEvent e){
        if(!c.state().search.isFocused())return false;
        if(e.key()==256){c.state().search.setFocused(false);return true;}
        return c.state().search.keyPressed(e.key(),e.scancode(),e.modifiers());
    }
    @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu,State> c,CharacterEvent e){return c.state().search.isFocused()&&c.state().search.charTyped(e.codepoint(),e.modifiers());}
    @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu,State> c){return c.state().search.canConsumeInput();}
    @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,State> c){return List.of(new DesktopWindowBounds(c.windowX()+111,c.windowY()+49,115,11));}
    @Override public void keyboardFocusChanged(DesktopWindowContext<AbstractContainerMenu,State> c,boolean focused){if(!focused)c.state().search.setFocused(false);}
    @Override public boolean focusNext(DesktopWindowContext<AbstractContainerMenu,State> c,boolean forward){if(c.state().selected<0)return false;c.state().search.setFocused(!c.state().search.isFocused());return true;}
    @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,State> c,int mx,int my){
        var s=c.state();if(s.picker){
            var p=picker(c);int row=(my-p.y-22)/20,n=s.pickerScroll+row;
            if(inside(mx,my,p.x+7,p.y+22,p.width-14,p.rows*20)&&n>=0&&n<s.eligible.size()){c.tooltip(c.minecraft().player.getInventory().getItem(s.eligible.get(n)),mx,my);return true;}return false;
        }
        int index=resultIndex(c,mx,my);var list=ChippedNative.results(c.model());if(index>=0&&index<list.size()){c.tooltip(list.get(index),mx,my);return true;}
        if(inside(mx-c.windowX(),my-c.windowY(),10,147,72,18)){int mode=(mx-c.windowX()-10)/18;c.tooltip(Component.translatable("text.chipped."+MODE_LABELS[mode]),mx,my);return true;}
        if(s.selected>=0&&inside(mx-c.windowX(),my-c.windowY(),8,23,203,18)){c.tooltip(s.source,mx,my);return true;}return false;
    }
}
