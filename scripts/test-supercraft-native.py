#!/usr/bin/env python3
"""Actual native method bodies against real ownership/menu/receipt contracts; zero network or game effects."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion';source=(main/'SupercraftMenuController.java').read_text();fixture=(root/'src/smoke/java/dev/kuan/kuanperion/SupercraftMenuControllerTest.java').read_text()
def extract(s,name):
 m=re.search(r'\b(?:public|private|static)\s+(?:static\s+)?[\w<>,.?]+\s+'+name+r'\(',s);assert m,name
 a=m.start();b=s.index('{',a)+1;d=1
 while d:d+=(s[b]=='{')-(s[b]=='}');b+=1
 return s[a:b]
methods='\n'.join(extract(source,n) for n in ['context','valid','owns','requestCatalog','catalog','recipe','craft','freeGrid','closeOwnedMenu','dispose','shutdownData'])
helpers='\n'.join(extract(fixture,n) for n in ['item','plan','menu','preview','replace']).replace('plan()','fixturePlan()').replace('menu()','fixtureMenu()').replace('preview(','fixturePreview(').replace('replace(','fixtureReplace(')
constants='\n'.join(l.strip() for l in fixture.splitlines() if l.strip().startswith(('static final ItemSnapshot AIR=','static final Map<String,String> IDS=')))+'\n'+'\n'.join(l.strip() for l in source.splitlines() if l.strip().startswith('private static final Pattern '))
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.supercraft.*;import java.util.*;import java.util.function.*;import java.util.concurrent.*;import java.util.regex.Pattern;
public final class SupercraftNativeBoundaryTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}static void denied(Runnable r){try{r.run();throw new AssertionError("expected denial");}catch(IllegalStateException expected){}}
 CONSTANTS
 HELPERS
 AutomationContext ctx=new AutomationContext(1,false,false,Set.of());Supplier<AutomationContext> observe=()->ctx;AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::context,()->{},()->{});InventoryReceipts receipts=new InventoryReceipts();
 class Minecraft {boolean same=true;Object level=new Object(),gameMode=new Object();Player player=new Player();boolean isSameThread(){return same;}}
 class Player {Menu containerMenu=new Menu();boolean spectator,passenger,using,sleeping;boolean isSpectator(){return spectator;}boolean isPassenger(){return passenger;}boolean isUsingItem(){return using;}boolean isSleeping(){return sleeping;}void closeContainer(){closes++;if(closeWorks){current=null;ctx=new AutomationContext(ctx.worldId(),false,false,Set.of());}}}
 class Menu {Carried getCarried(){return new Carried();}Slot getSlot(int i){return new Slot();}}class Carried{boolean isEmpty(){return cursorEmpty;}}class Slot{Stack getItem(){return new Stack();}}class Stack{int getMaxStackSize(){return stackLimit;}}
 Minecraft game=new Minecraft();Minecraft mc(){return game;}MenuSnapshot current;Map<Integer,ItemSnapshot> inv=new HashMap<>(fixturePlan().baseline());MenuSnapshot menu(){ACTIVE=this;return current;}Map<Integer,ItemSnapshot> inventory(){return inv;}boolean manual;boolean manualInput(){return manual;}
 int commands,nativeClicks,closes,stackLimit=64;boolean commandAccepted=true,nativeAccepted=true,nativeThrows,cursorEmpty=true,closeWorks=true;
 interface RecipeSender{boolean send(AutomationSession.Handle h,SupercraftPlan.Recipe recipe,long now);}RecipeSender sender=(h,r,n)->{commands++;return commandAccepted;};
 static class SkyblockItemCatalog{Map<String,String> names(){return IDS;}}static class HypixelItemCatalogClient{static int created;boolean closed;CompletableFuture<SkyblockItemCatalog> future=new CompletableFuture<>();HypixelItemCatalogClient(){created++;}CompletableFuture<SkyblockItemCatalog> refresh(){return future;}Optional<SkyblockItemCatalog> latest(){return !closed&&future.isDone()&&!future.isCompletedExceptionally()?Optional.of(future.join()):Optional.empty();}void close(){closed=true;future.cancel(true);}}
 HypixelItemCatalogClient catalogClient;CompletableFuture<?> catalogRequest;AutomationSession.Handle dataOwner,h;Object dataLevel;InventoryReceipts.Stamp openStamp;SupercraftPlan.Recipe activeRecipe;String expectedTitle;int boundContainer=-1;boolean requested,opened,attempted,sent;long requestedAt;
 static SupercraftNativeBoundaryTest ACTIVE;
 static class AutomationMenuActions {static GuardedMenuAction.Result purchaseClick(AutomationCoordinator c,AutomationCoordinator.Lease l,MenuContract contract,MenuContract.Ticket ticket,Supplier<AutomationContext> context,Supplier<MenuSnapshot> menus,long now){return GuardedMenuAction.perform(c,l,contract,ticket,GuardedMenuAction.Space.REQUIRE_FREE_SLOT,context,menus,t->{ACTIVE.nativeClicks++;if(ACTIVE.nativeThrows)throw new IllegalStateException("possibly sent");return ACTIVE.nativeAccepted;},now);}}
 METHODS
 void acquire(AutomationFeature f){h=session.begin(f,System.nanoTime(),90_000_000_000L);check(h!=null,"fixture current root");}
 void data(){acquire(AutomationFeature.SUPERCRAFT);requestCatalog(h);catalogClient.future.complete(new SkyblockItemCatalog());}
 void command(){session.expectMenu(h,new MenuContract(Set.of("Enchanted Wheat Recipe"),54),this::menu);check(recipe(h,fixturePlan().recipe(),System.nanoTime()),"one typed recipe");}
 void show(boolean record){current=fixtureMenu();ctx=new AutomationContext(1,false,false,Set.of(AutomationContext.Blocker.MENU));if(record)record(current);session.poll(System.nanoTime());}
 void record(MenuSnapshot m){for(int i=0;i<54;i++)receipts.observe(m.worldId(),m.containerId(),i,m.item(i));}
 void ready(boolean record){data();command();show(record);}GuardedMenuAction.Result attempt(){return craft(h,fixturePreview(current),System.nanoTime());}
 public static void main(String[]args){
  int count=HypixelItemCatalogClient.created;var f=new SupercraftNativeBoundaryTest();check(f.catalog().isEmpty()&&HypixelItemCatalogClient.created==count,"no startup data fetching");denied(()->f.requestCatalog(null));f.acquire(AutomationFeature.SUPERCRAFT);f.game.same=false;denied(()->f.requestCatalog(f.h));check(HypixelItemCatalogClient.created==count,"off-thread current root cannot fetch");f.game.same=true;f.requestCatalog(f.h);check(f.catalog().isEmpty()&&HypixelItemCatalogClient.created==count+1,"one root creates a demand-only pending public client");denied(()->f.requestCatalog(f.h));var client=f.catalogClient;f.dispose(f.h);check(client.closed&&client.future.isCancelled()&&f.dataOwner==null,"pending cancellation closes client and real request boundary");
  var d=new SupercraftNativeBoundaryTest();d.data();check(d.catalog().isPresent(),"current completed public data available");Object level=d.game.level;d.game.level=new Object();check(d.catalog().isEmpty(),"native level identity fence");d.game.level=level;d.session.invalidate();check(d.catalog().isEmpty(),"revoked root cannot borrow completed data");var done=d.catalogClient;d.dispose(d.h);check(!done.closed,"completed public cache may remain without background work");d.shutdownData();check(done.closed&&d.catalogClient==null,"world/shutdown closes cached client");
  var other=new SupercraftNativeBoundaryTest();other.acquire(AutomationFeature.AUTO_SELL);denied(()->other.requestCatalog(other.h));check(other.catalogClient==null,"other SCREEN lease cannot start Supercraft data");
  var good=new SupercraftNativeBoundaryTest();good.ready(true);good.closeOwnedMenu(7);check(good.closes==0,"no close before an issued craft");check(good.attempt()==GuardedMenuAction.Result.SENT&&good.nativeClicks==1&&good.sent,"actual native method allows exactly one freshly server-observed complete free recipe");check(good.attempt()==GuardedMenuAction.Result.REJECTED&&good.nativeClicks==1,"native one-attempt fence");good.closeOwnedMenu(8);check(good.closes==0,"wrong close ID");good.cursorEmpty=false;good.closeOwnedMenu(7);check(good.closes==0,"no cursor repair/close with a held item");good.cursorEmpty=true;good.ctx=new AutomationContext(1,false,false,Set.of(AutomationContext.Blocker.MENU,AutomationContext.Blocker.FOCUS));good.closeOwnedMenu(7);check(good.closes==0,"owned close cannot mask focus loss");good.ctx=new AutomationContext(1,false,false,Set.of(AutomationContext.Blocker.MENU));good.closeOwnedMenu(7);check(good.closes==1&&good.current==null,"exact owned empty-cursor close only");
  var predicted=new SupercraftNativeBoundaryTest();predicted.ready(false);check(predicted.attempt()==GuardedMenuAction.Result.REJECTED&&predicted.nativeClicks==0,"predicted menu alone not sufficient");predicted.receipts.observe(1,7,32,predicted.current.item(32));predicted.receipts.observe(1,7,24,predicted.current.item(24));check(predicted.attempt()==GuardedMenuAction.Result.REJECTED,"role/result packets alone cannot hide unknown empty-slot state");
  var hidden=new SupercraftNativeBoundaryTest();hidden.data();hidden.command();var paid=fixtureReplace(22,item("","minecraft:paper","Additional Fee",1,List.of("2 Coins")));hidden.record(paid);hidden.show(false);check(hidden.attempt()==GuardedMenuAction.Result.REJECTED&&hidden.nativeClicks==0,"local removal of a server fee item cannot hide it");
  var old=new SupercraftNativeBoundaryTest();old.data();old.record(fixtureMenu());old.command();old.show(false);check(old.attempt()==GuardedMenuAction.Result.REJECTED,"pre-recipe server packets cannot prove this menu");
  for(var b:AutomationContext.Blocker.values())if(b!=AutomationContext.Blocker.MENU){var x=new SupercraftNativeBoundaryTest();x.ready(true);x.ctx=new AutomationContext(1,false,false,Set.of(AutomationContext.Blocker.MENU,b));check(x.attempt()==GuardedMenuAction.Result.REJECTED&&x.nativeClicks==0,"all native blockers retained: "+b);}
  var manual=new SupercraftNativeBoundaryTest();manual.ready(true);manual.manual=true;check(manual.attempt()==GuardedMenuAction.Result.REJECTED&&manual.nativeClicks==0,"physical takeover");
  var gear=new SupercraftNativeBoundaryTest();gear.ready(true);gear.inv.put(42,item("WHEAT","minecraft:wheat","Wheat",1,List.of()));check(gear.attempt()==GuardedMenuAction.Result.REJECTED&&gear.nativeClicks==0,"native full43 revalidation includes body/saddle/equipment");
  var world=new SupercraftNativeBoundaryTest();world.ready(true);world.ctx=new AutomationContext(2,false,false,Set.of(AutomationContext.Blocker.MENU));check(world.attempt()==GuardedMenuAction.Result.REJECTED&&world.nativeClicks==0,"world scope");
  var ambiguous=new SupercraftNativeBoundaryTest();ambiguous.ready(true);ambiguous.nativeThrows=true;check(ambiguous.attempt()==GuardedMenuAction.Result.UNCERTAIN&&ambiguous.nativeClicks==1&&!ambiguous.sent,"native exception is ambiguous, not refundable/retryable");ambiguous.nativeThrows=false;check(ambiguous.attempt()==GuardedMenuAction.Result.REJECTED&&ambiguous.nativeClicks==1,"ambiguous attempts never replay");ambiguous.closeOwnedMenu(7);check(ambiguous.closes==0,"ambiguous result cannot close as a successful craft");
  var stale=new SupercraftNativeBoundaryTest();stale.ready(true);var previous=stale.h;stale.session.invalidate();stale.current=null;stale.ctx=new AutomationContext(1,false,false,Set.of());stale.h=stale.session.begin(AutomationFeature.SUPERCRAFT,System.nanoTime(),90_000_000_000L);stale.dataOwner=stale.h;stale.dispose(previous);check(stale.dataOwner==stale.h&&!stale.catalogClient.closed,"stale cleanup cannot discard a new owner's data");
  System.out.println("SupercraftNativeBoundaryTest: PASS (actual native method bodies + real leases/menu guard/receipts; no startup/foreign/thread/world fetching, complete fresh54 plus full43/source/cost gates, one attempt/uncertainty, scoped close/disposal; zero network/game effects)");
 }
}'''.replace('CONSTANTS',constants).replace('HELPERS',helpers).replace('METHODS',methods)
prism=Path.home()/'Library/Application Support/PrismLauncher';jdk=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=prism/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-supercraft-boundary-') as tmp:
 p=Path(tmp);(p/'SupercraftNativeBoundaryTest.java').write_text(java);files=list((root/'src/main/java/dev/kuan/kuanperion/automation').rglob('*.java'))
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',tmp,*map(str,files),str(p/'SupercraftNativeBoundaryTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.SupercraftNativeBoundaryTest'],check=True)

# The adapter is dormant: no unleased caller, startup fetch, generic command or raw native click path.
request=extract(source,'requestCatalog')
assert source.count('new HypixelItemCatalogClient()')==1 and 'new HypixelItemCatalogClient()' in request
for forbidden in ['.sendCommand(','.handleContainerInput(','.setDown(','.setPos(','.setSelectedSlot(','.thenAccept(','.whenComplete(','Thread.sleep','QUICK_MOVE']:
 assert forbidden not in source,forbidden
assert 'for(int slot=0;slot<54;slot++)' in extract(source,'craft')
assert 'AutomationMenuActions.purchaseClick(' in extract(source,'craft')
for p in main.rglob('*.java'):
 if p.name!='SupercraftMenuController.java':assert 'new SupercraftMenuController(' not in p.read_text(),p
print('SupercraftNativeWiringTest: PASS (dormant adapter, single guarded native click and typed recipe callback; demand-only catalog, no startup caller/action or unverified feature toggle)')
