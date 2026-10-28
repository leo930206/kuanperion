package dev.kuan.kuanperion.automation;
import java.util.*;
public final class AutomationStopsTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  var calls=new ArrayList<Integer>();AutomationStops.run(()->calls.add(1),()->calls.add(2));check(calls.equals(List.of(1,2)),"healthy callbacks ordered exactly once");
  var first=new IllegalStateException("first");var second=new IllegalArgumentException("second");calls.clear();
  try{AutomationStops.run(()->{calls.add(1);throw first;},()->{calls.add(2);throw first;},()->{calls.add(3);throw second;},()->calls.add(4));throw new AssertionError("original failure must escape");}
  catch(RuntimeException failure){check(failure==first&&Arrays.equals(failure.getSuppressed(),new Throwable[]{second}),"retain exact first/later failure; repeated first is never self-suppressed");}
  check(calls.equals(List.of(1,2,3,4)),"no failure skips later callbacks or retries earlier ones");
  AutomationStops.run();calls.clear();var error=new AssertionError("fatal VM/programmer error");
  try{AutomationStops.run(()->{throw error;},()->calls.add(1));throw new AssertionError("Error must not be treated as a recoverable stop exception");}catch(AssertionError failure){check(failure==error&&calls.isEmpty(),"never swallow/catch arbitrary Error");}
  System.out.println("AutomationStopsTest: PASS (exact callback order/once, first and suppressed errors, self-suppression protection, empty list, Error not swallowed)");
 }
}
