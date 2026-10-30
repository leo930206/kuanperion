package dev.kuan.kuanperion.automation.pest;
import java.util.*;
public final class PestSprayTabReceiptsTest {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[]args){
  long t=3_000_000_000L;var level=new Object();var player=new Object();var row=new Object();
  var scope=new PestSprayTabReceipts.Scope(1,level,player,1,1,0);var tabs=new PestSprayTabReceipts();
  var none=List.of("Pests:","Alive: 1","Spray: None","Skills:","Spray: Test Material");
  check(tabs.current(scope,row,none,t).isEmpty(),"cached text alone cannot mint an applied receipt");
  tabs.observe(scope,row,none,2,t);var a=tabs.current(scope,row,none,t).orElseThrow();
  check(a.entries().equals(none)&&a.scope().same(scope),"bounded immutable scoped receipt");
  check(tabs.current(scope,row,none,t+PestSprayTabReceipts.MAX_AGE).orElseThrow()==a&&tabs.current(scope,row,none,t+PestSprayTabReceipts.MAX_AGE+1).isEmpty()&&tabs.current(scope,row,none,t-1).isEmpty(),"polling never refreshes timestamp; future/expired fail closed");
  check(tabs.current(scope,new Object(),none,t).isEmpty(),"same display string from removed/recreated row cannot reuse receipt");
  for(var changed:List.of(new PestSprayTabReceipts.Scope(2,level,player,1,1,0),new PestSprayTabReceipts.Scope(1,new Object(),player,1,1,0),new PestSprayTabReceipts.Scope(1,level,new Object(),1,1,0),new PestSprayTabReceipts.Scope(1,level,player,2,1,0),new PestSprayTabReceipts.Scope(1,level,player,1,2,0),new PestSprayTabReceipts.Scope(1,level,player,1,1,1)))check(tabs.current(changed,row,none,t).isEmpty(),"world/identity/plot/correction scope cannot carry cached None to a new location");
  var active=List.of("Pests:","Alive: 1","Spray: Test Material (29m)");
  check(tabs.current(scope,row,active,t).isEmpty(),"local field mutation cannot become server evidence");
  tabs.observe(scope,row,none,1,t);check(tabs.current(scope,row,none,t).isEmpty(),"unrelated row update cannot mint a Spray receipt");
  tabs.observe(scope,row,active,2,t);var b=tabs.current(scope,row,active,t).orElseThrow();check(b.sequence()>a.sequence()&&b.epoch()==a.epoch(),"same-scope explicit applied refresh advances sequence");
  tabs.clear();check(tabs.current(scope,row,active,t).isEmpty(),"world/shutdown disposal invalidates all evidence");tabs.observe(scope,row,none,2,t);check(tabs.current(scope,row,none,t).orElseThrow().epoch()!=a.epoch(),"clear cannot resurrect an old sequence under same epoch");
  for(var invalid:List.of(List.of("Spray: None"),List.of("Pests:","Spray: None","Spray: None"),List.of("Pests:","Spray: None","Pests:","Spray: None"),List.of("Pests:","Unknown Field: x","Spray: None"),List.of("Pests:","Spray: None\u0000"))){tabs.observe(scope,row,invalid,Math.min(1,invalid.size()-1),t);check(tabs.current(scope,row,invalid,t).isEmpty(),"unscoped/duplicate/header/control/unknown role is not fresh proof");}
  check(PestSprayObservation.fieldEntry(List.of("Pests:\nAlive: 1\nSpray: None")).orElseThrow()==0,"multiline source entry is correlated to its actual packet row");
  System.out.println("PestSprayTabReceiptsTest: PASS exact applied-row evidence, bounded freshness, identity/plot/correction/epoch scope; no live cadence claim");
 }
}
