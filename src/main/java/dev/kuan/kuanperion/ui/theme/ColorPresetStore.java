package dev.kuan.kuanperion.ui.theme;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

/** Five local menu-color snapshots only. Never serializes gameplay settings or credentials. */
public final class ColorPresetStore {
 public static final int LIMIT=5;
 private static final int MAX_BYTES=65536;
 private final Path directory;
 private final Slot[] slots=new Slot[LIMIT];
 private record Slot(boolean occupied,JsonObject colors,String error){}
 public ColorPresetStore(Path directory){
  this.directory=directory;
  for(int i=1;i<=LIMIT;i++){
   Path file=path(i);
   if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS)){slots[i-1]=new Slot(false,null,null);continue;}
   try{
    if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>MAX_BYTES)throw new IOException("Invalid preset file");
    try(var input=Files.newInputStream(file)){
     byte[] bytes=input.readNBytes(MAX_BYTES+1);
     if(bytes.length>MAX_BYTES)throw new IOException("Preset too large");
     slots[i-1]=new Slot(true,canonical(JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject()),null);
    }
   }catch(IOException|RuntimeException error){slots[i-1]=new Slot(true,null,"配色檔案損毀或格式不符");}
  }
 }
 private Path path(int slot){if(slot<1||slot>LIMIT)throw new IllegalArgumentException("Slot must be 1..5");return directory.resolve("slot-"+slot+".json");}
 public boolean occupied(int slot){path(slot);return slots[slot-1].occupied;}
 public JsonObject read(int slot)throws IOException{
  path(slot);Slot saved=slots[slot-1];
  if(saved.error!=null)throw new IOException(saved.error);
  return saved.colors==null?null:saved.colors.deepCopy();
 }
 public void save(int slot,JsonObject source,boolean overwrite)throws IOException{
  Path file=path(slot);JsonObject colors=canonical(source);
  if(Files.isSymbolicLink(file))throw new IOException("Refusing symbolic link preset");
  if(Files.exists(file)&&!overwrite)throw new FileAlreadyExistsException(file.getFileName().toString());
  Files.createDirectories(directory);
  Path temporary=Files.createTempFile(directory,"color-",".tmp");
  try{
   Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(colors));
   if(Files.exists(file))Files.copy(file,file.resolveSibling(file.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
   try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException ex){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
   slots[slot-1]=new Slot(true,colors,null);
  }finally{Files.deleteIfExists(temporary);}
 }
 public int firstEmptySlot(){for(int i=1;i<=LIMIT;i++)if(!occupied(i))return i;return 0;}
 public void delete(int slot)throws IOException{
  Path file=path(slot);
  if(Files.exists(file,LinkOption.NOFOLLOW_LINKS)){
   if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Refusing non-file preset");
   // Keep the last deleted snapshot, but do not treat the backup as an active palette.
   Files.move(file,file.resolveSibling(file.getFileName()+".deleted.bak"),StandardCopyOption.REPLACE_EXISTING);
  }
  slots[slot-1]=new Slot(false,null,null);
 }
 private static JsonObject canonical(JsonObject source){
  JsonObject colors=new JsonObject();
  for(String name:SkyThemePalette.keys()){
   JsonElement value=source.get(name);
   if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||!value.getAsString().matches("[0-9a-fA-F]{8}"))throw new IllegalArgumentException("Invalid color: "+name);
   colors.addProperty(name,value.getAsString().toUpperCase(Locale.ROOT));
  }
  JsonElement enabled=source.get("gradientEnabled");
  if(enabled==null||!enabled.isJsonPrimitive()||!enabled.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Invalid gradient toggle");
  colors.addProperty("gradientEnabled",enabled.getAsBoolean());
  for(String name:new String[]{"gradientCoverage","gradientStrength"}){
   JsonElement value=source.get(name);
   if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Invalid gradient number");
   float number=value.getAsFloat(),min=name.equals("gradientCoverage")?.25f:0;
   if(!Float.isFinite(number)||number<min||number>1)throw new IllegalArgumentException("Gradient out of range");
   colors.addProperty(name,number);
  }
  JsonArray rainbow=new JsonArray();
  if(source.has("rainbowEntries"))for(JsonElement value:source.getAsJsonArray("rainbowEntries")){
   if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||!SkyThemePalette.keys().contains(value.getAsString()))throw new IllegalArgumentException("Invalid rainbow color name");
   rainbow.add(value.getAsString());
  }
  colors.add("rainbowEntries",rainbow);return colors;
 }
}
