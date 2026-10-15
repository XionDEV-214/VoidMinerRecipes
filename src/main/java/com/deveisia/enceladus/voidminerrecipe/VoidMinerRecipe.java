package com.deveisia.enceladus.voidminerrecipe;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(VoidMinerRecipe.MOD_ID)
public class VoidMinerRecipe {
    public static final String MOD_ID = "voidminerrecipe";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoidMinerRecipe() {
        LOGGER.info("[{}] loaded", MOD_ID);
    }
}
