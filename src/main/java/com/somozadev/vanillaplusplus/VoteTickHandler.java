package com.somozadev.vanillaplusplus;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber
public class VoteTickHandler {
    private static long lastAnnounce = 0;
    @SubscribeEvent
    public static void OnServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (DemocracyManager.currentVote != null) {
            long currentTime = System.currentTimeMillis();
            long endTime = DemocracyManager.currentVote.startTime + DemocracyManager.currentVote.durationMs;
            long remaining = endTime - currentTime;
            long secondsLeft = remaining / 1000;

            long intervalAnnounceTime = 0;
            if(secondsLeft <=  5)
                intervalAnnounceTime =  1000; // each sg
            else if(secondsLeft <= 15)
                intervalAnnounceTime = 5 * 1000; // each 5s
            else if(secondsLeft <= 30)
                intervalAnnounceTime = 15 * 1000; // each 15s
            else
                intervalAnnounceTime = 30 * 1000; // each 30s


            if (currentTime - lastAnnounce >= intervalAnnounceTime) {
                lastAnnounce = currentTime;
                ServerLifecycleHooks.getCurrentServer().getPlayerList()
                        .broadcastSystemMessage(Component.literal("§6 §l" + secondsLeft + " §8 seconds left"), false);
            }

            if (DemocracyManager.currentVote.IsExpired()) {
                DemocracyManager.FinishVote();
            }
        }
    }
}
