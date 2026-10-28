// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import java.util.Objects;
/** Finish all explicitly listed stop callbacks, then expose the original runtime failure. No retry or gameplay authority. */
public final class AutomationStops {
 private AutomationStops(){}
 public static void run(Runnable... stops){
  Objects.requireNonNull(stops);RuntimeException first=null;
  for(var stop:stops)try{stop.run();}catch(RuntimeException failure){
   if(first==null)first=failure;else if(first!=failure)first.addSuppressed(failure);
  }
  if(first!=null)throw first;
 }
}
