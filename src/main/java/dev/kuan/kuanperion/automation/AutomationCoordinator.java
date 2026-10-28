// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.Set;

/** Client-thread exclusive task lease. Preemption must cancel/clean up the old task first. */
public final class AutomationCoordinator {
 public enum Resource {COMMAND,MOVEMENT,LOOK,HOTBAR,SCREEN,USE,ATTACK}
 public record Lease(long token,AutomationFeature owner,long worldId,long started,long lifetime,Set<Resource> resources) {
  public Lease {resources=Set.copyOf(resources);}
 }
 private Lease active;private long nextToken;
 public Lease tryAcquire(AutomationFeature owner,AutomationContext context,long now,long lifetime){
  if(lifetime<=0||lifetime>300_000_000_000L)throw new IllegalArgumentException("Invalid task lifetime");
  validate(context,now);
  if(active!=null||!context.canIssueCommand()||owner.requiresGarden()&&!context.garden())return null;
  return active=new Lease(++nextToken,owner,context.worldId(),now,lifetime,owner.resources());
 }
 /** Passive exact identity query only: never validates/expires a lease or grants action permission. */
 public boolean holdsIdentity(Lease lease){return lease!=null&&lease.equals(active);}
 public boolean isOwner(Lease lease,AutomationContext context,long now){validate(context,now);return lease!=null&&active!=null&&active.token()==lease.token();}
 public boolean permits(Lease lease,Resource resource,AutomationContext context,long now){return isOwner(lease,context,now)&&context.canIssueCommand()&&lease.resources().contains(resource)&&(!lease.owner().requiresGarden()||context.garden());}
 public boolean release(Lease lease){if(lease==null||active==null||active.token()!=lease.token())return false;active=null;return true;}
 public void clear(){active=null;}
 private void validate(AutomationContext context,long now){
  if(active==null)return;long elapsed=now-active.started();
  if(context.hardBlocked()||active.owner().requiresGarden()&&!context.garden()||context.worldId()!=active.worldId()||elapsed<0||elapsed>=active.lifetime())active=null;
 }
}
