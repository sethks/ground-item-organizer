package com.menuorganizer;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * Ground Item Organizer Plugin
 * This plugin enhances the right-click menu on loot piles by organizing items
 * into user-defined categories. Players can create custom sections like "Food",
 * "Alchs", or "Rares" and assign item keywords to each. When right-clicking
 * a pile of items, matching items are grouped under their respective section
 * labels with customizable colors.
 * Features:
 * - Up to 5 custom sections with user-defined names and colors
 * - Partial keyword matching (e.g., "shark" matches "Raw shark")
 * - Visual separators to distinguish sections in the menu
 * - Ctrl+Click quick pickup for all matching items at once
 * - Option to remove organized items from the default menu
 */
@Slf4j
@PluginDescriptor(
		name = "Ground Item Organizer",
		description = "Organize ground items into custom categories in the right-click menu",
		tags = {"ground", "items", "loot", "organize", "menu", "pickup", "drops"}
)
public class MenuOrganizerPlugin extends Plugin implements KeyListener, MouseListener
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MenuOrganizerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	/**
	 * Cached list of enabled custom sections from configuration.
	 * Rebuilt whenever config changes. Order is preserved from config.
	 */
	private final List<CustomSection> customSections = new ArrayList<>();

	/**
	 * Tracks whether Ctrl key is held for quick pickup feature.
	 */
	private boolean ctrlHeld = false;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Ground Item Organizer started");
		rebuildCustomSections();
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(this);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Ground Item Organizer stopped");
		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(this);
		customSections.clear();
		ctrlHeld = false;
	}

	@Provides
	MenuOrganizerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuOrganizerConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("grounditemorganizer"))
		{
			return;
		}
		rebuildCustomSections();
	}

	/**
	 * Parses all 5 section slots from config. Sections with empty names are skipped.
	 */
	private void rebuildCustomSections()
	{
		customSections.clear();

		if (!config.section1Name().trim().isEmpty())
		{
			customSections.add(new CustomSection(
					config.section1Name(),
					config.section1Color(),
					config.section1Items()
			));
		}

		if (!config.section2Name().trim().isEmpty())
		{
			customSections.add(new CustomSection(
					config.section2Name(),
					config.section2Color(),
					config.section2Items()
			));
		}

		if (!config.section3Name().trim().isEmpty())
		{
			customSections.add(new CustomSection(
					config.section3Name(),
					config.section3Color(),
					config.section3Items()
			));
		}

		if (!config.section4Name().trim().isEmpty())
		{
			customSections.add(new CustomSection(
					config.section4Name(),
					config.section4Color(),
					config.section4Items()
			));
		}

		if (!config.section5Name().trim().isEmpty())
		{
			customSections.add(new CustomSection(
					config.section5Name(),
					config.section5Color(),
					config.section5Items()
			));
		}

//		log.debug("Rebuilt {} custom sections", customSections.size());
	}

	// ========================================================================
	// MENU ORGANIZATION
	// ========================================================================


	 //Called when the right-click menu opens
	 //This method reorganizes the menu using the modern Menu interface
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		// Skip if plugin is disabled or no sections configured
		if (!config.enableOrganizer())
		{
			return;
		}

		if (customSections.isEmpty())
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();

		if (entries == null || entries.length == 0)
		{
			return;
		}

		Map<CustomSection, List<GroundItemData>> sectionItems = new LinkedHashMap<>();
		for (CustomSection section : customSections)
		{
			sectionItems.put(section, new ArrayList<>());
		}

		// This list will hold entries that should be removed from the menu. (keyword SHOULD) /dd1
		List<MenuEntry> entriesToRemove = new ArrayList<>();

		for (MenuEntry entry : entries)
		{
			if (!isGroundItemTakeAction(entry))
			{
				continue;
			}

			String target = entry.getTarget();
			if (target == null || target.isEmpty())
			{
				continue;
			}

			String itemName = Text.removeTags(target);
			CustomSection matchedSection = findMatchingSection(itemName);

			if (matchedSection != null)
			{
				GroundItemData data = new GroundItemData(
						entry.getParam0(),
						entry.getParam1(),
						entry.getType(),
						entry.getIdentifier(),
						entry.getItemId(),
						entry.getOption(),
						entry.getTarget(),
						itemName
				);
				sectionItems.get(matchedSection).add(data);

				// If user wants originals removed, mark this entry for removal
				if (config.removeOrganizedItems())
				{
					entriesToRemove.add(entry);
				}
			}
		}

		// Check if we have any matching items
		boolean hasAnyMatches = sectionItems.values().stream()
				.anyMatch(list -> !list.isEmpty());

		if (!hasAnyMatches)
		{
			return;
		}

		// Remove the original entries if the user wants them hidden.
		// Using menu.removeMenuEntry() is the proper way to do this (thanks bald man)
		for (MenuEntry entry : entriesToRemove)
		{
			menu.removeMenuEntry(entry);
		}

		List<CustomSection> sectionsInReverse = new ArrayList<>(customSections);
		Collections.reverse(sectionsInReverse);

		for (CustomSection section : sectionsInReverse)
		{
			List<GroundItemData> items = sectionItems.get(section);

			// Skip sections with no matching items
			if (items.isEmpty())
			{
				continue;
			}

			List<GroundItemData> itemsReversed = new ArrayList<>(items);
			Collections.reverse(itemsReversed);

			for (GroundItemData data : itemsReversed)
			{
				// Colorize the item name with the section's color
				String colorizedTarget = section.getColorTag() + data.itemName + CustomSection.getColorTagClose();

				// Create the organized menu entry using the Menu interface.
				// The onClick handler captures the original action data and invokes
				// the same Take action when clicked. (once again, ty bald man)
				menu.createMenuEntry(-1)
						.setOption("Take")
						.setTarget(colorizedTarget)
						.setType(MenuAction.RUNELITE)
						.onClick(e -> {
							// Invoke the original Take action with all its parameters
							client.menuAction(
									data.param0,      // scene X coordinate
									data.param1,      // scene Y coordinate
									data.actionType,  // idk tbh
									data.identifier,  // item identifier
									data.itemId,      // item ID
									data.option,      // "Take"
									data.target       // original item name
							);
						});
			}

			// Add section separator above the items.
			// Added after the items, so it appears above them in the displayed menu.
			if (config.showSeparators())
			{
				String separatorText = section.getColorTag() + "-- " + section.getName() + " --" + CustomSection.getColorTagClose();

				// just cancel - people are dumb
				menu.createMenuEntry(-1)
						.setOption(separatorText)
						.setTarget("")
						.setType(MenuAction.CANCEL);
			}
		}
	}

	// Simple data class to hold the information needed to invoke a Take action.
	private static class GroundItemData
	{
		final int param0;       // Scene X coordinate (for ground items)
		final int param1;       // Scene Y coordinate (for ground items)
		final MenuAction actionType;  // (bald man fix)
		final int identifier;   // Item identifier
		final int itemId;       // Item ID
		final String option;    // Action text ("Take")
		final String target;    // Item name (may include color tags)
		final String itemName;  // Clean item name (no color tags)

		GroundItemData(int param0, int param1, MenuAction actionType, int identifier,
					   int itemId, String option, String target, String itemName)
		{
			this.param0 = param0;
			this.param1 = param1;
			this.actionType = actionType;
			this.identifier = identifier;
			this.itemId = itemId;
			this.option = option;
			this.target = target;
			this.itemName = itemName;
		}
	}

	//Checks if a menu entry is a ground item "Take" action.
	private boolean isGroundItemTakeAction(MenuEntry entry)
	{
		MenuAction type = entry.getType();

		// Check all possible ground item action types
		boolean isGroundItemAction =
				type == MenuAction.GROUND_ITEM_FIRST_OPTION ||
						type == MenuAction.GROUND_ITEM_SECOND_OPTION ||
						type == MenuAction.GROUND_ITEM_THIRD_OPTION ||
						type == MenuAction.GROUND_ITEM_FOURTH_OPTION ||
						type == MenuAction.GROUND_ITEM_FIFTH_OPTION;

		if (!isGroundItemAction)
		{
			return false;
		}

		// Verify the option text is actually "Take"
		String option = entry.getOption();
		return option != null && option.equalsIgnoreCase("Take");
	}

	//Finds which section an item belongs to based on keyword matching.
	private CustomSection findMatchingSection(String itemName)
	{
		if (itemName == null || itemName.isEmpty())
		{
			return null;
		}

		for (CustomSection section : customSections)
		{
			if (section.matchesItem(itemName))
			{
				return section;
			}
		}

		return null;
	}

	// ========================================================================
	// CTRL+CLICK QUICK PICKUP (STILL IN TESTING)
	// ========================================================================

	/**
	 * Handles Ctrl+Click to pick up all matching items from the hovered tile.
	 * When the user holds Ctrl and left-clicks on a tile, this method finds all
	 * ground items on that tile that match any configured section, and invokes
	 * the Take action for each one.
	 * IMPORTANT: This method must be called on the client thread because
	 * itemManager.getItemComposition() requires it. (also pending runelite approval)
	 */
	private void handleCtrlClickPickup()
	{
		if (!config.ctrlClickPickup())
		{
			return;
		}

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (customSections.isEmpty())
		{
			return;
		}

		// Get the tile the user is hovering over
		Tile clickedTile = client.getTopLevelWorldView().getSelectedSceneTile();
		if (clickedTile == null)
		{
			return;
		}

		List<TileItem> groundItems = clickedTile.getGroundItems();
		if (groundItems == null || groundItems.isEmpty())
		{
			return;
		}

		// Get the tile's scene coordinates for the menu action
		LocalPoint localPoint = clickedTile.getLocalLocation();
		final int sceneX = localPoint.getSceneX();
		final int sceneY = localPoint.getSceneY();

		// Check each ground item and pick up any that match our sections
		for (TileItem item : groundItems)
		{
			final int itemId = item.getId();

			// Look up the item name (this call requires client thread)
			String itemName = itemManager.getItemComposition(itemId).getName();

			// Check if this item matches any of our sections
			boolean matches = false;
			for (CustomSection section : customSections)
			{
				if (section.matchesItem(itemName))
				{
					matches = true;
					break;
				}
			}

			if (matches)
			{
				log.debug("Ctrl+Click: Picking up {}", itemName);

				// Invoke the Take action for this ground item
				client.menuAction(
						sceneX,
						sceneY,
						MenuAction.GROUND_ITEM_THIRD_OPTION,
						itemId,
						itemId,
						"Take",
						itemName
				);
			}
		}
	}

	// ========================================================================
	// INPUT LISTENERS
	// ========================================================================

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			ctrlHeld = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			ctrlHeld = false;
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		// Not used
	}

	// Handles mouse clicks for the Ctrl+Click quick pickup feature. (removed/moved to mousePressed for 1:1 compliance)
	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		if (ctrlHeld && e.getButton() == MouseEvent.BUTTON1)
		{
			// Invoke the pickup on the client thread
			clientThread.invoke(this::handleCtrlClickPickup);

			// Consume the event to prevent the game from also processing this click.
			// This is critical for 1:1 compliance - without this, the game would
			// also pick up an item resulting in 2 items per click.
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		return e;
	}
}