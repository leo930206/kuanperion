package dev.kuan.kuanperion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Compatibility-only old name; never used as the current mod identity. */
final class LegacyConfigMigration {
    private LegacyConfigMigration() {}
    static java.util.Map<String,String> legacyKeyOverrides(Path options) throws IOException {
        var stored=new java.util.HashMap<String,String>();
        if(!Files.isRegularFile(options))return java.util.Map.of();
        for(String line:Files.readAllLines(options)) {
            int colon=line.indexOf(':');
            if(colon>0)stored.put(line.substring(0,colon),line.substring(colon+1));
        }
        var result=new java.util.HashMap<String,String>();
        for(String action:java.util.List.of("settings","toggle_mining","toggle_farming")) {
            String old=stored.get("key_key.kuan_sba."+action);
            if(old!=null&&!stored.containsKey("key_key.kuanperion."+action))result.put(action,old);
        }
        return java.util.Map.copyOf(result);
    }
    static boolean migrate(Path configDirectory) throws IOException {
        Path target = configDirectory.resolve("kuanperion.properties");
        Path source = configDirectory.resolve("kuan-hypixel-skyblock-assistant.properties");
        if (Files.exists(target) || !Files.isRegularFile(source)) return false;
        // No REPLACE_EXISTING: concurrent creation must not overwrite newer settings.
        Files.copy(source, target);
        return true;
    }
}
