// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.supercraft;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.BooleanSupplier;
/** Read-only one-attempt proof ledger. Caller MUST separately verify actual recipe, exact quantity and cost before a guarded native click. No GUI/command authority lives here. */
public final class SupercraftReceipts {
 public enum State{IDLE,ARMED,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 private static final long SECOND=1_000_000_000L,DEADLINE=15*SECOND;
 private final InventoryReceipts items;private final CursorReceipts cursors;private final BooleanSupplier owns;
 private State state=State.IDLE;private SupercraftPlan.Plan plan;private MenuSnapshot menu;private InventoryReceipts.Stamp stamp;private CursorReceipts.Stamp cursorStamp;private long armedAt,sentAt;
 public SupercraftReceipts(InventoryReceipts items,CursorReceipts cursors,BooleanSupplier owns){this.items=Objects.requireNonNull(items);this.cursors=Objects.requireNonNull(cursors);this.owns=Objects.requireNonNull(owns);}
 public State state(){return state;}
 public boolean arm(SupercraftPlan.Plan plan,MenuSnapshot menu,Map<Integer,ItemSnapshot> current,boolean physicalCursorEmpty,long now){
  if(state!=State.IDLE||plan==null||!SupercraftPlan.stillMatches(plan,current)||menu==null||menu.worldId()<=0||menu.containerId()<1||menu.containerId()>255||menu.containerSlots()<9||menu.containerSlots()>54||menu.containerSlots()%9!=0||!physicalCursorEmpty||!owns.getAsBoolean())return false;
  for(int i=0;i<36;i++){var slot=menu.slot(menu.containerSlots()+i);if(slot==null||!slot.playerInventory()||!slot.item().equals(plan.baseline().get(i<27?i+9:i-27)))return false;}
  this.plan=plan;this.menu=menu;stamp=items.stamp(menu.worldId());cursorStamp=cursors.stamp(menu.worldId(),menu.containerId(),now);armedAt=now;state=State.ARMED;return true;
 }
 /** Record the actual guarded native result, never treat an estimate/preview as an issued craft. */
 public boolean sent(GuardedMenuAction.Result result,long now){
  if(state!=State.ARMED)return false;
  if(now-armedAt<0||now-armedAt>=SECOND||!owns.getAsBoolean()){state=State.UNCERTAIN;return false;}
  state=result==GuardedMenuAction.Result.SENT?State.AWAITING:result==GuardedMenuAction.Result.REJECTED?State.REJECTED:State.UNCERTAIN;sentAt=now;
  // Native caller records SENT on the client thread before any server packet can be applied.
  if(state==State.AWAITING){stamp=items.stamp(menu.worldId());cursorStamp=cursors.stamp(menu.worldId(),menu.containerId(),now);}return state==State.AWAITING;
 }
 public void poll(long world,Map<Integer,ItemSnapshot> current,boolean physicalCursorEmpty,long now){
  if(state!=State.ARMED&&state!=State.AWAITING)return;
  if(!owns.getAsBoolean()||world!=menu.worldId()||now-armedAt<0||state==State.ARMED&&now-armedAt>=SECOND||state==State.AWAITING&&(now-sentAt<0||now-sentAt>=DEADLINE)){state=State.UNCERTAIN;return;}
  if(state!=State.AWAITING||!physicalCursorEmpty||!cursors.emptyAfter(cursorStamp,now,DEADLINE)||!SupercraftPlan.complete(current))return;
  var baseline=plan.baseline();String output=plan.recipe().outputId();
  for(int slot=0;slot<43;slot++){
   var old=baseline.get(slot);var next=current.get(slot);if(old.equals(next))continue;
   if(slot<9||slot>=36)return;
   if(old.count()>0&&!old.skyblockId().equals(output)&&!plan.inputs().containsKey(old.skyblockId()))return;
   if(old.count()>0&&old.skyblockId().equals(output)&&(!next.skyblockId().equals(output)||!sameIdentity(old,next)||next.count()<old.count()))return;
   if(next.count()>0){
    if(!next.identified()||next.protectedItem()||!next.uuid().isEmpty())return;
    if(next.skyblockId().equals(output)){if(next.count()>plan.recipe().stackLimit())return;}
    else if(!plan.inputs().containsKey(next.skyblockId())||!sameIdentity(old,next)||next.count()>old.count())return;
   }
   var active=items.after(stamp,menu.containerId(),menu.containerSlots()+slot-9);var vanilla=items.after(stamp,0,slot);
   if(!sameApplied(active,next)&&!sameApplied(vanilla,next))return;
  }
  for(var need:plan.inputs().entrySet())if(delta(baseline,current,need.getKey())!=-need.getValue())return;
  if(delta(baseline,current,output)!=plan.outputCount())return;
  state=State.CONFIRMED;
 }
 public void cancel(){if(state==State.ARMED||state==State.AWAITING)state=State.UNCERTAIN;else if(state==State.IDLE)state=State.REJECTED;}
 private static int delta(Map<Integer,ItemSnapshot> before,Map<Integer,ItemSnapshot> after,String id){int result=0;for(int slot=9;slot<36;slot++){if(id.equals(before.get(slot).skyblockId()))result-=before.get(slot).count();if(id.equals(after.get(slot).skyblockId()))result+=after.get(slot).count();}return result;}
 private static boolean sameIdentity(ItemSnapshot a,ItemSnapshot b){return a!=null&&b!=null&&a.skyblockId().equals(b.skyblockId())&&a.minecraftId().equals(b.minecraftId())&&a.uuid().equals(b.uuid())&&a.name().equals(b.name())&&a.fingerprint().equals(b.fingerprint())&&a.lore().equals(b.lore());}
 private static boolean sameApplied(ItemSnapshot a,ItemSnapshot b){return sameIdentity(a,b)&&a.count()==b.count();}
}
