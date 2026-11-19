package com.t3ddz.sharedinv_reforged.neoforge;

import com.t3ddz.sharedinv_reforged.SharedInventoryCommand;
import com.t3ddz.sharedinv_reforged.SharedInventoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(SharedInventoryManager.MOD_ID)
public class NeoForgeEntry {
    public NeoForgeEntry() {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onLevelSave);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerDisconnect);
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
