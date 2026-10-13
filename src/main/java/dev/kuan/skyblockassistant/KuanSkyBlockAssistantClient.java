package dev.kuan.skyblockassistant;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class KuanSkyBlockAssistantClient implements ClientModInitializer {
    private static final String MOD_ID = "kuan-hypixel-skyblock-assistant";
    private static final String CONFIG_NAME = "kuan-hypixel-skyblock-assistant.properties";
    private static final List<String> TARGET_BLOCKS = new ArrayList<>();
    private static final Map<BlockPos, Integer> SKIPPED_TARGETS = new HashMap<>();
    private static final Map<BlockPos, String> DEPLETED_TARGETS = new HashMap<>();
    private static final int UNREACHABLE_SKIP_TICKS = 200;
    private static final int FARM_TURN_STOP_TICKS = 4;
    private static final int FARM_TURN_RESUME_TICKS = 10;
    private static boolean miningEnabled = false;
    private static boolean selectMode = false;
    private static boolean farmingEnabled = false;
    private static FarmingCrop farmingCrop = FarmingCrop.WHEAT;
    private static boolean farmingAutoSetHome = false;
    private static boolean farmingTurnOnMarkers = true;
    private static boolean farmingReverseLane = false;
    private static int farmingStateTicks = 0;
    private static int farmingStuckTicks = 0;
    private static double farmingLastX = 0.0;
    private static double farmingLastY = 0.0;
    private static double farmingLastZ = 0.0;
    private static int farmingStuckSampleTicks = 0;
    private static boolean rotateToTarget = true;
    private static int scanRadius = 5;
    private static final float ROTATION_DONE_EPSILON = 1.5f;
    private static int rotationStepDegrees = 13;
    private static int actionDelayTicks = 3;
    private static int hudX = 8;
    private static int hudY = 8;
    private static int miningTicks = 0;
    private static int totalMined = 0;
    private static final Deque<Integer> MINED_TICKS = new ArrayDeque<>();
    private static int cooldown = 0;
    private static boolean rightWasDown = false;
    private static boolean miningComboWasDown = false;
    private static boolean farmingComboWasDown = false;
    private static boolean selectComboWasDown = false;
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
        HudElementRegistry.addLast(id("mining_hud"), KuanSkyBlockAssistantClient::renderMiningHud);
        ClientTickEvents.START_CLIENT_TICK.register(KuanSkyBlockAssistantClient::freezeMovementIfMining);
        ClientTickEvents.END_CLIENT_TICK.register(KuanSkyBlockAssistantClient::tick);
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
            clearMiningStats();
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
            resetFarmingRuntime(mc);
            if (farmingAutoSetHome && mc.player != null) mc.player.connection.sendCommand("sethome");
            say(mc, "Farming：開（" + farmingCrop.label + "）");
        }
        saveConfig();
    }

    private static void stopFarming(Minecraft mc, boolean announce) {
        farmingEnabled = false;
        releaseFarmingKeys(mc);
        if (announce) say(mc, "Farming：關");
    }

    private static void resetFarmingRuntime(Minecraft mc) {
        farmingReverseLane = false;
        farmingStateTicks = 0;
        farmingStuckTicks = 0;
        farmingStuckSampleTicks = 0;
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
        LocalPlayer player = mc.player;
        lookAtFixed(player, farmingCrop.yaw, farmingCrop.pitch);
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
        if (farmingTurnOnMarkers && farmingCrop.shouldTurn(mc, player)) {
            farmingStateTicks = FARM_TURN_STOP_TICKS + FARM_TURN_RESUME_TICKS;
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
        mc.options.keyJump.setDown(farmingStuckTicks-- > 0);
    }

    private static void pressFarmingMoveKeys(Minecraft mc, FarmingCrop crop, boolean reversed) {
        if (mc.options == null) return;
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        DirectionKey key = reversed ? crop.reverseKey : crop.forwardKey;
        switch (key) {
            case FORWARD -> mc.options.keyUp.setDown(true);
            case BACK -> mc.options.keyDown.setDown(true);
            case LEFT -> mc.options.keyLeft.setDown(true);
            case RIGHT -> mc.options.keyRight.setDown(true);
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
        if (!selectMode || mc.player == null || mc.level == null || mc.getWindow() == null) {
            rightWasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (down && !rightWasDown && mc.hitResult instanceof BlockHitResult bhr) {
            BlockState state = mc.level.getBlockState(bhr.getBlockPos());
            String name = blockId(state);
            if (!state.isAir() && isAllowedTargetBlock(name)) {
                TARGET_BLOCKS.remove(name);
                TARGET_BLOCKS.add(name);
                saveConfig();
                say(mc, "已更新目標方塊 #" + TARGET_BLOCKS.size() + "：" + name);
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
            recordMinedBlock();
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

    private static void recordMinedBlock() {
        totalMined++;
        MINED_TICKS.addLast(miningTicks);
        trimMinedHistory();
    }

    private static void trimMinedHistory() {
        int cutoff = miningTicks - 20 * 20;
        while (!MINED_TICKS.isEmpty() && MINED_TICKS.peekFirst() < cutoff) MINED_TICKS.removeFirst();
    }

    private static void clearMiningStats() {
        miningTicks = 0;
        totalMined = 0;
        MINED_TICKS.clear();
    }

    private static void renderMiningHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        trimMinedHistory();
        double seconds = Math.min(20.0, Math.max(1.0, miningTicks / 20.0));
        double bps = MINED_TICKS.size() / seconds;
        int x = Math.max(0, Math.min(hudX, graphics.guiWidth() - 96));
        int y = Math.max(0, Math.min(hudY, graphics.guiHeight() - 24));
        graphics.fill(x - 3, y - 3, x + 96, y + 21, 0x88000000);
        graphics.text(mc.font, "Kuan SBA Mining", x, y, 0x55FFFF, true);
        graphics.text(mc.font, String.format("%.2f blocks/s", bps), x, y + 10, miningEnabled ? 0x55FF55 : 0xAAAAAA, true);
    }

    private static File configFile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, CONFIG_NAME);
    }

    private static void loadConfig() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(configFile())) { p.load(in); } catch (Throwable ignored) {}
        miningEnabled = Boolean.parseBoolean(p.getProperty("miningEnabled", "false"));
        farmingEnabled = Boolean.parseBoolean(p.getProperty("farmingEnabled", "false"));
        farmingCrop = FarmingCrop.fromConfig(p.getProperty("farmingCrop", "WHEAT"));
        farmingAutoSetHome = Boolean.parseBoolean(p.getProperty("farmingAutoSetHome", "false"));
        farmingTurnOnMarkers = Boolean.parseBoolean(p.getProperty("farmingTurnOnMarkers", "true"));
        if (miningEnabled && farmingEnabled) farmingEnabled = false;
        rotateToTarget = Boolean.parseBoolean(p.getProperty("rotateToTarget", "true"));
        scanRadius = parseInt(p.getProperty("scanRadius"), 5, 1, 8);
        rotationStepDegrees = parseInt(p.getProperty("rotationStepDegrees"), 13, 1, 40);
        actionDelayTicks = parseInt(p.getProperty("actionDelayTicks"), 3, 1, 20);
        hudX = parseInt(p.getProperty("hudX"), 8, 0, 10000);
        hudY = parseInt(p.getProperty("hudY"), 8, 0, 10000);
        TARGET_BLOCKS.clear();
        String targets = p.getProperty("targets", "").trim();
        if (!targets.isEmpty()) for (String s : targets.split(",")) {
            String id = s.trim();
            if (!id.isBlank() && isAllowedTargetBlock(id) && !TARGET_BLOCKS.contains(id)) TARGET_BLOCKS.add(id);
        }
    }

    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("miningEnabled", Boolean.toString(miningEnabled));
        p.setProperty("farmingEnabled", Boolean.toString(farmingEnabled));
        p.setProperty("farmingCrop", farmingCrop.name());
        p.setProperty("farmingAutoSetHome", Boolean.toString(farmingAutoSetHome));
        p.setProperty("farmingTurnOnMarkers", Boolean.toString(farmingTurnOnMarkers));
        p.setProperty("rotateToTarget", Boolean.toString(rotateToTarget));
        p.setProperty("scanRadius", Integer.toString(scanRadius));
        p.setProperty("rotationStepDegrees", Integer.toString(rotationStepDegrees));
        p.setProperty("actionDelayTicks", Integer.toString(actionDelayTicks));
        p.setProperty("hudX", Integer.toString(hudX));
        p.setProperty("hudY", Integer.toString(hudY));
        p.setProperty("targets", String.join(",", TARGET_BLOCKS));
        try (FileOutputStream out = new FileOutputStream(configFile())) { p.store(out, "Kuan Hypixel SkyBlock Assistant"); } catch (Throwable ignored) {}
    }

    private static int parseInt(String s, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(s))); } catch (Throwable ignored) { return fallback; }
    }

    private enum DirectionKey { FORWARD, BACK, LEFT, RIGHT }

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
        RED_MUSHROOM("Red Mush", -65.0f, 0.0f, DirectionKey.FORWARD, DirectionKey.BACK, "minecraft:quartz_block"),
        BROWN_MUSHROOM("Brown Mush", 115.0f, 0.0f, DirectionKey.BACK, DirectionKey.FORWARD, "minecraft:quartz_block");

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

        boolean shouldTurn(Minecraft mc, LocalPlayer player) {
            if (this == COCOA_BEAN) return isBlockNear(mc, player, "minecraft:glowstone", 1, 0, 1) || isBlockNear(mc, player, markerBlock, 1, 0, 1);
            if (this == SUGAR_CANE) return isBlockNear(mc, player, markerBlock, 1, 0, 0) || isBlockNear(mc, player, "minecraft:sea_lantern", 1, 0, 0);
            if (this == MELON || this == PUMPKIN || this == CACTUS || this == RED_MUSHROOM || this == BROWN_MUSHROOM) {
                return isBlockNear(mc, player, markerBlock, 1, 0, 0) || isBlockNear(mc, player, "#ladder", 1, 0, 1) || isBlockNear(mc, player, "#trapdoor", 1, 0, 0);
            }
            return isBlockNear(mc, player, markerBlock, 1, 0, 0) || isBlockNear(mc, player, "#trapdoor", 0, 0, 0);
        }

        static FarmingCrop fromConfig(String value) {
            if (value == null) return WHEAT;
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            if ("NETHERWART".equals(normalized)) normalized = "NETHER_WART";
            if ("COCOA".equals(normalized) || "COCOA_BEANS".equals(normalized)) normalized = "COCOA_BEAN";
            if ("RED_MUSH".equals(normalized)) normalized = "RED_MUSHROOM";
            if ("BROWN_MUSH".equals(normalized)) normalized = "BROWN_MUSHROOM";
            for (FarmingCrop crop : values()) if (crop.name().equals(normalized) || crop.label.toUpperCase(Locale.ROOT).replace(' ', '_').equals(normalized)) return crop;
            return WHEAT;
        }
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
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("HUD ←"), b -> { hudX = Math.max(0, hudX - 10); saveConfig(); rebuild(); }).bounds(x, y, 95, 20).build());
            addRenderableWidget(Button.builder(Component.literal("HUD →"), b -> { hudX += 10; saveConfig(); rebuild(); }).bounds(x + 105, y, 95, 20).build());
            addRenderableWidget(Button.builder(Component.literal("HUD ↑"), b -> { hudY = Math.max(0, hudY - 10); saveConfig(); rebuild(); }).bounds(x + 210, y, 95, 20).build());
            addRenderableWidget(Button.builder(Component.literal("HUD ↓"), b -> { hudY += 10; saveConfig(); rebuild(); }).bounds(x + 315, y, 95, 20).build());
            y += 24;
            addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("HUD 位置: " + hudX + ", " + hudY), this.font));
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
            addRenderableWidget(Button.builder(Component.literal("作物 ◀"), b -> { cycleCrop(-1); saveConfig(); rebuild(); }).bounds(x + 145, y, 70, 20).build());
            addRenderableWidget(Button.builder(Component.literal("作物 ▶"), b -> { cycleCrop(1); saveConfig(); rebuild(); }).bounds(x + 225, y, 70, 20).build());
            addRenderableWidget(new StringWidget(x + 305, y + 5, 115, 12, Component.literal(farmingCrop.label), this.font));
            y += 28;
            addRenderableWidget(Button.builder(toggleMsg("換行偵測", farmingTurnOnMarkers), b -> { farmingTurnOnMarkers = !farmingTurnOnMarkers; saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("自動 /sethome", farmingAutoSetHome), b -> { farmingAutoSetHome = !farmingAutoSetHome; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(new StringWidget(x + 290, y + 5, 130, 12, Component.literal("Shift+F 開關"), this.font));
            y += 24;
            addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("作物: Wheat/Potato/Carrot/Wart/Cactus/Cane/Melon/Pumpkin/Cocoa/Mushroom"), this.font));
            y = this.height - 34;
            addRenderableWidget(Button.builder(Component.literal("完成"), b -> Minecraft.getInstance().setScreenAndShow(parent)).bounds(x + 145, y, 130, 20).build());
        }

        private Component toggleMsg(String label, boolean on) {
            return Component.literal(label + "：" + (on ? "開" : "關"));
        }

        private void cycleCrop(int delta) {
            FarmingCrop[] crops = FarmingCrop.values();
            int next = (farmingCrop.ordinal() + delta + crops.length) % crops.length;
            farmingCrop = crops[next];
            resetFarmingRuntime(Minecraft.getInstance());
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
