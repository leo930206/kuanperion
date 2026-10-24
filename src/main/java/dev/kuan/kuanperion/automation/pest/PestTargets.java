// SPDX-License-Identifier: GPL-3.0-only
// Marker/mob association examined in supplied Aether PestTargetTracker; conservative local rewrite.
package dev.kuan.kuanperion.automation.pest;

import java.util.*;

/** Read-only candidates, not permission to attack, infer a kill, fly or move. */
public final class PestTargets {
    private PestTargets(){}
    public enum Kind {ARMOR_STAND,BAT,SILVERFISH,OTHER}
    public record Point(double x,double y,double z) {
        public Point {if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30_000_000||Math.abs(y)>30_000_000||Math.abs(z)>30_000_000)throw new IllegalArgumentException("Invalid entity position");}
        public double distanceSquared(Point other){double dx=x-other.x,dy=y-other.y,dz=z-other.z;return dx*dx+dy*dy+dz*dz;}
    }
    public record Fact(int id,UUID uuid,Kind kind,Point position,boolean available,boolean invisible,boolean knownHead) {
        public Fact {Objects.requireNonNull(uuid);Objects.requireNonNull(kind);Objects.requireNonNull(position);if(id<0)throw new IllegalArgumentException("Invalid entity ID");}
    }
    public record Target(int id,UUID uuid,Kind kind,Point position,int markerId){}
    public static List<Target> select(List<Fact> entities){
        if(entities==null||entities.size()>256||entities.stream().anyMatch(Objects::isNull))return List.of();
        if(entities.stream().map(Fact::id).distinct().count()!=entities.size()||entities.stream().map(Fact::uuid).distinct().count()!=entities.size())return List.of();
        var markers=entities.stream().filter(f->f.available()&&f.kind()==Kind.ARMOR_STAND&&f.invisible()&&f.knownHead()).sorted(Comparator.comparingInt(Fact::id)).toList();
        var mobs=entities.stream().filter(f->f.available()&&(f.kind()==Kind.BAT||f.kind()==Kind.SILVERFISH)).toList();
        var targets=new LinkedHashMap<Integer,Target>();
        for(var marker:markers){
            Fact nearest=null;double distance=Double.POSITIVE_INFINITY;boolean ambiguous=false;
            for(var mob:mobs){
                double next=marker.position().distanceSquared(mob.position());if(next>4)continue;
                if(next<distance){distance=next;nearest=mob;ambiguous=false;}else if(next==distance)ambiguous=true;
            }
            var candidate=nearest==null||ambiguous?marker:nearest;
            targets.putIfAbsent(candidate.id(),new Target(candidate.id(),candidate.uuid(),candidate.kind(),candidate.position(),marker.id()));
        }
        return List.copyOf(targets.values());
    }
}
