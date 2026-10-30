// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.mixin;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Invoker;
/** Only the guarded MISS-only spray opener may request ordinary vanilla selected-slot synchronization. */
@Mixin(MultiPlayerGameMode.class)
public interface SprayCarriedItemAccess {
 @Invoker("ensureHasSentCarriedItem") void kuanperion$syncSprayCarriedItem();
}
