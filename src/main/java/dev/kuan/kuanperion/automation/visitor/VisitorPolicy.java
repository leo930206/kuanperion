// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import java.util.Objects;import java.util.Map;import dev.kuan.kuanperion.automation.BazaarAcquisitionPlan;
/** Explicit authority. Notification preferences never authorize accepting an offer or spending materials. */
public record VisitorPolicy(VisitorValuation.Rules rules,int threshold,int maxVisitors,int timeoutSeconds,int cooldownSeconds,boolean onlyWhileFarming,Map<String,BazaarAcquisitionPlan.Material> buying){
 public VisitorPolicy{Objects.requireNonNull(rules);buying=Map.copyOf(buying);if(buying.size()>8||buying.keySet().stream().anyMatch(id->!rules.materialIds().contains(id)))throw new IllegalArgumentException("Purchase materials must also be authorized for submission");if(threshold<1||threshold>5||maxVisitors<1||maxVisitors>5||timeoutSeconds<90||timeoutSeconds>300||cooldownSeconds<30||cooldownSeconds>86400)throw new IllegalArgumentException("Invalid Visitor policy");}
 public VisitorPolicy(VisitorValuation.Rules rules,int threshold,int maxVisitors,int timeoutSeconds,int cooldownSeconds,boolean onlyWhileFarming){this(rules,threshold,maxVisitors,timeoutSeconds,cooldownSeconds,onlyWhileFarming,Map.of());}
 public BazaarAcquisitionPlan.Rules buyingRules(){return new BazaarAcquisitionPlan.Rules(buying,rules.maximumBuyCoins(),4096);}
 public boolean configured(){return !rules.visitors().isEmpty()&&!rules.materialIds().isEmpty()&&rules.maximumOfferCoins()>0;}
 public static VisitorPolicy defaults(){return new VisitorPolicy(VisitorValuation.Rules.defaults(),5,5,240,60,true);}
}
