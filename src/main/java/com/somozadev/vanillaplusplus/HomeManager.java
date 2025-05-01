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
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.units.qual.C;

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

    private static final Map<UUID, Map<String, BlockPos>> homes = new HashMap<>();
    private static final Map<UUID, Map<String, BlockPos>> sharedHomes = new HashMap<>();


    public static CompletableFuture<Suggestions> suggestHomes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder)
    {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        }catch (CommandSyntaxException e) {
            return Suggestions.empty();
        }

        Map<String, BlockPos> playerHomes = homes.get(player.getUUID());
        if(playerHomes!=null)
        {
            for(String homeName:playerHomes.keySet()){
                builder.suggest(homeName);
            }
        }
        return builder.buildFuture();
    }
    public static void SetHome(ServerPlayer player, String name){
        homes.computeIfAbsent(player.getUUID(), k->new HashMap<>()).put(name, player.blockPosition());
    }
    public static void SetHome(ServerPlayer player, String name, BlockPos pos){
        homes.computeIfAbsent(player.getUUID(), k->new HashMap<>()).put(name, pos);
    }
    public static boolean CanSetMoreHomes(ServerPlayer player)
    {   Map<String, BlockPos> ph = homes.get(player.getUUID());
        int currentCount = (ph != null) ? ph.size() : 0;
        int maxCount = VanillaPlusPlusConfig.COMMON.maxHomes.get();
        return currentCount < maxCount;
    }
    public static BlockPos GetHome(UUID playerId, String name) {
        Map<String, BlockPos> ph = homes.get(playerId);
        if (ph == null) return null;

        return ph.get(name);
    }
    public static BlockPos GetHome(ServerPlayer player, String name){
        Map<String, BlockPos> layerHomes = homes.get(player.getUUID());
        if(layerHomes != null){
            return layerHomes.get(name);
        }
        return null;
    }
    public static void RemoveHome(ServerPlayer player, String name){
        Map<String, BlockPos> playerHomes = homes.get(player.getUUID());
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

    public static void LoadHomes()
    {
        if(!FILE.exists()){
            try(FileReader reader = new FileReader(FILE)){
                Map<UUID, Map<String, BlockPos>> loaded = GSON.fromJson(reader, TYPE);
                if (loaded != null) {
                    homes.clear();
                    homes.putAll(loaded);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void ShareHomeToChat(ServerPlayer sender, String homeName){
        BlockPos pos = GetHome(sender, homeName);
        if (pos == null) return;

        sharedHomes.computeIfAbsent(sender.getUUID(), k -> new HashMap<>()).put(homeName, pos);

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
        BlockPos pos = sharedHomes.get(ownerUUID).get(homeName);
        SetHome(player, homeName, pos);
        player.sendSystemMessage(Component.literal("§o §e Home §l '" + homeName + "' §r §o §e added to your list."), false);
    }

    public static boolean HasHome(ServerPlayer player, String homeName){
        Map<String, BlockPos> playerHomes = homes.get(player.getUUID());
        return playerHomes != null && playerHomes.containsKey(homeName);
    }



    public class SetHomeCommand {
        public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            Map<String, BlockPos> playerHomes = homes.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
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
            if(pos != null){
                player.teleportTo(pos.getX()+.5, pos.getY(), pos.getZ()+.5);
                context.getSource().sendSuccess(() -> Component.literal("Teleported to home '" + name + "'!"), false);
                CooldownManager.SetCooldown(player, "home");
            }else {
                context.getSource().sendSuccess(() -> Component.literal("Home '" + name + "' not found."), false);
            }
            return 1;
        }
    }
}
