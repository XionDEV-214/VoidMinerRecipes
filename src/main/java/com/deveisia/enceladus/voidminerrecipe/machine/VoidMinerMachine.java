package com.deveisia.enceladus.voidminerrecipe.machine;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerRecipe;
import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;
import com.deveisia.enceladus.voidminerrecipe.ui.VeinSelectorFancyConfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class VoidMinerMachine extends WorkableElectricMultiblockMachine {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            VoidMinerMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private final VoidMinerTier voidTier;

    private RecipeModifier resolvedModifier;

    @Persisted
    @DescSynced
    private String selectedVeins = "";

    public VoidMinerMachine(IMachineBlockEntity holder, String tierKey) {
        super(holder);
        VoidMinerTier tier = VoidMinerTier.byKey(tierKey);
        if (tier == null) {
            VoidMinerRecipe.LOGGER.error("Unknown void miner tier '{}', falling back to mk1", tierKey);
            tier = VoidMinerTier.MK1;
        }
        this.voidTier = tier;
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new VoidMinerLogic(this);
    }

    @Override
    public @NotNull VoidMinerLogic getRecipeLogic() {
        return (VoidMinerLogic) super.getRecipeLogic();
    }

    public VoidMinerTier getVoidTier() {
        return voidTier;
    }

    @Override
    public boolean keepSubscribing() {
        return true;
    }

    @Nullable
    @Override
    protected GTRecipe getRealRecipe(GTRecipe recipe) {
        return resolveModifier().applyModifier(this, recipe);
    }

    private static final RecipeModifier HATCH_PARALLEL = VoidMinerMachine::hatchParallel;

    private static ModifierFunction hatchParallel(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (machine instanceof IMultiController controller && controller.isFormed()) {
            int parallels = controller.getParallelHatch()
                    .map(hatch -> ParallelLogic.getParallelAmount(machine, recipe, hatch.getCurrentParallel()))
                    .orElse(1);
            if (parallels <= 1) return ModifierFunction.IDENTITY;
            return ModifierFunction.builder()
                    .modifyAllContents(ContentModifier.multiplier(parallels))
                    .eutMultiplier(parallels)
                    .parallels(parallels)
                    .build();
        }
        return ModifierFunction.IDENTITY;
    }

    private RecipeModifier resolveModifier() {
        if (resolvedModifier != null) return resolvedModifier;
        RecipeModifier base = getDefinition().getRecipeModifier();
        RecipeModifier[] mods = base instanceof RecipeModifierList list ? list.getModifiers() :
                new RecipeModifier[] { base };
        boolean hasParallel = false;
        RecipeModifier[] result = new RecipeModifier[mods.length + 1];
        int n = 1;
        for (RecipeModifier mod : mods) {
            if (mod == GTRecipeModifiers.PARALLEL_HATCH || mod == HATCH_PARALLEL) {
                if (!hasParallel) {
                    result[0] = HATCH_PARALLEL;
                    hasParallel = true;
                }
            } else {
                result[n++] = mod;
            }
        }
        if (!hasParallel) result[0] = HATCH_PARALLEL;
        RecipeModifier[] trimmed = new RecipeModifier[n];
        System.arraycopy(result, 0, trimmed, 0, n);
        resolvedModifier = new RecipeModifierList(trimmed);
        return resolvedModifier;
    }

    public Set<ResourceLocation> getSelectedVeinSet() {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        if (selectedVeins.isEmpty()) return out;
        for (String s : selectedVeins.split(",")) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) out.add(id);
        }
        return out;
    }

    public boolean isVeinSelected(ResourceLocation id) {
        return getSelectedVeinSet().contains(id);
    }

    public void toggleVein(ResourceLocation id) {
        Set<ResourceLocation> set = getSelectedVeinSet();
        if (!set.remove(id)) set.add(id);
        applySelection(set);
    }

    public int countSelected(java.util.Collection<ResourceLocation> ids) {
        Set<ResourceLocation> set = getSelectedVeinSet();
        int n = 0;
        for (ResourceLocation id : ids) if (set.contains(id)) n++;
        return n;
    }

    public void toggleGroup(List<ResourceLocation> members, ResourceLocation representative) {
        Set<ResourceLocation> set = getSelectedVeinSet();
        if (members.stream().anyMatch(set::contains)) set.removeAll(members);
        else set.add(representative);
        applySelection(set);
    }

    public void clearVeins() {
        applySelection(new LinkedHashSet<>());
    }

    private void applySelection(Set<ResourceLocation> set) {
        List<String> parts = new ArrayList<>();
        for (ResourceLocation id : set) parts.add(id.toString());
        this.selectedVeins = String.join(",", parts);
        if (!isRemote()) {
            getRecipeLogic().markLastRecipeDirty();
            getRecipeLogic().updateTickSubscription();
        }
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        sideTabs.attachSubTab(new VeinSelectorFancyConfigurator(this));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        int n = getSelectedVeinSet().size();
        if (n == 0) {
            textList.add(Component.translatable("voidminerrecipe.multiblock.no_selection")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        textList.add(Component.translatable("voidminerrecipe.multiblock.selected_veins", n)
                .withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("voidminerrecipe.multiblock.fuel_per_cycle",
                FormattingUtil.formatNumbers((long) voidTier.fuelAmount * n))
                .withStyle(ChatFormatting.GRAY));
        GTRecipe active = getRecipeLogic().isActive() ? getRecipeLogic().getLastRecipe() : null;
        long power = active != null ? RecipeHelper.getRealEUt(active).getTotalEU() : voidTier.voltage() * n;
        textList.add(Component.translatable("voidminerrecipe.multiblock.power_per_tick",
                FormattingUtil.formatNumbers(power))
                .withStyle(ChatFormatting.GRAY));
    }
}
