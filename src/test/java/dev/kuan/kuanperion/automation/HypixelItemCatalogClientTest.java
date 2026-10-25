package dev.kuan.kuanperion.automation;
import java.util.*;import java.util.concurrent.*;import java.nio.charset.StandardCharsets;
public final class HypixelItemCatalogClientTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void failed(CompletableFuture<?> f){try{f.join();throw new AssertionError("failure accepted");}catch(CompletionException expected){}}
 static final byte[] DATA="{\"success\":true,\"lastUpdated\":1000,\"items\":[{\"id\":\"WHEAT\",\"name\":\"Wheat\"}]}".getBytes(StandardCharsets.UTF_8);
 static final class Fake implements HypixelMarketClient.Transport{
  int calls,closes;CompletableFuture<HypixelMarketClient.Response> pending;
  public CompletableFuture<HypixelMarketClient.Response> fetch(){calls++;pending=new CompletableFuture<>();return pending;}
  public void close(){closes++;}
  void success(){pending.complete(new HypixelMarketClient.Response(200,"application/json; charset=utf-8",DATA));}
 }
 public static void main(String[] args){
  long[] clock={2000};var wire=new Fake();var client=new HypixelItemCatalogClient(wire,()->clock[0]);check(wire.calls==0&&client.latest().isEmpty(),"no constructor/startup network or loop");
  var a=client.refresh();var b=client.refresh();check(wire.calls==1,"coalesced explicit demand");wire.success();check(a.join()==b.join()&&a.join().names().get("Wheat").equals("WHEAT"),"validated immutable result");
  clock[0]+=60_000;check(client.refresh().join()==a.join()&&wire.calls==1,"catalog cache based on receipt time, not old unchanged resource timestamp");
  clock[0]+=86_400_001;check(client.latest().isEmpty(),"cache expires without scheduling network");var c=client.refresh();check(wire.calls==2,"expired catalog fetched only when explicitly demanded");wire.pending.complete(new HypixelMarketClient.Response(302,"application/json",DATA));failed(c);failed(client.refresh());check(wire.calls==2,"redirect rejected and bounded backoff, no retry loop");
  clock[0]+=60_001;var html=client.refresh();wire.pending.complete(new HypixelMarketClient.Response(200,"text/html",DATA));failed(html);
  clock[0]+=60_001;var bad=client.refresh();wire.pending.complete(new HypixelMarketClient.Response(200,"application/json","{}".getBytes(StandardCharsets.UTF_8)));failed(bad);check(client.latest().isEmpty(),"invalid body never returns stale or partial authority");
  clock[0]+=60_001;var good=client.refresh();wire.success();good.join();clock[0]=2000;check(client.latest().isEmpty(),"clock reversal invalidates cache and backoff");var rewind=client.refresh();check(wire.calls==6,"clock reversal cannot stall future explicit refresh forever");wire.success();rewind.join();
  var request=HypixelItemCatalogClient.request();check(request.uri().toString().equals("https://api.hypixel.net/v2/resources/skyblock/items")&&request.method().equals("GET")&&request.uri().getRawQuery()==null,"fixed official public endpoint without player input");check(request.headers().map().keySet().stream().allMatch(k->k.equalsIgnoreCase("Accept")),"no account, tokens, cookies, telemetry or device headers");check(request.timeout().orElseThrow().toSeconds()==8,"bounded native duration");
  clock[0]+=86_400_001;var pending=client.refresh();check(wire.calls==7,"one explicit in-flight before close");client.close();failed(pending);check(wire.pending.isCancelled()&&wire.closes==1,"close cancels actual stream and dependent demand");failed(client.refresh());client.close();check(wire.closes==1&&wire.calls==7,"idempotent shutdown, no background refresh");
  System.out.println("HypixelItemCatalogClientTest: PASS (anonymous fixed official endpoint, demand-only coalescing/TTL/backoff, clock reset, strict status/media/body, real stream cancellation, no startup/player data)");
 }
}
