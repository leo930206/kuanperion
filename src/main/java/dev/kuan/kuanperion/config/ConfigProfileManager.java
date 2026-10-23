package dev.kuan.kuanperion.config;
import com.google.gson.*;
import dev.kuan.kuanperion.KuanperionClient;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Local profiles only. No remote sync; exported snapshots contain no Webhook or position data. */
public final class ConfigProfileManager {
    private static final Path DIR=FabricLoader.getInstance().getConfigDir().resolve("kuanperion/profiles");
    private ConfigProfileManager() {}
    private static Path path(String name) {
        String safe=name.replaceAll("[^\\p{L}\\p{N}_ .-]","_").trim();
        if(safe.isEmpty()||safe.equals(".")||safe.equals(".."))throw new IllegalArgumentException("Invalid profile name");
        return DIR.resolve(safe+".json");
    }
    public static List<String> list() {
        try { if(!Files.isDirectory(DIR))return List.of();
            try(var entries=Files.list(DIR)){return entries.filter(p->p.toString().endsWith(".json")).map(p->p.getFileName().toString().replaceFirst("\\.json$","")).sorted().toList();}
        }catch(IOException ex){throw new IllegalStateException(ex);}
    }
    public static void save(String name){write(name,new GsonBuilder().setPrettyPrinting().create().toJson(KuanperionClient.exportUiConfig()));}
    public static boolean load(String name){try{return KuanperionClient.applyUiConfig(read(name));}catch(RuntimeException ex){return false;}}
    public static void delete(String name){try{Files.deleteIfExists(path(name));}catch(IOException ex){throw new IllegalStateException(ex);}}
    public static boolean rename(String from,String to){try{Path a=path(from),b=path(to);if(a.equals(b))return true;if(Files.exists(b))return false;Files.move(a,b);return true;}catch(IOException|IllegalArgumentException ex){return false;}}
    public static String exportJsonSanitized(String name){return new GsonBuilder().setPrettyPrinting().create().toJson(sanitize(read(name)));}
    public static void importFromClipboard(String name,String json){write(name,new GsonBuilder().setPrettyPrinting().create().toJson(sanitize(JsonParser.parseString(json).getAsJsonObject())));}
    private static JsonObject sanitize(JsonObject object){
        JsonObject copy=object.deepCopy();
        for(String key:List.copyOf(copy.keySet()))if(key.startsWith("hotkey.")||key.equals("pestWebhookUrl")||key.startsWith("jerryFarmingPauseMarker")||key.equals("miningEnabled")||key.equals("farmingEnabled"))copy.remove(key);
        return copy;
    }
    private static JsonObject read(String name){try{Path file=path(name);if(Files.size(file)>1024*1024)throw new IllegalArgumentException("Profile too large");return JsonParser.parseString(Files.readString(file)).getAsJsonObject();}catch(IOException ex){throw new IllegalStateException(ex);}}
    private static void write(String name,String json){try{Files.createDirectories(DIR);Path file=path(name),tmp=Files.createTempFile(DIR,"profile-",".tmp");try{Files.writeString(tmp,json);Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(tmp);}}catch(IOException ex){throw new IllegalStateException(ex);}}
}
