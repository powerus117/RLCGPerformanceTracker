package com.gauntletperformancetracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Gauntlet Performance Tracker"
)
public class GauntletPerformanceTrackerPlugin extends Plugin
{
	// Var bits
	private static final int GauntletBossStartVarBit = 9177;
	private static final int GauntletMazeVarBit = 9178;

	// Animation Ids
	private static  final int playerMageAttackId = 1167;
	private static  final int playerRangeAttackId = 426;
	private static  final int playerMeleeAttackId = 440;
	private static  final int playerKickAttackId = 423;
	private static  final int playerPunchAttackId = 422;
	private static  final int[] playerAttackAnimationIds = {playerMageAttackId, playerMeleeAttackId, playerRangeAttackId, playerKickAttackId, playerPunchAttackId};

	private static  final int bossAttackAnimationId = 8419;
	private static  final int bossStompAnimationId = 8420;
	private static  final int bossSwitchToMageAnimationId = 8754;
	private static  final int bossSwitchToRangeAnimationId = 8755;

	// Item Ids
	private static  final int normalFoodId = 23874;
	private static  final int crystalFoodId = 25960;
	private static  final int corruptedFoodId = 25958;

	// Timings
	private static  final int weaponAttackSpeed = 4;
	private static  final int normalFoodDelay = 3;
	private static  final int fastFoodDelay = 2;

	// Region ids
	private static final int REGION_ID_GAUNTLET_LOBBY = 12127;

	// Ground object ids
	private static final int DAMAGE_TILE_ID = 36048;

	// Entity ids
	private static final List<Integer> HUNLLEF_IDS = List.of(
			NpcID.CRYSTALLINE_HUNLLEF,
			NpcID.CRYSTALLINE_HUNLLEF_9022,
			NpcID.CRYSTALLINE_HUNLLEF_9023,
			NpcID.CRYSTALLINE_HUNLLEF_9024,
			NpcID.CORRUPTED_HUNLLEF,
			NpcID.CORRUPTED_HUNLLEF_9036,
			NpcID.CORRUPTED_HUNLLEF_9037,
			NpcID.CORRUPTED_HUNLLEF_9038);
	private static final List<Integer> TORNADO_IDS = List.of(NullNpcID.NULL_9025, NullNpcID.NULL_9039);

	private final Pattern eatPattern = Pattern.compile("^eat");

	@Inject
	private Client client;

	@Inject
	private GauntletPerformanceTrackerConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private GauntletPerformanceTrackerOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	public boolean isBossActive = false;
	public boolean isInGauntletLobby = false;
	public boolean isInGauntlet = false; // Doesn't include lobby
	public int missedTicksCounter = 0;
	public int totalTicksCounter = 0;
	public int playerAttackCount = 0;
	public int wrongAttackStyleCount = 0;
	public int wrongOffensivePrayerCount = 0;
	public int wrongDefensivePrayerCount = 0;
	public int hunllefAttackCount = 0;
	public int hunllefStompAttackCount = 0;
	public int receivedDamage = 0;
	public int givenDamage = 0;
	public int tornadoHits = 0;
	public int floorTileHits = 0;
	public TickLossState tickLossState;

	private int previousAttackTick;
	private NPC hunllef;
	private final List<NPC> tornadoes = new ArrayList<>();
	private boolean isHunllefMaging = false;
	private final ArrayDeque<ItemMenuAction> actionStack = new ArrayDeque<>();

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	public float getDps(int totalDamage)
	{
		return totalDamage / (totalTicksCounter * 0.6f);
	}

	@Subscribe
	void onVarbitChanged(final VarbitChanged event)
	{
		final int varbit = event.getVarbitId();

		if (varbit == GauntletBossStartVarBit)
		{
			if (event.getValue() == 1)
			{
				isBossActive = true;
				ResetEncounterData();
			}
			else
			{
				isBossActive = false;
				tornadoes.clear();
				hunllef = null;
			}
		}
		else if (varbit == GauntletMazeVarBit)
		{
			if (event.getValue() == 1)
			{
				isInGauntlet = true;
			}
			else
			{
				isInGauntlet = false;
			}
		}
	}

