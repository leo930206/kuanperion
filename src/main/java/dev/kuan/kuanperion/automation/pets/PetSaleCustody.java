// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pets;
import dev.kuan.kuanperion.automation.*;import java.util.*;import java.util.function.*;
/** Proves the original authorized pet entered source-candidate George13 before any sale quote is accepted. */
public final class PetSaleCustody {
 public enum State{IDLE,SENDING,AWAITING,CONFIRMED,REJECTED,UNCERTAIN}
 /** No public constructor: native display/quote data cannot manufacture a custody authorization. */
 public static final class Token {
  private final AutomationSession.Handle owner;private final PetSalePreflight.Pet pet;private final PetSaleAttempt.Frame deposited;private final int inputSlot;private final long confirmedAt;private boolean claimed;
  private Token(AutomationSession.Handle owner,PetSalePreflight.Pet pet,PetSaleAttempt.Frame frame,int inputSlot,long now){this.owner=owner;this.pet=pet;deposited=frame;this.inputSlot=inputSlot;confirmedAt=now;}
  public PetSalePreflight.Pet pet(){return pet;}int inputSlot(){return inputSlot;}
  boolean claim(AutomationSession.Handle h,PetSaleAttempt.Frame f,long now){
   if(claimed||!owner.equals(h)||now-confirmedAt<0||now-confirmedAt>=15_000_000_000L||!deposited.inventory().equals(f.inventory())||f.menu().worldId()!=deposited.menu().worldId()||f.menu().containerId()!=deposited.menu().containerId()||!f.menu().title().equals(deposited.menu().title())||!PetSaleAttempt.applied(pet.item(),f.menu().item(inputSlot)))return false;claimed=true;return true;
  }
 }
 private final AutomationSession session;private final AutomationCoordinator coordinator;private final InventoryReceipts items;private final CursorReceipts cursors;private final Supplier<AutomationContext> observe;
 private State state=State.IDLE;private AutomationSession.Handle owner;private PetSalePreflight.Pet pet;private PetSaleAttempt.Frame before;private InventoryReceipts.Stamp stamp;private CursorReceipts.OpeningStamp cursorStamp;private long started;private Token token;
 public PetSaleCustody(AutomationSession session,AutomationCoordinator coordinator,InventoryReceipts items,CursorReceipts cursors,Supplier<AutomationContext> observe){this.session=session;this.coordinator=coordinator;this.items=items;this.cursors=cursors;this.observe=observe;}
 public State state(){return state;}public Optional<Token> token(){return Optional.ofNullable(token);}
 public boolean start(AutomationSession.Handle h,PetSalePreflight.Plan plan,PetSalePreflight.Pet pet,int inputSlot,PetSaleAttempt.Frame frame,InventoryReceipts.Stamp opening,CursorReceipts.OpeningStamp cursorOpening,long now,Supplier<GuardedMenuAction.Result> transfer){
  if(state!=State.IDLE||h==null||pet==null||plan==null||transfer==null||inputSlot!=13||!plan.pets().contains(pet)||pet.slot()<9||pet.slot()>=36||!PetSaleAttempt.fresh(frame,now)||!frame.cursorEmpty()||!PetSaleAttempt.complete(frame.inventory())||!PetSaleAttempt.aliases(frame)||!PetSalePreflight.stillMatches(plan,frame.inventory())||!Set.of("George","Sell Pets","Pet Collector","Offer Pets").contains(frame.menu().title())||!PetSaleAttempt.air(frame.menu().item(13))||!root(h,frame.menu().worldId(),now)||state!=State.IDLE)return false;
  int source=frame.menu().containerSlots()+pet.slot()-9;
  if(!cursors.emptyAfterOpening(cursorOpening,frame.menu().containerId(),now,1_000_000_000L)||!PetSaleAttempt.applied(items.after(opening,frame.menu().containerId(),13),frame.menu().item(13))||!PetSaleAttempt.applied(items.after(opening,frame.menu().containerId(),source),pet.item()))return false;
  owner=h;this.pet=pet;before=frame;started=now;state=State.SENDING;GuardedMenuAction.Result result;
  try{result=transfer.get();}catch(RuntimeException uncertain){result=GuardedMenuAction.Result.UNCERTAIN;}
  if(state!=State.SENDING)return false;
  boolean owns=root(h,frame.menu().worldId(),now);if(state!=State.SENDING)return false;
  if(!owns){state=State.UNCERTAIN;return false;}
  if(result!=GuardedMenuAction.Result.SENT){state=result==GuardedMenuAction.Result.REJECTED?State.REJECTED:State.UNCERTAIN;return false;}
  stamp=items.stamp(frame.menu().worldId());cursorStamp=cursors.openingStamp(frame.menu().worldId(),now);state=State.AWAITING;return true;
 }
 public void poll(PetSaleAttempt.Frame frame,long now){
  if(state!=State.AWAITING)return;boolean owns=root(owner,before.menu().worldId(),now);if(state!=State.AWAITING)return;
  if(!owns||now-started<0||now-started>=15_000_000_000L||!PetSaleAttempt.fresh(frame,now)||!PetSaleAttempt.complete(frame.inventory())||!PetSaleAttempt.aliases(frame)||frame.menu().worldId()!=before.menu().worldId()||!(frame.menu().containerId()==before.menu().containerId()&&frame.menu().title().equals(before.menu().title())||frame.menu().title().equals("Offer Pets"))){state=State.UNCERTAIN;return;}
  for(int i=0;i<43;i++)if(!before.inventory().get(i).equals(frame.inventory().get(i))&&(i!=pet.slot()||!PetSaleAttempt.air(frame.inventory().get(i)))){state=State.UNCERTAIN;return;}
  if(!PetSaleAttempt.air(frame.inventory().get(pet.slot()))||!PetSaleAttempt.applied(pet.item(),frame.menu().item(13))||!frame.cursorEmpty()||!cursors.emptyAfterOpening(cursorStamp,frame.menu().containerId(),now,15_000_000_000L))return;
  for(var slot:frame.menu().slots())if(slot.index()!=13&&pet.item().uuid().equalsIgnoreCase(slot.item().uuid()))return;
  int alias=frame.menu().containerSlots()+pet.slot()-9;
  if(!PetSaleAttempt.applied(items.after(stamp,0,pet.slot()),frame.inventory().get(pet.slot()))||!PetSaleAttempt.applied(items.after(stamp,frame.menu().containerId(),alias),frame.inventory().get(pet.slot()))||!PetSaleAttempt.applied(items.after(stamp,frame.menu().containerId(),13),frame.menu().item(13)))return;
  token=new Token(owner,pet,frame,13,now);state=State.CONFIRMED;
 }
 private boolean root(AutomationSession.Handle h,long world,long now){
  if(h==null||h.lease().owner()!=AutomationFeature.PET_SELL||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||h.lease().worldId()!=world||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;
  AutomationContext c;try{c=observe.get();}catch(RuntimeException unavailable){return false;}
  return session.isCurrent(h)&&coordinator.holdsIdentity(h.lease())&&c!=null&&c.worldId()==world&&(h.hold()==null||c.farmingActive())&&c.blockers().stream().allMatch(b->b==AutomationContext.Blocker.MENU||b==AutomationContext.Blocker.INVENTORY_FULL);
 }
 public void cancel(){if(state==State.IDLE)state=State.REJECTED;else if(state==State.SENDING||state==State.AWAITING)state=State.UNCERTAIN;}
}
