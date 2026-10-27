package dev.kuan.kuanperion.automation;
import java.util.*;import java.math.BigDecimal;import static dev.kuan.kuanperion.automation.GuardedMenuAction.Result.*;
/** Declared GUI proof and real parent session/ledgers, not a native menu/live claim. */
public final class BazaarPurchaseAttemptTest {
 static final long S=1_000_000_000L;static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","",0,List.of(),true);
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static ItemSnapshot item(String id,int n){return n==0?AIR:new ItemSnapshot(id,"minecraft:"+id.toLowerCase(Locale.ROOT),"",id,"native-"+id,n,List.of(),false);}
 static Map<Integer,ItemSnapshot> inv(){var m=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<43;i++)m.put(i,AIR);return m;}
 static MenuSnapshot menu(Map<Integer,ItemSnapshot> inv,int revision){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<54;i++)slots.add(new MenuSnapshot.Slot(i,i==13?item("WHEAT",1):AIR,false));for(int i=0;i<36;i++)slots.add(new MenuSnapshot.Slot(54+i,inv.get(i<27?i+9:i-27),true));return new MenuSnapshot(1,7,revision,"Declared Bazaar Confirm",54,slots);}
 static class H {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursors=new CursorReceipts();final SpendingBudget budget=new SpendingBudget(100);final AutomationSession session=new AutomationSession(new AutomationCoordinator(),()->ctx,()->{},()->{});final AutomationSession.Handle root;final CursorReceipts.OpeningStamp opening;final BazaarPurchaseAttempt task;Map<Integer,ItemSnapshot> inv=inv();MenuSnapshot m=menu(inv,1);int clicks;
  H(AutomationFeature owner){this(owner,true);}H(AutomationFeature owner,boolean farming){ctx=new AutomationContext(1,farming,true,Set.of());root=session.begin(owner,0,60*S);check(root!=null,"real parent");task=new BazaarPurchaseAttempt(session,items,cursors,budget,()->ctx);opening=cursors.openingStamp(1,0);cursors.observe(1,7,AIR,S);}
  BazaarPurchaseAttempt.Frame frame(long now){return new BazaarPurchaseAttempt.Frame(m.worldId(),now,inv,m,true);}
  boolean arm(){var c=new MenuContract(Set.of("Declared Bazaar Confirm"),54);var o=new BazaarPurchaseAttempt.Offer(new BazaarPurchaseLedger.Order("WHEAT","Wheat",3,new BigDecimal("14.00")),"minecraft:wheat",64,100);return task.arm(root,o,c,c.plan(m,13,MenuContract.Click.PICKUP),opening,frame(S),S);}
  boolean send(){return task.send(frame(S),S,()->{clicks++;return SENT;});}
  void delivery(){inv.put(9,item("WHEAT",3));m=menu(inv,2);items.observe(1,0,9,inv.get(9));items.observe(1,7,54,inv.get(9));cursors.observe(1,7,AIR,2*S);}
 }
 static void run(String mode){var owner=switch(mode){case "owner"->AutomationFeature.BOOK_COMBINE;case "pest-success"->AutomationFeature.PEST;case "composter-success"->AutomationFeature.COMPOSTER;case "supercraft-success","standalone-success"->AutomationFeature.SUPERCRAFT;default->AutomationFeature.VISITOR;};var h=new H(owner,!mode.equals("standalone-success"));
  if(mode.equals("owner")){check(!h.arm()&&h.budget.available()==100,"only purchasing parent");return;}
  if(mode.equals("external-shape")){var slots=new ArrayList<>(h.m.slots());slots.set(0,new MenuSnapshot.Slot(200,AIR,false));h.m=new MenuSnapshot(1,7,1,h.m.title(),54,slots);check(!h.arm(),"all external slots must exist, not only total count");return;}
  if(mode.equals("player-button")){var slots=new ArrayList<>(h.m.slots());slots.set(13,new MenuSnapshot.Slot(13,item("WHEAT",1),true));h.m=new MenuSnapshot(1,7,1,h.m.title(),54,slots);check(!h.arm(),"a player slot cannot be a purchase button");return;}
  if(mode.equals("frame-world")){h.m=new MenuSnapshot(2,7,1,h.m.title(),54,h.m.slots());check(!h.arm(),"frame must match leased world, not just internally consistent menu");return;}
  if(mode.equals("no-opening")){h.cursors.clear();check(!h.arm(),"server opening proof required, physical empty alone not authority");return;}
  if(mode.equals("protected")){h.inv.put(0,item("WHEAT",1));h.m=menu(h.inv,1);check(!h.arm(),"same-ID protected hotbar veto");return;}
  if(mode.equals("capacity")){for(int i=9;i<36;i++)h.inv.put(i,item("STONE",64));h.m=menu(h.inv,1);check(!h.arm(),"explicit empty main capacity");return;}
  check(h.arm()&&h.task.state()==BazaarPurchaseAttempt.State.ARMED&&h.budget.available()==100&&h.clicks==0,"arm no effect");
  if(mode.equals("server-cursor")){h.cursors.observe(1,7,item("STONE",1),S);check(!h.send()&&h.clicks==0&&h.budget.available()==100,"latest server carried item vetoes locally predicted empty cursor");return;}
  if(mode.equals("pre-drift")){h.inv.put(10,item("STONE",1));h.m=menu(h.inv,2);check(!h.send()&&h.clicks==0&&h.budget.available()==100,"drift before send");return;}
  if(mode.equals("rejected")||mode.equals("uncertain")){boolean noSend=mode.equals("rejected");check(!h.task.send(h.frame(S),S,()->noSend?REJECTED:UNCERTAIN)&&h.budget.available()==(noSend?100:86)&&h.task.state()==(noSend?BazaarPurchaseAttempt.State.REJECTED:BazaarPurchaseAttempt.State.UNCERTAIN),"native result holds/releases only known money");return;}
  check(h.send()&&h.clicks==1&&h.budget.available()==86,"single reserved click");h.delivery();if(!mode.equals("no-chat"))h.task.onSystemMessage(1,"[Bazaar] Bought 3x Wheat for 12.50 coins!");
  switch(mode){
   case "extra"->{h.inv.put(9,item("WHEAT",4));h.m=menu(h.inv,2);h.items.observe(1,0,9,h.inv.get(9));h.items.observe(1,7,54,h.inv.get(9));}
   case "partial-success"->{h.inv.put(9,item("WHEAT",1));h.m=menu(h.inv,2);h.items.observe(1,0,9,h.inv.get(9));h.items.observe(1,7,54,h.inv.get(9));h.task.poll(h.frame(2*S),2*S);check(h.task.state()==BazaarPurchaseAttempt.State.AWAITING&&h.budget.spent()==0,"partial delivery waits without retry");h.delivery();}
   case "chat-only"->{h.inv=inv();h.m=menu(h.inv,2);}
   case "no-active"->{h.items.clear();h.items.observe(1,0,9,h.inv.get(9));}
   case "no-canonical"->{h.items.clear();h.items.observe(1,7,54,h.inv.get(9));}
   case "no-cursor"->h.cursors.clear();
   case "physical-cursor"->{h.task.poll(new BazaarPurchaseAttempt.Frame(1,2*S,h.inv,h.m,false),2*S);check(h.task.state()!=BazaarPurchaseAttempt.State.CONFIRMED,"physical cursor");return;}
   case "unrelated"->{h.inv.put(10,item("STONE",1));h.m=menu(h.inv,2);}
   case "equipment"->{h.inv.put(40,item("STONE",1));h.m=menu(h.inv,2);}
   case "hotbar"->{h.inv.put(9,AIR);h.inv.put(0,item("WHEAT",3));h.m=menu(h.inv,2);}
   case "wrong-id"->{h.inv.put(9,new ItemSnapshot("WHEAT","minecraft:diamond","","Wheat","wrong",3,List.of(),false));h.m=menu(h.inv,2);h.items.observe(1,0,9,h.inv.get(9));h.items.observe(1,7,54,h.inv.get(9));}
   case "world"->h.ctx=new AutomationContext(2,true,true,Set.of());
   case "farm"->h.ctx=new AutomationContext(1,false,true,Set.of());
   case "focus"->h.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.FOCUS));
   case "root"->{h.session.cancel(2*S);h.session.begin(AutomationFeature.PEST,2*S,60*S);}
   case "deadline"->{h.task.poll(h.frame(16*S),16*S);check(h.task.state()==BazaarPurchaseAttempt.State.UNCERTAIN&&h.budget.available()==86,"deadline no refund");return;}
   case "cancel"->{h.task.cancel();check(h.task.state()==BazaarPurchaseAttempt.State.UNCERTAIN&&h.budget.available()==86&&h.session.isCurrent(h.root),"cancel no parent release or refund");return;}
  }
  h.task.poll(h.frame(2*S),2*S);
  if(mode.endsWith("success")){check(h.task.state()==BazaarPurchaseAttempt.State.CONFIRMED&&h.budget.spent()==13&&h.budget.available()==87&&h.session.isCurrent(h.root),"exact all43/double receipts/cursor + GAME cost commits once under same parent");h.task.poll(h.frame(3*S),3*S);check(h.budget.spent()==13&&!h.send()&&h.clicks==1,"no replay");}
  else check(h.task.state()!=BazaarPurchaseAttempt.State.CONFIRMED&&h.budget.spent()==0&&h.budget.available()==86&&h.clicks==1,"not confirmed: "+mode);
 }
 public static void main(String[] args){for(var m:List.of("external-shape","player-button","no-opening","server-cursor","pest-success","composter-success","supercraft-success","standalone-success","partial-success","no-chat","extra","frame-world","owner","protected","capacity","pre-drift","rejected","uncertain","chat-only","no-active","no-canonical","no-cursor","physical-cursor","unrelated","equipment","hotbar","wrong-id","world","farm","focus","root","deadline","cancel","success"))run(m);System.out.println("BazaarPurchaseAttemptTest: PASS (real parent/ledgers, protected43, exact native/main delivery, both aliases/cursor, GAME cost, budgets, cancellation/no replay; no current GUI/native/live claim)");}
}
