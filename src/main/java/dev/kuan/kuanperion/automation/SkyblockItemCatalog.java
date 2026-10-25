// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import com.google.gson.*;import com.google.gson.stream.*;import java.util.*;import java.io.*;import java.nio.*;import java.nio.charset.*;import java.math.BigDecimal;import java.util.regex.Pattern;
/** Bounded offline projection of the public official resource catalog. No name guessing, fetch or action authority. */
public final class SkyblockItemCatalog {
 public static final int MAX_BYTES=8*1024*1024;
 private final long updatedAt;private final Map<String,String> names;
 private SkyblockItemCatalog(long updatedAt,Map<String,String> names){this.updatedAt=updatedAt;this.names=Map.copyOf(names);}
 public long updatedAt(){return updatedAt;}public Map<String,String> names(){return names;}
 public static SkyblockItemCatalog parse(byte[] bytes,long now){
  if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES||now<=0)throw invalid();
  try{
   String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
   var reader=new JsonReader(new StringReader(text));reader.setStrictness(Strictness.STRICT);var root=read(reader,0,new int[]{0}).getAsJsonObject();if(reader.peek()!=JsonToken.END_DOCUMENT)throw invalid();
   var success=root.get("success");if(success==null||!success.isJsonPrimitive()||!success.getAsJsonPrimitive().isBoolean()||!success.getAsBoolean())throw invalid();
   var timestamp=root.get("lastUpdated");if(timestamp==null||!timestamp.isJsonPrimitive()||!timestamp.getAsJsonPrimitive().isNumber())throw invalid();
   long updated=timestamp.getAsBigDecimal().longValueExact();if(updated<=0||updated>now&&updated-now>30_000)throw invalid();
   var items=root.getAsJsonArray("items");if(items==null||items.isEmpty()||items.size()>8192)throw invalid();
   var names=new HashMap<String,String>();var ambiguous=new HashSet<String>();var ids=new HashSet<String>();
   for(var element:items){
    var item=element.getAsJsonObject();String id=string(item,"id"),raw=string(item,"name");if(!id.matches("[A-Z0-9_:-]{1,100}")||!ids.add(id))throw invalid();
    String name=raw.replaceAll("(?i)§[0-9a-fk-orx]","").trim();
    if(name.isBlank()||name.length()>128||Pattern.compile("[\\p{C}§]").matcher(name).find())continue;
    if(ambiguous.contains(name))continue;
    if(names.putIfAbsent(name,id)!=null){names.remove(name);ambiguous.add(name);}
   }
   if(names.isEmpty())throw invalid();return new SkyblockItemCatalog(updated,names);
  }catch(RuntimeException|IOException malformed){throw invalid();}
 }
 private static String string(JsonObject o,String key){var v=o.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw invalid();return v.getAsString();}
 private static IllegalArgumentException invalid(){return new IllegalArgumentException("Invalid public item catalog");}
 /** Duplicate-key rejection includes unknown metadata, before any material-name lookup is published. */
 private static JsonElement read(JsonReader reader,int depth,int[] tokens)throws IOException{
  if(depth>32||++tokens[0]>1_000_000)throw invalid();
  return switch(reader.peek()){
   case BEGIN_OBJECT->{var o=new JsonObject();reader.beginObject();while(reader.hasNext()){String key=reader.nextName();if(key.length()>256||o.size()>=2048||o.has(key))throw invalid();o.add(key,read(reader,depth+1,tokens));}reader.endObject();yield o;}
   case BEGIN_ARRAY->{var a=new JsonArray();reader.beginArray();while(reader.hasNext()){if(a.size()>=16384)throw invalid();a.add(read(reader,depth+1,tokens));}reader.endArray();yield a;}
   case STRING->{String s=reader.nextString();if(s.length()>16384)throw invalid();yield new JsonPrimitive(s);}
   case NUMBER->{String n=reader.nextString();if(n.length()>64)throw invalid();yield new JsonPrimitive(new BigDecimal(n));}
   case BOOLEAN->new JsonPrimitive(reader.nextBoolean());
   case NULL->{reader.nextNull();yield JsonNull.INSTANCE;}
   default->throw invalid();
  };
 }
}
