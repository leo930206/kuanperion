// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.renderer;

import dev.kuan.kuanperion.renderer.MinecraftTextRenderer;
import dev.kuan.kuanperion.renderer.NanoVGManager;
import dev.kuan.kuanperion.renderer.SVGRenderer;
import dev.kuan.kuanperion.util.UiLang;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

public class NVGRenderer {
    private final long vg;
    private final NVGColor c1 = NVGColor.malloc();
    private final NVGColor c2 = NVGColor.malloc();
    private final NVGPaint paint = NVGPaint.malloc();
    private final MinecraftTextRenderer minecraftText;
    private BlockIconCache blockIcons;
    private final float[] fontBounds = new float[4];
    private ScissorRegion scissorStack = null;
    private static final int MAX_TEXT_WIDTH_CACHE = 512;
    private final Map<String, Float> textWidthCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Float> eldest) { return size() > MAX_TEXT_WIDTH_CACHE; }
    };
    private String lastTextKey = null;
    private float textScale = 1.0f;
    private static final float HALF_PI = 1.5707964f;

    NVGRenderer(long vg) {
        this.vg = vg;
        this.minecraftText = new MinecraftTextRenderer(vg, this.paint);
    }

    void beginMinecraftTextFrame() {
        this.minecraftText.beginFrame();
    }

    void endMinecraftTextFrame() {
        this.minecraftText.endFrame();
    }

    public void minecraftText(Font.PreparedText text) {
        text.visit((Font.GlyphVisitor)this.minecraftText);
    }

    public void rect(float x, float y, float w, float h, int color) {
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgRect((long)this.vg, (float)x, (float)y, (float)w, (float)h);
        NVGRenderer.color(color, this.c1);
        NanoVG.nvgFillColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgFill((long)this.vg);
    }

    public void roundedRect(float x, float y, float w, float h, float radius, int color) {
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgRoundedRect((long)this.vg, (float)x, (float)y, (float)w, (float)h, (float)radius);
        NVGRenderer.color(color, this.c1);
        NanoVG.nvgFillColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgFill((long)this.vg);
    }

    public void rectOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
        float half = thickness / 2.0f;
        float r = Math.min(radius, Math.min(w, h) / 2.0f);
        NVGRenderer.color(color, this.c1);
        if (r < 0.5f) {
            this.rect(x - half, y - half, w + thickness, thickness, color);
            this.rect(x - half, y + h - half, w + thickness, thickness, color);
            this.rect(x - half, y + half, thickness, h - thickness, color);
            this.rect(x + w - half, y + half, thickness, h - thickness, color);
            return;
        }
        NanoVG.nvgStrokeWidth((long)this.vg, (float)thickness);
        NanoVG.nvgLineCap((long)this.vg, (int)0);
        NanoVG.nvgStrokeColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgMoveTo((long)this.vg, (float)(x + w / 2.0f), (float)y);
        NanoVG.nvgLineTo((long)this.vg, (float)(x + w - r), (float)y);
        NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + r), (float)r, (float)4.712389f, (float)((float)Math.PI * 2), (int)2);
        NanoVG.nvgLineTo((long)this.vg, (float)(x + w), (float)(y + h - r));
        NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + h - r), (float)r, (float)0.0f, (float)1.5707964f, (int)2);
        NanoVG.nvgLineTo((long)this.vg, (float)(x + r), (float)(y + h));
        NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + h - r), (float)r, (float)1.5707964f, (float)((float)Math.PI), (int)2);
        NanoVG.nvgLineTo((long)this.vg, (float)x, (float)(y + r));
        NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + r), (float)r, (float)((float)Math.PI), (float)4.712389f, (int)2);
        NanoVG.nvgLineTo((long)this.vg, (float)(x + w / 2.0f), (float)y);
        NanoVG.nvgStroke((long)this.vg);
    }

    public void rectOutlineSolid(float x, float y, float w, float h, float radius, float thickness, int color) {
        this.rectOutline(x, y, w, h, radius, thickness, NVGRenderer.opaqueFromAlpha(color));
    }

    public void rectOutlineVerticalSides(float x, float y, float w, float h, float radius, float thickness, int color, float ratio) {
        if (ratio <= 0.0f) {
            return;
        }
        if (ratio >= 1.0f) {
            this.rectOutlineSolid(x, y, w, h, radius, thickness, color);
            return;
        }
        float r = Math.min(radius, Math.min(w, h) / 2.0f);
        float half = thickness / 2.0f;
        int c = NVGRenderer.opaqueFromAlpha(color);
        if (ratio <= 0.4f) {
            float pad = h / 2.0f - ratio / 0.4f * (h / 2.0f - half);
            NanoVG.nvgSave((long)this.vg);
            NanoVG.nvgIntersectScissor((long)this.vg, (float)(x - half), (float)(y + pad), (float)(w + thickness), (float)(h - pad * 2.0f));
            this.rectOutlineSolid(x, y, w, h, radius, thickness, color);
            NanoVG.nvgRestore((long)this.vg);
        } else {
            NVGRenderer.color(c, this.c1);
            NanoVG.nvgStrokeWidth((long)this.vg, (float)thickness);
            NanoVG.nvgLineCap((long)this.vg, (int)0);
            NanoVG.nvgStrokeColor((long)this.vg, (NVGColor)this.c1);
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgMoveTo((long)this.vg, (float)(x + r), (float)y);
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + r), (float)r, (float)4.712389f, (float)((float)Math.PI), (int)1);
            NanoVG.nvgLineTo((long)this.vg, (float)x, (float)(y + h - r));
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + h - r), (float)r, (float)((float)Math.PI), (float)1.5707964f, (int)1);
            NanoVG.nvgMoveTo((long)this.vg, (float)(x + w - r), (float)y);
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + r), (float)r, (float)4.712389f, (float)((float)Math.PI * 2), (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w), (float)(y + h - r));
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + h - r), (float)r, (float)0.0f, (float)1.5707964f, (int)2);
            NanoVG.nvgStroke((long)this.vg);
            float segLen = (ratio - 0.4f) / 0.6f * (w / 2.0f - r);
            this.rect(x + r, y - half, segLen, thickness, c);
            this.rect(x + w - r - segLen, y - half, segLen, thickness, c);
            this.rect(x + r, y + h - half, segLen, thickness, c);
            this.rect(x + w - r - segLen, y + h - half, segLen, thickness, c);
        }
    }

    public void rectOutlineVerticalSidesGradient(float x, float y, float w, float h, float radius, float thickness, int colorTL, int colorBR, float ratio) {
        if (ratio <= 0.0f) {
            return;
        }
        int c1c = NVGRenderer.opaqueFromAlpha(colorTL);
        int c2c = NVGRenderer.opaqueFromAlpha(colorBR);
        NVGRenderer.color(c1c, this.c1);
        NVGRenderer.color(c2c, this.c2);
        NanoVG.nvgLinearGradient((long)this.vg, (float)x, (float)y, (float)(x + w), (float)(y + h), (NVGColor)this.c1, (NVGColor)this.c2, (NVGPaint)this.paint);
        float r = Math.min(radius, Math.min(w, h) / 2.0f);
        float half = thickness / 2.0f;
        NanoVG.nvgStrokeWidth((long)this.vg, (float)thickness);
        NanoVG.nvgLineCap((long)this.vg, (int)0);
        NanoVG.nvgStrokePaint((long)this.vg, (NVGPaint)this.paint);
        if (ratio >= 1.0f) {
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgMoveTo((long)this.vg, (float)(x + w / 2.0f), (float)y);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w - r), (float)y);
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + r), (float)r, (float)4.712389f, (float)((float)Math.PI * 2), (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w), (float)(y + h - r));
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + h - r), (float)r, (float)0.0f, (float)1.5707964f, (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + r), (float)(y + h));
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + h - r), (float)r, (float)1.5707964f, (float)((float)Math.PI), (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)x, (float)(y + r));
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + r), (float)r, (float)((float)Math.PI), (float)4.712389f, (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w / 2.0f), (float)y);
            NanoVG.nvgStroke((long)this.vg);
        } else if (ratio <= 0.4f) {
            float pad = h / 2.0f - ratio / 0.4f * (h / 2.0f - half);
            NanoVG.nvgSave((long)this.vg);
            NanoVG.nvgIntersectScissor((long)this.vg, (float)(x - half), (float)(y + pad), (float)(w + thickness), (float)(h - pad * 2.0f));
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgMoveTo((long)this.vg, (float)(x + w / 2.0f), (float)y);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w - r), (float)y);
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + r), (float)r, (float)4.712389f, (float)((float)Math.PI * 2), (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w), (float)(y + h - r));
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + h - r), (float)r, (float)0.0f, (float)1.5707964f, (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + r), (float)(y + h));
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + h - r), (float)r, (float)1.5707964f, (float)((float)Math.PI), (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)x, (float)(y + r));
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + r), (float)r, (float)((float)Math.PI), (float)4.712389f, (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w / 2.0f), (float)y);
            NanoVG.nvgStroke((long)this.vg);
            NanoVG.nvgRestore((long)this.vg);
        } else {
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgMoveTo((long)this.vg, (float)(x + r), (float)y);
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + r), (float)r, (float)4.712389f, (float)((float)Math.PI), (int)1);
            NanoVG.nvgLineTo((long)this.vg, (float)x, (float)(y + h - r));
            NanoVG.nvgArc((long)this.vg, (float)(x + r), (float)(y + h - r), (float)r, (float)((float)Math.PI), (float)1.5707964f, (int)1);
            NanoVG.nvgMoveTo((long)this.vg, (float)(x + w - r), (float)y);
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + r), (float)r, (float)4.712389f, (float)((float)Math.PI * 2), (int)2);
            NanoVG.nvgLineTo((long)this.vg, (float)(x + w), (float)(y + h - r));
            NanoVG.nvgArc((long)this.vg, (float)(x + w - r), (float)(y + h - r), (float)r, (float)0.0f, (float)1.5707964f, (int)2);
            NanoVG.nvgStroke((long)this.vg);
            float segLen = (ratio - 0.4f) / 0.6f * (w / 2.0f - r);
            NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgRect((long)this.vg, (float)(x + r), (float)(y - half), (float)segLen, (float)thickness);
            NanoVG.nvgRect((long)this.vg, (float)(x + w - r - segLen), (float)(y - half), (float)segLen, (float)thickness);
            NanoVG.nvgRect((long)this.vg, (float)(x + r), (float)(y + h - half), (float)segLen, (float)thickness);
            NanoVG.nvgRect((long)this.vg, (float)(x + w - r - segLen), (float)(y + h - half), (float)segLen, (float)thickness);
            NanoVG.nvgFill((long)this.vg);
        }
    }

    private static int opaqueFromAlpha(int argb) {
        int a = argb >> 24 & 0xFF;
        if (a == 255) {
            return argb;
        }
        int r = (argb >> 16 & 0xFF) * a / 255;
        int g = (argb >> 8 & 0xFF) * a / 255;
        int b = (argb & 0xFF) * a / 255;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public void circle(float cx, float cy, float radius, int color) {
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgCircle((long)this.vg, (float)cx, (float)cy, (float)radius);
        NVGRenderer.color(color, this.c1);
        NanoVG.nvgFillColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgFill((long)this.vg);
    }

    public void line(float x1, float y1, float x2, float y2, float thickness, int color) {
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgMoveTo((long)this.vg, (float)x1, (float)y1);
        NanoVG.nvgLineTo((long)this.vg, (float)x2, (float)y2);
        NanoVG.nvgStrokeWidth((long)this.vg, (float)thickness);
        NVGRenderer.color(color, this.c1);
        NanoVG.nvgStrokeColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgStroke((long)this.vg);
    }

    public void polyline(float[] points, int count, float thickness, int color) {
        if (count < 2) {
            return;
        }
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgMoveTo((long)this.vg, (float)points[0], (float)points[1]);
        for (int i = 1; i < count; ++i) {
            NanoVG.nvgLineTo((long)this.vg, (float)points[i * 2], (float)points[i * 2 + 1]);
        }
        NanoVG.nvgStrokeWidth((long)this.vg, (float)thickness);
        NVGRenderer.color(color, this.c1);
        NanoVG.nvgStrokeColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgStroke((long)this.vg);
    }

    public void linearGradient(float x, float y, float w, float h, float radius, int colorTop, int colorBottom) {
        NVGRenderer.color(colorTop, this.c1);
        NVGRenderer.color(colorBottom, this.c2);
        NanoVG.nvgLinearGradient((long)this.vg, (float)x, (float)y, (float)x, (float)(y + h), (NVGColor)this.c1, (NVGColor)this.c2, (NVGPaint)this.paint);
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgRoundedRect((long)this.vg, (float)x, (float)y, (float)w, (float)h, (float)radius);
        NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
        NanoVG.nvgFill((long)this.vg);
    }

    public void horizontalGradient(float x, float y, float w, float h, float radius, int colorLeft, int colorRight) {
        NVGRenderer.color(colorLeft, this.c1);
        NVGRenderer.color(colorRight, this.c2);
        NanoVG.nvgLinearGradient((long)this.vg, (float)x, (float)y, (float)(x + w), (float)y, (NVGColor)this.c1, (NVGColor)this.c2, (NVGPaint)this.paint);
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgRoundedRect((long)this.vg, (float)x, (float)y, (float)w, (float)h, (float)radius);
        NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
        NanoVG.nvgFill((long)this.vg);
    }

    public boolean blockIcon(String id,float x,float y,float size) {
        if(blockIcons==null)blockIcons=new BlockIconCache(vg,paint);
        return blockIcons.draw(id,x,y,size);
    }
    public void destroy() {
        if(blockIcons!=null)blockIcons.close();
        c1.free();c2.free();paint.free();
    }

    /** Soft corner washes: blue lower-left, pink upper-right; center remains white. */
    public void cornerWash(float x, float y, float w, float h, float radius, int blue, int pink, float coverage, float strength) {
        float reach = Math.max(w * coverage, h * coverage * 1.3125f);
        int[] colors = {blue, pink};
        float[] cx = {x + w * 0.10f, x + w * 0.96f};
        float[] cy = {y + h * 0.98f, y + h * 0.04f};
        for (int i=0; i<2; i++) {
            int tint=((int)((colors[i]>>>24)*strength)<<24)|(colors[i]&0xFFFFFF);
            color(tint, this.c1); color(colors[i] & 0xFFFFFF, this.c2);
            NanoVG.nvgRadialGradient(this.vg, cx[i], cy[i], 0, reach, this.c1, this.c2, this.paint);
            NanoVG.nvgBeginPath(this.vg);
            NanoVG.nvgRoundedRect(this.vg, x, y, w, h, radius);
            NanoVG.nvgFillPaint(this.vg, this.paint);
            NanoVG.nvgFill(this.vg);
        }
    }

    public void shadow(float x, float y, float w, float h, float radius, float blur, int color) {
        NVGRenderer.color(color, this.c1);
        NVGRenderer.color(color & 0xFFFFFF, this.c2);
        NanoVG.nvgBoxGradient((long)this.vg, (float)x, (float)(y + blur * 0.5f), (float)w, (float)h, (float)(radius + blur * 0.5f), (float)blur, (NVGColor)this.c1, (NVGColor)this.c2, (NVGPaint)this.paint);
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgRect((long)this.vg, (float)(x - blur), (float)(y - blur * 0.5f), (float)(w + blur * 2.0f), (float)(h + blur * 2.0f));
        NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
        NanoVG.nvgFill((long)this.vg);
    }

    public void glow(float x, float y, float w, float h, float radius, int color, float intensity) {
        intensity = Math.max(0.1f, Math.min(1.0f, intensity));
        int layers = (int)(intensity * 4.0f) + 1;
        int baseAlpha = color >> 24 & 0xFF;
        for (int i = 0; i < layers; ++i) {
            float spread = (float)(i + 1) * 6.0f * intensity;
            float feather = spread * 1.5f;
            int layerAlpha = (int)((float)baseAlpha * (1.0f - (float)i / (float)layers) * intensity);
            if (layerAlpha < 2) continue;
            NVGRenderer.color(layerAlpha << 24 | color & 0xFFFFFF, this.c1);
            NVGRenderer.color(color & 0xFFFFFF, this.c2);
            NanoVG.nvgBoxGradient((long)this.vg, (float)x, (float)y, (float)w, (float)h, (float)radius, (float)feather, (NVGColor)this.c1, (NVGColor)this.c2, (NVGPaint)this.paint);
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgRect((long)this.vg, (float)(x - spread), (float)(y - spread), (float)(w + spread * 2.0f), (float)(h + spread * 2.0f));
            NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
            NanoVG.nvgFill((long)this.vg);
        }
    }

    public void glowCircle(float cx, float cy, float radius, int color, float intensity) {
        intensity = Math.max(0.1f, Math.min(1.0f, intensity));
        int layers = (int)(intensity * 3.0f) + 1;
        int baseAlpha = color >> 24 & 0xFF;
        for (int i = 0; i < layers; ++i) {
            float spread = (float)(i + 1) * 8.0f * intensity;
            int layerAlpha = (int)((float)baseAlpha * (1.0f - (float)i / (float)layers) * intensity);
            if (layerAlpha < 2) continue;
            NVGRenderer.color(layerAlpha << 24 | color & 0xFFFFFF, this.c1);
            NVGRenderer.color(color & 0xFFFFFF, this.c2);
            NanoVG.nvgRadialGradient((long)this.vg, (float)cx, (float)cy, (float)(radius * 0.5f), (float)(radius + spread), (NVGColor)this.c1, (NVGColor)this.c2, (NVGPaint)this.paint);
            NanoVG.nvgBeginPath((long)this.vg);
            NanoVG.nvgCircle((long)this.vg, (float)cx, (float)cy, (float)(radius + spread));
            NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
            NanoVG.nvgFill((long)this.vg);
        }
        this.circle(cx, cy, radius, color);
    }

    public void blur(float x, float y, float w, float h, float blurRadius) {
        float clampedBlur = Math.min(blurRadius, 30.0f);
        this.rect(x, y, w, h, 0x40000000);
        this.shadow(x, y, w, h, 0.0f, clampedBlur, 0x28000000);
        this.rect(x, y, w, h, 0xCFFFFFF);
    }

    public void blur(float x, float y, float w, float h, float radius, float blurRadius) {
        float clampedBlur = Math.min(blurRadius, 30.0f);
        this.roundedRect(x, y, w, h, radius, 0x40000000);
        NVGRenderer.color(0x28000000, this.c1);
        NVGRenderer.color(0, this.c2);
        NanoVG.nvgBoxGradient((long)this.vg, (float)x, (float)(y + clampedBlur * 0.5f), (float)w, (float)h, (float)(radius + clampedBlur * 0.5f), (float)clampedBlur, (NVGColor)this.c1, (NVGColor)this.c2, (NVGPaint)this.paint);
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgRoundedRect((long)this.vg, (float)x, (float)y, (float)w, (float)h, (float)radius);
        NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
        NanoVG.nvgFill((long)this.vg);
        this.roundedRect(x, y, w, h, radius, 0xCFFFFFF);
    }

    public void blurCircle(float cx, float cy, float radius, float blurRadius) {
        float b = Math.min(Math.max(blurRadius, 0.0f), 30.0f);
        this.circle(cx, cy, radius, 0x40000000);
        int STEPS = 5;
        int MAX_A = 40;
        for (int i = 1; i <= 5; ++i) {
            float t = (float)i / 5.0f;
            float rr = radius + t * b;
            int a = Math.round(40.0f * (1.0f - t));
            int argb = (a & 0xFF) << 24;
            this.circle(cx, cy, rr, argb);
        }
        this.circle(cx, cy, radius, 0xCFFFFFF);
    }

    public void blurRingApprox(float cx, float cy, float radius, float blurRadius, float alpha) {
        float b = Math.min(Math.max(blurRadius, 0.0f), 30.0f);
        int STEPS = 5;
        int maxA = Math.round(Math.max(0.0f, Math.min(alpha, 1.0f)) * 40.0f);
        for (int i = 1; i <= 5; ++i) {
            float t = (float)i / 5.0f;
            float rr = radius + t * b;
            int a = Math.round((float)maxA * (1.0f - t));
            int argb = (a & 0xFF) << 24;
            this.circle(cx, cy, rr, argb);
        }
    }

    public void circleOutline(float cx, float cy, float radius, float strokeWidth, int color) {
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgCircle((long)this.vg, (float)cx, (float)cy, (float)Math.max(0.5f, radius));
        NanoVG.nvgStrokeWidth((long)this.vg, (float)Math.max(0.5f, strokeWidth));
        NVGRenderer.color(color, this.c1);
        NanoVG.nvgStrokeColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgStroke((long)this.vg);
    }

    public void renderSVG(String resourcePath, float x, float y, float width, float height, int color) {
        SVGRenderer.render(this.vg, resourcePath, x, y, width, height, color, this.paint);
    }

    public int createImageRGBA(int width, int height, ByteBuffer pixels) {
        return NanoVG.nvgCreateImageRGBA((long)this.vg, (int)width, (int)height, (int)0, (ByteBuffer)pixels);
    }

    public void deleteImage(int handle) {
        if (handle != -1) {
            NanoVG.nvgDeleteImage((long)this.vg, (int)handle);
        }
    }

    public void image(int handle, float x, float y, float width, float height, float radius, float alpha) {
        if (handle == -1) {
            return;
        }
        NanoVG.nvgSave((long)this.vg);
        NanoVG.nvgBeginPath((long)this.vg);
        NanoVG.nvgRoundedRect((long)this.vg, (float)x, (float)y, (float)width, (float)height, (float)radius);
        NanoVG.nvgImagePattern((long)this.vg, (float)x, (float)y, (float)width, (float)height, (float)0.0f, (int)handle, (float)alpha, (NVGPaint)this.paint);
        NanoVG.nvgFillPaint((long)this.vg, (NVGPaint)this.paint);
        NanoVG.nvgFill((long)this.vg);
        NanoVG.nvgRestore((long)this.vg);
    }

    public void text(String fontName, String text, float x, float y, float size, int color) {
        this.textLiteral(fontName, UiLang.localize(text), x, y, size * this.textScale, color);
    }

    public void textLiteral(String fontName, String text, float x, float y, float size, int color) {
        int fontId = NanoVGManager.getFontId(fontName);
        if (fontId == -1) {
            return;
        }
        NanoVG.nvgFontFaceId((long)this.vg, (int)fontId);
        NanoVG.nvgFontSize((long)this.vg, (float)size);
        NVGRenderer.color(color, this.c1);
        NanoVG.nvgFillColor((long)this.vg, (NVGColor)this.c1);
        NanoVG.nvgText((long)this.vg, (float)x, (float)(y + 0.5f), (CharSequence)text);
    }

    public void textCentered(String fontName, String text, float x, float y, float w, float h, float size, int color) {
        float tw = this.textWidth(fontName, text, size);
        float tx = x + (w - tw) / 2.0f;
        float renderedSize = size * this.textScale;
        float ty = y + (h - renderedSize) / 2.0f;
        this.text(fontName, text, tx, ty, size, color);
    }

    public void textRight(String fontName, String text, float x, float y, float w, float size, int color) {
        float tw = this.textWidth(fontName, text, size);
        this.text(fontName, text, x + w - tw, y, size, color);
    }

    public float textWidth(String fontName, String text, float size) {
        return this.textWidthLiteral(fontName, UiLang.localize(text), size * this.textScale);
    }

    public float textWidthLiteral(String fontName, String text, float size) {
        String key = fontName + "|" + text + "|" + size;
        if (key.equals(this.lastTextKey)) {
            return this.textWidthCache.getOrDefault(key, Float.valueOf(0.0f)).floatValue();
        }
        int fontId = NanoVGManager.getFontId(fontName);
        if (fontId == -1) {
            return 0.0f;
        }
        NanoVG.nvgFontFaceId((long)this.vg, (int)fontId);
        NanoVG.nvgFontSize((long)this.vg, (float)size);
        float w = NanoVG.nvgTextBounds((long)this.vg, (float)0.0f, (float)0.0f, (CharSequence)text, (float[])this.fontBounds);
        this.textWidthCache.put(key, Float.valueOf(w));
        this.lastTextKey = key;
        return w;
    }

    public List<String> wrapTextToWidth(String fontName, String text, float size, float maxWidth) {
        text = UiLang.localize(text);
        ArrayList<String> lines = new ArrayList<String>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        if (maxWidth <= 0.0f) {
            lines.add(text);
            return lines;
        }
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            String current = "";
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate;
                String string = candidate = current.isEmpty() ? word : current + " " + word;
                if (current.isEmpty() || this.textWidth(fontName, candidate, size) <= maxWidth) {
                    current = candidate;
                    continue;
                }
                lines.add(current);
                current = word;
                while (this.textWidth(fontName, current, size) > maxWidth && current.length() > 1) {
                    int split = this.fittingPrefixLength(fontName, current, size, maxWidth);
                    lines.add(current.substring(0, split));
                    current = current.substring(split);
                }
            }
            if (current.isEmpty()) continue;
            lines.add(current);
        }
        return lines.isEmpty() ? List.of(text) : lines;
    }

    private int fittingPrefixLength(String fontName, String text, float size, float maxWidth) {
        int low = 1;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (this.textWidth(fontName, text.substring(0, mid), size) <= maxWidth) {
                low = mid;
                continue;
            }
            high = mid - 1;
        }
        return Math.max(1, low);
    }

    public void pushScissor(float x, float y, float w, float h) {
        this.scissorStack = new ScissorRegion(this.scissorStack, x, y, x + w, y + h);
        this.scissorStack.apply(this.vg);
    }

    public void popScissor() {
        NanoVG.nvgResetScissor((long)this.vg);
        ScissorRegion scissorRegion = this.scissorStack = this.scissorStack != null ? this.scissorStack.parent : null;
        if (this.scissorStack != null) {
            this.scissorStack.apply(this.vg);
        }
    }

    public void scissor(float x, float y, float w, float h) {
        NanoVG.nvgScissor((long)this.vg, (float)x, (float)y, (float)w, (float)h);
    }

    public void resetScissor() {
        NanoVG.nvgResetScissor((long)this.vg);
    }

    public void save() {
        NanoVG.nvgSave((long)this.vg);
    }

    public void restore() {
        NanoVG.nvgRestore((long)this.vg);
    }

    public void translate(float dx, float dy) {
        NanoVG.nvgTranslate((long)this.vg, (float)dx, (float)dy);
    }

    public void scale(float sx, float sy) {
        NanoVG.nvgScale((long)this.vg, (float)sx, (float)sy);
    }

    public void skewX(float radians) {
        NanoVG.nvgSkewX((long)this.vg, (float)radians);
    }

    public void globalAlpha(float alpha) {
        NanoVG.nvgGlobalAlpha((long)this.vg, (float)Math.max(0.0f, Math.min(1.0f, alpha)));
    }

    public void setTextScale(float s) {
        if (this.textScale != s) {
            this.textScale = s;
            this.textWidthCache.clear();
            this.lastTextKey = null;
        }
    }

    static void color(int argb, NVGColor out) {
        NanoVG.nvgRGBA((byte)((byte)(argb >> 16 & 0xFF)), (byte)((byte)(argb >> 8 & 0xFF)), (byte)((byte)(argb & 0xFF)), (byte)((byte)(argb >> 24 & 0xFF)), (NVGColor)out);
    }

    private static final class ScissorRegion {
        final ScissorRegion parent;
        final float x1;
        final float y1;
        final float x2;
        final float y2;

        ScissorRegion(ScissorRegion parent, float x1, float y1, float x2, float y2) {
            this.parent = parent;
            if (parent != null) {
                this.x1 = Math.max(x1, parent.x1);
                this.y1 = Math.max(y1, parent.y1);
                this.x2 = Math.min(x2, parent.x2);
                this.y2 = Math.min(y2, parent.y2);
            } else {
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
            }
        }

        void apply(long vg) {
            float w = Math.max(0.0f, this.x2 - this.x1);
            float h = Math.max(0.0f, this.y2 - this.y1);
            NanoVG.nvgScissor((long)vg, (float)this.x1, (float)this.y1, (float)w, (float)h);
        }
    }
}
