package dev.kuan.kuanperion.automation.visitor;
import dev.kuan.kuanperion.automation.*;import dev.kuan.kuanperion.automation.flight.*;import dev.kuan.kuanperion.automation.pest.*;import java.util.*;
public final class VisitorTaskTest {
 static final long S=1_000_000_000L;static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final ItemSnapshot TOOL=new ItemSnapshot("HOE","minecraft:diamond_hoe","uuid","Hoe","hoe",1,List.of(),true);
 static final VisitorPolicy POLICY=new VisitorPolicy(VisitorValuationTest.rules(),1,5,240,60,true);
 static final VisitorTargets.Target NPC=new VisitorTargets.Target("Librarian",11,new UUID(1,11),new PestTargets.Point(0,72,0),12,new UUID(1,12),new PestTargets.Point(0,73,0));
 static final class Fake implements VisitorTask.Port {
  AutomationContext ctx=new AutomationContext(1,true,true,Set.of());FlightAnchor pose=new FlightAnchor(new PestTargets.Point(100,70,0),10,20,0,TOOL,false);boolean mayFly=true,manual,cursorEmpty=true,accept=true,dataReady=true,blocked,failDispose;MenuSnapshot menu;Map<Integer,ItemSnapshot> inventory=VisitorSubmissionTest.before();List<VisitorTargets.Target> targets=List.of(NPC);VisitorQueue queue=VisitorQueue.parse(List.of("Visitors: (1)","Librarian"));
  boolean acquireAllowed;int acquisitions,acquisitionCancels,acquisitionMessages;SpendingBudget acquiredBudget;VisitorTask.Acquisition acquisition=VisitorTask.Acquisition.RUNNING;
  public boolean beginAcquisition(AutomationSession.Handle h,VisitorOffer offer,VisitorPolicy policy,SpendingBudget budget,long now){acquisitions++;acquiredBudget=budget;return acquireAllowed;}
  public VisitorTask.Acquisition pollAcquisition(AutomationSession.Handle h,long now){return acquisition;}
  public boolean ownsAcquisitionScreen(){return acquireAllowed;}
  public void acquisitionMessage(long world,String text){acquisitionMessages++;}
  public void cancelAcquisition(){acquisitionCancels++;}
  int prepares,opens,clicks,restores,releases,disposes,closes,stops,pauses,dataRequests;List<PestPlot> commands=new ArrayList<>();
  public AutomationContext context(){return ctx;}public PestTargets.Point position(){return pose.position();}
  public VisitorTask.Frame frame(long now){return new VisitorTask.Frame(ctx.worldId(),now,pose,new PestTargets.Point(pose.position().x(),pose.position().y()+1.62,pose.position().z()),mayFly,manual,cursorEmpty,true);}
  public VisitorQueue queue(){return queue;}public List<VisitorTargets.Target> targets(VisitorQueue q){return targets;}public Map<Integer,ItemSnapshot> inventory(){return inventory;}
  public void requestData(AutomationSession.Handle h){dataRequests++;}public Optional<VisitorTask.Data> data(){return dataReady?Optional.of(new VisitorTask.Data(VisitorOfferTest.IDS,VisitorValuationTest.market(1000000))):Optional.empty();}public long wallMillis(){return 1000000;}
  public boolean prepare(AutomationSession.Handle h,FlightAnchor original,FlightAnchor region,long now){prepares++;pose=new FlightAnchor(pose.position(),pose.yaw(),pose.pitch(),original.slot(),original.item(),true);return accept;}
  public boolean travel(AutomationSession.Handle h,PestPlot plot,long now){commands.add(plot);return accept;}
  public FlightMotion move(AutomationSession.Handle h,PestTargets.Point goal,double tolerance,long now){if(blocked)return FlightMotion.BLOCKED;pose=new FlightAnchor(goal,pose.yaw(),pose.pitch(),pose.slot(),pose.item(),true);return FlightMotion.ARRIVED;}
  public VisitorTask.Open open(AutomationSession.Handle h,VisitorTargets.Target npc,long now){opens++;return accept?VisitorTask.Open.SENT:VisitorTask.Open.REJECTED;}
  public MenuSnapshot menu(){return menu;}
  public GuardedMenuAction.Result submit(AutomationSession.Handle h,MenuContract contract,MenuContract.Ticket ticket,long now){clicks++;check(ticket.slot()==VisitorSubmissionTest.offer(inventory).acceptSlot(),"semantic offer slot only");return accept?GuardedMenuAction.Result.SENT:GuardedMenuAction.Result.REJECTED;}
  public void closeOwnedMenu(int id){closes++;menu=null;ctx=new AutomationContext(ctx.worldId(),ctx.farmingActive(),true,Set.of());}
  public FlightMotion restore(AutomationSession.Handle h,FlightAnchor original,long now){restores++;pose=original;return FlightMotion.ARRIVED;}
  public void release(AutomationSession.Handle h){releases++;}public void dispose(AutomationSession.Handle h){disposes++;if(failDispose)throw new IllegalStateException("native data disposal failed");}
  void show(){menu=VisitorSubmissionTest.offer(inventory).menu();ctx=new AutomationContext(1,true,true,Set.of(AutomationContext.Blocker.MENU));}
 }
 static final class H {
  Fake f=new Fake();InventoryReceipts receipts=new InventoryReceipts();CursorReceipts cursors=new CursorReceipts();PlayerPositionReceipts positions=new PlayerPositionReceipts();AutomationSession session=new AutomationSession(new AutomationCoordinator(),f::context,()->f.pauses++,()->f.stops++);VisitorTask task=new VisitorTask(session,positions,receipts,cursors,f);long now;int positionPacket;
  void tick(){now+=100_000_000;session.poll(now);task.tick(now);}void start(){check(task.request(POLICY,true,now),"explicit visitor request");for(int i=0;i<10&&f.commands.isEmpty();i++)tick();check(f.commands.size()==1&&f.commands.getFirst().id()==0,"one typed Barn travel");}
  void arrive(int plot){var p=plot==0?new PestTargets.Point(0,72,0):new PestTargets.Point(100,72,0);f.pose=new FlightAnchor(p,10,20,0,TOOL,true);positions.observe(1,++positionPacket,new PlayerPositionReceipts.Position(p.x(),p.y(),p.z()),true,now+100_000_000);tick();}
  void submit(){start();arrive(0);for(int i=0;i<20&&f.opens==0;i++)tick();check(f.opens==1,"single exact NPC open");f.show();for(int i=0;i<20&&f.clicks==0;i++)tick();check(f.clicks==1&&task.state()==VisitorTask.State.WAIT_SUBMISSION,"single irreversible submission");}
  void prove(boolean cursor){task.onSystemMessage(1,"OFFER ACCEPTED with Librarian (UNCOMMON)");for(int i=9;i<26;i++){f.inventory.put(i,VisitorSubmissionTest.EMPTY);receipts.observe(1,0,i,VisitorSubmissionTest.EMPTY);}if(cursor)cursors.observe(1,f.menu==null?0:7,VisitorSubmissionTest.EMPTY,now+1);}
  void serverClose(){var ticket=task.beforeServerClose(7,now);check(ticket!=null,"owned pending submit HEAD ticket");f.menu=null;f.ctx=new AutomationContext(1,true,true,Set.of());check(task.afterServerClose(ticket,now+1),"applied TAIL close");check(!task.afterServerClose(ticket,now+2),"one use ticket");}
 }
 public static void main(String[] args){
  H h=new H();check(!h.task.request(VisitorPolicy.defaults(),true,0)&&h.f.dataRequests==0,"defaults give no material/visitor/spending authority or network");h.f.mayFly=false;check(!h.task.request(POLICY,true,0),"legal flight mandatory");
  h=new H();h.start();check(h.session.holdsFarming()&&h.f.dataRequests==1&&h.f.pauses==1,"one original Farming hold and demand-only data request");h.f.pose=new FlightAnchor(new PestTargets.Point(0,72,0),10,20,0,TOOL,true);h.tick();check(h.task.state()==VisitorTask.State.TRAVEL_OUT&&h.f.opens==0,"predicted Barn pose cannot bypass server position receipt");h.task.cancel(h.now);check(h.f.stops==1&&h.f.restores==0&&h.f.disposes==1,"cancel never auto restores/rearms");
  h=new H();h.f.dataReady=false;check(h.task.request(POLICY,true,0),"start data wait");for(int i=0;i<130&&h.task.active();i++)h.tick();check(h.task.state()==VisitorTask.State.TIMED_OUT&&h.f.commands.isEmpty()&&h.f.clicks==0,"unavailable official data bounded without gameplay");
  h=new H();h.start();h.arrive(0);h.f.targets=List.of(new VisitorTargets.Target("Librarian",11,new UUID(8,8),NPC.position(),12,NPC.markerUuid(),NPC.markerPosition()));for(int i=0;i<20&&h.f.opens==0;i++)h.tick(); // identity is selected from current dual facts, never a cached entity ID only
  if(h.f.opens>0){h.f.targets=List.of(NPC);h.tick();check(h.f.clicks==0,"changed NPC UUID cannot inherit a selected target");}h.task.cancel(h.now);
  h=new H();h.submit();h.prove(false);h.tick();check(h.task.confirmed()==0&&h.f.clicks==1,"no cursor proof means no success/reclick");h.cursors.observe(1,7,VisitorSubmissionTest.EMPTY,h.now+1);h.f.queue=VisitorQueue.parse(List.of("Visitors: (0)"));h.tick();check(h.task.confirmed()==1,"GAME + all consumed server slots + empty cursor confirms once");for(int i=0;i<10&&h.f.commands.size()<2;i++)h.tick();check(h.f.commands.size()==2&&h.f.closes==1&&h.f.restores==0,"explicit close and server-confirmed return required");h.arrive(1);for(int i=0;i<20&&h.task.active();i++)h.tick();check(h.task.state()==VisitorTask.State.COMPLETE&&h.f.restores==1&&h.f.stops==0&&h.f.pose.position().equals(new PestTargets.Point(100,70,0)),"exact original anchor before successful release");
  h=new H();h.submit();h.serverClose();h.prove(true);h.tick();check(h.task.confirmed()==1&&h.session.active(),"delayed server close preserves root before poll and permits new scope0 evidence");h.task.cancel(h.now);
  h=new H();h.submit();h.f.menu=null;h.f.ctx=new AutomationContext(1,true,true,Set.of());h.tick();check(!h.task.active()&&h.f.stops==1&&h.f.clicks==1&&h.f.closes==0,"unproven local/ESC close cancels rather than assume accept");
  h=new H();h.submit();var ticket=h.task.beforeServerClose(7,h.now);check(!h.task.afterServerClose(ticket,h.now+1),"ignored vanilla close with menu still visible denied");h.f.menu=null;check(!h.task.afterServerClose(ticket,h.now+2),"denied TAIL token cannot be replayed after later local close");h.task.cancel(h.now);
  h=new H();h.submit();h.prove(true);h.f.manual=true;h.tick();check(h.task.confirmed()==0&&!h.task.active()&&h.f.restores==0,"manual input wins over complete receipts");
  h=new H();h.submit();h.task.onSystemMessage(2,"OFFER ACCEPTED with Librarian (UNCOMMON)");h.f.queue=VisitorQueue.parse(List.of("Visitors: (0)"));for(int i=0;i<160&&h.task.active();i++)h.tick();check(h.task.state()==VisitorTask.State.TIMED_OUT&&h.task.confirmed()==0&&h.f.clicks==1&&h.f.closes==0,"NPC/queue disappearance is not submission proof and no timeout repair");
  h=new H();h.f.inventory.put(9,VisitorSubmissionTest.EMPTY);h.start();h.arrive(0);for(int i=0;i<20&&h.f.opens==0;i++)h.tick();h.f.show();for(int i=0;i<20&&h.f.commands.size()<2;i++)h.tick();check(h.f.clicks==0&&h.task.skipped()==1&&h.f.commands.size()==2,"missing materials do not buy/consume: explicit partial safe return");h.task.cancel(h.now);
  h=new H();h.submit();h.prove(true);h.f.queue=VisitorQueue.parse(List.of("Visitors: (0)"));h.tick();for(int i=0;i<10&&h.f.commands.size()<2;i++)h.tick();h.f.failDispose=true;h.arrive(1);for(int i=0;i<20&&h.task.active();i++)h.tick();check(h.task.state()==VisitorTask.State.FAILED&&h.f.disposes==1&&h.f.stops==1&&!h.session.active()&&!h.session.holdsFarming(),"successful server submission/return still must not resume Farming if native data disposal fails");
  System.out.println("VisitorTaskTest: PASS (demand data, typed Barn trip, dual identity, inventory-only single submit, server proof/scoped close, exact return and manual/timeout safety)");
 }
}
