package dev.kuan.kuanperion.automation.pest;
import java.util.*;
/** Explicit symbolic test IDs, not assertions about live spray/vinyl/time menus or crop mappings. */
public final class PestStrategyPlanTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final PestStrategyPlan.Profile W=new PestStrategyPlan.Profile("TEST_SPRAY_A","TEST_RECORD_A",PestStrategyPlan.Time.UNCHANGED),C=new PestStrategyPlan.Profile("TEST_SPRAY_B","",PestStrategyPlan.Time.DAY);
 static PestStrategyPlan.Rules rules(PestStrategyPlan.Mode mode){return new PestStrategyPlan.Rules(mode,List.of("WHEAT","CARROT_ITEM"),List.of("CARROT_ITEM","WHEAT"),Map.of("WHEAT",W,"CARROT_ITEM",C),Optional.of(W));}
 static PestStrategyPlan.Facts active(String... crops){return new PestStrategyPlan.Facts(1,S,PestStrategyPlan.State.ACTIVE,List.of(crops));}
 static PestStrategyPlan.Facts inactive(){return new PestStrategyPlan.Facts(1,S,PestStrategyPlan.State.INACTIVE,List.of());}
 static PestStrategyPlan.Facts unknown(){return new PestStrategyPlan.Facts(1,S,PestStrategyPlan.State.UNKNOWN,List.of());}
 static PestStrategyPlan.Result choose(PestStrategyPlan.Mode mode,PestStrategyPlan.Facts feast,PestStrategyPlan.Facts jacob){return PestStrategyPlan.select(rules(mode),feast,jacob,1,2*S);}
 public static void main(String[]args){
  var r=PestStrategyPlan.select(PestStrategyPlan.Rules.defaults(),active("WHEAT"),active("CARROT_ITEM"),1,2*S);check(r.status()==PestStrategyPlan.Status.DISABLED&&r.selection().isEmpty(),"default off grants no target/action");
  r=choose(PestStrategyPlan.Mode.FEAST,active("CARROT_ITEM","WHEAT"),unknown());check(r.selection().orElseThrow().cropId().equals("WHEAT")&&r.selection().orElseThrow().profile().equals(W),"explicit Feast priority, not arbitrary first active crop");
  r=choose(PestStrategyPlan.Mode.JACOB,unknown(),active("WHEAT","CARROT_ITEM"));check(r.selection().orElseThrow().cropId().equals("CARROT_ITEM"),"independent Jacob priority");
  r=choose(PestStrategyPlan.Mode.JACOB_THEN_FEAST,active("WHEAT"),active("CARROT_ITEM"));check(r.selection().orElseThrow().origin()==PestStrategyPlan.Origin.JACOB,"Jacob wins only with current known active evidence");
  r=choose(PestStrategyPlan.Mode.JACOB_THEN_FEAST,active("WHEAT"),inactive());check(r.selection().orElseThrow().origin()==PestStrategyPlan.Origin.FEAST,"known inactive Jacob permits Feast");
  r=choose(PestStrategyPlan.Mode.JACOB_THEN_FEAST,inactive(),inactive());check(r.selection().orElseThrow().origin()==PestStrategyPlan.Origin.FALLBACK&&r.selection().orElseThrow().cropId().isEmpty(),"explicit fallback only when selected relevant events are known inactive");
  for(var mode:List.of(PestStrategyPlan.Mode.FEAST,PestStrategyPlan.Mode.JACOB,PestStrategyPlan.Mode.JACOB_THEN_FEAST)){
   check(choose(mode,unknown(),unknown()).status()==PestStrategyPlan.Status.WAITING,"missing data never applies fallback: "+mode);
   check(choose(mode,active("UNAUTHORIZED"),active("UNAUTHORIZED")).status()==PestStrategyPlan.Status.NO_AUTHORIZED_CROP,"no implicit first active crop or broad fallback: "+mode);
  }
  check(choose(PestStrategyPlan.Mode.JACOB_THEN_FEAST,active("WHEAT"),unknown()).status()==PestStrategyPlan.Status.WAITING,"unknown higher-priority event does not silently yield");
  check(PestStrategyPlan.select(rules(PestStrategyPlan.Mode.FEAST),active("WHEAT"),null,2,2*S).status()==PestStrategyPlan.Status.WAITING,"world must match actual owner");
  check(PestStrategyPlan.select(rules(PestStrategyPlan.Mode.FEAST),active("WHEAT"),null,1,31*S).status()!=PestStrategyPlan.Status.WAITING,"freshness inclusive30s");
  check(PestStrategyPlan.select(rules(PestStrategyPlan.Mode.FEAST),active("WHEAT"),null,1,31*S+1).status()==PestStrategyPlan.Status.WAITING,"stale facts never act");
  check(PestStrategyPlan.select(rules(PestStrategyPlan.Mode.FEAST),active("WHEAT"),null,1,0).status()==PestStrategyPlan.Status.WAITING,"clock reversal/future observation denied");
  var inactiveOtherWorld=new PestStrategyPlan.Facts(2,S,PestStrategyPlan.State.INACTIVE,List.of());check(PestStrategyPlan.select(rules(PestStrategyPlan.Mode.FEAST),inactiveOtherWorld,null,1,2*S).status()==PestStrategyPlan.Status.WAITING,"wrong-world inactive cannot authorize fallback");
  var fallbackOnly=new PestStrategyPlan.Rules(PestStrategyPlan.Mode.FEAST,List.of(),List.of(),Map.of(),Optional.of(W));check(PestStrategyPlan.select(fallbackOnly,active("WHEAT"),null,1,2*S).status()==PestStrategyPlan.Status.NO_AUTHORIZED_CROP,"fallback is not implicit active crop authorization");
  try{new PestStrategyPlan.Profile("","",PestStrategyPlan.Time.UNCHANGED);throw new AssertionError("implicit reset/no-op profile");}catch(IllegalArgumentException expected){}
  var noFallback=new PestStrategyPlan.Rules(PestStrategyPlan.Mode.FEAST,List.of("WHEAT"),List.of(),Map.of("WHEAT",W),Optional.empty());check(PestStrategyPlan.select(noFallback,inactive(),null,1,2*S).selection().isEmpty(),"no implicit reset to default spray/record/time");
  for(var bad:List.of(List.of("WHEAT","WHEAT"),List.of("wheat"),List.of("UNKNOWN")))try{new PestStrategyPlan.Rules(PestStrategyPlan.Mode.FEAST,bad,List.of(),Map.of("WHEAT",W),Optional.empty());throw new AssertionError("bad exact priority");}catch(IllegalArgumentException expected){}
  try{new PestStrategyPlan.Facts(1,S,PestStrategyPlan.State.INACTIVE,List.of("WHEAT"));throw new AssertionError("contradictory inactive");}catch(IllegalArgumentException expected){}
  try{new PestStrategyPlan.Facts(1,S,PestStrategyPlan.State.ACTIVE,List.of("WHEAT","WHEAT"));throw new AssertionError("duplicate event crops");}catch(IllegalArgumentException expected){}
  var priorities=new ArrayList<>(List.of("WHEAT"));var profiles=new HashMap<>(Map.of("WHEAT",W));var immutable=new PestStrategyPlan.Rules(PestStrategyPlan.Mode.FEAST,priorities,List.of(),profiles,Optional.empty());priorities.clear();profiles.clear();check(PestStrategyPlan.select(immutable,active("WHEAT"),null,1,2*S).selection().isPresent(),"frozen explicit profiles/priorities");
  System.out.println("PestStrategyPlanTest: PASS (typed independent Feast/Jacob priority, fresh same-world facts, known-inactive-only explicit fallback, no-guess/off defaults; no fetch, GUI, task or live mapping claim)");
 }
}
