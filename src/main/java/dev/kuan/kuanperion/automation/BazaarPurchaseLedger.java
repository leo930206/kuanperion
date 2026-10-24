// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import java.math.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** One irreversible purchase, not a GUI driver. A quote is not a cost/quantity proof. */
public final class BazaarPurchaseLedger {
 public enum State {IDLE,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 public record Order(String productId,String displayName,int quantity,BigDecimal maximumCost){
  public Order{
   Objects.requireNonNull(productId);Objects.requireNonNull(displayName);Objects.requireNonNull(maximumCost);
   if(!productId.matches("[A-Z0-9_:-]{1,100}")||!displayName.matches("[A-Za-z0-9][A-Za-z0-9 '\\-:()+]{0,95}")||quantity<1||quantity>4096||maximumCost.signum()<=0||maximumCost.scale()>2)throw new IllegalArgumentException("Invalid purchase proof");
   maximumCost.setScale(0,RoundingMode.CEILING).longValueExact();
  }
 }
 private static final String INTEGER="(?:[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+)";
 private static final Pattern RECEIPT=Pattern.compile("\\[Bazaar] Bought ("+INTEGER+")x (.{1,96}) for ((?:0|"+INTEGER+")(?:\\.[0-9]{1,2})?) coins!");
 private final SpendingBudget budget;private final InventoryReceipts receipts;private final BooleanSupplier owns;
 private State state=State.IDLE;private Order order;private SpendingBudget.Reservation reservation;private InventoryReceipts.Stamp stamp;
 private Map<Integer,ItemSnapshot> baseline;private BigDecimal paid;private long world,started;
 public BazaarPurchaseLedger(SpendingBudget budget,InventoryReceipts receipts,BooleanSupplier owns){this.budget=budget;this.receipts=receipts;this.owns=owns;}
 public State state(){return state;}
 /** Supply only a GUI-validated exact product/amount/cost and a guarded native click, never an API quote. */
 public boolean begin(Order proof,long world,long purse,Map<Integer,ItemSnapshot> baseline,long now,Supplier<GuardedMenuAction.Result> guardedClick){
  if(state!=State.IDLE||world<=0||!owns.getAsBoolean()||!complete(baseline))return false;
  reservation=budget.reserve(proof.maximumCost().setScale(0,RoundingMode.CEILING).longValueExact(),purse);
  if(reservation==null){state=State.REJECTED;return false;}
  this.order=proof;this.world=world;this.baseline=Map.copyOf(baseline);stamp=receipts.stamp(world);started=now;state=State.AWAITING;
  try{
   var result=guardedClick.get();if(result==GuardedMenuAction.Result.SENT)return true;
   if(result==GuardedMenuAction.Result.REJECTED){budget.release(reservation);state=State.REJECTED;}
   else state=State.UNCERTAIN;
  }catch(RuntimeException ambiguous){state=State.UNCERTAIN;}
  return false;
 }
 /** Only the adapter's server GAME channel may call this; never signed player CHAT/local feedback. */
 public void onSystemMessage(long observedWorld,String message){
  if(state!=State.AWAITING||world!=observedWorld||!owns.getAsBoolean()||message==null||message.length()>512)return;
  var matcher=RECEIPT.matcher(message.replaceAll("(?i)§[0-9a-fk-orx]",""));
  if(!matcher.matches()||!order.displayName().equals(matcher.group(2)))return;
  try{
   if(Integer.parseInt(matcher.group(1).replace(",",""))!=order.quantity())return;
   var coins=new BigDecimal(matcher.group(3).replace(",",""));
   if(coins.compareTo(order.maximumCost())>0){state=State.UNCERTAIN;return;}
   if(coins.signum()<=0)return;
   if(paid!=null&&paid.compareTo(coins)!=0){state=State.UNCERTAIN;return;}
   paid=coins;
  }catch(NumberFormatException invalid){}
 }
 public void poll(long now){
  if(state!=State.AWAITING)return;
  if(!owns.getAsBoolean()||now-started<0||now-started>=15_000_000_000L){state=State.UNCERTAIN;return;}
  if(paid!=null&&receipts.confirmsIncrease(stamp,baseline,order.productId(),order.quantity())){
   state=budget.commit(reservation,paid.setScale(0,RoundingMode.CEILING).longValueExact())?State.CONFIRMED:State.UNCERTAIN;
  }
 }
 /** A sent/ambiguous purchase is never refunded just because the task stopped or a receipt was late. */
 public void cancel(){if(state==State.AWAITING)state=State.UNCERTAIN;else if(state==State.IDLE)state=State.REJECTED;}
 private static boolean complete(Map<Integer,ItemSnapshot> items){if(items==null||items.size()!=36)return false;for(int i=9;i<=44;i++)if(items.get(i)==null)return false;return true;}
}
