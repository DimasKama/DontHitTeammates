package com.dimaskama.donthitteammates.mixin;

import com.dimaskama.donthitteammates.client.DHTMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(
            method = "doAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isRiding()Z"
            ),
            cancellable = true
    )
    private void dontAttackTeammates(CallbackInfoReturnable<Boolean> cir) {
        HitResult crosshairTarget = ((MinecraftClient) (Object) this).crosshairTarget;
        if (
                crosshairTarget != null
                && crosshairTarget.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) crosshairTarget).getEntity() instanceof OtherClientPlayerEntity p
                && DHTMod.CONFIG.enabled && DHTMod.shouldProtect(p)
        ) {
            cir.setReturnValue(false);
        }
    }
}
