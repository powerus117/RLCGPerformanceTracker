package com.gauntletperformancetracker;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class GauntletPerformanceTrackerOverlay extends OverlayPanel
{
    private final GauntletPerformanceTrackerPlugin plugin;

    @Inject
    private GauntletPerformanceTrackerConfig config;

    @Inject
    public GauntletPerformanceTrackerOverlay(GauntletPerformanceTrackerPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if ((!plugin.isInGauntletLobby && !plugin.isBossActive) ||
            (plugin.isBossActive && config.hideOverlayDuringFight()) ||
            plugin.performanceStatistics.totalTicksCounter <= 0)
            return null;

        if (plugin.isBossActive)
        {
            String tickLossMessage = "undefined";
            Color tickLossColor = Color.RED;

            switch (plugin.tickLossState) {
                case NONE:
                    tickLossColor = Color.GREEN;
                    tickLossMessage = "No tick loss";
                    break;
                case POTENTIAL:
                    tickLossColor = Color.ORANGE;
                    tickLossMessage = "Losing ticks";
                    break;
                case LOSING:
                    tickLossColor = Color.RED;
                    tickLossMessage = "Losing ticks";
                    break;
            }

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text(tickLossMessage)
                    .color(tickLossColor)
                    .build());
        }
        else
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Total ticks")
                    .right(Integer.toString(plugin.performanceStatistics.totalTicksCounter))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Lost ticks")
                    .right(Integer.toString(plugin.performanceStatistics.missedTicksCounter))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Used ticks")
                    .right(String.format("%.2f", (1f - (float) plugin.performanceStatistics.missedTicksCounter / plugin.performanceStatistics.totalTicksCounter) * 100f) + "%")
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Player attacks")
                    .right(Integer.toString(plugin.performanceStatistics.playerAttackCount))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Wrong off pray")
                    .right(Integer.toString(plugin.performanceStatistics.wrongOffensivePrayerCount))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Wrong att style")
                    .right(Integer.toString(plugin.performanceStatistics.wrongAttackStyleCount))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Hunllef attacks")
                    .right(Integer.toString(plugin.performanceStatistics.hunllefAttackCount))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Wrong def pray")
                    .right(Integer.toString(plugin.performanceStatistics.wrongDefensivePrayerCount))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Hunllef stomps")
                    .right(Integer.toString(plugin.performanceStatistics.hunllefStompAttackCount))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Tornado hits")
                    .right(Integer.toString(plugin.performanceStatistics.tornadoHits))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Floor tile hits")
                    .right(Integer.toString(plugin.performanceStatistics.floorTileHits))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Damage taken")
                    .right(Integer.toString(plugin.performanceStatistics.receivedDamage))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("DPS taken")
                    .right(String.format("%.3f", plugin.getDps(plugin.performanceStatistics.receivedDamage)))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("DPS given")
                    .right(String.format("%.3f", plugin.getDps(plugin.performanceStatistics.givenDamage)))
                    .build());
        }

        return super.render(graphics);
    }
}
