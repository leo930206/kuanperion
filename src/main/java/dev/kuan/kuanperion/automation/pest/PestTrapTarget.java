// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;
import java.util.regex.Pattern;
/** A marker is only a candidate for an interaction; it cannot authorize a menu click by itself. */
public record PestTrapTarget(int entityId,UUID uuid,int trapId,String family,PestTargets.Point position) {
 private static final Set<String> FAMILIES=Set.of("Pest Trap","Mouse Trap","Vermin Trap");
 private static final Pattern NAME=Pattern.compile("(Pest Trap|Mouse Trap|Vermin Trap) #([1-9]|1[0-9]|2[0-4])");
 public PestTrapTarget {Objects.requireNonNull(uuid);Objects.requireNonNull(position);if(entityId<0||trapId<1||trapId>24||!FAMILIES.contains(family))throw new IllegalArgumentException("Invalid trap target");}
 public String label(){return family+" #"+trapId;}
 public static Optional<PestTrapTarget> read(int id,UUID uuid,String label,PestTargets.Point position){
  if(label==null||label.length()>64||id<0||uuid==null||position==null)return Optional.empty();var m=NAME.matcher(label);return m.matches()?Optional.of(new PestTrapTarget(id,uuid,Integer.parseInt(m.group(2)),m.group(1),position)):Optional.empty();
 }
 public static List<PestTrapTarget> unique(List<PestTrapTarget> candidates){
  if(candidates==null||candidates.size()>24)return List.of();var ids=new HashSet<Integer>();var entities=new HashSet<Integer>();var uuids=new HashSet<UUID>();
  for(var t:candidates)if(t==null||!ids.add(t.trapId())||!entities.add(t.entityId())||!uuids.add(t.uuid()))return List.of();return List.copyOf(candidates);
 }
}
