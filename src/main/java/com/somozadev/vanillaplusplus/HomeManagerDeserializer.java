package com.somozadev.vanillaplusplus;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;


import java.lang.reflect.Type;

public class HomeManagerDeserializer  implements JsonDeserializer<HomeData> {
    @Override
    public HomeData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        JsonObject posObj = obj.getAsJsonObject("pos");
        System.out.println("Pos: " + posObj);
        int x = posObj.get("x").getAsInt();
        int y = posObj.get("y").getAsInt();
        int z = posObj.get("z").getAsInt();
        BlockPos pos = new BlockPos(x, y, z);

        JsonObject dimObj = obj.getAsJsonObject("dimension");
        JsonObject locationObj = dimObj.getAsJsonObject("location");
        String namespace = locationObj.get("namespace").getAsString();
        String path = locationObj.get("path").getAsString();

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(namespace, path));

        return new HomeData(dimension, pos);
    }
}