package dev.kuan.kuanperion.automation;
import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.nio.*;
import java.util.concurrent.Flow;
public final class HypixelMarketClientTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void failed(CompletableFuture<?> f){try{f.join();throw new AssertionError("failure accepted");}catch(CompletionException expected){}}
 static final class Fake implements HypixelMarketClient.Transport {
  int calls;CompletableFuture<HypixelMarketClient.Response> pending;
  public CompletableFuture<HypixelMarketClient.Response> fetch(){calls++;pending=new CompletableFuture<>();return pending;}
  void success(){pending.complete(new HypixelMarketClient.Response(200,"application/json; charset=utf-8",BazaarMarketTest.data(BazaarMarketTest.fixture())));}
 }
 public static void main(String[] args){
  long[] clock={BazaarMarketTest.NOW};var transport=new Fake();var client=new HypixelMarketClient(transport,()->clock[0]);
  check(transport.calls==0&&client.latest().isEmpty(),"constructing/loading settings never fetches or starts a loop");
  var a=client.refresh();var b=client.refresh();check(transport.calls==1,"concurrent demand coalesced");transport.success();check(a.join()==b.join()&&client.latest().isPresent(),"validated immutable cached result");
  client.refresh().join();check(transport.calls==1,"minimum refresh period");
  clock[0]+=20_001;var c=client.refresh();check(transport.calls==2,"new request only on demand after interval");transport.pending.complete(new HypixelMarketClient.Response(302,"application/json",new byte[0]));failed(c);check(client.latest().isEmpty(),"redirect/error invalidates old snapshot, never follows");
  failed(client.refresh());check(transport.calls==2,"failure backoff no retry loop");
  clock[0]+=60_001;var d=client.refresh();transport.pending.complete(new HypixelMarketClient.Response(200,"text/html",BazaarMarketTest.data(BazaarMarketTest.fixture())));failed(d);
  clock[0]+=60_001;var e=client.refresh();transport.success();failed(e);check(client.latest().isEmpty(),"response too old rejected even after successful HTTP");
  var request=HypixelMarketClient.request();check(request.uri().toString().equals("https://api.hypixel.net/v2/skyblock/bazaar")&&request.method().equals("GET"),"fixed official public endpoint, no user parameters");
  check(request.headers().map().keySet().stream().allMatch(k->k.equalsIgnoreCase("Accept")),"no auth cookie player token or custom device headers");
  check(request.timeout().orElseThrow().toSeconds()==8,"bounded request duration");
  var subscriber=new HypixelMarketClient.LimitedBodySubscriber(4);boolean[] cancelled={false};
  subscriber.onSubscribe(new Flow.Subscription(){public void request(long n){}public void cancel(){cancelled[0]=true;}});
  subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2,3})));subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{4,5})));failed(subscriber.getBody().toCompletableFuture());check(cancelled[0],"oversized streaming body is cancelled before allocation");
  var valid=new HypixelMarketClient.LimitedBodySubscriber(4);valid.onSubscribe(new Flow.Subscription(){public void request(long n){}public void cancel(){}});valid.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2})));valid.onComplete();check(valid.getBody().toCompletableFuture().join().length==2,"normal bounded body completion");
  try(var nativeClient=HypixelMarketClient.newHttpClient()){check(nativeClient.followRedirects()==HttpClient.Redirect.NEVER,"native transport never follows redirects");}
  client.close();failed(client.refresh());check(transport.calls==4,"closed client cannot fetch again");
  System.out.println("HypixelMarketClientTest: PASS (on-demand only, fixed anonymous endpoint, coalescing/cache/backoff, stale/status/media rejection, bounded streaming body)");
 }
}
