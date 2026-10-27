package com.deveisia.enceladus.voidminerrecipe.jei;

import com.deveisia.enceladus.voidminerrecipe.util.VeinUtil;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGenLayers;
import com.gregtechceu.gtceu.api.data.worldgen.generator.VeinGenerator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything one JEI page needs to show a single ore vein: its name, where it generates and,
 * for every ore in it, the ore block (for the big preview) and the raw ore it yields.
 */
public record VeinPage(ResourceLocation id, String name, List<ResourceLocation> dimensions, List<Ore> ores) {

    /**
     * @param block the ore block as it appears in the world (stone / deepslate / netherrack / endstone variant)
     * @param raw   the raw ore the miner actually outputs (count 1, scaled by the category)
     * @param share fraction of the vein's ore blocks made of this material, 0..1 (or -1 if unknown)
     */
    public record Ore(Material material, String name, ItemStack block, ItemStack raw, double share) {}

    @Nullable
    public static VeinPage build(ResourceLocation id, @Nullable GTOreDefinition def) {
        List<Material> mats = VeinUtil.materials(def);
        if (def == null || mats.isEmpty()) return null;

        Map<String, Integer> weights = new HashMap<>();
        int total = 0;
        try {
            VeinGenerator gen = def.veinGenerator();
            if (gen != null) {
                for (VeinGenerator.VeinEntry entry : gen.getAllEntries()) {
                    Material m = entry.mapToMaterial();
                    if (m == null || m.isNull()) continue;
                    weights.merge(m.getName(), entry.chance(), Integer::sum);
                    total += entry.chance();
                }
            }
        } catch (Throwable ignored) {
            weights.clear();
            total = 0;
        }

        TagPrefix prefix = orePrefix(def);
        List<Ore> ores = new ArrayList<>();
        for (Material mat : mats) {
            ItemStack raw = ChemicalHelper.get(TagPrefix.rawOre, mat, 1);
            if (raw.isEmpty()) continue;
            ItemStack block = safeGet(prefix, mat);
            if (block.isEmpty() && prefix != TagPrefix.ore) block = safeGet(TagPrefix.ore, mat);
            if (block.isEmpty()) block = raw.copy();
            double share = total > 0 ? weights.getOrDefault(mat.getName(), 0) / (double) total : -1;
            ores.add(new Ore(mat, mat.getLocalizedName().getString(), block, raw, share));
        }
        if (ores.isEmpty()) return null;

        // Highest share first so the "main" ore of the vein is always the top row.
        ores.sort((a, b) -> Double.compare(b.share(), a.share()));

        List<ResourceLocation> dims = new ArrayList<>();
        try {
            if (def.dimensionFilter() != null) {
                for (ResourceKey<Level> key : def.dimensionFilter()) dims.add(key.location());
            }
        } catch (Throwable ignored) {}
        dims.sort((a, b) -> a.toString().compareTo(b.toString()));

        return new VeinPage(id, VeinUtil.displayName(id), List.copyOf(dims), List.copyOf(ores));
    }

    private static ItemStack safeGet(TagPrefix prefix, Material mat) {
        try {
            ItemStack s = ChemicalHelper.get(prefix, mat, 1);
            return s == null ? ItemStack.EMPTY : s;
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private static TagPrefix orePrefix(GTOreDefinition def) {
        IWorldGenLayer layer;
        try {
            layer = def.layer();
        } catch (Throwable t) {
            return TagPrefix.ore;
        }
        if (layer == WorldGenLayers.NETHERRACK) return TagPrefix.oreNetherrack;
        if (layer == WorldGenLayers.ENDSTONE) return TagPrefix.oreEndstone;
        if (layer == WorldGenLayers.DEEPSLATE) return TagPrefix.oreDeepslate;
        return TagPrefix.ore;
    }

    public List<ItemStack> rawOres() {
        List<ItemStack> out = new ArrayList<>(ores.size());
        for (Ore o : ores) out.add(o.raw());
        return out;
    }
}
