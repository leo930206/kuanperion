package dev.kuan.kuanperion.ui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
public final class UiSmoke implements ClientModInitializer {
    private int ticks,phase=-1;private MainGUI ui;private boolean waiting;
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        try{
            if(phase<0){
                if(mc.gui.overlay()!=null||!(mc.gui.screen() instanceof TitleScreen))return;
                ui=new MainGUI();mc.setScreenAndShow(ui);phase=0;ticks=0;System.out.println("UI_SMOKE_OPEN");return;
            }
            if(waiting||++ticks<35)return;ticks=0;
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(),1000,700);
            String[] names={"01-overview","02-farming","03-mining","04-jerry","05-colors","06-profiles","07-keybinds","08-settings","09-search","10-reopened"};
            if(phase>=names.length){System.out.println("UI_SMOKE_PASS");mc.stop();return;}
            System.out.println("UI_SMOKE_STATE "+phase+" main="+ui.getActiveMainTab()+" module="+(ui.getActiveModuleSubTab()==null?"none":ui.getActiveModuleSubTab().name())+" live="+(mc.gui.screen()==ui));
            if(phase==0) testSafety(mc);
            if(phase==1) testToggleClick();
            if(phase==9) testSettingsShortcut(mc);
            final String shot=names[phase];
            Screenshot.grab(mc.gameDirectory,shot+".png",mc.gameRenderer.mainRenderTarget(),1,message->System.out.println("UI_SMOKE_CAPTURE "+shot));
            phase++;
            switch(phase){
                case 1-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.get(0));
                case 2-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Mining")).findFirst().orElseThrow());
                case 3-> ui.openModuleDetailFromCard(MainGUIRegistry.MODULE_SUBTABS.stream().filter(s->s.name().equals("Jerry 偵測")).findFirst().orElseThrow());
                case 4-> switchTab(1);
                case 5-> switchTab(2);
                case 6-> switchTab(3);
                case 7-> switchTab(4);
                case 8->{ui.activateSearchField();ui.setSearchQuery("通知");}
                case 9->{ui.onClose();if(mc.gui.screen()==ui)throw new AssertionError("close did not dismiss");ui=new MainGUI();mc.setScreenAndShow(ui);}
            }
        }catch(Throwable error){System.out.println("UI_SMOKE_FAIL");error.printStackTrace();mc.stop();}
    }
    private void testSettingsShortcut(Minecraft mc) {
        var key=new net.minecraft.client.input.KeyEvent(39,0,0);
        ui.activateSearchField();
        ui.keyPressed(key);
        if(mc.gui.screen()!=ui)throw new AssertionError("settings key closed a focused text editor");
        ui.keyPressed(new net.minecraft.client.input.KeyEvent(256,0,0));
        if(mc.gui.screen()!=ui)throw new AssertionError("first escape should leave search");
        ui.keyPressed(key);
        if(mc.gui.screen()==ui)throw new AssertionError("settings key did not close");
        ui=new MainGUI();mc.setScreenAndShow(ui);
        System.out.println("UI_SMOKE_SHORTCUT_PASS editor priority / escape / settings-key close");
    }
    private void testToggleClick() {
        var group=ui.getActiveModuleSubTab().groups().get(0);
        var setting=(dev.kuan.kuanperion.ui.settings.ToggleSetting)group.getSettings().get(0);
        boolean old=setting.getValue();
        var layout=ui.context().layout;
        float x=layout.contX+layout.contW-60, y=layout.contY+219;
        var click=new MouseButtonEvent(x*MainGUI.uiScale/layout.pr,y*MainGUI.uiScale/layout.pr,new MouseButtonInfo(0,0));
        ui.mouseClicked(click,false);ui.mouseReleased(click);
        if(setting.getValue()==old)throw new AssertionError("rendered row click did not change live preference");
        setting.toggle();
        System.out.println("UI_SMOKE_INPUT_PASS toggle live binding while feature stopped");
    }
    private void testSafety(Minecraft mc) throws Exception {
        var webhook=dev.kuan.kuanperion.KuanperionClient.class.getDeclaredField("pestWebhookUrl");webhook.setAccessible(true);webhook.set(null,"https://example.invalid/secret-test-only");
        var snapshot=dev.kuan.kuanperion.KuanperionClient.exportUiConfig();
        if(snapshot.toString().contains("secret-test")||snapshot.has("pestWebhookUrl"))throw new AssertionError("profile secret leak");
        dev.kuan.kuanperion.config.ConfigProfileManager.save("ui-smoke-profile");
        snapshot.addProperty("scanRadius","6");snapshot.addProperty("miningEnabled","true");
        if(!dev.kuan.kuanperion.KuanperionClient.applyUiConfig(snapshot))throw new AssertionError("valid profile failed");
        if(!dev.kuan.kuanperion.KuanperionClient.exportUiConfig().get("scanRadius").getAsString().equals("6"))throw new AssertionError("profile not applied");
        snapshot.addProperty("scanRadius",7);
        if(dev.kuan.kuanperion.KuanperionClient.applyUiConfig(snapshot))throw new AssertionError("invalid profile accepted");
        webhook.set(null,"");
        boolean stencil=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
        int active=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
        int[] box=new int[4];org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX,box);
        try{
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
            org.lwjgl.opengl.GL11.glScissor(3,4,50,60);
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE3);
            dev.kuan.kuanperion.renderer.NanoVGManager.beginFrame(ui.width,ui.height);
            dev.kuan.kuanperion.renderer.NanoVGManager.endFrame();
            if(!org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_STENCIL_TEST))throw new AssertionError("stencil state lost");
            if(org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE)!=org.lwjgl.opengl.GL13.GL_TEXTURE3)throw new AssertionError("active texture lost");
            int[] after=new int[4];org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX,after);
            if(!java.util.Arrays.equals(after,new int[]{3,4,50,60}))throw new AssertionError("scissor rectangle lost");
        }finally{
            if(!stencil)org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
            org.lwjgl.opengl.GL11.glScissor(box[0],box[1],box[2],box[3]);org.lwjgl.opengl.GL13.glActiveTexture(active);
        }
        var parse=dev.kuan.kuanperion.ui.theme.Theme.class.getDeclaredMethod("parseAnimationTime",float.class);parse.setAccessible(true);
        if(!Float.isFinite((Float)parse.invoke(null,Float.NaN)))throw new AssertionError("corrupt theme animation");
        System.out.println("UI_SMOKE_SAFETY_PASS profile apply/reject/secret exclusion + GL state restoration");
    }
    private void switchTab(int tab)throws Exception{
        var field=MainGUI.class.getDeclaredField("actions");field.setAccessible(true);((MainGUIActions)field.get(ui)).switchMainTab(tab);
    }
}
