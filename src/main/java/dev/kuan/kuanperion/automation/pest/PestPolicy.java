// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
/** Independent automation preferences; legacy Pest notifications never enable this driver. */
public record PestPolicy(int threshold,int timeoutSeconds,int captureSeconds,int maxTargets,int radius,int cooldownSeconds,boolean onlyWhileFarming,boolean sweep){
 public PestPolicy {if(threshold<1||threshold>8||timeoutSeconds<30||timeoutSeconds>240||captureSeconds<5||captureSeconds>30||maxTargets<1||maxTargets>64||radius<16||radius>96||cooldownSeconds<10||cooldownSeconds>3600)throw new IllegalArgumentException("Invalid Pest limits");}
 public PestPolicy withSweep(boolean value){return new PestPolicy(threshold,timeoutSeconds,captureSeconds,maxTargets,radius,cooldownSeconds,onlyWhileFarming,value);}
 public static PestPolicy defaults(){return new PestPolicy(1,120,20,16,64,60,true,false);}
}
