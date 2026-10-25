package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class ComposterObservationTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static ItemSnapshot item(String name,List<String> lore){return new ItemSnapshot("","minecraft:paper","",name,name+lore,1,lore,true);}
 static MenuSnapshot menu(String title,List<MenuSnapshot.Slot> slots){return new MenuSnapshot(1,7,5,title,54,slots);}
 public static void main(String[]args){
  check(ComposterObservation.resource("1,234/40k").orElseThrow().equals(new ComposterObservation.Resource(1234,40000)),"upstream observed capacity ratio");
  check(ComposterObservation.resource("4.25k / 40k").orElseThrow().current()==4250,"exact decimal suffix, no lossy double rounding");
  check(ComposterObservation.resource("0/100k").orElseThrow().current()==0,"empty known distinct from unknown");
  for(String bad:List.of("-1/40k","1,00/40k","2/1","1/0","40k","Stored: 1/40k","1/40k Coins","1e3/40k","0.0001/40k","1/1g","NaN/40k","1000000001/1000000002"))check(ComposterObservation.resource(bad).isEmpty(),"unknown/bad capacity: "+bad);
  var slots=List.of(new MenuSnapshot.Slot(9,item("Organic Matter",List.of("1,234/40k"))),new MenuSnapshot.Slot(35,item("Fuel",List.of("4.25k/100k"))));
  var observed=ComposterObservation.inspect(menu("Composter",slots)).orElseThrow();check(observed.organic().current()==1234&&observed.fuel().current()==4250&&observed.organicSlot()==9&&observed.fuelSlot()==35,"semantic roles, not original numeric slots");
  check(ComposterObservation.inspect(menu("My Composter",slots)).isEmpty(),"exact container title, no contains spoof");
  var duplicate=new ArrayList<>(slots);duplicate.add(new MenuSnapshot.Slot(20,item("Fuel",List.of("1/100k"))));check(ComposterObservation.inspect(menu("Composter",duplicate)).isEmpty(),"duplicate semantic roles never picked first");
  var player=new ArrayList<>(slots);player.set(1,new MenuSnapshot.Slot(55,item("Fuel",List.of("1/100k")),true));check(ComposterObservation.inspect(menu("Composter",player)).isEmpty(),"player item cannot spoof a role");
  var conflicting=List.of(new MenuSnapshot.Slot(9,item("Organic Matter",List.of("1/40k","2/40k"))),slots.get(1));check(ComposterObservation.inspect(menu("Composter",conflicting)).isEmpty(),"ambiguous capacity lines unknown");
  check(ComposterObservation.increased(observed,observed,ComposterObservation.Kind.FUEL,100)==false,"unchanged local snapshot not replenishment");
  var after=ComposterObservation.inspect(menu("Composter",List.of(slots.get(0),new MenuSnapshot.Slot(35,item("Fuel",List.of("4,350/100k")))))).orElseThrow();check(ComposterObservation.increased(observed,after,ComposterObservation.Kind.FUEL,100),"exact resource delta, still requires independent server receipt");
  System.out.println("ComposterObservationTest: PASS (strict exact capacity arithmetic/limits, exact menu/semantic roles, no player/duplicate/ambiguous inference, local delta not action authority)");
 }
}
