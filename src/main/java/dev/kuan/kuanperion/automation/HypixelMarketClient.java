// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

/** Anonymous public prices, strictly on demand. No player/account data or startup polling. */
public final class HypixelMarketClient implements AutoCloseable {
 record Response(int status,String contentType,byte[] bytes){}
 interface Transport extends AutoCloseable {
  CompletableFuture<Response> fetch();
  default void close(){}
 }
 private final Transport transport;private final LongSupplier clock;
 private BazaarMarket cached;private CompletableFuture<BazaarMarket> inFlight;private CompletableFuture<Response> wire;
 private long nextAllowed;private boolean closed;
 public HypixelMarketClient(){this(new NativeTransport(),System::currentTimeMillis);}
 HypixelMarketClient(Transport transport,LongSupplier clock){this.transport=transport;this.clock=clock;}
 public synchronized Optional<BazaarMarket> latest(){
  if(cached!=null&&(clock.getAsLong()<cached.updatedAt()-30_000||clock.getAsLong()>cached.updatedAt()+BazaarMarket.MAX_AGE_MILLIS))cached=null;
  return Optional.ofNullable(cached);
 }
 public synchronized CompletableFuture<BazaarMarket> refresh(){
  if(closed)return CompletableFuture.failedFuture(unavailable());
  if(inFlight!=null)return inFlight.copy();
  long now=clock.getAsLong();
  if(now<nextAllowed)return latest().map(CompletableFuture::completedFuture).orElseGet(()->CompletableFuture.failedFuture(unavailable()));
  var result=new CompletableFuture<BazaarMarket>();inFlight=result;nextAllowed=now+20_000;
  try{
   wire=transport.fetch();
   wire.orTimeout(10,TimeUnit.SECONDS).whenComplete((response,error)->{
    BazaarMarket market=null;
    if(error==null&&response!=null&&response.status()==200&&response.contentType()!=null&&response.contentType().split(";",2)[0].strip().equalsIgnoreCase("application/json")){
     try{market=BazaarMarket.parse(response.bytes(),clock.getAsLong());}catch(IllegalArgumentException rejected){}
    }
    synchronized(HypixelMarketClient.this){
     if(closed){result.completeExceptionally(unavailable());return;}
     cached=market;inFlight=null;wire=null;
     if(market==null){nextAllowed=clock.getAsLong()+60_000;result.completeExceptionally(unavailable());}
     else result.complete(market);
    }
   });
  }catch(RuntimeException failed){inFlight=null;wire=null;cached=null;nextAllowed=now+60_000;result.completeExceptionally(unavailable());}
  return result.copy();
 }
 @Override public synchronized void close(){
  if(closed)return;closed=true;cached=null;
  if(inFlight!=null)inFlight.completeExceptionally(unavailable());if(wire!=null)wire.cancel(true);
  transport.close();
 }
 private static IllegalStateException unavailable(){return new IllegalStateException("Public Bazaar data unavailable; no purchase authorized");}
 static HttpRequest request(){return HttpRequest.newBuilder(URI.create("https://api.hypixel.net/v2/skyblock/bazaar")).timeout(Duration.ofSeconds(8)).header("Accept","application/json").GET().build();}
 static HttpClient newHttpClient(){return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();}
 private static final class NativeTransport implements Transport {
  private final HttpClient client=newHttpClient();
  public CompletableFuture<Response> fetch(){
   var http=client.sendAsync(request(),info->new LimitedBodySubscriber(BazaarMarket.MAX_BYTES));
   var result=http.thenApply(r->new Response(r.statusCode(),r.headers().firstValue("Content-Type").orElse(""),r.body()));
   // orTimeout completes this result exceptionally: propagate that to the actual HTTP stream.
   result.whenComplete((ignored,error)->{if(error!=null)http.cancel(true);});
   return result;
  }
  public void close(){client.shutdownNow();}
 }
 /** Bounded at the HTTP stream boundary, not after an unbounded byte-array download. */
 static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
  private final int limit;private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  private final CompletableFuture<byte[]> result=new CompletableFuture<>();private Flow.Subscription subscription;
  LimitedBodySubscriber(int limit){if(limit<1)throw new IllegalArgumentException("Invalid body limit");this.limit=limit;}
  public CompletionStage<byte[]> getBody(){return result;}
  public void onSubscribe(Flow.Subscription value){if(subscription!=null){value.cancel();return;}subscription=value;value.request(1);}
  public void onNext(List<ByteBuffer> buffers){
   if(result.isDone())return;
   for(var buffer:buffers){
    if(buffer.remaining()>limit-bytes.size()){subscription.cancel();result.completeExceptionally(unavailable());return;}
    byte[] chunk=new byte[buffer.remaining()];buffer.get(chunk);bytes.writeBytes(chunk);
   }
   subscription.request(1);
  }
  public void onError(Throwable error){result.completeExceptionally(unavailable());}
  public void onComplete(){result.complete(bytes.toByteArray());}
 }
}
