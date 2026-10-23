package dev.kuan.kuanperion.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Narrow bridge to vanilla held-block attack. Call site restricts it to active Farming in MainGUI. */
@Mixin(Minecraft.class)
public interface MinecraftAttackAccess {
 @Accessor("missTime") void kuanperion$setMissTime(int ticks);
 @Invoker("continueAttack") void kuanperion$continueAttack(boolean attack);
}
