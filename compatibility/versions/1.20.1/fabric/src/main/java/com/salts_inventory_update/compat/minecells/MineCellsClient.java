package com.salts_inventory_update.compat.minecells;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.KeyEvent;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native blue crafter and complete transparent parchment, matching the approved mockup. */
public final class MineCellsClient {
    private static final String SCREEN="com.github.mim1q.minecells.screen.cellcrafter.CellCrafterScreen";
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static boolean registered;
    private MineCellsClient() { }
    public static void initializeClient(){DesktopClientApi.onClientStarted(mc->{
        var type=NativeMenuSupport.type("minecells:cell_forge");if(type==null||registered)return;
        DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
            try{
                MineCellsWindows.attach(menu,inventory.player);
                var screen=(AbstractContainerScreen<?>)Class.forName(SCREEN).getConstructor(menu.getClass(),Inventory.class,Component.class).newInstance(menu,inventory,title);
                SCREENS.put(menu,new WeakReference<>(screen));return screen;
            }catch(ReflectiveOperationException failure){throw MineCellsWindows.upstream("Cell Crafter native screen",failure);}
        });
        DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","minecells/crafter"),100,Set.of(type),lookup->true,lookup->new Template());
        registered=true;
    });}
    public static AbstractContainerScreen<?> screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    public static boolean picker(AbstractContainerMenu menu){return Boolean.TRUE.equals(MineCellsWindows.field(screen(menu),"isRecipeListVisible"));}
    public static boolean textFocused(Object screen){
        if(!Boolean.TRUE.equals(MineCellsWindows.field(screen,"isRecipeListVisible")))return false;
        return MineCellsWindows.field(MineCellsWindows.field(screen,"recipeList"),"textBox") instanceof EditBox box&&box.isFocused();
    }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,Void> c){return Component.translatable("block.minecells.cell_crafter");}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,Void> c){
            Object root=MineCellsWindows.field(MineCellsWindows.field(screen(c.model()),"uiAdapter"),"rootComponent");
            if(root==null)return;
            try{
                Class<?> surface=Class.forName("io.wispforest.owo.ui.core.Surface");
                // Native rebuild() restores VANILLA_TRANSLUCENT, including on
                // mode changes. Clear it each preparation, keeping the world lit.
                ForeignMenuAccess.invoke(root,"surface",new Class<?>[]{surface},surface.getField("BLANK").get(null));
            }catch(ReflectiveOperationException failure){throw MineCellsWindows.upstream("Cell Crafter surface",failure);}
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c){
            if(picker(c.model()))return DesktopNativeLayout.builder()
                .keep("complete-parchment",new DesktopWindowBounds(-12,-5,200,176),2,4)
                .canvas(204,181).scrollable(false).build();
            return DesktopNativeLayout.builder().keep("native-crafting",new DesktopWindowBounds(3,8,170,70),4,4)
                .canvas(178,78).scrollable(false).build();
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c){
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(0,picker(c.model())?23:22,0,0),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> c){
            return new DesktopWindowDecoration<>(){
                @Override public DesktopChromeMetrics metrics(){return new DesktopChromeMetrics(22,7,8,11,11,3,6,5,0);}
                @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> draw,List<DesktopControlState> controls){
                    paintFrame(draw,picker(draw.model()));paintControls(draw,controls);
                }
            };
        }
        // The native browser disables all inventory slots. Prevent interactions
        // with the separate SIU inventory as well until the player returns.
        @Override public boolean modal(DesktopWindowContext<AbstractContainerMenu,Void> c){return picker(c.model());}
        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,Void> c,KeyEvent event){
            if(event.isEscape()&&picker(c.model())){ForeignMenuAccess.invoke(screen(c.model()),"toggleRecipeList");return true;}
            return false;
        }
        @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,Void> c){
            return picker(c.model())?List.of(new DesktopWindowBounds(c.contentX()+109,c.contentY()+31,70,9)):List.of();
        }
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,Void> c,ResourceLocation channel,byte[] bytes){
            if(!MineCellsWindows.SYNC.equals(channel))return;
            var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            try{
                var pos=data.readBlockPos();Object property=MineCellsWindows.field(c.model(),"blockPos");
                ForeignMenuAccess.invoke(property,"set",new Class<?>[]{Object.class},pos);
                int count=data.readInt();if(count<0||count>16384)throw new IllegalArgumentException("Invalid Cell Crafter recipe count");
                var recipes=new ArrayList<Object>();
                var recipeType=Class.forName("com.github.mim1q.minecells.recipe.CellForgeRecipe");
                var displayed=Class.forName(SCREEN.replace("CellCrafterScreen","CellCrafterRecipeList$DisplayedRecipe")).getConstructor(recipeType,boolean.class);
                for(int i=0;i<count;i++){
                    var id=data.readResourceLocation();boolean unlocked=data.readBoolean();
                    var recipe=c.minecraft().getConnection().getRecipeManager().byKey(id).orElseThrow();
                    recipes.add(displayed.newInstance(recipe,unlocked));
                }
                if(data.isReadable())throw new IllegalArgumentException("Trailing Cell Crafter recipes");
                ForeignMenuAccess.invoke(screen(c.model()),"updateRecipes",new Class<?>[]{List.class},recipes);
            }catch(ReflectiveOperationException failure){throw MineCellsWindows.upstream("Cell Crafter recipes",failure);}
            finally{data.release();}
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){SCREENS.remove(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "minecells-native-standard-controls-2";}
    }
    private static void paintFrame(DesktopRenderContext<AbstractContainerMenu,Void> c,boolean paper){
        int x=c.windowX(),y=c.windowY(),w=paper?204:178,h=paper?23:100;
        c.fill(x,y,x+w,y+h,0xff1e191b);
        c.fill(x+1,y+1,x+w-1,y+h-1,0xff774e24);
        c.fill(x+2,y+2,x+w-2,y+h-2,0xff182441);
        c.fill(x+2,y+2,x+w-2,y+3,0xffa87436);
        c.fill(x+2,y+2,x+3,y+h-2,0xff7f5d30);
        c.fill(x+3,y+21,x+w-3,y+22,0xff533a22);
    }
    private static void paintControls(DesktopRenderContext<AbstractContainerMenu,Void> c,List<DesktopControlState> controls){
        int x=c.windowX(),y=c.windowY(),w=c.windowWidth();
        int titleEnd=controls.stream().mapToInt(state->state.bounds().x()).min().orElse(x+w)-6;
        c.text(c.trimToWidth(Component.translatable("block.minecells.cell_crafter").getString(),Math.max(0,titleEnd-x-7)),x+7,y+8,0xfff4edd5,false);
        for(var state:controls){
            int col=switch(state.control()){case FOCUS->0;case CLOSE->2;case ELLIPSIS->3;case LOCK->5;case PIN->6;case LINK->9;default->-1;};
            if(col<0)continue;var b=state.bounds();
            c.texture(CONTROLS,b.x(),b.y(),col*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
        }
    }
}
