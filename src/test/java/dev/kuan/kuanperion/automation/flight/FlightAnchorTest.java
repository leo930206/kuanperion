package dev.kuan.kuanperion.automation.flight;
import dev.kuan.kuanperion.automation.ItemSnapshot;import dev.kuan.kuanperion.automation.pest.PestTargets.Point;import java.util.List;
public final class FlightAnchorTest {
 public static void main(String[]args){
  var p=new Point(.5,70.25,-.5);var item=new ItemSnapshot("TOOL","minecraft:diamond_hoe","uuid","","meta",1,List.of(),true);var a=new FlightAnchor(p,12,-5,3,item,true);
  if(!a.position().equals(p)||a.yaw()!=12||a.pitch()!=-5||a.slot()!=3||a.item()!=item||!a.flying())throw new AssertionError("anchor data changed");
  for(int slot:new int[]{-1,9})try{new FlightAnchor(p,0,0,slot,item,false);throw new AssertionError("invalid slot");}catch(IllegalArgumentException expected){}
  for(float pitch:new float[]{-91,91,Float.NaN})try{new FlightAnchor(p,0,pitch,0,item,false);throw new AssertionError("invalid pitch");}catch(IllegalArgumentException expected){}
  if(FlightMotion.values().length!=3)throw new AssertionError("motion mapping changed");
  System.out.println("FlightAnchorTest: PASS (exact immutable return pose/tool/slot/flight and bounded input validation)");
 }
}
