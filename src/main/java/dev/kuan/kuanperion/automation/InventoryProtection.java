// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.Set;
/** Exact user-authorized SkyBlock IDs only; protection always wins over inclusion. */
public record InventoryProtection(Set<String> allowedIds,Set<String> protectedUuids){
 public InventoryProtection{allowedIds=Set.copyOf(allowedIds);protectedUuids=protectedUuids.stream().map(InventoryProtection::normalizeUuid).collect(java.util.stream.Collectors.toUnmodifiableSet());}
 public boolean permits(ItemSnapshot item){return item!=null&&item.identified()&&!item.protectedItem()&&!protectedUuids.contains(normalizeUuid(item.uuid()))&&allowedIds.contains(item.skyblockId());}
 private static String normalizeUuid(String uuid){return uuid.replace("-","").toLowerCase(java.util.Locale.ROOT);}
}
