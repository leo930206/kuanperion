// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.config;

import dev.kuan.kuanperion.ui.theme.Theme;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class ThemeProfileManager {
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("kuanperion").resolve("themes");

    private ThemeProfileManager() {
    }

    public static List<String> list() {
        try {
            if (!Files.isDirectory(DIR)) return List.of();
            try (var entries = Files.list(DIR)) { return entries.filter(p -> p.toString().endsWith(".json")).map(p -> p.getFileName().toString().replace(".json", "")).sorted().toList(); }
        }
        catch (IOException e) {
            return List.of();
        }
    }

    public static void save(String name) {
        if (name.isBlank()) {
            return;
        }
        try {
            Files.createDirectories(DIR);
            Files.writeString(DIR.resolve(ThemeProfileManager.sanitize(name) + ".json"), (CharSequence)Theme.exportJson(), new OpenOption[0]);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(String name) {
        Path src = DIR.resolve(ThemeProfileManager.sanitize(name) + ".json");
        if (!Files.exists(src, new LinkOption[0])) {
            return;
        }
        try {
            String json = Files.readString(src);
            Theme.importJson(json);
            Theme.saveTheme();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delete(String name) {
        try {
            Files.deleteIfExists(DIR.resolve(ThemeProfileManager.sanitize(name) + ".json"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean rename(String oldName, String newName) {
        String oldSanitized = ThemeProfileManager.sanitize(oldName);
        String newSanitized = ThemeProfileManager.sanitize(newName);
        if (oldSanitized.isBlank() || newSanitized.isBlank()) {
            return false;
        }
        Path src = DIR.resolve(oldSanitized + ".json");
        if (!Files.exists(src, new LinkOption[0])) {
            return false;
        }
        if (oldSanitized.equals(newSanitized)) {
            return true;
        }
        Path dst = DIR.resolve(newSanitized + ".json");
        if (Files.exists(dst, new LinkOption[0])) {
            return false;
        }
        try {
            Files.move(src, dst, new CopyOption[0]);
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String exportJson(String name) {
        try {
            return Files.readString(DIR.resolve(ThemeProfileManager.sanitize(name) + ".json"));
        }
        catch (IOException e) {
            return "";
        }
    }

    public static void importJson(String name, String json) {
        if (name.isBlank() || json.isBlank()) {
            return;
        }
        try {
            Files.createDirectories(DIR);
            Files.writeString(DIR.resolve(ThemeProfileManager.sanitize(name) + ".json"), (CharSequence)json, new OpenOption[0]);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-. ]", "_").trim();
    }

    static {
        try {
            Files.createDirectories(DIR, new FileAttribute[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }
}
