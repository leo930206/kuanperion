package dev.kuan.kuanperion;

import java.lang.reflect.Method;

/** Optional, lazily resolved access to the location source used by CustomScoreboard. */
final class SkyBlockLocationBridge {
    private Object api;
    private Method onSkyBlock;
    private Method getIsland;
    private boolean attempted;
    private boolean failed;

    static SkyBlockLocationBridge discover() {
        return new SkyBlockLocationBridge();
    }

    static SkyBlockLocationBridge connect(Class<?> apiClass) {
        SkyBlockLocationBridge bridge = new SkyBlockLocationBridge();
        bridge.attempted = true;
        bridge.initialize(apiClass);
        return bridge;
    }

    private void initialize(Class<?> apiClass) {
        try {
            onSkyBlock = apiClass.getMethod("isOnSkyBlock");
            getIsland = apiClass.getMethod("getIsland");
            api = apiClass.getField("INSTANCE").get(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
        }
    }

    String islandName() {
        if (!attempted) {
            attempted = true;
            try {
                initialize(Class.forName("tech.thatgravyboat.skyblockapi.api.location.LocationAPI"));
            } catch (ClassNotFoundException | LinkageError e) {
                failed = true;
            }
        }
        if (failed || api == null) return null;
        try {
            if (!Boolean.TRUE.equals(onSkyBlock.invoke(api))) return null;
            Object island = getIsland.invoke(api);
            return island instanceof Enum<?> value ? value.name() : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            return null;
        }
    }
}
