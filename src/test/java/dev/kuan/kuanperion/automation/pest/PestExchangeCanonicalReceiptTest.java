package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Window-0 uses raw menu slots36..44, never hotbar inventory indices0..8. Native gate checks the pinned range too. */
public final class PestExchangeCanonicalReceiptTest {
 static final class P extends PestExchangeTaskTest.P {
  final int toolSlot;P(int slot){toolSlot=slot;inv.put(3,PestExchangeTaskTest.AIR);inv.put(slot,PestExchangeTaskTest.TOOL);}
  public PestExchangeTask.Frame exchangeFrame(long now){return new PestExchangeTask.Frame(1,now,level,menu==null?null:identity,menu,inv,cursor,toolSlot);}
 }
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[]args){
  for(int slot=0;slot<9;slot++)for(String mode:List.of("both","canonical-only","active-only","wrong-canonical")){
   var p=new P(slot);var task=new PestExchangeTask(p.session,p.coordinator,p.items,p.cursors,p);check(task.begin(p.owner,true,new PestIdentity.Vacuum(slot,PestExchangeTaskTest.TOOL,10),p.now),"slot-specific owned begin");PestExchangeTaskTest.sent(p,task);
   var old=p.inv.get(slot);var next=new ItemSnapshot(old.skyblockId(),old.minecraftId(),old.uuid(),old.name(),"emptied-"+slot,1,List.of("Ability: Vacuum RIGHT CLICK","within 10 blocks","Vacuum Bag: 0"),true);p.inv.put(slot,next);p.ack(true,true);
   if(!mode.equals("active-only"))p.items.observe(1,0,mode.equals("wrong-canonical")?slot:36+slot,next);
   if(!mode.equals("canonical-only"))p.items.observe(1,7,27+27+slot,next);
   task.tick(p.now);task.tick(p.now);check(task.state()==(mode.equals("both")?PestExchangeTask.State.COMPLETE:PestExchangeTask.State.AWAITING),"exact fresh pair required: hotbar="+slot+" canonical="+(36+slot)+" active="+(54+slot)+" mode="+mode+" state="+task.state());
   check(p.clicks==1&&p.closes==(mode.equals("both")?1:0)&&p.session.isCurrent(p.owner),"no replay, no premature close or new root");
  }
  System.out.println("PestExchangeCanonicalReceiptTest: PASS all9 hotbar slots x4 canonical/active cases; pinned native range checked separately");
 }
}
