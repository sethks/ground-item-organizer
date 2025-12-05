package com.menuorganizer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup("grounditemorganizer")
public interface MenuOrganizerConfig extends Config
{
	// ========================================================================
	// GENERAL SETTINGS SECTION
	// ========================================================================

	@ConfigSection(
			name = "General Settings",
			description = "Core plugin toggles and behavior options",
			position = 0
	)
	String generalSettings = "generalSettings";

	@ConfigItem(
			keyName = "enableOrganizer",
			name = "Enable Organizer",
			description = "Master toggle for the ground item organizer",
			section = generalSettings,
			position = 0
	)
	default boolean enableOrganizer()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showSeparators",
			name = "Show Section Separators",
			description = "Display visual separator labels (e.g., '-- Food --') above each section's items",
			section = generalSettings,
			position = 1
	)
	default boolean showSeparators()
	{
		return true;
	}

	// ========================================================================
	// CUSTOM SECTION 1
	// ========================================================================

	@ConfigSection(
			name = "Custom Section 1",
			description = "First customizable item category",
			position = 1,
			closedByDefault = false
	)
	String customSection1 = "customSection1";

	@ConfigItem(
			keyName = "section1Name",
			name = "Section Name",
			description = "The label that appears in the right-click menu for this category. " +
					"Leave blank to disable this section.",
			section = customSection1,
			position = 0
	)
	default String section1Name()
	{
		// Provide a default example so users understand the format
		return "Food";
	}

	@ConfigItem(
			keyName = "section1Color",
			name = "Section Color",
			description = "The color used for this section's label and item entries in the menu.",
			section = customSection1,
			position = 1
	)
	default Color section1Color()
	{
		return new Color(255, 170, 0);
	}

	@ConfigItem(
			keyName = "section1Items",
			name = "Items",
			description = "Comma-separated list of item name keywords. " +
					"Partial matches work (e.g., 'shark' matches 'Shark' and 'Raw shark').",
			section = customSection1,
			position = 1
	)
	default String section1Items()
	{
		// Default example showing comma-separated format
		return "garden pie, shark, lobster";
	}

	// ========================================================================
	// CUSTOM SECTION 2
	// ========================================================================

	@ConfigSection(
			name = "Custom Section 2",
			description = "Second customizable item category",
			position = 2,
			closedByDefault = false
	)
	String customSection2 = "customSection2";

	@ConfigItem(
			keyName = "section2Name",
			name = "Section Name",
			description = "The label that appears in the right-click menu for this category. " +
					"Leave blank to disable this section.",
			section = customSection2,
			position = 0
	)
	default String section2Name()
	{
		return "Runes";
	}

	@ConfigItem(
			keyName = "section2Color",
			name = "Section Color",
			description = "The color used for this section's label and item entries in the menu.",
			section = customSection2,
			position = 1
	)
	default Color section2Color()
	{
		return new Color(170, 120, 255);
	}

	@ConfigItem(
			keyName = "section2Items",
			name = "Items",
			description = "Comma-separated list of item name keywords. ",
			section = customSection2,
			position = 1
	)

	default String section2Items()
	{
		return "fire rune, water rune, air rune, earth rune, law rune, nature rune, cosmic rune";
	}

	// ========================================================================
	// CUSTOM SECTION 3
	// ========================================================================

	@ConfigSection(
			name = "Custom Section 3",
			description = "Third customizable item category",
			position = 3,
			closedByDefault = true
	)
	String customSection3 = "customSection3";

	@ConfigItem(
			keyName = "section3Name",
			name = "Section Name",
			description = "The label that appears in the right-click menu for this category. " +
					"Leave blank to disable this section.",
			section = customSection3,
			position = 0
	)
	default String section3Name()
	{
		return "Burst Nechryael";
	}

	@ConfigItem(
			keyName = "section3Color",
			name = "Section Color",
			description = "The color used for this section's label and item entries in the menu.",
			section = customSection3,
			position = 1
	)
	default Color section3Color()
	{
		return new Color(255, 80, 80);
	}

	@ConfigItem(
			keyName = "section3Items",
			name = "Items",
			description = "Comma-separated list of item name keywords. " +
					"Partial matches work.",
			section = customSection3,
			position = 1
	)
	default String section3Items()
	{
		return "Rune full helm, Rune boots, Rune chainbody";
	}

	// ========================================================================
	// CUSTOM SECTION 4
	// ========================================================================

	@ConfigSection(
			name = "Custom Section 4",
			description = "Fourth customizable item category",
			position = 4,
			closedByDefault = true
	)
	String customSection4 = "customSection4";

	@ConfigItem(
			keyName = "section4Name",
			name = "Section Name",
			description = "The label that appears in the right-click menu for this category. " +
					"Leave blank to disable this section.",
			section = customSection4,
			position = 0
	)
	default String section4Name()
	{
		// Empty by default - user fills in if needed
		return "Construction";
	}

	@ConfigItem(
			keyName = "section4Color",
			name = "Section Color",
			description = "The color used for this section's label and item entries in the menu.",
			section = customSection4,
			position = 1
	)
	default Color section4Color()
	{
		return new Color(100, 220, 100);
	}

	@ConfigItem(
			keyName = "section4Items",
			name = "Items",
			description = "Comma-separated list of item name keywords. " +
					"Partial matches work.",
			section = customSection4,
			position = 1
	)
	default String section4Items()
	{
		return "Oak plank, Teak plank, hammer";
	}

	// ========================================================================
	// CUSTOM SECTION 5
	// ========================================================================

	@ConfigSection(
			name = "Custom Section 5",
			description = "Fifth customizable item category",
			position = 5,
			closedByDefault = true
	)
	String customSection5 = "customSection5";

	@ConfigItem(
			keyName = "section5Name",
			name = "Section Name",
			description = "The label that appears in the right-click menu for this category. " +
					"Leave blank to disable this section.",
			section = customSection5,
			position = 0
	)
	default String section5Name()
	{
		// Empty by default - user can define their own
		return "";
	}

	@ConfigItem(
			keyName = "section5Color",
			name = "Section Color",
			description = "The color used for this section's label and item entries in the menu.",
			section = customSection5,
			position = 1
	)
	default Color section5Color()
	{
		return new Color(80, 220, 220);
	}

	@ConfigItem(
			keyName = "section5Items",
			name = "Items",
			description = "Comma-separated list of item name keywords. " +
					"Partial matches work.",
			section = customSection5,
			position = 1
	)
	default String section5Items()
	{
		return "";
	}
}
