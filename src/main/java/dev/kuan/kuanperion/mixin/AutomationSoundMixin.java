package dev.kuan.kuanperion.mixin;

import dev.kuan.kuanperion.KuanperionClient;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Filter runtime channel gain; Minecraft options and saved master volume are untouched. */
@Mixin(SoundEngine.class)
public abstract class AutomationSoundMixin {
 @Inject(method="calculateVolume(FLnet/minecraft/sounds/SoundSource;)F",at=@At("HEAD"),cancellable=true)
 private void kuanperion$automationVolume(float volume,SoundSource source,CallbackInfoReturnable<Float> result){
  if(KuanperionClient.shouldMuteAutomationSound())result.setReturnValue(0f);
 }
}
