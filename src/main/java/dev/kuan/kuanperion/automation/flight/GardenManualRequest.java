// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.flight;
import dev.kuan.kuanperion.automation.AutomationContext;
/** One transient UI intent, not a gameplay lease or saved automation preference. */
public final class GardenManualRequest {
 private boolean pending,farming;private long world,started;
 public boolean pending(){return pending;}
 public boolean offer(AutomationContext context,boolean occupied,long now){
  if(pending||occupied||!valid(context))return false;
  pending=true;world=context.worldId();farming=context.farmingActive();started=now;return true;
 }
 public boolean take(AutomationContext context,boolean occupied,boolean screenClosed,long now){
  if(!pending)return false;long elapsed=now-started;
  if(elapsed<0||elapsed>=30_000_000_000L||occupied||!valid(context)||context.worldId()!=world||context.farmingActive()!=farming){clear();return false;}
  if(!screenClosed)return false;clear();return true;
 }
 public boolean clear(){boolean previous=pending;pending=false;return previous;}
 private static boolean valid(AutomationContext context){return context!=null&&context.canIssueCommand()&&context.garden();}
}
