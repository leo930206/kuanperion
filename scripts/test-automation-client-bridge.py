#!/usr/bin/env python3
"""Exercise actual 26.2 observation/send adapter methods against explicit game boundaries."""
from pathlib import Path
import re,subprocess,sys,tempfile
root=Path(__file__).resolve().parent.parent
source=(root/'src/main/java/dev/kuan/kuanperion/AutomationClientBridge.java').read_text()
client=(root/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text()
assert 'ClientSendMessageEvents.COMMAND.register(command->automation.onOutgoingCommand())' in client
assert 'ClientTickEvents.END_CLIENT_TICK.register(client->automation.tick())' in client
chat_line=next(line for line in client.splitlines() if 'ClientReceiveMessageEvents.CHAT.register' in line)
assert 'automation.' not in chat_line,'Player chat must not drive Stash'
def extract(name):
 a=re.search(r' (?:public|private) [^{}\n()]+ '+name+r'\(',source).start();b=source.index('{',a)+1;d=1
 while d:
  d+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b].replace('dev.kuan.kuanperion.ui.MainGUI','OwnMenu')
methods='\n'.join(extract(n) for n in ['currentWorldId','observe','send'])
java=r'''package dev.kuan.kuanperion;
import java.util.*;import java.util.function.*;import dev.kuan.kuanperion.automation.*;
public class AutomationClientBridgeTest {
 record LegacyState(boolean farmingArmed,boolean mining,boolean recovering,boolean selecting,String island){}
 LegacyState flags=new LegacyState(true,false,false,false,"GARDEN");
 final Supplier<LegacyState> legacy=()->flags;final Function<Minecraft,List<String>> sidebar=mc->List.of("skyblock","gardenplot");
 Object levelIdentity;long worldGeneration;
 static class OwnMenu{}
 static class Gui {Object screen;Object screen(){return screen;}}
 static class Window {long handle(){return 1;}}
 static class Server {String ip="mc.hypixel.net";}
 static class Player {boolean alive=true;Inventory inv=new Inventory();Connection connection=new Connection();boolean isAlive(){return alive;}Inventory getInventory(){return inv;}}
 static class Inventory {int free=1;int getFreeSlot(){return free;}}
 static class Connection {List<String> commands=new ArrayList<>();void sendCommand(String s){commands.add(s);}}
 static class Minecraft {static final Minecraft INSTANCE=new Minecraft();Object level;Player player;Gui gui=new Gui();Server server=new Server();static Minecraft getInstance(){return INSTANCE;}Window getWindow(){return new Window();}Server getCurrentServer(){return server;}}
 static class GLFW {static final int GLFW_FOCUSED=1,GLFW_TRUE=1;static boolean focused=true;static int glfwGetWindowAttrib(long h,int v){return focused?1:0;}}
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 METHODS
 public static void main(String[] args){
  var adapter=new AutomationClientBridgeTest();var mc=Minecraft.getInstance();
  check(!adapter.send(AutomationCommand.PICKUP_STASH),"outside world cannot send");
  mc.level=new Object();mc.player=new Player();var first=adapter.observe();
  check(first.canIssueCommand()&&first.garden()&&adapter.send(AutomationCommand.PICKUP_STASH),"known SkyBlock safe context sends exact command");
  check(mc.player.connection.commands.equals(List.of("pickupstash")),"typed no-slash command only");
  check(!adapter.send(AutomationCommand.BOOSTER_COOKIE),"Stash boundary cannot send gameplay commands");
  mc.gui.screen=new Object();check(!adapter.send(AutomationCommand.PICKUP_STASH),"foreign GUI blocks");mc.gui.screen=new OwnMenu();check(adapter.observe().canIssueCommand(),"own settings does not block command-only task");mc.gui.screen=null;
  GLFW.focused=false;check(!adapter.send(AutomationCommand.PICKUP_STASH),"background blocks");GLFW.focused=true;
  for(int i=0;i<3;i++){adapter.flags=new LegacyState(true,i==0,i==1,i==2,"GARDEN");check(adapter.observe().hardBlocked()&&!adapter.send(AutomationCommand.PICKUP_STASH),"Mining/recovery/selection priority");}
  adapter.flags=new LegacyState(true,false,false,false,"GARDEN");mc.player.alive=false;check(!adapter.send(AutomationCommand.PICKUP_STASH),"dead blocks");mc.player.alive=true;
  mc.player.inv.free=-1;check(!adapter.send(AutomationCommand.PICKUP_STASH),"full inventory blocks");mc.player.inv.free=1;
  mc.server.ip="mc.hypixel.net.evil.example";check(!adapter.send(AutomationCommand.PICKUP_STASH),"other server rejected even with stale optional API island");
  adapter.flags=new LegacyState(true,false,false,false,null);mc.server.ip="mc.hypixel.net:25565";check(adapter.observe().canIssueCommand()&&adapter.observe().garden(),"official server plus native sidebar fallback");
  mc.server.ip="other.example";check(!adapter.send(AutomationCommand.PICKUP_STASH),"fake SkyBlock sidebar alone not enough");
  mc.server.ip="mc.hypixel.net";mc.level=new Object();check(adapter.observe().worldId()!=first.worldId(),"world identity changes generation");
  check(mc.player.connection.commands.size()==1,"blocked contexts generated no network calls");
  System.out.println("AutomationClientBridgeTest: PASS (actual observe/send methods, world/focus/GUI/inventory/legacy priority/official server)");
 }
}'''.replace('METHODS',methods)
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'AutomationClientBridgeTest.java';p.write_text(java);jdk=Path(sys.argv[1])/'bin'
 files=[root/'src/main/java/dev/kuan/kuanperion/automation'/(n+'.java') for n in ['AutomationContext','AutomationCommand','AutomationCoordinator','AutomationFeature']]
 subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,*map(str,files),str(p)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'dev.kuan.kuanperion.AutomationClientBridgeTest'],check=True)
