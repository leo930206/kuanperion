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
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
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
    private static final double FARMING_ONE_BLOCK_DISTANCE = 1.0;
    private static final int PUMPKIN_MELON_SECOND_MARKER_MIN_TICKS = 10;
    private static final int FARMING_BLOCKED_STEP_TICKS = 10;
    private static final String SUNFLOWER_TOOL_NAME = "Eclipse Sickle";
    private static final String WILD_ROSE_TOOL_NAME = "Wild Rose Cutter";
    private static boolean miningEnabled = false;
    private static boolean selectMode = false;
    private static boolean farmingEnabled = false;
    private static boolean farmingEnabledThisSession = false;
    private static FarmingSelectMode farmingSelectMode = FarmingSelectMode.NONE;
    private static FarmingCrop farmingCrop = FarmingCrop.WHEAT;
    private static boolean farmingAutoSetHome = false;
    private static boolean pestNotificationEnabled = false;
    private static boolean visitorNotificationEnabled = false;
    private static boolean placeholderNotificationEnabled = false;
    private static boolean farmingToolDetectionEnabled = false;
    private static boolean abnormalHandlingEnabled = true;
    private static String pestWebhookUrl = "";
    private static boolean farmingReverseLane = false;
    private static int farmingStateTicks = 0;
    private static int farmingLeftShiftTicks = 0;
    private static int farmingRightShiftTicks = 0;
    private static boolean farmingReturningToStart = false;
    private static boolean farmingAutoRecovering = false;
    private static boolean farmingWasInGarden = false;
    private static int farmingRecoveryScanTicks = 0;
    private static int limboChatSeenTick = -100000;
    private static final Deque<QueuedCommand> FARMING_COMMAND_QUEUE = new ArrayDeque<>();
    private static int farmingMarkerIgnoreTicks = 0;
    private static boolean farmingCropDetected = false;
    private static int farmingCropProbeTicks = 0;
    private static PumpkinMelonState pumpkinMelonState = PumpkinMelonState.INACTIVE;
    private static double pumpkinMelonStepStartX = 0.0;
    private static double pumpkinMelonStepStartZ = 0.0;
    private static double pumpkinMelonStepLastX = 0.0;
    private static double pumpkinMelonStepLastZ = 0.0;
    private static int pumpkinMelonStepStalledTicks = 0;
    private static int pumpkinMelonStepTicks = 0;
    private static double pumpkinMelonStepTargetDistance = FARMING_ONE_BLOCK_DISTANCE;
    private static boolean pumpkinMelonMarkerCleared = false;
    private static RowCropState rowCropState = RowCropState.INACTIVE;
    private static boolean rowCropMarkerCleared = false;
    private static double rowCropStepStartX = 0.0;
    private static double rowCropStepStartZ = 0.0;
    private static double rowCropStepLastX = 0.0;
    private static double rowCropStepLastZ = 0.0;
    private static int rowCropStepStalledTicks = 0;
    private static int rowCropWaitTicks = 0;
    private static SugarCaneState sugarCaneState = SugarCaneState.INACTIVE;
    private static boolean sugarCaneMarkerCleared = false;
    private static double sugarCaneStepStartX = 0.0;
    private static double sugarCaneStepStartZ = 0.0;
    private static double sugarCaneStepLastX = 0.0;
    private static double sugarCaneStepLastZ = 0.0;
    private static double sugarCaneStepDirectionX = 0.0;
    private static double sugarCaneStepDirectionZ = 0.0;
    private static double sugarCaneStepTargetDistance = 0.0;
    private static int sugarCaneStepStalledTicks = 0;
    private static CocoaState cocoaState = CocoaState.INACTIVE;
    private static boolean cocoaMarkerCleared = false;
    private static double cocoaStepStartX = 0.0;
    private static double cocoaStepStartZ = 0.0;
    private static double cocoaStepLastX = 0.0;
    private static double cocoaStepLastZ = 0.0;
    private static double cocoaStepDirectionX = 0.0;
    private static double cocoaStepDirectionZ = 0.0;
    private static double cocoaStepTargetDistance = 0.0;
    private static int cocoaStepStalledTicks = 0;
    private static CactusState cactusState = CactusState.INACTIVE;
    private static boolean cactusMarkerCleared = false;
    private static double cactusStepStartX = 0.0;
    private static double cactusStepStartZ = 0.0;
    private static double cactusStepLastX = 0.0;
    private static double cactusStepLastZ = 0.0;
    private static double cactusStepDirectionX = 0.0;
    private static double cactusStepDirectionZ = 0.0;
    private static double cactusStepTargetDistance = 0.0;
    private static int cactusStepStalledTicks = 0;
    private static int cactusWaitTicks = 0;
    private static FlowerState flowerState = FlowerState.INACTIVE;
    private static boolean flowerMarkerCleared = false;
    private static int flowerTransitionTicks = 0;
    private static double flowerStepStartX = 0.0;
    private static double flowerStepStartZ = 0.0;
    private static double flowerStepLastX = 0.0;
    private static double flowerStepLastZ = 0.0;
    private static double flowerStepDirectionX = 0.0;
    private static double flowerStepDirectionZ = 0.0;
    private static int flowerStepStalledTicks = 0;
    private static boolean rotateToTarget = true;
    private static int scanRadius = 5;
    private static final float ROTATION_DONE_EPSILON = 1.5f;
    private static int rotationStepDegrees = 13;
    private static int actionDelayTicks = 3;
    private static int miningSelectionTargetCount = 1;
    private static int miningTicks = 0;
    private static int cooldown = 0;
    private static boolean rightWasDown = false;
    private static boolean miningComboWasDown = false;
    private static boolean farmingComboWasDown = false;
    private static String lastPestMessage = "";
    private static int lastPestMessageTick = 0;
    private static BlockPos activeTarget = null;
    private static String activeTargetBlockId = null;
    private static boolean activeTargetStarted = false;
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
            clearActiveTarget(mc);
            saveConfig();
            sayAutoState(mc, "Mining", false);
            return;
        }
        if (TARGET_BLOCKS.isEmpty()) {
            say(mc, feature("Mining"), plain(" "), error("無法開啟"), plain("：尚未設定目標方塊"));
            return;
        }
        stopFarming(mc, false);
        miningEnabled = true;
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
            say(mc, feature("Farming"), plain(" "), success("開啟"), plain("，正在辨識準星指向的作物"));
        }
        saveConfig();
    }

    private static void stopFarming(Minecraft mc, boolean announce) {
        farmingEnabled = false;
        farmingEnabledThisSession = false;
        farmingAutoRecovering = false;
        farmingWasInGarden = false;
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
        farmingMarkerIgnoreTicks = 0;
        farmingCropDetected = false;
        farmingCropProbeTicks = 0;
        pumpkinMelonState = PumpkinMelonState.INACTIVE;
        pumpkinMelonStepTicks = 0;
        pumpkinMelonMarkerCleared = false;
        rowCropState = RowCropState.INACTIVE;
        rowCropMarkerCleared = false;
        rowCropWaitTicks = 0;
        sugarCaneState = SugarCaneState.INACTIVE;
        sugarCaneMarkerCleared = false;
        cocoaState = CocoaState.INACTIVE;
        cocoaMarkerCleared = false;
        cactusState = CactusState.INACTIVE;
        cactusMarkerCleared = false;
        cactusWaitTicks = 0;
        flowerState = FlowerState.INACTIVE;
        flowerMarkerCleared = false;
        flowerTransitionTicks = 0;
        farmingAutoRecovering = false;
        farmingWasInGarden = false;
        farmingRecoveryScanTicks = 0;
        FARMING_COMMAND_QUEUE.clear();
        farmingWasInGarden = detectFarmingLocation(mc) == FarmingLocation.GARDEN;
        if (mc.player != null) {
            pumpkinMelonStepStartX = mc.player.getX();
            pumpkinMelonStepStartZ = mc.player.getZ();
            pumpkinMelonStepLastX = mc.player.getX();
            pumpkinMelonStepLastZ = mc.player.getZ();
            pumpkinMelonStepStalledTicks = 0;
            pumpkinMelonStepTargetDistance = FARMING_ONE_BLOCK_DISTANCE;
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
        if (isAlternatingRowCrop(farmingCrop)) {
            tickAlternatingRowCrop(mc, player, blockProfile);
            return;
        }
        if (farmingCrop == FarmingCrop.SUGAR_CANE) {
            tickSugarCane(mc, player, blockProfile);
            return;
        }
        if (farmingCrop == FarmingCrop.COCOA_BEAN) {
            tickCocoa(mc, player, blockProfile);
            return;
        }
        if (farmingCrop == FarmingCrop.CACTUS) {
            tickCactus(mc, player, blockProfile);
            return;
        }
        if (isFlowerCrop(farmingCrop)) {
            tickFlower(mc, player, blockProfile);
            return;
        }
        if (farmingReturningToStart) {
            setAttackKey(mc, false);
            if (blockProfile.startDetection && isConfiguredBlockNear(mc, player, blockProfile.startBlock)) {
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
            farmingMarkerIgnoreTicks = 30;
            if (farmingCrop == FarmingCrop.MUSHROOM) {
                farmingLeftShiftTicks = MUSHROOM_LEFT_SHIFT_TICKS;
            } else {
                farmingStateTicks = FARM_TURN_STOP_TICKS + FARM_TURN_RESUME_TICKS;
            }
            releaseFarmingKeys(mc);
            return;
        }
    }

    private static boolean handleFarmingAutoRecovery(Minecraft mc) {
        if (!farmingEnabledThisSession || (!abnormalHandlingEnabled && !farmingAutoRecovering)) return false;
        FarmingLocation location = detectFarmingLocation(mc);
        if (farmingAutoRecovering) {
            releaseFarmingKeys(mc);
            if (location == FarmingLocation.GARDEN && FARMING_COMMAND_QUEUE.isEmpty()) {
                farmingAutoRecovering = false;
                farmingEnabled = false;
                farmingEnabledThisSession = false;
                farmingWasInGarden = false;
                say(mc, feature("Farming"), success(" 已返回 "), locationName("Garden"), plain("，Farming 維持關閉"));
                saveConfig();
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
        farmingEnabled = false;
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
        return abnormalHandlingEnabled && (location == FarmingLocation.ISLAND
                || location == FarmingLocation.HUB
                || location == FarmingLocation.LOBBY
                || location == FarmingLocation.LIMBO);
    }

    private static void tickFarmingCommandQueue(Minecraft mc) {
        if (FARMING_COMMAND_QUEUE.isEmpty()) return;
        QueuedCommand current = FARMING_COMMAND_QUEUE.peekFirst();
        current.ticks--;
        if (current.ticks > 0) return;
        FARMING_COMMAND_QUEUE.removeFirst();
        sendCommand(mc, current.command);
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
        if (containsLine(lines, "The Garden") || containsLine(lines, "Garden") || containsLine(lines, "Plot")) return FarmingLocation.GARDEN;
        if (containsLine(lines, "Private Island") && containsLine(lines, "Your Island")) return FarmingLocation.ISLAND;
        if (containsLine(lines, "Hub")) return FarmingLocation.HUB;
        if (isHoldingGameMenu(mc)) return FarmingLocation.LOBBY;
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
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_LEFT;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case HARVEST_LEFT -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonLaneMarkerDetected(mc, player, profile)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_EXTRA_ONE);
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case LEFT_EXTRA_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_FORWARD_TO_SECOND_MARKER);
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case LEFT_FORWARD_TO_SECOND_MARKER -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                pumpkinMelonStepTicks++;
                if (pumpkinMelonStepTicks >= PUMPKIN_MELON_SECOND_MARKER_MIN_TICKS && findFarmingMarkerBlock(mc, player) != null) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_ONE);
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_FORWARD_ONE);
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case LEFT_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_RIGHT;
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case HARVEST_RIGHT -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (profile.endDetection && isConfiguredBlockNear(mc, player, profile.endBlock)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.END_RIGHT_ONE);
                    applyFarmingInput(mc, DirectionKey.RIGHT, false, false);
                } else if (pumpkinMelonLaneMarkerDetected(mc, player, profile)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_EXTRA_ONE);
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_EXTRA_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_FORWARD_TO_SECOND_MARKER);
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case RIGHT_FORWARD_TO_SECOND_MARKER -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                pumpkinMelonStepTicks++;
                if (pumpkinMelonStepTicks >= PUMPKIN_MELON_SECOND_MARKER_MIN_TICKS && findFarmingMarkerBlock(mc, player) != null) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.LEFT_ONE);
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case LEFT_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RIGHT_FORWARD_ONE);
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case RIGHT_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_LEFT;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case END_RIGHT_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, false, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.RETURN_BACK_TO_START;
                    applyFarmingInput(mc, DirectionKey.BACK, false, false);
                }
            }
            case RETURN_BACK_TO_START -> {
                applyFarmingInput(mc, DirectionKey.BACK, false, false);
                if (profile.startDetection && isConfiguredBlockNear(mc, player, profile.startBlock)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RETURN_BACK_TWO, 2.0);
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                }
            }
            case RETURN_BACK_TWO -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RETURN_LEFT_ONE);
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case RETURN_LEFT_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (pumpkinMelonStepDone(player)) {
                    beginPumpkinMelonStep(player, PumpkinMelonState.RETURN_FORWARD_ONE);
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case RETURN_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = PumpkinMelonState.HARVEST_LEFT;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
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
        beginPumpkinMelonStep(player, state, FARMING_ONE_BLOCK_DISTANCE);
    }

    private static void beginPumpkinMelonStep(LocalPlayer player, PumpkinMelonState state, double targetDistance) {
        pumpkinMelonState = state;
        pumpkinMelonStepStartX = player.getX();
        pumpkinMelonStepStartZ = player.getZ();
        pumpkinMelonStepLastX = player.getX();
        pumpkinMelonStepLastZ = player.getZ();
        pumpkinMelonStepStalledTicks = 0;
        pumpkinMelonStepTicks = 0;
        pumpkinMelonStepTargetDistance = targetDistance;
    }

    private static boolean pumpkinMelonStepDone(LocalPlayer player) {
        pumpkinMelonStepTicks++;
        double dx = player.getX() - pumpkinMelonStepStartX;
        double dz = player.getZ() - pumpkinMelonStepStartZ;
        double tickDx = player.getX() - pumpkinMelonStepLastX;
        double tickDz = player.getZ() - pumpkinMelonStepLastZ;
        pumpkinMelonStepLastX = player.getX();
        pumpkinMelonStepLastZ = player.getZ();
        if (tickDx * tickDx + tickDz * tickDz < 0.0001) pumpkinMelonStepStalledTicks++;
        else pumpkinMelonStepStalledTicks = 0;
        return dx * dx + dz * dz >= pumpkinMelonStepTargetDistance * pumpkinMelonStepTargetDistance
                || pumpkinMelonStepStalledTicks >= FARMING_BLOCKED_STEP_TICKS;
    }

    private static boolean isAlternatingRowCrop(FarmingCrop crop) {
        return crop == FarmingCrop.NETHER_WART || crop == FarmingCrop.CARROT
                || crop == FarmingCrop.WHEAT || crop == FarmingCrop.POTATO;
    }

    private static DirectionKey rowCropPrimaryDirection() {
        return farmingCrop == FarmingCrop.NETHER_WART || farmingCrop == FarmingCrop.CARROT
                ? DirectionKey.RIGHT : DirectionKey.LEFT;
    }

    private static DirectionKey rowCropSecondaryDirection() {
        return rowCropPrimaryDirection() == DirectionKey.RIGHT ? DirectionKey.LEFT : DirectionKey.RIGHT;
    }

    private static void tickAlternatingRowCrop(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        DirectionKey primary = rowCropPrimaryDirection();
        DirectionKey secondary = rowCropSecondaryDirection();
        if (rowCropState == RowCropState.INACTIVE) {
            beginRowCropStep(player, RowCropState.START_FORWARD_ONE);
        }

        switch (rowCropState) {
            case START_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, false, false);
                if (rowCropStepDone(player, 1.0, 0.0)) {
                    rowCropState = RowCropState.HARVEST_PRIMARY;
                    rowCropMarkerCleared = false;
                    applyFarmingInput(mc, primary, true, false);
                }
            }
            case HARVEST_PRIMARY -> {
                applyFarmingInput(mc, primary, true, false);
                if (rowCropMarkerDetected(mc, player, profile)) {
                    beginRowCropStep(player, RowCropState.PRIMARY_EXTRA_ONE);
                    applyFarmingInput(mc, primary, true, false);
                }
            }
            case PRIMARY_EXTRA_ONE -> {
                applyFarmingInput(mc, primary, true, false);
                if (rowCropStepDone(player, 0.0, 1.0)) {
                    rowCropState = RowCropState.PRIMARY_JUMP_THREE_SECONDS;
                    rowCropWaitTicks = 60;
                    applyFarmingInput(mc, false, false, false, false, true, true);
                }
            }
            case PRIMARY_JUMP_THREE_SECONDS -> {
                applyFarmingInput(mc, false, false, false, false, true, true);
                if (--rowCropWaitTicks <= 0) {
                    rowCropState = RowCropState.PRIMARY_JUMP_LATERAL_ONE_SECOND;
                    rowCropWaitTicks = 20;
                    applyFarmingJumpLateralInput(mc, secondary, true);
                }
            }
            case PRIMARY_JUMP_LATERAL_ONE_SECOND -> {
                applyFarmingJumpLateralInput(mc, secondary, true);
                if (--rowCropWaitTicks <= 0) {
                    rowCropState = RowCropState.HARVEST_SECONDARY;
                    rowCropMarkerCleared = false;
                    applyFarmingInput(mc, secondary, true, false);
                }
            }
            case HARVEST_SECONDARY -> {
                applyFarmingInput(mc, secondary, true, false);
                if (profile.endDetection && isConfiguredBlockNear(mc, player, profile.endBlock)) {
                    beginRowCropStep(player, RowCropState.END_EXTRA_ONE);
                    applyFarmingInput(mc, secondary, false, false);
                } else if (rowCropMarkerDetected(mc, player, profile)) {
                    beginRowCropStep(player, RowCropState.SECONDARY_EXTRA_ONE);
                    applyFarmingInput(mc, secondary, true, false);
                }
            }
            case SECONDARY_EXTRA_ONE -> {
                applyFarmingInput(mc, secondary, true, false);
                if (rowCropStepDone(player, 0.0, 1.0)) {
                    rowCropState = RowCropState.SECONDARY_JUMP_THREE_SECONDS;
                    rowCropWaitTicks = 60;
                    applyFarmingInput(mc, false, false, false, false, true, true);
                }
            }
            case SECONDARY_JUMP_THREE_SECONDS -> {
                applyFarmingInput(mc, false, false, false, false, true, true);
                if (--rowCropWaitTicks <= 0) {
                    rowCropState = RowCropState.SECONDARY_JUMP_LATERAL_ONE_SECOND;
                    rowCropWaitTicks = 20;
                    applyFarmingJumpLateralInput(mc, primary, true);
                }
            }
            case SECONDARY_JUMP_LATERAL_ONE_SECOND -> {
                applyFarmingJumpLateralInput(mc, primary, true);
                if (--rowCropWaitTicks <= 0) {
                    rowCropState = RowCropState.HARVEST_PRIMARY;
                    rowCropMarkerCleared = false;
                    applyFarmingInput(mc, primary, true, false);
                }
            }
            case END_EXTRA_ONE -> {
                applyFarmingInput(mc, secondary, false, false);
                if (rowCropStepDone(player, 0.0, 1.0)) {
                    rowCropState = RowCropState.RETURN_TO_START;
                    applyFarmingInput(mc, secondary, false, false);
                }
            }
            case RETURN_TO_START -> {
                applyFarmingInput(mc, secondary, false, false);
                if (profile.startDetection && isConfiguredBlockNear(mc, player, profile.startBlock)) {
                    rowCropState = RowCropState.WAIT_ONE_SECOND;
                    rowCropWaitTicks = 20;
                    releaseFarmingKeys(mc);
                }
            }
            case WAIT_ONE_SECOND -> {
                releaseFarmingKeys(mc);
                if (--rowCropWaitTicks <= 0) {
                    rowCropState = RowCropState.HARVEST_PRIMARY;
                    rowCropMarkerCleared = false;
                    applyFarmingInput(mc, primary, true, false);
                }
            }
            case INACTIVE -> { }
        }
    }

    private static boolean rowCropMarkerDetected(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (!profile.rowDetection) return false;
        boolean markerPresent = findFarmingMarkerBlock(mc, player) != null;
        if (!rowCropMarkerCleared) {
            if (!markerPresent) rowCropMarkerCleared = true;
            return false;
        }
        if (markerPresent) {
            rowCropMarkerCleared = false;
            return true;
        }
        return false;
    }

    private static void beginRowCropStep(LocalPlayer player, RowCropState state) {
        rowCropState = state;
        rowCropStepStartX = player.getX();
        rowCropStepStartZ = player.getZ();
        rowCropStepLastX = player.getX();
        rowCropStepLastZ = player.getZ();
        rowCropStepStalledTicks = 0;
    }

    private static boolean rowCropStepDone(LocalPlayer player, double requiredX, double requiredZ) {
        double dx = Math.abs(player.getX() - rowCropStepStartX);
        double dz = Math.abs(player.getZ() - rowCropStepStartZ);
        double tickDx = player.getX() - rowCropStepLastX;
        double tickDz = player.getZ() - rowCropStepLastZ;
        rowCropStepLastX = player.getX();
        rowCropStepLastZ = player.getZ();
        if (tickDx * tickDx + tickDz * tickDz < 0.0001) rowCropStepStalledTicks++;
        else rowCropStepStalledTicks = 0;
        boolean distanceDone = (requiredX <= 0.0 || dx >= requiredX)
                && (requiredZ <= 0.0 || dz >= requiredZ);
        return distanceDone || rowCropStepStalledTicks >= FARMING_BLOCKED_STEP_TICKS;
    }

    private static void tickSugarCane(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (sugarCaneState == SugarCaneState.INACTIVE) {
            sugarCaneState = SugarCaneState.HARVEST_LEFT;
            sugarCaneMarkerCleared = false;
        }

        switch (sugarCaneState) {
            case HARVEST_LEFT -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (sugarCaneMarkerDetected(mc, player, profile)) {
                    beginSugarCaneStep(player, SugarCaneState.FORWARD_FIVE, DirectionKey.FORWARD, 5.0);
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case FORWARD_FIVE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (sugarCaneStepDone(player)) {
                    beginSugarCaneStep(player, SugarCaneState.RIGHT_ONE, DirectionKey.RIGHT, 1.0);
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (sugarCaneStepDone(player)) {
                    sugarCaneState = SugarCaneState.HARVEST_BACK;
                    sugarCaneMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                }
            }
            case HARVEST_BACK -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (profile.endDetection && isConfiguredBlockNear(mc, player, profile.endBlock)) {
                    sugarCaneState = SugarCaneState.RETURN_TO_START;
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                } else if (sugarCaneMarkerDetected(mc, player, profile)) {
                    beginSugarCaneStep(player, SugarCaneState.RIGHT_SEVEN, DirectionKey.RIGHT, 7.0);
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_SEVEN -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (sugarCaneStepDone(player)) {
                    sugarCaneState = SugarCaneState.HARVEST_LEFT;
                    sugarCaneMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case RETURN_TO_START -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (profile.startDetection && isConfiguredBlockNear(mc, player, profile.startBlock)) {
                    beginSugarCaneStep(player, SugarCaneState.RETURN_BACK_TWO, DirectionKey.BACK, 2.0);
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                }
            }
            case RETURN_BACK_TWO -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (sugarCaneStepDone(player)) {
                    sugarCaneState = SugarCaneState.HARVEST_LEFT;
                    sugarCaneMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case INACTIVE -> { }
        }
    }

    private static boolean sugarCaneMarkerDetected(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (!profile.rowDetection) return false;
        boolean markerPresent = findFarmingMarkerBlock(mc, player) != null;
        if (!sugarCaneMarkerCleared) {
            if (!markerPresent) sugarCaneMarkerCleared = true;
            return false;
        }
        if (markerPresent) {
            sugarCaneMarkerCleared = false;
            return true;
        }
        return false;
    }

    private static void beginSugarCaneStep(LocalPlayer player, SugarCaneState state, DirectionKey direction,
                                           double targetDistance) {
        sugarCaneState = state;
        sugarCaneStepStartX = player.getX();
        sugarCaneStepStartZ = player.getZ();
        sugarCaneStepLastX = player.getX();
        sugarCaneStepLastZ = player.getZ();
        sugarCaneStepStalledTicks = 0;
        sugarCaneStepTargetDistance = targetDistance;
        Vec3 directionVector = farmingMovementVector(direction, farmingCrop.yaw);
        sugarCaneStepDirectionX = directionVector.x;
        sugarCaneStepDirectionZ = directionVector.z;
    }

    private static boolean sugarCaneStepDone(LocalPlayer player) {
        double dx = player.getX() - sugarCaneStepStartX;
        double dz = player.getZ() - sugarCaneStepStartZ;
        double progress = dx * sugarCaneStepDirectionX + dz * sugarCaneStepDirectionZ;
        double tickDx = player.getX() - sugarCaneStepLastX;
        double tickDz = player.getZ() - sugarCaneStepLastZ;
        sugarCaneStepLastX = player.getX();
        sugarCaneStepLastZ = player.getZ();
        if (tickDx * tickDx + tickDz * tickDz < 0.0001) sugarCaneStepStalledTicks++;
        else sugarCaneStepStalledTicks = 0;
        return progress >= sugarCaneStepTargetDistance
                || sugarCaneStepStalledTicks >= FARMING_BLOCKED_STEP_TICKS;
    }

    private static void tickCocoa(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (cocoaState == CocoaState.INACTIVE) {
            beginCocoaStep(player, CocoaState.START_LEFT_ONE, DirectionKey.LEFT, 1.0);
        }

        switch (cocoaState) {
            case START_LEFT_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                if (cocoaStepDone(player)) {
                    cocoaState = CocoaState.HARVEST_FORWARD;
                    cocoaMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case HARVEST_FORWARD -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (cocoaMarkerDetected(mc, player, profile)) {
                    beginCocoaStep(player, CocoaState.RIGHT_ONE, DirectionKey.RIGHT, 1.0);
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (cocoaStepDone(player)) {
                    cocoaState = CocoaState.HARVEST_BACK;
                    cocoaMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                }
            }
            case HARVEST_BACK -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (profile.endDetection && isConfiguredBlockNear(mc, player, profile.endBlock)) {
                    cocoaState = CocoaState.RETURN_LEFT_TO_START;
                    applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                } else if (cocoaMarkerDetected(mc, player, profile)) {
                    beginCocoaStep(player, CocoaState.RIGHT_TWO, DirectionKey.RIGHT, 2.0);
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RIGHT_TWO -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (cocoaStepDone(player)) {
                    beginCocoaStep(player, CocoaState.FORWARD_ONE, DirectionKey.FORWARD, 1.0);
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (cocoaStepDone(player)) {
                    beginCocoaStep(player, CocoaState.LEFT_ONE, DirectionKey.LEFT, 1.0);
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case LEFT_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (cocoaStepDone(player)) {
                    cocoaState = CocoaState.HARVEST_FORWARD;
                    cocoaMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case RETURN_LEFT_TO_START -> {
                applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                if (profile.startDetection && isConfiguredBlockNear(mc, player, profile.startBlock)) {
                    beginCocoaStep(player, CocoaState.START_LEFT_ONE, DirectionKey.LEFT, 1.0);
                    applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                }
            }
            case INACTIVE -> { }
        }
    }

    private static boolean cocoaMarkerDetected(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (!profile.rowDetection) return false;
        boolean markerPresent = findFarmingMarkerBlock(mc, player) != null;
        if (!cocoaMarkerCleared) {
            if (!markerPresent) cocoaMarkerCleared = true;
            return false;
        }
        if (markerPresent) {
            cocoaMarkerCleared = false;
            return true;
        }
        return false;
    }

    private static void beginCocoaStep(LocalPlayer player, CocoaState state, DirectionKey direction,
                                       double targetDistance) {
        cocoaState = state;
        cocoaStepStartX = player.getX();
        cocoaStepStartZ = player.getZ();
        cocoaStepLastX = player.getX();
        cocoaStepLastZ = player.getZ();
        cocoaStepStalledTicks = 0;
        cocoaStepTargetDistance = targetDistance;
        Vec3 directionVector = farmingMovementVector(direction, farmingCrop.yaw);
        cocoaStepDirectionX = directionVector.x;
        cocoaStepDirectionZ = directionVector.z;
    }

    private static boolean cocoaStepDone(LocalPlayer player) {
        double dx = player.getX() - cocoaStepStartX;
        double dz = player.getZ() - cocoaStepStartZ;
        double progress = dx * cocoaStepDirectionX + dz * cocoaStepDirectionZ;
        double tickDx = player.getX() - cocoaStepLastX;
        double tickDz = player.getZ() - cocoaStepLastZ;
        cocoaStepLastX = player.getX();
        cocoaStepLastZ = player.getZ();
        if (tickDx * tickDx + tickDz * tickDz < 0.0001) cocoaStepStalledTicks++;
        else cocoaStepStalledTicks = 0;
        return progress >= cocoaStepTargetDistance || cocoaStepStalledTicks >= FARMING_BLOCKED_STEP_TICKS;
    }

    private static Vec3 farmingMovementVector(DirectionKey direction, float yaw) {
        double yawRadians = Math.toRadians(yaw);
        return switch (direction) {
            case FORWARD -> new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
            case BACK -> new Vec3(Math.sin(yawRadians), 0.0, -Math.cos(yawRadians));
            case LEFT -> new Vec3(Math.cos(yawRadians), 0.0, Math.sin(yawRadians));
            case RIGHT -> new Vec3(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians));
        };
    }

    private static void tickCactus(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (cactusState == CactusState.INACTIVE) {
            beginCactusStep(player, CactusState.START_FORWARD_ONE, DirectionKey.FORWARD, 1.0);
        }

        switch (cactusState) {
            case START_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, false, false);
                if (cactusStepDone(player)) {
                    cactusState = CactusState.HARVEST_LEFT;
                    cactusMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case HARVEST_LEFT -> {
                applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                if (cactusMarkerDetected(mc, player, profile)) {
                    beginCactusStep(player, CactusState.LEFT_EXTRA_ONE, DirectionKey.LEFT, 1.0);
                    applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                }
            }
            case LEFT_EXTRA_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                if (cactusStepDone(player)) {
                    beginCactusStep(player, CactusState.LEFT_FORWARD_TWO, DirectionKey.FORWARD, 2.0);
                    applyFarmingInput(mc, DirectionKey.FORWARD, false, false);
                }
            }
            case LEFT_FORWARD_TWO -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, false, false);
                if (cactusStepDone(player)) {
                    cactusState = CactusState.WAIT_BEFORE_HARVEST_RIGHT;
                    cactusWaitTicks = 20;
                    releaseFarmingKeys(mc);
                }
            }
            case WAIT_BEFORE_HARVEST_RIGHT -> {
                releaseFarmingKeys(mc);
                if (--cactusWaitTicks <= 0) {
                    cactusState = CactusState.HARVEST_RIGHT;
                    cactusMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case HARVEST_RIGHT -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (profile.endDetection && isConfiguredBlockNear(mc, player, profile.endBlock)) {
                    cactusState = CactusState.RETURN_BACK_TO_START;
                    applyFarmingInput(mc, DirectionKey.BACK, false, false);
                } else if (cactusMarkerDetected(mc, player, profile)) {
                    beginCactusStep(player, CactusState.RIGHT_EXTRA_ONE, DirectionKey.RIGHT, 1.0);
                    applyFarmingInput(mc, DirectionKey.RIGHT, false, false);
                }
            }
            case RIGHT_EXTRA_ONE -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, false, false);
                if (cactusStepDone(player)) {
                    beginCactusStep(player, CactusState.RIGHT_FORWARD_TWO, DirectionKey.FORWARD, 2.0);
                    applyFarmingInput(mc, DirectionKey.FORWARD, false, false);
                }
            }
            case RIGHT_FORWARD_TWO -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, false, false);
                if (cactusStepDone(player)) {
                    cactusState = CactusState.WAIT_BEFORE_HARVEST_LEFT;
                    cactusWaitTicks = 20;
                    releaseFarmingKeys(mc);
                }
            }
            case WAIT_BEFORE_HARVEST_LEFT -> {
                releaseFarmingKeys(mc);
                if (--cactusWaitTicks <= 0) {
                    cactusState = CactusState.HARVEST_LEFT;
                    cactusMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case RETURN_BACK_TO_START -> {
                applyFarmingInput(mc, DirectionKey.BACK, false, false);
                if (profile.startDetection && isConfiguredBlockNear(mc, player, profile.startBlock)) {
                    beginCactusStep(player, CactusState.RETURN_LEFT_ONE, DirectionKey.LEFT, 1.0);
                    applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                }
            }
            case RETURN_LEFT_ONE -> {
                applyFarmingInput(mc, DirectionKey.LEFT, false, false);
                if (cactusStepDone(player)) {
                    beginCactusStep(player, CactusState.START_FORWARD_ONE, DirectionKey.FORWARD, 1.0);
                    applyFarmingInput(mc, DirectionKey.FORWARD, false, false);
                }
            }
            case INACTIVE -> { }
        }
    }

    private static boolean cactusMarkerDetected(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (!profile.rowDetection) return false;
        boolean markerPresent = findFarmingMarkerBlock(mc, player) != null;
        if (!cactusMarkerCleared) {
            if (!markerPresent) cactusMarkerCleared = true;
            return false;
        }
        if (markerPresent) {
            cactusMarkerCleared = false;
            return true;
        }
        return false;
    }

    private static void beginCactusStep(LocalPlayer player, CactusState state, DirectionKey direction,
                                        double targetDistance) {
        cactusState = state;
        cactusStepStartX = player.getX();
        cactusStepStartZ = player.getZ();
        cactusStepLastX = player.getX();
        cactusStepLastZ = player.getZ();
        cactusStepStalledTicks = 0;
        cactusStepTargetDistance = targetDistance;
        Vec3 directionVector = farmingMovementVector(direction, farmingCrop.yaw);
        cactusStepDirectionX = directionVector.x;
        cactusStepDirectionZ = directionVector.z;
    }

    private static boolean cactusStepDone(LocalPlayer player) {
        double dx = player.getX() - cactusStepStartX;
        double dz = player.getZ() - cactusStepStartZ;
        double progress = dx * cactusStepDirectionX + dz * cactusStepDirectionZ;
        double tickDx = player.getX() - cactusStepLastX;
        double tickDz = player.getZ() - cactusStepLastZ;
        cactusStepLastX = player.getX();
        cactusStepLastZ = player.getZ();
        if (tickDx * tickDx + tickDz * tickDz < 0.0001) cactusStepStalledTicks++;
        else cactusStepStalledTicks = 0;
        return progress >= cactusStepTargetDistance || cactusStepStalledTicks >= FARMING_BLOCKED_STEP_TICKS;
    }

    private static boolean isFlowerCrop(FarmingCrop crop) {
        return crop == FarmingCrop.SUNFLOWER || crop == FarmingCrop.WILD_ROSE;
    }

    private static void tickFlower(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (flowerState == FlowerState.INACTIVE) {
            beginFlowerStep(player, FlowerState.START_BACK_ONE, DirectionKey.BACK);
        }

        switch (flowerState) {
            case START_BACK_ONE -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (flowerStepDone(player)) {
                    flowerState = FlowerState.HARVEST_RIGHT;
                    flowerMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case HARVEST_RIGHT -> {
                applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                if (flowerMarkerDetected(mc, player, profile)) {
                    flowerState = FlowerState.FORWARD_TWO_SECONDS;
                    flowerTransitionTicks = 20;
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                }
            }
            case FORWARD_TWO_SECONDS -> {
                if (flowerTransitionTicks > 10) {
                    applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                } else {
                    applyFarmingInput(mc, true, false, true, false, true, false);
                }
                if (--flowerTransitionTicks <= 0) {
                    flowerState = FlowerState.HARVEST_BACK;
                    flowerMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                }
            }
            case HARVEST_BACK -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (profile.endDetection && isConfiguredBlockNear(mc, player, profile.endBlock)) {
                    flowerState = FlowerState.RETURN_BACK_TO_START;
                    applyFarmingInput(mc, DirectionKey.BACK, true, false);
                } else if (flowerMarkerDetected(mc, player, profile)) {
                    flowerState = FlowerState.LEFT_TWO_SECONDS;
                    flowerTransitionTicks = 20;
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                }
            }
            case LEFT_TWO_SECONDS -> {
                if (flowerTransitionTicks > 10) {
                    applyFarmingInput(mc, DirectionKey.LEFT, true, false);
                } else {
                    applyFarmingInput(mc, true, false, true, false, true, false);
                }
                if (--flowerTransitionTicks <= 0) {
                    flowerState = FlowerState.HARVEST_RIGHT;
                    flowerMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case RETURN_BACK_TO_START -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (profile.startDetection && isConfiguredBlockNear(mc, player, profile.startBlock)) {
                    beginFlowerStep(player, FlowerState.RETURN_BACK_ONE, DirectionKey.BACK);
                }
            }
            case RETURN_BACK_ONE -> {
                applyFarmingInput(mc, DirectionKey.BACK, true, false);
                if (flowerStepDone(player)) {
                    flowerState = FlowerState.HARVEST_RIGHT;
                    flowerMarkerCleared = false;
                    applyFarmingInput(mc, DirectionKey.RIGHT, true, false);
                }
            }
            case INACTIVE -> { }
        }
    }

    private static boolean flowerMarkerDetected(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (!profile.rowDetection) return false;
        boolean markerPresent = findFarmingMarkerBlock(mc, player) != null;
        if (!flowerMarkerCleared) {
            if (!markerPresent) flowerMarkerCleared = true;
            return false;
        }
        if (markerPresent) {
            flowerMarkerCleared = false;
            return true;
        }
        return false;
    }

    private static void beginFlowerStep(LocalPlayer player, FlowerState state, DirectionKey direction) {
        flowerState = state;
        flowerStepStartX = player.getX();
        flowerStepStartZ = player.getZ();
        flowerStepLastX = player.getX();
        flowerStepLastZ = player.getZ();
        flowerStepStalledTicks = 0;
        Vec3 directionVector = farmingMovementVector(direction, farmingCrop.yaw);
        flowerStepDirectionX = directionVector.x;
        flowerStepDirectionZ = directionVector.z;
    }

    private static boolean flowerStepDone(LocalPlayer player) {
        double dx = player.getX() - flowerStepStartX;
        double dz = player.getZ() - flowerStepStartZ;
        double progress = dx * flowerStepDirectionX + dz * flowerStepDirectionZ;
        double tickDx = player.getX() - flowerStepLastX;
        double tickDz = player.getZ() - flowerStepLastZ;
        flowerStepLastX = player.getX();
        flowerStepLastZ = player.getZ();
        if (tickDx * tickDx + tickDz * tickDz < 0.0001) flowerStepStalledTicks++;
        else flowerStepStalledTicks = 0;
        return progress >= FARMING_ONE_BLOCK_DISTANCE || flowerStepStalledTicks >= FARMING_BLOCKED_STEP_TICKS;
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
                                warning(expectedTool));
                        stopFarming(mc, false);
                        saveConfig();
                        return true;
                    }
                }
                farmingCrop = detected;
                farmingCropDetected = true;
                farmingReverseLane = false;
                pumpkinMelonMarkerCleared = false;
                if (detected == FarmingCrop.PUMPKIN_MELON) beginPumpkinMelonStep(player, PumpkinMelonState.START_FORWARD_ONE);
                else pumpkinMelonState = PumpkinMelonState.INACTIVE;
                if (isAlternatingRowCrop(detected)) {
                    beginRowCropStep(player, RowCropState.START_FORWARD_ONE);
                } else {
                    rowCropState = RowCropState.INACTIVE;
                }
                rowCropMarkerCleared = false;
                rowCropWaitTicks = 0;
                sugarCaneState = detected == FarmingCrop.SUGAR_CANE ? SugarCaneState.HARVEST_LEFT : SugarCaneState.INACTIVE;
                sugarCaneMarkerCleared = false;
                if (detected == FarmingCrop.COCOA_BEAN) {
                    beginCocoaStep(player, CocoaState.START_LEFT_ONE, DirectionKey.LEFT, 1.0);
                } else {
                    cocoaState = CocoaState.INACTIVE;
                }
                cocoaMarkerCleared = false;
                if (detected == FarmingCrop.CACTUS) {
                    beginCactusStep(player, CactusState.START_FORWARD_ONE, DirectionKey.FORWARD, 1.0);
                } else {
                    cactusState = CactusState.INACTIVE;
                }
                cactusMarkerCleared = false;
                if (isFlowerCrop(detected)) {
                    beginFlowerStep(player, FlowerState.START_BACK_ONE, DirectionKey.BACK);
                } else {
                    flowerState = FlowerState.INACTIVE;
                }
                flowerMarkerCleared = false;
                flowerTransitionTicks = 0;
                MushroomColor mushroom = MushroomColor.fromBlockId(cropBlockId);
                if (detected == FarmingCrop.MUSHROOM) {
                    farmingReverseLane = mushroom == MushroomColor.BROWN;
                } else {
                    String detectedName = "minecraft:melon".equals(cropBlockId) ? "Melon"
                            : ("minecraft:pumpkin".equals(cropBlockId) || "minecraft:carved_pumpkin".equals(cropBlockId)) ? "Pumpkin" : detected.label;
                    say(mc, feature("Farming"), plain(" 準星作物辨識："), cropName(detectedName));
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
            case "minecraft:pumpkin", "minecraft:carved_pumpkin" -> "Pumpkin Dicer";
            case "minecraft:sunflower" -> SUNFLOWER_TOOL_NAME;
            case "minecraft:rose_bush" -> WILD_ROSE_TOOL_NAME;
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

    private static int farmingTicksNow() {
        return Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
    }

    private static boolean isFarmingPauseScreen(Screen screen) {
        return screen != null;
    }

    private static void pressFarmingMoveKeys(Minecraft mc, FarmingCrop crop, boolean reversed) {
        applyFarmingInput(mc, reversedKey(crop, reversed), true, false);
    }

    private static DirectionKey reversedKey(FarmingCrop crop, boolean reversed) {
        return reversed ? crop.reverseKey : crop.forwardKey;
    }

    private static void applyFarmingInput(Minecraft mc, DirectionKey key, boolean attack, boolean jump) {
        boolean forward = key == DirectionKey.FORWARD;
        boolean back = key == DirectionKey.BACK;
        boolean left = key == DirectionKey.LEFT;
        boolean right = key == DirectionKey.RIGHT;
        applyFarmingInput(mc, forward, back, left, right, attack, jump);
    }

    private static void applyFarmingJumpLateralInput(Minecraft mc, DirectionKey lateral, boolean attack) {
        applyFarmingInput(mc, false, false, lateral == DirectionKey.LEFT, lateral == DirectionKey.RIGHT, attack, true);
    }

    private static void applyFarmingInput(Minecraft mc, boolean forward, boolean back, boolean left, boolean right,
                                          boolean attack, boolean jump) {
        if (mc == null || mc.options == null) return;
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
                } else if (!TARGET_BLOCKS.contains(name)) {
                    TARGET_BLOCKS.add(name);
                    boolean finished = TARGET_BLOCKS.size() >= miningSelectionTargetCount;
                    selectMode = !finished;
                    saveConfig();
                    say(mc, feature("Mining"), plain(" 目標方塊已設定 " + TARGET_BLOCKS.size() + "/"
                            + miningSelectionTargetCount + "："), blockName(name),
                            finished ? success("（選取結束）") : plain(""));
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
            return;
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
    private static Component blockName(String text) {
        String display = text != null && text.startsWith("minecraft:") ? text.substring("minecraft:".length()) : text;
        return Component.literal(display == null ? "" : display).withColor(COLOR_BLOCK);
    }
    private static Component locationName(String text) { return Component.literal(text).withColor(COLOR_LOCATION); }
    private static Component cropName(String text) { return Component.literal(text).withStyle(style -> style.withColor(COLOR_CROP).withBold(true)); }

    private static void sayAutoState(Minecraft mc, String featureName, boolean enabled) {
        say(mc, feature(featureName), plain(" "), enabled ? success("開啟") : error("關閉"));
    }

    private static void handleReceivedMessage(Component message) {
        if (message == null) return;
        String text = StringUtil.stripColor(message.getString());
        int now = farmingTicksNow();
        if (text.contains("You fell into the void") && abnormalHandlingEnabled && farmingEnabled
                && farmingEnabledThisSession && farmingWasInGarden && !farmingAutoRecovering) {
            stopFarming(Minecraft.getInstance(), true);
            return;
        }
        if (text.contains("/limbo for more information")) {
            limboChatSeenTick = now;
            if (abnormalHandlingEnabled && farmingEnabled && farmingEnabledThisSession && farmingWasInGarden
                    && !farmingAutoRecovering && sidebarLines(Minecraft.getInstance()).isEmpty()) {
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
        visitorNotificationEnabled = Boolean.parseBoolean(p.getProperty("visitorNotificationEnabled", "false"));
        placeholderNotificationEnabled = Boolean.parseBoolean(p.getProperty("placeholderNotificationEnabled", "false"));
        farmingToolDetectionEnabled = Boolean.parseBoolean(p.getProperty("farmingToolDetectionEnabled", "false"));
        boolean legacyAbnormalHandling = Boolean.parseBoolean(p.getProperty("islandReturnEnabled", "true"))
                || Boolean.parseBoolean(p.getProperty("hubReturnEnabled", "true"))
                || Boolean.parseBoolean(p.getProperty("lobbyReturnEnabled", "true"))
                || Boolean.parseBoolean(p.getProperty("limboReturnEnabled", "true"));
        abnormalHandlingEnabled = Boolean.parseBoolean(p.getProperty("abnormalHandlingEnabled",
                Boolean.toString(legacyAbnormalHandling)));
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
            if (!id.isBlank() && isAllowedTargetBlock(id) && !TARGET_BLOCKS.contains(id) && TARGET_BLOCKS.size() < 5) {
                TARGET_BLOCKS.add(id);
            }
        }
    }

    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("miningEnabled", "false");
        p.setProperty("farmingEnabled", "false");
        p.setProperty("farmingCrop", farmingCrop.name());
        p.setProperty("farmingAutoSetHome", Boolean.toString(farmingAutoSetHome));
        p.setProperty("pestNotificationEnabled", Boolean.toString(pestNotificationEnabled));
        p.setProperty("visitorNotificationEnabled", Boolean.toString(visitorNotificationEnabled));
        p.setProperty("placeholderNotificationEnabled", Boolean.toString(placeholderNotificationEnabled));
        p.setProperty("farmingToolDetectionEnabled", Boolean.toString(farmingToolDetectionEnabled));
        p.setProperty("abnormalHandlingEnabled", Boolean.toString(abnormalHandlingEnabled));
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
        String defaultRowBlock = legacyRowBlock.isBlank() ? "minecraft:yellow_stained_glass" : legacyRowBlock;
        FARMING_BLOCK_PROFILE.rowBlock = sharedFarmingBlock(p, "rowBlock", defaultRowBlock);
        FARMING_BLOCK_PROFILE.startBlock = sharedFarmingBlock(p, "startBlock",
                p.getProperty("farmingStartBlock", "minecraft:light_blue_stained_glass"));
        FARMING_BLOCK_PROFILE.endBlock = sharedFarmingBlock(p, "endBlock",
                p.getProperty("farmingEndBlock", "minecraft:pink_stained_glass"));
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
        END_RIGHT_ONE,
        RETURN_BACK_TO_START,
        RETURN_BACK_TWO,
        RETURN_LEFT_ONE,
        RETURN_FORWARD_ONE
    }

    private enum RowCropState {
        INACTIVE,
        START_FORWARD_ONE,
        HARVEST_PRIMARY,
        PRIMARY_EXTRA_ONE,
        PRIMARY_JUMP_THREE_SECONDS,
        PRIMARY_JUMP_LATERAL_ONE_SECOND,
        HARVEST_SECONDARY,
        SECONDARY_EXTRA_ONE,
        SECONDARY_JUMP_THREE_SECONDS,
        SECONDARY_JUMP_LATERAL_ONE_SECOND,
        END_EXTRA_ONE,
        RETURN_TO_START,
        WAIT_ONE_SECOND
    }

    private enum SugarCaneState {
        INACTIVE,
        HARVEST_LEFT,
        FORWARD_FIVE,
        RIGHT_ONE,
        HARVEST_BACK,
        RIGHT_SEVEN,
        RETURN_TO_START,
        RETURN_BACK_TWO
    }

    private enum CocoaState {
        INACTIVE,
        START_LEFT_ONE,
        HARVEST_FORWARD,
        RIGHT_ONE,
        HARVEST_BACK,
        RIGHT_TWO,
        FORWARD_ONE,
        LEFT_ONE,
        RETURN_LEFT_TO_START
    }

    private enum CactusState {
        INACTIVE,
        START_FORWARD_ONE,
        HARVEST_LEFT,
        LEFT_EXTRA_ONE,
        LEFT_FORWARD_TWO,
        WAIT_BEFORE_HARVEST_RIGHT,
        HARVEST_RIGHT,
        RIGHT_EXTRA_ONE,
        RIGHT_FORWARD_TWO,
        WAIT_BEFORE_HARVEST_LEFT,
        RETURN_BACK_TO_START,
        RETURN_LEFT_ONE
    }

    private enum FlowerState {
        INACTIVE,
        START_BACK_ONE,
        HARVEST_RIGHT,
        FORWARD_TWO_SECONDS,
        HARVEST_BACK,
        LEFT_TWO_SECONDS,
        RETURN_BACK_TO_START,
        RETURN_BACK_ONE
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
        WHEAT("Wheat", 90.0f, 5.5f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        POTATO("Potato", 90.0f, 5.5f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        CARROT("Carrot", -90.0f, 5.5f, DirectionKey.RIGHT, DirectionKey.LEFT, "minecraft:quartz_block"),
        NETHER_WART("Nether Wart", -90.0f, 5.5f, DirectionKey.RIGHT, DirectionKey.LEFT, "minecraft:quartz_block"),
        CACTUS("Cactus", 90.0f, 0.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        SUGAR_CANE("Sugar Cane", 45.0f, 0.0f, DirectionKey.LEFT, DirectionKey.BACK, "minecraft:quartz_block"),
        PUMPKIN_MELON("Pumpkim & Melon", 90.0f, -57.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        COCOA_BEAN("Cocoa Bean", 0.0f, -45.0f, DirectionKey.FORWARD, DirectionKey.BACK, "#dirt"),
        SUNFLOWER("Sunflower", -45.0f, 0.0f, DirectionKey.RIGHT, DirectionKey.BACK, "minecraft:quartz_block"),
        WILD_ROSE("Wild Rose", -45.0f, 0.0f, DirectionKey.RIGHT, DirectionKey.BACK, "minecraft:quartz_block"),
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
                case "minecraft:pumpkin", "minecraft:carved_pumpkin", "minecraft:melon" -> PUMPKIN_MELON;
                case "minecraft:cocoa" -> COCOA_BEAN;
                case "minecraft:sunflower" -> SUNFLOWER;
                case "minecraft:rose_bush" -> WILD_ROSE;
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
            if ("MOONFLOWER".equals(normalized)) normalized = "SUNFLOWER";
            if ("ROSE_BUSH".equals(normalized)) normalized = "WILD_ROSE";
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

    private static final class MiningTargetCountScreen extends Screen {
        private final Screen parent;

        MiningTargetCountScreen(Screen parent) {
            super(Component.literal("選擇目標方塊數量"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int y = this.height / 2 - 45;
            addRenderableWidget(new StringWidget(centerX - 120, y, 240, 12,
                    Component.literal("請輸入要選擇的方塊種類數量"), this.font));
            y += 20;
            EditBox countBox = new EditBox(this.font, centerX - 50, y, 100, 20,
                    Component.literal("目標方塊數量"));
            countBox.setMaxLength(1);
            countBox.setValue("");
            addRenderableWidget(countBox);
            y += 24;
            addRenderableWidget(new StringWidget(centerX - 120, y, 240, 12,
                    Component.literal("留空代表 1，最少 1、最多 5，不可重複"), this.font));
            y += 20;
            Button confirm = Button.builder(Component.literal("確認"), b -> {
                String value = countBox.getValue().trim();
                miningSelectionTargetCount = value.isEmpty() ? 1 : parseInt(value, 1, 1, 5);
                miningEnabled = false;
                clearActiveTarget(Minecraft.getInstance());
                TARGET_BLOCKS.clear();
                selectMode = true;
                farmingSelectMode = FarmingSelectMode.NONE;
                saveConfig();
                Minecraft.getInstance().setScreenAndShow(null);
                say(Minecraft.getInstance(), feature("Mining"), plain(" 方塊選擇：請右鍵選擇 "),
                        warning(Integer.toString(miningSelectionTargetCount)), plain(" 種目標方塊"));
            }).bounds(centerX - 105, y, 100, 20).build();
            addRenderableWidget(confirm);
            addRenderableWidget(Button.builder(Component.literal("取消"), b -> Minecraft.getInstance().setScreenAndShow(parent))
                    .bounds(centerX + 5, y, 100, 20).build());
            countBox.setResponder(value -> {
                boolean valid = value.isEmpty() || value.matches("[1-5]");
                countBox.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
                confirm.active = valid;
            });
        }

        @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
        @Override public boolean isPauseScreen() { return false; }
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
            addRenderableWidget(Button.builder(toggleMsg("異常狀況處理", abnormalHandlingEnabled), b -> { abnormalHandlingEnabled = !abnormalHandlingEnabled; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(Button.builder(toggleMsg("害蟲通知", pestNotificationEnabled), b -> { pestNotificationEnabled = !pestNotificationEnabled; saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("Visitor 通知", visitorNotificationEnabled), b -> { visitorNotificationEnabled = !visitorNotificationEnabled; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("？？？通知", placeholderNotificationEnabled), b -> { placeholderNotificationEnabled = !placeholderNotificationEnabled; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
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
            addFarmingBlockDisplay(x, y, farmingMarkerSummary());
            addFarmingBlockDisplay(x + 145, y, blockProfile.startBlock);
            addFarmingBlockDisplay(x + 290, y, blockProfile.endBlock);

            y += 28;
            addSectionTitle("Mining", y);
            y += 22;
            addRenderableWidget(Button.builder(toggleMsg("Mining", miningEnabled), b -> { toggleMining(Minecraft.getInstance()); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("自動轉向", rotateToTarget), b -> { rotateToTarget = !rotateToTarget; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("選擇目標方塊"), b -> openMiningTargetCountScreen()).bounds(x + 290, y, 130, 20).build());
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
            y += 28;
            int maxRows = Math.min(5, TARGET_BLOCKS.size());
            for (int i = 0; i < maxRows; i++) {
                final int idx = i;
                String name = TARGET_BLOCKS.get(i);
                var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
                addRenderableWidget(new ItemDisplayWidget(this.minecraft, x, y + 1, 18, 18,
                        Component.literal(name), block.asItem().getDefaultInstance(), false, true));
                addRenderableWidget(new StringWidget(x + 22, y + 5, 208, 12, Component.literal((i + 1) + ". " + name), this.font));
                addRenderableWidget(Button.builder(Component.literal("↑"), b -> { move(idx, -1); rebuild(); }).bounds(x + 240, y, 36, 20).build());
                addRenderableWidget(Button.builder(Component.literal("↓"), b -> { move(idx, 1); rebuild(); }).bounds(x + 280, y, 36, 20).build());
                addRenderableWidget(Button.builder(Component.literal("刪"), b -> { TARGET_BLOCKS.remove(idx); saveConfig(); rebuild(); }).bounds(x + 320, y, 45, 20).build());
                y += 24;
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

        private void addFarmingBlockDisplay(int x, int y, String id) {
            try {
                if (id != null && !id.isBlank()) {
                    var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
                    var stack = block.asItem().getDefaultInstance();
                    if (!stack.isEmpty()) {
                        addRenderableWidget(new ItemDisplayWidget(this.minecraft, x, y, 18, 18,
                                Component.literal(id), stack, false, true));
                    }
                }
            } catch (Throwable ignored) {}
            addRenderableWidget(new StringWidget(x + 22, y + 5, 108, 12,
                    Component.literal(shortBlockName(id)), this.font));
        }

        private String shortBlockName(String id) {
            if (id == null || id.isBlank()) return "未設定";
            String s = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
            return s.length() > 18 ? s.substring(0, 18) : s;
        }

        private void openMiningTargetCountScreen() {
            Minecraft.getInstance().setScreenAndShow(new MiningTargetCountScreen(this));
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
