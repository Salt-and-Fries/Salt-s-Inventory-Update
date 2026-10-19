package com.salts_inventory_update.controlify.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
/** Supplies native tinting absent from the pre-pipeline blit signature. */
final class WorkspaceImmediateTint implements AutoCloseable {
    private final GuiGraphicsExtractor graphics;
    private final float[] previous;
    private WorkspaceImmediateTint(GuiGraphicsExtractor graphics,int color) {
        this.graphics=graphics;graphics.unwrap().flush();previous=RenderSystem.getShaderColor().clone();
        RenderSystem.setShaderColor(previous[0]*((color>>>16)&255)/255f,previous[1]*((color>>>8)&255)/255f,previous[2]*(color&255)/255f,previous[3]*(color>>>24)/255f);
    }
    static WorkspaceImmediateTint enter(GuiGraphicsExtractor graphics,int color) {return color==0xFFFFFFFF?null:new WorkspaceImmediateTint(graphics,color);}
    @Override public void close(){try {graphics.unwrap().flush();}finally {RenderSystem.setShaderColor(previous[0],previous[1],previous[2],previous[3]);}}
}
