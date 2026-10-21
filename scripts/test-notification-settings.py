#!/usr/bin/env python3
"""Execute production notification routing/config/modal code with fake game/network boundaries."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
root=Path(__file__).resolve().parent.parent
base=root/'src/main/java/dev/kuan/skyblockassistant'
source=(base/'KuanSkyBlockAssistantClient.java').read_text()
def method(name):
    match=re.search(r'    private static [^\n]+ '+name+r'\(',source)
    assert match, f'Missing production method {name}'
    start=match.start(); opening=source.index('{',start); depth=1; end=opening+1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}'); end+=1
    return source[start:end]
methods='\n'.join(method(n) for n in ['sendNotification','hasNotificationChannel','loadNotificationConfig','saveNotificationConfig','openNotificationSettings','renderNotificationOverlay'])
classes=[]
for name in ['NotificationSettingsScreen','WebhookSettingsScreen']:
    text=(base/(name+'.java')).read_text()
    classes.append(text[text.index('final class '+name):].replace('final class '+name,'static final class '+name,1))
assert 'Discord Webhook:' not in source, 'Remove legacy footer Webhook field'
other=source[source.index('addSectionTitle("Other", y)'):source.index('private void addSectionTitle')]
assert other.index('Component.literal("通知設定")')<other.index('toggleMsg("Jerry 偵測"'), 'Settings left; Jerry right'
assert 'toggleJerryDetection(this)).bounds(x + 145, y, 130, 20)' in other
assert 'HudElementRegistry.addLast' in source and 'renderNotificationOverlay' in source
harness=r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.function.*;
import java.net.URI;
public class NotificationSettingsTest {
    static boolean discordNotificationEnabled, chatNotificationEnabled, screenNotificationEnabled, jerryNotificationEnabled;
    static String pestWebhookUrl="";
    static final ScreenNotificationQueue SCREEN_NOTIFICATIONS=new ScreenNotificationQueue();
    static final List<String> discord=new ArrayList<>(),chat=new ArrayList<>();
    static int saves;
    static void sendDiscordNotification(String text) { discord.add(text); }
    static void say(Minecraft mc,Component text) { if(mc.player!=null) chat.add(text.getString()); }
    static Component plain(String text) { return Component.literal(text); }
    static void saveConfig() { saves++; }
    enum ChatFormatting { WHITE,GREEN,RED }
    static class Component {
        String text; Component(String text) { this.text=text; }
        static Component literal(String text) { return new Component(text); }
        String getString() { return text; }
        Component withStyle(ChatFormatting style) { return this; }
        Component append(Component more) { text+=more.text; return this; }
    }
    static class Minecraft {
        static final Minecraft INSTANCE=new Minecraft(); Object player=new Object(),level=new Object(); Screen shown; Gui gui=new Gui(); Font font=new Font();
        static Minecraft getInstance() { return INSTANCE; }
        void setScreenAndShow(Screen screen) { shown=screen; if(screen!=null) { screen.widgets.clear();screen.init(); } }
    }
    static class Hud { boolean hidden; boolean isHidden() { return hidden; } }
    static class Gui { Hud hud=new Hud(); }
    static class Font { int lineHeight=9; int width(String text) { return text.length()*8; } }
    static class Pose {
        int depth;float x,y,scaleX,scaleY;
        void pushMatrix() { depth++; }
        void popMatrix() { depth--; }
        void translate(float x,float y) { this.x=x;this.y=y; }
        void scale(float x,float y) { scaleX=x;scaleY=y; }
    }
    static class GuiGraphicsExtractor {
        int width=960,height=540,draws;String text;boolean shadow;Pose stack=new Pose();
        int guiWidth() { return width; } int guiHeight() { return height; } Pose pose() { return stack; }
        void text(Font font,String text,int x,int y,int color,boolean shadow) {
            check(x==-font.width(text)/2 && y==-font.lineHeight/2,"text anchor centered");
            this.text=text;this.shadow=shadow;draws++;
        }
    }
    static class Screen {
        int width=960,height=540;Object font=new Object();List<Object> widgets=new ArrayList<>();
        Screen(Component title) {}
        protected void init() {}
        <T> T addRenderableWidget(T widget) { widgets.add(widget);return widget; }
        void setInitialFocus(Object widget) {}
        public void onClose() {}
        public boolean isPauseScreen() { return true; }
    }
    static class SettingsScreen extends Screen { SettingsScreen() { super(Component.literal("main")); } }
    static class StringWidget { StringWidget(Object... args) {} }
    static class EditBox {
        String value="";int maxLength=300;Consumer<String> responder;
        EditBox(Object... args) {}
        void setMaxLength(int n) { maxLength=n; }
        String getValue() { return value; }
        void setValue(String value) { this.value=value.substring(0,Math.min(value.length(),maxLength));if(responder!=null) responder.accept(this.value); }
        void setResponder(Consumer<String> cb) { responder=cb; }
        void setTextColor(int color) {}
    }
    static class Button {
        boolean active=true;Component label;Consumer<Button> callback;
        static Builder builder(Component label,Consumer<Button> cb) { return new Builder(label,cb); }
        void setMessage(Component value) { label=value; }
        static class Builder {
            Component label;Consumer<Button> cb;
            Builder(Component label,Consumer<Button> cb) { this.label=label;this.cb=cb; }
            Builder bounds(int x,int y,int width,int height) { return this; }
            Button build() { Button b=new Button();b.label=label;b.callback=cb;return b; }
        }
    }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static Button button(Screen screen,String prefix) { return screen.widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.label.getString().startsWith(prefix)).findFirst().orElseThrow(); }
    static void click(Screen screen,String prefix) { Button b=button(screen,prefix);b.callback.accept(b); }
    static EditBox input(Screen screen) { return (EditBox)screen.widgets.stream().filter(EditBox.class::isInstance).findFirst().orElseThrow(); }
    public static void main(String[] args) {
        Minecraft mc=Minecraft.getInstance();
        for(int mask=0;mask<8;mask++) {
            discordNotificationEnabled=(mask&1)!=0;chatNotificationEnabled=(mask&2)!=0;screenNotificationEnabled=(mask&4)!=0;
            pestWebhookUrl="https://example.test/webhook";discord.clear();chat.clear();SCREEN_NOTIFICATIONS.clear();
            sendNotification("event message","event title");
            check(discord.size()==((mask&1)!=0?1:0),"independent Discord channel "+mask);
            check(chat.size()==((mask&2)!=0?1:0),"independent chat channel "+mask);
            check((SCREEN_NOTIFICATIONS.current(System.nanoTime())!=null)==((mask&4)!=0),"independent screen channel "+mask);
            check(hasNotificationChannel()==(mask!=0),"effective channels "+mask);
        }
        discordNotificationEnabled=chatNotificationEnabled=screenNotificationEnabled=true;
        pestWebhookUrl="";discord.clear();chat.clear();SCREEN_NOTIFICATIONS.clear();
        sendNotification("local only","local title");
        check(discord.isEmpty() && chat.size()==1 && SCREEN_NOTIFICATIONS.current(System.nanoTime())!=null,"blank Webhook cannot block local channels");
        chatNotificationEnabled=screenNotificationEnabled=false;
        check(!hasNotificationChannel(),"blank Discord-only channel is unavailable");
        loadNotificationConfig(new Properties());
        check(!discordNotificationEnabled && !chatNotificationEnabled && !screenNotificationEnabled,"new installations start without a configured channel");
        Properties legacy=new Properties();legacy.setProperty("pestWebhookUrl","https://example.test/old");legacy.setProperty("jerryDiscordNotificationEnabled","false");
        loadNotificationConfig(legacy);
        check(discordNotificationEnabled && !chatNotificationEnabled && !screenNotificationEnabled,"legacy defaults preserve Discord");
        check(pestWebhookUrl.equals("https://example.test/old") && !jerryNotificationEnabled,"preserve existing Webhook and Jerry preference");
        discordNotificationEnabled=false;chatNotificationEnabled=screenNotificationEnabled=jerryNotificationEnabled=true;
        Properties saved=new Properties();saveNotificationConfig(saved);loadNotificationConfig(saved);
        check(!discordNotificationEnabled && chatNotificationEnabled && screenNotificationEnabled && jerryNotificationEnabled,"all settings roundtrip");
        check(!saved.containsKey("jerryDiscordNotificationEnabled"),"migrate old Jerry notification property");
        long second=1_000_000_000L;
        ScreenNotificationQueue queue=new ScreenNotificationQueue();
        queue.add("one",0);queue.add("two",0);
        check(queue.current(0).equals("one") && queue.current(3*second-1).equals("one"),"first alert lasts full three seconds");
        check(queue.current(3*second).equals("two"),"simultaneous second alert queued, not overwritten");
        check(queue.current(6*second-1).equals("two") && queue.current(6*second)==null,"second alert also lasts three seconds");
        queue.add("new",10*second);queue.clear();check(queue.current(10*second)==null,"disabling screen clears pending alerts");
        screenNotificationEnabled=true;SCREEN_NOTIFICATIONS.clear();SCREEN_NOTIFICATIONS.add("Jerry!",System.nanoTime());
        GuiGraphicsExtractor graphics=new GuiGraphicsExtractor();renderNotificationOverlay(graphics);
        check(graphics.draws==1 && graphics.stack.x==480 && graphics.stack.y==270,"overlay centered in viewport");
        check(graphics.stack.scaleX==4 && graphics.stack.scaleY==4 && graphics.stack.depth==0 && graphics.shadow,"large shadowed text restores matrix");
        SCREEN_NOTIFICATIONS.clear();SCREEN_NOTIFICATIONS.add("x".repeat(200),System.nanoTime());renderNotificationOverlay(graphics);
        check(graphics.stack.scaleX<4 && graphics.stack.scaleX*mc.font.width(graphics.text)<=graphics.width-32,"long labels fit viewport");
        mc.gui.hud.hidden=true;int drawn=graphics.draws;renderNotificationOverlay(graphics);
        check(graphics.draws==drawn,"F1 respects hidden HUD");mc.gui.hud.hidden=false;
        mc.level=null;renderNotificationOverlay(graphics);
        check(SCREEN_NOTIFICATIONS.current(System.nanoTime())==null,"disconnect clears stale screen alerts");mc.level=new Object();
        SettingsScreen main=new SettingsScreen();
        openNotificationSettings(main);
        Screen settings=mc.shown;
        check(settings instanceof NotificationSettingsScreen,"notification settings modal opens");
        check(button(settings,"Discord 訊息通知").label.getString().endsWith("關閉"),"Discord starts off from saved config");
        int before=saves;
        click(settings,"Discord 訊息通知");
        check(mc.shown instanceof WebhookSettingsScreen,"enabling Discord opens Webhook entry");
        Screen webhook=mc.shown;
        check(input(webhook).getValue().equals(pestWebhookUrl),"existing Webhook prefilled");
        for(String invalid:List.of("", "abc", "ftp://example.test/hook", "https://", "https://bad host/hook")) {
            input(webhook).setValue(invalid);Button confirm=button(webhook,"確認");
            check(!confirm.active,"invalid Webhook cannot confirm");confirm.callback.accept(confirm);
            check(!discordNotificationEnabled && saves==before,"invalid input never enables Discord");
        }
        click(webhook,"取消");check(mc.shown==settings && saves==before,"cancel leaves Discord off and settings unchanged");
        click(settings,"Discord 訊息通知");webhook=mc.shown;webhook.onClose();
        check(mc.shown==settings && !discordNotificationEnabled && saves==before,"Esc leaves Discord off");
        click(settings,"Discord 訊息通知");webhook=mc.shown;
        input(webhook).setValue(" https://example.test/new ");click(webhook,"確認");
        check(discordNotificationEnabled && pestWebhookUrl.equals("https://example.test/new") && mc.shown==settings,"confirm saves trimmed URL and enables Discord");
        boolean oldChat=chatNotificationEnabled,oldScreen=screenNotificationEnabled;
        click(settings,"遊戲聊天室通知");
        check(chatNotificationEnabled!=oldChat && screenNotificationEnabled==oldScreen && discordNotificationEnabled,"chat independent");
        SCREEN_NOTIFICATIONS.add("pending",System.nanoTime());click(settings,"遊戲螢幕通知");
        check(!screenNotificationEnabled && SCREEN_NOTIFICATIONS.current(System.nanoTime())==null,"screen disabled clears queue immediately");
        click(settings,"Discord 訊息通知");
        check(!discordNotificationEnabled && pestWebhookUrl.equals("https://example.test/new"),"turn off Discord keeps URL");
        click(settings,"完成");check(mc.shown==main,"Done returns to main settings");
        System.out.println("NotificationSettingsTest: PASS (all eight channel combinations, migration, three-second queue/centered renderer, Webhook modal, toggles)");
    }
    // METHODS
    // SCREENS
}
'''.replace('    // METHODS',methods).replace('    // SCREENS','\n'.join(classes))
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'NotificationSettingsTest.java';path.write_text(harness)
    java=Path(sys.argv[1])/'bin'
    subprocess.run([str(java/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,str(base/'ScreenNotificationQueue.java'),str(path)],check=True)
    subprocess.run([str(java/'java'),'-cp',tmp,'dev.kuan.skyblockassistant.NotificationSettingsTest'],check=True)
