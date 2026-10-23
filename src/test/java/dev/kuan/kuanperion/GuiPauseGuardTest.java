package dev.kuan.kuanperion;

public final class GuiPauseGuardTest {
    public static void main(String[] args) {
        GuiPauseGuard guard = new GuiPauseGuard();
        check(!guard.shouldPause(true, false, 0), "no GUI means no initial delay");
        check(guard.shouldPause(true, true, 0), "GUI pauses immediately");
        check(guard.shouldPause(true, false, 50_000_000L), "closing GUI starts the buffer");
        check(guard.shouldPause(true, false, 549_999_999L), "wait the full half-second after closure");
        for (int query = 0; query < 100; query++) {
            check(guard.shouldPause(true, false, 549_999_999L), "multiple queries do not advance time");
        }
        check(!guard.shouldPause(true, false, 550_000_000L), "resume exactly half a second after closure");
        check(!guard.shouldPause(true, false, 2_000_000_000L), "idle gameplay does not restart the buffer");

        check(guard.shouldPause(true, true, 3_000_000_000L), "Bazaar search input pauses");
        check(guard.shouldPause(true, false, 3_050_000_000L), "search transition gap remains paused");
        check(guard.shouldPause(true, true, 3_450_000_000L), "results GUI cancels the old countdown");
        check(guard.shouldPause(true, true, 5_000_000_000L), "GUI may stay open indefinitely");
        check(guard.shouldPause(true, false, 6_000_000_000L), "closing results starts a fresh half-second");
        check(guard.shouldPause(true, false, 6_499_999_999L), "old countdown cannot resume results early");
        check(!guard.shouldPause(true, false, 6_500_000_000L), "resume after results close for half a second");

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
        check(mining.shouldPause(true, false, 50_000_000L), "Mining starts its own buffer");
        farming.shouldPause(true, true, 100_000_000L);
        check(farming.shouldPause(true, false, 150_000_000L), "Farming starts a later buffer");
        check(mining.shouldPause(true, false, 549_999_999L), "Mining waits until 500ms");
        check(!mining.shouldPause(true, false, 550_000_000L), "Mining resumes at 500ms");
        check(farming.shouldPause(true, false, 550_000_000L), "Mining resume cannot end Farming buffer");
        check(farming.shouldPause(true, false, 649_999_999L), "Farming waits until its own 500ms");
        check(!farming.shouldPause(true, false, 650_000_000L), "Farming resumes at its own 500ms");
        System.out.println("GuiPauseGuardTest: PASS (half-second boundary, Bazaar transition, repeated queries, toggles, session reset, independence)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
