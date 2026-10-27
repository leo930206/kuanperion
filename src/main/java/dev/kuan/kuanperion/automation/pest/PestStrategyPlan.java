// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;
/** Read-only crop/event selection. Purpose adapted from GPL Dynamic Pest policy; see THIRD_PARTY_NOTICES. No live mapping, feed or action authority. */
public final class PestStrategyPlan {
 public enum Mode{OFF,FEAST,JACOB,JACOB_THEN_FEAST}
 public enum State{UNKNOWN,INACTIVE,ACTIVE}
 public enum Time{UNCHANGED,DAY,NIGHT}
 public enum Origin{FEAST,JACOB,FALLBACK}
 public enum Status{DISABLED,WAITING,NO_AUTHORIZED_CROP,SELECTED,NO_ACTION}
 /** Exact explicitly configured target IDs; empty means leave that particular setting unchanged. */
 public record Profile(String sprayId,String vinylId,Time time){
  public Profile{Objects.requireNonNull(sprayId);Objects.requireNonNull(vinylId);Objects.requireNonNull(time);if(!optionalId(sprayId)||!optionalId(vinylId)||sprayId.isEmpty()&&vinylId.isEmpty()&&time==Time.UNCHANGED)throw new IllegalArgumentException("No explicit strategy target");}
 }
 public record Rules(Mode mode,List<String> feastPriority,List<String> jacobPriority,Map<String,Profile> profiles,Optional<Profile> fallback){
  public Rules{Objects.requireNonNull(mode);feastPriority=List.copyOf(feastPriority);jacobPriority=List.copyOf(jacobPriority);profiles=Map.copyOf(profiles);Objects.requireNonNull(fallback);
   if(profiles.size()>16||profiles.keySet().stream().anyMatch(k->!id(k))||!priority(feastPriority,profiles)||!priority(jacobPriority,profiles))throw new IllegalArgumentException("Invalid explicit crop priorities");
  }
  public static Rules defaults(){return new Rules(Mode.OFF,List.of(),List.of(),Map.of(),Optional.empty());}
  public boolean configured(){return mode!=Mode.OFF&&(fallback.isPresent()||switch(mode){case FEAST->!feastPriority.isEmpty();case JACOB->!jacobPriority.isEmpty();case JACOB_THEN_FEAST->!jacobPriority.isEmpty()||!feastPriority.isEmpty();default->false;});}
 }
 /** The future server/approved-data adapter supplies receipt-time facts. Missing is not inactive. Never persisted/replayed. */
 public record Facts(long world,long observedAt,State state,List<String> crops){
  public Facts{Objects.requireNonNull(state);crops=List.copyOf(crops);if(world<=0||crops.size()>16||new HashSet<>(crops).size()!=crops.size()||crops.stream().anyMatch(k->!id(k))||state==State.ACTIVE&&crops.isEmpty()||state!=State.ACTIVE&&!crops.isEmpty())throw new IllegalArgumentException("Invalid crop/event evidence");}
  boolean fresh(long expectedWorld,long now){long age=now-observedAt;return world==expectedWorld&&expectedWorld>0&&age>=0&&age<=30_000_000_000L&&state!=State.UNKNOWN;}
 }
 public static final class Selection {
  private final Origin origin;private final String cropId;private final Profile profile;
  private Selection(Origin origin,String cropId,Profile profile){this.origin=origin;this.cropId=cropId;this.profile=profile;}
  public Origin origin(){return origin;}public String cropId(){return cropId;}public Profile profile(){return profile;}
 }
 public record Result(Status status,Optional<Selection> selection){public Result{Objects.requireNonNull(status);Objects.requireNonNull(selection);if((status==Status.SELECTED)!=selection.isPresent())throw new IllegalArgumentException("Invalid strategy decision");}}
 /** Necessary target planning only. A native consumer still needs fresh root/menu/item/cost/receipt proofs for every effect. */
 public static Result select(Rules rules,Facts feast,Facts jacob,long world,long now){
  if(rules==null||!rules.configured())return result(Status.DISABLED);
  return switch(rules.mode()){
   case FEAST->event(rules,feast,rules.feastPriority(),Origin.FEAST,world,now);
   case JACOB->event(rules,jacob,rules.jacobPriority(),Origin.JACOB,world,now);
   case JACOB_THEN_FEAST->{
    if(jacob==null||!jacob.fresh(world,now))yield result(Status.WAITING);
    if(jacob.state()==State.ACTIVE)yield event(rules,jacob,rules.jacobPriority(),Origin.JACOB,world,now);
    yield event(rules,feast,rules.feastPriority(),Origin.FEAST,world,now);
   }
   default->result(Status.DISABLED);
  };
 }
 private static Result event(Rules rules,Facts facts,List<String> priority,Origin origin,long world,long now){
  if(facts==null||!facts.fresh(world,now))return result(Status.WAITING);
  if(facts.state()==State.ACTIVE){for(String crop:priority)if(facts.crops().contains(crop))return new Result(Status.SELECTED,Optional.of(new Selection(origin,crop,rules.profiles().get(crop))));return result(Status.NO_AUTHORIZED_CROP);}
  return rules.fallback().isPresent()?new Result(Status.SELECTED,Optional.of(new Selection(Origin.FALLBACK,"",rules.fallback().get()))):result(Status.NO_ACTION);
 }
 private static Result result(Status status){return new Result(status,Optional.empty());}
 private static boolean priority(List<String> values,Map<String,Profile> profiles){return values.size()<=16&&new HashSet<>(values).size()==values.size()&&values.stream().allMatch(v->id(v)&&profiles.containsKey(v));}
 private static boolean optionalId(String value){return value.isEmpty()||id(value);}
 private static boolean id(String value){return value!=null&&value.matches("[A-Z0-9_:-]{1,100}");}
 private PestStrategyPlan(){}
}
