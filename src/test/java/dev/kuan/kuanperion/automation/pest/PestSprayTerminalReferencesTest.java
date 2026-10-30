package dev.kuan.kuanperion.automation.pest;
public final class PestSprayTerminalReferencesTest {
 static void cleared(PestSprayTask task){try{for(String name:new String[]{"initial","anchor","last","useTab"}){var field=PestSprayTask.class.getDeclaredField(name);field.setAccessible(true);PestSprayTaskTest.check(field.get(task)==null,"terminal task must release native world/player/Tab references: "+name);}}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 public static void main(String[]args){
  var f=new PestSprayTaskTest.Fixture();f.readyUse();f.acknowledge();f.decrease(1,true);f.tick();f.tick();PestSprayTaskTest.check(f.task.state()==PestSprayTask.State.COMPLETE,"normal completion fixture");cleared(f.task);
  f=new PestSprayTaskTest.Fixture();f.readyUse();f.task.cancel(f.now);cleared(f.task);
  f=new PestSprayTaskTest.Fixture();f.rejectAcquire=true;PestSprayTaskTest.check(!f.task.request(PestSprayPlanTest.policy(),f.now)&&!f.task.active(),"failed acquire fixture");cleared(f.task);
  System.out.println("PestSprayTerminalReferencesTest: PASS terminal world/player/Tab reference disposal, no gameplay mutation");
 }
}
