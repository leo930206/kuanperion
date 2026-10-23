package dev.kuan.kuanperion;

public final class FarmingToolGuardTest {
    public static void main(String[] args) {
        FarmingToolGuard guard = new FarmingToolGuard();
        guard.reset("Pumpkin Dicer");
        check(!guard.matches("Melon Dicer"), "startup rejects the wrong tool");
        check(guard.matches("Blessed Pumpkin Dicer Mk. III"), "prefixes and tiers remain accepted");
        check(guard.matches("PUMPKIN DICER Mk. II"), "matching remains case-insensitive");
        check(!guard.update(true, "Pumpkin Dicer"), "correct tool keeps Farming running");

        int notifications = 0;
        for (int tick = 0; tick < 100_000; tick++) {
            boolean wasPaused = guard.isPaused();
            boolean paused = guard.update(true, "Melon Dicer");
            if (paused && !wasPaused) notifications++;
            check(paused, "wrong tool waits indefinitely");
        }
        check(notifications == 1, "one notification for a continuous mismatch");
        check(!guard.update(true, "Pumpkin Dicer Mk. III"), "correct tool resumes without restart");
        check(!guard.isPaused(), "resuming clears the pause");
        boolean wasPaused = guard.isPaused();
        if (guard.update(true, "") && !wasPaused) notifications++;
        check(notifications == 2, "switching away again notifies again, including empty hand");
        check(guard.update(true, null), "missing held tool remains paused");
        check(!guard.update(false, "Melon Dicer"), "disabling detection resumes immediately");
        check(guard.update(true, "Melon Dicer"), "re-enabling detection catches mismatch");
        check(guard.expectedTool().equals("Pumpkin Dicer"), "expected tool remains the starting crop's tool");

        guard.reset("Melon Dicer");
        check(!guard.isPaused(), "new session clears the pause");
        check(!guard.update(true, "Melon Dicer"), "new session uses the newly detected crop");
        check(guard.update(true, "Pumpkin Dicer"), "shared crop profile still distinguishes tools");
        guard.reset(null);
        check(!guard.isPaused(), "stopping clears pause state");
        check(!guard.update(true, ""), "no detected requirement does not block startup probing");
        System.out.println("FarmingToolGuardTest: PASS (startup, matching, indefinite wait, resume, notification edges, toggle, reset)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
