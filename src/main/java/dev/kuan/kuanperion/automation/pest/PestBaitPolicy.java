// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.*;
/** Separate explicit consent and whole-stack withdrawal ceilings; existing trap/notification flags grant neither. */
public record PestBaitPolicy(boolean enabled,PestBaitButtonPlan.Rules rules,PestBaitBuying buying) {
 public PestBaitPolicy{Objects.requireNonNull(rules);Objects.requireNonNull(buying);}
 public PestBaitPolicy(boolean enabled,PestBaitButtonPlan.Rules rules){this(enabled,rules,PestBaitBuying.defaults());}
 public static PestBaitPolicy defaults(){return new PestBaitPolicy(false,PestBaitButtonPlan.Rules.defaults());}
 public boolean canBuy(){return configured()&&buying.configured()&&buying.targetCount()<=rules.maximumPerTrap()&&buying.targetCount()<=rules.maximumRunItems();}
 public PestBaitPolicy withBuying(PestBaitBuying value){return new PestBaitPolicy(enabled,rules,value);}
 public boolean configured(){return enabled&&rules.configured();}
 public PestBaitPolicy withEnabled(boolean b){return new PestBaitPolicy(b,rules,buying);}
 public PestBaitPolicy withId(String id){return new PestBaitPolicy(enabled,new PestBaitButtonPlan.Rules(id,rules.maximumPerTrap(),rules.maximumRunItems(),rules.protectedUuids()),buying);}
 public PestBaitPolicy withPerTrap(int n){return new PestBaitPolicy(enabled,new PestBaitButtonPlan.Rules(rules.baitId(),n,rules.maximumRunItems(),rules.protectedUuids()),buying);}
 public PestBaitPolicy withRun(int n){return new PestBaitPolicy(enabled,new PestBaitButtonPlan.Rules(rules.baitId(),rules.maximumPerTrap(),n,rules.protectedUuids()),buying);}
}
