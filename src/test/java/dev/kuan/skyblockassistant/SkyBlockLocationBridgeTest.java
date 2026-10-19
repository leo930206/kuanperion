package dev.kuan.skyblockassistant;

public final class SkyBlockLocationBridgeTest {
    public enum Island { PRIVATE_ISLAND, HUB, GARDEN, DWARVEN_MINES }
    public static final class FakeApi {
        public static final FakeApi INSTANCE = new FakeApi();
        boolean active = true;
        Island island = Island.GARDEN;
        public boolean isOnSkyBlock() { return active; }
        public Island getIsland() { return island; }
    }
    public static final class ThrowingApi {
        public static final ThrowingApi INSTANCE = new ThrowingApi();
        public boolean isOnSkyBlock() { return true; }
        public Island getIsland() { throw new IllegalStateException("API not ready"); }
    }
    public static void main(String[] args) {
        SkyBlockLocationBridge bridge = SkyBlockLocationBridge.connect(FakeApi.class);
        check("GARDEN".equals(bridge.islandName()), "read the same API island enum used by CustomScoreboard");
        FakeApi.INSTANCE.island = Island.PRIVATE_ISLAND;
        check("PRIVATE_ISLAND".equals(bridge.islandName()), "Private Island does not depend on rendered sidebar text");
        FakeApi.INSTANCE.island = Island.HUB;
        check("HUB".equals(bridge.islandName()), "Hub updates without keeping stale Garden state");
        FakeApi.INSTANCE.active = false;
        check(bridge.islandName() == null, "outside SkyBlock, ignore stale API island values");
        FakeApi.INSTANCE.active = true;
        FakeApi.INSTANCE.island = null;
        check(bridge.islandName() == null, "unready API allows native fallback");
        FakeApi.INSTANCE.island = Island.DWARVEN_MINES;
        check("DWARVEN_MINES".equals(bridge.islandName()), "unhandled islands remain explicit, not misidentified as Hub");
        check(SkyBlockLocationBridge.connect(Object.class).islandName() == null, "incompatible API fails safely");
        check(SkyBlockLocationBridge.connect(ThrowingApi.class).islandName() == null, "API failures allow native fallback");
        check(SkyBlockLocationBridge.discover().islandName() == null, "API remains optional on a bare test classpath");
        System.out.println("SkyBlockLocationBridgeTest: PASS (API islands, transitions, outside SkyBlock, unavailable API, no hard dependency)");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
