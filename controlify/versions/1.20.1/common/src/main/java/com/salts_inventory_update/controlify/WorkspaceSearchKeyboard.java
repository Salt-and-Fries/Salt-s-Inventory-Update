package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.KeyEvent;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.HatState;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenkeyboard.ChatKeyboardWidget;
import dev.isxander.controlify.screenkeyboard.KeyPressConsumer;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Uses the native Controlify 2.x keyboard, which predates KeyboardOverlayScreen. */
public final class WorkspaceSearchKeyboard extends Screen implements ScreenProcessorProvider {
    private static WorkspaceSearchKeyboard owner;
    private static boolean exitAwaitingNeutral;
    private final RadialInventoryScreen parent;
    private final DesktopWorkspace desktop;
    private final UUID window;
    private final ControllerEntity openingController;
    private final InputMode returnInputMode;
    private final boolean returnVirtualMouseEnabled;
    private final Set<Integer> heldKeys = new HashSet<>(), heldMouse = new HashSet<>();
    private ChatKeyboardWidget keyboard;
    private ScreenProcessor<WorkspaceSearchKeyboard> processor;
    private boolean openingAwaitingNeutral = true, closed;
    private char pendingHighSurrogate;
    private WorkspaceSearchKeyboard(RadialInventoryScreen parent, DesktopWorkspace desktop, UUID window) {
        super(Component.translatable("controlify.binding.salts_inventory_controlify.creative_search"));
        this.parent=parent;this.desktop=desktop;this.window=window;
        openingController=ControlifyApi.get().getCurrentController().orElse(null);
        returnVirtualMouseEnabled=Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled();
        InputMode mode=ControlifyApi.get().currentInputMode();
        returnInputMode=mode.isController()?mode:openingController!=null&&openingController.input().map(input->input.config().config().mixedInput).orElse(false)?InputMode.MIXED:InputMode.CONTROLLER;
    }
    public static boolean open(RadialInventoryScreen parent, DesktopWorkspace desktop, UUID window, DesktopWindowBounds bounds) {
        if(bounds==null) return false;
        CreativeSearchTarget target=new CreativeSearchTarget(window,bounds);
        if(target.current(desktop)==null) return false;
        return install(parent,desktop,window,()->target.focus(desktop));
    }
    public static boolean openFocused(RadialInventoryScreen parent, DesktopWorkspace desktop, UUID window,double x,double y) {
        if(!desktop.textInputActive(window)||!Double.isFinite(x)||!Double.isFinite(y)) return false;
        return install(parent,desktop,window,()-> {
            try {desktop.pointer(window,DesktopWorkspace.PointerAction.PRESS,x,y,0,0,0);return desktop.textInputActive(window);}
            finally {desktop.pointer(window,DesktopWorkspace.PointerAction.RELEASE,x,y,0,0,0);}
        });
    }
    private static boolean install(RadialInventoryScreen parent,DesktopWorkspace desktop,UUID window,BooleanSupplier focus) {
        Minecraft client=Minecraft.getInstance();
        if(parent==null||desktop==null||window==null||client.screen!=parent||active()||waitingNeutral()||!desktop.isValid()) return false;
        WorkspaceSearchKeyboard overlay=new WorkspaceSearchKeyboard(parent,desktop,window);owner=overlay;
        NativeCursorWarpGuard.keyboardTransition(parent,()->client.setScreen(overlay));
        boolean focused=false;
        try {focused=focus.getAsBoolean();return focused&&client.screen==overlay;}
        finally {if(!focused&&client.screen==overlay)overlay.onClose();}
    }
    @Override protected void init() {
        int keyboardWidth=Math.max(1,width*9/10),keyboardHeight=Math.max(1,height*2/5);
        keyboard=addRenderableWidget(new ChatKeyboardWidget(this,(width-keyboardWidth)/2,height-keyboardHeight-6,keyboardWidth,keyboardHeight,KeyPressConsumer.of(
            (key,scan,mods)->sendKey(key,scan,mods),
            (character,mods)-> {
                if(!ownsTarget()||openingAwaitingNeutral)return;
                if(Character.isHighSurrogate(character)){pendingHighSurrogate=character;return;}
                int codepoint=character;
                if(Character.isLowSurrogate(character)){if(pendingHighSurrogate==0)return;codepoint=Character.toCodePoint(pendingHighSurrogate,character);}
                pendingHighSurrogate=0;desktop.charTyped(codepoint,mods);
            })));
        setInitialFocus(keyboard);
    }
    private boolean ownsTarget(){return !closed&&Minecraft.getInstance().screen==this&&desktop.isValid()&&desktop.windows().stream().anyMatch(candidate->candidate.handle().instanceId().equals(window)&&candidate.handle().isOpen());}
    private void sendKey(int key,int scan,int mods){
        if(!ownsTarget()||openingAwaitingNeutral)return;
        if(key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){onClose();return;}
        desktop.keyPressed(key,scan,mods);desktop.keyReleased(key,scan,mods);
    }
    @Override public ScreenProcessor<WorkspaceSearchKeyboard> screenProcessor(){
        if(processor==null)processor=new ScreenProcessor<>(this){
            @Override public void onControllerUpdate(dev.isxander.controlify.controller.ControllerEntity controller){
                if(!ownsTarget())return;
                var input=controller.input().orElseThrow();
                if(openingAwaitingNeutral){
                    float threshold=input.config().config().buttonActivationThreshold;
                    if(neutral(input.stateNow(),threshold)&&neutral(input.stateThen(),threshold))openingAwaitingNeutral=false;
                    return;
                }
                // Native 2.x keys consume component button handling, including Back.
                // Keep its configured binding while this modal adapter owns the target.
                if(dev.isxander.controlify.bindings.ControlifyBindings.GUI_BACK.on(controller).justPressed()){
                    WorkspaceSearchKeyboard.this.onClose();
                    return;
                }
                super.onControllerUpdate(controller);
            }
        };
        return processor;
    }
    public static boolean active(){return owner!=null&&!owner.closed&&Minecraft.getInstance().screen==owner;}
    public static boolean activeFor(Screen parent){return active()&&owner.parent==parent;}
    RadialInventoryScreen workspaceParent(){return parent;}
    public static boolean waitingNeutral(){return exitAwaitingNeutral||owner!=null&&owner.closed&&(!owner.heldKeys.isEmpty()||!owner.heldMouse.isEmpty());}
    public static boolean suppressWorkspaceInput(ControllerStateView state,float threshold){
        if(active())return true;
        if(exitAwaitingNeutral){if(neutral(state,threshold))exitAwaitingNeutral=false;return true;}
        return waitingNeutral();
    }
    public static boolean suppressWorkspaceKey(KeyEvent event,boolean release){
        if(active())return true;if(owner==null)return false;
        boolean consumed=waitingNeutral()||owner.heldKeys.contains(event.key());
        if(release)owner.heldKeys.remove(event.key());return consumed;
    }
    public static boolean suppressWorkspaceMouse(MouseButtonEvent event,boolean release){
        if(active())return true;if(owner==null)return false;
        boolean consumed=waitingNeutral()||owner.heldMouse.contains(event.button());
        if(release)owner.heldMouse.remove(event.button());return consumed;
    }
    public static void resetFor(Screen parent){if(owner!=null&&owner.parent==parent){owner.closed=true;owner.heldKeys.clear();owner.heldMouse.clear();owner=null;exitAwaitingNeutral=false;}}
    private static boolean neutral(ControllerStateView state,float threshold){
        if(state==null)return true;
        for(var button:state.getButtons())if(state.isButtonDown(button))return false;
        for(var axis:state.getAxes())if(Math.abs(state.getAxisState(axis)-state.getAxisResting(axis))>.01f)return false;
        for(var hat:state.getHats())if(state.getHatState(hat)!=HatState.CENTERED)return false;
        return true;
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partialTick){
        parent.render(graphics,Integer.MIN_VALUE,Integer.MIN_VALUE,partialTick);
        graphics.fill(0,height*3/5-12,width,height,0xCC151515);
        super.render(graphics,x,y,partialTick);
    }
    @Override public boolean mouseClicked(double x,double y,int button){heldMouse.add(button);if(!openingAwaitingNeutral&&keyboard.isMouseOver(x,y)){setFocused(keyboard);keyboard.mouseClicked(x,y,button);}return true;}
    @Override public boolean mouseReleased(double x,double y,int button){heldMouse.remove(button);keyboard.mouseReleased(x,y,button);return true;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(!openingAwaitingNeutral)keyboard.mouseDragged(x,y,button,dx,dy);return true;}
    @Override public boolean mouseScrolled(double x,double y,double amount){return true;}
    @Override public boolean keyPressed(int key,int scan,int mods){heldKeys.add(key);sendKey(key,scan,mods);return true;}
    @Override public boolean keyReleased(int key,int scan,int mods){heldKeys.remove(key);if(desktop.isValid())desktop.keyReleased(key,scan,mods);return true;}
    @Override public boolean charTyped(char character,int mods){if(ownsTarget()&&!openingAwaitingNeutral)desktop.charTyped(character,mods);return true;}
    @Override public void onClose(){
        if(closed||Minecraft.getInstance().screen!=this)return;closed=true;exitAwaitingNeutral=true;
        if(desktop.isValid())for(int key:heldKeys)desktop.keyReleased(key,0,0);
        if(openingController!=null&&ControlifyApi.get().getCurrentController().orElse(null)==openingController&&!ControlifyApi.get().currentInputMode().isController())ControlifyApi.get().setInputMode(returnInputMode);
        NativeCursorWarpGuard.keyboardTransition(parent,()->{
            Minecraft.getInstance().setScreen(parent);
            if(!returnVirtualMouseEnabled)Controlify.instance().virtualMouseHandler().disableVirtualMouse();
        });
    }
    @Override public void removed(){if(!closed){closed=true;heldKeys.clear();heldMouse.clear();exitAwaitingNeutral=false;if(owner==this)owner=null;}}
    @Override public boolean isPauseScreen(){return false;}
}
