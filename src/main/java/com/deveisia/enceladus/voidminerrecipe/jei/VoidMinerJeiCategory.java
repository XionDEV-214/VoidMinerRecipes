package com.deveisia.enceladus.voidminerrecipe.jei;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerRecipe;
import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VoidMinerJeiCategory implements IRecipeCategory<VoidMinerJeiRecipe> {
    public static final RecipeType<VoidMinerJeiRecipe> TYPE = RecipeType.create(VoidMinerRecipe.MOD_ID,
            "void_miner", VoidMinerJeiRecipe.class);

    private static final int WIDTH = 176;
    private static final int HEIGHT = 124;

    private static final long CYCLE_MS = 1000L;
    private static final int COLS = 8;
    private static final int TEXT = 0xFF404040;

    private final IDrawable background;
    private final IDrawable icon;

    public VoidMinerJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ChemicalHelper.get(TagPrefix.rawOre, GTMaterials.Iron));
    }

    @Override
    public @NotNull RecipeType<VoidMinerJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("voidminerrecipe.jei.category");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VoidMinerJeiRecipe recipe, IFocusGroup focuses) {
        VoidMinerTier tier = recipe.tier();
        Fluid fuel = tier.fuelFluid();
        if (fuel != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 6, 20).addFluidStack(fuel, tier.fuelAmount);
        }

        List<ItemStack> all = new ArrayList<>();
        for (var vein : recipe.veins()) all.addAll(vein.ores());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(all);
    }

    private static String compact(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000.0);
        return Integer.toString(n);
    }

    @Override
    public void draw(VoidMinerJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gg, double mouseX,
                     double mouseY) {
        Font font = Minecraft.getInstance().font;
        VoidMinerTier tier = recipe.tier();

        gg.drawString(font, Component.translatable(tier.langKey()), 6, 4, TEXT, false);
        gg.drawString(font, Component.translatable("voidminerrecipe.jei.fuel"), 6, 42, TEXT, false);

        List<VoidMinerJeiRecipe.VeinView> veins = recipe.veins();
        if (!veins.isEmpty()) {
            int idx = (int) ((System.currentTimeMillis() / CYCLE_MS) % veins.size());
            var vein = veins.get(idx);

            gg.drawString(font, vein.name(), 30, 16, TEXT, false);
            gg.drawString(font, (idx + 1) + " / " + veins.size(), WIDTH - 6 - font.width(
                    (idx + 1) + " / " + veins.size()), 4, TEXT, false);

            List<ItemStack> ores = vein.ores();
            for (int i = 0; i < ores.size() && i < COLS * 3; i++) {
                int x = 30 + (i % COLS) * 18;
                int y = 28 + (i / COLS) * 18;
                gg.renderItem(ores.get(i), x, y);
                if (tier.outputCount > 1) {
                    gg.renderItemDecorations(font, ores.get(i), x, y,
                            compact(tier.outputCount));
                }
            }
        }

        int y = 86;
        gg.drawString(font, Component.translatable("voidminerrecipe.jei.power", tier.voltage()), 6, y, TEXT,
                false);
        gg.drawString(font, Component.translatable("voidminerrecipe.jei.duration",
                FormattingUtil.formatNumbers(tier.duration / 20.0)), 6, y + 10, TEXT, false);
        gg.drawString(font, Component.translatable("voidminerrecipe.jei.fuel_amount",
                FormattingUtil.formatNumbers(tier.fuelAmount)), 6, y + 20, TEXT, false);
        gg.drawString(font, Component.translatable("voidminerrecipe.jei.multi_select"), 6, y + 30, 0xFF808080,
                false);
    }
}
