// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.*;
/** Target identity remains separate from its pose and from permission to attack it. */
public record GreenhouseTarget(Ref ref,GreenhouseIdentity.Kind kind,Point position){
 public sealed interface Ref permits EntityRef,BlockRef{}
 public record EntityRef(int id,UUID uuid)implements Ref{public EntityRef{if(id<0)throw new IllegalArgumentException("Invalid entity");Objects.requireNonNull(uuid);}}
 public record BlockRef(int x,int y,int z)implements Ref{public BlockRef{if(Math.abs((long)x)>30_000_000||Math.abs((long)z)>30_000_000||y< -2048||y>2048)throw new IllegalArgumentException("Invalid block");}}
 public GreenhouseTarget{Objects.requireNonNull(ref);Objects.requireNonNull(kind);Objects.requireNonNull(position);}
}
