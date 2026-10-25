// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.*;
/** Read-only identity candidates, not a name/nearest-match interaction shortcut. */
public final class VisitorTargets {
 public enum Kind{MARKER,CHARACTER,OTHER}
 public record Fact(int id,UUID uuid,Kind kind,Point position,String name,boolean alive,boolean invisible,boolean listedPlayer){
  public Fact{Objects.requireNonNull(uuid);Objects.requireNonNull(kind);Objects.requireNonNull(position);Objects.requireNonNull(name);if(id<0||name.length()>256)throw new IllegalArgumentException("Invalid visitor fact");}
 }
 public record Target(String visitor,int id,UUID uuid,Point position,int markerId,UUID markerUuid,Point markerPosition){}
 private VisitorTargets(){}
 public static List<Target> select(VisitorQueue queue,List<Fact> facts){
  if(queue==null||!queue.known()||facts==null||facts.size()>256||facts.stream().anyMatch(Objects::isNull)||facts.stream().map(Fact::id).distinct().count()!=facts.size()||facts.stream().map(Fact::uuid).distinct().count()!=facts.size())return List.of();
  var targets=new ArrayList<Target>();
  for(var entry:queue.entries()){
   var markers=facts.stream().filter(f->f.alive()&&f.kind()==Kind.MARKER&&f.invisible()&&!f.listedPlayer()&&f.name().equals(entry.name())).toList();if(markers.size()!=1)continue;var marker=markers.getFirst();
   var characters=facts.stream().filter(f->f.alive()&&f.kind()==Kind.CHARACTER&&!f.listedPlayer()&&f.position().y()<=marker.position().y()&&f.position().distanceSquared(marker.position())<=4).toList();if(characters.size()!=1)continue;var npc=characters.getFirst();
   targets.add(new Target(entry.name(),npc.id(),npc.uuid(),npc.position(),marker.id(),marker.uuid(),marker.position()));
  }
  if(targets.stream().map(Target::id).distinct().count()!=targets.size())return List.of();return List.copyOf(targets);
 }
}
