// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.pest.PestSprayGroundMotion;
import net.minecraft.client.player.LocalPlayer;import net.minecraft.world.entity.ai.attributes.Attributes;import net.minecraft.world.effect.MobEffects;
/** Read-only narrow vanilla26.2 support/gravity projection. No velocity or position mutation. */
final class PestSprayGroundView {
 private PestSprayGroundView(){}
 static boolean residual(LocalPlayer p,double vy){
  if(p==null)return false;
  boolean support=p.onGround()&&p.verticalCollisionBelow;
  boolean exceptional=p.noPhysics||p.isInWater()||p.isInLava()||p.onClimbable()||p.isFallFlying()||p.getAbilities().flying||p.hasEffect(MobEffects.LEVITATION)||p.hasEffect(MobEffects.SLOW_FALLING);
  return PestSprayGroundMotion.residual(support,exceptional,p.getGravity(),p.getAttributeValue(Attributes.AIR_DRAG_MODIFIER),p.shouldDiscardFriction(),vy);
 }
}
