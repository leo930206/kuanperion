// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
/** Pure bounded look and key proposal; no world, velocity or teleport writes. */
public final class PestSteering {
 private PestSteering(){}
 public record Input(float yaw,float pitch,boolean forward,boolean back,boolean up,boolean down){}
 public static float rotate(float current,float desired,float step,boolean wrap){
  if(!Float.isFinite(current)||!Float.isFinite(desired)||!Float.isFinite(step)||step<=0||step>30)throw new IllegalArgumentException("Invalid look");
  float delta=desired-current;if(wrap)delta=(delta%360+540)%360-180;
  return current+Math.max(-step,Math.min(step,delta));
 }
 public static Input toward(PestTargets.Point from,PestTargets.Point to,float yaw,float pitch,float step){
  return toward(from,to,new PestTargets.Point(0,0,0),yaw,pitch,step);
 }
 public static Input toward(PestTargets.Point from,PestTargets.Point to,PestTargets.Point velocity,float yaw,float pitch,float step){
  double dx=to.x()-from.x(),dy=to.y()-from.y(),dz=to.z()-from.z(),horizontal=Math.hypot(dx,dz);
  float desired=horizontal<.01?yaw:(float)Math.toDegrees(Math.atan2(-dx,dz));float next=rotate(yaw,desired,step,true);
  float error=(desired-next)%360;error=(error+540)%360-180;
  double radians=Math.toRadians(next),speed=velocity.x()*-Math.sin(radians)+velocity.z()*Math.cos(radians);
  double wanted=Math.min(.22,horizontal*.25),vertical=Math.max(-.16,Math.min(.16,dy*.3));
  boolean aligned=Math.abs(error)<12;
  boolean forward=aligned&&speed<wanted-.035&&(horizontal>.12||speed<-.035);
  boolean back=aligned&&speed>wanted+.035;
  return new Input(next,rotate(pitch,0,step,false),forward,back,velocity.y()<vertical-.035,velocity.y()>vertical+.035);
 }
}
