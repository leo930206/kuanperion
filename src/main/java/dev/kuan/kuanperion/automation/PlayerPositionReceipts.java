// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** Server-applied absolute positions are evidence, not ownership or permission to move. */
public final class PlayerPositionReceipts {
 public record Position(double x,double y,double z){
  public Position{if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30_000_000||Math.abs(y)>30_000_000||Math.abs(z)>30_000_000)throw new IllegalArgumentException("Invalid server position");}
 }
 public record Stamp(long world,long epoch,long sequence,long requestedAt){}
 public record Receipt(long world,long sequence,int teleportId,Position position,long observedAt){}
 private long world,epoch=1,sequence;private Receipt latest;
 // Protocol IDs are opaque, not necessarily positive/monotonic. Only a bounded replay window is kept.
 private final LinkedHashSet<Integer> recentIds=new LinkedHashSet<>();
 private void scope(long next){if(world!=next){clear();world=next;}}
 public Stamp stamp(long world,long now){if(world<=0)throw new IllegalArgumentException("Invalid world");scope(world);return new Stamp(world,epoch,sequence,now);}
 public void observe(long world,int teleportId,Position position,boolean absolute,long now){
  if(world<=0){clear();return;}scope(world);sequence++;latest=null;
  if(!recentIds.add(teleportId))return;
  if(recentIds.size()>32)recentIds.remove(recentIds.iterator().next());
  if(!absolute||position==null)return;
  latest=new Receipt(world,sequence,teleportId,position,now);
 }
 public Optional<Receipt> since(Stamp stamp,long now,long maxAge){
  if(maxAge<=0||maxAge>30_000_000_000L)throw new IllegalArgumentException("Invalid receipt age");
  if(stamp==null||stamp.world()!=world||stamp.epoch()!=epoch||latest==null||latest.sequence()<=stamp.sequence())return Optional.empty();
  long age=now-latest.observedAt(),sinceRequest=latest.observedAt()-stamp.requestedAt();
  return age>=0&&age<=maxAge&&sinceRequest>=0?Optional.of(latest):Optional.empty();
 }
 public int cachedIds(){return recentIds.size();}
 public void clear(){latest=null;recentIds.clear();world=0;sequence=0;epoch++;}
}
