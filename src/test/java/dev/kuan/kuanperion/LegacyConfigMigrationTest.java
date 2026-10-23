package dev.kuan.kuanperion;
import java.nio.file.*;
import java.util.*;

public final class LegacyConfigMigrationTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("kuanperion-migration-test");
        try {
            Path old = root.resolve("kuan-hypixel-skyblock-assistant.properties");
            Path now = root.resolve("kuanperion.properties");
            if (LegacyConfigMigration.migrate(root)) throw new AssertionError("no source");
            String saved = "scanRadius=8\ntargets=minecraft:stone\npestWebhookUrl=PRIVATE\njerryMarkerMaxSeconds=300.5\n";
            Files.writeString(old, saved);
            if (!LegacyConfigMigration.migrate(root)) throw new AssertionError("migration missing");
            if (!Files.readString(now).equals(saved)) throw new AssertionError("values changed");
            if (!Files.readString(old).equals(saved)) throw new AssertionError("old config modified");
            Files.writeString(now,"scanRadius=2\n");
            if (LegacyConfigMigration.migrate(root)) throw new AssertionError("overwrote new config");
            if (!Files.readString(now).equals("scanRadius=2\n")) throw new AssertionError("new values lost");
            Path options=root.resolve("options.txt");
            Files.writeString(options,"key_key.kuan_sba.settings:key.keyboard.p\nkey_key.kuan_sba.toggle_mining:key.keyboard.j\nkey_key.kuanperion.toggle_mining:key.keyboard.k\n");
            Map<String,String> keys=LegacyConfigMigration.legacyKeyOverrides(options);
            if(!keys.get("settings").equals("key.keyboard.p"))throw new AssertionError("legacy key missing");
            if(keys.containsKey("toggle_mining"))throw new AssertionError("new key overwritten");
            if(keys.containsKey("toggle_farming"))throw new AssertionError("invented key");
            System.out.println("LegacyConfigMigrationTest: PASS (missing source, byte-preserving copy, never overwrite, source retained)");
        } finally {
            try (var entries=Files.walk(root)) { for (Path p:entries.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
    }
}
