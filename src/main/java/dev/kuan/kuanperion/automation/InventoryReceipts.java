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
 public void clear(){world=0;latest.clear();sequence++;}
}
