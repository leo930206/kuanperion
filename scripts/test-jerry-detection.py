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
    'handleReceivedMessage', 'handleJerryMessage', 'stopFarming', 'clearActiveTarget',
    'setAttackKey', 'releaseFarmingKeys', 'releaseFarmingMovementKeys',
    'shouldLockManualLook', 'shouldLockManualAttack'])
assert 'p.setProperty("jerryStopActionsEnabled"' in client
assert 'toggleMsg("Jerry 偵測"' not in client
assert 'Component.literal("Jerry 偵測")' in client
harness = r'''package dev.kuan.kuanperion;
import java.util.*;
import java.util.function.*;
public class JerryDetectionIntegrationTest {
 static void invalidateAutomationFarmSession(){}
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
    static boolean sendHomeCommand(Minecraft mc,String command,boolean announce){commands.add(command);return commandAccepted;}
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
        farmingMarkersCreated=0;commandAccepted=true;jerryAutoSethomeEnabled=false;commands.clear();chat.clear();
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
        System.out.println("JerryDetectionIntegrationTest: PASS (seven message variants/four colors/all 16 color switch combinations/prefix/suffix, action combinations, cleanup, recovery, no master, independent channels)");
    }
    // METHODS
}
'''.replace('    // METHODS',methods)
java_home=Path(sys.argv[1])
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'JerryDetectionIntegrationTest.java'
    path.write_text(harness)
    subprocess.run([str(java_home/'bin/javac'),'--release','21','-encoding','UTF-8','-d',tmp,str(base/'JerryMessageMatcher.java'),str(base/'JerryColor.java'),str(base/'FarmingStallGuard.java'),str(base/'NotificationType.java'),str(base/'NotificationChannels.java'),str(path)],check=True)
    subprocess.run([str(java_home/'bin/java'),'-cp',tmp,'dev.kuan.kuanperion.JerryDetectionIntegrationTest'],check=True)
