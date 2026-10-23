// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui.theme;

import dev.kuan.kuanperion.ui.theme.Theme;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public enum ThemePreset {
    SKY_BLOSSOM("Sky Blossom", "sky-blossom"),
    SLATE("Slate", "slate"),
    SAGE("Sage", "sage"),
    MATCHA("Matcha", "matcha"),
    DUSK("Dusk", "dusk"),
    EMBER("Ember", "ember");

    private final String label;
    private final String resourceName;

    private ThemePreset(String label, String resourceName) {
        this.label = label;
        this.resourceName = resourceName;
    }

    public String label() {
        return this.label;
    }

    public String json() {
        String path="/assets/kuanperion/themes/"+resourceName+".json";
        try (InputStream stream=ThemePreset.class.getResourceAsStream(path)) {
            if(stream==null)throw new IllegalStateException("Missing theme preset: "+path);
            return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }catch(IOException error){throw new IllegalStateException("Could not read theme preset: "+path,error);}
    }

    public void apply() {
        Theme.importJson(this.json());
    }
}