	private void ResetEncounterData()
	{
		missedTicksCounter = 0;
		totalTicksCounter = 0;
		playerAttackCount = 0;
		wrongAttackStyleCount = 0;
		wrongOffensivePrayerCount = 0;
		wrongDefensivePrayerCount = 0;
		hunllefAttackCount = 0;
		hunllefStompAttackCount = 0;
		receivedDamage = 0;
		givenDamage = 0;
		tornadoHits = 0;
		floorTileHits = 0;
		tickLossState = TickLossState.NONE;

		isHunllefMaging = false;
		previousAttackTick = client.getTickCount(); // This gives 4 ticks leeway at the start of the fight
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		isInGauntletLobby = isInTheGauntletLobby();

		if (!isBossActive)
			return;

		totalTicksCounter++;

		int currentTickCount = client.getTickCount();
		int tickDifference = currentTickCount - previousAttackTick;
		if (tickDifference >= weaponAttackSpeed + normalFoodDelay)
		{
			tickLossState = TickLossState.LOSING;
		}
		else if (tickDifference >= weaponAttackSpeed)
		{
			// Means we are potentially losing ticks unless we eat
			tickLossState = TickLossState.POTENTIAL;
		}
		else
		{
			tickLossState = TickLossState.NONE;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		for (NPC tornado : tornadoes)
		{
			if (playerLocation.equals(tornado.getWorldLocation()))
			{
				tornadoHits++;
			}
		}

		var scene = client.getWorldView(client.getLocalPlayer().getLocalLocation().getWorldView()).getScene();
		var tiles = scene.getTiles();
		int tileX = playerLocation.getX() - scene.getBaseX();
		int tileY = playerLocation.getY() - scene.getBaseY();
		var currentTile = tiles[playerLocation.getPlane()][tileX][tileY];
		if (currentTile != null && currentTile.getGroundObject() != null && currentTile.getGroundObject().getId() == DAMAGE_TILE_ID)
		{
			floorTileHits++;
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!isBossActive)
			return;

		final int animationId = event.getActor().getAnimation();

		if (animationId < 0)
			return;

		if (event.getActor().getName().equals(client.getLocalPlayer().getName()))
		{
			// Local player animation changed
			if (Arrays.stream(playerAttackAnimationIds).anyMatch(value -> value == animationId))
			{
				// Player attack
				playerAttackCount++;

				if (!hasCorrectAttackStyle(animationId))
				{
					wrongAttackStyleCount++;
				}

				if (!hasCorrectOffsenivePrayerActive(animationId))
				{
					wrongOffensivePrayerCount++;
				}

				int currentAttackTick = client.getTickCount();
				int ticksBetweenAttacks = currentAttackTick - previousAttackTick; // Also takes eating into account

				int lostTicks = ticksBetweenAttacks - weaponAttackSpeed;
				if (lostTicks > 0)
				{
					missedTicksCounter += lostTicks;
				}

				previousAttackTick = currentAttackTick;
			}
		}
		else if (event.getActor().getName().equals("Corrupted Hunllef") ||
				event.getActor().getName().equals("Crystalline Hunllef"))
		{
			// Hunllef animation changed
			if (animationId == bossAttackAnimationId)
			{
				hunllefAttackCount++;

				if (!hasCorrectDefensivePrayerActive())
				{
					wrongDefensivePrayerCount++;
				}
			}
			else if (animationId == bossStompAnimationId)
			{
				hunllefStompAttackCount++;
			}
			else if (animationId == bossSwitchToMageAnimationId)
			{
				isHunllefMaging = true;
			}
			else if (animationId == bossSwitchToRangeAnimationId)
			{
				isHunllefMaging = false;
			}
		}
	}

	@Subscribe
	private void onNpcSpawned(final NpcSpawned event)
	{
		if (!isInGauntlet)
			return;

		final NPC npc = event.getNpc();

		if (TORNADO_IDS.contains(npc.getId()))
		{
			tornadoes.add(npc);
		}
		else if (HUNLLEF_IDS.contains(npc.getId()))
		{
			hunllef = npc;
		}
	}

	@Subscribe
	void onNpcDespawned(final NpcDespawned event)
	{
		if (!isInGauntlet)
			return;

		final NPC npc = event.getNpc();

		if (TORNADO_IDS.contains(npc.getId()))
		{
			tornadoes.removeIf(t -> t == npc);
		}
		else if (HUNLLEF_IDS.contains(npc.getId()))
		{
			hunllef = null;
		}
	}

	@Subscribe
	private void onMenuOptionClicked(final MenuOptionClicked event)
	{
		if (!isInGauntlet)
			return;

		String menuOption = Text.removeTags(event.getMenuOption()).toLowerCase();
		if (eatPattern.matcher(menuOption).find()&&
				actionStack.stream().noneMatch(a ->
				{
					if (a instanceof ItemMenuAction.ItemAction)
					{
						ItemMenuAction.ItemAction i = (ItemMenuAction.ItemAction) a;
						return i.itemID == event.getItemId();
					}
					return false;
				}))
		{
			ItemContainer oldInv = client.getItemContainer(InventoryID.INVENTORY);
			int slot = event.getMenuEntry().getParam0();
			int pushItem = oldInv.getItems()[event.getMenuEntry().getParam0()].getId();
			ItemMenuAction newAction = new ItemMenuAction.ItemAction(oldInv.getItems(), pushItem, slot);
			actionStack.push(newAction);
		}
	}

	@Subscribe
	private void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
	{
		if (!isInGauntlet)
			return;

		ItemContainer itemContainer = itemContainerChanged.getItemContainer();
		int containerId = itemContainer.getId();

		if (containerId == InventoryID.INVENTORY.getId())
		{
			onInventoryChanged(itemContainer);
		}
	}

	@Subscribe
	private void onHitsplatApplied(final HitsplatApplied event)
	{
		if (!isInGauntlet)
			return;

		int damageAmount = event.getHitsplat().getAmount();
		if (event.getActor().getName().equals(client.getLocalPlayer().getName()))
		{
			// Local player got hit
			receivedDamage += damageAmount;
		}
		else if (event.getActor().getName().equals("Corrupted Hunllef") ||
				event.getActor().getName().equals("Crystalline Hunllef"))
		{
			givenDamage += damageAmount;
		}
	}

	private void onInventoryChanged(ItemContainer itemContainer)
	{
		while (!actionStack.isEmpty())
		{
			ItemMenuAction.ItemAction itemAction = (ItemMenuAction.ItemAction) actionStack.pop();
			Item[] oldInv = itemAction.oldInventory;
			int nextItem = itemAction.itemID;
			int nextSlot = itemAction.slot;
			if (itemContainer.getItems()[nextSlot].getId() != oldInv[nextSlot].getId())
			{
				// Slot changed so we assume item has been eaten
				if (nextItem == normalFoodId)
				{
					previousAttackTick += normalFoodDelay;
				}
				else if (nextItem == crystalFoodId || nextItem == corruptedFoodId)
				{
					previousAttackTick += fastFoodDelay;
				}
			}
		}
	}

	private boolean hasCorrectOffsenivePrayerActive(int animationId)
	{
		boolean isNoWeaponAttack = animationId == playerKickAttackId || animationId == playerPunchAttackId;
		if (!config.countNoWeaponOffPrayer() && isNoWeaponAttack)
			return true;

		if (animationId == playerMeleeAttackId || isNoWeaponAttack)
		{
			return client.isPrayerActive(Prayer.PIETY) ||
					client.isPrayerActive(Prayer.ULTIMATE_STRENGTH) ||
					client.isPrayerActive(Prayer.SUPERHUMAN_STRENGTH) ||
					client.isPrayerActive(Prayer.BURST_OF_STRENGTH);
		}

		if (animationId == playerMageAttackId)
		{
			return client.isPrayerActive(Prayer.AUGURY) ||
					client.isPrayerActive(Prayer.MYSTIC_MIGHT) ||
					client.isPrayerActive(Prayer.MYSTIC_LORE) ||
					client.isPrayerActive(Prayer.MYSTIC_WILL);
		}

		if (animationId == playerRangeAttackId)
		{
			return client.isPrayerActive(Prayer.RIGOUR) ||
					client.isPrayerActive(Prayer.EAGLE_EYE) ||
					client.isPrayerActive(Prayer.HAWK_EYE) ||
					client.isPrayerActive(Prayer.SHARP_EYE);
		}

		return false;
	}

	private boolean hasCorrectAttackStyle(int animationId)
	{
		switch (hunllef.getId())
		{
			// Protect from Melee
			case NpcID.CORRUPTED_HUNLLEF:
			case NpcID.CRYSTALLINE_HUNLLEF:
				return animationId != playerMeleeAttackId &&
						animationId != playerKickAttackId &&
						animationId != playerPunchAttackId;
			// Protect from Missiles
			case NpcID.CRYSTALLINE_HUNLLEF_9022:
			case NpcID.CORRUPTED_HUNLLEF_9036:
				return animationId != playerRangeAttackId;
			// Protect from Magic
			case NpcID.CRYSTALLINE_HUNLLEF_9023:
			case NpcID.CORRUPTED_HUNLLEF_9037:
				return animationId != playerMageAttackId;
		}

		return false;
	}

	private boolean hasCorrectDefensivePrayerActive()
	{
		return isHunllefMaging ? client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC) : client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES);
	}

	private boolean isInTheGauntletLobby()
	{
		Player player = client.getLocalPlayer();

		if (player == null)
		{
			return false;
		}

		int regionId = WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID();
		return regionId == REGION_ID_GAUNTLET_LOBBY;
	}

	@Provides
	GauntletPerformanceTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GauntletPerformanceTrackerConfig.class);
	}
}
