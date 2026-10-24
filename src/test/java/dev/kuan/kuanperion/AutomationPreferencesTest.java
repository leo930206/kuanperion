package dev.kuan.kuanperion;
public final class AutomationPreferencesTest {
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
 public static void main(String[] args){
  for(int bits=0;bits<32;bits++){
   boolean farm=(bits&1)!=0,fp=(bits&2)!=0,mine=(bits&4)!=0,mp=(bits&8)!=0,world=(bits&16)!=0;
   check(AutomationSoundPolicy.muted(farm,fp,mine,mp,world)==(world&&(farm&&fp||mine&&mp)),"independent mute switches/world exit");
   for(float volume:new float[]{0,.25f,1})check(AutomationSoundPolicy.volume(volume,farm,fp,mine,mp,world)==(world&&(farm&&fp||mine&&mp)?0:volume),"restore exact original volume including zero");
  }
  check(ChatPrefixPalette.colorAt(0,10)==0x23B6F5,"saturated sky start");
  check(ChatPrefixPalette.colorAt(9,10)==0xF06C93,"saturated pink end");
  check(ChatPrefixPalette.BRACKET==0xAAAAAA,"gray brackets");
  FarmingStallGuard guard=new FarmingStallGuard();long second=1_000_000_000L;
  check(!guard.update(true,0,0,0,8*second),"arm custom stall");
  check(!guard.update(true,0,0,8*second-1,8*second),"eight second boundary");
  check(guard.update(true,0,0,8*second,8*second),"eight seconds triggers");
  check(!guard.update(true,0,0,9*second,8*second),"only once");
  check(!guard.update(true,0,0,10*second,2*second),"duration edit starts new interval");
  check(guard.update(true,0,0,12*second,2*second),"new custom duration");
  check(!guard.update(false,0,0,13*second,2*second),"disabled resets");
  System.out.println("AutomationPreferencesTest: PASS (32 mute states / original zero preserved / saturated prefix / custom stall intervals)");
 }
}
