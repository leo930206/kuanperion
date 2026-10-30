#!/usr/bin/env python3
"""Actual read-only support projection body with physical fixtures + pinned vanilla26.2 gravity/friction bytecode.
This is not a live client physics simulation. Parent/selection model tests prove residual does not bypass position stability.
"""
from pathlib import Path
import subprocess,tempfile,re
r=Path(__file__).resolve().parents[1];p=Path.home()/'Library/Application Support/PrismLauncher';jdk=p/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';mc=p/'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'
s=(r/'src/main/java/dev/kuan/kuanperion/PestSprayGroundView.java').read_text();a=s.index(' static boolean residual(');b=s.index('{',a)+1;depth=1
while depth:depth+=(s[b]=='{')-(s[b]=='}');b+=1
body=s[a:b]
java=r'''package dev.kuan.kuanperion.automation.pest;
public class PestSprayGroundNativeTest {
 static class MobEffects{static final int LEVITATION=1,SLOW_FALLING=2;}static class Attributes{static final int AIR_DRAG_MODIFIER=1;}
 static class Abilities{boolean flying;}
 static class LocalPlayer{boolean ground=true,verticalCollisionBelow=true,noPhysics,water,lava,climb,fall,levitation,slow,discard;double gravity=.08,modifier=1;Abilities abilities=new Abilities();boolean onGround(){return ground;}boolean isInWater(){return water;}boolean isInLava(){return lava;}boolean onClimbable(){return climb;}boolean isFallFlying(){return fall;}Abilities getAbilities(){return abilities;}boolean hasEffect(int x){return x==1?levitation:slow;}double getGravity(){return gravity;}double getAttributeValue(int x){return modifier;}boolean shouldDiscardFriction(){return discard;}}
 METHOD
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  double y=-.08*(double).98F;check(!residual(null,y),"null fails closed");var p=new LocalPlayer();check(residual(p,y),"ordinary supported exact post-collision gravity residual");
  for(String mode:new String[]{"ground","below","physics","water","lava","climb","fall","flight","levitation","slow","gravity","modifier","discard","not-exact","upward","nonfinite"}){
   p=new LocalPlayer();double vy=y;switch(mode){case "ground"->p.ground=false;case "below"->p.verticalCollisionBelow=false;case "physics"->p.noPhysics=true;case "water"->p.water=true;case "lava"->p.lava=true;case "climb"->p.climb=true;case "fall"->p.fall=true;case "flight"->p.abilities.flying=true;case "levitation"->p.levitation=true;case "slow"->p.slow=true;case "gravity"->p.gravity=0;case "modifier"->p.modifier=Double.NaN;case "discard"->p.discard=true;case "not-exact"->vy=Math.nextUp(y);case "upward"->vy=.08;case "nonfinite"->vy=Double.NaN;}check(!residual(p,vy),"no velocity exemption for "+mode);
  }
  System.out.println("PestSprayGroundNativeTest: PASS actual support projection body, controlled physical facts; not client/server physics proof");
 }
}'''.replace('METHOD',body)
with tempfile.TemporaryDirectory(prefix='kuanperion-spray-ground-native-') as d:
 f=Path(d)/'PestSprayGroundNativeTest.java';f.write_text(java)
 subprocess.run([str(jdk/'javac'),'--release','21','-sourcepath',str(r/'src/main/java'),'-d',d,str(f)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',d,'dev.kuan.kuanperion.automation.pest.PestSprayGroundNativeTest'],check=True)
def dump(c):return subprocess.check_output([str(jdk/'javap'),'-c','-p','-classpath',str(mc),c],text=True)
living=dump('net.minecraft.world.entity.LivingEntity')
travel=living.split('private void travelInAir(',1)[1].split('\n  protected ',1)[0]
for token in ['handleRelativeFrictionAndCalculateMovement:','getEffectiveGravity:','dsub','float 0.98f','AIR_DRAG_MODIFIER','computeModifiedFriction:','setDeltaMovement:(DDD)V']:assert token in travel,token
assert travel.index('handleRelativeFrictionAndCalculateMovement:')<travel.index('getEffectiveGravity:')<travel.index('float 0.98f')<travel.rindex('setDeltaMovement:(DDD)V')
friction=living.split('private static float computeModifiedFriction(',1)[1].split('\n  public ',1)[0]
for token in ['fconst_1','fload_0','fload_1','fmul','fsub','Mth.clamp:']:assert token in friction,token
entity=dump('net.minecraft.world.entity.Entity');omni=entity.split('protected boolean omnidirectionalAirMover();',1)[1].split('\n  ',2)[1:];assert 'iconst_0' in '\n'.join(omni),'ordinary Entity air motion baseline'
for c in ['net.minecraft.world.entity.player.Player','net.minecraft.client.player.AbstractClientPlayer','net.minecraft.client.player.LocalPlayer']:
 assert 'boolean omnidirectionalAirMover(' not in dump(c),'Re-audit changed Player air-mover override'
client=(r/'src/main/java/dev/kuan/kuanperion/KuanperionClient.java').read_text();assert 'ClientTickEvents.END_CLIENT_TICK.register(client->automation.tick())' in client
controller=(r/'src/main/java/dev/kuan/kuanperion/PestSprayController.java').read_text();assert 'PestSprayGroundView.residual(p,velocity.y)' in controller
assert not any(x in s for x in ['setDeltaMovement','setPos','setNoGravity','setDown','Minecraft.getInstance'])
print('PestSprayGroundVanillaPin: PASS real26.2 post-move gravity/float-drag algebra and Player inheritance; read-only projection, no velocity writes')
