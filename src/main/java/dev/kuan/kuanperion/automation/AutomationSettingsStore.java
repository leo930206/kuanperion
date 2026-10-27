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
   var text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(fingerprint)).toString();
   var reader=new com.google.gson.stream.JsonReader(new java.io.StringReader(text));reader.setStrictness(Strictness.STRICT);var parsed=readJson(reader,0,new int[]{0});if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT)throw new IllegalArgumentException("Trailing settings data");
   if(!parsed.isJsonObject())throw new IllegalArgumentException("Not an object");
   var candidate=parsed.getAsJsonObject();var version=candidate.get("schemaVersion");
   if(version==null||!version.isJsonPrimitive()||!version.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Missing schema");
   int schema=version.getAsBigDecimal().intValueExact();
   if(schema!=SCHEMA){writable=false;issue="設定格式版本不相容，未啟用自動化或覆寫檔案";return;}
   for(String name:new String[]{"features","stash","autoSell","junk","pest","greenhouse","composter","visitor","supercraft","bookCombine"})if(candidate.has(name)&&!candidate.get(name).isJsonObject())throw new IllegalArgumentException("Invalid settings section");
   root=candidate;
  }catch(IOException|RuntimeException invalid){writable=false;issue="自動化設定無法讀取，原檔保留且自動化停用";}
 }
 /** Reject ambiguous consent before Gson can collapse duplicate names into last-wins values. Unknown valid data remains intact. */
 private static JsonElement readJson(com.google.gson.stream.JsonReader reader,int depth,int[] nodes)throws IOException{
  if(depth>32||++nodes[0]>32768)throw new IllegalArgumentException("Settings nesting/entry bound");
  return switch(reader.peek()){
   case BEGIN_OBJECT->{var object=new JsonObject();reader.beginObject();while(reader.hasNext()){String key=reader.nextName();if(object.has(key))throw new IllegalArgumentException("Duplicate settings field");object.add(key,readJson(reader,depth+1,nodes));}reader.endObject();yield object;}
   case BEGIN_ARRAY->{var array=new JsonArray();reader.beginArray();while(reader.hasNext())array.add(readJson(reader,depth+1,nodes));reader.endArray();yield array;}
   case STRING->new JsonPrimitive(reader.nextString());case NUMBER->new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));case BOOLEAN->new JsonPrimitive(reader.nextBoolean());case NULL->{reader.nextNull();yield JsonNull.INSTANCE;}default->throw new IllegalArgumentException("Invalid settings JSON");
  };
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
 private dev.kuan.kuanperion.automation.pest.PestPolicy readPestPolicy(){var s=section(root,"pest");return new dev.kuan.kuanperion.automation.pest.PestPolicy(strictInteger(s,"threshold",1,1,8),strictInteger(s,"timeoutSeconds",120,30,240),strictInteger(s,"captureSeconds",20,5,30),strictInteger(s,"maxTargets",16,1,64),strictInteger(s,"radius",64,16,96),strictInteger(s,"cooldownSeconds",60,10,3600),strictBoolean(s,"onlyWhileFarming",true),strictBoolean(s,"sweep",false),strictInteger(s,"travelPlots",0,0,24),strictBoolean(s,"clearTraps",false),strictInteger(s,"trapPlot",-1,-1,24));}
 public void setPestPolicy(dev.kuan.kuanperion.automation.pest.PestPolicy p)throws IOException{update(copy->{var s=object(copy,"pest");s.addProperty("threshold",p.threshold());s.addProperty("timeoutSeconds",p.timeoutSeconds());s.addProperty("captureSeconds",p.captureSeconds());s.addProperty("maxTargets",p.maxTargets());s.addProperty("radius",p.radius());s.addProperty("cooldownSeconds",p.cooldownSeconds());s.addProperty("onlyWhileFarming",p.onlyWhileFarming());s.addProperty("sweep",p.sweep());s.addProperty("travelPlots",p.travelPlots());s.addProperty("clearTraps",p.clearTraps());s.addProperty("trapPlot",p.trapPlot());});}
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
 public boolean visitorPolicyValid(){try{readVisitorPolicy();return writable;}catch(RuntimeException invalid){return false;}}
 public dev.kuan.kuanperion.automation.visitor.VisitorPolicy visitorPolicy(){try{return readVisitorPolicy();}catch(RuntimeException invalid){return dev.kuan.kuanperion.automation.visitor.VisitorPolicy.defaults();}}
 private dev.kuan.kuanperion.automation.visitor.VisitorPolicy readVisitorPolicy(){
  var s=section(root,"visitor");var rules=new dev.kuan.kuanperion.automation.visitor.VisitorValuation.Rules(visitorStrings(s,"visitors",128),visitorStrings(s,"materialIds",256),visitorLong(s,"maximumOfferCoins",0),visitorLong(s,"maximumBuyCoins",0),strictInteger(s,"minimumCopper",1,0,1000000));
  return new dev.kuan.kuanperion.automation.visitor.VisitorPolicy(rules,strictInteger(s,"threshold",5,1,5),strictInteger(s,"maxVisitors",5,1,5),strictInteger(s,"timeoutSeconds",240,90,300),strictInteger(s,"cooldownSeconds",60,30,86400),strictBoolean(s,"onlyWhileFarming",true));
 }
 private static long visitorLong(JsonObject s,String key,long fallback){if(!s.has(key))return fallback;var v=s.get(key);if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Invalid Visitor budget");long n=v.getAsBigDecimal().longValueExact();if(n<0||n>1000000000000L)throw new IllegalArgumentException("Invalid Visitor budget");return n;}
 private static java.util.Set<String> visitorStrings(JsonObject s,String key,int limit){var v=s.get(key);if(v==null)return java.util.Set.of();if(!v.isJsonArray()||v.getAsJsonArray().size()>limit)throw new IllegalArgumentException("Invalid Visitor list");var result=new java.util.HashSet<String>();for(var entry:v.getAsJsonArray()){if(!entry.isJsonPrimitive()||!entry.getAsJsonPrimitive().isString()||!result.add(entry.getAsString()))throw new IllegalArgumentException("Invalid/duplicate Visitor entry");}return java.util.Set.copyOf(result);}
 public void setVisitorPolicy(dev.kuan.kuanperion.automation.visitor.VisitorPolicy p)throws IOException{update(copy->{var s=object(copy,"visitor");var r=p.rules();s.add("visitors",JSON.toJsonTree(new java.util.TreeSet<>(r.visitors())));s.add("materialIds",JSON.toJsonTree(new java.util.TreeSet<>(r.materialIds())));s.addProperty("maximumOfferCoins",r.maximumOfferCoins());s.addProperty("maximumBuyCoins",r.maximumBuyCoins());s.addProperty("minimumCopper",r.minimumCopper());s.addProperty("threshold",p.threshold());s.addProperty("maxVisitors",p.maxVisitors());s.addProperty("timeoutSeconds",p.timeoutSeconds());s.addProperty("cooldownSeconds",p.cooldownSeconds());s.addProperty("onlyWhileFarming",p.onlyWhileFarming());});}
 public boolean bookPolicyValid(){try{readBookPolicy();return writable;}catch(RuntimeException invalid){return false;}}
 public BookCombineTask.Policy bookPolicy(){try{return readBookPolicy();}catch(RuntimeException invalid){return BookCombineTask.Policy.defaults();}}
 private BookCombineTask.Policy readBookPolicy(){
  var s=section(root,"bookCombine");var ceilings=new java.util.TreeMap<String,Integer>();
  if(s.has("ceilings")){var raw=s.get("ceilings");if(!raw.isJsonObject()||raw.getAsJsonObject().size()>64)throw new IllegalArgumentException("Invalid book authorization");for(var e:raw.getAsJsonObject().entrySet())ceilings.put(e.getKey(),strictInteger(raw.getAsJsonObject(),e.getKey(),0,1,10));}
  return new BookCombineTask.Policy(ceilings,visitorStrings(s,"protectedUuids",128),strictInteger(s,"delayMillis",1000,500,5000),strictInteger(s,"timeoutSeconds",60,10,180),strictBoolean(s,"onlyWhileFarming",true));
 }
 public void setBookPolicy(BookCombineTask.Policy p)throws IOException{update(copy->{var s=object(copy,"bookCombine");s.add("ceilings",JSON.toJsonTree(new java.util.TreeMap<>(p.ceilings())));s.add("protectedUuids",JSON.toJsonTree(new java.util.TreeSet<>(p.protectedUuids())));s.addProperty("delayMillis",p.delayMillis());s.addProperty("timeoutSeconds",p.timeoutSeconds());s.addProperty("onlyWhileFarming",p.onlyWhileFarming());});}
 public boolean supercraftPolicyValid(){try{readSupercraftPolicy();return writable;}catch(RuntimeException invalid){return false;}}
 public dev.kuan.kuanperion.automation.supercraft.SupercraftPolicy supercraftPolicy(){try{return readSupercraftPolicy();}catch(RuntimeException invalid){return dev.kuan.kuanperion.automation.supercraft.SupercraftPolicy.defaults();}}
 private dev.kuan.kuanperion.automation.supercraft.SupercraftPolicy readSupercraftPolicy(){
  var s=section(root,"supercraft");dev.kuan.kuanperion.automation.supercraft.SupercraftPlan.Recipe recipe=null;
  if(s.has("recipe")){var r=s.getAsJsonObject("recipe");var raw=r.getAsJsonObject("inputs");if(raw==null||raw.size()>9)throw new IllegalArgumentException("Invalid Supercraft inputs");var inputs=new java.util.TreeMap<String,Integer>();for(var e:raw.entrySet())inputs.put(e.getKey(),strictInteger(raw,e.getKey(),0,1,4096));recipe=new dev.kuan.kuanperion.automation.supercraft.SupercraftPlan.Recipe(strictString(r,"outputId"),strictInteger(r,"outputPerCraft",0,1,64),strictInteger(r,"stackLimit",0,1,64),inputs);}
  var rules=new dev.kuan.kuanperion.automation.supercraft.SupercraftPlan.Rules(visitorStrings(s,"outputs",64),visitorStrings(s,"ingredients",256),strictInteger(s,"maxOutput",0,0,4096),strictInteger(s,"maxConsumed",0,0,65536),strictInteger(s,"maxCrafts",0,0,4096),visitorStrings(s,"protectedUuids",128));
  var task=new dev.kuan.kuanperion.automation.supercraft.SupercraftTask.Policy(recipe,rules,strictInteger(s,"outputCount",0,0,4096),strictInteger(s,"timeoutSeconds",90,60,180),strictBoolean(s,"onlyWhileFarming",true));return new dev.kuan.kuanperion.automation.supercraft.SupercraftPolicy(task,strictInteger(s,"intervalSeconds",600,30,86400));
 }
 public void setSupercraftPolicy(dev.kuan.kuanperion.automation.supercraft.SupercraftPolicy p)throws IOException{update(copy->{
  var s=object(copy,"supercraft");var t=p.task();var rules=t.rules();var recipe=t.recipe();if(recipe==null)s.remove("recipe");else{var r=object(s,"recipe");r.addProperty("outputId",recipe.outputId());r.addProperty("outputPerCraft",recipe.outputPerCraft());r.addProperty("stackLimit",recipe.stackLimit());r.add("inputs",JSON.toJsonTree(new java.util.TreeMap<>(recipe.inputsPerCraft())));}
  s.add("outputs",JSON.toJsonTree(new java.util.TreeSet<>(rules.outputs())));s.add("ingredients",JSON.toJsonTree(new java.util.TreeSet<>(rules.ingredients())));s.add("protectedUuids",JSON.toJsonTree(new java.util.TreeSet<>(rules.protectedUuids())));s.addProperty("maxOutput",rules.maxOutput());s.addProperty("maxConsumed",rules.maxConsumed());s.addProperty("maxCrafts",rules.maxCrafts());s.addProperty("outputCount",t.outputCount());s.addProperty("timeoutSeconds",t.timeoutSeconds());s.addProperty("onlyWhileFarming",t.onlyWhileFarming());s.addProperty("intervalSeconds",p.intervalSeconds());
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
