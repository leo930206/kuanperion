package dev.kuan.kuanperion.renderer;
import java.util.ArrayList;
import java.util.List;
/** Frame-local render work, executed on the Minecraft render thread, not extraction. */
public final class UiRenderQueue {
    private static final List<Runnable> TASKS = new ArrayList<>();
    private UiRenderQueue() {}
    public static void enqueue(Runnable task) { if (task!=null) TASKS.add(task); }
    public static void flush() {
        List<Runnable> tasks = List.copyOf(TASKS); TASKS.clear();
        for (Runnable task:tasks) task.run();
    }
    public static void flushBeforeGui() {}
    public static void clear() { TASKS.clear(); }
}
