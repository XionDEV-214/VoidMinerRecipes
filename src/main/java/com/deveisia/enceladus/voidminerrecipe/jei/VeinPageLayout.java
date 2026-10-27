package com.deveisia.enceladus.voidminerrecipe.jei;

import com.gregtechceu.gtceu.utils.FormattingUtil;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared layout for "one ore vein per JEI page".
 *
 * <pre>
 * +------------------------------------------------+
 * | Vein name                          Overworld   |
 * | gtceu:magnetite_vein                           |
 * | [ORE]  Magnetite                       [raw]  |
 * |  BLK   45% of vein                     x64    |
 * | [ORE]  Iron                            [raw]  |
 * | ...                                            |
 * |------------------------------------------------|
 * | category specific footer (power, fuel, ...)    |
 * +------------------------------------------------+
 * </pre>
 *
 * Up to 4 ores are listed one per row; 5-8 ores switch to two columns; anything beyond 8 is still
 * registered (searchable, R/U works) but only summarized as "+N more".
 */
public final class VeinPageLayout {
    private VeinPageLayout() {}

    public static final int WIDTH = 176;
    public static final int ROW_TOP = 26;
    public static final int ROW_H = 22;
    public static final int MAX_ROWS = 4;
    /** y where the category specific footer starts. */
    public static final int FOOTER_Y = ROW_TOP + ROW_H * MAX_ROWS + 4;
    public static final int BLOCK_SIZE = 20;

    public static final int TEXT = 0xFF404040;
    public static final int MUTED = 0xFF808080;

    private static int columns(int n) {
        return n <= MAX_ROWS ? 1 : 2;
    }

    private static int shown(int n) {
        return Math.min(n, MAX_ROWS * columns(n));
    }

    private static int colWidth(int cols) {
        return cols == 1 ? WIDTH - 8 : (WIDTH - 8) / 2;
    }

    private static int cellX(int i, int cols) {
        return 4 + (i / MAX_ROWS) * colWidth(cols);
    }

    private static int cellY(int i) {
        return ROW_TOP + (i % MAX_ROWS) * ROW_H;
    }

    /**
     * @param perOre  how many raw ores of each material one cycle yields for this single vein
     * @param exact   whether perOre is exact (false = "base value, before machine bonuses")
     * @param notes   extra tooltip lines appended to every output slot
     */
    public static void setRecipe(IRecipeLayoutBuilder builder, IJeiHelpers helpers, VeinPage page, long perOre,
                                 boolean exact, List<Component> notes) {
        List<VeinPage.Ore> ores = page.ores();
        int cols = columns(ores.size());
        int shown = shown(ores.size());
        int count = (int) Math.max(1, Math.min(Integer.MAX_VALUE, perOre));

        CompactItemRenderer blockRenderer = new CompactItemRenderer(helpers, BLOCK_SIZE, false);
        CompactItemRenderer outRenderer = new CompactItemRenderer(helpers, 16, true);

        for (int i = 0; i < shown; i++) {
            VeinPage.Ore ore = ores.get(i);
            int x = cellX(i, cols);
            int y = cellY(i);
            int w = colWidth(cols);

            // Big ore block. CATALYST role: not consumed, but pressing U on an ore block lists the veins
            // that contain it.
            builder.addSlot(RecipeIngredientRole.CATALYST, x + 1, y + 1)
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, blockRenderer)
                    .addItemStack(ore.block());

            ItemStack out = ore.raw().copy();
            out.setCount(count);
            final double share = ore.share();
            builder.addSlot(RecipeIngredientRole.OUTPUT, x + w - 18, y + 3)
                    .setStandardSlotBackground()
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, outRenderer)
                    .addItemStack(out)
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable(exact ? "voidminerrecipe.jei.per_cycle" :
                                "voidminerrecipe.jei.per_cycle_base", FormattingUtil.formatNumbers(perOre))
                                .withStyle(ChatFormatting.GOLD));
                        if (share >= 0) {
                            tooltip.add(Component.translatable("voidminerrecipe.jei.vein_share", percent(share))
                                    .withStyle(ChatFormatting.GRAY));
                        }
                        for (Component c : notes) tooltip.add(c);
                    });
        }

        if (shown < ores.size()) {
            List<ItemStack> rest = new ArrayList<>();
            for (int i = shown; i < ores.size(); i++) {
                ItemStack s = ores.get(i).raw().copy();
                s.setCount(count);
                rest.add(s);
            }
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(rest);
        }
    }

    public static void drawHeader(GuiGraphics gg, Font font, VeinPage page) {
        String dims = dimensionText(page.dimensions());
        int dimsW = dims.isEmpty() ? 0 : font.width(dims) + 6;
        gg.drawString(font, fit(font, page.name(), WIDTH - 8 - dimsW), 4, 4, TEXT, false);
        if (!dims.isEmpty()) gg.drawString(font, dims, WIDTH - 4 - font.width(dims), 4, MUTED, false);
        gg.drawString(font, fit(font, page.id().toString(), WIDTH - 8), 4, 14, MUTED, false);
    }

    public static void drawRows(GuiGraphics gg, Font font, VeinPage page) {
        List<VeinPage.Ore> ores = page.ores();
        int cols = columns(ores.size());
        int shown = shown(ores.size());
        for (int i = 0; i < shown; i++) {
            VeinPage.Ore ore = ores.get(i);
            int x = cellX(i, cols);
            int y = cellY(i);
            int w = colWidth(cols);
            int textX = x + BLOCK_SIZE + 5;
            int textW = w - BLOCK_SIZE - 5 - 22;
            gg.drawString(font, fit(font, ore.name(), textW), textX, y + 3, TEXT, false);
            if (ore.share() >= 0) {
                String s = cols == 1 ?
                        Component.translatable("voidminerrecipe.jei.vein_share", percent(ore.share())).getString() :
                        percent(ore.share()) + "%";
                gg.drawString(font, fit(font, s, textW), textX, y + 13, MUTED, false);
            }
        }
        if (shown < ores.size()) {
            String more = Component.translatable("voidminerrecipe.jei.more_ores", ores.size() - shown).getString();
            gg.drawString(font, more, WIDTH - 4 - font.width(more), FOOTER_Y - 10, MUTED, false);
        }
        gg.fill(4, FOOTER_Y - 2, WIDTH - 4, FOOTER_Y - 1, 0x30000000);
    }

    public static String percent(double share) {
        double p = share * 100;
        return p >= 10 || p == 0 ? String.format(Locale.ROOT, "%.0f", p) : String.format(Locale.ROOT, "%.1f", p);
    }

    public static String fit(Font font, String s, int width) {
        if (width <= 0) return "";
        if (font.width(s) <= width) return s;
        String ell = "...";
        return font.plainSubstrByWidth(s, Math.max(0, width - font.width(ell))) + ell;
    }

    private static String dimensionText(List<ResourceLocation> dims) {
        if (dims.isEmpty()) return "";
        String first = pretty(dims.get(0));
        return dims.size() == 1 ? first : first + " +" + (dims.size() - 1);
    }

    private static String pretty(ResourceLocation id) {
        String key = "dimension." + id.getNamespace() + "." + id.getPath();
        Component c = Component.translatable(key);
        String s = c.getString();
        if (!s.equals(key)) return s;
        StringBuilder sb = new StringBuilder();
        for (String p : id.getPath().split("[_/]")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
