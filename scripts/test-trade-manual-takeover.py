#!/usr/bin/env python3
"""Compose actual manual hook bodies with real Trade services/tasks/session; never close a taken-over GUI."""
from pathlib import Path
import re, subprocess, tempfile
root=Path(__file__).resolve().parent.parent
main=root/'src/main/java/dev/kuan/kuanperion';source=(main/'AutomationClientBridge.java').read_text()
def body(name):
 m=re.search(r' public [^{}\n()]+ '+name+r'\(',source);assert m,name
 a=m.start();b=source.index('{',a)+1;depth=1
 while depth:depth+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
methods='\n'.join(body(n) for n in ['onManualInventoryInput','onManualLook','onOutgoingCommand'])
java=r'''package dev.kuan.kuanperion.automation;
import java.nio.file.*;import java.util.*;
public final class TradeManualTakeoverTest {
 static final long S=1_000_000_000L;boolean ownGameplayCommand,observedGameplayCommand,closed,useJunk;int pauses,stops;
 static class Service {int calls;void cancel(long n){calls++;}void onOutgoingCommand(){calls++;}}
 Service stash=new Service(),pest=new Service(),greenhouse=new Service(),composter=new Service(),visitor=new Service(),supercraft=new Service(),book=new Service();
 static class Gui{Object current;Object screen(){return current;}}
 static class Minecraft{static final Minecraft I=new Minecraft();static Minecraft getInstance(){return I;}Gui gui=new Gui();}
 final AutoSellTaskTest.Fake sellFake=new AutoSellTaskTest.Fake();final JunkTaskTest.Fake junkFake=new JunkTaskTest.Fake();
 final AutomationSession session=new AutomationSession(new AutomationCoordinator(),()->useJunk?junkFake.context():sellFake.context(),()->pauses++,()->stops++);
 final AutoSellService autoSell;final JunkService junk;
 TradeManualTakeoverTest(Path path)throws Exception{var store=AutomationSettingsStore.load(path);store.setAutoSellPolicy(new AutoSellPolicy(Set.of("WHEAT"),Set.of(),2,128,1000,60,60,85,true));store.setJunkPolicy(JunkTaskTest.policy());autoSell=new AutoSellService(store,session,new InventoryReceipts(),sellFake,()->closed,n->{});junk=new JunkService(store,session,new InventoryReceipts(),junkFake,()->closed,n->{});}
 void queue(){check(autoSell.manualRequest(0)&&junk.manualRequest(0)&&!session.active(),"two explicit inert requests, no root");}
 METHODS
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{var dir=Files.createTempDirectory("trade-manual-takeover-");try{
  var h=new TradeManualTakeoverTest(dir.resolve("slot.json"));h.queue();h.onManualInventoryInput();h.closed=true;h.autoSell.tick(S);h.junk.tick(S);check(!h.autoSell.pending()&&!h.junk.pending()&&!h.session.active()&&h.pauses==0&&h.stops==0&&h.sellFake.commands==0&&h.junkFake.opens==0,"manual inventory click clears pending Trade without gameplay or a Farm stop");
  h=new TradeManualTakeoverTest(dir.resolve("look.json"));h.queue();Minecraft.I.gui.current=new Object();h.onManualLook();check(h.autoSell.pending()&&h.junk.pending(),"moving a settings pointer is not a world takeover");Minecraft.I.gui.current=null;h.onManualLook();check(!h.autoSell.pending()&&!h.junk.pending()&&h.pauses==0,"actual manual world look clears pending Trade");
  h=new TradeManualTakeoverTest(dir.resolve("command.json"));h.queue();h.ownGameplayCommand=true;h.onOutgoingCommand();check(h.autoSell.pending()&&h.junk.pending()&&h.observedGameplayCommand,"trusted typed command records acknowledgement without impersonating manual takeover");h.ownGameplayCommand=false;h.onOutgoingCommand();check(!h.autoSell.pending()&&!h.junk.pending()&&h.stops==0,"ordinary command clears idle pending Trade");
  for(boolean command:List.of(false,true)){
   h=new TradeManualTakeoverTest(dir.resolve("active-sell-"+command+".json"));h.closed=true;check(h.autoSell.manualRequest(0),"active sell fixture");h.autoSell.tick(0);h.sellFake.menu=AutoSellTaskTest.menu(AutoSellTaskTest.wheat(64),1);h.sellFake.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));h.session.poll(S);h.autoSell.tick(S);var menu=h.sellFake.menu;int sales=h.sellFake.sales;
   if(command)h.onOutgoingCommand();else h.onManualInventoryInput();check(!h.session.active()&&!h.autoSell.active()&&h.stops==1&&h.sellFake.closed==0&&h.sellFake.menu==menu&&h.sellFake.sales==sales,"invalidate ownership before Trade cancellation: manual takeover never closes/touches the player's now-owned NPC menu");
   h=new TradeManualTakeoverTest(dir.resolve("active-junk-"+command+".json"));h.useJunk=true;h.closed=true;check(h.junk.manualRequest(0),"active Junk fixture");h.junk.tick(0);var inventory=h.junkFake.visible;check(inventory!=null,"owned inventory is open");if(command)h.onOutgoingCommand();else h.onManualInventoryInput();check(!h.session.active()&&!h.junk.active()&&h.stops==1&&h.junkFake.closes==0&&h.junkFake.visible==inventory&&h.junkFake.drops==0,"manual takeover revokes Root before Junk cleanup, leaving inventory/cursor and items to player");
  }
  System.out.println("TradeManualTakeoverTest: PASS (actual manual look/inventory/command bodies, inert pending cancellation, trusted sender preserved, revocation-before-cleanup with real active Trade roots)");
 }finally{try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}}
}'''.replace('METHODS',methods)
jdk=Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='trade-manual-hook-') as temp:
 p=Path(temp);(p/'TradeManualTakeoverTest.java').write_text(java)
 tests=[root/'src/test/java/dev/kuan/kuanperion/automation'/n for n in ['AutoSellTaskTest.java','JunkTaskTest.java']]
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',temp,*map(str,(main/'automation').rglob('*.java')),*map(str,tests),str(p/'TradeManualTakeoverTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp+':'+str(gson),'dev.kuan.kuanperion.automation.TradeManualTakeoverTest'],check=True)
