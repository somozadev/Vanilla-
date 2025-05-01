package com.somozadev.vanillaplusplus;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class CommandRegistry
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("sethome").
                        then(Commands.argument("name", StringArgumentType.word()).
                                executes(HomeManager.SetHomeCommand::execute)));
        dispatcher.register(
                Commands.literal("delhome").
                        then(Commands.argument("name",StringArgumentType.word()).suggests(HomeManager::suggestHomes).
                                executes(HomeManager.DelHomeCommand::execute)));
        dispatcher.register(
                Commands.literal("home").
                        then(Commands.argument("name",StringArgumentType.word()).suggests(HomeManager::suggestHomes).
                                executes(HomeManager.HomeCommand::execute)));
        dispatcher.register(Commands.literal("share").then(Commands.literal("home").then(Commands.argument("name", StringArgumentType.word()).suggests(HomeManager::suggestHomes).executes(
                context -> {
                    ServerPlayer sender = context.getSource().getPlayerOrException();
                    String homeName = StringArgumentType.getString(context, "name");
                    if(!HomeManager.HasHome(sender, homeName)) {
                        context.getSource().sendFailure(Component.literal("§c You don't have a home with that name."));
                        return 0;
                    }
                    HomeManager.ShareHomeToChat(sender, homeName);
                    return 1;
                }))));
        dispatcher.register(
                Commands.literal("acceptsharedhome")
                        .requires(source -> source.hasPermission(0)) // hidden
                        .then(Commands.argument("ownerUUID", StringArgumentType.string())
                                .then(Commands.argument("homeName", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            UUID ownerUUID = UUID.fromString(StringArgumentType.getString(context, "ownerUUID"));
                                            String homeName = StringArgumentType.getString(context, "homeName");
                                            HomeManager.AcceptSharedHome(player, ownerUUID, homeName);
                                            return 1;
                                        })))
        );



        dispatcher.register(
                Commands.literal("vote")
                        .then(Commands.literal("gotosleep").executes(context -> {
                                    if (DemocracyManager.StartVote(DemocracyManager.VoteType.SLEEP, VanillaPlusPlusConfig.COMMON.voteGoToSleepTimeout.get())) {
                                        context.getSource().getServer().getPlayerList().broadcastSystemMessage(Component.literal("§a §o Voting for skipping night started!"), false);
                                        DemocracyManager.Vote( context.getSource().getPlayerOrException(), true);

                                    } else {
                                        context.getSource().sendFailure(Component.literal("There is already an active vote."));
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("clearweather").executes(context -> {
                            if (DemocracyManager.StartVote(DemocracyManager.VoteType.WEATHER, VanillaPlusPlusConfig.COMMON.voteGoToSleepTimeout.get())) {
                                context.getSource().getServer().getPlayerList().broadcastSystemMessage(Component.literal("§a §o Voting for clearing weather started!"), false);
                                DemocracyManager.Vote( context.getSource().getPlayerOrException(), true);
                            } else {
                                context.getSource().sendFailure(Component.literal("There is already an active vote."));
                            }
                            return 1;
                        }))
                        .then(Commands.literal("yes").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!DemocracyManager.Vote(player, true)) {
                                        context.getSource().sendFailure(Component.literal("Couldn't vote. Maybe you already voted ?"));
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("no").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!DemocracyManager.Vote(player, false)) {
                                        context.getSource().sendFailure(Component.literal("Couldn't vote. Maybe you already voted ?"));
                                    }
                                    return 1;
                                }))
        );

    }
}
