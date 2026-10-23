// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;

public class Theme {
    public static final long ANIM_FAST = 150L;
    public static final long ANIM_NORMAL = 250L;
    public static final long ANIM_SLOW = 400L;
    public static int BG_PRIMARY = -14670549;
    public static int BG_SECONDARY = SkyThemePalette.color("Panel BG");
    public static int BG_TERTIARY = -15262943;
    public static int BG_HOVER = SkyThemePalette.color("Hover BG");
    public static int BG_ACTIVE = -12498088;
    public static int BG_FIELD = SkyThemePalette.color("Input BG");
    public static int PANEL_BG = SkyThemePalette.color("Main Panel BG");
    public static int SIDEBAR_BG = SkyThemePalette.color("Sidebar BG");
    public static int CARD_BG = SkyThemePalette.color("Card BG");
    public static int ELEMENT_BG = SkyThemePalette.color("Element BG");
    public static int SEPARATOR = SkyThemePalette.color("Separator");
    public static int ACCENT_PRIMARY = SkyThemePalette.color("Accent");
    public static int ACCENT_SECONDARY = SkyThemePalette.color("Accent 2");
    public static int ACCENT_DIM = 1439903535;
    public static int ACCENT_GLOW = -1714213073;
    public static int ACCENT_ENABLED = -11751600;
    public static int ACCENT_WARNING = -680437;
    public static int ACCENT_ERROR = -1096636;
    public static int STATUS_SUCCESS = -15681151;
    public static int STATUS_WARNING = -680437;
    public static int STATUS_ERROR = -1096636;
    public static int BORDER_DEFAULT = SkyThemePalette.color("Border");
    public static int BORDER_HOVER = -7891547;
    public static int BORDER_ACTIVE = -2937041;
    public static int BORDER_ACCENT = -2937041;
    public static final int BORDER_DRAG = -5592321;
    public static final int BORDER_RESIZE = -22016;
    public static int TEXT_PRIMARY = SkyThemePalette.color("Text");
    public static int TEXT_SECONDARY = SkyThemePalette.color("Text Dim");
    public static final int TEXT_TERTIARY = -9735552;
    public static int TEXT_DIM = -5195066;
    public static final int TEXT_DISABLED = -11840157;
    public static final int TEXT_ACCENT = -10262799;
    public static int TEXT_MUTED = SkyThemePalette.color("Text Muted");
    public static int TEXT_LABEL = SkyThemePalette.color("Text Label");
    public static int TEXT_VALUE = SkyThemePalette.color("Text Value");
    public static int GROUP_ACTIVE = SkyThemePalette.color("Group Active");
    public static int PILL_TRACK = SkyThemePalette.color("[Toggle] Track");
    public static int PILL_KNOB_OFF = SkyThemePalette.color("[Toggle] Knob");
    public static int SLIDER_LEFT = SkyThemePalette.color("[Slider] Left");
    public static int DROPDOWN_BTN_BG = SkyThemePalette.color("[Dropdown] BG");
    public static int ACTION_BTN_BG = SkyThemePalette.color("[Action] BG");
    public static int ACTION_BTN_HOVER = SkyThemePalette.color("[Action] Hover");
    public static int GRADIENT_BLUE = SkyThemePalette.color("Gradient Blue");
    public static int GRADIENT_PINK = SkyThemePalette.color("Gradient Pink");
    public static boolean GRADIENT_ENABLED = true;
    public static float GRADIENT_COVERAGE = .48f, GRADIENT_STRENGTH = .60f;
    public static final int PADDING = 8;
    public static final int MARGIN = 4;
    public static final int CORNER_RADIUS = 4;
    public static final int BORDER_WIDTH = 1;
    public static final int TITLE_BAR_HEIGHT = 28;
    public static final int TAB_WIDTH = 100;
    public static final int TAB_HEIGHT = 30;
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int WINDOW_MIN_WIDTH = 450;
    public static final int WINDOW_MIN_HEIGHT = 400;
    public static final int WINDOW_DEFAULT_WIDTH = 620;
    public static final int WINDOW_DEFAULT_HEIGHT = 450;
    public static final int BORDER_THIN = 1;
    public static final int BORDER_MEDIUM = 2;
    public static final int BORDER_THICK = 3;
    public static final int SPACING_XS = 2;
    public static final int SPACING_SM = 5;
    public static final int SPACING_MD = 10;
    public static final int SPACING_LG = 15;
    public static final int SPACING_XL = 20;
    public static int HUD_BG = -14735568;
    public static int HUD_BORDER = -8480078;
    public static int HUD_TITLE = -1;
    public static int HUD_LABEL = -3945515;
    public static int HUD_VALUE = -1;
    public static int HUD_ACCENT = -9390119;
    public static int HUD_SEP = -10653310;
    public static int HUD_BAR_BG = -13682107;
    public static int HUD_SUCCESS = -7677531;
    public static int HUD_WARNING = -1523848;
    public static int HUD_ERROR = -1209709;
    public static final float ANIM_TIME_MIN_MS = 50.0f;
    public static final float ANIM_TIME_MAX_MS = 1000.0f;
    public static float ANIM_TIME_MS = 250.0f;
    public static final float UI_SCALE_MIN = 0.5f;
    public static final float UI_SCALE_MAX = 3.0f;
    public static float UI_SCALE = 1.5f;
    public static final float TEXT_SCALE_MIN = 0.75f;
    public static final float TEXT_SCALE_MAX = 2.0f;
    public static float TEXT_SCALE = 1.0f;
    public static int SETTING_SPACING = 4;
    public static final Set<String> rainbowEntries = new HashSet<String>();
    private static float rainbowHue = 0.0f;
    public static final List<ThemeEntry> ENTRIES = List.of(Theme.entry("Accent", () -> ACCENT_PRIMARY, v -> {
        ACCENT_PRIMARY = v;
    }), Theme.entry("Accent 2", () -> ACCENT_SECONDARY, v -> {
        ACCENT_SECONDARY = v;
    }), Theme.entry("Panel BG", () -> BG_SECONDARY, v -> {
        BG_SECONDARY = v;
    }), Theme.entry("Hover BG", () -> BG_HOVER, v -> {
        BG_HOVER = v;
    }), Theme.entry("Input BG", () -> BG_FIELD, v -> {
        BG_FIELD = v;
    }), Theme.entry("Card BG", () -> CARD_BG, v -> {
        CARD_BG = v;
    }), Theme.entry("Element BG", () -> ELEMENT_BG, v -> {
        ELEMENT_BG = v;
    }), Theme.entry("Sidebar BG", () -> SIDEBAR_BG, v -> {
        SIDEBAR_BG = v;
    }), Theme.entry("Main Panel BG", () -> PANEL_BG, v -> {
        PANEL_BG = v;
    }), Theme.entry("Border", () -> BORDER_DEFAULT, v -> {
        BORDER_DEFAULT = v;
    }), Theme.entry("Separator", () -> SEPARATOR, v -> {
        SEPARATOR = v;
    }), Theme.entry("Text", () -> TEXT_PRIMARY, v -> {
        TEXT_PRIMARY = v;
    }), Theme.entry("Text Dim", () -> TEXT_SECONDARY, v -> {
        TEXT_SECONDARY = v;
    }), Theme.entry("Text Muted", () -> TEXT_MUTED, v -> {
        TEXT_MUTED = v;
    }), Theme.entry("Text Label", () -> TEXT_LABEL, v -> {
        TEXT_LABEL = v;
    }), Theme.entry("Text Value", () -> TEXT_VALUE, v -> {
        TEXT_VALUE = v;
    }), Theme.entry("Group Active", () -> GROUP_ACTIVE, v -> {
        GROUP_ACTIVE = v;
    }), Theme.entry("[Toggle] Track", () -> PILL_TRACK, v -> {
        PILL_TRACK = v;
    }), Theme.entry("[Toggle] Knob", () -> PILL_KNOB_OFF, v -> {
        PILL_KNOB_OFF = v;
    }), Theme.entry("[Slider] Left", () -> SLIDER_LEFT, v -> {
        SLIDER_LEFT = v;
    }), Theme.entry("[Dropdown] BG", () -> DROPDOWN_BTN_BG, v -> {
        DROPDOWN_BTN_BG = v;
    }), Theme.entry("[Action] BG", () -> ACTION_BTN_BG, v -> {
        ACTION_BTN_BG = v;
    }), Theme.entry("[Action] Hover", () -> ACTION_BTN_HOVER, v -> {
        ACTION_BTN_HOVER = v;
    }), Theme.entry("Gradient Blue", () -> GRADIENT_BLUE, v -> GRADIENT_BLUE = v),
        Theme.entry("Gradient Pink", () -> GRADIENT_PINK, v -> GRADIENT_PINK = v));
    public static final List<ThemeEntry> HUD_ENTRIES = List.of(Theme.entry("HUD Background", () -> HUD_BG, v -> {
        HUD_BG = v;
    }), Theme.entry("HUD Border", () -> HUD_BORDER, v -> {
        HUD_BORDER = v;
    }), Theme.entry("HUD Title", () -> HUD_TITLE, v -> {
        HUD_TITLE = v;
    }), Theme.entry("HUD Label", () -> HUD_LABEL, v -> {
        HUD_LABEL = v;
    }), Theme.entry("HUD Value", () -> HUD_VALUE, v -> {
        HUD_VALUE = v;
    }), Theme.entry("HUD Accent", () -> HUD_ACCENT, v -> {
        HUD_ACCENT = v;
    }), Theme.entry("HUD Separator", () -> HUD_SEP, v -> {
        HUD_SEP = v;
    }), Theme.entry("HUD Bar BG", () -> HUD_BAR_BG, v -> {
        HUD_BAR_BG = v;
    }), Theme.entry("HUD Success", () -> HUD_SUCCESS, v -> {
        HUD_SUCCESS = v;
    }), Theme.entry("HUD Warning", () -> HUD_WARNING, v -> {
        HUD_WARNING = v;
    }), Theme.entry("HUD Error", () -> HUD_ERROR, v -> {
        HUD_ERROR = v;
    }));
    private static final int[] DEFAULT_MENU_COLORS = ENTRIES.stream().mapToInt(entry -> entry.getter.get()).toArray();
    private static final int[] DEFAULT_HUD_COLORS = HUD_ENTRIES.stream().mapToInt(entry -> entry.getter.get()).toArray();
    private static final File THEME_FILE = FabricLoader.getInstance().getConfigDir().resolve("kuanperion-theme.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void tickRainbow() {
        if (rainbowEntries.isEmpty()) {
            return;
        }
        rainbowHue = (rainbowHue + 0.003f) % 1.0f;
        int rgb = Color.HSBtoRGB(rainbowHue, 0.8f, 1.0f);
        int color = Theme.withAlpha(rgb, 255);
        for (ThemeEntry e : ENTRIES) {
            if (!rainbowEntries.contains(e.label)) continue;
            e.setter.accept(color);
        }
    }

