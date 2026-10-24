// SPDX-License-Identifier: GPL-3.0-only
// Stash signal/command behavior adapted from Aether 1.3.1-r6 bootstrap handlers;
// rewritten as a bounded client-tick state machine. See THIRD_PARTY_NOTICES.md.
package dev.kuan.kuanperion.automation;

import java.util.Locale;
import java.util.Objects;

/** Command-only task: no sleeps, threads, menu clicks, rotations, equipment or movement changes. */
public final class StashTask {
 private static final long SECOND=1_000_000_000L;
 public enum State {IDLE,WAITING,COMPLETE,BLOCKED,CANCELLED,TIMED_OUT,EXHAUSTED,FAILED}
 public enum Signal {ADDED,EMPTY,BLOCKED,NONE}
 public record Policy(int delaySeconds,int maxAttempts,int timeoutSeconds,boolean onlyWhileFarming){
  public Policy {if(delaySeconds<2||delaySeconds>60||maxAttempts<1||maxAttempts>30||timeoutSeconds<10||timeoutSeconds>300)throw new IllegalArgumentException("Invalid Stash limits");}
  public static Policy defaults(){return new Policy(3,10,60,true);}
 }
 private final AutomationCoordinator coordinator;
 private AutomationCoordinator.Lease lease;private Policy policy=Policy.defaults();
 private State state=State.IDLE;private long started,world,lastSent;private int attempts;private boolean manual,everSent;
 public StashTask(AutomationCoordinator coordinator){this.coordinator=Objects.requireNonNull(coordinator);}
 public boolean active(){return state==State.WAITING;}public State state(){return state;}public int attempts(){return attempts;}
 public boolean request(AutomationContext context,Policy policy,boolean manual,long now){
  if(active()||context.hardBlocked()||!manual&&policy.onlyWhileFarming()&&!context.farmingActive())return false;
  this.policy=Objects.requireNonNull(policy);this.manual=manual;started=now;world=context.worldId();attempts=0;lease=null;state=State.WAITING;return true;
 }
 public void tick(AutomationContext context,long now,AutomationCommand.Sender sender){
  if(!active())return;
  long elapsed=now-started;
  if(context.worldId()!=world||context.hardBlocked()||!manual&&policy.onlyWhileFarming()&&!context.farmingActive()){cancel();return;}
  if(elapsed<0||elapsed>=policy.timeoutSeconds()*SECOND){finish(State.TIMED_OUT);return;}
  if(context.blockers().contains(AutomationContext.Blocker.INVENTORY_FULL)){finish(State.BLOCKED);return;}
  if(lease!=null&&!coordinator.isOwner(lease,context,now)){finish(State.CANCELLED);return;}
  if(!context.canIssueCommand())return;
  long since=now-lastSent;
  if(everSent&&since>=0&&since<policy.delaySeconds()*SECOND)return;
  if(attempts>=policy.maxAttempts()){finish(State.EXHAUSTED);return;}
  if(lease==null)lease=coordinator.tryAcquire(AutomationFeature.STASH,context,now,Math.max(1,policy.timeoutSeconds()*SECOND-elapsed));
  if(!coordinator.permits(lease,AutomationCoordinator.Resource.COMMAND,context,now))return;
  boolean accepted;
  try{accepted=sender.send(AutomationCommand.PICKUP_STASH);}catch(RuntimeException failure){accepted=false;}
  if(!accepted){finish(State.FAILED);return;}
  lastSent=now;everSent=true;attempts++;
 }
 public void onSignal(Signal signal,AutomationContext context){if(active()&&(context.worldId()!=world||context.hardBlocked())){cancel();return;}if(active()&&signal==Signal.EMPTY)finish(State.COMPLETE);else if(active()&&signal==Signal.BLOCKED)finish(State.BLOCKED);}
 public void cancel(){if(active())finish(State.CANCELLED);}
 private void finish(State terminal){coordinator.release(lease);lease=null;state=terminal;}
 /** Only call for server GAME messages, never signed player CHAT messages. */
 public static Signal classify(String raw){
  if(raw==null||raw.length()>512)return Signal.NONE;
  String text=raw.replaceAll("(?i)§[0-9a-fk-orx]","").strip().toLowerCase(Locale.ROOT);
  // Reject local feedback and player-shaped relays; the upstream contains-match was too permissive.
  if(text.startsWith("[")||text.contains(":")||text.contains(">"))return Signal.NONE;
  if(text.equals("your stash isn't holding any items or materials!"))return Signal.EMPTY;
  if(text.startsWith("couldn't unstash your "))return Signal.BLOCKED;
  if((text.startsWith("your ")||text.matches("^[0-9][0-9,]* .*"))&&text.endsWith("stashed away!"))return Signal.ADDED;
  return Signal.NONE;
 }
}
