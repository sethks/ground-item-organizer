package com.menuorganizer;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * - Option to remove organized items from the default menu
 */
@Slf4j
@PluginDescriptor(
		name = "Ground Item Organizer",
		description = "Organize ground items into custom categories in the right-click menu",
		tags = {"ground", "items", "loot", "organize", "menu", "pickup", "drops"}
)
public class MenuOrganizerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private MenuOrganizerConfig config;

	@Inject
	private ConfigManager configManager;

	/**
	 * Cached list of enabled custom sections from configuration.
	 * Rebuilt whenever config changes. Order is preserved from config.
	 */
	private final List<CustomSection> customSections = new ArrayList<>();

	@Override
	protected void startUp() throws Exception
	{
		log.info("Ground Item Organizer started");
		rebuildCustomSections();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Ground Item Organizer stopped");
		customSections.clear();
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
	// MENU ORGANIZATION - done for reordering ONLY
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

		//use buckets instead
		List<MenuEntry> nonGroundItemEntries = new ArrayList<>();
		List<MenuEntry> unmatchedGroundItems = new ArrayList<>();
		Map<CustomSection, List<MenuEntry>> sectionEntries = new LinkedHashMap<>();

		for (CustomSection section : customSections)
		{
			sectionEntries.put(section, new ArrayList<>());
		}

		// This list will hold entries that should be removed from the menu. (keyword SHOULD) /dd1
		// fixed - categorize entry instead /dd1
		for (MenuEntry entry : entries)
		{
			if (!isGroundItemTakeAction(entry))
			{
				nonGroundItemEntries.add(entry);
				continue;
			}

			String target = entry.getTarget();
			if (target == null || target.isEmpty())
			{
				unmatchedGroundItems.add(entry);
				continue;
			}

			String itemName = Text.removeTags(target);
			CustomSection matchedSection = findMatchingSection(itemName);

			if (matchedSection != null)
			{
				// Colorize the entry - this only changes the display text /dd1
				String colorizedTarget = matchedSection.getColorTag() + itemName + CustomSection.getColorTagClose();
				entry.setTarget(colorizedTarget);

				sectionEntries.get(matchedSection).add(entry);
			}
			else
			{
				unmatchedGroundItems.add(entry);
			}
		}

		// Check if we have any organized items
		boolean hasOrganizedItems = sectionEntries.values().stream()
				.anyMatch(list -> !list.isEmpty());

		if (!hasOrganizedItems)
		{
			return;
		}

		// Build the reordered menu array
		// RuneLite menu: LAST entry in array = TOP of displayed menu
		List<MenuEntry> reorderedEntries = new ArrayList<>();

		// 1. Non-ground-item entries (bottom of menu)
		reorderedEntries.addAll(nonGroundItemEntries);

		// 2. Unmatched ground items
		reorderedEntries.addAll(unmatchedGroundItems);

		// 3. Sections in reverse order (so Section 1 ends up at top)
		List<CustomSection> sectionsReversed = new ArrayList<>(customSections);
		Collections.reverse(sectionsReversed);

		for (CustomSection section : sectionsReversed)
		{
			List<MenuEntry> items = sectionEntries.get(section);

			if (items.isEmpty())
			{
				continue;
			}

			// Add the section's items (these are the ORIGINAL MenuEntry objects, just reordered)
			reorderedEntries.addAll(items);

			// Add separator if enabled
			if (config.showSeparators())
			{
				String separatorText = section.getColorTag() + "-- " + section.getName() + " --" + CustomSection.getColorTagClose();

				// Create separator - CANCEL ---> does nothing when clicked (client-side only)
				MenuEntry separator = menu.createMenuEntry(-1)
						.setOption(separatorText)
						.setTarget("")
						.setType(MenuAction.CANCEL);

				reorderedEntries.add(separator);
			}
		}

		// Set the final reordered menu
		menu.setMenuEntries(reorderedEntries.toArray(new MenuEntry[0]));
	}

	//data clazz gone /dd1

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
}