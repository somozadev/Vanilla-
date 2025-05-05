package com.somozadev.vanillaplusplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Relative;
import java.util.Set;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class HomeManager{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<UUID, Map<String, BlockPos>>>(){}.getType();
    private static final File FILE = new File("config/vanillaplusplus/homes.json");

    private static final Map<UUID, Map<String, HomeData>> homes = new HashMap<>();
    private static final Map<UUID, Map<String, HomeData>> sharedHomes = new HashMap<>();




    public static CompletableFuture<Suggestions> suggestHomes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder)
    {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        }catch (CommandSyntaxException e) {
            return Suggestions.empty();
        }

        Map<String, HomeData> playerHomes = homes.get(player.getUUID());
        if(playerHomes!=null)
        {
            for(String homeName:playerHomes.keySet()){
                builder.suggest(homeName);
            }
        }
        return builder.buildFuture();
    }
    public static void SetHome(ServerPlayer player, String name){
        homes.computeIfAbsent(player.getUUID(), k->new HashMap<>()).put(name, new HomeData(player.level().dimension(),player.blockPosition()));
    }
    public static void SetHome(ServerPlayer player, String name, ResourceKey<Level> dimension, BlockPos pos){
        homes.computeIfAbsent(player.getUUID(), k->new HashMap<>()).put(name, new HomeData(dimension, pos));
    }
    public static boolean CanSetMoreHomes(ServerPlayer player)
    {   Map<String, HomeData> ph = homes.get(player.getUUID());
        int currentCount = (ph != null) ? ph.size() : 0;
        int maxCount = VanillaPlusPlusConfig.COMMON.maxHomes.get();
        return currentCount < maxCount;
    }
    public static BlockPos GetHome(UUID playerId, String name) {
        Map<String, HomeData> ph = homes.get(playerId);
        if (ph == null) return null;

        return ph.get(name).pos;
    }
    public static BlockPos GetHome(ServerPlayer player, String name){
        Map<String, HomeData> layerHomes = homes.get(player.getUUID());
        if(layerHomes != null){
            return layerHomes.get(name).pos;
        }
        return null;
    }
    public static ResourceKey<Level> GetDimension(ServerPlayer player, String name){
        Map<String, HomeData> layerHomes = homes.get(player.getUUID());
        if(layerHomes != null){
            return layerHomes.get(name).dimension;
        }
        return null;
    }
    public static void RemoveHome(ServerPlayer player, String name){
        Map<String, HomeData> playerHomes = homes.get(player.getUUID());
        if(playerHomes != null){
            playerHomes.remove(name);
        }
    }

    public static void SaveHomes()
    {
        try {
            FILE.getParentFile().mkdirs();
            try(FileWriter writer = new FileWriter(FILE)){
                GSON.toJson(homes, TYPE, writer);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void LoadHomes() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                Map<UUID, Map<String, HomeData>> loaded = GSON.fromJson(reader, TYPE);
                if (loaded != null) {
                    homes.clear();
                    homes.putAll(loaded);
                    //    System.out.println("[VanillaPlusPlus] Homes loaded successfully.");
                }
            } catch (Exception e) {
                //System.err.println("[VanillaPlusPlus] Failed to load homes:");
                e.printStackTrace();
            }
        } else {
            try {
                FILE.getParentFile().mkdirs();
                FILE.createNewFile();
                try (FileWriter writer = new FileWriter(FILE)) {
                    GSON.toJson(homes, TYPE, writer); // Guardar estructura vacía
                }
                //System.out.println("[VanillaPlusPlus] Created new homes.json file.");
            } catch (IOException e) {
                //System.err.println("[VanillaPlusPlus] Failed to create homes.json:");
                e.printStackTrace();
            }
        }
    }

    public static void ShareHomeToChat(ServerPlayer sender, String homeName){
        BlockPos pos = GetHome(sender, homeName);
        if (pos == null) return;

        sharedHomes.computeIfAbsent(sender.getUUID(), k -> new HashMap<>()).put(homeName, new HomeData(sender.level().dimension(),pos));

        String command = "/acceptsharedhome " + sender.getUUID() + " " + homeName;
        MutableComponent msg = Component.literal(sender.getName().getString() + " shared a home: " ).append(Component.literal("[" + homeName + "]")).withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to save this home to your list"))));
        sender.getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }
    public static void AcceptSharedHome(ServerPlayer player, UUID ownerUUID, String homeName) {
        if (!sharedHomes.containsKey(ownerUUID) || !sharedHomes.get(ownerUUID).containsKey(homeName)) {
            player.sendSystemMessage(Component.literal("§c §o This shared home no longer exists or was not shared."), false);
            return;
        }
        if (!CanSetMoreHomes(player)) {
            player.sendSystemMessage(Component.literal("§c §o You have reached your home limit."), false);
            return;
        }
        String baseName = homeName;
        int counter = 1;
        while (HasHome(player, homeName)) {
            homeName = baseName + "_" + counter++;
        }
        BlockPos pos = sharedHomes.get(ownerUUID).get(homeName).pos;
        ResourceKey<Level> dim = sharedHomes.get(ownerUUID).get(homeName).dimension;
        SetHome(player, homeName, dim, pos);
        player.sendSystemMessage(Component.literal("§o §e Home §l '" + homeName + "' §r §o §e added to your list."), false);
    }

    public static boolean HasHome(ServerPlayer player, String homeName){
        Map<String, HomeData> playerHomes = homes.get(player.getUUID());
        return playerHomes != null && playerHomes.containsKey(homeName);
    }



    public class SetHomeCommand {
        public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            Map<String, HomeData> playerHomes = homes.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
            if (playerHomes.size() >= VanillaPlusPlus.GetMaxHomes() && !playerHomes.containsKey(name)) {
                context.getSource().sendFailure(Component.literal("You have the maximum amount of homes!"));
                return 0;
            }
            HomeManager.SetHome(player, name);
            context.getSource().sendSuccess(() -> Component.literal("Home '" + name + "' set!"), false);
            HomeManager.SaveHomes();
            return 1;
        }
    }

    public class DelHomeCommand {
        public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            HomeManager.RemoveHome(player, name);
            context.getSource().sendSuccess(() -> Component.literal("Home '" + name + "' deleted!"), false);
            HomeManager.SaveHomes();
            return 1;
        }
        }

    public class HomeCommand {
        public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            if(CooldownManager.IsOnCooldown(player, "home", VanillaPlusPlus.GetTeleportDelay())) {
                String timeLeft = String.format("You must wait %d seconds.", CooldownManager.GetRemainingTime(player, "home", VanillaPlusPlus.GetTeleportDelay()) / 1000 );
                context.getSource().sendFailure(Component.literal(timeLeft));
                return 0;
            }
            BlockPos pos = HomeManager.GetHome(player, name);
            ResourceKey<Level> dim = HomeManager.GetDimension(player, name);

            if(pos != null){

                ServerLevel level = player.getServer().getLevel(dim);
                player.teleportTo(level,pos.getX() + 0.5, (double) pos.getY(),pos.getZ() + 0.5,Set.of(), player.getYRot(),player.getXRot(),false );
                context.getSource().sendSuccess(() -> Component.literal("Teleported to home '" + name + "'!"), false);
                CooldownManager.SetCooldown(player, "home");
            }else {
                context.getSource().sendSuccess(() -> Component.literal("Home '" + name + "' not found."), false);
            }
            return 1;
        }
    }
}
