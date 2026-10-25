package dev.kuan.kuanperion.automation.pest;
public final class PestPlotTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  check(PestPlot.of(0).command().equals("barn")&&PestPlot.of(24).command().equals("tptoplot 24"),"closed typed Garden commands only");
  for(int id=0;id<=24;id++){var p=PestPlot.of(id);check(PestPlot.at(p.centerX(),p.centerZ()).orElseThrow().id()==id,"all 25 unique plot centers");check(p.contains(p.minX(),p.minZ())&&!p.contains(p.minX()+96,p.minZ()),"half-open exact shared boundaries");}
  check(PestPlot.of(1).centerZ()==-96&&PestPlot.of(3).centerX()==96&&PestPlot.of(21).centerX()==-192,"curated Garden orientation");
  check(PestPlot.at(240,0).isEmpty()&&PestPlot.at(Double.NaN,0).isEmpty(),"unknown/outside coordinates not invented as barn");
  for(int bad:new int[]{-1,25,100})try{PestPlot.of(bad);throw new AssertionError("unbounded command ID");}catch(IllegalArgumentException expected){}
  System.out.println("PestPlotTest: PASS (strict typed IDs/commands, unique curated layout, bounded half-open coordinates, no string normalization/invented plot)");
 }
}
