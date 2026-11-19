package com.t3ddz.sharedinv_reforged.mixin;

import com.t3ddz.sharedinv_reforged.SharedInventoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("TAIL"))
    void sharedinv$removeOriginalInvOnDisconnect(ServerPlayer player, CallbackInfo ci) {
        SharedInventoryManager.originalInventories.remove(player.getUUID());
    }
}
