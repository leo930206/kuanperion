// SPDX-License-Identifier: GPL-3.0-only
// Garden grid data adapted from supplied Aether util/GardenPlots; local strict typed projection.
package dev.kuan.kuanperion.automation.pest;
import java.util.Optional;
/** Plot geometry is destination validation data, not authority to teleport or fly. */
public final class PestPlot {
 private static final int[][] GRID={{21,13,9,14,22},{15,5,1,6,16},{10,2,0,3,11},{17,7,4,8,18},{23,19,12,20,24}};
 private static final PestPlot[] PLOTS=new PestPlot[25];
 static{for(int z=0;z<5;z++)for(int x=0;x<5;x++){int id=GRID[z][x];PLOTS[id]=new PestPlot(id,(x-2)*96-48,(z-2)*96-48);}}
 private final int id,minX,minZ;
 private PestPlot(int id,int x,int z){this.id=id;minX=x;minZ=z;}
 public static PestPlot of(int id){if(id<0||id>24)throw new IllegalArgumentException("Invalid Garden plot");return PLOTS[id];}
 public static Optional<PestPlot> at(double x,double z){
  if(!Double.isFinite(x)||!Double.isFinite(z)||x< -240||x>=240||z< -240||z>=240)return Optional.empty();
  return Optional.of(PLOTS[GRID[(int)Math.floor((z+48)/96)+2][(int)Math.floor((x+48)/96)+2]]);
 }
 public int id(){return id;}public int minX(){return minX;}public int minZ(){return minZ;}public double centerX(){return minX+48;}public double centerZ(){return minZ+48;}
 public boolean contains(double x,double z){return Double.isFinite(x)&&Double.isFinite(z)&&x>=minX&&x<minX+96&&z>=minZ&&z<minZ+96;}
 public String command(){return id==0?"barn":"tptoplot "+id;}
}
