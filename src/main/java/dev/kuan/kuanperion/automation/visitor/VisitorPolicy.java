// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.visitor;
import java.util.Objects;
/** Explicit authority. Notification preferences never authorize accepting an offer or spending materials. */
public record VisitorPolicy(VisitorValuation.Rules rules,int threshold,int maxVisitors,int timeoutSeconds,int cooldownSeconds,boolean onlyWhileFarming){
 public VisitorPolicy{Objects.requireNonNull(rules);if(threshold<1||threshold>5||maxVisitors<1||maxVisitors>5||timeoutSeconds<90||timeoutSeconds>300||cooldownSeconds<30||cooldownSeconds>86400)throw new IllegalArgumentException("Invalid Visitor policy");}
 public boolean configured(){return !rules.visitors().isEmpty()&&!rules.materialIds().isEmpty()&&rules.maximumOfferCoins()>0;}
 public static VisitorPolicy defaults(){return new VisitorPolicy(VisitorValuation.Rules.defaults(),5,5,240,60,true);}
}
