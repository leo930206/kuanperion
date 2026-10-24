package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutoSellTaskTest {
 static final long S=1000000000L;
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemSnapshot wheat(int count){return new ItemSnapshot("WHEAT","minecraft:wheat","","Wheat","wheat-meta",count,List.of(),false);}
 static final class Fake implements AutoSellTask.Port {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());MenuSnapshot menu;int commands,sales,closed;boolean accepted=true;
  public AutomationContext context(){return ctx;}public MenuSnapshot menu(){return menu;}
  public List<ItemSnapshot> inventory(){return List.of(wheat(64));}
  public boolean command(AutomationSession.Handle h,AutomationCommand c){check(c==AutomationCommand.BOOSTER_COOKIE,"only exact allowed command");commands++;return accepted;}
  public boolean sell(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,InventoryProtection rules,long now){check(rules.permits(ticket.observed().item(ticket.slot()))&&ticket.observed().slot(ticket.slot()).playerInventory(),"exact authorized inventory item only");sales++;return true;}
  public void closeOwnedMenu(int id){if(menu!=null&&menu.containerId()==id){closed++;menu=null;}}
 }
 static MenuSnapshot menu(ItemSnapshot item,int state){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<90;i++)slots.add(new MenuSnapshot.Slot(i,i==54?item:new ItemSnapshot("","minecraft:air","","","",0,List.of(),true),i>=54));return new MenuSnapshot(1,4,state,"Booster Cookie",54,slots);}
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
  System.out.println("AutoSellTaskTest: PASS (empty authorization, typed NPC menu, protected sale, server-only receipt, unknown menu, timeout, revocable Farming hold)");
 }
}
