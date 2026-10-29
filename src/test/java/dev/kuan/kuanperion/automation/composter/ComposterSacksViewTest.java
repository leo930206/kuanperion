package dev.kuan.kuanperion.automation.composter;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class ComposterSacksViewTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final ItemSnapshot AIR=new ItemSnapshot("","minecraft:air","","","air",0,List.of(),true);
 static ItemSnapshot item(String name,List<String> lore){return new ItemSnapshot("","minecraft:paper","",name,name+lore,1,lore,false);}
 static MenuSnapshot menu(int id,String title,Map<Integer,ItemSnapshot> changes){var slots=new ArrayList<MenuSnapshot.Slot>();for(int i=0;i<90;i++)slots.add(new MenuSnapshot.Slot(i,changes.getOrDefault(i,AIR),i>=54));return new MenuSnapshot(1,id,1,title,54,slots);}
 static MenuSnapshot main(int id,long organic){return menu(id,"Composter",Map.of(39,item("Organic Matter",List.of(organic+"/10000")),41,item("Fuel",List.of("5000/10000"))));}
 static MenuSnapshot detail(int id){return menu(id,"Organic Matter",Map.of(11,item("Fill from Sacks",List.of("Click to fill!"))));}
 public static void main(String[]args){
  var p=ComposterTaskTest.POLICY;check(p.maximumSacksUnits()==0,"old settings never grant Sacks");boolean bad=false;try{p.withSacksUnits(10000);}catch(IllegalArgumentException e){bad=true;}check(bad,"fill-all cannot silently widen a 90% target");p=p.withTargetPercent(100).withSacksUnits(20000);check(p.withDelay(1000).withBuying(p.buying()).maximumSacksUnits()==20000,"all policy edits preserve consent");
  var m=main(7,1000);check(ComposterSacksView.open(m,ComposterObservation.Kind.ORGANIC,9999).isEmpty(),"reserve whole resource capacity, not only current deficit");check(ComposterSacksView.open(m,ComposterObservation.Kind.ORGANIC,10000).orElseThrow().slot()==39,"source39 plus semantic resource required");check(ComposterSacksView.open(menu(7,"Composter",Map.of(9,m.item(39),41,m.item(41))),ComposterObservation.Kind.ORGANIC,20000).isEmpty(),"not arbitrary resource slot");
  check(ComposterSacksView.fill(detail(8),ComposterObservation.Kind.ORGANIC).orElseThrow().slot()==11,"source11 plus explicit Sacks role");check(ComposterSacksView.fill(detail(8),ComposterObservation.Kind.FUEL).isEmpty(),"kind mismatch");check(ComposterSacksView.fill(menu(8,"Organic Matter",Map.of(11,item("Fill from Sacks",List.of("Cost: 100 coins")))),ComposterObservation.Kind.ORGANIC).isEmpty(),"no implicit coin spend");check(ComposterSacksView.fill(menu(8,"Organic Matter",Map.of(11,item("Fill from Sacks",List.of()),12,item("Fill from Sacks",List.of()))),ComposterObservation.Kind.ORGANIC).isEmpty(),"duplicate role refused");
  System.out.println("ComposterSacksViewTest: PASS");
 }
}
