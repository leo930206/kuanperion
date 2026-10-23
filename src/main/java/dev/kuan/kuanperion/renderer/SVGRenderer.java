// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.renderer;

import dev.kuan.kuanperion.renderer.NanoVGManager;
import dev.kuan.kuanperion.util.UiResources;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

final class SVGRenderer {
    private static final Pattern SVG_LENGTH_PATTERN = Pattern.compile("^\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?:[eE][+-]?\\d+)?)\\s*([a-zA-Z%]*)\\s*$");
    private static final Map<String, Integer> imageCache = new HashMap<String, Integer>();

    private SVGRenderer() {
    }

    static void render(long vg, String resourcePath, float x, float y, float width, float height, int argbColor, NVGPaint paintScratch) {
        int physH;
        float px = NanoVGManager.getPxRatio();
        int physW = Math.max(1, (int)Math.ceil(width * px));
        String cacheKey = resourcePath + "@" + physW + "x" + (physH = Math.max(1, (int)Math.ceil(height * px)));
        int imgHandle = SVGRenderer.getOrLoad(vg, cacheKey, resourcePath, physW, physH);
        if (imgHandle == -1) {
            return;
        }
        float alpha = (float)(argbColor >> 24 & 0xFF) / 255.0f;
        float r = (float)(argbColor >> 16 & 0xFF) / 255.0f;
        float g = (float)(argbColor >> 8 & 0xFF) / 255.0f;
        float b = (float)(argbColor & 0xFF) / 255.0f;
        NanoVG.nvgSave((long)vg);
        NanoVG.nvgBeginPath((long)vg);
        NanoVG.nvgRect((long)vg, (float)x, (float)y, (float)width, (float)height);
        NanoVG.nvgImagePattern((long)vg, (float)x, (float)y, (float)width, (float)height, (float)0.0f, (int)imgHandle, (float)alpha, (NVGPaint)paintScratch);
        try (MemoryStack stack = MemoryStack.stackPush();){
            NVGColor tintColor = NVGColor.malloc((MemoryStack)stack);
            NanoVG.nvgRGBAf((float)r, (float)g, (float)b, (float)alpha, (NVGColor)tintColor);
            NVGPaint.ninnerColor((long)paintScratch.address(), (NVGColor)tintColor);
            NVGPaint.nouterColor((long)paintScratch.address(), (NVGColor)tintColor);
        }
        NanoVG.nvgFillPaint((long)vg, (NVGPaint)paintScratch);
        NanoVG.nvgFill((long)vg);
        NanoVG.nvgRestore((long)vg);
    }

    private static int getOrLoad(long vg, String cacheKey, String resourcePath, int width, int height) {
        Integer cached = imageCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int handle = SVGRenderer.loadSVG(vg, resourcePath, width, height);
        if (handle != -1) {
            imageCache.put(cacheKey, handle);
        }
        return handle;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static int loadSVG(long vg, String resourcePath, int width, int height) {
        String svgText;
        try (InputStream in = UiResources.open(resourcePath);){
            if (in == null) {
                System.err.println("[Kuanperion] SVG not found: " + resourcePath);
                int n = -1;
                return n;
            }
            svgText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            System.err.println("[Kuanperion] IOException reading SVG " + resourcePath + ": " + e.getMessage());
            return -1;
        }
        NSVGImage svgImage = NanoSVG.nsvgParse((CharSequence)svgText, (CharSequence)"px", (float)96.0f);
        if (svgImage == null) {
            System.err.println("[Kuanperion] NanoSVG parse failed: " + resourcePath);
            return -1;
        }
        float[] svgSize = SVGRenderer.readSvgSize(svgText);
        float svgW = svgSize[0];
        float svgH = svgSize[1];
        if (svgW <= 0.0f || svgH <= 0.0f) {
            NanoSVG.nsvgDelete((NSVGImage)svgImage);
            System.err.println("[Kuanperion] SVG has no usable size metadata: " + resourcePath);
            return -1;
        }
        long rasterizer = NanoSVG.nsvgCreateRasterizer();
        if (rasterizer == 0L) {
            NanoSVG.nsvgDelete((NSVGImage)svgImage);
            System.err.println("[Kuanperion] NanoSVG could not create rasterizer");
            return -1;
        }
        float scale = svgW > 0.0f && svgH > 0.0f ? Math.min((float)width / svgW, (float)height / svgH) : 1.0f;
        int rW = Math.max(1, (int)(svgW * scale));
        int rH = Math.max(1, (int)(svgH * scale));
        ByteBuffer pixels = MemoryUtil.memAlloc((int)(rW * rH * 4));
        NanoSVG.nsvgRasterize((long)rasterizer, (NSVGImage)svgImage, (float)0.0f, (float)0.0f, (float)scale, (ByteBuffer)pixels, (int)rW, (int)rH, (int)(rW * 4));
        NanoSVG.nsvgDeleteRasterizer((long)rasterizer);
        NanoSVG.nsvgDelete((NSVGImage)svgImage);
        int nvgId = NanoVG.nvgCreateImageRGBA((long)vg, (int)rW, (int)rH, (int)0, (ByteBuffer)pixels);
        MemoryUtil.memFree((ByteBuffer)pixels);
        if (nvgId != -1) return nvgId;
        System.err.println("[Kuanperion] NanoVG image creation failed: " + resourcePath);
        return nvgId;
    }

    private static float[] readSvgSize(String svgText) {
        try {
            String[] parts;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setCoalescing(true);
            factory.setExpandEntityReferences(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            }
            catch (Exception exception) {
                // empty catch block
            }
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svgText)));
            Element root = document.getDocumentElement();
            if (root == null || !"svg".equalsIgnoreCase(root.getLocalName() != null ? root.getLocalName() : root.getTagName())) {
                return new float[]{-1.0f, -1.0f};
            }
            Float width = SVGRenderer.parseSvgLength(root.getAttribute("width"));
            Float height = SVGRenderer.parseSvgLength(root.getAttribute("height"));
            if (width != null && width.floatValue() > 0.0f && height != null && height.floatValue() > 0.0f) {
                return new float[]{width.floatValue(), height.floatValue()};
            }
            String viewBox = root.getAttribute("viewBox");
            if (!viewBox.isBlank() && (parts = viewBox.trim().split("[,\\s]+")).length == 4) {
                float vbWidth = Float.parseFloat(parts[2]);
                float vbHeight = Float.parseFloat(parts[3]);
                if (vbWidth > 0.0f && vbHeight > 0.0f) {
                    return new float[]{vbWidth, vbHeight};
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return new float[]{-1.0f, -1.0f};
    }

    private static Float parseSvgLength(String value) {
        String unit;
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = SVG_LENGTH_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        float magnitude = Float.parseFloat(matcher.group(1));
        return switch (unit = matcher.group(2).toLowerCase()) {
            case "", "px" -> Float.valueOf(magnitude);
            case "pt" -> Float.valueOf(magnitude * 1.3333334f);
            case "pc" -> Float.valueOf(magnitude * 16.0f);
            case "mm" -> Float.valueOf(magnitude * 3.7795277f);
            case "cm" -> Float.valueOf(magnitude * 37.795277f);
            case "in" -> Float.valueOf(magnitude * 96.0f);
            default -> null;
        };
    }

    static void destroy(long vg) {
        for (int handle : imageCache.values()) {
            NanoVG.nvgDeleteImage((long)vg, (int)handle);
        }
        imageCache.clear();
    }
}
