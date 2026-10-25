// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.flight;
import dev.kuan.kuanperion.automation.ItemSnapshot;import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.Objects;
public record FlightAnchor(Point position,float yaw,float pitch,int slot,ItemSnapshot item,boolean flying){
 public FlightAnchor{Objects.requireNonNull(position);Objects.requireNonNull(item);if(slot<0||slot>8||!Float.isFinite(yaw)||!Float.isFinite(pitch)||Math.abs(pitch)>90)throw new IllegalArgumentException("Invalid flight anchor");}
}
