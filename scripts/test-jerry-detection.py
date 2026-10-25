#!/usr/bin/env python3
"""Run production Jerry chat/actions and automation cleanup with fake game boundaries."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
base = root / 'src/main/java/dev/kuan/kuanperion'
client = (base / 'KuanperionClient.java').read_text()
def extract(name):
    match = re.search(r'    (?:private|public) static [^\n]+ ' + name + r'\(', client)
    assert match, f'Missing production method: {name}'
    start = match.start()
    opening = client.index('{', start)
    depth, end = 1, opening + 1
    while depth:
        depth += (client[end] == '{') - (client[end] == '}')
        end += 1
    return client[start:end]
methods = '\n'.join(extract(name) for name in [
    'handleReceivedMessage', 'handleJerryMessage', 'sendJerryHomeCommand', 'sendHomeCommand', 'stopFarming', 'clearActiveTarget',
    'setAttackKey', 'releaseFarmingKeys', 'releaseFarmingMovementKeys',
    'shouldLockManualLook', 'shouldLockManualAttack'])
bridge=(base/'AutomationClientBridge.java').read_text()
def bridge_method(name):
    match=re.search(r' (?:public|private) [^{}\n()]+ '+name+r'\(',bridge);assert match
    a=match.start();b=bridge.index('{',a)+1;depth=1
    while depth:
        depth+=(bridge[b]=='{')-(bridge[b]=='}');b+=1
    return bridge[a:b]
bridge_methods='\n'.join(bridge_method(n) for n in ['sendAutomaticJerryHome','onOutgoingCommand','suspendForJerry'])
assert 'p.setProperty("jerryStopActionsEnabled"' in client
assert 'toggleMsg("Jerry 偵測"' not in client
assert 'Component.literal("Jerry 偵測")' in client
harness = r'''package dev.kuan.kuanperion;
import java.util.*;
import java.util.function.*;
import dev.kuan.kuanperion.automation.*;
public class JerryDetectionIntegrationTest {
 static void invalidateAutomationFarmSession(){if(automation!=null)automation.session.invalidate();}
 static class AutomationClientBridge {
  boolean ownGameplayCommand,observedGameplayCommand;
  AutomationCoordinator coordinator=new AutomationCoordinator();
  AutomationSession session=new AutomationSession(coordinator,()->new AutomationContext(1,farmingEnabled,true,Set.of()),()->{},()->{farmingEnabled=false;});
  class Service {boolean suspended,active;void cancel(long n){}void onOutgoingCommand(){}void suspend(long n){suspended=true;if(active){active=false;session.cancel(n);}}void suspend(String reason){suspend(System.nanoTime());}}
  Service stash=new Service(),pest=new Service(),greenhouse=new Service(),composter=new Service(),autoSell=new Service(),junk=new Service();
  List<Service> services(){return List.of(pest,greenhouse,composter,autoSell,junk,stash);}
  // BRIDGE_METHODS
 }
 static AutomationClientBridge automation;
    static boolean jerryStopActionsEnabled;
    static boolean farmingManualStartPending;
    static int farmingMarkersCreated;
    static void markJerryFarmingPause(Minecraft mc) { check(!farmingEnabled && !miningEnabled,"stop automation before preparing the beam");farmingMarkersCreated++; }
    static final FarmingStallGuard FARMING_STALL_GUARD=new FarmingStallGuard();
    static Set<JerryColor> jerryColors=Set.of(JerryColor.values());
    static final Map<NotificationType,NotificationChannels> NOTIFICATION_CHANNELS = new EnumMap<>(NotificationType.class);
    static boolean miningEnabled, farmingEnabled, farmingAutoRecovering, farmingEnabledThisSession;
    static boolean farmingWorldChanged, activeTargetStarted;
    static boolean farmingDesiredForward, farmingDesiredBack, farmingDesiredLeft, farmingDesiredRight, farmingDesiredJump;
    static boolean abnormalHandlingEnabled;
    static String activeTargetBlockId, lastFarmingLocationDiagnostic, pestWebhookUrl;
    static boolean jerryAutoSethomeEnabled;
    static String farmingRecoveryOrigin="";
    static final List<String> commands=new ArrayList<>();
    static boolean commandAccepted=true;

    static NotificationChannels notificationChannels(NotificationType type){return NOTIFICATION_CHANNELS.getOrDefault(type,NotificationChannels.NONE);}
    static final List<String> chat=new ArrayList<>();
    static String plain(String s){return s;}static String warning(String s){return s;}static void say(Minecraft mc,String text){chat.add(text);}
    static void sendNotification(NotificationType type,String content,String title,String chatContent){sendNotification(type,content,title);if(notificationChannels(type).chat())chat.add(chatContent);}
    static Object activeTarget;
    static int limboChatSeenTick, saves, releases;
    static final List<String> FARMING_COMMAND_QUEUE = new ArrayList<>();
    static final List<String> sent = new ArrayList<>();
    static final List<String> titles = new ArrayList<>();
    static final Guard FARMING_GUI_PAUSE_GUARD = new Guard(), MINING_GUI_PAUSE_GUARD = new Guard(), FARMING_TOOL_GUARD = new Guard();
    static class Guard { int resets; void reset() { resets++; } void reset(Object ignored) { resets++; } }
    static class Key { boolean down; void setDown(boolean value) { down=value; } }
    static class Options { Key keyAttack = new Key(); }
    static class GameMode { int stops; void stopDestroyBlock() { stops++; } }
    static class Minecraft {
        static final Minecraft INSTANCE = new Minecraft();
        Options options = new Options(); GameMode gameMode = new GameMode();
        static class Player {Connection connection=new Connection();}
        static class Connection {void sendCommand(String command){commands.add(command);if(!commandAccepted)throw new IllegalStateException("fixture send error");if(automation!=null)automation.onOutgoingCommand();}}
        Player player=new Player();boolean isSameThread(){return true;}
        static Minecraft getInstance() { return INSTANCE; }
    }
    enum FarmingLocation { LIMBO }
    static int farmingTicksNow() { return 100; }
    static List<String> sidebarLines(Minecraft mc) { return List.of(); }
    static void startFarmingAutoRecovery(Minecraft mc,FarmingLocation l) { throw new AssertionError("unexpected recovery"); }
    static void applyLockedMovement(Minecraft mc,boolean f,boolean b,boolean l,boolean r,boolean j) {
        check(!f && !b && !l && !r && !j,"all movement released"); releases++;
    }
    static void sendNotification(NotificationType type, String message, String title) { if (NOTIFICATION_CHANNELS.getOrDefault(type,NotificationChannels.NONE).available(pestWebhookUrl)) { sent.add(message); titles.add(title); } }
    static void saveConfig() { saves++; }
    static void sayAutoState(Minecraft mc,String feature,boolean enabled) {}
    static class StringUtil { static String stripColor(String text) { return text.replaceAll("(?i)§[0-9a-fk-or]", ""); } }
    record Component(String getString) { static Component literal(String text) { return new Component(text); } }
    static void check(boolean ok,String message) { if (!ok) throw new AssertionError(message); }
    static final List<String> TEMPLATES=List.of(
                "You found a %s Jerry!",
                "You discovered a %s Jerry!",
                "Some %s Jerry was hiding, but you found it!",
                "wild %s Jerry spawned!",
                "%s Jerry appeared!",
                "There is a %s Jerry!",
                "You located a hidden %s Jerry!");
    static final String APPEARED="§b ☺ §eA §aGreen Jerry§e appeared!";
    static void reset() {
        automation=null;farmingMarkersCreated=0;commandAccepted=true;jerryAutoSethomeEnabled=false;commands.clear();chat.clear();
        FARMING_STALL_GUARD.reset();
        jerryColors=Set.of(JerryColor.values());
        NOTIFICATION_CHANNELS.clear();NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(true,false,false));jerryStopActionsEnabled=true;
        miningEnabled=farmingEnabled=farmingAutoRecovering=farmingEnabledThisSession=farmingWorldChanged=false;
        farmingDesiredForward=farmingDesiredJump=true;
        pestWebhookUrl="test-only";sent.clear();titles.clear();saves=0;releases=0;
        activeTarget=new Object();activeTargetStarted=true;activeTargetBlockId="stone";
        FARMING_COMMAND_QUEUE.clear();
        Minecraft.getInstance().options.keyAttack.down=true;
    }
    public static void main(String[] args) {
        Minecraft mc=Minecraft.getInstance();
        reset();jerryAutoSethomeEnabled=true;commands.clear();
        handleJerryMessage(mc,"You found a Green Jerry!");
        check(commands.equals(List.of("sethome")),"Jerry sends sethome independently of channels");
        jerryColors=Set.of();commands.clear();handleJerryMessage(mc,"You found a Green Jerry!");
        check(commands.isEmpty(),"all colors disabled means no auto sethome");
        jerryAutoSethomeEnabled=false;

        handleReceivedMessage(null);
        for (String color : List.of("Green", "Blue", "Purple", "Golden"))
        for (String template : TEMPLATES)
        for (String raw : List.of(template.formatted(color), template.formatted(color).toUpperCase(Locale.ROOT).replace(" ", "  ")))
        for (String message : List.of(raw,
                "§b[Prefix]§e" + raw.replace("Jerry", "§aJerry§e") + "§r[Suffix]",
                "prefix" + raw + "suffix",
                "prefix\n" + raw + "\nsuffix"))
        for(int notify=0;notify<2;notify++) for(int stop=0;stop<2;stop++) {
            reset();NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(notify==1,false,false));jerryStopActionsEnabled=stop==1;
            miningEnabled=true;
            handleReceivedMessage(Component.literal(message));
            check(sent.size()==notify,"independent notification option: " + message);
            if(notify==1) {
                check(sent.get(0).equals("Jerry 通知：發現 "+color+" Jerry！"),"Discord/chat text includes actual color: "+message);
                check(titles.get(0).equals("發現 "+color+" Jerry！"),"screen text includes actual color: "+message);
            }
            check(miningEnabled==(stop==0),"independent stop option: " + message);
            check(farmingMarkersCreated==0,"Mining-only Jerry stop never creates a Farming beam");
            if(stop==1) {
                check(activeTarget==null && !activeTargetStarted,"mining target cleared");
                check(!mc.options.keyAttack.down && releases==1,"attack and movement released");
                check(!shouldLockManualAttack() && !shouldLockManualLook(),"manual input unlocked");
            }
        }
        for(int mask=0;mask<16;mask++) {
            EnumSet<JerryColor> enabled=EnumSet.noneOf(JerryColor.class);
            for(JerryColor color:JerryColor.values()) if((mask & (1<<color.ordinal()))!=0) enabled.add(color);
            for(JerryColor color:JerryColor.values()) for(String template:TEMPLATES) {
                reset();jerryColors=Set.copyOf(enabled);miningEnabled=true;
                handleReceivedMessage(Component.literal("§a[Green Jerry] " + template.formatted("§6"+color.label+"§r") + " [Golden Jerry]"));
                boolean selected=enabled.contains(color);
                check(sent.size()==(selected?1:0),"only selected color not prefix/suffix colors: "+mask+color+template);
                if(selected) {
                    check(sent.get(0).equals("Jerry 通知：發現 "+color.label+" Jerry！"),"display actual matched color, not unrelated prefix/suffix");
                    check(titles.get(0).equals("發現 "+color.label+" Jerry！"),"display same matched color on screen");
                }
                check(miningEnabled==!selected,"disabled color cannot stop automation: "+mask+color);
                if(!selected) check(activeTarget!=null && mc.options.keyAttack.down && releases==0 && saves==0,"disabled color has no side effects");
            }
        }
        reset();jerryColors=Set.of(JerryColor.GREEN,JerryColor.GOLDEN);
        handleReceivedMessage(Component.literal("You found a Blue Jerry! You found a Golden Jerry!"));
        check(sent.size()==1,"find enabled second event in same message after disabled event");
        check(sent.get(0).equals("Jerry 通知：發現 Golden Jerry！") && titles.get(0).equals("發現 Golden Jerry！"),"display enabled Golden match, not earlier disabled Blue");
        reset();NOTIFICATION_CHANNELS.clear();jerryStopActionsEnabled=false;miningEnabled=true;
        handleReceivedMessage(Component.literal(APPEARED));
        check(miningEnabled && sent.isEmpty(),"no channels and stop off does nothing without needing a master");
        reset();farmingEnabled=farmingEnabledThisSession=true;farmingWorldChanged=true;
        handleReceivedMessage(Component.literal("A Golden Jerry appeared!"));
        check(!farmingEnabled && !farmingEnabledThisSession && !farmingWorldChanged,"farming session stopped");
        check(farmingMarkersCreated==1,"Jerry stopping Farming creates its return beam");
        check(!mc.options.keyAttack.down && !farmingDesiredForward && !farmingDesiredJump,"farming inputs cleared");
        reset();farmingEnabled=true;jerryStopActionsEnabled=false;
        handleReceivedMessage(Component.literal(APPEARED));check(farmingMarkersCreated==0 && farmingEnabled,"notify-only cannot create beam");
        reset();farmingEnabled=true;jerryColors=Set.of(JerryColor.GOLDEN);
        handleReceivedMessage(Component.literal(APPEARED));check(farmingMarkersCreated==0 && farmingEnabled,"disabled color cannot create beam");
        reset();farmingAutoRecovering=true;FARMING_COMMAND_QUEUE.add("warp garden");
        handleReceivedMessage(Component.literal("A Purple Jerry appeared!"));
        check(!farmingAutoRecovering && FARMING_COMMAND_QUEUE.isEmpty(),"pending recovery cancelled");
        reset();pestWebhookUrl="";miningEnabled=true;
        handleReceivedMessage(Component.literal(APPEARED));
        check(!miningEnabled && sent.isEmpty(),"missing webhook cannot prevent stop");
        reset();handleReceivedMessage(Component.literal("GREEN   JERRY   appeared!"));
        check(sent.size()==1 && releases==0,"case/spacing and idle notification without input changes");
        for(String wrong:List.of("Jerry appeared!", "Red Jerry appeared!", "You discovered a Custom Color Jerry!", "You located a hidden Jerry!", "Jerry", "Jerry disappeared!", "Jerry appearedness", "A Green Jerry", "Hello", "You located a hidden Chest!", "You discovered a Sword!", "You discovered a Jerryfish!", "You located a hidden Jerryfish!", "You discovered a Sword! Jerry is nearby", "You found a Blue Jerryfish!", "wild Blue Jerry disappeared!", "wild Blue Jerry spawnedness!", "Some Blue Jerry was hiding, but you missed it!", "Some Blue Jerry was hiding!", "There is a Blue Jerryfish!")) {
            reset();miningEnabled=true;handleReceivedMessage(Component.literal(wrong));
            check(miningEnabled && sent.isEmpty(),"ignore non-appearance: "+wrong);
        }
        reset();NOTIFICATION_CHANNELS.clear();miningEnabled=true;
        handleReceivedMessage(Component.literal(APPEARED));
        check(!miningEnabled && sent.isEmpty(),"stop actions independent of all channels being off");
        reset();pestWebhookUrl="";NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,true,false));
        handleReceivedMessage(Component.literal(APPEARED));
        check(sent.size()==1,"local-only Jerry notifications work with no master or Webhook");
        reset();jerryAutoSethomeEnabled=true;NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,true,false));
        handleReceivedMessage(Component.literal(APPEARED));
        check(commands.equals(List.of("sethome"))&&chat.size()==1&&chat.get(0).equals("發現 Green Jerry，且已發送 /sethome"),"Jerry and auto sethome share exactly one chat line");
        reset();jerryAutoSethomeEnabled=true;NOTIFICATION_CHANNELS.clear();
        handleReceivedMessage(Component.literal(APPEARED));check(commands.size()==1&&chat.size()==1&&chat.get(0).equals("已發送 /sethome。"),"auto sethome reports even when Jerry chat is disabled");
        reset();jerryAutoSethomeEnabled=true;jerryColors=Set.of();handleReceivedMessage(Component.literal(APPEARED));check(commands.isEmpty()&&chat.isEmpty(),"unselected Jerry never sends command or feedback");
        reset();jerryAutoSethomeEnabled=true;commandAccepted=false;NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,true,false));
        handleReceivedMessage(Component.literal(APPEARED));check(chat.size()==1&&chat.get(0).contains("Green Jerry")&&chat.get(0).contains("發送失敗"),"failed auto sethome also joins detection on one chat line");
        reset();jerryAutoSethomeEnabled=true;commandAccepted=false;NOTIFICATION_CHANNELS.clear();
        handleReceivedMessage(Component.literal(APPEARED));check(chat.equals(List.of("無法發送 /sethome，請檢查連線。")),"failure without Jerry chat matches manual warning");
        for(JerryColor color:JerryColor.values())for(boolean enabled:new boolean[]{false,true}){
            reset();jerryAutoSethomeEnabled=true;NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(true,enabled,true));
            handleReceivedMessage(Component.literal("You found a "+color.label+" Jerry!"));
            check(chat.equals(List.of(enabled?"發現 "+color.label+" Jerry，且已發送 /sethome":"已發送 /sethome。")),"exact two success formats for each color");
        }
        for(var owner:List.of(AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER))for(boolean stop:new boolean[]{false,true})for(boolean chatEnabled:new boolean[]{false,true}){
            reset();farmingEnabled=farmingEnabledThisSession=true;jerryAutoSethomeEnabled=true;jerryStopActionsEnabled=stop;NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,chatEnabled,false));automation=new AutomationClientBridge();
            check(automation.session.begin(owner,System.nanoTime(),120_000_000_000L)!=null,"new actor holds original Farming");handleReceivedMessage(Component.literal(APPEARED));
            check(commands.equals(List.of("sethome")),"one automatic sethome through actual bridge/event bodies");
            check(chat.equals(List.of(chatEnabled?"發現 Green Jerry，且已發送 /sethome":"已發送 /sethome。")),"no additional chat line while holding new work");
            check(farmingEnabled==!stop&&automation.session.active()==!stop&&farmingMarkersCreated==(stop?1:0),"trusted auto command must preserve independent legacy stop toggle, active root and beam ordering: "+owner+stop);
            if(!stop){mc.player.connection.sendCommand("warp garden");check(!farmingEnabled&&!automation.session.active(),"a real subsequent manual command still preempts held work");}
        }
        for(boolean stop:new boolean[]{false,true})for(int actor=0;actor<6;actor++){
            reset();jerryStopActionsEnabled=stop;automation=new AutomationClientBridge();var service=automation.services().get(actor);service.active=true;
            var owner=List.of(AutomationFeature.PEST,AutomationFeature.GREENHOUSE,AutomationFeature.COMPOSTER,AutomationFeature.AUTO_SELL,AutomationFeature.JUNK,AutomationFeature.STASH).get(actor);
            if(owner!=AutomationFeature.STASH)check(automation.session.begin(owner,System.nanoTime(),120_000_000_000L)!=null,"standalone task fixture");
            handleReceivedMessage(Component.literal(APPEARED));
            check(service.active==!stop&&automation.services().stream().allMatch(v->v.suspended==stop)&&(!automation.session.active()||!stop)&&farmingMarkersCreated==0,"Jerry global stop must include all standalone/manual roots and separate Stash even without Farming: "+owner+stop);
        }
        reset();jerryStopActionsEnabled=true;automation=new AutomationClientBridge();handleReceivedMessage(Component.literal(APPEARED));check(automation.services().stream().allMatch(v->v.suspended)&&!automation.session.active()&&farmingMarkersCreated==0,"Jerry global stop must suspend idle enabled timers, not wait for an active legacy Farm");
        System.out.println("JerryDetectionIntegrationTest: PASS (seven message variants/four colors/all 16 color switch combinations/prefix/suffix, action combinations, cleanup, recovery, no master, independent channels)");
    }
    // METHODS
}
'''.replace('    // METHODS',methods).replace('  // BRIDGE_METHODS',bridge_methods)
java_home=Path(sys.argv[1])
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'JerryDetectionIntegrationTest.java'
    path.write_text(harness)
    gson=Path.home()/'Library/Application Support/PrismLauncher/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar'
    subprocess.run([str(java_home/'bin/javac'),'--release','21','-encoding','UTF-8','-cp',str(gson),'-d',tmp,*map(str,(base/'automation').rglob('*.java')),str(base/'JerryMessageMatcher.java'),str(base/'JerryColor.java'),str(base/'FarmingStallGuard.java'),str(base/'NotificationType.java'),str(base/'NotificationChannels.java'),str(path)],check=True)
    subprocess.run([str(java_home/'bin/java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.JerryDetectionIntegrationTest'],check=True)

    mutated=harness.replace('boolean autoSethomeSent=jerryAutoSethomeEnabled&&sendJerryHomeCommand(mc);','boolean autoSethomeSent=jerryAutoSethomeEnabled&&sendHomeCommand(mc,"sethome",false);').replace('JerryDetectionIntegrationTest','JerryOriginMutationTest')
    assert mutated!=harness
    path=Path(tmp)/'JerryOriginMutationTest.java';path.write_text(mutated)
    subprocess.run([str(java_home/'bin/javac'),'--release','21','-encoding','UTF-8','-cp',tmp+':'+str(gson),'-d',tmp,str(path)],check=True)
    result=subprocess.run([str(java_home/'bin/java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.JerryOriginMutationTest'],capture_output=True,text=True)
    assert result.returncode!=0 and 'trusted auto command must preserve independent legacy stop toggle' in result.stderr,'Regression must reject the old manual-sender Jerry route'
    print('JerryOriginMutationTest: PASS (old automatic-home route through manual command sender rejected by actual composed legacy/event/hold regression)')

    stopped=harness.replace('if(jerryStopActionsEnabled&&automation!=null)automation.suspendForJerry();','if(jerryStopActionsEnabled&&(miningEnabled||farmingEnabled||farmingAutoRecovering)&&automation!=null)automation.suspendForJerry();').replace('JerryDetectionIntegrationTest','JerryGlobalStopMutationTest')
    assert stopped!=harness
    path=Path(tmp)/'JerryGlobalStopMutationTest.java';path.write_text(stopped)
    subprocess.run([str(java_home/'bin/javac'),'--release','21','-encoding','UTF-8','-cp',tmp+':'+str(gson),'-d',tmp,str(path)],check=True)
    result=subprocess.run([str(java_home/'bin/java'),'-cp',tmp+':'+str(gson),'dev.kuan.kuanperion.JerryGlobalStopMutationTest'],capture_output=True,text=True)
    assert result.returncode!=0 and 'Jerry global stop must include' in result.stderr,'Regression must reject legacy-only global stop'
    print('JerryGlobalStopMutationTest: PASS (legacy-Farm-only stop rejected by composed standalone/idle regression)')
