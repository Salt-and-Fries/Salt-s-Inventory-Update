package com.salts_inventory_update.controlify;

import com.salts_inventory_update.controlify.model.CursorWarpEchoTracker;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;

/** Distinguishes only native keyboard-transition warp echoes from real mouse input. */
public final class NativeCursorWarpGuard {
    private static final CursorWarpEchoTracker ECHOES = new CursorWarpEchoTracker();
    private static final ThreadLocal<RadialInventoryScreen> TRANSITION = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ECHO_CALLBACK = ThreadLocal.withInitial(() -> false);
    private static long matchedCallbacks;
    private NativeCursorWarpGuard() { }
    public static void keyboardTransition(RadialInventoryScreen parent, Runnable transition) {
        var previous=TRANSITION.get();TRANSITION.set(parent);
        try { transition.run(); }
        finally { if(previous==null)TRANSITION.remove();else TRANSITION.set(previous); }
    }
    public static void nativeWarp(long window, double x, double y, Runnable warp) {
        var parent=TRANSITION.get();
        if(parent==null || workspace()!=parent || window!=Minecraft.getInstance().getWindow().getWindow()) { warp.run();return; }
        var receipt=ECHOES.record(parent,window,x,y,System.nanoTime());
        try {
            warp.run();
            // GLFW may round fractional physical positions; retain its exact resulting coordinates.
            double[] actualX=new double[1],actualY=new double[1];
            GLFW.glfwGetCursorPos(window,actualX,actualY);
            if(receipt!=null)receipt.actualPosition(actualX[0],actualY[0]);
        } catch(RuntimeException|Error failure) { ECHOES.discard(receipt);throw failure; }
    }
    /** Native 2.x briefly grabs then releases the cursor, which can restore its previous position. */
    public static void nativeCursorMode(long window,int mode,int value,Runnable change) {
        var parent=TRANSITION.get();
        if(mode!=GLFW.GLFW_CURSOR || parent==null || workspace()!=parent
            || window!=Minecraft.getInstance().getWindow().getWindow()
            || GLFW.glfwGetInputMode(window,mode)==value) { change.run();return; }
        double[] beforeX=new double[1],beforeY=new double[1];
        GLFW.glfwGetCursorPos(window,beforeX,beforeY);
        var before=ECHOES.record(parent,window,beforeX[0],beforeY[0],System.nanoTime());
        try {
            change.run();
            double[] afterX=new double[1],afterY=new double[1];
            GLFW.glfwGetCursorPos(window,afterX,afterY);
            if(beforeX[0]!=afterX[0] || beforeY[0]!=afterY[0])
                ECHOES.record(parent,window,afterX[0],afterY[0],System.nanoTime());
        } catch(RuntimeException|Error failure) { ECHOES.discard(before);throw failure; }
    }
    public static GLFWCursorPosCallbackI wrap(GLFWCursorPosCallbackI original) {
        if(original==null)return null;
        return (window,x,y) -> {
            boolean echo=ECHOES.consume(workspace(),window,x,y,System.nanoTime());
            boolean previous=ECHO_CALLBACK.get();
            ECHO_CALLBACK.set(previous||echo);
            if(echo)matchedCallbacks++;
            try { original.invoke(window,x,y); }
            finally { if(previous)ECHO_CALLBACK.set(true);else ECHO_CALLBACK.remove(); }
        };
    }
    public static boolean knownWarpCallback() { return ECHO_CALLBACK.get(); }
    private static RadialInventoryScreen workspace() {
        var screen=Minecraft.getInstance().screen;
        if(screen instanceof RadialInventoryScreen parent)return parent;
        return screen instanceof WorkspaceSearchKeyboard keyboard?keyboard.workspaceParent():null;
    }
}
