#!/usr/bin/env python3
"""Execute the actual shared native ownership guard behind explicit game boundaries."""
from pathlib import Path
import re,subprocess,tempfile
r=Path(__file__).resolve().parent.parent;m=r/'src/main/java/dev/kuan/kuanperion';p=m/'GardenFlightCore.java';assert p.is_file(),'Shared flight core not yet extracted'
s=p.read_text();a=s.index('boolean allowed(');a=s.rfind('\n',0,a)+1;b=s.index('{',a)+1;d=1
while d:d+=(s[b]=='{')-(s[b]=='}');b+=1
method=s[a:b]
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public final class FlightCoreGuardTest {
 AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationContext context(){return ctx;}boolean manual;boolean manualInput(){return manual;}
 AutomationFeature feature=AutomationFeature.PEST;AutomationCoordinator coordinator=new AutomationCoordinator();AutomationSession session=new AutomationSession(coordinator,this::context,()->{},()->{});AutomationSession.Handle owner;Object ownedLevel;
 static final class Abilities {boolean mayfly=true;}static final class Player {Abilities abilities=new Abilities();Abilities getAbilities(){return abilities;}}
 static final class Minecraft {Player player=new Player();Object level=new Object();boolean same=true;boolean isSameThread(){return same;}}
 Minecraft game=new Minecraft();Minecraft mc(){return game;}
 METHOD
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  var t=new FlightCoreGuardTest();var h=t.session.begin(AutomationFeature.PEST,0,120_000_000_000L);
  check(t.allowed(h,1,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.LOOK,AutomationCoordinator.Resource.HOTBAR),"existing Pest flight guard still permits its exact live lease");
  t.manual=true;check(!t.allowed(h,2,AutomationCoordinator.Resource.MOVEMENT),"physical input wins");t.manual=false;t.game.player.abilities.mayfly=false;check(!t.allowed(h,3,AutomationCoordinator.Resource.MOVEMENT),"no server flight grant means no flight");t.game.player.abilities.mayfly=true;
  t.game.same=false;check(!t.allowed(h,4,AutomationCoordinator.Resource.MOVEMENT),"client thread only");t.game.same=true;
  t.ctx=new AutomationContext(1,true,false,Set.of());check(!t.allowed(h,5,AutomationCoordinator.Resource.MOVEMENT),"native Garden proof mandatory");
  for(var blocker:List.of(AutomationContext.Blocker.MENU,AutomationContext.Blocker.FOCUS,AutomationContext.Blocker.MINING,AutomationContext.Blocker.RECOVERY)){t.ctx=new AutomationContext(1,true,true,Set.of(blocker));check(!t.allowed(h,6,AutomationCoordinator.Resource.MOVEMENT),"blocker retains priority: "+blocker);}
  t.ctx=new AutomationContext(1,true,true,Set.of());t.owner=h;t.ownedLevel=t.game.level;t.game.level=new Object();check(!t.allowed(h,7,AutomationCoordinator.Resource.MOVEMENT),"prepared level identity cannot be reused after replacement");t.ownedLevel=t.game.level;
  t.session.invalidate();check(!t.allowed(h,8,AutomationCoordinator.Resource.MOVEMENT),"revoked task cannot mutate input");t.owner=null;t.feature=AutomationFeature.GREENHOUSE;h=t.session.begin(AutomationFeature.GREENHOUSE,9,120_000_000_000L);
  check(t.allowed(h,10,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.HOTBAR)&&!t.allowed(h,10,AutomationCoordinator.Resource.USE)&&!t.allowed(h,10,AutomationCoordinator.Resource.SCREEN),"sharing flight does not grant Greenhouse Pest use or screen authority");
  t.feature=AutomationFeature.PEST;check(!t.allowed(h,11,AutomationCoordinator.Resource.MOVEMENT),"driver owner fixed at construction, not inferred from caller handle");
  t.session.invalidate();t.owner=null;t.feature=AutomationFeature.COMPOSTER;h=t.session.begin(AutomationFeature.COMPOSTER,12,120_000_000_000L);
  check(t.allowed(h,13,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.HOTBAR,AutomationCoordinator.Resource.SCREEN,AutomationCoordinator.Resource.ATTACK)&&!t.allowed(h,13,AutomationCoordinator.Resource.USE),"Composter receives only its declared machine/menu capabilities, not Pest vacuum use");
  t.feature=AutomationFeature.PEST;check(!t.allowed(h,14,AutomationCoordinator.Resource.MOVEMENT),"Composter lease cannot move through Pest driver");
  t.session.invalidate();t.owner=null;t.feature=AutomationFeature.VISITOR;h=t.session.begin(AutomationFeature.VISITOR,15,120_000_000_000L);
  check(t.allowed(h,16,AutomationCoordinator.Resource.MOVEMENT,AutomationCoordinator.Resource.HOTBAR,AutomationCoordinator.Resource.SCREEN,AutomationCoordinator.Resource.USE)&&!t.allowed(h,16,AutomationCoordinator.Resource.ATTACK),"Visitor can legally fly/interact but never inherits harvest/attack authority");t.feature=AutomationFeature.PEST;check(!t.allowed(h,17,AutomationCoordinator.Resource.MOVEMENT),"Visitor cannot borrow Pest driver");
  System.out.println("FlightCoreGuardTest: PASS (actual guard body; fixed feature/current lease/world/Garden/flight/thread/physical/blocker/resource isolation)");
 }
}
'''.replace('METHOD',method)
prism=Path.home()/'Library/Application Support/PrismLauncher';jdk=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=prism/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory() as temp:
 t=Path(temp);(t/'FlightCoreGuardTest.java').write_text(java);sources=[str(f) for f in (m/'automation').rglob('*.java')]
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',temp,*sources,str(t/'FlightCoreGuardTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',temp+':'+str(gson),'dev.kuan.kuanperion.FlightCoreGuardTest'],check=True)
code=re.sub(r'/\*.*?\*/|//[^\n]*','',s,flags=re.S)
for forbidden in ['.sendCommand(','.useItem(','.attack(','.handleContainerInput(','.setPos(','.setDeltaMovement(','.noPhysics','mayfly=','mayfly =','HttpClient']:assert forbidden not in code,forbidden
assert 'feature!=AutomationFeature.PEST&&feature!=AutomationFeature.GREENHOUSE&&feature!=AutomationFeature.COMPOSTER&&feature!=AutomationFeature.VISITOR' in code
assert '!bounds.permits(from,to)' in code and 'hasChunkAt(' in code and 'noCollision(p,swept)' in code
facade=(m/'PestFlightController.java').read_text()
for call in ['flight.move(h,goal,tolerance,now)','flight.release(h)','flight.dispose(h)','flight.restore(h,flightAnchor(anchor),now)']:assert call in facade,call
assert 'PestIdentity.sameVacuum(tool,' in facade and 'new GardenFlightCore(coordinator,session,observe,AutomationFeature.PEST)' in facade
print('FlightCoreWiringTest: PASS (Pest facade delegates one owned key/path core, vacuum authority retained, no use/attack/network/teleport in shared primitive)')
