// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.greenhouse;
import java.util.*;
/** One watched target, server updates only. Caller must also prove its mature target actually changed; this signal alone is never success. */
public final class HarvestReceipts {
 public record Stamp(long world,long generation,GreenhouseTarget.Ref target,long requestedAt){}
 private long generation,lastAt;private Stamp current;private boolean received;
 public Stamp watch(long world,GreenhouseTarget.Ref target,long now){
  if(world<=0||target==null)throw new IllegalArgumentException("Invalid harvest watch");clear();current=new Stamp(world,generation,target,now);return current;
 }
 public Optional<GreenhouseTarget.Ref> watched(){return current==null?Optional.empty():Optional.of(current.target());}
 public void entityChanged(long world,int id,long now){
  if(current==null)return;if(current.world()!=world){clear();return;}
  if(current.target() instanceof GreenhouseTarget.EntityRef ref&&ref.id()==id)changed(now);
 }
 public void blockChanged(long world,int x,int y,int z,long now){
  if(current==null)return;if(current.world()!=world){clear();return;}
  if(current.target() instanceof GreenhouseTarget.BlockRef ref&&ref.x()==x&&ref.y()==y&&ref.z()==z)changed(now);
 }
 private void changed(long now){received=now-current.requestedAt()>=0;lastAt=now;}
 public boolean after(Stamp stamp,long now){long age=now-lastAt;return stamp!=null&&stamp.equals(current)&&received&&age>=0&&age<=2_000_000_000L;}
 public void clear(){current=null;received=false;lastAt=0;generation++;}
}
