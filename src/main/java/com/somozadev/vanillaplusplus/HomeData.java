package com.somozadev.vanillaplusplus;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public  class HomeData {
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    public HomeData(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }
    public BlockPos GetPos() {
        return pos;
    }

    public ResourceKey<Level> GetDimension() {
        return dimension;
    }
}
