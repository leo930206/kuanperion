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
methods='\n'.join(method(n) for n in ['notificationChannels','hasNotificationChannel','sendNotification','loadNotificationConfig','saveNotificationConfig','toggleTabNotification','openNotificationSettings','renderNotificationOverlay'])
classes=[]
for name in ['NotificationSettingsScreen','WebhookSettingsScreen','PestThresholdScreen','VisitorThresholdScreen','ScreenDurationScreen']:
    s=(base/(name+'.java')).read_text()
    classes.append(s[s.index('final class '+name):].replace('final class '+name,'static final class '+name,1))
assert 'Component.literal("通知設定")' not in source, 'No global notification settings button'
assert 'toggleMsg("Jerry 偵測"' not in source, 'Jerry has no master toggle'
assert 'Component.literal("Jerry 偵測")' in source
for feature in ['PESTS','VISITORS']:
    assert f'b -> toggleTabNotification(this, NotificationType.{feature})' in source, 'Outer buttons own master switches'
assert '害蟲' not in ''.join(p.read_text() for p in base.glob('*.java')), 'Use Pest throughout current production text'
assert '遊戲聊天室通知' not in (base/'NotificationSettingsScreen.java').read_text()
assert '遊戲螢幕通知' not in (base/'NotificationSettingsScreen.java').read_text()
harness=r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.function.*;
import java.net.URI;
import java.math.BigDecimal;
public class NotificationSettingsTest {
    static final Map<NotificationType,NotificationChannels> NOTIFICATION_CHANNELS=new EnumMap<>(NotificationType.class);
    static final ScreenNotificationQueue SCREEN_NOTIFICATIONS=new ScreenNotificationQueue();
    static final GardenTabNotifications TAB_NOTIFICATIONS=new GardenTabNotifications();
    static boolean pestNotificationEnabled,visitorNotificationEnabled,jerryStopActionsEnabled;
    static Set<JerryColor> jerryColors=Set.of(JerryColor.values());
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
        int rgb; String text; Component(String text) { this.text=text; }
        static Component literal(String text) { return new Component(text); }
        String getString() { return text; }
        Component withColor(int rgb) { this.rgb=rgb;return this; }
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
    static class SettingsScreen extends Screen {
        SettingsScreen() { super(Component.literal("main")); }
        void rebuild() { Minecraft.getInstance().setScreenAndShow(this); }
    }
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
        boolean active=true;Component label;Consumer<Button> callback;int x,y,width,height;
        static Builder builder(Component label,Consumer<Button> cb) { return new Builder(label,cb); }
        void setMessage(Component value) { label=value; }
        static class Builder {
            Component label;Consumer<Button> cb;int x,y,width,height;
            Builder(Component label,Consumer<Button> cb) { this.label=label;this.cb=cb; }
            Builder bounds(int x,int y,int width,int height) { this.x=x;this.y=y;this.width=width;this.height=height;return this; }
            Button build() { Button b=new Button();b.label=label;b.callback=cb;b.x=x;b.y=y;b.width=width;b.height=height;return b; }
        }
    }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static Button button(Screen screen,String prefix) { return screen.widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.label.getString().startsWith(prefix)).findFirst().orElseThrow(); }
    static void click(Screen screen,String prefix) { Button b=button(screen,prefix);b.callback.accept(b); }
    static EditBox input(Screen screen) { return (EditBox)screen.widgets.stream().filter(EditBox.class::isInstance).findFirst().orElseThrow(); }
    static void checkRows(Screen screen,NotificationType type) {
        List<Button> rows=screen.widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
        List<String> expected=type==NotificationType.JERRY
                ? List.of("Green：","Blue：","Purple：","Golden：","出現 Jerry 是否停下動作","Discord 訊息通知","遊戲內聊天室通知","遊戲內螢幕通知","完成")
                : List.of(type==NotificationType.PESTS ? "出現多少 Pest 要通知：" : "出現多少 Visitor 要通知：","Discord 訊息通知","遊戲內聊天室通知","遊戲內螢幕通知","完成");
        check(rows.size()==(type==NotificationType.JERRY ? 9 : 5),"correct feature button count: "+type);
        for(int i=0;i<expected.size();i++) check(rows.get(i).label.getString().startsWith(expected.get(i)),"row order/wording: "+type+i);
        if(type!=NotificationType.JERRY) check(rows.get(0).label.getString().equals(expected.get(0)+(type==NotificationType.PESTS ? pestNotificationThreshold : visitorNotificationThreshold)),"threshold label retains current saved value");
        if(type==NotificationType.JERRY) {
            Button discord=rows.get(5);
            int[] colors={0x55FF55,0x5555FF,0xAA00AA,0xFFAA00};
            for(int i=0;i<4;i++) {
                Button part=rows.get(i);
                check(part.width*4==discord.width && part.height==discord.height && part.x==discord.x+i*part.width,"four equal widths fill one full row");
                check(part.y==rows.get(0).y && part.y==rows.get(4).y-28 && rows.get(4).y==discord.y-28,"color row above stop action row, then Discord");
                check(part.label.rgb==colors[i],"name uses supplied Minecraft color");
                check(part.label.getString().equals(JerryColor.values()[i].label+"："+(jerryColors.contains(JerryColor.values()[i]) ? "開啟" : "關閉")),"color toggle text reflects saved value");
            }
        }
    }
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
        check(jerryColors.equals(Set.of(JerryColor.values())),"fresh and missing color settings default all on");
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
        visitorNotificationThreshold=3;pestNotificationThreshold=6;
        int savesBeforeEnable=saves;
        toggleTabNotification(parent,NotificationType.VISITORS);Screen visitor=mc.shown;
        check(visitor instanceof NotificationSettingsScreen,"enable Visitor goes straight to settings, not threshold entry");
        check(visitorNotificationEnabled && !pestNotificationEnabled && visitorNotificationThreshold==3 && pestNotificationThreshold==6,"enable retains previous independent thresholds");
        check(saves==savesBeforeEnable+1,"enable saved immediately");
        checkRows(visitor,NotificationType.VISITORS);
        click(visitor,"完成");check(mc.shown==parent && visitorNotificationEnabled,"Done leaves Visitor enabled");
        openNotificationSettings(parent,NotificationType.VISITORS);visitor=mc.shown;
        click(visitor,"出現多少 Visitor 要通知：");Screen threshold=mc.shown;
        check(threshold instanceof VisitorThresholdScreen,"threshold dialog only opens on explicit edit");
        input(threshold).setValue("6");check(!button(threshold,"確認").active,"invalid visitor threshold");
        click(threshold,"確認");check(visitorNotificationEnabled && visitorNotificationThreshold==3,"invalid edit preserves enabled state and prior count");
        click(threshold,"取消");check(mc.shown==visitor && visitorNotificationThreshold==3 && visitorNotificationEnabled,"cancel edit preserves prior threshold");
        click(visitor,"出現多少 Visitor 要通知：");mc.shown.onClose();
        check(mc.shown==visitor && visitorNotificationEnabled && visitorNotificationThreshold==3,"Esc edit does not disable feature");
        click(visitor,"出現多少 Visitor 要通知：");threshold=mc.shown;input(threshold).setValue("4");click(threshold,"確認");
        check(visitorNotificationEnabled && visitorNotificationThreshold==4 && mc.shown==visitor,"explicit threshold edit remains available");
        click(visitor,"Discord 訊息通知");Screen url=mc.shown;
        for(String invalid:List.of("", "abc", "ftp://example.test/hook", "https://")) {
            input(url).setValue(invalid);click(url,"確認");check(!notificationChannels(NotificationType.VISITORS).discord(),"invalid Webhook cannot enable");
        }
        click(url,"取消");check(pestWebhookUrl.equals("https://example.test/one"),"cancel retains shared URL");
        click(visitor,"Discord 訊息通知");url=mc.shown;input(url).setValue(" https://example.test/two ");click(url,"確認");
        check(notificationChannels(NotificationType.VISITORS).discord() && pestWebhookUrl.endsWith("/two"),"visitor enables only its Discord flag and changes common URL");
        savesBeforeEnable=saves;
        toggleTabNotification(parent,NotificationType.PESTS);Screen pest=mc.shown;
        check(pest instanceof NotificationSettingsScreen,"enable Pest goes straight to settings, not threshold entry");
        check(pestNotificationEnabled && pestNotificationThreshold==6 && visitorNotificationThreshold==4,"Pest reuses previous threshold without changing Visitor");
        check(saves==savesBeforeEnable+1,"Pest enabled state saved immediately");
        checkRows(pest,NotificationType.PESTS);
        click(pest,"出現多少 Pest 要通知：");threshold=mc.shown;
        check(threshold instanceof PestThresholdScreen,"Pest threshold is manually editable");
        input(threshold).setValue("9");check(!button(threshold,"確認").active,"invalid Pest threshold");
        click(threshold,"取消");check(pestNotificationEnabled && pestNotificationThreshold==6 && mc.shown==pest,"cancel Pest edit leaves previous threshold");
        click(pest,"出現多少 Pest 要通知：");mc.shown.onClose();
        check(pestNotificationEnabled && pestNotificationThreshold==6 && mc.shown==pest,"Esc Pest edit leaves previous threshold");
        click(pest,"出現多少 Pest 要通知：");threshold=mc.shown;input(threshold).setValue("8");click(threshold,"確認");
        check(pestNotificationEnabled && pestNotificationThreshold==8 && mc.shown==pest,"manual Pest edit confirmed");
        click(pest,"Discord 訊息通知");url=mc.shown;check(input(url).getValue().endsWith("/two"),"pest sees shared URL");
        input(url).setValue("https://example.test/three");click(url,"確認");
        check(notificationChannels(NotificationType.PESTS).discord() && notificationChannels(NotificationType.VISITORS).discord(),"second feature cannot alter first feature flag");
        discord.clear();sendNotification(NotificationType.PESTS,"pest","pest");sendNotification(NotificationType.VISITORS,"visitor","visitor");
        check(discord.size()==2 && discord.stream().allMatch(s->s.url().endsWith("/three")),"editing one URL updates all Discord destinations");
        click(visitor,"Discord 訊息通知");check(!notificationChannels(NotificationType.VISITORS).discord() && notificationChannels(NotificationType.PESTS).discord(),"disable only visitor Discord");
        click(visitor,"Discord 訊息通知");url=mc.shown;check(input(url).getValue().endsWith("/three"),"existing modal reads latest shared URL, not stale snapshot");url.onClose();
        openNotificationSettings(parent,NotificationType.JERRY);Screen jerry=mc.shown;
        check(jerry.widgets.stream().filter(Button.class::isInstance).count()==9,"Jerry needs four quarter-width color toggles above Discord");
        for(String color:List.of("Green","Blue","Purple","Golden")) check(button(jerry,color+"：").label.getString().equals(color+"：開啟"),"default color switch: "+color);
        checkRows(jerry,NotificationType.JERRY);
        check(jerry.widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).noneMatch(b->b.label.getString().startsWith("Jerry 偵測：")),"Jerry has no master toggle");
        NOTIFICATION_CHANNELS.putIfAbsent(NotificationType.JERRY,NotificationChannels.NONE);
        Map<NotificationType,NotificationChannels> beforeColors=new EnumMap<>(NOTIFICATION_CHANNELS);
        String sharedBeforeColors=pestWebhookUrl;boolean stopBeforeColors=jerryStopActionsEnabled;
        click(jerry,"Blue：");click(jerry,"Purple：");
        check(jerryColors.equals(Set.of(JerryColor.GREEN,JerryColor.GOLDEN)),"Green/Golden-only preference saved independently");
        check(NOTIFICATION_CHANNELS.equals(beforeColors) && pestWebhookUrl.equals(sharedBeforeColors) && jerryStopActionsEnabled==stopBeforeColors,"color toggles preserve channels, seconds, shared URL, and stop switch");
        checkRows(jerry,NotificationType.JERRY);
        jerry.width=634;jerry.height=394;jerry.widgets.clear();jerry.init();checkRows(jerry,NotificationType.JERRY);
        click(visitor,"出現多少 Visitor 要通知：");input(mc.shown).setValue("4");click(mc.shown,"確認");
        check(jerryColors.equals(Set.of(JerryColor.GREEN,JerryColor.GOLDEN)),"stale Visitor preferences cannot overwrite Jerry color selection");
        Properties colorConfig=new Properties();saveNotificationConfig(colorConfig);jerryColors=Set.of();loadNotificationConfig(colorConfig);
        check(jerryColors.equals(Set.of(JerryColor.GREEN,JerryColor.GOLDEN)),"color preferences survive restart");
        openNotificationSettings(parent,NotificationType.JERRY);jerry=mc.shown;checkRows(jerry,NotificationType.JERRY);
        click(jerry,"Green：");click(jerry,"Golden：");check(jerryColors.isEmpty(),"all four colors can be off");
        click(jerry,"遊戲內聊天室通知");check(jerryColors.isEmpty(),"other settings do not re-enable colors");
        click(jerry,"遊戲內聊天室通知");
        saveNotificationConfig(colorConfig);loadNotificationConfig(colorConfig);check(jerryColors.isEmpty(),"all-off preference persists");
        openNotificationSettings(parent,NotificationType.JERRY);jerry=mc.shown;checkRows(jerry,NotificationType.JERRY);
        for(JerryColor color:JerryColor.values()) click(jerry,color.label+"：");
        check(jerryColors.equals(Set.of(JerryColor.values())),"all colors can be re-enabled");
        click(jerry,"遊戲內聊天室通知");check(notificationChannels(NotificationType.JERRY).chat() && !notificationChannels(NotificationType.VISITORS).chat(),"Jerry chat independent");
        click(jerry,"出現 Jerry 是否停下動作");check(jerryStopActionsEnabled,"Jerry stop configurable without master");
        click(jerry,"遊戲內螢幕通知");Screen duration=mc.shown;
        check(duration instanceof ScreenDurationScreen && !notificationChannels(NotificationType.JERRY).screen(),"enabling screen prompts before saving");
        click(duration,"確認");
        SCREEN_NOTIFICATIONS.clear();SCREEN_NOTIFICATIONS.add(NotificationType.PESTS,"pest",3,System.nanoTime());SCREEN_NOTIFICATIONS.add(NotificationType.JERRY,"jerry",3,System.nanoTime());
        click(jerry,"遊戲內螢幕通知");check(SCREEN_NOTIFICATIONS.current(System.nanoTime()).equals("pest"),"Jerry screen switch does not clear pest overlay");
        click(jerry,"完成");check(mc.shown==parent,"return to main settings");
        // Duration dialogs are independent for all three event types.
        for(NotificationType type:NotificationType.values()) {
            NOTIFICATION_CHANNELS.put(type,NotificationChannels.NONE);
            openNotificationSettings(parent,type);Screen feature=mc.shown;
            int before=saves;
            click(feature,"遊戲內螢幕通知");Screen dialog=mc.shown;
            check(dialog instanceof ScreenDurationScreen && input(dialog).getValue().equals("3"),"legacy default prefilled");
            for(String invalid:List.of("", "0", "-1", "1..5", "1.2.3", "0.0", "a", " 3", "3 ", "03", "2147483648", "999999999999999999999")) {
                input(dialog).setValue(invalid);check(!button(dialog,"確認").active,"invalid duration rejected");
                click(dialog,"確認");check(saves==before && !notificationChannels(type).screen(),"invalid callback does not enable");
            }
            click(dialog,"取消");check(mc.shown==feature && saves==before,"Cancel leaves duration unchanged");
            click(feature,"遊戲內螢幕通知");dialog=mc.shown;dialog.onClose();
            check(saves==before && !notificationChannels(type).screen(),"Esc leaves screen off");
            click(feature,"遊戲內螢幕通知");dialog=mc.shown;
            BigDecimal seconds=new BigDecimal(List.of("0.5","1.25","2.75").get(type.ordinal()));input(dialog).setValue(seconds.toPlainString());
            dialog.widgets.clear();dialog.init();check(input(dialog).getValue().equals(seconds.toPlainString()),"draft survives resize");
            click(dialog,"確認");check(notificationChannels(type).screen() && notificationChannels(type).screenSeconds().compareTo(seconds)==0,"feature duration saved");
            check(button(feature,"遊戲內螢幕通知").label.getString().contains(seconds.toPlainString()+" 秒"),"button shows duration");
            click(feature,"遊戲內聊天室通知");check(notificationChannels(type).screenSeconds().compareTo(seconds)==0,"other switch preserves duration");
            click(feature,"遊戲內螢幕通知");click(feature,"遊戲內螢幕通知");dialog=mc.shown;
            check(input(dialog).getValue().equals(seconds.toPlainString()),"re-enabling prefills previous duration");dialog.onClose();
        }
        check(notificationChannels(NotificationType.PESTS).screenSeconds().compareTo(new BigDecimal("0.5"))==0 && notificationChannels(NotificationType.VISITORS).screenSeconds().compareTo(new BigDecimal("1.25"))==0 && notificationChannels(NotificationType.JERRY).screenSeconds().compareTo(new BigDecimal("2.75"))==0,"durations stay independent");
        Properties durations=new Properties();saveNotificationConfig(durations);loadNotificationConfig(durations);
        check(notificationChannels(NotificationType.JERRY).screenSeconds().compareTo(new BigDecimal("2.75"))==0 && notificationChannels(NotificationType.PESTS).screenSeconds().compareTo(new BigDecimal("0.5"))==0,"durations persist across reload");
        durations.setProperty("jerryNotificationScreenSeconds","invalid");loadNotificationConfig(durations);
        check(notificationChannels(NotificationType.JERRY).screenSeconds().compareTo(new BigDecimal("3"))==0 && notificationChannels(NotificationType.PESTS).screenSeconds().compareTo(new BigDecimal("0.5"))==0,"invalid saved duration safely defaults only that feature");
        NOTIFICATION_CHANNELS.put(NotificationType.JERRY,new NotificationChannels(false,false,true,9));SCREEN_NOTIFICATIONS.clear();
        sendNotification(NotificationType.JERRY,"duration","custom");long now=System.nanoTime();
        check(SCREEN_NOTIFICATIONS.current(now+4_000_000_000L).equals("custom"),"dispatcher passes selected duration, not fixed three seconds");
        check(SCREEN_NOTIFICATIONS.current(now+10_000_000_000L)==null,"dispatcher duration eventually expires");

        for(NotificationType type:List.of(NotificationType.PESTS,NotificationType.VISITORS)) {
            pestNotificationEnabled=visitorNotificationEnabled=true;
            TAB_NOTIFICATIONS.resetPests();TAB_NOTIFICATIONS.resetVisitors();
            check(TAB_NOTIFICATIONS.pestReached(true,8,1) && TAB_NOTIFICATIONS.visitorsReached(true,5,1),"seed both histories");
            Map<NotificationType,NotificationChannels> before=new EnumMap<>(NOTIFICATION_CHANNELS);
            int savedPestThreshold=pestNotificationThreshold,savedVisitorThreshold=visitorNotificationThreshold;
            SCREEN_NOTIFICATIONS.clear();
            SCREEN_NOTIFICATIONS.add(type,"remove me",9,System.nanoTime());
            SCREEN_NOTIFICATIONS.add(NotificationType.JERRY,"keep Jerry",9,System.nanoTime());
            toggleTabNotification(parent,type);
            check(mc.shown==parent,"outer disable stays in main screen");
            check((type==NotificationType.PESTS ? !pestNotificationEnabled && visitorNotificationEnabled : pestNotificationEnabled && !visitorNotificationEnabled),"disable only selected master");
            check(NOTIFICATION_CHANNELS.equals(before) && pestNotificationThreshold==savedPestThreshold && visitorNotificationThreshold==savedVisitorThreshold,"disable preserves thresholds, channels, and durations");
            check(SCREEN_NOTIFICATIONS.current(System.nanoTime()).equals("keep Jerry"),"disable removes only selected screen alerts");
            check(TAB_NOTIFICATIONS.pestReached(true,8,1)==(type==NotificationType.PESTS),"reset only selected Pest history");
            check(TAB_NOTIFICATIONS.visitorsReached(true,5,1)==(type==NotificationType.VISITORS),"reset only selected Visitor history");
            openNotificationSettings(parent,type);Screen settings=mc.shown;checkRows(settings,type);
            click(settings,"出現多少");input(mc.shown).setValue("2");click(mc.shown,"確認");
            click(settings,"遊戲內聊天室通知");
            check(type==NotificationType.PESTS ? !pestNotificationEnabled : !visitorNotificationEnabled,"preferences cannot silently re-enable outer master");
            Map<NotificationType,NotificationChannels> channelsBeforeEnable=new EnumMap<>(NOTIFICATION_CHANNELS);
            String webhookBeforeEnable=pestWebhookUrl;Set<JerryColor> colorsBeforeEnable=jerryColors;
            toggleTabNotification(parent,type);
            check(mc.shown instanceof NotificationSettingsScreen,"re-enable directly opens settings");
            check((type==NotificationType.PESTS ? pestNotificationThreshold : visitorNotificationThreshold)==2,"re-enable retains last threshold");
            check(NOTIFICATION_CHANNELS.equals(channelsBeforeEnable) && pestWebhookUrl.equals(webhookBeforeEnable) && jerryColors.equals(colorsBeforeEnable),"enable preserves channels, durations, shared URL, and Jerry colors");
            check(pestNotificationEnabled && visitorNotificationEnabled,"can re-enable from outer switch");
            checkRows(mc.shown,type);
        }

        System.out.println("NotificationSettingsTest: PASS (color row geometry/name colors/persistence, four-row Visitor/Pest dialogs, outer master switches, renamed labels, independent channels, shared Webhook, thresholds, safe migration, overlay isolation, decimal durations)");
    }
    // METHODS
    // SCREENS
}
'''.replace('    // METHODS',methods).replace('    // SCREENS','\n'.join(classes))
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'NotificationSettingsTest.java';path.write_text(harness)
    java=Path(sys.argv[1])/'bin'
    deps=[base/(n+'.java') for n in ['ScreenNotificationQueue','NotificationType','NotificationChannels','GardenTabNotifications','JerryColor']]
    subprocess.run([str(java/'javac'),'--release','21','-encoding','UTF-8','-d',tmp,*map(str,deps),str(path)],check=True)
    subprocess.run([str(java/'java'),'-cp',tmp,'dev.kuan.skyblockassistant.NotificationSettingsTest'],check=True)
