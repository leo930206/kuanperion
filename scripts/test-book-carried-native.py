#!/usr/bin/env python3
"""Execute the actual narrow native deposit method body with deterministic physical-input fixtures.
No game, connection or account is started. Pinned26.2 linkage is checked separately by build/native fixtures.
"""
from pathlib import Path
import subprocess
import sys
import tempfile
import re

root=Path(__file__).resolve().parent.parent
jdk=Path(sys.argv[1]) if len(sys.argv)>1 else Path.home()/'Library/Application Support/PrismLauncher/java/java-runtime-epsilon/jre.bundle/Contents/Home'
source=(root/'src/main/java/dev/kuan/kuanperion/AutomationMenuActions.java').read_text()
start=source.index(' static boolean depositBookNative(')
brace=source.index('{',start);depth=1;end=brace+1
while depth:
    if source[end]=='{':depth+=1
    if source[end]=='}':depth-=1
    end+=1
method=source[start:end]
assert re.search(r'^ static boolean depositBookNative\(',source,re.M),'Carried-book sender must remain package-private'
assert {p.name for p in (root/'src/main/java').rglob('*.java') if 'AutomationMenuActions.depositBookNative(' in p.read_text()}=={'BookMenuController.java'}

harness=r'''
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class NativeBookDepositBodyTest {
 static ItemSnapshot empty(){return new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);}
 static ItemSnapshot book(String id,String name,int count,boolean protection){return new ItemSnapshot(id,"minecraft:enchanted_book","own-uuid",name,"exact-components",count,List.of(),protection);}
 static ItemSnapshot own;static MenuContract.Ticket ticket;
 static void reset(){
  var m=new Minecraft();Minecraft.instance=m;own=book("ENCHANTED_BOOK","Enchanted Book",1,false);var observed=new ArrayList<MenuSnapshot.Slot>();
  for(int i=0;i<90;i++){observed.add(new MenuSnapshot.Slot(i,empty(),i>=54));m.player.containerMenu.slots.add(new NativeSlot(i,i<54?m.external:m.player.inventory,i<54?i:i<81?i-54+9:i-81));}
  m.frame=new MenuSnapshot(1,4,1,"Anvil",54,observed);m.player.containerMenu.carried=new Stack(own);ticket=new MenuContract.Ticket(m.frame,54,MenuContract.Click.PICKUP);
 }
 static void replaceOwn(ItemSnapshot replacement){own=replacement;Minecraft.instance.player.containerMenu.carried=new Stack(own);}
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  reset();check(AutomationMenuActions.depositBookNative(ticket,own)&&Minecraft.instance.gameMode.calls==1&&!AutomationMenuActions.ownedInput,"exact owned main-slot PICKUP, scoped native-input flag");
  for(String mode:List.of("thread","level","player","gameMode","cursor","identity","protected","count","name","hotbar","throw","alias","external","index","occupied","inactive","fake","placement","capacity","stale","container","no-screen")){
   reset();var m=Minecraft.instance;
   switch(mode){
    case "thread"->m.thread=false;case "level"->m.level=null;case "player"->m.player=null;case "gameMode"->m.gameMode=null;
    case "cursor"->m.player.containerMenu.carried=new Stack(empty());case "identity"->replaceOwn(book("WHEAT","Enchanted Book",1,false));
    case "protected"->replaceOwn(book("ENCHANTED_BOOK","Enchanted Book",1,true));case "count"->replaceOwn(book("ENCHANTED_BOOK","Enchanted Book",2,false));case "name"->replaceOwn(book("ENCHANTED_BOOK","Renamed",1,false));
    case "hotbar"->ticket=new MenuContract.Ticket(m.frame,81,MenuContract.Click.PICKUP);case "throw"->ticket=new MenuContract.Ticket(m.frame,54,MenuContract.Click.THROW);
    case "alias"->m.player.containerMenu.slots.get(54).raw=10;case "external"->m.player.containerMenu.slots.get(54).container=m.external;case "index"->m.player.containerMenu.slots.get(54).index=55;
    case "occupied"->m.player.containerMenu.slots.get(54).stack=new Stack(own);
    case "inactive"->m.player.containerMenu.slots.get(54).active=false;case "fake"->m.player.containerMenu.slots.get(54).fake=true;case "placement"->m.player.containerMenu.slots.get(54).allowed=false;case "capacity"->m.player.containerMenu.slots.get(54).capacity=0;
    case "stale"->m.frame=new MenuSnapshot(1,4,2,"Anvil",54,m.frame.slots());case "container"->m.player.containerMenu.containerId=5;case "no-screen"->m.frame=null;
   }
   check(!AutomationMenuActions.depositBookNative(ticket,own),mode+" must refuse");check(m.gameMode==null||m.gameMode.calls==0,mode+" native effect denied");check(!AutomationMenuActions.ownedInput,mode+" no leaked native ownership");
  }
  reset();AutomationMenuActions.ownedInput=true;check(!AutomationMenuActions.depositBookNative(ticket,own)&&AutomationMenuActions.ownedInput&&Minecraft.instance.gameMode.calls==0,"no nested native effect or ownership theft");AutomationMenuActions.ownedInput=false;
  reset();Minecraft.instance.gameMode.failure=true;boolean threw=false;try{AutomationMenuActions.depositBookNative(ticket,own);}catch(IllegalStateException expected){threw=true;}
  check(threw&&Minecraft.instance.gameMode.calls==1&&!AutomationMenuActions.ownedInput,"possibly-sent native failure propagates with ownership restored");
  System.out.println("NativeBookDepositBodyTest: PASS (actual method body, exact own cursor/empty main alias/thread/menu/protection/click guards, no game/server)");
 }
}
enum ContainerInput{PICKUP,QUICK_MOVE,THROW}
final class Stack{final ItemSnapshot snapshot;Stack(ItemSnapshot s){snapshot=s;}boolean isEmpty(){return snapshot.count()==0;}}
final class NativeSlot{int index,raw,capacity=64;boolean active=true,fake,allowed=true;Object container;Stack stack=new Stack(NativeBookDepositBodyTest.empty());NativeSlot(int i,Object c,int r){index=i;container=c;raw=r;}int getContainerSlot(){return raw;}Stack getItem(){return stack;}boolean isActive(){return active;}boolean isFake(){return fake;}boolean mayPlace(Stack item){return allowed;}int getMaxStackSize(Stack item){return capacity;}}
final class NativeMenu{int containerId=4;List<NativeSlot> slots=new ArrayList<>();Stack carried;Stack getCarried(){return carried;}}
final class NativePlayer{final Object inventory=new Object();final NativeMenu containerMenu=new NativeMenu();Object getInventory(){return inventory;}}
final class NativeMode{int calls;boolean failure;void handleContainerInput(int id,int slot,int button,ContainerInput input,NativePlayer player){
 if(!AutomationMenuActions.ownedInput||id!=4||slot!=54||button!=0||input!=ContainerInput.PICKUP)throw new AssertionError("wrong native effect");calls++;if(failure)throw new IllegalStateException("possibly sent");}}
final class Minecraft{static Minecraft instance;boolean thread=true;Object level=new Object(),external=new Object();NativePlayer player=new NativePlayer();NativeMode gameMode=new NativeMode();MenuSnapshot frame;static Minecraft getInstance(){return instance;}boolean isSameThread(){return thread;}}
final class AutomationGameView{static MenuSnapshot menu(Minecraft mc,long world){return mc.frame;}static ItemSnapshot item(Stack stack,boolean equipment){return stack.snapshot;}}
'''

