package com.t3ddz.sharedinv_reforged;

import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class SharedInventory {
    public static final Object2ReferenceMap<UUID, SharedInventory> playerInvs = new Object2ReferenceOpenHashMap<>();

    public final NonNullList<ItemStack> main = NonNullList.withSize(36, ItemStack.EMPTY);
    public final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
    public final NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);

    public final String name;
    public final Set<UUID> players = new HashSet<>();

    public SharedInventory(String name) {
        this.name = name;
    }

    public ListTag writeNbt(HolderLookup.Provider registries, ListTag list) {
        for (int i = 0; i < this.main.size(); ++i) {
            ItemStack stack = this.main.get(i);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) i);
                list.add(list.size(), new ItemStackNbt(stack).toNbt(registries, tag));
            }
        }
        for (int i = 0; i < this.armor.size(); ++i) {
            ItemStack stack = this.armor.get(i);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) (100 + i));
                list.add(list.size(), new ItemStackNbt(stack).toNbt(registries, tag));
            }
        }
        ItemStack offhandStack = this.offhand.get(0);
        if (!offhandStack.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putByte("Slot", (byte) 150);
            list.add(list.size(), new ItemStackNbt(offhandStack).toNbt(registries, tag));
        }
        return list;
    }

    public CompoundTag toNbt(HolderLookup.Provider registries, CompoundTag tag) {
        tag.putString("name", this.name);
        tag.put("i", this.writeNbt(registries, new ListTag()));
        ListTag playerList = new ListTag();
        this.players.stream()
                .map(NbtOps.INSTANCE.withEncoder(UUIDUtil.CODEC))
                .map(DataResult::getOrThrow)
                .forEach(value -> playerList.add(playerList.size(), value));
        tag.put("players", playerList);
        return tag;
    }

    private static final Function<Tag, UUID> parseUuid = NbtOps.INSTANCE
            .withParser(UUIDUtil.CODEC)
            .andThen(DataResult::getOrThrow);

    public static SharedInventory fromNbt(HolderLookup.Provider registries, CompoundTag tag) {
        SharedInventory inv = new SharedInventory(tag.getString("name"));

        ListTag playerList = tag.getList("players", 10);
        for (Tag playerTag : playerList) {
            UUID uuid = parseUuid.apply(playerTag);
            inv.players.add(uuid);
            SharedInventory.playerInvs.put(uuid, inv);
        }

        ListTag items = tag.getList("i", 10);
        for (Tag element : items) {
            if (!(element instanceof CompoundTag compound)) continue;
            int slot = compound.getByte("Slot") & 255;
            ItemStack stack = ItemStackNbt.fromNbt(registries, compound).orElse(ItemStack.EMPTY);
            if (slot >= 0 && slot < inv.main.size()) {
                inv.main.set(slot, stack);
            } else if (slot >= 100 && slot < 100 + inv.armor.size()) {
                inv.armor.set(slot - 100, stack);
            } else if (slot == 150) {
                inv.offhand.set(0, stack);
            }
        }
        return inv;
    }

    public void addPlayer(UUID uuid) {
        if (this.players.contains(uuid)) return;

        SharedInventory existing = SharedInventory.playerInvs.get(uuid);
        if (existing != null) {
            existing.removePlayer(uuid);
        }

        this.players.add(uuid);
        SharedInventory.playerInvs.put(uuid, this);
    }

    public void removePlayer(UUID uuid) {
        this.players.remove(uuid);
        SharedInventory.playerInvs.remove(uuid);
    }
}
