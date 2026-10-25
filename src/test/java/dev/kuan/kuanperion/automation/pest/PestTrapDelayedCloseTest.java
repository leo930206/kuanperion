package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Regression timeline: a server may leave the release menu visible for another tick before closing. */
public final class PestTrapDelayedCloseTest {
 public static void main(String[]args){
  var x=new PestTrapReleaseTest.H();x.start();x.f.menu=PestTrapReleaseTest.menu(7,"Pest Trap",List.of(new MenuSnapshot.Slot(4,PestTrapReleaseTest.button("Release All Pests"))));x.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));
  x.session.poll(PestTrapReleaseTest.S);x.task.tick(PestTrapReleaseTest.S);
  x.session.poll(2*PestTrapReleaseTest.S);x.task.tick(2*PestTrapReleaseTest.S);
  if(!x.session.holdsFarming())throw new AssertionError("fixture must hold original Farm while server release is pending");
  // Applied server close; current adapter lacks a typed close observation path. Native HEAD/TAIL proof must be added, not a generic null-menu exception.
  var close=x.task.beforeServerClose(7,3*PestTrapReleaseTest.S);if(close==null)throw new AssertionError("matching server HEAD must issue a scoped ticket");
  x.f.menu=null;x.f.ctx=new AutomationContext(1,true,true,Set.of());
  if(!x.task.afterServerClose(close,3*PestTrapReleaseTest.S)||x.task.afterServerClose(close,3*PestTrapReleaseTest.S))throw new AssertionError("server TAIL proof must be one-use");
  x.session.poll(3*PestTrapReleaseTest.S);x.task.tick(3*PestTrapReleaseTest.S);
  if(!x.session.active()||!x.session.holdsFarming())throw new AssertionError("DELAYED_SERVER_CLOSE_LOST_ROOT: verified pending release must retain the original hold after an actual server close; manual/local close must still cancel");
  var manual=new PestTrapReleaseTest.H();manual.start();manual.f.menu=PestTrapReleaseTest.menu(8,"Pest Trap",List.of(new MenuSnapshot.Slot(4,PestTrapReleaseTest.button("Release All Pests"))));manual.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));manual.session.poll(PestTrapReleaseTest.S);manual.task.tick(PestTrapReleaseTest.S);manual.session.poll(2*PestTrapReleaseTest.S);manual.f.menu=null;manual.f.ctx=new AutomationContext(1,true,true,Set.of());manual.session.poll(3*PestTrapReleaseTest.S);manual.task.tick(3*PestTrapReleaseTest.S);if(manual.session.active())throw new AssertionError("manual/local close must still cancel, no generic null-menu exemption");
  var rejected=new PestTrapReleaseTest.H();rejected.start();rejected.f.menu=PestTrapReleaseTest.menu(9,"Pest Trap",List.of(new MenuSnapshot.Slot(4,PestTrapReleaseTest.button("Release All Pests"))));rejected.f.ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));rejected.session.poll(PestTrapReleaseTest.S);rejected.task.tick(PestTrapReleaseTest.S);rejected.session.poll(2*PestTrapReleaseTest.S);
  if(rejected.task.beforeServerClose(10,3*PestTrapReleaseTest.S)!=null)throw new AssertionError("wrong packet ID must not issue a ticket");
  var ignored=rejected.task.beforeServerClose(9,3*PestTrapReleaseTest.S);if(ignored==null||rejected.task.afterServerClose(ignored,3*PestTrapReleaseTest.S))throw new AssertionError("visible menu proves server close was not applied");
  rejected.f.menu=null;rejected.f.ctx=new AutomationContext(1,true,true,Set.of());if(rejected.task.afterServerClose(ignored,3*PestTrapReleaseTest.S))throw new AssertionError("rejected TAIL consumed its ticket, later local absence cannot replay it");
  rejected.session.poll(3*PestTrapReleaseTest.S);if(rejected.session.active())throw new AssertionError("unproved disappearance remains cancellation");
  var stale=new PestTrapReleaseTest.H();stale.start();stale.f.menu=PestTrapReleaseTest.menu(11,"Pest Trap",List.of(new MenuSnapshot.Slot(4,PestTrapReleaseTest.button("Release All Pests"))));stale.task.tick(PestTrapReleaseTest.S);var old=stale.task.beforeServerClose(11,2*PestTrapReleaseTest.S);stale.session.invalidate();stale.f.menu=null;stale.f.ctx=new AutomationContext(1,true,true,Set.of());if(stale.task.afterServerClose(old,3*PestTrapReleaseTest.S))throw new AssertionError("revoked root ticket never clears a new task's menu ownership");
  System.out.println("PestTrapDelayedCloseTest: PASS (delayed exact server HEAD/TAIL preserves original hold, opaque one-use ticket, local close still cancels)");
 }
}
