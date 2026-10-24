package dev.kuan.kuanperion.automation;
import java.nio.charset.StandardCharsets;
public final class BazaarMarketTest {
 static final long NOW=1_800_000_000_000L;
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 static byte[] data(String body){return body.getBytes(StandardCharsets.UTF_8);}
 static String fixture(){return "{\"success\":true,\"lastUpdated\":"+NOW+",\"products\":{\"WHEAT\":{\"product_id\":\"WHEAT\",\"buy_summary\":[{\"amount\":2,\"pricePerUnit\":4.2},{\"amount\":64,\"pricePerUnit\":4.8}],\"sell_summary\":[{\"amount\":3,\"pricePerUnit\":3.7}],\"quick_status\":{\"buyPrice\":999}}}}";}
 static void rejected(String input){try{BazaarMarket.parse(data(input),NOW);throw new AssertionError("untrusted payload accepted");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
  var market=BazaarMarket.parse(data(fixture()),NOW);
  check(market.estimateBuy("WHEAT",3,NOW).orElseThrow().coins()==14,"instant buy traverses buy_summary, rounds total up, never quick_status");
  check(market.estimateSell("WHEAT",3,NOW).orElseThrow().coins()==11,"instant sell traverses sell_summary and rounds down");
  check(market.estimateBuy("WHEAT",67,NOW).isEmpty(),"no extrapolation beyond available top30 depth");
  check(market.estimateBuy("WHEAT",1,NOW+120_001).isEmpty(),"stale cache not authority");
  check(market.estimateBuy("WHEAT",0,NOW).isEmpty()&&market.estimateBuy("WHEAT",4097,NOW).isEmpty()&&market.estimateBuy("wheat",1,NOW).isEmpty(),"exact IDs and bounded quantity");
  rejected(fixture().replace("true","false"));rejected(fixture().replace("\"product_id\":\"WHEAT\"","\"product_id\":\"CARROT_ITEM\""));
  rejected(fixture().replace("4.2","-4.2"));rejected(fixture().replace("4.2","\"4.2\""));rejected(fixture().replace("\"amount\":2","\"amount\":2.5"));
  rejected(fixture().replace("4.2","1e100"));rejected(fixture().replace("\"amount\":2","\"amount\":-1"));
  rejected(fixture().replace(Long.toString(NOW),Long.toString(NOW-120_001)));rejected(fixture().replace(Long.toString(NOW),Long.toString(NOW+30_001)));
  rejected("[".repeat(40)+"0"+"]".repeat(40));rejected(fixture()+" trailing");rejected(fixture().replace("\"success\":true","\"success\":\"true\""));
  rejected(fixture().replace("\"success\"","success"));
  rejected(" ".repeat(8*1024*1024+1));
  check(market.books().get("WHEAT").buy().size()==2,"immutable validated snapshot");
  try{market.books().clear();throw new AssertionError("mutable snapshot");}catch(UnsupportedOperationException expected){}
  System.out.println("BazaarMarketTest: PASS (official book sides, exact bounded depth/rounding, freshness, schema/product/price/size/depth validation)");
 }
}