with tempfile.TemporaryDirectory(prefix='kuanperion-book-native-body-') as directory:
    tmp=Path(directory)
    (tmp/'NativeBookDepositBodyTest.java').write_text(harness)
    pure=root/'src/main/java/dev/kuan/kuanperion/automation'
    variants=[('original',method),('no-protection',method.replace('ownBook.protectedItem()||','')),('no-empty-destination',method.replace('||!slot.getItem().isEmpty()',''))]
    for name,body in variants:
        (tmp/'AutomationMenuActions.java').write_text('package dev.kuan.kuanperion;import dev.kuan.kuanperion.automation.*;final class AutomationMenuActions{static boolean ownedInput;'+body+'}')
        subprocess.run([str(jdk/'bin/javac'),'--release','21','-encoding','UTF-8','-d',str(tmp),*[str(pure/(p+'.java')) for p in ('ItemSnapshot','MenuSnapshot','MenuContract')],str(tmp/'AutomationMenuActions.java'),str(tmp/'NativeBookDepositBodyTest.java')],check=True,capture_output=True,text=True,timeout=60)
        result=subprocess.run([str(jdk/'bin/java'),'-cp',str(tmp),'dev.kuan.kuanperion.NativeBookDepositBodyTest'],capture_output=True,text=True,timeout=30)
        if name=='original':
            assert result.returncode==0,result.stdout+result.stderr
            print(result.stdout,end='')
        else:
            assert result.returncode!=0 and ('protected' if name=='no-protection' else 'occupied') in result.stderr,result.stdout+result.stderr
            print('NativeBookDepositMutation: rejected '+name)
