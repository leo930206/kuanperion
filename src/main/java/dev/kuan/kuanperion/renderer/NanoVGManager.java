// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.renderer;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.renderer.SVGRenderer;
import dev.kuan.kuanperion.util.UiResources;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33C;

public final class NanoVGManager {
    private static int uiFbo, uiStencil, uiWidth, uiHeight;
    private static long vg = -1L;
    private static NVGRenderer renderer;
    private static boolean initialized;
    private static boolean drawing;
    private static float pxRatio;
    private static GlFrameState frameState;
    private static int mainRtFbo;
    private static int overrideTargetFbo;
    private static final Map<String, Integer> fontIds;
    private static final Map<String, ByteBuffer> fontBuffers;
    private static final String UNICODE_FALLBACK_FONT = "Kuanperion-Unicode-Fallback";

    private NanoVGManager() {
    }

    public static void init() {
        if (initialized) return;
        try (GlFrameState state = new GlFrameState()) { initializeContext(); }
    }

    private static void initializeContext() {
        if (initialized) {
            return;
        }
        vg = NanoVGGL3.nvgCreate((int)3);
        if (vg <= 0L) {
            throw new RuntimeException("[Kuanperion] Failed to create NanoVG context");
        }
        renderer = new NVGRenderer(vg);
        NanoVGManager.loadFont("Inter-Regular", "/assets/kuanperion/fonts/Inter-Regular.otf");
        NanoVGManager.loadFont("Inter-Bold", "/assets/kuanperion/fonts/Inter-Bold.otf");
        NanoVGManager.loadFont("Inter-Mono", "/assets/kuanperion/fonts/Inter-Mono.otf");
        NanoVGManager.loadFont("Scoreboard-Bold", "/assets/kuanperion/fonts/scoreboard/Inter-Bold.otf");
        NanoVGManager.loadUnicodeFallbackFont();
        initialized = true;
    }

    public static void destroy() {
        if (!initialized) {
            return;
        }
        if(uiFbo!=0) GL30.glDeleteFramebuffers(uiFbo);
        if(uiStencil!=0) GL30.glDeleteRenderbuffers(uiStencil);
        uiFbo=uiStencil=uiWidth=uiHeight=0;
        SVGRenderer.destroy(vg);
        NanoVGGL3.nvgDelete((long)vg);
        vg = -1L;
        renderer = null;
        fontIds.clear();
        fontBuffers.clear();
        initialized = false;
        drawing = false;
    }

