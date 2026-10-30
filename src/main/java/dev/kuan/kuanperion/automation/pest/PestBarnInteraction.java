// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation.pest;
import dev.kuan.kuanperion.automation.*;import java.util.*;
/** Original-parent one-shot NPC opener. Native ports must independently prove entity type/name/ray/geometry. */
public final class PestBarnInteraction {
 public enum Kind {ROTATE,INTERACT}public enum Effect {SENT,REJECTED,UNCERTAIN}
 private enum State {IDLE,ALIGNING,READY,SENDING_ROTATE,SENDING_INTERACT,DONE,REVOKED}
 public record Frame(long world,long observedAt,Object level,Object player,Object gameMode,Object entity,PestTargets.Point position,PestTargets.Point eye,PestTargets.Point target,float yaw,float pitch,int selected,ItemSnapshot tool,boolean usable,boolean hit){}
 public interface Port {boolean allowed(AutomationSession.Handle h,long now);Frame frame(AutomationSession.Handle h,PestBarnTarget target,long now);Effect effect(AutomationSession.Handle h,Action action,long now);}
 public final class Action {
  private final AutomationSession.Handle bound;private final Kind kind;private final Frame before;private final PestBarnTarget selected;private final float yaw,pitch;private boolean claimed;
  private Action(Kind k,Frame f,float yaw,float pitch){bound=owner;selected=target;kind=k;before=f;this.yaw=yaw;this.pitch=pitch;}
  public Kind kind(){return kind;}public Frame snapshot(){return before;}public PestBarnTarget target(){return selected;}public float yaw(){return yaw;}public float pitch(){return pitch;}
  public boolean claim(AutomationSession.Handle h,Frame f,long now){long expected=revision;if(claimed||pending!=this||!Objects.equals(bound,h)||!live(this)||!root(h,now)||revision!=expected||claimed||!live(this)||!base(f,h,target,now)||!same(before,f))return false;claimed=true;return true;}
  public boolean matchesSnapshot(Frame f){return claimed&&live(this)&&same(before,f);}
 }
 private final AutomationSession session;private final AutomationCoordinator coordinator;private final Port port;private AutomationSession.Handle owner;private PestBarnTarget target;private State state=State.IDLE;private long revision,warmAt;private Frame warm,ready;private Action pending;
 public PestBarnInteraction(AutomationSession s,AutomationCoordinator c,Port p){session=Objects.requireNonNull(s);coordinator=Objects.requireNonNull(c);port=Objects.requireNonNull(p);}
 /** Lease-only route tombstone survives disposal so an old Barn attempt can never fall back to Abiphone. */
 public boolean uses(AutomationSession.Handle h){return h!=null&&h.equals(owner);}
 public PestTask.BarnReady bind(AutomationSession.Handle h,PestBarnTarget t,long now){
  long expected=revision;
  try{
   if(state==State.SENDING_ROTATE||state==State.SENDING_INTERACT||uses(h)&&(state==State.DONE||state==State.REVOKED))return PestTask.BarnReady.REJECTED;
   boolean valid=root(h,now);var f=t==null?null:port.frame(h,t,now);if(revision!=expected)return PestTask.BarnReady.REJECTED;
   if(!valid||!base(f,h,t,now)){if(uses(h))revoke();return PestTask.BarnReady.REJECTED;}
   if(!uses(h)){owner=h;target=t;warm=ready=null;state=State.ALIGNING;revision++;}
   else if(target==null||target.entityId()!=t.entityId()||!target.uuid().equals(t.uuid())){revoke();return PestTask.BarnReady.REJECTED;}target=t;
   var e=f.eye();var a=f.target();double dx=a.x()-e.x(),dy=a.y()-e.y(),dz=a.z()-e.z();float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
   if(Math.abs(PestSteering.rotate(f.yaw(),yaw,30,true)-f.yaw())>3||Math.abs(f.pitch()-pitch)>3){
    var result=send(Kind.ROTATE,f,PestSteering.rotate(f.yaw(),yaw,12,true),PestSteering.rotate(f.pitch(),pitch,12,false),now);return result==Effect.SENT?PestTask.BarnReady.AIMING:PestTask.BarnReady.REJECTED;
   }
   if(!f.hit()){warm=ready=null;state=State.ALIGNING;return PestTask.BarnReady.AIMING;}
   if(warm==null||!same(warm,f)||now-warmAt<0){warm=f;warmAt=now;ready=null;state=State.ALIGNING;return PestTask.BarnReady.AIMING;}
   if(now-warmAt<150_000_000L)return PestTask.BarnReady.AIMING;ready=f;state=State.READY;return PestTask.BarnReady.READY;
  }catch(RuntimeException unavailable){if(revision==expected&&uses(h))revoke();return PestTask.BarnReady.REJECTED;}
 }
 public boolean open(AutomationSession.Handle h,long now){
  if(!uses(h)||state!=State.READY)return false;long expected=revision;
  try{boolean valid=root(h,now);var f=port.frame(h,target,now);if(revision!=expected)return false;if(!valid||!base(f,h,target,now)||!f.hit()||!same(ready,f)){revoke();return false;}return send(Kind.INTERACT,f,f.yaw(),f.pitch(),now)==Effect.SENT;}
  catch(RuntimeException unavailable){if(revision==expected&&uses(h))revoke();return false;}
 }
 private Effect send(Kind kind,Frame f,float yaw,float pitch,long now){
  pending=new Action(kind,f,yaw,pitch);var action=pending;state=kind==Kind.ROTATE?State.SENDING_ROTATE:State.SENDING_INTERACT;long expected=++revision;Effect result;
  try{result=port.effect(owner,action,now);}catch(RuntimeException ambiguous){result=Effect.UNCERTAIN;}
  if(revision!=expected)return Effect.REJECTED;
  if(result!=Effect.SENT||!action.claimed){revoke();return result==Effect.UNCERTAIN?result:Effect.REJECTED;}
  pending=null;warm=ready=null;state=kind==Kind.ROTATE?State.ALIGNING:State.DONE;revision++;return Effect.SENT;
 }
 private boolean live(Action a){return pending==a&&Objects.equals(owner,a.bound)&&session.isCurrent(a.bound)&&coordinator.holdsIdentity(a.bound.lease())&&state==(a.kind==Kind.ROTATE?State.SENDING_ROTATE:State.SENDING_INTERACT);}
 private boolean root(AutomationSession.Handle h,long now){
  if(h==null||h.lease().owner()!=AutomationFeature.PEST||!session.isCurrent(h)||!coordinator.holdsIdentity(h.lease())||now-h.lease().started()<0||now-h.lease().started()>=h.lease().lifetime())return false;long expected=revision;
  try{boolean valid=port.allowed(h,now);return valid&&revision==expected&&session.isCurrent(h)&&coordinator.holdsIdentity(h.lease());}catch(RuntimeException unavailable){return false;}
 }
 private static boolean base(Frame f,AutomationSession.Handle h,PestBarnTarget t,long now){return f!=null&&h!=null&&t!=null&&f.world()==h.lease().worldId()&&now-f.observedAt()>=0&&now-f.observedAt()<=1_000_000_000L&&f.level()!=null&&f.player()!=null&&f.gameMode()!=null&&f.entity()!=null&&f.position()!=null&&f.eye()!=null&&f.target()!=null&&f.usable()&&f.target().equals(t.aim())&&PestPlot.of(0).contains(f.position().x(),f.position().z())&&f.eye().distanceSquared(f.target())>.01&&f.eye().distanceSquared(f.target())<=2.75*2.75&&Float.isFinite(f.yaw())&&Float.isFinite(f.pitch())&&f.selected()>=0&&f.selected()<9&&f.tool()!=null&&PestIdentity.vacuum(f.selected(),f.tool()).isPresent();}
 private static boolean same(Frame a,Frame b){return a!=null&&b!=null&&a.world()==b.world()&&a.level()==b.level()&&a.player()==b.player()&&a.gameMode()==b.gameMode()&&a.entity()==b.entity()&&a.position().equals(b.position())&&a.eye().equals(b.eye())&&a.target().equals(b.target())&&a.yaw()==b.yaw()&&a.pitch()==b.pitch()&&a.selected()==b.selected()&&a.tool().equals(b.tool())&&a.usable()==b.usable()&&a.hit()==b.hit();}
 private void revoke(){warm=ready=null;pending=null;target=null;state=State.REVOKED;revision++;}
 public void dispose(AutomationSession.Handle h){if(uses(h))revoke();}
}
