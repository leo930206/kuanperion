// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.*;
/** Server-applied carried-item evidence. Never authority to move an item or repair a user's cursor. */
public final class CursorReceipts {
 public record Stamp(long world,int container,long epoch,long sequence,long requestedAt){}
 public record Receipt(long world,int container,long sequence,ItemSnapshot item,long observedAt){}
 private long world,epoch=1,sequence;private int container=-1;private Receipt latest;
 private void scope(long world,int container){if(this.world!=world||this.container!=container){clear();this.world=world;this.container=container;}}
 public Stamp stamp(long world,int container,long now){if(world<=0||container<0||container>255)throw new IllegalArgumentException("Invalid cursor scope");scope(world,container);return new Stamp(world,container,epoch,sequence,now);}
 public void observe(long world,int container,ItemSnapshot item,long now){
  if(world<=0||container<0||container>255){clear();return;}scope(world,container);sequence++;latest=null;
  if(item==null||item.count()>4096||item.fingerprint().length()>65536||item.lore().size()>128||item.lore().stream().anyMatch(line->line.length()>512))return;
  if(item.count()==0&&(!item.minecraftId().equals("minecraft:air")||!item.skyblockId().isEmpty()||!item.uuid().isEmpty()))return;
  latest=new Receipt(world,container,sequence,item,now);
 }
 public Optional<Receipt> after(Stamp stamp,long now,long maxAge){
  if(maxAge<=0||maxAge>30_000_000_000L)throw new IllegalArgumentException("Invalid cursor receipt age");
  if(stamp==null||latest==null||stamp.epoch()!=epoch||stamp.world()!=world||stamp.container()!=container||latest.sequence()<=stamp.sequence())return Optional.empty();
  long age=now-latest.observedAt();return age>=0&&age<=maxAge&&latest.observedAt()-stamp.requestedAt()>=0?Optional.of(latest):Optional.empty();
 }
 public boolean emptyAfter(Stamp stamp,long now,long maxAge){return after(stamp,now,maxAge).filter(r->r.item().count()==0&&r.item().minecraftId().equals("minecraft:air")).isPresent();}
 public int cached(){return latest==null?0:1;}
 public void clear(){world=0;container=-1;epoch++;sequence=0;latest=null;}
}
