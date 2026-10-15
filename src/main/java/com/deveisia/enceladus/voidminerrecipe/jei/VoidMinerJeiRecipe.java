package com.deveisia.enceladus.voidminerrecipe.jei;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record VoidMinerJeiRecipe(VoidMinerTier tier, List<VeinView> veins) {
    public record VeinView(ResourceLocation id, String name, List<ItemStack> ores) {}
}
