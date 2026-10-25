// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.*;import java.util.concurrent.*;import java.util.function.LongSupplier;
/** Necessary public item-name data, strictly on demand. No player/account data or background polling. */
public final class HypixelItemCatalogClient implements AutoCloseable {
 private static final long TTL=86_400_000L;
 private final HypixelMarketClient.Transport transport;private final LongSupplier clock;
 private SkyblockItemCatalog cached;private CompletableFuture<SkyblockItemCatalog> inFlight;private CompletableFuture<HypixelMarketClient.Response> wire;
 private long receivedAt,nextAllowed,lastClock;private boolean closed,clockSeen;
 public HypixelItemCatalogClient(){this(new NativeTransport(),System::currentTimeMillis);}
 HypixelItemCatalogClient(HypixelMarketClient.Transport transport,LongSupplier clock){this.transport=Objects.requireNonNull(transport);this.clock=Objects.requireNonNull(clock);}
 private long now(){long now=clock.getAsLong();if(clockSeen&&now<lastClock){cached=null;nextAllowed=0;}clockSeen=true;lastClock=now;return now;}
 public synchronized Optional<SkyblockItemCatalog> latest(){long now=now();if(cached!=null&&(now-receivedAt<0||now-receivedAt>TTL))cached=null;return Optional.ofNullable(cached);}
 public synchronized CompletableFuture<SkyblockItemCatalog> refresh(){
  long now=now();if(closed)return CompletableFuture.failedFuture(unavailable());
  if(inFlight!=null)return inFlight.copy();var fresh=latest();if(fresh.isPresent())return CompletableFuture.completedFuture(fresh.get());
  if(now<nextAllowed)return CompletableFuture.failedFuture(unavailable());
  var result=new CompletableFuture<SkyblockItemCatalog>();inFlight=result;nextAllowed=now+60_000;
  try{
   wire=transport.fetch();wire.orTimeout(10,TimeUnit.SECONDS).whenComplete((response,error)->{
    long received=clock.getAsLong();SkyblockItemCatalog catalog=null;
    if(error==null&&response!=null&&response.status()==200&&response.contentType()!=null&&response.contentType().split(";",2)[0].strip().equalsIgnoreCase("application/json")){
     try{catalog=SkyblockItemCatalog.parse(response.bytes(),received);}catch(IllegalArgumentException rejected){}
    }
    synchronized(HypixelItemCatalogClient.this){
     if(closed||inFlight!=result){result.completeExceptionally(unavailable());return;}
     cached=catalog;receivedAt=received;lastClock=received;clockSeen=true;inFlight=null;wire=null;
     if(catalog==null){nextAllowed=received+60_000;result.completeExceptionally(unavailable());}else result.complete(catalog);
    }
   });
  }catch(RuntimeException failed){cached=null;inFlight=null;wire=null;result.completeExceptionally(unavailable());}
  return result.copy();
 }
 @Override public synchronized void close(){
  if(closed)return;closed=true;cached=null;var f=inFlight;var w=wire;inFlight=null;wire=null;
  if(f!=null)f.completeExceptionally(unavailable());if(w!=null)w.cancel(true);transport.close();
 }
 private static IllegalStateException unavailable(){return new IllegalStateException("Public item catalog unavailable; no material inferred");}
 static HttpRequest request(){return HttpRequest.newBuilder(URI.create("https://api.hypixel.net/v2/resources/skyblock/items")).timeout(Duration.ofSeconds(8)).header("Accept","application/json").GET().build();}
 private static final class NativeTransport implements HypixelMarketClient.Transport {
  private final HttpClient client=HypixelMarketClient.newHttpClient();
  public CompletableFuture<HypixelMarketClient.Response> fetch(){
   var http=client.sendAsync(request(),info->new HypixelMarketClient.LimitedBodySubscriber(SkyblockItemCatalog.MAX_BYTES));
   var result=http.thenApply(r->new HypixelMarketClient.Response(r.statusCode(),r.headers().firstValue("Content-Type").orElse(""),r.body()));
   result.whenComplete((ignored,error)->{if(error!=null)http.cancel(true);});return result;
  }
  public void close(){client.shutdownNow();}
 }
}
