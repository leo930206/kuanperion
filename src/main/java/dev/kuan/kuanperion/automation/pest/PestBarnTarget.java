// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;
/** Native adapter must prove the exact loaded RemotePlayer family; this record alone never grants interaction. */
public record PestBarnTarget(int entityId,UUID uuid,PestTargets.Point aim){
 public PestBarnTarget{Objects.requireNonNull(uuid);Objects.requireNonNull(aim);if(entityId<0||!PestPlot.of(0).contains(aim.x(),aim.z())||Math.abs(aim.y())>=2048)throw new IllegalArgumentException("Invalid Barn NPC target");}
 public record Facts(long world,long observedAt,boolean complete,List<PestBarnTarget> targets){
  public Facts{targets=List.copyOf(targets);if(targets.size()>32||!complete&&!targets.isEmpty())throw new IllegalArgumentException("Invalid Barn observation");}
  public boolean fresh(long w,long now){return complete&&world==w&&world>0&&now-observedAt>=0&&now-observedAt<=1_000_000_000L;}
 }
 public static Optional<PestBarnTarget> select(List<PestBarnTarget> candidates,PestTargets.Point desk){
  if(candidates==null||candidates.size()>32||desk==null||!PestPlot.of(0).contains(desk.x(),desk.z()))return Optional.empty();
  var ids=new HashSet<Integer>();var uuids=new HashSet<UUID>();PestBarnTarget result=null;
  for(var t:candidates){if(t==null||!ids.add(t.entityId())||!uuids.add(t.uuid()))return Optional.empty();if(t.aim().distanceSquared(desk)>36)continue;if(result!=null)return Optional.empty();result=t;}
  return Optional.ofNullable(result);
 }
}
