package dev.kuan.skyblockassistant;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.DisplaySlot;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Locale;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KuanSkyBlockAssistantClient implements ClientModInitializer {
    private static final String MOD_ID = "kuan-hypixel-skyblock-assistant";
    private static final String CONFIG_NAME = "kuan-hypixel-skyblock-assistant.properties";
    private static final List<String> TARGET_BLOCKS = new ArrayList<>();
    private static final FarmingBlockProfile FARMING_BLOCK_PROFILE = new FarmingBlockProfile();
    private static final Pattern PEST_MESSAGE_PATTERN = Pattern.compile("GROSS!.*Pest.*appeared in.*Plot\\s*-\\s*([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Map<BlockPos, Integer> SKIPPED_TARGETS = new HashMap<>();
    private static final Map<BlockPos, String> DEPLETED_TARGETS = new HashMap<>();
    private static final int UNREACHABLE_SKIP_TICKS = 200;
    private static final int FARM_TURN_STOP_TICKS = 4;
    private static final int FARM_TURN_RESUME_TICKS = 10;
    private static final int MUSHROOM_LEFT_SHIFT_TICKS = 10;
    private static final double FARMING_ONE_BLOCK_DISTANCE = 0.9;
    private static final int PUMPKIN_MELON_START_FORWARD_TICKS = 10;
    // Reserved for the flower crop profiles once their in-world block IDs and movement logic are configured.
    private static final String SUNFLOWER_TOOL_NAME = "Eclipse Sickle";
    private static final String MOONFLOWER_TOOL_NAME = "Eclipse Sickle";
    private static final String WILD_ROSE_TOOL_NAME = "Wild Rose Cutter";
    private static boolean miningEnabled = false;
    private static boolean selectMode = false;
    private static boolean farmingEnabled = false;
    private static boolean farmingEnabledThisSession = false;
    private static FarmingSelectMode farmingSelectMode = FarmingSelectMode.NONE;
    private static FarmingCrop farmingCrop = FarmingCrop.WHEAT;
    private static boolean farmingAutoSetHome = false;
    private static boolean pestNotificationEnabled = false;
    private static boolean farmingToolDetectionEnabled = false;
    private static boolean islandReturnEnabled = true;
    private static boolean hubReturnEnabled = true;
    private static boolean lobbyReturnEnabled = true;
    private static boolean limboReturnEnabled = true;
    private static String pestWebhookUrl = "";
    private static boolean farmingReverseLane = false;
    private static int farmingStateTicks = 0;
    private static int farmingLeftShiftTicks = 0;
    private static int farmingRightShiftTicks = 0;
    private static boolean farmingReturningToStart = false;
    private static boolean farmingAutoRecovering = false;
    private static boolean farmingRecoveryHomeSent = false;
    private static boolean farmingWasInGarden = false;
    private static boolean farmingResumeAfterRecovery = false;
    private static int farmingRecoveryScanTicks = 0;
    private static int limboChatSeenTick = -100000;
    private static final Deque<QueuedCommand> FARMING_COMMAND_QUEUE = new ArrayDeque<>();
    private static int farmingStuckTicks = 0;
    private static double farmingLastX = 0.0;
    private static double farmingLastY = 0.0;
    private static double farmingLastZ = 0.0;
    private static int farmingStuckSampleTicks = 0;
    private static int farmingMarkerIgnoreTicks = 0;
    private static boolean farmingCropDetected = false;
    private static int farmingCropProbeTicks = 0;
    private static PumpkinMelonState pumpkinMelonState = PumpkinMelonState.INACTIVE;
    private static double pumpkinMelonStepStartX = 0.0;
    private static double pumpkinMelonStepStartZ = 0.0;
    private static int pumpkinMelonStepTicks = 0;
    private static boolean pumpkinMelonMarkerCleared = false;
    private static boolean rotateToTarget = true;
    private static int scanRadius = 5;
    private static final float ROTATION_DONE_EPSILON = 1.5f;
    private static int rotationStepDegrees = 13;
    private static int actionDelayTicks = 3;
    private static int miningTicks = 0;
    private static int cooldown = 0;
    private static boolean rightWasDown = false;
    private static boolean miningComboWasDown = false;
    private static boolean farmingComboWasDown = false;
    private static String lastFarmingMarkerMessage = "";
    private static int lastFarmingMarkerMessageTick = 0;
    private static String lastPestMessage = "";
    private static int lastPestMessageTick = 0;
    private static BlockPos activeTarget = null;
    private static String activeTargetBlockId = null;
    private static boolean activeTargetStarted = false;
    private static boolean miningWaitingForTarget = false;
    private static KeyMapping settingsKey;
    private static KeyMapping toggleMiningKey;
    private static KeyMapping toggleFarmingKey;

    @Override
    public void onInitializeClient() {
        loadConfig();
        KeyMapping.Category category = KeyMapping.Category.register(id("keys"));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_APOSTROPHE, category));
        toggleMiningKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.toggle_mining", InputConstants.Type.KEYSYM, InputConstants.KEY_M, category));
        toggleFarmingKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.toggle_farming", InputConstants.Type.KEYSYM, InputConstants.KEY_F, category));
        ClientTickEvents.START_CLIENT_TICK.register(KuanSkyBlockAssistantClient::freezeMovementIfMining);
        ClientTickEvents.END_CLIENT_TICK.register(KuanSkyBlockAssistantClient::tick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleReceivedMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleReceivedMessage(message));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void tick(Minecraft mc) {
        while (settingsKey.consumeClick()) mc.setScreenAndShow(new SettingsScreen(null));
        handleShiftHotkeys(mc);
        handleRightClickSelection(mc);
        freezeMovementIfMining(mc);
        if (miningEnabled && mc.options != null) mc.options.pauseOnLostFocus = false;
        tickSkippedTargets();
        if (cooldown > 0) cooldown--;
        if (miningEnabled) {
            miningTicks++;
            mineBestTarget(mc);
        } else {
            miningTicks = 0;
        }
        if (farmingAutoRecovering) {
            if (mc.options != null) mc.options.pauseOnLostFocus = false;
            tickFarmingCommandQueue(mc);
            handleFarmingAutoRecovery(mc);
        } else if (farmingEnabled) {
            if (mc.options != null) mc.options.pauseOnLostFocus = false;
            tickFarming(mc);
        }
    }

    private static void handleShiftHotkeys(Minecraft mc) {
        if (mc.getWindow() == null) return;
        long handle = mc.getWindow().handle();
        boolean shift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean miningCombo = shift && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
        boolean farmingCombo = shift && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        if (miningCombo && !miningComboWasDown) toggleMining(mc);
        if (farmingCombo && !farmingComboWasDown) toggleFarming(mc);
        miningComboWasDown = miningCombo;
        farmingComboWasDown = farmingCombo;
    }

    private static void toggleMining(Minecraft mc) {
        if (miningEnabled) {
            miningEnabled = false;
            miningWaitingForTarget = false;
            clearActiveTarget(mc);
            saveConfig();
            sayAutoState(mc, "Mining", false);
            return;
        }
        if (TARGET_BLOCKS.isEmpty()) {
            say(mc, plain("Auto "), feature("Mining"), plain(" "), error("無法開啟"), plain("：尚未設定目標方塊"));
            return;
        }
        stopFarming(mc, false);
        miningEnabled = true;
        miningWaitingForTarget = false;
        saveConfig();
        sayAutoState(mc, "Mining", true);
    }

    private static void toggleFarming(Minecraft mc) {
        if (farmingEnabled) {
            stopFarming(mc, true);
        } else {
            miningEnabled = false;
            clearActiveTarget(mc);
            farmingEnabled = true;
            farmingEnabledThisSession = true;
            resetFarmingRuntime(mc);
            if (farmingAutoSetHome && mc.player != null) mc.player.connection.sendCommand("sethome");
            say(mc, plain("Auto "), feature("Farming"), plain(" "), success("開啟"), plain("，正在辨識準星指向的作物"));
        }
        saveConfig();
    }

    private static void stopFarming(Minecraft mc, boolean announce) {
        farmingEnabled = false;
        farmingEnabledThisSession = false;
        farmingAutoRecovering = false;
        farmingRecoveryHomeSent = false;
        farmingWasInGarden = false;
        farmingResumeAfterRecovery = false;
        FARMING_COMMAND_QUEUE.clear();
        releaseFarmingKeys(mc);
        if (announce) sayAutoState(mc, "Farming", false);
    }

    private static void resetFarmingRuntime(Minecraft mc) {
        farmingReverseLane = false;
        farmingStateTicks = 0;
        farmingLeftShiftTicks = 0;
        farmingRightShiftTicks = 0;
        farmingReturningToStart = false;
        farmingStuckTicks = 0;
        farmingStuckSampleTicks = 0;
        farmingMarkerIgnoreTicks = 0;
        farmingCropDetected = false;
        farmingCropProbeTicks = 0;
        pumpkinMelonState = PumpkinMelonState.INACTIVE;
        pumpkinMelonStepTicks = 0;
        pumpkinMelonMarkerCleared = false;
        farmingAutoRecovering = false;
        farmingRecoveryHomeSent = false;
        farmingWasInGarden = false;
        farmingResumeAfterRecovery = false;
        farmingRecoveryScanTicks = 0;
        FARMING_COMMAND_QUEUE.clear();
        if (mc.player != null) {
            farmingLastX = mc.player.getX();
            farmingLastY = mc.player.getY();
            farmingLastZ = mc.player.getZ();
            pumpkinMelonStepStartX = mc.player.getX();
            pumpkinMelonStepStartZ = mc.player.getZ();
        }
    }

    private static void tickFarming(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.options == null) {
            releaseFarmingKeys(mc);
            return;
        }
        tickFarmingCommandQueue(mc);
        if (handleFarmingAutoRecovery(mc)) return;
        if (isFarmingPauseScreen(mc.gui.screen())) {
            releaseFarmingKeys(mc);
            return;
        }
        LocalPlayer player = mc.player;
        if (probeStartingCrop(mc, player)) return;
        FarmingBlockProfile blockProfile = farmingProfile();
        lookAtFixed(player, farmingCrop.yaw, farmingCrop.pitch);
        if (farmingCrop == FarmingCrop.PUMPKIN_MELON) {
            tickPumpkinMelon(mc, player, blockProfile);
            return;
        }
        if (farmingReturningToStart) {
            setAttackKey(mc, false);
            if (blockProfile.startDetection && isConfiguredBlockNear(mc, player, blockProfile.startBlock)) {
                say(mc, feature("Farming"), plain(" 偵測到開始方塊："), blockName(blockProfile.startBlock));
                farmingReturningToStart = false;
                farmingReverseLane = false;
                farmingRightShiftTicks = MUSHROOM_LEFT_SHIFT_TICKS;
                farmingMarkerIgnoreTicks = 30;
                applyFarmingInput(mc, DirectionKey.RIGHT, false, false);
            } else {
                applyFarmingInput(mc, DirectionKey.BACK, false, false);
            }
            return;
        }
        setAttackKey(mc, true);
        if (farmingRightShiftTicks > 0) {
            farmingRightShiftTicks--;
            applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
            return;
        }
        if (farmingLeftShiftTicks > 0) {
            farmingLeftShiftTicks--;
            applyFarmingInput(mc, DirectionKey.LEFT, true, false);
            if (farmingLeftShiftTicks == 0) {
                farmingReverseLane = !farmingReverseLane;
                farmingMarkerIgnoreTicks = 30;
                if (farmingAutoSetHome) player.connection.sendCommand("sethome");
            }
            return;
        }
        if (farmingStateTicks > 0) {
            farmingStateTicks--;
            releaseFarmingKeys(mc);
            if (farmingStateTicks == FARM_TURN_RESUME_TICKS) {
                farmingReverseLane = !farmingReverseLane;
                if (farmingAutoSetHome) player.connection.sendCommand("sethome");
            }
            return;
        }
        setAttackKey(mc, true);
        pressFarmingMoveKeys(mc, farmingCrop, farmingReverseLane);
        if (blockProfile.endDetection && isConfiguredBlockNear(mc, player, blockProfile.endBlock)) {
            say(mc, feature("Farming"), plain(" 偵測到結束方塊："), blockName(blockProfile.endBlock), warning("，開始後退尋找開始方塊"));
            farmingReturningToStart = true;
            applyFarmingInput(mc, DirectionKey.BACK, false, false);
            return;
        }
        String marker = null;
        if (farmingMarkerIgnoreTicks > 0) {
            farmingMarkerIgnoreTicks--;
        } else if (blockProfile.rowDetection) {
            marker = findFarmingMarkerBlock(mc, player);
        }
        if (marker != null) {
            announceFarmingMarker(mc, marker);
            farmingMarkerIgnoreTicks = 30;
            if (farmingCrop == FarmingCrop.MUSHROOM) {
                farmingLeftShiftTicks = MUSHROOM_LEFT_SHIFT_TICKS;
            } else {
                farmingStateTicks = FARM_TURN_STOP_TICKS + FARM_TURN_RESUME_TICKS;
            }
            releaseFarmingKeys(mc);
            return;
        }
        farmingStuckSampleTicks++;
        if (farmingStuckSampleTicks >= 60) {
            double moved = player.distanceToSqr(farmingLastX, farmingLastY, farmingLastZ);
            farmingLastX = player.getX();
            farmingLastY = player.getY();
            farmingLastZ = player.getZ();
            farmingStuckSampleTicks = 0;
            if (moved < 1.0) farmingStuckTicks = 12;
        }
        if (farmingStuckTicks-- > 0) applyFarmingInput(mc, reversedKey(farmingCrop, farmingReverseLane), true, true);
    }

    private static boolean handleFarmingAutoRecovery(Minecraft mc) {
        if (!farmingEnabledThisSession) return false;
        FarmingLocation location = detectFarmingLocation(mc);
        if (farmingAutoRecovering) {
            releaseFarmingKeys(mc);
            if (location == FarmingLocation.GARDEN && FARMING_COMMAND_QUEUE.isEmpty()) {
                if (!farmingRecoveryHomeSent) {
                    sendCommand(mc, "home");
                    farmingRecoveryHomeSent = true;
                    enqueueFarmingCommand("__resume__", 60);
                    say(mc, feature("Farming"), success(" 已返回 "), locationName("Garden"), plain("，正在執行 "), commandName("/home"));
                }
            }
            return true;
        }
        farmingRecoveryScanTicks++;
        if (farmingRecoveryScanTicks < 20) return false;
        farmingRecoveryScanTicks = 0;
        if (location == FarmingLocation.GARDEN) {
            farmingWasInGarden = true;
            return false;
        }
        if (farmingWasInGarden && shouldAutoReturn(location)) {
            startFarmingAutoRecovery(mc, location, location == FarmingLocation.LOBBY ? 80 : 20);
            return true;
        }
        return false;
    }

    private static void startFarmingAutoRecovery(Minecraft mc, FarmingLocation location, int firstDelayTicks) {
        if (!shouldAutoReturn(location)) return;
        farmingAutoRecovering = true;
        farmingResumeAfterRecovery = true;
        farmingEnabled = false;
        farmingRecoveryHomeSent = false;
        farmingRecoveryScanTicks = 0;
        FARMING_COMMAND_QUEUE.clear();
        releaseFarmingKeys(mc);
        say(mc, feature("Farming"), warning(" 離開 "), locationName("Garden"), plain("：偵測到 "), locationName(location.label), warning("，開始自動回傳"));
        int delay = Math.max(1, firstDelayTicks);
        if (location == FarmingLocation.LIMBO) {
            enqueueFarmingCommand("lobby", delay);
            enqueueFarmingCommand("play sb", 100);
            enqueueFarmingCommand("warp garden", 180);
        } else if (location == FarmingLocation.LOBBY) {
            enqueueFarmingCommand("play sb", delay);
            enqueueFarmingCommand("warp garden", 120);
        } else if (location == FarmingLocation.HUB || location == FarmingLocation.ISLAND) {
            enqueueFarmingCommand("warp garden", delay);
        }
        saveConfig();
    }

    private static boolean shouldAutoReturn(FarmingLocation location) {
        return (location == FarmingLocation.ISLAND && islandReturnEnabled)
                || (location == FarmingLocation.HUB && hubReturnEnabled)
                || (location == FarmingLocation.LOBBY && lobbyReturnEnabled)
                || (location == FarmingLocation.LIMBO && limboReturnEnabled);
    }

    private static void startFarmingHomeRecovery(Minecraft mc, String reason) {
        farmingAutoRecovering = true;
        farmingResumeAfterRecovery = true;
        farmingEnabled = false;
        farmingRecoveryHomeSent = true;
        farmingRecoveryScanTicks = 0;
        FARMING_COMMAND_QUEUE.clear();
        releaseFarmingKeys(mc);
        say(mc, feature("Farming"), warning(" " + reason), plain("：正在執行 "), commandName("/home"), plain("，完成後重新開啟"));
        enqueueFarmingCommand("home", 20);
        enqueueFarmingCommand("__resume__", 80);
        saveConfig();
    }

    private static void tickFarmingCommandQueue(Minecraft mc) {
        if (FARMING_COMMAND_QUEUE.isEmpty()) return;
        QueuedCommand current = FARMING_COMMAND_QUEUE.peekFirst();
        current.ticks--;
        if (current.ticks > 0) return;
        FARMING_COMMAND_QUEUE.removeFirst();
        if ("__resume__".equals(current.command)) {
            farmingAutoRecovering = false;
            farmingRecoveryHomeSent = false;
            farmingEnabled = farmingResumeAfterRecovery;
            farmingEnabledThisSession = farmingResumeAfterRecovery;
            resetFarmingRuntime(mc);
            farmingWasInGarden = true;
            farmingResumeAfterRecovery = false;
            saveConfig();
            if (farmingEnabled) {
                say(mc, feature("Farming"), success(" 回傳完成"), plain("，正在恢復採集"));
            } else {
                say(mc, feature("Farming"), success(" 回傳流程完成"));
            }
        } else {
            sendCommand(mc, current.command);
        }
    }

    private static void enqueueFarmingCommand(String command, int delayTicks) {
        FARMING_COMMAND_QUEUE.addLast(new QueuedCommand(command, Math.max(1, delayTicks)));
    }

    private static void sendCommand(Minecraft mc, String command) {
        try { if (mc.player != null) mc.player.connection.sendCommand(command); } catch (Throwable ignored) {}
    }

    private static FarmingLocation detectFarmingLocation(Minecraft mc) {
        if (mc.player == null || mc.level == null) return FarmingLocation.UNKNOWN;
        List<String> lines = sidebarLines(mc);
        int now = farmingTicksNow();
        if (lines.isEmpty() && now - limboChatSeenTick <= 600) return FarmingLocation.LIMBO;
        if (isHoldingGameMenu(mc)) return FarmingLocation.LOBBY;
        if (containsLine(lines, "The Garden") || containsLine(lines, "Garden") || containsLine(lines, "Plot")) return FarmingLocation.GARDEN;
        if (containsLine(lines, "Private Island") && containsLine(lines, "Your Island")) return FarmingLocation.ISLAND;
        if (containsLine(lines, "Hub")) return FarmingLocation.HUB;
        return FarmingLocation.UNKNOWN;
    }

    private static boolean isHoldingGameMenu(Minecraft mc) {
        try {
            if (mc.player == null) return false;
            var stack = mc.player.getMainHandItem();
            if (stack == null || stack.isEmpty()) return false;
            String name = StringUtil.stripColor(stack.getHoverName().getString());
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return name.contains("Game Menu") || (id != null && id.toString().equals("minecraft:nether_star"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean containsLine(List<String> lines, String needle) {
        for (String line : lines) if (line.contains(needle)) return true;
        return false;
    }

    private static List<String> sidebarLines(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        try {
            Scoreboard scoreboard = mc.level.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) return lines;
            lines.add(cleanSidebarText(objective.getDisplayName().getString()));
            for (PlayerScoreEntry score : scoreboard.listPlayerScores(objective)) {
                if (score == null || score.isHidden()) continue;
                String text = score.display() != null ? score.display().getString() : score.owner();
                lines.add(cleanSidebarText(text));
            }
        } catch (Throwable ignored) {}
        return lines;
    }

    private static String cleanSidebarText(String text) {
        if (text == null) return "";
        return StringUtil.stripColor(text).replaceAll("[^A-Za-z ]", "").trim();
    }

    private static boolean looksLikeLimbo(Minecraft mc) {
        try {
            if (!(mc.hitResult instanceof BlockHitResult bhr)) return false;
            String id = blockId(mc.level.getBlockState(bhr.getBlockPos()));
            return id.contains("sign") && mc.player.getMainHandItem().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void setSelectedFarmingBlock(Minecraft mc, FarmingSelectMode mode, String name) {
        FarmingBlockProfile profile = farmingProfile();
        if (mode == FarmingSelectMode.ROW) {
            profile.rowBlock = name;
            say(mc, feature("Farming"), plain(" 共用換行方塊已設定："), blockName(name));
        } else if (mode == FarmingSelectMode.START) {
            profile.startBlock = name;
            say(mc, feature("Farming"), plain(" 共用開始方塊已設定："), blockName(name));
        } else if (mode == FarmingSelectMode.END) {
            profile.endBlock = name;
            say(mc, feature("Farming"), plain(" 共用結束方塊已設定："), blockName(name));
        }
    }

    private static boolean isConfiguredBlockNear(Minecraft mc, LocalPlayer player, String blockId) {
        if (blockId == null || blockId.isBlank()) return false;
        return isBlockNear(mc, player, blockId, 1, 0, 1);
    }

    private static void tickPumpkinMelon(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (pumpkinMelonState == PumpkinMelonState.INACTIVE) beginPumpkinMelonStep(player, PumpkinMelonState.START_FORWARD_ONE);
        switch (pumpkinMelonState) {
            case START_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                pumpkinMelonStepTicks++;
                if (pumpkinMelonStepTicks >= PUMPKIN_MELON_START_FORWARD_TICKS) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_LEFT;
                    announcePumpkinAction(mc, "開始持續往左採集");
                }
            }
            case HARVEST_LEFT -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonLaneMarkerDetected(mc, player, profile)) {
                    announceFarmingMarker(mc, profile.rowBlock.isBlank() ? farmingCrop.markerBlock : profile.rowBlock);
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_EXTRA_ONE);
                    announcePumpkinAction(mc, "原本往左，先繼續往左一格");
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case LEFT_EXTRA_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_FORWARD_TO_SECOND_MARKER);
                    announcePumpkinAction(mc, "左移完成，按住往前並尋找第二個換行方塊");
                }
            }
            case LEFT_FORWARD_TO_SECOND_MARKER -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                pumpkinMelonStepTicks++;
                if (pumpkinMelonStepTicks >= PUMPKIN_MELON_START_FORWARD_TICKS && findFarmingMarkerBlock(mc, player) != null) {
                    announcePumpkinAction(mc, "再次偵測到換行方塊，往右一格");
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_ONE);
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_FORWARD_ONE);
                    announcePumpkinAction(mc, "右移完成，往前一格");
                }
            }
            case LEFT_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_RIGHT;
                    announcePumpkinAction(mc, "開始持續往右採集");
                }
            }
            case HARVEST_RIGHT -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (profile.endDetection && isConfiguredBlockNear(mc, player, profile.endBlock)) {
                    say(mc, feature("Farming"), plain(" 偵測到結束方塊："), blockName(profile.endBlock), warning("，停止採集並往後尋找開始方塊"));
                    pumpkinMelonState = PumpkinMelonState.RETURN_BACK_TO_START;
                    applyFarmingInput(mc, DirectionKey.BACK, false, false);
                } else if (pumpkinMelonLaneMarkerDetected(mc, player, profile)) {
                    announceFarmingMarker(mc, profile.rowBlock.isBlank() ? farmingCrop.markerBlock : profile.rowBlock);
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_EXTRA_ONE);
                    announcePumpkinAction(mc, "原本往右，先繼續往右一格");
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_EXTRA_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_FORWARD_TO_SECOND_MARKER);
                    announcePumpkinAction(mc, "右移完成，按住往前並尋找第二個換行方塊");
                }
            }
            case RIGHT_FORWARD_TO_SECOND_MARKER -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                pumpkinMelonStepTicks++;
                if (pumpkinMelonStepTicks >= PUMPKIN_MELON_START_FORWARD_TICKS && findFarmingMarkerBlock(mc, player) != null) {
                    announcePumpkinAction(mc, "再次偵測到換行方塊，往左一格");
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_ONE);
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case LEFT_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_FORWARD_ONE);
                    announcePumpkinAction(mc, "左移完成，往前一格");
                }
            }
            case RIGHT_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_LEFT;
                    announcePumpkinAction(mc, "開始持續往左採集");
                }
            }
            case RETURN_BACK_TO_START -> {
                applyFarmingInput(mc, DirectionKey.BACK, false, false);
                if (profile.startDetection && isConfiguredBlockNear(mc, player, profile.startBlock)) {
                    say(mc, feature("Farming"), plain(" 偵測到開始方塊："), blockName(profile.startBlock));
                    beginPumpkinMelonStep(player, PumpkinMelonState.RETURN_BACK_ONE);
                    announcePumpkinAction(mc, "往後一格");
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                }
            }
            case RETURN_BACK_ONE -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RETURN_LEFT_ONE);
                    announcePumpkinAction(mc, "往左一格");
                }
            }
            case RETURN_LEFT_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RETURN_FORWARD_ONE);
                    announcePumpkinAction(mc, "往前一格");
                }
            }
            case RETURN_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_LEFT;
                    announcePumpkinAction(mc, "返回循環起點，開始持續往左採集");
                }
            }
            case INACTIVE -> { }
        }
    }

    private static boolean pumpkinMelonLaneMarkerDetected(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (!profile.rowDetection) return false;
        boolean markerPresent = findFarmingMarkerBlock(mc, player) != null;
        if (!pumpkinMelonMarkerCleared) {
            if (!markerPresent) pumpkinMelonMarkerCleared = true;
            return false;
        }
        if (markerPresent) {
            pumpkinMelonMarkerCleared = false;
            return true;
        }
        return false;
    }

    private static void beginPumpkinMelonStep(LocalPlayer player, PumpkinMelonState state) {
        pumpkinMelonState = state;
        pumpkinMelonStepStartX = player.getX();
        pumpkinMelonStepStartZ = player.getZ();
        pumpkinMelonStepTicks = 0;
    }

    private static boolean pumpkinMelonStepDone(LocalPlayer player) {
        pumpkinMelonStepTicks++;
        double dx = player.getX() - pumpkinMelonStepStartX;
        double dz = player.getZ() - pumpkinMelonStepStartZ;
        return dx * dx + dz * dz >= FARMING_ONE_BLOCK_DISTANCE * FARMING_ONE_BLOCK_DISTANCE
                || pumpkinMelonStepTicks >= 20;
    }

    private static boolean probeStartingCrop(Minecraft mc, LocalPlayer player) {
        if (farmingCropDetected) return false;
        releaseFarmingKeys(mc);
        farmingCropProbeTicks++;
        HitResult hit = mc.hitResult instanceof BlockHitResult ? mc.hitResult : player.pick(5.0, 0.0f, false);
        if (hit instanceof BlockHitResult blockHit) {
            String cropBlockId = blockId(mc.level.getBlockState(blockHit.getBlockPos()));
            FarmingCrop detected = FarmingCrop.fromBlockId(cropBlockId);
            if (detected != null) {
                String expectedTool = expectedToolForCropBlock(cropBlockId);
                if (farmingToolDetectionEnabled && expectedTool != null) {
                    String heldTool = StringUtil.stripColor(player.getMainHandItem().getHoverName().getString());
                    if (!heldTool.toLowerCase(Locale.ROOT).contains(expectedTool.toLowerCase(Locale.ROOT))) {
                        say(mc, feature("Farming"), error(" 工具錯誤："), cropName(detected.label), plain(" 需要 "),
                                warning(expectedTool), plain("，目前拿著："), blockName(heldTool.isBlank() ? "空手" : heldTool));
                        stopFarming(mc, false);
                        saveConfig();
                        return true;
                    }
                    say(mc, feature("Farming"), success(" 工具確認："), warning(expectedTool));
                }
                farmingCrop = detected;
                farmingCropDetected = true;
                farmingReverseLane = false;
                pumpkinMelonState = detected == FarmingCrop.PUMPKIN_MELON ? PumpkinMelonState.START_FORWARD_ONE : PumpkinMelonState.INACTIVE;
                pumpkinMelonStepTicks = 0;
                pumpkinMelonMarkerCleared = false;
                pumpkinMelonStepStartX = player.getX();
                pumpkinMelonStepStartZ = player.getZ();
                MushroomColor mushroom = MushroomColor.fromBlockId(cropBlockId);
                if (detected == FarmingCrop.MUSHROOM) {
                    farmingReverseLane = mushroom == MushroomColor.BROWN;
                    say(mc, plain("Auto "), feature("Farming"), plain(" "), success("開啟"), plain("，方向確認："),
                            cropName(mushroom == MushroomColor.BROWN ? "Brown Mushroom" : "Red Mushroom"));
                } else {
                    String detectedName = "minecraft:melon".equals(cropBlockId) ? "Melon"
                            : "minecraft:pumpkin".equals(cropBlockId) ? "Pumpkin" : detected.label;
                    say(mc, feature("Farming"), plain(" 準星作物辨識："), cropName(detectedName));
                    if (detected == FarmingCrop.PUMPKIN_MELON) announcePumpkinAction(mc, "按住往前約一格的時間");
                }
                saveConfig();
                return true;
            }
        }
        if (farmingCropProbeTicks >= 100) {
            say(mc, feature("Farming"), error(" 無法辨識準星指向的作物，已關閉"), plain("：請將準星對準作物後再開啟"));
            stopFarming(mc, false);
            saveConfig();
        }
        return true;
    }

    private static String expectedToolForCropBlock(String cropBlockId) {
        if (cropBlockId == null) return null;
        return switch (cropBlockId) {
            case "minecraft:wheat" -> "Euclid's Wheat Sickle";
            case "minecraft:carrots" -> "Gauss Carrot Shovel";
            case "minecraft:potatoes" -> "Pythagorean Potato Shovel";
            case "minecraft:sugar_cane" -> "Turing Sugar Cane Cutter";
            case "minecraft:cactus" -> "Cactus Knife";
            case "minecraft:cocoa" -> "Cocoa Chopper";
            case "minecraft:nether_wart" -> "Newton Nether Wart Cutter";
            case "minecraft:red_mushroom", "minecraft:red_mushroom_block",
                 "minecraft:brown_mushroom", "minecraft:brown_mushroom_block" -> "Fungi Cutter";
            case "minecraft:melon" -> "Melon Dicer";
            case "minecraft:pumpkin" -> "Pumpkin Dicer";
            default -> null;
        };
    }

    private static String findFarmingMarkerBlock(Minecraft mc, LocalPlayer player) {
        if (!farmingProfile().rowBlock.isBlank()) return findConfiguredFarmingMarker(mc, player);
        return farmingCrop.findDefaultMarker(mc, player);
    }

    private static String findConfiguredFarmingMarker(Minecraft mc, LocalPlayer player) {
        if (mc.level == null) return null;
        String expected = farmingProfile().rowBlock;
        BlockPos base = player.blockPosition();
        for (int y = 0; y <= 1; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            String id = blockId(mc.level.getBlockState(base.offset(dx, y, dz)));
            if (blockMatches(id, expected)) return id;
        }
        return null;
    }

    private static void announceFarmingMarker(Minecraft mc, String marker) {
        int now = farmingTicksNow();
        if (!marker.equals(lastFarmingMarkerMessage) || now - lastFarmingMarkerMessageTick > 40) {
            say(mc, feature("Farming"), plain(" 偵測到換行方塊："), blockName(marker));
            lastFarmingMarkerMessage = marker;
            lastFarmingMarkerMessageTick = now;
        }
    }

    private static void announcePumpkinAction(Minecraft mc, String action) {
        say(mc, feature("Farming"), plain(" "), cropName(FarmingCrop.PUMPKIN_MELON.label), plain(" 動作："), warning(action));
    }

    private static int farmingTicksNow() {
        return Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
    }

    private static boolean isFarmingPauseScreen(Screen screen) {
        return screen instanceof ChatScreen
                || screen instanceof InventoryScreen
                || screen instanceof CreativeModeInventoryScreen;
    }

    private static void pressFarmingMoveKeys(Minecraft mc, FarmingCrop crop, boolean reversed) {
        applyFarmingInput(mc, reversedKey(crop, reversed), true, false);
    }

    private static DirectionKey reversedKey(FarmingCrop crop, boolean reversed) {
        return reversed ? crop.reverseKey : crop.forwardKey;
    }

    private static void applyFarmingInput(Minecraft mc, DirectionKey key, boolean attack, boolean jump) {
        if (mc == null || mc.options == null) return;
        boolean forward = key == DirectionKey.FORWARD;
        boolean back = key == DirectionKey.BACK;
        boolean left = key == DirectionKey.LEFT;
        boolean right = key == DirectionKey.RIGHT;
        setAttackKey(mc, attack);
        mc.options.keyUp.setDown(forward);
        mc.options.keyDown.setDown(back);
        mc.options.keyLeft.setDown(left);
        mc.options.keyRight.setDown(right);
        mc.options.keyJump.setDown(jump);
        mc.options.keyShift.setDown(false);
        if (mc.player != null && mc.player.input != null) {
            mc.player.input.keyPresses = new Input(forward, back, left, right, jump, false, false);
            mc.player.setSprinting(false);
        }
    }

    private static void releaseFarmingKeys(Minecraft mc) {
        setAttackKey(mc, false);
        releaseFarmingMovementKeys(mc);
    }

    private static void releaseFarmingMovementKeys(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        if (mc.player != null && mc.player.input != null) mc.player.input.keyPresses = Input.EMPTY;
    }

    private static void lookAtFixed(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    private static boolean isBlockNear(Minecraft mc, LocalPlayer player, String expected, int radius, int yMin, int yMax) {
        if (mc.level == null) return false;
        BlockPos base = player.blockPosition();
        for (int y = yMin; y <= yMax; y++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (blockMatches(blockId(mc.level.getBlockState(base.offset(dx, y, dz))), expected)) return true;
        }
        return false;
    }

    private static boolean blockMatches(String actual, String expected) {
        if (actual == null || expected == null) return false;
        if (actual.equals(expected)) return true;
        if ("#trapdoor".equals(expected)) return actual.endsWith("_trapdoor") || actual.endsWith(":trapdoor");
        if ("#ladder".equals(expected)) return actual.equals("minecraft:ladder");
        if ("#dirt".equals(expected)) return actual.equals("minecraft:dirt") || actual.equals("minecraft:grass_block");
        return false;
    }

    private static void freezeMovementIfMining(Minecraft mc) {
        if (!miningEnabled || mc.player == null) return;
        LocalPlayer player = mc.player;
        if (player.input != null) player.input.keyPresses = Input.EMPTY;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(0.0, velocity.y, 0.0);
        player.setSprinting(false);
    }

    private static void handleRightClickSelection(Minecraft mc) {
        if ((!selectMode && farmingSelectMode == FarmingSelectMode.NONE) || mc.player == null || mc.level == null || mc.getWindow() == null) {
            rightWasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (down && !rightWasDown && mc.hitResult instanceof BlockHitResult bhr) {
            BlockState state = mc.level.getBlockState(bhr.getBlockPos());
            String name = blockId(state);
            if (!state.isAir() && isAllowedTargetBlock(name)) {
                if (farmingSelectMode != FarmingSelectMode.NONE) {
                    setSelectedFarmingBlock(mc, farmingSelectMode, name);
                    farmingSelectMode = FarmingSelectMode.NONE;
                    saveConfig();
                } else {
                    TARGET_BLOCKS.remove(name);
                    TARGET_BLOCKS.add(name);
                    selectMode = false;
                    saveConfig();
                    say(mc, feature("Mining"), plain(" 目標方塊已設定 #" + TARGET_BLOCKS.size() + "："), blockName(name), success("（選取結束）"));
                }
            } else if ("minecraft:bedrock".equals(name)) {
                say(mc, feature("Mining"), error(" 設定失敗"), plain("："), blockName("minecraft:bedrock"), error(" 不可設為目標方塊"));
            }
        }
        rightWasDown = down;
    }

    private static void mineBestTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null || TARGET_BLOCKS.isEmpty()) {
            clearActiveTarget(mc);
            return;
        }
        LocalPlayer player = mc.player;
        if (activeTarget != null && mineActiveTarget(mc, player, activeTarget)) return;
        if (cooldown > 0) return;

        BlockPos base = player.blockPosition();
        Candidate best = null;
        int r = Math.max(1, Math.min(8, scanRadius));
        for (BlockPos p0 : BlockPos.betweenClosed(base.offset(-r, -r, -r), base.offset(r, r, r))) {
            BlockPos pos = p0.immutable();
            if (SKIPPED_TARGETS.containsKey(pos)) continue;
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) continue;
            String id = blockId(state);
            String waitingFor = DEPLETED_TARGETS.get(pos);
            if (waitingFor != null) {
                if (!waitingFor.equals(id)) continue;
                DEPLETED_TARGETS.remove(pos);
            }
            if (!isAllowedTargetBlock(id) || !TARGET_BLOCKS.contains(id)) continue;
            double dist = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist > (r + 1.5) * (r + 1.5)) continue;
            if (!canSeeBlockCenter(player, mc, pos, r + 1.5)) continue;
            int priority = TARGET_BLOCKS.indexOf(id);
            Candidate c = new Candidate(pos, priority, dist);
            if (best == null || c.betterThan(best)) best = c;
        }
        if (best == null) {
            if (!miningWaitingForTarget) {
                miningWaitingForTarget = true;
                say(mc, feature("Mining"), warning(" 等待中"), plain("：目前沒有可採集的目標方塊"));
            }
            return;
        }
        if (miningWaitingForTarget) {
            miningWaitingForTarget = false;
            say(mc, feature("Mining"), success(" 已找到目標"), plain("，恢復自動挖礦"));
        }
        activeTarget = best.pos;
        activeTargetBlockId = blockId(mc.level.getBlockState(activeTarget));
        activeTargetStarted = false;
        mineActiveTarget(mc, player, activeTarget);
    }

    private static boolean mineActiveTarget(Minecraft mc, LocalPlayer player, BlockPos target) {
        if (mc.level == null || mc.gameMode == null) return false;
        int r = Math.max(1, Math.min(8, scanRadius));
        double dist = player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        BlockState state = mc.level.getBlockState(target);
        String currentBlockId = blockId(state);
        if (activeTargetBlockId != null && !activeTargetBlockId.equals(currentBlockId)) {
            markDepletedUntilRespawn(target, activeTargetBlockId);
            clearActiveTarget(mc);
            cooldown = Math.max(1, actionDelayTicks);
            return false;
        }
        if (state.isAir() || dist > (r + 1.5) * (r + 1.5)) {
            if (state.isAir() && activeTargetBlockId != null) markDepletedUntilRespawn(target, activeTargetBlockId);
            clearActiveTarget(mc);
            cooldown = Math.max(1, actionDelayTicks);
            return false;
        }

        if (rotateToTarget && !lookAtSmooth(player, target)) return true;
        BlockPos aimedTarget = getAimedTargetBlock(mc, player, r + 1.5);
        if (aimedTarget == null || !aimedTarget.equals(target)) {
            skipTarget(target);
            clearActiveTarget(mc);
            cooldown = Math.max(1, actionDelayTicks);
            return false;
        }
        setAttackKey(mc, true);
        Direction face = faceFromPlayer(player, target);
        if (!activeTargetStarted) {
            mc.gameMode.startDestroyBlock(target, face);
            activeTargetStarted = true;
        }
        mc.gameMode.continueDestroyBlock(target, face);
        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private static void clearActiveTarget(Minecraft mc) {
        activeTarget = null;
        activeTargetBlockId = null;
        activeTargetStarted = false;
        setAttackKey(mc, false);
        try { if (mc != null && mc.gameMode != null) mc.gameMode.stopDestroyBlock(); } catch (Throwable ignored) {}
    }

    private static void setAttackKey(Minecraft mc, boolean down) {
        try { if (mc != null && mc.options != null) mc.options.keyAttack.setDown(down); } catch (Throwable ignored) {}
    }

    private static void skipTarget(BlockPos pos) {
        SKIPPED_TARGETS.put(pos.immutable(), UNREACHABLE_SKIP_TICKS);
    }

    private static void markDepletedUntilRespawn(BlockPos pos, String expectedBlockId) {
        if (isAllowedTargetBlock(expectedBlockId)) {
            DEPLETED_TARGETS.put(pos.immutable(), expectedBlockId);
        }
        SKIPPED_TARGETS.remove(pos);
    }

    private static void tickSkippedTargets() {
        Iterator<Map.Entry<BlockPos, Integer>> it = SKIPPED_TARGETS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> e = it.next();
            int next = e.getValue() - 1;
            if (next <= 0) it.remove(); else e.setValue(next);
        }
    }

    private static boolean canSeeBlockCenter(LocalPlayer player, Minecraft mc, BlockPos pos, double reach) {
        if (mc.level == null) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        if (eye.distanceToSqr(center) > reach * reach) return false;
        HitResult hit = player.pick(reach, 0.0f, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos)) return true;
        return hasLineOfSightToCenter(mc, eye, center, pos);
    }

    private static boolean hasLineOfSightToCenter(Minecraft mc, Vec3 eye, Vec3 center, BlockPos target) {
        Vec3 delta = center.subtract(eye);
        int steps = Math.max(8, (int) Math.ceil(delta.length() * 8.0));
        for (int i = 1; i < steps; i++) {
            Vec3 p = eye.add(delta.scale(i / (double) steps));
            BlockPos bp = BlockPos.containing(p);
            if (bp.equals(target)) return true;
            if (!mc.level.getBlockState(bp).isAir()) return false;
        }
        return true;
    }

    private static BlockPos getAimedTargetBlock(Minecraft mc, LocalPlayer player, double reach) {
        if (mc.level == null) return null;
        HitResult hit = player.pick(reach, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit)) return null;
        BlockPos pos = blockHit.getBlockPos().immutable();
        String aimedBlockId = blockId(mc.level.getBlockState(pos));
        if (!isAllowedTargetBlock(aimedBlockId) || !TARGET_BLOCKS.contains(aimedBlockId)) return null;
        return pos;
    }

    private static Direction faceFromPlayer(LocalPlayer player, BlockPos pos) {
        BlockPos pp = player.blockPosition();
        int dx = pos.getX() - pp.getX();
        int dy = pos.getY() - pp.getY();
        int dz = pos.getZ() - pp.getZ();
        if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz)) return dy > 0 ? Direction.DOWN : Direction.UP;
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? Direction.WEST : Direction.EAST;
        return dz > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    private static boolean lookAtSmooth(LocalPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        double dx = pos.getX() + 0.5 - eye.x;
        double dy = pos.getY() + 0.5 - eye.y;
        double dz = pos.getZ() + 0.5 - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, h)));
        float yawDiff = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDiff = Mth.wrapDegrees(targetPitch - player.getXRot());
        float nextYaw = player.getYRot() + Mth.clamp(yawDiff, -rotationStepDegrees, rotationStepDegrees);
        float nextPitch = player.getXRot() + Mth.clamp(pitchDiff, -rotationStepDegrees, rotationStepDegrees);
        player.setYRot(nextYaw);
        player.setXRot(Mth.clamp(nextPitch, -90.0f, 90.0f));
        player.setYHeadRot(nextYaw);
        player.setYBodyRot(nextYaw);
        return Math.abs(yawDiff) <= ROTATION_DONE_EPSILON && Math.abs(pitchDiff) <= ROTATION_DONE_EPSILON;
    }

    private static boolean isAllowedTargetBlock(String id) {
        return id != null && !id.equals("minecraft:bedrock") && !id.equals("minecraft:air") && !id.equals("unknown");
    }

    private static String blockId(BlockState state) {
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "unknown" : id.toString();
    }

    private static final int COLOR_FEATURE = 0xC084FC;
    private static final int COLOR_PLAIN = 0xFFFFFF;
    private static final int COLOR_SUCCESS = 0x55FF55;
    private static final int COLOR_ERROR = 0xFF5555;
    private static final int COLOR_WARNING = 0xFFAA00;
    private static final int COLOR_BLOCK = 0xFFFF55;
    private static final int COLOR_LOCATION = 0x55FFFF;
    private static final int COLOR_COMMAND = 0xFFD166;
    private static final int COLOR_CROP = 0xADFF2F;

    private static void say(Minecraft mc, Component... parts) {
        if (mc.player == null) return;
        MutableComponent message = hsbaPrefix();
        for (Component part : parts) message.append(part);
        mc.player.sendSystemMessage(message);
    }

    private static MutableComponent hsbaPrefix() {
        String text = "[Kuan HSBA]";
        int start = 0x55FFFF;
        int end = 0x2F80FF;
        MutableComponent out = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double t = text.length() <= 1 ? 0.0 : i / (double)(text.length() - 1);
            int r = (int)(((start >> 16) & 0xFF) * (1.0 - t) + ((end >> 16) & 0xFF) * t);
            int g = (int)(((start >> 8) & 0xFF) * (1.0 - t) + ((end >> 8) & 0xFF) * t);
            int b = (int)((start & 0xFF) * (1.0 - t) + (end & 0xFF) * t);
            int color = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(style -> style.withColor(color).withBold(true)));
        }
        return out.append(Component.literal(" "));
    }

    private static Component plain(String text) { return Component.literal(text).withColor(COLOR_PLAIN); }
    private static Component feature(String text) { return Component.literal(text).withStyle(style -> style.withColor(COLOR_FEATURE).withBold(true)); }
    private static Component success(String text) { return Component.literal(text).withColor(COLOR_SUCCESS); }
    private static Component error(String text) { return Component.literal(text).withColor(COLOR_ERROR); }
    private static Component warning(String text) { return Component.literal(text).withColor(COLOR_WARNING); }
    private static Component blockName(String text) { return Component.literal(text).withColor(COLOR_BLOCK); }
    private static Component locationName(String text) { return Component.literal(text).withColor(COLOR_LOCATION); }
    private static Component commandName(String text) { return Component.literal(text).withColor(COLOR_COMMAND); }
    private static Component cropName(String text) { return Component.literal(text).withStyle(style -> style.withColor(COLOR_CROP).withBold(true)); }

    private static void sayAutoState(Minecraft mc, String featureName, boolean enabled) {
        say(mc, plain("Auto "), feature(featureName), plain(" "), enabled ? success("開啟") : error("關閉"));
    }

    private static void handleReceivedMessage(Component message) {
        if (message == null) return;
        String text = StringUtil.stripColor(message.getString());
        int now = farmingTicksNow();
        if (text.contains("You fell into the void") && farmingEnabled && farmingEnabledThisSession && farmingWasInGarden && !farmingAutoRecovering && detectFarmingLocation(Minecraft.getInstance()) == FarmingLocation.GARDEN) {
            startFarmingHomeRecovery(Minecraft.getInstance(), "掉入虛空");
            return;
        }
        if (text.contains("/limbo for more information")) {
            limboChatSeenTick = now;
            if (farmingEnabled && farmingEnabledThisSession && farmingWasInGarden && !farmingAutoRecovering && limboReturnEnabled && sidebarLines(Minecraft.getInstance()).isEmpty()) {
                startFarmingAutoRecovery(Minecraft.getInstance(), FarmingLocation.LIMBO, 20);
                return;
            }
        }
        if (!pestNotificationEnabled || pestWebhookUrl.isBlank()) return;
        Matcher matcher = PEST_MESSAGE_PATTERN.matcher(text);
        if (!matcher.find()) return;
        if (text.equals(lastPestMessage) && now - lastPestMessageTick < 200) return;
        lastPestMessage = text;
        lastPestMessageTick = now;
        String plot = matcher.group(1);
        sendPestDiscordNotification(plot, text);
    }

    private static void sendPestDiscordNotification(String plot, String rawMessage) {
        try {
            String content = "害蟲出現！Plot " + plot + "\n" + rawMessage;
            String json = "{\"content\":\"" + jsonEscape(content) + "\"}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(pestWebhookUrl.trim()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding()).exceptionally(err -> null);
        } catch (Throwable ignored) {}
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int)c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static File configFile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, CONFIG_NAME);
    }

    private static void loadConfig() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(configFile())) { p.load(in); } catch (Throwable ignored) {}
        // Runtime toggles should not auto-resume on client/server join; otherwise recovery can fire while just entering Hypixel.
        miningEnabled = false;
        farmingEnabled = false;
        farmingEnabledThisSession = false;
        farmingCrop = FarmingCrop.fromConfig(p.getProperty("farmingCrop", "WHEAT"));
        farmingAutoSetHome = Boolean.parseBoolean(p.getProperty("farmingAutoSetHome", "false"));
        pestNotificationEnabled = Boolean.parseBoolean(p.getProperty("pestNotificationEnabled", "false"));
        farmingToolDetectionEnabled = Boolean.parseBoolean(p.getProperty("farmingToolDetectionEnabled", "false"));
        islandReturnEnabled = Boolean.parseBoolean(p.getProperty("islandReturnEnabled", "true"));
        hubReturnEnabled = Boolean.parseBoolean(p.getProperty("hubReturnEnabled", "true"));
        lobbyReturnEnabled = Boolean.parseBoolean(p.getProperty("lobbyReturnEnabled", "true"));
        limboReturnEnabled = Boolean.parseBoolean(p.getProperty("limboReturnEnabled", "true"));
        pestWebhookUrl = p.getProperty("pestWebhookUrl", "").trim();
        loadFarmingBlockProfiles(p);
        if (miningEnabled && farmingEnabled) farmingEnabled = false;
        rotateToTarget = Boolean.parseBoolean(p.getProperty("rotateToTarget", "true"));
        scanRadius = parseInt(p.getProperty("scanRadius"), 5, 1, 8);
        rotationStepDegrees = parseInt(p.getProperty("rotationStepDegrees"), 13, 1, 40);
        actionDelayTicks = parseInt(p.getProperty("actionDelayTicks"), 3, 1, 20);
        TARGET_BLOCKS.clear();
        String targets = p.getProperty("targets", "").trim();
        if (!targets.isEmpty()) for (String s : targets.split(",")) {
            String id = s.trim();
            if (!id.isBlank() && isAllowedTargetBlock(id) && !TARGET_BLOCKS.contains(id)) TARGET_BLOCKS.add(id);
        }
    }

    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("miningEnabled", "false");
        p.setProperty("farmingEnabled", "false");
        p.setProperty("farmingCrop", farmingCrop.name());
        p.setProperty("farmingAutoSetHome", Boolean.toString(farmingAutoSetHome));
        p.setProperty("pestNotificationEnabled", Boolean.toString(pestNotificationEnabled));
        p.setProperty("farmingToolDetectionEnabled", Boolean.toString(farmingToolDetectionEnabled));
        p.setProperty("islandReturnEnabled", Boolean.toString(islandReturnEnabled));
        p.setProperty("hubReturnEnabled", Boolean.toString(hubReturnEnabled));
        p.setProperty("lobbyReturnEnabled", Boolean.toString(lobbyReturnEnabled));
        p.setProperty("limboReturnEnabled", Boolean.toString(limboReturnEnabled));
        p.setProperty("pestWebhookUrl", pestWebhookUrl == null ? "" : pestWebhookUrl.trim());
        saveFarmingBlockProfiles(p);
        p.setProperty("rotateToTarget", Boolean.toString(rotateToTarget));
        p.setProperty("scanRadius", Integer.toString(scanRadius));
        p.setProperty("rotationStepDegrees", Integer.toString(rotationStepDegrees));
        p.setProperty("actionDelayTicks", Integer.toString(actionDelayTicks));
        p.setProperty("targets", String.join(",", TARGET_BLOCKS));
        try (FileOutputStream out = new FileOutputStream(configFile())) { p.store(out, "Kuan Hypixel SkyBlock Assistant"); } catch (Throwable ignored) {}
    }

    private static void loadFarmingBlockProfiles(Properties p) {
        String legacyRowBlock = p.getProperty("farmingMarkers", "").split(",", 2)[0].trim();
        FARMING_BLOCK_PROFILE.rowDetection = Boolean.parseBoolean(sharedFarmingProperty(p, "rowDetection", p.getProperty("farmingTurnOnMarkers", "true")));
        FARMING_BLOCK_PROFILE.startDetection = Boolean.parseBoolean(sharedFarmingProperty(p, "startDetection", p.getProperty("farmingStartDetection", "true")));
        FARMING_BLOCK_PROFILE.endDetection = Boolean.parseBoolean(sharedFarmingProperty(p, "endDetection", p.getProperty("farmingEndDetection", "true")));
        FARMING_BLOCK_PROFILE.rowBlock = sharedFarmingBlock(p, "rowBlock", legacyRowBlock);
        FARMING_BLOCK_PROFILE.startBlock = sharedFarmingBlock(p, "startBlock", p.getProperty("farmingStartBlock", ""));
        FARMING_BLOCK_PROFILE.endBlock = sharedFarmingBlock(p, "endBlock", p.getProperty("farmingEndBlock", ""));
    }

    private static String sharedFarmingProperty(Properties p, String suffix, String fallback) {
        String shared = "farming.shared." + suffix;
        if (p.containsKey(shared)) return p.getProperty(shared, fallback);
        String selected = "farming." + farmingCrop.name() + "." + suffix;
        if (p.containsKey(selected)) return p.getProperty(selected, fallback);
        String mushroom = "farming.MUSHROOM." + suffix;
        if (p.containsKey(mushroom)) return p.getProperty(mushroom, fallback);
        return fallback;
    }

    private static String sharedFarmingBlock(Properties p, String suffix, String fallback) {
        String value = sharedFarmingProperty(p, suffix, fallback).trim();
        if (!value.isBlank()) return value;
        for (FarmingCrop crop : FarmingCrop.values()) {
            String old = p.getProperty("farming." + crop.name() + "." + suffix, "").trim();
            if (!old.isBlank()) return old;
        }
        return value;
    }

    private static void saveFarmingBlockProfiles(Properties p) {
        p.setProperty("farming.shared.rowDetection", Boolean.toString(FARMING_BLOCK_PROFILE.rowDetection));
        p.setProperty("farming.shared.startDetection", Boolean.toString(FARMING_BLOCK_PROFILE.startDetection));
        p.setProperty("farming.shared.endDetection", Boolean.toString(FARMING_BLOCK_PROFILE.endDetection));
        p.setProperty("farming.shared.rowBlock", FARMING_BLOCK_PROFILE.rowBlock);
        p.setProperty("farming.shared.startBlock", FARMING_BLOCK_PROFILE.startBlock);
        p.setProperty("farming.shared.endBlock", FARMING_BLOCK_PROFILE.endBlock);
    }

    private static int parseInt(String s, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(s))); } catch (Throwable ignored) { return fallback; }
    }

    private enum FarmingLocation {
        UNKNOWN("未知"), GARDEN("Garden"), HUB("Hub"), LOBBY("Lobby"), LIMBO("Limbo"), ISLAND("Island");
        final String label;
        FarmingLocation(String label) { this.label = label; }
    }

    private static final class QueuedCommand {
        final String command;
        int ticks;
        QueuedCommand(String command, int ticks) { this.command = command; this.ticks = ticks; }
    }

    private enum DirectionKey { FORWARD, BACK, LEFT, RIGHT }

    private enum PumpkinMelonState {
        INACTIVE,
        START_FORWARD_ONE,
        HARVEST_LEFT,
        LEFT_EXTRA_ONE,
        LEFT_FORWARD_TO_SECOND_MARKER,
        RIGHT_ONE,
        LEFT_FORWARD_ONE,
        HARVEST_RIGHT,
        RIGHT_EXTRA_ONE,
        RIGHT_FORWARD_TO_SECOND_MARKER,
        LEFT_ONE,
        RIGHT_FORWARD_ONE,
        RETURN_BACK_TO_START,
        RETURN_BACK_ONE,
        RETURN_LEFT_ONE,
        RETURN_FORWARD_ONE
    }

    private enum MushroomColor {
        UNKNOWN("未知"), RED("紅色"), BROWN("棕色");

        final String label;

        MushroomColor(String label) {
            this.label = label;
        }

        static MushroomColor fromBlockId(String id) {
            if ("minecraft:red_mushroom".equals(id) || "minecraft:red_mushroom_block".equals(id)) return RED;
            if ("minecraft:brown_mushroom".equals(id) || "minecraft:brown_mushroom_block".equals(id)) return BROWN;
            return UNKNOWN;
        }
    }

    private enum FarmingSelectMode { NONE, ROW, START, END }

    private static FarmingBlockProfile farmingProfile() {
        return FARMING_BLOCK_PROFILE;
    }

    private static final class FarmingBlockProfile {
        boolean rowDetection = true;
        boolean startDetection = true;
        boolean endDetection = true;
        String rowBlock = "";
        String startBlock = "";
        String endBlock = "";
    }

    private enum FarmingCrop {
        WHEAT("Wheat", -90.0f, 6.0f, DirectionKey.RIGHT, DirectionKey.LEFT, "minecraft:quartz_block"),
        POTATO("Potato", -90.0f, 6.0f, DirectionKey.RIGHT, DirectionKey.LEFT, "minecraft:quartz_block"),
        CARROT("Carrot", 90.0f, 6.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        NETHER_WART("Nether Wart", 90.0f, 6.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        CACTUS("Cactus", -90.0f, 0.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        SUGAR_CANE("Sugar Cane", 135.0f, 0.0f, DirectionKey.BACK, DirectionKey.RIGHT, "minecraft:quartz_block"),
        PUMPKIN_MELON("Pumpkim & Melon", 90.0f, -57.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        COCOA_BEAN("Cocoa Bean", -180.0f, -45.0f, DirectionKey.BACK, DirectionKey.FORWARD, "#dirt"),
        MUSHROOM("Mushroom", -65.0f, 0.0f, DirectionKey.FORWARD, DirectionKey.BACK, "minecraft:quartz_block");

        final String label;
        final float yaw;
        final float pitch;
        final DirectionKey forwardKey;
        final DirectionKey reverseKey;
        final String markerBlock;

        FarmingCrop(String label, float yaw, float pitch, DirectionKey forwardKey, DirectionKey reverseKey, String markerBlock) {
            this.label = label;
            this.yaw = yaw;
            this.pitch = pitch;
            this.forwardKey = forwardKey;
            this.reverseKey = reverseKey;
            this.markerBlock = markerBlock;
        }

        String findDefaultMarker(Minecraft mc, LocalPlayer player) {
            if (this == COCOA_BEAN) return firstNear(mc, player, 1, 0, 1, "minecraft:glowstone", markerBlock);
            if (this == SUGAR_CANE) return firstNear(mc, player, 1, 0, 0, markerBlock, "minecraft:sea_lantern");
            if (this == PUMPKIN_MELON || this == CACTUS || this == MUSHROOM) {
                return firstNear(mc, player, 1, 0, 1, markerBlock, "#ladder", "#trapdoor");
            }
            return firstNear(mc, player, 1, 0, 0, markerBlock, "#trapdoor");
        }

        static FarmingCrop fromBlockId(String id) {
            if (id == null) return null;
            return switch (id) {
                case "minecraft:wheat" -> WHEAT;
                case "minecraft:potatoes" -> POTATO;
                case "minecraft:carrots" -> CARROT;
                case "minecraft:nether_wart" -> NETHER_WART;
                case "minecraft:cactus" -> CACTUS;
                case "minecraft:sugar_cane" -> SUGAR_CANE;
                case "minecraft:pumpkin", "minecraft:melon" -> PUMPKIN_MELON;
                case "minecraft:cocoa" -> COCOA_BEAN;
                case "minecraft:red_mushroom", "minecraft:red_mushroom_block",
                     "minecraft:brown_mushroom", "minecraft:brown_mushroom_block" -> MUSHROOM;
                default -> null;
            };
        }

        static FarmingCrop fromConfig(String value) {
            if (value == null) return WHEAT;
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            if ("NETHERWART".equals(normalized)) normalized = "NETHER_WART";
            if ("COCOA".equals(normalized) || "COCOA_BEANS".equals(normalized)) normalized = "COCOA_BEAN";
            if ("MELON".equals(normalized) || "PUMPKIN".equals(normalized) || "PUMPKIN_AND_MELON".equals(normalized) || "PUMPKIM_&_MELON".equals(normalized)) normalized = "PUMPKIN_MELON";
            if ("RED_MUSH".equals(normalized) || "RED_MUSHROOM".equals(normalized) || "BROWN_MUSH".equals(normalized) || "BROWN_MUSHROOM".equals(normalized)) normalized = "MUSHROOM";
            for (FarmingCrop crop : values()) if (crop.name().equals(normalized) || crop.label.toUpperCase(Locale.ROOT).replace(' ', '_').equals(normalized)) return crop;
            return WHEAT;
        }
    }

    private static String firstNear(Minecraft mc, LocalPlayer player, int radius, int yMin, int yMax, String... expectedBlocks) {
        for (String expected : expectedBlocks) if (isBlockNear(mc, player, expected, radius, yMin, yMax)) return expected;
        return null;
    }

    private record Candidate(BlockPos pos, int priority, double dist) {
        boolean betterThan(Candidate other) {
            if (dist != other.dist) return dist < other.dist;
            return priority < other.priority;
        }
    }

    private static final class SettingsScreen extends Screen {
        private final Screen parent;
        SettingsScreen(Screen parent) {
            super(Component.literal("Kuan Hypixel SkyBlock Assistant"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int x = (this.width - 420) / 2;
            int y = 28;

            addSectionTitle("Farming", y);
            y += 22;
            addRenderableWidget(Button.builder(toggleMsg("Farming", farmingEnabled), b -> { toggleFarming(Minecraft.getInstance()); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("工具偵測", farmingToolDetectionEnabled), b -> { farmingToolDetectionEnabled = !farmingToolDetectionEnabled; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("害蟲通知", pestNotificationEnabled), b -> { pestNotificationEnabled = !pestNotificationEnabled; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(Button.builder(toggleMsg("Island 回傳", islandReturnEnabled), b -> { islandReturnEnabled = !islandReturnEnabled; saveConfig(); rebuild(); }).bounds(x, y, 99, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("Hub 回傳", hubReturnEnabled), b -> { hubReturnEnabled = !hubReturnEnabled; saveConfig(); rebuild(); }).bounds(x + 107, y, 99, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("Lobby 回傳", lobbyReturnEnabled), b -> { lobbyReturnEnabled = !lobbyReturnEnabled; saveConfig(); rebuild(); }).bounds(x + 214, y, 99, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("Limbo 回傳", limboReturnEnabled), b -> { limboReturnEnabled = !limboReturnEnabled; saveConfig(); rebuild(); }).bounds(x + 321, y, 99, 20).build());
            y += 28;
            FarmingBlockProfile blockProfile = farmingProfile();
            addRenderableWidget(Button.builder(toggleMsg("換行偵測", blockProfile.rowDetection), b -> { blockProfile.rowDetection = !blockProfile.rowDetection; saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("開始偵測", blockProfile.startDetection), b -> { blockProfile.startDetection = !blockProfile.startDetection; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("結束偵測", blockProfile.endDetection), b -> { blockProfile.endDetection = !blockProfile.endDetection; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("換行方塊"), b -> beginFarmingBlockPick(FarmingSelectMode.ROW)).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("開始方塊"), b -> beginFarmingBlockPick(FarmingSelectMode.START)).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("結束方塊"), b -> beginFarmingBlockPick(FarmingSelectMode.END)).bounds(x + 290, y, 130, 20).build());
            y += 22;
            addRenderableWidget(new StringWidget(x, y, 130, 12, Component.literal(shortBlockName(farmingMarkerSummary())), this.font));
            addRenderableWidget(new StringWidget(x + 145, y, 130, 12, Component.literal(shortBlockName(blockProfile.startBlock)), this.font));
            addRenderableWidget(new StringWidget(x + 290, y, 130, 12, Component.literal(shortBlockName(blockProfile.endBlock)), this.font));

            y += 28;
            addSectionTitle("Mining", y);
            y += 22;
            addRenderableWidget(Button.builder(toggleMsg("Mining", miningEnabled), b -> { toggleMining(Minecraft.getInstance()); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("選擇方塊"), b -> beginMiningBlockPick()).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("自動轉向", rotateToTarget), b -> { rotateToTarget = !rotateToTarget; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("偵測半徑 -"), b -> { scanRadius = Math.max(1, scanRadius - 1); saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("偵測半徑 +"), b -> { scanRadius = Math.min(8, scanRadius + 1); saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(new StringWidget(x + 290, y + 5, 130, 12, Component.literal("偵測半徑: " + scanRadius), this.font));
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("轉動速度 -"), b -> { rotationStepDegrees = Math.max(1, rotationStepDegrees - 1); saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("轉動速度 +"), b -> { rotationStepDegrees = Math.min(40, rotationStepDegrees + 1); saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(new StringWidget(x + 290, y + 5, 130, 12, Component.literal("轉動速度: " + rotationStepDegrees), this.font));
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("挖礦延遲 -"), b -> { actionDelayTicks = Math.max(1, actionDelayTicks - 1); saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("挖礦延遲 +"), b -> { actionDelayTicks = Math.min(20, actionDelayTicks + 1); saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(new StringWidget(x + 290, y + 5, 130, 12, Component.literal("挖礦延遲: " + actionDelayTicks), this.font));
            y += 22;
            addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("目標方塊清單（同距離時上面優先）：右鍵選取會加入或移到最新位置"), this.font));
            y += 18;
            int maxRows = Math.min(8, TARGET_BLOCKS.size());
            for (int i = 0; i < maxRows; i++) {
                final int idx = i;
                String name = TARGET_BLOCKS.get(i);
                addRenderableWidget(new StringWidget(x, y + 5, 230, 12, Component.literal((i + 1) + ". " + name), this.font));
                addRenderableWidget(Button.builder(Component.literal("↑"), b -> { move(idx, -1); rebuild(); }).bounds(x + 240, y, 36, 20).build());
                addRenderableWidget(Button.builder(Component.literal("↓"), b -> { move(idx, 1); rebuild(); }).bounds(x + 280, y, 36, 20).build());
                addRenderableWidget(Button.builder(Component.literal("刪"), b -> { TARGET_BLOCKS.remove(idx); saveConfig(); rebuild(); }).bounds(x + 320, y, 45, 20).build());
                y += 24;
            }
            if (TARGET_BLOCKS.size() > maxRows) {
                addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("... 還有 " + (TARGET_BLOCKS.size() - maxRows) + " 個"), this.font));
            }

            int webhookY = this.height - 34;
            addRenderableWidget(new StringWidget(20, webhookY + 5, 110, 12, Component.literal("Discord Webhook:"), this.font));
            EditBox webhookBox = new EditBox(this.font, 130, webhookY, 300, 20, Component.literal("Discord Webhook"));
            webhookBox.setMaxLength(300);
            webhookBox.setValue(pestWebhookUrl == null ? "" : pestWebhookUrl);
            webhookBox.setResponder(value -> { pestWebhookUrl = value.trim(); saveConfig(); });
            addRenderableWidget(webhookBox);
            addRenderableWidget(Button.builder(Component.literal("完成"), b -> Minecraft.getInstance().setScreenAndShow(parent)).bounds(x + 145, webhookY, 130, 20).build());
        }

        private void addSectionTitle(String label, int y) {
            Component title = Component.literal("──────── " + label + " ────────");
            int titleWidth = this.font.width(title);
            addRenderableWidget(new StringWidget((this.width - titleWidth) / 2, y, titleWidth, 12, title, this.font));
        }

        private Component toggleMsg(String label, boolean on) {
            return Component.literal(label + "：")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(on ? "開啟" : "關閉").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        private String farmingMarkerSummary() {
            if (farmingProfile().rowBlock.isBlank()) return "預設";
            return farmingProfile().rowBlock;
        }

        private String shortBlockName(String id) {
            if (id == null || id.isBlank()) return "未設定";
            String s = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
            return s.length() > 18 ? s.substring(0, 18) : s;
        }

        private void beginMiningBlockPick() {
            selectMode = true;
            farmingSelectMode = FarmingSelectMode.NONE;
            saveConfig();
            Minecraft.getInstance().setScreenAndShow(parent);
            say(Minecraft.getInstance(), feature("Mining"), plain(" 方塊選擇：請右鍵選擇目標方塊"));
        }

        private void beginFarmingBlockPick(FarmingSelectMode mode) {
            farmingSelectMode = mode;
            selectMode = false;
            saveConfig();
            Minecraft.getInstance().setScreenAndShow(parent);
            Component target = switch (mode) {
                case ROW -> blockName("換行方塊");
                case START -> blockName("開始方塊");
                case END -> blockName("結束方塊");
                default -> plain("");
            };
            say(Minecraft.getInstance(), feature("Farming"), plain(" 方塊選擇：請右鍵選擇"), target);
        }

        private void move(int idx, int delta) {
            int j = idx + delta;
            if (idx < 0 || idx >= TARGET_BLOCKS.size() || j < 0 || j >= TARGET_BLOCKS.size()) return;
            String s = TARGET_BLOCKS.remove(idx);
            TARGET_BLOCKS.add(j, s);
            saveConfig();
        }

        private void rebuild() {
            Minecraft.getInstance().setScreenAndShow(new SettingsScreen(parent));
        }

        @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
        @Override public boolean isPauseScreen() { return false; }
    }
}
