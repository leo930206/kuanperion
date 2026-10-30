#!/usr/bin/env python3
"""Actual spray buyer demand/poll/disposal bodies, real plans/root/budget; controlled physical/HTTP/child edges.
The child navigation, terminal and receipt ledgers are separately exercised by BazaarAcquisitionControllerTest.
"""
from pathlib import Path
import subprocess,tempfile,re
r=Path(__file__).resolve().parents[1]
s=(r/'src/main/java/dev/kuan/kuanperion/PestSprayController.java').read_text()
def method(name):
 m=re.search(r' (?:public|private) [^\n{}]+ '+name+r'\(',s);assert m,name
 a=m.start();b=s.index('{',a)+1;depth=1
 while depth:depth+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b]
java=r'''package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.math.BigDecimal;import java.util.concurrent.CompletableFuture;import java.util.function.*;
public class PestSprayBuyingNativeTest {
 static final long NOW=1_800_000_000_000L;static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static class Minecraft{static Minecraft instance;boolean thread=true;Object level,player;boolean isSameThread(){return thread;}static Minecraft getInstance(){return instance;}}
 static class VisitorGameView{static Map<Integer,ItemSnapshot> inventory(Minecraft mc){return current.inv;}}
 static PestSprayTaskTest.Fixture current;
 static class HypixelItemCatalogClient{SkyblockItemCatalog value;boolean closed,fail;Optional<SkyblockItemCatalog> latest(){return Optional.ofNullable(value);}void close(){closed=true;if(fail)throw new IllegalStateException("catalog close");}}
 static class HypixelMarketClient{static int made;int refreshes;boolean closed,fail;BazaarMarket value;HypixelMarketClient(){made++;}void refresh(){refreshes++;}Optional<BazaarMarket> latest(){return Optional.ofNullable(value);}void close(){closed=true;if(fail)throw new IllegalStateException("price close");}}
 static class BazaarAcquisitionController{
  enum State{PURCHASING,COMPLETE,FAILED,UNCERTAIN}State state=State.PURCHASING;boolean cancelled,fail;int ticks,messages;AutomationSession.Handle root;BazaarAcquisitionPlan.Plan plan;SpendingBudget cap;
  BazaarAcquisitionController(AutomationCoordinator c,AutomationSession s,Supplier<AutomationContext> o,InventoryReceipts i,CursorReceipts u,AutomationSession.Handle h,BazaarAcquisitionPlan.Plan p,SpendingBudget b,Object opener,LongSupplier purse){root=h;plan=p;cap=b;}
  boolean begin(long n){return plan.requests().getFirst().usesBudget(cap);}void tick(long n){ticks++;}State state(){return state;}boolean ownsScreen(){return state==State.PURCHASING;}void onSystemMessage(long w,String t){messages++;}void cancel(){cancelled=true;if(fail)throw new IllegalStateException("child cancel");}
 }
 static final class Native {
  AutomationCoordinator coordinator;AutomationSession session;Supplier<AutomationContext> observe;Supplier<PestSprayPolicy> policy=PestSprayPlanTest::policy;
  InventoryReceipts buyItems=new InventoryReceipts();CursorReceipts buyCursors=new CursorReceipts();Object buyOpener=new Object();LongSupplier buyPurse=()->1000;
  HypixelItemCatalogClient catalog;CompletableFuture<?> request;HypixelMarketClient buyPrices;BazaarAcquisitionController acquisition;AutomationSession.Handle dataOwner,buyOwner;Object dataLevel,dataPlayer;String materialName="Test Material";PestSprayPolicy buyPolicy;PestSprayBuying buying;SpendingBudget buyBudget;long buyStarted;PestSprayTask.Frame buyAnchor;
  AutomationContext context(){return current.context();}boolean manualInput(){return current.manualInput();}PestSprayTask.Frame sprayFrame(long n){return current.sprayFrame(n);}
  METHODS
 }
 static Native setup(){current=new PestSprayTaskTest.Fixture();current.inv=new HashMap<>(current.inv);current.inv.put(9,PestSprayPlanTest.AIR);var mc=new Minecraft();Minecraft.instance=mc;mc.level=current.level;mc.player=current.player;var t=new Native();t.coordinator=current.coordinator;t.session=current.session;t.observe=current::context;t.dataOwner=current.session.begin(AutomationFeature.PEST_SPRAY,current.now,120_000_000_000L);t.dataLevel=mc.level;t.dataPlayer=mc.player;t.catalog=new HypixelItemCatalogClient();return t;}
 static boolean begin(Native t){return t.beginSprayAcquisition(t.dataOwner,PestSprayPlanTest.policy(),new PestSprayBuying(16,1000),new SpendingBudget(1000),current.now);}
 static void data(Native t){t.catalog.value=SkyblockItemCatalog.parse(("{\"success\":true,\"lastUpdated\":"+NOW+",\"items\":[{\"id\":\"TEST_MATERIAL\",\"name\":\"Test Material\"}]}").getBytes(java.nio.charset.StandardCharsets.UTF_8),NOW);t.buyPrices.value=new BazaarMarket(System.currentTimeMillis(),Map.of("TEST_MATERIAL",new BazaarMarket.Book(List.of(new BazaarMarket.Level(100,BigDecimal.TEN)),List.of())));}
 public static void main(String[]args){
  for(String mode:List.of("thread","missing-native","manual","world","player","unarmed","foreign-budget","no-consent","already-stocked","protected-stock")){
   var t=setup();var h=t.dataOwner;var cap=new SpendingBudget(1000);var buy=new PestSprayBuying(16,1000);switch(mode){case "thread"->Minecraft.instance.thread=false;case "missing-native"->t.buyItems=null;case "manual"->current.manual=true;case "world"->Minecraft.instance.level=new Object();case "player"->Minecraft.instance.player=new Object();case "unarmed"->current.session.invalidate();case "foreign-budget"->cap=new SpendingBudget(1001);case "no-consent"->buy=PestSprayBuying.defaults();case "already-stocked"->current.inv.put(9,PestSprayPlanTest.material(12));case "protected-stock"->current.inv.put(0,PestSprayPlanTest.material(1));}
   int before=HypixelMarketClient.made;check(!t.beginSprayAcquisition(h,PestSprayPlanTest.policy(),buy,cap,current.now)&&HypixelMarketClient.made==before&&t.buyOwner==null,"no data/HTTP/child effect before independent valid demand: "+mode);t.shutdownData();
  }
  var t=setup();check(begin(t)&&t.buyOwner.equals(t.dataOwner)&&t.buyPrices.refreshes==1,"one original-root explicit Coins-bound price demand");var h=t.dataOwner;var cap=t.buyBudget;check(!begin(t)&&t.buyPrices.refreshes==1,"no duplicate demand or budget replacement");check(t.pollSprayAcquisition(h,current.now)==PestSprayTask.Acquisition.RUNNING&&t.acquisition==null,"wait for demanded data without menu/command");data(t);check(t.pollSprayAcquisition(h,current.now)==PestSprayTask.Acquisition.RUNNING&&t.acquisition.root.equals(h)&&t.acquisition.plan.requests().getFirst().quantity()==16&&t.acquisition.cap==cap&&cap.available()==1000,"exact single deficit keeps original budget; estimate never spends");var child=t.acquisition;t.sprayAcquisitionMessage(1,"[Bazaar] Bought");check(t.ownsSprayAcquisitionScreen()&&child.messages==1,"owned child screen/GAME bridge only");child.state=BazaarAcquisitionController.State.COMPLETE;check(t.pollSprayAcquisition(h,current.now)==PestSprayTask.Acquisition.COMPLETE&&t.session.isCurrent(h)&&t.session.holdsFarming(),"child completion cannot finish/rearm parent");
  var prices=t.buyPrices;t.disposeSprayData(new AutomationSession.Handle(h.lease(),h.hold(),h.generation()+1));check(t.buyOwner==h&&!prices.closed,"stale cleanup cannot revoke current buyer");t.disposeSprayData(h);check(child.cancelled&&prices.closed&&t.buyOwner==null&&t.buyAnchor==null&&t.dataOwner==null,"terminal disposal cancels child, closes prices, releases all world references");
  t=setup();check(begin(t),"cleanup fault setup");data(t);t.pollSprayAcquisition(t.dataOwner,current.now);child=t.acquisition;child.fail=true;prices=t.buyPrices;var catalog=t.catalog;catalog.fail=true;t.request=new CompletableFuture<>();try{t.disposeSprayData(t.dataOwner);throw new AssertionError("fault lost");}catch(IllegalStateException expected){}check(prices.closed&&catalog.closed&&t.buyOwner==null&&t.buyAnchor==null&&t.buyPrices==null&&t.catalog==null&&t.dataOwner==null,"all cleanup actions run even after child/transport errors");
  t=setup();check(begin(t),"timeout setup");check(t.pollSprayAcquisition(t.dataOwner,current.now+12_000_000_000L)==PestSprayTask.Acquisition.FAILED&&t.acquisition==null,"bounded data wait, no speculative order");t.shutdownData();
  for(String mode:List.of("manual","world","player","position","selected","receipt-epoch","receipt-sequence","foreign-screen")){t=setup();check(begin(t),"late setup");data(t);switch(mode){case "manual"->current.manual=true;case "world"->Minecraft.instance.level=new Object();case "player"->Minecraft.instance.player=new Object();case "position"->current.x++;case "selected"->current.selected=8;case "receipt-epoch"->current.positionEpoch++;case "receipt-sequence"->current.positionSequence++;case "foreign-screen"->current.context=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.FOCUS));}check(t.pollSprayAcquisition(t.dataOwner,current.now)==PestSprayTask.Acquisition.FAILED&&t.acquisition==null,"late physical/root proof blocks child creation: "+mode);t.shutdownData();}
  System.out.println("PestSprayBuyingNativeTest: PASS actual demand/poll/disposal bodies, real root/plan/cap, controlled native/HTTP/child edges; not server purchase proof");
 }
}'''.replace('METHODS','\n'.join(method(n) for n in ['beginSprayAcquisition','sprayAcquisitionCurrent','pollSprayAcquisition','ownsSprayAcquisitionScreen','sprayAcquisitionMessage','cancelSprayAcquisition','shutdownSprayBuying','disposeSprayData','shutdownData']))
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-spray-buying-native-') as d:
 f=Path(d)/'PestSprayBuyingNativeTest.java';f.write_text(java)
 result=subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-sourcepath',str(r/'src/main/java')+':'+str(r/'src/test/java'),'-d',d,str(f)],capture_output=True,text=True)
 assert result.returncode==0,result.stdout+result.stderr
 subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.automation.pest.PestSprayBuyingNativeTest'],check=True)
assert s.count('new HypixelMarketClient()')==1 and 'new HypixelMarketClient()' in method('beginSprayAcquisition'),'Only independently authorized shortage can create price HTTP'
assert not any(x in s for x in ['.thenAccept(','.whenComplete(','sendCommand(','Thread.sleep(','.setCarried(']),'No async gameplay/raw command/cursor repair'
print('PestSprayBuyingBoundaries: PASS independent demand-only original root, shared standard purchase path')
