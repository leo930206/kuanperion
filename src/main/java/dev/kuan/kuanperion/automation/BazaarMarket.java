// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import com.google.gson.*;
import java.math.*;
import java.io.*;
import com.google.gson.stream.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** Public Hypixel top-30 order book. Estimates are never an authorization to spend coins. */
public record BazaarMarket(long updatedAt,Map<String,Book> books) {
 public static final int MAX_BYTES=8*1024*1024;
 public static final long MAX_AGE_MILLIS=120_000;
 private static final BigDecimal MAX_PRICE=new BigDecimal("1000000000000");
 public record Level(long amount,BigDecimal price){}
 public record Book(List<Level> buy,List<Level> sell){public Book{buy=List.copyOf(buy);sell=List.copyOf(sell);}}
 public record Estimate(String productId,int quantity,long coins,long updatedAt){}
 public BazaarMarket{books=Map.copyOf(books);}
 public Optional<Estimate> estimateBuy(String id,int quantity,long now){return estimate(id,quantity,now,true);}
 public Optional<Estimate> estimateSell(String id,int quantity,long now){return estimate(id,quantity,now,false);}
 private Optional<Estimate> estimate(String id,int quantity,long now,boolean buy){
  if(id==null||quantity<1||quantity>4096||!fresh(updatedAt,now))return Optional.empty();
  var book=books.get(id);if(book==null)return Optional.empty();
  int remaining=quantity;BigDecimal total=BigDecimal.ZERO;
  // Official buy_summary = instant-buy asks; sell_summary = instant-sell bids.
  for(var level:buy?book.buy():book.sell()){
   long taken=Math.min(remaining,level.amount());total=total.add(level.price().multiply(BigDecimal.valueOf(taken)));remaining-=taken;if(remaining==0)break;
  }
  if(remaining!=0)return Optional.empty();
  try{return Optional.of(new Estimate(id,quantity,total.setScale(0,buy?RoundingMode.CEILING:RoundingMode.FLOOR).longValueExact(),updatedAt));}
  catch(ArithmeticException overflow){return Optional.empty();}
 }
 public static BazaarMarket parse(byte[] bytes,long now){
  if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw invalid();
  try{
   String input=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
   boundedStructure(input);
   var reader=new JsonReader(new StringReader(input));reader.setStrictness(Strictness.STRICT);
   JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();if(reader.peek()!=JsonToken.END_DOCUMENT)throw invalid();
   if(!bool(root.get("success")))throw invalid();long updated=integer(root.get("lastUpdated"));if(!fresh(updated,now))throw invalid();
   var products=root.getAsJsonObject("products");if(products==null||products.size()>8192)throw invalid();
   var books=new HashMap<String,Book>();
   for(var entry:products.entrySet()){
    String id=entry.getKey();if(!id.matches("[A-Z0-9_:-]{1,100}"))throw invalid();
    var product=entry.getValue().getAsJsonObject();var key=product.get("product_id");
    if(key==null||!key.isJsonPrimitive()||!key.getAsJsonPrimitive().isString()||!id.equals(key.getAsString()))throw invalid();
    books.put(id,new Book(levels(product.getAsJsonArray("buy_summary"),true),levels(product.getAsJsonArray("sell_summary"),false)));
   }
   return new BazaarMarket(updated,books);
  }catch(RuntimeException|IOException malformed){throw invalid();}
 }
 private static List<Level> levels(JsonArray array,boolean buy){
  if(array==null||array.size()>30)throw invalid();var result=new ArrayList<Level>();BigDecimal previous=null;
  for(var element:array){
   var level=element.getAsJsonObject();long amount=integer(level.get("amount"));BigDecimal price=number(level.get("pricePerUnit"));
   if(amount<1||amount>1_000_000_000_000L||price.signum()<=0||price.compareTo(MAX_PRICE)>0||price.scale()>12)throw invalid();
   if(previous!=null&&(buy?previous.compareTo(price)>0:previous.compareTo(price)<0))throw invalid();
   result.add(new Level(amount,price));previous=price;
  }
  return result;
 }
 private static boolean bool(JsonElement e){return e!=null&&e.isJsonPrimitive()&&e.getAsJsonPrimitive().isBoolean()&&e.getAsBoolean();}
 private static BigDecimal number(JsonElement e){if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())throw invalid();return e.getAsBigDecimal();}
 private static long integer(JsonElement e){try{return number(e).longValueExact();}catch(ArithmeticException e1){throw invalid();}}
 private static boolean fresh(long updated,long now){return updated>0&&now>=updated-30_000&&now<=updated+MAX_AGE_MILLIS&&updated<=Long.MAX_VALUE-MAX_AGE_MILLIS;}
 private static IllegalArgumentException invalid(){return new IllegalArgumentException("Invalid or stale public Bazaar data");}
 /** Bound JSON allocation/depth before Gson; braces inside strings are not structural. */
 private static void boundedStructure(String input){
  int depth=0,tokens=0;boolean quoted=false,escaped=false;
  for(int i=0;i<input.length();i++){
   char c=input.charAt(i);
   if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='"')quoted=false;continue;}
   if(c=='"')quoted=true;
   else if(c=='{'||c=='['){if(++depth>32||++tokens>200_000)throw invalid();}
   else if(c=='}'||c==']'){if(--depth<0)throw invalid();}
  }
  if(quoted||depth!=0)throw invalid();
 }
}
