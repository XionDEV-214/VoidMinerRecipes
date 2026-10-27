package com.deveisia.enceladus.voidminerrecipe.jei;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerRecipe;
import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** One JEI tab per Void Miner tier; every page of a tab is a single ore vein. */
public class VoidMinerJeiCategory implements IRecipeCategory<VoidMinerJeiRecipe> {
    private static final Map<VoidMinerTier, RecipeType<VoidMinerJeiRecipe>> TYPES = new EnumMap<>(VoidMinerTier.class);

    static {
        for (VoidMinerTier tier : VoidMinerTier.values()) {
            TYPES.put(tier, RecipeType.create(VoidMinerRecipe.MOD_ID, "void_miner_" + tier.key,
                    VoidMinerJeiRecipe.class));
        }
    }

    public static RecipeType<VoidMinerJeiRecipe> type(VoidMinerTier tier) {
        return TYPES.get(tier);
    }

    private static final int WIDTH = VeinPageLayout.WIDTH;
    private static final int FOOTER = VeinPageLayout.FOOTER_Y;
    private static final int HEIGHT = FOOTER + 34;
    private static final int TEXT = VeinPageLayout.TEXT;
    private static final int MUTED = VeinPageLayout.MUTED;

    private final VoidMinerTier tier;
    private final IJeiHelpers helpers;
    private final IDrawable icon;

    public VoidMinerJeiCategory(IJeiHelpers helpers, VoidMinerTier tier, ItemStack iconStack) {
        this.tier = tier;
        this.helpers = helpers;
        this.icon = helpers.getGuiHelper().createDrawableItemStack(iconStack);
    }

    @Override
    public @NotNull RecipeType<VoidMinerJeiRecipe> getRecipeType() {
        return type(tier);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable(tier.langKey());
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(VoidMinerJeiRecipe recipe) {
        return new ResourceLocation(VoidMinerRecipe.MOD_ID, tier.key + "/" + recipe.vein().id().getNamespace() +
                "/" + recipe.vein().id().getPath());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VoidMinerJeiRecipe recipe, IFocusGroup focuses) {
        VeinPageLayout.setRecipe(builder, helpers, recipe.vein(), tier.outputCount, true, List.of(
                Component.translatable("voidminerrecipe.jei.multi_select").withStyle(ChatFormatting.DARK_GRAY)));

        Fluid fuel = tier.fuelFluid();
        if (fuel != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, FOOTER + 9)
                    .setStandardSlotBackground()
                    .addFluidStack(fuel, tier.fuelAmount)
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                            Component.translatable("voidminerrecipe.jei.multi_select")
                                    .withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void draw(VoidMinerJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gg, double mouseX,
                     double mouseY) {
        Font font = Minecraft.getInstance().font;
        VeinPageLayout.drawHeader(gg, font, recipe.vein());
        VeinPageLayout.drawRows(gg, font, recipe.vein());

        int x = 27;
        int w = WIDTH - x - 4;
        String power = GTValues.VNF[tier.voltageTier] + ChatFormatting.RESET + " " +
                Component.translatable("voidminerrecipe.jei.power", FormattingUtil.formatNumbers(tier.voltage()))
                        .getString();
        gg.drawString(font, VeinPageLayout.fit(font, power, w), x, FOOTER + 2, TEXT, false);
        gg.drawString(font, VeinPageLayout.fit(font, Component.translatable("voidminerrecipe.jei.duration",
                FormattingUtil.formatNumbers(tier.duration / 20.0)).getString(), w), x, FOOTER + 12, TEXT, false);
        gg.drawString(font, VeinPageLayout.fit(font, Component.translatable("voidminerrecipe.jei.fuel_amount",
                FormattingUtil.formatNumbers(tier.fuelAmount)).getString(), w), x, FOOTER + 22, MUTED, false);
        if (tier.fuelFluid() == null) {
            gg.drawString(font, "?", 10, FOOTER + 13, 0xFFAA0000, false);
        }
    }
}
