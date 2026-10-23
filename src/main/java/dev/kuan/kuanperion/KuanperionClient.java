package dev.kuan.kuanperion;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kuan.kuanperion.ui.MainGUIRegistry;
import dev.kuan.kuanperion.ui.settings.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.DisplaySlot;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
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

public final class KuanperionClient implements ClientModInitializer {
    private static final String MOD_ID = "kuanperion";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final SkyBlockLocationBridge SKYBLOCK_LOCATION_BRIDGE = SkyBlockLocationBridge.discover();
    private static String lastFarmingLocationDiagnostic = "";
    private static final String CONFIG_NAME = "kuanperion.properties";
    private static final List<String> TARGET_BLOCKS = new ArrayList<>();
    private static final FarmingBlockProfile FARMING_BLOCK_PROFILE = new FarmingBlockProfile();
    private static final FarmingToolGuard FARMING_TOOL_GUARD = new FarmingToolGuard();
    private static final FarmingStallGuard FARMING_STALL_GUARD = new FarmingStallGuard();
    private static final GuiPauseGuard FARMING_GUI_PAUSE_GUARD = new GuiPauseGuard();
    private static final GuiPauseGuard MINING_GUI_PAUSE_GUARD = new GuiPauseGuard();
    private static final GardenTabNotifications TAB_NOTIFICATIONS = new GardenTabNotifications();
    private static final ScreenNotificationQueue SCREEN_NOTIFICATIONS = new ScreenNotificationQueue();
    private static final JerryFarmingPauseMarkers JERRY_FARMING_MARKERS = new JerryFarmingPauseMarkers();
    private static final Map<NotificationType, NotificationChannels> NOTIFICATION_CHANNELS = new EnumMap<>(NotificationType.class);
    private static BigDecimal jerryMarkerMaxSeconds = JerryFarmingPauseMarkers.DEFAULT_MAX_SECONDS;
    private static Set<JerryColor> jerryColors = Set.of(JerryColor.values());
    private static int pestNotificationThreshold = 1;
    private static int visitorNotificationThreshold = 5;
    private static int tabNotificationTicks;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Map<BlockPos, Integer> SKIPPED_TARGETS = new HashMap<>();
    private static final Map<BlockPos, String> DEPLETED_TARGETS = new HashMap<>();
    private static final int UNREACHABLE_SKIP_TICKS = 200;
    private static final int FARMING_RECOVERY_COMMAND_DELAY_TICKS = 60;
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
    private static boolean farmingManualStartPending = false;
    private static FarmingCrop farmingCrop = FarmingCrop.WHEAT;
    private static boolean farmingAutoSetHome = false;
    private static boolean pestNotificationEnabled = false;
    private static boolean visitorNotificationEnabled = false;
    private static boolean jerryStopActionsEnabled = false;
    private static boolean farmingMenuPauseEnabled = true;
    private static boolean miningMenuPauseEnabled = true;
    private static boolean farmingToolDetectionEnabled = false;
    private static boolean abnormalHandlingEnabled = true;
    private static String pestWebhookUrl = "";
    private static boolean farmingReverseLane = false;
    private static int farmingStateTicks = 0;
    private static int farmingLeftShiftTicks = 0;
    private static int farmingRightShiftTicks = 0;
    private static boolean farmingReturningToStart = false;
    private static boolean farmingAutoRecovering = false;
    private static boolean farmingWorldChanged = false;
    private static int farmingRecoveryScanTicks = 0;
    private static int limboChatSeenTick = -100000;
    private static final Deque<QueuedCommand> FARMING_COMMAND_QUEUE = new ArrayDeque<>();
    private static int farmingMarkerIgnoreTicks = 0;
    private static boolean farmingCropDetected = false;
    private static int farmingCropProbeTicks = 0;
    private static PumpkinMelonState pumpkinMelonState = PumpkinMelonState.INACTIVE;
    private static boolean pumpkinMelonStartRight = false;
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
    private static boolean farmingDesiredForward = false;
    private static boolean farmingDesiredBack = false;
    private static boolean farmingDesiredLeft = false;
    private static boolean farmingDesiredRight = false;
    private static boolean farmingDesiredJump = false;
    private static double flowerStepStartX = 0.0;
    private static double flowerStepStartZ = 0.0;
    private static double flowerStepLastX = 0.0;
    private static double flowerStepLastZ = 0.0;
    private static double flowerStepDirectionX = 0.0;
    private static double flowerStepDirectionZ = 0.0;
    private static int flowerStepStalledTicks = 0;
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
    private static BlockPos activeTarget = null;
    private static String activeTargetBlockId = null;
    private static boolean activeTargetStarted = false;
    private static KeyMapping settingsKey;
    private static KeyMapping toggleMiningKey;
    private static KeyMapping toggleFarmingKey;

