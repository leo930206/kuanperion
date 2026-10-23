package dev.kuan.kuanperion.util;
import java.io.InputStream;
public final class UiResources {
    private UiResources() {}
    public static InputStream open(String path) { return UiResources.class.getResourceAsStream(path); }
    public static InputStream getResourceAsStream(String path) { return UiResources.class.getResourceAsStream(path); }
}
