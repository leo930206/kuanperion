package dev.kuan.kuanperion;

/** Choose the starting half of the existing route from the detected crop and standing column. */
final class PumpkinMelonLaneStart {
    static boolean startsRight(String cropBlockId, double x) {
        if (cropBlockId == null || !Double.isFinite(x)) return false;
        int column = (int)Math.floor(x);
        return switch (cropBlockId) {
            case "minecraft:pumpkin", "minecraft:carved_pumpkin" -> column == 140 || column == 134 || column == 128;
            case "minecraft:melon" -> column == 122 || column == 116 || column == 110;
            default -> false;
        };
    }
}
