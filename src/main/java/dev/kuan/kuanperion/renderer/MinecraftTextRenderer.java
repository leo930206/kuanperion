// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.renderer;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kuan.kuanperion.renderer.MinecraftGlyphQuad;
import dev.kuan.kuanperion.renderer.NVGRenderer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;

final class MinecraftTextRenderer
implements Font.GlyphVisitor {
    private static final Matrix4fc IDENTITY = new Matrix4f();
    private static final int[] TEXTURE_PARAMETERS = new int[]{10241, 10240, 10242, 10243, 36418, 36419, 36420, 36421};
    private final long vg;
    private final NVGPaint paint;
    private final Map<GlTexture, Atlas> atlases = new IdentityHashMap<GlTexture, Atlas>();
    private final MinecraftGlyphQuad quad = new MinecraftGlyphQuad(this::drawQuad);
    private int image;

    MinecraftTextRenderer(long vg, NVGPaint paint) {
        this.vg = vg;
        this.paint = paint;
    }

    void beginFrame() {
        this.atlases.entrySet().removeIf(entry -> {
            if (!((GlTexture)entry.getKey()).isClosed()) {
                return false;
            }
            NanoVG.nvgDeleteImage((long)this.vg, (int)((Atlas)entry.getValue()).image);
            return true;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void endFrame() {
        int binding = GL11.glGetInteger((int)32873);
        try {
            for (Map.Entry<GlTexture, Atlas> entry : this.atlases.entrySet()) {
                Atlas atlas = entry.getValue();
                if (!atlas.active) continue;
                if (!entry.getKey().isClosed()) {
                    GL11.glBindTexture((int)3553, (int)entry.getKey().glId());
                    for (int i = 0; i < TEXTURE_PARAMETERS.length; ++i) {
                        GL11.glTexParameteri((int)3553, (int)TEXTURE_PARAMETERS[i], (int)atlas.savedParameters[i]);
                    }
                }
                atlas.active = false;
            }
        }
        finally {
            GL11.glBindTexture((int)3553, (int)binding);
        }
    }

    public void acceptGlyph(TextRenderable.Styled glyph) {
        this.draw((TextRenderable)glyph);
    }

    public void acceptEffect(TextRenderable effect) {
        this.draw(effect);
    }

    private void draw(TextRenderable glyph) {
        GlTexture texture;
        GpuTexture gpuTexture = glyph.textureView().texture();
        if (!(gpuTexture instanceof GlTexture) || (texture = (GlTexture)gpuTexture).isClosed()) {
            return;
        }
        Atlas atlas = this.atlases.computeIfAbsent(texture, this::createAtlas);
        if (!atlas.active) {
            this.activate(texture, atlas);
        }
        this.image = atlas.image;
        glyph.render(IDENTITY, (VertexConsumer)this.quad, 0xF000F0, true);
        this.quad.flush();
    }

    private Atlas createAtlas(GlTexture texture) {
        int handle = NanoVGGL3.nvglCreateImageFromHandle((long)this.vg, (int)texture.glId(), (int)texture.getWidth(0), (int)texture.getHeight(0), (int)65568);
        if (handle == 0) {
            throw new IllegalStateException("Unable to import Minecraft font texture into NanoVG");
        }
        return new Atlas(handle);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void activate(GlTexture texture, Atlas atlas) {
        int binding = GL11.glGetInteger((int)32873);
        try {
            GL11.glBindTexture((int)3553, (int)texture.glId());
            for (int i = 0; i < TEXTURE_PARAMETERS.length; ++i) {
                atlas.savedParameters[i] = GL11.glGetTexParameteri((int)3553, (int)TEXTURE_PARAMETERS[i]);
            }
            atlas.active = true;
            GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
            GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
            if (texture.getFormat() == GpuFormat.R8_UNORM) {
                GL11.glTexParameteri((int)3553, (int)36418, (int)1);
                GL11.glTexParameteri((int)3553, (int)36419, (int)1);
                GL11.glTexParameteri((int)3553, (int)36420, (int)1);
                GL11.glTexParameteri((int)3553, (int)36421, (int)6403);
            }
        }
        finally {
            GL11.glBindTexture((int)3553, (int)binding);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drawQuad(MinecraftGlyphQuad q) {
        float du = q.u[3] - q.u[0];
        float dv = q.v[1] - q.v[0];
        if (du == 0.0f || dv == 0.0f || q.color >>> 24 == 0) {
            return;
        }
        NanoVG.nvgSave((long)this.vg);
        try {
            NanoVG.nvgShapeAntiAlias((long)this.vg, (boolean)false);
            NanoVG.nvgTransform((long)this.vg, (float)(q.x[3] - q.x[0]), (float)(q.y[3] - q.y[0]), (float)(q.x[1] - q.x[0]), (float)(q.y[1] - q.y[0]), (float)q.x[0], (float)q.y[0]);
            NanoVG.nvgImagePattern((long)this.vg, (float)(-q.u[0] / du), (float)(-q.v[0] / dv), (float)(1.0f / du), (float)(1.0f / dv), (float)0.0f, (int)this.image, (float)1.0f, (NVGPaint)this.paint);
            NVGRenderer.color(q.color, this.paint.innerColor());
            NVGRenderer.color(q.color, this.paint.outerColor());
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgRect((long)this.vg, (float)0.0f, (float)0.0f, (float)1.0f, (float)1.0f);
            NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
            NanoVG.nvgFill((long)this.vg);
        }
        finally {
            NanoVG.nvgRestore((long)this.vg);
        }
    }

    private static final class Atlas {
        final int image;
        final int[] savedParameters = new int[TEXTURE_PARAMETERS.length];
        boolean active;

        Atlas(int image) {
            this.image = image;
        }
    }
}
