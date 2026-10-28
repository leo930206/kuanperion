package dev.kuan.kuanperion.smokemixin;
import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.input.MouseButtonEvent;
/** Opt-in isolated smoke diagnostics only; never intercepts input or ships in production. */
@Mixin(MainGUI.class)
public abstract class UiNavigationTraceMixin {
    @Shadow private int activeMain;
    @Shadow private int activeFilter;
    @Shadow private ModulesTab.SubTab activeSubTab;
    @Unique private void kuanperion$trace(String event){
        String stack=StackWalker.getInstance().walk(frames->frames.limit(16).map(f->f.getClassName()+"."+f.getMethodName()+":"+f.getLineNumber()).collect(java.util.stream.Collectors.joining(" <- ")));
        System.out.println("UI_NAV_TRACE "+event+" main="+activeMain+" filter="+activeFilter+" module="+(activeSubTab==null?"none":activeSubTab.name())+" stack="+stack);
    }
    @Inject(method="mouseClicked",at=@At("HEAD"))
    private void pressed(MouseButtonEvent event,boolean doubled,CallbackInfoReturnable<Boolean> ci){kuanperion$trace("mouse "+event.button()+" at "+event.x()+","+event.y());}
    @Inject(method="selectSidebarEntry",at=@At("HEAD"))
    private void sidebar(int index,CallbackInfo ci){kuanperion$trace("sidebar "+index);}
    @Inject(method={"setActiveMainTab","setActiveFilterIndex","setActiveModuleSubTab"},at=@At("RETURN"))
    private void navigation(CallbackInfo ci){kuanperion$trace("setter");}
}
