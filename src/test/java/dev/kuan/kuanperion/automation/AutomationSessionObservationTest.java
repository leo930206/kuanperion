package dev.kuan.kuanperion.automation;
import java.util.*;
/** Observation failure/reentrancy must revoke only its original root, never clear a newer owner. */
public final class AutomationSessionObservationTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class H {
  boolean farm=true,visible,throwContext,nullContext,onceContext,throwMenu,stopThrows;int stops;Runnable contextHook,menuHook;
  MenuSnapshot menu=new MenuSnapshot(1,7,1,"Observed",9,List.of(new MenuSnapshot.Slot(0,new ItemSnapshot("","minecraft:paper","","role","role",1,List.of(),false))));
  AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::observe,()->{},()->{stops++;farm=false;if(stopThrows)throw new IllegalStateException("stop callback failure");});AutomationSession.Handle h,newer;
  AutomationContext observe(){if(contextHook!=null){var r=contextHook;contextHook=null;r.run();}if(throwContext||onceContext){onceContext=false;throw new IllegalStateException("context unavailable");}if(nullContext)return null;return new AutomationContext(1,farm,true,visible?Set.of(AutomationContext.Blocker.MENU):Set.of());}
  MenuSnapshot read(){if(menuHook!=null){var r=menuHook;menuHook=null;r.run();}if(throwMenu)throw new IllegalStateException("menu unavailable");return visible?menu:null;}
  void start(AutomationFeature feature){h=session.begin(feature,0,120_000_000_000L);check(h!=null,"original root");}
  void claim(){session.expectMenu(h,new MenuContract(Set.of("Observed"),9),this::read);visible=true;session.poll(1);check(session.isCurrent(h),"valid original claimed menu");}
  void replace(){session.invalidate();visible=false;newer=session.begin(AutomationFeature.AUTO_SELL,3,60_000_000_000L);check(newer!=null,"new owner");}
 }
 public static void main(String[]args){
  for(var feature:List.of(AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.VISITOR,AutomationFeature.AUTO_SELL,AutomationFeature.JUNK,AutomationFeature.SUPERCRAFT)){
   var h=new H();h.start(feature);h.throwContext=true;h.session.poll(2);check(!h.session.active()&&!h.session.holdsFarming()&&h.stops==1,"unavailable context revokes original hold without escaping "+feature);
   h=new H();h.farm=false;h.start(feature);h.nullContext=true;h.session.poll(2);check(!h.session.active()&&h.stops==0,"standalone failure never invents a Farm to stop "+feature);
  }
  var h=new H();h.start(AutomationFeature.PEST);h.onceContext=true;h.session.poll(2);check(!h.session.active()&&h.stops==1,"single failing poll followed by healthy finish context still stops original");
  h=new H();h.start(AutomationFeature.PEST);h.claim();h.throwMenu=true;h.session.poll(2);check(!h.session.active()&&h.stops==1,"owned menu read exception cannot strand a hold");
  h=new H();h.start(AutomationFeature.PEST);h.claim();var phase=new MenuContract(Set.of("Next"),9);check(h.session.transitionMenu(h.h,h.menu,new MenuContract(Set.of("Observed"),9),phase,2,1_000_000_000L),"declared transition");h.throwMenu=true;h.session.poll(3);check(!h.session.active()&&h.stops==1,"transition menu read failure revokes original root");
  h=new H();h.start(AutomationFeature.PEST);var ref=h;h.contextHook=()->ref.replace();h.session.poll(3);check(h.session.isCurrent(h.newer)&&h.stops==0,"reentrant context cannot clobber newly acquired owner");
  h=new H();h.start(AutomationFeature.PEST);h.claim();var menuRef=h;h.menuHook=()->menuRef.replace();h.session.poll(3);check(h.session.isCurrent(h.newer)&&h.stops==0,"old menu read return cannot cancel new owner");
  h=new H();h.start(AutomationFeature.PEST);h.claim();var failedRef=h;h.menuHook=()->{failedRef.replace();throw new IllegalStateException("old read failed after owner changed");};h.session.poll(3);check(h.session.isCurrent(h.newer)&&h.stops==0,"old observation exception cannot cancel a new root");
  h=new H();h.start(AutomationFeature.PEST);h.claim();var rebind=h;h.menuHook=()->rebind.session.expectMenu(rebind.h,new MenuContract(Set.of("Next"),9),()->null);h.session.poll(3);check(h.session.isCurrent(h.h)&&h.stops==0,"old read cannot validate or cancel a newly rebound contract");h.session.poll(4);check(!h.session.active()&&h.stops==1,"new contract still checked on its own next poll");
  h=new H();h.start(AutomationFeature.PEST);h.claim();var transitionRef=h;h.menuHook=()->{transitionRef.replace();transitionRef.visible=true;};check(!h.session.transitionMenu(h.h,h.menu,new MenuContract(Set.of("Observed"),9),new MenuContract(Set.of("Next"),9),3,1_000_000_000L)&&h.session.isCurrent(h.newer),"old transition read cannot install bindings on a newer root");
  h=new H();h.start(AutomationFeature.PEST);h.claim();h.throwMenu=true;check(!h.session.transitionMenu(h.h,h.menu,new MenuContract(Set.of("Observed"),9),new MenuContract(Set.of("Next"),9),3,1_000_000_000L)&&!h.session.active()&&h.stops==1,"transition registration read failure safely revokes original root");
  h=new H();h.start(AutomationFeature.PEST);var same=h;var contract=new MenuContract(Set.of("Observed"),9);java.util.function.Supplier<MenuSnapshot> reader=same::read;same.session.expectMenu(same.h,contract,reader);same.visible=true;same.session.poll(1);same.menuHook=()->{same.session.expectMenu(same.h,contract,reader);same.visible=false;};same.session.poll(3);check(same.session.isCurrent(same.h)&&same.stops==0,"even same object/supplier rebind invalidates an old menu observation");
  h=new H();h.start(AutomationFeature.PEST);h.throwContext=true;h.stopThrows=true;try{h.session.poll(2);throw new AssertionError("stop failure swallowed");}catch(IllegalStateException expected){}check(!h.session.active()&&!h.session.holdsFarming()&&h.stops==1,"root revoked before stop callback; stop failures not silently hidden");
  System.out.println("AutomationSessionObservationTest: PASS (all seven shared-root services, null/throwing context/menu/transition, original-only hold revocation, reentrant newer owner/contract protection, stop failure not swallowed)");
 }
}
