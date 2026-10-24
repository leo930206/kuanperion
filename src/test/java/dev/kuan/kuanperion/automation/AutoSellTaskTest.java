package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutoSellTaskTest {
 static final long S=1000000000L;
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemSnapshot wheat(int count){return new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","wheat-meta",count,List.of(),false);}
 static final class Fake implements AutoSellTask.Port {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());MenuSnapshot menu;int commands,sales,closed;boolean accepted=true,refuseClose,throwClose,throwCommand;
  public AutomationContext context(){return ctx;}public MenuSnapshot menu(){return menu;}
  public List<ItemSnapshot> inventory(){return List.of(wheat(64));}
  public boolean command(AutomationSession.Handle h,AutomationCommand c){check(c==AutomationCommand.BOOSTER_COOKIE,"only exact allowed command");commands++;if(throwCommand)throw new IllegalStateException("native command failure");return accepted;}
  public boolean sell(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,long now){check(rules.permits(ticket.observed().item(ticket.slot()))&&ticket.observed().slot(ticket.slot()).playerInventory(),"exact authorized inventory item only");sales++;return true;}
  public void closeOwnedMenu(int id){if(menu!=null&&menu.containerId()==id){closed++;if(throwClose)throw new IllegalStateException("native close failure");if(!refuseClose)menu=null;}}
 }
 static MenuSnapshot menu(ItemSnapshot item,int state){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<90;i++)slots.add(new MenuSnapshot.Slot(i,i==54?item:new ItemSnapshot("","minecraft:air","","","",0,List.of(),true),i>=54));return new MenuSnapshot(1,4,state,"Booster Cookie",54,slots);}
 record Fixture(Fake port,AutomationSession session,InventoryReceipts receipts,AutoSellTask task,int[] hooks){}
 static Fixture ready(){var f=new Fake();int[] hooks={0,0};var session=new AutomationSession(new AutomationCoordinator(),f::context,()->hooks[0]++,()->hooks[1]++);var receipts=new InventoryReceipts();var task=new AutoSellTask(session,receipts,f);var policy=new AutoSellPolicy(Set.of("WHEAT"),Set.of(),5,320,1000,60,60,85,true);check(task.request(policy,true,0),"cleanup fixture requested");task.tick(0);f.menu=menu(wheat(64),1);f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(S);task.tick(S);return new Fixture(f,session,receipts,task,hooks);}
 static void complete(Fixture fixture){var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);fixture.receipts.observe(1,4,54,empty);fixture.port.menu=menu(empty,2);fixture.task.tick(2*S);fixture.task.tick(3*S);}
 public static void main(String[]args){
  var f=new Fake();var coor=new AutomationCoordinator();int[] hooks={0,0};var session=new AutomationSession(coor,f::context,()->hooks[0]++,()->hooks[1]++);var receipts=new InventoryReceipts();var task=new AutoSellTask(session,receipts,f);
  check(!task.request(AutoSellPolicy.defaults(),true,0),"empty whitelist no command/hold");
  var p=new AutoSellPolicy(Set.of("WHEAT"),Set.of(),5,320,1000,60,60,85,true);
  check(task.request(p,true,0)&&hooks[0]==1,"manual opt-in starts safe hold");task.tick(0);check(f.commands==1&&f.sales==0,"one menu command");
  f.menu=menu(wheat(64),1);f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(S);task.tick(S);
  check(f.sales==1,"one authorized sale");task.tick(2*S);check(f.sales==1&&task.active(),"client prediction/no receipt never sells again");
  var empty=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);receipts.observe(1,4,55,empty);task.tick(3*S);check(f.sales==1,"unrelated server slot not a sale receipt");
  receipts.observe(1,4,54,empty);f.menu=menu(empty,2);task.tick(4*S);task.tick(5*S);
  check(task.state()==AutoSellTask.State.COMPLETE&&!session.holdsFarming()&&hooks[1]==0&&f.closed==1&&task.soldItems()==64,"server receipt then controlled close and original-session resume");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.menu=null;check(task.request(p,true,10*S),"fresh request");task.tick(10*S);f.menu=new MenuSnapshot(1,5,1,"Unexpected Menu",54,List.of());f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(11*S);task.tick(11*S);
  check(!task.active()&&f.closed==1&&hooks[1]==1,"unexpected user menu not closed, no blind resume");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.menu=null;check(task.request(p,true,20*S),"request before timeout");task.tick(20*S);task.tick(80*S);check(task.state()==AutoSellTask.State.TIMED_OUT&&hooks[1]==2,"bounded menu wait");
  f.ctx=new AutomationContext(1,false,true,Set.of());check(!task.request(p,false,81*S),"only-while-Farming auto gate");
  f.ctx=new AutomationContext(1,true,true,Set.of());f.menu=null;check(task.request(p,true,90*S),"manual control handoff fixture");task.tick(90*S);f.menu=menu(wheat(64),3);f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));session.poll(91*S);task.tick(91*S);
  session.cancel(92*S);int closed=f.closed;task.tick(93*S);check(!task.active()&&f.closed==closed,"manual inventory action relinquishes old GUI, never closes it under user's cursor");
  var x=ready();x.port.refuseClose=true;complete(x);check(x.task.state()==AutoSellTask.State.FAILED&&!x.session.active()&&x.hooks[1]==1,"unclosed owned menu cannot report success or resume Farming");
  x=ready();x.port.throwClose=true;boolean threw=false;try{complete(x);}catch(RuntimeException failure){threw=true;}check(!threw&&x.task.state()==AutoSellTask.State.FAILED&&!x.session.holdsFarming()&&x.hooks[1]==1,"close exception still releases ownership and safely stops Farming");
  x=ready();x.port.menu=new MenuSnapshot(1,4,9,"Unexpected Menu",54,menu(wheat(64),1).slots());x.task.tick(2*S);check(x.port.closed==0&&x.port.menu!=null&&x.hooks[1]==1,"unexpected same-container GUI is not closed even before session poll");
  var nativeFailure=new Fake();nativeFailure.throwCommand=true;int[] stopped={0};var failureSession=new AutomationSession(new AutomationCoordinator(),nativeFailure::context,()->{},()->stopped[0]++);var failedTask=new AutoSellTask(failureSession,new InventoryReceipts(),nativeFailure);check(failedTask.request(p,true,0),"native command failure fixture");threw=false;try{failedTask.tick(0);}catch(RuntimeException failure){threw=true;}check(!threw&&failedTask.state()==AutoSellTask.State.FAILED&&!failureSession.active()&&stopped[0]==1,"native command exception never escapes tick or strands Farming hold");
  x=ready();x.port.menu=new MenuSnapshot(1,4,9,"Booster Cookie",54,List.of());x.task.tick(2*S);check(x.port.closed==0&&x.task.state()==AutoSellTask.State.FAILED,"unexpected shape is not closed merely because its title/container match");
  x=ready();x.receipts.observe(1,4,54,empty);x.port.menu=menu(empty,2);x.task.tick(2*S);x.port.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU,AutomationContext.Blocker.FOCUS));x.task.tick(3*S);check(x.task.active()&&x.port.closed==0&&x.session.holdsFarming(),"temporary focus loss defers completion/close/resume rather than acting in the background");x.port.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));x.task.tick(4*S);check(x.task.state()==AutoSellTask.State.COMPLETE,"restored focus can safely finish owned task");
  System.out.println("AutoSellTaskTest: PASS (empty authorization, typed NPC menu, protected sale, server-only receipt, unknown menu, timeout, revocable Farming hold)");
 }
}
