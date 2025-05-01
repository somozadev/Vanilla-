package com.somozadev.vanillaplusplus;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class VanillaPlusPlusConfig {

    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final Config COMMON;
    static {
        Pair<Config, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Config::new);
        COMMON = pair.getLeft();
        COMMON_CONFIG = pair.getRight();
    }

    public static class Config {
        public final ForgeConfigSpec.IntValue teleportDelay;
        public final ForgeConfigSpec.IntValue maxHomes;
        public final ForgeConfigSpec.IntValue weatherClearDuration;
        public final ForgeConfigSpec.IntValue setDayTickHour;
        public final ForgeConfigSpec.IntValue voteClearWeatherTimeout;
        public final ForgeConfigSpec.IntValue voteGoToSleepTimeout;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.push("Vanilla Plus Plus Settings");

            teleportDelay = builder
                    .comment("Delay in seconds before teleporting to a home")
                    .defineInRange("teleportDelay", 15, 5, 60);

            maxHomes = builder
                    .comment("Maximum number of homes a player can set")
                    .defineInRange("maxHomes", 5, 1, 100);

            weatherClearDuration = builder.comment("Duration in minutes that the weather stays clear after a successful vote")
                            .defineInRange("weatherClearDuration", 120, 10, 600);

            setDayTickHour = builder.comment("Tick value of the new day after a successful vote")
                    .defineInRange("setDayTickHour", 2000, 0, 23999);

            voteClearWeatherTimeout = builder.comment("Delay for voting to clear weather after a successful vote in seconds")
                    .defineInRange("voteClearWeatherTimeout", 30, 5, 120);

            voteGoToSleepTimeout = builder.comment("Delay for voting to sleep after a successful vote in seconds")
                    .defineInRange("voteGoToSleepTimeout", 30, 5, 120);
            builder.pop();
        }
    }

}
