#!/usr/bin/env python3
"""Actual typed sender body + fixed service/lifecycle/UI constructor route; no game/network actions."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion';bridge=(main/'AutomationClientBridge.java').read_text()
def body(name):
 m=re.search(r' (?:public|private) [^{}\n()]+ '+name+r'\(',bridge);assert m,name
 a=m.start();b=bridge.index('{',a)+1;d=1
 while d:d+=(bridge[b]=='{')-(bridge[b]=='}');b+=1
 return bridge[a:b]
methods=body('sendSupercraftRecipe')+'\n'+body('onOutgoingCommand')
java='''package dev.kuan.kuanperion;
import java.util.*;import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.supercraft.*;
public final class SupercraftServiceWiringTest {
 boolean ownGameplayCommand,observedGameplayCommand;AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext observe(){return ctx;}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::observe,()->{},()->{});List<StashService.Notice> notices=new ArrayList<>();java.util.function.Consumer<StashService.Notice> feedback=notices::add;
 static class Service{int cancellations;void cancel(long n){cancellations++;}void onOutgoingCommand(){}}
 Service stash=new Service(),pest=new Service(),greenhouse=new Service(),composter=new Service(),visitor=new Service(),supercraft=new Service(),autoSell=new Service(),junk=new Service();
 static class Minecraft{static Minecraft instance=new Minecraft();static Minecraft getInstance(){return instance;}Object level=new Object();Player player=new Player();Gui gui=new Gui();boolean same=true;boolean isSameThread(){return same;}}
 static class Gui{Object screen;Object screen(){return screen;}}static class Player{Connection connection=new Connection();}
 static SupercraftServiceWiringTest active;static class Connection{List<String> commands=new ArrayList<>();boolean observe=true,fail;void sendCommand(String value){commands.add(value);if(fail)throw new IllegalStateException("native send uncertain");if(observe)active.onOutgoingCommand();}}
 METHODS
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static SupercraftPlan.Recipe recipe(){return new SupercraftPlan.Recipe("ENCHANTED_WHEAT",1,64,Map.of("WHEAT",160));}
 public static void main(String[]args){
  var h=new SupercraftServiceWiringTest();active=h;var game=Minecraft.getInstance();var handle=h.session.begin(AutomationFeature.SUPERCRAFT,System.nanoTime(),90_000_000_000L);check(handle!=null,"leased root");
  check(h.sendSupercraftRecipe(handle,recipe(),System.nanoTime()),"observed exact typed recipe sender");check(game.player.connection.commands.equals(List.of("recipe ENCHANTED_WHEAT"))&&h.notices.size()==1&&h.session.isCurrent(handle)&&h.supercraft.cancellations==0,"own send acknowledgement does not cancel root; one feedback only");
  check(!h.ownGameplayCommand,"trusted flag cleared");game.gui.screen=new Object();check(!h.sendSupercraftRecipe(handle,recipe(),System.nanoTime()),"any currently open screen blocks sender, including own settings");game.gui.screen=null;
  game.same=false;check(!h.sendSupercraftRecipe(handle,recipe(),System.nanoTime()),"off-thread sender has no effect");game.same=true;
  for(var blocker:AutomationContext.Blocker.values()){h.ctx=new AutomationContext(1,true,true,Set.of(blocker));check(!h.sendSupercraftRecipe(handle,recipe(),System.nanoTime()),"all blockers retained: "+blocker);}h.ctx=new AutomationContext(1,true,true,Set.of());
  game.player.connection.observe=false;check(!h.sendSupercraftRecipe(handle,recipe(),System.nanoTime())&&h.notices.size()==1&&!h.ownGameplayCommand,"unobserved command cannot be reported as sent");game.player.connection.observe=true;
  h.onOutgoingCommand();check(!h.session.active()&&h.supercraft.cancellations==1,"real manual command immediately cancels active/pending service");
  var foreign=h.session.begin(AutomationFeature.AUTO_SELL,System.nanoTime(),90_000_000_000L);int before=game.player.connection.commands.size();check(!h.sendSupercraftRecipe(foreign,recipe(),System.nanoTime())&&game.player.connection.commands.size()==before,"foreign lease cannot borrow recipe command");h.session.cancel(System.nanoTime());
  handle=h.session.begin(AutomationFeature.SUPERCRAFT,System.nanoTime(),90_000_000_000L);game.player.connection.fail=true;try{h.sendSupercraftRecipe(handle,recipe(),System.nanoTime());throw new AssertionError("expected native failure");}catch(IllegalStateException expected){}check(!h.ownGameplayCommand&&h.notices.size()==1,"failed send cannot leak trusted authority or announce success");
  System.out.println("SupercraftServiceWiringTest: PASS (actual typed sender/event bodies, exact current lease/thread/screen/context/observed dispatch, no generic command, manual takeover)");
 }
}'''.replace('METHODS',methods)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-supercraft-service-') as temp:
 p=Path(temp);(p/'SupercraftServiceWiringTest.java').write_text(java)
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',temp,*map(str,(main/'automation').rglob('*.java')),str(p/'SupercraftServiceWiringTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp+':'+str(gson),'dev.kuan.kuanperion.SupercraftServiceWiringTest'],check=True)
for name in ['onManualInventoryInput','onOutgoingCommand','onManualLook','invalidateFarmSession','worldChanged']:
 assert 'supercraft.cancel(' in body(name),name
assert 'supercraft.suspend(now)' in body('suspendForJerry')
assert 'supercraft.tick(now)' in body('tick') and 'supercraftMenu.shutdownData()' in body('worldChanged') and 'supercraftMenu.shutdownData()' in body('shutdown')
assert 'supercraft.cancel(' in body('shutdown')
assert bridge.count('new SupercraftMenuController(')==1 and bridge.count('new SupercraftService(')==1
assert 'this::sendSupercraftRecipe' in bridge and 'public SupercraftService supercraft()' in bridge
client=(main/'KuanperionClient.java').read_text();assert 'List.of(jerryTab,webhookTab,dev.kuan.kuanperion.ui.SupercraftModuleSettings.tab(automation.supercraft()))' in client
ui=(main/'ui/SupercraftModuleSettings.java').read_text()
for forbidden in ['sendCommand(','sendSupercraftRecipe(','.craft(','HypixelItemCatalogClient','Minecraft.getInstance()']:assert forbidden not in ui,forbidden
assert 'service.manualRequest(System.nanoTime())' in ui and '關閉選單後執行一批' in ui
service=(main/'automation/supercraft/SupercraftService.java').read_text()
assert 'now-pendingAt>=30*SECOND' in service and 'pending=false;request(true,now)' in service
print('SupercraftLifecycleWiringTest: PASS (single fixed-owner service/Other UI, deferred expiring requests, exact task/native guards, manual/world/shutdown/Jerry cancellation; no purchase/maximize or live compatibility claim)')
