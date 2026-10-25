// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.ItemSnapshot;import java.util.*;
/** Explicit crop, plot and exact tool consent; an old notification flag cannot authorize this task. */
public record GreenhousePolicy(Set<Integer> plots,Set<GreenhouseIdentity.Kind> kinds,String toolId,String toolUuid,int radius,int maxTargets,int timeoutSeconds,int intervalSeconds,boolean onlyWhileFarming){
 public GreenhousePolicy{
  plots=Set.copyOf(plots);kinds=Set.copyOf(kinds);Objects.requireNonNull(toolId);Objects.requireNonNull(toolUuid);
  if(plots.size()>24||plots.stream().anyMatch(id->id<1||id>24)||radius<4||radius>25||maxTargets<1||maxTargets>256||timeoutSeconds<90||timeoutSeconds>300||intervalSeconds<60||intervalSeconds>86400||!toolId.isEmpty()&&!toolId.matches("[A-Z0-9_]{1,64}")||!toolUuid.isEmpty()&&!toolUuid.matches("(?i)(?:[0-9a-f]{32}|[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})"))throw new IllegalArgumentException("Invalid Greenhouse policy");
 }
 public GreenhousePolicy withPlots(Set<Integer> v){return new GreenhousePolicy(v,kinds,toolId,toolUuid,radius,maxTargets,timeoutSeconds,intervalSeconds,onlyWhileFarming);}
 public GreenhousePolicy withKinds(Set<GreenhouseIdentity.Kind> v){return new GreenhousePolicy(plots,v,toolId,toolUuid,radius,maxTargets,timeoutSeconds,intervalSeconds,onlyWhileFarming);}
 public GreenhousePolicy withTool(String id,String uuid){return new GreenhousePolicy(plots,kinds,id,uuid,radius,maxTargets,timeoutSeconds,intervalSeconds,onlyWhileFarming);}
 public GreenhousePolicy withRadius(int v){return new GreenhousePolicy(plots,kinds,toolId,toolUuid,v,maxTargets,timeoutSeconds,intervalSeconds,onlyWhileFarming);}
 public GreenhousePolicy withMaxTargets(int v){return new GreenhousePolicy(plots,kinds,toolId,toolUuid,radius,v,timeoutSeconds,intervalSeconds,onlyWhileFarming);}
 public GreenhousePolicy withTimeout(int v){return new GreenhousePolicy(plots,kinds,toolId,toolUuid,radius,maxTargets,v,intervalSeconds,onlyWhileFarming);}
 public GreenhousePolicy withInterval(int v){return new GreenhousePolicy(plots,kinds,toolId,toolUuid,radius,maxTargets,timeoutSeconds,v,onlyWhileFarming);}
 public GreenhousePolicy withOnlyFarming(boolean v){return new GreenhousePolicy(plots,kinds,toolId,toolUuid,radius,maxTargets,timeoutSeconds,intervalSeconds,v);}
 public boolean configured(){return !plots.isEmpty()&&!kinds.isEmpty()&&!toolId.isEmpty()&&!toolUuid.isEmpty();}
 public boolean matchesTool(ItemSnapshot item){return configured()&&item!=null&&item.count()==1&&toolId.equals(item.skyblockId())&&toolUuid.equals(item.uuid());}
 public static GreenhousePolicy defaults(){return new GreenhousePolicy(Set.of(),Set.of(),"","",25,128,240,600,true);}
}
