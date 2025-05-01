package com.somozadev.vanillaplusplus;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    public static boolean IsOnCooldown(ServerPlayer player, String action, long cooldownDurationMs) {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> cooldowns = playerCooldowns.getOrDefault(player.getUUID(), new HashMap<>());
        long lastActionTime = cooldowns.getOrDefault(action, 0L);

        long elapsed = currentTime - lastActionTime;
        return elapsed < cooldownDurationMs;
    }

    public static long GetRemainingTime(ServerPlayer player, String action, long cooldownDurationMs) {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> cooldowns = playerCooldowns.getOrDefault(player.getUUID(), new HashMap<>());
        long lastActionTime = cooldowns.getOrDefault(action, 0L);

        long elapsed = currentTime - lastActionTime;
        return Math.max(0, cooldownDurationMs - elapsed);
    }

    public static void SetCooldown(ServerPlayer player, String action) {
        playerCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(action, System.currentTimeMillis());
    }
}
