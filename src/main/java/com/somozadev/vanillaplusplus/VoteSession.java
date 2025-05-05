package com.somozadev.vanillaplusplus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.stats.Stats.*;

public class VoteSession {
    public final DemocracyManager.VoteType type;
    public final Set<UUID> yesVotes = new HashSet<>();
    public final Set<UUID> noVotes = new HashSet<>();
    public final long startTime;
    public final long durationMs;

    public VoteSession(DemocracyManager.VoteType type, long durationMs) {
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.durationMs = durationMs + 1000; //adds 1 sg extra to sync with logs
    }

    public boolean IsExpired() {
        return System.currentTimeMillis() > startTime + durationMs;
    }

    public boolean Vote(ServerPlayer player, boolean vote) {

        UUID uuid = player.getUUID();
        if (yesVotes.contains(uuid) || noVotes.contains(uuid)) {
            return false; //call it player already voted  >
        }
        if (vote) {
            yesVotes.add(uuid);
        } else {
            noVotes.add(uuid);
        }


        String voteMessage = "§o §7 "+ player.getDisplayName().getString() + " voted " + (vote ? "§a YES" : "§c NO");
        ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastSystemMessage(Component.literal(voteMessage), false);

        int totalPlayers = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerCount();
        int majority = (totalPlayers / 2) + 1;
        if(yesVotes.size() >= majority) {
            DemocracyManager.FinishVote();
        }

        //TODO: TP , SORT , LOCK CHESTS , WHISPER,


        return true;
    }


    public void executeAction() {

        switch (type) {
            case WEATHER -> {
                int durationMinutes = VanillaPlusPlusConfig.COMMON.weatherClearDuration.get() * 60 * 20;
                ServerLifecycleHooks.getCurrentServer().overworld().setWeatherParameters(durationMinutes, 0, false, false);
                ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastSystemMessage(Component.literal("§7 Weather cleared by §l §ad§be§cm§do§ec§fr§aa§bc§cy§d!"), false);
            }
            case SLEEP -> {
                ServerLifecycleHooks.getCurrentServer().overworld().setDayTime(VanillaPlusPlusConfig.COMMON.setDayTickHour.get());
                Stat<ResourceLocation> stat = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
                for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                    player.getStats().setValue(player,stat, 0);
                }
                ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastSystemMessage(Component.literal("Skipped the night by §l §ad§be§cm§do§ec§fr§aa§bc§cy§d!!"), false);
            }
        }
    }

}
