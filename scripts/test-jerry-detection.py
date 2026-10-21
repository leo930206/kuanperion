#!/usr/bin/env python3
"""Run production Jerry chat/actions and modal callbacks with fake game boundaries."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
base = root / 'src/main/java/dev/kuan/skyblockassistant'
client = (base / 'KuanSkyBlockAssistantClient.java').read_text()
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
    'shouldLockManualLook', 'shouldLockManualAttack', 'toggleJerryDetection'])
screen = (base / 'JerrySettingsScreen.java').read_text()
screen = screen[screen.index('final class JerrySettingsScreen'):].replace(
    'final class JerrySettingsScreen', 'static final class JerrySettingsScreen', 1)
for key in ['jerryDetectionEnabled', 'jerryNotificationEnabled', 'jerryStopActionsEnabled']:
    assert f'p.getProperty("{key}"' in client, f'Load {key}'
    assert f'p.setProperty("{key}"' in client, f'Save {key}'
assert client.index('addSectionTitle("Other", y)') > client.index('int maxRows = Math.min(5, TARGET_BLOCKS.size())')
assert 'toggleMsg("Jerry 偵測", jerryDetectionEnabled)' in client
assert 'toggleJerryDetection(this)' in client
harness = r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.function.*;
public class JerryDetectionIntegrationTest {
    static boolean jerryDetectionEnabled, jerryNotificationEnabled, jerryStopActionsEnabled;
    static boolean miningEnabled, farmingEnabled, farmingAutoRecovering, farmingEnabledThisSession;
    static boolean farmingWorldChanged, activeTargetStarted;
    static boolean farmingDesiredForward, farmingDesiredBack, farmingDesiredLeft, farmingDesiredRight, farmingDesiredJump;
    static boolean abnormalHandlingEnabled;
    static String activeTargetBlockId, lastFarmingLocationDiagnostic, pestWebhookUrl;
    static Object activeTarget;
    static int limboChatSeenTick, saves, releases;
    static final List<String> FARMING_COMMAND_QUEUE = new ArrayList<>();
    static final List<String> sent = new ArrayList<>();
    static final Guard FARMING_GUI_PAUSE_GUARD = new Guard(), MINING_GUI_PAUSE_GUARD = new Guard(), FARMING_TOOL_GUARD = new Guard();
    static class Guard { int resets; void reset() { resets++; } void reset(Object ignored) { resets++; } }
    static class Key { boolean down; void setDown(boolean value) { down=value; } }
    static class Options { Key keyAttack = new Key(); }
    static class GameMode { int stops; void stopDestroyBlock() { stops++; } }
    static class Minecraft {
        static final Minecraft INSTANCE = new Minecraft();
        Options options = new Options(); GameMode gameMode = new GameMode(); Screen shown;
        static Minecraft getInstance() { return INSTANCE; }
        void setScreenAndShow(Screen value) { shown=value; if (value!=null) { value.widgets.clear(); value.init(); } }
    }
    enum FarmingLocation { LIMBO }
    static int farmingTicksNow() { return 100; }
    static List<String> sidebarLines(Minecraft mc) { return List.of(); }
    static void startFarmingAutoRecovery(Minecraft mc,FarmingLocation l) { throw new AssertionError("unexpected recovery"); }
    static void applyLockedMovement(Minecraft mc,boolean f,boolean b,boolean l,boolean r,boolean j) {
        check(!f && !b && !l && !r && !j,"all movement released"); releases++;
    }
    static void sendNotification(String message, String title) { if (!pestWebhookUrl.isBlank()) sent.add(message); }
    static void saveConfig() { saves++; }
    static void sayAutoState(Minecraft mc,String feature,boolean enabled) {}
    static class StringUtil { static String stripColor(String text) { return text.replaceAll("(?i)§[0-9a-fk-or]", ""); } }
    enum ChatFormatting { WHITE, GREEN, RED }
    static class Component {
        String text;
        Component(String text) { this.text=text; }
        static Component literal(String text) { return new Component(text); }
        String getString() { return text; }
        Component withStyle(ChatFormatting value) { return this; }
        Component append(Component value) { text+=value.text; return this; }
    }
    static class Screen {
        int width=960,height=540; Object font=new Object(); List<Object> widgets=new ArrayList<>();
        Screen(Component title) {}
        protected void init() {}
        <T> T addRenderableWidget(T widget) { widgets.add(widget); return widget; }
        public void onClose() {}
        public boolean isPauseScreen() { return true; }
    }
    static class SettingsScreen extends Screen {
        SettingsScreen() { super(Component.literal("settings")); }
        int rebuilds;
        void rebuild() { rebuilds++; Minecraft.getInstance().setScreenAndShow(this); }
    }
    static class StringWidget { StringWidget(Object... args) {} }
    static class Button {
        Component label; Consumer<Button> callback; int width,height;
        static Builder builder(Component label,Consumer<Button> callback) { return new Builder(label,callback); }
        void setMessage(Component value) { label=value; }
        static class Builder {
            Component label; Consumer<Button> callback; int width,height;
            Builder(Component l,Consumer<Button> cb) { label=l; callback=cb; }
            Builder bounds(int x,int y,int w,int h) { width=w; height=h; return this; }
            Button build() { Button b=new Button(); b.label=label;b.callback=callback;b.width=width;b.height=height;return b; }
        }
    }
    static void check(boolean ok,String message) { if (!ok) throw new AssertionError(message); }
    static Button button(Screen screen,String prefix) {
        return screen.widgets.stream().filter(o->o instanceof Button).map(o->(Button)o)
            .filter(b->b.label.getString().startsWith(prefix)).findFirst().orElseThrow();
    }
    static void click(Screen screen,String prefix) { Button b=button(screen,prefix); b.callback.accept(b); }
    static final String APPEARED="§b ☺ §eA §aGreen Jerry§e appeared!";
    static void reset() {
        jerryDetectionEnabled=true; jerryNotificationEnabled=true;jerryStopActionsEnabled=true;
        miningEnabled=farmingEnabled=farmingAutoRecovering=farmingEnabledThisSession=farmingWorldChanged=false;
        farmingDesiredForward=farmingDesiredJump=true;
        pestWebhookUrl="test-only";sent.clear();saves=0;releases=0;
        activeTarget=new Object();activeTargetStarted=true;activeTargetBlockId="stone";
        FARMING_COMMAND_QUEUE.clear();
        Minecraft.getInstance().options.keyAttack.down=true;
    }
    public static void main(String[] args) {
        Minecraft mc=Minecraft.getInstance();
        handleReceivedMessage(null);
        for (String color : List.of("Green", "Blue", "Purple", "Golden", "Coler", "Custom Color", "Super-Rare", ""))
        for (String message : List.of(
                "§b ☺ §e" + color + " §aJerry§e appeared!",
                "§eYou located a hidden §a" + color + " Jerry§e!",
                "§eYou discovered a §a" + color + " Jerry§e!",
                "YOU  DISCOVERED  A  " + color.toUpperCase(Locale.ROOT) + "  JERRY!"))
        for(int notify=0;notify<2;notify++) for(int stop=0;stop<2;stop++) {
            reset();jerryNotificationEnabled=notify==1;jerryStopActionsEnabled=stop==1;
            miningEnabled=true;
            handleReceivedMessage(Component.literal(message));
            check(sent.size()==notify,"independent notification option: " + message);
            check(miningEnabled==(stop==0),"independent stop option: " + message);
            if(stop==1) {
                check(activeTarget==null && !activeTargetStarted,"mining target cleared");
                check(!mc.options.keyAttack.down && releases==1,"attack and movement released");
                check(!shouldLockManualAttack() && !shouldLockManualLook(),"manual input unlocked");
            }
        }
        reset();jerryDetectionEnabled=false;miningEnabled=true;
        handleReceivedMessage(Component.literal(APPEARED));
        check(miningEnabled && sent.isEmpty(),"disabled master does nothing");
        reset();farmingEnabled=farmingEnabledThisSession=true;farmingWorldChanged=true;
        handleReceivedMessage(Component.literal("A Golden Jerry appeared!"));
        check(!farmingEnabled && !farmingEnabledThisSession && !farmingWorldChanged,"farming session stopped");
        check(!mc.options.keyAttack.down && !farmingDesiredForward && !farmingDesiredJump,"farming inputs cleared");
        reset();farmingAutoRecovering=true;FARMING_COMMAND_QUEUE.add("warp garden");
        handleReceivedMessage(Component.literal("A Purple Jerry appeared!"));
        check(!farmingAutoRecovering && FARMING_COMMAND_QUEUE.isEmpty(),"pending recovery cancelled");
        reset();pestWebhookUrl="";miningEnabled=true;
        handleReceivedMessage(Component.literal(APPEARED));
        check(!miningEnabled && sent.isEmpty(),"missing webhook cannot prevent stop");
        reset();handleReceivedMessage(Component.literal("JERRY   appeared!"));
        check(sent.size()==1 && releases==0,"case/spacing and idle notification without input changes");
        for(String wrong:List.of("Jerry", "Jerry disappeared!", "Jerry appearedness", "A Green Jerry", "Hello", "You located a hidden Chest!", "You discovered a Sword!", "You discovered a Jerryfish!", "You located a hidden Jerryfish!", "You discovered a Sword! Jerry is nearby")) {
            reset();miningEnabled=true;handleReceivedMessage(Component.literal(wrong));
            check(miningEnabled && sent.isEmpty(),"ignore non-appearance: "+wrong);
        }
        reset();jerryDetectionEnabled=false;jerryNotificationEnabled=false;jerryStopActionsEnabled=true;
        SettingsScreen parent=new SettingsScreen();
        toggleJerryDetection(parent);
        check(mc.shown instanceof JerrySettingsScreen && !jerryDetectionEnabled,"opening modal does not enable yet");
        Screen modal=mc.shown;
        check(button(modal,"出現 Jerry 是否通知").label.getString().endsWith("關閉"),"saved option shown");
        click(modal,"出現 Jerry 是否通知");click(modal,"出現 Jerry 是否停下動作");
        check(!jerryNotificationEnabled && jerryStopActionsEnabled,"modal edits remain local");
        click(modal,"取消");
        check(mc.shown==parent && !jerryDetectionEnabled && saves==0,"cancel changes nothing");
        toggleJerryDetection(parent);modal=mc.shown;click(modal,"出現 Jerry 是否通知");
        // Reinitialization must not reset the draft.
        modal.widgets.clear();modal.init();
        check(button(modal,"出現 Jerry 是否通知").label.getString().endsWith("開啟"),"draft survives resize");
        click(modal,"確認");
        check(jerryDetectionEnabled && jerryNotificationEnabled && jerryStopActionsEnabled,"confirm commits both options");
        check(saves==1 && parent.rebuilds==1,"confirm saves and returns to refreshed settings");
        toggleJerryDetection(parent);
        check(!jerryDetectionEnabled && saves==2,"enabled master toggles straight off");
        toggleJerryDetection(parent);mc.shown.onClose();
        check(!jerryDetectionEnabled && mc.shown==parent && saves==2,"Esc does not enable");
        System.out.println("JerryDetectionIntegrationTest: PASS (three message variants/any color, action combinations, cleanup, recovery, modal, cancellation)");
    }
    // METHODS
    // SCREEN
}
'''.replace('    // METHODS',methods).replace('    // SCREEN',screen)
java_home=Path(sys.argv[1])
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'JerryDetectionIntegrationTest.java'
    path.write_text(harness)
    subprocess.run([str(java_home/'bin/javac'),'--release','21','-encoding','UTF-8','-d',tmp,str(path)],check=True)
    subprocess.run([str(java_home/'bin/java'),'-cp',tmp,'dev.kuan.skyblockassistant.JerryDetectionIntegrationTest'],check=True)
