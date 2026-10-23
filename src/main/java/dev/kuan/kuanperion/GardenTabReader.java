package dev.kuan.kuanperion;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

/** Reads cached server Tab entries even when the overlay is closed. */
final class GardenTabReader {
    // Match 26.2 PlayerTabOverlay ordering: section headers must precede their fields.
    private static final Comparator<PlayerInfo> TAB_ORDER = Comparator
            .comparingInt((PlayerInfo info) -> -info.getTabListOrder())
            .thenComparingInt(info -> info.getGameMode() == GameType.SPECTATOR ? 1 : 0)
            .thenComparing(info -> info.getTeam() == null ? "" : info.getTeam().getName())
            .thenComparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER);

    static List<String> read(Minecraft mc) {
        if (mc.player == null || mc.level == null) return List.of();
        return mc.player.connection.getListedOnlinePlayers().stream()
                .sorted(TAB_ORDER).limit(80)
                .map(info -> {
                    Component text = info.getTabListDisplayName();
                    if (text == null) text = PlayerTeam.formatNameForTeam(info.getTeam(),
                            Component.literal(info.getProfile().name()));
                    return text.getString();
                }).toList();
    }
}
