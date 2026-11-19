package com.t3ddz.sharedinv_reforged.mixin;

import com.t3ddz.sharedinv_reforged.IPlayerInventory;
import com.t3ddz.sharedinv_reforged.SharedInventory;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    public void sharedinv$resetInvRefs(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        SharedInventory inv = SharedInventory.playerInvs.get(oldPlayer.getUUID());
        if (inv != null) {
            ((IPlayerInventory) ((ServerPlayer) (Object) this).getInventory()).sharedinv$updateFrom(inv);
        }
    }
}
