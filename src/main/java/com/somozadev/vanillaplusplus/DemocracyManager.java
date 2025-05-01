package com.somozadev.vanillaplusplus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
public class DemocracyManager {
    public enum VoteType {
        WEATHER, SLEEP
    }

    public static VoteSession currentVote = null;

    public static boolean StartVote(VoteType type, long durationSeconds) {
        if (currentVote != null && !currentVote.IsExpired()) {
            return false; // already an active vote
        }
        currentVote = new VoteSession(type, durationSeconds * 1000);
        return true;
    }
    public static boolean Vote(ServerPlayer player, boolean yes) {
        if (currentVote == null || currentVote.IsExpired()) {
            return false;
        }
        return currentVote.Vote(player, yes);
    }
    public static void FinishVote() {
        int yes = currentVote.yesVotes.size();
        int totalOnline = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerCount();
        float requiredMajority = 0.5f;
        String resultMessage;
        if (yes >= Math.ceil(totalOnline * requiredMajority)) {
            resultMessage = "§l §a Vote passed with " + yes  + " / " + totalOnline + " votes.";
            currentVote.executeAction();
        } else {
            resultMessage = "§l §c Vote failed with " + yes  + " / " + totalOnline + " votes.";
        }

        ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastSystemMessage(Component.literal(resultMessage), false);
        DemocracyManager.currentVote = null;
    }
}
