package dev.kuan.skyblockassistant;

public final class GuiPauseGuardTest {
    public static void main(String[] args) {
        GuiPauseGuard guard = new GuiPauseGuard();
        check(!guard.shouldPause(true, false, 0), "no GUI means no initial delay");
        check(guard.shouldPause(true, true, 0), "GUI pauses immediately");
        check(guard.shouldPause(true, false, 50_000_000L), "closing GUI starts the buffer");
        check(guard.shouldPause(true, false, 1_049_999_999L), "wait the full second after closure");
        for (int query = 0; query < 100; query++) {
            check(guard.shouldPause(true, false, 1_049_999_999L), "multiple queries do not advance time");
        }
        check(!guard.shouldPause(true, false, 1_050_000_000L), "resume exactly one second after closure");
        check(!guard.shouldPause(true, false, 2_000_000_000L), "idle gameplay does not restart the buffer");

        check(guard.shouldPause(true, true, 3_000_000_000L), "Bazaar search input pauses");
        check(guard.shouldPause(true, false, 3_050_000_000L), "search transition gap remains paused");
        check(guard.shouldPause(true, true, 3_950_000_000L), "results GUI cancels the old countdown");
        check(guard.shouldPause(true, true, 5_000_000_000L), "GUI may stay open indefinitely");
        check(guard.shouldPause(true, false, 6_000_000_000L), "closing results starts a fresh second");
        check(guard.shouldPause(true, false, 6_999_999_999L), "old countdown cannot resume results early");
        check(!guard.shouldPause(true, false, 7_000_000_000L), "resume after results close for a second");

        guard.shouldPause(true, true, 8_000_000_000L);
        check(!guard.shouldPause(false, true, 8_050_000_000L), "disabled smart pause allows open GUIs");
        check(!guard.shouldPause(true, false, 8_100_000_000L), "disabled mode clears stale history");
        guard.shouldPause(true, true, 9_000_000_000L);
        guard.shouldPause(true, false, 9_050_000_000L);
        check(!guard.shouldPause(false, false, 9_100_000_000L), "disabling cancels an active buffer");
        guard.shouldPause(true, true, 10_000_000_000L);
        guard.reset();
        check(!guard.shouldPause(true, false, 10_050_000_000L), "new session has no stale delay");

        GuiPauseGuard mining = new GuiPauseGuard();
        GuiPauseGuard farming = new GuiPauseGuard();
        mining.shouldPause(true, true, 0);
        check(!farming.shouldPause(true, false, 50_000_000L), "Mining and Farming buffers are independent");
        System.out.println("GuiPauseGuardTest: PASS (one-second boundary, Bazaar transition, repeated queries, toggles, session reset, independence)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
