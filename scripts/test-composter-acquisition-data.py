#!/usr/bin/env python3
"""Execute actual Composter demand/disposal bodies with real parent leases, fake HTTP/native edges; no network/game."""
from pathlib import Path
import re,subprocess,tempfile
r=Path(__file__).resolve().parent.parent;s=(r/'src/main/java/dev/kuan/kuanperion/ComposterFlightController.java').read_text()
def method(name):
 m=re.search(r' (?:public|private) [^\n{}]+ '+name+r'\(',s);assert m,name
 a=m.start();b=s.index('{',a)+1;d=1
 while d:d+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b]
methods='\n'.join(method(n) for n in ['beginAcquisition','cancelAcquisition','dispose','shutdownData'])
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.composter.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
public class ComposterAcquisitionDataTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static class Minecraft {boolean same=true;Object level=new Object();boolean isSameThread(){return same;}}
 Minecraft game=new Minecraft();Minecraft mc(){return game;}
 AutomationContext context=new AutomationContext(1,true,true,Set.of());AutomationContext context(){return context;}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::context,()->{},()->{});
 static class BazaarAcquisitionController{boolean cancelled;void cancel(){cancelled=true;}}
 class Flight{AutomationSession.Handle owner;boolean failDispose;Object levelIdentity(){return game.level;}AutomationSession.Handle owner(){return owner;}boolean allowed(AutomationSession.Handle h,long now,AutomationCoordinator.Resource resource){return game.same&&session.isCurrent(h)&&coordinator.permits(h.lease(),resource,context,now);}void dispose(AutomationSession.Handle h){if(Objects.equals(h,owner)){owner=null;if(failDispose)throw new IllegalStateException("flight disposal");}}}
 Flight flight=new Flight();InventoryReceipts purchaseItems=new InventoryReceipts();CursorReceipts purchaseCursors=new CursorReceipts();Object purchaseOpener=new Object();java.util.function.LongSupplier purse=()->1000;
 MenuSnapshot menu(){return null;}
 ComposterTask.Frame frame(long now){var p=new PestTargets.Point(0,72,0);var item=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);return new ComposterTask.Frame(1,now,new FlightAnchor(p,0,0,0,item,true),p,true,false,true,true);}
 static class HypixelItemCatalogClient{static int made;int refreshes;boolean closed;HypixelItemCatalogClient(){made++;}void refresh(){refreshes++;}void close(){closed=true;}}
 static class HypixelMarketClient{static int made;int refreshes;boolean closed;HypixelMarketClient(){made++;}void refresh(){refreshes++;}void close(){closed=true;}}
 HypixelItemCatalogClient catalog;HypixelMarketClient prices;AutomationSession.Handle dataOwner,acquisitionOwner;Object dataLevel;long dataRequestedAt;List<BazaarAcquisitionPlan.Need> acquisitionNeeds=List.of();ComposterPolicy acquisitionPolicy;SpendingBudget acquisitionBudget;BazaarAcquisitionController acquisition;boolean acquisitionComplete;
 METHODS
 static ComposterPolicy policy(){return ComposterPolicy.defaults().withMaterials(Map.of("BOX_OF_SEEDS",new ComposterPolicy.Material(ComposterObservation.Kind.ORGANIC,25000))).withBuying(new ComposterBuying(Map.of("BOX_OF_SEEDS",new ComposterBuying.Stock(2,new BazaarAcquisitionPlan.Material("minecraft:paper",64,1000))),1000));}
 AutomationSession.Handle root(){var h=session.begin(AutomationFeature.COMPOSTER,0,240_000_000_000L);flight.owner=h;return h;}
 boolean begin(AutomationSession.Handle h){return beginAcquisition(h,policy(),Set.of(ComposterObservation.Kind.ORGANIC),new SpendingBudget(1000),1);}
 public static void main(String[]args){
  var t=new ComposterAcquisitionDataTest();check(HypixelItemCatalogClient.made==0&&HypixelMarketClient.made==0&&!t.begin(null),"no construction/startup/null-parent network");var h=t.root();t.game.same=false;check(!t.begin(h)&&t.catalog==null,"offthread demand denied before clients");t.game.same=true;
  check(t.begin(h)&&t.catalog.refreshes==1&&t.prices.refreshes==1&&t.dataOwner==h&&t.acquisitionNeeds.getFirst().targetCount()==2,"exact original root constructs only official data clients, explicit fixed stock target");check(!t.begin(h)&&t.catalog.refreshes==1,"no duplicate demand while acquisition pending");
  var c=t.catalog;var p=t.prices;t.dispose(new AutomationSession.Handle(h.lease(),h.hold(),h.generation()+1));check(t.dataOwner==h&&!c.closed,"stale disposal cannot affect current run");t.dispose(h);check(t.dataOwner==null&&t.acquisitionOwner==null&&c.closed&&p.closed,"native disposal cleans both owned sequence and transports, not just the HTTP fields");
  t=new ComposterAcquisitionDataTest();h=t.root();check(t.begin(h),"next original root");t.flight.failDispose=true;c=t.catalog;p=t.prices;try{t.dispose(h);throw new AssertionError("expected physical disposal failure");}catch(IllegalStateException expected){}check(t.dataOwner==null&&t.acquisitionOwner==null&&c.closed&&p.closed,"physical disposal failure still cancels acquisition and both transports");
  System.out.println("ComposterAcquisitionDataTest: PASS actual native demand/disposal bodies, real root and explicit fixed stock; controlled HTTP/native edges only");
 }
}'''.replace('METHODS',methods)
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-composter-data-') as tmp:
 f=Path(tmp)/'ComposterAcquisitionDataTest.java';f.write_text(java)
 sources=list((r/'src/main/java/dev/kuan/kuanperion/automation').rglob('*.java'))
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',tmp,*map(str,sources),str(f)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.ComposterAcquisitionDataTest'],check=True)
for name in ['HypixelMarketClient','HypixelItemCatalogClient']:assert s.count('new '+name+'()')==1 and 'new '+name+'()' in method('beginAcquisition')
for forbidden in ['whenComplete(','.thenAccept(','.sendCommand(','.handleContainerInput(','Thread.sleep(']:assert forbidden not in s,forbidden
print('ComposterAcquisitionDataWiring: PASS demand-only fixed official clients and no callback gameplay/raw commands/clicks')
