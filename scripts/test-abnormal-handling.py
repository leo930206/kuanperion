#!/usr/bin/env python3
"""Run the legacy client's actual recovery methods with fake Minecraft boundaries."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
source = (root / "src/main/java/dev/kuan/skyblockassistant/KuanSkyBlockAssistantClient.java").read_text()


methods = [re.search(r"    private static final int FARMING_RECOVERY_COMMAND_DELAY_TICKS = \d+;", source).group(0)]
for name in ["handleFarmingAutoRecovery", "finishFarmingRecoveryOnArrival", "startFarmingAutoRecovery", "shouldAutoReturn", "handleReceivedMessage", "shouldLockManualLook", "onFarmingWorldChange", "tickFarmingCommandQueue", "enqueueFarmingCommand", "isFarmingRecoveryCommandReady"]:
    # Find declarations rather than earlier invocations.
    declaration = re.search(r"    (?:private|public) static [^\n]+ " + name + r"\(", source)
    start = declaration.start()
    opening = source.index("{", start)
    depth, end = 1, opening + 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    methods.append(source[start:end])

start = source.index("    private static final class QueuedCommand")
opening = source.index("{", start)
depth, end = 1, opening + 1
while depth:
    depth += (source[end] == "{") - (source[end] == "}")
    end += 1
methods.append(source[start:end])

for name in ["detectFarmingLocation", "sidebarLines", "containsLine", "cleanSidebarText"]:
    declaration = re.search(r"    private static [^\n]+ " + name + r"\(", source)
    start = declaration.start()
    opening = source.index("{", start)
    depth, end = 1, opening + 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    # Keep controlled location/sidebar stubs for recovery tests; exercise the real readers separately.
    method = source[start:end].replace("detectFarmingLocation(", "detectSidebarLocation(")
    method = method.replace("sidebarLines(", "readSidebarLines(")
    methods.append(method)

harness = r'''package dev.kuan.skyblockassistant;
import java.util.*;
import java.util.regex.*;
public final class AbnormalHandlingTest {
    static final FarmingToolGuard FARMING_TOOL_GUARD = new FarmingToolGuard();
    static boolean farmingToolDetectionEnabled;
    static boolean miningEnabled;
    static boolean farmingWorldChanged;
    static int completionMessages;
    static boolean farmingEnabled, farmingEnabledThisSession, abnormalHandlingEnabled;
    static boolean farmingAutoRecovering, farmingWasInGarden, pestNotificationEnabled;
    static int farmingRecoveryScanTicks, limboChatSeenTick, lastPestMessageTick, stops;
    static String pestWebhookUrl = "", lastPestMessage = "";
    static final Pattern PEST_MESSAGE_PATTERN = Pattern.compile("a^ ");
    static final Deque<QueuedCommand> FARMING_COMMAND_QUEUE = new ArrayDeque<>();
    static final List<String> sentCommands = new ArrayList<>();
    static final TestLogger LOGGER = new TestLogger();
    static class TestLogger { void info(String message, Object... values) {} }
    static void sendCommand(Minecraft mc, String command) { sentCommands.add(command); }
    static void queueTicks(Minecraft mc, int ticks) {
        for (int tick = 0; tick < ticks; tick++) tickFarmingCommandQueue(mc);
    }
    static FarmingLocation location;
    static boolean sidebarEmpty;
    enum FarmingLocation {
        GARDEN, ISLAND, HUB, LOBBY, LIMBO, UNKNOWN;
        final String label = name();
    }
    static class Minecraft {
        Object player = new Object();
        Level level = new Level();
        static Minecraft getInstance() { return new Minecraft(); }
    }
    static class Level {
        Scoreboard scoreboard = new Scoreboard();
        Scoreboard getScoreboard() { return scoreboard; }
    }
    enum DisplaySlot { SIDEBAR }
    static class Objective {
        Component getDisplayName() { return new Component("SKYBLOCK"); }
    }
    record PlayerScoreEntry(String owner, Component display) {
        boolean isHidden() { return owner.startsWith("#"); }
        Component ownerName() { return display != null ? display : new Component(owner); }
    }
    record PlayerTeam(String prefix, String suffix) {
        static Component formatNameForTeam(PlayerTeam team, Component name) {
            return team == null ? name : new Component(team.prefix + name.getString() + team.suffix);
        }
    }
    static class Scoreboard {
        Objective objective = new Objective();
        List<PlayerScoreEntry> entries = new ArrayList<>();
        Map<String, PlayerTeam> teams = new HashMap<>();
        Objective getDisplayObjective(DisplaySlot slot) { return objective; }
        List<PlayerScoreEntry> listPlayerScores(Objective obj) { return entries; }
        PlayerTeam getPlayersTeam(String owner) { return teams.get(owner); }
    }
    static class FakeLocationBridge {
        String island;
        String islandName() { return island; }
    }
    static final FakeLocationBridge SKYBLOCK_LOCATION_BRIDGE = new FakeLocationBridge();
    static void traceFarmingLocation(Minecraft mc, FarmingLocation value) {}
    static boolean holdingGameMenu;
    static boolean isHoldingGameMenu(Minecraft mc) { return holdingGameMenu; }
    static void sidebarRow(Minecraft mc, String prefix, String suffix) {
        Scoreboard board = mc.level.scoreboard;
        String owner = "§" + "abcdef0123456789".charAt(board.entries.size());
        board.entries.add(new PlayerScoreEntry(owner, null));
        board.teams.put(owner, new PlayerTeam(prefix, suffix));
    }
    record Component(String text) { String getString() { return text; } }
    static class StringUtil { static String stripColor(String text) { return text.replaceAll("(?i)§[0-9a-fk-or]", ""); } }
    static FarmingLocation detectFarmingLocation(Minecraft mc) { return location; }
    static List<String> sidebarLines(Minecraft mc) { return sidebarEmpty ? List.of() : List.of("SKYBLOCK"); }
    static void releaseFarmingKeys(Minecraft mc) {}
    static void saveConfig() {}
    static void stopFarming(Minecraft mc, boolean announce) {
        farmingWorldChanged = false;
        farmingAutoRecovering = false;
        FARMING_TOOL_GUARD.reset(null);
        farmingEnabled = false;
        farmingEnabledThisSession = false;
        FARMING_COMMAND_QUEUE.clear();
        stops++;
    }
    static int farmingTicksNow() { return 10000; }
    static void say(Minecraft mc, String... text) {
        if (String.join("", text).contains("已返回")) completionMessages++;
    }
    static String feature(String s) { return s; }
    static String success(String s) { return s; }
    static String warning(String s) { return s; }
    static String locationName(String s) { return s; }
    static String plain(String s) { return s; }
    static void sendPestDiscordNotification(String plot, String text) {}
    static void reset() {
        farmingWorldChanged = false;
        sentCommands.clear();
        SKYBLOCK_LOCATION_BRIDGE.island = null;
        miningEnabled = false;
        completionMessages = 0;
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
            check(!handleFarmingAutoRecovery(mc) && !farmingAutoRecovering, "location alone must not trigger " + target);
        }
        reset();
        handleReceivedMessage(new Component("Use /limbo for more information"));
        check(!farmingAutoRecovering, "Limbo chat must not bypass the world-change prerequisite");
        for (FarmingLocation target : List.of(FarmingLocation.ISLAND, FarmingLocation.HUB, FarmingLocation.LOBBY, FarmingLocation.LIMBO)) {
            reset();
            location = target;
            onFarmingWorldChange();
            check(FARMING_TOOL_GUARD.update(true, target == FarmingLocation.LIMBO ? "" : "Game Menu"), "precondition: teleport caused a wrong-tool wait");
            check(handleFarmingAutoRecovery(mc), "must react to " + target + " without prior Garden");
            check(farmingAutoRecovering && !farmingEnabled, "recovery disables Farming");
            check(!farmingWorldChanged, "one world change is consumed by one recovery");
            check(!FARMING_TOOL_GUARD.isPaused() && FARMING_TOOL_GUARD.expectedTool() == null, "recovery must clear the old tool wait and requirement");
            check(farmingToolDetectionEnabled, "recovery must preserve the tool detection setting");
            List<String> expected = switch (target) {
                case ISLAND, HUB -> List.of("warp garden@60");
                case LOBBY -> List.of("play sb@60", "warp garden@60");
                case LIMBO -> List.of("lobby@60", "play sb@60", "warp garden@60");
                default -> throw new AssertionError();
            };
            check(FARMING_COMMAND_QUEUE.stream().map(c -> c.command + "@" + c.ticks).toList().equals(expected), "all recovery commands wait 60 ticks (about three seconds)");
            int size = FARMING_COMMAND_QUEUE.size();
            handleFarmingAutoRecovery(mc);
            check(FARMING_COMMAND_QUEUE.size() == size, "recovery cannot enqueue duplicates");
            location = FarmingLocation.GARDEN;
            check(!FARMING_COMMAND_QUEUE.isEmpty(), "precondition: commands remain queued on arrival");
            handleFarmingAutoRecovery(mc);
            check(!farmingEnabled && !farmingAutoRecovering && !farmingEnabledThisSession, "Garden must finish immediately even with queued commands");
            check(FARMING_COMMAND_QUEUE.isEmpty(), "arrival cancels leftover return commands");
        }
        reset();
        location = FarmingLocation.HUB;
        onFarmingWorldChange();
        handleFarmingAutoRecovery(mc);
        check(shouldLockManualLook(), "recovery locks manual look before arrival");
        location = FarmingLocation.GARDEN;
        check(finishFarmingRecoveryOnArrival(mc), "start-of-tick arrival check finishes immediately");
        check(!shouldLockManualLook(), "arrival releases manual look lock immediately");
        check(!farmingAutoRecovering && !farmingEnabled && FARMING_COMMAND_QUEUE.isEmpty(), "arrival cancels pending commands and keeps Farming off");
        check(!finishFarmingRecoveryOnArrival(mc) && completionMessages == 1, "completion message is sent only once");
        reset();
        location = FarmingLocation.GARDEN;
        check(!finishFarmingRecoveryOnArrival(mc) && farmingEnabled, "normal Garden farming must not be stopped");
        reset();
        farmingAutoRecovering = true;
        check(!finishFarmingRecoveryOnArrival(mc) && farmingAutoRecovering, "unknown location must not finish recovery early");
        reset();
        location = FarmingLocation.HUB;
        onFarmingWorldChange();
        farmingRecoveryScanTicks = 0;
        for (int tick = 0; tick < 19; tick++) check(!handleFarmingAutoRecovery(mc), "retain scan cadence");
        check(handleFarmingAutoRecovery(mc), "scan triggers on tick 20");
        reset();
        handleReceivedMessage(new Component("You fell into the void!"));
        check(stops == 1 && !farmingEnabled && FARMING_COMMAND_QUEUE.isEmpty(), "void stops without warping, no Garden prerequisite");
        reset();
        onFarmingWorldChange();
        FARMING_TOOL_GUARD.update(true, "");
        handleReceivedMessage(new Component("Use /limbo for more information"));
        check(farmingAutoRecovering && !farmingEnabled, "Limbo message works without prior Garden");
        check(!FARMING_TOOL_GUARD.isPaused(), "chat-triggered Limbo recovery clears wrong-tool wait");
        reset();
        sidebarEmpty = false;
        onFarmingWorldChange();
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
        onFarmingWorldChange();
        check(!handleFarmingAutoRecovery(mc) && !farmingAutoRecovering, "world load alone does not choose a destination");
        location = FarmingLocation.HUB;
        farmingRecoveryScanTicks = 19;
        check(handleFarmingAutoRecovery(mc) && farmingAutoRecovering, "world load may precede destination update");
        reset();
        location = FarmingLocation.HUB;
        check(!handleFarmingAutoRecovery(mc), "destination may arrive before world load");
        onFarmingWorldChange();
        farmingRecoveryScanTicks = 19;
        check(handleFarmingAutoRecovery(mc), "destination and world load need not occur in the same tick");
        reset();
        handleReceivedMessage(new Component("Use /limbo for more information"));
        onFarmingWorldChange();
        location = FarmingLocation.LIMBO;
        check(handleFarmingAutoRecovery(mc), "Limbo message before world load still works after both signals");
        reset();
        abnormalHandlingEnabled = false;
        onFarmingWorldChange();
        check(!farmingWorldChanged, "disabled abnormal handling does not record events");
        reset();
        farmingEnabled = farmingEnabledThisSession = false;
        onFarmingWorldChange();
        check(!farmingWorldChanged, "inactive farming does not record initial join or unrelated loads");
        reset();
        farmingAutoRecovering = true;
        farmingEnabled = false;
        onFarmingWorldChange();
        check(!farmingWorldChanged, "return-route loads cannot arm a second recovery");
        reset();
        FARMING_TOOL_GUARD.update(true, "Unrelated Tool");
        check(!handleFarmingAutoRecovery(mc), "unknown location does not issue commands");
        check(FARMING_TOOL_GUARD.isPaused(), "normal wrong-tool wait remains active without abnormal recovery");
        // Reproduce the live incident: /lobby takes longer than the next command delay.
        reset(); mc = new Minecraft(); location = FarmingLocation.LIMBO;
        onFarmingWorldChange(); startFarmingAutoRecovery(mc, location);
        queueTicks(mc, 59);
        check(sentCommands.isEmpty(), "initial /lobby still waits a full three seconds");
        queueTicks(mc, 1);
        check(sentCommands.equals(List.of("lobby")), "first command leaves Limbo");
        location = FarmingLocation.UNKNOWN;
        queueTicks(mc, 300);
        check(sentCommands.equals(List.of("lobby")) && FARMING_COMMAND_QUEUE.size() == 2,
                "slow Lobby load must not consume /play sb or /warp garden");
        location = FarmingLocation.LOBBY;
        queueTicks(mc, 30);
        check(sentCommands.size() == 1, "Lobby arrival starts, rather than bypasses, the three-second wait");
        location = FarmingLocation.UNKNOWN; queueTicks(mc, 1);
        location = FarmingLocation.LOBBY; queueTicks(mc, 59);
        check(sentCommands.size() == 1, "losing destination readiness resets the full three-second countdown");
        queueTicks(mc, 1);
        check(sentCommands.equals(List.of("lobby", "play sb")), "send /play sb only after confirmed Lobby wait");
        queueTicks(mc, 180);
        check(sentCommands.size() == 2 && FARMING_COMMAND_QUEUE.size() == 1,
                "never dispatch /warp garden while still in Lobby");
        location = FarmingLocation.UNKNOWN; queueTicks(mc, 300);
        check(sentCommands.size() == 2, "slow SkyBlock load does not consume the Garden command");
        location = FarmingLocation.HUB;
        mc.player = null; queueTicks(mc, 120);
        check(sentCommands.size() == 2 && FARMING_COMMAND_QUEUE.size() == 1, "no player cannot discard pending commands");
        mc.player = new Object(); mc.level = null; queueTicks(mc, 120);
        check(sentCommands.size() == 2, "missing world cannot advance command countdown");
        mc.level = new Level(); queueTicks(mc, 59);
        check(sentCommands.size() == 2, "SkyBlock arrival also waits three seconds");
        queueTicks(mc, 1);
        check(sentCommands.equals(List.of("lobby", "play sb", "warp garden")), "complete route in order after real arrivals");
        location = FarmingLocation.GARDEN;
        check(finishFarmingRecoveryOnArrival(mc) && !shouldLockManualLook(), "arrival still finishes immediately and unlocks controls");
        for (FarmingLocation start : List.of(FarmingLocation.HUB, FarmingLocation.ISLAND, FarmingLocation.LOBBY)) {
            reset(); mc = new Minecraft(); location = start;
            onFarmingWorldChange(); startFarmingAutoRecovery(mc, location);
            queueTicks(mc, 59); check(sentCommands.isEmpty(), "retain initial three-second wait for " + start);
            queueTicks(mc, 1);
            check(sentCommands.equals(List.of(start == FarmingLocation.LOBBY ? "play sb" : "warp garden")), "retain first command for " + start);
        }
        // /play sb can land directly in Garden: do not send an unnecessary Garden warp.
        reset(); mc = new Minecraft(); location = FarmingLocation.LOBBY;
        onFarmingWorldChange(); startFarmingAutoRecovery(mc, location); queueTicks(mc, 60);
        location = FarmingLocation.GARDEN; tickFarmingCommandQueue(mc);
        check(sentCommands.equals(List.of("play sb")) && FARMING_COMMAND_QUEUE.isEmpty()
                && !farmingAutoRecovering, "direct Garden arrival cancels pending warp immediately");
        // SkyBlock can resume on another island, with either API or native title confirmation.
        for (boolean useApi : List.of(true, false)) {
            reset(); mc = new Minecraft(); location = FarmingLocation.LOBBY;
            onFarmingWorldChange(); startFarmingAutoRecovery(mc, location); queueTicks(mc, 60);
            location = FarmingLocation.UNKNOWN;
            if (useApi) SKYBLOCK_LOCATION_BRIDGE.island = "DWARVEN_MINES";
            else sidebarEmpty = false;
            queueTicks(mc, 60);
            check(sentCommands.equals(List.of("play sb", "warp garden")), "any confirmed SkyBlock island may warp to Garden");
        }
        reset();
        mc = new Minecraft();
        sidebarRow(mc, "§a⏣ Pri", "vate Island");
        sidebarRow(mc, "§a♟ Your ", "Island");
        check(detectSidebarLocation(mc) == FarmingLocation.ISLAND, "screenshot Island text stored in team prefix/suffix must be detected");
        mc = new Minecraft();
        sidebarRow(mc, "§a⏣ HUB", "");
        sidebarRow(mc, "§a♟ Your ", "Island");
        holdingGameMenu = true;
        check(detectSidebarLocation(mc) == FarmingLocation.HUB, "screenshot Hub plus Your Island must be Hub, not Island or Lobby");
        holdingGameMenu = false;
        mc = new Minecraft();
        sidebarRow(mc, "✦ PRIVATE\u00a0", "ISLAND ✦");
        check(detectSidebarLocation(mc) == FarmingLocation.ISLAND, "Private Island keyword alone suffices despite non-breaking space and icons");
        check(containsLine(List.of("§a[✦] pRiVaTe\u200b IsLaNd!"), "Private Island"), "ignore colors, symbols, invisible characters, case and spacing");
        check(containsLine(List.of("⏣ Hub"), "Hub"), "match a keyword within a decorated line");
        check(!containsLine(List.of("Your Island"), "Private Island"), "Your Island alone is not unique to the private island");
        mc = new Minecraft();
        sidebarRow(mc, "⏣ The ", "Garden");
        check(detectSidebarLocation(mc) == FarmingLocation.GARDEN, "Garden uses the same complete sidebar reader");
        mc = new Minecraft();
        mc.level.scoreboard.entries.add(new PlayerScoreEntry("entry", new Component("§a⏣ Hub")));
        check(detectSidebarLocation(mc) == FarmingLocation.HUB, "display-name-only entries remain supported");
        mc = new Minecraft();
        mc.level.scoreboard.entries.add(new PlayerScoreEntry("#hidden", new Component("Hub")));
        mc.level.scoreboard.entries.add(null);
        check(detectSidebarLocation(mc) == FarmingLocation.UNKNOWN, "hidden and null entries do not cause false detection");
        mc.level.scoreboard.objective = null;
        limboChatSeenTick = farmingTicksNow();
        check(detectSidebarLocation(mc) == FarmingLocation.LIMBO, "empty-sidebar Limbo detection remains supported");
        reset();
        holdingGameMenu = false;
        mc = new Minecraft();
        mc.level.scoreboard.objective = null;
        SKYBLOCK_LOCATION_BRIDGE.island = "PRIVATE_ISLAND";
        check(detectSidebarLocation(mc) == FarmingLocation.ISLAND, "API Private Island works without a native sidebar");
        mc = new Minecraft();
        sidebarRow(mc, "Plot - ", "1");
        SKYBLOCK_LOCATION_BRIDGE.island = "HUB";
        check(detectSidebarLocation(mc) == FarmingLocation.HUB, "API Hub overrides stale native Garden text");
        SKYBLOCK_LOCATION_BRIDGE.island = "GARDEN";
        mc.level.scoreboard.objective = null;
        check(detectSidebarLocation(mc) == FarmingLocation.GARDEN, "API Garden confirms successful return without native sidebar");
        SKYBLOCK_LOCATION_BRIDGE.island = "DWARVEN_MINES";
        mc = new Minecraft();
        sidebarRow(mc, "Hub", "");
        check(detectSidebarLocation(mc) == FarmingLocation.UNKNOWN, "known unsupported API island must not trigger a false Hub return");
        SKYBLOCK_LOCATION_BRIDGE.island = null;
        check(detectSidebarLocation(mc) == FarmingLocation.HUB, "native fallback still works when API unavailable");
        System.out.println("AbnormalHandlingTest: PASS (world-load prerequisite, both signal orders, arrival-gated three-second commands, slow world loads, void unchanged, API/native locations, arrival cleanup)");
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
