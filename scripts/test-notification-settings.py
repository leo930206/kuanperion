#!/usr/bin/env python3
"""Run per-feature notification routing, migration, shared URL, modal, and overlay tests."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
root=Path(__file__).resolve().parent.parent
base=root/'src/main/java/dev/kuan/skyblockassistant'
source=(base/'KuanSkyBlockAssistantClient.java').read_text()
def method(name):
    m=re.search(r'    private static [^\n]+ '+name+r'\(',source)
    assert m, f'Missing production method {name}'
    start=m.start();opening=source.index('{',start);depth=1;end=opening+1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}');end+=1
    return source[start:end]
methods='\n'.join(method(n) for n in ['notificationChannels','hasNotificationChannel','sendNotification','loadNotificationConfig','saveNotificationConfig','openNotificationSettings','renderNotificationOverlay'])
classes=[]
for name in ['NotificationSettingsScreen','WebhookSettingsScreen','PestThresholdScreen','VisitorThresholdScreen','ScreenDurationScreen']:
    s=(base/(name+'.java')).read_text()
    classes.append(s[s.index('final class '+name):].replace('final class '+name,'static final class '+name,1))
assert 'Component.literal("通知設定")' not in source, 'No global notification settings button'
assert 'toggleMsg("Jerry 偵測"' not in source, 'Jerry has no master toggle'
assert 'Component.literal("Jerry 偵測")' in source
harness=r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.function.*;
import java.net.URI;
public class NotificationSettingsTest {
    static final Map<NotificationType,NotificationChannels> NOTIFICATION_CHANNELS=new EnumMap<>(NotificationType.class);
    static final ScreenNotificationQueue SCREEN_NOTIFICATIONS=new ScreenNotificationQueue();
    static final GardenTabNotifications TAB_NOTIFICATIONS=new GardenTabNotifications();
    static boolean pestNotificationEnabled,visitorNotificationEnabled,jerryStopActionsEnabled;
    static int pestNotificationThreshold=1,visitorNotificationThreshold=5,saves;
    static String pestWebhookUrl="";
    record Sent(String text,String url) {}
    static final List<Sent> discord=new ArrayList<>();
    static final List<String> chat=new ArrayList<>();
    static void sendDiscordNotification(String text) { discord.add(new Sent(text,pestWebhookUrl)); }
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
        for(NotificationType type:NotificationType.values()) for(int mask=0;mask<8;mask++) {
            NOTIFICATION_CHANNELS.clear();
            NotificationChannels channels=new NotificationChannels((mask&1)!=0,(mask&2)!=0,(mask&4)!=0);
            NOTIFICATION_CHANNELS.put(type,channels);
            pestWebhookUrl="https://example.test/shared";discord.clear();chat.clear();SCREEN_NOTIFICATIONS.clear();
            sendNotification(type,"event","title");
            check(discord.size()==((mask&1)!=0?1:0),"feature Discord "+type+mask);
            check(chat.size()==((mask&2)!=0?1:0),"feature chat "+type+mask);
            check((SCREEN_NOTIFICATIONS.current(System.nanoTime())!=null)==((mask&4)!=0),"feature screen "+type+mask);
            check(hasNotificationChannel(type)==(mask!=0),"feature availability");
            for(NotificationType other:NotificationType.values()) if(other!=type)
                check(notificationChannels(other).equals(NotificationChannels.NONE),"other feature unchanged");
        }
        NOTIFICATION_CHANNELS.clear();
        NOTIFICATION_CHANNELS.put(NotificationType.PESTS,new NotificationChannels(false,true,false));
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,new NotificationChannels(true,false,false));
        NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,false,true));
        discord.clear();chat.clear();SCREEN_NOTIFICATIONS.clear();
        sendNotification(NotificationType.PESTS,"pest","pest");
        sendNotification(NotificationType.VISITORS,"visitor","visitor");
        sendNotification(NotificationType.JERRY,"jerry","jerry");
        check(chat.equals(List.of("pest")) && discord.size()==1 && discord.get(0).text().equals("visitor"),"mixed feature routing independent");
        check(SCREEN_NOTIFICATIONS.current(System.nanoTime()).equals("jerry"),"Jerry only uses screen");
        pestWebhookUrl="";check(hasNotificationChannel(NotificationType.PESTS) && !hasNotificationChannel(NotificationType.VISITORS),"shared blank URL only disables Discord availability");
        Properties legacy=new Properties();legacy.setProperty("pestWebhookUrl","https://example.test/old");
        legacy.setProperty("discordNotificationEnabled","true");legacy.setProperty("chatNotificationEnabled","true");legacy.setProperty("jerryDetectionEnabled","false");legacy.setProperty("jerryStopActionsEnabled","true");
        loadNotificationConfig(legacy);
        check(notificationChannels(NotificationType.PESTS).chat() && notificationChannels(NotificationType.VISITORS).discord(),"migrate global channels separately");
        check(notificationChannels(NotificationType.JERRY).equals(NotificationChannels.NONE) && !jerryStopActionsEnabled,"disabled legacy Jerry cannot start notifications or stop actions");
        legacy.setProperty("jerryDetectionEnabled","true");loadNotificationConfig(legacy);
        check(notificationChannels(NotificationType.JERRY).discord() && jerryStopActionsEnabled,"active legacy Jerry preserved");
        legacy.setProperty("jerryNotificationEnabled","false");loadNotificationConfig(legacy);
        check(notificationChannels(NotificationType.JERRY).equals(NotificationChannels.NONE) && jerryStopActionsEnabled,"stop independent of notifications");
        NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,false,true));
        jerryStopActionsEnabled=false;
        Properties saved=new Properties();saveNotificationConfig(saved);loadNotificationConfig(saved);
        check(notificationChannels(NotificationType.JERRY).screen() && !jerryStopActionsEnabled,"new settings roundtrip without Jerry master");
        check(!saved.containsKey("jerryDetectionEnabled") && !saved.containsKey("discordNotificationEnabled"),"do not save obsolete global/master switches");
        loadNotificationConfig(new Properties());
        check(!jerryStopActionsEnabled && notificationChannels(NotificationType.JERRY).equals(NotificationChannels.NONE),"safe fresh-install defaults");
        long second=1_000_000_000L;
        ScreenNotificationQueue q=new ScreenNotificationQueue();
        q.add(NotificationType.PESTS,"one",3,0);q.add(NotificationType.VISITORS,"two",3,0);
        check(q.current(3*second-1).equals("one") && q.current(3*second).equals("two"),"each screen alert lasts three seconds");
        check(q.current(6*second)==null,"screen expires exactly at boundary");
        q.add(NotificationType.PESTS,"pest",3,10*second);q.add(NotificationType.VISITORS,"visitor",3,10*second);
        q.remove(NotificationType.PESTS,10*second);
        check(q.current(10*second).equals("visitor"),"disabling one feature does not remove other feature alerts");
        q.add(NotificationType.JERRY,"jerry",3,10*second);q.remove(NotificationType.JERRY,10*second);
        check(q.current(10*second).equals("visitor") && q.current(13*second)==null,"remove pending alerts only for selected feature");
        SCREEN_NOTIFICATIONS.clear();SCREEN_NOTIFICATIONS.add(NotificationType.JERRY,"Jerry!",3,System.nanoTime());
        GuiGraphicsExtractor graphics=new GuiGraphicsExtractor();renderNotificationOverlay(graphics);
        check(graphics.draws==1 && graphics.stack.x==480 && graphics.stack.y==270 && graphics.stack.scaleX==4 && graphics.stack.depth==0,"centered large overlay");
        mc.gui.hud.hidden=true;int drawn=graphics.draws;renderNotificationOverlay(graphics);check(graphics.draws==drawn,"hidden HUD");mc.gui.hud.hidden=false;
        SettingsScreen parent=new SettingsScreen();
        pestNotificationEnabled=visitorNotificationEnabled=false;jerryStopActionsEnabled=false;
        NOTIFICATION_CHANNELS.clear();pestWebhookUrl="https://example.test/one";
        openNotificationSettings(parent,NotificationType.VISITORS);Screen visitor=mc.shown;
        click(visitor,"Visitor 通知");Screen threshold=mc.shown;
        check(threshold instanceof VisitorThresholdScreen,"visitor enable keeps 1-5 dialog");
        input(threshold).setValue("6");check(!button(threshold,"確認").active,"invalid visitor threshold");
        click(threshold,"取消");check(!visitorNotificationEnabled && mc.shown==visitor,"cancel cannot enable visitor");
        click(visitor,"Visitor 通知");threshold=mc.shown;input(threshold).setValue("3");click(threshold,"確認");
        check(visitorNotificationEnabled && visitorNotificationThreshold==3 && mc.shown==visitor,"visitor threshold confirmed");
        click(visitor,"Discord 訊息通知");Screen url=mc.shown;
        for(String invalid:List.of("", "abc", "ftp://example.test/hook", "https://")) {
            input(url).setValue(invalid);click(url,"確認");check(!notificationChannels(NotificationType.VISITORS).discord(),"invalid Webhook cannot enable");
        }
        click(url,"取消");check(pestWebhookUrl.equals("https://example.test/one"),"cancel retains shared URL");
        click(visitor,"Discord 訊息通知");url=mc.shown;input(url).setValue(" https://example.test/two ");click(url,"確認");
        check(notificationChannels(NotificationType.VISITORS).discord() && pestWebhookUrl.endsWith("/two"),"visitor enables only its Discord flag and changes common URL");
        openNotificationSettings(parent,NotificationType.PESTS);Screen pest=mc.shown;
        click(pest,"Discord 訊息通知");url=mc.shown;check(input(url).getValue().endsWith("/two"),"pest sees shared URL");
        input(url).setValue("https://example.test/three");click(url,"確認");
        check(notificationChannels(NotificationType.PESTS).discord() && notificationChannels(NotificationType.VISITORS).discord(),"second feature cannot alter first feature flag");
        discord.clear();sendNotification(NotificationType.PESTS,"pest","pest");sendNotification(NotificationType.VISITORS,"visitor","visitor");
        check(discord.size()==2 && discord.stream().allMatch(s->s.url().endsWith("/three")),"editing one URL updates all Discord destinations");
        click(visitor,"Discord 訊息通知");check(!notificationChannels(NotificationType.VISITORS).discord() && notificationChannels(NotificationType.PESTS).discord(),"disable only visitor Discord");
        click(visitor,"Discord 訊息通知");url=mc.shown;check(input(url).getValue().endsWith("/three"),"existing modal reads latest shared URL, not stale snapshot");url.onClose();
        openNotificationSettings(parent,NotificationType.JERRY);Screen jerry=mc.shown;
        check(jerry.widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).noneMatch(b->b.label.getString().startsWith("Jerry 偵測：")),"Jerry has no master toggle");
        click(jerry,"遊戲聊天室通知");check(notificationChannels(NotificationType.JERRY).chat() && !notificationChannels(NotificationType.VISITORS).chat(),"Jerry chat independent");
        click(jerry,"出現 Jerry 是否停下動作");check(jerryStopActionsEnabled,"Jerry stop configurable without master");
        click(jerry,"遊戲螢幕通知");Screen duration=mc.shown;
        check(duration instanceof ScreenDurationScreen && !notificationChannels(NotificationType.JERRY).screen(),"enabling screen prompts before saving");
        click(duration,"確認");
        SCREEN_NOTIFICATIONS.clear();SCREEN_NOTIFICATIONS.add(NotificationType.PESTS,"pest",3,System.nanoTime());SCREEN_NOTIFICATIONS.add(NotificationType.JERRY,"jerry",3,System.nanoTime());
        click(jerry,"遊戲螢幕通知");check(SCREEN_NOTIFICATIONS.current(System.nanoTime()).equals("pest"),"Jerry screen switch does not clear pest overlay");
        click(jerry,"完成");check(mc.shown==parent,"return to main settings");
        // Duration dialogs are independent for all three event types.
        for(NotificationType type:NotificationType.values()) {
            NOTIFICATION_CHANNELS.put(type,NotificationChannels.NONE);
            openNotificationSettings(parent,type);Screen feature=mc.shown;
            int before=saves;
            click(feature,"遊戲螢幕通知");Screen dialog=mc.shown;
            check(dialog instanceof ScreenDurationScreen && input(dialog).getValue().equals("3"),"legacy default prefilled");
            for(String invalid:List.of("", "0", "-1", "1.5", "a", " 3", "3 ", "03", "2147483648", "999999999999999999999")) {
                input(dialog).setValue(invalid);check(!button(dialog,"確認").active,"invalid duration rejected");
                click(dialog,"確認");check(saves==before && !notificationChannels(type).screen(),"invalid callback does not enable");
            }
            click(dialog,"取消");check(mc.shown==feature && saves==before,"Cancel leaves duration unchanged");
            click(feature,"遊戲螢幕通知");dialog=mc.shown;dialog.onClose();
            check(saves==before && !notificationChannels(type).screen(),"Esc leaves screen off");
            click(feature,"遊戲螢幕通知");dialog=mc.shown;
            int seconds=5+type.ordinal();input(dialog).setValue(Integer.toString(seconds));
            dialog.widgets.clear();dialog.init();check(input(dialog).getValue().equals(Integer.toString(seconds)),"draft survives resize");
            click(dialog,"確認");check(notificationChannels(type).screen() && notificationChannels(type).screenSeconds()==seconds,"feature duration saved");
            check(button(feature,"遊戲螢幕通知").label.getString().contains(seconds+" 秒"),"button shows duration");
            click(feature,"遊戲聊天室通知");check(notificationChannels(type).screenSeconds()==seconds,"other switch preserves duration");
            click(feature,"遊戲螢幕通知");click(feature,"遊戲螢幕通知");dialog=mc.shown;
            check(input(dialog).getValue().equals(Integer.toString(seconds)),"re-enabling prefills previous duration");dialog.onClose();
        }
        check(notificationChannels(NotificationType.PESTS).screenSeconds()==5 && notificationChannels(NotificationType.VISITORS).screenSeconds()==6 && notificationChannels(NotificationType.JERRY).screenSeconds()==7,"durations stay independent");
        Properties durations=new Properties();saveNotificationConfig(durations);loadNotificationConfig(durations);
        check(notificationChannels(NotificationType.JERRY).screenSeconds()==7 && notificationChannels(NotificationType.PESTS).screenSeconds()==5,"durations persist across reload");
        durations.setProperty("jerryNotificationScreenSeconds","invalid");loadNotificationConfig(durations);
        check(notificationChannels(NotificationType.JERRY).screenSeconds()==3 && notificationChannels(NotificationType.PESTS).screenSeconds()==5,"invalid saved duration safely defaults only that feature");
        NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,false,true,9));SCREEN_NOTIFICATIONS.clear();
        sendNotification(NotificationType.JERRY,"duration","custom");long now=System.nanoTime();
        check(SCREEN_NOTIFICATIONS.current(now+4_000_000_000L).equals("custom"),"dispatcher passes selected duration, not fixed three seconds");
        check(SCREEN_NOTIFICATIONS.current(now+10_000_000_000L)==null,"dispatcher duration eventually expires");

        System.out.println("NotificationSettingsTest: PASS (per-feature channels, shared live Webhook, thresholds, safe migration, Jerry without master, overlay isolation, per-feature duration dialogs)");
    }
    // METHODS
    // SCREENS
}
'''.replace('    // METHODS',methods).replace('    // SCREENS','\n'.join(classes))
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'NotificationSettingsTest.java';path.write_text(harness)
    java=Path(sys.argv[1])/'bin'
    deps=[base/(n+'.java') for n in ['ScreenNotificationQueue','NotificationType','NotificationChannels','GardenTabNotifications']]
    subprocess.run([str(java/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,*map(str,deps),str(path)],check=True)
    subprocess.run([str(java/'java'),'-cp',tmp,'dev.kuan.skyblockassistant.NotificationSettingsTest'],check=True)
