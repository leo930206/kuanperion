// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

/** Owned external preferences only. Never reads/writes legacy theme/options/notification files. */
public final class AutomationSettingsStore {
 private static final int SCHEMA=1,MAX_BYTES=262144;
 private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 private final Path path;private JsonObject root;private byte[] fingerprint;private boolean writable;private String issue="";
 private AutomationSettingsStore(Path path){this.path=path.toAbsolutePath().normalize();}
 public static AutomationSettingsStore load(Path path){var store=new AutomationSettingsStore(path);store.read();return store;}
 public boolean writable(){return writable;}public String issue(){return issue;}
 private void read(){
  root=new JsonObject();root.addProperty("schemaVersion",SCHEMA);writable=true;
  try{
   fingerprint=readBytes();if(fingerprint==null)return;
   var parsed=JsonParser.parseString(new String(fingerprint,StandardCharsets.UTF_8));
   if(!parsed.isJsonObject())throw new IllegalArgumentException("Not an object");
   var candidate=parsed.getAsJsonObject();var version=candidate.get("schemaVersion");
   if(version==null||!version.isJsonPrimitive()||!version.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Missing schema");
   int schema=version.getAsBigDecimal().intValueExact();
   if(schema!=SCHEMA){writable=false;issue="設定格式版本不相容，未啟用自動化或覆寫檔案";return;}
   for(String name:new String[]{"features","stash","autoSell","junk","pest","greenhouse","composter"})if(candidate.has(name)&&!candidate.get(name).isJsonObject())throw new IllegalArgumentException("Invalid settings section");
   root=candidate;
  }catch(IOException|RuntimeException invalid){writable=false;issue="自動化設定無法讀取，原檔保留且自動化停用";}
 }
 private byte[] readBytes()throws IOException {
  if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return null;
  if(Files.isSymbolicLink(path)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>MAX_BYTES)throw new IOException("Invalid config file");
  byte[] bytes=Files.readAllBytes(path);if(bytes.length>MAX_BYTES)throw new IOException("Oversized config");return bytes;
 }
 public boolean enabled(AutomationFeature feature){return writable&&bool(section(root,"features"),feature.id(),false);}
 public StashTask.Policy stashPolicy(){var section=section(root,"stash");return new StashTask.Policy(integer(section,"delaySeconds",3,2,60),integer(section,"maxAttempts",10,1,30),integer(section,"timeoutSeconds",60,10,300),bool(section,"onlyWhileFarming",true));}
 public void setEnabled(AutomationFeature feature,boolean enabled)throws IOException {update(copy->object(copy,"features").addProperty(feature.id(),enabled));}
 public void setStashPolicy(StashTask.Policy policy)throws IOException {update(copy->{var s=object(copy,"stash");s.addProperty("delaySeconds",policy.delaySeconds());s.addProperty("maxAttempts",policy.maxAttempts());s.addProperty("timeoutSeconds",policy.timeoutSeconds());s.addProperty("onlyWhileFarming",policy.onlyWhileFarming());});}
 public AutoSellPolicy autoSellPolicy(){
  var s=section(root,"autoSell");try{return new AutoSellPolicy(strings(s,"allowedIds"),strings(s,"protectedUuids"),integer(s,"maxStacks",5,1,27),integer(s,"maxItems",320,1,4096),integer(s,"delayMillis",1000,500,5000),integer(s,"timeoutSeconds",60,10,180),integer(s,"cooldownSeconds",60,10,3600),integer(s,"thresholdPercent",85,1,100),bool(s,"onlyWhileFarming",true));}catch(RuntimeException invalid){return AutoSellPolicy.defaults();}
 }
 public void setAutoSellPolicy(AutoSellPolicy policy)throws IOException{update(copy->{var s=object(copy,"autoSell");s.add("allowedIds",JSON.toJsonTree(new java.util.TreeSet<>(policy.allowedIds())));s.add("protectedUuids",JSON.toJsonTree(new java.util.TreeSet<>(policy.protectedUuids())));s.addProperty("maxStacks",policy.maxStacks());s.addProperty("maxItems",policy.maxItems());s.addProperty("delayMillis",policy.delayMillis());s.addProperty("timeoutSeconds",policy.timeoutSeconds());s.addProperty("cooldownSeconds",policy.cooldownSeconds());s.addProperty("thresholdPercent",policy.thresholdPercent());s.addProperty("onlyWhileFarming",policy.onlyWhileFarming());});}
 public JunkPolicy junkPolicy(){
  var s=section(root,"junk");try{JunkPolicy.Location anchor=null;if(s.has("anchor")){var a=s.getAsJsonObject("anchor");anchor=new JunkPolicy.Location(strictString(a,"dimension"),strictNumber(a,"x"),strictNumber(a,"y"),strictNumber(a,"z"));}return new JunkPolicy(strings(s,"allowedIds"),strings(s,"protectedUuids"),strictInteger(s,"maxStacks",3,1,27),strictInteger(s,"maxItems",192,1,4096),strictInteger(s,"delayMillis",1000,500,5000),strictInteger(s,"timeoutSeconds",60,10,180),strictInteger(s,"cooldownSeconds",60,10,3600),strictInteger(s,"thresholdPercent",85,1,100),bool(s,"onlyWhileFarming",true),anchor,s.has("radius")?strictNumber(s,"radius"):1.5);}catch(RuntimeException invalid){return JunkPolicy.defaults();}
 }
 public void setJunkPolicy(JunkPolicy policy)throws IOException{update(copy->{var s=object(copy,"junk");s.add("allowedIds",JSON.toJsonTree(new java.util.TreeSet<>(policy.allowedIds())));s.add("protectedUuids",JSON.toJsonTree(new java.util.TreeSet<>(policy.protectedUuids())));s.addProperty("maxStacks",policy.maxStacks());s.addProperty("maxItems",policy.maxItems());s.addProperty("delayMillis",policy.delayMillis());s.addProperty("timeoutSeconds",policy.timeoutSeconds());s.addProperty("cooldownSeconds",policy.cooldownSeconds());s.addProperty("thresholdPercent",policy.thresholdPercent());s.addProperty("onlyWhileFarming",policy.onlyWhileFarming());s.addProperty("radius",policy.radius());if(policy.anchor()==null)s.remove("anchor");else{var a=object(s,"anchor");a.addProperty("dimension",policy.anchor().dimension());a.addProperty("x",policy.anchor().x());a.addProperty("y",policy.anchor().y());a.addProperty("z",policy.anchor().z());}});}
 public boolean pestPolicyValid(){try{readPestPolicy();return writable;}catch(RuntimeException invalid){return false;}}
 public dev.kuan.kuanperion.automation.pest.PestPolicy pestPolicy(){try{return readPestPolicy();}catch(RuntimeException invalid){return dev.kuan.kuanperion.automation.pest.PestPolicy.defaults();}}
 private dev.kuan.kuanperion.automation.pest.PestPolicy readPestPolicy(){var s=section(root,"pest");return new dev.kuan.kuanperion.automation.pest.PestPolicy(strictInteger(s,"threshold",1,1,8),strictInteger(s,"timeoutSeconds",120,30,240),strictInteger(s,"captureSeconds",20,5,30),strictInteger(s,"maxTargets",16,1,64),strictInteger(s,"radius",64,16,96),strictInteger(s,"cooldownSeconds",60,10,3600),strictBoolean(s,"onlyWhileFarming",true),strictBoolean(s,"sweep",false),strictInteger(s,"travelPlots",0,0,24),strictBoolean(s,"clearTraps",false));}
 public void setPestPolicy(dev.kuan.kuanperion.automation.pest.PestPolicy p)throws IOException{update(copy->{var s=object(copy,"pest");s.addProperty("threshold",p.threshold());s.addProperty("timeoutSeconds",p.timeoutSeconds());s.addProperty("captureSeconds",p.captureSeconds());s.addProperty("maxTargets",p.maxTargets());s.addProperty("radius",p.radius());s.addProperty("cooldownSeconds",p.cooldownSeconds());s.addProperty("onlyWhileFarming",p.onlyWhileFarming());s.addProperty("sweep",p.sweep());s.addProperty("travelPlots",p.travelPlots());s.addProperty("clearTraps",p.clearTraps());});}
 public boolean greenhousePolicyValid(){try{readGreenhousePolicy();return writable;}catch(RuntimeException invalid){return false;}}
 public dev.kuan.kuanperion.automation.greenhouse.GreenhousePolicy greenhousePolicy(){try{return readGreenhousePolicy();}catch(RuntimeException invalid){return dev.kuan.kuanperion.automation.greenhouse.GreenhousePolicy.defaults();}}
 private dev.kuan.kuanperion.automation.greenhouse.GreenhousePolicy readGreenhousePolicy(){
  var s=section(root,"greenhouse");var plots=new java.util.HashSet<Integer>();var kinds=java.util.EnumSet.noneOf(dev.kuan.kuanperion.automation.greenhouse.GreenhouseIdentity.Kind.class);
  if(s.has("plots")){var a=s.get("plots");if(!a.isJsonArray()||a.getAsJsonArray().size()>24)throw new IllegalArgumentException("Invalid Greenhouse plots");for(var v:a.getAsJsonArray()){if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber()||!plots.add(v.getAsBigDecimal().intValueExact()))throw new IllegalArgumentException("Invalid/duplicate Greenhouse plot");}}
  if(s.has("kinds")){var a=s.get("kinds");if(!a.isJsonArray()||a.getAsJsonArray().size()>3)throw new IllegalArgumentException("Invalid Greenhouse crops");for(var v:a.getAsJsonArray()){if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString()||!kinds.add(dev.kuan.kuanperion.automation.greenhouse.GreenhouseIdentity.Kind.valueOf(v.getAsString())))throw new IllegalArgumentException("Invalid/duplicate Greenhouse crop");}}
  return new dev.kuan.kuanperion.automation.greenhouse.GreenhousePolicy(plots,kinds,s.has("toolId")?strictString(s,"toolId"):"",s.has("toolUuid")?strictString(s,"toolUuid"):"",strictInteger(s,"radius",25,4,25),strictInteger(s,"maxTargets",128,1,256),strictInteger(s,"timeoutSeconds",240,90,300),strictInteger(s,"intervalSeconds",600,60,86400),strictBoolean(s,"onlyWhileFarming",true));
 }
 public void setGreenhousePolicy(dev.kuan.kuanperion.automation.greenhouse.GreenhousePolicy p)throws IOException{update(copy->{var s=object(copy,"greenhouse");s.add("plots",JSON.toJsonTree(new java.util.TreeSet<>(p.plots())));s.add("kinds",JSON.toJsonTree(p.kinds().stream().map(Enum::name).sorted().toList()));s.addProperty("toolId",p.toolId());s.addProperty("toolUuid",p.toolUuid());s.addProperty("radius",p.radius());s.addProperty("maxTargets",p.maxTargets());s.addProperty("timeoutSeconds",p.timeoutSeconds());s.addProperty("intervalSeconds",p.intervalSeconds());s.addProperty("onlyWhileFarming",p.onlyWhileFarming());});}
 public boolean composterPolicyValid(){try{readComposterPolicy();return writable;}catch(RuntimeException invalid){return false;}}
 public dev.kuan.kuanperion.automation.composter.ComposterPolicy composterPolicy(){try{return readComposterPolicy();}catch(RuntimeException invalid){return dev.kuan.kuanperion.automation.composter.ComposterPolicy.defaults();}}
 private dev.kuan.kuanperion.automation.composter.ComposterPolicy readComposterPolicy(){
  var s=section(root,"composter");dev.kuan.kuanperion.automation.composter.ComposterPolicy.Anchor anchor=null;
  if(s.has("anchor")){var a=s.getAsJsonObject("anchor");if(!a.has("x")||!a.has("y")||!a.has("z"))throw new IllegalArgumentException("Missing Composter coordinates");anchor=new dev.kuan.kuanperion.automation.composter.ComposterPolicy.Anchor(strictInteger(a,"x",0,-48,47),strictInteger(a,"y",72,-2048,2048),strictInteger(a,"z",0,-48,47),strictString(a,"blockId"));}
  var materials=new java.util.TreeMap<String,dev.kuan.kuanperion.automation.composter.ComposterPolicy.Material>();
  if(s.has("materials")){var entries=s.getAsJsonObject("materials");if(entries.size()>32)throw new IllegalArgumentException("Too many Composter materials");for(var e:entries.entrySet()){var m=e.getValue().getAsJsonObject();materials.put(e.getKey(),new dev.kuan.kuanperion.automation.composter.ComposterPolicy.Material(dev.kuan.kuanperion.automation.composter.ComposterObservation.Kind.valueOf(strictString(m,"kind")),strictInteger(m,"units",0,1,1000000000)));}}
  return new dev.kuan.kuanperion.automation.composter.ComposterPolicy(anchor,materials,strings(s,"protectedUuids"),strictInteger(s,"targetPercent",90,1,100),strictInteger(s,"maxStacks",3,1,27),strictInteger(s,"maxItems",192,1,1728),strictInteger(s,"delayMillis",1000,500,5000),strictInteger(s,"timeoutSeconds",240,90,300),strictInteger(s,"intervalSeconds",7200,60,86400),strictBoolean(s,"onlyWhileFarming",true));
 }
 public void setComposterPolicy(dev.kuan.kuanperion.automation.composter.ComposterPolicy p)throws IOException{update(copy->{
  var s=object(copy,"composter");if(p.anchor()==null)s.remove("anchor");else{var a=object(s,"anchor");a.addProperty("x",p.anchor().x());a.addProperty("y",p.anchor().y());a.addProperty("z",p.anchor().z());a.addProperty("blockId",p.anchor().blockId());}
  var previous=section(s,"materials");var materials=new JsonObject();for(var e:new java.util.TreeMap<>(p.materials()).entrySet()){var m=section(previous,e.getKey()).deepCopy();m.addProperty("kind",e.getValue().kind().name());m.addProperty("units",e.getValue().units());materials.add(e.getKey(),m);}s.add("materials",materials);
  s.add("protectedUuids",JSON.toJsonTree(new java.util.TreeSet<>(p.protectedUuids())));s.addProperty("targetPercent",p.targetPercent());s.addProperty("maxStacks",p.maxStacks());s.addProperty("maxItems",p.maxItems());s.addProperty("delayMillis",p.delayMillis());s.addProperty("timeoutSeconds",p.timeoutSeconds());s.addProperty("intervalSeconds",p.intervalSeconds());s.addProperty("onlyWhileFarming",p.onlyWhileFarming());
 });}
 private static boolean strictBoolean(JsonObject object,String key,boolean fallback){if(!object.has(key))return fallback;var value=object.get(key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Invalid Pest toggle");return value.getAsBoolean();}
 private static String strictString(JsonObject o,String key){var v=o.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Invalid text");return v.getAsString();}
 private static double strictNumber(JsonObject o,String key){var v=o.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Invalid coordinate/limit");return v.getAsDouble();}
 private static int strictInteger(JsonObject o,String key,int fallback,int min,int max){if(!o.has(key))return fallback;var v=o.get(key);if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Invalid Junk limit");int n=v.getAsBigDecimal().intValueExact();if(n<min||n>max)throw new IllegalArgumentException("Invalid Junk limit");return n;}
 private static java.util.Set<String> strings(JsonObject s,String key){var value=s.get(key);if(value==null)return java.util.Set.of();if(!value.isJsonArray()||value.getAsJsonArray().size()>128)throw new IllegalArgumentException("Invalid ID list");var result=new java.util.HashSet<String>();for(var v:value.getAsJsonArray()){if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Invalid ID");result.add(v.getAsString());}return java.util.Set.copyOf(result);}
 private void update(Consumer<JsonObject> edit)throws IOException {
  if(!writable)throw new IOException(issue);
  if(!Arrays.equals(fingerprint,readBytes()))throw new IOException("設定已被其他視窗修改，未覆寫；請重新啟動讀取最新設定");
  var copy=root.deepCopy();edit.accept(copy);byte[] bytes=JSON.toJson(copy).getBytes(StandardCharsets.UTF_8);
  if(bytes.length>MAX_BYTES)throw new IOException("Config exceeds limit");
  Files.createDirectories(path.getParent());Path temporary=Files.createTempFile(path.getParent(),"automation-",".tmp");
  try{
   Files.write(temporary,bytes);
   if(!Arrays.equals(fingerprint,readBytes()))throw new IOException("設定已變更，未覆寫");
   Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   root=copy;fingerprint=bytes;
  }finally{Files.deleteIfExists(temporary);}
 }
 private static JsonObject section(JsonObject root,String name){var value=root.get(name);return value!=null&&value.isJsonObject()?value.getAsJsonObject():new JsonObject();}
 private static JsonObject object(JsonObject root,String name){if(!root.has(name)){var o=new JsonObject();root.add(name,o);return o;}return root.getAsJsonObject(name);}
 private static boolean bool(JsonObject section,String name,boolean fallback){var v=section.get(name);return v!=null&&v.isJsonPrimitive()&&v.getAsJsonPrimitive().isBoolean()?v.getAsBoolean():fallback;}
 private static int integer(JsonObject section,String name,int fallback,int min,int max){try{var v=section.get(name);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())return fallback;int n=v.getAsBigDecimal().intValueExact();return n>=min&&n<=max?n:fallback;}catch(RuntimeException invalid){return fallback;}}
}
