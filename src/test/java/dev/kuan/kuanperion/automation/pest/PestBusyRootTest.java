package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
/** Actual Pest service/task/session and existing deterministic target; not a native chain success claim. */
public final class PestBusyRootTest {
 static final long S=1_000_000_000L;static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void run(String mode)throws Exception{
  Path dir=Files.createTempDirectory("pest-busy-root-");PestService service=null;AutomationSession session=null;
  try{
   var f=new PestTaskTest.Fake();int[] reads={0},allowedReads={0},hooks={0,0};boolean[] closed={true};AutomationSession.Handle[] started={null};
   var port=(PestTask.Port)Proxy.newProxyInstance(PestTask.Port.class.getClassLoader(),new Class<?>[]{PestTask.Port.class},(p,m,args)->{
    reads[0]++;if(m.getName().equals("prepare"))started[0]=(AutomationSession.Handle)args[0];
    try{return m.invoke(f,args);}catch(InvocationTargetException e){throw e.getCause();}
   });
   session=new AutomationSession(new AutomationCoordinator(),port::context,()->hooks[0]++,()->hooks[1]++);
   var path=dir.resolve("automation.json");var store=AutomationSettingsStore.load(path);
   service=new PestService(store,session,port,()->{allowedReads[0]++;return closed[0];},n->{});service.setEnabled(true);byte[] saved=Files.readAllBytes(path);
   var prior=session.begin(AutomationFeature.SUPERCRAFT,0,60*S);check(prior!=null,"preceding task holds original Farm");reads[0]=allowedReads[0]=0;
   if(mode.equals("manual")){check(!service.manualRequest(S)&&!service.pending()&&!service.active()&&reads[0]==0&&allowedReads[0]==0&&session.isCurrent(prior)&&Arrays.equals(saved,Files.readAllBytes(path)),"explicit Pest request cannot inspect/query a foreign root's temporary world/tool/targets");return;}
   service.tick(S);check(reads[0]==0&&allowedReads[0]==0&&started[0]==null&&session.isCurrent(prior)&&Arrays.equals(saved,Files.readAllBytes(path)),"busy automatic Pest defers before all native/UI reads and preserves root/consent");
   if(mode.equals("busy"))return;
   check(session.finish(prior,true,S),"preceding successful task releases hold");reads[0]=0;
   if(mode.equals("ui")){closed[0]=false;service.tick(S);check(!service.active()&&started[0]==null&&reads[0]==0,"open UI retains priority after preceding task finishes");closed[0]=true;}
   if(mode.equals("farm-off")){session.invalidate();f.ctx=new AutomationContext(1,false,true,Set.of());service.tick(S);check(!service.active()&&!session.active()&&started[0]==null&&hooks[0]==1&&hooks[1]==0&&Arrays.equals(saved,Files.readAllBytes(path)),"current manual Farm-off defeats due Pest without rearming or persistence");return;}
   if(mode.equals("threshold")){f.alive=0;service.tick(S);check(!service.active()&&!session.active()&&started[0]==null&&hooks[0]==1&&Arrays.equals(saved,Files.readAllBytes(path)),"fresh lower pest count defeats formerly due threshold");return;}
   service.tick(S);check(service.active()&&started[0]!=null&&started[0].lease().owner()==AutomationFeature.PEST&&f.prepares==1&&hooks[0]==2&&hooks[1]==0,"Pest dispatches at SAME timestamp after owner release, without one-second throttle loss");
   check(started[0].generation()==prior.generation()&&started[0].hold().farmingSession()==prior.hold().farmingSession()&&session.holdsFarming()&&Arrays.equals(saved,Files.readAllBytes(path)),"Pest retains only original manual Farm generation and current threshold/tool proof");
  }finally{if(service!=null)service.cancel(2*S);if(session!=null)session.invalidate();try(var files=Files.walk(dir)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
 public static void main(String[] args)throws Exception{if(args.length>0)run(args[0]);else for(String mode:List.of("busy","manual","handoff","ui","farm-off","threshold"))run(mode);System.out.println("PestBusyRootTest: PASS (real Pest service/task/session, no busy preflight/UI reads or manual request, same-timestamp due dispatch, fresh Farm/threshold/UI, original generation)");}
}
