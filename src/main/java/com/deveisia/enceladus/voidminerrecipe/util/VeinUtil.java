package com.deveisia.enceladus.voidminerrecipe.util;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.generator.VeinGenerator;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.ClientProxy;

import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class VeinUtil {
    private VeinUtil() {}

    private static final class ClientSide {
        static Set<ResourceLocation> keys() {
            return ClientProxy.CLIENT_ORE_VEINS.keySet();
        }

        @Nullable
        static GTOreDefinition get(ResourceLocation id) {
            return ClientProxy.CLIENT_ORE_VEINS.get(id);
        }
    }

    @Nullable
    public static GTOreDefinition get(ResourceLocation id, boolean client) {
        return client ? ClientSide.get(id) : GTRegistries.ORE_VEINS.get(id);
    }

    public static List<ResourceLocation> ids(boolean client) {
        Collection<ResourceLocation> src = client ? ClientSide.keys() : GTRegistries.ORE_VEINS.keys();
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation id : src) {
            GTOreDefinition def = get(id, client);
            if (def != null && !materials(def).isEmpty()) out.add(id);
        }
        out.sort(Comparator.comparing(ResourceLocation::toString));
        return out;
    }

    public static List<VeinGroup> groups(boolean client) {
        List<ResourceLocation> ids = ids(client);
        Map<Set<String>, List<ResourceLocation>> classes = new LinkedHashMap<>();
        for (ResourceLocation id : ids) {
            Set<String> set = new HashSet<>();
            for (Material m : materials(get(id, client))) set.add(m.getName());
            classes.computeIfAbsent(set, k -> new ArrayList<>()).add(id);
        }

        List<Set<String>> keys = new ArrayList<>(classes.keySet());
        List<Set<String>> maximal = new ArrayList<>();
        for (Set<String> k : keys) {
            boolean dominated = false;
            for (Set<String> o : keys) {
                if (o.size() > k.size() && o.containsAll(k)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) maximal.add(k);
        }

        Map<Set<String>, List<ResourceLocation>> absorbed = new HashMap<>();
        for (Set<String> k : keys) {
            if (maximal.contains(k)) continue;
            Set<String> best = null;
            for (Set<String> m : maximal) {
                if (!m.containsAll(k)) continue;
                if (best == null || m.size() < best.size() ||
                        (m.size() == best.size() &&
                                classes.get(m).get(0).toString().compareTo(classes.get(best).get(0).toString()) < 0)) {
                    best = m;
                }
            }
            if (best != null) absorbed.computeIfAbsent(best, x -> new ArrayList<>()).addAll(classes.get(k));
        }

        List<VeinGroup> out = new ArrayList<>();
        for (Set<String> k : maximal) {
            List<ResourceLocation> own = classes.get(k);
            List<ResourceLocation> members = new ArrayList<>(own);
            List<ResourceLocation> extra = new ArrayList<>(absorbed.getOrDefault(k, List.of()));
            extra.sort(Comparator.comparing(ResourceLocation::toString));
            members.addAll(extra);
            out.add(new VeinGroup(own.get(0), members, own.size()));
        }
        out.sort(Comparator.comparing(g -> g.representative().toString()));
        return out;
    }

    public static List<Material> materials(@Nullable GTOreDefinition def) {
        if (def == null) return List.of();
        VeinGenerator gen = def.veinGenerator();
        if (gen == null) return List.of();
        Set<String> seen = new HashSet<>();
        List<Material> out = new ArrayList<>();
        for (Material mat : gen.getAllMaterials()) {
            if (!seen.add(mat.getName())) continue;
            if (ChemicalHelper.get(TagPrefix.rawOre, mat, 1).isEmpty()) continue;
            out.add(mat);
        }
        return out;
    }

    public static List<ItemStack> oreStacks(@Nullable GTOreDefinition def, int count) {
        List<ItemStack> out = new ArrayList<>();
        for (Material mat : materials(def)) {
            out.add(ChemicalHelper.get(TagPrefix.rawOre, mat, count));
        }
        return out;
    }

    public static String nameKey(ResourceLocation id) {
        return id.getNamespace() + ".jei.ore_vein." + id.getPath();
    }

    public static String displayName(ResourceLocation id) {
        String key = nameKey(id);
        Language lang = Language.getInstance();
        if (lang.has(key)) return lang.getOrDefault(key);
        String[] parts = id.getPath().replace('/', ' ').split("[_ ]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
