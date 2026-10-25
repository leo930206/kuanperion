// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;import java.util.regex.Pattern;
/** One inventory-backed irreversible submission protocol, not an executable GUI/task driver. */
public final class VisitorSubmission {
 public enum State{IDLE,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 private static final long DEADLINE=15_000_000_000L;
 private final InventoryReceipts receipts;private final CursorReceipts cursors;private final BooleanSupplier owns;
 private State state=State.IDLE;private VisitorOffer offer;private Map<Integer,ItemSnapshot> baseline;private InventoryReceipts.Stamp stamp;private CursorReceipts.Stamp cursorStamp;private long started;private boolean accepted;
 public VisitorSubmission(InventoryReceipts receipts,CursorReceipts cursors,BooleanSupplier owns){this.receipts=Objects.requireNonNull(receipts);this.cursors=Objects.requireNonNull(cursors);this.owns=Objects.requireNonNull(owns);}
 public State state(){return state;}
 /** Inventory0..35 plus pinned26.2 equipment36..42; the caller must snapshot all43 together. No purchase is performed. */
 public boolean begin(VisitorOffer offer,VisitorValuation.Rules rules,Map<Integer,ItemSnapshot> inventory,BazaarMarket market,long wallMillis,long now,boolean cursorEmpty,Supplier<GuardedMenuAction.Result> guardedClick){
  if(state!=State.IDLE||offer==null||guardedClick==null||!owns.getAsBoolean()||!cursorEmpty||!complete(inventory)||!offer.rewards().contains("Click to give!"))return false;
  var main=new HashMap<Integer,ItemSnapshot>();for(int i=0;i<36;i++)main.put(i,inventory.get(i));var value=VisitorValuation.estimate(offer,rules,main,market,wallMillis);
  if(value.isEmpty()||!value.get().missing().isEmpty()||!safeInventory(offer,inventory)||!matchesMenu(offer.menu(),inventory))return false;
  this.offer=offer;baseline=Map.copyOf(inventory);stamp=receipts.stamp(offer.menu().worldId());cursorStamp=cursors.stamp(offer.menu().worldId(),offer.menu().containerId(),now);started=now;state=State.AWAITING;
  try{var result=guardedClick.get();if(result==GuardedMenuAction.Result.SENT)return true;state=result==GuardedMenuAction.Result.REJECTED?State.REJECTED:State.UNCERTAIN;}
  catch(RuntimeException ambiguous){state=State.UNCERTAIN;}return false;
 }
 /** Adapter must call only for the server GAME channel, never player CHAT or local feedback. */
 public void onSystemMessage(long world,String message){
  if(state!=State.AWAITING||world!=offer.menu().worldId()||!owns.getAsBoolean()||message==null||message.length()>512)return;
  String clean=message.replaceAll("(?i)§[0-9a-fk-orx]","");
  accepted|=clean.matches("OFFER ACCEPTED with "+Pattern.quote(offer.visitor())+" \\((?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL)\\)");
 }
 public void poll(long world,Map<Integer,ItemSnapshot> current,boolean cursorEmpty,long now){
  if(state!=State.AWAITING)return;
  if(world!=offer.menu().worldId()||!owns.getAsBoolean()||now-started<0||now-started>=DEADLINE){state=State.UNCERTAIN;return;}
  if(!accepted||!cursorEmpty||!cursors.emptyAfter(cursorStamp,now,DEADLINE)||!complete(current)||!safeInventory(offer,current))return;
  for(int slot=0;slot<43;slot++){
   var old=baseline.get(slot);if(old.count()>0&&(slot<9||slot>=36||old.protectedItem()||!old.uuid().isEmpty())&&!old.equals(current.get(slot)))return;
  }
  for(var need:offer.requirements()){
   int delta=0;
   for(int slot=9;slot<36;slot++){
    var old=baseline.get(slot);var next=current.get(slot);boolean oldId=old.skyblockId().equals(need.itemId()),newId=next.skyblockId().equals(need.itemId());
    if(oldId)delta-=old.count();if(newId)delta+=next.count();
    if((oldId||newId)&&!sameApplied(old,next)){
     var active=receipts.after(stamp,offer.menu().containerId(),offer.menu().containerSlots()+slot-9);var vanilla=receipts.after(stamp,0,slot);
     if(!sameApplied(active,next)&&!sameApplied(vanilla,next))return;
    }
   }
   if(delta!=-need.quantity())return;
  }
  state=State.CONFIRMED;
 }
 public void cancel(){if(state==State.AWAITING)state=State.UNCERTAIN;else if(state==State.IDLE)state=State.REJECTED;}
 private static boolean complete(Map<Integer,ItemSnapshot> m){if(m==null||m.size()!=43)return false;for(int i=0;i<43;i++){var item=m.get(i);if(item==null||item.count()>64)return false;}return true;}
 /** Server chooses consumption order: any same-ID hotbar/equipment/protected stack vetoes the whole attempt. */
 private static boolean safeInventory(VisitorOffer offer,Map<Integer,ItemSnapshot> items){
  for(var need:offer.requirements())for(int i=0;i<43;i++){var item=items.get(i);if(item.count()>0&&item.skyblockId().equals(need.itemId())&&(i<9||i>=36||item.protectedItem()||!item.uuid().isEmpty()))return false;}return true;
 }
 private static boolean matchesMenu(MenuSnapshot menu,Map<Integer,ItemSnapshot> items){
  for(int i=0;i<36;i++){var slot=menu.slot(menu.containerSlots()+i);int inv=i<27?i+9:i-27;if(slot==null||!slot.playerInventory()||!slot.item().equals(items.get(inv)))return false;}return true;
 }
 /** Protection is locally derived, not a server component; every actual identity/count field must match. */
 private static boolean sameApplied(ItemSnapshot a,ItemSnapshot b){return a!=null&&b!=null&&a.skyblockId().equals(b.skyblockId())&&a.minecraftId().equals(b.minecraftId())&&a.uuid().equals(b.uuid())&&a.name().equals(b.name())&&a.fingerprint().equals(b.fingerprint())&&a.count()==b.count()&&a.lore().equals(b.lore());}
}
