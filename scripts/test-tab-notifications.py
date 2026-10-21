#!/usr/bin/env python3
"""Exercise production polling and Tab ordering with fake Minecraft/network boundaries."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
base = root / "src/main/java/dev/kuan/skyblockassistant"
client = (base / "KuanSkyBlockAssistantClient.java").read_text()
reader = (base / "GardenTabReader.java").read_text()
reader = reader[reader.index("final class GardenTabReader"):].replace("final class GardenTabReader", "static final class GardenTabReader", 1)
screen = (base / "PestThresholdScreen.java").read_text()
screen = screen[screen.index("final class PestThresholdScreen"):].replace("final class PestThresholdScreen", "static final class PestThresholdScreen", 1)
visitor_screen = (base / "VisitorThresholdScreen.java").read_text()
visitor_screen = visitor_screen[visitor_screen.index("final class VisitorThresholdScreen"):].replace("final class VisitorThresholdScreen", "static final class VisitorThresholdScreen", 1)
start = re.search(r"    private static void tickTabNotifications\(", client).start()
opening = client.index("{", start)
depth, end = 1, opening + 1
while depth:
    depth += (client[end] == "{") - (client[end] == "}")
    end += 1
poll = client[start:end]
for name in ["notificationChannels", "hasNotificationChannel"]:
    start = re.search(r"    private static [^\n]+ " + name + r"\(", client).start()
    end = client.index("\n    }", start) + len("\n    }")
    poll += "\n" + client[start:end]
assert "tickTabNotifications(mc);" in client[client.index("private static void tick(Minecraft"):client.index("private static void handleShiftHotkeys")]
assert "PEST_MESSAGE_PATTERN" not in client, "old spawn-chat notifications must be removed"
assert 'p.setProperty("pestNotificationThreshold"' in client, "save threshold"
assert 'p.getProperty("pestNotificationThreshold"' in client, "load threshold"
assert 'p.setProperty("visitorNotificationThreshold"' in client, "save visitor threshold"
assert 'p.getProperty("visitorNotificationThreshold"' in client, "load visitor threshold"

harness = r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.function.*;
public class TabNotificationIntegrationTest {
    static final GardenTabNotifications TAB_NOTIFICATIONS = new GardenTabNotifications();
    static boolean pestNotificationEnabled, visitorNotificationEnabled;
    static final Map<NotificationType,NotificationChannels> NOTIFICATION_CHANNELS = new EnumMap<>(NotificationType.class);
    static {
        NOTIFICATION_CHANNELS.put(NotificationType.PESTS,new NotificationChannels(true,false,false));
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,new NotificationChannels(true,false,false));
    }
    static int pestNotificationThreshold = 4, visitorNotificationThreshold = 5, tabNotificationTicks;
    static int saves;
    static void saveConfig() { saves++; }
    static String pestWebhookUrl = "test-only-not-a-real-webhook";
    static List<String> sent = new ArrayList<>();
    static void sendNotification(NotificationType type, String text, String title) { sent.add(text); }
    record Component(String getString) { static Component literal(String text) { return new Component(text); } }
    record Profile(String name) {}
    record PlayerTeam(String getName) {
        static Component formatNameForTeam(PlayerTeam team, Component name) { return name; }
    }
    enum GameType { SURVIVAL, SPECTATOR }
    record PlayerInfo(int getTabListOrder, GameType getGameMode, PlayerTeam getTeam,
                      Profile getProfile, Component getTabListDisplayName) {}
    static class Connection {
        List<PlayerInfo> entries = new ArrayList<>();
        Collection<PlayerInfo> getListedOnlinePlayers() { return entries; }
    }
    static class Player { Connection connection = new Connection(); }
    static class Minecraft {
        static final Minecraft INSTANCE = new Minecraft();
        Player player = new Player(); Object level = new Object(); Screen shown;
        static Minecraft getInstance() { return INSTANCE; }
        void setScreenAndShow(Screen screen) { shown = screen; }
    }
    static class Screen {
        int width = 600, height = 400; Object font = new Object();
        List<Object> widgets = new ArrayList<>();
        Screen(Component title) {}
        protected void init() {}
        <T> T addRenderableWidget(T widget) { widgets.add(widget); return widget; }
        void setInitialFocus(Object widget) {}
        public void onClose() {}
        public boolean isPauseScreen() { return true; }
    }
    static class EditBox {
        String value = ""; int maxLength; Consumer<String> responder;
        EditBox(Object... args) {}
        void setMaxLength(int length) { maxLength = length; }
        void setTextColor(int color) {}
        void setResponder(Consumer<String> callback) { responder = callback; }
        String getValue() { return value; }
        void setValue(String text) {
            value = text.substring(0, Math.min(text.length(), maxLength));
            responder.accept(value);
        }
    }
    static class StringWidget { StringWidget(Object... args) {} }
    static class Button {
        boolean active = true; Consumer<Button> callback;
        static Builder builder(Component label, Consumer<Button> callback) { return new Builder(callback); }
        static class Builder {
            Consumer<Button> callback;
            Builder(Consumer<Button> callback) { this.callback = callback; }
            Builder bounds(int x, int y, int w, int h) { return this; }
            Button build() { Button button = new Button(); button.callback = callback; return button; }
        }
    }
    static PlayerInfo row(String name, String text, int order, String team, GameType mode) {
        return new PlayerInfo(order, mode, team == null ? null : new PlayerTeam(team),
                new Profile(name), text == null ? null : new Component(text));
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static void scan(Minecraft mc) { for (int i = 0; i < 20; i++) tickTabNotifications(mc); }
    static void garden(Minecraft mc, int pests, int visitors) {
        mc.player.connection.entries = new ArrayList<>(List.of(
                row("c", "Visitors: (" + visitors + ")", 1, null, GameType.SURVIVAL),
                row("b", " Alive: " + pests, 2, null, GameType.SURVIVAL),
                row("a", "Pests:", 3, null, GameType.SURVIVAL)));
    }
    public static void main(String[] args) {
        Minecraft mc = new Minecraft();
        garden(mc, 4, 5);
        check(GardenTabReader.read(mc).equals(List.of("Pests:", " Alive: 4", "Visitors: (5)")), "sort by server Tab order, not collection iteration");
        pestNotificationEnabled = visitorNotificationEnabled = true;
        scan(mc);
        check(sent.size() == 2, "both alerts without Farming, GUI or Tab-key dependencies");
        check(sent.get(0).contains("4") && sent.get(1).contains("5"), "include actual count in alerts");
        int expected = 2;
        scan(mc); check(sent.size() == expected, "sustained matching values stay silent");
        for (int count = 5; count <= 8; count++) {
            garden(mc, count, 5); scan(mc);
            check(sent.size() == ++expected, "new pest count notifies while Visitor stays five: " + count);
            scan(mc); check(sent.size() == expected, "sustained new count stays silent");
        }
        garden(mc, 4, 5); scan(mc);
        check(sent.size() == expected, "returning to a previously notified count stays silent");
        mc.player.connection.entries.clear(); scan(mc);
        garden(mc, 7, 5); scan(mc);
        check(sent.size() == expected, "missing Tab must not clear per-count history");
        garden(mc, 3, 4); scan(mc);
        garden(mc, 8, 5); scan(mc); expected += 2;
        check(sent.size() == expected, "reset sends only observed pest count and full visitors, not skipped counts");
        visitorNotificationEnabled = false; TAB_NOTIFICATIONS.resetVisitors();
        garden(mc, 1, 5); scan(mc); garden(mc, 4, 5); scan(mc);
        check(sent.size() == ++expected, "visitor toggle does not affect pests");
        pestNotificationEnabled = false; TAB_NOTIFICATIONS.resetPests();
        visitorNotificationEnabled = true; scan(mc);
        check(sent.size() == ++expected, "visitor still works when pests are disabled");
        garden(mc, 4, 4); scan(mc);
        pestWebhookUrl = ""; garden(mc, 4, 5); scan(mc);
        check(sent.size() == expected, "blank webhook skips sending and preserves pending alert");
        pestWebhookUrl = "test-only"; scan(mc);
        check(sent.size() == ++expected, "setting webhook after a match allows notification");
        mc.player = null; scan(mc);
        check(sent.size() == expected && GardenTabReader.read(mc).isEmpty(), "disconnect safety");
        mc.player = new Player(); garden(mc, 4, 5); mc.level = null; scan(mc);
        check(sent.size() == expected, "world loading safety"); mc.level = new Object();

        mc.player.connection.entries = new ArrayList<>(List.of(
                row("z", "spectator", 0, "a", GameType.SPECTATOR),
                row("z", "z-team", 0, "z", GameType.SURVIVAL),
                row("B", "second", 0, "a", GameType.SURVIVAL),
                row("a", "first", 0, "a", GameType.SURVIVAL),
                row("fallback", null, 10, null, GameType.SURVIVAL)));
        check(GardenTabReader.read(mc).equals(List.of("fallback", "first", "second", "z-team", "spectator")), "vanilla team, spectator, profile ordering and fallback");
        mc.player.connection.entries.clear();
        for (int i = 0; i < 90; i++) mc.player.connection.entries.add(row("r" + i, "line" + i, 90 - i, null, GameType.SURVIVAL));
        check(GardenTabReader.read(mc).size() == 80, "same 80-entry limit as vanilla Tab");
        Screen parent = new Screen(Component.literal("settings"));
        List<Integer> chosen = new ArrayList<>();
        PestThresholdScreen screen = new PestThresholdScreen(parent, chosen::add);
        screen.init();
        EditBox input = (EditBox) screen.widgets.stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
        List<Button> buttons = screen.widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
        Button confirm = buttons.get(0);
        check(!confirm.active, "blank initial input cannot confirm");
        for (String invalid : List.of("", "0", "9", "12", "01", "-1", " 1", "a")) {
            input.setValue(invalid);
            check(!confirm.active, "invalid input disables confirmation: " + invalid);
            confirm.callback.accept(confirm);
            check(chosen.isEmpty(), "callback also rejects invalid threshold: " + invalid);
        }
        for (int i = 1; i <= 8; i++) {
            input.setValue(Integer.toString(i));
            check(confirm.active, "valid threshold enables confirmation");
            confirm.callback.accept(confirm);
            check(chosen.remove(0) == i, "selected threshold is passed unchanged");
        }
        input.setValue("4"); buttons.get(1).callback.accept(buttons.get(1));
        check(chosen.isEmpty() && Minecraft.getInstance().shown == parent, "cancel does not confirm");
        screen.onClose();
        check(chosen.isEmpty() && Minecraft.getInstance().shown == parent && !screen.isPauseScreen(), "Escape returns unchanged");
        // Production polling uses the selected visitor threshold and per-count history.
        visitorNotificationThreshold = 3; visitorNotificationEnabled = true;
        pestNotificationEnabled = false; TAB_NOTIFICATIONS.resetVisitors(); sent.clear();
        garden(mc, 0, 3); scan(mc);
        check(sent.size() == 1 && sent.get(0).contains("3"), "visitor three notifies at threshold three");
        garden(mc, 0, 4); scan(mc); garden(mc, 0, 5); scan(mc);
        check(sent.size() == 3, "3, 4, 5 notify individually");
        garden(mc, 0, 3); scan(mc); garden(mc, 0, 5); scan(mc);
        check(sent.size() == 3, "revisiting qualifying visitor counts stays silent");
        mc.player.connection.entries.clear(); scan(mc); garden(mc, 0, 4); scan(mc);
        check(sent.size() == 3, "missing Tab preserves visitor count history");
        garden(mc, 0, 2); scan(mc); garden(mc, 0, 5); scan(mc);
        check(sent.size() == 4, "below threshold resets; jump only notifies observed five");
        garden(mc, 0, 3); scan(mc);
        check(sent.size() == 5, "three was not invented during jump to five");
        // Validate the actual Visitor input screen; feature modal callbacks are covered separately.
        List<Integer> visitorChosen = new ArrayList<>();
        VisitorThresholdScreen visitorScreen = new VisitorThresholdScreen(parent, visitorChosen::add);
        visitorScreen.init();
        EditBox visitorInput = (EditBox) visitorScreen.widgets.stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
        List<Button> visitorButtons = visitorScreen.widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
        Button visitorConfirm = visitorButtons.get(0);
        check(!visitorConfirm.active, "blank visitor input cannot confirm");
        for(String invalid:List.of("", "0", "6", "9", "12", "01", "-1", " 1", "1 ", "a", "１")) {
            visitorInput.setValue(invalid);check(!visitorConfirm.active,"invalid visitor input");
            visitorConfirm.callback.accept(visitorConfirm);check(visitorChosen.isEmpty(),"invalid callback rejected");
        }
        for(int i=1;i<=5;i++) {
            visitorInput.setValue(Integer.toString(i));check(visitorConfirm.active,"valid visitor threshold");
            visitorConfirm.callback.accept(visitorConfirm);check(visitorChosen.remove(0)==i,"unchanged selected threshold");
        }
        visitorButtons.get(1).callback.accept(visitorButtons.get(1));visitorScreen.onClose();
        check(visitorChosen.isEmpty() && Minecraft.getInstance().shown==parent,"Cancel/Esc unchanged");
        TAB_NOTIFICATIONS.resetVisitors(); sent.clear();
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,new NotificationChannels(false,true,false)); pestWebhookUrl = "";
        garden(mc, 0, 3); scan(mc);
        check(sent.size() == 1, "local channels work without Webhook");
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,NotificationChannels.NONE); garden(mc, 0, 4); scan(mc);
        check(sent.size() == 1, "all channels off sends nothing and does not consume count");
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,new NotificationChannels(false,false,true)); scan(mc);
        check(sent.size() == 2, "screen-only routing processes pending observed count");
        TAB_NOTIFICATIONS.resetPests(); TAB_NOTIFICATIONS.resetVisitors(); sent.clear();
        pestNotificationEnabled = visitorNotificationEnabled = true;
        NOTIFICATION_CHANNELS.put(NotificationType.PESTS,new NotificationChannels(false,true,false));
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,NotificationChannels.NONE);
        garden(mc,4,3);scan(mc);
        check(sent.size()==1 && sent.get(0).contains("害蟲"),"visitor channels off cannot block pest notifications");
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,new NotificationChannels(false,true,false));scan(mc);
        check(sent.size()==2 && sent.get(1).contains("Visitor"),"pest-only polling must not consume unavailable visitor count");
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,NotificationChannels.NONE);garden(mc,5,4);scan(mc);
        check(sent.size()==3 && sent.get(2).contains("害蟲"),"independent pest next count");
        NOTIFICATION_CHANNELS.put(NotificationType.VISITORS,new NotificationChannels(false,false,true));scan(mc);
        check(sent.size()==4 && sent.get(3).contains("Visitor"),"new visitor screen channel sees its pending count only");
        System.out.println("TabNotificationIntegrationTest passed (polling, ordering, toggles, missing data, pest/visitor threshold UI)");
    }
'''
with tempfile.TemporaryDirectory(prefix="kuan-tab-test-") as tmp:
    test = Path(tmp) / "TabNotificationIntegrationTest.java"
    test.write_text(harness + reader + "\n" + screen + "\n" + visitor_screen + "\n" + poll + "\n}\n")
    java = Path(sys.argv[1]) / "bin"
    subprocess.run([str(java / "javac"), "--release", "21", "-encoding", "UTF-8", "-d", tmp,
                    str(base / "GardenTabNotifications.java"), str(base / "NotificationType.java"), str(base / "NotificationChannels.java"), str(test)], check=True)
    subprocess.run([str(java / "java"), "-cp", tmp, "dev.kuan.skyblockassistant.TabNotificationIntegrationTest"], check=True)