    private static ThemeEntry entry(String label, Supplier<Integer> getter, Consumer<Integer> setter) {
        return new ThemeEntry(label, getter, setter);
    }

    public static void saveTheme() {
        JsonObject obj = new JsonObject();
        for (ThemeEntry themeEntry : ENTRIES) {
            obj.addProperty(themeEntry.label, String.format("%08X", themeEntry.getter.get()));
        }
        for (ThemeEntry themeEntry : HUD_ENTRIES) {
            obj.addProperty(themeEntry.label, String.format("%08X", themeEntry.getter.get()));
        }
        obj.addProperty("animSpeed", (Number)Float.valueOf(ANIM_TIME_MS));
        obj.addProperty("settingSpacing", (Number)SETTING_SPACING);
        obj.addProperty("uiScale", (Number)Float.valueOf(UI_SCALE));
        obj.addProperty("textScale", (Number)Float.valueOf(TEXT_SCALE));
        obj.addProperty("themeSchema", 2);
        obj.addProperty("gradientEnabled", GRADIENT_ENABLED);
        obj.addProperty("gradientCoverage",GRADIENT_COVERAGE);
        obj.addProperty("gradientStrength",GRADIENT_STRENGTH);
        obj.addProperty("descriptionGap",dev.kuan.kuanperion.ui.UiLayoutMetrics.descriptionGap());
        obj.addProperty("inputHeight",dev.kuan.kuanperion.ui.UiLayoutMetrics.inputHeight());
        JsonArray rainbowArr = new JsonArray();
        for (String s : rainbowEntries) {
            rainbowArr.add(s);
        }
        obj.add("rainbowEntries", (JsonElement)rainbowArr);
        try (FileWriter fileWriter = new FileWriter(THEME_FILE);){
            fileWriter.write(GSON.toJson((JsonElement)obj));
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public static void loadTheme() {
        if (!THEME_FILE.exists()) {
            return;
        }
        try (FileReader fr = new FileReader(THEME_FILE);){
            String hex;
            JsonObject obj = JsonParser.parseReader((Reader)fr).getAsJsonObject();
            var savedColors = new java.util.HashMap<String,String>();
            for (var entry : obj.entrySet())
                if (entry.getValue().isJsonPrimitive()) savedColors.put(entry.getKey(), entry.getValue().getAsString());
            boolean rainbow = obj.has("rainbowEntries") && !obj.getAsJsonArray("rainbowEntries").isEmpty()
                || obj.has("rainbowTheme") && obj.get("rainbowTheme").getAsBoolean();
            boolean migrateDefault = !obj.has("themeSchema") && SkyThemePalette.isLegacyDefault(savedColors, rainbow);
            boolean migrateSky = (!obj.has("themeSchema") || obj.get("themeSchema").getAsInt()<2)
                && SkyThemePalette.isPreviousSkyDefault(savedColors,rainbow);
            if(migrateSky){
                var skyBackup=THEME_FILE.toPath().resolveSibling("kuanperion-theme.before-light-sky.bak");
                if(!java.nio.file.Files.exists(skyBackup))java.nio.file.Files.copy(THEME_FILE.toPath(),skyBackup);
            }
            if (migrateDefault) {
                var backup = THEME_FILE.toPath().resolveSibling("kuanperion-theme.before-sky-blossom.bak");
                if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(THEME_FILE.toPath(), backup);
            }
            GRADIENT_ENABLED = obj.has("gradientEnabled") ? obj.get("gradientEnabled").getAsBoolean() : migrateDefault;
            for (ThemeEntry e2 : ENTRIES) {
                if (!obj.has(e2.label)) continue;
                hex = obj.get(e2.label).getAsString();
                e2.setter.accept((int)Long.parseLong(hex, 16));
            }
            for (ThemeEntry e2 : HUD_ENTRIES) {
                if (!obj.has(e2.label)) continue;
                hex = obj.get(e2.label).getAsString();
                e2.setter.accept((int)Long.parseLong(hex, 16));
            }
            if (obj.has("animSpeed")) {
                ANIM_TIME_MS = Theme.parseAnimationTime(obj.get("animSpeed").getAsFloat());
            }
            if (obj.has("settingSpacing")) {
                SETTING_SPACING = Math.max(0, Math.min(20, obj.get("settingSpacing").getAsInt()));
            }
            if (obj.has("uiScale")) {
                UI_SCALE = finiteRange(obj.get("uiScale").getAsFloat(), 0.5f, 3.0f, 1.5f);
            }
            if (obj.has("textScale")) {
                TEXT_SCALE = finiteRange(obj.get("textScale").getAsFloat(), 0.75f, 2.0f, 1.0f);
            }
            rainbowEntries.clear();
            if (obj.has("rainbowEntries")) {
                obj.get("rainbowEntries").getAsJsonArray().forEach(e -> rainbowEntries.add(e.getAsString()));
            } else if (obj.has("rainbowTheme") && obj.get("rainbowTheme").getAsBoolean()) {
                rainbowEntries.add("Accent");
            }
            loadAppearance(obj);
            if(migrateDefault || migrateSky){
                // Exact previous default only; preserve custom colors, toggle, UI scale and HUD values.
                boolean enabled=GRADIENT_ENABLED;
                resetMenuColorsToDefaults();
                if(migrateSky)GRADIENT_ENABLED=enabled;
                saveTheme();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String exportJson() {
        JsonObject obj = new JsonObject();
        for (ThemeEntry e : ENTRIES) {
            obj.addProperty(e.label, String.format("%08X", e.getter.get()));
        }
        for (ThemeEntry e : HUD_ENTRIES) {
            obj.addProperty(e.label, String.format("%08X", e.getter.get()));
        }
        obj.addProperty("animSpeed", (Number)Float.valueOf(ANIM_TIME_MS));
        obj.addProperty("settingSpacing", (Number)SETTING_SPACING);
        obj.addProperty("themeSchema", 2);
        obj.addProperty("gradientEnabled", GRADIENT_ENABLED);
        obj.addProperty("gradientCoverage",GRADIENT_COVERAGE);
        obj.addProperty("gradientStrength",GRADIENT_STRENGTH);
        obj.addProperty("descriptionGap",dev.kuan.kuanperion.ui.UiLayoutMetrics.descriptionGap());
        obj.addProperty("inputHeight",dev.kuan.kuanperion.ui.UiLayoutMetrics.inputHeight());
        JsonArray rainbowArr2 = new JsonArray();
        for (String s : rainbowEntries) {
            rainbowArr2.add(s);
        }
        obj.add("rainbowEntries", (JsonElement)rainbowArr2);
        return GSON.toJson((JsonElement)obj);
    }

    public static void importJson(String json) {
        try {
            String hex;
            JsonObject obj = JsonParser.parseString((String)json).getAsJsonObject();
            GRADIENT_ENABLED = obj.has("gradientEnabled") && obj.get("gradientEnabled").getAsBoolean();
            for (ThemeEntry e2 : ENTRIES) {
                if (!obj.has(e2.label)) continue;
                hex = obj.get(e2.label).getAsString();
                e2.setter.accept((int)Long.parseLong(hex, 16));
            }
            for (ThemeEntry e2 : HUD_ENTRIES) {
                if (!obj.has(e2.label)) continue;
                hex = obj.get(e2.label).getAsString();
                e2.setter.accept((int)Long.parseLong(hex, 16));
            }
            if (obj.has("animSpeed")) {
                ANIM_TIME_MS = Theme.parseAnimationTime(obj.get("animSpeed").getAsFloat());
            }
            if (obj.has("settingSpacing")) {
                SETTING_SPACING = Math.max(0, Math.min(20, obj.get("settingSpacing").getAsInt()));
            }
            loadAppearance(obj);
            rainbowEntries.clear();
            if (obj.has("rainbowEntries")) {
                obj.get("rainbowEntries").getAsJsonArray().forEach(e -> rainbowEntries.add(e.getAsString()));
            } else if (obj.has("rainbowTheme") && obj.get("rainbowTheme").getAsBoolean()) {
                rainbowEntries.add("Accent");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void loadAppearance(JsonObject obj) {
        if(obj.has("gradientCoverage"))GRADIENT_COVERAGE=finiteRange(obj.get("gradientCoverage").getAsFloat(),.25f,1,.48f);
        if(obj.has("gradientStrength"))GRADIENT_STRENGTH=finiteRange(obj.get("gradientStrength").getAsFloat(),0,1,.60f);
        if(obj.has("descriptionGap"))dev.kuan.kuanperion.ui.UiLayoutMetrics.setDescriptionGap(obj.get("descriptionGap").getAsFloat());
        if(obj.has("inputHeight"))dev.kuan.kuanperion.ui.UiLayoutMetrics.setInputHeight(obj.get("inputHeight").getAsFloat());
    }

    public static void resetToDefaults() {
        Theme.resetColorsToDefaults();
        dev.kuan.kuanperion.ui.UiLayoutMetrics.reset();
        ANIM_TIME_MS = 250.0f;
        SETTING_SPACING = 4;
        UI_SCALE = 1.5f;
        TEXT_SCALE = 1.0f;
    }

    private static void resetMenuColorsToDefaults() {
        for (int i = 0; i < ENTRIES.size(); ++i) ENTRIES.get(i).setter.accept(DEFAULT_MENU_COLORS[i]);
        rainbowEntries.clear();
        GRADIENT_ENABLED = true;
        GRADIENT_COVERAGE=.48f;GRADIENT_STRENGTH=.60f;
    }

    public static void resetColorsToDefaults() {
        resetMenuColorsToDefaults();
        for (int i = 0; i < HUD_ENTRIES.size(); ++i) {
            Theme.HUD_ENTRIES.get((int)i).setter.accept(DEFAULT_HUD_COLORS[i]);
        }
        rainbowEntries.clear();
    }

    public static int panelShadow() {
        boolean light = (PANEL_BG >>> 16 & 255) + (PANEL_BG >>> 8 & 255) + (PANEL_BG & 255) > 540;
        return light ? 0x38446E8F : 0xBF000000;
    }
    public static int subtleBorder() { return blend(BORDER_DEFAULT, PANEL_BG, 0.25f); }
    public static int disabledText() { return blend(TEXT_SECONDARY, PANEL_BG, 0.25f); }

    public static int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return alpha << 24 | color & 0xFFFFFF;
    }

    public static float animationFactor() {
        return Theme.animationFactor(1.0f);
    }

    public static float animationFactor(float multiplier) {
        float durationMs = Math.max(1.0f, ANIM_TIME_MS / Math.max(0.001f, multiplier));
        float frameMs = 16.666666f;
        return 1.0f - (float)Math.exp(-frameMs / (durationMs / 3.0f));
    }

    private static float finiteRange(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    private static float parseAnimationTime(float storedValue) {
        if (!Float.isFinite(storedValue)) return 250.0f;
        if (storedValue <= 1.0f) {
            return Theme.legacyAnimSpeedToDuration(storedValue);
        }
        return Math.max(50.0f, Math.min(1000.0f, storedValue));
    }

    private static float legacyAnimSpeedToDuration(float legacySpeed) {
        float speed = Math.max(0.001f, Math.min(0.95f, legacySpeed));
        float frameMs = 16.666666f;
        float durationMs = (float)((double)(-frameMs * 3.0f) / Math.log(1.0f - speed));
        return Math.max(50.0f, Math.min(1000.0f, durationMs));
    }

    public static int withAlpha(int color, float alphaPercent) {
        return Theme.withAlpha(color, (int)(alphaPercent * 255.0f));
    }

    public static int blend(int color1, int color2, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int a1 = color1 >> 24 & 0xFF;
        int r1 = color1 >> 16 & 0xFF;
        int g1 = color1 >> 8 & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = color2 >> 24 & 0xFF;
        int r2 = color2 >> 16 & 0xFF;
        int g2 = color2 >> 8 & 0xFF;
        int b2 = color2 & 0xFF;
        int a = (int)((float)a1 + (float)(a2 - a1) * ratio);
        int r = (int)((float)r1 + (float)(r2 - r1) * ratio);
        int g = (int)((float)g1 + (float)(g2 - g1) * ratio);
        int b = (int)((float)b1 + (float)(b2 - b1) * ratio);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int lighter(int color, float amount) {
        return Theme.blend(color, -1, amount);
    }

    public static int darker(int color, float amount) {
        return Theme.blend(color, -16777216, amount);
    }

    public static int getAlpha(int color) {
        return color >> 24 & 0xFF;
    }

    public static int getRed(int color) {
        return color >> 16 & 0xFF;
    }

    public static int getGreen(int color) {
        return color >> 8 & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static int fromARGB(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int fromRGB(int red, int green, int blue) {
        return Theme.fromARGB(255, red, green, blue);
    }

    public static int getRainbowColor(float time, float position, int alpha) {
        float hue = (time + position) % 1.0f;
        int rgb = Color.HSBtoRGB(hue, 0.8f, 1.0f);
        return Theme.withAlpha(rgb, alpha);
    }

    public static int hsvToArgb(float h, float s, float v, int alpha) {
        int rgb = Color.HSBtoRGB(h, s, v);
        return Theme.withAlpha(rgb, alpha);
    }

    public static float[] argbToHsv(int argb) {
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        return Color.RGBtoHSB(r, g, b, null);
    }

    public static class ThemeEntry {
        public final String label;
        public final Supplier<Integer> getter;
        public final Consumer<Integer> setter;

        public ThemeEntry(String label, Supplier<Integer> getter, Consumer<Integer> setter) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }
    }
}
