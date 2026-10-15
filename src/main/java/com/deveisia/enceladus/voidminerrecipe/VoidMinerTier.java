package com.deveisia.enceladus.voidminerrecipe;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

public enum VoidMinerTier {
    MK1("mk1", GTValues.LuV, 400, 1, "gtceu:oganesson", 1000),
    MK2("mk2", GTValues.ZPM, 200, 1, "gtceu:metastable_oganesson", 1000),
    MK3("mk3", GTValues.UV, 100, 1, "gtceu:star_wrath", 1000),
    MK4("mk4", GTValues.UHV, 50, 1, "gtceu:quark_gluon_plasma", 1000),
    SPACE_ELEVATOR("space_elevator", GTValues.UEV, 72000, 1024000,
            "gtceu:phononic_quark_gluon_plasma_fuel", 2048000);

    public final String key;

    public final int voltageTier;
    public final int duration;

    public final int outputCount;
    public final ResourceLocation fuelId;

    public final int fuelAmount;

    VoidMinerTier(String key, int voltageTier, int duration, int outputCount, String fuelId, int fuelAmount) {
        this.key = key;
        this.voltageTier = voltageTier;
        this.duration = duration;
        this.outputCount = outputCount;
        this.fuelId = new ResourceLocation(fuelId);
        this.fuelAmount = fuelAmount;
    }

    public long voltage() {
        return GTValues.VA[voltageTier];
    }

    @Nullable
    public Fluid fuelFluid() {
        Fluid f = ForgeRegistries.FLUIDS.getValue(fuelId);
        return f == null || f == Fluids.EMPTY ? null : f;
    }

    public String langKey() {
        return VoidMinerRecipe.MOD_ID + ".tier." + key;
    }

    @Nullable
    public static VoidMinerTier byKey(String key) {
        for (VoidMinerTier t : values()) {
            if (t.key.equals(key)) return t;
        }
        return null;
    }
}
