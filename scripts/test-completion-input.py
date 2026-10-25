#!/usr/bin/env python3
"""Execute actual flight disposal and post-stop physical reconciliation; no GLFW/game/account actions."""
from pathlib import Path
import re,subprocess,tempfile
root=Path(__file__).resolve().parent.parent;main=root/'src/main/java/dev/kuan/kuanperion';source=(main/'GardenFlightCore.java').read_text()
def method(name):
 a=source.index(' public void '+name+'(');b=source.index('{',a)+1;d=1
 while d:d+=(source[b]=='{')-(source[b]=='}');b+=1
 return source[a:b]
methods='\n'.join(method(n) for n in ['release','dispose','afterStop'])
java='''package dev.kuan.kuanperion;
import dev.kuan.kuanperion.automation.*;import java.util.*;
public class CompletionInputTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}static class Key {boolean down,physical;int writes;void setDown(boolean b){down=b;writes++;}}static class Options {Key keyUp=new Key(),keyDown=new Key(),keyLeft=new Key(),keyRight=new Key(),keyJump=new Key(),keyShift=new Key();}static class Gui {Object screen;Object screen(){return screen;}}static class Minecraft {boolean same=true;Object level=new Object();Options options=new Options();Gui gui=new Gui();Object window=new Object();Object getWindow(){return window;}boolean isSameThread(){return same;}}
 Minecraft game=new Minecraft();Minecraft mc(){return game;}boolean failPhysical;boolean physical(Key key,boolean unknown){if(failPhysical)throw new IllegalStateException("input source unavailable");return key.physical;}
 AutomationContext ctx=new AutomationContext(1,true,true,Set.of());AutomationSession session=new AutomationSession(new AutomationCoordinator(),()->ctx,()->{},()->game.options.keyUp.setDown(false));AutomationSession.Handle owner,retiredOwner;Object ownedLevel,retiredLevel,preparedTool,bounds,search,searchGoal;List<Object> route=List.of();Set<Key> pressed=new HashSet<>();
 METHODS
 AutomationSession.Handle start(){var h=session.begin(AutomationFeature.PEST,System.nanoTime(),1_000_000_000L);owner=h;ownedLevel=game.level;game.options.keyUp.physical=true;game.options.keyUp.down=true;pressed.add(game.options.keyUp);return h;}
 public static void main(String[]args){
  var f=new CompletionInputTest();var h=f.start();f.dispose(h);check(f.owner==null&&f.retiredOwner==h&&f.game.options.keyUp.down,"disposal cleans owner while remembering exact failure-only physical token");f.session.finish(h,false,System.nanoTime());check(!f.game.options.keyUp.down,"legacy safe-stop fixture really clears physical movement");f.afterStop(h);check(f.game.options.keyUp.down&&f.retiredOwner==null,"physical state is restored after legacy stop, with one-use matching disposal token");int writes=f.game.options.keyUp.writes;f.game.options.keyUp.physical=false;f.afterStop(h);check(f.game.options.keyUp.writes==writes,"no replay after physical state changes");
  var success=new CompletionInputTest();var sh=success.start();success.dispose(sh);check(success.session.finish(sh,true,System.nanoTime())&&success.game.options.keyUp.down,"success disposal already honors physical input before hold release; no post-success work needed");
  var newer=new CompletionInputTest();var old=newer.start();newer.dispose(old);newer.session.finish(old,false,System.nanoTime());var current=newer.session.begin(AutomationFeature.VISITOR,System.nanoTime(),1_000_000_000L);newer.afterStop(old);check(!newer.game.options.keyUp.down&&newer.session.isCurrent(current)&&newer.retiredOwner==null,"old physical token cannot overwrite a newer active root and is consumed on denial");
  var level=new CompletionInputTest();var lh=level.start();level.dispose(lh);level.session.finish(lh,false,System.nanoTime());level.game.level=new Object();level.afterStop(lh);check(!level.game.options.keyUp.down,"native world identity must still match retired overlay");
  var screen=new CompletionInputTest();var gh=screen.start();screen.dispose(gh);screen.session.finish(gh,false,System.nanoTime());screen.game.gui.screen=new Object();screen.afterStop(gh);check(!screen.game.options.keyUp.down,"do not press physical world keys through a GUI");
  var thread=new CompletionInputTest();var th=thread.start();thread.game.same=false;boolean denied=false;try{thread.dispose(th);}catch(IllegalStateException expected){denied=true;}check(denied&&thread.owner==th&&thread.retiredOwner==null,"off-thread cannot dispose native state");thread.game.same=true;thread.dispose(th);thread.session.finish(th,false,System.nanoTime());thread.game.same=false;thread.afterStop(th);check(!thread.game.options.keyUp.down&&thread.retiredOwner==th,"off-thread post-stop cannot mutate keys or consume token");thread.game.same=true;thread.afterStop(th);check(thread.game.options.keyUp.down,"authorized client cleanup may still consume its token");
  var throwing=new CompletionInputTest();var eh=throwing.start();throwing.failPhysical=true;boolean cleanupFailed=false;try{throwing.dispose(eh);}catch(IllegalStateException expected){cleanupFailed=true;}check(cleanupFailed&&throwing.owner==null&&throwing.pressed.isEmpty()&&throwing.retiredOwner==eh,"native input-read failure still clears owned path/overlay bookkeeping and retains failure-only token");throwing.session.finish(eh,false,System.nanoTime());throwing.failPhysical=false;throwing.afterStop(eh);check(throwing.game.options.keyUp.down,"post-stop reconciliation may recover only real physical input, never old injected keys");
  var foreign=new CompletionInputTest();var fh=foreign.start();foreign.afterStop(fh);check(foreign.owner==fh&&foreign.retiredOwner==null,"no token means no physical reconciliation");
  System.out.println("CompletionInputTest: PASS (actual native disposal/post-stop bodies; original physical keys restored after stop only, one-use owner/world/thread/new-root/GUI fences; no live input claim)");
 }
}'''.replace('METHODS',methods)
prism=Path.home()/'Library/Application Support/PrismLauncher';jdk=prism/'java/java-runtime-epsilon/jre.bundle/Contents/Home/bin';gson=prism/'libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
with tempfile.TemporaryDirectory(prefix='kuanperion-completion-input-') as tmp:
 p=Path(tmp);(p/'CompletionInputTest.java').write_text(java);sources=list((main/'automation').rglob('*.java'))
 subprocess.run([str(jdk/'javac'),'--release','21','-cp',str(gson),'-d',tmp,*map(str,sources),str(p/'CompletionInputTest.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.CompletionInputTest'],check=True)
# All four actual native facades delegate the failure-only hook to their own flight core.
for name in ['Pest','Greenhouse','Composter','Visitor']:
 s=(main/(name+'FlightController.java')).read_text();assert 'public void afterStop(AutomationSession.Handle h){flight.afterStop(h);}' in s,name
