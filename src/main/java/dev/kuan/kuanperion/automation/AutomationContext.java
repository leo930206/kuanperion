// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.Set;
/** Immutable observation, not permission to alter the legacy automation state. */
public record AutomationContext(long worldId,boolean farmingActive,boolean garden,Set<Blocker> blockers) {
 public enum Blocker {DISCONNECTED,DEAD,FOCUS,OUTSIDE_SKYBLOCK,MINING,RECOVERY,SELECTION,MENU,INVENTORY_FULL}
 public AutomationContext {blockers=Set.copyOf(blockers);}
 public boolean canIssueCommand(){return worldId>0&&blockers.isEmpty();}
 public boolean hardBlocked(){return worldId<=0||blockers.stream().anyMatch(b->b!=Blocker.FOCUS&&b!=Blocker.MENU&&b!=Blocker.INVENTORY_FULL);}
}
