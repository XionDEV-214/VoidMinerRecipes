package com.deveisia.enceladus.voidminerrecipe.util;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record VeinGroup(ResourceLocation representative, List<ResourceLocation> members, int ownCount) {}
