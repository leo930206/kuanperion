// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.LinkedHashMap;
import java.util.Map;
/** Only server packet observations enter this ledger; client-predicted mutations are not receipts. */
public final class InventoryReceipts {
 public record Stamp(long world,long sequence){}
 private record Key(int container,int slot){}
 private record Receipt(long sequence,ItemSnapshot item){}
 private long world,sequence;
 private final Map<Key,Receipt> latest=new LinkedHashMap<>(){protected boolean removeEldestEntry(Map.Entry<Key,Receipt> entry){return size()>1024;}};
 public Stamp stamp(long world){return new Stamp(world,sequence);}
 public void observe(long observedWorld,int container,int slot,ItemSnapshot item){
  if(observedWorld<=0||container<0||container>255||slot<0||slot>511||item==null)return;
  if(observedWorld!=world){world=observedWorld;latest.clear();}latest.put(new Key(container,slot),new Receipt(++sequence,item));
 }
 public ItemSnapshot after(Stamp stamp,int container,int slot){var receipt=latest.get(new Key(container,slot));return stamp!=null&&stamp.world()==world&&receipt!=null&&receipt.sequence()>stamp.sequence()?receipt.item():null;}
 public boolean confirmsDecrease(Stamp stamp,int container,int slot,ItemSnapshot before,int amount){
  if(before==null||amount<=0||amount>before.count())return false;var after=after(stamp,container,slot);if(after==null||after.count()!=before.count()-amount)return false;
  return after.count()==0||before.fingerprint().equals(after.fingerprint())&&before.skyblockId().equals(after.skyblockId())&&before.uuid().equals(after.uuid());
 }
 /** Window 0 player inventory slots 9..44 only; caller freezes this complete baseline before click. */
 public boolean confirmsIncrease(Stamp stamp,Map<Integer,ItemSnapshot> before,String id,int amount){
  if(stamp==null||before==null||before.size()!=36||id==null||id.isBlank()||amount<=0||amount>4096)return false;
  long delta=0;boolean fresh=false;
  for(int slot=9;slot<=44;slot++){
   var old=before.get(slot);if(old==null)return false;var next=after(stamp,0,slot);if(next==null)continue;
   fresh=true;if(id.equals(next.skyblockId()))delta+=next.count();if(id.equals(old.skyblockId()))delta-=old.count();
  }
  return fresh&&delta==amount;
 }
 public void clear(){world=0;latest.clear();sequence++;}
}
