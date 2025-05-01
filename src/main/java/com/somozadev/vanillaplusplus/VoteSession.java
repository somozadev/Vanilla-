package com.somozadev.vanillaplusplus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

        //TODO: check if vote cap > half player base
        //TODO: TP , SORT , LOCK CHESTS , WHISPER, MORE VOTES


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
                ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastSystemMessage(Component.literal("Skipped the night by §l §ad§be§cm§do§ec§fr§aa§bc§cy§d!!"), false);
            }
        }
    }

}
