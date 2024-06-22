package com.gauntletperformancetracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GauntletPerformanceTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GauntletPerformanceTrackerPlugin.class);
		RuneLite.main(args);
	}
}