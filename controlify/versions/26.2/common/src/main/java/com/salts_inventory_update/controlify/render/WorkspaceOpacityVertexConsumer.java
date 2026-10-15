package com.salts_inventory_update.controlify.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Delegates native geometry while applying the captured opacity to every emitted color. */
public final class WorkspaceOpacityVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float opacity;
    private final boolean premultiplied;

    public WorkspaceOpacityVertexConsumer(VertexConsumer delegate, float opacity, boolean premultiplied) {
        this.delegate = delegate; this.opacity = opacity; this.premultiplied = premultiplied;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(premultiplied ? WorkspaceOpacity.channel(r, opacity) : r,
                premultiplied ? WorkspaceOpacity.channel(g, opacity) : g,
                premultiplied ? WorkspaceOpacity.channel(b, opacity) : b, WorkspaceOpacity.channel(a, opacity));
        return this;
    }
    @Override public VertexConsumer setColor(int argb) { delegate.setColor(WorkspaceOpacity.color(argb, opacity, premultiplied)); return this; }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    @Override public VertexConsumer setLineWidth(float width) { delegate.setLineWidth(width); return this; }
}
