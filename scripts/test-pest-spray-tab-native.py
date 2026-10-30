#!/usr/bin/env python3
"""Actual spray-Tab adapter bodies + real receipt ledger, with controlled packet/client wrappers.
Pinned26.2 bytecode audited separately below. No applied-Mixin/client/server claim.
"""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parents[1]
s=(root/'src/main/java/dev/kuan/kuanperion/PestSprayTabView.java').read_text()
def method(name):
 m=re.search(r' (?:private )?static [^{}\n]+ '+name+r'\([^\n]*?\)\{',s);assert m,name
 end=m.end();depth=1
 while depth:depth+=(s[end]=='{')-(s[end]=='}');end+=1
 return s[m.start():end]
java=r'''package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class PestSprayTabNativeTest {
 static long now=3_000_000_000L;static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 record Text(String value,int style){}record Profile(UUID id){}record Entry(UUID profileId,Text displayName){}
 static class Row{Profile profile=new Profile(UUID.randomUUID());Text display;Row(String text){display=new Text(text,0);}Profile getProfile(){return profile;}Text getTabListDisplayName(){return display;}}
 static class ClientPacketListener{Map<UUID,Row> known=new HashMap<>();Row getPlayerInfo(UUID id){return known.get(id);}}
 static class ClientboundPlayerInfoUpdatePacket{enum Action{UPDATE_DISPLAY_NAME,UPDATE_LATENCY}EnumSet<Action> actions;List<Entry> entries;ClientboundPlayerInfoUpdatePacket(EnumSet<Action>a,List<Entry>e){actions=a;entries=e;}EnumSet<Action> actions(){return actions;}List<Entry> entries(){return entries;}}
 static class Player{ClientPacketListener connection=new ClientPacketListener();double x=0,z=-96;double getX(){return x;}double getZ(){return z;}}
 static class Minecraft{boolean thread=true;Object level=new Object();Player player=new Player();List<Row> listed=new ArrayList<>();boolean isSameThread(){return thread;}}
 static class GardenTabReader{static List<Row> players(Minecraft m){return m.listed;}static String text(Row r){return r.display.value();}}
 METHODS
 static ClientboundPlayerInfoUpdatePacket packet(Row row,Text text){return new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),List.of(new Entry(row.profile.id(),text)));}
 public static void main(String[]args){
  var m=new Minecraft();var row=new Row("Spray: None");m.listed.add(new Row("Pests:"));m.listed.add(row);m.player.connection.known.put(row.profile.id(),row);
  var stamp=new PlayerPositionReceipts.Stamp(1,1,0,now);var tabs=new PestSprayTabReceipts();var listener=m.player.connection;
  check(current(m,1,stamp,tabs,now).isEmpty(),"cached Tab read cannot refresh or authorize");
  for(String mode:List.of("latency","unknown-id","not-applied","lookalike-style","unlisted","wrong-listener","thread","duplicate","wrong-row","wrong-world-stamp")){
   var packet=packet(row,row.display);var receiver=listener;var inputStamp=stamp;switch(mode){
    case "latency"->packet.actions=EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY);
    case "unknown-id"->packet.entries=List.of(new Entry(UUID.randomUUID(),row.display));
    case "not-applied"->packet.entries=List.of(new Entry(row.profile.id(),new Text("Spray: Test Material (29m)",0)));
    case "lookalike-style"->packet.entries=List.of(new Entry(row.profile.id(),new Text("Spray: None",1)));
    case "unlisted"->m.listed=List.of(new Row("Pests:"));case "wrong-listener"->receiver=new ClientPacketListener();case "thread"->m.thread=false;
    case "duplicate"->packet.entries=List.of(new Entry(row.profile.id(),row.display),new Entry(row.profile.id(),row.display));
    case "wrong-row"->packet.entries=List.of(new Entry(m.listed.getFirst().profile.id(),m.listed.getFirst().display));
    case "wrong-world-stamp"->inputStamp=new PlayerPositionReceipts.Stamp(2,1,0,now);
   }
   applied(m,receiver,packet,1,inputStamp,tabs,now);m.thread=true;m.listed=List.of(new Row("Pests:"),row);
   check(current(m,1,stamp,tabs,now).isEmpty(),"no fabricated applied receipt: "+mode);
  }
  applied(m,listener,packet(row,row.display),1,stamp,tabs,now);var none=current(m,1,stamp,tabs,now).orElseThrow();
  check(PestSprayObservation.parse(none.entries(),"Test Material").state()==PestSprayObservation.State.NONE,"actual adapter correlates unique applied displayed row to scope");
  // An unrelated packet must neither mint nor refresh the existing receipt.
  applied(m,listener,new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),List.of(new Entry(row.profile.id(),row.display))),1,stamp,tabs,now+1);
  check(current(m,1,stamp,tabs,now+1).orElseThrow()==none,"unrelated update cannot refresh timestamp/sequence");
  row.display=new Text("Spray: Test Material (29m)",0);check(current(m,1,stamp,tabs,now).isEmpty(),"predicted cache mutation without packet correlation is not active proof");
  applied(m,listener,packet(row,row.display),1,stamp,tabs,now+1);var active=current(m,1,stamp,tabs,now+1).orElseThrow();check(active.sequence()>none.sequence(),"only actual relevant display write advances usable fact");
  m.player.x=96;check(current(m,1,stamp,tabs,now+1).isEmpty(),"different local plot cannot use old field");m.player.x=0;
  check(current(m,1,new PlayerPositionReceipts.Stamp(1,1,1,now),tabs,now+1).isEmpty(),"correction invalidates scope even at same coordinates");
  m.listed=List.of(new Row("Pests:"),new Row(row.display.value()));check(current(m,1,stamp,tabs,now+1).isEmpty(),"removed/recreated listed row cannot resurrect old fact");
  tabs.clear();m.listed=List.of(new Row("Pests:"),row);check(current(m,1,stamp,tabs,now+1).isEmpty(),"world/shutdown clear revokes cached read");
  System.out.println("PestSprayTabNativeTest: PASS actual adapter bodies/real ledger; ignored/unrelated/unapplied/foreign-row receipts rejected; controlled wrappers, not applied Mixin/live proof");
 }
}'''.replace('METHODS','\n'.join(method(n) for n in ['applied','current','scope']))
p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=p/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-spray-tab-native-') as d:
 f=Path(d)/'PestSprayTabNativeTest.java';f.write_text(java)
 try:subprocess.run([str(jdk/'javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-sourcepath',str(root/'src/main/java'),'-d',d,str(f)],check=True,capture_output=True,text=True,timeout=90)
 except subprocess.CalledProcessError as e:print(e.stderr);raise
 subprocess.run([str(jdk/'java'),'-cp',d+':'+str(gson),'dev.kuan.kuanperion.automation.pest.PestSprayTabNativeTest'],check=True,timeout=30)
# Native pin: handler performs same-thread dispatch, ignores unknown IDs, then applies actions; display action calls real setter.
jar=p/'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'
code=subprocess.check_output([str(jdk/'javap'),'-c','-p','-classpath',str(jar),'net.minecraft.client.multiplayer.ClientPacketListener'],text=True)
handler=code.split('public void handlePlayerInfoUpdate(',1)[1].split('\n  private void applyPlayerInfoUpdate(',1)[0]
apply=code.split('private void applyPlayerInfoUpdate(',1)[1].split('\n  private ',1)[0]
assert 'ensureRunningOnSameThread' in handler and 'Ignoring player info update for unknown player' in handler and 'applyPlayerInfoUpdate:' in handler
assert 'Entry.displayName:' in apply and 'PlayerInfo.setTabListDisplayName:' in apply
mix=(root/'src/main/java/dev/kuan/kuanperion/mixin/AutomationReceiptMixin.java').read_text()
assert '@Inject(method="handlePlayerInfoUpdate",at=@At("TAIL"))' in mix and 'observeAutomationSprayTab((ClientPacketListener)(Object)this,packet)' in mix
bridge=(root/'src/main/java/dev/kuan/kuanperion/AutomationClientBridge.java').read_text()
assert bridge.count('()->sprayTabs.clear()')==2,'World and shutdown must dispose the observation ledger'
assert 'PestSprayTabView.applied(mc,listener,packet,world,positions.stamp(world,now),sprayTabs,now)' in bridge
print('PestSprayTabWiring: PASS pinned26.2 native application sites/source wiring/lifecycle disposal; no transformed-Mixin claim')
