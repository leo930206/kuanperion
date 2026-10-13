package dev.kuan.skyblockassistant;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.ClipContext;
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
import java.util.List;
import java.util.Properties;

public final class KuanSkyBlockAssistantClient implements ClientModInitializer {
    private static final String MOD_ID = "kuan-hypixel-skyblock-assistant";
    private static final String CONFIG_NAME = "kuan-hypixel-skyblock-assistant.properties";
    private static final List<String> TARGET_BLOCKS = new ArrayList<>();
    private static boolean miningEnabled = false;
    private static boolean selectMode = false;
    private static boolean rotateToTarget = true;
    private static int scanRadius = 5;
    private static final float ROTATION_STEP_DEGREES = 13.0f;
    private static final float ROTATION_DONE_EPSILON = 1.5f;
    private static int actionDelayTicks = 3;
    private static int cooldown = 0;
    private static boolean rightWasDown = false;
    private static BlockPos lastTarget = null;
    private static BlockPos activeTarget = null;
    private static String activeTargetBlockId = null;
    private static boolean activeTargetStarted = false;
    private static KeyMapping settingsKey;
    private static KeyMapping toggleMiningKey;
    private static KeyMapping selectModeKey;

    @Override
    public void onInitializeClient() {
        loadConfig();
        KeyMapping.Category category = KeyMapping.Category.register(id("keys"));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_APOSTROPHE, category));
        toggleMiningKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.toggle_mining", InputConstants.Type.KEYSYM, InputConstants.KEY_M, category));
        selectModeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kuan_sba.select_blocks", InputConstants.Type.KEYSYM, InputConstants.KEY_B, category));
        ClientTickEvents.START_CLIENT_TICK.register(KuanSkyBlockAssistantClient::freezeMovementIfMining);
        ClientTickEvents.END_CLIENT_TICK.register(KuanSkyBlockAssistantClient::tick);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void tick(Minecraft mc) {
        while (settingsKey.consumeClick()) mc.setScreenAndShow(new SettingsScreen(null));
        while (toggleMiningKey.consumeClick()) {
            miningEnabled = !miningEnabled;
            if (!miningEnabled) clearActiveTarget(mc);
            saveConfig();
            say(mc, "Mining：" + (miningEnabled ? "開" : "關"));
        }
        while (selectModeKey.consumeClick()) {
            selectMode = !selectMode;
            saveConfig();
            say(mc, selectMode ? "方塊選取模式：開（右鍵方塊加入/更新，B 結束）" : "方塊選取模式：關");
        }
        handleRightClickSelection(mc);
        freezeMovementIfMining(mc);
        if (cooldown > 0) cooldown--;
        if (miningEnabled) mineBestTarget(mc);
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
            if (!state.isAir()) {
                TARGET_BLOCKS.remove(name);
                TARGET_BLOCKS.add(name);
                saveConfig();
                say(mc, "已更新目標方塊 #" + TARGET_BLOCKS.size() + "：" + name);
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
            if (pos.equals(lastTarget)) continue;
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) continue;
            int priority = TARGET_BLOCKS.indexOf(blockId(state));
            if (priority < 0) continue;
            double dist = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist > (r + 1.5) * (r + 1.5)) continue;
            if (!hasLineOfSight(mc, player, pos)) continue;
            Candidate c = new Candidate(pos, priority, dist);
            if (best == null || c.betterThan(best)) best = c;
        }
        if (best == null) {
            lastTarget = null;
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
            lastTarget = target;
            clearActiveTarget(mc);
            cooldown = Math.max(1, actionDelayTicks);
            return false;
        }
        if (state.isAir() || dist > (r + 1.5) * (r + 1.5)) {
            lastTarget = target;
            clearActiveTarget(mc);
            cooldown = Math.max(1, actionDelayTicks);
            return false;
        }

        if (rotateToTarget && !lookAtSmooth(player, target)) return true;
        if (!isCrosshairOnTarget(player, target, r + 1.5)) {
            lastTarget = target;
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

    private static boolean hasLineOfSight(Minecraft mc, LocalPlayer player, BlockPos target) {
        if (mc.level == null) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 aim = aimPoint(player, target);
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, aim, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(target);
    }

    private static boolean isCrosshairOnTarget(LocalPlayer player, BlockPos target, double reach) {
        HitResult hit = player.pick(reach, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(target)) return false;
        return isHitNearFaceCenter(blockHit, target);
    }

    private static boolean isHitNearFaceCenter(BlockHitResult hit, BlockPos target) {
        double margin = 0.24;
        Vec3 loc = hit.getLocation();
        double lx = loc.x - target.getX();
        double ly = loc.y - target.getY();
        double lz = loc.z - target.getZ();
        return switch (hit.getDirection().getAxis()) {
            case X -> ly >= margin && ly <= 1.0 - margin && lz >= margin && lz <= 1.0 - margin;
            case Y -> lx >= margin && lx <= 1.0 - margin && lz >= margin && lz <= 1.0 - margin;
            case Z -> lx >= margin && lx <= 1.0 - margin && ly >= margin && ly <= 1.0 - margin;
        };
    }

    private static Vec3 aimPoint(LocalPlayer player, BlockPos pos) {
        Direction face = faceFromPlayer(player, pos);
        return Vec3.atCenterOf(pos).add(face.getStepX() * 0.501, face.getStepY() * 0.501, face.getStepZ() * 0.501);
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
        Vec3 aim = aimPoint(player, pos);
        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, h)));
        float yawDiff = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDiff = Mth.wrapDegrees(targetPitch - player.getXRot());
        float nextYaw = player.getYRot() + Mth.clamp(yawDiff, -ROTATION_STEP_DEGREES, ROTATION_STEP_DEGREES);
        float nextPitch = player.getXRot() + Mth.clamp(pitchDiff, -ROTATION_STEP_DEGREES, ROTATION_STEP_DEGREES);
        player.setYRot(nextYaw);
        player.setXRot(Mth.clamp(nextPitch, -90.0f, 90.0f));
        player.setYHeadRot(nextYaw);
        player.setYBodyRot(nextYaw);
        return Math.abs(yawDiff) <= ROTATION_DONE_EPSILON && Math.abs(pitchDiff) <= ROTATION_DONE_EPSILON;
    }

    private static String blockId(BlockState state) {
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "unknown" : id.toString();
    }

    private static void say(Minecraft mc, String text) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Kuan SBA] " + text).withStyle(ChatFormatting.AQUA));
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
        rotateToTarget = Boolean.parseBoolean(p.getProperty("rotateToTarget", "true"));
        scanRadius = parseInt(p.getProperty("scanRadius"), 5, 1, 8);
        actionDelayTicks = parseInt(p.getProperty("actionDelayTicks"), 3, 1, 20);
        TARGET_BLOCKS.clear();
        String targets = p.getProperty("targets", "").trim();
        if (!targets.isEmpty()) for (String s : targets.split(",")) if (!s.isBlank()) TARGET_BLOCKS.add(s.trim());
    }

    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("miningEnabled", Boolean.toString(miningEnabled));
        p.setProperty("rotateToTarget", Boolean.toString(rotateToTarget));
        p.setProperty("scanRadius", Integer.toString(scanRadius));
        p.setProperty("actionDelayTicks", Integer.toString(actionDelayTicks));
        p.setProperty("targets", String.join(",", TARGET_BLOCKS));
        try (FileOutputStream out = new FileOutputStream(configFile())) { p.store(out, "Kuan Hypixel SkyBlock Assistant"); } catch (Throwable ignored) {}
    }

    private static int parseInt(String s, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(s))); } catch (Throwable ignored) { return fallback; }
    }

    private record Candidate(BlockPos pos, int priority, double dist) {
        boolean betterThan(Candidate other) {
            if (priority != other.priority) return priority < other.priority;
            return dist < other.dist;
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
            addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("Mining Assistant"), this.font));
            y += 22;
            addRenderableWidget(Button.builder(toggleMsg("Mining", miningEnabled), b -> { miningEnabled = !miningEnabled; saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("選取模式", selectMode), b -> { selectMode = !selectMode; saveConfig(); rebuild(); }).bounds(x + 145, y, 130, 20).build());
            addRenderableWidget(Button.builder(toggleMsg("自動轉向", rotateToTarget), b -> { rotateToTarget = !rotateToTarget; saveConfig(); rebuild(); }).bounds(x + 290, y, 130, 20).build());
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("偵測半徑 -"), b -> { scanRadius = Math.max(1, scanRadius - 1); saveConfig(); rebuild(); }).bounds(x, y, 95, 20).build());
            addRenderableWidget(Button.builder(Component.literal("偵測半徑 +"), b -> { scanRadius = Math.min(8, scanRadius + 1); saveConfig(); rebuild(); }).bounds(x + 110, y, 95, 20).build());
            addRenderableWidget(new StringWidget(x + 220, y + 5, 62, 12, Component.literal("半徑: " + scanRadius), this.font));
            addRenderableWidget(Button.builder(Component.literal("延遲 -"), b -> { actionDelayTicks = Math.max(1, actionDelayTicks - 1); saveConfig(); rebuild(); }).bounds(x + 290, y, 60, 20).build());
            addRenderableWidget(Button.builder(Component.literal("延遲 +"), b -> { actionDelayTicks = Math.min(20, actionDelayTicks + 1); saveConfig(); rebuild(); }).bounds(x + 360, y, 60, 20).build());
            y += 26;
            addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("目標方塊優先級（上面優先）：右鍵選取會加入或移到最新位置"), this.font));
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
            if (TARGET_BLOCKS.size() > maxRows) addRenderableWidget(new StringWidget(x, y, 420, 12, Component.literal("... 還有 " + (TARGET_BLOCKS.size() - maxRows) + " 個"), this.font));
            y = this.height - 34;
            addRenderableWidget(Button.builder(Component.literal("清空目標"), b -> { TARGET_BLOCKS.clear(); saveConfig(); rebuild(); }).bounds(x, y, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("完成"), b -> Minecraft.getInstance().setScreenAndShow(parent)).bounds(x + 290, y, 130, 20).build());
        }

        private Component toggleMsg(String label, boolean on) {
            return Component.literal(label + "：" + (on ? "開" : "關"));
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
