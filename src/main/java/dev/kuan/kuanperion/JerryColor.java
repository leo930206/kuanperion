package dev.kuan.kuanperion;

/** The four Jerry variants and their Minecraft chat colors. */
enum JerryColor {
    GREEN("Green", 0x55FF55),
    BLUE("Blue", 0x5555FF),
    PURPLE("Purple", 0xAA00AA),
    GOLDEN("Golden", 0xFFAA00);

    final String label;
    final int rgb;
    final String configKey;

    JerryColor(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
        this.configKey = "jerry" + label + "Enabled";
    }

    static JerryColor fromName(String name) {
        for (JerryColor color : values()) if (color.label.equalsIgnoreCase(name)) return color;
        throw new IllegalArgumentException("Unknown Jerry color");
    }
}
