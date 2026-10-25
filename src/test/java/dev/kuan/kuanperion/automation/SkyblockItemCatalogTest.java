package dev.kuan.kuanperion.automation;
import java.util.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;
public final class SkyblockItemCatalogTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static String document(String items){return "{\"success\":true,\"lastUpdated\":1000,\"items\":["+items+"]}";}
 static String item(String id,String name){return "{\"id\":\""+id+"\",\"name\":\""+name+"\"}";}
 static SkyblockItemCatalog parse(String s){return SkyblockItemCatalog.parse(s.getBytes(StandardCharsets.UTF_8),2000);}
 static void reject(String json){try{parse(json);throw new AssertionError("accepted malformed catalog");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args)throws Exception{
  var catalog=parse(document(item("WHEAT","Wheat")+","+item("ENCHANTED_WHEAT","Enchanted Wheat")));check(catalog.names().get("Enchanted Wheat").equals("ENCHANTED_WHEAT")&&catalog.updatedAt()==1000,"exact official display-name mapping, no fuzzy lookup");
  var ambiguous=parse(document(item("BOOK_A","Book")+","+item("BOOK_B","Book")+","+item("WHEAT","Wheat")));check(!ambiguous.names().containsKey("Book")&&ambiguous.names().size()==1,"ambiguous names omitted rather than pick first or last");
  var styled=parse(document(item("WHEAT","§aWheat")+","+item("CARROT_ITEM","Carrot")));check(styled.names().get("Wheat").equals("WHEAT")&&!styled.names().containsKey("wheat"),"legacy formatting only, not case/fuzzy normalization");
  for(String body:List.of(document(item("WHEAT","Wheat")+","+item("WHEAT","Other")),document("{\"id\":\"WHEAT\",\"id\":\"OTHER\",\"name\":\"Wheat\"}"),document("{\"id\":\"WHEAT\",\"name\":\"Wheat\",\"unknown\":{\"x\":1,\"x\":2}}"),document(item("bad command","Wheat")),document("{\"id\":123,\"name\":\"Wheat\"}"),document("{\"id\":\"WHEAT\",\"name\":123}"),document(""),document(item("WHEAT","Wheat")).replace("true","false"),document(item("WHEAT","Wheat")).replace("1000","1000.5"),document(item("WHEAT","Wheat"))+"{}",document(item("WHEAT","Wheat")).replace("1000","999999")))reject(body);
  reject("{\"success\":true,\"lastUpdated\":1000,\"success\":true,\"items\":["+item("WHEAT","Wheat")+"]}");
  var hidden=parse(document(item("BAD_NAME","bad\\u200bname")+","+item("WHEAT","Wheat")));check(!hidden.names().containsValue("BAD_NAME"),"control/format names excluded");
  reject(document(item("WHEAT","Wheat"))+" trailing");
  try{SkyblockItemCatalog.parse(new byte[]{(byte)0xc3,(byte)0x28},2000);throw new AssertionError("invalid UTF8");}catch(IllegalArgumentException expected){}
  try{SkyblockItemCatalog.parse(new byte[SkyblockItemCatalog.MAX_BYTES+1],2000);throw new AssertionError("unbounded bytes");}catch(IllegalArgumentException expected){}
  var deep="0";for(int i=0;i<34;i++)deep="["+deep+"]";reject(document(item("WHEAT","Wheat")).replace("\"items\":","\"unknown\":"+deep+",\"items\":"));
  try{catalog.names().clear();throw new AssertionError("mutable catalog");}catch(UnsupportedOperationException expected){}
  if(args.length==1){var actual=SkyblockItemCatalog.parse(Files.readAllBytes(Path.of(args[0])),1793000000000L);check(actual.names().get("ENCHANTED_WHEAT")==null&&actual.names().get("Enchanted Wheat").equals("ENCHANTED_WHEAT"),"full official cached snapshot exact name/ID");System.out.println("Official cached item catalog: PASS ("+actual.names().size()+" unique names, no network)");}
  System.out.println("SkyblockItemCatalogTest: PASS (bounded strict schema/UTF8/depth/duplicate keys, exact IDs and unambiguous names only, immutable no-network lookup)");
 }
}
