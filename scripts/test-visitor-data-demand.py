#!/usr/bin/env python3
"""Execute actual native demand/disposal bodies with fake clients; zero network or account effects."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion';s=(main/'VisitorFlightController.java').read_text()
def body(name):
 match=re.search(r' public [^\n{}]+ '+name+r'\(',s);assert match,name
 a=match.start();b=s.index('{',a)+1;depth=1
 while depth:depth+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b]
methods='\n'.join(body(n) for n in ['requestData','data','dispose','shutdownData'])
java='''package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.concurrent.*;
public class VisitorDataDemandTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final class Minecraft {boolean same=true;Object level=new Object();boolean isSameThread(){return same;}}
 Minecraft game=new Minecraft();Minecraft mc(){return game;}
 AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext context(){return ctx;}
 AutomationSession session=new AutomationSession(new AutomationCoordinator(),this::context,()->{},()->{});
 static class Flight{int calls;void dispose(AutomationSession.Handle h){calls++;}}Flight flight=new Flight();
 static class Items {Map<String,String> names(){return Map.of("Wheat","WHEAT");}}
 static class HypixelItemCatalogClient {static int created;boolean closed;CompletableFuture<Items> future=new CompletableFuture<>();HypixelItemCatalogClient(){created++;}CompletableFuture<Items> refresh(){return future;}Optional<Items> latest(){return !closed&&future.isDone()&&!future.isCompletedExceptionally()?Optional.of(future.join()):Optional.empty();}void close(){closed=true;future.cancel(true);}}
 static class HypixelMarketClient {static int created;boolean closed;CompletableFuture<BazaarMarket> future=new CompletableFuture<>();HypixelMarketClient(){created++;}CompletableFuture<BazaarMarket> refresh(){return future;}Optional<BazaarMarket> latest(){return !closed&&future.isDone()&&!future.isCompletedExceptionally()?Optional.of(future.join()):Optional.empty();}void close(){closed=true;future.cancel(true);}}
 HypixelMarketClient prices;HypixelItemCatalogClient catalog;CompletableFuture<?> priceRequest,catalogRequest;AutomationSession.Handle dataOwner;Object dataLevel;long requestedAt;boolean requested;
 METHODS
 static void denied(Runnable r){try{r.run();throw new AssertionError("expected request guard refusal");}catch(IllegalStateException expected){}}
 public static void main(String[]args){
  var h=new VisitorDataDemandTest();check(HypixelItemCatalogClient.created==0&&HypixelMarketClient.created==0&&h.data().isEmpty(),"construction/startup performs no network client creation");denied(()->h.requestData(null));
  var root=h.session.begin(AutomationFeature.VISITOR,System.nanoTime(),120_000_000_000L);h.game.same=false;denied(()->h.requestData(root));check(HypixelMarketClient.created==0,"off-thread cannot initialize data client");h.game.same=true;
  h.requestData(root);check(HypixelMarketClient.created==1&&HypixelItemCatalogClient.created==1&&h.data().isEmpty(),"one live Visitor root creates only public data clients and waits");denied(()->h.requestData(root));
  h.catalog.future.complete(new Items());h.prices.future.complete(VisitorValuationTest.market(1000000));check(h.data().isPresent(),"completed immutable public data visible only to current root");var level=h.game.level;h.game.level=new Object();check(h.data().isEmpty(),"late data cannot cross a native level identity");h.game.level=level;
  var prices=h.prices;var catalog=h.catalog;h.dispose(new AutomationSession.Handle(root.lease(),root.hold(),root.generation()+1));check(h.dataOwner==root,"stale handle cannot discard current task data");
  h.session.invalidate();check(h.data().isEmpty(),"late completion grants no revoked root access");h.dispose(root);check(h.dataOwner==null&&!prices.closed&&!catalog.closed,"completed cache can remain without background request/gameplay");
  h.shutdownData();check(prices.closed&&catalog.closed&&h.prices==null&&h.catalog==null,"world/client shutdown closes public clients and drops references");
  var pending=new VisitorDataDemandTest();var p=pending.session.begin(AutomationFeature.VISITOR,System.nanoTime(),120_000_000_000L);pending.requestData(p);var pp=pending.prices;var cc=pending.catalog;pending.dispose(p);check(pp.closed&&cc.closed&&pp.future.isCancelled()&&cc.future.isCancelled()&&pending.data().isEmpty(),"cancellation closes pending real-client boundaries, not just a detached promise");
  var wrong=new VisitorDataDemandTest();var other=wrong.session.begin(AutomationFeature.PEST,System.nanoTime(),120_000_000_000L);denied(()->wrong.requestData(other));check(wrong.dataOwner==null,"no old Farming/Pest caller can start Visitor lookup");
  System.out.println("VisitorDataDemandTest: PASS (actual request/data/disposal bodies, no startup/offthread/foreign lease fetching, immutable current-world/root data, pending cancellation and shutdown)");
 }
}'''.replace('METHODS',methods)
prism=Path.home()/'Library/Application Support/PrismLauncher';jdk=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=prism/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory() as temp:
 p=Path(temp);(p/'VisitorDataDemandTest.java').write_text(java)
 sources=list((main/'automation').rglob('*.java'))+list((root/'src/test/java/dev/kuan/kuanperion/automation/visitor').glob('*.java'))
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',temp,*map(str,sources),str(p/'VisitorDataDemandTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp+':'+str(gson),'dev.kuan.kuanperion.automation.visitor.VisitorDataDemandTest'],check=True)
# Construction is inside the guarded demand method only, no background refresh or gameplay callback.
request=body('requestData')
for name in ['HypixelMarketClient','HypixelItemCatalogClient']:
 assert s.count('new '+name+'()')==1 and 'new '+name+'()' in request
for forbidden in ['whenComplete(','.thenAccept(','.sendCommand(','.handleContainerInput(','Thread.sleep']:
 assert forbidden not in methods,forbidden
print('VisitorDataDemandWiringTest: PASS (only exact current Visitor demand constructs anonymous clients; callbacks never invoke gameplay)')
