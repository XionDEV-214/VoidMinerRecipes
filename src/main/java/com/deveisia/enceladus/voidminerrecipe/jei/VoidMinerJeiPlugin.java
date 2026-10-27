package com.deveisia.enceladus.voidminerrecipe.jei;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerRecipe;
import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;
import com.deveisia.enceladus.voidminerrecipe.util.VeinUtil;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class VoidMinerJeiPlugin implements IModPlugin {
    private static final String[] MACHINE_NAMESPACES = { "gtceu", "kubejs" };

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return new ResourceLocation(VoidMinerRecipe.MOD_ID, "jei_plugin");
    }

    @Nullable
    private static Item machineItem(VoidMinerTier tier) {
        for (String ns : MACHINE_NAMESPACES) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ns, "void_miner_" + tier.key));
            if (item != null && item != Items.AIR) return item;
        }
        return null;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        for (VoidMinerTier tier : VoidMinerTier.values()) {
            Item machine = machineItem(tier);
            ItemStack icon = machine != null ? new ItemStack(machine) :
                    ChemicalHelper.get(TagPrefix.rawOre, GTMaterials.Iron);
            registration.addRecipeCategories(new VoidMinerJeiCategory(helpers, tier, icon));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<VeinPage> pages = new ArrayList<>();
        for (ResourceLocation id : VeinUtil.ids(true)) {
            VeinPage page = VeinPage.build(id, VeinUtil.get(id, true));
            if (page != null) pages.add(page);
        }
        for (VoidMinerTier tier : VoidMinerTier.values()) {
            List<VoidMinerJeiRecipe> recipes = new ArrayList<>(pages.size());
            for (VeinPage page : pages) recipes.add(new VoidMinerJeiRecipe(tier, page));
            registration.addRecipes(VoidMinerJeiCategory.type(tier), recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (VoidMinerTier tier : VoidMinerTier.values()) {
            Item item = machineItem(tier);
            if (item != null) registration.addRecipeCatalyst(new ItemStack(item), VoidMinerJeiCategory.type(tier));
        }
    }
}
