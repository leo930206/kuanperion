// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

final class MinecraftGlyphQuad
implements VertexConsumer {
    private final Sink sink;
    final float[] x = new float[4];
    final float[] y = new float[4];
    final float[] u = new float[4];
    final float[] v = new float[4];
    int color;
    private int vertices;

    MinecraftGlyphQuad(Sink sink) {
        this.sink = sink;
    }

    public VertexConsumer addVertex(float x, float y, float z) {
        if (this.vertices == 4) {
            this.flush();
        }
        this.x[this.vertices] = x;
        this.y[this.vertices] = y;
        ++this.vertices;
        return this;
    }

    public VertexConsumer setColor(int color) {
        this.color = color;
        return this;
    }

    public VertexConsumer setColor(int r, int g, int b, int a) {
        return this.setColor(a << 24 | r << 16 | g << 8 | b);
    }

    public VertexConsumer setUv(float u, float v) {
        this.u[this.vertices - 1] = u;
        this.v[this.vertices - 1] = v;
        return this;
    }

    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    public VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }

    public VertexConsumer setLineWidth(float width) {
        return this;
    }

    void flush() {
        if (this.vertices == 0) {
            return;
        }
        if (this.vertices != 4) {
            throw new IllegalStateException("Incomplete Minecraft glyph quad");
        }
        this.sink.draw(this);
        this.vertices = 0;
    }

    static interface Sink {
        public void draw(MinecraftGlyphQuad var1);
    }
}
