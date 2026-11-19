package com.t3ddz.sharedinv_reforged;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface IPlayerInventory {
    default void sharedinv$updateFrom(SharedInventory inv) {
        throw new UnsupportedOperationException("This method should be overridden in the mixin");
    }

    default void sharedinv$clear() {
        throw new UnsupportedOperationException("This method should be overridden in the mixin");
    }

    default SavedInventory sharedinv$save() {
        throw new UnsupportedOperationException("This method should be overridden in the mixin");
    }

    default void sharedinv$restore(SavedInventory inventory) {
        throw new UnsupportedOperationException("This method should be overridden in the mixin");
    }

    record SavedInventory(NonNullList<ItemStack> main, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) { }
}
