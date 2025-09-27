package com.gauntletperformancetracker;

import lombok.Setter;
import net.runelite.api.Client;

import javax.inject.Inject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

public class GauntletPerformanceTrackerWriter {

    @Inject
    private Client client;
    @Setter
    private String username;

    @Inject
    private GauntletPerformanceTrackerConfig config;

    @Inject
    private JsonUtils jsonUtils;

    @Inject
    public GauntletPerformanceTrackerWriter() {

    }

    private static final File TRACKER_DIR = new File(RUNELITE_DIR, "gauntletPerformanceTracker");
    private static final File DATA_DIR = new File(TRACKER_DIR, "data");
    private static final String FILE_PREFIX = "gauntletTracker";

    private String getDataFilePath(String fileName) {
        username = client.getLocalPlayer().getName();
        File directory = new File(DATA_DIR + File.separator + username);
        directory.mkdirs();
        return directory + File.separator + fileName;
    }

    public String getTrackerFilePath() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
        String formattedDate = dateFormat.format(currentDate);
        String fileName = FILE_PREFIX + "-" + username + "-" + formattedDate + ".json";
        return getDataFilePath(fileName);
    }

    public void writePerformanceToFile(PerformanceStatistics performanceStatistics) {
        if (!config.saveResultsToFile())
            return;

        jsonUtils.writeJsonFile(getTrackerFilePath(), performanceStatistics, new PerformanceTrackerSerializer());

    }
}
