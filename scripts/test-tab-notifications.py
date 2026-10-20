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
start = re.search(r"    private static void tickTabNotifications\(", client).start()
opening = client.index("{", start)
depth, end = 1, opening + 1
while depth:
    depth += (client[end] == "{") - (client[end] == "}")
    end += 1
poll = client[start:end]
assert "tickTabNotifications(mc);" in client[client.index("private static void tick(Minecraft"):client.index("private static void handleShiftHotkeys")]
assert "PEST_MESSAGE_PATTERN" not in client, "old spawn-chat notifications must be removed"
assert 'p.setProperty("pestNotificationThreshold"' in client, "save threshold"
assert 'p.getProperty("pestNotificationThreshold"' in client, "load threshold"

harness = r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.function.*;
public class TabNotificationIntegrationTest {
    static final GardenTabNotifications TAB_NOTIFICATIONS = new GardenTabNotifications();
    static boolean pestNotificationEnabled, visitorNotificationEnabled;
    static int pestNotificationThreshold = 4, tabNotificationTicks;
    static String pestWebhookUrl = "test-only-not-a-real-webhook";
    static List<String> sent = new ArrayList<>();
    static void sendDiscordNotification(String text) { sent.add(text); }
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
        scan(mc); check(sent.size() == 2, "sustained matching values stay silent");
        mc.player.connection.entries.clear(); scan(mc);
        garden(mc, 7, 5); scan(mc);
        check(sent.size() == 2, "missing Tab must not rearm alerts");
        garden(mc, 3, 4); scan(mc);
        garden(mc, 8, 5); scan(mc);
        check(sent.size() == 4, "both rearm after their own conditions clear");
        visitorNotificationEnabled = false; TAB_NOTIFICATIONS.resetVisitors();
        garden(mc, 1, 5); scan(mc); garden(mc, 4, 5); scan(mc);
        check(sent.size() == 5, "visitor toggle does not affect pests");
        pestNotificationEnabled = false; TAB_NOTIFICATIONS.resetPests();
        visitorNotificationEnabled = true; scan(mc);
        check(sent.size() == 6, "visitor still works when pests are disabled");
        garden(mc, 4, 4); scan(mc);
        pestWebhookUrl = ""; garden(mc, 4, 5); scan(mc);
        check(sent.size() == 6, "blank webhook skips sending and preserves pending alert");
        pestWebhookUrl = "test-only"; scan(mc);
        check(sent.size() == 7, "setting webhook after a match allows notification");
        mc.player = null; scan(mc);
        check(sent.size() == 7 && GardenTabReader.read(mc).isEmpty(), "disconnect safety");
        mc.player = new Player(); garden(mc, 4, 5); mc.level = null; scan(mc);
        check(sent.size() == 7, "world loading safety"); mc.level = new Object();

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
        System.out.println("TabNotificationIntegrationTest passed (polling, ordering, toggles, missing data, threshold UI)");
    }
'''
with tempfile.TemporaryDirectory(prefix="kuan-tab-test-") as tmp:
    test = Path(tmp) / "TabNotificationIntegrationTest.java"
    test.write_text(harness + reader + "\n" + screen + "\n" + poll + "\n}\n")
    java = Path(sys.argv[1]) / "bin"
    subprocess.run([str(java / "javac"), "--release", "21", "-encoding", "UTF-8", "-d", tmp,
                    str(base / "GardenTabNotifications.java"), str(test)], check=True)
    subprocess.run([str(java / "java"), "-cp", tmp, "dev.kuan.skyblockassistant.TabNotificationIntegrationTest"], check=True)
