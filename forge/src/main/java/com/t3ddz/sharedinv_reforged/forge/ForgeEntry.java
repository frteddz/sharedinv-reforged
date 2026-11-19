package com.t3ddz.sharedinv_reforged.forge;

import com.t3ddz.sharedinv_reforged.SharedInventoryCommand;
import com.t3ddz.sharedinv_reforged.SharedInventoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(SharedInventoryManager.MOD_ID)
public class ForgeEntry {
    public ForgeEntry() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelSave);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerDisconnect);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        SharedInventoryCommand.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        SharedInventoryManager.load(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        SharedInventoryManager.save(event.getServer());
    }

    private void onLevelSave(LevelEvent.Save event) {
        SharedInventoryManager.save(event.getLevel().getServer());
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        SharedInventoryManager.onPlayerJoin(event.getEntity());
    }

    private void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SharedInventoryManager.onPlayerDisconnect(player);
        }
    }
}
