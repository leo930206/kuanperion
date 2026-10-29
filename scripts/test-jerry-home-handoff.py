#!/usr/bin/env python3
"""Real bridge sender/event bodies + real exclusive session; no native network, player account or world effects."""
from pathlib import Path
import re,subprocess,tempfile,sys
r=Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion';source=(m/'AutomationClientBridge.java').read_text()
def extract(name):
 match=re.search(r' (?:public|private) [^{}\n()]+ '+name+r'\(',source);assert match,'Missing trusted literal Jerry-home boundary: '+name
 a=match.start();b=source.index('{',a)+1;depth=1
 while depth:
  depth+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
methods='\n'.join(extract(name) for name in ['sendAutomaticJerryHome','onOutgoingCommand'])
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class JerryHomeHandoffTest {
 boolean farming=true,ownGameplayCommand,observedGameplayCommand;int stopped,paused;
 AutomationContext observe(){return new AutomationContext(1,farming,true,Set.of());}
 AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::observe,()->paused++,()->{stopped++;farming=false;});
 static class Service {int calls;void cancel(long now){calls++;}void onOutgoingCommand(){calls++;}}
 Service stash=new Service(),pest=new Service(),greenhouse=new Service(),composter=new Service(),visitor=new Service(),supercraft=new Service(),book=new Service(),pets=new Service(),autoSell=new Service(),junk=new Service();
 static JerryHomeHandoffTest current;
 static class Connection {boolean observed=true,throwing;List<String> commands=new ArrayList<>();void sendCommand(String command){commands.add(command);if(throwing)throw new IllegalStateException("fixture send error");if(observed)current.onOutgoingCommand();}}
 static class Player {Connection connection=new Connection();}
 static class Minecraft {static final Minecraft INSTANCE=new Minecraft();Player player=new Player();Object level=new Object();boolean same=true;static Minecraft getInstance(){return INSTANCE;}boolean isSameThread(){return same;}}
 METHODS
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 void start(AutomationFeature feature){check(session.begin(feature,System.nanoTime(),120_000_000_000L)!=null,"fixture root hold");}
 public static void main(String[]args){
  for(var feature:List.of(AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.VISITOR,AutomationFeature.SUPERCRAFT)){
   var h=new JerryHomeHandoffTest();current=h;var mc=Minecraft.getInstance();mc.player=new Player();h.start(feature);
   check(h.sendAutomaticJerryHome()&&mc.player.connection.commands.equals(List.of("sethome")),"one observed literal record-only command");
   check(h.session.active()&&h.session.holdsFarming()&&h.farming&&h.stopped==0&&h.pest.calls==0&&h.greenhouse.calls==0&&h.composter.calls==0&&h.visitor.calls==0&&h.supercraft.calls==0,"Jerry auto home must not preempt "+feature+" or prematurely clear legacy Farming before its own stop-toggle/beam handler");
   check(!h.ownGameplayCommand&&!h.observedGameplayCommand,"no trusted flag leaks after normal send");h.onOutgoingCommand();check(!h.session.active()&&!h.farming&&h.stopped==1,"subsequent ordinary manual commands retain takeover priority");
  }
  var h=new JerryHomeHandoffTest();current=h;var mc=Minecraft.getInstance();mc.player=new Player();h.start(AutomationFeature.PEST);mc.player.connection.throwing=true;check(!h.sendAutomaticJerryHome()&&!h.ownGameplayCommand&&!h.observedGameplayCommand&&h.session.active(),"native send failure scopes flags and does not invent a manual stop");mc.player.connection.throwing=false;
  h.ownGameplayCommand=true;h.observedGameplayCommand=false;check(h.sendAutomaticJerryHome()&&h.ownGameplayCommand&&!h.observedGameplayCommand,"nested record-only send must not forge or erase outer owner's dispatch acknowledgement");h.ownGameplayCommand=false;
  mc.player.connection.observed=false;check(!h.sendAutomaticJerryHome()&&h.session.active()&&!h.ownGameplayCommand,"blocked/unobserved send not falsely announced as sent");mc.same=false;int count=mc.player.connection.commands.size();check(!h.sendAutomaticJerryHome()&&mc.player.connection.commands.size()==count,"client-thread guard before native effect");mc.same=true;mc.player=null;check(!h.sendAutomaticJerryHome(),"no player, no record-only command");
  System.out.println("JerryHomeHandoffTest: PASS (actual typed sender/event bodies, original held Farm intact for all four flight owners, literal command only, manual priority, failure/blocked/nested cleanup)");
 }
}'''.replace('METHODS',methods)
prism=Path.home()/'Library/Application Support/PrismLauncher';jdk=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=prism/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory() as temp:
 p=Path(temp);(p/'JerryHomeHandoffTest.java').write_text(java);sources=[str(f) for f in(m/'automation').rglob('*.java')]
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',temp,*sources,str(p/'JerryHomeHandoffTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp+':'+str(gson),'dev.kuan.kuanperion.JerryHomeHandoffTest'],check=True)
assert methods.count('.sendCommand("sethome")')==1 and '.sendCommand(command)' not in methods,'No arbitrary command/warp exemption'

client=(m/'KuanperionClient.java').read_text()
assert 'boolean autoSethomeSent=jerryAutoSethomeEnabled&&sendJerryHomeCommand(mc);' in client
assert 'return automation==null?sendHomeCommand(mc,"sethome",false):automation.sendAutomaticJerryHome();' in client
assert client.count('automation.sendAutomaticJerryHome()')==1,'Trusted exemption is only for Jerry automatic home, not manual shortcuts or arbitrary commands'
print('JerryHomeWiringTest: PASS (only trusted legacy Jerry sender exempted; manual hotkeys/commands, original stop toggle, message and marker handler retained)')
