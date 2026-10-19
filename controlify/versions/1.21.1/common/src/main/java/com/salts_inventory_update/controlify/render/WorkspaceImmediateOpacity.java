package com.salts_inventory_update.controlify.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
/** Uses the native immediate shader scope on1.21.1; queued versions capture per-state opacity. */
public final class WorkspaceImmediateOpacity implements AutoCloseable {
    private final GuiGraphicsExtractor graphics;
    private final float[] previous;
    private final WorkspaceOpacity.Scope scope;
    private WorkspaceImmediateOpacity(GuiGraphicsExtractor graphics,float opacity) {
        this.graphics=graphics;graphics.unwrap().flush();previous=RenderSystem.getShaderColor().clone();scope=WorkspaceOpacity.enter(opacity);
        RenderSystem.setShaderColor(previous[0],previous[1],previous[2],previous[3]*opacity);
    }
    public static WorkspaceImmediateOpacity enter(GuiGraphicsExtractor graphics,float opacity) {return new WorkspaceImmediateOpacity(graphics,opacity);}
    @Override public void close() {try {graphics.unwrap().flush();}finally {RenderSystem.setShaderColor(previous[0],previous[1],previous[2],previous[3]);scope.close();}}
}
