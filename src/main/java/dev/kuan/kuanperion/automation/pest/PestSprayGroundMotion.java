// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
/** Pinned LivingEntity.travelInAir: after collision y=0, then subtract gravity and apply float drag.
 * Only an exactly matching downward residual on native-supported ordinary ground is exempt from raw speed.
 * Repeated actual position stability and every original pose/receipt/native identity guard still apply. */
public final class PestSprayGroundMotion {
 private PestSprayGroundMotion(){}
 public static boolean residual(boolean support,boolean exceptional,double gravity,double modifier,boolean discard,double vy){
  if(!support||exceptional||!Double.isFinite(gravity)||gravity<=0||!Double.isFinite(modifier)||!Double.isFinite(vy)||vy>=0)return false;
  float drag=Math.max(0F,Math.min(1F,1F-(1F-.98F)*(float)modifier));
  double expected=-gravity*(discard?1D:(double)drag);
  return Double.isFinite(expected)&&vy==expected;
 }
}
