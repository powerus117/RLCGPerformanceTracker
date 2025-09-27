package com.gauntletperformancetracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class PerformanceTrackerSerializer implements JsonSerializer<PerformanceStatistics> {
    private static final String TRACKER_TOTAL_TICKS_KEY = "totalTicks";
    private static final String TRACKER_LOST_TICKS_KEY = "lostTicks";
    private static final String TRACKER_USED_TICKS_KEY = "usedTicks";
    private static final String TRACKER_PLAYER_ATTACKS_KEY = "playerAttacks";
    private static final String TRACKER_WRONG_OFF_PRAYER_KEY = "wrongOffensivePrayer";
    private static final String TRACKER_WRONG_STYLE_KEY = "wrongAttackStyle";
    private static final String TRACKER_HUNLLEF_ATTACKS_KEY = "hunllefAttacks";
    private static final String TRACKER_WRONG_DEF_PRAYER_KEY = "wrongDefensivePrayer";
    private static final String TRACKER_HUNLLEF_STOMPS_KEY = "hunleffStomps";
    private static final String TRACKER_TORNADO_HITS_KEY = "tornadoHits";
    private static final String TRACKER_FLOOR_TILE_HITS_KEY = "floorTileHits";
    private static final String TRACKER_DAMAGE_TAKEN_KEY = "damageTaken";
    private static final String TRACKER_DPS_TAKEN_KEY = "dpsTaken";
    private static final String TRACKER_DPS_GIVEN_KEY = "dpsGiven";


    @Override
    public JsonElement serialize(PerformanceStatistics performanceStatistics, Type type, JsonSerializationContext context) {
        JsonObject trackerJson = new JsonObject();
        trackerJson.addProperty(TRACKER_TOTAL_TICKS_KEY, performanceStatistics.totalTicksCounter);
        trackerJson.addProperty(TRACKER_LOST_TICKS_KEY, performanceStatistics.missedTicksCounter);
        trackerJson.addProperty(TRACKER_USED_TICKS_KEY, String.format("%.2f", (1f - (float) performanceStatistics.missedTicksCounter / performanceStatistics.totalTicksCounter) * 100f) + "%");
        trackerJson.addProperty(TRACKER_PLAYER_ATTACKS_KEY, performanceStatistics.playerAttackCount);
        trackerJson.addProperty(TRACKER_WRONG_OFF_PRAYER_KEY, performanceStatistics.wrongOffensivePrayerCount);
        trackerJson.addProperty(TRACKER_WRONG_STYLE_KEY, performanceStatistics.wrongAttackStyleCount);
        trackerJson.addProperty(TRACKER_HUNLLEF_ATTACKS_KEY, performanceStatistics.hunllefAttackCount);
        trackerJson.addProperty(TRACKER_WRONG_DEF_PRAYER_KEY, performanceStatistics.wrongDefensivePrayerCount);
        trackerJson.addProperty(TRACKER_HUNLLEF_STOMPS_KEY, performanceStatistics.hunllefStompAttackCount);
        trackerJson.addProperty(TRACKER_TORNADO_HITS_KEY, performanceStatistics.tornadoHits);
        trackerJson.addProperty(TRACKER_FLOOR_TILE_HITS_KEY, performanceStatistics.floorTileHits);
        trackerJson.addProperty(TRACKER_DAMAGE_TAKEN_KEY, performanceStatistics.receivedDamage);
        trackerJson.addProperty(TRACKER_DPS_TAKEN_KEY, getDps(performanceStatistics.totalTicksCounter, performanceStatistics.receivedDamage));
        trackerJson.addProperty(TRACKER_DPS_GIVEN_KEY, getDps(performanceStatistics.totalTicksCounter, performanceStatistics.givenDamage));

        return trackerJson;
    }

    public float getDps(int totalTicks, int totalDamage) {
        return totalDamage / (totalTicks * 0.6f);
    }
}
