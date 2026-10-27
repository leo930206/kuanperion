package dev.kuan.kuanperion.automation;
import java.util.*;
import static dev.kuan.kuanperion.automation.BookCombineTaskTest.*;
public final class BookCarriedResultTest {
 static final class H {
  final Fake f=new Fake();final InventoryReceipts items=new InventoryReceipts();final CursorReceipts cursors=new CursorReceipts();final BookCombinePlanner planner=new BookCombinePlanner(Map.of("harvesting",5),Set.of());final BookCombinePlanner.Plan pair;
  final InventoryProtection rules=new InventoryProtection(Set.of("ENCHANTED_BOOK"),Set.of());final InventoryReceipts.Stamp since;final CursorReceipts.Stamp cursorSince;final BookCombinePlanner.Book result=book(-1,5,"result",1);boolean owned=true;
  final BookReturnTransfer transfer=new BookReturnTransfer(items,cursors,()->owned);
  H(){f.open(null);pair=planner.select(new ArrayList<>(f.inventory.values())).orElseThrow();f.inventory.clear();since=items.stamp(1);cursorSince=cursors.stamp(1,4,0);}
  BookReturnTransfer.Plan plan(){return BookReturnTransfer.carriedResult(f.view(),f.inventorySnapshot(),planner,pair,result,9,rules,since,cursorSince).orElseThrow();}
  void proof(boolean first,boolean second,boolean cursor){if(first)items.observe(1,4,20,empty());if(second)items.observe(1,4,24,empty());if(cursor)cursors.observe(1,4,result.item(),S);}
  boolean begin(){return transfer.beginCarried(plan(),f.view(),f.inventorySnapshot(),result.item(),S);}
  void send(){check(transfer.armNext(f.view(),f.inventorySnapshot(),result.item(),S),"one exact empty main deposit");check(transfer.ticket().slot()==54&&transfer.step()==BookReturnTransfer.Step.DEPOSIT&&transfer.armedStillMatches(f.view(),f.inventorySnapshot(),result.item(),S),"no extra pickup or hotbar target");check(transfer.sent(GuardedMenuAction.Result.SENT,S),"sent once");}
 }
 public static void main(String[]args){
  var h=new H();h.proof(true,true,true);check(h.begin(),"fresh post-combine carried result after both consumed inputs");h.send();var returned=book(9,5,"result",1);h.f.inventory.put(9,returned);h.transfer.poll(h.f.view(),h.f.inventorySnapshot(),empty(),false,2*S);check(h.transfer.state()==BookReturnTransfer.State.AWAITING,"predicted destination/empty physical cursor is not completion");h.items.observe(1,4,54,returned.item());h.cursors.observe(1,4,empty(),2*S);h.transfer.poll(h.f.view(),h.f.inventorySnapshot(),empty(),false,2*S);check(h.transfer.state()==BookReturnTransfer.State.AWAITING,"canonical destination required too");h.items.observe(1,0,9,returned.item());h.transfer.poll(h.f.view(),h.f.inventorySnapshot(),empty(),false,3*S);check(h.transfer.confirmedStillMatches(h.f.view(),h.f.inventorySnapshot(),empty(),3*S),"full43/two destination aliases/fresh empty cursor before release");
  for(int missing=0;missing<3;missing++){h=new H();h.proof(missing!=0,missing!=1,missing!=2);check(!h.begin(),"missing own input/cursor receipt cannot adopt: "+missing);}
  h=new H();h.proof(true,true,true);h.cursors.clear();check(!h.begin(),"cleared cursor epoch cannot be borrowed");
  h=new H();h.proof(true,true,false);h.cursors.observe(1,5,h.result.item(),S);check(!h.begin(),"wrong container never authorizes deposit");
  h=new H();h.proof(true,true,true);h.owned=false;check(!h.begin(),"revoked root never adopts a cursor");
  h=new H();h.proof(true,true,true);check(h.begin(),"drift setup");h.f.extra.put(40,AutoSellTaskTest.wheat(1));check(!h.transfer.armNext(h.f.view(),h.f.inventorySnapshot(),h.result.item(),S),"equipment drift no deposit");
  h=new H();h.proof(true,true,true);check(h.begin(),"manual setup");h.transfer.poll(h.f.view(),h.f.inventorySnapshot(),h.result.item(),true,S);check(h.transfer.state()==BookReturnTransfer.State.UNCERTAIN,"manual takeover never repairs/deposits");
  h=new H();h.proof(true,true,true);check(h.begin(),"uncertain setup");check(h.transfer.armNext(h.f.view(),h.f.inventorySnapshot(),h.result.item(),S),"arm");check(!h.transfer.sent(GuardedMenuAction.Result.UNCERTAIN,S)&&!h.transfer.armNext(h.f.view(),h.f.inventorySnapshot(),h.result.item(),2*S),"uncertain deposit never retries");
  h=new H();h.f.placed.put(13,h.result);check(BookReturnTransfer.carriedResult(h.f.view(),h.f.inventorySnapshot(),h.planner,h.pair,h.result,9,h.rules,h.since,h.cursorSince).isEmpty(),"external result plus carried result is ambiguous");
  h=new H();check(BookReturnTransfer.carriedResult(h.f.view(),h.f.inventorySnapshot(),h.planner,h.pair,book(-1,4,"wrong",1),9,h.rules,h.since,h.cursorSince).isEmpty(),"only actual expected output metadata/level can be adopted");check(BookReturnTransfer.carriedResult(h.f.view(),h.f.inventorySnapshot(),h.planner,h.pair,h.result,0,h.rules,h.since,h.cursorSince).isEmpty(),"never target hotbar");
  System.out.println("BookCarriedResultTest: PASS (post-combine own cursor/input proof, direct explicit main deposit, full43/two aliases, no guess/repair/replay)");
 }
}
