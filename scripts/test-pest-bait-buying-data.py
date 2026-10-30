#!/usr/bin/env python3
"""Actual native demand/disposal bodies with real original Pest leases and fake HTTP edges; no network/game."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent
s=(root/'src/main/java/dev/kuan/kuanperion/PestFlightController.java').read_text()
def method(name):
 m=re.search(r' (?:public|private) [^\n{}]+ '+name+r'\(',s);assert m,name
 a=m.start();b=s.index('{',a)+1;depth=1
 while depth:depth+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b]
methods='\n'.join(method(n) for n in ['beginBaitAcquisition','cancelBaitAcquisition','dispose','shutdownBaitData'])
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
public class PestBaitPurchaseDataTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static class Minecraft{boolean same=true;Object level=new Object();boolean isSameThread(){return same;}}Minecraft game=new Minecraft();Minecraft mc(){return game;}
 AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext context(){return ctx;}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::context,()->{},()->{});
 class Flight{AutomationSession.Handle owner;boolean fail;AutomationSession.Handle owner(){return owner;}Object levelIdentity(){return game.level;}boolean allowed(AutomationSession.Handle h,long now,AutomationCoordinator.Resource r){return game.same&&session.isCurrent(h)&&coordinator.permits(h.lease(),r,ctx,now);}void dispose(AutomationSession.Handle h){if(Objects.equals(h,owner)){owner=null;if(fail)throw new IllegalStateException("physical cleanup");}}}Flight flight=new Flight();Object latestFacts,preparedTool;
 static class Barn{void dispose(AutomationSession.Handle h){}}Barn barn=new Barn(); // Barn native capability disposal is separately exercised by test-pest-barn-native.py.
 static class HypixelItemCatalogClient{static int made;int refreshes;boolean closed;HypixelItemCatalogClient(){made++;}void refresh(){refreshes++;}void close(){closed=true;}}
 static class HypixelMarketClient{static int made;int refreshes;boolean closed;HypixelMarketClient(){made++;}void refresh(){refreshes++;}void close(){closed=true;}}
 static class BazaarAcquisitionController{boolean cancelled;void cancel(){cancelled=true;}}
 InventoryReceipts baitItems=new InventoryReceipts();CursorReceipts baitCursors=new CursorReceipts();Object baitOpener=new Object();java.util.function.LongSupplier baitPurse=()->1000;
 HypixelItemCatalogClient baitCatalog;HypixelMarketClient baitPrices;AutomationSession.Handle baitDataOwner,baitBuyingOwner;Object baitDataLevel;long baitDataRequestedAt;PestBaitPolicy baitBuyingPolicy;SpendingBudget baitBuyingBudget;int baitBuyingRemaining;BazaarAcquisitionController baitAcquisition;
 PestBaitButtonTransfer.Frame baitFrame(long n){var inv=new HashMap<Integer,ItemSnapshot>();var air=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);for(int i=0;i<43;i++)inv.put(i,air);return new PestBaitButtonTransfer.Frame(1,n,game.level,null,null,inv,air,new PestTrapObservation(Set.of(),true,Set.of(1),true),List.of());}
 METHODS
 static PestBaitPolicy policy(){return new PestBaitPolicy(true,new PestBaitButtonPlan.Rules("TASTY_CHEESE",16,64,Set.of()),new PestBaitBuying("minecraft:paper",64,16,1000));}
 AutomationSession.Handle root(){var h=session.begin(AutomationFeature.PEST,0,240_000_000_000L);flight.owner=h;return h;}
 boolean begin(AutomationSession.Handle h){return beginBaitAcquisition(h,policy(),new SpendingBudget(1000),64,1);}
 public static void main(String[]args){
  var t=new PestBaitPurchaseDataTest();check(HypixelItemCatalogClient.made==0&&!t.begin(null)&&t.baitCatalog==null,"no startup/null-parent fetching");var h=t.root();t.game.same=false;check(!t.begin(h)&&t.baitCatalog==null,"offthread request denied before HTTP construction");t.game.same=true;
  check(!t.beginBaitAcquisition(h,policy(),new SpendingBudget(1001),64,1)&&t.baitCatalog==null,"cannot replace original module cap with a larger budget");
  check(!t.beginBaitAcquisition(h,policy(),new SpendingBudget(1000),8,1)&&t.baitCatalog==null,"configured purchase must fit remaining original withdrawal ceiling");
  check(t.begin(h)&&t.baitDataOwner==h&&t.baitBuyingOwner==h&&t.baitCatalog.refreshes==1&&t.baitPrices.refreshes==1,"explicit original-parent shortage alone requests both official data sources");check(!t.begin(h)&&t.baitCatalog.refreshes==1,"no duplicate in-flight demand");
  var c=t.baitCatalog;var p=t.baitPrices;t.dispose(new AutomationSession.Handle(h.lease(),h.hold(),h.generation()+1));check(t.baitDataOwner==h&&!c.closed,"stale disposal cannot cancel a current buyer");t.dispose(h);check(t.baitDataOwner==null&&t.baitBuyingOwner==null&&c.closed&&p.closed,"actual root disposal closes sequence and both owned transports");
  t=new PestBaitPurchaseDataTest();h=t.root();check(t.begin(h),"cleanup exception fixture");c=t.baitCatalog;p=t.baitPrices;t.flight.fail=true;try{t.dispose(h);throw new AssertionError("expected physical cleanup error");}catch(IllegalStateException expected){}check(c.closed&&p.closed&&t.baitDataOwner==null&&t.baitBuyingOwner==null,"physical cleanup failure cannot leak HTTP or original acquisition");
  System.out.println("PestBaitPurchaseDataTest: PASS actual demand/disposal bodies + real leases, controlled HTTP edges");
 }
}'''.replace('METHODS',methods)
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-bait-demand-') as d:
 f=Path(d)/'PestBaitPurchaseDataTest.java';f.write_text(java)
 try:subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-sourcepath',str(root/'src/main/java'),'-d',d,str(f)],check=True,capture_output=True,text=True,timeout=90)
 except subprocess.CalledProcessError as e:print(e.stderr);raise
 subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.PestBaitPurchaseDataTest'],check=True)
for name in ['HypixelItemCatalogClient','HypixelMarketClient']:assert s.count('new '+name+'()')==1 and 'new '+name+'()' in method('beginBaitAcquisition')
assert '.thenAccept(' not in s and '.whenComplete(' not in s,'No asynchronous gameplay callbacks'
bridge=(root/'src/main/java/dev/kuan/kuanperion/AutomationClientBridge.java').read_text()
assert 'pest.onSystemMessage(currentWorldId(),text)' in bridge,'Only native GAME routing can feed the purchase result'
print('PestBaitPurchaseDataWiring: PASS demand-only fixed official clients and original-root GAME result path')
