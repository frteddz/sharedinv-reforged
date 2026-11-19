package com.t3ddz.sharedinv_reforged;

import com.t3ddz.sharedinv_reforged.mixin.MinecraftServerAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class SharedInventoryManager {
    public static final String MOD_ID = "sharedinv_reforged";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Object2ObjectMap<String, SharedInventory> inventories = new Object2ObjectOpenHashMap<>();
    public static Object2ReferenceMap<UUID, IPlayerInventory.SavedInventory> originalInventories = new Object2ReferenceOpenHashMap<>();
    public static SharedInventory defaultInv = null;

    private SharedInventoryManager() { }

    public static void restorePlayerSlots(Player player) {
        IPlayerInventory inventory = (IPlayerInventory) player.getInventory();
        IPlayerInventory.SavedInventory original = originalInventories.get(player.getUUID());
        if (original == null) {
            inventory.sharedinv$clear();
        } else {
            inventory.sharedinv$restore(original);
        }
    }

    public static void save(MinecraftServer server) {
        try {
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            inventories.forEach((name, inv) -> {
                CompoundTag tag = new CompoundTag();
                list.add(list.size(), inv.toNbt(server.registryAccess(), tag));
            });
            root.put("i", list);
            if (defaultInv != null) {
                root.putString("d", defaultInv.name);
            }
            NbtIo.writeCompressed(root, statePath(server));
        } catch (Exception e) {
            LOGGER.error("Failed to save shared inventory", e);
        }
    }

    public static void load(MinecraftServer server) {
        Path path = statePath(server);
        if (!Files.exists(path)) {
            defaultInv = new SharedInventory("group_1");
            inventories.put(defaultInv.name, defaultInv);
            return;
        }

        try {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            ListTag list = root.getList("i", 10);
            for (int i = 0; i < list.size(); ++i) {
                SharedInventory inv = SharedInventory.fromNbt(server.registryAccess(), list.getCompound(i));
                inventories.put(inv.name, inv);
            }
            String defaultName = root.getString("d");
            SharedInventory found = inventories.get(defaultName);
            if (!defaultName.isEmpty() && found != null) {
                defaultInv = found;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load shared inventory", e);
        }
    }

    public static void onPlayerJoin(Player player) {
        IPlayerInventory inventory = (IPlayerInventory) player.getInventory();
        originalInventories.put(player.getUUID(), inventory.sharedinv$save());

        SharedInventory inv = SharedInventory.playerInvs.get(player.getUUID());
        if (inv == null) {
            if (defaultInv == null) return;
            defaultInv.addPlayer(player.getUUID());
            inv = defaultInv;
        }
        inventory.sharedinv$updateFrom(inv);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        originalInventories.remove(player.getUUID());
    }

    private static Path statePath(MinecraftServer server) {
        LevelStorageSource.LevelStorageAccess access = ((MinecraftServerAccessor) server).sharedinv$storageSource();
        return access.getLevelDirectory().path().resolve("sharedinv.nbt");
    }
}
