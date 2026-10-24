package dev.kuan.kuanperion;
/** Runtime gain only: never writes Minecraft's persisted sound options. */
public final class AutomationSoundPolicy {
 private AutomationSoundPolicy(){}
 public static boolean muted(boolean farming,boolean farmingPreference,boolean mining,boolean miningPreference,boolean inWorld){return inWorld&&(farming&&farmingPreference||mining&&miningPreference);}
 public static float volume(float original,boolean farming,boolean farmingPreference,boolean mining,boolean miningPreference,boolean inWorld){return muted(farming,farmingPreference,mining,miningPreference,inWorld)?0:original;}
}
