package com.deveisia.enceladus.voidminerrecipe.machine;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerRecipe;
import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;
import com.deveisia.enceladus.voidminerrecipe.util.VeinUtil;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class VoidMinerLogic extends RecipeLogic {
    private boolean warnedMissingFuel = false;

    public VoidMinerLogic(VoidMinerMachine machine) {
        super(machine);
    }

    @Override
    public VoidMinerMachine getMachine() {
        return (VoidMinerMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        if (getMachine().isRemote()) return;
        lastFailedMatches = null;
        failureReasonMap.clear();
        lastRecipe = null;
        lastOriginRecipe = null;
        GTRecipe origin = buildRecipe();
        if (origin != null) start(origin);
        recipeDirty = false;
    }

    private boolean start(GTRecipe origin) {
        GTRecipe modified = machine.fullModifyRecipe(origin);
        if (modified == null) {
            if (failureReasonMap.isEmpty()) {
                putFailureReason(this, origin, Component.translatable("gtceu.recipe_modifier.default_fail"));
            }
            return false;
        }

        ActionResult check = checkRecipe(modified);
        if (!check.isSuccess()) {
            putFailureReason(this, origin, check.reason());
            return false;
        }

        setupRecipe(modified);
        if (lastRecipe == null || !isWorking()) return false;
        lastOriginRecipe = origin;
        return true;
    }

    @Nullable
    private GTRecipe buildRecipe() {
        VoidMinerMachine m = getMachine();
        VoidMinerTier tier = m.getVoidTier();

        Map<Material, Integer> perMaterial = new LinkedHashMap<>();
        int veinCount = 0;
        for (ResourceLocation id : m.getSelectedVeinSet()) {
            GTOreDefinition def = VeinUtil.get(id, false);
            var mats = VeinUtil.materials(def);
            if (mats.isEmpty()) continue;
            veinCount++;
            for (Material mat : mats) perMaterial.merge(mat, 1, Integer::sum);
        }
        if (veinCount == 0) return null;

        Fluid fuel = tier.fuelFluid();
        if (fuel == null) {
            if (!warnedMissingFuel) {
                warnedMissingFuel = true;
                VoidMinerRecipe.LOGGER.error("Void miner fuel fluid '{}' is not registered", tier.fuelId);
            }
            return null;
        }

        int fuelAmount = (int) Math.min(Integer.MAX_VALUE, (long) tier.fuelAmount * veinCount);
        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw()
                .duration(tier.duration)
                .EUt(tier.voltage(), veinCount)
                .inputFluids(FluidIngredient.of(fuel, fuelAmount));

        for (Map.Entry<Material, Integer> e : perMaterial.entrySet()) {
            int count = (int) Math.min(Integer.MAX_VALUE, (long) tier.outputCount * e.getValue());
            ItemStack stack = ChemicalHelper.get(TagPrefix.rawOre, e.getKey(), count);
            if (!stack.isEmpty()) builder.outputItems(stack);
        }
        return builder.buildRawRecipe();
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            runAttempt = 0;
            runDelay = 0;
            consecutiveRecipes++;
            handleRecipeIO(lastRecipe, IO.OUT);
        }

        lastRecipe = null;
        lastOriginRecipe = null;
        progress = 0;
        duration = 0;
        isActive = false;

        if (suspendAfterFinish) {
            consecutiveRecipes = 0;
            setStatus(Status.SUSPEND);
            return;
        }

        lastFailedMatches = null;
        failureReasonMap.clear();
        GTRecipe next = buildRecipe();
        boolean started = next != null && start(next);
        recipeDirty = false;
        if (!started) {
            consecutiveRecipes = 0;
            setStatus(Status.IDLE);
        }
    }
}
