#!/usr/bin/env python3
"""Execute the real typed George send/event bodies; game/connection boundaries are controlled, not live Hypixel."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion';bridge=(main/'AutomationClientBridge.java').read_text()
def body(name):
 m=re.search(r' (?:public|private) [^{}\n()]+ '+name+r'\(',bridge);assert m,name
 a=m.start();b=bridge.index('{',a)+1;d=1
 while d:d+=(bridge[b]=='{')-(bridge[b]=='}');b+=1
 return bridge[a:b]
methods=body('sendPetGeorge')+'\n'+body('onOutgoingCommand')
java='''package dev.kuan.kuanperion;
import java.util.*;import dev.kuan.kuanperion.automation.*;
public final class PetGeorgeWiringTest {
 boolean ownGameplayCommand,observedGameplayCommand;AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext observe(){return ctx;}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::observe,()->{},()->{});List<StashService.Notice> notices=new ArrayList<>();java.util.function.Consumer<StashService.Notice> feedback=notices::add;
 static class Service{int cancellations;void cancel(long n){cancellations++;}void onOutgoingCommand(){}}
 Service stash=new Service(),pest=new Service(),spray=new Service(),greenhouse=new Service(),composter=new Service(),visitor=new Service(),supercraft=new Service(),book=new Service(),pets=new Service(),autoSell=new Service(),junk=new Service();
 static class Menu{Object carried;Object getCarried(){return carried;}}
 static class Minecraft{static Minecraft instance=new Minecraft();static Minecraft getInstance(){return instance;}Object level=new Object();Player player=new Player();Gui gui=new Gui();boolean same=true;boolean isSameThread(){return same;}}
 static class Gui{Object screen,overlay;Object screen(){return screen;}Object overlay(){return overlay;}}static class Stack{boolean empty=true;boolean isEmpty(){return empty;}}static class Container{Stack carried=new Stack();Stack getCarried(){return carried;}}static class Player{Connection connection=new Connection();Container inventoryMenu=new Container(),containerMenu=inventoryMenu;}
 static PetGeorgeWiringTest active;static class Connection{List<String> commands=new ArrayList<>();boolean observe=true,fail;void sendCommand(String value){commands.add(value);if(fail)throw new IllegalStateException("native send uncertain");if(observe)active.onOutgoingCommand();}}
 METHODS
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args){
  var h=new PetGeorgeWiringTest();active=h;var game=Minecraft.getInstance();var handle=h.session.begin(AutomationFeature.PET_SELL,System.nanoTime(),90_000_000_000L);check(handle!=null,"leased Book root");
  check(h.sendPetGeorge(handle,System.nanoTime()),"observed exact typed George command");check(game.player.connection.commands.equals(List.of("call george"))&&h.notices.size()==1&&h.session.isCurrent(handle)&&h.pets.cancellations==0,"only /call george, one notice, trusted event cannot cancel owner");
  check(!h.ownGameplayCommand,"trusted flag cleared");
  game.gui.screen=new Object();check(!h.sendPetGeorge(handle,System.nanoTime()),"any screen blocks");game.gui.screen=null;game.gui.overlay=new Object();check(!h.sendPetGeorge(handle,System.nanoTime()),"overlay blocks");game.gui.overlay=null;
  game.player.containerMenu=new Container();check(!h.sendPetGeorge(handle,System.nanoTime()),"existing foreign native container blocks");game.player.containerMenu=game.player.inventoryMenu;game.player.containerMenu.carried.empty=false;check(!h.sendPetGeorge(handle,System.nanoTime()),"carried item blocks");game.player.containerMenu.carried.empty=true;
  game.same=false;check(!h.sendPetGeorge(handle,System.nanoTime()),"off-thread blocks");game.same=true;
  for(var blocker:AutomationContext.Blocker.values())if(blocker!=AutomationContext.Blocker.INVENTORY_FULL){h.ctx=new AutomationContext(1,true,true,Set.of(blocker));check(!h.sendPetGeorge(handle,System.nanoTime()),"all blockers retained: "+blocker);}h.ctx=new AutomationContext(1,true,true,Set.of());
  game.player.connection.observe=false;check(!h.sendPetGeorge(handle,System.nanoTime())&&h.notices.size()==1&&!h.ownGameplayCommand,"unobserved send not announced");game.player.connection.observe=true;
  h.onOutgoingCommand();check(!h.session.active()&&h.pets.cancellations==1,"ordinary manual command preempts Book");
  var foreign=h.session.begin(AutomationFeature.AUTO_SELL,System.nanoTime(),90_000_000_000L);int before=game.player.connection.commands.size();check(!h.sendPetGeorge(foreign,System.nanoTime())&&game.player.connection.commands.size()==before,"foreign root cannot send anvil");h.session.cancel(System.nanoTime());
  handle=h.session.begin(AutomationFeature.PET_SELL,System.nanoTime(),90_000_000_000L);game.player.connection.fail=true;try{h.sendPetGeorge(handle,System.nanoTime());throw new AssertionError("expected native failure");}catch(IllegalStateException expected){}check(!h.ownGameplayCommand&&h.notices.size()==1,"uncertain send leaks no trusted authority or success notice");
  game.player.connection.fail=false;h.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.INVENTORY_FULL));check(h.sendPetGeorge(handle,System.nanoTime())&&h.notices.size()==2,"Book full-inventory exception allows opening; selected inputs will free their own main return slots");
  System.out.println("PetGeorgeWiringTest: PASS (actual typed /call george sender/event, root/thread/context/screen/cursor/observed command gates, no arbitrary command)");
 }
}'''.replace('METHODS',methods)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-book-wiring-') as temp:
 p=Path(temp);(p/'PetGeorgeWiringTest.java').write_text(java)
 subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-d',temp,*map(str,(main/'automation').rglob('*.java')),str(p/'PetGeorgeWiringTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp+':'+str(gson),'dev.kuan.kuanperion.PetGeorgeWiringTest'],check=True)
for name in ['onManualInventoryInput','onOutgoingCommand','onManualLook','invalidateFarmSession','worldChanged','shutdown']:
 assert 'pets.cancel(' in body(name),name
assert 'pets.suspend(now)' in body('suspendForJerry')
assert 'pets.tick(now)' in body('tick') and '!pets.active()' in body('beforeMovementTick')
assert bridge.count('new PetSaleService(')==1 and bridge.count('new PetGeorgeController(')==1 and 'this::sendPetGeorge' in bridge
client=(main/'KuanperionClient.java').read_text();assert 'automation.junk(),automation.book()' in client
ui=(main/'ui/PetSaleModuleSettings.java').read_text()
for forbidden in ['sendCommand(','sendPetGeorge(','.combine(','Minecraft.getInstance()']:assert forbidden not in ui,forbidden
assert 'service.manualRequest(System.nanoTime())' in ui and 'Pet Sell' in ui
print('PetGeorgeLifecycleWiringTest: PASS (lazy policy-bound native port, Trade UI, tick/manual/world/Jerry/shutdown cancellation; real server formats/fees remain live acceptance gates)')

for suffix in ['beforePetServerClose','afterPetServerClose']:
 closed=body(suffix)
 for forbidden in ['.sendCommand(','.closeContainer(','.setScreen(','.handleContainerInput(','.finish(']:assert forbidden not in closed,forbidden
 assert '!mc.isSameThread()' in closed
assert 'menu.containerId()!=packet.getContainerId()' in body('beforePetServerClose')
for guard in ['petCloseWorld!=currentWorldId()','petCloseId!=packet.getContainerId()','mc.gui.screen()!=null','mc.player.containerMenu!=mc.player.inventoryMenu']:assert guard in body('afterPetServerClose'),guard
assert 'beforePetServerClose(packet)' in body('beforeServerClose') and 'afterPetServerClose(packet)' in body('afterServerClose')
print('PetAppliedCloseWiring: PASS exact server HEAD/TAIL scope, no local absence shortcut/native action; pure private-ticket proof tested separately')
