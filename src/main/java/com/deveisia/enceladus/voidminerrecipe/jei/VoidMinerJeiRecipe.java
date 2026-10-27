package com.deveisia.enceladus.voidminerrecipe.jei;

import com.deveisia.enceladus.voidminerrecipe.VoidMinerTier;

/** One JEI page = one tier x one ore vein. */
public record VoidMinerJeiRecipe(VoidMinerTier tier, VeinPage vein) {}
