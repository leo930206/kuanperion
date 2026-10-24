// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.List;
import java.util.Objects;
/** Immutable server item observation; display names/lore alone never authorize disposal. */
public record ItemSnapshot(String skyblockId,String minecraftId,String uuid,String name,String fingerprint,int count,List<String> lore,boolean protectedItem){
 public ItemSnapshot{Objects.requireNonNull(skyblockId);Objects.requireNonNull(minecraftId);Objects.requireNonNull(uuid);Objects.requireNonNull(name);Objects.requireNonNull(fingerprint);lore=List.copyOf(lore);if(count<0)throw new IllegalArgumentException("Negative count");}
 public boolean identified(){return count>0&&!skyblockId.isBlank()&&!fingerprint.isBlank();}
}
