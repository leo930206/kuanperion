// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import java.util.Objects;
/** The same 3D fence guards proposed A* edges and actual movement rechecks. */
public record PestFlightBounds(PestTargets.Point center,int radius){
 public PestFlightBounds{Objects.requireNonNull(center);if(radius<16||radius>96)throw new IllegalArgumentException("Invalid flight radius");}
 public boolean contains(PestTargets.Point point){return point!=null&&center.distanceSquared(point)<=radius*radius;}
 public boolean permits(PestTargets.Point from,PestTargets.Point to){return contains(from)&&contains(to);}
}