    @Override
    public void onInitializeClient() {
        try { LegacyConfigMigration.migrate(new File(Minecraft.getInstance().gameDirectory,"config").toPath()); }
        catch (java.io.IOException error) { LOGGER.warn("Kuanperion configuration migration failed; original file retained",error); }
        loadConfig();
        dev.kuan.kuanperion.ui.theme.Theme.loadTheme();
        JerryFarmingMarkerRenderer.register(() -> JERRY_FARMING_MARKERS.isEmpty() ? List.of()
                : JERRY_FARMING_MARKERS.inScope(jerryFarmingMarkerScope(Minecraft.getInstance())));
        KeyMapping.Category category = KeyMapping.Category.register(id("keys"));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuanperion.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_APOSTROPHE, category));
        toggleMiningKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuanperion.toggle_mining", InputConstants.Type.KEYSYM, InputConstants.KEY_M, category));
        toggleFarmingKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuanperion.toggle_farming", InputConstants.Type.KEYSYM, InputConstants.KEY_F, category));
        try {
            var oldKeys=LegacyConfigMigration.legacyKeyOverrides(new File(Minecraft.getInstance().gameDirectory,"options.txt").toPath());
            var bindings=Map.of("settings",settingsKey,"toggle_mining",toggleMiningKey,"toggle_farming",toggleFarmingKey);
            for(var entry:oldKeys.entrySet())bindings.get(entry.getKey()).setKey(InputConstants.getKey(entry.getValue()));
            KeyMapping.resetMapping();
        }catch(java.io.IOException|IllegalArgumentException error){LOGGER.warn("Legacy keybinding migration skipped",error);}
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> dev.kuan.kuanperion.renderer.NanoVGManager.destroy());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            SCREEN_NOTIFICATIONS.clear();
            onFarmingWorldChange();
        });
        HudElementRegistry.addLast(id("notifications"), (graphics, delta) -> renderNotificationOverlay(graphics));
        ClientTickEvents.START_CLIENT_TICK.register(KuanperionClient::enforceAutomationMovementLock);
        ClientTickEvents.END_CLIENT_TICK.register(KuanperionClient::tick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> { if (!overlay) handleReceivedMessage(message); });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleReceivedMessage(message));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean shouldLockManualAttack() {
        return miningEnabled;
    }

    public static boolean shouldLockManualLook() {
        return miningEnabled || farmingEnabled || farmingAutoRecovering;
    }

    private static void tick(Minecraft mc) {
        expireJerryFarmingMarkers();
        if (mc.player == null || mc.level == null) SCREEN_NOTIFICATIONS.clear();
        while (settingsKey.consumeClick()) {
            if (mc.gui.screen() instanceof dev.kuan.kuanperion.ui.MainGUI) mc.gui.screen().onClose();
            else mc.setScreenAndShow(new dev.kuan.kuanperion.ui.MainGUI());
        }
        tickTabNotifications(mc);
        handleShiftHotkeys(mc);
        handleRightClickSelection(mc);
        enforceAutomationMovementLock(mc);
        if (miningEnabled && mc.options != null) mc.options.pauseOnLostFocus = false;
        boolean miningPaused = miningEnabled && isMiningPauseScreen(mc.gui.screen());
        tickSkippedTargets();
        if (!miningPaused && cooldown > 0) cooldown--;
        if (miningEnabled) {
            if (miningPaused) {
                pauseMining(mc);
            } else {
                miningTicks++;
                mineBestTarget(mc);
            }
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
        tickFarmingStallNotification(mc, System.nanoTime());
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
        MINING_GUI_PAUSE_GUARD.reset();
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
            MINING_GUI_PAUSE_GUARD.reset();
            clearActiveTarget(mc);
            farmingEnabled = true;
            farmingEnabledThisSession = true;
            resetFarmingRuntime(mc);
            farmingManualStartPending = true;
            if (farmingAutoSetHome && mc.player != null) mc.player.connection.sendCommand("sethome");
            say(mc, feature("Farming"), plain(" "), success("開啟"), plain("，正在辨識準星指向的作物"));
        }
        saveConfig();
    }

    private static void stopFarming(Minecraft mc, boolean announce) {
        farmingManualStartPending = false;
        FARMING_STALL_GUARD.reset();
        farmingWorldChanged = false;
        lastFarmingLocationDiagnostic = "";
        FARMING_GUI_PAUSE_GUARD.reset();
        FARMING_TOOL_GUARD.reset(null);
        farmingEnabled = false;
        farmingEnabledThisSession = false;
        farmingAutoRecovering = false;
        FARMING_COMMAND_QUEUE.clear();
        releaseFarmingKeys(mc);
        if (announce) sayAutoState(mc, "Farming", false);
    }

    private static void resetFarmingRuntime(Minecraft mc) {
        farmingManualStartPending = false;
        FARMING_STALL_GUARD.reset();
        farmingWorldChanged = false;
        lastFarmingLocationDiagnostic = "";
        FARMING_GUI_PAUSE_GUARD.reset();
        FARMING_TOOL_GUARD.reset(null);
        farmingReverseLane = false;
        farmingStateTicks = 0;
        farmingLeftShiftTicks = 0;
        farmingRightShiftTicks = 0;
        farmingReturningToStart = false;
        farmingMarkerIgnoreTicks = 0;
        farmingCropDetected = false;
        farmingCropProbeTicks = 0;
        pumpkinMelonState = PumpkinMelonState.INACTIVE;
        pumpkinMelonStartRight = false;
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
        farmingRecoveryScanTicks = 0;
        FARMING_COMMAND_QUEUE.clear();
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
        if (farmingCropDetected && pauseFarmingForWrongTool(mc, mc.player)) return;
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

    private static void tickFarmingStallNotification(Minecraft mc, long now) {
        if (!abnormalHandlingEnabled || !farmingEnabled || !farmingEnabledThisSession
                || !farmingCropDetected || farmingAutoRecovering || mc == null
                || mc.player == null || mc.level == null || mc.options == null
                || !mc.player.isAlive() || mc.isPaused() || mc.gui.screen() != null
                || isFarmingPauseScreen(null) || isFarmingIntentionalWait() || pestWebhookUrl.isBlank()) {
            FARMING_STALL_GUARD.reset();
            return;
        }
        // A deliberately changed tool is not an unexpected stall, even with tool detection disabled.
        String heldTool = StringUtil.stripColor(mc.player.getMainHandItem().getHoverName().getString());
        boolean correctTool = FARMING_TOOL_GUARD.matches(heldTool);
        if (FARMING_STALL_GUARD.update(correctTool, mc.player.getX(), mc.player.getZ(), now)) {
            sendDiscordNotification("Farming 異常通知：已連續 5 秒沒有移動，請檢查遊戲狀況。");
        }
    }

    private static boolean isFarmingIntentionalWait() {
        if (farmingStateTicks > 0) return true;
        if (isAlternatingRowCrop(farmingCrop)) {
            return rowCropState == RowCropState.PRIMARY_JUMP_THREE_SECONDS
                    || rowCropState == RowCropState.SECONDARY_JUMP_THREE_SECONDS
                    || rowCropState == RowCropState.WAIT_ONE_SECOND;
        }
        return farmingCrop == FarmingCrop.CACTUS && (cactusState == CactusState.WAIT_BEFORE_HARVEST_LEFT
                || cactusState == CactusState.WAIT_BEFORE_HARVEST_RIGHT);
    }

    private static void onFarmingWorldChange() {
        FARMING_STALL_GUARD.reset();
        if (farmingEnabled && farmingEnabledThisSession && abnormalHandlingEnabled && !farmingAutoRecovering) {
            farmingWorldChanged = true;
        }
    }

    private static boolean handleFarmingAutoRecovery(Minecraft mc) {
        if (!farmingEnabledThisSession || (!abnormalHandlingEnabled && !farmingAutoRecovering)) return false;
        if (finishFarmingRecoveryOnArrival(mc)) return true;
        FarmingLocation location = detectFarmingLocation(mc);
        traceFarmingLocation(mc, location);
        if (farmingAutoRecovering) {
            releaseFarmingKeys(mc);
            return true;
        }
        farmingRecoveryScanTicks++;
        if (farmingRecoveryScanTicks < 20) return false;
        farmingRecoveryScanTicks = 0;
        if (shouldAutoReturn(location)) {
            startFarmingAutoRecovery(mc, location);
            return true;
        }
        return false;
    }

    private static boolean finishFarmingRecoveryOnArrival(Minecraft mc) {
        if (!farmingAutoRecovering || detectFarmingLocation(mc) != FarmingLocation.GARDEN) return false;
        traceFarmingLocation(mc, FarmingLocation.GARDEN);
        stopFarming(mc, false);
        say(mc, feature("Farming"), success(" 已返回 "), locationName("Garden"), plain("，Farming 維持關閉"));
        saveConfig();
        return true;
    }

    private static void startFarmingAutoRecovery(Minecraft mc, FarmingLocation location) {
        if (!shouldAutoReturn(location)) return;
        FARMING_STALL_GUARD.reset();
        farmingWorldChanged = false;
        FARMING_TOOL_GUARD.reset(null);
        farmingAutoRecovering = true;
        farmingEnabled = false;
        farmingRecoveryScanTicks = 0;
        FARMING_COMMAND_QUEUE.clear();
        releaseFarmingKeys(mc);
        say(mc, feature("Farming"), plain(" 偵測到 "), locationName(location.label), warning("，開始自動回傳"));
        if (location == FarmingLocation.LIMBO) {
            enqueueFarmingCommand("lobby", FARMING_RECOVERY_COMMAND_DELAY_TICKS);
            enqueueFarmingCommand("play sb", FARMING_RECOVERY_COMMAND_DELAY_TICKS);
            enqueueFarmingCommand("warp garden", FARMING_RECOVERY_COMMAND_DELAY_TICKS);
        } else if (location == FarmingLocation.LOBBY) {
            enqueueFarmingCommand("play sb", FARMING_RECOVERY_COMMAND_DELAY_TICKS);
            enqueueFarmingCommand("warp garden", FARMING_RECOVERY_COMMAND_DELAY_TICKS);
        } else if (location == FarmingLocation.HUB || location == FarmingLocation.ISLAND) {
            enqueueFarmingCommand("warp garden", FARMING_RECOVERY_COMMAND_DELAY_TICKS);
        }
        saveConfig();
    }

    private static boolean shouldAutoReturn(FarmingLocation location) {
        return abnormalHandlingEnabled && farmingWorldChanged && (location == FarmingLocation.ISLAND
                || location == FarmingLocation.HUB
                || location == FarmingLocation.LOBBY
                || location == FarmingLocation.LIMBO);
    }

    private static void tickFarmingCommandQueue(Minecraft mc) {
        if (finishFarmingRecoveryOnArrival(mc)) return;
        if (FARMING_COMMAND_QUEUE.isEmpty()) return;
        QueuedCommand current = FARMING_COMMAND_QUEUE.peekFirst();
        FarmingLocation location = detectFarmingLocation(mc);
        boolean ready = isFarmingRecoveryCommandReady(mc, current.command, location);
        if (current.ready == null || current.ready != ready) {
            current.ready = ready;
            LOGGER.info("[Kuanperion] Farming recovery command=/{} phase={} location={}",
                    current.command, ready ? "delay_3s" : "wait_location", location);
        }
        if (!ready) {
            // Transfer/loading time must not consume the next command's three-second wait.
            current.ticks = FARMING_RECOVERY_COMMAND_DELAY_TICKS;
            return;
        }
        current.ticks--;
        if (current.ticks > 0) return;
        FARMING_COMMAND_QUEUE.removeFirst();
        LOGGER.info("[Kuanperion] Farming recovery command=/{} phase=dispatch location={}", current.command, location);
        sendCommand(mc, current.command);
    }

    private static boolean isFarmingRecoveryCommandReady(Minecraft mc, String command, FarmingLocation location) {
        if (mc.player == null || mc.level == null) return false;
        return switch (command) {
            case "lobby" -> location == FarmingLocation.LIMBO;
            case "play sb" -> location == FarmingLocation.LOBBY;
            case "warp garden" -> location != FarmingLocation.LOBBY && location != FarmingLocation.LIMBO
                    && (location == FarmingLocation.HUB || location == FarmingLocation.ISLAND
                    || SKYBLOCK_LOCATION_BRIDGE.islandName() != null || containsLine(sidebarLines(mc), "SKYBLOCK"));
            default -> false;
        };
    }

    private static void enqueueFarmingCommand(String command, int delayTicks) {
        FARMING_COMMAND_QUEUE.addLast(new QueuedCommand(command, Math.max(1, delayTicks)));
    }

    private static void sendCommand(Minecraft mc, String command) {
        try { if (mc.player != null) mc.player.connection.sendCommand(command); } catch (Throwable ignored) {}
    }

    private static FarmingLocation detectFarmingLocation(Minecraft mc) {
        if (mc.player == null || mc.level == null) return FarmingLocation.UNKNOWN;
        String island = SKYBLOCK_LOCATION_BRIDGE.islandName();
        if (island != null) {
            return switch (island) {
                case "PRIVATE_ISLAND" -> FarmingLocation.ISLAND;
                case "HUB" -> FarmingLocation.HUB;
                case "GARDEN" -> FarmingLocation.GARDEN;
                default -> FarmingLocation.UNKNOWN;
            };
        }
        List<String> lines = sidebarLines(mc);
        int now = farmingTicksNow();
        if (lines.isEmpty() && now - limboChatSeenTick <= 600) return FarmingLocation.LIMBO;
        if (containsLine(lines, "The Garden") || containsLine(lines, "Garden") || containsLine(lines, "Plot")) return FarmingLocation.GARDEN;
        if (containsLine(lines, "Hub")) return FarmingLocation.HUB;
        if (containsLine(lines, "Private Island")) return FarmingLocation.ISLAND;
        if (isHoldingGameMenu(mc)) return FarmingLocation.LOBBY;
        return FarmingLocation.UNKNOWN;
    }

    private static void traceFarmingLocation(Minecraft mc, FarmingLocation location) {
        String apiIsland = SKYBLOCK_LOCATION_BRIDGE.islandName();
        String diagnostic = location.name() + ":" + apiIsland + ":" + farmingWorldChanged;
        if (diagnostic.equals(lastFarmingLocationDiagnostic)) return;
        lastFarmingLocationDiagnostic = diagnostic;
        LOGGER.info("[Kuanperion] Farming location={} skyblockApi={} worldChanged={} nativeSidebar={}",
                location, apiIsland, farmingWorldChanged, sidebarLines(mc));
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
        String keyword = cleanSidebarText(needle);
        if (keyword.isEmpty()) return false;
        for (String line : lines) if (cleanSidebarText(line).contains(keyword)) return true;
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
                PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
                String text = PlayerTeam.formatNameForTeam(team, score.ownerName()).getString();
                lines.add(cleanSidebarText(text));
            }
        } catch (Throwable ignored) {}
        return lines;
    }

    private static String cleanSidebarText(String text) {
        if (text == null) return "";
        return StringUtil.stripColor(text).replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);
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

    private static boolean isConfiguredBlockNear(Minecraft mc, LocalPlayer player, String blockId) {
        if (blockId == null || blockId.isBlank()) return false;
        return isBlockNear(mc, player, blockId, 1, 0, 1);
    }

    private static void beginPumpkinMelonFromLane(LocalPlayer player, String cropBlockId) {
        pumpkinMelonStartRight = PumpkinMelonLaneStart.startsRight(cropBlockId, player.getX());
        pumpkinMelonMarkerCleared = false;
        beginPumpkinMelonStep(player, PumpkinMelonState.START_FORWARD_ONE);
    }

    private static void tickPumpkinMelon(Minecraft mc, LocalPlayer player, FarmingBlockProfile profile) {
        if (pumpkinMelonState == PumpkinMelonState.INACTIVE) beginPumpkinMelonStep(player, PumpkinMelonState.START_FORWARD_ONE);
        switch (pumpkinMelonState) {
            case START_FORWARD_ONE -> {
                applyFarmingInput(mc, DirectionKey.FORWARD, true, false);
                if (pumpkinMelonStepDone(player)) {
                    pumpkinMelonState = pumpkinMelonStartRight ? PumpkinMelonState.HARVEST_RIGHT : PumpkinMelonState.HARVEST_LEFT;
                    applyFarmingInput(mc, pumpkinMelonStartRight ? DirectionKey.RIGHT : DirectionKey.LEFT, true, false);
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
                FARMING_TOOL_GUARD.reset(expectedTool);
                if (farmingToolDetectionEnabled && expectedTool != null) {
                    String heldTool = StringUtil.stripColor(player.getMainHandItem().getHoverName().getString());
                    if (!FARMING_TOOL_GUARD.matches(heldTool)) {
                        say(mc, feature("Farming"), error(" 工具錯誤："), cropName(detected.label), plain(" 需要 "),
                                warning(expectedTool));
                        stopFarming(mc, false);
                        saveConfig();
                        return true;
                    }
                }
                farmingCrop = detected;
                farmingCropDetected = true;
                if (farmingManualStartPending) {
                    clearJerryFarmingMarkersOnStart(mc);
                    farmingManualStartPending = false;
                }
                farmingReverseLane = false;
                pumpkinMelonMarkerCleared = false;
                if (detected == FarmingCrop.PUMPKIN_MELON) beginPumpkinMelonFromLane(player, cropBlockId);
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

    private static boolean pauseFarmingForWrongTool(Minecraft mc, LocalPlayer player) {
        boolean wasPaused = FARMING_TOOL_GUARD.isPaused();
        String heldTool = StringUtil.stripColor(player.getMainHandItem().getHoverName().getString());
        if (!FARMING_TOOL_GUARD.update(farmingToolDetectionEnabled, heldTool)) return false;
        releaseFarmingKeys(mc);
        if (!wasPaused) {
            say(mc, feature("Farming"), warning(" 工具錯誤，已暫停：請切換回 "),
                    warning(FARMING_TOOL_GUARD.expectedTool()), plain(" 後繼續，等待不限時"));
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
        return FARMING_GUI_PAUSE_GUARD.shouldPause(farmingMenuPauseEnabled, screen != null, System.nanoTime());
    }

    private static boolean isMiningPauseScreen(Screen screen) {
        return MINING_GUI_PAUSE_GUARD.shouldPause(miningMenuPauseEnabled, screen != null, System.nanoTime());
    }

    private static void pauseMining(Minecraft mc) {
        setAttackKey(mc, false);
        if (!activeTargetStarted) return;
        activeTargetStarted = false;
        try { if (mc != null && mc.gameMode != null) mc.gameMode.stopDestroyBlock(); } catch (Throwable ignored) {}
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
        farmingDesiredForward = forward;
        farmingDesiredBack = back;
        farmingDesiredLeft = left;
        farmingDesiredRight = right;
        farmingDesiredJump = jump;
        setAttackKey(mc, attack);
        applyLockedMovement(mc, forward, back, left, right, jump);
    }

    private static void applyLockedMovement(Minecraft mc, boolean forward, boolean back, boolean left,
                                            boolean right, boolean jump) {
        if (mc == null || mc.options == null) return;
        mc.options.keyUp.setDown(forward);
        mc.options.keyDown.setDown(back);
        mc.options.keyLeft.setDown(left);
        mc.options.keyRight.setDown(right);
        mc.options.keyJump.setDown(jump);
        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
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
        farmingDesiredForward = false;
        farmingDesiredBack = false;
        farmingDesiredLeft = false;
        farmingDesiredRight = false;
        farmingDesiredJump = false;
        applyLockedMovement(mc, false, false, false, false, false);
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

    private static void enforceAutomationMovementLock(Minecraft mc) {
        if (mc == null || mc.player == null || mc.options == null) return;
        if (finishFarmingRecoveryOnArrival(mc)) return;
        if (miningEnabled || farmingAutoRecovering) {
            applyLockedMovement(mc, false, false, false, false, false);
            Vec3 velocity = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(0.0, velocity.y, 0.0);
            return;
        }
        if (!farmingEnabled) return;
        if (isFarmingPauseScreen(mc.gui.screen())) {
            applyLockedMovement(mc, false, false, false, false, false);
            return;
        }
        applyLockedMovement(mc, farmingDesiredForward, farmingDesiredBack, farmingDesiredLeft,
                farmingDesiredRight, farmingDesiredJump);
    }

    private static void handleRightClickSelection(Minecraft mc) {
        if (!selectMode || mc.player == null || mc.level == null || mc.getWindow() == null) {
            rightWasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (down && !rightWasDown && mc.hitResult instanceof BlockHitResult bhr) {
            BlockState state = mc.level.getBlockState(bhr.getBlockPos());
            String name = blockId(state);
            if (!state.isAir() && isAllowedTargetBlock(name)) {
                if (!TARGET_BLOCKS.contains(name)) {
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
        // Finish the current block; every subsequent selection starts at highest priority.
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
            if (!isAllowedTargetBlock(id) || !TARGET_BLOCKS.contains(id)) continue;
            int priority = TARGET_BLOCKS.indexOf(id);
            String waitingFor = DEPLETED_TARGETS.get(pos);
            if (waitingFor != null) {
                int previousPriority = TARGET_BLOCKS.indexOf(waitingFor);
                // A higher-priority respawn is eligible even when its type differs.
                // Still ignore lower-priority depleted forms until the original returns.
                if (!waitingFor.equals(id) && previousPriority >= 0 && priority >= previousPriority) continue;
                DEPLETED_TARGETS.remove(pos);
            }
            double dist = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist > (r + 1.5) * (r + 1.5)) continue;
            if (!canSeeBlockCenter(player, mc, pos, r + 1.5)) continue;
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

        if (!lookAtSmooth(player, target)) return true;
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
        String text = "[Kuanperion]";
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
        if (handleJerryMessage(Minecraft.getInstance(), text)) return;
        int now = farmingTicksNow();
        if (text.contains("You fell into the void") && abnormalHandlingEnabled && farmingEnabled
                && farmingEnabledThisSession && !farmingAutoRecovering) {
            stopFarming(Minecraft.getInstance(), true);
            return;
        }
        if (text.contains("/limbo for more information")) {
            limboChatSeenTick = now;
            if (abnormalHandlingEnabled && farmingEnabled && farmingEnabledThisSession
                    && !farmingAutoRecovering && sidebarLines(Minecraft.getInstance()).isEmpty()) {
                startFarmingAutoRecovery(Minecraft.getInstance(), FarmingLocation.LIMBO);
                return;
            }
        }
    }

    private static String jerryFarmingMarkerScope(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) return null;
        String server;
        String place;
        if (mc.getCurrentServer() != null) {
            server = "server:" + mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            FarmingLocation location = detectFarmingLocation(mc);
            if (location == FarmingLocation.UNKNOWN) return null;
            place = location.name();
        } else if (mc.getSingleplayerServer() != null) {
            server = "local:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            place = "LOCAL";
        } else return null;
        return server + "|" + mc.level.dimension().identifier() + "|" + place;
    }

    private static void expireJerryFarmingMarkers() {
        if (!JERRY_FARMING_MARKERS.isEmpty() && JERRY_FARMING_MARKERS.expire(jerryMarkerMaxSeconds)) saveConfig();
    }

    private static void markJerryFarmingPause(Minecraft mc) {
        String scope = jerryFarmingMarkerScope(mc);
        if (scope != null) JERRY_FARMING_MARKERS.add(scope, mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private static void clearJerryFarmingMarkersOnStart(Minecraft mc) {
        String scope = jerryFarmingMarkerScope(mc);
        if (scope != null) JERRY_FARMING_MARKERS.resumeNear(scope, mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private static boolean handleJerryMessage(Minecraft mc, String text) {
        JerryColor color = JerryMessageMatcher.matchingColor(text, jerryColors);
        if (color == null) return false;
        // Stop first: missing/invalid webhooks must not keep automation running.
        if (jerryStopActionsEnabled && (miningEnabled || farmingEnabled || farmingAutoRecovering)) {
            boolean stoppedFarming = farmingEnabled;
            miningEnabled = false;
            MINING_GUI_PAUSE_GUARD.reset();
            clearActiveTarget(mc);
            stopFarming(mc, false);
            if (stoppedFarming) markJerryFarmingPause(mc);
            saveConfig();
        }
        String announcement = "發現 " + color.label + " Jerry！";
        sendNotification(NotificationType.JERRY, "Jerry 通知：" + announcement, announcement);
        return true;
    }

    private static void tickTabNotifications(Minecraft mc) {
        if (!pestNotificationEnabled && !visitorNotificationEnabled) return;
        if (mc.player == null || mc.level == null) return;
        if (!(pestNotificationEnabled && hasNotificationChannel(NotificationType.PESTS))
                && !(visitorNotificationEnabled && hasNotificationChannel(NotificationType.VISITORS))) return;
        if (++tabNotificationTicks < 20) return;
        tabNotificationTicks = 0;
        GardenTabNotifications.Counts counts = GardenTabNotifications.parse(GardenTabReader.read(mc));
        if (hasNotificationChannel(NotificationType.PESTS) && TAB_NOTIFICATIONS.pestReached(pestNotificationEnabled, counts.pests(), pestNotificationThreshold)) {
            sendNotification(NotificationType.PESTS, "Garden Pest 通知：目前 " + counts.pests()
                    + " 隻（通知門檻：" + pestNotificationThreshold + "）。", "Pest：" + counts.pests() + " 隻");
        }
        if (hasNotificationChannel(NotificationType.VISITORS) && TAB_NOTIFICATIONS.visitorsReached(visitorNotificationEnabled, counts.visitors(), visitorNotificationThreshold)) {
            sendNotification(NotificationType.VISITORS, "Garden Visitor 通知：目前 " + counts.visitors()
                    + " 位（通知門檻：" + visitorNotificationThreshold + "）。", "Visitor：" + counts.visitors() + " 位");
        }
    }

    private static NotificationChannels notificationChannels(NotificationType type) {
        return NOTIFICATION_CHANNELS.getOrDefault(type, NotificationChannels.NONE);
    }

    private static boolean hasNotificationChannel(NotificationType type) {
        return notificationChannels(type).available(pestWebhookUrl);
    }

    private static void sendNotification(NotificationType type, String content, String screenText) {
        NotificationChannels channels = notificationChannels(type);
        if (channels.discord() && !pestWebhookUrl.isBlank()) sendDiscordNotification(content);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (channels.chat()) say(mc, plain(content));
        if (channels.screen()) SCREEN_NOTIFICATIONS.add(type, screenText, channels.screenSeconds(), System.nanoTime());
    }

    private static void renderNotificationOverlay(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            SCREEN_NOTIFICATIONS.clear();
            return;
        }
        if (mc.gui.hud.isHidden()) return;
        String text = SCREEN_NOTIFICATIONS.current(System.nanoTime());
        if (text == null) return;
        int textWidth = mc.font.width(text);
        float scale = Math.min(4.0f, Math.max(0.1f, (graphics.guiWidth() - 32.0f) / Math.max(1, textWidth)));
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(graphics.guiWidth() / 2.0f, graphics.guiHeight() / 2.0f);
            graphics.pose().scale(scale, scale);
            graphics.text(mc.font, text, -textWidth / 2, -mc.font.lineHeight / 2, 0xFFFFFF55, true);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    /** The main screen owns Visitor/Pest master switches; their dialogs only edit preferences. */
    private static void toggleTabNotification(SettingsScreen parent, NotificationType type) {
        if (type == NotificationType.JERRY) throw new IllegalArgumentException("Jerry has no master switch");
        boolean enabled;
        if (type == NotificationType.PESTS) {
            pestNotificationEnabled = !pestNotificationEnabled;
            enabled = pestNotificationEnabled;
            TAB_NOTIFICATIONS.resetPests();
        } else {
            visitorNotificationEnabled = !visitorNotificationEnabled;
            enabled = visitorNotificationEnabled;
            TAB_NOTIFICATIONS.resetVisitors();
        }
        if (!enabled) SCREEN_NOTIFICATIONS.remove(type, System.nanoTime());
        saveConfig();
        if (enabled) openNotificationSettings(parent, type);
        else parent.rebuild();
    }

    private static void openNotificationSettings(SettingsScreen parent, NotificationType type) {
        int threshold = switch (type) { case PESTS -> pestNotificationThreshold; case VISITORS -> visitorNotificationThreshold; case JERRY -> 0; };
        NotificationSettingsScreen.Options initial = new NotificationSettingsScreen.Options(
                notificationChannels(type), threshold, jerryStopActionsEnabled, jerryColors, jerryMarkerMaxSeconds);
        Minecraft.getInstance().setScreenAndShow(new NotificationSettingsScreen(parent, type, initial,
                () -> pestWebhookUrl, value -> pestWebhookUrl = value, options -> {
            NOTIFICATION_CHANNELS.put(type, options.channels());
            if (!options.channels().screen())
                SCREEN_NOTIFICATIONS.remove(type, System.nanoTime());
            if (type == NotificationType.PESTS) {
                if (pestNotificationThreshold != options.threshold()) TAB_NOTIFICATIONS.resetPests();
                pestNotificationThreshold = options.threshold();
            } else if (type == NotificationType.VISITORS) {
                if (visitorNotificationThreshold != options.threshold()) TAB_NOTIFICATIONS.resetVisitors();
                visitorNotificationThreshold = options.threshold();
            } else {
                jerryStopActionsEnabled = options.stopActions();
                jerryColors = options.colors();
                jerryMarkerMaxSeconds = options.markerMaxSeconds();
            }
            saveConfig();
        }));
    }

    private static void loadNotificationConfig(Properties p) {
        jerryMarkerMaxSeconds = JerryFarmingPauseMarkers.parseMaxSeconds(p.getProperty("jerryMarkerMaxSeconds"));
        if (jerryMarkerMaxSeconds == null) jerryMarkerMaxSeconds = JerryFarmingPauseMarkers.DEFAULT_MAX_SECONDS;
        EnumSet<JerryColor> colors = EnumSet.noneOf(JerryColor.class);
        for (JerryColor color : JerryColor.values()) {
            if (Boolean.parseBoolean(p.getProperty(color.configKey, "true"))) colors.add(color);
        }
        jerryColors = Set.copyOf(colors);
        pestWebhookUrl = p.getProperty("pestWebhookUrl", "").trim();
        NotificationChannels legacy = new NotificationChannels(
                Boolean.parseBoolean(p.getProperty("discordNotificationEnabled", Boolean.toString(!pestWebhookUrl.isBlank()))),
                Boolean.parseBoolean(p.getProperty("chatNotificationEnabled", "false")),
                Boolean.parseBoolean(p.getProperty("screenNotificationEnabled", "false")));
        boolean legacyJerry = Boolean.parseBoolean(p.getProperty("jerryDetectionEnabled", "false"));
        boolean legacyJerryNotify = Boolean.parseBoolean(p.getProperty("jerryNotificationEnabled",
                p.getProperty("jerryDiscordNotificationEnabled", "true")));
        boolean newSettings = "2".equals(p.getProperty("notificationSettingsVersion"));
        for (NotificationType type : NotificationType.values()) {
            NotificationChannels defaults = newSettings || (type == NotificationType.JERRY && !(legacyJerry && legacyJerryNotify))
                    ? NotificationChannels.NONE : legacy;
            String key = type.configKey;
            var seconds = NotificationChannels.parseScreenSeconds(p.getProperty(key + "ScreenSeconds"));
            if (seconds == null) seconds = NotificationChannels.DEFAULT_SCREEN_SECONDS;
            NOTIFICATION_CHANNELS.put(type, new NotificationChannels(
                    Boolean.parseBoolean(p.getProperty(key + "DiscordEnabled", Boolean.toString(defaults.discord()))),
                    Boolean.parseBoolean(p.getProperty(key + "ChatEnabled", Boolean.toString(defaults.chat()))),
                    Boolean.parseBoolean(p.getProperty(key + "ScreenEnabled", Boolean.toString(defaults.screen()))), seconds));
        }
        jerryStopActionsEnabled = (newSettings || legacyJerry)
                && Boolean.parseBoolean(p.getProperty("jerryStopActionsEnabled", newSettings ? "false" : "true"));
    }

    private static void saveNotificationConfig(Properties p) {
        p.setProperty("jerryMarkerMaxSeconds", jerryMarkerMaxSeconds.toPlainString());
        for (JerryColor color : JerryColor.values()) {
            p.setProperty(color.configKey, Boolean.toString(jerryColors.contains(color)));
        }
        p.setProperty("notificationSettingsVersion", "2");
        p.setProperty("pestWebhookUrl", pestWebhookUrl == null ? "" : pestWebhookUrl.trim());
        p.setProperty("jerryStopActionsEnabled", Boolean.toString(jerryStopActionsEnabled));
        for (NotificationType type : NotificationType.values()) {
            String key = type.configKey;
            NotificationChannels channels = notificationChannels(type);
            p.setProperty(key + "DiscordEnabled", Boolean.toString(channels.discord()));
            p.setProperty(key + "ChatEnabled", Boolean.toString(channels.chat()));
            p.setProperty(key + "ScreenEnabled", Boolean.toString(channels.screen()));
            p.setProperty(key + "ScreenSeconds", channels.screenSeconds().toPlainString());
        }
    }

    private static void sendDiscordNotification(String content) {
        try {
            String json = "{\"content\":\"" + jsonEscape(content) + "\"}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(pestWebhookUrl.trim()))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((response, error) -> {
                // Never log the webhook URL or exception message, which can contain its secret token.
                if (error != null) LOGGER.warn("[Kuanperion] Discord notification request failed");
                else if (response.statusCode() < 200 || response.statusCode() >= 300)
                    LOGGER.warn("[Kuanperion] Discord notification HTTP status={}", response.statusCode());
            });
        } catch (IllegalArgumentException error) {
            LOGGER.warn("[Kuanperion] Invalid Discord notification webhook URL");
        }
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
        pestNotificationThreshold = GardenTabNotifications.parseThreshold(p.getProperty("pestNotificationThreshold", "1"));
        if (pestNotificationThreshold == 0) pestNotificationThreshold = 1;
        visitorNotificationEnabled = Boolean.parseBoolean(p.getProperty("visitorNotificationEnabled", "false"));
        visitorNotificationThreshold = GardenTabNotifications.parseVisitorThreshold(p.getProperty("visitorNotificationThreshold", "5"));
        if (visitorNotificationThreshold == 0) visitorNotificationThreshold = 5;
        farmingMenuPauseEnabled = Boolean.parseBoolean(p.getProperty("farmingMenuPauseEnabled", "true"));
        miningMenuPauseEnabled = Boolean.parseBoolean(p.getProperty("miningMenuPauseEnabled", "true"));
        farmingToolDetectionEnabled = Boolean.parseBoolean(p.getProperty("farmingToolDetectionEnabled", "false"));
        boolean legacyAbnormalHandling = Boolean.parseBoolean(p.getProperty("islandReturnEnabled", "true"))
                || Boolean.parseBoolean(p.getProperty("hubReturnEnabled", "true"))
                || Boolean.parseBoolean(p.getProperty("lobbyReturnEnabled", "true"))
                || Boolean.parseBoolean(p.getProperty("limboReturnEnabled", "true"));
        abnormalHandlingEnabled = Boolean.parseBoolean(p.getProperty("abnormalHandlingEnabled",
                Boolean.toString(legacyAbnormalHandling)));
        loadNotificationConfig(p);
        JERRY_FARMING_MARKERS.load(p);
        if (miningEnabled && farmingEnabled) farmingEnabled = false;
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
        // Persist legacy marker timestamps once; never restart their timeout on the next launch.
        if (!JERRY_FARMING_MARKERS.isEmpty()) {
            JERRY_FARMING_MARKERS.expire(jerryMarkerMaxSeconds);
            saveConfig();
        }
    }

    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("miningEnabled", "false");
        p.setProperty("farmingEnabled", "false");
        p.setProperty("farmingCrop", farmingCrop.name());
        p.setProperty("farmingAutoSetHome", Boolean.toString(farmingAutoSetHome));
        p.setProperty("pestNotificationEnabled", Boolean.toString(pestNotificationEnabled));
        p.setProperty("pestNotificationThreshold", Integer.toString(pestNotificationThreshold));
        p.setProperty("visitorNotificationEnabled", Boolean.toString(visitorNotificationEnabled));
        p.setProperty("visitorNotificationThreshold", Integer.toString(visitorNotificationThreshold));
        p.setProperty("farmingMenuPauseEnabled", Boolean.toString(farmingMenuPauseEnabled));
        p.setProperty("miningMenuPauseEnabled", Boolean.toString(miningMenuPauseEnabled));
        p.setProperty("farmingToolDetectionEnabled", Boolean.toString(farmingToolDetectionEnabled));
        p.setProperty("abnormalHandlingEnabled", Boolean.toString(abnormalHandlingEnabled));
        saveNotificationConfig(p);
        JERRY_FARMING_MARKERS.save(p);
        p.setProperty("scanRadius", Integer.toString(scanRadius));
        p.setProperty("rotationStepDegrees", Integer.toString(rotationStepDegrees));
        p.setProperty("actionDelayTicks", Integer.toString(actionDelayTicks));
        p.setProperty("targets", String.join(",", TARGET_BLOCKS));
        try (FileOutputStream out = new FileOutputStream(configFile())) { p.store(out, "Kuanperion"); } catch (Throwable ignored) {}
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
        Boolean ready;
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

    private static FarmingBlockProfile farmingProfile() {
        return FARMING_BLOCK_PROFILE;
    }

    private static final class FarmingBlockProfile {
        final boolean rowDetection = true;
        final boolean startDetection = true;
        final boolean endDetection = true;
        final String rowBlock = "minecraft:yellow_stained_glass";
        final String startBlock = "minecraft:light_blue_stained_glass";
        final String endBlock = "minecraft:pink_stained_glass";
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
            if (priority != other.priority) return priority < other.priority;
            return dist < other.dist;
        }
    }

    /** Export editable values only: never Webhook secrets, marker locations or runtime toggles. */
    public static com.google.gson.JsonObject exportUiConfig() {
        saveConfig();
        Properties values=new Properties();
        try(var input=new FileInputStream(configFile())) {values.load(input);}catch(java.io.IOException ex){throw new IllegalStateException("Cannot read settings",ex);}
        var out=new com.google.gson.JsonObject();
        for(String key:values.stringPropertyNames())if(profileKeyAllowed(key))out.addProperty(key,values.getProperty(key));
        return out;
    }
    private static boolean profileKeyAllowed(String key) {
        return !key.equals("pestWebhookUrl")&&!key.equals("miningEnabled")&&!key.equals("farmingEnabled")&&!key.startsWith("jerryFarmingPauseMarker");
    }
    public static boolean applyUiConfig(com.google.gson.JsonObject input) {
        Properties values=new Properties();
        try {
            saveConfig();
            try(var old=new FileInputStream(configFile())){values.load(old);}
            for(var entry:input.entrySet()) {
                if(!profileKeyAllowed(entry.getKey())||!values.containsKey(entry.getKey()))continue;
                if(!entry.getValue().isJsonPrimitive()||!entry.getValue().getAsJsonPrimitive().isString())return false;
                values.setProperty(entry.getKey(),entry.getValue().getAsString());
            }
            java.nio.file.Path target=configFile().toPath();
            java.nio.file.Path staging=java.nio.file.Files.createTempFile(target.getParent(),"kuanperion-profile-",".properties");
            try {
                try(var out=java.nio.file.Files.newOutputStream(staging)){values.store(out,"Kuanperion");}
                java.nio.file.Files.move(staging,target,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }finally{java.nio.file.Files.deleteIfExists(staging);}
            miningEnabled=false;selectMode=false;clearActiveTarget(Minecraft.getInstance());stopFarming(Minecraft.getInstance(),false);
            SCREEN_NOTIFICATIONS.clear();TAB_NOTIFICATIONS.resetPests();TAB_NOTIFICATIONS.resetVisitors();
            loadConfig();return true;
        }catch(java.io.IOException|IllegalArgumentException ex){LOGGER.warn("Cannot apply UI profile",ex);return false;}
    }

    /** Presentation-only bindings: every callback uses the existing automation/config state. */
    public static List<MainGUIRegistry.ModuleSection> uiSections() {
        var farming = SettingGroup.alwaysOn("農耕設定", "保留原本農場標記與作物辨識，不改變農耕流程")
            .add(new ToggleSetting("工具偵測", () -> farmingToolDetectionEnabled, v -> { farmingToolDetectionEnabled=v; saveConfig(); }))
            .add(new ToggleSetting("智慧暫停", () -> farmingMenuPauseEnabled, v -> { farmingMenuPauseEnabled=v; saveConfig(); }))
            .add(new ToggleSetting("異常狀況處理", () -> abnormalHandlingEnabled, v -> { abnormalHandlingEnabled=v; farmingWorldChanged=false; saveConfig(); }));
        var farmTab = new ModulesTab.SubTab("Farming", "沿用現有農場路線", () -> farmingEnabled,
            v -> { if (v!=farmingEnabled) toggleFarming(Minecraft.getInstance()); }, List.of(farming));
        var mining = SettingGroup.alwaysOn("挖礦設定", "目標種類優先序、偵測範圍與操作節奏")
            .add(new ToggleSetting("智慧暫停", () -> miningMenuPauseEnabled, v -> { miningMenuPauseEnabled=v; saveConfig(); }))
            .add(new SliderSetting("偵測半徑",1,8,()->(float)scanRadius,v->{scanRadius=Math.round(v);saveConfig();}).withDecimals(0))
            .add(new SliderSetting("轉動速度",1,40,()->(float)rotationStepDegrees,v->{rotationStepDegrees=Math.round(v);saveConfig();}).withDecimals(0).withSuffix("°"))
            .add(new SliderSetting("挖礦延遲",1,20,()->(float)actionDelayTicks,v->{actionDelayTicks=Math.round(v);saveConfig();}).withDecimals(0).withSuffix(" ticks"));
        var targets = SettingGroup.alwaysOn("目標方塊", "選擇 1～5 種方塊；排列順序即挖礦優先序")
            .add(new SliderSetting("選擇種類數量",1,5,()->(float)miningSelectionTargetCount,v->miningSelectionTargetCount=Math.round(v)).withDecimals(0))
            .add(new ActionSetting("開始選擇目標方塊", KuanperionClient::beginUiTargetSelection));
        for (int i=0;i<5;i++) {
            final int index=i;
            targets.add(new InfoSetting("優先序 "+(i+1),()->index<TARGET_BLOCKS.size()?TARGET_BLOCKS.get(index):"未設定"));
            targets.add(new ActionSetting("↑ 優先序 "+(i+1),()->moveUiTarget(index,-1)).visibleWhen(()->index>0&&index<TARGET_BLOCKS.size()));
            targets.add(new ActionSetting("↓ 優先序 "+(i+1),()->moveUiTarget(index,1)).visibleWhen(()->index+1<TARGET_BLOCKS.size()));
            targets.add(new ActionSetting("移除優先序 "+(i+1),()->{ if(index<TARGET_BLOCKS.size()){TARGET_BLOCKS.remove(index);saveConfig();}}).visibleWhen(()->index<TARGET_BLOCKS.size()));
        }
        var mineTab=new ModulesTab.SubTab("Mining", "依種類優先序挖掘",()->miningEnabled,
            v->{if(v!=miningEnabled)toggleMining(Minecraft.getInstance());},List.of(mining,targets));
        var pestTab=new ModulesTab.SubTab("Pest 通知", "依數量通知，不自動除蟲",()->pestNotificationEnabled,
            v->setUiNotificationEnabled(NotificationType.PESTS,v),List.of(uiNotificationGroup(NotificationType.PESTS)));
        var visitorTab=new ModulesTab.SubTab("Visitor 通知", "訪客達門檻時通知",()->visitorNotificationEnabled,
            v->setUiNotificationEnabled(NotificationType.VISITORS,v),List.of(uiNotificationGroup(NotificationType.VISITORS)));
        var jerry=SettingGroup.alwaysOn("Jerry 偵測", "分別選擇顏色、停止動作與 Farming 光柱期限");
        for(JerryColor color:JerryColor.values()) jerry.add(new ToggleSetting(color.label,()->jerryColors.contains(color),v->{
            EnumSet<JerryColor> colors=EnumSet.noneOf(JerryColor.class);colors.addAll(jerryColors);if(v)colors.add(color);else colors.remove(color);jerryColors=Set.copyOf(colors);saveConfig();}));
        jerry.add(new ToggleSetting("出現 Jerry 是否停下動作",()->jerryStopActionsEnabled,v->{jerryStopActionsEnabled=v;saveConfig();}));
        jerry.add(new TextSetting("光柱最久存在時間（秒）", "正小數，預設 300，不設上限",()->jerryMarkerMaxSeconds.toPlainString(),text->{
            BigDecimal parsed=JerryFarmingPauseMarkers.parseMaxSeconds(text);
            if(parsed!=null){jerryMarkerMaxSeconds=parsed;saveConfig();}else uiInputError("請輸入大於 0 的秒數");}));
        var jerryTab=new ModulesTab.SubTab("Jerry 偵測", "四色篩選、通知與停止動作",List.of(jerry,uiNotificationGroup(NotificationType.JERRY)));
        var webhook=SettingGroup.alwaysOn("共用 Webhook", "只用於你啟用的 Discord 通知；不會匯出到設定檔")
            .add(new TextSetting("Webhook 網址", "http(s)://…",()->pestWebhookUrl,text->{
                String value=text.trim();
                try { URI uri=URI.create(value); if(!value.isEmpty()&&((!"https".equalsIgnoreCase(uri.getScheme())&&!"http".equalsIgnoreCase(uri.getScheme()))||uri.getHost()==null||uri.getUserInfo()!=null))throw new IllegalArgumentException();
                    pestWebhookUrl=value;saveConfig();
                }catch(IllegalArgumentException ex){uiInputError("Webhook 網址無效，原值未變更");}}));
        var webhookTab=new ModulesTab.SubTab("Discord 通知", "現有共用 Webhook 設定；沒有遠端控制",List.of(webhook));
        return List.of(new MainGUIRegistry.ModuleSection("farming","Farming",List.of(farmTab,pestTab,visitorTab)),
            new MainGUIRegistry.ModuleSection("mining","Mining",List.of(mineTab)),
            new MainGUIRegistry.ModuleSection("other","Other",List.of(jerryTab,webhookTab)));
    }

    private static SettingGroup uiNotificationGroup(NotificationType type) {
        var group=SettingGroup.alwaysOn("通知方式", "三種通知獨立開關；各功能互不影響");
        if(type!=NotificationType.JERRY) group.add(new SliderSetting("通知門檻",1,type==NotificationType.PESTS?8:5,
            ()->(float)(type==NotificationType.PESTS?pestNotificationThreshold:visitorNotificationThreshold),v->{
                if(type==NotificationType.PESTS){pestNotificationThreshold=Math.round(v);TAB_NOTIFICATIONS.resetPests();}
                else{visitorNotificationThreshold=Math.round(v);TAB_NOTIFICATIONS.resetVisitors();}saveConfig();}).withDecimals(0));
        group.add(new ToggleSetting("Discord 訊息通知",()->notificationChannels(type).discord(),v->{
            if(v&&pestWebhookUrl.isBlank()){uiInputError("請先在 Discord 通知設定 Webhook 網址");return;}
            var old=notificationChannels(type);setUiChannels(type,new NotificationChannels(v,old.chat(),old.screen(),old.screenSeconds()));}));
        group.add(new ToggleSetting("遊戲內聊天室通知",()->notificationChannels(type).chat(),v->{var old=notificationChannels(type);setUiChannels(type,new NotificationChannels(old.discord(),v,old.screen(),old.screenSeconds()));}));
        group.add(new ToggleSetting("遊戲內螢幕通知",()->notificationChannels(type).screen(),v->{var old=notificationChannels(type);setUiChannels(type,new NotificationChannels(old.discord(),old.chat(),v,old.screenSeconds()));}));
        group.add(new TextSetting("螢幕通知顯示時間（秒）","大於 0，可含小數",()->notificationChannels(type).screenSeconds().toPlainString(),text->{
            BigDecimal seconds=NotificationChannels.parseScreenSeconds(text);
            if(seconds==null){uiInputError("請輸入有效正小數秒數");return;}
            var old=notificationChannels(type);setUiChannels(type,new NotificationChannels(old.discord(),old.chat(),old.screen(),seconds));}));
        return group;
    }
    private static void setUiChannels(NotificationType type,NotificationChannels channels) {
        NOTIFICATION_CHANNELS.put(type,channels);
        if(!channels.screen())SCREEN_NOTIFICATIONS.remove(type,System.nanoTime());
        saveConfig();
    }
    private static void setUiNotificationEnabled(NotificationType type,boolean enabled) {
        if(type==NotificationType.PESTS){pestNotificationEnabled=enabled;TAB_NOTIFICATIONS.resetPests();}
        else{visitorNotificationEnabled=enabled;TAB_NOTIFICATIONS.resetVisitors();}
        if(!enabled)SCREEN_NOTIFICATIONS.remove(type,System.nanoTime());
        saveConfig();
    }
    private static void uiInputError(String message) { dev.kuan.kuanperion.notification.NotificationManager.error("設定未套用",message); }
    private static void beginUiTargetSelection() {
        miningSelectionTargetCount=Math.max(1,Math.min(5,miningSelectionTargetCount));
        miningEnabled=false;clearActiveTarget(Minecraft.getInstance());TARGET_BLOCKS.clear();selectMode=true;saveConfig();
        Minecraft.getInstance().setScreenAndShow(null);
        say(Minecraft.getInstance(),feature("Mining"),plain(" 方塊選擇：請右鍵選擇 "),warning(Integer.toString(miningSelectionTargetCount)),plain(" 種目標方塊"));
    }
    private static void moveUiTarget(int index,int delta) {
        int next=index+delta;
        if(index<0||index>=TARGET_BLOCKS.size()||next<0||next>=TARGET_BLOCKS.size())return;
        String target=TARGET_BLOCKS.remove(index);TARGET_BLOCKS.add(next,target);saveConfig();
    }
    public static boolean matchesSettingsKey(net.minecraft.client.input.KeyEvent event) {
        return settingsKey != null && settingsKey.matches(event);
    }

    public static List<Setting> uiKeybinds() {
        return List.of(new KeybindSetting("開啟設定",settingsKey),
            new KeybindSetting("Mining（搭配 Shift）",toggleMiningKey),
            new KeybindSetting("Farming（搭配 Shift）",toggleFarmingKey));
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
            super(Component.literal("Kuanperion"));
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
            addRenderableWidget(Button.builder(toggleMsg("異常狀況處理", abnormalHandlingEnabled), b -> { abnormalHandlingEnabled = !abnormalHandlingEnabled; farmingWorldChanged = false; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(Button.builder(toggleMsg("Visitor 通知" + (visitorNotificationEnabled ? " ≥" + visitorNotificationThreshold : ""), visitorNotificationEnabled), b -> toggleTabNotification(this, NotificationType.VISITORS)).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("Pest 通知" + (pestNotificationEnabled ? " ≥" + pestNotificationThreshold : ""), pestNotificationEnabled), b -> toggleTabNotification(this, NotificationType.PESTS)).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("智慧暫停", farmingMenuPauseEnabled), b -> { farmingMenuPauseEnabled = !farmingMenuPauseEnabled; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 36;
            addSectionTitle("Mining", y);
            y += 22;
            addRenderableWidget(Button.builder(toggleMsg("Mining", miningEnabled), b -> { toggleMining(Minecraft.getInstance()); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("選擇目標方塊"), b -> openMiningTargetCountScreen()).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("智慧暫停", miningMenuPauseEnabled), b -> { miningMenuPauseEnabled = !miningMenuPauseEnabled; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
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
                addRenderableWidget(Button.builder(Component.literal("↑"), b -> { move(idx, -1); rebuild(); }).bounds(x + 295, y, 36, 20).build());
                addRenderableWidget(Button.builder(Component.literal("↓"), b -> { move(idx, 1); rebuild(); }).bounds(x + 335, y, 36, 20).build());
                addRenderableWidget(Button.builder(Component.literal("×"), b -> { TARGET_BLOCKS.remove(idx); saveConfig(); rebuild(); }).bounds(x + 375, y, 45, 20).build());
                y += 24;
            }
            y += 12;
            addSectionTitle("Other", y);
            y += 22;
            addRenderableWidget(Button.builder(Component.literal("Jerry 偵測"),
                    b -> openNotificationSettings(this, NotificationType.JERRY)).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("完成"), b -> Minecraft.getInstance().setScreenAndShow(parent))
                    .bounds(x + 145, this.height - 34, 130, 20).build());
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

        private void openMiningTargetCountScreen() {
            Minecraft.getInstance().setScreenAndShow(new MiningTargetCountScreen(this));
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
