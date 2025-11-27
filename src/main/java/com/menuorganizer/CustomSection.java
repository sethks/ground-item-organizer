package com.menuorganizer;

import lombok.Getter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CustomSection
{
    // The display name shown in the right-click menu (e.g., "Food", "Runes")
    private final String name;

    // The color used when rendering this section's label and item entries [d1n - finished]
    private final Color color;

    // List of lowercase item keywords for matching against ground item names [pal - ez]
    private final List<String> itemKeywords;

    public CustomSection(String name, Color color, String items)
    {
        this.name = name.trim();
        this.color = color;

        // Parse the comma-separated items string into a list of lowercase keywords.
        // We store them lowercase so matching can be case-insensitive.
        // Empty strings are filtered out to handle cases like "item1,,item2" gracefully xD.
        if (items == null || items.trim().isEmpty())
        {
            this.itemKeywords = new ArrayList<>();
        }
        else
        {
            this.itemKeywords = Arrays.stream(items.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }


    public boolean isEnabled()
    {
        return !name.isEmpty();
    }

    public boolean matchesItem(String itemName)
    {
        if (itemName == null || itemName.isEmpty())
        {
            return false;
        }

        // Convert to lowercase for case-insensitive matching
        String lowerItemName = itemName.toLowerCase();

        // Check if any of our keywords appear anywhere in the item name
        for (String keyword : itemKeywords)
        {
            if (lowerItemName.contains(keyword))
            {
                return true;
            }
        }

        return false;
    }

    public String getColorTag()
    {
        // Convert the RGB values to a hex string without the alpha channel.
        return String.format("<col=%02x%02x%02x>",
                color.getRed(),
                color.getGreen(),
                color.getBlue());
    }

    public static String getColorTagClose()
    {
        return "</col>";
    }

    @Override
    public String toString()
    {
        return "CustomSection{" +
                "name='" + name + '\'' +
                ", color=" + String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()) +
                ", keywords=" + itemKeywords.size() +
                '}';
    }
}
