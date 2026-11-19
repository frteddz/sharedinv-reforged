package com.t3ddz.sharedinv_reforged.mixin;

import com.t3ddz.sharedinv_reforged.IPlayerInventory;
import com.t3ddz.sharedinv_reforged.SharedInventory;
import com.t3ddz.sharedinv_reforged.SharedInventoryManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void sharedinv$inplaceOriginalInventoryOnWrite(Player player, CallbackInfo ci) {
        if (SharedInventory.playerInvs.containsKey(player.getUUID())) {
            SharedInventoryManager.restorePlayerSlots(player);
        }
    }

    @Inject(method = "save", at = @At("TAIL"))
    private void sharedinv$rejoinTeamAfterWriteInventory(Player player, CallbackInfo ci) {
        SharedInventory inv = SharedInventory.playerInvs.get(player.getUUID());
        if (inv != null) {
            ((IPlayerInventory) player.getInventory()).sharedinv$updateFrom(inv);
        }
    }
}
