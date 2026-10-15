package com.salts_inventory_update.controlify.render;

import net.minecraft.client.renderer.state.gui.pip.*;
import org.joml.Matrix3x2fc;

/** Keeps native concrete renderer dispatch while moving pose-independent previews into their window. */
public final class WorkspacePictureInPicture {
    private WorkspacePictureInPicture() {}

    public static PictureInPictureRenderState transform(PictureInPictureRenderState state, Matrix3x2fc pose) {
        if (!WorkspaceRenderTransform.active()) return state;
        var p = WorkspacePreviewTransform.map(state.x0(), state.y0(), state.x1(), state.y1(),
                pose.m00(), pose.m10(), pose.m20(), pose.m01(), pose.m11(), pose.m21());
        if (state instanceof GuiEntityRenderState entity)
            return new GuiEntityRenderState(entity.renderState(), entity.translation(), entity.rotation(),
                    entity.overrideCameraAngle(), p.x0(), p.y0(), p.x1(), p.y1(), p.modelScale(entity.scale()), entity.scissorArea());
        if (state instanceof GuiSkinRenderState skin)
            return new GuiSkinRenderState(skin.playerModel(), skin.texture(), skin.rotationX(), skin.rotationY(), skin.pivotY(),
                    p.x0(), p.y0(), p.x1(), p.y1(), p.modelScale(skin.scale()), skin.scissorArea());
        if (state instanceof GuiBookModelRenderState book)
            return new GuiBookModelRenderState(book.bookModel(), book.texture(), book.open(), book.flip(),
                    p.x0(), p.y0(), p.x1(), p.y1(), p.modelScale(book.scale()), book.scissorArea());
        if (state instanceof GuiBannerResultRenderState banner) {
            var transformed = new GuiBannerResultRenderState(banner.flag(), banner.baseColor(), banner.resultBannerPatterns(),
                    p.x0(), p.y0(), p.x1(), p.y1(), banner.scissorArea());
            ((WorkspacePictureScaleAccess) (Object) transformed).saltsControlifyPreviewScale(p.scaleFactor());
            return transformed;
        }
        return state;
    }
}