    public static void beginFrame(float width, float height) {
        if (!initialized) {
            throw new IllegalStateException("[Kuanperion] NanoVGManager.init() must be called first");
        }
        if (drawing) {
            throw new IllegalStateException("[Kuanperion] endFrame() was not called before beginFrame()");
        }
        NanoVGManager.saveFrameState();
        float computedRatio = 1.0f;
        try {
            int mcFbo;
            RenderTarget rt = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            mainRtFbo = mcFbo = attachUiTarget(rt);
            int targetFbo = overrideTargetFbo >= 0 ? overrideTargetFbo : mcFbo;
            overrideTargetFbo = -1;
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,targetFbo);
            GL11.glViewport(0,0,rt.width,rt.height);
            computedRatio = (float)rt.width / width;
        }
        catch (LinkageError | RuntimeException e) {
            System.err.println("[Kuanperion] Failed to bind main render target for NanoVG: " + e.getMessage());
            overrideTargetFbo = -1;
            restoreFrameState();
            throw new IllegalStateException("Kuanperion UI needs an OpenGL render target", e);
        }
        pxRatio = computedRatio;
        GL33C.glBindSampler((int)0, (int)0);
        renderer.beginMinecraftTextFrame();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        NanoVG.nvgBeginFrame((long)vg, (float)width, (float)height, (float)pxRatio);
        NanoVG.nvgTextAlign((long)vg, (int)9);
        drawing = true;
    }

    public static void endFrame() {
        if (!drawing) {
            throw new IllegalStateException("[Kuanperion] beginFrame() was not called before endFrame()");
        }
        try {
            NanoVG.nvgEndFrame((long)vg);
        }
        finally {
            try {
                renderer.endMinecraftTextFrame();
            }
            finally {
                NanoVGManager.restoreFrameState();
            }
        }
    }

    private static int attachUiTarget(RenderTarget rt) {
        if (!(rt.getColorTexture() instanceof GlTexture texture)) throw new IllegalStateException("Unsupported rendering backend");
        int savedRbo=GL11.glGetInteger(GL30.GL_RENDERBUFFER_BINDING);
        try {
            if(uiFbo==0)uiFbo=GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,uiFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture.glId(),0);
            if(uiStencil==0||uiWidth!=rt.width||uiHeight!=rt.height){
                if(uiStencil!=0)GL30.glDeleteRenderbuffers(uiStencil);
                uiStencil=GL30.glGenRenderbuffers();uiWidth=rt.width;uiHeight=rt.height;
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,uiStencil);
                GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER,GL30.GL_STENCIL_INDEX8,uiWidth,uiHeight);
                GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER,GL30.GL_STENCIL_ATTACHMENT,GL30.GL_RENDERBUFFER,uiStencil);
            }
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Incomplete UI framebuffer");
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glStencilMask(0xff); GL11.glClearStencil(0); GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            return uiFbo;
        } finally {GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER,savedRbo);}
    }

    private static void saveFrameState() { frameState = new GlFrameState(); }
    private static void restoreFrameState() {
        try { if(frameState!=null)frameState.close(); }
        finally { frameState=null;drawing=false; }
    }

    public static NVGRenderer getRenderer() {
        return renderer;
    }

    public static long getVg() {
        return vg;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void setOverrideTargetFbo(int fbo) {
        overrideTargetFbo = fbo;
    }

    public static int getMainRtFbo() {
        return mainRtFbo;
    }


    public static boolean isDrawing() {
        return drawing;
    }

    public static float getPxRatio() {
        return pxRatio;
    }

    public static int getFontId(String name) {
        return fontIds.getOrDefault(name, -1);
    }

    public static void loadFont(String name, String resourcePath) {
        if (fontIds.containsKey(name)) {
            return;
        }
        try (InputStream in = UiResources.open(resourcePath);){
            if (in == null) {
                System.err.println("[Kuanperion] Font resource not found: " + resourcePath);
                return;
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder()).put(bytes);
            buffer.flip();
            int id = NanoVG.nvgCreateFontMem((long)vg, (CharSequence)name, (ByteBuffer)buffer, (boolean)false);
            if (id == -1) {
                System.err.println("[Kuanperion] NanoVG failed to load font: " + name);
                return;
            }
            fontIds.put(name, id);
            fontBuffers.put(name, buffer);
        }
        catch (IOException e) {
            System.err.println("[Kuanperion] IOException loading font " + name + ": " + e.getMessage());
        }
    }

    private static void loadUnicodeFallbackFont() {
        String[] candidates = NanoVGManager.getUnicodeFallbackCandidates();
        boolean loadedAny = false;
        for (int i = 0; i < candidates.length; ++i) {
            String candidate = candidates[i];
            int id = NanoVGManager.loadFontFromFile("Kuanperion-Unicode-Fallback-" + i, candidate);
            if (id == -1) continue;
            NanoVGManager.attachFallbackToUiFonts(id);
            loadedAny = true;
        }
        if (!loadedAny) {
            System.err.println("[Kuanperion] No Unicode fallback font found; some glyphs may render as missing.");
        }
    }

    private static String[] getUnicodeFallbackCandidates() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            String winDir = System.getenv("WINDIR");
            String fontDir = winDir == null || winDir.isBlank() ? "C:\\Windows\\Fonts" : winDir + "\\Fonts";
            return new String[]{fontDir + "\\msyh.ttc", fontDir + "\\msjh.ttc", fontDir + "\\simsun.ttc", fontDir + "\\YuGothR.ttc", fontDir + "\\msgothic.ttc", fontDir + "\\malgun.ttf", fontDir + "\\seguisym.ttf", fontDir + "\\seguiemj.ttf", fontDir + "\\arial.ttf"};
        }
        if (osName.contains("mac")) {
            return new String[]{"/System/Library/Fonts/PingFang.ttc", "/System/Library/Fonts/\u30d2\u30e9\u30ae\u30ce\u89d2\u30b4\u30b7\u30c3\u30af W3.ttc", "/System/Library/Fonts/Hiragino Sans GB.ttc", "/System/Library/Fonts/AppleSDGothicNeo.ttc", "/System/Library/Fonts/Apple Symbols.ttf", "/System/Library/Fonts/Supplemental/Arial Unicode.ttf", "/System/Library/Fonts/Supplemental/Arial.ttf"};
        }
        return new String[]{"/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf", "/usr/share/fonts/opentype/noto/NotoSansCJKtc-Regular.otf", "/usr/share/fonts/opentype/noto/NotoSansCJKkr-Regular.otf", "/usr/share/fonts/google-noto-cjk/NotoSansCJKjp-Regular.otf", "/usr/share/fonts/google-noto-cjk/NotoSansCJKsc-Regular.otf", "/usr/share/fonts/google-noto-cjk/NotoSansCJKtc-Regular.otf", "/usr/share/fonts/google-noto-cjk/NotoSansCJKkr-Regular.otf", "/usr/share/fonts/opentype/noto-cjk/NotoSansCJK-Regular.ttc", "/usr/share/fonts/truetype/noto/NotoSansSymbols2-Regular.ttf", "/usr/share/fonts/opentype/noto/NotoSansSymbols2-Regular.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"};
    }

    private static int loadFontFromFile(String name, String filePath) {
        if (fontIds.containsKey(name)) {
            return fontIds.getOrDefault(name, -1);
        }
        Path path = Path.of(filePath, new String[0]);
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return -1;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder()).put(bytes);
            buffer.flip();
            int id = NanoVG.nvgCreateFontMem((long)vg, (CharSequence)name, (ByteBuffer)buffer, (boolean)false);
            if (id == -1) {
                System.err.println("[Kuanperion] NanoVG failed to load font: " + filePath);
                return -1;
            }
            fontIds.put(name, id);
            fontBuffers.put(name, buffer);
            return id;
        }
        catch (IOException e) {
            System.err.println("[Kuanperion] IOException loading font " + filePath + ": " + e.getMessage());
            return -1;
        }
    }

    private static void attachFallbackToUiFonts(int fallbackId) {
        if (fallbackId == -1) {
            return;
        }
        NanoVGManager.addFallback("Inter-Regular", fallbackId);
        NanoVGManager.addFallback("Inter-Bold", fallbackId);
        NanoVGManager.addFallback("Inter-Mono", fallbackId);
        NanoVGManager.addFallback("Scoreboard-Bold", fallbackId);
    }

    private static void addFallback(String baseFont, int fallbackId) {
        int baseId = fontIds.getOrDefault(baseFont, -1);
        if (baseId != -1) {
            NanoVG.nvgAddFallbackFontId((long)vg, (int)baseId, (int)fallbackId);
        }
    }

    static {
        initialized = false;
        drawing = false;
        pxRatio = 1.0f;
        mainRtFbo = 0;
        overrideTargetFbo = -1;
        fontIds = new HashMap<String, Integer>();
        fontBuffers = new HashMap<String, ByteBuffer>();
    }
}
