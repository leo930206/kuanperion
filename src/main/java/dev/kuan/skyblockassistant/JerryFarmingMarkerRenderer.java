package dev.kuan.skyblockassistant;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;

/** Snapshot game-side marker data before submitting native full-bright beacon geometry. */
final class JerryFarmingMarkerRenderer {
    private static final RenderStateDataKey<List<JerryFarmingPauseMarkers.Marker>> MARKERS = RenderStateDataKey.create();
    private static final int LIGHT_GREEN = 0xFF90EE90;

    static void register(Supplier<List<JerryFarmingPauseMarkers.Marker>> snapshot) {
        LevelExtractionEvents.END_EXTRACTION.register(context ->
                ((FabricRenderState)context.levelState()).setData(MARKERS, List.copyOf(snapshot.get())));
        LevelRenderEvents.COLLECT_SUBMITS.register(JerryFarmingMarkerRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        var camera = context.levelState().cameraRenderState;
        if (camera == null || !camera.initialized) return;
        var markers = ((FabricRenderState)context.levelState()).getDataOrDefault(MARKERS, List.of());
        for (var marker : markers) {
            var pose = context.poseStack();
            pose.pushPose();
            try {
                // BeaconRenderer adds its own half-block center offset.
                pose.translate(marker.x() - camera.pos.x - 0.5, marker.y() - camera.pos.y, marker.z() - camera.pos.z - 0.5);
                BeaconRenderer.submitBeaconBeam(pose, context.submitNodeCollector(), BeaconRenderer.BEAM_LOCATION,
                        1.0f, (float)(context.levelState().gameTime % 40), 0, BeaconRenderer.MAX_RENDER_Y,
                        LIGHT_GREEN, 0.2f, 0.35f);
            } finally {
                pose.popPose();
            }
        }
    }
}
