#!/usr/bin/env python3
"""Run the legacy client's actual recovery methods with fake Minecraft boundaries."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
source = (root / "src/main/java/dev/kuan/skyblockassistant/KuanSkyBlockAssistantClient.java").read_text()


methods = []
for name in ["handleFarmingAutoRecovery", "startFarmingAutoRecovery", "shouldAutoReturn", "handleReceivedMessage"]:
    # Find declarations rather than earlier invocations.
    declaration = re.search(r"    private static [^\n]+ " + name + r"\(", source)
    start = declaration.start()
    opening = source.index("{", start)
    depth, end = 1, opening + 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    methods.append(source[start:end])

harness = r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.regex.*;
public final class AbnormalHandlingTest {
    static final FarmingToolGuard FARMING_TOOL_GUARD = new FarmingToolGuard();
    static boolean farmingToolDetectionEnabled;
    static boolean farmingEnabled, farmingEnabledThisSession, abnormalHandlingEnabled;
    static boolean farmingAutoRecovering, farmingWasInGarden, pestNotificationEnabled;
    static int farmingRecoveryScanTicks, limboChatSeenTick, lastPestMessageTick, stops;
    static String pestWebhookUrl = "", lastPestMessage = "";
    static final Pattern PEST_MESSAGE_PATTERN = Pattern.compile("a^ ");
    static final Deque<String> FARMING_COMMAND_QUEUE = new ArrayDeque<>();
    static FarmingLocation location;
    static boolean sidebarEmpty;
    enum FarmingLocation {
        GARDEN, ISLAND, HUB, LOBBY, LIMBO, UNKNOWN;
        final String label = name();
    }
    static class Minecraft {
        static Minecraft getInstance() { return new Minecraft(); }
    }
    record Component(String text) { String getString() { return text; } }
    static class StringUtil { static String stripColor(String text) { return text; } }
    static FarmingLocation detectFarmingLocation(Minecraft mc) { return location; }
    static List<String> sidebarLines(Minecraft mc) { return sidebarEmpty ? List.of() : List.of("SKYBLOCK"); }
    static void releaseFarmingKeys(Minecraft mc) {}
    static void saveConfig() {}
    static void enqueueFarmingCommand(String command, int ticks) { FARMING_COMMAND_QUEUE.add(command + "@" + ticks); }
    static void stopFarming(Minecraft mc, boolean announce) {
        farmingEnabled = false;
        farmingEnabledThisSession = false;
        FARMING_COMMAND_QUEUE.clear();
        stops++;
    }
    static int farmingTicksNow() { return 10000; }
    static void say(Minecraft mc, String... text) {}
    static String feature(String s) { return s; }
    static String success(String s) { return s; }
    static String warning(String s) { return s; }
    static String locationName(String s) { return s; }
    static String plain(String s) { return s; }
    static void sendPestDiscordNotification(String plot, String text) {}
    static void reset() {
        FARMING_TOOL_GUARD.reset("Euclid's Wheat Sickle");
        farmingToolDetectionEnabled = true;
        farmingEnabled = farmingEnabledThisSession = abnormalHandlingEnabled = true;
        farmingAutoRecovering = farmingWasInGarden = pestNotificationEnabled = false;
        farmingRecoveryScanTicks = 19;
        limboChatSeenTick = -100000;
        sidebarEmpty = true;
        location = FarmingLocation.UNKNOWN;
        stops = 0;
        FARMING_COMMAND_QUEUE.clear();
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        for (FarmingLocation target : List.of(FarmingLocation.ISLAND, FarmingLocation.HUB, FarmingLocation.LOBBY, FarmingLocation.LIMBO)) {
            reset();
            location = target;
            check(FARMING_TOOL_GUARD.update(true, target == FarmingLocation.LIMBO ? "" : "Game Menu"), "precondition: teleport caused a wrong-tool wait");
            check(handleFarmingAutoRecovery(mc), "must react to " + target + " without prior Garden");
            check(farmingAutoRecovering && !farmingEnabled, "recovery disables Farming");
            check(!FARMING_TOOL_GUARD.isPaused() && FARMING_TOOL_GUARD.expectedTool() == null, "recovery must clear the old tool wait and requirement");
            check(farmingToolDetectionEnabled, "recovery must preserve the tool detection setting");
            List<String> expected = switch (target) {
                case ISLAND, HUB -> List.of("warp garden@20");
                case LOBBY -> List.of("play sb@80", "warp garden@120");
                case LIMBO -> List.of("lobby@20", "play sb@100", "warp garden@180");
                default -> throw new AssertionError();
            };
            check(new ArrayList<>(FARMING_COMMAND_QUEUE).equals(expected), "preserve recovery commands and delays");
            int size = FARMING_COMMAND_QUEUE.size();
            handleFarmingAutoRecovery(mc);
            check(FARMING_COMMAND_QUEUE.size() == size, "recovery cannot enqueue duplicates");
            location = FarmingLocation.GARDEN;
            FARMING_COMMAND_QUEUE.clear();
            handleFarmingAutoRecovery(mc);
            check(!farmingEnabled && !farmingAutoRecovering && !farmingEnabledThisSession, "Garden finishes recovery without restarting");
        }
        reset();
        location = FarmingLocation.HUB;
        farmingRecoveryScanTicks = 0;
        for (int tick = 0; tick < 19; tick++) check(!handleFarmingAutoRecovery(mc), "retain scan cadence");
        check(handleFarmingAutoRecovery(mc), "scan triggers on tick 20");
        reset();
        handleReceivedMessage(new Component("You fell into the void!"));
        check(stops == 1 && !farmingEnabled && FARMING_COMMAND_QUEUE.isEmpty(), "void stops without warping, no Garden prerequisite");
        reset();
        FARMING_TOOL_GUARD.update(true, "");
        handleReceivedMessage(new Component("Use /limbo for more information"));
        check(farmingAutoRecovering && !farmingEnabled, "Limbo message works without prior Garden");
        check(!FARMING_TOOL_GUARD.isPaused(), "chat-triggered Limbo recovery clears wrong-tool wait");
        reset();
        sidebarEmpty = false;
        handleReceivedMessage(new Component("Use /limbo for more information"));
        check(!farmingAutoRecovering, "retain empty-sidebar safeguard for Limbo");
        for (boolean disableSetting : List.of(true, false)) {
            reset();
            if (disableSetting) abnormalHandlingEnabled = false;
            else { farmingEnabled = false; farmingEnabledThisSession = false; }
            location = FarmingLocation.HUB;
            check(!handleFarmingAutoRecovery(mc), "disabled setting or stopped session must not recover");
            handleReceivedMessage(new Component("You fell into the void!"));
            handleReceivedMessage(new Component("Use /limbo for more information"));
            check(stops == 0 && !farmingAutoRecovering && FARMING_COMMAND_QUEUE.isEmpty(), "messages respect setting and session guards");
        }
        reset();
        FARMING_TOOL_GUARD.update(true, "Unrelated Tool");
        check(!handleFarmingAutoRecovery(mc), "unknown location does not issue commands");
        check(FARMING_TOOL_GUARD.isPaused(), "normal wrong-tool wait remains active without abnormal recovery");
        System.out.println("AbnormalHandlingTest: PASS (no Garden prerequisite, locations, void, Limbo, disabled guards, commands, no restart)");
    }
    // CLIENT_METHODS
}
'''
harness = harness.replace("    // CLIENT_METHODS", "\n".join(methods))
java_home = Path(sys.argv[1])
with tempfile.TemporaryDirectory() as tmp:
    path = Path(tmp) / "AbnormalHandlingTest.java"
    path.write_text(harness)
    subprocess.run([str(java_home / "bin/javac"), "--release", "21", "-encoding", "UTF-8", "-d", tmp, str(root / "src/main/java/dev/kuan/skyblockassistant/FarmingToolGuard.java"), str(path)], check=True)
    subprocess.run([str(java_home / "bin/java"), "-cp", tmp, "dev.kuan.skyblockassistant.AbnormalHandlingTest"], check=True)
