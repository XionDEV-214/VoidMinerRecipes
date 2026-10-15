package com.deveisia.enceladus.voidminerrecipe.jei;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerRecipe;
import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;
import com.deveisia.enceladus.voidminerrecipe.util.VeinUtil;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class VoidMinerJeiPlugin implements IModPlugin {
    private static final String[] MACHINE_NAMESPACES = { "gtceu", "kubejs" };

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return new ResourceLocation(VoidMinerRecipe.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new VoidMinerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ResourceLocation> ids = VeinUtil.ids(true);
        List<VoidMinerJeiRecipe> recipes = new ArrayList<>();
        for (VoidMinerTier tier : VoidMinerTier.values()) {
            List<VoidMinerJeiRecipe.VeinView> views = new ArrayList<>();
            for (ResourceLocation id : ids) {
                var def = VeinUtil.get(id, true);
                List<ItemStack> ores = VeinUtil.oreStacks(def, 1);
                if (!ores.isEmpty()) views.add(new VoidMinerJeiRecipe.VeinView(id, VeinUtil.displayName(id), ores));
            }
            recipes.add(new VoidMinerJeiRecipe(tier, views));
        }
        registration.addRecipes(VoidMinerJeiCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (VoidMinerTier tier : VoidMinerTier.values()) {
            for (String ns : MACHINE_NAMESPACES) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ns, "void_miner_" + tier.key));
                if (item != null && item != Items.AIR) {
                    registration.addRecipeCatalyst(new ItemStack(item), VoidMinerJeiCategory.TYPE);
                    break;
                }
            }
        }
    }
}
