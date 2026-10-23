package dev.kuan.kuanperion.notification;
import dev.kuan.kuanperion.renderer.NVGRenderer;
import dev.kuan.kuanperion.ui.theme.Theme;
/** Local feedback for profile/theme actions. No Webhook or remote services. */
public final class NotificationManager {
    private static String message="";
    private static long until;
    private NotificationManager() {}
    public static void success(String title,String body) { show(title,body); }
    public static void error(String title,String body) { show(title,body); }
    public static void warning(String title,String body) { show(title,body); }
    public static void info(String title,String body) { show(title,body); }
    private static void show(String title,String body) { message=title+" · "+body; until=System.currentTimeMillis()+4000; }
    public static void render(NVGRenderer nvg,float width,float height) {
        if (System.currentTimeMillis()>until) return;
        float w=Math.min(width-24,Math.max(260,nvg.textWidth("Inter-Regular",message,12)+32));
        nvg.roundedRect((width-w)/2,height-52,w,32,7,Theme.BG_SECONDARY);
        nvg.textCentered("Inter-Regular",message,(width-w)/2,height-52,w,32,12,Theme.TEXT_PRIMARY);
    }
}
