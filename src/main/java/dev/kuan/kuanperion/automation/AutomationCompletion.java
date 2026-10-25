// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.Objects;
/** Dispose all owned work before any successful Farming release. Failure-only physical reconciliation follows legacy stop, never precedes it. */
public final class AutomationCompletion {
 public record Result(boolean completed,boolean failed){}
 private AutomationCompletion(){}
 public static Result finish(AutomationSession session,AutomationSession.Handle handle,boolean success,long now,Runnable release,Runnable dispose,Runnable afterStop){
  Objects.requireNonNull(session);Objects.requireNonNull(release);Objects.requireNonNull(dispose);Objects.requireNonNull(afterStop);
  boolean failed=false,completed=false;
  try{release.run();}catch(RuntimeException cleanup){failed=true;}
  try{dispose.run();}catch(RuntimeException cleanup){failed=true;}
  try{completed=session.finish(handle,success&&!failed,now);}catch(RuntimeException cleanup){failed=true;}
  if(!completed)try{afterStop.run();}catch(RuntimeException cleanup){failed=true;}
  return new Result(completed,failed||success&&!completed);
 }
}
