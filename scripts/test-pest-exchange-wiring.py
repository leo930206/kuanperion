#!/usr/bin/env python3
"""Execute actual typed Phillip command/outgoing-event bodies; inspect closed lifecycle wiring, not live Hypixel."""
from pathlib import Path
import re,subprocess,tempfile
r=Path(__file__).resolve().parents[1];m=r/'src/main/java/dev/kuan/kuanperion';bridge=(m/'AutomationClientBridge.java').read_text()
def body(name):
 match=re.search(r' (?:public|private) [^{}\n()]+ '+name+r'\(',bridge);assert match,name
 a=match.start();b=bridge.index('{',a)+1;depth=1
 while depth:depth+=(bridge[b]=='{')-(bridge[b]=='}');b+=1
 return bridge[a:b]
methods=body('sendPestExchange')+'\n'+body('onOutgoingCommand')
java=r'''
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestExchangeWiringTest {
 AutomationContext ctx=new AutomationContext(1,true,true,Set.of());Runnable onObserve=()->{};AutomationContext observe(){onObserve.run();return ctx;}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::observe,()->{},()->{});boolean ownGameplayCommand,observedGameplayCommand;
 static class Service{int cancellations;void cancel(long n){cancellations++;}void onOutgoingCommand(){}}
 Service stash=new Service(),pest=new Service(),spray=new Service(),greenhouse=new Service(),composter=new Service(),visitor=new Service(),supercraft=new Service(),book=new Service(),pets=new Service(),autoSell=new Service(),junk=new Service();
 static PestExchangeWiringTest active;
 static class Stack{boolean empty=true;boolean isEmpty(){return empty;}}static class Container{final Stack carried=new Stack();Stack getCarried(){return carried;}}
 static class Connection{List<String> commands=new ArrayList<>();boolean observe=true,failure;void sendCommand(String value){commands.add(value);if(failure)throw new IllegalStateException("uncertain native command");if(observe)active.onOutgoingCommand();}}
 static class Player{final Connection connection=new Connection();final Container inventoryMenu=new Container();Container containerMenu=inventoryMenu;}
 static class Gui{Object current,cover;Object screen(){return current;}Object overlay(){return cover;}}
 static class Minecraft{static Minecraft instance;boolean thread=true;Player player=new Player();Object level=new Object();final Gui gui=new Gui();static Minecraft getInstance(){return instance;}boolean isSameThread(){return thread;}}
 METHODS
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  for(String mode:List.of("ok","thread","screen","overlay","cursor","container","garden","blocker","world","revoked","foreign","expired","unobserved","throw","late-level","late-player")){
   var h=new PestExchangeWiringTest();active=h;var game=new Minecraft();Minecraft.instance=game;long now=System.nanoTime();var owner=h.session.begin(mode.equals("foreign")?AutomationFeature.VISITOR:AutomationFeature.PEST,now,120_000_000_000L);check(owner!=null,"original root");
   switch(mode){case "thread"->game.thread=false;case "screen"->game.gui.current=new Object();case "overlay"->game.gui.cover=new Object();case "cursor"->game.player.containerMenu.carried.empty=false;case "container"->game.player.containerMenu=new Container();case "garden"->h.ctx=new AutomationContext(1,true,false,Set.of());case "blocker"->h.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MINING));case "world"->h.ctx=new AutomationContext(2,true,true,Set.of());case "revoked"->h.session.cancel(now);case "expired"->now+=121_000_000_000L;case "unobserved"->game.player.connection.observe=false;case "throw"->game.player.connection.failure=true;case "late-level"->h.onObserve=()->{h.onObserve=()->{};game.level=new Object();};case "late-player"->h.onObserve=()->{h.onObserve=()->{};game.player=new Player();};}
   boolean sent=false,threw=false;try{sent=h.sendPestExchange(owner,now);}catch(IllegalStateException expected){threw=true;}
   int count=game.player.connection.commands.size();check(sent==mode.equals("ok")&&threw==mode.equals("throw"),mode+" expected guard/uncertainty result");
   check(count==(List.of("ok","unobserved","throw").contains(mode)?1:0),mode+" no other command or retry");
   if(count>0)check(game.player.connection.commands.equals(List.of("call phillip")),"fixed source literal only");
   check(!h.ownGameplayCommand&&!h.observedGameplayCommand,"trusted-command flags restored, not leaked: "+mode);
   if(mode.equals("ok")){check(h.session.isCurrent(owner)&&h.pest.cancellations==0,"own outgoing event retains original root");h.ownGameplayCommand=true;h.observedGameplayCommand=true;check(h.sendPestExchange(owner,now)&&h.ownGameplayCommand&&h.observedGameplayCommand,"nested flag scope restored");h.ownGameplayCommand=false;h.observedGameplayCommand=false;h.onOutgoingCommand();check(!h.session.active()&&h.pest.cancellations==1,"real external command preempts Pest root");}
  }
  System.out.println("PestExchangeWiringTest: PASS actual typed /call phillip and outgoing-event bodies; original-root/thread/world/screen/cursor/late-identity/uncertain guards; controlled connection only");
 }
}
'''.replace('METHODS',methods)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-exchange-wiring-') as d:
 p=Path(d);(p/'PestExchangeWiringTest.java').write_text(java)
 subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-sourcepath',str(r/'src/main/java'),'-d',d,str(p/'PestExchangeWiringTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.PestExchangeWiringTest'],check=True)
flight=(m/'PestFlightController.java').read_text();task=(m/'automation/pest/PestTask.java').read_text();ui=(m/'ui/PestModuleSettings.java').read_text()
assert 'this::sendPestExchange' in bridge and 'ExchangeOpener exchangeOpener' in flight
assert methods.count('sendCommand("call phillip")')==1 and 'Thread.sleep' not in methods
for name in ['onManualInventoryInput','onOutgoingCommand','onManualLook','invalidateFarmSession','worldChanged','shutdown']:assert 'pest.cancel(' in body(name),name
assert 'pest.suspend(now)' in body('suspendForJerry') and 'pest.tick(now)' in body('tick')
for marker in ['State.EXCHANGING','!exchangeAttempted','exchange.begin(original,true,tool,now)','exchange.tick(now)','exchange.cancel()','exchange.ownsMenu()','case SKIPPED']:assert marker in task,marker
assert 'policy.exchangeAfterCleanup()' in task and 'f.alive()==0&&f.targets().isEmpty()' in task
for text in [(m/'automation/pest/PestExchangeTask.java').read_text(),(m/'automation/pest/PestExchangeView.java').read_text(),ui]:
 for forbidden in ['sendCommand(','HttpClient','URLConnection','Thread.sleep(','.setCarried(','.handleContainerInput(']:assert forbidden not in text,forbidden
assert '清理後兌換整袋 Pest（獨立授權）' in ui and '改用 Barn 實體 Phillip（獨立授權）' in ui
assert 'Barn 兌換桌座標 X Y Z（留空未設定）' in ui and '.withDeskText(value)' in ui
assert 'Abiphone' in ui and '暫定' in ui and '不會改設出生點' in ui
print('PestExchangeLifecycleWiring: PASS actual original-root parent/return, strict UI consent and existing manual/Jerry/world/shutdown hooks; no new network or idle service')
