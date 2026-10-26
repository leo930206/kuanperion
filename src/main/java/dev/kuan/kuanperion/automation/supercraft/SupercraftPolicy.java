// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.supercraft;
import java.util.Objects;
/** One selected batch, with a bounded interval; a recipe declaration never replaces the current server preview. */
public record SupercraftPolicy(SupercraftTask.Policy task,int intervalSeconds) {
 public SupercraftPolicy{Objects.requireNonNull(task);if(intervalSeconds<30||intervalSeconds>86400)throw new IllegalArgumentException("Invalid Supercraft interval");}
 public boolean configured(){return task.configured();}
 public static SupercraftPolicy defaults(){return new SupercraftPolicy(SupercraftTask.Policy.defaults(),600);}
}
