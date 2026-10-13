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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
    private static final List<String> FARMING_MARKER_BLOCKS = new ArrayList<>();
    private static final Pattern PEST_MESSAGE_PATTERN = Pattern.compile("GROSS!.*Pest.*appeared in.*Plot\\s*-\\s*([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static String farmingStartBlock = "";
    private static String farmingEndBlock = "";
    private static final Map<BlockPos, Integer> SKIPPED_TARGETS = new HashMap<>();
    private static final Map<BlockPos, String> DEPLETED_TARGETS = new HashMap<>();
    private static final int UNREACHABLE_SKIP_TICKS = 200;
    private static final int FARM_TURN_STOP_TICKS = 4;
    private static final int FARM_TURN_RESUME_TICKS = 10;
    private static final int MUSHROOM_LEFT_SHIFT_TICKS = 10;
    private static boolean miningEnabled = false;
    private static boolean selectMode = false;
    private static boolean farmingEnabled = false;
    private static boolean farmingEnabledThisSession = false;
    private static FarmingSelectMode farmingSelectMode = FarmingSelectMode.NONE;
    private static FarmingCrop farmingCrop = FarmingCrop.WHEAT;
    private static boolean farmingAutoSetHome = false;
    private static boolean farmingTurnOnMarkers = true;
    private static boolean farmingStartDetection = true;
    private static boolean farmingEndDetection = true;
    private static boolean pestNotificationEnabled = false;
    private static String pestWebhookUrl = "";
    private static boolean farmingReverseLane = false;
    private static int farmingStateTicks = 0;
    private static int farmingLeftShiftTicks = 0;
    private static int farmingRightShiftTicks = 0;
    private static boolean farmingReturningToStart = false;
    private static boolean farmingAutoRecovering = false;
    private static boolean farmingRecoveryHomeSent = false;
    private static int farmingRecoveryScanTicks = 0;
    private static final Deque<QueuedCommand> FARMING_COMMAND_QUEUE = new ArrayDeque<>();
    private static int farmingStuckTicks = 0;
    private static double farmingLastX = 0.0;
    private static double farmingLastY = 0.0;
    private static double farmingLastZ = 0.0;
    private static int farmingStuckSampleTicks = 0;
    private static int farmingMarkerIgnoreTicks = 0;
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
    private static boolean selectComboWasDown = false;
    private static String lastFarmingMarkerMessage = "";
    private static int lastFarmingMarkerMessageTick = 0;
    private static String lastPestMessage = "";
    private static int lastPestMessageTick = 0;
    private static BlockPos activeTarget = null;
    private static String activeTargetBlockId = null;
    private static boolean activeTargetStarted = false;
    private static KeyMapping settingsKey;
    private static KeyMapping toggleMiningKey;
    private static KeyMapping toggleFarmingKey;
    private static KeyMapping selectModeKey;

    @Override
    public void onInitializeClient() {
        loadConfig();
        KeyMapping.Category category = KeyMapping.Category.register(id("keys"));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_APOSTROPHE, category));
        toggleMiningKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.toggle_mining", InputConstants.Type.KEYSYM, InputConstants.KEY_M, category));
        toggleFarmingKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.toggle_farming", InputConstants.Type.KEYSYM, InputConstants.KEY_F, category));
        selectModeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.select_blocks", InputConstants.Type.KEYSYM, InputConstants.KEY_B, category));
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
        if (farmingEnabled) {
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
        boolean selectCombo = shift && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS;
        if (miningCombo && !miningComboWasDown) toggleMining(mc);
        if (farmingCombo && !farmingComboWasDown) toggleFarming(mc);
        if (selectCombo && !selectComboWasDown) toggleSelectMode(mc);
        miningComboWasDown = miningCombo;
        farmingComboWasDown = farmingCombo;
        selectComboWasDown = selectCombo;
    }

    private static void toggleMining(Minecraft mc) {
        miningEnabled = !miningEnabled;
        if (miningEnabled) stopFarming(mc, false);
        if (!miningEnabled) clearActiveTarget(mc);
        saveConfig();
        say(mc, "Mining：" + (miningEnabled ? "開" : "關"));
    }

    private static void toggleSelectMode(Minecraft mc) {
        selectMode = !selectMode;
        saveConfig();
        say(mc, selectMode ? "方塊選取模式：開（右鍵方塊加入/更新，Shift+B 結束）" : "方塊選取模式：關");
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
            say(mc, "Farming：開（" + farmingCrop.label + "）");
        }
        saveConfig();
    }

    private static void stopFarming(Minecraft mc, boolean announce) {
        farmingEnabled = false;
        farmingEnabledThisSession = false;
        farmingAutoRecovering = false;
        farmingRecoveryHomeSent = false;
        FARMING_COMMAND_QUEUE.clear();
        releaseFarmingKeys(mc);
        if (announce) say(mc, "Farming：關");
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
        farmingAutoRecovering = false;
        farmingRecoveryHomeSent = false;
        farmingRecoveryScanTicks = 0;
        FARMING_COMMAND_QUEUE.clear();
        if (mc.player != null) {
            farmingLastX = mc.player.getX();
            farmingLastY = mc.player.getY();
            farmingLastZ = mc.player.getZ();
        }
    }

    private static void tickFarming(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.options == null) {
            releaseFarmingKeys(mc);
            return;
        }
        tickFarmingCommandQueue(mc);
        if (handleFarmingAutoRecovery(mc)) return;
        LocalPlayer player = mc.player;
        lookAtFixed(player, farmingCrop.yaw, farmingCrop.pitch);
        if (farmingReturningToStart) {
            setAttackKey(mc, false);
            if (farmingStartDetection && isConfiguredBlockNear(mc, player, farmingStartBlock)) {
                say(mc, "Farming 偵測到開始方塊：" + farmingStartBlock);
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
        if (farmingEndDetection && isConfiguredBlockNear(mc, player, farmingEndBlock)) {
            say(mc, "Farming 偵測到結束方塊：" + farmingEndBlock + "，開始後退找開始方塊");
            farmingReturningToStart = true;
            applyFarmingInput(mc, DirectionKey.BACK, false, false);
            return;
        }
        String marker = null;
        if (farmingMarkerIgnoreTicks > 0) {
            farmingMarkerIgnoreTicks--;
        } else if (farmingTurnOnMarkers) {
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
        farmingRecoveryScanTicks++;
        if (farmingRecoveryScanTicks < 40 && !farmingAutoRecovering) return false;
        farmingRecoveryScanTicks = 0;
        FarmingLocation location = detectFarmingLocation(mc);
        if (!farmingAutoRecovering && location != FarmingLocation.GARDEN && location != FarmingLocation.UNKNOWN) {
            farmingAutoRecovering = true;
            farmingRecoveryHomeSent = false;
            FARMING_COMMAND_QUEUE.clear();
            releaseFarmingKeys(mc);
            say(mc, "Farming 偵測到離開 Garden：" + location.label + "，嘗試回 Garden");
            if (location == FarmingLocation.LIMBO) {
                enqueueFarmingCommand("lobby", 20);
                enqueueFarmingCommand("play sb", 100);
                enqueueFarmingCommand("warp garden", 180);
            } else if (location == FarmingLocation.LOBBY) {
                enqueueFarmingCommand("play sb", 20);
                enqueueFarmingCommand("warp garden", 120);
            } else if (location == FarmingLocation.HUB || location == FarmingLocation.ISLAND) {
                enqueueFarmingCommand("warp garden", 20);
            }
            return true;
        }
        if (farmingAutoRecovering) {
            releaseFarmingKeys(mc);
            if (location == FarmingLocation.GARDEN && FARMING_COMMAND_QUEUE.isEmpty()) {
                if (!farmingRecoveryHomeSent) {
                    sendCommand(mc, "home");
                    farmingRecoveryHomeSent = true;
                    enqueueFarmingCommand("__resume__", 60);
                    say(mc, "Farming 已回到 Garden，輸入 /home 後準備恢復採集");
                }
            }
            return true;
        }
        return false;
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
            resetFarmingRuntime(mc);
            say(mc, "Farming 恢復採集");
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
        if (containsLine(lines, "Limbo") || looksLikeLimbo(mc)) return FarmingLocation.LIMBO;
        if (containsLine(lines, "The Garden") || containsLine(lines, "Garden") || containsLine(lines, "Plot")) return FarmingLocation.GARDEN;
        if (containsLine(lines, "Your Island")) return FarmingLocation.ISLAND;
        if (containsLine(lines, "SkyBlock Hub") || containsLine(lines, "Hub") || containsLine(lines, "Village") || containsLine(lines, "Auction House") || containsLine(lines, "Bazaar Alley")) return FarmingLocation.HUB;
        if (containsLine(lines, "HYPIXEL") || containsLine(lines, "PROTOTYPE") || containsLine(lines, "LOBBY") || containsLine(lines, "Lobby")) return FarmingLocation.LOBBY;
        return FarmingLocation.UNKNOWN;
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
        if (mode == FarmingSelectMode.ROW) {
            FARMING_MARKER_BLOCKS.clear();
            FARMING_MARKER_BLOCKS.add(name);
            say(mc, "已設定換行方塊：" + name);
        } else if (mode == FarmingSelectMode.START) {
            farmingStartBlock = name;
            say(mc, "已設定開始方塊：" + name);
        } else if (mode == FarmingSelectMode.END) {
            farmingEndBlock = name;
            say(mc, "已設定結束方塊：" + name);
        }
    }

    private static boolean isConfiguredBlockNear(Minecraft mc, LocalPlayer player, String blockId) {
        if (blockId == null || blockId.isBlank()) return false;
        return isBlockNear(mc, player, blockId, 1, 0, 1);
    }

    private static String findFarmingMarkerBlock(Minecraft mc, LocalPlayer player) {
        if (!FARMING_MARKER_BLOCKS.isEmpty()) return findConfiguredFarmingMarker(mc, player);
        return farmingCrop.findDefaultMarker(mc, player);
    }

    private static String findConfiguredFarmingMarker(Minecraft mc, LocalPlayer player) {
        if (mc.level == null) return null;
        BlockPos base = player.blockPosition();
        for (int y = 0; y <= 1; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            String id = blockId(mc.level.getBlockState(base.offset(dx, y, dz)));
            if (FARMING_MARKER_BLOCKS.contains(id)) return id;
        }
        return null;
    }

    private static void announceFarmingMarker(Minecraft mc, String marker) {
        int now = farmingTicksNow();
        if (!marker.equals(lastFarmingMarkerMessage) || now - lastFarmingMarkerMessageTick > 40) {
            say(mc, "Farming 偵測到換行方塊：" + marker);
            lastFarmingMarkerMessage = marker;
            lastFarmingMarkerMessageTick = now;
        }
    }

    private static int farmingTicksNow() {
        return Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
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
                    saveConfig();
                    say(mc, "已更新 Mining 目標方塊 #" + TARGET_BLOCKS.size() + "：" + name);
                }
            } else if ("minecraft:bedrock".equals(name)) {
                say(mc, "Bedrock 不可設為目標方塊");
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
        if (best == null) return;
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

    private static void say(Minecraft mc, String text) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Kuan SBA] " + text).withStyle(ChatFormatting.AQUA));
    }

    private static void handleReceivedMessage(Component message) {
        if (!pestNotificationEnabled || pestWebhookUrl.isBlank() || message == null) return;
        String text = StringUtil.stripColor(message.getString());
        Matcher matcher = PEST_MESSAGE_PATTERN.matcher(text);
        if (!matcher.find()) return;
        int now = farmingTicksNow();
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
        farmingTurnOnMarkers = Boolean.parseBoolean(p.getProperty("farmingTurnOnMarkers", "true"));
        farmingStartDetection = Boolean.parseBoolean(p.getProperty("farmingStartDetection", "true"));
        farmingEndDetection = Boolean.parseBoolean(p.getProperty("farmingEndDetection", "true"));
        pestNotificationEnabled = Boolean.parseBoolean(p.getProperty("pestNotificationEnabled", "false"));
        pestWebhookUrl = p.getProperty("pestWebhookUrl", "").trim();
        farmingStartBlock = p.getProperty("farmingStartBlock", "").trim();
        farmingEndBlock = p.getProperty("farmingEndBlock", "").trim();
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
        FARMING_MARKER_BLOCKS.clear();
        String farmingMarkers = p.getProperty("farmingMarkers", "").trim();
        if (!farmingMarkers.isEmpty()) for (String s : farmingMarkers.split(",")) {
            String id = s.trim();
            if (!id.isBlank() && isAllowedTargetBlock(id) && !FARMING_MARKER_BLOCKS.contains(id)) FARMING_MARKER_BLOCKS.add(id);
        }
    }

    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("miningEnabled", "false");
        p.setProperty("farmingEnabled", "false");
        p.setProperty("farmingCrop", farmingCrop.name());
        p.setProperty("farmingAutoSetHome", Boolean.toString(farmingAutoSetHome));
        p.setProperty("farmingTurnOnMarkers", Boolean.toString(farmingTurnOnMarkers));
        p.setProperty("farmingStartDetection", Boolean.toString(farmingStartDetection));
        p.setProperty("farmingEndDetection", Boolean.toString(farmingEndDetection));
        p.setProperty("pestNotificationEnabled", Boolean.toString(pestNotificationEnabled));
        p.setProperty("pestWebhookUrl", pestWebhookUrl == null ? "" : pestWebhookUrl.trim());
        p.setProperty("farmingStartBlock", farmingStartBlock == null ? "" : farmingStartBlock);
        p.setProperty("farmingEndBlock", farmingEndBlock == null ? "" : farmingEndBlock);
        p.setProperty("rotateToTarget", Boolean.toString(rotateToTarget));
        p.setProperty("scanRadius", Integer.toString(scanRadius));
        p.setProperty("rotationStepDegrees", Integer.toString(rotationStepDegrees));
        p.setProperty("actionDelayTicks", Integer.toString(actionDelayTicks));
        p.setProperty("targets", String.join(",", TARGET_BLOCKS));
        p.setProperty("farmingMarkers", String.join(",", FARMING_MARKER_BLOCKS));
        try (FileOutputStream out = new FileOutputStream(configFile())) { p.store(out, "Kuan Hypixel SkyBlock Assistant"); } catch (Throwable ignored) {}
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

    private enum FarmingSelectMode { NONE, ROW, START, END }

    private enum FarmingCrop {
        WHEAT("Wheat", -90.0f, 6.0f, DirectionKey.RIGHT, DirectionKey.LEFT, "minecraft:quartz_block"),
        POTATO("Potato", -90.0f, 6.0f, DirectionKey.RIGHT, DirectionKey.LEFT, "minecraft:quartz_block"),
        CARROT("Carrot", 90.0f, 6.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        NETHER_WART("Nether Wart", 90.0f, 6.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        CACTUS("Cactus", -90.0f, 0.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        SUGAR_CANE("Sugar Cane", 135.0f, 0.0f, DirectionKey.BACK, DirectionKey.RIGHT, "minecraft:quartz_block"),
        MELON("Melon", 90.0f, 30.0f, DirectionKey.LEFT, DirectionKey.RIGHT, "minecraft:quartz_block"),
        PUMPKIN("Pumpkin", 90.0f, 30.0f, DirectionKey.RIGHT, DirectionKey.LEFT, "minecraft:quartz_block"),
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
            if (this == MELON || this == PUMPKIN || this == CACTUS || this == MUSHROOM) {
                return firstNear(mc, player, 1, 0, 1, markerBlock, "#ladder", "#trapdoor");
            }
            return firstNear(mc, player, 1, 0, 0, markerBlock, "#trapdoor");
        }

        static FarmingCrop fromConfig(String value) {
            if (value == null) return WHEAT;
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            if ("NETHERWART".equals(normalized)) normalized = "NETHER_WART";
            if ("COCOA".equals(normalized) || "COCOA_BEANS".equals(normalized)) normalized = "COCOA_BEAN";
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
        private final boolean cropDropdownOpen;
        SettingsScreen(Screen parent) {
            this(parent, false);
        }
        SettingsScreen(Screen parent, boolean cropDropdownOpen) {
            super(Component.literal("Kuan Hypixel SkyBlock Assistant"));
            this.parent = parent;
            this.cropDropdownOpen = cropDropdownOpen;
        }

        @Override
        protected void init() {
            int x = (this.width - 420) / 2;
            int y = 28;
            addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("──────── Mining ────────"), this.font));
            y += 22;
            addRenderableWidget(Button.builder(toggleMsg("Mining", miningEnabled), b -> { toggleMining(Minecraft.getInstance()); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("選取模式", selectMode), b -> { selectMode = !selectMode; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
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
                y += 18;
            }
            y += 12;
            addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("──────── Farming ────────"), this.font));
            y += 20;
            addRenderableWidget(Button.builder(toggleMsg("Farming", farmingEnabled), b -> { toggleFarming(Minecraft.getInstance()); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("作物：" + farmingCrop.label), b -> Minecraft.getInstance().setScreenAndShow(new SettingsScreen(parent, !cropDropdownOpen))).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("害蟲通知", pestNotificationEnabled), b -> { pestNotificationEnabled = !pestNotificationEnabled; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(new StringWidget(x, y + 5, 90, 12, Component.literal("Webhook:"), this.font));
            EditBox webhookBox = new EditBox(this.font, x + 90, y, 330, 20, Component.literal("Discord Webhook URL"));
            webhookBox.setMaxLength(300);
            webhookBox.setValue(pestWebhookUrl == null ? "" : pestWebhookUrl);
            webhookBox.setSuggestion("Discord webhook URL");
            webhookBox.setResponder(value -> { pestWebhookUrl = value.trim(); saveConfig(); });
            addRenderableWidget(webhookBox);
            y += 28;
            if (cropDropdownOpen) {
                int cropY = y;
                FarmingCrop[] crops = FarmingCrop.values();
                for (int i = 0; i < crops.length; i++) {
                    final FarmingCrop crop = crops[i];
                    int col = i / 5;
                    int row = i % 5;
                    addRenderableWidget(Button.builder(Component.literal(crop.label), b -> { farmingCrop = crop; resetFarmingRuntime(Minecraft.getInstance()); saveConfig(); rebuild(); }).bounds(x + 145 + col * 145, cropY + row * 22, 130, 20).build());
                }
                y += 5 * 22 + 6;
            }
            addRenderableWidget(Button.builder(toggleMsg("換行偵測", farmingTurnOnMarkers), b -> { farmingTurnOnMarkers = !farmingTurnOnMarkers; saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("開始偵測", farmingStartDetection), b -> { farmingStartDetection = !farmingStartDetection; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("結束偵測", farmingEndDetection), b -> { farmingEndDetection = !farmingEndDetection; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("換行方塊"), b -> beginFarmingBlockPick(FarmingSelectMode.ROW)).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("開始方塊"), b -> beginFarmingBlockPick(FarmingSelectMode.START)).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("結束方塊"), b -> beginFarmingBlockPick(FarmingSelectMode.END)).bounds(x + 290, y, 130, 20).build());
            y += 22;
            addRenderableWidget(new StringWidget(x, y, 130, 12, Component.literal(shortBlockName(farmingMarkerSummary())), this.font));
            addRenderableWidget(new StringWidget(x + 145, y, 130, 12, Component.literal(shortBlockName(farmingStartBlock)), this.font));
            addRenderableWidget(new StringWidget(x + 290, y, 130, 12, Component.literal(shortBlockName(farmingEndBlock)), this.font));
            y = this.height - 34;
            addRenderableWidget(Button.builder(Component.literal("完成"), b -> Minecraft.getInstance().setScreenAndShow(parent)).bounds(x + 145, y, 130, 20).build());
        }

        private Component toggleMsg(String label, boolean on) {
            return Component.literal(label + "：")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(on ? "開啟" : "關閉").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        private String farmingMarkerSummary() {
            if (FARMING_MARKER_BLOCKS.isEmpty()) return "預設";
            return FARMING_MARKER_BLOCKS.get(FARMING_MARKER_BLOCKS.size() - 1);
        }

        private String shortBlockName(String id) {
            if (id == null || id.isBlank()) return "未設定";
            String s = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
            return s.length() > 18 ? s.substring(0, 18) : s;
        }

        private void beginFarmingBlockPick(FarmingSelectMode mode) {
            farmingSelectMode = mode;
            selectMode = false;
            saveConfig();
            Minecraft.getInstance().setScreenAndShow(parent);
            say(Minecraft.getInstance(), switch (mode) {
                case ROW -> "請右鍵第一個要作為換行方塊的方塊";
                case START -> "請右鍵第一個要作為開始方塊的方塊";
                case END -> "請右鍵第一個要作為結束方塊的方塊";
                default -> "";
            });
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
