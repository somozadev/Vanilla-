package com.somozadev.vanillaplusplus;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(VanillaPlusPlus.MODID)
public class VanillaPlusPlus {
    public static final String MODID = "vanillaplusplus";

    public VanillaPlusPlus() {
        MinecraftForge.EVENT_BUS.register(this);
        HomeManager.LoadHomes();
        ModLoadingContext mlCtx = new ModLoadingContext();
        mlCtx.registerConfig(ModConfig.Type.COMMON, VanillaPlusPlusConfig.COMMON_CONFIG);
    }

    public static long GetTeleportDelay(){
        return VanillaPlusPlusConfig.COMMON.teleportDelay.get() * 1000L;
    }
    public static long GetMaxHomes() {
        return VanillaPlusPlusConfig.COMMON.maxHomes.get();
    }


}

