// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.Set;
/** Exact user-authorized SkyBlock IDs only; protection always wins over inclusion. */
public record InventoryProtection(Set<String> allowedIds,Set<String> protectedUuids){
 public InventoryProtection{allowedIds=Set.copyOf(allowedIds);protectedUuids=Set.copyOf(protectedUuids);}
 public boolean permits(ItemSnapshot item){return item!=null&&item.identified()&&!item.protectedItem()&&!protectedUuids.contains(item.uuid())&&allowedIds.contains(item.skyblockId());}
}
